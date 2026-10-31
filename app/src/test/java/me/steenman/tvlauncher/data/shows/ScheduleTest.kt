package me.steenman.tvlauncher.data.shows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {

    private val schedule = Schedule(kids = LocalTime.of(5, 0), grownUps = LocalTime.of(19, 0))
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, day, hour, minute)

    @Test
    fun `kids by day, grown-ups in the evening and night`() {
        assertEquals(Audience.KIDS, schedule.audienceAt(LocalTime.of(5, 0)))
        assertEquals(Audience.KIDS, schedule.audienceAt(LocalTime.of(15, 0)))
        assertEquals(Audience.KIDS, schedule.audienceAt(LocalTime.of(18, 59)))
        assertEquals(Audience.GROWN_UPS, schedule.audienceAt(LocalTime.of(19, 0)))
        assertEquals(Audience.GROWN_UPS, schedule.audienceAt(LocalTime.of(23, 30)))
        assertEquals(Audience.GROWN_UPS, schedule.audienceAt(LocalTime.of(3, 0)))
    }

    @Test
    fun `a schedule may start kids' mode after grown-ups' mode`() {
        val nights = Schedule(kids = LocalTime.of(22, 0), grownUps = LocalTime.of(6, 0))
        assertEquals(Audience.KIDS, nights.audienceAt(LocalTime.of(23, 0)))
        assertEquals(Audience.KIDS, nights.audienceAt(LocalTime.of(2, 0)))
        assertEquals(Audience.GROWN_UPS, nights.audienceAt(LocalTime.of(12, 0)))
    }

    @Test
    fun `next switch is the first one after now`() {
        assertEquals(at(31, 19), schedule.nextSwitch(at(31, 15)))
        assertEquals(at(31, 19), schedule.nextSwitch(at(31, 5)))
        assertEquals(at(31, 5).plusDays(1), schedule.nextSwitch(at(31, 19)))
        assertEquals(at(31, 5), schedule.nextSwitch(at(31, 3)))
    }

    @Test
    fun `override lasts until it ends, then the schedule applies`() {
        val override = AudienceOverride(Audience.GROWN_UPS, until = at(31, 19))
        assertEquals(Audience.GROWN_UPS, schedule.audienceAt(at(31, 15), override))
        assertEquals(Audience.GROWN_UPS, schedule.audienceAt(at(31, 19), override))
        assertEquals(Audience.KIDS, schedule.audienceAt(at(31, 15), null))

        val kidsTonight = AudienceOverride(Audience.KIDS, until = at(31, 5).plusDays(1))
        assertEquals(Audience.KIDS, schedule.audienceAt(at(31, 23), kidsTonight))
        assertEquals(Audience.KIDS, schedule.audienceAt(at(31, 5).plusDays(1), kidsTonight))
    }

    @Test
    fun `no schedule, no audience`() {
        assertNull((null as Schedule?).audienceAt(at(31, 15), null))
    }

    @Test
    fun `rows keep the shows for the audience and for both`() {
        val kids = Show("K", "k", null, Audience.KIDS)
        val grownUps = Show("G", "g", null, Audience.GROWN_UPS)
        val both = Show("B", "b", null)
        val rows = listOf(ShowRowConfig("p", listOf(kids, grownUps, both)))
        assertEquals(listOf(kids, both), rows.forAudience(Audience.KIDS).single().shows)
        assertEquals(listOf(grownUps, both), rows.forAudience(Audience.GROWN_UPS).single().shows)
        assertEquals(rows, rows.forAudience(null))
    }
}
