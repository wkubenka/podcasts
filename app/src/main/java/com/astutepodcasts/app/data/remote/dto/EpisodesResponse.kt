package com.astutepodcasts.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodesResponse(
    @SerialName("items") val items: List<EpisodeDto> = emptyList()
)
