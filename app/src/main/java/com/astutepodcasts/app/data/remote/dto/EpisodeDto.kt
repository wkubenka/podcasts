package com.astutepodcasts.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    @SerialName("id") val id: Long,
    @SerialName("feedId") val podcastId: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("enclosureUrl") val enclosureUrl: String = "",
    @SerialName("enclosureLength") val enclosureLength: Long = 0,
    @SerialName("image") val image: String = "",
    @SerialName("feedImage") val feedImage: String = "",
    @SerialName("datePublished") val datePublished: Long = 0,
    @SerialName("duration") val duration: Int = 0,
    @SerialName("episode") val episode: Int? = null,
    @SerialName("season") val season: Int? = null
)
