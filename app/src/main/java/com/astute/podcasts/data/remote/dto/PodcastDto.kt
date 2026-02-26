package com.astute.podcasts.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PodcastDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("author") val author: String = "",
    @SerialName("image") val image: String = "",
    @SerialName("artwork") val artwork: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("episodeCount") val episodeCount: Int = 0,
    @SerialName("lastUpdateTime") val lastUpdateTime: Long = 0
)
