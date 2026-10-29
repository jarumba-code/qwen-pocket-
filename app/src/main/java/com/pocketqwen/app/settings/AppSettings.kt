package com.pocketqwen.app.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object AppSettingsKey {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val IS_FIRST_LAUNCH = preferencesKey<Boolean>("is_first_launch")
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class AppSettings(private val context: Context) {
    private val dataStore = context.createDataStore(name = "pocketqwen_settings")

    suspend fun <T> set(key: androidx.datastore.preferences.core.PreferenceKey<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    fun getThemeModeFlow(): Flow<ThemeMode> {
        return dataStore.data.map {
            val value = it[AppSettingsKey.THEME_MODE]
            ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[AppSettingsKey.THEME_MODE] = mode.name }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        dataStore.edit { it[AppSettingsKey.IS_FIRST_LAUNCH] = isFirst }
    }

    suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.first().map { it[AppSettingsKey.IS_FIRST_LAUNCH] ?: true }
    }
}
