package com.pocketqwen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun accentFor(name: String): Color = when (name) {
    "GOLDEN" -> Color(0xFFB58A35)
    else -> Color(0xFFB58A35)
}

@Composable
fun PocketQwenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "GOLDEN",
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) PocketQwenDarkColorScheme else PocketQwenLightColorScheme
    val accent = accentFor(accentColor)
    val colorScheme = base.copy(
        primary = if (darkTheme) Color(0xFFE0BC70) else accent,
        onPrimary = if (darkTheme) Color(0xFF382800) else Color.White,
        primaryContainer = if (darkTheme) Color(0xFF594313) else Color(0xFFF1E2BD),
        onPrimaryContainer = if (darkTheme) Color(0xFFF6E5BD) else Color(0xFF352609),
        secondary = if (darkTheme) Color(0xFFD7B66F) else Color(0xFF9A742C),
        tertiary = if (darkTheme) Color(0xFFB9C5D8) else Color(0xFF24334D),
        surfaceTint = accent
    )
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
