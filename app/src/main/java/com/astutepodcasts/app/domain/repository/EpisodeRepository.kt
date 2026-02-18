package com.astutepodcasts.app.domain.repository

import com.astutepodcasts.app.domain.model.Episode

interface EpisodeRepository {
    suspend fun getEpisodesForPodcast(feedId: Long): List<Episode>
}
