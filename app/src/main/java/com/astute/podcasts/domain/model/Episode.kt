package com.astute.podcasts.domain.model

data class Episode(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val description: String,
    val audioUrl: String,
    val artworkUrl: String?,
    val publishedAt: Long,
    val durationSeconds: Int,
    val fileSize: Long,
    val episodeNumber: Int?,
    val seasonNumber: Int?,
    val downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val localFilePath: String? = null,
    val lastPlayedPositionMs: Long = 0,
    val lastPlayedAt: Long = 0,
    val isArchived: Boolean = false
)
