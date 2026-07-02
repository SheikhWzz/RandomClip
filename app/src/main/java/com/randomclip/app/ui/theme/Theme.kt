package com.randomclip.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MochaBase = Color(0xFF1E1E2E)
val MochaText = Color(0xFFCDD6F4)
val MochaAccent = Color(0xFF89B4FA)
val MochaSurface = Color(0xFF313244)
val MochaOverlay = Color(0xCC1E1E2E)

private val DarkColorScheme = darkColorScheme(
    primary = MochaAccent,
    onPrimary = MochaBase,
    secondary = MochaAccent,
    background = MochaBase,
    onBackground = MochaText,
    surface = MochaSurface,
    onSurface = MochaText,
)

@Composable
fun RandomClipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
