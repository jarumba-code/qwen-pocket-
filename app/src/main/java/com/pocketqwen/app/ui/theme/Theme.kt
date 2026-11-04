package com.pocketqwen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun accentFor(name: String): Color = when (name) {
    "RED" -> Color(0xFFD32F2F)
    "BLUE" -> Color(0xFF1677FF)
    "GREEN" -> Color(0xFF16834A)
    "LIGHT_BLUE" -> Color(0xFF039BE5)
    "DARK_BLUE" -> Color(0xFF173B75)
    "BLACK" -> Color(0xFF252525)
    "GOLDEN" -> Color(0xFFB8860B)
    "PINK" -> Color(0xFFD81B60)
    "PURPLE" -> Color(0xFF7E57C2)
    else -> Color(0xFF1677FF)
}

@Composable
fun PocketQwenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "BLUE",
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) PocketQwenDarkColorScheme else PocketQwenLightColorScheme
    val accent = accentFor(accentColor)
    val colorScheme = base.copy(
        primary = if (darkTheme) accent.copy(alpha = 0.92f) else accent,
        onPrimary = Color.White,
        primaryContainer = if (darkTheme) accent.copy(alpha = 0.28f) else accent.copy(alpha = 0.14f),
        onPrimaryContainer = if (darkTheme) Color(0xFFF5F5F5) else Color(0xFF161616),
        secondary = accent,
        tertiary = accent,
        surfaceTint = accent
    )
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
