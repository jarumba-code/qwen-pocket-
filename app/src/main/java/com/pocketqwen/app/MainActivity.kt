package com.pocketqwen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.repository.MockAiRepository
import com.pocketqwen.app.ui.screens.MainScreen
import com.pocketqwen.app.ui.theme.PocketQwenTheme
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PocketQwenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Stage 1 only builds and packages pinned llama.cpp shared libraries.
                    // Keep the explicit placeholder repository active until the JNI bridge
                    // and model-loading path are connected and validated in a later stage.
                    val repository: AiRepository = MockAiRepository()
                    val chatViewModel: ChatViewModel =
                        viewModel(factory = ChatViewModelFactory(repository))
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(
                            context = this@MainActivity,
                            repository = repository
                        )
                    )
                    MainScreen(
                        chatViewModel = chatViewModel,
                        settingsViewModel = settingsViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
