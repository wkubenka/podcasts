package com.astutepodcasts.app.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.astutepodcasts.app.domain.model.Episode

object EpisodeMediaItemMapper {

    private const val EXTRA_EPISODE_ID = "episode_id"
    private const val EXTRA_PODCAST_ID = "podcast_id"
    private const val EXTRA_ARTWORK_URL = "artwork_url"
    private const val EXTRA_DURATION_SECONDS = "duration_seconds"

    fun toMediaItem(episode: Episode): MediaItem {
        val extras = Bundle().apply {
            putLong(EXTRA_EPISODE_ID, episode.id)
            putLong(EXTRA_PODCAST_ID, episode.podcastId)
            episode.artworkUrl?.let { putString(EXTRA_ARTWORK_URL, it) }
            putInt(EXTRA_DURATION_SECONDS, episode.durationSeconds)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(episode.title)
            .setDescription(episode.description)
            .apply {
                episode.artworkUrl?.let { setArtworkUri(Uri.parse(it)) }
            }
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(episode.id.toString())
            .setUri(episode.audioUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    fun getEpisodeId(mediaItem: MediaItem): Long {
        return mediaItem.mediaMetadata.extras?.getLong(EXTRA_EPISODE_ID) ?: 0L
    }

    fun getArtworkUrl(mediaItem: MediaItem): String? {
        return mediaItem.mediaMetadata.extras?.getString(EXTRA_ARTWORK_URL)
    }
}
