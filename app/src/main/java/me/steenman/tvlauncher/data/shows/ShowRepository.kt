package me.steenman.tvlauncher.data.shows

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * The bundled show config, and its artwork: each image URL is downloaded once, cropped to the
 * tile size and kept on disk, so later starts and offline starts never download it again.
 */
class ShowRepository(private val context: Context) {

    private val artworkDir = File(context.filesDir, "artwork")

    suspend fun loadConfig(): List<ShowRowConfig> = withContext(Dispatchers.IO) {
        runCatching { ShowConfig.parse(context.assets.open(CONFIG_ASSET).use { it.readBytes().decodeToString() }) }
            .onFailure { Log.w(TAG, "Ignoring $CONFIG_ASSET", it) }
            .getOrDefault(emptyList())
    }

    /** The artwork for [url] from disk, downloading it first if needed; null if that fails. */
    suspend fun artwork(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val file = fileFor(url)
        runCatching {
            if (!file.exists()) download(url, file)
            BitmapFactory.decodeFile(file.path)?.asImageBitmap()
        }.onFailure { Log.w(TAG, "Artwork failed: $url", it) }.getOrNull()
    }

    /** Deletes cached artwork for URLs that are no longer in the config. */
    suspend fun pruneArtwork(keep: Collection<String>) = withContext(Dispatchers.IO) {
        val wanted = keep.map { fileFor(it).name }.toSet()
        artworkDir.listFiles()?.filter { it.name !in wanted }?.forEach { it.delete() }
    }

    private fun download(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        val source = try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
        val tile = cropToTile(checkNotNull(BitmapFactory.decodeByteArray(source, 0, source.size)) { "Not an image" })
        artworkDir.mkdirs()
        // Written under a temporary name, so a half-written file is never mistaken for a cached one.
        val partial = File(artworkDir, "${target.name}.part")
        partial.outputStream().use { tile.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, it) }
        check(partial.renameTo(target)) { "Cannot store $target" }
    }

    /** Center-crops to 16:9 and scales to the same size as app banners. */
    private fun cropToTile(bitmap: Bitmap): Bitmap {
        val cropHeight = minOf(bitmap.height, bitmap.width * TILE_HEIGHT_PX / TILE_WIDTH_PX)
        val cropWidth = cropHeight * TILE_WIDTH_PX / TILE_HEIGHT_PX
        val left = (bitmap.width - cropWidth) / 2
        val top = (bitmap.height - cropHeight) / 2
        val tile = Bitmap.createBitmap(TILE_WIDTH_PX, TILE_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        Canvas(tile).drawBitmap(
            bitmap,
            Rect(left, top, left + cropWidth, top + cropHeight),
            Rect(0, 0, TILE_WIDTH_PX, TILE_HEIGHT_PX),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        return tile
    }

    private fun fileFor(url: String): File {
        val hash = MessageDigest.getInstance("SHA-256").digest(url.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(artworkDir, "$hash.webp")
    }

    private companion object {
        const val TAG = "TvLauncher"
        const val CONFIG_ASSET = "shows.json"
        const val TIMEOUT_MS = 10_000
        const val TILE_WIDTH_PX = 320
        const val TILE_HEIGHT_PX = 180
        const val WEBP_QUALITY = 85
    }
}
