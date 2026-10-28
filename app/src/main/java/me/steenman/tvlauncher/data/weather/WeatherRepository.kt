package me.steenman.tvlauncher.data.weather

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Fetches the forecast for one fixed location and keeps the last successful response on disk,
 * so the home screen can show it immediately on start and while offline.
 */
class WeatherRepository(
    context: Context,
    private val latitude: Double,
    private val longitude: Double,
) {
    private val cacheFile = AtomicFile(File(context.filesDir, "weather.json"))

    val isConfigured: Boolean get() = !latitude.isNaN() && !longitude.isNaN()

    suspend fun cached(): Forecast? = withContext(Dispatchers.IO) {
        runCatching {
            val entry = JSONObject(cacheFile.readFully().decodeToString())
            OpenMeteo.parse(entry.getString("body"), Instant.ofEpochMilli(entry.getLong("fetchedAt")))
        }.getOrNull()
    }

    /** Returns the new forecast, or null if it couldn't be fetched (the cache is then left alone). */
    suspend fun fetch(): Forecast? = withContext(Dispatchers.IO) {
        runCatching {
            val fetchedAt = Instant.now()
            val body = download(OpenMeteo.url(latitude, longitude))
            val forecast = OpenMeteo.parse(body, fetchedAt)
            store(body, fetchedAt)
            forecast
        }.onFailure { Log.w(TAG, "Weather fetch failed", it) }.getOrNull()
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }
    }

    private fun store(body: String, fetchedAt: Instant) {
        val entry = JSONObject().put("fetchedAt", fetchedAt.toEpochMilli()).put("body", body)
        val out = cacheFile.startWrite()
        try {
            out.write(entry.toString().encodeToByteArray())
            cacheFile.finishWrite(out)
        } catch (e: Exception) {
            cacheFile.failWrite(out)
            throw e
        }
    }

    private companion object {
        const val TAG = "TvLauncher"
        const val TIMEOUT_MS = 10_000
    }
}
