package com.astute.podcasts.domain.repository

import com.astute.podcasts.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {
    suspend fun getEpisodesForPodcast(feedId: Long): List<Episode>
    suspend fun getLocalEpisodesForPodcast(podcastId: Long): List<Episode>
    fun observeEpisodesForPodcast(podcastId: Long): Flow<List<Episode>>
    fun getRecentlyPlayedEpisodes(): Flow<List<Episode>>
    suspend fun setArchived(episodeId: Long, archived: Boolean)
}
