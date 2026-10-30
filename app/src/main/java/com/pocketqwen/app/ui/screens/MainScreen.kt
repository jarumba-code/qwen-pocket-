package com.pocketqwen.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import com.pocketqwen.app.model.Conversation
import com.pocketqwen.app.storage.ConversationStore
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.viewmodel.ChatViewModel
import com.pocketqwen.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }
    var activeConversationId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val context = LocalContext.current
    val conversationStore = remember(context) { ConversationStore(context) }
    val conversations = remember { mutableStateListOf<Conversation>().apply { addAll(conversationStore.loadAll()) } }
    val messages by chatViewModel.messages.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeConversationId) { val id = activeConversationId; if (id != null) { val saved = conversations.firstOrNull { it.id == id }?.messages ?: emptyList(); chatViewModel.activateConversation(id, saved) } }

    // Keep the current conversation snapshot updated while the user chats.
    LaunchedEffect(messages, activeConversationId) {
        if (messages.isNotEmpty()) {
            val id = activeConversationId ?: java.util.UUID.randomUUID().toString().also {
                activeConversationId = it
            }
            val title = messages.firstOrNull { it.isUser }?.text
                ?.replace("\n", " ")?.take(42) ?: "New conversation"
            val index = conversations.indexOfFirst { it.id == id }
            val snapshot = Conversation(id, title, messages.toList(), System.currentTimeMillis())
            if (index >= 0) conversations[index] = snapshot else conversations.add(0, snapshot)
        }
    }

    LaunchedEffect(conversations.toList()) {
        conversationStore.save(conversations.toList())
    }

    BackHandler(enabled = showSettings || drawerState.isOpen) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else showSettings = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier.fillMaxSize(),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.width(304.dp)
            ) {
                Text(
                    "PocketQwen",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 18.dp)
                )
                NavigationDrawerItem(
                    label = { Text("New chat") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        if (messages.isNotEmpty()) {
                            val id = activeConversationId ?: java.util.UUID.randomUUID().toString()
                            val title = messages.firstOrNull { it.isUser }?.text?.take(42) ?: "New conversation"
                            val index = conversations.indexOfFirst { it.id == id }
                            val item = Conversation(id, title, messages.toList())
                            if (index >= 0) conversations[index] = item else conversations.add(0, item)
                        }
                        activeConversationId = java.util.UUID.randomUUID().toString()
                        chatViewModel.clearMessages()
                        showSettings = false
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = showSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        showSettings = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                Text(
                    "Recent chats",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                if (conversations.isEmpty()) {
                    Text(
                        "Your conversations will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(conversations, key = { it.id }) { conversation ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NavigationDrawerItem(
                                    label = {
                                        Text(conversation.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    },
                                    selected = activeConversationId == conversation.id,
                                    icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = null) },
                                    onClick = {
                                        activeConversationId = conversation.id
                                        chatViewModel.activateConversation(conversation.id, conversation.messages)
                                        showSettings = false
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp, end = 2.dp)
                                )
                                IconButton(onClick = { pendingDelete = conversation }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete ${conversation.title}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(bottom = 12.dp))
                Text(
                    "Offline · on-device Qwen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (showSettings) "Settings" else "PocketQwen") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open sidebar")
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty() && !showSettings) {
                            TextButton(onClick = {
                                val id = activeConversationId ?: java.util.UUID.randomUUID().toString()
                                val title = messages.firstOrNull { it.isUser }?.text?.take(42) ?: "New conversation"
                                val index = conversations.indexOfFirst { it.id == id }
                                val item = Conversation(id = id, title = title, messages = messages.toList())
                                if (index >= 0) conversations[index] = item else conversations.add(0, item)
                                activeConversationId = null
                                chatViewModel.clearMessages()
                            }) { Text("New chat") }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
    }

    pendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("This permanently deletes “${conversation.title}” from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    conversations.removeAll { it.id == conversation.id }
                    if (activeConversationId == conversation.id) {
                        activeConversationId = null
                        chatViewModel.clearMessages()
                    }
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}
