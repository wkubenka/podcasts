package com.astute.podcasts.ui.podcastdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.model.Podcast
import com.astute.podcasts.domain.repository.EpisodeRepository
import com.astute.podcasts.domain.repository.PodcastRepository
import com.astute.podcasts.domain.repository.SubscriptionRepository
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
import com.astute.podcasts.ui.toUserMessage

enum class EpisodeSortOrder { NEWEST_FIRST, OLDEST_FIRST }

data class PodcastDetailUiState(
    val podcast: Podcast? = null,
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val isSubscribed: Boolean = false,
    val error: String? = null,
    val showArchived: Boolean = false,
    val sortOrder: EpisodeSortOrder = EpisodeSortOrder.NEWEST_FIRST
) {
    val filteredEpisodes: List<Episode>
        get() {
            val visible = if (showArchived) episodes else episodes.filter { !it.isArchived }
            return when (sortOrder) {
                EpisodeSortOrder.NEWEST_FIRST -> visible.sortedByDescending { it.publishedAt }
                EpisodeSortOrder.OLDEST_FIRST -> visible.sortedBy { it.publishedAt }
            }
        }
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
                                roomEp.withFeedMetadataFrom(episode)
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

    fun toggleSortOrder() {
        _uiState.update {
            it.copy(
                sortOrder = when (it.sortOrder) {
                    EpisodeSortOrder.NEWEST_FIRST -> EpisodeSortOrder.OLDEST_FIRST
                    EpisodeSortOrder.OLDEST_FIRST -> EpisodeSortOrder.NEWEST_FIRST
                }
            )
        }
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

    /**
     * Overlays RSS feed metadata onto this episode, preserving all local-only
     * state (download status, playback progress, archive flag, etc.).
     */
    private fun Episode.withFeedMetadataFrom(api: Episode): Episode = copy(
        title = api.title,
        description = api.description,
        audioUrl = api.audioUrl,
        artworkUrl = api.artworkUrl,
        publishedAt = api.publishedAt,
        durationSeconds = api.durationSeconds,
        fileSize = api.fileSize,
        episodeNumber = api.episodeNumber,
        seasonNumber = api.seasonNumber
    )

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

                // Merge: start from local episode (preserving all local state)
                // and overlay fresh feed metadata from the API
                val localStatusMap = localEpisodes.associate { it.id to it }
                val mergedEpisodes = apiEpisodes.map { episode ->
                    localStatusMap[episode.id]?.let { local ->
                        local.withFeedMetadataFrom(episode)
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
