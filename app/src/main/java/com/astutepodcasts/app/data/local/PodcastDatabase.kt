package com.astutepodcasts.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.dao.SubscriptionDao
import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.data.local.entity.PodcastEntity
import com.astutepodcasts.app.data.local.entity.SubscriptionEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, SubscriptionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun subscriptionDao(): SubscriptionDao
}
