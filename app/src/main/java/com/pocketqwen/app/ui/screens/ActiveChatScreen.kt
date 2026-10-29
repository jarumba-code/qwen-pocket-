package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun ActiveChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (message.isUser) {
                        Column(
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = androidx.compose.foundation.layout.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = androidx.compose.foundation.layout.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                            if (message.status == MessageStatus.SENDING) {
                                Text(
                                    text = "...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = androidx.compose.foundation.layout.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = androidx.compose.foundation.layout.padding(horizontal = 16.dp)
                )
            }
        }

        androidx.compose.material3.TextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text(text = "Message PocketQwen...")
            },
            singleLine = false,
            maxLines = 5,
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
