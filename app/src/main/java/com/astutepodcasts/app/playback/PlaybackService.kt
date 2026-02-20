package com.astutepodcasts.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.astutepodcasts.app.MainActivity
import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.data.local.dao.PodcastDao
import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import com.astutepodcasts.app.data.mapper.toDomain
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var podcastDao: PodcastDao
    @Inject lateinit var episodeDao: EpisodeDao

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).putExtra(EXTRA_OPEN_NOW_PLAYING, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        mediaLibrarySession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
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
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceFuture {
                val children = when {
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
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            return serviceFuture {
                mediaItems.mapNotNull { item -> resolvePlayableItem(item) }
            }
        }
    }

    private suspend fun resolvePlayableItem(item: MediaItem): MediaItem? {
        val mediaId = item.mediaId
        if (mediaId.startsWith(EPISODE_PREFIX)) {
            val episodeId = mediaId.removePrefix(EPISODE_PREFIX).toLongOrNull() ?: return null
            val entity = episodeDao.getEpisodeById(episodeId) ?: return null
            return EpisodeMediaItemMapper.toMediaItem(entity.toDomain())
        }
        return item
    }

    private fun getRootChildren(): List<MediaItem> {
        return listOf(
            browsableFolder(SUBSCRIPTIONS_ID, "Subscriptions", MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS),
            browsableFolder(RECENT_ID, "Recent Episodes", MediaMetadata.MEDIA_TYPE_FOLDER_EPISODES),
            browsableFolder(DOWNLOADS_ID, "Downloads", MediaMetadata.MEDIA_TYPE_FOLDER_EPISODES),
            browsableFolder(CONTINUE_LISTENING_ID, "Continue Listening", MediaMetadata.MEDIA_TYPE_FOLDER_EPISODES),
        )
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

    private fun <T> serviceFuture(block: suspend () -> T): ListenableFuture<T> {
        val future = SettableFuture.create<T>()
        serviceScope.launch {
            try {
                future.set(block())
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "open_now_playing"
        private const val ROOT_ID = "root"
        private const val SUBSCRIPTIONS_ID = "subscriptions"
        private const val RECENT_ID = "recent"
        private const val DOWNLOADS_ID = "downloads"
        private const val CONTINUE_LISTENING_ID = "continue_listening"
        private const val PODCAST_PREFIX = "podcast:"
        private const val EPISODE_PREFIX = "episode:"
    }
}
