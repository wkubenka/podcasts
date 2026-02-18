package com.astutepodcasts.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class EpisodeEntity(
    @PrimaryKey val id: Long,
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
    val downloadStatus: String,
    val localFilePath: String?,
    val lastPlayedPositionMs: Long = 0,
    val lastPlayedAt: Long = 0
)
