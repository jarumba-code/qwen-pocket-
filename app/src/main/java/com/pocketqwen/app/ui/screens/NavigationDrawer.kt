package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DismissValue
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawer
import androidx.compose.material3.ModalDrawerState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Chat
import androidx.compose.material3.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationDrawer(
    onNewChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    IconButton(
        onClick = { drawerState.open() },
        modifier = modifier
    ) {
        Icon(
            imageVector = androidx.compose.material3.icons.filled.Menu,
            contentDescription = "Open navigation drawer"
        )
    }

    ModalDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.width(280.dp).fillMaxHeight()
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
                            imageVector = Chat,
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

                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))
                    )

                    // Actions
                    ListItem(
                        headlineContent = { Text(text = "New chat") },
                        leadingIcon = {
                            Icon(
                                imageVector = Add,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onNewChat()
                            drawerState.close()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    )

                    // History placeholder
                    Text(
                        text = "Conversation history",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
                    )

                    ListItem(
                        headlineContent = { Text(text = "Settings") },
                        leadingIcon = {
                            Icon(
                                imageVector = Settings,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onNavigateToSettings()
                            drawerState.close()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        content = { }
    )
}

private fun rememberDrawerState(initialValue: DrawerValue): ModalDrawerState =
    androidx.compose.material3.rememberDrawerState(initialValue = initialValue)
