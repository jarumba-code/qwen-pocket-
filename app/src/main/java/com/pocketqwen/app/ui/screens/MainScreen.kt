package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showChat by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { showSettings = false },
            modifier = modifier
        )
    } else if (showChat) {
        EmptyChatScreen(
            onSendMessage = { msg ->
                chatViewModel.sendMessage(msg)
            },
            onNavigateToSettings = { showSettings = true },
            modifier = modifier
        )
    } else {
        ActiveChatScreen(
            viewModel = chatViewModel,
            onNavigateToSettings = { showSettings = true },
            onNewChat = { },
            modifier = modifier
        )
    }
}
