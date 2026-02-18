package com.astutepodcasts.app.data.mapper

import com.astutepodcasts.app.data.remote.dto.PodcastDto
import com.astutepodcasts.app.domain.model.Podcast

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
