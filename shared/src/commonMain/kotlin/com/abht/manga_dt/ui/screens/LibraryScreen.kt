package com.abht.manga_dt.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.LibrarySortOrder
import com.abht.manga_dt.ui.models.LayoutMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onMangaClick: (LibraryManga) -> Unit,
    onHistoryItemClick: (HistoryEntry) -> Unit = {},
    onExploreClick: () -> Unit = {},
    showSortSheet: Boolean = false,
    onDismissSortSheet: () -> Unit = {}
) {
    val libraryItems = LibraryManager.libraryItems
    val historyItems = HistoryManager.historyEntries
    val coroutineScope = rememberCoroutineScope()
    val tabs = remember(libraryItems, historyItems, LibraryManager.categories) {
        val base = mutableListOf("All", "Reading", "Favorites", "History")
        LibraryManager.categories.forEach { cat ->
            if (cat !in base) {
                base.add(cat)
            }
        }
        base.toList()
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var localShowSortSheet by remember { mutableStateOf(false) }
    val effectiveShowSortSheet = showSortSheet || localShowSortSheet
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val layoutMode = LibraryManager.layoutMode
    val sortOrder = LibraryManager.sortOrder

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Category Tabs: [ All | Reading | Favorites | History | ... ]
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.lastIndex),
                edgePadding = 16.dp,
                divider = { HorizontalDivider() }
            ) {
                tabs.forEachIndexed { index, tabName ->
                    val count = when (tabName) {
                        "All" -> libraryItems.size
                        "History" -> historyItems.size
                        else -> libraryItems.count { it.category.equals(tabName, ignoreCase = true) }
                    }
                    val isSelected = pagerState.currentPage == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (tabName) {
                                    "History" -> {
                                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    "Favorites" -> {
                                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    "Reading" -> {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                }
                                Text(tabName)
                                if (count > 0) {
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                // Add Category Tab Button
                Tab(
                    selected = false,
                    onClick = { showAddCategoryDialog = true },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("New")
                        }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) { pageIndex ->
                val currentTab = tabs.getOrElse(pageIndex) { "All" }
                if (currentTab == "History") {
                    // History View inside Category Tab
                    HistoryScreen(
                        onHistoryItemClick = onHistoryItemClick,
                        showTopBar = false
                    )
                } else {
                    val categoryManga = remember(libraryItems, currentTab, sortOrder) {
                        var list = libraryItems

                        // Category filter
                        if (currentTab != "All") {
                            list = list.filter { it.category.equals(currentTab, ignoreCase = true) }
                        }

                        // Sort order
                        when (sortOrder) {
                            LibrarySortOrder.LAST_READ -> list.sortedByDescending { it.lastReadAt }
                            LibrarySortOrder.ALPHABETICAL -> list.sortedBy { it.title.lowercase() }
                            LibrarySortOrder.DATE_ADDED -> list.sortedByDescending { it.addedAt }
                            LibrarySortOrder.UNREAD_COUNT -> list.sortedByDescending { it.unreadCount }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (libraryItems.isEmpty()) {
                            // Empty Library State
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CollectionsBookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "Your Library is Empty",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Add manga you love from Browse to follow updates and track your reading progress.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else if (categoryManga.isEmpty()) {
                            // Empty Filtered Category State
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "No manga in \"$currentTab\"",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            // Manga Grid / List View
                            LazyVerticalGrid(
                                columns = when (layoutMode) {
                                    LayoutMode.COMFORTABLE -> GridCells.Adaptive(minSize = 135.dp)
                                    LayoutMode.COMPACT -> GridCells.Adaptive(minSize = 110.dp)
                                    LayoutMode.LIST -> GridCells.Fixed(1)
                                },
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (AppSettings.floatingNavBar) 84.dp else 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = categoryManga,
                                    key = { "${it.sourceId}_${it.mangaUrl}_${it.title}" }
                                ) { manga ->
                                    when (layoutMode) {
                                        LayoutMode.COMFORTABLE -> LibraryComfortableCard(manga = manga, onClick = { onMangaClick(manga) })
                                        LayoutMode.COMPACT -> LibraryCompactCard(manga = manga, onClick = { onMangaClick(manga) })
                                        LayoutMode.LIST -> LibraryListCard(manga = manga, onClick = { onMangaClick(manga) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Category Dialog
        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("New Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Category name (e.g. Shonen, Favorites)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                LibraryManager.addCategory(newCategoryName)
                                newCategoryName = ""
                                showAddCategoryDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Sort Bottom Sheet
        if (effectiveShowSortSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    localShowSortSheet = false
                    onDismissSortSheet()
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Sort Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))

                    LibrarySortOrder.entries.forEach { order ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LibraryManager.updateSortOrder(order)
                                    localShowSortSheet = false
                                    onDismissSortSheet()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortOrder == order,
                                onClick = {
                                    LibraryManager.updateSortOrder(order)
                                    localShowSortSheet = false
                                    onDismissSortSheet()
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(order.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryComfortableCard(
    manga: LibraryManga,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            ) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )

                // Unread Badge
                if (manga.unreadCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "${manga.unreadCount}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = manga.sourceId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun LibraryCompactCard(
    manga: LibraryManga,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
        ) {
            AsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = rememberVectorPainter(Icons.Default.BrokenImage)
            )

            // Title Overlay at bottom
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Total chapters badge
            if (manga.totalChapters > 0) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${manga.totalChapters}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryListCard(
    manga: LibraryManga,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .width(55.dp)
                    .height(78.dp)
            ) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = manga.sourceId,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = manga.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (manga.lastReadChapterTitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Last: ${manga.lastReadChapterTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
