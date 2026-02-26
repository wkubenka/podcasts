package com.astute.podcasts.ui.nowplaying

import androidx.lifecycle.ViewModel
import com.astute.podcasts.domain.model.PlaybackState
import com.astute.podcasts.playback.PlaybackManager
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
