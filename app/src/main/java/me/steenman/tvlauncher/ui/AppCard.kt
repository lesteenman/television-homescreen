package me.steenman.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import me.steenman.tvlauncher.data.LauncherApp

/** 16:9 app tile: the app's banner, or its icon and label when it has none. */
@Composable
fun AppCard(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .aspectRatio(16f / 9f)
            // Keep the scaled-up focused card above its neighbours.
            .zIndex(if (focused) 1f else 0f)
            .onFocusChanged { focused = it.isFocused },
        colors = CardDefaults.colors(containerColor = CardContainer),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White)),
        ),
    ) {
        if (app.banner != null) {
            Image(
                bitmap = app.banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                app.icon?.let {
                    Image(bitmap = it, contentDescription = null, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
