package com.astutepodcasts.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingResponse(
    @SerialName("feeds") val feeds: List<PodcastDto> = emptyList()
)
