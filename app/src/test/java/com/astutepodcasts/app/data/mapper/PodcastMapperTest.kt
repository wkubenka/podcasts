package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastMapperTest {

    // --- PodcastDto.toDomain() ---

    @Test
    fun `toDomain maps all fields from dto`() {
        val dto = TestData.podcastDto(
            id = 42,
            title = "My Podcast",
            url = "https://feed.example.com/rss",
            author = "Jane Doe",
            artwork = "https://example.com/art.jpg",
            image = "https://example.com/img.jpg",
            description = "A great podcast",
            language = "en",
            episodeCount = 100,
            lastUpdateTime = 999L
        )

        val podcast = dto.toDomain()

        assertEquals(42L, podcast.id)
        assertEquals("My Podcast", podcast.title)
        assertEquals("Jane Doe", podcast.author)
        assertEquals("A great podcast", podcast.description)
        assertEquals("https://example.com/art.jpg", podcast.artworkUrl)
        assertEquals("https://feed.example.com/rss", podcast.feedUrl)
        assertEquals("en", podcast.language)
        assertEquals(100, podcast.episodeCount)
        assertEquals(999L, podcast.lastUpdateTime)
    }

    @Test
    fun `toDomain falls back to image when artwork is blank`() {
        val dto = TestData.podcastDto(artwork = "", image = "https://example.com/fallback.jpg")
        assertEquals("https://example.com/fallback.jpg", dto.toDomain().artworkUrl)
    }

    @Test
    fun `toDomain returns null artworkUrl when both artwork and image are blank`() {
        val dto = TestData.podcastDto(artwork = "", image = "")
        assertNull(dto.toDomain().artworkUrl)
    }

    @Test
    fun `toDomain maps blank language to null`() {
        val dto = TestData.podcastDto(language = "")
        assertNull(dto.toDomain().language)
    }

    @Test
    fun `toDomain maps whitespace-only language to null`() {
        val dto = TestData.podcastDto(language = "   ")
        assertNull(dto.toDomain().language)
    }

    @Test
    fun `toDomain preserves non-blank language`() {
        val dto = TestData.podcastDto(language = "fr")
        assertEquals("fr", dto.toDomain().language)
    }

    @Test
    fun `toDomain maps url to feedUrl`() {
        val dto = TestData.podcastDto(url = "https://my-feed.com/rss")
        assertEquals("https://my-feed.com/rss", dto.toDomain().feedUrl)
    }

    // --- Podcast.toEntity() ---

    @Test
    fun `toEntity maps all fields from domain`() {
        val podcast = TestData.podcast(
            id = 7,
            title = "Entity Podcast",
            artworkUrl = "https://art.com/pic.png",
            feedUrl = "https://feed.com/rss",
            language = "de"
        )

        val entity = podcast.toEntity()

        assertEquals(7L, entity.id)
        assertEquals("Entity Podcast", entity.title)
        assertEquals("https://art.com/pic.png", entity.artworkUrl)
        assertEquals("https://feed.com/rss", entity.feedUrl)
        assertEquals("de", entity.language)
    }

    @Test
    fun `toEntity preserves null artworkUrl`() {
        val podcast = TestData.podcast(artworkUrl = null)
        assertNull(podcast.toEntity().artworkUrl)
    }

    @Test
    fun `toEntity preserves null language`() {
        val podcast = TestData.podcast(language = null)
        assertNull(podcast.toEntity().language)
    }

    // --- PodcastEntity.toDomain() ---

    @Test
    fun `entity toDomain maps all fields`() {
        val entity = TestData.podcastEntity(
            id = 99,
            title = "From Entity",
            author = "Author",
            description = "Desc",
            artworkUrl = "https://art.com/a.jpg",
            feedUrl = "https://feed.com/f",
            language = "ja",
            episodeCount = 50,
            lastUpdateTime = 12345L
        )

        val podcast = entity.toDomain()

        assertEquals(99L, podcast.id)
        assertEquals("From Entity", podcast.title)
        assertEquals("Author", podcast.author)
        assertEquals("Desc", podcast.description)
        assertEquals("https://art.com/a.jpg", podcast.artworkUrl)
        assertEquals("https://feed.com/f", podcast.feedUrl)
        assertEquals("ja", podcast.language)
        assertEquals(50, podcast.episodeCount)
        assertEquals(12345L, podcast.lastUpdateTime)
    }

    // --- Round-trip ---

    @Test
    fun `round trip dto to domain to entity to domain preserves data`() {
        val dto = TestData.podcastDto(
            id = 5,
            title = "Round Trip",
            url = "https://feed.com/rss",
            author = "Author",
            artwork = "https://art.com/pic.jpg",
            image = "",
            description = "Description",
            language = "en",
            episodeCount = 25,
            lastUpdateTime = 500L
        )

        val firstDomain = dto.toDomain()
        val entity = firstDomain.toEntity()
        val secondDomain = entity.toDomain()

        assertEquals(firstDomain, secondDomain)
    }

    @Test
    fun `round trip domain to entity to domain preserves data`() {
        val original = TestData.podcast()
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original, roundTripped)
    }
}
