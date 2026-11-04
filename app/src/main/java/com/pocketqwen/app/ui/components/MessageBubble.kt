package com.pocketqwen.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus

private fun splitCodeBlocks(text: String): List<Pair<Boolean, String>> {
    val parts = mutableListOf<Pair<Boolean, String>>()
    val regex = Regex("""```([^\n]*)\n([\s\S]*?)```""")
    var cursor = 0
    regex.findAll(text).forEach { match ->
        if (match.range.first > cursor) parts += false to text.substring(cursor, match.range.first)
        val language = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        parts += true to (language + "\n" + code)
        cursor = match.range.last + 1
    }
    if (cursor < text.length) parts += false to text.substring(cursor)
    return parts
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onEdit: (String, String) -> Unit = { _, _ -> }
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var editing by remember(message.id) { mutableStateOf(false) }
    var editedText by remember(message.id, message.text) { mutableStateOf(message.text) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                val parts = if (!isUser && message.text.contains("```")) splitCodeBlocks(message.text) else listOf(false to message.text)
                parts.forEach { (isCode, value) ->
                    if (isCode) {
                        val firstNewline = value.indexOf('\n')
                        val language = if (firstNewline >= 0) value.substring(0, firstNewline).ifBlank { "Code" } else "Code"
                        val code = if (firstNewline >= 0) value.substring(firstNewline + 1) else value
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(language, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) { Text("Copy") }
                            }
                            Text(
                                text = code,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 3.dp)
                            )
                        }
                    } else if (value.isNotBlank()) {
                        val linkedText = buildAnnotatedString {
                            val regex = Regex("https://[^\\s)]+")
                            var cursor = 0
                            regex.findAll(value).forEach { match ->
                                append(value.substring(cursor, match.range.first))
                                val url = match.value.trimEnd('.', ',', ';')
                                pushStringAnnotation("URL", url)
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { append(url) }
                                pop()
                                cursor = match.range.last + 1
                            }
                            if (cursor < value.length) append(value.substring(cursor))
                        }
                        if (linkedText.getStringAnnotations("URL", 0, linkedText.length).isEmpty()) {
                            Text(value, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            ClickableText(text = linkedText,
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                onClick = { offset ->
                                    linkedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                        runCatching { uriHandler.openUri(it.item) }
                                    }
                                })
                        }
                    }
                }
                if (!isUser && message.text.isNotBlank() && message.status != MessageStatus.SENDING) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(message.text)) }) { Text("Copy response") }
                }
                if (isUser && message.status != MessageStatus.SENDING) {
                    TextButton(onClick = { editedText = message.text; editing = true }) { Text("Edit") }
                }
                if (message.status == MessageStatus.SENDING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Edit message") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = { onEdit(message.id, editedText); editing = false }, enabled = editedText.isNotBlank()) { Text("Resend") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancel") } }
        )
    }
}
