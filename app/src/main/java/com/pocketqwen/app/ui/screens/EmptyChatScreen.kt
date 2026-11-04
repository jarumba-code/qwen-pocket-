package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun EmptyChatScreen(
    viewModel: ChatViewModel,
    userName: String = "",
    modifier: Modifier = Modifier
) {
    var draftSuggestion by remember { mutableStateOf("") }
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (userName.isBlank()) "Good day. What can I help you with?" else "Good day, $userName.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "What would you like to work on today?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            )
            Text(
                text = "Private, on-device AI conversations powered by a bundled local language model.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )
            val suggestions = listOf(
                "Explain a difficult concept",
                "Help me write some code",
                "Summarize a long text",
                "Brainstorm ideas"
            )
            suggestions.forEach { prompt ->
                OutlinedButton(
                    onClick = { draftSuggestion = prompt },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(prompt, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when (prompt) {
                                "Explain a difficult concept" -> "Break a topic into clear, simple steps."
                                "Help me write some code" -> "Work through a programming problem."
                                "Summarize a long text" -> "Find the key ideas and important details."
                                else -> "Explore ideas and possible approaches."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
        MessageComposer(
            onSend = viewModel::sendMessage,
            modifier = Modifier.fillMaxWidth(),
            initialText = draftSuggestion
        )
    }
}
