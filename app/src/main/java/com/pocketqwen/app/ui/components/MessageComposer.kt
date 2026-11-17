package com.pocketqwen.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    onStop: () -> Unit = {},
    initialText: String = ""
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    val buttonWidth by animateDpAsState(if (isGenerating) 62.dp else 48.dp, label = "send-stop-width")
    val radius by animateDpAsState(if (isGenerating) 15.dp else 24.dp, label = "send-stop-radius")
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Message EKIP…") },
            singleLine = false,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier.width(buttonWidth).height(48.dp).clickable(
                enabled = isGenerating || text.isNotBlank(),
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isGenerating) onStop()
                else text.trim().takeIf { it.isNotEmpty() }?.let { onSend(it); text = "" }
            },
            shape = RoundedCornerShape(radius),
            color = if (isGenerating) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isGenerating) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isGenerating) "Stop generation" else "Send message"
                )
            }
        }
    }
}
