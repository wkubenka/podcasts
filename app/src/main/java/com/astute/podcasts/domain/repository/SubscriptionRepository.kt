package com.astute.podcasts.domain.repository

import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.model.Podcast
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getSubscribedPodcasts(): Flow<List<Podcast>>
    fun getRecentEpisodes(): Flow<List<Episode>>
    fun isSubscribed(podcastId: Long): Flow<Boolean>
    suspend fun subscribe(podcast: Podcast, episodes: List<Episode>)
    suspend fun unsubscribe(podcastId: Long)
    suspend fun refreshFeeds()
}
