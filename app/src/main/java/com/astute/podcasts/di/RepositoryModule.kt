package com.astute.podcasts.di

import com.astute.podcasts.data.repository.DownloadRepositoryImpl
import com.astute.podcasts.data.repository.EpisodeRepositoryImpl
import com.astute.podcasts.data.repository.PodcastRepositoryImpl
import com.astute.podcasts.data.repository.SubscriptionRepositoryImpl
import com.astute.podcasts.domain.repository.DownloadRepository
import com.astute.podcasts.domain.repository.EpisodeRepository
import com.astute.podcasts.domain.repository.PodcastRepository
import com.astute.podcasts.domain.repository.SubscriptionRepository
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
