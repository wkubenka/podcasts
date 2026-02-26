package com.astute.podcasts.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    @SerialName("feeds") val feeds: List<PodcastDto> = emptyList()
)
