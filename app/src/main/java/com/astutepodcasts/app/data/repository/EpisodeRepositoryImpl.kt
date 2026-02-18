package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.mapper.EpisodeIdGenerator
import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.remote.RssFeedParser
import com.astutepodcasts.app.data.remote.RssFeedService
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val podcastDao: PodcastDao,
    private val rssFeedService: RssFeedService,
    private val rssFeedParser: RssFeedParser,
    private val episodeIdGenerator: EpisodeIdGenerator
) : EpisodeRepository {

    override suspend fun getEpisodesForPodcast(feedId: Long): List<Episode> {
        val podcast = podcastDao.getById(feedId)
            ?: throw Exception("Podcast not found in database")
        val xml = rssFeedService.fetchFeed(podcast.feedUrl)
        val parsed = rssFeedParser.parse(xml, feedId, podcast.artworkUrl)
        return resolveEpisodeIds(feedId, parsed)
    }

    override suspend fun getLocalEpisodesForPodcast(podcastId: Long): List<Episode> {
        return episodeDao.getByPodcastId(podcastId).map { it.toDomain() }
    }

    override fun observeEpisodesForPodcast(podcastId: Long): Flow<List<Episode>> {
        return episodeDao.observeByPodcastId(podcastId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentlyPlayedEpisodes(): Flow<List<Episode>> {
        return episodeDao.getRecentlyPlayed().map { entities ->
            entities.map { it.toDomain() }
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
