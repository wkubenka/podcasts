package com.astutepodcasts.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Podcast> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isShowingTrending: Boolean = true
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        loadTrending()

        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        loadTrending()
                    } else {
                        search(query)
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    private fun loadTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val trending = podcastRepository.getTrendingPodcasts()
                _uiState.update {
                    it.copy(
                        results = trending,
                        isLoading = false,
                        isShowingTrending = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load trending")
                }
            }
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val results = podcastRepository.searchPodcasts(query)
            _uiState.update {
                it.copy(
                    results = results,
                    isLoading = false,
                    isShowingTrending = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = e.message ?: "Search failed")
            }
        }
    }
}
