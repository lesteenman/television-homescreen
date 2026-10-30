package me.steenman.tvlauncher.data.shows

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

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
        )

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
        )
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
    fun `each TV has its own file named after its device name`() {
        val base = "https://gist.githubusercontent.com/u/abc/raw/"
        assertEquals("${base}living-room-tv-2.json", ShowConfig.remoteUrl(base, "Living Room TV 2"))
        assertEquals("${base}keuken.json", ShowConfig.remoteUrl(base.trimEnd('/'), "  Keuken! "))
        assertEquals("${base}tv.json", ShowConfig.remoteUrl(base, "📺"))
    }

    @Test
    fun `bundled config files parse`() {
        listOf("src/main/assets/shows.json", "src/debug/assets/shows.json").forEach { path ->
            val rows = ShowConfig.parse(File(path).readText())
            assert(rows.isNotEmpty()) { "$path has no rows" }
        }
    }
}
