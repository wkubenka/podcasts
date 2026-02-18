package com.astutepodcasts.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    suspend fun getByPodcastId(podcastId: Long): List<EpisodeEntity>

    @Query(
        """
        SELECT e.* FROM episodes e
        INNER JOIN subscriptions s ON e.podcastId = s.podcastId
        ORDER BY e.publishedAt DESC
        LIMIT :limit
        """
    )
    fun getRecentSubscribedEpisodes(limit: Int = 50): Flow<List<EpisodeEntity>>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteByPodcastId(podcastId: Long)
}
