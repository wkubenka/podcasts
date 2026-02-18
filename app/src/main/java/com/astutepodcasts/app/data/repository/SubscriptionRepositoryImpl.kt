package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.dao.SubscriptionDao
import com.astutepodcasts.app.data.local.entity.SubscriptionEntity
import com.astutepodcasts.app.data.mapper.EpisodeIdGenerator
import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.mapper.toEntity
import com.astutepodcasts.app.data.remote.RssFeedParser
import com.astutepodcasts.app.data.remote.RssFeedService
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
    private val rssFeedService: RssFeedService,
    private val rssFeedParser: RssFeedParser,
    private val episodeIdGenerator: EpisodeIdGenerator
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
        val feedInfos = podcastDao.getSubscribedPodcastFeedInfos()
        var failureCount = 0
        for (feedInfo in feedInfos) {
            try {
                val xml = rssFeedService.fetchFeed(feedInfo.feedUrl)
                val podcast = podcastDao.getById(feedInfo.id)
                val parsed = rssFeedParser.parse(xml, feedInfo.id, podcast?.artworkUrl)
                val resolved = resolveEpisodeIds(feedInfo.id, parsed)
                episodeDao.upsertAllPreservingDownloadStatus(resolved.map { it.toEntity() })
            } catch (_: Exception) {
                failureCount++
            }
        }
        if (failureCount > 0 && failureCount == feedInfos.size) {
            throw Exception("Failed to refresh feeds. Check your connection.")
        }
    }

    private suspend fun resolveEpisodeIds(podcastId: Long, episodes: List<Episode>): List<Episode> {
        val existing = episodeDao.getEpisodeIdsByAudioUrl(podcastId)
        val urlToId = existing.associate { it.audioUrl to it.id }
        return episodes.map { episode ->
            val existingId = urlToId[episode.audioUrl]
            episode.copy(id = existingId ?: episodeIdGenerator.generateId(episode.audioUrl))
        }
    }
}
