package com.astutepodcasts.app.ui

import androidx.lifecycle.ViewModel
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.PlaybackState
import com.astutepodcasts.app.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    fun play(episode: Episode) = playbackManager.play(episode)

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun skipBackward() = playbackManager.skipBackward()

    fun skipForward() = playbackManager.skipForward()
}
