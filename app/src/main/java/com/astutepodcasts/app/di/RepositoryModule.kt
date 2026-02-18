package com.astutepodcasts.app.di

import com.astutepodcasts.app.data.repository.DownloadRepositoryImpl
import com.astutepodcasts.app.data.repository.EpisodeRepositoryImpl
import com.astutepodcasts.app.data.repository.PodcastRepositoryImpl
import com.astutepodcasts.app.data.repository.SubscriptionRepositoryImpl
import com.astutepodcasts.app.domain.repository.DownloadRepository
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import com.astutepodcasts.app.domain.repository.PodcastRepository
import com.astutepodcasts.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindPodcastRepository(impl: PodcastRepositoryImpl): PodcastRepository

    @Binds
    abstract fun bindEpisodeRepository(impl: EpisodeRepositoryImpl): EpisodeRepository

    @Binds
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository
}
