package me.steenman.tvlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val TextPrimary = Color(0xFFE6F1F2)
val TextSecondary = Color(0xB3E6F1F2)
val CardContainer = Color(0x26FFFFFF)

/** Static background: no images, nothing from the network. */
val BackgroundGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0B1426), Color(0xFF0F2A3A), Color(0xFF0E3B3F)),
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary, content = content)
    }
}
