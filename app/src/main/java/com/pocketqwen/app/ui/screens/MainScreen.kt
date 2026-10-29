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
    var showChat: Boolean = showChatState.value
        get() = showChatState.value
        set(value) { showChatState.value = value }
    val showSettingsState = remember { mutableStateOf(false) }
    var showSettings: Boolean
        get() = showSettingsState.value
        set(value) { showSettingsState.value = value }

    if (showSettings) {
        SettingsScreen(
            viewModel = settingsViewModel,
            onNavigateBack = { showSettings = false },
            modifier = modifier
        )
    } else {
        EmptyChatScreen(
            viewModel = chatViewModel,
            onNavigateToSettings = { showSettings = true },
            modifier = modifier
        )
    }
}
