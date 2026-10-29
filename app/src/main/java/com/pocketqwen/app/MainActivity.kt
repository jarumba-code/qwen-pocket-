package com.pocketqwen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pocketqwen.app.repository.MockAiRepository
import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.ui.theme.PocketQwenTheme
import com.pocketqwen.app.ui.screens.MainScreen
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

class ChatViewModelFactory(private val repository: AiRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}

class SettingsViewModelFactory(
    private val context: android.content.Context,
    private val repository: AiRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
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
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val repository = MockAiRepository()
                    val chatViewModel = viewModel(factory = ChatViewModelFactory(repository))
                    val settingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(context = this@MainActivity, repository = repository)
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
