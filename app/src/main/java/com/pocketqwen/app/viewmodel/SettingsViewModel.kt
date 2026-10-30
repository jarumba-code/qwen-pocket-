package com.pocketqwen.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.settings.AppSettings
import com.pocketqwen.app.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val aiRepository: AiRepository
) : ViewModel() {
    private val appSettings = AppSettings(context)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()
    private val _modelStatus = MutableStateFlow("Preparing local model…")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()
    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _themeMode.value = appSettings.getThemeModeFlow().first()
            _isFirstLaunch.value = appSettings.isFirstLaunch()
            refreshModelStatus()
        }
    }

    fun refreshModelStatus() {
        if (_isModelLoading.value) return
        viewModelScope.launch {
            if (aiRepository.isModelAvailable()) {
                _modelStatus.value = aiRepository.getModelStatus()
                return@launch
            }
            _isModelLoading.value = true
            _modelStatus.value = "Loading bundled Qwen model on this device…"
            try {
                aiRepository.initializeModel()
                _modelStatus.value = aiRepository.getModelStatus()
            } catch (e: Exception) {
                _modelStatus.value = "Model initialization failed: ${e.message ?: "unknown error"}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            appSettings.setThemeMode(mode)
            _themeMode.value = mode
        }
    }

    fun dismissFirstLaunch() {
        viewModelScope.launch {
            appSettings.setFirstLaunch(false)
            _isFirstLaunch.value = false
        }
    }

    fun getThemeModeName(): String = when (_themeMode.value) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System default"
    }
}
