package com.astute.podcasts.data.repository

import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.data.local.dao.PodcastDao
import com.astute.podcasts.data.mapper.EpisodeIdGenerator
import com.astute.podcasts.data.mapper.toDomain
import com.astute.podcasts.data.remote.RssFeedParser
import com.astute.podcasts.data.remote.RssFeedService
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.repository.DownloadRepository
import com.astute.podcasts.domain.repository.EpisodeRepository
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
    private val episodeIdGenerator: EpisodeIdGenerator,
    private val downloadRepository: DownloadRepository
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

    override suspend fun setArchived(episodeId: Long, archived: Boolean) {
        episodeDao.updateArchived(episodeId, archived)
        if (archived) {
            downloadRepository.deleteDownload(episodeId)
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
