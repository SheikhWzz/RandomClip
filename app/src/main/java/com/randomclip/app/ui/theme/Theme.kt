package com.randomclip.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BackgroundColor = Color(0xFF0D0D0D)
val SurfaceColor = Color(0xFF1A1A1A)
val AccentColor = Color(0xFFFF9500)
val AccentPressedColor = Color(0xFFE67E00)
val TextPrimaryColor = Color(0xFFF5F5F5)
val TextSecondaryColor = Color(0xFF8A8A8A)
val DividerColor = Color(0xFF2A2A2A)
val ErrorColor = Color(0xFFFF3B30)
val OverlayColor = Color(0xCC0D0D0D)

private val DarkColorScheme = darkColorScheme(
    primary = AccentColor,
    onPrimary = BackgroundColor,
    secondary = AccentColor,
    background = BackgroundColor,
    onBackground = TextPrimaryColor,
    surface = SurfaceColor,
    onSurface = TextPrimaryColor,
    outline = DividerColor,
    error = ErrorColor,
)

@Composable
fun RandomClipTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
