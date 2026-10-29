package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    onSelectModel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showChat by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            showSettings -> SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { showSettings = false },
                onSelectModel = onSelectModel,
                modifier = Modifier.fillMaxSize()
            )

            showChat -> ActiveChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { showSettings = true },
                modifier = Modifier.fillMaxSize()
            )

            else -> EmptyChatScreen(
                viewModel = chatViewModel,
                onNavigateToSettings = { showSettings = true },
                onMessageSent = { message ->
                    chatViewModel.sendMessage(message)
                    showChat = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
