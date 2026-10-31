package me.steenman.tvlauncher

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.steenman.tvlauncher.data.AppRepository
import me.steenman.tvlauncher.data.FavoritesStore
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Audience
import me.steenman.tvlauncher.data.shows.AudienceOverride
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.data.shows.ShowConfig
import me.steenman.tvlauncher.data.shows.ShowList
import me.steenman.tvlauncher.data.shows.ShowRepository
import me.steenman.tvlauncher.data.shows.ShowRowConfig
import me.steenman.tvlauncher.data.shows.audienceAt
import me.steenman.tvlauncher.data.shows.forAudience
import me.steenman.tvlauncher.data.weather.Forecast
import me.steenman.tvlauncher.data.weather.WeatherRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

data class HomeUiState(
    /** Favorites as shown: apps that have a show row are left out. */
    val favorites: List<LauncherApp> = emptyList(),
    val showRows: List<ShowRow> = emptyList(),
    val apps: List<LauncherApp> = emptyList(),
    val loaded: Boolean = false,
    /** Every stored favorite, including those hidden because they have a show row. */
    val favoriteKeys: Set<String> = emptySet(),
    /** The mode the show rows are in, or null when the list has no schedule. */
    val audience: Audience? = null,
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
    private val showConfig = MutableStateFlow(ShowList(emptyList()))

    /** The mode in effect; see [keepAudienceCurrent]. */
    private val audience = MutableStateFlow<Audience?>(null)
    private var audienceOverride: AudienceOverride? = null

    /** Loaded artwork by image URL. Only grows when a new image arrives. */
    private val artwork = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    /**
     * Requests for [artworkWorker]. Conflated: a request made while it's busy runs once more
     * afterwards, so a list that changes mid-download still gets its new images.
     */
    private val artworkRequests = Channel<Unit>(Channel.CONFLATED)

    val uiState: StateFlow<HomeUiState> =
        combine(apps, favoritesStore.favorites, showConfig, artwork, audience) { apps, favorites, config, artwork, audience ->
            homeUiState(apps, favorites, config.rows, artwork, audience)
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private val showConfigLoad = viewModelScope.launch {
        showConfig.value = showRepository.loadConfig()
        updateAudience()
        loadArtwork()
    }

    /**
     * Prunes artwork no longer in the list and loads what isn't in memory yet: from disk, or
     * downloaded once. One request at a time, so pruning never removes a download in progress.
     * Failed images retry on the next request.
     */
    private val artworkWorker = viewModelScope.launch {
        for (request in artworkRequests) {
            val urls = showConfig.value.imageUrls()
            if (urls.isNotEmpty()) showRepository.pruneArtwork(urls)
            for (url in urls - artwork.value.keys) {
                showRepository.artwork(url)?.let { image -> artwork.update { it + (url to image) } }
            }
        }
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
                updateAudience()
                loadArtwork()
            }
        }
    }

    /**
     * Keeps the mode right for as long as the caller is active: recomputed now, and again at each
     * scheduled switch (kids-and-grown-ups design.md D4). An override ends at the next switch.
     */
    suspend fun keepAudienceCurrent() {
        showConfig.map { it.schedule }.distinctUntilChanged().collectLatest { schedule ->
            updateAudience()
            if (schedule == null) return@collectLatest
            while (true) {
                val now = LocalDateTime.now()
                // A second late, so the switch has certainly happened when it's recomputed.
                delay(Duration.between(now, schedule.nextSwitch(now)).toMillis() + 1_000)
                updateAudience()
            }
        }
    }

    /** Switches to the other mode until the next scheduled switch, or back to the scheduled one. */
    fun toggleAudience() {
        val schedule = showConfig.value.schedule ?: return
        val now = LocalDateTime.now()
        val current = schedule.audienceAt(now, audienceOverride) ?: return
        audienceOverride =
            if (current != schedule.audienceAt(now.toLocalTime())) null
            else AudienceOverride(current.other, until = schedule.nextSwitch(now))
        updateAudience()
    }

    private fun updateAudience() {
        audience.value = showConfig.value.schedule.audienceAt(LocalDateTime.now(), audienceOverride)
    }

    private fun loadArtwork() {
        artworkRequests.trySend(Unit)
    }

    /** Artwork for both modes, so switching never needs a download. */
    private fun ShowList.imageUrls(): Set<String> =
        rows.flatMap { row -> row.shows.mapNotNull { it.image } }.toSet()

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
    audience: Audience? = null,
): HomeUiState {
    val installed = apps.orEmpty()
    val byKey = installed.associateBy { it.key }
    val byPackage = installed.associateBy { it.packageName }
    // Rows for apps that aren't installed, or without shows, are hidden.
    // Rows left without shows in this mode are hidden like empty rows, so their app is a favorite again.
    val showRows = config.forAudience(audience).mapNotNull { row ->
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
        audience = audience,
    )
}
