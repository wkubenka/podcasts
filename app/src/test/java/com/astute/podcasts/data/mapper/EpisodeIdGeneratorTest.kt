package com.astute.podcasts.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeIdGeneratorTest {

    private val generator = EpisodeIdGenerator()

    @Test
    fun `same url produces same id`() {
        val url = "https://example.com/episode1.mp3"
        val id1 = generator.generateId(url)
        val id2 = generator.generateId(url)
        assertEquals(id1, id2)
    }

    @Test
    fun `different urls produce different ids`() {
        val id1 = generator.generateId("https://example.com/episode1.mp3")
        val id2 = generator.generateId("https://example.com/episode2.mp3")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `generated id is always positive`() {
        val urls = listOf(
            "https://example.com/a.mp3",
            "https://example.com/b.mp3",
            "https://example.com/c.mp3",
            "https://example.com/very-long-url-with-many-segments/season-1/episode-42/audio.mp3",
            "",
            "a"
        )
        for (url in urls) {
            val id = generator.generateId(url)
            assertTrue("Expected positive id for '$url', got $id", id > 0 || id == 0L)
            assertTrue("Expected non-negative id for '$url', got $id", id >= 0)
        }
    }

    @Test
    fun `id is masked with Long MAX_VALUE`() {
        // The masking ensures the sign bit is always 0
        val id = generator.generateId("https://example.com/test.mp3")
        assertEquals(0L, id and Long.MIN_VALUE)
    }

    @Test
    fun `similar urls produce different ids`() {
        val id1 = generator.generateId("https://example.com/ep1.mp3")
        val id2 = generator.generateId("https://example.com/ep2.mp3")
        assertNotEquals(id1, id2)
    }

    @Test
    fun `empty string produces a valid id`() {
        val id = generator.generateId("")
        assertTrue(id >= 0)
    }
}
