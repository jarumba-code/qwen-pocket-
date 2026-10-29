package com.pocketqwen.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.model.ChatMessage

@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) {
        Alignment.End
    } else {
        Alignment.Start
    }

    val contentModifier = Modifier
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .widthIn(max = 280.dp)
        .clipToBounds()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = contentModifier,
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedShape(12.dp),
                modifier = Modifier.padding(
                    start = if (isUser) 8.dp else 0.dp,
                    end = if (isUser) 0.dp else 8.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth()
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clipToBounds()
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedShape(10.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (message.status == ChatMessage.MessageStatus.SENDING) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
