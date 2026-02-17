package com.astutepodcasts.app.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.astutepodcasts.app.domain.model.Podcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onPodcastClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val trendingPodcasts = sampleTrendingPodcasts()

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { isActive = false },
                    expanded = false,
                    onExpandedChange = { },
                    placeholder = { Text("Search podcasts...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
            },
            expanded = false,
            onExpandedChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) { }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (query.isBlank()) "Trending Podcasts" else "Search Results",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn {
            items(trendingPodcasts) { podcast ->
                SearchResultItem(
                    podcast = podcast,
                    onClick = { onPodcastClick(podcast.id) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    podcast: Podcast,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = podcast.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = podcast.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = podcast.author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${podcast.episodeCount} episodes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun sampleTrendingPodcasts() = listOf(
    Podcast(10, "Huberman Lab", "Andrew Huberman", "Science-based tools for everyday life", null, "", "en", 200, 0),
    Podcast(11, "Lex Fridman Podcast", "Lex Fridman", "Conversations about science and technology", null, "", "en", 400, 0),
    Podcast(12, "The Joe Rogan Experience", "Joe Rogan", "Long-form conversations", null, "", "en", 2000, 0),
    Podcast(13, "Acquired", "Ben Gilbert & David Rosenthal", "The greatest business stories", null, "", "en", 150, 0),
    Podcast(14, "Hardcore History", "Dan Carlin", "In-depth history deep dives", null, "", "en", 70, 0),
)
