package com.astutepodcasts.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val episodes: List<Episode> = emptyList(),
    val progressMap: Map<Long, Int> = emptyMap()
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadRepository.getDownloadedEpisodes(),
        downloadRepository.getActiveDownloadProgress()
    ) { episodes, progressMap ->
        DownloadsUiState(
            episodes = episodes,
            progressMap = progressMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsUiState())

    fun deleteDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(episodeId)
        }
    }

    fun cancelDownload(episodeId: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(episodeId)
        }
    }
}
