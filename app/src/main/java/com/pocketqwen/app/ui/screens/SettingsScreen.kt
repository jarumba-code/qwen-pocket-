package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.settings.ThemeMode
import com.pocketqwen.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val modelStatus by viewModel.modelStatus.collectAsState()
    val isModelLoading by viewModel.isModelLoading.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Appearance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp)) }
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
        item {
            Text("Local model", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qwen2.5-0.5B-Instruct · Q4_K_M", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (isModelLoading) CircularProgressIndicator()
                    }
                    Text(
                        modelStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (modelStatus.contains("ready", ignoreCase = true)) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(onClick = viewModel::refreshModelStatus, enabled = !isModelLoading, modifier = Modifier.padding(top = 10.dp)) {
                        Text(if (isModelLoading) "Loading model…" else "Load / check model")
                    }
                    Text(
                        "The model runs locally. First load copies the bundled file into private app storage and can take time; later launches reuse that copy. Inference does not require network access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        item {
            Text("About PocketQwen", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Text("Version 1.0.0 · Private, on-device AI", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
