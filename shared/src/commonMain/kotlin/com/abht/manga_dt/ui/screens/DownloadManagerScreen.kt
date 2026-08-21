package com.abht.manga_dt.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abht.manga_dt.models.DownloadStatus
import com.abht.manga_dt.models.DownloadTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    tasks: List<DownloadTask>,
    onToggleTask: (DownloadTask) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Downloads") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tasks) { task ->
                DownloadItem(task = task, onToggle = { onToggleTask(task) })
            }
        }
    }
}

@Composable
fun DownloadItem(task: DownloadTask, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = task.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(task.subtitle, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onToggle) {
                Icon(
                    if (task.status == DownloadStatus.DOWNLOADING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}
