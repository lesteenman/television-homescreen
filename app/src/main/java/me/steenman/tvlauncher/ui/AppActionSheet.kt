package me.steenman.tvlauncher.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import me.steenman.tvlauncher.R
import me.steenman.tvlauncher.data.LauncherApp

/** Long-press menu for an app card. Move actions only appear for favorites. */
@Composable
fun AppActionSheet(
    app: LauncherApp,
    isFavorite: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onDismiss: () -> Unit,
    onAddFavorite: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onAppInfo: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        val firstItem = remember { FocusRequester() }
        val first = Modifier.focusRequester(firstItem)

        Surface(
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = Color(0xF0101C2A)),
            modifier = Modifier.width(360.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
                if (isFavorite) {
                    ActionItem(R.string.remove_favorite, onRemoveFavorite, onDismiss, first)
                    if (canMoveLeft) ActionItem(R.string.move_left, onMoveLeft, onDismiss)
                    if (canMoveRight) ActionItem(R.string.move_right, onMoveRight, onDismiss)
                } else {
                    ActionItem(R.string.add_favorite, onAddFavorite, onDismiss, first)
                }
                ActionItem(R.string.app_info, onAppInfo, onDismiss)
            }
        }

        LaunchedEffect(Unit) { runCatching { firstItem.requestFocus() } }
    }
}

@Composable
private fun ActionItem(
    @StringRes label: Int,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        selected = false,
        onClick = {
            onClick()
            onDismiss()
        },
        headlineContent = { Text(stringResource(label)) },
        modifier = modifier,
    )
}
