package com.astutepodcasts.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.ui.components.EpisodeListItem
import com.astutepodcasts.app.ui.components.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPodcastClick: (Long) -> Unit,
    onEpisodePlayClick: (Episode) -> Unit,
    onEpisodeDownloadClick: (Episode) -> Unit,
    onCancelDownloadClick: (Long) -> Unit,
    onDeleteDownloadClick: (Long) -> Unit,
    downloadProgressMap: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Astute Podcasts") }
        )

        if (uiState.subscribedPodcasts.isEmpty()) {
            EmptyHomeState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "Your Podcasts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        FilterChip(
                            selected = uiState.sortOrder == PodcastSortOrder.RECENT_EPISODES,
                            onClick = { viewModel.setSortOrder(PodcastSortOrder.RECENT_EPISODES) },
                            label = { Text("Recent") }
                        )
                        FilterChip(
                            selected = uiState.sortOrder == PodcastSortOrder.ALPHABETICAL,
                            onClick = { viewModel.setSortOrder(PodcastSortOrder.ALPHABETICAL) },
                            label = { Text("A\u2013Z") }
                        )
                    }
                }
                val rows = uiState.subscribedPodcasts.chunked(3)
                items(rows) { rowPodcasts ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        rowPodcasts.forEach { podcast ->
                            PodcastCard(
                                podcast = podcast,
                                onClick = { onPodcastClick(podcast.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining slots with empty spacers for incomplete rows
                        repeat(3 - rowPodcasts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recent Episodes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.recentEpisodes) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onPlayClick = { onEpisodePlayClick(episode) },
                        onDownloadClick = { onEpisodeDownloadClick(episode) },
                        onCancelDownloadClick = { onCancelDownloadClick(episode.id) },
                        onDeleteDownloadClick = { onDeleteDownloadClick(episode.id) },
                        downloadProgress = downloadProgressMap[episode.id]?.let { it / 100f },
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No subscriptions yet",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Search for podcasts to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
