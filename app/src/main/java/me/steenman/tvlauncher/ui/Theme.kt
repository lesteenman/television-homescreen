package me.steenman.tvlauncher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import kotlin.math.roundToInt

val TextPrimary = Color(0xFFE6F1F2)
val TextSecondary = Color(0xB3E6F1F2)
val CardContainer = Color(0x26FFFFFF)

private val GradientColors = listOf(Color(0xFF0B1426), Color(0xFF0F2A3A), Color(0xFF0E3B3F))

/**
 * The static diagonal background gradient, rendered once into a small bitmap.
 * Drawing the gradient shader full-screen every frame cost ~40 ms per frame on the
 * Chromecast HD's GPU; stretching a pre-rendered texture is nearly free.
 */
private val GradientBitmap: ImageBitmap by lazy {
    val width = 480
    val height = 270
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        isDither = true
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            GradientColors.map { it.toArgb() }.toIntArray(),
            null,
            Shader.TileMode.CLAMP,
        )
    }
    Canvas(bitmap).drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    bitmap.asImageBitmap()
}

/** Static background: no images from the network, just the pre-rendered gradient. */
fun Modifier.gradientBackground(): Modifier = drawBehind {
    drawImage(
        image = GradientBitmap,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.Low,
    )
}

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalContentColor provides TextPrimary, content = content)
    }
}
