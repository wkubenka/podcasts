package com.astutepodcasts.app.ui.nowplaying

import androidx.lifecycle.ViewModel
import com.astutepodcasts.app.domain.model.PlaybackState
import com.astutepodcasts.app.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)

    fun skipBackward() = playbackManager.skipBackward()

    fun skipForward() = playbackManager.skipForward()

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)
}
