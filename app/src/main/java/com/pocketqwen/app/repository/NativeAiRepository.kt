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

/** llama.cpp-backed local repository. No prompt or response is sent to a cloud service. */
class NativeAiRepository(private val context: Context) : AiRepository {
    @Volatile private var loaded = false
    @Volatile private var status = "Bundled Qwen model is ready to initialize"
    private val generationActive = AtomicBoolean(false)
    private val initializationLock = Any()

    init { System.loadLibrary("pocketqwen_native") }

    override fun isModelAvailable(): Boolean = loaded
    override fun getModelStatus(): String = status
    override suspend fun initializeModel(): Boolean = initialize()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (loaded) return@withContext true
        synchronized(initializationLock) {
            if (loaded) return@synchronized true
            initializeLocked()
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
            loaded = nativeLoadModel(modelFile.absolutePath)
            status = if (loaded) "Qwen2.5-0.5B ready · on-device only"
                else "Native model initialization failed. Check available RAM and app logs."
            return loaded
        } catch (e: Exception) {
            temporaryFile.delete()
            status = "Model unavailable: " + (e.message ?: "asset copy failed")
            loaded = false
            return false
        }
    }

    override fun generateResponse(message: String): Flow<String> = callbackFlow {
        if (message.isBlank()) {
            close(IllegalArgumentException("Enter a message first."))
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
                nativeGenerateResponse(message.trim(), object : TokenCallback {
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
            generationActive.set(false)
        }
    }

    override fun cancelGeneration() {
        nativeCancelGeneration()
    }

    fun release() {
        nativeCancelGeneration()
        nativeRelease()
        loaded = false
        status = "Model unloaded"
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerateResponse(message: String, callback: TokenCallback)
    private external fun nativeCancelGeneration()
    private external fun nativeIsModelAvailable(): Boolean
    private external fun nativeGetModelStatus(): String
    private external fun nativeRelease()

    companion object {
        private const val LOG_TAG = "PocketQwenNative"
        private const val MODEL_FILENAME = "qwen.gguf"
        private const val MIN_MODEL_BYTES = 100_000_000L
    }
}
