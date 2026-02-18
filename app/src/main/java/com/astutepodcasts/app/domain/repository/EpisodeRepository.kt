package com.astutepodcasts.app.domain.repository

import com.astutepodcasts.app.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {
    suspend fun getEpisodesForPodcast(feedId: Long): List<Episode>
    suspend fun getLocalEpisodesForPodcast(podcastId: Long): List<Episode>
    fun observeEpisodesForPodcast(podcastId: Long): Flow<List<Episode>>
}
