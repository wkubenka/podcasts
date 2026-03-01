package com.astute.podcasts.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        // NoOpCacheEvictor: cached episodes persist until explicitly deleted
        // via deleteDownload() or onEpisodeFinished(). This guarantees that
        // resuming an episode days later still uses the original stream bytes,
        // avoiding the DAI position-mismatch problem.
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
    }
}
