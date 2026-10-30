package com.pocketqwen.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun MessageComposer(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    onStop: () -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    val buttonWidth by animateDpAsState(targetValue = if (isGenerating) 62.dp else 48.dp, label = "send-stop-width")
    val cornerRadius by animateDpAsState(targetValue = if (isGenerating) 15.dp else 24.dp, label = "send-stop-corners")
    val canSend = text.isNotBlank()
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom) {
        TextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("Message PocketQwen…") },
            singleLine = false, maxLines = 5, shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier.width(buttonWidth).height(48.dp).clickable(
                enabled = isGenerating || canSend,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isGenerating) onStop() else {
                    val message = text.trim()
                    if (message.isNotEmpty()) { onSend(message); text = "" }
                }
            },
            shape = RoundedCornerShape(cornerRadius),
            color = if (isGenerating) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isGenerating) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isGenerating) "Stop generation" else "Send message"
                )
            }
        }
    }
}
