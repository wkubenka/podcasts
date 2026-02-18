package com.astutepodcasts.app.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.astutepodcasts.app.data.local.PodcastDatabase
import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.dao.SubscriptionDao
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
        ).build()
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
