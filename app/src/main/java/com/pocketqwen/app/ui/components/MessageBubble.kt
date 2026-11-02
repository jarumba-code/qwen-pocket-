package com.pocketqwen.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage
import com.pocketqwen.app.model.MessageStatus

private data class MessagePart(val text: String, val code: Boolean, val language: String = "")

/** Splits Markdown fenced code from prose so code can be copied independently. */
private fun splitMessage(text: String): List<MessagePart> {
    val result = mutableListOf<MessagePart>()
    val fence = Regex("```([\\w+#.-]*)[ \\t]*\\n([\\s\\S]*?)(?:```|$)")
    var cursor = 0
    fence.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            val prose = text.substring(cursor, match.range.first).trim()
            if (prose.isNotEmpty()) result += MessagePart(prose, false)
        }
        result += MessagePart(match.groupValues[2].trimEnd(), true, match.groupValues[1])
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        val prose = text.substring(cursor).trim()
        if (prose.isNotEmpty()) result += MessagePart(prose, false)
    }
    if (result.isEmpty() && text.isNotEmpty()) result += MessagePart(text, false)
    return result
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.isUser
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomEnd = if (isUser) 5.dp else 18.dp,
                bottomStart = if (isUser) 18.dp else 5.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (isUser) {
                    Text(message.text, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    splitMessage(message.text).forEach { part ->
                        if (part.code) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = part.language.ifBlank { "CODE" }.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = { copyText(context, "Code", part.text) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.ContentCopy,
                                                contentDescription = "Copy code only",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = part.text,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = part.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            )
                        }
                    }
                    if (message.text.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(
                                onClick = { copyText(context, "PocketQwen response", message.text) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy full response",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
                if (message.status == MessageStatus.SENDING) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}
