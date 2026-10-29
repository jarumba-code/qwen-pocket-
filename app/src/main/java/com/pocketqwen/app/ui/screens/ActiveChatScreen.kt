package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.ui.components.MessageBubble
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun ActiveChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "PocketQwen", style = MaterialTheme.typography.titleLarge)
                },
                startIcon = {
                    IconButton(onClick = { /* drawer opened via navigation host */ }) {
                        Icon(
                            imageVector = Menu,
                            contentDescription = "Open navigation drawer"
                        )
                    }
                },
                endIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = androidx.compose.material3.icons.filled.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = MaterialTheme.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
            if (isLoading) {
                MessageBubble(
                    message = ChatMessage(
                        id = "loading",
                        text = "Thinking...",
                        isUser = false,
                        status = ChatMessage.MessageStatus.SENDING
                    )
                )
            }
            if (error != null) {
                androidx.compose.material3.Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        MessageComposer(onSend = { msg ->
            viewModel.sendMessage(msg)
        }, modifier = Modifier.fillMaxWidth())
    }
}
