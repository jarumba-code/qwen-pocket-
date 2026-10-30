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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun EmptyChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "What can I help you with?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Private, on-device AI conversations. The local model is not connected yet; messages currently receive clearly labeled preview responses.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
            )
            val suggestions = listOf(
                "Explain a difficult concept",
                "Help me write some code",
                "Summarize a long text",
                "Brainstorm ideas"
            )
            suggestions.forEach { prompt ->
                OutlinedButton(
                    onClick = { viewModel.sendMessage(prompt) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}