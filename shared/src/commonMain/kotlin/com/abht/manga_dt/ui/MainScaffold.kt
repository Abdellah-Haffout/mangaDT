package com.abht.manga_dt.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.data.MangaSource
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.Strings
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
    onNavigateToSync: () -> Unit = {}
) {
    val screens = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Updates,
        Screen.Browse,
        Screen.Profile
    )

    val pagerState = rememberPagerState(pageCount = { screens.size })
    val coroutineScope = rememberCoroutineScope()
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

    LaunchedEffect(Unit) {
        if (sources.isEmpty()) {
            sources = sourceManager.getAvailableSources()
        }
    }

    @Composable
    fun PersistentTopSearchBar() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSearchActive) 0.dp else 16.dp, vertical = if (isSearchActive) 0.dp else 6.dp)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* Execute search */ },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
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
                                onOpenSettings = onNavigateToSettings
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
    } else {
        // Mobile / Compact Bottom NavigationBar
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    screens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.getLabel()) },
                            label = { Text(screen.getLabel()) },
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
        ) { paddingValues ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Persistent Top Search Bar
                PersistentTopSearchBar()

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
                                onOpenSettings = onNavigateToSettings
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
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
