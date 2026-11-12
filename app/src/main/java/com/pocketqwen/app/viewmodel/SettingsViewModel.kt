package com.pocketqwen.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketqwen.app.repository.AiRepository
import com.pocketqwen.app.settings.AppSettings
import com.pocketqwen.app.settings.PerformanceMode
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
    private val _performanceMode = MutableStateFlow(PerformanceMode.PERFORMANCE)
    val performanceMode: StateFlow<PerformanceMode> = _performanceMode.asStateFlow()
    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()
    private val _accentColor = MutableStateFlow("GOLDEN")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()
    private val _contextWindow = MutableStateFlow(aiRepository.contextWindowTokens)
    val contextWindow: StateFlow<Int> = _contextWindow.asStateFlow()
    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()
    private val _modelStatus = MutableStateFlow("Preparing local model…")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()
    private val _isModelLoading = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded: StateFlow<Boolean> = _isModelLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            _themeMode.value = appSettings.getThemeModeFlow().first()
            _performanceMode.value = appSettings.getPerformanceModeFlow().first()
            _isFirstLaunch.value = appSettings.isFirstLaunch()
            _userName.value = appSettings.getUserNameFlow().first()
            _accentColor.value = appSettings.getAccentColorFlow().first()
            _contextWindow.value = aiRepository.contextWindowTokens
            aiRepository.configurePerformanceMode(_performanceMode.value)
            refreshModelStatus()
        }
    }

    fun refreshModelStatus() {
        if (_isModelLoading.value) return
        viewModelScope.launch {
            if (aiRepository.isModelAvailable()) {
                _isModelLoaded.value = true
                _modelStatus.value = aiRepository.getModelStatus()
                return@launch
            }
            _isModelLoading.value = true
            _isModelLoaded.value = false
            _modelStatus.value = "Loading bundled Qwen model on this device…"
            try {
                aiRepository.initializeModel()
                _isModelLoaded.value = aiRepository.isModelAvailable()
                _contextWindow.value = aiRepository.contextWindowTokens
                _modelStatus.value = aiRepository.getModelStatus()
            } catch (e: Exception) {
                _isModelLoaded.value = false
                _modelStatus.value = "Model initialization failed: ${e.message ?: "unknown error"}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        if (_isModelLoading.value || _performanceMode.value == mode) return
        viewModelScope.launch {
            val wasLoaded = aiRepository.isModelAvailable()
            _isModelLoading.value = true
            _modelStatus.value = if (wasLoaded) "Applying ${mode.name.lowercase().replaceFirstChar { it.uppercase() }} mode…" else "Saving performance mode…"
            try {
                appSettings.setPerformanceMode(mode)
                _performanceMode.value = mode
                aiRepository.configurePerformanceMode(mode)
                _isModelLoaded.value = aiRepository.isModelAvailable()
                _modelStatus.value = aiRepository.getModelStatus()
            } catch (e: Exception) {
                _modelStatus.value = "Could not apply performance mode: ${e.message ?: "unknown error"}"
                _isModelLoaded.value = aiRepository.isModelAvailable()
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun ejectModel() {
        if (_isModelLoading.value || !_isModelLoaded.value) return
        viewModelScope.launch {
            _isModelLoading.value = true
            _modelStatus.value = "Ejecting local model and releasing memory…"
            try {
                aiRepository.releaseModel()
                _isModelLoaded.value = false
                _modelStatus.value = aiRepository.getModelStatus()
            } catch (e: Exception) {
                _modelStatus.value = "Could not eject model: ${e.message ?: "unknown error"}"
                _isModelLoaded.value = aiRepository.isModelAvailable()
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

    fun setUserName(name: String) {
        viewModelScope.launch {
            appSettings.setUserName(name)
            _userName.value = name.trim().take(40)
        }
    }

    fun setAccentColor(color: String) {
        if (color !in AppSettings.ACCENT_COLORS) return
        viewModelScope.launch { appSettings.setAccentColor(color); _accentColor.value = color }
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
