package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.model.Episode

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
