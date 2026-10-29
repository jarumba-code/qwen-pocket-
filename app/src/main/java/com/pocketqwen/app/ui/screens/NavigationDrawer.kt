package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    onNewChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }

    if (drawerState.isOpen || drawerState.isClosing) {
        ModalDrawer(
            drawerState = drawerState,
            drawerContent = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        // Branding
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = androidx.compose.material3.icons.filled.Chat,
                                contentDescription = "PocketQwen logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "PocketQwen",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))

                        // Actions
                        ListItem(
                            text = { Text(text = "New chat") },
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material3.icons.filled.Add,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onNewChat()
                                isOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                        // History placeholder
                        androidx.compose.material3.Text(
                            text = "Conversation history",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        androidx.compose.material3.Text(
                            text = "No conversations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

                        ListItem(
                            text = { Text(text = "Settings") },
                            leadingIcon = {
                                Icon(
                                    imageVector = androidx.compose.material3.icons.filled.Settings,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onNavigateToSettings()
                                isOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            content = { }
        )
    }
}
