package com.pocketqwen.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel

@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val messages by chatViewModel.messages.collectAsState()
    BackHandler(enabled = showSettings) { showSettings = false }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PocketQwen",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (messages.isNotEmpty() && !showSettings) {
                    TextButton(onClick = { chatViewModel.clearMessages() }) {
                        Text("New chat")
                    }
                }
                TextButton(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "Back" else "Settings")
                }
            }
        }

        if (showSettings) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { showSettings = false },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else if (messages.isEmpty()) {
            EmptyChatScreen(
                viewModel = chatViewModel,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            ActiveChatScreen(
                viewModel = chatViewModel,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}