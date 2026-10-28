package me.steenman.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import me.steenman.tvlauncher.ShowRow
import me.steenman.tvlauncher.data.LauncherApp
import me.steenman.tvlauncher.data.shows.Show

/**
 * One app's row: the app tile, then its shows, scrolling sideways. Tiles are as wide as the
 * grid's columns, so the row lines up with the tiles above and below it.
 */
@Composable
fun ShowRowView(
    row: ShowRow,
    columns: Int,
    spacing: Dp,
    edgePadding: Dp,
    focusFirst: FocusRequester?,
    onLaunchApp: (LauncherApp) -> Unit,
    onLaunchShow: (LauncherApp, Show) -> Unit,
    onMenu: (LauncherApp) -> Unit,
) {
    BoxWithConstraints {
        val tileWidth = (maxWidth - spacing * (columns - 1)) / columns
        LazyRow(
            // Reach into the grid's side margins, so a focused tile scrolled to the edge isn't clipped.
            modifier = Modifier.extendHorizontally(edgePadding),
            contentPadding = PaddingValues(horizontal = edgePadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(key = "app") {
                AppCard(
                    app = row.app,
                    onClick = { onLaunchApp(row.app) },
                    onLongClick = { onMenu(row.app) },
                    modifier = Modifier
                        .width(tileWidth)
                        .then(if (focusFirst != null) Modifier.focusRequester(focusFirst) else Modifier),
                )
            }
            itemsIndexed(row.shows, key = { index, tile -> "$index:${tile.show.link}" }) { _, tile ->
                ShowCard(
                    show = tile.show,
                    artwork = tile.artwork,
                    onClick = { onLaunchShow(row.app, tile.show) },
                    modifier = Modifier.width(tileWidth),
                )
            }
        }
    }
}

/** 16:9 show tile: the cached artwork, or the title while there is none. */
@Composable
private fun ShowCard(
    show: Show,
    artwork: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(16f / 9f)
            .zIndex(if (focused) 1f else 0f)
            .onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(containerColor = CardContainer),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White)),
        ),
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = show.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = show.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Lays the content out [extra] wider on both sides than the space it was given. */
private fun Modifier.extendHorizontally(extra: Dp) = layout { measurable, constraints ->
    val extraPx = extra.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + 2 * extraPx,
            maxWidth = constraints.maxWidth + 2 * extraPx,
        ),
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extraPx, 0) }
}
