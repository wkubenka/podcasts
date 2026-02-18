package com.astutepodcasts.app.ui.podcastdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import com.astutepodcasts.app.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository
) : ViewModel() {

    private val podcastId: Long = savedStateHandle["podcastId"] ?: 0L

    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val podcastDeferred = async { podcastRepository.getPodcastById(podcastId) }
                val episodesDeferred = async { episodeRepository.getEpisodesForPodcast(podcastId) }

                val podcast = podcastDeferred.await()
                val episodes = episodesDeferred.await()

                _uiState.update {
                    it.copy(
                        podcast = podcast,
                        episodes = episodes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load podcast")
                }
            }
        }
    }
}
