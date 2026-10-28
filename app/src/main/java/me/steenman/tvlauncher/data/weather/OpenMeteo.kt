package me.steenman.tvlauncher.data.weather

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/** Request and response format of the Open-Meteo forecast API (no API key needed). */
object OpenMeteo {

    // One spare day, so a forecast cached overnight still has 4 days after "today".
    private const val FORECAST_DAYS = 6

    fun url(latitude: Double, longitude: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=$FORECAST_DAYS"

    /** Throws on anything that isn't a complete forecast, so a bad response never replaces the cache. */
    fun parse(body: String, fetchedAt: Instant): Forecast {
        val root = JSONObject(body)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxima = daily.getJSONArray("temperature_2m_max")
        val minima = daily.getJSONArray("temperature_2m_min")
        val rain = daily.optJSONArray("precipitation_probability_max")

        val days = (0 until dates.length()).map { i ->
            DayForecast(
                date = LocalDate.parse(dates.getString(i)),
                condition = Condition.fromWmo(codes.getInt(i)),
                max = maxima.getDouble(i).roundToInt(),
                min = minima.getDouble(i).roundToInt(),
                rainChance = rain?.intOrNull(i),
            )
        }
        require(days.isNotEmpty()) { "Forecast has no days" }

        return Forecast(
            fetchedAt = fetchedAt,
            currentTemperature = current.getDouble("temperature_2m").roundToInt(),
            currentCondition = Condition.fromWmo(current.getInt("weather_code")),
            days = days,
        )
    }

    private fun JSONArray.intOrNull(index: Int): Int? =
        if (index >= length() || isNull(index)) null else getDouble(index).roundToInt()
}
