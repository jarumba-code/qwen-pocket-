package com.pocketqwen.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (isUser) {
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedShape(12.dp),
                    modifier = Modifier.padding(
                        start = 8.dp,
                        end = 0.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = androidx.compose.foundation.layout.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.align(androidx.compose.ui.Alignment.Start)
            ) {
                Surface(
                    color = bubbleColor,
                    shape = RoundedShape(12.dp),
                    modifier = Modifier.padding(
                        start = 0.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text(
                        text = message.text,
                        color = textColor,
                        modifier = androidx.compose.foundation.layout.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
