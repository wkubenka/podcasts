package com.astutepodcasts.app.ui.podcastdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.ui.components.EpisodeListItem
import com.astutepodcasts.app.ui.components.EpisodeListItemPlaceholder
import com.astutepodcasts.app.ui.components.HtmlText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    podcastId: Long,
    onBackClick: () -> Unit,
    onEpisodePlayClick: (Episode) -> Unit,
    onEpisodeDownloadClick: (Episode) -> Unit,
    onCancelDownloadClick: (Long) -> Unit,
    onDeleteDownloadClick: (Long) -> Unit,
    downloadProgressMap: Map<Long, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    uiState.podcast?.title ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        when {
            uiState.isLoading -> {
                LazyColumn {
                    items(8) {
                        EpisodeListItemPlaceholder()
                    }
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = viewModel::retry) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.podcast != null -> {
                val podcast = uiState.podcast!!

                LazyColumn {
                    item {
                        PodcastHeader(
                            podcast = podcast,
                            isSubscribed = uiState.isSubscribed,
                            onSubscribeClick = viewModel::toggleSubscription
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodes (${uiState.filteredEpisodes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = viewModel::toggleSortOrder,
                                label = {
                                    Text(
                                        when (uiState.sortOrder) {
                                            EpisodeSortOrder.NEWEST_FIRST -> "Newest first"
                                            EpisodeSortOrder.OLDEST_FIRST -> "Oldest first"
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = uiState.showArchived,
                                onClick = viewModel::toggleShowArchived,
                                label = { Text("Show archived") }
                            )
                        }
                    }
                    items(uiState.filteredEpisodes) { episode ->
                        EpisodeListItem(
                            episode = episode,
                            podcastArtworkUrl = podcast.artworkUrl,
                            onPlayClick = { onEpisodePlayClick(episode) },
                            onDownloadClick = { onEpisodeDownloadClick(episode) },
                            onCancelDownloadClick = { onCancelDownloadClick(episode.id) },
                            onDeleteDownloadClick = { onDeleteDownloadClick(episode.id) },
                            downloadProgress = downloadProgressMap[episode.id]?.let { it / 100f },
                            onArchiveClick = if (episode.isArchived) null else { { viewModel.archiveEpisode(episode.id) } },
                            onUnarchiveClick = if (episode.isArchived) { { viewModel.unarchiveEpisode(episode.id) } } else null,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastHeader(
    podcast: Podcast,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = podcast.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            var descriptionExpanded by remember { mutableStateOf(false) }
            HtmlText(
                html = podcast.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { descriptionExpanded = !descriptionExpanded }
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isSubscribed) {
                OutlinedButton(onClick = onSubscribeClick) {
                    Text("Subscribed")
                }
            } else {
                Button(onClick = onSubscribeClick) {
                    Text("Subscribe")
                }
            }
        }
    }
}
