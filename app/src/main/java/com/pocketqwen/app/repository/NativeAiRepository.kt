package com.pocketqwen.app.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Real llama.cpp-backed AI repository.
 *
 * All native inference runs off the main thread via a JNI bridge.
 * Model loading and generation are done synchronously inside the native
 * layer; this wrapper simply marshals calls onto a background dispatcher.
 *
 * Until a model file is loaded, every call returns an error indicating the
 * model is not installed.
 */
class LlamaRepository(private val context: android.content.Context) : AiRepository {

    private var isModelLoaded = false

    override fun isModelAvailable(): Boolean = isModelLoaded

    override fun getModelStatus(): String {
        return if (isModelLoaded) "Local model loaded" else "Local model not installed"
    }

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")

        val result = withContext(Dispatchers.IO) {
            nativeGenerateResponse(message)
        }

        emit(result)
        emit("[End of response]")
    }

    /**
     * Attempt to load a model from the given path. Returns true on success.
     */
    fun loadModel(modelPath: String): Boolean {
        val result = nativeLoadModel(modelPath)
        isModelLoaded = result
        return result
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerateResponse(message: String): String
    private external fun nativeIsModelAvailable(): Boolean
    private external fun nativeGetModelStatus(): String
}
