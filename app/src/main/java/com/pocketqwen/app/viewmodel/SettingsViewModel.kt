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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val appSettings = AppSettings(context)

    private val _themeMode = MutableStateFlow<ThemeMode>(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow<Boolean>(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _modelStatus = MutableStateFlow<String?>(null)
    val modelStatus: StateFlow<String?> = _modelStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _themeMode.value = appSettings.getThemeModeFlow().first()
            _isFirstLaunch.value = appSettings.isFirstLaunch()
            _modelStatus.value = aiRepository.getModelStatus()
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
