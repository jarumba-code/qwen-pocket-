package com.pocketqwen.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketqwen.app.viewmodel.SettingsViewModel
import com.pocketqwen.app.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    studyViewModel: StudyViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    val isFirstLaunch by settingsViewModel.isFirstLaunch.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val modelStatus by settingsViewModel.modelStatus.collectAsState()
    val isModelLoading by settingsViewModel.isModelLoading.collectAsState()

    if (isFirstLaunch) {
        WelcomeScreen(
            onContinue = { enteredName ->
                settingsViewModel.setUserName(enteredName)
                settingsViewModel.dismissFirstLaunch()
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showSettings) "Settings" else "Ekip Study Partner",
                        style = if (showSettings) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                fontSize = 20.sp
                            )
                        },
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to study partner")
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(
                            onClick = {
                                studyViewModel.stopGeneration()
                                showSettings = true
                            }
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSettings) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { showSettings = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StudyScreen(
                    viewModel = studyViewModel,
                    modelStatus = modelStatus,
                    isModelLoading = isModelLoading,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
