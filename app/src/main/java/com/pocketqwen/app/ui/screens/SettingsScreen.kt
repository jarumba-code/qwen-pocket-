package com.pocketqwen.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Appearance", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 4.dp))
        }
        items(3) { index ->
            val mode = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)[index]
            Card(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.setThemeMode(mode) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Follow your phone's theme"
                                ThemeMode.LIGHT -> "Use a light appearance"
                                ThemeMode.DARK -> "Use a dark appearance"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                }
            }
        }
        item {
            Text("Local model", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Model status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        modelStatus ?: "Checking model status…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                    Text(
                        "A file picker alone does not mean a model is loaded. GGUF loading will be added after native inference is verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        item {
            Text("AI inference", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Preview mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Replies are simulated for interface testing. No prompt is sent to a cloud AI service, and Qwen is not running yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
        item {
            Text("About PocketQwen", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Text("Version 1.0.0 · Private, on-device AI project",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}