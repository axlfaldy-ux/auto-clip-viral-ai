package com.autoclip.viralai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClipColors = darkColorScheme(
    primary = Color(0xFFB8F35A), onPrimary = Color(0xFF17220A),
    secondary = Color(0xFF86D9CC), background = Color(0xFF0B0D0C),
    surface = Color(0xFF151917), surfaceVariant = Color(0xFF202622),
    onBackground = Color(0xFFF2F5EF), onSurface = Color(0xFFF2F5EF),
    onSurfaceVariant = Color(0xFFB4BDB5), outline = Color(0xFF3B443E),
)

@Composable
fun AutoClipTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClipColors, content = content)
}
