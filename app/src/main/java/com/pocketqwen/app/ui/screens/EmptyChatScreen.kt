package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun EmptyChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputTextState = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "PocketQwen",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "What can I help you with?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "PocketQwen is designed for private, on-device AI conversations. Your data stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.sendMessage("Explain a difficult concept") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Explain a difficult concept")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.sendMessage("Help me write some code") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Help me write some code")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.sendMessage("Summarize a long text") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Summarize a long text")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.sendMessage("Brainstorm ideas") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Brainstorm ideas")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.TextField(
            value = inputTextState.value,
            onValueChange = { inputTextState.value = it },
            placeholder = {
                Text(text = "Message PocketQwen...")
            },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (inputTextState.value.isNotBlank()) {
                    viewModel.sendMessage(inputTextState.value)
                    inputTextState.value = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Send")
        }
    }
}
