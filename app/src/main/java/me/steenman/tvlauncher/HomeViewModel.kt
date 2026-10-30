package me.steenman.tvlauncher

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.steenman.tvlauncher.data.AppRepository
import me.steenman.tvlauncher.data.FavoritesStore
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.data.shows.ShowConfig
import me.steenman.tvlauncher.data.shows.ShowRepository
import me.steenman.tvlauncher.data.shows.ShowRowConfig
import me.steenman.tvlauncher.data.weather.Forecast
import me.steenman.tvlauncher.data.weather.WeatherRepository
import java.time.Duration
import java.time.Instant

data class HomeUiState(
    /** Favorites as shown: apps that have a show row are left out. */
    val favorites: List<LauncherApp> = emptyList(),
    val showRows: List<ShowRow> = emptyList(),
    val apps: List<LauncherApp> = emptyList(),
    val loaded: Boolean = false,
    /** Every stored favorite, including those hidden because they have a show row. */
    val favoriteKeys: Set<String> = emptySet(),
) {
    fun isFavorite(app: LauncherApp): Boolean = app.key in favoriteKeys
}

/** An installed app with its configured shows. */
data class ShowRow(val app: LauncherApp, val shows: List<ShowTile>)

data class ShowTile(val show: Show, val artwork: ImageBitmap?)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(application)
    private val favoritesStore = FavoritesStore(application)

    private val showRepository = ShowRepository(application)

    private val apps = MutableStateFlow<List<LauncherApp>?>(null)
    private val showConfig = MutableStateFlow<List<ShowRowConfig>>(emptyList())

    /** Loaded artwork by image URL. Only grows when a new image arrives. */
    private val artwork = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    private val artworkLoading = Mutex()

    val uiState: StateFlow<HomeUiState> =
        combine(apps, favoritesStore.favorites, showConfig, artwork, ::homeUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private val showConfigLoad = viewModelScope.launch {
        val config = showRepository.loadConfig()
        showConfig.value = config
        if (config.isNotEmpty()) showRepository.pruneArtwork(config.imageUrls())
        loadArtwork()
    }

    /** This TV's own show list (design.md D1 of remote-show-list), or null to use only the stored one. */
    private val showListUrl: String? = BuildConfig.SHOW_LIST_URL.takeIf { it.isNotBlank() }?.let { base ->
        val name = Settings.Global.getString(application.contentResolver, Settings.Global.DEVICE_NAME)
        ShowConfig.remoteUrl(base, name?.takeIf { it.isNotBlank() } ?: Build.MODEL)
    }
    private var showsFetchedAt: Instant? = null

    /**
     * Keeps the show list at most [SHOWS_REFRESH] old for as long as the caller is active, like
     * [keepWeatherFresh]. A list that can't be fetched or parsed leaves the current one in place.
     */
    suspend fun keepShowsFresh() {
        val url = showListUrl ?: return
        showConfigLoad.join()
        while (true) {
            val due = showsFetchedAt?.plus(SHOWS_REFRESH)
            val wait = due?.let { minOf(Duration.between(Instant.now(), it), SHOWS_REFRESH) }
            if (wait != null && wait > Duration.ZERO) {
                delay(wait.toMillis())
                continue
            }
            val fresh = showRepository.fetchConfig(url)
            if (fresh == null) {
                delay(SHOWS_RETRY.toMillis())
                continue
            }
            showsFetchedAt = Instant.now()
            if (fresh != showConfig.value) {
                showConfig.value = fresh
                showRepository.pruneArtwork(fresh.imageUrls())
                loadArtwork()
            }
        }
    }

    /** Loads artwork that isn't in memory yet: from disk, or downloaded once. Failed images retry on the next call. */
    private fun loadArtwork() {
        viewModelScope.launch {
            if (!artworkLoading.tryLock()) return@launch
            try {
                for (url in showConfig.value.imageUrls() - artwork.value.keys) {
                    showRepository.artwork(url)?.let { image -> artwork.update { it + (url to image) } }
                }
            } finally {
                artworkLoading.unlock()
            }
        }
    }

    private fun List<ShowRowConfig>.imageUrls(): Set<String> =
        flatMap { row -> row.shows.mapNotNull { it.image } }.toSet()

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
        loadArtwork()
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
        val SHOWS_REFRESH: Duration = Duration.ofMinutes(15)
        val SHOWS_RETRY: Duration = Duration.ofMinutes(5)
    }
}

/** Puts installed apps, stored favorites, the show config and loaded artwork together. */
internal fun homeUiState(
    apps: List<LauncherApp>?,
    favoriteKeys: List<String>,
    config: List<ShowRowConfig>,
    artwork: Map<String, ImageBitmap>,
): HomeUiState {
    val installed = apps.orEmpty()
    val byKey = installed.associateBy { it.key }
    val byPackage = installed.associateBy { it.packageName }
    // Rows for apps that aren't installed, or without shows, are hidden.
    val showRows = config.mapNotNull { row ->
        val app = byPackage[row.app] ?: return@mapNotNull null
        ShowRow(app, row.shows.map { ShowTile(it, it.image?.let(artwork::get)) })
            .takeIf { it.shows.isNotEmpty() }
    }
    val showRowPackages = showRows.map { it.app.packageName }.toSet()
    return HomeUiState(
        // Favorites whose app is (temporarily) missing or has a show row are hidden, not forgotten.
        favorites = favoriteKeys.mapNotNull { byKey[it] }.filterNot { it.packageName in showRowPackages },
        showRows = showRows,
        apps = installed,
        loaded = apps != null,
        favoriteKeys = favoriteKeys.toSet(),
    )
}
