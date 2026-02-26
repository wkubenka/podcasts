package com.astute.podcasts.domain.model

data class Podcast(
    val id: Long,
    val title: String,
    val author: String,
    val description: String,
    val artworkUrl: String?,
    val localArtworkPath: String? = null,
    val feedUrl: String,
    val language: String?,
    val episodeCount: Int,
    val lastUpdateTime: Long
)
