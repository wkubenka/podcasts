package com.astute.podcasts.playback

import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.data.local.dao.PodcastDao
import com.astute.podcasts.data.local.entity.EpisodeEntity
import com.astute.podcasts.data.mapper.toDomain
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class MediaBrowseTree @Inject constructor(
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao
) {

    fun getRootItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Astute Podcasts")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
    }

    fun getRootChildren(): List<MediaItem> {
        return listOf(
            browsableFolder(SUBSCRIPTIONS_ID, "Subscriptions", MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS),
            browsableFolder(RECENT_ID, "Recent Episodes", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            browsableFolder(DOWNLOADS_ID, "Downloads", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            browsableFolder(CONTINUE_LISTENING_ID, "Continue Listening", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        )
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        return when {
            parentId == ROOT_ID -> getRootChildren()
            parentId == SUBSCRIPTIONS_ID -> getSubscribedPodcasts()
            parentId == RECENT_ID -> getRecentEpisodes()
            parentId == DOWNLOADS_ID -> getDownloadedEpisodes()
            parentId == CONTINUE_LISTENING_ID -> getContinueListening()
            parentId.startsWith(PODCAST_PREFIX) -> {
                val podcastId = parentId.removePrefix(PODCAST_PREFIX).toLongOrNull()
                if (podcastId != null) getPodcastEpisodes(podcastId) else emptyList()
            }
            else -> emptyList()
        }
    }

    suspend fun resolvePlayableItem(item: MediaItem): MediaItem? {
        val mediaId = item.mediaId
        if (mediaId.startsWith(EPISODE_PREFIX)) {
            val episodeId = mediaId.removePrefix(EPISODE_PREFIX).toLongOrNull() ?: return null
            val entity = episodeDao.getEpisodeById(episodeId) ?: return null
            return EpisodeMediaItemMapper.toMediaItem(entity.toDomain())
        }
        return item
    }

    private suspend fun getSubscribedPodcasts(): List<MediaItem> {
        return podcastDao.getSubscribedPodcasts().first().map { podcast ->
            MediaItem.Builder()
                .setMediaId("$PODCAST_PREFIX${podcast.id}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(podcast.title)
                        .setArtist(podcast.author)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST)
                        .apply {
                            podcast.artworkUrl?.let { setArtworkUri(Uri.parse(it)) }
                        }
                        .build()
                )
                .build()
        }
    }

    private suspend fun getRecentEpisodes(): List<MediaItem> {
        return episodeDao.getRecentSubscribedEpisodes(50).first().map { it.toBrowseMediaItem() }
    }

    private suspend fun getDownloadedEpisodes(): List<MediaItem> {
        return episodeDao.getDownloadedEpisodes().first().map { it.toBrowseMediaItem() }
    }

    private suspend fun getContinueListening(): List<MediaItem> {
        return episodeDao.getRecentlyPlayed().first().map { it.toBrowseMediaItem() }
    }

    private suspend fun getPodcastEpisodes(podcastId: Long): List<MediaItem> {
        return episodeDao.getByPodcastId(podcastId).map { it.toBrowseMediaItem() }
    }

    private fun EpisodeEntity.toBrowseMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId("$EPISODE_PREFIX$id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDescription(description)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                    .setExtras(Bundle().apply {
                        putInt("duration_seconds", durationSeconds)
                    })
                    .apply {
                        artworkUrl?.let { setArtworkUri(Uri.parse(it)) }
                    }
                    .build()
            )
            .build()
    }

    private fun browsableFolder(mediaId: String, title: String, mediaType: Int): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(mediaType)
                    .build()
            )
            .build()
    }

    companion object {
        const val ROOT_ID = "root"
        const val SUBSCRIPTIONS_ID = "subscriptions"
        const val RECENT_ID = "recent"
        const val DOWNLOADS_ID = "downloads"
        const val CONTINUE_LISTENING_ID = "continue_listening"
        const val PODCAST_PREFIX = "podcast:"
        const val EPISODE_PREFIX = "episode:"
    }
}
