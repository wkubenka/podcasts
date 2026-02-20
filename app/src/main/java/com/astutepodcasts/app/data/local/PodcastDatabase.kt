package com.astutepodcasts.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.dao.SubscriptionDao
import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.data.local.entity.PodcastEntity
import com.astutepodcasts.app.data.local.entity.SubscriptionEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, SubscriptionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class PodcastDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedPositionMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE episodes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE podcasts ADD COLUMN localArtworkPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE podcasts ADD COLUMN artworkCachedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
