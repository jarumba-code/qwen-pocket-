package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.settings.PerformanceMode
import com.pocketqwen.app.settings.ThemeMode
import com.pocketqwen.app.viewmodel.SettingsViewModel

private data class AccentOption(val id: String, val label: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("RED", "Red", Color(0xFFC44747)),
    AccentOption("BLUE", "Blue", Color(0xFF3267C8)),
    AccentOption("GREEN", "Green", Color(0xFF34845B)),
    AccentOption("LIGHT_BLUE", "Light blue", Color(0xFF39A9D6)),
    AccentOption("DARK_BLUE", "Dark blue", Color(0xFF243B73)),
    AccentOption("BLACK", "Black", Color(0xFF333333)),
    AccentOption("GOLDEN", "Champagne gold", Color(0xFFC5A04A)),
    AccentOption("PINK", "Pink", Color(0xFFD65B9B)),
    AccentOption("PURPLE", "Purple", Color(0xFF8054B8))
)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val performanceMode by viewModel.performanceMode.collectAsState()
    val contextWindow by viewModel.contextWindow.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val isModelLoading by viewModel.isModelLoading.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    var nameDraft by remember(userName) { mutableStateOf(userName) }
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Personalize PocketQwen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
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

        item { Text("Brand accent", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Champagne gold is the PocketQwen brand accent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                "Balanced uses about half of the available CPU threads; Performance uses all available threads. Context size is configured separately and can increase memory use.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Text("Context window", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${contextWindow.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")} tokens selected automatically",
                        style = MaterialTheme.typography.titleMedium)
                    Text(
                        "PocketQwen detects physical and available RAM. 2–3 GB devices use 2,048–4,096 tokens; 4 GB-class devices and above can use up to 8,192 tokens. The app may reduce this target when memory is tight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                        "PocketQwen runs the bundled Qwen model on this device. Ejecting releases model and inference memory; the bundled model file remains available to load again. Changing the context window reloads the model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        item {
            Text("Legal", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth(), onClick = {
                runCatching { uriHandler.openUri("https://qwen-pocket.vercel.app/privacy.html") }
            }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Privacy Policy", style = MaterialTheme.typography.titleMedium)
                    Text("Read how local conversations and optional online features handle data on our website.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { runCatching { uriHandler.openUri("https://qwen-pocket.vercel.app/privacy.html") } }) {
                        Text("Open Privacy Policy")
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth(), onClick = {
                runCatching { uriHandler.openUri("https://qwen-pocket.vercel.app/terms.html") }
            }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("User Agreement", style = MaterialTheme.typography.titleMedium)
                    Text("Review the terms governing use of PocketQwen.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { runCatching { uriHandler.openUri("https://qwen-pocket.vercel.app/terms.html") } }) {
                        Text("Open User Agreement")
                    }
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

}
