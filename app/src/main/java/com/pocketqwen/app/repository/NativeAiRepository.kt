package com.pocketqwen.app.repository

import android.app.ActivityManager
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
    private data class PreparedPrompt(val text: String, val tokenCount: Int, val availableOutputTokens: Int)

    @Volatile private var loaded = false
    @Volatile private var status = "Bundled Qwen model is ready to initialize"
    @Volatile private var performanceMode = PerformanceMode.PERFORMANCE
    @Volatile private var configuredContextTokens = recommendedContextWindow()
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

    private fun recommendedContextWindow(): Int {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return 2048
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        // Android reserves part of advertised RAM for firmware. A 3.5 GB
        // reported device is treated as a nominal 4 GB class handset.
        val fourGbClass = memory.totalMem >= 3_500_000_000L
        val available = memory.availMem
        return when {
            fourGbClass && available >= 850_000_000L -> 8192
            fourGbClass -> 4096
            available >= 950_000_000L -> 4096
            else -> 2048
        }
    }

    private fun initializeLocked(): Boolean {
        configuredContextTokens = recommendedContextWindow()
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
            status = if (loaded) "Qwen2.5-0.5B ready · ${configuredContextTokens}-token context · ${performanceMode.name.lowercase().replaceFirstChar { it.uppercase() }} mode · on-device only"
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
        require(tokens == 2048 || tokens == 4096 || tokens == 8192) { "Supported context windows are 2,048, 4,096 and 8,192 tokens." }
        synchronized(initializationLock) {
            if (configuredContextTokens == tokens) return@synchronized
            val wasLoaded = loaded
            configuredContextTokens = tokens
            if (wasLoaded) {
                modelOperationInProgress = true
                try {
                    nativeCancelGeneration(); nativeRelease(); loaded = false
                    status = "Reloading model with $tokens-token context…"
                    initializeLocked()
                } finally { modelOperationInProgress = false }
            }
        }
    }
    override suspend fun countContextTokens(messages: List<ChatMessage>): Int? = withContext(Dispatchers.IO) {
        if (!loaded) return@withContext null
        if (messages.isEmpty()) return@withContext 0
        runCatching { buildPrompt(messages, null).tokenCount }.getOrNull()?.takeIf { it >= 0 }
    }
    override fun generateResponse(messages: List<ChatMessage>): Flow<String> = generateResponse(messages, null)
    override fun generateResponse(messages: List<ChatMessage>, additionalContext: String?): Flow<String> = callbackFlow {
        val latestUserMessage = messages.lastOrNull { it.isUser }?.text.orEmpty()
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
                val preparedPrompt = buildPrompt(messages, additionalContext)
                nativeGenerateResponse(preparedPrompt.text, preparedPrompt.availableOutputTokens, object : TokenCallback {
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
    /**
     * Build a context-bounded Qwen prompt using llama.cpp's actual tokenizer.
     * Oldest complete turns are removed first; the latest user message is never silently dropped.
     * Generation receives every token position remaining in the context window.
     */
    private fun buildPrompt(messages: List<ChatMessage>, additionalContext: String?): PreparedPrompt {
        val system = """You are EKIP, a helpful, careful, practical assistant powered by a local model on this device.
Answer the user's latest request directly. Use earlier messages only when they are relevant to the current topic; do not carry unrelated subjects into a new question.
Be clear and naturally conversational. Start with the answer, then add useful explanation. Ask one focused clarification only when essential information is missing. For brainstorming, provide genuinely distinct options, avoid repeating or merely rewording earlier ideas, and do not recycle the same examples.
For programming, explain assumptions, provide complete code where appropriate, and never claim code was tested unless it was actually tested.
For factual, legal, financial, and technical questions, distinguish known facts from estimates. Admit uncertainty and do not invent sources, citations, capabilities, or current information.
For health questions, provide general educational information and safe next steps without diagnosing. Do not automatically refuse ordinary symptom questions. Explain when professional assessment is appropriate and direct urgent symptoms to local emergency medical care.
Keep all responses age-appropriate. Do not provide instructions that facilitate violence, self-harm, exploitation or abuse of children, dangerous challenges, or other serious harm. For unsafe requests, briefly explain the limit and redirect to a safe alternative.
When no web-search context is supplied, do not claim to browse the internet or use external tools. When web-search excerpts are supplied, you may summarize them and cite their URLs while noting possible incompleteness or staleness. The local model may be wrong; be transparent when confidence is limited."""
        // Reserve output room while trimming history. The final output capacity is calculated
        // from the exact prompt token count instead of a fixed generation-token ceiling.
        val searchLimit = ((configuredContextTokens - MIN_OUTPUT_RESERVE - CONTEXT_SAFETY_MARGIN - 300) * 3)
            .coerceIn(300, 12000)
        val searchContext = additionalContext?.takeIf { it.isNotBlank() }
            ?.let { "\n\n" + it.take(searchLimit) }.orEmpty()
        val prefix = "<|im_start|>system\n${system}${searchContext}\n<|im_end|>\n"
        val suffix = "<|im_start|>assistant\n"
        val validMessages = messages.filter {
            it.status != MessageStatus.SENDING && it.text.isNotBlank()
        }
        val latestUser = validMessages.lastOrNull { it.isUser }
            ?: throw IllegalArgumentException("Enter a message first.")
        val retained = validMessages.toMutableList()
        val promptBudget = configuredContextTokens - MIN_OUTPUT_RESERVE - CONTEXT_SAFETY_MARGIN

        fun render(): String = prefix + retained.joinToString("") { message ->
            val role = if (message.isUser) "user" else "assistant"
            "<|im_start|>$role\n${message.text}\n<|im_end|>\n"
        } + suffix

        var prompt = render()
        var tokenCount = nativeCountTokens(prompt)
        while (tokenCount > promptBudget && retained.size > 1) {
            // Drop the oldest complete user/assistant turn where possible.
            val removeCount = if (retained.size >= 3 && retained[0].isUser && !retained[1].isUser) 2 else 1
            repeat(minOf(removeCount, retained.size - 1)) { retained.removeAt(0) }
            prompt = render()
            tokenCount = nativeCountTokens(prompt)
        }
        if (tokenCount < 0 || tokenCount > promptBudget || retained.none { it.id == latestUser.id }) {
            throw IllegalArgumentException(
                "This request exceeds the selected ${configuredContextTokens}-token context window. Shorten it or select a larger context."
            )
        }

        val availableOutputTokens = configuredContextTokens - tokenCount
        if (availableOutputTokens <= 0) {
            throw IllegalArgumentException("No generation capacity remains in the local context window.")
        }
        return PreparedPrompt(prompt, tokenCount, availableOutputTokens)
    }

    private fun isIdentityQuestion(message: String): Boolean {
        val normalized = message.trim().lowercase().replace(Regex("[.!?\\s]+$"), "")
        return normalized == "who are you" || normalized == "what are you"
    }

    private external fun nativeLoadModel(modelPath: String, performanceMode: Int, contextTokens: Int): Boolean
    private external fun nativeGenerateResponse(message: String, availableOutputTokens: Int, callback: TokenCallback)
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
        private const val MIN_OUTPUT_RESERVE = 128
        private const val IDENTITY_RESPONSE =
            "I am EKIP, an AI assistant that uses a local language model on this device. Ordinary chat does not require cloud inference."
    }
}
