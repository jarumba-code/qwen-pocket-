package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.settings.PerformanceMode
import com.pocketqwen.app.settings.ThemeMode
import com.pocketqwen.app.viewmodel.SettingsViewModel

private data class AccentOption(val id: String, val label: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("BLUE", "Blue", Color(0xFF1677FF))
)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val performanceMode by viewModel.performanceMode.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val isModelLoading by viewModel.isModelLoading.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    var nameDraft by remember(userName) { mutableStateOf(userName) }
    var showPrivacy by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Personalize PocketQwen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(40) },
                        label = { Text("Your name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.setUserName(nameDraft.trim()) },
                        enabled = nameDraft.trim() != userName,
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Save name") }
                    Text(
                        "Your name is stored locally in app settings and can be left blank.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Text("Accent color", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a color for buttons and highlights. Your light or dark background stays unchanged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    accentOptions.chunked(3).forEach { rowOptions ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowOptions.forEach { option ->
                                val selected = accentColor == option.id
                                Card(
                                    onClick = { viewModel.setAccentColor(option.id) },
                                    modifier = Modifier.weight(1f).border(
                                        width = if (selected) 2.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Spacer(
                                            modifier = Modifier.size(24.dp).background(option.color, CircleShape)
                                        )
                                        Text(option.label, style = MaterialTheme.typography.labelMedium)
                                        if (selected) Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            repeat(3 - rowOptions.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        item { Text("Appearance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
        items(3) { index ->
            val mode = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)[index]
            Card(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.setThemeMode(mode) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(when (mode) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }, style = MaterialTheme.typography.titleMedium)
                        Text(when (mode) {
                            ThemeMode.SYSTEM -> "Follow your phone's theme"
                            ThemeMode.LIGHT -> "Use a light appearance"
                            ThemeMode.DARK -> "Use a dark appearance"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                }
            }
        }

        item { Text("Performance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
        items(2) { index ->
            val mode = listOf(PerformanceMode.BALANCED, PerformanceMode.PERFORMANCE)[index]
            Card(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.setPerformanceMode(mode) }, enabled = !isModelLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (mode == PerformanceMode.PERFORMANCE) "Performance" else "Balanced", style = MaterialTheme.typography.titleMedium)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    RadioButton(selected = performanceMode == mode, onClick = { viewModel.setPerformanceMode(mode) }, enabled = !isModelLoading)
                }
            }
        }
        item {
            Text(
                "Both modes keep the context window at 2,048 tokens. Performance uses all available CPU threads; Balanced uses about half. Changing modes applies the selected CPU thread count.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Text("Local model", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qwen2.5-0.5B-Instruct · Q4_K_M", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (isModelLoading) CircularProgressIndicator()
                    }
                    Text(modelStatus, style = MaterialTheme.typography.bodyMedium, color = if (isModelLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::refreshModelStatus, enabled = !isModelLoading && !isModelLoaded, modifier = Modifier.weight(1f)) {
                            Text(if (isModelLoading) "Loading…" else "Load model")
                        }
                        OutlinedButton(onClick = viewModel::ejectModel, enabled = isModelLoaded && !isModelLoading, modifier = Modifier.weight(1f)) {
                            Text("Eject model")
                        }
                    }
                    Text(
                        "PocketQwen runs the bundled Qwen model on this device. Ejecting releases the model and inference context from memory; the bundled model file remains available to load again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            Text("Privacy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth(), onClick = { showPrivacy = true }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("How PocketQwen handles your data", style = MaterialTheme.typography.titleMedium)
                    Text("Local chats are processed on this device. Optional online features, when enabled, may send the specific query or report details to their service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showPrivacy = true }) { Text("Read privacy information") }
                }
            }
        }

        item {
            Text("About PocketQwen", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PocketQwen · Version 1.1.0", style = MaterialTheme.typography.titleMedium)
                    Text("Built by JIMM.tech in Nigeria.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "PocketQwen is an AI project focused on privacy, data sovereignty, and making local AI more accessible on everyday devices. Founded by Collins Asanya, a Nigerian college student, the project explores how AI can run directly on users' devices, reducing dependence on cloud inference where supported.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "PocketQwen uses the Qwen2.5-0.5B-Instruct model. Model output can be inaccurate, and capabilities depend on the model and device. The local chat model does not require cloud inference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("PocketQwen privacy") },
            text = { Text("Offline chat prompts and responses are processed by the bundled model on your device. Conversation history and settings are stored in app-private storage. Web Search, if enabled, sends your search query to the configured search service. Submitting a report sends only the information shown in the report form. Do not submit passwords, private keys, or sensitive account information. Model answers may be inaccurate.") },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Close") } }
        )
    }

}
