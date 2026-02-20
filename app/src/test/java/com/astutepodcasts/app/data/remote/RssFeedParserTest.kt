package com.astutepodcasts.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RssFeedParserTest {

    private val parser = RssFeedParser()
    private val podcastId = 42L
    private val podcastArtwork = "https://example.com/podcast-art.jpg"

    private fun rss(vararg items: String): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<rss version=\"2.0\" xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\">")
        append("<channel>")
        append("<title>Test Podcast</title>")
        for (item in items) append(item)
        append("</channel></rss>")
    }

    private fun item(
        title: String = "Episode Title",
        description: String = "Episode description",
        enclosureUrl: String = "https://example.com/audio.mp3",
        enclosureLength: String = "50000000",
        pubDate: String = "Wed, 01 Jan 2025 12:00:00 +0000",
        duration: String = "3600",
        imageHref: String? = null,
        episodeNumber: String? = null,
        seasonNumber: String? = null,
        itunesSummary: String? = null,
        includeEnclosure: Boolean = true
    ): String = buildString {
        append("<item>")
        append("<title>$title</title>")
        if (description.isNotEmpty()) append("<description>$description</description>")
        if (itunesSummary != null) append("<itunes:summary>$itunesSummary</itunes:summary>")
        if (includeEnclosure) {
            append("<enclosure url=\"$enclosureUrl\" length=\"$enclosureLength\" type=\"audio/mpeg\" />")
        }
        if (imageHref != null) append("<itunes:image href=\"$imageHref\" />")
        append("<pubDate>$pubDate</pubDate>")
        append("<itunes:duration>$duration</itunes:duration>")
        if (episodeNumber != null) append("<itunes:episode>$episodeNumber</itunes:episode>")
        if (seasonNumber != null) append("<itunes:season>$seasonNumber</itunes:season>")
        append("</item>")
    }

    // --- Basic parsing ---

    @Test
    fun `parse returns empty list for empty channel`() {
        val xml = rss()
        val episodes = parser.parse(xml, podcastId, podcastArtwork)
        assertTrue(episodes.isEmpty())
    }

    @Test
    fun `parse single episode`() {
        val xml = rss(item(title = "My Episode"))
        val episodes = parser.parse(xml, podcastId, podcastArtwork)
        assertEquals(1, episodes.size)
        assertEquals("My Episode", episodes[0].title)
    }

    @Test
    fun `parse multiple episodes`() {
        val xml = rss(
            item(title = "Episode 1", enclosureUrl = "https://example.com/ep1.mp3"),
            item(title = "Episode 2", enclosureUrl = "https://example.com/ep2.mp3"),
            item(title = "Episode 3", enclosureUrl = "https://example.com/ep3.mp3")
        )
        val episodes = parser.parse(xml, podcastId, podcastArtwork)
        assertEquals(3, episodes.size)
        assertEquals("Episode 1", episodes[0].title)
        assertEquals("Episode 2", episodes[1].title)
        assertEquals("Episode 3", episodes[2].title)
    }

    // --- Item filtering ---

    @Test
    fun `items without enclosure are skipped`() {
        val xml = rss(
            item(title = "Has Audio", enclosureUrl = "https://example.com/audio.mp3"),
            item(title = "No Audio", includeEnclosure = false)
        )
        val episodes = parser.parse(xml, podcastId, podcastArtwork)
        assertEquals(1, episodes.size)
        assertEquals("Has Audio", episodes[0].title)
    }

    // --- Field extraction ---

    @Test
    fun `enclosure url and length are parsed`() {
        val xml = rss(item(enclosureUrl = "https://cdn.example.com/ep.mp3", enclosureLength = "12345678"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("https://cdn.example.com/ep.mp3", ep.audioUrl)
        assertEquals(12345678L, ep.fileSize)
    }

    @Test
    fun `itunes summary used as fallback when description is empty`() {
        val xml = rss(item(description = "", itunesSummary = "Summary text"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("Summary text", ep.description)
    }

    @Test
    fun `description takes priority over itunes summary`() {
        val xml = rss(item(description = "Primary desc", itunesSummary = "Summary text"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("Primary desc", ep.description)
    }

    @Test
    fun `itunes image href is parsed`() {
        val xml = rss(item(imageHref = "https://example.com/ep-art.jpg"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("https://example.com/ep-art.jpg", ep.artworkUrl)
    }

    @Test
    fun `episode and season numbers are parsed`() {
        val xml = rss(item(episodeNumber = "5", seasonNumber = "2"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(5, ep.episodeNumber)
        assertEquals(2, ep.seasonNumber)
    }

    @Test
    fun `missing episode and season numbers are null`() {
        val xml = rss(item())
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertNull(ep.episodeNumber)
        assertNull(ep.seasonNumber)
    }

    // --- Duration parsing ---

    @Test
    fun `duration in HH MM SS format`() {
        val xml = rss(item(duration = "1:30:45"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(1 * 3600 + 30 * 60 + 45, ep.durationSeconds)
    }

    @Test
    fun `duration in MM SS format`() {
        val xml = rss(item(duration = "45:30"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(45 * 60 + 30, ep.durationSeconds)
    }

    @Test
    fun `duration in pure seconds`() {
        val xml = rss(item(duration = "3600"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(3600, ep.durationSeconds)
    }

    @Test
    fun `blank duration returns 0`() {
        val xml = rss(item(duration = ""))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(0, ep.durationSeconds)
    }

    // --- Date parsing ---

    @Test
    fun `RFC 2822 date with numeric timezone`() {
        val xml = rss(item(pubDate = "Wed, 01 Jan 2025 00:00:00 +0000"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertTrue("Expected positive timestamp, got ${ep.publishedAt}", ep.publishedAt > 0)
    }

    @Test
    fun `RFC 2822 date with named timezone`() {
        val xml = rss(item(pubDate = "Wed, 01 Jan 2025 00:00:00 GMT"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertTrue("Expected positive timestamp, got ${ep.publishedAt}", ep.publishedAt > 0)
    }

    @Test
    fun `RFC 2822 date without day name`() {
        val xml = rss(item(pubDate = "01 Jan 2025 00:00:00 +0000"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertTrue("Expected positive timestamp, got ${ep.publishedAt}", ep.publishedAt > 0)
    }

    @Test
    fun `unparseable date returns 0`() {
        val xml = rss(item(pubDate = "not-a-date"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(0L, ep.publishedAt)
    }

    @Test
    fun `blank pubDate returns 0`() {
        val xml = rss(item(pubDate = ""))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(0L, ep.publishedAt)
    }

    // --- Artwork fallback ---

    @Test
    fun `episode without itunes image falls back to podcast artwork`() {
        val xml = rss(item(imageHref = null))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(podcastArtwork, ep.artworkUrl)
    }

    @Test
    fun `episode without itunes image and null podcast artwork gets null`() {
        val xml = rss(item(imageHref = null))
        val ep = parser.parse(xml, podcastId, null)[0]
        assertNull(ep.artworkUrl)
    }

    // --- HTML descriptions ---

    @Test
    fun `CDATA description with HTML tags is preserved`() {
        val html = "<p>Episode with <b>bold</b> and <a href=\"https://example.com\">links</a></p>"
        val xml = rss(item(description = "<![CDATA[$html]]>"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(html, ep.description)
    }

    @Test
    fun `description with whitespace only falls back to itunes summary`() {
        val xml = rss(item(description = "   ", itunesSummary = "Fallback summary"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("Fallback summary", ep.description)
    }

    @Test
    fun `missing description element falls back to itunes summary`() {
        val xml = rss(item(description = "", itunesSummary = "Only summary"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("Only summary", ep.description)
    }

    @Test
    fun `missing description and no itunes summary gives empty string`() {
        val xml = rss(item(description = ""))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals("", ep.description)
    }

    // --- Default values ---

    @Test
    fun `episode id defaults to 0`() {
        val xml = rss(item())
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(0L, ep.id)
    }

    @Test
    fun `podcastId is passed through to episodes`() {
        val xml = rss(item())
        val ep = parser.parse(xml, 99L, podcastArtwork)[0]
        assertEquals(99L, ep.podcastId)
    }

    @Test
    fun `invalid enclosure length defaults to 0`() {
        val xml = rss(item(enclosureLength = "not-a-number"))
        val ep = parser.parse(xml, podcastId, podcastArtwork)[0]
        assertEquals(0L, ep.fileSize)
    }
}
