package me.steenman.tvlauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.steenman.tvlauncher.data.AppRepository
import me.steenman.tvlauncher.data.FavoritesStore
import me.steenman.tvlauncher.data.LauncherApp

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

    fun refresh() {
        viewModelScope.launch { apps.value = repository.loadApps() }
    }

    fun addFavorite(app: LauncherApp) = viewModelScope.launch { favoritesStore.add(app.key) }

    fun removeFavorite(app: LauncherApp) = viewModelScope.launch { favoritesStore.remove(app.key) }

    fun moveFavorite(app: LauncherApp, offset: Int) =
        viewModelScope.launch { favoritesStore.move(app.key, offset) }

    fun onPackageFullyRemoved(packageName: String) {
        viewModelScope.launch { favoritesStore.removePackage(packageName) }
        refresh()
    }
}
