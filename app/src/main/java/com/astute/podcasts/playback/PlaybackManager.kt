package com.astute.podcasts.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.astute.podcasts.data.local.PlaybackPreferences
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.domain.model.DownloadStatus
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.model.PlaybackState
import com.astute.podcasts.domain.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackManager @Inject constructor(
    private val connection: PlaybackServiceConnection,
    private val playbackPreferences: PlaybackPreferences,
    private val downloadRepository: DownloadRepository,
    private val episodeDao: EpisodeDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentEpisode: Episode? = null
    private var hasAttachedListener = false
    private var downloadObserverJob: Job? = null
    private var pollCount = 0

    init {
        scope.launch {
            val savedSpeed = playbackPreferences.playbackSpeed.first()
            _playbackState.update { it.copy(playbackSpeed = savedSpeed) }
        }

        scope.launch {
            connection.controller.collect { controller ->
                if (controller != null && !hasAttachedListener) {
                    attachPlayerListener(controller)
                    hasAttachedListener = true
                    startPositionPolling()
                }
            }
        }
    }

    fun play(episode: Episode) {
        // Save current episode's position before switching
        saveCurrentPosition()

        downloadObserverJob?.cancel()
        currentEpisode = episode
        _playbackState.update {
            it.copy(
                currentEpisode = episode,
                isBuffering = true
            )
        }

        connection.connect()

        scope.launch {
            val controller = awaitController()
            val mediaItem = EpisodeMediaItemMapper.toMediaItem(episode)
            val startPositionMs = episode.lastPlayedPositionMs.coerceAtLeast(0)
            controller.setMediaItem(mediaItem, startPositionMs)
            controller.playbackParameters = PlaybackParameters(_playbackState.value.playbackSpeed)
            controller.prepare()
            controller.play()

            when (episode.downloadStatus) {
                DownloadStatus.NOT_DOWNLOADED, DownloadStatus.FAILED -> {
                    downloadRepository.downloadEpisode(episode)
                    observeDownloadCompletion(episode)
                }
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                    observeDownloadCompletion(episode)
                }
                DownloadStatus.DOWNLOADED -> { /* already local, nothing to do */ }
            }
        }
    }

    private fun observeDownloadCompletion(episode: Episode) {
        downloadObserverJob = scope.launch {
            downloadRepository.observeEpisodeDownloaded(episode.id)
                .filterNotNull()
                .first { filePath ->
                    // Only update metadata if we're still playing this episode
                    if (currentEpisode?.id != episode.id) return@first true

                    // Update episode metadata so the UI reflects the download,
                    // but do NOT swap the media source mid-playback. Dynamic ad
                    // insertion can cause the streamed and downloaded versions to
                    // differ, so seeking to the same timestamp in the downloaded
                    // file may land on different content. The local file will be
                    // used automatically the next time this episode is played.
                    val updatedEpisode = episode.copy(
                        localFilePath = filePath,
                        downloadStatus = DownloadStatus.DOWNLOADED
                    )

                    currentEpisode = updatedEpisode
                    _playbackState.update { it.copy(currentEpisode = updatedEpisode) }
                    true
                }
        }
    }

    fun togglePlayPause() {
        scope.launch {
            val controller = connection.controller.value ?: return@launch
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            val controller = connection.controller.value ?: return@launch
            controller.seekTo(positionMs)
        }
    }

    fun skipBackward() {
        scope.launch {
            val controller = connection.controller.value ?: return@launch
            val newPosition = (controller.currentPosition - 10_000).coerceAtLeast(0)
            controller.seekTo(newPosition)
        }
    }

    fun skipForward() {
        scope.launch {
            val controller = connection.controller.value ?: return@launch
            val newPosition = (controller.currentPosition + 30_000)
                .coerceAtMost(controller.duration.coerceAtLeast(0))
            controller.seekTo(newPosition)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.update { it.copy(playbackSpeed = speed) }
        scope.launch {
            val controller = connection.controller.value
            controller?.playbackParameters = PlaybackParameters(speed)
            playbackPreferences.setPlaybackSpeed(speed)
        }
    }

    private fun saveCurrentPosition() {
        val episode = currentEpisode ?: return
        val controller = connection.controller.value ?: return
        val positionMs = controller.currentPosition.coerceAtLeast(0)
        if (positionMs > 0) {
            scope.launch(Dispatchers.IO) {
                episodeDao.updatePlaybackPosition(
                    episodeId = episode.id,
                    positionMs = positionMs,
                    lastPlayedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private fun attachPlayerListener(controller: Player) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (!isPlaying) {
                    saveCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = controller.duration.coerceAtLeast(0)
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    onEpisodeFinished()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem == null) {
                    _playbackState.update {
                        it.copy(currentEpisode = null, isPlaying = false)
                    }
                }
            }
        })
    }

    private fun onEpisodeFinished() {
        val episode = currentEpisode ?: return
        downloadObserverJob?.cancel()

        // Delete downloaded file if present
        if (episode.downloadStatus == DownloadStatus.DOWNLOADED ||
            episode.downloadStatus == DownloadStatus.DOWNLOADING ||
            episode.downloadStatus == DownloadStatus.QUEUED
        ) {
            scope.launch(Dispatchers.IO) {
                downloadRepository.deleteDownload(episode.id)
            }
        }

        // Clear saved playback position since episode is finished
        scope.launch(Dispatchers.IO) {
            episodeDao.updatePlaybackPosition(
                episodeId = episode.id,
                positionMs = 0,
                lastPlayedAt = 0
            )
            // Auto-archive finished episode
            episodeDao.updateArchived(episode.id, true)
        }

        // Stop playback and clear state
        scope.launch {
            val controller = connection.controller.value
            controller?.stop()
            controller?.clearMediaItems()
        }
        currentEpisode = null
        _playbackState.update {
            PlaybackState(playbackSpeed = it.playbackSpeed)
        }
    }

    private fun startPositionPolling() {
        scope.launch {
            while (true) {
                val controller = connection.controller.value
                if (controller != null) {
                    _playbackState.update {
                        it.copy(
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0),
                            durationMs = controller.duration.coerceAtLeast(0)
                        )
                    }
                    // Save position every ~10s (20 polls at 500ms each)
                    pollCount++
                    if (pollCount >= 20 && controller.isPlaying) {
                        pollCount = 0
                        saveCurrentPosition()
                    }
                }
                delay(500)
            }
        }
    }

    private suspend fun awaitController(): Player {
        connection.connect()
        var controller = connection.controller.value
        while (controller == null) {
            delay(50)
            controller = connection.controller.value
        }
        return controller
    }
}
