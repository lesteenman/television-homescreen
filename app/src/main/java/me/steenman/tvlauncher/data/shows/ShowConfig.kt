package me.steenman.tvlauncher.data.shows

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalTime

/** A TV's show list: its rows, and its schedule for kids and grown-ups if it has one. */
data class ShowList(val rows: List<ShowRowConfig>, val schedule: Schedule? = null)

/** One app's row of shows, in the order of the config file. */
data class ShowRowConfig(
    /** Package name of the app that opens the shows, e.g. `com.netflix.ninja`. */
    val app: String,
    val shows: List<Show>,
)

data class Show(
    val title: String,
    /** Deep link, opened as ACTION_VIEW restricted to the row's app. */
    val link: String,
    /** Artwork URL (16:9 looks best), or null for a title-only tile. */
    val image: String?,
    /** Who the show is for; null for both. */
    val audience: Audience? = null,
)

/** The `shows.json` format (design.md D2). It doesn't care where the text came from. */
object ShowConfig {

    const val VERSION = 1

    /** Throws on an invalid file or another [VERSION]; unknown fields are ignored. */
    fun parse(json: String): ShowList {
        val root = JSONObject(json)
        val version = root.getInt("version")
        require(version == VERSION) { "Unsupported shows.json version $version" }
        val schedule = root.optJSONObject("schedule")?.let {
            Schedule(kids = LocalTime.parse(it.getString("kids")), grownUps = LocalTime.parse(it.getString("grown-ups")))
        }
        val rows = root.getJSONArray("rows").objects().map { row ->
            ShowRowConfig(
                app = row.getString("app"),
                shows = row.getJSONArray("shows").objects().map { show ->
                    Show(
                        title = show.getString("title"),
                        link = show.getString("link"),
                        image = show.optString("image").takeIf { it.isNotBlank() },
                        audience = show.optString("for").takeIf { it.isNotBlank() }?.let(Audience::fromKey),
                    )
                },
            )
        }
        return ShowList(rows, schedule)
    }

    /**
     * The URL of this TV's own list (design.md D1): [base] is the gist's raw URL ending in `/`,
     * and the file is named after the device name, e.g. "Living Room TV 2" -> `living-room-tv-2.json`.
     */
    fun remoteUrl(base: String, deviceName: String): String {
        val slug = deviceName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "tv" }
        return "${base.trimEnd('/')}/$slug.json"
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
