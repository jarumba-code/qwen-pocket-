package com.pocketqwen.app.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * llama.cpp-backed local repository. The model is bundled as assets/qwen.gguf
 * by the Android build workflow and copied to private app storage on first use.
 */
class NativeAiRepository(private val context: Context) : AiRepository {
    @Volatile private var loaded = false
    @Volatile private var status = "Preparing bundled Qwen model"

    init {
        System.loadLibrary("pocketqwen_native")
    }

    override fun isModelAvailable(): Boolean = loaded

    override fun getModelStatus(): String = status

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (loaded) return@withContext true
        try {
            val modelFile = File(context.filesDir, MODEL_FILENAME)
            if (!modelFile.exists() || modelFile.length() < MIN_MODEL_BYTES) {
                context.assets.open(MODEL_FILENAME).use { input ->
                    modelFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            status = "Loading Qwen2.5-0.5B locally…"
            loaded = nativeLoadModel(modelFile.absolutePath)
            status = if (loaded) "Qwen2.5-0.5B ready · on-device" else "Model failed to load"
            loaded
        } catch (e: Exception) {
            status = "Model unavailable: ${e.message ?: "asset copy failed"}"
            loaded = false
            false
        }
    }

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")
        val result = withContext(Dispatchers.IO) {
            if (!loaded && !initialize()) {
                throw IllegalStateException(status)
            }
            nativeGenerateResponse(message)
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
