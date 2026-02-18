package com.astutepodcasts.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String,
    val description: String,
    val artworkUrl: String?,
    val feedUrl: String,
    val language: String?,
    val episodeCount: Int,
    val lastUpdateTime: Long
)
