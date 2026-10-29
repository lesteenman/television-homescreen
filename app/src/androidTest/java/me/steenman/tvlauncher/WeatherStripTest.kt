package me.steenman.tvlauncher

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.steenman.tvlauncher.data.weather.Condition
import me.steenman.tvlauncher.data.weather.DayForecast
import me.steenman.tvlauncher.data.weather.Forecast
import me.steenman.tvlauncher.ui.LauncherTheme
import me.steenman.tvlauncher.ui.WEATHER_STRIP_TAG
import me.steenman.tvlauncher.ui.Weather
import me.steenman.tvlauncher.ui.WeatherStrip
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WeatherStripTest {

    @get:Rule
    val compose = createComposeRule()

    // A Wednesday, so the following days are Thu–Sun and Mon is the spare sixth day.
    private val today = LocalDate.of(2026, 10, 28)

    private val forecast = Forecast(
        fetchedAt = Instant.parse("2026-10-28T09:00:00Z"),
        currentTemperature = 12,
        currentCondition = Condition.RAIN,
        days = listOf(
            DayForecast(today, Condition.RAIN, max = 14, min = 6, rainChance = 80),
            DayForecast(today.plusDays(1), Condition.CLEAR, max = 15, min = 5, rainChance = 0),
            DayForecast(today.plusDays(2), Condition.SNOW, max = 2, min = -3, rainChance = 10),
            DayForecast(today.plusDays(3), Condition.FOG, max = 9, min = 4, rainChance = null),
            DayForecast(today.plusDays(4), Condition.THUNDERSTORM, max = 18, min = 11, rainChance = 90),
            DayForecast(today.plusDays(5), Condition.CLOUDY, max = 11, min = 7, rainChance = 20),
        ),
    )

    private fun day(dayOfWeek: DayOfWeek) = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    @Test
    fun showsTodayAndFourFollowingDays() {
        compose.setContent { LauncherTheme { WeatherStrip(forecast, today, stale = false) } }

        compose.onNodeWithText("12°").assertIsDisplayed()
        compose.onNodeWithText("↑14°  ↓6°").assertIsDisplayed()
        compose.onNodeWithText("80% rain").assertIsDisplayed()
        listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            .forEach { compose.onNodeWithText(day(it)).assertIsDisplayed() }
        compose.onNodeWithText("15°/5°").assertIsDisplayed()
        compose.onNodeWithText("2°/-3°").assertIsDisplayed()
        compose.onNodeWithText(day(DayOfWeek.MONDAY)).assertDoesNotExist()
        compose.onNodeWithContentDescription("Thunderstorm").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Rain").assertCountEquals(1)
        compose.onNodeWithText("Updated", substring = true).assertDoesNotExist()
    }

    @Test
    fun nothingInTheStripIsFocusable() {
        compose.setContent { LauncherTheme { WeatherStrip(forecast, today, stale = true) } }

        val inStrip = hasTestTag(WEATHER_STRIP_TAG) or hasAnyAncestor(hasTestTag(WEATHER_STRIP_TAG))
        compose.onAllNodes(inStrip and SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused))
            .assertCountEquals(0)
    }

    @Test
    fun staleDataShowsWhenItWasFetched() {
        compose.setContent { LauncherTheme { WeatherStrip(forecast, today, stale = true) } }

        compose.onNodeWithText("Updated", substring = true).assertIsDisplayed()
    }

    @Test
    fun skipsDaysThatHavePassed() {
        compose.setContent { LauncherTheme { WeatherStrip(forecast, today.plusDays(1), stale = true) } }

        compose.onNodeWithText("↑15°  ↓5°").assertIsDisplayed()
        compose.onNodeWithText(day(DayOfWeek.MONDAY)).assertIsDisplayed()
    }

    @Test
    fun freshForecastReplacesTheStaleLabel() {
        val old = forecast.copy(fetchedAt = Instant.now().minus(Duration.ofHours(12)), days = forecastFromToday())
        var shown by mutableStateOf(old)
        compose.setContent { LauncherTheme { Weather(shown) } }
        compose.onNodeWithText("Updated", substring = true).assertIsDisplayed()

        shown = old.copy(fetchedAt = Instant.now())
        compose.onNodeWithText("Updated", substring = true).assertDoesNotExist()
    }

    /** [Weather] uses the real date, so give it days starting today. */
    private fun forecastFromToday() =
        forecast.days.mapIndexed { i, day -> day.copy(date = LocalDate.now().plusDays(i.toLong())) }

    @Test
    fun hiddenWithoutData() {
        compose.setContent { LauncherTheme { Weather(forecast = null) } }
        compose.onNodeWithTag(WEATHER_STRIP_TAG).assertDoesNotExist()
    }

    @Test
    fun hiddenWhenTheCachedForecastHasRunOut() {
        compose.setContent { LauncherTheme { WeatherStrip(forecast, today.plusDays(6), stale = true) } }
        compose.onNodeWithTag(WEATHER_STRIP_TAG).assertDoesNotExist()
    }
}
