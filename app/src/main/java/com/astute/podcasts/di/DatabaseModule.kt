package com.astute.podcasts.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.astute.podcasts.data.local.PodcastDatabase
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.data.local.dao.PodcastDao
import com.astute.podcasts.data.local.dao.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PodcastDatabase {
        return Room.databaseBuilder(
            context,
            PodcastDatabase::class.java,
            "podcast_database"
        ).addMigrations(PodcastDatabase.MIGRATION_1_2, PodcastDatabase.MIGRATION_2_3, PodcastDatabase.MIGRATION_3_4).build()
    }

    @Provides
    fun providePodcastDao(database: PodcastDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideEpisodeDao(database: PodcastDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideSubscriptionDao(database: PodcastDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
