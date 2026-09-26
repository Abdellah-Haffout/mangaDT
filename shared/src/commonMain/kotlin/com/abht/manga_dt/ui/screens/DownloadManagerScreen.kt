package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.DownloadManager
import com.abht.manga_dt.data.DownloadedChapterInfo
import com.abht.manga_dt.data.DownloadedMangaGroup
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.models.DownloadStatus
import com.abht.manga_dt.models.DownloadTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    onBack: () -> Unit,
    onNavigateToMangaDetails: ((sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit)? = null,
    onNavigateToReader: ((DownloadedChapterInfo) -> Unit)? = null
) {
    val strings = Strings.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // targetId to isManga
    var showDeleteAllReadConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteMangaReadConfirmDialog by remember { mutableStateOf<String?>(null) } // mangaKey
    var expandedMangaUrl by remember { mutableStateOf<String?>(null) }

    val activeTasks = DownloadManager.downloadTasks
    val downloadedGroups = remember(DownloadManager.downloadedChapters.values.toList()) {
        DownloadManager.getDownloadedMangaGroups()
    }
    val totalStorageUsed = remember(DownloadManager.downloadedChapters.values.toList()) {
        DownloadManager.getTotalStorageUsed()
    }
    val (totalReadCount, totalReadBytes) = remember(DownloadManager.downloadedChapters.values.toList(), HistoryManager.historyEntries) {
        DownloadManager.getReadDownloadedChaptersCount()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.downloads,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.storageUsed}: ${DownloadManager.formatBytes(totalStorageUsed)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                    }
                },
                actions = {
                    if (totalReadCount > 0) {
                        IconButton(onClick = { showDeleteAllReadConfirmDialog = true }) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = strings.deleteReadChaptersNowTitle,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    if (activeTasks.isNotEmpty()) {
                        IconButton(onClick = { DownloadManager.resumeAll() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = strings.resumeAll, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { DownloadManager.pauseAll() }) {
                            Icon(Icons.Default.Pause, contentDescription = strings.pauseAll)
                        }
                        IconButton(onClick = { DownloadManager.clearCompletedTasks() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = strings.clearCompleted)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.downloadQueueTab)
                            if (activeTasks.isNotEmpty()) {
                                Badge { Text("${activeTasks.size}") }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(strings.downloadedMangaTab)
                            if (downloadedGroups.isNotEmpty()) {
                                Badge { Text("${downloadedGroups.size}") }
                            }
                        }
                    }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Download Queue Tab
                    if (activeTasks.isEmpty()) {
                        EmptyDownloadsView(
                            icon = Icons.Default.CloudDownload,
                            title = strings.noActiveDownloads,
                            subtitle = strings.offlineReadingAvailable
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = activeTasks,
                                key = { it.id }
                            ) { task ->
                                DownloadTaskCard(
                                    task = task,
                                    onPause = { DownloadManager.pauseTask(task.id) },
                                    onResume = { DownloadManager.resumeTask(task.id) },
                                    onCancel = { DownloadManager.cancelTask(task.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Downloaded Manga Tab
                    if (downloadedGroups.isEmpty()) {
                        EmptyDownloadsView(
                            icon = Icons.Default.FolderZip,
                            title = strings.noDownloadedManga,
                            subtitle = strings.offlineReadingAvailable
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Banner for Deleting Read Chapters
                            if (totalReadCount > 0) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Default.CleaningServices,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = strings.deleteReadChaptersNowTitle,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                    Text(
                                                        text = "$totalReadCount ${strings.chapters} • ${DownloadManager.formatBytes(totalReadBytes)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { showDeleteAllReadConfirmDialog = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(strings.clear, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            items(
                                items = downloadedGroups,
                                key = { it.mangaUrl.ifBlank { it.mangaTitle } }
                            ) { group ->
                                val isExpanded = expandedMangaUrl == (group.mangaUrl.ifBlank { group.mangaTitle })
                                DownloadedMangaCard(
                                    group = group,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        val key = group.mangaUrl.ifBlank { group.mangaTitle }
                                        expandedMangaUrl = if (isExpanded) null else key
                                    },
                                    onMangaClick = {
                                        onNavigateToMangaDetails?.invoke(
                                            group.sourceId,
                                            group.mangaUrl,
                                            group.mangaTitle,
                                            group.mangaCover
                                        )
                                    },
                                    onChapterClick = { chapter ->
                                        onNavigateToReader?.invoke(chapter)
                                    },
                                    onDeleteChapter = { chapterUrl ->
                                        showDeleteConfirmDialog = Pair(chapterUrl, false)
                                    },
                                    onDeleteManga = {
                                        showDeleteConfirmDialog = Pair(group.mangaUrl.ifBlank { group.mangaTitle }, true)
                                    },
                                    onDeleteReadForManga = {
                                        showDeleteMangaReadConfirmDialog = group.mangaUrl.ifBlank { group.mangaTitle }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    showDeleteConfirmDialog?.let { (targetId, isManga) ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.deleteDownload) },
            text = { Text(strings.deleteDownloadConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isManga) {
                            DownloadManager.deleteMangaDownloads(targetId)
                        } else {
                            DownloadManager.deleteDownloadedChapter(targetId)
                        }
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.clear)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete All Read Chapters Confirmation Dialog
    if (showDeleteAllReadConfirmDialog) {
        val (readCount, readBytes) = DownloadManager.getReadDownloadedChaptersCount()
        AlertDialog(
            onDismissRequest = { showDeleteAllReadConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.deleteReadChaptersNowTitle) },
            text = {
                Text(
                    if (readCount > 0) strings.deleteReadChaptersConfirmMessage(readCount, DownloadManager.formatBytes(readBytes))
                    else strings.noReadChaptersToDelete
                )
            },
            confirmButton = {
                if (readCount > 0) {
                    Button(
                        onClick = {
                            DownloadManager.deleteReadChapters()
                            showDeleteAllReadConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.clear)
                    }
                } else {
                    TextButton(onClick = { showDeleteAllReadConfirmDialog = false }) { Text(strings.ok) }
                }
            },
            dismissButton = {
                if (readCount > 0) {
                    TextButton(onClick = { showDeleteAllReadConfirmDialog = false }) { Text(strings.cancel) }
                }
            }
        )
    }

    // Delete Manga's Read Chapters Confirmation Dialog
    showDeleteMangaReadConfirmDialog?.let { mangaKey ->
        val (mangaReadCount, mangaReadBytes) = DownloadManager.getReadDownloadedChaptersCount(mangaUrl = mangaKey, mangaTitle = mangaKey)
        AlertDialog(
            onDismissRequest = { showDeleteMangaReadConfirmDialog = null },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.deleteReadChaptersForManga) },
            text = {
                Text(
                    if (mangaReadCount > 0) strings.deleteReadChaptersConfirmMessage(mangaReadCount, DownloadManager.formatBytes(mangaReadBytes))
                    else strings.noReadChaptersToDelete
                )
            },
            confirmButton = {
                if (mangaReadCount > 0) {
                    Button(
                        onClick = {
                            DownloadManager.deleteReadChapters(mangaUrl = mangaKey, mangaTitle = mangaKey)
                            showDeleteMangaReadConfirmDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.clear)
                    }
                } else {
                    TextButton(onClick = { showDeleteMangaReadConfirmDialog = null }) { Text(strings.ok) }
                }
            },
            dismissButton = {
                if (mangaReadCount > 0) {
                    TextButton(onClick = { showDeleteMangaReadConfirmDialog = null }) { Text(strings.cancel) }
                }
            }
        )
    }
}

@Composable
private fun DownloadTaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val strings = Strings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task Cover
            Card(
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(44.dp, 60.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                AsyncImage(
                    model = task.thumbnailUrl,
                    contentDescription = task.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.width(12.dp))

            // Task Details & Progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when (task.status) {
                        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                        DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                        DownloadStatus.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (task.status) {
                        DownloadStatus.DOWNLOADING -> strings.downloading
                        DownloadStatus.QUEUED -> strings.downloadQueued
                        DownloadStatus.PAUSED -> "Paused"
                        DownloadStatus.COMPLETED -> strings.downloaded
                        DownloadStatus.ERROR -> "Error"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (task.status) {
                            DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                            DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                            DownloadStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    val percent = (task.progress * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (task.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(20.dp))
                        }
                    }
                    DownloadStatus.PAUSED, DownloadStatus.ERROR -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                    }
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun DownloadedMangaCard(
    group: DownloadedMangaGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onMangaClick: () -> Unit,
    onChapterClick: (DownloadedChapterInfo) -> Unit,
    onDeleteChapter: (String) -> Unit,
    onDeleteManga: () -> Unit,
    onDeleteReadForManga: () -> Unit
) {
    val strings = Strings.current
    val readChaptersForManga = remember(group.chapters, HistoryManager.historyEntries) {
        group.chapters.count { DownloadManager.isChapterRead(it.chapterUrl, group.mangaTitle, group.mangaUrl) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Poster
                Card(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(56.dp, 80.dp)
                        .clickable(onClick = onMangaClick),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AsyncImage(
                        model = group.mangaCover,
                        contentDescription = group.mangaTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.mangaTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${group.chapters.size} ${strings.chapters}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = DownloadManager.formatBytes(group.totalSizeBytes),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        if (readChaptersForManga > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "$readChaptersForManga ${strings.read}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Delete Read Chapters for this manga
                if (readChaptersForManga > 0) {
                    IconButton(onClick = onDeleteReadForManga) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = strings.deleteReadChaptersForManga,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                IconButton(onClick = onDeleteManga) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = strings.deleteDownload,
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            // Expanded Chapters List
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(2.dp))

                    group.chapters.forEach { chapter ->
                        val isRead = DownloadManager.isChapterRead(chapter.chapterUrl, group.mangaTitle, group.mangaUrl)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRead) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterClick(chapter) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isRead) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isRead) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = chapter.chapterTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (isRead) {
                                                Text(
                                                    text = strings.read,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color(0xFF4CAF50)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${chapter.pageCount} pages • ${DownloadManager.formatBytes(chapter.sizeBytes)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteChapter(chapter.chapterUrl) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = strings.deleteDownload,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
