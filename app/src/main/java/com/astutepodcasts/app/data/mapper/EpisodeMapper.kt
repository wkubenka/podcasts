package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.data.remote.dto.EpisodeDto
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.model.Episode

fun EpisodeDto.toDomain(overridePodcastId: Long? = null): Episode = Episode(
    id = id,
    podcastId = overridePodcastId ?: podcastId,
    title = title,
    description = description,
    audioUrl = enclosureUrl,
    artworkUrl = image.takeIf { it.isNotBlank() } ?: feedImage.takeIf { it.isNotBlank() },
    publishedAt = datePublished,
    durationSeconds = duration,
    fileSize = enclosureLength,
    episodeNumber = episode,
    seasonNumber = season
)

fun Episode.toEntity(): EpisodeEntity = EpisodeEntity(
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
    downloadStatus = downloadStatus.name,
    localFilePath = localFilePath,
    lastPlayedPositionMs = lastPlayedPositionMs,
    lastPlayedAt = lastPlayedAt
)

fun EpisodeEntity.toDomain(): Episode = Episode(
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
    downloadStatus = DownloadStatus.valueOf(downloadStatus),
    localFilePath = localFilePath,
    lastPlayedPositionMs = lastPlayedPositionMs,
    lastPlayedAt = lastPlayedAt
)
