package com.astutepodcasts.app.domain.repository

import com.astutepodcasts.app.domain.model.Podcast

interface PodcastRepository {
    suspend fun searchPodcasts(query: String): List<Podcast>
    suspend fun getTrendingPodcasts(): List<Podcast>
    suspend fun getPodcastById(id: Long): Podcast?
}
