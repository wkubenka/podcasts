package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.remote.PodcastIndexApi
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val api: PodcastIndexApi
) : EpisodeRepository {

    override suspend fun getEpisodesForPodcast(feedId: Long): List<Episode> {
        return api.getEpisodesByFeedId(feedId).items.map { it.toDomain(overridePodcastId = feedId) }
    }
}
