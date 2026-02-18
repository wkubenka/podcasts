package com.astutepodcasts.app.ui.podcastdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import com.astutepodcasts.app.domain.repository.PodcastRepository
import com.astutepodcasts.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.astutepodcasts.app.ui.toUserMessage

data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val isSubscribed: Boolean = false,
    val error: String? = null,
    val showArchived: Boolean = false
) {
    val filteredEpisodes: List<Episode>
        get() = if (showArchived) episodes else episodes.filter { !it.isArchived }
}

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val episodeRepository: EpisodeRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val podcastId: Long = savedStateHandle["podcastId"] ?: 0L

    private val _uiState = MutableStateFlow(PodcastDetailUiState())
    val uiState: StateFlow<PodcastDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        subscriptionRepository.isSubscribed(podcastId)
            .onEach { subscribed ->
                _uiState.update { it.copy(isSubscribed = subscribed) }
            }
            .launchIn(viewModelScope)

        // Observe Room episodes reactively so download status changes are reflected
        episodeRepository.observeEpisodesForPodcast(podcastId)
            .onEach { roomEpisodes ->
                if (roomEpisodes.isEmpty()) return@onEach
                _uiState.update { state ->
                    if (state.episodes.isEmpty()) {
                        // Initial load hasn't completed yet, use Room data directly
                        state.copy(episodes = roomEpisodes)
                    } else {
                        // Merge download statuses from Room into current episodes
                        val statusMap = roomEpisodes.associate {
                            it.id to it
                        }
                        val updated = state.episodes.map { episode ->
                            statusMap[episode.id]?.let { roomEp ->
                                episode.copy(
                                    downloadStatus = roomEp.downloadStatus,
                                    localFilePath = roomEp.localFilePath,
                                    isArchived = roomEp.isArchived
                                )
                            } ?: episode
                        }
                        state.copy(episodes = updated)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun retry() {
        load()
    }

    fun toggleShowArchived() {
        _uiState.update { it.copy(showArchived = !it.showArchived) }
    }

    fun archiveEpisode(episodeId: Long) {
        viewModelScope.launch {
            episodeRepository.setArchived(episodeId, true)
        }
    }

    fun unarchiveEpisode(episodeId: Long) {
        viewModelScope.launch {
            episodeRepository.setArchived(episodeId, false)
        }
    }

    fun toggleSubscription() {
        val state = _uiState.value
        val podcast = state.podcast ?: return
        viewModelScope.launch {
            if (state.isSubscribed) {
                subscriptionRepository.unsubscribe(podcastId)
            } else {
                subscriptionRepository.subscribe(podcast, state.episodes)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val podcastDeferred = async { podcastRepository.getPodcastById(podcastId) }
                val episodesDeferred = async { episodeRepository.getEpisodesForPodcast(podcastId) }
                val localEpisodesDeferred = async { episodeRepository.getLocalEpisodesForPodcast(podcastId) }

                val podcast = podcastDeferred.await()
                val apiEpisodes = episodesDeferred.await()
                val localEpisodes = localEpisodesDeferred.await()

                // Merge: use API episode data but preserve download status from Room
                val localStatusMap = localEpisodes.associate { it.id to it }
                val mergedEpisodes = apiEpisodes.map { episode ->
                    localStatusMap[episode.id]?.let { local ->
                        episode.copy(
                            downloadStatus = local.downloadStatus,
                            localFilePath = local.localFilePath,
                            isArchived = local.isArchived
                        )
                    } ?: episode
                }

                _uiState.update {
                    it.copy(
                        podcast = podcast,
                        episodes = mergedEpisodes,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.toUserMessage())
                }
            }
        }
    }
}
