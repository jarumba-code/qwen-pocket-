package com.pocketqwen.app.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * llama.cpp-backed repository. Model weights remain separate from the APK.
 * All loading and token generation are dispatched off Android's main thread.
 */
class LlamaRepository(context: Context) : AiRepository {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val modelStatus = MutableStateFlow("Checking local inference engine…")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadMutex = Mutex()
    private val nativeLibraryAvailable: Boolean

    init {
        nativeLibraryAvailable = try {
            System.loadLibrary("pocketqwen_native")
            true
        } catch (error: UnsatisfiedLinkError) {
            modelStatus.value = "Native inference library unavailable: ${error.message ?: "library load failed"}"
            false
        } catch (error: SecurityException) {
            modelStatus.value = "Native inference library could not be loaded."
            false
        }

        if (nativeLibraryAvailable) {
            val savedPath = preferences.getString(KEY_MODEL_PATH, null)
            val savedFile = savedPath?.let(::File)
            if (savedFile != null && savedFile.isFile) {
                modelStatus.value = "Restoring local model…"
                scope.launch {
                    loadMutex.withLock {
                        loadModelBlocking(savedFile.absolutePath, persistPath = false)
                    }
                }
            } else {
                modelStatus.value = "No model selected — choose a local .gguf file in Settings."
            }
        }
    }

    override fun isModelAvailable(): Boolean {
        if (!nativeLibraryAvailable) return false
        return runCatching { nativeIsLoaded() }.getOrDefault(false)
    }

    override fun getModelStatus(): String = modelStatus.value

    override fun observeModelStatus(): Flow<String> = modelStatus.asStateFlow()

    override suspend fun loadModel(modelPath: String): Boolean =
        withContext(Dispatchers.IO) {
            loadMutex.withLock {
                loadModelBlocking(modelPath, persistPath = true)
            }
        }

    private fun loadModelBlocking(modelPath: String, persistPath: Boolean): Boolean {
        if (!nativeLibraryAvailable) {
            modelStatus.value = "Native inference library is unavailable in this build."
            return false
        }

        val file = File(modelPath).absoluteFile
        if (!file.isFile || !file.name.endsWith(".gguf", ignoreCase = true)) {
            modelStatus.value = "Choose a valid local .gguf model file."
            return false
        }

        modelStatus.value = "Loading ${file.name}…"
        return try {
            val loaded = nativeLoadModel(file.absolutePath)
            if (!loaded) {
                modelStatus.value = "Could not load ${file.name}."
                return false
            }

            if (persistPath) {
                val previousPath = preferences.getString(KEY_MODEL_PATH, null)
                preferences.edit().putString(KEY_MODEL_PATH, file.absolutePath).apply()
                // Remove an older app-private copy only after the replacement has loaded.
                val modelDirectory = File(appContext.filesDir, "models").canonicalFile
                if (!previousPath.isNullOrBlank() && previousPath != file.absolutePath) {
                    val previous = runCatching { File(previousPath).canonicalFile }.getOrNull()
                    if (previous != null &&
                        previous.path.startsWith(modelDirectory.path + File.separator) &&
                        previous.isFile
                    ) {
                        previous.delete()
                    }
                }
            }

            modelStatus.value = "Loaded locally: ${file.name}"
            true
        } catch (error: Exception) {
            modelStatus.value = "Model load failed: ${error.message ?: error.javaClass.simpleName}"
            false
        }
    }

    override fun generateResponse(message: String): Flow<String> = flow {
        emit("[Thinking...]")
        val response = withContext(Dispatchers.Default) {
            if (!nativeLibraryAvailable) {
                throw IllegalStateException("Native inference is unavailable in this APK.")
            }
            if (!nativeIsLoaded()) {
                throw IllegalStateException("No model is loaded. Open Settings and choose a local .gguf file.")
            }
            nativeGenerate(message)
        }
        if (response.isBlank()) {
            throw IllegalStateException("The local model returned an empty response.")
        }
        emit(response)
    }

    /** Called when the activity is finishing; the selected model path remains saved. */
    fun release() {
        if (!nativeLibraryAvailable) return
        scope.launch {
            runCatching { nativeRelease() }
        }
    }

    private external fun nativeLoadModel(modelPath: String): Boolean
    private external fun nativeGenerate(message: String): String
    private external fun nativeIsLoaded(): Boolean
    private external fun nativeRelease()

    private companion object {
        const val PREFS_NAME = "pocketqwen_local_model"
        const val KEY_MODEL_PATH = "model_path"
    }
}
