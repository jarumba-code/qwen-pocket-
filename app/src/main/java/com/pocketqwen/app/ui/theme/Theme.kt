package com.pocketqwen.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun accentFor(name: String): Color = when (name) {
    "RED" -> Color(0xFFC44747)
    "BLUE" -> Color(0xFF3267C8)
    "GREEN" -> Color(0xFF34845B)
    "LIGHT_BLUE" -> Color(0xFF39A9D6)
    "DARK_BLUE" -> Color(0xFF243B73)
    "BLACK" -> Color(0xFF333333)
    "PINK" -> Color(0xFFD65B9B)
    "PURPLE" -> Color(0xFF8054B8)
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
    val darkAccent = Color(
        red = (accent.red * 0.72f + 0.28f).coerceAtMost(1f),
        green = (accent.green * 0.72f + 0.28f).coerceAtMost(1f),
        blue = (accent.blue * 0.72f + 0.28f).coerceAtMost(1f)
    )
    val primaryAccent = if (darkTheme) darkAccent else accent
    val colorScheme = base.copy(
        primary = primaryAccent,
        onPrimary = if (darkTheme) Color(0xFF171717) else Color.White,
        primaryContainer = if (darkTheme) accent.copy(alpha = 0.24f) else accent.copy(alpha = 0.16f),
        onPrimaryContainer = if (darkTheme) Color(0xFFF5F1E8) else Color(0xFF28231A),
        secondary = primaryAccent,
        surfaceTint = primaryAccent
    )
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
