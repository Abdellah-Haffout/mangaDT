package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.DownloadManager
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.data.MangaDataCache
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.OfflineMangaManager
import com.abht.manga_dt.data.StatisticsManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.data.ThemeMode
import com.abht.manga_dt.data.currentTimeMillis
import com.abht.manga_dt.ui.components.BackHandler
import com.abht.manga_dt.ui.components.CaptchaWebViewDialog
import com.abht.manga_dt.ui.components.MaterialYouMangaPosterCard
import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.DownloadStatus
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaReadingStats
import com.abht.manga_dt.models.MangaStatus
import com.abht.manga_dt.ui.theme.extractDominantColor
import com.abht.manga_dt.ui.theme.generateSeedFromMetadata
import com.abht.manga_dt.ui.theme.rememberAnimatedDynamicColorScheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MangaDetailsScreen(
    sourceId: String,
    mangaUrl: String,
    initialTitle: String = "",
    initialCover: String = "",
    onBackClick: () -> Unit,
    onChapterClick: (chapter: Chapter, initialPage: Int, scrollOffset: Int) -> Unit,
    onMangaClick: ((sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit)? = null
) {
    val sourceManager = remember { MangaSourceManager() }
    val coroutineScope = rememberCoroutineScope()
    val cacheKey = "$sourceId::$mangaUrl"
    val offlineCached = remember(sourceId, mangaUrl, initialTitle) {
        OfflineMangaManager.getManga(sourceId, mangaUrl, initialTitle) ?: MangaDataCache.cachedMangaDetails[cacheKey]
    }

    var mangaDetails by remember(sourceId, mangaUrl) { mutableStateOf(offlineCached) }
    var isLoading by remember(sourceId, mangaUrl) { mutableStateOf(offlineCached == null) }
    var errorMessage by remember(sourceId, mangaUrl) { mutableStateOf<String?>(null) }
    var isChaptersAscending by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var chapterSearchQuery by remember { mutableStateOf("") }
    var isChapterSearchActive by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    var selectedGenreForDialog by remember { mutableStateOf<String?>(null) }
    var genreMangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoadingGenreManga by remember { mutableStateOf(false) }
    var similarMangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoadingSimilar by remember { mutableStateOf(false) }
    var showBatchDownloadMenu by remember { mutableStateOf(false) }
    var showChaptersBottomSheet by remember { mutableStateOf(false) }
    val strings = Strings.current

    // Back handling for search and dialogs in MangaDetails
    BackHandler(enabled = showChaptersBottomSheet) { showChaptersBottomSheet = false }
    BackHandler(enabled = isChapterSearchActive) {
        isChapterSearchActive = false
        chapterSearchQuery = ""
    }
    BackHandler(enabled = showCategoryPicker) { showCategoryPicker = false }
    BackHandler(enabled = showCaptchaDialog) { showCaptchaDialog = false }
    BackHandler(enabled = selectedGenreForDialog != null) { selectedGenreForDialog = null }

    fun loadDetails(forceRefresh: Boolean = false) {
        coroutineScope.launch {
            if (forceRefresh) {
                MangaDataCache.cachedMangaDetails.remove(cacheKey)
            }
            if (mangaDetails == null) {
                isLoading = true
            }
            errorMessage = null
            try {
                val result = sourceManager.getMangaDetails(sourceId, mangaUrl)
                if (result != null) {
                    mangaDetails = result
                    OfflineMangaManager.saveManga(result)
                } else if (mangaDetails == null) {
                    val fallback = OfflineMangaManager.getManga(sourceId, mangaUrl, initialTitle)
                    if (fallback != null) {
                        mangaDetails = fallback
                    } else {
                        errorMessage = "Failed to load manga details"
                    }
                }
            } catch (e: Exception) {
                if (mangaDetails == null) {
                    val fallback = OfflineMangaManager.getManga(sourceId, mangaUrl, initialTitle)
                    if (fallback != null) {
                        mangaDetails = fallback
                    } else {
                        errorMessage = e.message ?: "An error occurred while loading details"
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sourceId, mangaUrl) {
        loadDetails(forceRefresh = false)
    }

    val displayTitle = mangaDetails?.title?.ifBlank { initialTitle } ?: initialTitle
    val displayCover = mangaDetails?.thumbnailUrl?.ifBlank { initialCover } ?: initialCover
    val allChapters = mangaDetails?.chapters ?: emptyList()

    val metadataSeed = remember(displayTitle, displayCover) { generateSeedFromMetadata(displayTitle, displayCover) }
    val cachedSeed = remember(displayCover) {
        if (displayCover.isNotBlank()) MangaDataCache.cachedArtworkSeeds[displayCover] else null
    }
    var artworkSeed by remember(displayCover, cachedSeed) { mutableStateOf(cachedSeed) }
    val activeSeedColor = artworkSeed ?: metadataSeed
    val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (AppSettings.themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val isAmoled = AppSettings.amoledBlack && isDark
    val dynamicColorScheme = rememberAnimatedDynamicColorScheme(
        seedColor = activeSeedColor,
        isDark = isDark,
        isAmoled = isAmoled
    )

    // Filter and sort chapters
    val filteredChapters = remember(allChapters, isChaptersAscending, chapterSearchQuery) {
        val list = if (chapterSearchQuery.isBlank()) {
            allChapters
        } else {
            allChapters.filter {
                it.title.contains(chapterSearchQuery, ignoreCase = true) ||
                it.chapterNumber.toString().contains(chapterSearchQuery) ||
                (it.scanlator != null && it.scanlator.contains(chapterSearchQuery, ignoreCase = true))
            }
        }
        if (isChaptersAscending) list.sortedBy { it.chapterNumber } else list.sortedByDescending { it.chapterNumber }
    }

    // Check reading history for Resume button
    val lastReadEntry = remember(displayTitle, mangaUrl, HistoryManager.historyEntries) {
        HistoryManager.historyEntries.firstOrNull {
            (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) || it.mangaTitle.equals(displayTitle, ignoreCase = true)
        }
    }

    // Check reading stats for this manga
    val mangaStats = remember(displayTitle, mangaUrl, StatisticsManager.mangaStatsMap) {
        StatisticsManager.getMangaStats(displayTitle, mangaUrl, sourceId)
    }

    // Load manga by clicked genre tag
    LaunchedEffect(selectedGenreForDialog) {
        val genre = selectedGenreForDialog ?: return@LaunchedEffect
        isLoadingGenreManga = true
        try {
            val searchRes = sourceManager.searchManga(sourceId, genre)
            if (searchRes.isNotEmpty()) {
                genreMangaList = searchRes.filter { it.url != mangaUrl && !it.title.equals(displayTitle, ignoreCase = true) }
            } else {
                val pop = sourceManager.getPopularManga(sourceId, 1) + sourceManager.getPopularManga(sourceId, 2)
                val filtered = pop.filter { it.tags.any { t -> t.contains(genre, ignoreCase = true) } || it.title.contains(genre, ignoreCase = true) }
                genreMangaList = (if (filtered.isNotEmpty()) filtered else pop).filter { it.url != mangaUrl && !it.title.equals(displayTitle, ignoreCase = true) }
            }
        } catch (e: Exception) {
            genreMangaList = emptyList()
        } finally {
            isLoadingGenreManga = false
        }
    }

    // Load similar manga recommendations from current source
    LaunchedEffect(sourceId, mangaDetails?.tags, displayTitle) {
        if (similarMangaList.isEmpty() && sourceId.isNotBlank()) {
            isLoadingSimilar = true
            try {
                val currentTags = mangaDetails?.tags ?: emptyList()
                val popular = sourceManager.getPopularManga(sourceId, 1)
                val filtered = popular
                    .filter { it.url != mangaUrl && !it.title.equals(displayTitle, ignoreCase = true) }
                    .sortedByDescending { item ->
                        item.tags.count { tag -> currentTags.any { it.equals(tag, ignoreCase = true) } }
                    }
                similarMangaList = filtered.take(10)
            } catch (e: Exception) {
                // Ignore fallback
            } finally {
                isLoadingSimilar = false
            }
        }
    }

    // Check Library status
    val isInLibrary = remember(displayTitle, mangaUrl, LibraryManager.libraryItems) {
        LibraryManager.isMangaInLibrary(mangaUrl, displayTitle)
    }
    val libraryItem = remember(isInLibrary, LibraryManager.libraryItems) {
        LibraryManager.getLibraryManga(mangaUrl, displayTitle)
    }

    fun handleAddToLibrary() {
        val item = LibraryManga(
            id = mangaUrl.ifBlank { displayTitle },
            title = displayTitle,
            thumbnailUrl = displayCover,
            sourceId = sourceId,
            mangaUrl = mangaUrl,
            category = "Reading",
            addedAt = currentTimeMillis(),
            totalChapters = allChapters.size,
            author = mangaDetails?.author,
            status = mangaDetails?.status ?: MangaStatus.UNKNOWN
        )
        LibraryManager.addToLibrary(item)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = dynamicColorScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = displayTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        // Search Chapters Toggle
                        IconButton(onClick = {
                            isChapterSearchActive = !isChapterSearchActive
                            if (!isChapterSearchActive) {
                                chapterSearchQuery = ""
                            } else {
                                showChaptersBottomSheet = true
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Chapters")
                        }

                        // Sort Chapters Toggle
                        IconButton(onClick = {
                            isChaptersAscending = !isChaptersAscending
                            showChaptersBottomSheet = true
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = if (isChaptersAscending) "Sort Descending" else "Sort Ascending"
                            )
                        }

                        // Batch Download Chapters
                        Box {
                            IconButton(onClick = { showBatchDownloadMenu = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = strings.batchDownloadTitle)
                            }
                            DropdownMenu(
                                expanded = showBatchDownloadMenu,
                                onDismissRequest = { showBatchDownloadMenu = false }
                            ) {
                                val targetManga = mangaDetails ?: Manga(
                                    id = mangaUrl,
                                    title = displayTitle,
                                    thumbnailUrl = displayCover,
                                    source = sourceId,
                                    url = mangaUrl,
                                    chapters = allChapters
                                )

                                val unreadChapters = remember(allChapters, mangaStats) {
                                    val readUrls = mangaStats?.chaptersRead ?: emptySet()
                                    allChapters.filter { it.url !in readUrls && !DownloadManager.isChapterDownloaded(it.url) }
                                }

                                DropdownMenuItem(
                                    text = { Text(strings.downloadNextCount(1)) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showBatchDownloadMenu = false
                                        val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(1)
                                        DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.downloadNextCount(5)) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showBatchDownloadMenu = false
                                        val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(5)
                                        DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.downloadNextCount(10)) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                    onClick = {
                                        showBatchDownloadMenu = false
                                        val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(10)
                                        DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                    }
                                )
                                if (unreadChapters.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(strings.downloadAllUnread) },
                                        leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                        onClick = {
                                            showBatchDownloadMenu = false
                                            DownloadManager.enqueueBatchDownload(targetManga, unreadChapters)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(strings.downloadAllChapters) },
                                    leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                                    onClick = {
                                        showBatchDownloadMenu = false
                                        DownloadManager.enqueueBatchDownload(targetManga, allChapters)
                                    }
                                )

                                val (readDownloadCount, readDownloadBytes) = remember(allChapters, DownloadManager.downloadedChapters.size) {
                                    DownloadManager.getReadDownloadedChaptersCount(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                                }
                                if (readDownloadCount > 0) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("${strings.deleteReadChaptersForManga} ($readDownloadCount)") },
                                        leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showBatchDownloadMenu = false
                                            val (deleted, freed) = DownloadManager.deleteReadChapters(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(strings.deleteReadChaptersSuccessMessage(deleted, DownloadManager.formatBytes(freed)))
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Refresh Button
                        IconButton(onClick = { loadDetails(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Details")
                        }
                    }
                )
            }
        ) { padding ->
            if (isLoading && mangaDetails == null) {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading manga details...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else if (errorMessage != null && mangaDetails == null) {
                val isCaptcha = errorMessage?.let { err ->
                    val lower = err.lowercase()
                    lower.contains("403") || lower.contains("503") || lower.contains("cloudflare") ||
                    lower.contains("turnstile") || lower.contains("captcha") || lower.contains("challenge") ||
                    lower.contains("ddos") || lower.contains("just a moment") || lower.contains("cf-chl") ||
                    lower.contains("protection") || lower.contains("access denied") || lower.contains("forbidden")
                } == true

                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isCaptcha) {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = strings.captchaRequired,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { showCaptchaDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.solveCaptcha, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(onClick = { loadDetails(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.retry)
                        }
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.padding(padding).fillMaxSize()) {
                    val isWideScreen = maxWidth >= 720.dp

                    if (isWideScreen) {
                        // Desktop / Wide Screen Dual-Pane Layout
                        MangaDetailsWideLayout(
                            sourceId = sourceId,
                            mangaUrl = mangaUrl,
                            displayTitle = displayTitle,
                            displayCover = displayCover,
                            mangaDetails = mangaDetails,
                            allChapters = allChapters,
                            filteredChapters = filteredChapters,
                            lastReadEntry = lastReadEntry,
                            mangaStats = mangaStats,
                            similarManga = similarMangaList,
                            isLoadingSimilar = isLoadingSimilar,
                            isInLibrary = isInLibrary,
                            libraryItem = libraryItem,
                            chapterSearchQuery = chapterSearchQuery,
                            onChapterSearchQueryChange = { chapterSearchQuery = it },
                            isChaptersAscending = isChaptersAscending,
                            onToggleSort = { isChaptersAscending = !isChaptersAscending },
                            onChapterClick = onChapterClick,
                            onAddToLibrary = ::handleAddToLibrary,
                            onOpenCategoryPicker = { showCategoryPicker = true },
                            onGenreClick = { selectedGenreForDialog = it },
                            onMangaClick = onMangaClick,
                            onArtworkColorExtracted = { color ->
                                artworkSeed = color
                                if (displayCover.isNotBlank()) MangaDataCache.cachedArtworkSeeds[displayCover] = color
                            },
                            onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    } else {
                        // Mobile / Phone Single-Column Layout
                        MangaDetailsCompactLayout(
                            sourceId = sourceId,
                            mangaUrl = mangaUrl,
                            displayTitle = displayTitle,
                            displayCover = displayCover,
                            mangaDetails = mangaDetails,
                            allChapters = allChapters,
                            lastReadEntry = lastReadEntry,
                            mangaStats = mangaStats,
                            similarManga = similarMangaList,
                            isLoadingSimilar = isLoadingSimilar,
                            isInLibrary = isInLibrary,
                            libraryItem = libraryItem,
                            isChaptersAscending = isChaptersAscending,
                            isDescriptionExpanded = isDescriptionExpanded,
                            onToggleDescription = { isDescriptionExpanded = !isDescriptionExpanded },
                            onChapterClick = onChapterClick,
                            onOpenChaptersBottomSheet = { showChaptersBottomSheet = true },
                            onAddToLibrary = ::handleAddToLibrary,
                            onOpenCategoryPicker = { showCategoryPicker = true },
                            onGenreClick = { selectedGenreForDialog = it },
                            onMangaClick = onMangaClick,
                            onArtworkColorExtracted = { color ->
                                artworkSeed = color
                                if (displayCover.isNotBlank()) MangaDataCache.cachedArtworkSeeds[displayCover] = color
                            },
                            onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    }
                }
            }

            // Chapters Bottom Sheet (for Mobile / Compact Screens)
            if (showChaptersBottomSheet) {
                MangaChaptersModalBottomSheet(
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                    displayTitle = displayTitle,
                    displayCover = displayCover,
                    mangaDetails = mangaDetails,
                    allChapters = allChapters,
                    filteredChapters = filteredChapters,
                    lastReadEntry = lastReadEntry,
                    mangaStats = mangaStats,
                    chapterSearchQuery = chapterSearchQuery,
                    onChapterSearchQueryChange = { chapterSearchQuery = it },
                    isChapterSearchActive = isChapterSearchActive,
                    onToggleChapterSearch = {
                        isChapterSearchActive = !isChapterSearchActive
                        if (!isChapterSearchActive) chapterSearchQuery = ""
                    },
                    isChaptersAscending = isChaptersAscending,
                    onToggleSort = { isChaptersAscending = !isChaptersAscending },
                    onChapterClick = { ch, page, offset ->
                        showChaptersBottomSheet = false
                        onChapterClick(ch, page, offset)
                    },
                    onDismissRequest = { showChaptersBottomSheet = false },
                    onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                )
            }

            // Category Picker Dialog (Shared across Wide & Compact)
            if (showCategoryPicker) {
                AlertDialog(
                    onDismissRequest = { showCategoryPicker = false },
                    title = {
                        Text(
                            text = "Library Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Assign \"$displayTitle\" to a category:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            LibraryManager.categories.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LibraryManager.updateCategory(mangaUrl, displayTitle, cat)
                                            showCategoryPicker = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = libraryItem?.category == cat,
                                        onClick = {
                                            LibraryManager.updateCategory(mangaUrl, displayTitle, cat)
                                            showCategoryPicker = false
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(cat, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                LibraryManager.removeFromLibrary(mangaUrl, displayTitle)
                                showCategoryPicker = false
                            }
                        ) {
                            Text("Remove from Library", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCategoryPicker = false }) {
                            Text("Done")
                        }
                    }
                )
            }

            // Genre / Tag Manga Explorer Dialog
            if (selectedGenreForDialog != null) {
                val genre = selectedGenreForDialog ?: ""
                AlertDialog(
                    onDismissRequest = { selectedGenreForDialog = null },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.mangaWithGenreTitle(genre),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 460.dp)
                        ) {
                            if (isLoadingGenreManga) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = strings.loadingGenreManga,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            } else if (genreMangaList.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.noMangaInGenre,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 100.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(genreMangaList, key = { it.url.ifBlank { it.title } }) { item ->
                                        Box(modifier = Modifier.height(160.dp)) {
                                            MaterialYouMangaPosterCard(
                                                manga = item,
                                                onClick = {
                                                    selectedGenreForDialog = null
                                                    onMangaClick?.invoke(sourceId, item.url, item.title, item.thumbnailUrl)
                                                },
                                                cornerRadiusDp = 10,
                                                showSourceBadge = false,
                                                showRatingBadge = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedGenreForDialog = null }) {
                            Text(strings.close)
                        }
                    }
                )
            }

            if (showCaptchaDialog) {
                val sourceUrl = if (mangaUrl.startsWith("http")) mangaUrl else sourceManager.getSourceBaseUrl(sourceId)
                CaptchaWebViewDialog(
                    url = sourceUrl,
                    title = displayTitle,
                    onDismiss = { showCaptchaDialog = false },
                    onSolved = {
                        showCaptchaDialog = false
                        loadDetails(forceRefresh = true)
                    }
                )
            }
        }
    }
}

/**
 * Dual Split Buttons Action Bar (Read + Download + Library)
 * Matches Material 3 Segmented Pill button specification
 */
@Composable
fun MangaDetailsActionSplitButtonsBar(
    sourceId: String,
    mangaUrl: String,
    displayTitle: String,
    displayCover: String?,
    mangaDetails: Manga?,
    allChapters: List<Chapter>,
    lastReadEntry: HistoryEntry?,
    isInLibrary: Boolean,
    libraryItem: LibraryManga?,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onAddToLibrary: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onShowSnackbar: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = Strings.current
    var showReadDropdown by remember { mutableStateOf(false) }
    var showLibraryDropdown by remember { mutableStateOf(false) }
    var showBatchDownloadMenu by remember { mutableStateOf(false) }

    val firstChapter = allChapters.minByOrNull { it.chapterNumber }
        ?: allChapters.lastOrNull()
        ?: allChapters.firstOrNull()
    val latestChapter = allChapters.maxByOrNull { it.chapterNumber }
        ?: allChapters.firstOrNull()
    val resumeChapter = if (lastReadEntry != null) {
        allChapters.firstOrNull { it.url == lastReadEntry.chapterUrl || it.title == lastReadEntry.chapterTitle } ?: firstChapter
    } else {
        firstChapter
    }

    val targetManga = mangaDetails ?: Manga(
        id = mangaUrl,
        title = displayTitle,
        thumbnailUrl = displayCover ?: "",
        source = sourceId,
        url = mangaUrl,
        chapters = allChapters
    )

    val unreadChapters = remember(allChapters, lastReadEntry) {
        allChapters.filter { !DownloadManager.isChapterDownloaded(it.url) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ==========================================
        // 1. READ & DOWNLOAD SPLIT BUTTON GROUP (PROMINENT)
        // ==========================================
        Row(
            modifier = Modifier
                .weight(1.35f)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // (a) Read Primary Segment
            Surface(
                onClick = {
                    if (lastReadEntry != null && resumeChapter != null) {
                        onChapterClick(resumeChapter, lastReadEntry.lastPage.coerceAtLeast(1), lastReadEntry.scrollOffset)
                    } else if (firstChapter != null) {
                        onChapterClick(firstChapter, 1, 0)
                    } else if (lastReadEntry != null) {
                        onChapterClick(
                            Chapter(
                                id = "resume",
                                mangaId = mangaUrl,
                                title = lastReadEntry.chapterTitle,
                                chapterNumber = 1f,
                                url = lastReadEntry.chapterUrl
                            ),
                            lastReadEntry.lastPage.coerceAtLeast(1),
                            lastReadEntry.scrollOffset
                        )
                    }
                },
                enabled = resumeChapter != null || lastReadEntry != null,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (lastReadEntry != null) {
                            val chName = lastReadEntry.chapterTitle.replace(Regex("(?i)chapter\\s*"), "").trim()
                            if (lastReadEntry.lastPage > 1) "Resume $chName" else "Resume $chName"
                        } else strings.startReading,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            // (b) Fast Download Prominent Action Segment
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showBatchDownloadMenu = true },
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .width(42.dp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = strings.batchDownloadTitle,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Batch Download Dropdown Menu
                DropdownMenu(
                    expanded = showBatchDownloadMenu,
                    onDismissRequest = { showBatchDownloadMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.downloadNextCount(1)) },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showBatchDownloadMenu = false
                            val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(1)
                            DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                            onShowSnackbar?.invoke(strings.downloadNextCount(1))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.downloadNextCount(5)) },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showBatchDownloadMenu = false
                            val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(5)
                            DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                            onShowSnackbar?.invoke(strings.downloadNextCount(5))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.downloadNextCount(10)) },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                        onClick = {
                            showBatchDownloadMenu = false
                            val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(10)
                            DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                            onShowSnackbar?.invoke(strings.downloadNextCount(10))
                        }
                    )
                    if (unreadChapters.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(strings.downloadAllUnread) },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                            onClick = {
                                showBatchDownloadMenu = false
                                DownloadManager.enqueueBatchDownload(targetManga, unreadChapters)
                                onShowSnackbar?.invoke(strings.downloadAllUnread)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(strings.downloadAllChapters) },
                        leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                        onClick = {
                            showBatchDownloadMenu = false
                            DownloadManager.enqueueBatchDownload(targetManga, allChapters)
                            onShowSnackbar?.invoke(strings.downloadAllChapters)
                        }
                    )

                    val (readDownloadCount, readDownloadBytes) = remember(allChapters, DownloadManager.downloadedChapters.size) {
                        DownloadManager.getReadDownloadedChaptersCount(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                    }
                    if (readDownloadCount > 0) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("${strings.deleteReadChaptersForManga} ($readDownloadCount)") },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showBatchDownloadMenu = false
                                val (deleted, freed) = DownloadManager.deleteReadChapters(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                                onShowSnackbar?.invoke(strings.deleteReadChaptersSuccessMessage(deleted, DownloadManager.formatBytes(freed)))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(2.dp))

            // (c) Read Dropdown Options Segment (Incognito, First/Latest Ch, Remove from History)
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showReadDropdown = true },
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Read Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showReadDropdown,
                    onDismissRequest = { showReadDropdown = false }
                ) {
                    // 1. Incognito Reading Option
                    DropdownMenuItem(
                        text = { Text(strings.readIncognito) },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showReadDropdown = false
                            HistoryManager.startIncognitoSession(mangaUrl)
                            val ch = resumeChapter ?: firstChapter
                            if (ch != null) {
                                onChapterClick(ch, 1, 0)
                            }
                        }
                    )

                    // 2. Read from First Chapter
                    if (firstChapter != null) {
                        DropdownMenuItem(
                            text = { Text(strings.readFirstChapter) },
                            leadingIcon = { Icon(Icons.Default.SkipPrevious, contentDescription = null) },
                            onClick = {
                                showReadDropdown = false
                                onChapterClick(firstChapter, 1, 0)
                            }
                        )
                    }

                    // 3. Read Latest Chapter
                    if (latestChapter != null && latestChapter != firstChapter) {
                        DropdownMenuItem(
                            text = { Text(strings.readLatestChapter) },
                            leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                            onClick = {
                                showReadDropdown = false
                                onChapterClick(latestChapter, 1, 0)
                            }
                        )
                    }

                    // 4. Remove from History (if exists)
                    if (lastReadEntry != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text(strings.removeFromHistory, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showReadDropdown = false
                                HistoryManager.removeHistoryForManga(mangaUrl, displayTitle)
                                onShowSnackbar?.invoke(strings.removedFromHistory)
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. FAVORITE / IN LIBRARY SPLIT BUTTON GROUP
        // ==========================================
        val libContainerColor = if (isInLibrary) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        }
        val libContentColor = if (isInLibrary) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // (a) Library Primary Segment
            Surface(
                onClick = {
                    if (isInLibrary) {
                        onOpenCategoryPicker()
                    } else {
                        onAddToLibrary()
                    }
                },
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                color = libContainerColor,
                contentColor = libContentColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isInLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isInLibrary) MaterialTheme.colorScheme.primary else libContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isInLibrary) (libraryItem?.category ?: strings.inLibrary) else strings.addToLibrary,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(2.dp))

            // (b) Library Dropdown Options Segment (Categories & Remove)
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showLibraryDropdown = true },
                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 24.dp, bottomEnd = 24.dp),
                    color = libContainerColor,
                    contentColor = libContentColor,
                    modifier = Modifier
                        .width(36.dp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Library Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLibraryDropdown,
                    onDismissRequest = { showLibraryDropdown = false }
                ) {
                    Text(
                        text = strings.changeCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    LibraryManager.categories.forEach { cat ->
                        val isSelected = isInLibrary && libraryItem?.category == cat
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = {
                                showLibraryDropdown = false
                                if (!isInLibrary) {
                                    onAddToLibrary()
                                }
                                LibraryManager.updateCategory(mangaUrl, displayTitle, cat)
                            }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text(strings.changeCategory) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showLibraryDropdown = false
                            onOpenCategoryPicker()
                        }
                    )

                    if (isInLibrary) {
                        DropdownMenuItem(
                            text = { Text(strings.removeFromLibrary, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showLibraryDropdown = false
                                LibraryManager.removeFromLibrary(mangaUrl, displayTitle)
                                onShowSnackbar?.invoke(strings.removeFromLibrary)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large Screen / Desktop Dual-Pane Layout (Spacious, Clear & Beautiful)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MangaDetailsWideLayout(
    sourceId: String,
    mangaUrl: String,
    displayTitle: String,
    displayCover: String,
    mangaDetails: Manga?,
    allChapters: List<Chapter>,
    filteredChapters: List<Chapter>,
    lastReadEntry: HistoryEntry?,
    mangaStats: MangaReadingStats?,
    similarManga: List<Manga>,
    isLoadingSimilar: Boolean,
    isInLibrary: Boolean,
    libraryItem: LibraryManga?,
    chapterSearchQuery: String,
    onChapterSearchQueryChange: (String) -> Unit,
    isChaptersAscending: Boolean,
    onToggleSort: () -> Unit,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onAddToLibrary: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onGenreClick: (String) -> Unit,
    onMangaClick: ((sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit)?,
    onArtworkColorExtracted: (Color) -> Unit,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Ambient Blurred Backdrop Background
        if (displayCover.isNotBlank()) {
            AsyncImage(
                model = displayCover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp),
                alpha = 0.15f
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. MAIN INFO COLUMN (Spans the wide full-screen area, weight = 1f)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // HERO ROW: Poster + Main Info & Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Poster Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .width(200.dp)
                                .height(285.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                        ) {
                            AsyncImage(
                                model = displayCover,
                                contentDescription = displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = rememberVectorPainter(Icons.Default.BrokenImage),
                                onSuccess = { state ->
                                    extractDominantColor(state.result.image)?.let(onArtworkColorExtracted)
                                }
                            )
                        }

                        // Right of Poster: Title, Author, Badges, Action Buttons
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 285.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                // Title
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                // Alternative Title
                                mangaDetails?.altTitle?.let { alt ->
                                    if (alt.isNotBlank() && alt != displayTitle) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = alt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Author
                                mangaDetails?.author?.let { author ->
                                    if (author.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = author,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Metadata Badges: Source + Status + Rating + NSFW
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Source
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = sourceId,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    // Status
                                    val status = mangaDetails?.status ?: MangaStatus.UNKNOWN
                                    val (statusBg, statusFg) = when (status) {
                                        MangaStatus.ONGOING -> Pair(Color(0xFF2E7D32).copy(alpha = 0.2f), Color(0xFF4CAF50))
                                        MangaStatus.COMPLETED -> Pair(Color(0xFF1565C0).copy(alpha = 0.2f), Color(0xFF42A5F5))
                                        MangaStatus.DROPPED -> Pair(Color(0xFFC62828).copy(alpha = 0.2f), Color(0xFFEF5350))
                                        MangaStatus.ON_HOLD -> Pair(Color(0xFFEF6C00).copy(alpha = 0.2f), Color(0xFFFFA726))
                                        MangaStatus.UNKNOWN -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = status.name,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = statusFg,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }

                                    mangaDetails?.rating?.let { rating ->
                                        if (rating > 0f) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFFD700).copy(alpha = 0.2f)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFFB300))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = if (rating > 10f) "${(rating / 10f).formatOneDec()}/10" else "${rating.formatOneDec()}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFFFFB300)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (mangaDetails?.isNsfw == true) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = "18+",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Primary Action Split Buttons
                            MangaDetailsActionSplitButtonsBar(
                                sourceId = sourceId,
                                mangaUrl = mangaUrl,
                                displayTitle = displayTitle,
                                displayCover = displayCover,
                                mangaDetails = mangaDetails,
                                allChapters = allChapters,
                                lastReadEntry = lastReadEntry,
                                isInLibrary = isInLibrary,
                                libraryItem = libraryItem,
                                onChapterClick = onChapterClick,
                                onAddToLibrary = onAddToLibrary,
                                onOpenCategoryPicker = onOpenCategoryPicker,
                                onShowSnackbar = onShowSnackbar
                            )
                        }
                    }

                    val hasStartedReading = lastReadEntry != null || (mangaStats != null && mangaStats.chaptersRead.isNotEmpty())
                    if (hasStartedReading) {
                        Spacer(Modifier.height(20.dp))
                        // Reading Statistics Card
                        MangaReadingStatsCard(
                            stats = mangaStats,
                            totalChapters = allChapters.size,
                            lastReadEntry = lastReadEntry,
                            allChapters = allChapters,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Clickable Genres & Tags
                    mangaDetails?.tags?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Genres & Themes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier.clickable { onGenreClick(tag) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Synopsis / Description
                    mangaDetails?.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Spacer(Modifier.height(18.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Synopsis",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Similar Manga & Recommendations Carousel
                    SimilarMangaSection(
                        similarManga = similarManga,
                        isLoading = isLoadingSimilar,
                        onMangaClick = onMangaClick,
                        sourceId = sourceId,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                    )
                }
            }

            // 2. RIGHT PANE: Chapters Explorer (Fixed Compact Width: 380.dp)
            Card(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: Chapters Count + Filter Search + Sort Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Chapters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${filteredChapters.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Search in chapters
                            OutlinedTextField(
                                value = chapterSearchQuery,
                                onValueChange = onChapterSearchQueryChange,
                                placeholder = { Text("Filter...", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    if (chapterSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onChapterSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            // Sort button
                            IconButton(
                                onClick = onToggleSort,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = if (isChaptersAscending) "Oldest" else "Newest",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Chapters List
                    if (filteredChapters.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (chapterSearchQuery.isNotBlank()) "No chapters match \"$chapterSearchQuery\"" else "No chapters found",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filteredChapters,
                                key = { it.url.ifBlank { "${it.chapterNumber}_${it.title}" } }
                            ) { chapter ->
                                val isLastRead = lastReadEntry != null &&
                                        (chapter.url == lastReadEntry.chapterUrl || chapter.title == lastReadEntry.chapterTitle)

                                ChapterTileDesktop(
                                    chapter = chapter,
                                    manga = mangaDetails ?: Manga(id = mangaUrl, title = displayTitle, thumbnailUrl = displayCover, source = sourceId, url = mangaUrl, chapters = allChapters),
                                    isLastRead = isLastRead,
                                    lastReadPage = if (isLastRead) lastReadEntry?.lastPage ?: 1 else null,
                                    onClick = {
                                        val initPage = if (isLastRead) (lastReadEntry?.lastPage?.coerceAtLeast(1) ?: 1) else 1
                                        val initOffset = if (isLastRead) (lastReadEntry?.scrollOffset ?: 0) else 0
                                        onChapterClick(chapter, initPage, initOffset)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Desktop Chapter Tile
 */
@Composable
private fun ChapterTileDesktop(
    chapter: Chapter,
    manga: Manga,
    isLastRead: Boolean,
    lastReadPage: Int?,
    onClick: () -> Unit
) {
    val scanlator = chapter.scanlator?.takeIf { it.isNotBlank() }
    val dateStr = chapter.uploadDate?.takeIf { it > 0 }?.let { formatTimestamp(it) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isLastRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isLastRead) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Chapter Number Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Ch. ${if (chapter.chapterNumber % 1 == 0f) chapter.chapterNumber.toInt().toString() else chapter.chapterNumber.toString()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isLastRead) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (chapter.title.isNotBlank()) chapter.title else "Chapter ${chapter.chapterNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isLastRead) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isLastRead && lastReadPage != null && lastReadPage > 1) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "page $lastReadPage",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (scanlator != null || dateStr != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            scanlator?.let { scan ->
                                Text(
                                    text = scan,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            if (scanlator != null && dateStr != null) {
                                Text("•", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 10.sp)
                            }
                            dateStr?.let { d ->
                                Text(
                                    text = d,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChapterDownloadButton(chapter = chapter, manga = manga)
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Read",
                    tint = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Mobile / Phone Compact Layout
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MangaDetailsCompactLayout(
    sourceId: String,
    mangaUrl: String,
    displayTitle: String,
    displayCover: String,
    mangaDetails: Manga?,
    allChapters: List<Chapter>,
    lastReadEntry: HistoryEntry?,
    mangaStats: MangaReadingStats?,
    similarManga: List<Manga>,
    isLoadingSimilar: Boolean,
    isInLibrary: Boolean,
    libraryItem: LibraryManga?,
    isChaptersAscending: Boolean,
    isDescriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onOpenChaptersBottomSheet: () -> Unit,
    onAddToLibrary: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onGenreClick: (String) -> Unit,
    onMangaClick: ((sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit)?,
    onArtworkColorExtracted: (Color) -> Unit,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    val strings = Strings.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 1. HERO HEADER: Backdrop + Poster + Metadata
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Blurred Background Backdrop
                if (displayCover.isNotBlank()) {
                    AsyncImage(
                        model = displayCover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp),
                        onSuccess = { state ->
                            extractDominantColor(state.result.image)?.let(onArtworkColorExtracted)
                        }
                    )
                }

                // Gradient Scrim Overlay fading to theme background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Foreground Row: Poster Card + Core Metadata Details
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Elevated Poster Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(120.dp)
                            .height(175.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        AsyncImage(
                            model = displayCover,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            error = rememberVectorPainter(Icons.Default.BrokenImage),
                            onSuccess = { state ->
                                extractDominantColor(state.result.image)?.let(onArtworkColorExtracted)
                            }
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    // Title, Author, Status, Rating Badges
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        mangaDetails?.author?.let { author ->
                            if (author.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Badges: Status + Rating + NSFW
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            mangaDetails?.status?.let { status ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = status.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            mangaDetails?.rating?.let { rating ->
                                if (rating > 0f) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFB300).copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = rating.formatOneDec(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = Color(0xFFFFB300)
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

        // 2. ACTION BUTTONS: Split Buttons (Read + Download + Library)
        item {
            MangaDetailsActionSplitButtonsBar(
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                displayTitle = displayTitle,
                displayCover = displayCover,
                mangaDetails = mangaDetails,
                allChapters = allChapters,
                lastReadEntry = lastReadEntry,
                isInLibrary = isInLibrary,
                libraryItem = libraryItem,
                onChapterClick = onChapterClick,
                onAddToLibrary = onAddToLibrary,
                onOpenCategoryPicker = onOpenCategoryPicker,
                onShowSnackbar = onShowSnackbar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 3. READING STATISTICS CARD (ONLY IF USER HAS STARTED READING)
        val hasStartedReading = lastReadEntry != null || (mangaStats != null && mangaStats.chaptersRead.isNotEmpty())
        if (hasStartedReading) {
            item {
                MangaReadingStatsCard(
                    stats = mangaStats,
                    totalChapters = allChapters.size,
                    lastReadEntry = lastReadEntry,
                    allChapters = allChapters
                )
            }
        }

        // 4. CHAPTERS TRIGGER CARD (OPENS BOTTOM SHEET ON COMPACT SCREENS)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onOpenChaptersBottomSheet() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = strings.chapters,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${allChapters.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            lastReadEntry?.let { last ->
                                Text(
                                    text = "${strings.lastRead}: ${last.chapterTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } ?: run {
                                Text(
                                    text = if (isChaptersAscending) "الأقدم أولاً" else "الأحدث أولاً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = onOpenChaptersBottomSheet,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = strings.browse,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 5. GENRES & CLICKABLE TAGS
        mangaDetails?.tags?.let { tags ->
            if (tags.isNotEmpty()) {
                item {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onGenreClick(tag) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. SYNOPSIS / DESCRIPTION
        mangaDetails?.description?.let { desc ->
            if (desc.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(onClick = onToggleDescription),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Synopsis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isDescriptionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )
                        }
                    }
                }
            }
        }

        // 7. SIMILAR MANGA & RECOMMENDATIONS CAROUSEL
        item {
            SimilarMangaSection(
                similarManga = similarManga,
                isLoading = isLoadingSimilar,
                onMangaClick = onMangaClick,
                sourceId = sourceId
            )
        }
    }
}

/**
 * Chapters Modal Bottom Sheet for Small / Mobile Screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MangaChaptersModalBottomSheet(
    sourceId: String,
    mangaUrl: String,
    displayTitle: String,
    displayCover: String,
    mangaDetails: Manga?,
    allChapters: List<Chapter>,
    filteredChapters: List<Chapter>,
    lastReadEntry: HistoryEntry?,
    mangaStats: MangaReadingStats?,
    chapterSearchQuery: String,
    onChapterSearchQueryChange: (String) -> Unit,
    isChapterSearchActive: Boolean,
    onToggleChapterSearch: () -> Unit,
    isChaptersAscending: Boolean,
    onToggleSort: () -> Unit,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onDismissRequest: () -> Unit,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    val strings = Strings.current
    var showBatchMenu by remember { mutableStateOf(false) }

    val targetManga = mangaDetails ?: Manga(
        id = mangaUrl,
        title = displayTitle,
        thumbnailUrl = displayCover,
        source = sourceId,
        url = mangaUrl,
        chapters = allChapters
    )

    val unreadChapters = remember(allChapters, mangaStats) {
        val readUrls = mangaStats?.chaptersRead ?: emptySet()
        allChapters.filter { it.url !in readUrls && !DownloadManager.isChapterDownloaded(it.url) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = strings.chapters,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${filteredChapters.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Search toggle
                    IconButton(onClick = onToggleChapterSearch) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Chapters",
                            tint = if (isChapterSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Sort toggle
                    IconButton(onClick = onToggleSort) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = if (isChaptersAscending) "Oldest first" else "Newest first",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Batch download menu
                    Box {
                        IconButton(onClick = { showBatchMenu = true }) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = strings.batchDownloadTitle,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showBatchMenu,
                            onDismissRequest = { showBatchMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.downloadNextCount(1)) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showBatchMenu = false
                                    val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(1)
                                    DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.downloadNextCount(5)) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showBatchMenu = false
                                    val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(5)
                                    DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.downloadNextCount(10)) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showBatchMenu = false
                                    val toDownload = (if (unreadChapters.isNotEmpty()) unreadChapters else allChapters).take(10)
                                    DownloadManager.enqueueBatchDownload(targetManga, toDownload)
                                }
                            )
                            if (unreadChapters.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(strings.downloadAllUnread) },
                                    leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                    onClick = {
                                        showBatchMenu = false
                                        DownloadManager.enqueueBatchDownload(targetManga, unreadChapters)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(strings.downloadAllChapters) },
                                leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null) },
                                onClick = {
                                    showBatchMenu = false
                                    DownloadManager.enqueueBatchDownload(targetManga, allChapters)
                                }
                            )

                            val (readDownloadCount, _) = remember(allChapters, DownloadManager.downloadedChapters.size) {
                                DownloadManager.getReadDownloadedChaptersCount(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                            }
                            if (readDownloadCount > 0) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("${strings.deleteReadChaptersForManga} ($readDownloadCount)") },
                                    leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showBatchMenu = false
                                        val (deleted, freed) = DownloadManager.deleteReadChapters(mangaUrl = mangaUrl, mangaTitle = displayTitle)
                                        if (deleted > 0) {
                                            val msg = strings.deleteReadChaptersSuccessMessage(deleted, DownloadManager.formatBytes(freed))
                                            onShowSnackbar?.invoke(msg)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (if active)
            AnimatedVisibility(visible = isChapterSearchActive) {
                OutlinedTextField(
                    value = chapterSearchQuery,
                    onValueChange = onChapterSearchQueryChange,
                    placeholder = { Text("Search chapters...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (chapterSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onChapterSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Chapters List
            if (filteredChapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chapterSearchQuery.isNotBlank()) "No chapters matching \"$chapterSearchQuery\"" else "No chapters found",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = filteredChapters,
                        key = { it.id.ifBlank { it.url } }
                    ) { chapter ->
                        val isLastRead = lastReadEntry != null && (chapter.url == lastReadEntry.chapterUrl || chapter.title == lastReadEntry.chapterTitle)
                        val isRead = isLastRead || (mangaStats != null && chapter.url in mangaStats.chaptersRead)

                        Surface(
                            onClick = { onChapterClick(chapter, if (isLastRead) lastReadEntry.lastPage.coerceAtLeast(1) else 1, if (isLastRead) lastReadEntry.scrollOffset else 0) },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isLastRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isLastRead) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isLastRead) MaterialTheme.colorScheme.primary else if (isRead) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isLastRead) {
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = if (lastReadEntry != null && lastReadEntry.lastPage > 1) "READING (p. ${lastReadEntry.lastPage})" else "READING",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    val scanlator = chapter.scanlator?.takeIf { it.isNotBlank() }
                                    val dateStr = chapter.uploadDate?.let { formatTimestamp(it) }

                                    if (scanlator != null || dateStr != null) {
                                        Spacer(Modifier.height(3.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            scanlator?.let { scan ->
                                                Text(
                                                    text = scan,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            if (scanlator != null && dateStr != null) {
                                                Text("•", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 10.sp)
                                            }
                                            dateStr?.let { d ->
                                                Text(
                                                    text = d,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChapterDownloadButton(
                                        chapter = chapter,
                                        manga = targetManga
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

private fun Float.formatOneDec(): String {
    val rounded = (this * 10).toInt() / 10f
    return if (rounded % 1 == 0f) "${rounded.toInt()}" else "$rounded"
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diff = currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 30 -> "${days / 30}mo ago"
        days > 0 -> "${days}d ago"
        hours > 0 -> "${hours}h ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}

/**
 * Reading Statistics Progress Card
 */
@Composable
private fun MangaReadingStatsCard(
    stats: MangaReadingStats?,
    totalChapters: Int,
    lastReadEntry: HistoryEntry?,
    allChapters: List<Chapter>,
    modifier: Modifier = Modifier
) {
    val strings = Strings.current
    val readChaptersCount = stats?.chaptersRead?.size ?: (if (lastReadEntry != null) 1 else 0)
    val effectiveTotal = if (totalChapters > 0) totalChapters else allChapters.size
    val progress = if (effectiveTotal > 0) {
        (readChaptersCount.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val percentInt = (progress * 100).toInt()

    val pagesRead = stats?.totalPagesRead ?: (lastReadEntry?.lastPage ?: 0)
    val timeSpentSeconds = stats?.totalTimeSeconds ?: 0L
    val timeSpentText = if (timeSpentSeconds >= 3600) {
        val hours = timeSpentSeconds / 3600
        val mins = (timeSpentSeconds % 3600) / 60
        "${hours}h ${mins}m"
    } else if (timeSpentSeconds >= 60) {
        "${timeSpentSeconds / 60}m"
    } else if (timeSpentSeconds > 0) {
        "${timeSpentSeconds}s"
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon + Title + Percent Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.readingProgressTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = strings.percentCompleted(percentInt),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Animated Linear Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // 3-Column Quick Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chapters Read metric
                Column {
                    Text(
                        text = strings.readCountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (effectiveTotal > 0) "$readChaptersCount / $effectiveTotal" else "$readChaptersCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pages Read metric
                if (pagesRead > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.pagesReadCount(pagesRead),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Time Spent metric
                if (timeSpentText != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.timeSpentOnManga(timeSpentText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Recommendations / Similar Manga Horizontal Carousel Section
 */
@Composable
private fun SimilarMangaSection(
    similarManga: List<Manga>,
    isLoading: Boolean,
    onMangaClick: ((sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit)?,
    sourceId: String,
    modifier: Modifier = Modifier
) {
    val strings = Strings.current

    if (isLoading || similarManga.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.similarMangaTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading && similarManga.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    similarManga.forEach { item ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(190.dp)
                        ) {
                            MaterialYouMangaPosterCard(
                                manga = item,
                                onClick = {
                                    onMangaClick?.invoke(sourceId, item.url, item.title, item.thumbnailUrl)
                                },
                                cornerRadiusDp = 12,
                                showSourceBadge = false,
                                showRatingBadge = true
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chapter Download Action Button with live download states
 */
@Composable
private fun ChapterDownloadButton(
    chapter: Chapter,
    manga: Manga,
    modifier: Modifier = Modifier
) {
    val isDownloaded = remember(chapter.url, DownloadManager.downloadedChapters.keys.contains(chapter.url)) {
        DownloadManager.isChapterDownloaded(chapter.url)
    }
    val task = DownloadManager.downloadTasks.firstOrNull { it.id == chapter.url }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isDownloaded -> {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            task?.status == DownloadStatus.DOWNLOADING -> {
                IconButton(
                    onClick = { DownloadManager.pauseTask(chapter.url) },
                    modifier = Modifier.size(36.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { task.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            task?.status == DownloadStatus.QUEUED -> {
                IconButton(
                    onClick = { DownloadManager.cancelTask(chapter.url) },
                    modifier = Modifier.size(36.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            task?.status == DownloadStatus.PAUSED -> {
                IconButton(
                    onClick = { DownloadManager.resumeTask(chapter.url) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume Download",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            task?.status == DownloadStatus.ERROR -> {
                IconButton(
                    onClick = {
                        DownloadManager.enqueueDownload(manga, chapter)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Download Failed - Tap to Retry",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                IconButton(
                    onClick = { DownloadManager.enqueueDownload(manga, chapter) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Download Chapter",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        val strings = Strings.current
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteDownload) },
            text = { Text(strings.deleteDownloadConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    DownloadManager.deleteDownloadedChapter(chapter.url)
                    showDeleteDialog = false
                }) {
                    Text(strings.clear, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
