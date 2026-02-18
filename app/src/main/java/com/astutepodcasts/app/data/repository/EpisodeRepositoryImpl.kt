package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.remote.PodcastIndexApi
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val api: PodcastIndexApi,
    private val episodeDao: EpisodeDao
) : EpisodeRepository {

    override suspend fun getEpisodesForPodcast(feedId: Long): List<Episode> {
        return api.getEpisodesByFeedId(feedId).items.map { it.toDomain(overridePodcastId = feedId) }
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
}
