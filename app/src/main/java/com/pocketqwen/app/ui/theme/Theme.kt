package com.pocketqwen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme as MaterialTheme3
import androidx.compose.runtime.Composable

@Composable
fun PocketQwenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        PocketQwenDarkColorScheme
    } else {
        PocketQwenLightColorScheme
    }

    MaterialTheme3(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
