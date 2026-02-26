package com.astute.podcasts.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astute.podcasts.domain.model.Episode
import com.astute.podcasts.domain.model.Podcast
import com.astute.podcasts.domain.repository.EpisodeRepository
import com.astute.podcasts.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.astute.podcasts.ui.toUserMessage

enum class PodcastSortOrder {
    RECENT_EPISODES,
    ALPHABETICAL
}

data class HomeUiState(
    val subscribedPodcasts: List<Podcast> = emptyList(),
    val recentEpisodes: List<Episode> = emptyList(),
    val continueListening: List<Episode> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOrder: PodcastSortOrder = PodcastSortOrder.RECENT_EPISODES,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val episodeRepository: EpisodeRepository
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val sortOrder = MutableStateFlow(PodcastSortOrder.RECENT_EPISODES)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        subscriptionRepository.getSubscribedPodcasts(),
        subscriptionRepository.getRecentEpisodes(),
        episodeRepository.getRecentlyPlayedEpisodes(),
        isRefreshing,
        sortOrder,
        error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val podcasts = values[0] as List<Podcast>
        @Suppress("UNCHECKED_CAST")
        val episodes = values[1] as List<Episode>
        @Suppress("UNCHECKED_CAST")
        val continueListening = values[2] as List<Episode>
        val refreshing = values[3] as Boolean
        val sort = values[4] as PodcastSortOrder
        val errorMsg = values[5] as String?

        val sortedPodcasts = when (sort) {
            PodcastSortOrder.RECENT_EPISODES -> {
                val podcastOrderFromEpisodes = episodes
                    .map { it.podcastId }
                    .distinct()
                val podcastsWithEpisodes = podcastOrderFromEpisodes.mapNotNull { id ->
                    podcasts.find { it.id == id }
                }
                val podcastsWithoutEpisodes = podcasts.filter { podcast ->
                    podcast.id !in podcastOrderFromEpisodes
                }
                podcastsWithEpisodes + podcastsWithoutEpisodes
            }
            PodcastSortOrder.ALPHABETICAL -> {
                podcasts.sortedBy { it.title.lowercase() }
            }
        }
        HomeUiState(
            subscribedPodcasts = sortedPodcasts,
            recentEpisodes = episodes,
            continueListening = continueListening,
            isRefreshing = refreshing,
            sortOrder = sort,
            error = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        refreshFeeds()
    }

    fun setSortOrder(order: PodcastSortOrder) {
        sortOrder.value = order
    }

    fun refreshFeeds() {
        viewModelScope.launch {
            isRefreshing.value = true
            error.value = null
            try {
                subscriptionRepository.refreshFeeds()
            } catch (e: Exception) {
                error.value = e.toUserMessage()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun clearError() {
        error.value = null
    }
}
