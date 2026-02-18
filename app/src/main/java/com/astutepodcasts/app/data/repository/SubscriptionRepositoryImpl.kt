package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.dao.SubscriptionDao
import com.astutepodcasts.app.data.local.entity.SubscriptionEntity
import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.mapper.toEntity
import com.astutepodcasts.app.data.remote.PodcastIndexApi
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
    private val subscriptionDao: SubscriptionDao,
    private val api: PodcastIndexApi
) : SubscriptionRepository {

    override fun getSubscribedPodcasts(): Flow<List<Podcast>> {
        return podcastDao.getSubscribedPodcasts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentEpisodes(): Flow<List<Episode>> {
        return episodeDao.getRecentSubscribedEpisodes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun isSubscribed(podcastId: Long): Flow<Boolean> {
        return subscriptionDao.isSubscribed(podcastId)
    }

    override suspend fun subscribe(podcast: Podcast, episodes: List<Episode>) {
        podcastDao.insert(podcast.toEntity())
        episodeDao.upsertAllPreservingDownloadStatus(episodes.map { it.toEntity() })
        subscriptionDao.insert(
            SubscriptionEntity(
                podcastId = podcast.id,
                subscribedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun unsubscribe(podcastId: Long) {
        subscriptionDao.deleteByPodcastId(podcastId)
    }

    override suspend fun refreshFeeds() {
        val podcastIds = subscriptionDao.getAllSubscribedPodcastIds()
        for (podcastId in podcastIds) {
            try {
                val episodes = api.getEpisodesByFeedId(podcastId).items
                    .map { it.toDomain(overridePodcastId = podcastId) }
                episodeDao.upsertAllPreservingDownloadStatus(episodes.map { it.toEntity() })
            } catch (_: Exception) {
                // Skip failed feeds silently
            }
        }
    }
}
