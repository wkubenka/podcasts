package com.astute.podcasts.data.mapper

import com.astute.podcasts.data.local.entity.EpisodeEntity
import com.astute.podcasts.domain.model.DownloadStatus
import com.astute.podcasts.domain.model.Episode

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
    lastPlayedAt = lastPlayedAt,
    isArchived = isArchived
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
    lastPlayedAt = lastPlayedAt,
    isArchived = isArchived
)
