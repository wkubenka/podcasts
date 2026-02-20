package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeMapperTest {

    // --- Episode.toEntity() ---

    @Test
    fun `toEntity maps all fields`() {
        val episode = TestData.episode(
            id = 42,
            podcastId = 7,
            title = "Great Episode",
            description = "Desc",
            audioUrl = "https://audio.com/ep.mp3",
            artworkUrl = "https://art.com/ep.jpg",
            publishedAt = 999L,
            durationSeconds = 1800,
            fileSize = 25_000_000L,
            episodeNumber = 5,
            seasonNumber = 2,
            downloadStatus = DownloadStatus.DOWNLOADED,
            localFilePath = "/data/ep.mp3",
            lastPlayedPositionMs = 60000,
            lastPlayedAt = 500L,
            isArchived = true
        )

        val entity = episode.toEntity()

        assertEquals(42L, entity.id)
        assertEquals(7L, entity.podcastId)
        assertEquals("Great Episode", entity.title)
        assertEquals("Desc", entity.description)
        assertEquals("https://audio.com/ep.mp3", entity.audioUrl)
        assertEquals("https://art.com/ep.jpg", entity.artworkUrl)
        assertEquals(999L, entity.publishedAt)
        assertEquals(1800, entity.durationSeconds)
        assertEquals(25_000_000L, entity.fileSize)
        assertEquals(5, entity.episodeNumber)
        assertEquals(2, entity.seasonNumber)
        assertEquals("DOWNLOADED", entity.downloadStatus)
        assertEquals("/data/ep.mp3", entity.localFilePath)
        assertEquals(60000L, entity.lastPlayedPositionMs)
        assertEquals(500L, entity.lastPlayedAt)
        assertTrue(entity.isArchived)
    }

    @Test
    fun `toEntity stores downloadStatus as enum name string`() {
        for (status in DownloadStatus.entries) {
            val episode = TestData.episode(downloadStatus = status)
            assertEquals(status.name, episode.toEntity().downloadStatus)
        }
    }

    @Test
    fun `toEntity preserves null optional fields`() {
        val episode = TestData.episode(
            artworkUrl = null,
            episodeNumber = null,
            seasonNumber = null,
            localFilePath = null
        )
        val entity = episode.toEntity()
        assertNull(entity.artworkUrl)
        assertNull(entity.episodeNumber)
        assertNull(entity.seasonNumber)
        assertNull(entity.localFilePath)
    }

    // --- EpisodeEntity.toDomain() ---

    @Test
    fun `entity toDomain maps all fields`() {
        val entity = TestData.episodeEntity(
            id = 55,
            podcastId = 3,
            title = "Entity Episode",
            description = "Entity Desc",
            audioUrl = "https://audio.com/ent.mp3",
            artworkUrl = "https://art.com/ent.jpg",
            publishedAt = 777L,
            durationSeconds = 2400,
            fileSize = 30_000_000L,
            episodeNumber = 10,
            seasonNumber = 3,
            downloadStatus = "DOWNLOADING",
            localFilePath = "/data/ent.mp3",
            lastPlayedPositionMs = 120000,
            lastPlayedAt = 800L,
            isArchived = false
        )

        val episode = entity.toDomain()

        assertEquals(55L, episode.id)
        assertEquals(3L, episode.podcastId)
        assertEquals("Entity Episode", episode.title)
        assertEquals("Entity Desc", episode.description)
        assertEquals("https://audio.com/ent.mp3", episode.audioUrl)
        assertEquals("https://art.com/ent.jpg", episode.artworkUrl)
        assertEquals(777L, episode.publishedAt)
        assertEquals(2400, episode.durationSeconds)
        assertEquals(30_000_000L, episode.fileSize)
        assertEquals(10, episode.episodeNumber)
        assertEquals(3, episode.seasonNumber)
        assertEquals(DownloadStatus.DOWNLOADING, episode.downloadStatus)
        assertEquals("/data/ent.mp3", episode.localFilePath)
        assertEquals(120000L, episode.lastPlayedPositionMs)
        assertEquals(800L, episode.lastPlayedAt)
        assertEquals(false, episode.isArchived)
    }

    @Test
    fun `entity toDomain restores all DownloadStatus values`() {
        for (status in DownloadStatus.entries) {
            val entity = TestData.episodeEntity(downloadStatus = status.name)
            assertEquals(status, entity.toDomain().downloadStatus)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `entity toDomain throws for invalid downloadStatus string`() {
        val entity = TestData.episodeEntity(downloadStatus = "INVALID_STATUS")
        entity.toDomain()
    }

    // --- Round-trip ---

    @Test
    fun `round trip domain to entity to domain preserves data`() {
        val original = TestData.episode(
            downloadStatus = DownloadStatus.QUEUED,
            localFilePath = "/some/path.mp3",
            lastPlayedPositionMs = 5000,
            lastPlayedAt = 123L,
            isArchived = true
        )
        val roundTripped = original.toEntity().toDomain()
        assertEquals(original, roundTripped)
    }

    @Test
    fun `round trip preserves all download statuses`() {
        for (status in DownloadStatus.entries) {
            val original = TestData.episode(downloadStatus = status)
            assertEquals(original, original.toEntity().toDomain())
        }
    }
}
