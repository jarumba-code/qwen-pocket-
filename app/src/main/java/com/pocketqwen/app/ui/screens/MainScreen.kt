package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val showChatState = remember { mutableStateOf(true) }
    val showSettingsState = remember { mutableStateOf(false) }

    if (showSettingsState.value) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { showSettingsState.value = false },
            modifier = modifier
        )
    } else {
        EmptyChatScreen(
            viewModel = chatViewModel,
            onNavigateToSettings = { showSettingsState.value = true },
            modifier = modifier
        )
    }
}
