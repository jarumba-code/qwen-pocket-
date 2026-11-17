package com.pocketqwen.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketqwen.app.viewmodel.StudyMode
import com.pocketqwen.app.viewmodel.StudyUiState
import com.pocketqwen.app.viewmodel.StudyViewModel

@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    modelStatus: String,
    isModelLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val estimatedTokens = (notes.length + 3) / 4
    val inputBudget = (viewModel.contextWindowTokens - 900).coerceAtLeast(0)
    val tooLong = estimatedTokens > inputBudget
    val busy = uiState is StudyUiState.Loading || uiState is StudyUiState.Generating
    val canGenerate = notes.isNotBlank() && !tooLong && !busy

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "WORKS OFFLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                "Make sense of your notes.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-0.35).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Paste what you have from class. Get the main ideas or a simpler explanation, right on your phone.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("YOUR NOTES")
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = viewModel::clearNotes,
                    enabled = notes.isNotEmpty() && !busy
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Clear", modifier = Modifier.padding(start = 5.dp))
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = viewModel::updateNotes,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 184.dp, max = 280.dp),
                placeholder = {
                    Text(
                        "Paste or type your class notes here…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                minLines = 7,
                maxLines = 10,
                shape = RoundedCornerShape(10.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 25.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${notes.length} characters · about ${estimatedTokens} tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tooLong) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = viewModel::loadSampleNotes,
                    enabled = !busy
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(15.dp))
                    Text("Try sample notes", modifier = Modifier.padding(start = 5.dp))
                }
            }

            if (tooLong) {
                Text(
                    "These notes are longer than the current model context allows. Shorten them before generating a response.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            SectionLabel("STUDY TOOLS")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.generate(StudyMode.SUMMARIZE) },
                    enabled = canGenerate,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Summarize", modifier = Modifier.padding(start = 7.dp))
                }
                OutlinedButton(
                    onClick = { viewModel.generate(StudyMode.EXPLAIN) },
                    enabled = canGenerate,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Explain simply", modifier = Modifier.padding(start = 7.dp))
                }
            }
        }

        when (val result = uiState) {
            StudyUiState.Idle -> Unit

            is StudyUiState.Loading -> {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Preparing your ${result.mode.heading.lowercase()}…", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isModelLoading) modelStatus else "The first model start can take a little while on this phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = viewModel::stopGeneration) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop generation")
                        }
                    }
                }
            }

            is StudyUiState.Generating -> {
                ResultCard {
                    ResultHeading(result.mode.heading.uppercase())
                    Text(
                        result.partialText.ifBlank { "Working through your notes…" },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = viewModel::stopGeneration,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Stop", modifier = Modifier.padding(start = 7.dp))
                    }
                }
            }

            is StudyUiState.Done -> {
                ResultCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ResultHeading(result.mode.heading.uppercase(), modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(result.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
                        }
                    }
                    Text(
                        result.text,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = viewModel::clearNotes,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("New notes")
                    }
                }
            }

            is StudyUiState.Error -> {
                ResultCard {
                    ResultHeading("COULDN'T FINISH")
                    Text(
                        result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = viewModel::retry,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Try again")
                        }
                        TextButton(
                            onClick = viewModel::clearNotes,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("New notes")
                        }
                    }
                }
            }
        }

        Text(
            "AI can make mistakes. Check important details against your course material.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ResultHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.1.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ResultCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}
