package me.steenman.tvlauncher.data.shows

import java.time.LocalDateTime
import java.time.LocalTime

/** Who a show is for; a show without one is for both. */
enum class Audience(val key: String) {
    KIDS("kids"),
    GROWN_UPS("grown-ups");

    val other: Audience get() = if (this == KIDS) GROWN_UPS else KIDS

    companion object {
        fun fromKey(key: String): Audience =
            entries.firstOrNull { it.key == key } ?: throw IllegalArgumentException("Unknown audience '$key'")
    }
}

/** The times each mode starts, in local time (kids-and-grown-ups design.md D2); wraps past midnight. */
data class Schedule(val kids: LocalTime, val grownUps: LocalTime) {

    init {
        require(kids != grownUps) { "Kids and grown-ups start at the same time" }
    }

    fun audienceAt(time: LocalTime): Audience {
        val kidsTime = if (kids < grownUps) time >= kids && time < grownUps else time >= kids || time < grownUps
        return if (kidsTime) Audience.KIDS else Audience.GROWN_UPS
    }

    /** The first switch strictly after [now]. */
    fun nextSwitch(now: LocalDateTime): LocalDateTime =
        listOf(kids, grownUps)
            .flatMap { listOf(now.toLocalDate().atTime(it), now.toLocalDate().plusDays(1).atTime(it)) }
            .filter { it > now }
            .min()
}

/** A mode chosen with the header button, in effect until [until] (design.md D3). */
data class AudienceOverride(val audience: Audience, val until: LocalDateTime)

/** The mode at [now]: the override while it lasts, else the schedule's; null without a schedule. */
fun Schedule?.audienceAt(now: LocalDateTime, override: AudienceOverride?): Audience? {
    if (this == null) return null
    return override?.takeIf { now < it.until }?.audience ?: audienceAt(now.toLocalTime())
}

/** Rows with only the shows for [audience] and those for both; with no audience, every show. */
fun List<ShowRowConfig>.forAudience(audience: Audience?): List<ShowRowConfig> =
    if (audience == null) this
    else map { row -> row.copy(shows = row.shows.filter { it.audience == null || it.audience == audience }) }
