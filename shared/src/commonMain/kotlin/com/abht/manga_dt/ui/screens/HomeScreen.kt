package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.data.MangaSource
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus
import com.abht.manga_dt.ui.components.*
import com.abht.manga_dt.ui.models.LayoutMode
import kotlinx.coroutines.launch

enum class HomeCategoryType(val icon: ImageVector) {
    POPULAR(Icons.Default.Whatshot),
    LATEST(Icons.Default.NewReleases),
    FAVORITES(Icons.Default.Favorite);

    @Composable
    fun getLabel(): String = when (this) {
        POPULAR -> Strings.current.popular
        LATEST -> Strings.current.latest
        FAVORITES -> Strings.current.favorites
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sources: List<MangaSource>,
    onMangaClick: (sourceId: String, mangaUrl: String, title: String, coverUrl: String) -> Unit,
    onNavigateToSource: (MangaSource) -> Unit = {},
    onHistoryItemClick: (HistoryEntry) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onExploreMoreClick: (() -> Unit)? = null
) {
    val strings = Strings.current
    val coroutineScope = rememberCoroutineScope()
    val sourceManager = remember { MangaSourceManager() }

    var selectedCategoryType by remember {
        mutableStateOf(
            runCatching { HomeCategoryType.valueOf(AppSettings.homeDefaultCategory) }.getOrDefault(HomeCategoryType.POPULAR)
        )
    }
    var selectedGenre by remember { mutableStateOf(strings.genre) }
    var selectedLanguage by remember { mutableStateOf(strings.language) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }

    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showGenreDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showLayoutGridSizeDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showGenreDialog) { showGenreDialog = false }
    BackHandler(enabled = showLangDialog) { showLangDialog = false }
    BackHandler(enabled = showSourceDialog) { showSourceDialog = false }
    BackHandler(enabled = showLayoutGridSizeDialog) { showLayoutGridSizeDialog = false }

    val historyItems = HistoryManager.historyEntries
    val libraryItems = LibraryManager.libraryItems
    val favoriteItems = remember(libraryItems) {
        libraryItems.filter { it.category.equals("Favorites", ignoreCase = true) || it.category.equals("المفضلة", ignoreCase = true) }
    }

    val isNsfwAllowed = AppSettings.isNsfwAllowed

    val filteredSources = remember(sources, isNsfwAllowed) {
        if (!isNsfwAllowed) {
            sources.filter { it.contentType != "HENTAI" && !it.name.contains("hentai", ignoreCase = true) && !it.id.contains("hentai", ignoreCase = true) }
        } else sources
    }

    // Determine target source
    val activeSource = remember(filteredSources, selectedSourceId, AppSettings.pinnedSourceIds) {
        if (selectedSourceId != null) {
            filteredSources.firstOrNull { it.id == selectedSourceId }
        } else {
            filteredSources.firstOrNull { it.id in AppSettings.pinnedSourceIds } ?: filteredSources.firstOrNull()
        }
    }

    fun loadHomeData() {
        coroutineScope.launch {
            if (activeSource == null) return@launch
            isLoading = true
            errorMessage = null
            try {
                val list = when (selectedCategoryType) {
                    HomeCategoryType.LATEST -> sourceManager.getPopularManga(activeSource.id, 2)
                    HomeCategoryType.POPULAR -> sourceManager.getPopularManga(activeSource.id, 1)
                    HomeCategoryType.FAVORITES -> emptyList()
                }
                mangaList = list
            } catch (e: Exception) {
                errorMessage = e.message ?: strings.noMangaFound
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeSource?.id, selectedCategoryType, isNsfwAllowed) {
        if (selectedCategoryType != HomeCategoryType.FAVORITES) {
            loadHomeData()
        }
    }

    // Filter manga for Home Screen
    val displayedManga = remember(mangaList, selectedCategoryType, favoriteItems, selectedGenre, isNsfwAllowed) {
        val baseList = if (selectedCategoryType == HomeCategoryType.FAVORITES) {
            favoriteItems.map { lib ->
                Manga(
                    id = lib.id,
                    title = lib.title,
                    thumbnailUrl = lib.thumbnailUrl,
                    source = lib.sourceId,
                    url = lib.mangaUrl,
                    author = lib.author,
                    status = lib.status
                )
            }
        } else {
            var result = mangaList
            if (selectedGenre != strings.genre && selectedGenre != "All" && selectedGenre != "الكل") {
                result = result.filter { manga ->
                    manga.tags.any { it.contains(selectedGenre, ignoreCase = true) } ||
                    manga.description?.contains(selectedGenre, ignoreCase = true) == true
                }
            }
            result
        }

        if (!isNsfwAllowed) {
            val nsfwTags = setOf("hentai", "adult", "pornographic", "smut", "erotica", "18+", "nsfw")
            baseList.filterNot { manga ->
                manga.isNsfw || manga.tags.any { tag -> nsfwTags.contains(tag.lowercase().trim()) }
            }
        } else {
            baseList
        }
    }

    val availableCategories = remember(AppSettings.homeShowFavoritesTab) {
        if (AppSettings.homeShowFavoritesTab) {
            HomeCategoryType.values().toList()
        } else {
            listOf(HomeCategoryType.POPULAR, HomeCategoryType.LATEST)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // CATEGORIES & FILTER ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Capsules: [ الأكثر شعبية | أحدث الفصول | المفضلة ]
                availableCategories.forEach { catType ->
                    MaterialYouCategoryCapsule(
                        label = catType.getLabel(),
                        isSelected = selectedCategoryType == catType,
                        onClick = { selectedCategoryType = catType },
                        icon = catType.icon
                    )
                }

                if (AppSettings.homeShowFilterPills) {
                    Spacer(Modifier.width(4.dp))

                    // Layout & Grid Size Pill ("العرض والشبكة")
                    MaterialYouFilterDropdownPill(
                        label = if (AppSettings.homeGridColumns > 0) strings.gridColumnsCountLabel(AppSettings.homeGridColumns) else strings.gridSizeAndLayout,
                        isSelected = AppSettings.homeGridColumns > 0,
                        onClick = { showLayoutGridSizeDialog = true },
                        icon = Icons.Default.GridView,
                        showDropDownArrow = true
                    )

                    // Source Selector Pill ("المصدر")
                    MaterialYouFilterDropdownPill(
                        label = activeSource?.name ?: strings.sources,
                        isSelected = selectedSourceId != null,
                        onClick = { showSourceDialog = true }
                    )

                    // Genre / Tag Filter Pill ("التصنيف")
                    MaterialYouFilterDropdownPill(
                        label = selectedGenre,
                        isSelected = selectedGenre != strings.genre,
                        onClick = { showGenreDialog = true }
                    )

                    // Language Filter Pill ("اللغة")
                    MaterialYouFilterDropdownPill(
                        label = selectedLanguage,
                        isSelected = selectedLanguage != strings.language,
                        onClick = { showLangDialog = true }
                    )

                    // Explore More Manga Pill ("تصفح المزيد")
                    MaterialYouFilterDropdownPill(
                        label = strings.exploreMoreManga,
                        isSelected = false,
                        onClick = {
                            if (activeSource != null) {
                                onNavigateToSource(activeSource)
                            } else {
                                onExploreMoreClick?.invoke()
                            }
                        },
                        icon = Icons.Default.Explore,
                        showDropDownArrow = false
                    )
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // MANGA GRID CONTENT & DISCOVERY
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && mangaList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = strings.loadingManga,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else if (errorMessage != null && mangaList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.padding(24.dp).fillMaxWidth(0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(text = errorMessage ?: strings.noMangaFound, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                Button(onClick = { loadHomeData() }) {
                                    Text(strings.retry)
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Continue Reading Shortcuts Row (if enabled and history exists)
                        if (AppSettings.homeShowContinueReading && historyItems.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                                MaterialYouSectionHeader(
                                    title = strings.continueReading,
                                    badgeCount = historyItems.size
                                )
                                Spacer(Modifier.height(4.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(historyItems.take(AppSettings.homeContinueReadingCount)) { entry ->
                                        MaterialYouContinueReadingCard(
                                            entry = entry,
                                            onClick = {
                                                onMangaClick(entry.sourceId, entry.mangaUrl, entry.mangaTitle, entry.mangaCover)
                                            },
                                            onResumeClick = {
                                                onHistoryItemClick(entry)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Main Manga Grid / List Content
                        if (displayedManga.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = strings.noMangaFound,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val bottomListPadding = if (AppSettings.floatingNavBar) 88.dp else 16.dp
                            when (AppSettings.homeLayoutMode) {
                                com.abht.manga_dt.ui.models.LayoutMode.LIST -> {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomListPadding),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = displayedManga,
                                            key = { "${it.source}_${it.url}_${it.title}" }
                                        ) { manga ->
                                            MaterialYouMangaListCard(
                                                manga = manga,
                                                onClick = {
                                                    onMangaClick(manga.source, manga.url, manga.title, manga.thumbnailUrl)
                                                },
                                                cornerRadiusDp = AppSettings.homeCardCornersDp
                                            )
                                        }

                                        item {
                                            ExploreMoreMangaBanner(
                                                activeSourceName = activeSource?.name,
                                                onBrowseSource = {
                                                    if (activeSource != null) onNavigateToSource(activeSource)
                                                    else onExploreMoreClick?.invoke()
                                                },
                                                onBrowseAllSources = { onExploreMoreClick?.invoke() }
                                            )
                                        }
                                    }
                                }
                                com.abht.manga_dt.ui.models.LayoutMode.COMPACT -> {
                                    val gridColumns = if (AppSettings.homeGridColumns > 0) {
                                        GridCells.Fixed(AppSettings.homeGridColumns)
                                    } else {
                                        GridCells.Adaptive(minSize = 108.dp)
                                    }
                                    LazyVerticalGrid(
                                        columns = gridColumns,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomListPadding),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = displayedManga,
                                            key = { "${it.source}_${it.url}_${it.title}" }
                                        ) { manga ->
                                            MaterialYouMangaPosterCard(
                                                manga = manga,
                                                onClick = {
                                                    onMangaClick(manga.source, manga.url, manga.title, manga.thumbnailUrl)
                                                },
                                                cornerRadiusDp = (AppSettings.homeCardCornersDp - 4).coerceAtLeast(4),
                                                showSourceBadge = AppSettings.homeShowSourceBadge,
                                                showRatingBadge = AppSettings.homeShowRatingBadge
                                            )
                                        }

                                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                            ExploreMoreMangaBanner(
                                                activeSourceName = activeSource?.name,
                                                onBrowseSource = {
                                                    if (activeSource != null) onNavigateToSource(activeSource)
                                                    else onExploreMoreClick?.invoke()
                                                },
                                                onBrowseAllSources = { onExploreMoreClick?.invoke() }
                                            )
                                        }
                                    }
                                }
                                com.abht.manga_dt.ui.models.LayoutMode.COMFORTABLE -> {
                                    val gridColumns = if (AppSettings.homeGridColumns > 0) {
                                        GridCells.Fixed(AppSettings.homeGridColumns)
                                    } else {
                                        GridCells.Adaptive(minSize = 145.dp)
                                    }
                                    LazyVerticalGrid(
                                        columns = gridColumns,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomListPadding),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(
                                            items = displayedManga,
                                            key = { "${it.source}_${it.url}_${it.title}" }
                                        ) { manga ->
                                            MaterialYouMangaPosterCard(
                                                manga = manga,
                                                onClick = {
                                                    onMangaClick(manga.source, manga.url, manga.title, manga.thumbnailUrl)
                                                },
                                                cornerRadiusDp = AppSettings.homeCardCornersDp,
                                                showSourceBadge = AppSettings.homeShowSourceBadge,
                                                showRatingBadge = AppSettings.homeShowRatingBadge
                                            )
                                        }

                                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                            ExploreMoreMangaBanner(
                                                activeSourceName = activeSource?.name,
                                                onBrowseSource = {
                                                    if (activeSource != null) onNavigateToSource(activeSource)
                                                    else onExploreMoreClick?.invoke()
                                                },
                                                onBrowseAllSources = { onExploreMoreClick?.invoke() }
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

        // Genre Selector Dialog
        if (showGenreDialog) {
            val genres = listOf(strings.genre, "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Isekai", "Romance", "Sci-Fi", "Shonen", "Supernatural")
            AlertDialog(
                onDismissRequest = { showGenreDialog = false },
                title = { Text(strings.selectGenreTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        genres.forEach { genre ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedGenre = genre
                                        showGenreDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedGenre == genre, onClick = { selectedGenre = genre; showGenreDialog = false })
                                Spacer(Modifier.width(8.dp))
                                Text(genre, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showGenreDialog = false }) { Text(strings.close) } }
            )
        }

        // Language Selector Dialog
        if (showLangDialog) {
            val languages = listOf(strings.language, "EN", "AR", "JA", "FR", "ES", "RU", "ID")
            AlertDialog(
                onDismissRequest = { showLangDialog = false },
                title = { Text(strings.selectLanguageTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        languages.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLanguage = lang
                                        showLangDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedLanguage == lang, onClick = { selectedLanguage = lang; showLangDialog = false })
                                Spacer(Modifier.width(8.dp))
                                Text(lang, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showLangDialog = false }) { Text(strings.close) } }
            )
        }

        // Source Selector Dialog
        if (showSourceDialog) {
            AlertDialog(
                onDismissRequest = { showSourceDialog = false },
                title = { Text(strings.selectSourceTitle, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        filteredSources.forEach { src ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSourceId = src.id
                                        showSourceDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = activeSource?.id == src.id,
                                    onClick = {
                                        selectedSourceId = src.id
                                        showSourceDialog = false
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(src.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text((src.locale ?: "EN").uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showSourceDialog = false }) { Text(strings.close) } }
            )
        }

        // Layout & Grid Size Bottom Sheet
        if (showLayoutGridSizeDialog) {
            ModalBottomSheet(
                onDismissRequest = { showLayoutGridSizeDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = strings.gridSizeAndLayout,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showLayoutGridSizeDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Layout Mode Selector
                    Text(
                        text = strings.selectLayoutTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple(LayoutMode.COMFORTABLE, strings.layoutComfortable, Icons.Default.GridView),
                            Triple(LayoutMode.COMPACT, strings.layoutCompact, Icons.Default.ViewModule),
                            Triple(LayoutMode.LIST, strings.layoutList, Icons.AutoMirrored.Filled.ViewList)
                        )
                        modes.forEach { (mode, label, icon) ->
                            val isSelected = AppSettings.homeLayoutMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { AppSettings.updateHomeLayoutMode(mode) },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (AppSettings.homeLayoutMode != LayoutMode.LIST) {
                        Spacer(Modifier.height(20.dp))

                        // Columns Count / Grid Size
                        Text(
                            text = strings.gridColumnsCount,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))

                        val columnOptions = listOf(
                            0 to strings.gridColumnsAuto,
                            2 to "2",
                            3 to "3",
                            4 to "4",
                            5 to "5",
                            6 to "6"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columnOptions.forEach { (cols, label) ->
                                val isSelected = AppSettings.homeGridColumns == cols
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { AppSettings.updateHomeGridColumns(cols) },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Explore More Manga Banner Card
 */
@Composable
fun ExploreMoreMangaBanner(
    activeSourceName: String?,
    onBrowseSource: () -> Unit,
    onBrowseAllSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = Strings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.exploreMoreManga,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = activeSourceName?.let { "$it • ${strings.browseAllManga}" } ?: strings.exploreSources,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeSourceName != null) {
                    Button(
                        onClick = onBrowseSource,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.browseAllManga)
                    }
                }
                OutlinedButton(
                    onClick = onBrowseAllSources,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.exploreSources)
                }
            }
        }
    }
}
