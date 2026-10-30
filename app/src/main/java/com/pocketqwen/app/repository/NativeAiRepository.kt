package com.pocketqwen.app.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** llama.cpp-backed local repository. The CI build bundles assets/qwen.gguf. */
class NativeAiRepository(private val context: Context) : AiRepository {
    @Volatile private var loaded = false
    @Volatile private var status = "Bundled Qwen model is ready to initialize"

    init { System.loadLibrary("pocketqwen_native") }

    override fun isModelAvailable(): Boolean = loaded
    override fun getModelStatus(): String = status
    override suspend fun initializeModel(): Boolean = initialize()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (loaded) return@withContext true
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        val temporaryFile = File(context.filesDir, "$MODEL_FILENAME.part")
        try {
            status = "Copying bundled Qwen model to private storage…"
            if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) {
                temporaryFile.delete()
                context.assets.open(MODEL_FILENAME).use { input ->
                    temporaryFile.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                val copiedLength = temporaryFile.length()
                if (copiedLength < MIN_MODEL_BYTES) {
                    temporaryFile.delete()
                    throw IOException("Bundled model file is incomplete ($copiedLength bytes)")
                }
                if (modelFile.exists() && !modelFile.delete()) {
                    throw IOException("Could not replace an incomplete model file")
                }
                if (!temporaryFile.renameTo(modelFile)) {
                    throw IOException("Could not finalize model copy in private storage")
                }
            }
            status = "Loading Qwen2.5-0.5B locally…"
            loaded = nativeLoadModel(modelFile.absolutePath)
            status = if (loaded) "Qwen2.5-0.5B ready · on-device only"
                else "Model failed to load; check device memory and model file"
            loaded
        } catch (e: Exception) {
            temporaryFile.delete()
            status = "Model unavailable: ${e.message ?: "asset copy failed"}"
            loaded = false
            false
        }
    }

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")
        val result = withContext(Dispatchers.IO) {
            if (!loaded && !initialize()) throw IllegalStateException(status)
            val trimmed = message.trim()
            if (trimmed.isEmpty()) throw IllegalArgumentException("Enter a message first.")
            nativeGenerateResponse(trimmed).also {
                if (it.isBlank()) throw IllegalStateException("Qwen returned an empty response.")
            }
        }
        emit(result)
        emit("[End of response]")
    }

    fun release() {
        nativeRelease()
        loaded = false
        status = "Model unloaded"
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerateResponse(message: String): String
    private external fun nativeIsModelAvailable(): Boolean
    private external fun nativeGetModelStatus(): String
    private external fun nativeRelease()

    companion object {
        private const val MODEL_FILENAME = "qwen.gguf"
        private const val MIN_MODEL_BYTES = 100_000_000L
    }
}
