package com.astute.podcasts.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.domain.model.DownloadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Named

@HiltWorker
class EpisodeDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val episodeDao: EpisodeDao,
    @param:Named("download") private val httpClient: OkHttpClient
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val episodeId = inputData.getLong(KEY_EPISODE_ID, -1L)
        if (episodeId == -1L) return@withContext Result.failure()

        val episode = episodeDao.getEpisodeById(episodeId)
            ?: return@withContext Result.failure()

        episodeDao.updateDownloadStatus(episodeId, DownloadStatus.DOWNLOADING.name)

        val downloadsDir = File(applicationContext.getExternalFilesDir(null), "downloads")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val outputFile = File(downloadsDir, "episode_$episodeId.mp3")

        try {
            val request = Request.Builder().url(episode.audioUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                episodeDao.updateDownloadStatus(episodeId, DownloadStatus.FAILED.name)
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                episodeDao.updateDownloadStatus(episodeId, DownloadStatus.FAILED.name)
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            var bytesDownloaded = 0L

            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) {
                            outputFile.delete()
                            episodeDao.updateDownloadStatus(episodeId, DownloadStatus.NOT_DOWNLOADED.name)
                            return@withContext Result.failure()
                        }
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        if (contentLength > 0) {
                            val progress = (bytesDownloaded * 100 / contentLength).toInt()
                            setProgress(workDataOf(
                                KEY_PROGRESS to progress,
                                KEY_EPISODE_ID to episodeId
                            ))
                        }
                    }
                }
            }

            episodeDao.updateDownloadStatus(
                episodeId,
                DownloadStatus.DOWNLOADED.name,
                outputFile.absolutePath
            )
            Result.success(workDataOf(KEY_FILE_PATH to outputFile.absolutePath))
        } catch (e: Exception) {
            outputFile.delete()
            if (!isStopped) {
                episodeDao.updateDownloadStatus(episodeId, DownloadStatus.FAILED.name)
            }
            Result.failure()
        }
    }

    companion object {
        const val KEY_EPISODE_ID = "episode_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
        const val TAG_ALL_DOWNLOADS = "episode_download"

        fun tagForEpisode(episodeId: Long) = "download_episode_$episodeId"
    }
}
