package com.astutepodcasts.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PodcastSortOrder {
    RECENT_EPISODES,
    ALPHABETICAL
}

data class HomeUiState(
    val subscribedPodcasts: List<Podcast> = emptyList(),
    val recentEpisodes: List<Episode> = emptyList(),
    val isRefreshing: Boolean = false,
    val sortOrder: PodcastSortOrder = PodcastSortOrder.RECENT_EPISODES
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val sortOrder = MutableStateFlow(PodcastSortOrder.RECENT_EPISODES)

    val uiState: StateFlow<HomeUiState> = combine(
        subscriptionRepository.getSubscribedPodcasts(),
        subscriptionRepository.getRecentEpisodes(),
        isRefreshing,
        sortOrder
    ) { podcasts, episodes, refreshing, sort ->
        val sortedPodcasts = when (sort) {
            PodcastSortOrder.RECENT_EPISODES -> {
                // Order podcasts by their most recent episode's publish time
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
            isRefreshing = refreshing,
            sortOrder = sort
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
            try {
                subscriptionRepository.refreshFeeds()
            } finally {
                isRefreshing.value = false
            }
        }
    }
}
