package com.astutepodcasts.app.data.repository

import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.mapper.toDomain
import com.astutepodcasts.app.data.mapper.toEntity
import com.astutepodcasts.app.data.remote.PodcastIndexApi
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.PodcastRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastRepositoryImpl @Inject constructor(
    private val api: PodcastIndexApi,
    private val podcastDao: PodcastDao
) : PodcastRepository {

    private val cache = mutableMapOf<Long, Podcast>()

    override suspend fun searchPodcasts(query: String): List<Podcast> {
        val podcasts = api.searchByTerm(query).feeds.map { it.toDomain() }
        podcasts.forEach { cache[it.id] = it }
        podcastDao.insertAll(podcasts.map { it.toEntity() })
        return podcasts
    }

    override suspend fun getTrendingPodcasts(): List<Podcast> {
        val podcasts = api.getTrending().feeds.map { it.toDomain() }.distinctBy { it.id }
        podcasts.forEach { cache[it.id] = it }
        podcastDao.insertAll(podcasts.map { it.toEntity() })
        return podcasts
    }

    override suspend fun getPodcastById(id: Long): Podcast? {
        return cache[id] ?: podcastDao.getById(id)?.toDomain()
    }
}
