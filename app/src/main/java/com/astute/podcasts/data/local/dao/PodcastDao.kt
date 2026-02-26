package com.astute.podcasts.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.astute.podcasts.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

data class PodcastFeedInfo(val id: Long, val feedUrl: String)

data class PodcastArtworkInfo(
    val id: Long,
    val artworkUrl: String?,
    val localArtworkPath: String?,
    val artworkCachedAt: Long
)

@Dao
interface PodcastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(podcasts: List<PodcastEntity>)

    @Query(
        """
        INSERT INTO podcasts (id, title, author, description, artworkUrl, feedUrl, language, episodeCount, lastUpdateTime, localArtworkPath, artworkCachedAt)
        VALUES (:id, :title, :author, :description, :artworkUrl, :feedUrl, :language, :episodeCount, :lastUpdateTime, NULL, 0)
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            author = excluded.author,
            description = excluded.description,
            artworkUrl = excluded.artworkUrl,
            feedUrl = excluded.feedUrl,
            language = excluded.language,
            episodeCount = excluded.episodeCount,
            lastUpdateTime = excluded.lastUpdateTime
        """
    )
    suspend fun upsertPreservingArtworkCache(
        id: Long, title: String, author: String, description: String,
        artworkUrl: String?, feedUrl: String, language: String?,
        episodeCount: Int, lastUpdateTime: Long
    )

    @Transaction
    suspend fun upsertAllPreservingArtworkCache(podcasts: List<PodcastEntity>) {
        for (podcast in podcasts) {
            upsertPreservingArtworkCache(
                podcast.id, podcast.title, podcast.author, podcast.description,
                podcast.artworkUrl, podcast.feedUrl, podcast.language,
                podcast.episodeCount, podcast.lastUpdateTime
            )
        }
    }

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: Long): PodcastEntity?

    @Query(
        """
        SELECT p.* FROM podcasts p
        INNER JOIN subscriptions s ON p.id = s.podcastId
        ORDER BY s.subscribedAt DESC
        """
    )
    fun getSubscribedPodcasts(): Flow<List<PodcastEntity>>

    @Query(
        """
        SELECT p.id, p.feedUrl FROM podcasts p
        INNER JOIN subscriptions s ON p.id = s.podcastId
        """
    )
    suspend fun getSubscribedPodcastFeedInfos(): List<PodcastFeedInfo>

    @Query(
        """
        SELECT p.id, p.artworkUrl, p.localArtworkPath, p.artworkCachedAt
        FROM podcasts p
        INNER JOIN subscriptions s ON p.id = s.podcastId
        WHERE p.artworkUrl IS NOT NULL
        AND (p.localArtworkPath IS NULL OR p.artworkCachedAt < :staleThreshold)
        """
    )
    suspend fun getSubscribedPodcastsNeedingArtworkRefresh(staleThreshold: Long): List<PodcastArtworkInfo>

    @Query("UPDATE podcasts SET localArtworkPath = :localArtworkPath, artworkCachedAt = :artworkCachedAt WHERE id = :podcastId")
    suspend fun updateArtworkCache(podcastId: Long, localArtworkPath: String?, artworkCachedAt: Long)
}
