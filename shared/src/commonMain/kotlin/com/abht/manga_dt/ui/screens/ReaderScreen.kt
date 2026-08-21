package com.abht.manga_dt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.models.ReaderPage
import com.abht.manga_dt.ui.models.ReaderBackground
import com.abht.manga_dt.ui.models.ReaderScaleMode
import com.abht.manga_dt.ui.models.ReadingMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    title: String,
    chapterTitle: String = "Chapter 1",
    sourceId: String = "",
    chapterUrl: String = "",
    initialPage: Int = 1,
    initialScrollOffset: Int = 0,
    mangaUrl: String = "",
    initialPages: List<ReaderPage> = emptyList(),
    onBack: () -> Unit,
    onChapterChange: ((newChapter: com.abht.manga_dt.models.Chapter) -> Unit)? = null,
    onPreviousChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null
) {
    val sourceManager = remember { MangaSourceManager() }
    val coroutineScope = rememberCoroutineScope()
    var pages by remember(sourceId, chapterUrl) { mutableStateOf(initialPages) }
    var isLoadingPages by remember(sourceId, chapterUrl) { mutableStateOf(chapterUrl.isNotBlank() && initialPages.isEmpty()) }
    var pageLoadError by remember(sourceId, chapterUrl) { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var hasJumpedToInitialPage by remember(sourceId, chapterUrl) { mutableStateOf(false) }

    // Dynamic chapter lookup from cache
    val cachedManga = remember(mangaUrl, sourceId, title) {
        (if (mangaUrl.isNotBlank()) com.abht.manga_dt.data.MangaDataCache.cachedMangaDetails["$sourceId::$mangaUrl"] else null)
            ?: com.abht.manga_dt.data.MangaDataCache.cachedMangaDetails.values.firstOrNull { m -> 
                m.title.equals(title, ignoreCase = true) || m.chapters.any { it.url == chapterUrl || it.title == chapterTitle } 
            }
    }
    val allMangaChapters = remember(cachedManga) {
        cachedManga?.chapters?.sortedBy { it.chapterNumber } ?: emptyList()
    }
    val currentChapterIndex = remember(allMangaChapters, chapterUrl, chapterTitle) {
        allMangaChapters.indexOfFirst { it.url == chapterUrl || it.title.equals(chapterTitle, ignoreCase = true) }
    }
    val resolvedPrevChapter = remember(allMangaChapters, currentChapterIndex) {
        if (currentChapterIndex > 0) allMangaChapters[currentChapterIndex - 1] else null
    }
    val resolvedNextChapter = remember(allMangaChapters, currentChapterIndex) {
        if (currentChapterIndex in 0 until allMangaChapters.size - 1) allMangaChapters[currentChapterIndex + 1] else null
    }

    LaunchedEffect(sourceId, chapterUrl) {
        if (chapterUrl.isNotBlank() && initialPages.isEmpty()) {
            isLoadingPages = true
            pageLoadError = null
            try {
                val fetchedPages = sourceManager.getPages(sourceId, chapterUrl)
                if (fetchedPages.isNotEmpty()) {
                    pages = fetchedPages
                } else {
                    pageLoadError = "No pages found for this chapter"
                }
            } catch (e: Exception) {
                pageLoadError = e.message ?: "Failed to load chapter pages"
            } finally {
                isLoadingPages = false
            }
        }
    }

    val readingMode = AppSettings.readingMode
    val readerBackground = AppSettings.readerBackground
    val readerScaleMode = AppSettings.readerScaleMode
    val showPagePill = AppSettings.showPageNumberPill

    val backgroundColor = when (readerBackground) {
        ReaderBackground.BLACK -> Color(0xFF000000)
        ReaderBackground.DARK_GRAY -> Color(0xFF1E1E1E)
        ReaderBackground.WHITE -> Color(0xFFFFFFFF)
    }

    val contentScale = when (readerScaleMode) {
        ReaderScaleMode.FIT_WIDTH -> ContentScale.FillWidth
        ReaderScaleMode.FIT_SCREEN -> ContentScale.Fit
        ReaderScaleMode.FIT_HEIGHT -> ContentScale.FillHeight
        ReaderScaleMode.ORIGINAL -> ContentScale.Inside
    }

    val totalPages = pages.size.coerceAtLeast(1)
    val initialItemIdx = (initialPage - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialItemIdx,
        initialFirstVisibleItemScrollOffset = initialScrollOffset.coerceAtLeast(0)
    )
    val pagerState = rememberPagerState(
        initialPage = if (readingMode == ReadingMode.RTL && totalPages > 1) (totalPages - 1 - initialItemIdx).coerceIn(0, totalPages - 1) else initialItemIdx.coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )

    // Track current page index accurately based on visible viewport center
    val currentPageIndex by remember(readingMode, totalPages) {
        derivedStateOf {
            when (readingMode) {
                ReadingMode.WEBTOON -> {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) {
                        (listState.firstVisibleItemIndex + 1).coerceIn(1, totalPages)
                    } else {
                        val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2 + layoutInfo.viewportStartOffset
                        val currentItem = visibleItems.minByOrNull { item ->
                            val itemCenter = item.offset + item.size / 2
                            kotlin.math.abs(itemCenter - viewportCenter)
                        } ?: visibleItems.first()
                        (currentItem.index + 1).coerceIn(1, totalPages)
                    }
                }
                ReadingMode.RTL -> {
                    (totalPages - pagerState.currentPage).coerceIn(1, totalPages)
                }
                ReadingMode.LTR, ReadingMode.VERTICAL_PAGED -> {
                    (pagerState.currentPage + 1).coerceIn(1, totalPages)
                }
            }
        }
    }

    val currentScrollOffset by remember(readingMode) {
        derivedStateOf {
            if (readingMode == ReadingMode.WEBTOON) {
                listState.firstVisibleItemScrollOffset
            } else {
                0
            }
        }
    }

    fun jumpToPage(targetPage: Int) {
        val pageIdx = (targetPage - 1).coerceIn(0, totalPages - 1)
        coroutineScope.launch {
            when (readingMode) {
                ReadingMode.WEBTOON -> {
                    listState.scrollToItem(pageIdx, 0)
                }
                ReadingMode.RTL -> {
                    val target = (totalPages - 1) - pageIdx
                    pagerState.scrollToPage(target)
                }
                ReadingMode.LTR, ReadingMode.VERTICAL_PAGED -> {
                    pagerState.scrollToPage(pageIdx)
                }
            }
        }
    }

    var isReadyToTrackProgress by remember(sourceId, chapterUrl) { mutableStateOf(false) }

    // Auto-jump to initial page and exact scroll offset when chapter pages are loaded
    LaunchedEffect(pages, initialPage, initialScrollOffset) {
        if (pages.isNotEmpty()) {
            if (initialPage > 1 || initialScrollOffset > 0) {
                val targetIdx = (initialPage - 1).coerceIn(0, pages.size - 1)
                kotlinx.coroutines.delay(80)
                when (readingMode) {
                    ReadingMode.WEBTOON -> listState.scrollToItem(targetIdx, initialScrollOffset.coerceAtLeast(0))
                    ReadingMode.RTL -> pagerState.scrollToPage((pages.size - 1) - targetIdx)
                    ReadingMode.LTR, ReadingMode.VERTICAL_PAGED -> pagerState.scrollToPage(targetIdx)
                }
                kotlinx.coroutines.delay(100)
            }
            isReadyToTrackProgress = true
        }
    }

    // Live progress saving to History and Library ONLY after initial jump is ready
    LaunchedEffect(currentPageIndex, currentScrollOffset, isReadyToTrackProgress, pages.size, chapterUrl) {
        if (isReadyToTrackProgress && pages.isNotEmpty() && chapterUrl.isNotBlank()) {
            com.abht.manga_dt.data.HistoryManager.updatePageProgress(
                mangaTitle = title,
                chapterUrl = chapterUrl,
                page = currentPageIndex,
                totalPages = pages.size,
                scrollOffset = currentScrollOffset
            )
            if (mangaUrl.isNotBlank()) {
                com.abht.manga_dt.data.LibraryManager.updateProgress(
                    mangaUrl = mangaUrl,
                    title = title,
                    chapterTitle = chapterTitle,
                    chapterUrl = chapterUrl
                )
            }
        }
    }

    // --- Fine-grained Reading Analytics Tracking (100% Local & Offline) ---
    var lastActiveTime by remember { mutableStateOf(com.abht.manga_dt.data.currentTimeMillis()) }
    var lastRecordedPage by remember { mutableStateOf(initialPage) }
    val mangaTags = remember(cachedManga) { cachedManga?.tags ?: emptyList() }
    val mangaCover = remember(cachedManga) { cachedManga?.thumbnailUrl ?: "" }

    // Real-time active reading stopwatch (every 5 seconds)
    LaunchedEffect(title, chapterUrl, isReadyToTrackProgress) {
        if (isReadyToTrackProgress) {
            var lastTick = com.abht.manga_dt.data.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(5000)
                val now = com.abht.manga_dt.data.currentTimeMillis()
                val idleDuration = now - lastActiveTime
                if (idleDuration < 90_000) { // Active within last 90 seconds
                    val elapsedSeconds = ((now - lastTick) / 1000L).coerceIn(1L, 10L)
                    val isCompleted = currentPageIndex >= totalPages && totalPages > 1
                    com.abht.manga_dt.data.StatisticsManager.recordReadingSession(
                        mangaTitle = title,
                        mangaCover = mangaCover,
                        sourceId = sourceId,
                        mangaUrl = mangaUrl,
                        durationSeconds = elapsedSeconds,
                        pagesTurned = 0,
                        completedChapterUrl = if (isCompleted) chapterUrl else null,
                        tags = mangaTags
                    )
                }
                lastTick = now
            }
        }
    }

    // Page turn & chapter completion tracker
    LaunchedEffect(currentPageIndex, isReadyToTrackProgress) {
        if (isReadyToTrackProgress && currentPageIndex != lastRecordedPage) {
            lastActiveTime = com.abht.manga_dt.data.currentTimeMillis()
            val deltaPages = (currentPageIndex - lastRecordedPage).let { if (it > 0) it else 1 }
            lastRecordedPage = currentPageIndex
            val isCompleted = currentPageIndex >= totalPages && totalPages > 1
            com.abht.manga_dt.data.StatisticsManager.recordReadingSession(
                mangaTitle = title,
                mangaCover = mangaCover,
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                durationSeconds = 0,
                pagesTurned = deltaPages,
                completedChapterUrl = if (isCompleted) chapterUrl else null,
                tags = mangaTags
            )
        }
    }

    fun nextPage() {
        if (currentPageIndex < totalPages) {
            jumpToPage(currentPageIndex + 1)
        }
    }

    fun prevPage() {
        if (currentPageIndex > 1) {
            jumpToPage(currentPageIndex - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoadingPages) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading chapter pages...", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (pageLoadError != null && pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(pageLoadError ?: "Error loading pages", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        coroutineScope.launch {
                            isLoadingPages = true
                            pageLoadError = null
                            try {
                                val fetched = sourceManager.getPages(sourceId, chapterUrl)
                                if (fetched.isNotEmpty()) pages = fetched else pageLoadError = "No pages found"
                            } catch (e: Exception) {
                                pageLoadError = e.message
                            } finally {
                                isLoadingPages = false
                            }
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            // --- Main Reader Content ---
            when (readingMode) {
                ReadingMode.WEBTOON -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showControls = !showControls }
                                )
                            }
                    ) {
                        itemsIndexed(
                            items = pages,
                            key = { index, page -> "${page.pageNumber}_${page.url.ifBlank { index.toString() }}" }
                        ) { index, page ->
                            ReaderImageItem(
                                page = page,
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxWidth(),
                                isWebtoon = true
                            )
                        }
                    }
                }
            ReadingMode.RTL -> {
                // Japanese Manga (Right-to-Left)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.25f -> nextPage()      // Tap Left = Next in RTL
                                        offset.x > width * 0.75f -> prevPage()      // Tap Right = Prev in RTL
                                        else -> showControls = !showControls       // Center = Toggle UI
                                    }
                                }
                            )
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ReaderImageItem(
                                page = page,
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            ReadingMode.LTR -> {
                // Comics / Western Manhwa (Left-to-Right)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.25f -> prevPage()      // Tap Left = Prev in LTR
                                        offset.x > width * 0.75f -> nextPage()      // Tap Right = Next in LTR
                                        else -> showControls = !showControls       // Center = Toggle UI
                                    }
                                }
                            )
                        }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ReaderImageItem(
                                page = page,
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            ReadingMode.VERTICAL_PAGED -> {
                // Single Page Vertical
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { showControls = !showControls }
                            )
                        }
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ReaderImageItem(
                                page = page,
                                contentScale = contentScale,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

        // --- Floating Page Number Pill (when controls are hidden) ---
        AnimatedVisibility(
            visible = !showControls && showPagePill && pages.isNotEmpty(),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = Color.White
            ) {
                Text(
                    text = "$currentPageIndex / $totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // --- Top Bar Controls Overlay ---
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Reading Mode Toggle Button
                    IconButton(onClick = {
                        val nextMode = when (readingMode) {
                            ReadingMode.WEBTOON -> ReadingMode.RTL
                            ReadingMode.RTL -> ReadingMode.LTR
                            ReadingMode.LTR -> ReadingMode.VERTICAL_PAGED
                            ReadingMode.VERTICAL_PAGED -> ReadingMode.WEBTOON
                        }
                        AppSettings.updateReadingMode(nextMode)
                    }) {
                        Icon(
                            imageVector = when (readingMode) {
                                ReadingMode.WEBTOON -> Icons.Default.SwapVert
                                ReadingMode.RTL -> Icons.AutoMirrored.Filled.MenuBook
                                ReadingMode.LTR -> Icons.Default.SwapHoriz
                                ReadingMode.VERTICAL_PAGED -> Icons.Default.ViewAgenda
                            },
                            contentDescription = "Reading Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Reader Settings Button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Reader Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // --- Bottom Bar Controls Overlay (Kotatsu Style) ---
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Page Scrubbing Slider with indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentPageIndex",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )

                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = { newPage ->
                                jumpToPage(newPage.toInt())
                            },
                            valueRange = 1f..totalPages.toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = "$totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Chapter navigation and Quick Mode bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Chapter
                        // Previous Chapter
                        val hasPrev = onPreviousChapter != null || (resolvedPrevChapter != null && onChapterChange != null)
                        TextButton(
                            onClick = { 
                                if (onPreviousChapter != null) onPreviousChapter.invoke()
                                else if (resolvedPrevChapter != null && onChapterChange != null) onChapterChange(resolvedPrevChapter)
                            },
                            enabled = hasPrev,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Prev")
                        }

                        // Current Mode Label Chip
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            onClick = { showSettingsSheet = true }
                        ) {
                            Text(
                                text = readingMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        // Next Chapter
                        val hasNext = onNextChapter != null || (resolvedNextChapter != null && onChapterChange != null)
                        TextButton(
                            onClick = { 
                                if (onNextChapter != null) onNextChapter.invoke()
                                else if (resolvedNextChapter != null && onChapterChange != null) onChapterChange(resolvedNextChapter)
                            },
                            enabled = hasNext,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                        }
                    }
                }
            }
        }

        // --- Kotatsu Reader Settings BottomSheet ---
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Reader Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))

                    // 1. Reading Mode
                    Text(
                        text = "Reading Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReadingMode.WEBTOON to "Webtoon",
                            ReadingMode.RTL to "Manga (RTL)",
                            ReadingMode.LTR to "Comic (LTR)"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = readingMode == mode,
                                onClick = { 
                                    val current = currentPageIndex
                                    AppSettings.updateReadingMode(mode)
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(60)
                                        jumpToPage(current)
                                    }
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. Background Color
                    Text(
                        text = "Background Color",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReaderBackground.BLACK to "AMOLED Black",
                            ReaderBackground.DARK_GRAY to "Dark Gray",
                            ReaderBackground.WHITE to "White"
                        ).forEach { (bg, label) ->
                            FilterChip(
                                selected = readerBackground == bg,
                                onClick = { AppSettings.updateReaderBackground(bg) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3. Scale Mode
                    Text(
                        text = "Scale Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ReaderScaleMode.FIT_WIDTH to "Fit Width",
                            ReaderScaleMode.FIT_SCREEN to "Fit Screen",
                            ReaderScaleMode.FIT_HEIGHT to "Fit Height"
                        ).forEach { (scale, label) ->
                            FilterChip(
                                selected = readerScaleMode == scale,
                                onClick = { AppSettings.updateReaderScaleMode(scale) },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 4. Page Indicator Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Page Number Indicator", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Show floating page bubble while reading", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = showPagePill,
                            onCheckedChange = { AppSettings.updateShowPageNumberPill(it) }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ReaderImageItem(
    page: ReaderPage,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    isWebtoon: Boolean = false
) {
    var isImageLoading by remember(page.url) { mutableStateOf(true) }
    var isImageError by remember(page.url) { mutableStateOf(false) }

    val imageModifier = if (isWebtoon) {
        Modifier.fillMaxWidth().wrapContentHeight()
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = modifier.defaultMinSize(minHeight = 200.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isImageLoading && !isImageError) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp).padding(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                strokeWidth = 3.dp
            )
        }

        AsyncImage(
            model = page.url,
            contentDescription = "Page ${page.pageNumber}",
            modifier = imageModifier,
            contentScale = contentScale,
            onLoading = { isImageLoading = true; isImageError = false },
            onSuccess = { isImageLoading = false; isImageError = false },
            onError = { isImageLoading = false; isImageError = true },
            error = rememberVectorPainter(Icons.Default.BrokenImage)
        )

        if (isImageError) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.BrokenImage,
                    contentDescription = "Failed to load page",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Page ${page.pageNumber} failed to load",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

