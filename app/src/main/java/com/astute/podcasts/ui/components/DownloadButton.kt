package com.astute.podcasts.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.astute.podcasts.domain.model.DownloadStatus

@Composable
fun DownloadButton(
    downloadStatus: DownloadStatus,
    downloadProgress: Float?,
    onDownloadClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (downloadStatus) {
        DownloadStatus.NOT_DOWNLOADED -> {
            IconButton(onClick = onDownloadClick, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DownloadStatus.QUEUED -> {
            IconButton(onClick = onCancelClick, modifier = modifier) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        DownloadStatus.DOWNLOADING -> {
            IconButton(onClick = onCancelClick, modifier = modifier) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { downloadProgress ?: 0f },
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel download",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        DownloadStatus.DOWNLOADED -> {
            IconButton(onClick = onDeleteClick, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        DownloadStatus.FAILED -> {
            IconButton(onClick = onDownloadClick, modifier = modifier) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Download failed, tap to retry",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
