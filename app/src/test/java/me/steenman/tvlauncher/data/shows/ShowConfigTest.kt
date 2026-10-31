package me.steenman.tvlauncher.data.shows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.time.LocalTime

class ShowConfigTest {

    @Test
    fun `parses rows and shows in file order`() {
        val rows = ShowConfig.parse(
            """
            {
              "version": 1,
              "rows": [
                {
                  "app": "com.netflix.ninja",
                  "shows": [
                    { "title": "A", "link": "https://www.netflix.com/title/1", "image": "https://img/a.jpg" },
                    { "title": "B", "link": "https://www.netflix.com/title/2" }
                  ]
                },
                { "app": "com.spotify.tv.android", "shows": [] }
              ]
            }
            """,
        ).rows

        assertEquals(
            listOf(
                ShowRowConfig(
                    "com.netflix.ninja",
                    listOf(
                        Show("A", "https://www.netflix.com/title/1", "https://img/a.jpg"),
                        Show("B", "https://www.netflix.com/title/2", null),
                    ),
                ),
                ShowRowConfig("com.spotify.tv.android", emptyList()),
            ),
            rows,
        )
    }

    @Test
    fun `ignores unknown fields and treats a blank image as none`() {
        val rows = ShowConfig.parse(
            """
            {"version": 1, "comment": "x", "rows": [
              {"app": "p", "colour": "red", "shows": [{"title": "T", "link": "l", "image": "", "year": 2020}]}
            ]}
            """,
        ).rows
        assertEquals(listOf(ShowRowConfig("p", listOf(Show("T", "l", null)))), rows)
    }

    @Test
    fun `rejects other versions and broken files`() {
        assertThrows(IllegalArgumentException::class.java) { ShowConfig.parse("""{"version": 2, "rows": []}""") }
        assertThrows(Exception::class.java) { ShowConfig.parse("""{"rows": []}""") }
        assertThrows(Exception::class.java) { ShowConfig.parse("""{"version": 1, "rows": [{"shows": []}]}""") }
        assertThrows(Exception::class.java) { ShowConfig.parse("not json") }
    }

    @Test
    fun `parses audiences and the schedule`() {
        val list = ShowConfig.parse(
            """
            {"version": 1, "schedule": {"kids": "05:00", "grown-ups": "19:00"}, "rows": [
              {"app": "p", "shows": [
                {"title": "K", "link": "k", "for": "kids"},
                {"title": "G", "link": "g", "for": "grown-ups"},
                {"title": "B", "link": "b"}
              ]}
            ]}
            """,
        )
        assertEquals(Schedule(LocalTime.of(5, 0), LocalTime.of(19, 0)), list.schedule)
        assertEquals(listOf(Audience.KIDS, Audience.GROWN_UPS, null), list.rows.single().shows.map { it.audience })
    }

    @Test
    fun `a list without a schedule has none`() {
        assertNull(ShowConfig.parse("""{"version": 1, "rows": []}""").schedule)
    }

    @Test
    fun `rejects unknown audiences and broken schedules`() {
        val row = """{"app": "p", "shows": [{"title": "T", "link": "l", "for": "teens"}]}"""
        assertThrows(IllegalArgumentException::class.java) { ShowConfig.parse("""{"version": 1, "rows": [$row]}""") }
        listOf(
            """{"kids": "5 uur", "grown-ups": "19:00"}""",
            """{"kids": "05:00"}""",
            """{"kids": "19:00", "grown-ups": "19:00"}""",
        ).forEach { schedule ->
            assertThrows(Exception::class.java) { ShowConfig.parse("""{"version": 1, "schedule": $schedule, "rows": []}""") }
        }
    }

    @Test
    fun `each TV has its own file named after its device name`() {
        val base = "https://gist.githubusercontent.com/u/abc/raw/"
        assertEquals("${base}living-room-tv-2.json", ShowConfig.remoteUrl(base, "Living Room TV 2"))
        assertEquals("${base}keuken.json", ShowConfig.remoteUrl(base.trimEnd('/'), "  Keuken! "))
        assertEquals("${base}tv.json", ShowConfig.remoteUrl(base, "📺"))
    }

    @Test
    fun `bundled config files parse`() {
        listOf("src/main/assets/shows.json", "src/debug/assets/shows.json").forEach { path ->
            val rows = ShowConfig.parse(File(path).readText()).rows
            assert(rows.isNotEmpty()) { "$path has no rows" }
        }
    }
}
