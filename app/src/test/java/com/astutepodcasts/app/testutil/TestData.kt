package com.astutepodcasts.app.testutil

import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.data.local.entity.PodcastEntity
import com.astutepodcasts.app.data.remote.dto.PodcastDto
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast

object TestData {

    fun podcast(
        id: Long = 1L,
        title: String = "Test Podcast",
        author: String = "Test Author",
        description: String = "Test description",
        artworkUrl: String? = "https://example.com/artwork.jpg",
        feedUrl: String = "https://example.com/feed.xml",
        language: String? = "en",
        episodeCount: Int = 10,
        lastUpdateTime: Long = 1000000L
    ) = Podcast(
        id = id,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        language = language,
        episodeCount = episodeCount,
        lastUpdateTime = lastUpdateTime
    )

    fun episode(
        id: Long = 100L,
        podcastId: Long = 1L,
        title: String = "Test Episode",
        description: String = "Episode description",
        audioUrl: String = "https://example.com/audio.mp3",
        artworkUrl: String? = "https://example.com/episode-art.jpg",
        publishedAt: Long = 1000000L,
        durationSeconds: Int = 3600,
        fileSize: Long = 50_000_000L,
        episodeNumber: Int? = 1,
        seasonNumber: Int? = 1,
        downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
        localFilePath: String? = null,
        lastPlayedPositionMs: Long = 0,
        lastPlayedAt: Long = 0,
        isArchived: Boolean = false
    ) = Episode(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        artworkUrl = artworkUrl,
        publishedAt = publishedAt,
        durationSeconds = durationSeconds,
        fileSize = fileSize,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
        downloadStatus = downloadStatus,
        localFilePath = localFilePath,
        lastPlayedPositionMs = lastPlayedPositionMs,
        lastPlayedAt = lastPlayedAt,
        isArchived = isArchived
    )

    fun podcastDto(
        id: Long = 1L,
        title: String = "Test Podcast",
        url: String = "https://example.com/feed.xml",
        author: String = "Test Author",
        image: String = "https://example.com/image.jpg",
        artwork: String = "https://example.com/artwork.jpg",
        description: String = "Test description",
        language: String = "en",
        episodeCount: Int = 10,
        lastUpdateTime: Long = 1000000L
    ) = PodcastDto(
        id = id,
        title = title,
        url = url,
        author = author,
        image = image,
        artwork = artwork,
        description = description,
        language = language,
        episodeCount = episodeCount,
        lastUpdateTime = lastUpdateTime
    )

    fun podcastEntity(
        id: Long = 1L,
        title: String = "Test Podcast",
        author: String = "Test Author",
        description: String = "Test description",
        artworkUrl: String? = "https://example.com/artwork.jpg",
        feedUrl: String = "https://example.com/feed.xml",
        language: String? = "en",
        episodeCount: Int = 10,
        lastUpdateTime: Long = 1000000L
    ) = PodcastEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        artworkUrl = artworkUrl,
        feedUrl = feedUrl,
        language = language,
        episodeCount = episodeCount,
        lastUpdateTime = lastUpdateTime
    )

    fun episodeEntity(
        id: Long = 100L,
        podcastId: Long = 1L,
        title: String = "Test Episode",
        description: String = "Episode description",
        audioUrl: String = "https://example.com/audio.mp3",
        artworkUrl: String? = "https://example.com/episode-art.jpg",
        publishedAt: Long = 1000000L,
        durationSeconds: Int = 3600,
        fileSize: Long = 50_000_000L,
        episodeNumber: Int? = 1,
        seasonNumber: Int? = 1,
        downloadStatus: String = "NOT_DOWNLOADED",
        localFilePath: String? = null,
        lastPlayedPositionMs: Long = 0,
        lastPlayedAt: Long = 0,
        isArchived: Boolean = false
    ) = EpisodeEntity(
        id = id,
        podcastId = podcastId,
        title = title,
        description = description,
        audioUrl = audioUrl,
        artworkUrl = artworkUrl,
        publishedAt = publishedAt,
        durationSeconds = durationSeconds,
        fileSize = fileSize,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
        downloadStatus = downloadStatus,
        localFilePath = localFilePath,
        lastPlayedPositionMs = lastPlayedPositionMs,
        lastPlayedAt = lastPlayedAt,
        isArchived = isArchived
    )
}
