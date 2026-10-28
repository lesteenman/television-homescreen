package me.steenman.tvlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import me.steenman.tvlauncher.HomeUiState
import me.steenman.tvlauncher.R
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Show
import me.steenman.tvlauncher.data.weather.Forecast

private const val COLUMNS = 5
private val TileSpacing = 20.dp

// Overscan-safe margins for 1080p TVs.
private val SafeHorizontal = 48.dp
private val SafeVertical = 27.dp

@Composable
fun HomeScreen(
    state: HomeUiState,
    weather: Forecast?,
    resetSignal: Int,
    onLaunch: (LauncherApp) -> Unit,
    onLaunchShow: (LauncherApp, Show) -> Unit,
    onOpenSettings: () -> Unit,
    onAppInfo: (LauncherApp) -> Unit,
    onAddFavorite: (LauncherApp) -> Unit,
    onRemoveFavorite: (LauncherApp) -> Unit,
    onMoveFavorite: (LauncherApp, Int) -> Unit,
    /** Null when this device has no way to start the screensaver; the button is then hidden. */
    onStartScreensaver: (() -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val initialFocus = remember { FocusRequester() }
    var menuFor by remember { mutableStateOf<LauncherApp?>(null) }

    // Initial open and every HOME press: back to the top, focus on the first tile.
    LaunchedEffect(resetSignal, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        gridState.scrollToItem(0)
        withFrameNanos { }
        runCatching { initialFocus.requestFocus() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .gradientBackground(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(COLUMNS),
            state = gridState,
            contentPadding = PaddingValues(horizontal = SafeHorizontal, vertical = SafeVertical),
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
            verticalArrangement = Arrangement.spacedBy(TileSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth("header") { Header(weather, onOpenSettings, onStartScreensaver) }

            // Apps with shows come first; the remaining favorites follow under their own title.
            state.showRows.forEachIndexed { index, row ->
                fullWidth("shows:${row.app.key}") {
                    ShowRowView(
                        row = row,
                        columns = COLUMNS,
                        spacing = TileSpacing,
                        edgePadding = SafeHorizontal,
                        // Only the very first tile on screen takes initial focus.
                        focusFirst = if (index == 0) initialFocus else null,
                        onLaunchApp = onLaunch,
                        onLaunchShow = onLaunchShow,
                        onMenu = { menuFor = it },
                    )
                }
            }

            if (state.favorites.isNotEmpty()) {
                fullWidth("favorites-title") { SectionTitle(stringResource(R.string.favorites)) }
                appTiles(
                    prefix = "fav",
                    apps = state.favorites,
                    focusFirst = if (state.showRows.isEmpty()) initialFocus else null,
                    onLaunch = onLaunch,
                    onMenu = { menuFor = it },
                )
            }

            fullWidth("apps-title") { SectionTitle(stringResource(R.string.all_apps)) }
            appTiles(
                prefix = "app",
                apps = state.apps,
                focusFirst = if (state.favorites.isEmpty() && state.showRows.isEmpty()) initialFocus else null,
                onLaunch = onLaunch,
                onMenu = { menuFor = it },
            )
        }
    }

    menuFor?.let { app ->
        val index = state.favorites.indexOfFirst { it.key == app.key }
        AppActionSheet(
            app = app,
            isFavorite = state.isFavorite(app),
            canMoveLeft = index > 0,
            canMoveRight = index >= 0 && index < state.favorites.lastIndex,
            onDismiss = { menuFor = null },
            onAddFavorite = { onAddFavorite(app) },
            onRemoveFavorite = { onRemoveFavorite(app) },
            onMoveLeft = { onMoveFavorite(app, -1) },
            onMoveRight = { onMoveFavorite(app, +1) },
            onAppInfo = { onAppInfo(app) },
        )
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun LazyGridScope.appTiles(
    prefix: String,
    apps: List<LauncherApp>,
    focusFirst: FocusRequester?,
    onLaunch: (LauncherApp) -> Unit,
    onMenu: (LauncherApp) -> Unit,
) = itemsIndexed(apps, key = { _, app -> "$prefix:${app.key}" }) { index, app ->
    AppCard(
        app = app,
        onClick = { onLaunch(app) },
        onLongClick = { onMenu(app) },
        modifier = if (index == 0 && focusFirst != null) Modifier.focusRequester(focusFirst) else Modifier,
    )
}

@Composable
private fun Header(weather: Forecast?, onOpenSettings: () -> Unit, onStartScreensaver: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderButton(stringResource(R.string.settings), onOpenSettings)
            onStartScreensaver?.let { HeaderButton(stringResource(R.string.screensaver), it) }
        }
        Weather(weather)
        Clock()
    }
}

@Composable
private fun HeaderButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(containerColor = CardContainer, contentColor = TextPrimary),
    ) {
        Text(text)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp),
    )
}
