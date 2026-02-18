package com.astutepodcasts.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.astutepodcasts.app.domain.model.Episode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EpisodeListItem(
    episode: Episode,
    podcastArtworkUrl: String? = null,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit = {},
    onDeleteDownloadClick: () -> Unit = {},
    downloadProgress: Float? = null,
    onArchiveClick: (() -> Unit)? = null,
    onUnarchiveClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(if (episode.isArchived) Modifier.alpha(0.5f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.artworkUrl ?: podcastArtworkUrl,
            contentDescription = episode.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatEpisodeMetadata(episode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onArchiveClick != null) {
            IconButton(onClick = onArchiveClick) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive ${episode.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onUnarchiveClick != null) {
            IconButton(onClick = onUnarchiveClick) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = "Unarchive ${episode.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DownloadButton(
            downloadStatus = episode.downloadStatus,
            downloadProgress = downloadProgress,
            onDownloadClick = onDownloadClick,
            onCancelClick = onCancelDownloadClick,
            onDeleteClick = onDeleteDownloadClick
        )
        IconButton(onClick = onPlayClick) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play ${episode.title}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatEpisodeMetadata(episode: Episode): String {
    val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        .format(Date(episode.publishedAt * 1000))
    val duration = formatDuration(episode.durationSeconds)
    return "$date • $duration"
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
