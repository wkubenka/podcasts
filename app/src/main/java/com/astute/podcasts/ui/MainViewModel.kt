package com.astute.podcasts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.model.PlaybackState
import com.astute.podcasts.domain.repository.DownloadRepository
import com.astute.podcasts.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val downloadProgress: StateFlow<Map<Long, Int>> = downloadRepository.getActiveDownloadProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun play(episode: Episode) = playbackManager.play(episode)

    fun togglePlayPause() = playbackManager.togglePlayPause()

    fun skipBackward() = playbackManager.skipBackward()

    fun skipForward() = playbackManager.skipForward()

    fun downloadEpisode(episode: Episode) {
        viewModelScope.launch {
            downloadRepository.downloadEpisode(episode)
        }
    }

    fun cancelDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(episodeId)
        }
    }

    fun deleteDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(episodeId)
        }
    }
}
