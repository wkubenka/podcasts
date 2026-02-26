package com.astute.podcasts.data.repository

import android.content.Context
import com.astute.podcasts.data.local.dao.PodcastDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ArtworkCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("download") private val httpClient: OkHttpClient,
    private val podcastDao: PodcastDao
) {

    private val artworkDir: File
        get() = File(context.filesDir, "artwork").also { if (!it.exists()) it.mkdirs() }

    suspend fun cacheArtwork(podcastId: Long, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val file = File(artworkDir, "podcast_$podcastId.jpg")
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            podcastDao.updateArtworkCache(podcastId, path, System.currentTimeMillis())
            path
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshStaleArtwork() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val staleThreshold = System.currentTimeMillis() - thirtyDaysMs
        val stale = podcastDao.getSubscribedPodcastsNeedingArtworkRefresh(staleThreshold)
        for (info in stale) {
            val url = info.artworkUrl ?: continue
            cacheArtwork(info.id, url)
        }
    }

    fun deleteArtwork(podcastId: Long) {
        File(artworkDir, "podcast_$podcastId.jpg").delete()
    }
}
