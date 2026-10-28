package me.steenman.tvlauncher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time and date, refreshed on every system minute tick and whenever the screen resumes. */
@Composable
fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                now = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LifecycleResumeEffect(Unit) {
        now = LocalDateTime.now()
        onPauseOrDispose { }
    }

    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    val datePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM")

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = now.format(DateTimeFormatter.ofPattern(timePattern)),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Light,
        )
        Text(
            text = now.format(DateTimeFormatter.ofPattern(datePattern)),
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
        )
    }
}
