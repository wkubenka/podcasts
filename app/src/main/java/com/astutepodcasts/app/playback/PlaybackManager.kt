package com.astutepodcasts.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import com.astutepodcasts.app.data.local.PlaybackPreferences
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.PlaybackState
import com.astutepodcasts.app.domain.repository.DownloadRepository
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
    private val downloadRepository: DownloadRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentEpisode: Episode? = null
    private var hasAttachedListener = false
    private var downloadObserverJob: Job? = null

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
            controller.setMediaItem(mediaItem)
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
                    val controller = connection.controller.value ?: return@first false
                    // Only swap if we're still playing this episode
                    if (currentEpisode?.id != episode.id) return@first true

                    val positionMs = controller.currentPosition
                    val updatedEpisode = episode.copy(
                        localFilePath = filePath,
                        downloadStatus = DownloadStatus.DOWNLOADED
                    )
                    val localMediaItem = EpisodeMediaItemMapper.toMediaItem(updatedEpisode)
                    controller.setMediaItem(localMediaItem, positionMs)
                    controller.prepare()
                    controller.play()

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

    private fun attachPlayerListener(controller: Player) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playbackState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = controller.duration.coerceAtLeast(0)
                    )
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
