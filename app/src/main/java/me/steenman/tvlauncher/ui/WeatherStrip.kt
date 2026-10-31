package me.steenman.tvlauncher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import me.steenman.tvlauncher.R
import me.steenman.tvlauncher.data.weather.Condition
import me.steenman.tvlauncher.data.weather.DayForecast
import me.steenman.tvlauncher.data.weather.Forecast
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val WEATHER_STRIP_TAG = "weather"

private const val FOLLOWING_DAYS = 4
private val StaleAfter: Duration = Duration.ofHours(3)

/**
 * Today's weather and the next days, for the header. Read-only: nothing in it is focusable.
 * Shows nothing when there is no forecast, or when a cached one has run out of days.
 */
@Composable
fun Weather(forecast: Forecast?, modifier: Modifier = Modifier) {
    if (forecast == null) return
    // Re-evaluated once a minute, but only recomposes the strip when the value flips.
    val stale by produceState(forecast.isStale(), forecast) {
        // The state outlives a change of forecast, so a fresh forecast must reset it.
        value = forecast.isStale()
        while (!value) {
            delay(60_000)
            value = forecast.isStale()
        }
    }
    WeatherStrip(forecast, today = LocalDate.now(), stale = stale, modifier = modifier)
}

private fun Forecast.isStale() = Duration.between(fetchedAt, Instant.now()) > StaleAfter

@Composable
internal fun WeatherStrip(forecast: Forecast, today: LocalDate, stale: Boolean, modifier: Modifier = Modifier) {
    // A forecast cached days ago still starts at its own first day; skip the days already past.
    val days = forecast.days.filter { !it.date.isBefore(today) }
    val todayForecast = days.firstOrNull()?.takeIf { it.date == today } ?: return

    FittingRow(spacing = 20.dp, modifier = modifier.testTag(WEATHER_STRIP_TAG)) {
        Today(forecast, todayForecast, stale)
        if (days.size > 1) {
            Divider()
            days.drop(1).take(FOLLOWING_DAYS).forEach { Day(it) }
        }
    }
}

/**
 * A row, vertically centred, that leaves out trailing children that don't fit its width, so a
 * crowded header loses the last forecast days instead of overlapping the clock. The first child
 * (today) is always placed; the divider (second child) only when a day follows it.
 */
@Composable
private fun FittingRow(spacing: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val gap = spacing.roundToPx()
        var count = 0
        var width = 0
        for (placeable in placeables) {
            val next = width + (if (count > 0) gap else 0) + placeable.width
            if (count > 0 && next > constraints.maxWidth) break
            width = next
            count++
        }
        if (count == 2) {
            count = 1
            width = placeables[0].width
        }
        val shown = placeables.take(count)
        val height = shown.maxOfOrNull { it.height } ?: 0
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height) {
            var x = 0
            shown.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width + gap
            }
        }
    }
}

@Composable
private fun Today(forecast: Forecast, today: DayForecast, stale: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ConditionIcon(forecast.currentCondition, Modifier.size(44.dp))
        Text(
            text = degrees(forecast.currentTemperature),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
        )
        Column {
            Text(
                text = "↑${degrees(today.max)}  ↓${degrees(today.min)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            today.rainChance?.let {
                Text(
                    text = stringResource(R.string.weather_rain_chance, it),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            if (stale) {
                val context = LocalContext.current
                val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
                val time = forecast.fetchedAt.atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern(pattern))
                Text(
                    text = stringResource(R.string.weather_updated, time),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun Day(day: DayForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
        ConditionIcon(day.condition, Modifier.padding(vertical = 2.dp).size(26.dp))
        Text(
            text = "${degrees(day.max)}/${degrees(day.min)}",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .width(1.dp)
            .height(44.dp)
            .background(TextSecondary.copy(alpha = 0.3f)),
    )
}

@Composable
private fun ConditionIcon(condition: Condition, modifier: Modifier) {
    Image(
        painter = rememberVectorPainter(WeatherIcons.forCondition(condition)),
        contentDescription = stringResource(condition.label),
        modifier = modifier,
    )
}

private fun degrees(value: Int) = "$value°"

private val Condition.label: Int
    get() = when (this) {
        Condition.CLEAR -> R.string.weather_clear
        Condition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
        Condition.CLOUDY -> R.string.weather_cloudy
        Condition.FOG -> R.string.weather_fog
        Condition.DRIZZLE -> R.string.weather_drizzle
        Condition.RAIN -> R.string.weather_rain
        Condition.SNOW -> R.string.weather_snow
        Condition.THUNDERSTORM -> R.string.weather_thunderstorm
    }
