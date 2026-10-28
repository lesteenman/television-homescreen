package me.steenman.tvlauncher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import me.steenman.tvlauncher.data.weather.Condition

/**
 * Static vector icons for the weather condition groups, on a 24×24 grid.
 * Each is built once and cached; none is animated or loaded from the network.
 */
object WeatherIcons {

    private val Sun = Color(0xFFFFD166)
    private val Cloud = Color(0xFFE6F1F2)
    private val Mist = Color(0xFFB8C7CC)
    private val Rain = Color(0xFF7FC8F8)
    private val Snow = Color(0xFFFFFFFF)

    private val cache = mutableMapOf<Condition, ImageVector>()

    fun forCondition(condition: Condition): ImageVector = cache.getOrPut(condition) { build(condition) }

    private fun build(condition: Condition): ImageVector =
        ImageVector.Builder(condition.name, 24.dp, 24.dp, 24f, 24f).apply {
            when (condition) {
                Condition.CLEAR -> sun()
                Condition.PARTLY_CLOUDY -> {
                    group(scaleX = 0.7f, scaleY = 0.7f, translationX = -1f, translationY = -1.5f) { sun() }
                    group(scaleX = 0.72f, scaleY = 0.72f, pivotX = 24f, pivotY = 22f) { cloud() }
                }
                Condition.CLOUDY -> cloud()
                Condition.FOG -> {
                    stroke(Mist) { line(3f, 8f, 21f, 8f) }
                    stroke(Mist) { line(5f, 12f, 19f, 12f) }
                    stroke(Mist) { line(3f, 16f, 21f, 16f) }
                }
                Condition.DRIZZLE -> {
                    raisedCloud()
                    listOf(7f, 12f, 17f).forEach { x -> dot(x, 19.5f, 1.1f, Rain) }
                }
                Condition.RAIN -> {
                    raisedCloud()
                    listOf(8f, 12.5f, 17f).forEach { x -> stroke(Rain) { line(x, 17f, x - 1.5f, 22f) } }
                }
                Condition.SNOW -> {
                    raisedCloud()
                    listOf(7f to 18.5f, 12f to 21f, 17f to 18.5f).forEach { (x, y) -> dot(x, y, 1.5f, Snow) }
                }
                Condition.THUNDERSTORM -> {
                    raisedCloud()
                    path(fill = SolidColor(Sun)) {
                        moveTo(13f, 13f); lineTo(9f, 19f); lineTo(12f, 19f)
                        lineTo(10.5f, 24f); lineTo(16f, 17f); lineTo(13f, 17f); close()
                    }
                }
            }
        }.build()

    private fun ImageVector.Builder.sun() {
        dot(12f, 12f, 4.5f, Sun)
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val (dx, dy) = Math.cos(angle).toFloat() to Math.sin(angle).toFloat()
            stroke(Sun) { line(12f + 7f * dx, 12f + 7f * dy, 12f + 9.5f * dx, 12f + 9.5f * dy) }
        }
    }

    /** The Material "cloud" shape, spanning x 0–24 and y 4–20. */
    private fun ImageVector.Builder.cloud() {
        path(fill = SolidColor(Cloud)) {
            moveTo(19.35f, 10.04f)
            curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
            curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
            curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
            curveToRelative(0f, 3.31f, 2.69f, 6f, 6f, 6f)
            horizontalLineToRelative(13f)
            curveToRelative(2.76f, 0f, 5f, -2.24f, 5f, -5f)
            curveToRelative(0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
            close()
        }
    }

    /** A smaller cloud in the top part of the icon, leaving room for precipitation below. */
    private fun ImageVector.Builder.raisedCloud() =
        group(scaleX = 0.8f, scaleY = 0.8f, pivotX = 12f, pivotY = 0f, translationY = -1.5f) { cloud() }

    private fun ImageVector.Builder.dot(x: Float, y: Float, r: Float, color: Color) {
        path(fill = SolidColor(color)) {
            moveTo(x - r, y)
            arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * r, dy1 = 0f)
            arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * r, dy1 = 0f)
            close()
        }
    }

    private fun ImageVector.Builder.stroke(color: Color, block: PathBuilder.() -> Unit) =
        path(stroke = SolidColor(color), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, pathBuilder = block)

    private fun PathBuilder.line(x1: Float, y1: Float, x2: Float, y2: Float) {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
}
