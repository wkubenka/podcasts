package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.data.remote.dto.EpisodeDto
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
