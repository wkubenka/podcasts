package com.astutepodcasts.app.ui.podcastdetail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.ui.components.EpisodeListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    podcastId: Long,
    onBackClick: () -> Unit,
    onEpisodePlayClick: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val podcast = samplePodcast(podcastId)
    val episodes = sampleEpisodes(podcastId)
    var isSubscribed by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(podcast.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn {
            item {
                PodcastHeader(
                    podcast = podcast,
                    isSubscribed = isSubscribed,
                    onSubscribeClick = { isSubscribed = !isSubscribed }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Episodes (${episodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(episodes) { episode ->
                EpisodeListItem(
                    episode = episode,
                    podcastArtworkUrl = podcast.artworkUrl,
                    onPlayClick = { onEpisodePlayClick(episode) },
                    onDownloadClick = { },
                    onClick = { }
                )
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
            Text(
                text = podcast.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun samplePodcast(id: Long) = Podcast(
    id = id,
    title = "Sample Podcast",
    author = "Sample Author",
    description = "This is a sample podcast description that tells you about the show and what to expect from each episode.",
    artworkUrl = null,
    feedUrl = "",
    language = "en",
    episodeCount = 5,
    lastUpdateTime = 0
)

private fun sampleEpisodes(podcastId: Long) = listOf(
    Episode(100, podcastId, "Episode 5: The Latest", "Most recent episode", "", null, 1708300000, 2400, 0, 5, 1),
    Episode(101, podcastId, "Episode 4: Deep Dive", "An in-depth look", "", null, 1708200000, 3600, 0, 4, 1),
    Episode(102, podcastId, "Episode 3: Interview", "Special guest interview", "", null, 1708100000, 2700, 0, 3, 1),
    Episode(103, podcastId, "Episode 2: The Basics", "Foundation concepts", "", null, 1708000000, 1800, 0, 2, 1),
    Episode(104, podcastId, "Episode 1: Introduction", "Welcome to the show", "", null, 1707900000, 1200, 0, 1, 1),
)
