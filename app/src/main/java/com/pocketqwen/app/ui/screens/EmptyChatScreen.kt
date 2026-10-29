package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.ui.components.SuggestionCard
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun EmptyChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onMessageSent: (String) -> Unit = { viewModel.sendMessage(it) },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "PocketQwen",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "What can I help you with?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Private, on-device AI conversations. Your data stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))
        SuggestionCard(
            title = "Explain a difficult concept",
            description = "Break a topic down into clear, simple steps.",
            onClick = { onMessageSent("Explain a difficult concept") }
        )
        SuggestionCard(
            title = "Help me write some code",
            description = "Work through a programming problem.",
            onClick = { onMessageSent("Help me write some code") }
        )
        SuggestionCard(
            title = "Summarize a long text",
            description = "Find the key ideas and important details.",
            onClick = { onMessageSent("Summarize a long text") }
        )
        SuggestionCard(
            title = "Brainstorm ideas",
            description = "Explore ideas and possible approaches.",
            onClick = { onMessageSent("Brainstorm ideas") }
        )

        Spacer(modifier = Modifier.height(12.dp))
        MessageComposer(onSend = onMessageSent)
        TextButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Settings")
        }
    }
}
