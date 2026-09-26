package com.abht.manga_dt.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.abht.manga_dt.data.currentTimeMillis
import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.LibraryManga
import com.abht.manga_dt.models.ReaderPage
import com.abht.manga_dt.ui.components.BackHandler
import com.abht.manga_dt.ui.models.ReaderBackground
import com.abht.manga_dt.ui.models.ReaderScaleMode
import com.abht.manga_dt.ui.models.ReadingMode
import kotlinx.coroutines.delay
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
    onChapterChange: ((newChapter: Chapter) -> Unit)? = null,
    onPreviousChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null
) {
    val strings = Strings.current
    val sourceManager = remember { MangaSourceManager() }
    val coroutineScope = rememberCoroutineScope()

    var pages by remember(sourceId, chapterUrl) { mutableStateOf(initialPages) }
    var isLoadingPages by remember(sourceId, chapterUrl) { mutableStateOf(chapterUrl.isNotBlank() && initialPages.isEmpty()) }
    var pageLoadError by remember(sourceId, chapterUrl) { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChapterListSheet by remember { mutableStateOf(false) }
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var isReadyToTrackProgress by remember(sourceId, chapterUrl) { mutableStateOf(false) }

    // Back handling for sheets & dialogs in reader
    BackHandler(enabled = showChapterListSheet) { showChapterListSheet = false }
    BackHandler(enabled = showSettingsSheet) { showSettingsSheet = false }
    BackHandler(enabled = showGoToPageDialog) { showGoToPageDialog = false }

    // Dynamic chapter lookup from cache, OfflineMangaManager and DownloadManager
    val cachedManga = remember(mangaUrl, sourceId, title) {
        OfflineMangaManager.getManga(sourceId, mangaUrl, title)
            ?: (if (mangaUrl.isNotBlank()) MangaDataCache.cachedMangaDetails["$sourceId::$mangaUrl"] else null)
            ?: MangaDataCache.cachedMangaDetails.values.firstOrNull { m ->
                m.title.equals(title, ignoreCase = true) || m.chapters.any { it.url == chapterUrl || it.title == chapterTitle }
            }
    }

    val downloadedChaptersList = remember(mangaUrl, title, sourceId, chapterUrl) {
        DownloadManager.downloadedChapters.values.filter {
            (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) ||
            it.mangaTitle.equals(title, ignoreCase = true) ||
            (sourceId.isNotBlank() && it.sourceId == sourceId && it.mangaTitle.equals(title, ignoreCase = true)) ||
            it.chapterUrl == chapterUrl
        }.map {
            Chapter(
                id = it.chapterUrl,
                mangaId = it.mangaUrl.ifBlank { it.mangaTitle },
                url = it.chapterUrl,
                title = it.chapterTitle,
                chapterNumber = it.chapterNumber,
                uploadDate = if (it.downloadTimestamp > 0) it.downloadTimestamp else null
            )
        }.distinctBy { it.url }.sortedBy { it.chapterNumber }
    }

    val initialChapters = when {
        !cachedManga?.chapters.isNullOrEmpty() -> cachedManga!!.chapters.sortedBy { it.chapterNumber }
        downloadedChaptersList.isNotEmpty() -> downloadedChaptersList
        chapterUrl.isNotBlank() -> listOf(
            Chapter(
                id = chapterUrl,
                mangaId = mangaUrl.ifBlank { title },
                url = chapterUrl,
                title = chapterTitle,
                chapterNumber = 0f
            )
        )
        else -> emptyList()
    }

    var allMangaChapters by remember(mangaUrl, sourceId, title, chapterUrl) {
        mutableStateOf(initialChapters)
    }

    // Effect to ensure all offline downloaded chapters or full online chapter list is loaded
    LaunchedEffect(sourceId, mangaUrl, title, chapterUrl) {
        // 1. Sync offline downloaded chapters if list is minimal
        val offline = DownloadManager.downloadedChapters.values.filter {
            (mangaUrl.isNotBlank() && it.mangaUrl == mangaUrl) ||
            it.mangaTitle.equals(title, ignoreCase = true) ||
            (sourceId.isNotBlank() && it.sourceId == sourceId && it.mangaTitle.equals(title, ignoreCase = true)) ||
            it.chapterUrl == chapterUrl
        }.map {
            Chapter(
                id = it.chapterUrl,
                mangaId = it.mangaUrl.ifBlank { it.mangaTitle },
                url = it.chapterUrl,
                title = it.chapterTitle,
                chapterNumber = it.chapterNumber,
                uploadDate = if (it.downloadTimestamp > 0) it.downloadTimestamp else null
            )
        }.distinctBy { it.url }.sortedBy { it.chapterNumber }

        if (offline.isNotEmpty() && (allMangaChapters.size <= 1 || allMangaChapters.none { it.url == chapterUrl })) {
            allMangaChapters = offline
        }

        // 2. Fetch full chapters from online source if mangaUrl is provided
        if (mangaUrl.isNotBlank() && sourceId.isNotBlank()) {
            try {
                val details = sourceManager.getMangaDetails(sourceId, mangaUrl)
                if (details != null && details.chapters.isNotEmpty()) {
                    MangaDataCache.cachedMangaDetails["$sourceId::$mangaUrl"] = details
                    OfflineMangaManager.saveManga(details)
                    allMangaChapters = details.chapters.sortedBy { it.chapterNumber }
                }
            } catch (e: Exception) {
                // If offline, keep offline chapters
                if (offline.isNotEmpty()) {
                    allMangaChapters = offline
                }
            }
        }
    }

    val currentChapterIndex = remember(allMangaChapters, chapterUrl, chapterTitle) {
        val byUrl = allMangaChapters.indexOfFirst { it.url == chapterUrl }
        if (byUrl >= 0) byUrl else allMangaChapters.indexOfFirst { it.title.equals(chapterTitle, ignoreCase = true) }
    }
    val resolvedPrevChapter = remember(allMangaChapters, currentChapterIndex) {
        if (currentChapterIndex > 0) allMangaChapters[currentChapterIndex - 1] else null
    }
    val resolvedNextChapter = remember(allMangaChapters, currentChapterIndex) {
        if (currentChapterIndex in 0 until allMangaChapters.size - 1) allMangaChapters[currentChapterIndex + 1] else null
    }

    // Favorite Status in Library
    val isFavorite = remember(LibraryManager.libraryItems, sourceId, mangaUrl, title) {
        LibraryManager.libraryItems.any {
            it.sourceId == sourceId && (it.mangaUrl == mangaUrl || it.title.equals(title, ignoreCase = true))
        }
    }

    // Load pages effect (Checks DownloadManager for offline pages first, then online source)
    LaunchedEffect(sourceId, chapterUrl) {
        if (chapterUrl.isNotBlank() && initialPages.isEmpty()) {
            isLoadingPages = true
            pageLoadError = null
            try {
                val downloaded = com.abht.manga_dt.data.DownloadManager.getDownloadedPages(chapterUrl)
                if (downloaded != null && downloaded.isNotEmpty()) {
                    pages = downloaded
                } else {
                    val fetchedPages = sourceManager.getPages(sourceId, chapterUrl)
                    if (fetchedPages.isNotEmpty()) {
                        pages = fetchedPages
                    } else {
                        pageLoadError = strings.pageFailedToLoad
                    }
                }
            } catch (e: Exception) {
                // In case of network exception, retry from DownloadManager
                val downloaded = com.abht.manga_dt.data.DownloadManager.getDownloadedPages(chapterUrl)
                if (downloaded != null && downloaded.isNotEmpty()) {
                    pages = downloaded
                } else {
                    pageLoadError = e.message ?: strings.pageFailedToLoad
                }
            } finally {
                isLoadingPages = false
            }
        }
    }

    val readingMode = AppSettings.readingMode
    val readerBackground = AppSettings.readerBackground
    val readerScaleMode = AppSettings.readerScaleMode
    val showPagePill = AppSettings.showPageNumberPill
    val cropBorders = AppSettings.readerCropBorders
    val doubleTapZoomScale = AppSettings.readerDoubleTapZoom
    val panSensitivity = AppSettings.readerPanSensitivity

    val backgroundColor = when (readerBackground) {
        ReaderBackground.BLACK -> Color(0xFF000000)
        ReaderBackground.DARK_GRAY -> Color(0xFF141414)
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

    // Track current page index accurately based on viewport center
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

    fun navigateToChapter(chapter: Chapter) {
        if (onChapterChange != null) {
            onChapterChange(chapter)
        }
    }

    // Auto-jump to initial page and scroll offset when loaded
    LaunchedEffect(pages, initialPage, initialScrollOffset) {
        if (pages.isNotEmpty()) {
            if (initialPage > 1 || initialScrollOffset > 0) {
                val targetIdx = (initialPage - 1).coerceIn(0, pages.size - 1)
                delay(80)
                when (readingMode) {
                    ReadingMode.WEBTOON -> listState.scrollToItem(targetIdx, initialScrollOffset.coerceAtLeast(0))
                    ReadingMode.RTL -> pagerState.scrollToPage((pages.size - 1) - targetIdx)
                    ReadingMode.LTR, ReadingMode.VERTICAL_PAGED -> pagerState.scrollToPage(targetIdx)
                }
                delay(100)
            }
            isReadyToTrackProgress = true
        }
    }

    val mangaTags = remember(cachedManga) { cachedManga?.tags ?: emptyList() }
    val mangaCover = remember(cachedManga) { cachedManga?.thumbnailUrl ?: "" }
    val isMangaNsfw = remember(cachedManga, mangaTags) { 
        cachedManga?.isNsfw == true || AppSettings.isMangaNsfw(cachedManga?.isNsfw ?: false, mangaTags) 
    }
    val isIncognitoActive = isMangaNsfw && AppSettings.nsfwIncognitoMode

    // Live progress saving to History and Library (triggered only on discrete page changes)
    LaunchedEffect(currentPageIndex, isReadyToTrackProgress, pages.size, chapterUrl, isIncognitoActive) {
        if (isReadyToTrackProgress && pages.isNotEmpty() && chapterUrl.isNotBlank() && !isIncognitoActive) {
            HistoryManager.updatePageProgress(
                mangaTitle = title,
                chapterUrl = chapterUrl,
                page = currentPageIndex,
                totalPages = pages.size,
                scrollOffset = 0,
                isNsfw = isMangaNsfw,
                tags = mangaTags
            )
            if (mangaUrl.isNotBlank()) {
                LibraryManager.updateProgress(
                    mangaUrl = mangaUrl,
                    title = title,
                    chapterTitle = chapterTitle,
                    chapterUrl = chapterUrl
                )
            }
            // Auto-delete read chapter if enabled and user reached the end of the chapter
            if (currentPageIndex >= pages.size && AppSettings.autoDeleteReadChapters) {
                if (DownloadManager.isChapterDownloaded(chapterUrl)) {
                    DownloadManager.deleteDownloadedChapter(chapterUrl)
                }
            }
        }
    }

    // Reading Analytics Tracker (100% Local & Offline)
    var lastActiveTime by remember { mutableStateOf(currentTimeMillis()) }
    var lastRecordedPage by remember { mutableStateOf(initialPage) }

    // Immediate initial session record
    LaunchedEffect(isReadyToTrackProgress) {
        if (isReadyToTrackProgress && pages.isNotEmpty()) {
            StatisticsManager.recordReadingSession(
                mangaTitle = title,
                mangaCover = mangaCover,
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                durationSeconds = 1L,
                pagesTurned = 1,
                completedChapterUrl = null,
                tags = mangaTags,
                isNsfw = isMangaNsfw
            )
        }
    }

    // Continuous time ticker
    LaunchedEffect(title, chapterUrl, isReadyToTrackProgress) {
        if (isReadyToTrackProgress) {
            var lastTick = currentTimeMillis()
            while (true) {
                delay(3000)
                val now = currentTimeMillis()
                val elapsedSeconds = ((now - lastTick) / 1000L).coerceIn(1L, 10L)
                val isCompleted = currentPageIndex >= totalPages && totalPages > 1
                StatisticsManager.recordReadingSession(
                    mangaTitle = title,
                    mangaCover = mangaCover,
                    sourceId = sourceId,
                    mangaUrl = mangaUrl,
                    durationSeconds = elapsedSeconds,
                    pagesTurned = 0,
                    completedChapterUrl = if (isCompleted) chapterUrl else null,
                    tags = mangaTags,
                    isNsfw = isMangaNsfw
                )
                lastTick = now
            }
        }
    }

    // Page turn tracker
    LaunchedEffect(currentPageIndex, isReadyToTrackProgress) {
        if (isReadyToTrackProgress && currentPageIndex != lastRecordedPage) {
            lastActiveTime = currentTimeMillis()
            val deltaPages = (currentPageIndex - lastRecordedPage).let { if (it > 0) it else 1 }
            lastRecordedPage = currentPageIndex
            val isCompleted = currentPageIndex >= totalPages && totalPages > 1
            StatisticsManager.recordReadingSession(
                mangaTitle = title,
                mangaCover = mangaCover,
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                durationSeconds = 0,
                pagesTurned = deltaPages,
                completedChapterUrl = if (isCompleted) chapterUrl else null,
                tags = mangaTags,
                isNsfw = isMangaNsfw
            )
        }
    }

    // Exit flush tracker
    DisposableEffect(title, chapterUrl) {
        onDispose {
            val now = currentTimeMillis()
            val elapsed = ((now - lastActiveTime) / 1000L).coerceIn(1L, 30L)
            StatisticsManager.recordReadingSession(
                mangaTitle = title,
                mangaCover = mangaCover,
                sourceId = sourceId,
                mangaUrl = mangaUrl,
                durationSeconds = elapsed,
                pagesTurned = 0,
                completedChapterUrl = null,
                tags = mangaTags,
                isNsfw = isMangaNsfw
            )
        }
    }

    fun nextPage() {
        if (currentPageIndex < totalPages) {
            jumpToPage(currentPageIndex + 1)
        } else if (resolvedNextChapter != null && onChapterChange != null) {
            navigateToChapter(resolvedNextChapter)
        }
    }

    fun prevPage() {
        if (currentPageIndex > 1) {
            jumpToPage(currentPageIndex - 1)
        } else if (resolvedPrevChapter != null && onChapterChange != null) {
            navigateToChapter(resolvedPrevChapter)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoadingPages) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = strings.loadingManga,
                        color = if (readerBackground == ReaderBackground.WHITE) Color.DarkGray else Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (pageLoadError != null && pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = pageLoadError ?: strings.pageFailedToLoad,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoadingPages = true
                                    pageLoadError = null
                                    try {
                                        val fetched = sourceManager.getPages(sourceId, chapterUrl)
                                        if (fetched.isNotEmpty()) pages = fetched else pageLoadError = strings.pageFailedToLoad
                                    } catch (e: Exception) {
                                        pageLoadError = e.message
                                    } finally {
                                        isLoadingPages = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.retry)
                        }
                    }
                }
            }
        } else {
            // --- Main Reader Content Area with Zoom & Pan Support ---
            when (readingMode) {
                ReadingMode.WEBTOON -> {
                    ZoomableWebtoonContainer(
                        modifier = Modifier.fillMaxSize(),
                        resetTrigger = chapterUrl,
                        doubleTapZoomScale = doubleTapZoomScale,
                        panSensitivity = panSensitivity,
                        onSingleTap = { _, _ -> showControls = !showControls }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = pages,
                                key = { index, page -> "${page.pageNumber}_${page.url.ifBlank { index.toString() }}" }
                            ) { index, page ->
                                ReaderImageItem(
                                    page = page,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxWidth(),
                                    isWebtoon = true,
                                    cropBorders = cropBorders
                                )
                            }

                            // Kotatsu End of Chapter Transition Banner in Webtoon
                            item {
                                EndOfChapterCard(
                                    chapterTitle = chapterTitle,
                                    nextChapter = resolvedNextChapter,
                                    prevChapter = resolvedPrevChapter,
                                    onNextClick = { if (resolvedNextChapter != null) navigateToChapter(resolvedNextChapter) },
                                    onPrevClick = { if (resolvedPrevChapter != null) navigateToChapter(resolvedPrevChapter) },
                                    onBack = onBack
                                )
                            }
                        }
                    }
                }
                ReadingMode.RTL -> {
                    // Japanese Manga (Right-to-Left) with Zoom
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ZoomablePageContainer(
                                modifier = Modifier.fillMaxSize(),
                                resetTrigger = page.url,
                                doubleTapZoomScale = doubleTapZoomScale,
                                panSensitivity = panSensitivity,
                                onSingleTap = { offset, size ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.25f -> nextPage()
                                        offset.x > width * 0.75f -> prevPage()
                                        else -> showControls = !showControls
                                    }
                                }
                            ) {
                                ReaderImageItem(
                                    page = page,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxSize(),
                                    cropBorders = cropBorders
                                )
                            }
                        }
                    }
                }
                ReadingMode.LTR -> {
                    // Comics / Manhwa (Left-to-Right) with Zoom
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ZoomablePageContainer(
                                modifier = Modifier.fillMaxSize(),
                                resetTrigger = page.url,
                                doubleTapZoomScale = doubleTapZoomScale,
                                panSensitivity = panSensitivity,
                                onSingleTap = { offset, size ->
                                    val width = size.width
                                    when {
                                        offset.x < width * 0.25f -> prevPage()
                                        offset.x > width * 0.75f -> nextPage()
                                        else -> showControls = !showControls
                                    }
                                }
                            ) {
                                ReaderImageItem(
                                    page = page,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxSize(),
                                    cropBorders = cropBorders
                                )
                            }
                        }
                    }
                }
                ReadingMode.VERTICAL_PAGED -> {
                    // Single Page Vertical with Zoom
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIdx ->
                        val page = pages.getOrNull(pageIdx)
                        if (page != null) {
                            ZoomablePageContainer(
                                modifier = Modifier.fillMaxSize(),
                                resetTrigger = page.url,
                                doubleTapZoomScale = doubleTapZoomScale,
                                panSensitivity = panSensitivity,
                                onSingleTap = { _, _ -> showControls = !showControls }
                            ) {
                                ReaderImageItem(
                                    page = page,
                                    contentScale = contentScale,
                                    modifier = Modifier.fillMaxSize(),
                                    cropBorders = cropBorders
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Kotatsu Floating HUD / Status Capsule (when controls are hidden) ---
        AnimatedVisibility(
            visible = !showControls && showPagePill && pages.isNotEmpty(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xDD121316),
                contentColor = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$currentPageIndex / $totalPages",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                    Text(
                        text = readingMode.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    )
                }
            }
        }

        // --- Kotatsu Floating Glassmorphic Top Bar ---
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xEE121316),
                contentColor = Color.White,
                tonalElevation = 8.dp
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
                            contentDescription = strings.close,
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sourceId.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = sourceId.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (isIncognitoActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = strings.nsfwBadgeText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1. Quick Add to Library / Favorite Toggle
                    IconButton(
                        onClick = {
                            if (isFavorite) {
                                LibraryManager.removeFromLibrary(mangaUrl = mangaUrl, title = title)
                            } else {
                                LibraryManager.addToLibrary(
                                    LibraryManga(
                                        id = if (mangaUrl.isNotBlank()) "$sourceId::$mangaUrl" else "$sourceId::$title",
                                        title = title,
                                        thumbnailUrl = mangaCover,
                                        sourceId = sourceId,
                                        mangaUrl = mangaUrl,
                                        category = "Default",
                                        addedAt = currentTimeMillis()
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                        )
                    }

                    // 2. Quick Reading Mode Cycle Icon
                    IconButton(onClick = {
                        val nextMode = when (readingMode) {
                            ReadingMode.WEBTOON -> ReadingMode.RTL
                            ReadingMode.RTL -> ReadingMode.LTR
                            ReadingMode.LTR -> ReadingMode.VERTICAL_PAGED
                            ReadingMode.VERTICAL_PAGED -> ReadingMode.WEBTOON
                        }
                        val cur = currentPageIndex
                        AppSettings.updateReadingMode(nextMode)
                        coroutineScope.launch {
                            delay(60)
                            jumpToPage(cur)
                        }
                    }) {
                        Icon(
                            imageVector = when (readingMode) {
                                ReadingMode.WEBTOON -> Icons.Default.SwapVert
                                ReadingMode.RTL -> Icons.AutoMirrored.Filled.MenuBook
                                ReadingMode.LTR -> Icons.Default.SwapHoriz
                                ReadingMode.VERTICAL_PAGED -> Icons.Default.ViewAgenda
                            },
                            contentDescription = strings.readerMode,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 3. Quick Chapters List Drawer Trigger
                    IconButton(onClick = { showChapterListSheet = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = strings.chapterList,
                            tint = Color.White
                        )
                    }

                    // 4. Reader Settings Button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = strings.readerSettings,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // --- Kotatsu Floating Glassmorphic Bottom Bar ---
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xEE121316),
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // A. Page Scrubber Slider with Direct Jump Tappable Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            onClick = { showGoToPageDialog = true }
                        ) {
                            Text(
                                text = "$currentPageIndex",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

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
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.1f),
                            onClick = { jumpToPage(totalPages) }
                        ) {
                            Text(
                                text = "$totalPages",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // B. Kotatsu Chapter Navigation Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Chapter
                        val hasPrev = onPreviousChapter != null || (resolvedPrevChapter != null && onChapterChange != null)
                        FilledTonalButton(
                            onClick = {
                                if (onPreviousChapter != null) onPreviousChapter.invoke()
                                else if (resolvedPrevChapter != null && onChapterChange != null) onChapterChange(resolvedPrevChapter)
                            },
                            enabled = hasPrev,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.previousChapter, fontSize = 12.sp)
                        }

                        // Quick Chapter Selector Center Pill
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            onClick = { showChapterListSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                val chapterCountText = if (allMangaChapters.isNotEmpty() && currentChapterIndex >= 0) {
                                    "${currentChapterIndex + 1} / ${allMangaChapters.size}"
                                } else {
                                    chapterTitle
                                }
                                Text(
                                    text = chapterCountText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Next Chapter
                        val hasNext = onNextChapter != null || (resolvedNextChapter != null && onChapterChange != null)
                        Button(
                            onClick = {
                                if (onNextChapter != null) onNextChapter.invoke()
                                else if (resolvedNextChapter != null && onChapterChange != null) onChapterChange(resolvedNextChapter)
                            },
                            enabled = hasNext,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(strings.nextChapter, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // --- KOTATSU CHAPTERS SELECTOR MODAL BOTTOM SHEET ---
        if (showChapterListSheet) {
            ChapterSelectorSheet(
                chapters = allMangaChapters,
                currentChapterUrl = chapterUrl,
                onSelectChapter = { selectedChapter ->
                    showChapterListSheet = false
                    navigateToChapter(selectedChapter)
                },
                onDismiss = { showChapterListSheet = false }
            )
        }

        // --- GO TO PAGE NUMBER DIALOG ---
        if (showGoToPageDialog) {
            var inputPageText by remember { mutableStateOf(currentPageIndex.toString()) }
            AlertDialog(
                onDismissRequest = { showGoToPageDialog = false },
                title = { Text(strings.goToPage, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${strings.goToPage} (1 - $totalPages)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        OutlinedTextField(
                            value = inputPageText,
                            onValueChange = { inputPageText = it.filter { ch -> ch.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val target = inputPageText.toIntOrNull()
                                    if (target != null) {
                                        jumpToPage(target.coerceIn(1, totalPages))
                                    }
                                    showGoToPageDialog = false
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = inputPageText.toIntOrNull()
                            if (target != null) {
                                jumpToPage(target.coerceIn(1, totalPages))
                            }
                            showGoToPageDialog = false
                        }
                    ) {
                        Text(strings.ok)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoToPageDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        // --- KOTATSU READER SETTINGS BOTTOM SHEET ---
        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.readerSettings,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showSettingsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = strings.close)
                        }
                    }

                    // 1. Reading Mode Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.readerMode,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ReadingMode.WEBTOON to strings.readingModeWebtoon,
                                ReadingMode.RTL to strings.readingModeRTL,
                                ReadingMode.LTR to strings.readingModeLTR,
                                ReadingMode.VERTICAL_PAGED to strings.readingModeVerticalPaged
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = readingMode == mode,
                                    onClick = {
                                        val cur = currentPageIndex
                                        AppSettings.updateReadingMode(mode)
                                        coroutineScope.launch {
                                            delay(60)
                                            jumpToPage(cur)
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. Background Theme Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.readerBackground,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ReaderBackground.BLACK to strings.readerBgBlack,
                                ReaderBackground.DARK_GRAY to strings.readerBgDarkGray,
                                ReaderBackground.WHITE to strings.readerBgWhite
                            ).forEach { (bg, label) ->
                                FilterChip(
                                    selected = readerBackground == bg,
                                    onClick = { AppSettings.updateReaderBackground(bg) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 3. Image Scaling Mode
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.readerScale,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ReaderScaleMode.FIT_WIDTH to strings.readerScaleFitWidth,
                                ReaderScaleMode.FIT_SCREEN to strings.readerScaleFitScreen,
                                ReaderScaleMode.FIT_HEIGHT to strings.readerScaleFitHeight,
                                ReaderScaleMode.ORIGINAL to strings.readerScaleOriginal
                            ).forEach { (scale, label) ->
                                FilterChip(
                                    selected = readerScaleMode == scale,
                                    onClick = { AppSettings.updateReaderScaleMode(scale) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 4. Double-Tap Zoom Scale
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.readerDoubleTapZoom,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${doubleTapZoomScale}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.readerDoubleTapZoomDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1.25f, 1.5f, 2.0f, 2.5f, 3.0f).forEach { scaleOption ->
                                FilterChip(
                                    selected = doubleTapZoomScale == scaleOption,
                                    onClick = { AppSettings.updateReaderDoubleTapZoom(scaleOption) },
                                    label = { Text("${scaleOption}x", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 5. 1-Finger Pan Sensitivity & Acceleration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.readerPanSensitivity,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${panSensitivity}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.readerPanSensitivityDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                1.0f to "1.0x",
                                1.5f to "1.5x",
                                2.0f to "2.0x",
                                2.5f to "2.5x"
                            ).forEach { (sensOption, label) ->
                                FilterChip(
                                    selected = panSensitivity == sensOption,
                                    onClick = { AppSettings.updateReaderPanSensitivity(sensOption) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 6. Feature Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // A. Page Indicator Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.pageIndicator, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(strings.pageIndicatorDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = showPagePill,
                                onCheckedChange = { AppSettings.updateShowPageNumberPill(it) }
                            )
                        }

                        // B. Crop Image Borders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.cropBorders, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(strings.cropBordersDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = cropBorders,
                                onCheckedChange = { AppSettings.updateReaderCropBorders(it) }
                            )
                        }

                        // C. Keep Screen On
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.keepScreenOn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(strings.keepScreenOnDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = AppSettings.readerKeepScreenOn,
                                onCheckedChange = { AppSettings.updateReaderKeepScreenOn(it) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelectorSheet(
    chapters: List<Chapter>,
    currentChapterUrl: String,
    onSelectChapter: (Chapter) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = Strings.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredChapters = remember(chapters, searchQuery) {
        if (searchQuery.isBlank()) chapters
        else chapters.filter { it.title.contains(searchQuery, ignoreCase = true) || it.chapterNumber.toString().contains(searchQuery) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.chapterList,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${chapters.size} ${strings.chapters}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(12.dp))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchChapters, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (filteredChapters.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(strings.noMangaFound, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredChapters, key = { it.url }) { ch ->
                        val isCurrent = ch.url == currentChapterUrl
                        Surface(
                            onClick = { onSelectChapter(ch) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Default.PlayCircle else Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = ch.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (DownloadManager.isChapterDownloaded(ch.url)) {
                                        Icon(
                                            Icons.Default.DownloadDone,
                                            contentDescription = "Downloaded",
                                            tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "Active",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
private fun EndOfChapterCard(
    chapterTitle: String,
    nextChapter: Chapter?,
    prevChapter: Chapter?,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onBack: () -> Unit
) {
    val strings = Strings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Text(
                text = strings.endOfChapter,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$chapterTitle • ${strings.finishedChapter}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (nextChapter != null) {
                Button(
                    onClick = onNextClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.goToNextChapter, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉 ${strings.noMoreChapters}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (prevChapter != null) {
                    OutlinedButton(
                        onClick = onPrevClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.previousChapter, fontSize = 12.sp)
                    }
                }
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.close, fontSize = 12.sp)
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
    isWebtoon: Boolean = false,
    cropBorders: Boolean = false
) {
    val strings = Strings.current
    var isImageLoading by remember(page.url) { mutableStateOf(true) }
    var isImageError by remember(page.url) { mutableStateOf(false) }
    var reloadKey by remember(page.url) { mutableStateOf(0) }

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

        key(reloadKey) {
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
        }

        if (isImageError) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.padding(24.dp).clickable {
                    isImageLoading = true
                    isImageError = false
                    reloadKey++
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = strings.pageFailedToLoad,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "${strings.pageFailedToLoad} (${page.pageNumber})",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(
                        onClick = {
                            isImageLoading = true
                            isImageError = false
                            reloadKey++
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.tapToRetry, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Kotatsu-style Zoomable & Pannable Container
 * Supports smooth pinch-to-zoom, double-tap to zoom, drag-to-pan, and bounds clamping.
 */
@Composable
fun ZoomablePageContainer(
    modifier: Modifier = Modifier,
    resetTrigger: Any? = null,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapZoomScale: Float = 1.5f,
    panSensitivity: Float = 1.5f,
    onSingleTap: ((Offset, Size) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var scale by remember(resetTrigger) { mutableStateOf(1f) }
    var offset by remember(resetTrigger) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        if (newScale <= 1.02f) {
            scale = 1f
            offset = Offset.Zero
        } else {
            val maxX = (containerSize.width * (newScale - 1f)) / 2f
            val maxY = (containerSize.height * (newScale - 1f)) / 2f
            val newOffset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
            )
            scale = newScale
            offset = newOffset
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(containerSize, scale, doubleTapZoomScale) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = doubleTapZoomScale
                            val maxX = (containerSize.width * (targetScale - 1f)) / 2f
                            val maxY = (containerSize.height * (targetScale - 1f)) / 2f
                            val targetX = (containerSize.width / 2f - tapOffset.x) * (targetScale - 1f)
                            val targetY = (containerSize.height / 2f - tapOffset.y) * (targetScale - 1f)
                            scale = targetScale
                            offset = Offset(
                                targetX.coerceIn(-maxX, maxX),
                                targetY.coerceIn(-maxY, maxY)
                            )
                        }
                    },
                    onTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            onSingleTap?.invoke(tapOffset, containerSize)
                        }
                    }
                )
            }
            .pointerInput(scale, containerSize, panSensitivity) {
                if (scale > 1.02f) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val maxX = (containerSize.width * (scale - 1f)) / 2f
                        val maxY = (containerSize.height * (scale - 1f)) / 2f
                        val dragMag = kotlin.math.sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                        val velocityFactor = (1f + (dragMag / 25f).coerceAtMost(1.5f))
                        val factor = panSensitivity * velocityFactor
                        offset = Offset(
                            x = (offset.x + dragAmount.x * factor).coerceIn(-maxX, maxX),
                            y = (offset.y + dragAmount.y * factor).coerceIn(-maxY, maxY)
                        )
                    }
                }
            }
            .transformable(state = transformState, lockRotationOnZoomPan = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            content = content
        )

        // Floating Quick Reset Pill when zoomed in
        AnimatedVisibility(
            visible = scale > 1.15f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                contentColor = Color.White,
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                    Text(
                        text = "${(scale * 10).toInt() / 10f}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}

/**
 * Kotatsu-style Zoomable & Pannable Container specifically designed for continuous vertical Webtoons.
 * Supports smooth 2-finger pinch-to-zoom (up to 5x), double-tap to zoom/reset,
 * 1-finger horizontal panning across wide pages when zoomed in, seamless vertical scrolling,
 * and quick-reset floating pill.
 */
@Composable
fun ZoomableWebtoonContainer(
    modifier: Modifier = Modifier,
    resetTrigger: Any? = null,
    minScale: Float = 1f,
    maxScale: Float = 5f,
    doubleTapZoomScale: Float = 1.5f,
    panSensitivity: Float = 1.5f,
    onSingleTap: ((Offset, Size) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var scale by remember(resetTrigger) { mutableStateOf(1f) }
    var offsetX by remember(resetTrigger) { mutableStateOf(0f) }
    var offsetY by remember(resetTrigger) { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        if (newScale <= 1.02f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } else {
            val maxX = (containerSize.width * (newScale - 1f)) / 2f
            val maxY = (containerSize.height * (newScale - 1f)) / 2f
            scale = newScale
            offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
            offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { containerSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(containerSize, scale, doubleTapZoomScale) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.05f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            val targetScale = doubleTapZoomScale
                            val maxX = (containerSize.width * (targetScale - 1f)) / 2f
                            val maxY = (containerSize.height * (targetScale - 1f)) / 2f
                            val targetX = (containerSize.width / 2f - tapOffset.x) * (targetScale - 1f)
                            val targetY = (containerSize.height / 2f - tapOffset.y) * (targetScale - 1f)
                            scale = targetScale
                            offsetX = targetX.coerceIn(-maxX, maxX)
                            offsetY = targetY.coerceIn(-maxY, maxY)
                        }
                    },
                    onTap = { tapOffset ->
                        onSingleTap?.invoke(tapOffset, containerSize)
                    }
                )
            }
            .pointerInput(scale, containerSize, panSensitivity) {
                if (scale > 1.02f) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val maxX = (containerSize.width * (scale - 1f)) / 2f
                        val velocityFactor = (1f + (kotlin.math.abs(dragAmount) / 25f).coerceAtMost(1.5f))
                        val acceleratedDelta = dragAmount * panSensitivity * velocityFactor
                        offsetX = (offsetX + acceleratedDelta).coerceIn(-maxX, maxX)
                    }
                }
            }
            .transformable(state = transformState, lockRotationOnZoomPan = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            content = content
        )

        // Floating Quick Reset Pill when zoomed in
        AnimatedVisibility(
            visible = scale > 1.15f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC000000),
                contentColor = Color.White,
                onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                    Text(
                        text = "${(scale * 10).toInt() / 10f}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }
            }
        }
    }
}
