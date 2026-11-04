package com.pocketqwen.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.pocketQwenDataStore by preferencesDataStore(
    name = "pocketqwen_settings"
)

private object AppSettingsKey {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")
    val USER_NAME = stringPreferencesKey("user_name")
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class PerformanceMode(val nativeValue: Int, val description: String) {
    BALANCED(
        nativeValue = 0,
        description = "Uses about half of the available CPU threads to balance speed, battery use, and responsiveness."
    ),
    PERFORMANCE(
        nativeValue = 1,
        description = "Uses all available CPU threads for faster text generation. May use more battery and produce more heat."
    )
}

class AppSettings(private val context: Context) {
    private val dataStore = context.pocketQwenDataStore

    fun getThemeModeFlow(): Flow<ThemeMode> = dataStore.data.map { preferences ->
        val savedValue = preferences[AppSettingsKey.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(savedValue) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun getPerformanceModeFlow(): Flow<PerformanceMode> = dataStore.data.map { preferences ->
        val savedValue = preferences[AppSettingsKey.PERFORMANCE_MODE] ?: PerformanceMode.BALANCED.name
        runCatching { PerformanceMode.valueOf(savedValue) }.getOrDefault(PerformanceMode.BALANCED)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[AppSettingsKey.THEME_MODE] = mode.name
        }
    }

    suspend fun setPerformanceMode(mode: PerformanceMode) {
        dataStore.edit { preferences ->
            preferences[AppSettingsKey.PERFORMANCE_MODE] = mode.name
        }
    }

    fun getUserNameFlow(): Flow<String> = dataStore.data.map { preferences ->
        preferences[AppSettingsKey.USER_NAME].orEmpty()
    }

    // Older installs may have a saved blue accent; map it to the new gold brand accent.
    fun getAccentColorFlow(): Flow<String> = dataStore.data.map { preferences ->
        when (preferences[AppSettingsKey.ACCENT_COLOR]) {
            "GOLDEN" -> "GOLDEN"
            else -> "GOLDEN"
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[AppSettingsKey.USER_NAME] = name.trim().take(40)
        }
    }

    suspend fun setAccentColor(color: String) {
        if (color != "GOLDEN") return
        dataStore.edit { preferences ->
            preferences[AppSettingsKey.ACCENT_COLOR] = color
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        dataStore.edit { preferences ->
            preferences[AppSettingsKey.IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.first()[AppSettingsKey.IS_FIRST_LAUNCH] ?: true
    }
}
