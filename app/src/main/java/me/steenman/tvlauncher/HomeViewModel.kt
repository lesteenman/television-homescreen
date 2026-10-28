package me.steenman.tvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.steenman.tvlauncher.data.AppRepository
import me.steenman.tvlauncher.data.FavoritesStore
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.weather.Forecast
import me.steenman.tvlauncher.data.weather.WeatherRepository
import java.time.Duration
import java.time.Instant

data class HomeUiState(
    val favorites: List<LauncherApp> = emptyList(),
    val apps: List<LauncherApp> = emptyList(),
    val loaded: Boolean = false,
) {
    fun isFavorite(app: LauncherApp): Boolean = favorites.any { it.key == app.key }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

    private val apps = MutableStateFlow<List<LauncherApp>?>(null)

    val uiState: StateFlow<HomeUiState> =
        combine(apps, favoritesStore.favorites) { apps, favoriteKeys ->
            val installed = apps.orEmpty()
            val byKey = installed.associateBy { it.key }
            HomeUiState(
                // Favorites whose app is (temporarily) missing are hidden, not forgotten.
                favorites = favoriteKeys.mapNotNull { byKey[it] },
                apps = installed,
                loaded = apps != null,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private val weatherRepository =
        WeatherRepository(application, BuildConfig.WEATHER_LATITUDE, BuildConfig.WEATHER_LONGITUDE)

    private val _weather = MutableStateFlow<Forecast?>(null)
    val weather: StateFlow<Forecast?> = _weather.asStateFlow()

    private val weatherCacheLoad = viewModelScope.launch { _weather.value = weatherRepository.cached() }

    /**
     * Keeps the forecast at most [WEATHER_REFRESH] old for as long as the caller is active
     * (the activity runs it while resumed). Returning home soon after a fetch makes no request.
     */
    suspend fun keepWeatherFresh() {
        if (!weatherRepository.isConfigured) return
        weatherCacheLoad.join()
        while (true) {
            val due = _weather.value?.fetchedAt?.plus(WEATHER_REFRESH)
            // Capped so a clock that jumped backwards can't postpone the next fetch indefinitely.
            val wait = due?.let { minOf(Duration.between(Instant.now(), it), WEATHER_REFRESH) }
            if (wait != null && wait > Duration.ZERO) {
                delay(wait.toMillis())
                continue
            }
            val fresh = weatherRepository.fetch()
            if (fresh != null) _weather.value = fresh else delay(WEATHER_RETRY.toMillis())
        }
    }

    fun refresh() {
        viewModelScope.launch { apps.value = repository.loadApps() }
    }

    fun addFavorite(app: LauncherApp) = viewModelScope.launch { favoritesStore.add(app.key) }

    fun removeFavorite(app: LauncherApp) = viewModelScope.launch { favoritesStore.remove(app.key) }

    fun moveFavorite(app: LauncherApp, offset: Int) =
        viewModelScope.launch { favoritesStore.move(app.key, offset) }

    fun onPackageChanged(packageName: String) {
        repository.invalidate(packageName)
        refresh()
    }

    fun onPackageFullyRemoved(packageName: String) {
        viewModelScope.launch { favoritesStore.removePackage(packageName) }
        onPackageChanged(packageName)
    }

    private companion object {
        val WEATHER_REFRESH: Duration = Duration.ofMinutes(30)
        val WEATHER_RETRY: Duration = Duration.ofMinutes(5)
    }
}
