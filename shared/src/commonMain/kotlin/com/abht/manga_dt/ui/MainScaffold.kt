package com.abht.manga_dt.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.DownloadManager
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.data.MangaSource
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.data.currentTimeMillis
import com.abht.manga_dt.models.DownloadStatus
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.ui.components.*
import com.abht.manga_dt.ui.models.LayoutMode
import com.abht.manga_dt.ui.screens.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector) {
    object Home : Screen("home", Icons.Default.Home)
    object Library : Screen("library", Icons.Default.CollectionsBookmark)
    object Updates : Screen("updates", Icons.Default.NewReleases)
    object Browse : Screen("browse", Icons.Default.Explore)
    object Profile : Screen("profile", Icons.Default.Person)

    @Composable
    fun getLabel(): String = when (this) {
        Home -> Strings.current.home
        Library -> Strings.current.library
        Updates -> Strings.current.updates
        Browse -> Strings.current.browse
        Profile -> Strings.current.profile
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    isExpanded: Boolean = false,
    onNavigateToSource: (MangaSource) -> Unit,
    onNavigateToMangaDetails: (sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit = { _, _, _, _ -> },
    onNavigateToReader: (mangaId: String, chapterId: String) -> Unit = { _, _ -> },
    onNavigateToHistoryItem: (HistoryEntry) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToProfileStats: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {}
) {
    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Updates,
        Screen.Browse,
        Screen.Profile
    )

    val initialPageIndex = remember {
        when (AppSettings.defaultAppTab.uppercase()) {
            "LIBRARY" -> 1
            "UPDATES" -> 2
            "BROWSE" -> 3
            "PROFILE" -> 4
            else -> 0
        }
    }

    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { screens.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val sourceManager = remember { MangaSourceManager() }
    var sources by remember { mutableStateOf(com.abht.manga_dt.data.MangaDataCache.cachedSources ?: emptyList()) }
    var isSidebarExpanded by remember { mutableStateOf(false) }

    val strings = Strings.current

    // Global Search State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf(HomeSearchMode.MANGA) }
    var onlineSearchResults by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    // Screen specific action states
    var showLibrarySortSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val historyItems = HistoryManager.historyEntries
    val libraryItems = LibraryManager.libraryItems
    val isNsfwAllowed = AppSettings.isNsfwAllowed

    val filteredSources = remember(sources, isNsfwAllowed) {
        if (!isNsfwAllowed) {
            sources.filter { it.contentType != "HENTAI" && !it.name.contains("hentai", ignoreCase = true) && !it.id.contains("hentai", ignoreCase = true) }
        } else sources
    }

    val activeSource = remember(filteredSources, AppSettings.pinnedSourceIds) {
        filteredSources.firstOrNull { it.id in AppSettings.pinnedSourceIds } ?: filteredSources.firstOrNull()
    }

    // --- Back Handling for Main Screen Quality of Life ---
    // 1. If Search is active, back exits search
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    // 2. If on any tab other than Home (e.g. Browse, Library, Updates, Profile), back returns to Home
    BackHandler(enabled = !isSearchActive && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    // 3. Exit confirmation on Home tab
    var lastBackPressTime by remember { mutableStateOf(0L) }
    var allowDirectExit by remember { mutableStateOf(false) }
    BackHandler(enabled = !isSearchActive && pagerState.currentPage == 0 && AppSettings.exitConfirmation && !allowDirectExit) {
        val now = currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            allowDirectExit = true
        } else {
            lastBackPressTime = now
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = strings.pressBackAgainToExit,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Debounced Online Search
    var searchJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(searchQuery, searchMode, activeSource?.id, isSearchActive, isNsfwAllowed) {
        if (isSearchActive && searchMode == HomeSearchMode.MANGA && searchQuery.isNotBlank() && activeSource != null) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(350)
                isSearchingOnline = true
                try {
                    val results = sourceManager.searchManga(activeSource.id, searchQuery)
                    onlineSearchResults = results
                } catch (e: Exception) {
                    onlineSearchResults = emptyList()
                } finally {
                    isSearchingOnline = false
                }
            }
        } else {
            onlineSearchResults = emptyList()
        }
    }

    // Local Matching Library
    val localMatchingLibrary = remember(libraryItems, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else libraryItems.filter { it.title.contains(searchQuery, ignoreCase = true) || it.author?.contains(searchQuery, ignoreCase = true) == true }
    }

    // Local Matching History
    val localMatchingHistory = remember(historyItems, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else historyItems.filter { it.mangaTitle.contains(searchQuery, ignoreCase = true) || it.chapterTitle.contains(searchQuery, ignoreCase = true) }
    }

    // Matching Sources
    val matchingSources = remember(filteredSources, searchQuery) {
        if (searchQuery.isBlank()) filteredSources
        else filteredSources.filter { it.name.contains(searchQuery, ignoreCase = true) || it.locale?.contains(searchQuery, ignoreCase = true) == true }
    }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarExpanded) 260.dp else 84.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
    )

    val density = LocalDensity.current
    val topBarHeightDp = 68.dp
    val bottomBarHeightDp = 80.dp
    val topBarHeightPx = remember(density) { with(density) { topBarHeightDp.toPx() } }
    val bottomBarHeightPx = remember(density) { with(density) { bottomBarHeightDp.toPx() } }

    var topBarOffsetPx by remember { mutableFloatStateOf(0f) }
    var bottomBarOffsetPx by remember { mutableFloatStateOf(0f) }

    // When tab changes, search becomes active, or pin is enabled, reset immediately to fully visible
    LaunchedEffect(pagerState.currentPage, isSearchActive, AppSettings.pinNavigationUi) {
        topBarOffsetPx = 0f
        bottomBarOffsetPx = 0f
    }

    val nestedScrollConnection = remember(topBarHeightPx, bottomBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!AppSettings.pinNavigationUi && !isSearchActive) {
                    val delta = available.y
                    // delta < 0: finger moving UP (dragging content up, scrolling down) -> bars hide
                    // delta > 0: finger moving DOWN (dragging content down, scrolling up) -> bars reveal
                    val newTopOffset = (topBarOffsetPx + delta).coerceIn(-topBarHeightPx, 0f)
                    val newBottomOffset = (bottomBarOffsetPx - delta).coerceIn(0f, bottomBarHeightPx)
                    topBarOffsetPx = newTopOffset
                    bottomBarOffsetPx = newBottomOffset
                } else {
                    topBarOffsetPx = 0f
                    bottomBarOffsetPx = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        if (sources.isEmpty()) {
            sources = sourceManager.getAvailableSources()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PersistentTopSearchBar() {
        val currentTopBarHeightDp = with(density) {
            ((topBarHeightPx + topBarOffsetPx).coerceAtLeast(0f)).toDp()
        }
        val topBarAlpha = if (topBarHeightPx > 0f) ((topBarHeightPx + topBarOffsetPx) / topBarHeightPx).coerceIn(0f, 1f) else 1f

        val outerBoxModifier = if (isSearchActive) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .height(currentTopBarHeightDp)
                .clipToBounds()
        }

        Box(
            modifier = outerBoxModifier
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isSearchActive) Modifier.fillMaxSize() else Modifier)
                    .graphicsLayer {
                        if (!isSearchActive) {
                            translationY = topBarOffsetPx
                            alpha = topBarAlpha
                        } else {
                            translationY = 0f
                            alpha = 1f
                        }
                    }
                    .padding(
                        start = if (isSearchActive) 0.dp else 16.dp,
                        end = if (isSearchActive) 0.dp else 16.dp,
                        top = if (isSearchActive) 0.dp else 2.dp,
                        bottom = if (isSearchActive) 0.dp else 4.dp
                    )
            ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* Execute search */ },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                windowInsets = if (isSearchActive) SearchBarDefaults.windowInsets else WindowInsets(0.dp),
                placeholder = {
                    Text(
                        text = when {
                            isSearchActive -> when (searchMode) {
                                HomeSearchMode.MANGA -> strings.searchManga
                                HomeSearchMode.LOCAL -> strings.searchLocal
                                HomeSearchMode.SOURCES -> strings.searchSources
                            }
                            pagerState.currentPage == 0 -> strings.searchManga
                            pagerState.currentPage == 1 -> "${strings.library} (${libraryItems.size})"
                            pagerState.currentPage == 2 -> strings.updates
                            pagerState.currentPage == 3 -> strings.sources
                            else -> strings.appName
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                },
                leadingIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { isSearchActive = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.close,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = strings.searchManga,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val activeTasks = remember(DownloadManager.downloadTasks.size, DownloadManager.downloadTasks.map { it.status }) {
                            DownloadManager.downloadTasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                        }
                        val isDownloading = activeTasks.isNotEmpty()
                        val currentTask = activeTasks.firstOrNull { it.status == DownloadStatus.DOWNLOADING } ?: activeTasks.firstOrNull()
                        val currentProgress = currentTask?.progress ?: 0f

                        // Telegram-style Active Download Indicator
                        AnimatedVisibility(
                            visible = isDownloading,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            TelegramDownloadIndicator(
                                activeTasksCount = activeTasks.size,
                                progress = currentProgress,
                                onClick = onNavigateToDownloads
                            )
                        }

                        if (isSearchActive && searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = strings.clear,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (!isSearchActive) {
                            // Contextual Action Icons dynamically matching active Tab
                            when (pagerState.currentPage) {
                                1 -> { // Library Actions
                                    IconButton(onClick = { showLibrarySortSheet = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        val next = when (LibraryManager.layoutMode) {
                                            LayoutMode.COMFORTABLE -> LayoutMode.COMPACT
                                            LayoutMode.COMPACT -> LayoutMode.LIST
                                            LayoutMode.LIST -> LayoutMode.COMFORTABLE
                                        }
                                        LibraryManager.updateLayoutMode(next)
                                    }) {
                                        val icon = when (LibraryManager.layoutMode) {
                                            LayoutMode.COMFORTABLE -> Icons.Default.GridView
                                            LayoutMode.COMPACT -> Icons.Default.ViewCompact
                                            LayoutMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                                        }
                                        Icon(icon, contentDescription = "Layout Mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                3 -> { // Browse Actions
                                    val isAscending = AppSettings.browseSortAscending
                                    IconButton(onClick = { AppSettings.setBrowseSort(!isAscending) }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        val nextMode = when (AppSettings.browseLayoutMode) {
                                            LayoutMode.LIST -> LayoutMode.COMFORTABLE
                                            LayoutMode.COMFORTABLE -> LayoutMode.COMPACT
                                            LayoutMode.COMPACT -> LayoutMode.LIST
                                        }
                                        AppSettings.setBrowseLayout(nextMode)
                                    }) {
                                        val icon = when (AppSettings.browseLayoutMode) {
                                            LayoutMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                                            LayoutMode.COMFORTABLE -> Icons.Default.GridView
                                            LayoutMode.COMPACT -> Icons.Default.ViewCompact
                                        }
                                        Icon(icon, contentDescription = "Layout Mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Shared Three Dots Menu
                            Box {
                                IconButton(onClick = { showOptionsMenu = !showOptionsMenu }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(strings.profileAndStats) },
                                        leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onNavigateToProfileStats()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.localSync) },
                                        leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onNavigateToSync()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.downloads) },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onNavigateToDownloads()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.settings) },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        onClick = {
                                            showOptionsMenu = false
                                            onNavigateToSettings()
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // EXPANDED SEARCH OVERLAY CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    // Search Mode Switcher Chips
                    MaterialYouSearchModeChips(
                        selectedMode = searchMode,
                        onModeSelected = { searchMode = it }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Mode 1: LOCAL (Library & History)
                    if (searchMode == HomeSearchMode.LOCAL) {
                        if (searchQuery.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(8.dp))
                                    Text(strings.typeToSearchLocal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else if (localMatchingLibrary.isEmpty() && localMatchingHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noResultsFor(searchQuery), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                if (localMatchingLibrary.isNotEmpty()) {
                                    MaterialYouSectionHeader(title = strings.libraryResultsCount(localMatchingLibrary.size))
                                    Spacer(Modifier.height(6.dp))
                                    localMatchingLibrary.forEach { lib ->
                                        Card(
                                            onClick = {
                                                isSearchActive = false
                                                onNavigateToMangaDetails(lib.sourceId, lib.mangaUrl, lib.title, lib.thumbnailUrl)
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                        ) {
                                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(lib.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    Text(strings.categoryFormat(lib.category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }

                                if (localMatchingHistory.isNotEmpty()) {
                                    MaterialYouSectionHeader(title = strings.historyResultsCount(localMatchingHistory.size))
                                    Spacer(Modifier.height(6.dp))
                                    localMatchingHistory.forEach { hist ->
                                        MaterialYouContinueReadingCard(
                                            entry = hist,
                                            onClick = {
                                                isSearchActive = false
                                                onNavigateToMangaDetails(hist.sourceId, hist.mangaUrl, hist.mangaTitle, hist.mangaCover)
                                            },
                                            onResumeClick = {
                                                isSearchActive = false
                                                onNavigateToHistoryItem(hist)
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Mode 2: SOURCES
                    else if (searchMode == HomeSearchMode.SOURCES) {
                        if (matchingSources.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noResultsFor(searchQuery), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 240.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(matchingSources, key = { it.id }) { src ->
                                    Card(
                                        onClick = {
                                            isSearchActive = false
                                            onNavigateToSource(src)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = (src.locale ?: "EN").uppercase(),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(src.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(strings.languageFormat((src.locale ?: strings.all).uppercase()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Mode 3: ONLINE MANGA
                    else {
                        if (isSearchingOnline) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    Text(strings.searchingInSource(activeSource?.name ?: strings.sources), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else if (searchQuery.isBlank()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(8.dp))
                                    Text(strings.typeToSearchOnline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else if (onlineSearchResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(strings.noResultsFor(searchQuery), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 145.dp),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(onlineSearchResults, key = { "${it.source}_${it.url}_${it.title}" }) { manga ->
                                    MaterialYouMangaPosterCard(
                                        manga = manga,
                                        onClick = {
                                            isSearchActive = false
                                            onNavigateToMangaDetails(manga.source, manga.url, manga.title, manga.thumbnailUrl)
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
}

    if (isExpanded) {
        // Desktop / Large Screen: Animated Material 3 Expandable NavigationRail / Drawer
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                modifier = Modifier
                    .width(sidebarWidth)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                AnimatedContent(
                    targetState = isSidebarExpanded,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200, delayMillis = 50)) togetherWith
                        fadeOut(animationSpec = tween(120))
                    }
                ) { expanded ->
                    if (expanded) {
                        // EXPANDED VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                // Header: Brand Logo, Name & Collapse Button
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.AutoStories,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = strings.appName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Manga & Webtoon",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    IconButton(onClick = { isSidebarExpanded = false }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuOpen,
                                            contentDescription = strings.collapseSidebar,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Main Navigation Items
                                screens.forEachIndexed { index, screen ->
                                    NavigationDrawerItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.getLabel()) },
                                        label = { Text(screen.getLabel(), fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) },
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            unselectedContainerColor = Color.Transparent,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                Spacer(Modifier.height(10.dp))

                                // Shortcuts Section
                                Text(
                                    text = "${strings.favorites} & ${strings.history}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )

                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Favorite, contentDescription = strings.favorites) },
                                    label = { Text(strings.favorites) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.History, contentDescription = strings.history) },
                                    label = { Text(strings.history) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // Bottom Section: Profile, Sync & Settings
                            Column {
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Insights, contentDescription = strings.profileAndStats) },
                                    label = { Text(strings.profileAndStats) },
                                    selected = false,
                                    onClick = onNavigateToProfileStats,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Sync, contentDescription = strings.localSync) },
                                    label = { Text(strings.localSync) },
                                    selected = false,
                                    onClick = onNavigateToSync,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = strings.settings) },
                                    label = { Text(strings.settings) },
                                    selected = false,
                                    onClick = onNavigateToSettings,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        // COLLAPSED VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { isSidebarExpanded = true }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = strings.expandSidebar,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                screens.forEachIndexed { index, screen ->
                                    val isSelected = pagerState.currentPage == index
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .size(56.dp, 36.dp)
                                            .clickable {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.getLabel(),
                                                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onNavigateToProfileStats,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = strings.profileAndStats,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToSync,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = strings.localSync,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToSettings,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = strings.settings,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Content Pane
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Persistent Top Search Bar
                PersistentTopSearchBar()

                if (!isSearchActive) {
                    // Content View (Horizontal Pager)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) { pageIndex ->
                    when (screens[pageIndex]) {
                        Screen.Home -> {
                            HomeScreen(
                                sources = sources,
                                onMangaClick = onNavigateToMangaDetails,
                                onNavigateToSource = onNavigateToSource,
                                onHistoryItemClick = onNavigateToHistoryItem,
                                onOpenSettings = onNavigateToSettings,
                                onExploreMoreClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(screens.indexOf(Screen.Browse))
                                    }
                                }
                            )
                        }
                        Screen.Library -> {
                            LibraryScreen(
                                onMangaClick = { manga ->
                                    onNavigateToMangaDetails(manga.sourceId, manga.mangaUrl, manga.title, manga.thumbnailUrl)
                                },
                                onHistoryItemClick = onNavigateToHistoryItem,
                                onExploreClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(screens.indexOf(Screen.Browse))
                                    }
                                },
                                showSortSheet = showLibrarySortSheet,
                                onDismissSortSheet = { showLibrarySortSheet = false }
                            )
                        }
                        Screen.Updates -> {
                            PlaceholderScreen(Screen.Updates.getLabel())
                        }
                        Screen.Browse -> {
                            BrowseScreen(
                                sources = sources,
                                onSourceClick = onNavigateToSource
                            )
                        }
                        Screen.Profile -> {
                            ProfileStatsScreen(
                                showBackButton = false,
                                onNavigateToMangaDetails = onNavigateToMangaDetails,
                                onNavigateToReader = onNavigateToHistoryItem,
                                onNavigateToDeepAnalytics = onNavigateToProfileStats
                            )
                        }
                    }
                }
            }
        }
    }
} else {
        // Mobile / Compact Bottom NavigationBar
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                if (AppSettings.showFloatingContinueButton && historyItems.isNotEmpty() && pagerState.currentPage == 0) {
                    val latest = historyItems.first()
                    val fabAlpha = if (bottomBarHeightPx > 0f) (1f - (bottomBarOffsetPx / bottomBarHeightPx)).coerceIn(0f, 1f) else 1f
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToHistoryItem(latest) },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = {
                            Text(
                                text = latest.mangaTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.graphicsLayer {
                            translationY = bottomBarOffsetPx * 1.5f
                            alpha = fabAlpha
                        }
                    )
                }
            },
            bottomBar = {
                if (!AppSettings.floatingNavBar) {
                    val currentBottomBarHeightDp = with(density) {
                        ((bottomBarHeightPx - bottomBarOffsetPx).coerceAtLeast(0f)).toDp()
                    }
                    val bottomBarAlpha = if (bottomBarHeightPx > 0f) (1f - (bottomBarOffsetPx / bottomBarHeightPx)).coerceIn(0f, 1f) else 1f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(currentBottomBarHeightDp)
                            .clipToBounds()
                    ) {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = bottomBarOffsetPx
                                    alpha = bottomBarAlpha
                                },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            screens.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    selected = pagerState.currentPage == index,
                                    alwaysShowLabel = AppSettings.showNavBarLabels,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    icon = { Icon(screen.icon, contentDescription = screen.getLabel()) },
                                    label = if (AppSettings.showNavBarLabels) { { Text(screen.getLabel()) } } else null,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                // LAYER 1: Content Scrolling (Full height when floating navbar is active so lists visibly scroll beneath it!)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (isSearchActive) 0.dp else paddingValues.calculateTopPadding(),
                            bottom = if (AppSettings.floatingNavBar || isSearchActive) 0.dp else paddingValues.calculateBottomPadding()
                        )
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Persistent Top Search Bar
                    PersistentTopSearchBar()

                    if (!isSearchActive) {
                        // Content View (Horizontal Pager)
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true
                        ) { pageIndex ->
                            when (screens[pageIndex]) {
                                Screen.Home -> {
                                    HomeScreen(
                                        sources = sources,
                                        onMangaClick = onNavigateToMangaDetails,
                                        onNavigateToSource = onNavigateToSource,
                                        onHistoryItemClick = onNavigateToHistoryItem,
                                        onOpenSettings = onNavigateToSettings,
                                        onExploreMoreClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(screens.indexOf(Screen.Browse))
                                            }
                                        }
                                    )
                                }
                                Screen.Library -> {
                                    LibraryScreen(
                                        onMangaClick = { manga ->
                                            onNavigateToMangaDetails(manga.sourceId, manga.mangaUrl, manga.title, manga.thumbnailUrl)
                                        },
                                        onHistoryItemClick = onNavigateToHistoryItem,
                                        onExploreClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(screens.indexOf(Screen.Browse))
                                            }
                                        },
                                        showSortSheet = showLibrarySortSheet,
                                        onDismissSortSheet = { showLibrarySortSheet = false }
                                    )
                                }
                                Screen.Updates -> {
                                    PlaceholderScreen(Screen.Updates.getLabel())
                                }
                                Screen.Browse -> {
                                    BrowseScreen(
                                        sources = sources,
                                        onSourceClick = onNavigateToSource
                                    )
                                }
                                Screen.Profile -> {
                                    ProfileStatsScreen(
                                        showBackButton = false,
                                        onNavigateToMangaDetails = onNavigateToMangaDetails,
                                        onNavigateToReader = onNavigateToHistoryItem,
                                        onNavigateToDeepAnalytics = onNavigateToProfileStats
                                    )
                                }
                            }
                        }
                    }
                }

                // LAYER 2: Sleek Floating Navigation Bar Pill (Solid theme matching Popular/Latest category pills)
                if (AppSettings.floatingNavBar && !isSearchActive) {
                    val bottomBarAlpha = if (bottomBarHeightPx > 0f) (1f - (bottomBarOffsetPx / bottomBarHeightPx)).coerceIn(0f, 1f) else 1f
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 24.dp, end = 24.dp, bottom = 14.dp)
                            .navigationBarsPadding()
                            .graphicsLayer {
                                translationY = bottomBarOffsetPx * 1.5f
                                alpha = bottomBarAlpha
                            }
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            screens.forEachIndexed { index, screen ->
                                val isSelected = pagerState.currentPage == index
                                val itemColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .padding(horizontal = 14.dp, vertical = if (AppSettings.showNavBarLabels) 3.dp else 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                screen.icon,
                                                contentDescription = screen.getLabel(),
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        if (AppSettings.showNavBarLabels) {
                                            Text(
                                                text = screen.getLabel(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = itemColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

/**
 * Telegram-style animated circular download progress indicator
 */
@Composable
fun TelegramDownloadIndicator(
    activeTasksCount: Int,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Circular Progress Ring
            if (progress > 0f) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            // Downward animated arrow icon
            Icon(
                Icons.Default.ArrowDownward,
                contentDescription = "Downloads Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(15.dp)
                    .offset(y = bounceOffset.dp)
            )

            // Badge count if multiple downloads in queue
            if (activeTasksCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (activeTasksCount > 9) "9+" else "$activeTasksCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
