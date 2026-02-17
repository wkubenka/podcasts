package com.astutepodcasts.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.ui.components.EpisodeListItem
import com.astutepodcasts.app.ui.components.PodcastCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPodcastClick: (Long) -> Unit,
    onEpisodePlayClick: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val samplePodcasts = samplePodcastList()
    val sampleEpisodes = sampleEpisodeList()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Astute Podcasts") }
        )

        if (samplePodcasts.isEmpty()) {
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
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(samplePodcasts) { podcast ->
                            PodcastCard(
                                podcast = podcast,
                                onClick = { onPodcastClick(podcast.id) }
                            )
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
                items(sampleEpisodes) { episode ->
                    EpisodeListItem(
                        episode = episode,
                        onPlayClick = { onEpisodePlayClick(episode) },
                        onDownloadClick = { },
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

private fun samplePodcastList() = listOf(
    Podcast(1, "The Daily", "The New York Times", "Daily news podcast", null, "", "en", 500, 0),
    Podcast(2, "Serial", "Serial Productions", "True crime stories", null, "", "en", 50, 0),
    Podcast(3, "Radiolab", "WNYC Studios", "Science and curiosity", null, "", "en", 300, 0),
    Podcast(4, "99% Invisible", "Roman Mars", "Design and architecture", null, "", "en", 400, 0),
)

private fun sampleEpisodeList() = listOf(
    Episode(1, 1, "Monday's Headlines", "Today's top stories", "", null, 1708300000, 1800, 0, 1, null),
    Episode(2, 2, "The Investigation Begins", "A new case unfolds", "", null, 1708200000, 3600, 0, 1, 1),
    Episode(3, 3, "The Sound of Science", "Exploring acoustics", "", null, 1708100000, 2700, 0, null, null),
    Episode(4, 4, "Hidden Design", "Unseen architecture", "", null, 1708000000, 2400, 0, null, null),
)
