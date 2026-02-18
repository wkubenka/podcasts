package com.astutepodcasts.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astutepodcasts.app.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(podcast: PodcastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(podcasts: List<PodcastEntity>)

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
}
