package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.abht.manga_dt.data.HistoryManager
import com.abht.manga_dt.data.LibraryManager
import com.abht.manga_dt.data.MangaDataCache
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.currentTimeMillis
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.ui.components.CaptchaWebViewDialog
import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.Manga
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
    onChapterClick: (chapter: Chapter, initialPage: Int, scrollOffset: Int) -> Unit
) {
    val sourceManager = remember { MangaSourceManager() }
    val coroutineScope = rememberCoroutineScope()
    val cacheKey = "$sourceId::$mangaUrl"
    val cached = MangaDataCache.cachedMangaDetails[cacheKey]

    var mangaDetails by remember(sourceId, mangaUrl) { mutableStateOf(cached) }
    var isLoading by remember(sourceId, mangaUrl) { mutableStateOf(cached == null) }
    var errorMessage by remember(sourceId, mangaUrl) { mutableStateOf<String?>(null) }
    var isChaptersAscending by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var chapterSearchQuery by remember { mutableStateOf("") }
    var isChapterSearchActive by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showCaptchaDialog by remember { mutableStateOf(false) }
    val strings = Strings.current

    fun loadDetails(forceRefresh: Boolean = false) {
        coroutineScope.launch {
            if (forceRefresh) {
                MangaDataCache.cachedMangaDetails.remove(cacheKey)
            }
            isLoading = true
            errorMessage = null
            try {
                val result = sourceManager.getMangaDetails(sourceId, mangaUrl)
                if (result != null) {
                    mangaDetails = result
                } else {
                    errorMessage = "Failed to load manga details"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "An error occurred while loading details"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sourceId, mangaUrl) {
        if (mangaDetails == null) {
            loadDetails(forceRefresh = false)
        }
    }

    val displayTitle = mangaDetails?.title?.ifBlank { initialTitle } ?: initialTitle
    val displayCover = mangaDetails?.thumbnailUrl?.ifBlank { initialCover } ?: initialCover
    val allChapters = mangaDetails?.chapters ?: emptyList()

    val metadataSeed = remember(displayTitle, displayCover) { generateSeedFromMetadata(displayTitle, displayCover) }
    var artworkSeed by remember(displayCover) { mutableStateOf<Color?>(null) }
    val activeSeedColor = artworkSeed ?: metadataSeed
    val dynamicColorScheme = rememberAnimatedDynamicColorScheme(seedColor = activeSeedColor, isDark = AppSettings.darkTheme)

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

    MaterialTheme(colorScheme = dynamicColorScheme) {
        Scaffold(
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
                    actions = {
                        // Search Chapters Toggle
                        IconButton(onClick = {
                            isChapterSearchActive = !isChapterSearchActive
                            if (!isChapterSearchActive) chapterSearchQuery = ""
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Chapters")
                        }

                        // Sort Chapters Toggle
                        IconButton(onClick = { isChaptersAscending = !isChaptersAscending }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = if (isChaptersAscending) "Sort Descending" else "Sort Ascending"
                            )
                        }

                        // Refresh Button
                        IconButton(onClick = { loadDetails(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Details")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
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
                            isInLibrary = isInLibrary,
                            libraryItem = libraryItem,
                            chapterSearchQuery = chapterSearchQuery,
                            onChapterSearchQueryChange = { chapterSearchQuery = it },
                            isChaptersAscending = isChaptersAscending,
                            onToggleSort = { isChaptersAscending = !isChaptersAscending },
                            onChapterClick = onChapterClick,
                            onAddToLibrary = ::handleAddToLibrary,
                            onOpenCategoryPicker = { showCategoryPicker = true },
                            onArtworkColorExtracted = { artworkSeed = it }
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
                            filteredChapters = filteredChapters,
                            lastReadEntry = lastReadEntry,
                            isInLibrary = isInLibrary,
                            libraryItem = libraryItem,
                            chapterSearchQuery = chapterSearchQuery,
                            onChapterSearchQueryChange = { chapterSearchQuery = it },
                            isChapterSearchActive = isChapterSearchActive,
                            isChaptersAscending = isChaptersAscending,
                            isDescriptionExpanded = isDescriptionExpanded,
                            onToggleDescription = { isDescriptionExpanded = !isDescriptionExpanded },
                            onChapterClick = onChapterClick,
                            onAddToLibrary = ::handleAddToLibrary,
                            onOpenCategoryPicker = { showCategoryPicker = true },
                            onArtworkColorExtracted = { artworkSeed = it }
                        )
                    }
                }
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
    isInLibrary: Boolean,
    libraryItem: LibraryManga?,
    chapterSearchQuery: String,
    onChapterSearchQueryChange: (String) -> Unit,
    isChaptersAscending: Boolean,
    onToggleSort: () -> Unit,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onAddToLibrary: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onArtworkColorExtracted: (Color) -> Unit
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
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. LEFT SIDEBAR: Poster, Title, Meta Badges, Action Buttons, Tags & Synopsis
            Card(
                modifier = Modifier
                    .width(360.dp)
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Elevated Large Poster Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(190.dp)
                            .height(275.dp),
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

                    Spacer(Modifier.height(16.dp))

                    // Title
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Alternative / Native Title
                    mangaDetails?.altTitle?.let { alt ->
                        if (alt.isNotBlank() && alt != displayTitle) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = alt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
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
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Badges Row: Source + Status + Rating + NSFW
                    FlowRow(
                        horizontalArrangement = Arrangement.Center,
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        mangaDetails?.rating?.let { rating ->
                            if (rating > 0f) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFFFB300))
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = if (rating > 10f) "${(rating / 10f).formatOneDec()}/10" else "${rating.formatOneDec()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFB300)
                                        )
                                    }
                                }
                            }
                        }

                        if (mangaDetails?.isNsfw == true) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "18+",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Primary Action Buttons
                    val firstChapter = allChapters.minByOrNull { it.chapterNumber }
                        ?: allChapters.lastOrNull()
                        ?: allChapters.firstOrNull()
                    val resumeChapter = if (lastReadEntry != null) {
                        allChapters.firstOrNull { it.url == lastReadEntry.chapterUrl || it.title == lastReadEntry.chapterTitle } ?: firstChapter
                    } else {
                        firstChapter
                    }

                    Button(
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
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (lastReadEntry != null) {
                                val chName = lastReadEntry.chapterTitle.replace(Regex("(?i)chapter\\s*"), "").trim()
                                if (lastReadEntry.lastPage > 1) "Resume Ch. $chName (p. ${lastReadEntry.lastPage})" else "Resume Ch. $chName"
                            } else "Start Reading",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    if (isInLibrary) {
                        FilledTonalButton(
                            onClick = onOpenCategoryPicker,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(libraryItem?.category ?: "In Library")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onAddToLibrary,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add to Library")
                        }
                    }

                    // Genres & Tags
                    mangaDetails?.tags?.let { tags ->
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Synopsis / Description
                    mangaDetails?.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "Synopsis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. RIGHT PANE: Chapters Explorer
            Card(
                modifier = Modifier
                    .weight(1f)
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
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Chapters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${filteredChapters.size}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Search in chapters
                            OutlinedTextField(
                                value = chapterSearchQuery,
                                onValueChange = onChapterSearchQueryChange,
                                placeholder = { Text("Filter chapters...", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    if (chapterSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onChapterSearchQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            // Sort button
                            FilledTonalButton(
                                onClick = onToggleSort,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isChaptersAscending) "Oldest" else "Newest", fontSize = 13.sp)
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
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredChapters,
                                key = { it.url.ifBlank { "${it.chapterNumber}_${it.title}" } }
                            ) { chapter ->
                                val isLastRead = lastReadEntry != null &&
                                        (chapter.url == lastReadEntry.chapterUrl || chapter.title == lastReadEntry.chapterTitle)

                                ChapterTileDesktop(
                                    chapter = chapter,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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

                Spacer(Modifier.width(14.dp))

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

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Read",
                tint = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
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
    filteredChapters: List<Chapter>,
    lastReadEntry: HistoryEntry?,
    isInLibrary: Boolean,
    libraryItem: LibraryManga?,
    chapterSearchQuery: String,
    onChapterSearchQueryChange: (String) -> Unit,
    isChapterSearchActive: Boolean,
    isChaptersAscending: Boolean,
    isDescriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
    onChapterClick: (Chapter, Int, Int) -> Unit,
    onAddToLibrary: () -> Unit,
    onOpenCategoryPicker: () -> Unit,
    onArtworkColorExtracted: (Color) -> Unit
) {
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
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                // Foreground Hero Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Poster Card
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

                    // Metadata Column
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 4.dp)
                    ) {
                        // Title
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Alternative Title
                        mangaDetails?.altTitle?.let { alt ->
                            if (alt.isNotBlank() && alt != displayTitle) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = alt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        // Author
                        mangaDetails?.author?.let { author ->
                            if (author.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = sourceId,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }

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
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFFFFB300))
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = if (rating > 10f) "${(rating / 10f).formatOneDec()}/10" else "${rating.formatOneDec()}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
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
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. ACTION BUTTONS: Resume/Start Reading + In Library
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val firstChapter = allChapters.minByOrNull { it.chapterNumber }
                    ?: allChapters.lastOrNull()
                    ?: allChapters.firstOrNull()
                val resumeChapter = if (lastReadEntry != null) {
                    allChapters.firstOrNull { it.url == lastReadEntry.chapterUrl || it.title == lastReadEntry.chapterTitle } ?: firstChapter
                } else {
                    firstChapter
                }

                Button(
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
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (lastReadEntry != null) {
                            val chName = lastReadEntry.chapterTitle.replace(Regex("(?i)chapter\\s*"), "").trim()
                            if (lastReadEntry.lastPage > 1) "Resume Ch. $chName (p. ${lastReadEntry.lastPage})" else "Resume Ch. $chName"
                        } else "Start Reading",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isInLibrary) {
                    FilledTonalButton(
                        onClick = onOpenCategoryPicker,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(libraryItem?.category ?: "In Library", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    OutlinedButton(
                        onClick = onAddToLibrary,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("In Library")
                    }
                }
            }
        }

        // 3. GENRES & TAGS
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
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. SYNOPSIS / DESCRIPTION
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

        // 5. CHAPTER SEARCH BAR
        if (isChapterSearchActive) {
            item {
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
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        // 6. CHAPTERS HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
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

                Text(
                    text = if (isChaptersAscending) "Oldest first" else "Newest first",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // 7. CHAPTERS LIST
        if (filteredChapters.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chapterSearchQuery.isNotBlank()) "No chapters matching \"$chapterSearchQuery\"" else "No chapters found",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(
                items = filteredChapters,
                key = { it.id.ifBlank { it.url } }
            ) { chapter ->
                val isLastRead = lastReadEntry != null && (chapter.url == lastReadEntry.chapterUrl || chapter.title == lastReadEntry.chapterTitle)

                Surface(
                    onClick = { onChapterClick(chapter, if (isLastRead) (lastReadEntry.lastPage.coerceAtLeast(1)) else 1, if (isLastRead) lastReadEntry.scrollOffset else 0) },
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
                                    color = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isLastRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(20.dp)
                        )
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
