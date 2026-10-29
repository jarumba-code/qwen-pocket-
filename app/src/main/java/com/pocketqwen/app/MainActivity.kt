package com.pocketqwen.app

import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.repository.LlamaRepository
import com.pocketqwen.app.ui.screens.MainScreen
import com.pocketqwen.app.ui.theme.PocketQwenTheme
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class ChatViewModelFactory(
    private val repository: AiRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

class SettingsViewModelFactory(
    private val context: android.content.Context,
    private val repository: AiRepository
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

class MainActivity : ComponentActivity() {
    private val aiRepository: LlamaRepository by lazy { LlamaRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = aiRepository
        setContent {
            PocketQwenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val chatViewModel: ChatViewModel =
                        viewModel(factory = ChatViewModelFactory(repository))
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(
                            context = this@MainActivity,
                            repository = repository
                        )
                    )
                    val scope = rememberCoroutineScope()
                    val modelPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            scope.launch {
                                settingsViewModel.setModelStatus("Copying selected GGUF model into app storage…")
                                try {
                                    val modelFile = withContext(Dispatchers.IO) {
                                        copyModelToPrivateStorage(uri)
                                    }
                                    settingsViewModel.loadModel(modelFile.absolutePath)
                                } catch (error: Exception) {
                                    settingsViewModel.setModelStatus(
                                        "Model selection failed: ${error.message ?: error.javaClass.simpleName}"
                                    )
                                }
                            }
                        }
                    }

                    MainScreen(
                        chatViewModel = chatViewModel,
                        settingsViewModel = settingsViewModel,
                        onSelectModel = { modelPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            aiRepository.release()
        }
        super.onDestroy()
    }

    private fun copyModelToPrivateStorage(uri: Uri): File {
        var displayName: String? = null
        var reportedSize = -1L
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) reportedSize = cursor.getLong(sizeIndex)
            }
        }

        val originalName = displayName ?: "selected-model.gguf"
        if (!originalName.lowercase(Locale.ROOT).endsWith(".gguf")) {
            throw IllegalArgumentException("Choose a GGUF model file with a .gguf extension.")
        }

        val requiredBytes = if (reportedSize > 0) reportedSize + 64L * 1024L * 1024L else 0L
        if (requiredBytes > 0 && StatFs(filesDir.absolutePath).availableBytes < requiredBytes) {
            throw IOException("Not enough free storage to copy this model. Free up space and try again.")
        }

        val modelDirectory = File(filesDir, "models")
        if (!modelDirectory.exists() && !modelDirectory.mkdirs()) {
            throw IOException("Unable to create app-private model storage.")
        }
        val safeName = originalName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(100)
        val destination = File(modelDirectory, "${System.currentTimeMillis()}-$safeName")
        val temporary = File(modelDirectory, destination.name + ".partial")

        try {
            val input = contentResolver.openInputStream(uri)
                ?: throw IOException("The selected file could not be opened.")
            input.use { source ->
                FileOutputStream(temporary).use { target ->
                    source.copyTo(target, 64 * 1024)
                }
            }
            if (temporary.length() <= 0L) {
                throw IOException("The selected model file is empty.")
            }
            if (!temporary.renameTo(destination)) {
                throw IOException("Could not finalize the model copy in app storage.")
            }
            return destination
        } catch (error: Exception) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }
}
