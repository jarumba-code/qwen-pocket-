package com.pocketqwen.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.ui.components.MessageBubble
import com.pocketqwen.app.ui.components.MessageComposer
import com.pocketqwen.app.viewmodel.ChatViewModel

@Composable
fun ThinkingDots() {
    val transition = rememberInfiniteTransition(label = "qwen-thinking")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "qwen-logo-pulse"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Image(
            painter = painterResource(id = com.pocketqwen.app.R.drawable.ic_qwen_logo),
            contentDescription = "PocketQwen is thinking",
            modifier = Modifier.size(28.dp).alpha(alpha)
        )
        Text(
            "PocketQwen is thinking…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreamingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Image(
            painter = painterResource(id = com.pocketqwen.app.R.drawable.ic_qwen_logo),
            contentDescription = "PocketQwen is writing",
            modifier = Modifier.size(28.dp)
        )
        CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
        Text(
            "PocketQwen is writing…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActiveChatScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val thinking by viewModel.thinking.collectAsState()
    val error by viewModel.error.collectAsState()
    val listState = rememberLazyListState()
    // Approximate only: the exact llama.cpp tokenizer count (including prompt/template
    // overhead) is not exposed to the UI yet. Keep the label explicit to avoid false precision.
    val estimatedTokens = remember(messages) {
        (messages.sumOf { it.text.length } + 3) / 4
    }.coerceAtMost(1024)
    val remainingTokens = (1024 - estimatedTokens).coerceAtLeast(0)
    val remainingFraction = remainingTokens / 1024f
    LaunchedEffect(messages.size, messages.lastOrNull()?.text, isLoading, thinking) {
        val count = messages.size + if (thinking || (isLoading && !thinking)) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(messages, key = { it.id }) { MessageBubble(message = it) }
            if (thinking) item { ThinkingDots() }
            else if (isLoading) item { StreamingIndicator() }
            if (error != null) item {
                Text("Error: " + error, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                progress = { remainingFraction },
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$remainingTokens / 1,024 context tokens left",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Approximate estimate · context resets in a new chat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text("Private · local model · no cloud inference", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 2.dp))
        MessageComposer(onSend = viewModel::sendMessage, isGenerating = isLoading, onStop = viewModel::stopGeneration)
    }
}
