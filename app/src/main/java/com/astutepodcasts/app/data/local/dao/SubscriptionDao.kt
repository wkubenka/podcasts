package com.astutepodcasts.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.astutepodcasts.app.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE podcastId = :podcastId")
    suspend fun deleteByPodcastId(podcastId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE podcastId = :podcastId)")
    fun isSubscribed(podcastId: Long): Flow<Boolean>

    @Query("SELECT podcastId FROM subscriptions")
    suspend fun getAllSubscribedPodcastIds(): List<Long>
}
