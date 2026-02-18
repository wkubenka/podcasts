package com.astutepodcasts.app.domain.repository

import com.astutepodcasts.app.domain.model.Episode
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun downloadEpisode(episode: Episode)
    suspend fun cancelDownload(episodeId: Long)
    suspend fun deleteDownload(episodeId: Long)
    fun getDownloadedEpisodes(): Flow<List<Episode>>
    fun getActiveDownloadProgress(): Flow<Map<Long, Int>>
    fun observeEpisodeDownloaded(episodeId: Long): Flow<String?>
}
