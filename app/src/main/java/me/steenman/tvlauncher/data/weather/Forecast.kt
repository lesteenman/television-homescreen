package me.steenman.tvlauncher.data.weather

import java.time.Instant
import java.time.LocalDate

/** A forecast as shown on the home screen. Temperatures are whole degrees Celsius. */
data class Forecast(
    val fetchedAt: Instant,
    val currentTemperature: Int,
    val currentCondition: Condition,
    /** Today first (in the location's time zone), then the following days. */
    val days: List<DayForecast>,
)

data class DayForecast(
    val date: LocalDate,
    val condition: Condition,
    val max: Int,
    val min: Int,
    /** Highest chance of precipitation that day, in percent; null when the model has none. */
    val rainChance: Int?,
)

/** WMO weather codes grouped into the conditions we have icons for. */
enum class Condition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM;

    companion object {
        fun fromWmo(code: Int): Condition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67, 80, 81, 82 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}
