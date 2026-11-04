package com.pocketqwen.app.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import com.pocketqwen.app.settings.PerformanceMode
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus

/** llama.cpp-backed local repository. No prompt or response is sent to a cloud service. */
class NativeAiRepository(private val context: Context) : AiRepository {
    @Volatile private var loaded = false
    @Volatile private var status = "Bundled Qwen model is ready to initialize"
    @Volatile private var performanceMode = PerformanceMode.BALANCED
    @Volatile private var configuredContextTokens = 1024
    @Volatile private var modelOperationInProgress = false
    private val generationActive = AtomicBoolean(false)
    private val initializationLock = Any()

    init { System.loadLibrary("pocketqwen_native") }

    override fun isModelAvailable(): Boolean = loaded
    override fun getModelStatus(): String = status
    override suspend fun initializeModel(): Boolean = initialize()

    override suspend fun configurePerformanceMode(mode: PerformanceMode) = withContext(Dispatchers.IO) {
        synchronized(initializationLock) {
            if (performanceMode == mode) return@synchronized
            val wasLoaded = loaded
            performanceMode = mode
            if (wasLoaded) {
                modelOperationInProgress = true
                try {
                    status = "Applying ${mode.name.lowercase().replaceFirstChar { it.uppercase() }} mode…"
                    nativeCancelGeneration()
                    nativeRelease()
                    loaded = false
                    status = "Reloading model with ${mode.name.lowercase().replaceFirstChar { it.uppercase() }} mode…"
                    initializeLocked()
                } finally {
                    modelOperationInProgress = false
                }
            }
        }
    }

    override suspend fun releaseModel() = withContext(Dispatchers.IO) {
        synchronized(initializationLock) {
            modelOperationInProgress = true
            try {
                if (loaded || nativeIsModelAvailable()) {
                    status = "Ejecting local model…"
                    nativeCancelGeneration()
                    nativeRelease()
                }
                loaded = false
                status = "Model unloaded"
            } finally {
                modelOperationInProgress = false
            }
        }
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (loaded) return@withContext true
        synchronized(initializationLock) {
            if (loaded) return@synchronized true
            modelOperationInProgress = true
            try {
                initializeLocked()
            } finally {
                modelOperationInProgress = false
            }
        }
    }

    private fun initializeLocked(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        val temporaryFile = File(context.filesDir, "$MODEL_FILENAME.part")
        try {
            status = "Checking bundled model…"
            val assetExists = runCatching { context.assets.open(MODEL_FILENAME).use { }; true }.getOrDefault(false)
            if (!assetExists) throw IOException("Bundled qwen.gguf asset is missing from this APK")
            if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) {
                status = "Copying bundled model to private storage…"
                temporaryFile.delete()
                context.assets.open(MODEL_FILENAME).use { input ->
                    temporaryFile.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                val copiedLength = temporaryFile.length()
                if (copiedLength < MIN_MODEL_BYTES) {
                    temporaryFile.delete()
                    throw IOException("Bundled model is incomplete ($copiedLength bytes)")
                }
                if (modelFile.exists() && !modelFile.delete()) throw IOException("Could not replace incomplete model file")
                if (!temporaryFile.renameTo(modelFile)) throw IOException("Could not finalize model copy")
            }
            status = "Loading Qwen model into memory…"
            loaded = nativeLoadModel(modelFile.absolutePath, performanceMode.nativeValue, configuredContextTokens)
            status = if (loaded) "Qwen2.5-0.5B ready · ${performanceMode.name.lowercase().replaceFirstChar { it.uppercase() }} mode · on-device only"
                else "Native model initialization failed. Check available RAM and app logs."
            return loaded
        } catch (e: Exception) {
            temporaryFile.delete()
            status = "Model unavailable: " + (e.message ?: "asset copy failed")
            loaded = false
            return false
        }
    }

    override val contextWindowTokens: Int get() = configuredContextTokens

    override suspend fun configureContextWindow(tokens: Int) = withContext(Dispatchers.IO) {
        require(tokens == 1024 || tokens == 2048) { "Supported context windows are 1,024 and 2,048 tokens." }
        synchronized(initializationLock) {
            if (configuredContextTokens == tokens) return@synchronized
            val wasLoaded = loaded
            configuredContextTokens = tokens
            if (wasLoaded) {
                modelOperationInProgress = true
                try {
                    nativeCancelGeneration()
                    nativeRelease()
                    loaded = false
                    status = "Reloading model with $tokens-token context…"
                    initializeLocked()
                } finally { modelOperationInProgress = false }
            }
        }
    }

    override suspend fun countContextTokens(messages: List<ChatMessage>): Int? = withContext(Dispatchers.IO) {
        if (!loaded || messages.isEmpty()) return@withContext 0
        runCatching {
            val latest = messages.lastOrNull { it.isUser }?.text.orEmpty()
            val budget = minOf(selectOutputBudget(latest), (configuredContextTokens / 3).coerceAtLeast(128))
            nativeCountTokens(buildPrompt(messages, budget, null))
        }.getOrNull()?.takeIf { it >= 0 }
    }

    override fun generateResponse(messages: List<ChatMessage>): Flow<String> = generateResponse(messages, null)

    override fun generateResponse(messages: List<ChatMessage>, additionalContext: String?): Flow<String> = callbackFlow {
        val latestUserMessage = messages.lastOrNull { it.isUser }?.text.orEmpty()
        val outputBudget = selectOutputBudget(latestUserMessage)
        if (latestUserMessage.isBlank()) {
            close(IllegalArgumentException("Enter a message first."))
            return@callbackFlow
        }
        if (isIdentityQuestion(latestUserMessage)) {
            trySend(IDENTITY_RESPONSE)
            close()
            return@callbackFlow
        }
        if (modelOperationInProgress) {
            close(IllegalStateException("The local model is loading, unloading, or changing modes. Please try again shortly."))
            return@callbackFlow
        }
        if (generationActive.getAndSet(true)) {
            close(IllegalStateException("A response is already being generated."))
            return@callbackFlow
        }
        val worker = launch(Dispatchers.IO) {
            val responseStartNs = System.nanoTime()
            try {
                if (!loaded && !initialize()) throw IllegalStateException(status)
                val prompt = buildPrompt(messages, outputBudget, additionalContext)
                nativeGenerateResponse(prompt, outputBudget, object : TokenCallback {
                    override fun onToken(text: String) {
                        if (text.isNotEmpty()) trySend(text)
                    }
                })
                close()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                close(e)
            } finally {
                Log.i(LOG_TAG, "response_total_ms=${(System.nanoTime() - responseStartNs) / 1_000_000.0}")
                generationActive.set(false)
            }
        }
        awaitClose {
            if (worker.isActive) {
                nativeCancelGeneration()
                worker.cancel()
            }
            // Keep generationActive set until the native call really returns.
            // Cancelling the coroutine does not interrupt a synchronous JNI call.
        }
    }

    override fun cancelGeneration() {
        nativeCancelGeneration()
    }

    /**
     * Build a Qwen chat-template prompt with recent turns from this conversation.
     * Uses the actual llama.cpp tokenizer to trim older history to the selected context.
     */
    private fun buildPrompt(messages: List<ChatMessage>, outputBudget: Int, additionalContext: String?): String {
        val system = """You are Ekip, a helpful, careful, practical assistant powered by a local model on this device.
Answer the user's latest request directly. Use earlier messages only when they are relevant to the current topic; do not carry unrelated subjects into a new question.
Be clear and naturally conversational. Start with the answer, then add useful explanation. Ask one focused clarification only when essential information is missing.
For programming, explain assumptions, provide complete code where appropriate, and never claim code was tested unless it was actually tested.
For factual, legal, financial, and technical questions, distinguish known facts from estimates. Admit uncertainty and do not invent sources, citations, capabilities, or current information.
For health questions, provide general educational information and safe next steps without diagnosing. Do not automatically refuse ordinary symptom questions. Explain when professional assessment is appropriate and direct urgent symptoms to local emergency medical care.
Keep all responses age-appropriate. Do not provide instructions that facilitate violence, self-harm, exploitation or abuse of children, dangerous challenges, or other serious harm. For unsafe requests, briefly explain the limit and redirect to a safe alternative.
When no web-search context is supplied, do not claim to browse the internet or use external tools. When web-search excerpts are supplied, you may summarize those excerpts and cite their provided URLs, while making clear that results can be incomplete or stale. The local model may be wrong; be transparent when confidence is limited."""
        val searchLimit = if (configuredContextTokens <= 1024) 1400 else 3600
        val searchContext = additionalContext?.takeIf { it.isNotBlank() }?.let { "\n\n" + it.take(searchLimit) }.orEmpty()
        val prefix = "<|im_start|>system\n${system}${searchContext}\n<|im_end|>\n"
        val suffix = "<|im_start|>assistant\n"
        val validMessages = messages.filter {
            it.status != MessageStatus.SENDING && it.text.isNotBlank()
        }
        val latestUser = validMessages.lastOrNull { it.isUser }
            ?: throw IllegalArgumentException("Enter a message first.")
        val retained = validMessages.toMutableList()
        val promptBudget = configuredContextTokens - outputBudget - CONTEXT_SAFETY_MARGIN
        fun render(): String = prefix + retained.joinToString("") { message ->
            val role = if (message.isUser) "user" else "assistant"
            "<|im_start|>$role\n${message.text}\n<|im_end|>\n"
        } + suffix
        var prompt = render()
        var tokenCount = nativeCountTokens(prompt)
        while (tokenCount > promptBudget && retained.size > 1) {
            retained.removeAt(0)
            prompt = render()
            tokenCount = nativeCountTokens(prompt)
        }
        if (tokenCount < 0 || tokenCount > promptBudget || retained.none { it.id == latestUser.id }) {
            throw IllegalArgumentException("This request exceeds the selected ${configuredContextTokens}-token context window. Shorten it or select a larger context.")
        }
        return prompt
    }

    private fun selectOutputBudget(message: String): Int {
        val text = message.lowercase()
        val essay = text.contains("essay") || text.contains("500 word") ||
            text.contains("500-word") || text.contains("600 word") ||
            text.contains("700 word") || text.contains("800 word") ||
            text.contains("1000 word") || text.contains("long-form") ||
            text.contains("detailed report")
        val detailed = text.contains("explain in detail") || text.contains("comprehensive") ||
            text.contains("in depth") || text.contains("step by step") ||
            text.contains("long answer") || text.contains("detailed explanation")
        val code = (text.contains("write") || text.contains("generate") ||
            text.contains("create") || text.contains("build")) &&
            (text.contains("code") || text.contains("program") || text.contains("script") ||
            text.contains("function") || text.contains("class") || text.contains("app"))
        return when {
            essay -> 512
            code || detailed -> 384
            text.length > 700 -> 256
            else -> 192
        }
    }

    private fun isIdentityQuestion(message: String): Boolean {
        val normalized = message.trim().lowercase().replace(Regex("[.!?\\s]+$"), "")
        return normalized == "who are you" || normalized == "what are you"
    }

    private external fun nativeLoadModel(modelPath: String, performanceMode: Int, contextTokens: Int): Boolean
    private external fun nativeGenerateResponse(message: String, maxTokens: Int, callback: TokenCallback)
    private external fun nativeCancelGeneration()
    private external fun nativeIsModelAvailable(): Boolean
    private external fun nativeGetModelStatus(): String
    private external fun nativeCountTokens(prompt: String): Int
    private external fun nativeRelease()

    companion object {
        private const val LOG_TAG = "EkipNative"
        private const val MODEL_FILENAME = "qwen.gguf"
        private const val MIN_MODEL_BYTES = 100_000_000L
        private const val CONTEXT_SAFETY_MARGIN = 64
        private const val IDENTITY_RESPONSE =
            "I am Ekip, an AI assistant that uses a local language model on this device. Ordinary chat does not require cloud inference."
    }
}
