package com.astute.podcasts.domain.repository

import com.astute.podcasts.domain.model.Podcast

interface PodcastRepository {
    suspend fun searchPodcasts(query: String): List<Podcast>
    suspend fun getTrendingPodcasts(): List<Podcast>
    suspend fun getPodcastById(id: Long): Podcast?
}
