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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pocketqwen.app.settings.PerformanceMode
import com.pocketqwen.app.settings.ThemeMode
import com.pocketqwen.app.viewmodel.SettingsViewModel

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
            Text("Performance", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
        items(2) { index ->
            val mode = listOf(PerformanceMode.BALANCED, PerformanceMode.PERFORMANCE)[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.setPerformanceMode(mode) },
                enabled = !isModelLoading
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (mode == PerformanceMode.PERFORMANCE) "Performance" else "Balanced",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    RadioButton(
                        selected = performanceMode == mode,
                        onClick = { viewModel.setPerformanceMode(mode) },
                        enabled = !isModelLoading
                    )
                }
            }
        }
        item {
            Text(
                "Both modes keep the context window at 1034 tokens. Changing modes reloads the model to apply the new CPU thread count.",
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
                        Text("Gemma 3 1B · Q4_K_M", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (isModelLoading) CircularProgressIndicator()
                    }
                    Text(
                        modelStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isModelLoaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::refreshModelStatus,
                            enabled = !isModelLoading && !isModelLoaded,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isModelLoading) "Loading…" else "Load model")
                        }
                        OutlinedButton(
                            onClick = viewModel::ejectModel,
                            enabled = isModelLoaded && !isModelLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Eject model")
                        }
                    }
                    Text(
                        "PocketQwen loads the model automatically on startup. Ejecting releases the model and its inference context from memory; the bundled model file remains on this device and can be loaded again here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        item {
            Text("About PocketQwen", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Text("Gemma Model Experiment 1 · Experimental build", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Text("Privacy & data", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Local-first processing", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Prompts and responses are processed by the bundled model on this device. This build does not enable web search, advertising, analytics, or account sign-in. If online features are added in a future build, they must be clearly identified because queries may leave the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text("Manage your data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
                    Text(
                        "Open Recent chats in the sidebar to delete an individual conversation, or use Delete all chats there to clear local conversation history. Uninstalling the app removes its private app data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        "No subscriptions or paid features are included in this experiment, so no refund or unsubscribe flow is needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
