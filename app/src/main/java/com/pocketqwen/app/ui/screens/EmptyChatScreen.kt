package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.ui.components.SuggestionCard
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun EmptyChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "PocketQwen",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "What can I help you with?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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

        suggestionButton("Explain a difficult concept", "Get clear explanations of complex topics", viewModel)
        Spacer(modifier = Modifier.height(8.dp))
        suggestionButton("Help me write some code", "Write, review, and debug code snippets", viewModel)
        Spacer(modifier = Modifier.height(8.dp))
        suggestionButton("Summarize a long text", "Get concise summaries of lengthy content", viewModel)
        Spacer(modifier = Modifier.height(8.dp))
        suggestionButton("Brainstorm ideas", "Generate creative ideas together", viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Settings")
        }

        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.material3.TextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text(text = "Message PocketQwen...")
            },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Send")
        }
    }
}

@Composable
private fun suggestionButton(title: String, description: String, viewModel: ChatViewModel) {
    Button(
        onClick = { viewModel.sendMessage(title) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = androidx.compose.foundation.layout.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
