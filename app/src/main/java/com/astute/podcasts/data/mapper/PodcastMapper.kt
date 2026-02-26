package com.astute.podcasts.data.mapper

import com.astute.podcasts.data.local.entity.PodcastEntity
import com.astute.podcasts.data.remote.dto.PodcastDto
import com.astute.podcasts.domain.model.Podcast

fun PodcastDto.toDomain(): Podcast = Podcast(
    id = id,
    title = title,
    author = author,
    description = description,
    artworkUrl = artwork.takeIf { it.isNotBlank() } ?: image.takeIf { it.isNotBlank() },
    feedUrl = url,
    language = language.takeIf { it.isNotBlank() },
    episodeCount = episodeCount,
    lastUpdateTime = lastUpdateTime
)

fun Podcast.toEntity(): PodcastEntity = PodcastEntity(
    id = id,
    title = title,
    author = author,
    description = description,
    artworkUrl = artworkUrl,
    feedUrl = feedUrl,
    language = language,
    episodeCount = episodeCount,
    lastUpdateTime = lastUpdateTime,
    localArtworkPath = localArtworkPath
)

fun PodcastEntity.toDomain(): Podcast = Podcast(
    id = id,
    title = title,
    author = author,
    description = description,
    artworkUrl = artworkUrl,
    localArtworkPath = localArtworkPath,
    feedUrl = feedUrl,
    language = language,
    episodeCount = episodeCount,
    lastUpdateTime = lastUpdateTime
)
