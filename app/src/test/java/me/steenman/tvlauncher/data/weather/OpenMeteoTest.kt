package me.steenman.tvlauncher.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class OpenMeteoTest {

    private val fetchedAt = Instant.parse("2026-10-28T09:00:00Z")

    // Trimmed real response shape from api.open-meteo.com.
    private val response = """
        {
          "latitude": 52.1, "longitude": 5.18, "timezone": "Europe/Amsterdam",
          "current_units": {"temperature_2m": "°C", "weather_code": "wmo code"},
          "current": {"time": "2026-10-28T10:00", "interval": 900, "temperature_2m": 11.6, "weather_code": 3},
          "daily": {
            "time": ["2026-10-28", "2026-10-29", "2026-10-30"],
            "weather_code": [61, 0, 95],
            "temperature_2m_max": [13.4, 15.5, -0.4],
            "temperature_2m_min": [6.5, 5.9, -3.6],
            "precipitation_probability_max": [80, 0, null]
          }
        }
    """.trimIndent()

    @Test
    fun `parses current conditions and days`() {
        val forecast = OpenMeteo.parse(response, fetchedAt)

        assertEquals(fetchedAt, forecast.fetchedAt)
        assertEquals(12, forecast.currentTemperature)
        assertEquals(Condition.CLOUDY, forecast.currentCondition)
        assertEquals(
            listOf(
                DayForecast(LocalDate.of(2026, 10, 28), Condition.RAIN, max = 13, min = 7, rainChance = 80),
                DayForecast(LocalDate.of(2026, 10, 29), Condition.CLEAR, max = 16, min = 6, rainChance = 0),
                DayForecast(LocalDate.of(2026, 10, 30), Condition.THUNDERSTORM, max = 0, min = -4, rainChance = null),
            ),
            forecast.days,
        )
    }

    @Test
    fun `rain chance is optional`() {
        val withoutRain = response.replace(Regex(""",\s*"precipitation_probability_max": \[[^]]*]"""), "")

        val forecast = OpenMeteo.parse(withoutRain, fetchedAt)

        assertNull(forecast.days.first().rainChance)
    }

    @Test
    fun `rejects incomplete responses`() {
        assertThrows(Exception::class.java) { OpenMeteo.parse("""{"error": true, "reason": "x"}""", fetchedAt) }
        assertThrows(Exception::class.java) {
            OpenMeteo.parse(response.replace("\"temperature_2m\": 11.6", "\"temperature_2m\": null"), fetchedAt)
        }
    }

    @Test
    fun `maps WMO codes to icon groups`() {
        val expected = mapOf(
            0 to Condition.CLEAR,
            1 to Condition.PARTLY_CLOUDY, 2 to Condition.PARTLY_CLOUDY,
            3 to Condition.CLOUDY,
            45 to Condition.FOG, 48 to Condition.FOG,
            51 to Condition.DRIZZLE, 53 to Condition.DRIZZLE, 55 to Condition.DRIZZLE,
            56 to Condition.DRIZZLE, 57 to Condition.DRIZZLE,
            61 to Condition.RAIN, 63 to Condition.RAIN, 65 to Condition.RAIN, 66 to Condition.RAIN,
            67 to Condition.RAIN, 80 to Condition.RAIN, 81 to Condition.RAIN, 82 to Condition.RAIN,
            71 to Condition.SNOW, 73 to Condition.SNOW, 75 to Condition.SNOW, 77 to Condition.SNOW,
            85 to Condition.SNOW, 86 to Condition.SNOW,
            95 to Condition.THUNDERSTORM, 96 to Condition.THUNDERSTORM, 99 to Condition.THUNDERSTORM,
            42 to Condition.CLOUDY,
        )
        expected.forEach { (code, condition) -> assertEquals("code $code", condition, Condition.fromWmo(code)) }
    }

    @Test
    fun `request asks for the fields the parser reads`() {
        val url = OpenMeteo.url(52.1, 5.18)
        listOf(
            "latitude=52.1", "longitude=5.18", "current=temperature_2m,weather_code",
            "daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
            "timezone=auto",
        ).forEach { assert(it in url) { "missing $it in $url" } }
    }
}
