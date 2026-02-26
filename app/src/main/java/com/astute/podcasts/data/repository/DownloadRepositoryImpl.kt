package com.astute.podcasts.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.data.mapper.toDomain
import com.astute.podcasts.data.worker.EpisodeDownloadWorker
import com.astute.podcasts.domain.model.DownloadStatus
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val workManager: WorkManager,
    private val episodeDao: EpisodeDao
) : DownloadRepository {

    override suspend fun downloadEpisode(episode: Episode) {
        episodeDao.updateDownloadStatus(episode.id, DownloadStatus.QUEUED.name)

        val tag = EpisodeDownloadWorker.tagForEpisode(episode.id)
        val workRequest = OneTimeWorkRequestBuilder<EpisodeDownloadWorker>()
            .setInputData(workDataOf(EpisodeDownloadWorker.KEY_EPISODE_ID to episode.id))
            .addTag(tag)
            .addTag(EpisodeDownloadWorker.TAG_ALL_DOWNLOADS)
            .build()

        workManager.enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, workRequest)
    }

    override suspend fun cancelDownload(episodeId: Long) {
        val tag = EpisodeDownloadWorker.tagForEpisode(episodeId)
        workManager.cancelUniqueWork(tag)

        val episode = episodeDao.getEpisodeById(episodeId)
        episode?.localFilePath?.let { path ->
            File(path).delete()
        }
        episodeDao.updateDownloadStatus(episodeId, DownloadStatus.NOT_DOWNLOADED.name, null)
    }

    override suspend fun deleteDownload(episodeId: Long) {
        val episode = episodeDao.getEpisodeById(episodeId)
        episode?.localFilePath?.let { path ->
            File(path).delete()
        }
        episodeDao.updateDownloadStatus(episodeId, DownloadStatus.NOT_DOWNLOADED.name, null)
    }

    override fun getDownloadedEpisodes(): Flow<List<Episode>> =
        episodeDao.getDownloadedEpisodes().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeEpisodeDownloaded(episodeId: Long): Flow<String?> =
        episodeDao.observeById(episodeId).map { entity ->
            entity?.localFilePath?.takeIf { entity.downloadStatus == DownloadStatus.DOWNLOADED.name }
        }

    override fun getActiveDownloadProgress(): Flow<Map<Long, Int>> {
        return workManager.getWorkInfosByTagFlow(EpisodeDownloadWorker.TAG_ALL_DOWNLOADS)
            .map { workInfos ->
                workInfos
                    .filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .mapNotNull { workInfo ->
                        val episodeId = workInfo.progress.getLong(EpisodeDownloadWorker.KEY_EPISODE_ID, -1L)
                            .takeIf { it != -1L }
                            ?: workInfo.tags
                                .firstOrNull { it.startsWith("download_episode_") }
                                ?.removePrefix("download_episode_")
                                ?.toLongOrNull()
                            ?: return@mapNotNull null
                        val progress = if (workInfo.state == WorkInfo.State.RUNNING) {
                            workInfo.progress.getInt(EpisodeDownloadWorker.KEY_PROGRESS, 0)
                        } else {
                            0
                        }
                        episodeId to progress
                    }
                    .toMap()
            }
    }
}
