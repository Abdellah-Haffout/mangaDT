package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.*
import com.abht.manga_dt.models.*
import com.abht.manga_dt.ui.components.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileStatsScreen(
    onBack: () -> Unit = {},
    onNavigateToMangaDetails: (sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit,
    onNavigateToReader: (HistoryEntry) -> Unit = {},
    onNavigateToDeepAnalytics: () -> Unit = {},
    showBackButton: Boolean = true
) {
    val strings = Strings.current
    val isArabic = AppSettings.appLanguage == AppLanguage.ARABIC

    val mangaStats = remember(StatisticsManager.mangaStatsMap) {
        StatisticsManager.mangaStatsMap.values.sortedByDescending { it.totalTimeSeconds }
    }
    val userProfile = StatisticsManager.userProfile

    val totalTimeSeconds = remember(mangaStats) { StatisticsManager.getTotalReadingTimeSeconds() }
    val totalChaptersRead = remember(mangaStats) { StatisticsManager.getTotalChaptersReadCount() }
    val totalPagesRead = remember(mangaStats) { StatisticsManager.getTotalPagesReadCount() }
    val totalMangaCount = remember(mangaStats) { StatisticsManager.getTotalMangaCount() }
    val streakDays = remember(StatisticsManager.dailyRecordsMap) { StatisticsManager.getCurrentStreakDays() }
    val weeklyActivity = remember(StatisticsManager.dailyRecordsMap) { StatisticsManager.getWeeklyActivityDetails() }
    val topGenres = remember(mangaStats) { StatisticsManager.getTopGenres() }
    val (rankTitle, rankProgress) = remember(totalTimeSeconds) { StatisticsManager.getReaderRank(totalTimeSeconds, isArabic) }

    // Live library & history data for persona showcase
    val historyEntries: List<HistoryEntry> = HistoryManager.historyEntries
    val libraryItems: List<LibraryManga> = LibraryManager.libraryItems

    val latestHistoryItem = remember(historyEntries) { historyEntries.firstOrNull() }

    val favoriteMangaList = remember(libraryItems, mangaStats) {
        val fromLibrary = libraryItems.filter {
            it.category.equals("Favorites", ignoreCase = true) ||
            it.category.equals("المفضلة", ignoreCase = true)
        }
        if (fromLibrary.isNotEmpty()) fromLibrary
        else {
            // If none explicitly in Favorites, suggest top 5 most read
            mangaStats.take(5).map {
                LibraryManga(
                    id = it.mangaKey,
                    title = it.mangaTitle,
                    thumbnailUrl = it.mangaCover,
                    sourceId = it.sourceId,
                    mangaUrl = it.mangaUrl,
                    category = "Favorites"
                )
            }
        }
    }

    val planToReadList = remember(libraryItems) {
        libraryItems.filter {
            it.category.equals("Plan to Read", ignoreCase = true) ||
            it.category.equals("أنوي قراءتها", ignoreCase = true) ||
            it.category.equals("لاحقاً", ignoreCase = true)
        }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearStatsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showEditProfileDialog) { showEditProfileDialog = false }
    BackHandler(enabled = showClearStatsDialog) { showClearStatsDialog = false }

    val heroBannerCover = remember(latestHistoryItem, favoriteMangaList) {
        latestHistoryItem?.mangaCover?.ifBlank { null }
            ?: favoriteMangaList.firstOrNull()?.thumbnailUrl?.ifBlank { null }
            ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.profileAndStats, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDeepAnalytics) {
                        Icon(Icons.Default.Insights, contentDescription = strings.deepAnalyticsTitle, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = strings.editProfile)
                    }
                    IconButton(onClick = { showClearStatsDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = strings.clearStats)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isThreePaneScreen = maxWidth >= 980.dp

            if (isThreePaneScreen) {
                // =========================================================================
                // 🖥️ 3-COLUMN UNIFIED DASHBOARD FOR LARGE SCREENS & DESKTOP
                // Column 1: Standard Profile / Identity (الهوية والبروفايل)
                // Column 2: Overview & Taste (النشاط والذوق)
                // Column 3: Deep Analytics & Per-Manga Stats (التحليلات والإحصائيات)
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // -----------------------------------------------------------------
                    // 1️⃣ COLUMN 1: STANDARD PROFILE / IDENTITY (Width: 320.dp)
                    // -----------------------------------------------------------------
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SectionHeaderTitle(title = strings.profile, icon = Icons.Default.Person)

                        // Profile Hero Card
                        ProfileHeroCard(
                            userProfile = userProfile,
                            heroBannerCover = heroBannerCover,
                            rankTitle = rankTitle,
                            rankProgress = rankProgress,
                            streakDays = streakDays,
                            streakDaysLabel = strings.streakDays,
                            totalTimeSeconds = totalTimeSeconds,
                            totalChaptersRead = totalChaptersRead,
                            totalMangaCount = totalMangaCount,
                            strings = strings,
                            isArabic = isArabic,
                            onEditClick = { showEditProfileDialog = true }
                        )

                        // Reading Vibe Insight Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReadingVibeCard(
                                modifier = Modifier.weight(1f),
                                title = if (isArabic) "قارئ ليلي" else "Night Reader",
                                subtitle = if (isArabic) "أعلى تركيز" else "Peak mood",
                                icon = Icons.Default.NightsStay,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            ReadingVibeCard(
                                modifier = Modifier.weight(1f),
                                title = if (isArabic) "المعدل اليومي" else "Daily Flow",
                                subtitle = StatisticsManager.formatDuration((totalTimeSeconds / streakDays.coerceAtLeast(1)), isArabic),
                                icon = Icons.Default.Speed,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Local Privacy & GPL Badge
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.privacyLocalNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // 2️⃣ COLUMN 2: OVERVIEW & TASTE (Weight: 1f)
                    // -----------------------------------------------------------------
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // A. Currently Reading Spotlight
                        item {
                            SectionHeaderTitle(title = strings.currentlyReading, icon = Icons.Default.PlayCircleFilled)
                            Spacer(Modifier.height(6.dp))

                            if (latestHistoryItem != null) {
                                CurrentlyReadingSpotlightCard(
                                    item = latestHistoryItem,
                                    strings = strings,
                                    onCardClick = {
                                        onNavigateToMangaDetails(
                                            latestHistoryItem.sourceId,
                                            latestHistoryItem.mangaUrl,
                                            latestHistoryItem.mangaTitle,
                                            latestHistoryItem.mangaCover
                                        )
                                    },
                                    onResumeClick = { onNavigateToReader(latestHistoryItem) }
                                )
                            } else {
                                EmptySectionCard(message = strings.noCurrentReading)
                            }
                        }

                        // B. My Favorites Showcase
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeaderTitle(title = strings.myFavorites, icon = Icons.Default.Favorite)
                                if (favoriteMangaList.isNotEmpty()) {
                                    Text(text = "${favoriteMangaList.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(6.dp))

                            if (favoriteMangaList.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(favoriteMangaList, key = { it.id }) { item ->
                                        ShowcaseMangaCard(
                                            title = item.title,
                                            coverUrl = item.thumbnailUrl,
                                            subtitle = item.sourceId,
                                            onClick = {
                                                onNavigateToMangaDetails(item.sourceId, item.mangaUrl, item.title, item.thumbnailUrl)
                                            }
                                        )
                                    }
                                }
                            } else {
                                EmptySectionCard(message = strings.noFavoritesYet)
                            }
                        }

                        // C. Plan to Read Queue
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeaderTitle(title = strings.planToRead, icon = Icons.Default.BookmarkBorder)
                                if (planToReadList.isNotEmpty()) {
                                    Text(text = "${planToReadList.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(6.dp))

                            if (planToReadList.isNotEmpty()) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(planToReadList, key = { it.id }) { item ->
                                        ShowcaseMangaCard(
                                            title = item.title,
                                            coverUrl = item.thumbnailUrl,
                                            subtitle = item.sourceId,
                                            onClick = {
                                                onNavigateToMangaDetails(item.sourceId, item.mangaUrl, item.title, item.thumbnailUrl)
                                            }
                                        )
                                    }
                                }
                            } else {
                                EmptySectionCard(message = strings.noPlanToReadYet)
                            }
                        }

                        // D. Taste DNA & Top Genres
                        item {
                            SectionHeaderTitle(title = strings.tasteDna, icon = Icons.Default.Grain)
                            Spacer(Modifier.height(6.dp))
                            TasteDnaCard(topGenres = topGenres, strings = strings)
                        }
                    }

                    // -----------------------------------------------------------------
                    // 3️⃣ COLUMN 3: DEEP ANALYTICS & STATS (Weight: 1f)
                    // -----------------------------------------------------------------
                    LazyColumn(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Overview Metrics Cards (2x2 Grid)
                        item {
                            SectionHeaderTitle(title = strings.detailedAnalytics, icon = Icons.Default.Analytics)
                            Spacer(Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.totalReadingTime,
                                        value = StatisticsManager.formatDuration(totalTimeSeconds, isArabic),
                                        icon = Icons.Default.Timer,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.chaptersRead,
                                        value = "$totalChaptersRead",
                                        icon = Icons.AutoMirrored.Filled.MenuBook,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.pagesRead,
                                        value = "$totalPagesRead",
                                        icon = Icons.Default.AutoStories,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.mangaRead,
                                        value = "$totalMangaCount",
                                        icon = Icons.Default.CollectionsBookmark,
                                        tint = Color(0xFF00897B)
                                    )
                                }
                            }
                        }

                        // Weekly Activity Visual Chart
                        item {
                            WeeklyActivityCard(weeklyActivity = weeklyActivity, isArabic = isArabic, strings = strings)
                        }

                        // Per-Manga Detailed Stats Title & List
                        item {
                            SectionHeaderTitle(title = strings.mangaStatisticsTitle, icon = Icons.Default.FormatListNumbered)
                        }

                        if (mangaStats.isEmpty()) {
                            item {
                                EmptySectionCard(message = strings.noStatsYet)
                            }
                        } else {
                            items(mangaStats, key = { it.mangaKey }) { manga ->
                                PerMangaStatCard(
                                    stats = manga,
                                    isArabic = isArabic,
                                    onClick = {
                                        onNavigateToMangaDetails(manga.sourceId, manga.mangaUrl, manga.mangaTitle, manga.mangaCover)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // 📱 MOBILE SINGLE UNIFIED ALL-IN-ONE FLOW
                // =========================================================================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (AppSettings.floatingNavBar) 88.dp else 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profile Hero Card (Part 1: Profile)
                    item {
                        ProfileHeroCard(
                            userProfile = userProfile,
                            heroBannerCover = heroBannerCover,
                            rankTitle = rankTitle,
                            rankProgress = rankProgress,
                            streakDays = streakDays,
                            streakDaysLabel = strings.streakDays,
                            totalTimeSeconds = totalTimeSeconds,
                            totalChaptersRead = totalChaptersRead,
                            totalMangaCount = totalMangaCount,
                            strings = strings,
                            isArabic = isArabic,
                            onEditClick = { showEditProfileDialog = true },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 2. Currently Reading Spotlight (Part 2: Overview & Taste)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeaderTitle(title = strings.currentlyReading, icon = Icons.Default.PlayCircleFilled)
                            Spacer(Modifier.height(8.dp))

                            if (latestHistoryItem != null) {
                                CurrentlyReadingSpotlightCard(
                                    item = latestHistoryItem,
                                    strings = strings,
                                    onCardClick = {
                                        onNavigateToMangaDetails(
                                            latestHistoryItem.sourceId,
                                            latestHistoryItem.mangaUrl,
                                            latestHistoryItem.mangaTitle,
                                            latestHistoryItem.mangaCover
                                        )
                                    },
                                    onResumeClick = { onNavigateToReader(latestHistoryItem) }
                                )
                            } else {
                                EmptySectionCard(message = strings.noCurrentReading)
                            }
                        }
                    }

                    // 3. Favorites Showcase (Part 2: Overview & Taste)
                    item {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeaderTitle(title = strings.myFavorites, icon = Icons.Default.Favorite)
                                if (favoriteMangaList.isNotEmpty()) {
                                    Text(text = "${favoriteMangaList.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            if (favoriteMangaList.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(favoriteMangaList, key = { it.id }) { item ->
                                        ShowcaseMangaCard(
                                            title = item.title,
                                            coverUrl = item.thumbnailUrl,
                                            subtitle = item.sourceId,
                                            onClick = {
                                                onNavigateToMangaDetails(item.sourceId, item.mangaUrl, item.title, item.thumbnailUrl)
                                            }
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    EmptySectionCard(message = strings.noFavoritesYet)
                                }
                            }
                        }
                    }

                    // 4. Plan to Read Queue (Part 2: Overview & Taste)
                    item {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeaderTitle(title = strings.planToRead, icon = Icons.Default.BookmarkBorder)
                                if (planToReadList.isNotEmpty()) {
                                    Text(text = "${planToReadList.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            if (planToReadList.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(planToReadList, key = { it.id }) { item ->
                                        ShowcaseMangaCard(
                                            title = item.title,
                                            coverUrl = item.thumbnailUrl,
                                            subtitle = item.sourceId,
                                            onClick = {
                                                onNavigateToMangaDetails(item.sourceId, item.mangaUrl, item.title, item.thumbnailUrl)
                                            }
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    EmptySectionCard(message = strings.noPlanToReadYet)
                                }
                            }
                        }
                    }

                    // 5. Taste DNA & Genres (Part 2: Overview & Taste)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SectionHeaderTitle(title = strings.tasteDna, icon = Icons.Default.Grain)
                            Spacer(Modifier.height(8.dp))
                            TasteDnaCard(topGenres = topGenres, strings = strings)
                        }
                    }

                    // Deep Analytics Launcher Banner
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDeepAnalytics() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Insights,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = strings.deepAnalyticsTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = strings.deepAnalyticsSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = onNavigateToDeepAnalytics,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(strings.openDeepAnalytics, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 6. Overview Metrics (Part 3: Deep Analytics)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SectionHeaderTitle(title = strings.detailedAnalytics, icon = Icons.Default.Analytics)
                            Spacer(Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.totalReadingTime,
                                        value = StatisticsManager.formatDuration(totalTimeSeconds, isArabic),
                                        icon = Icons.Default.Timer,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.chaptersRead,
                                        value = "$totalChaptersRead",
                                        icon = Icons.AutoMirrored.Filled.MenuBook,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.pagesRead,
                                        value = "$totalPagesRead",
                                        icon = Icons.Default.AutoStories,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    StatMetricCard(
                                        modifier = Modifier.weight(1f),
                                        title = strings.mangaRead,
                                        value = "$totalMangaCount",
                                        icon = Icons.Default.CollectionsBookmark,
                                        tint = Color(0xFF00897B)
                                    )
                                }
                            }
                        }
                    }

                    // 7. Weekly Activity Chart (Part 3: Deep Analytics)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            WeeklyActivityCard(weeklyActivity = weeklyActivity, isArabic = isArabic, strings = strings)
                        }
                    }

                    // 8. Reading Vibe Cards
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SectionHeaderTitle(title = strings.readingVibe, icon = Icons.Default.Bedtime)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReadingVibeCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (isArabic) "قارئ ليلي" else "Night Reader",
                                    subtitle = if (isArabic) "أعلى تركيز قرائي" else "Peak reading mood",
                                    icon = Icons.Default.NightsStay,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                ReadingVibeCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (isArabic) "المعدل اليومي" else "Daily Flow",
                                    subtitle = StatisticsManager.formatDuration((totalTimeSeconds / streakDays.coerceAtLeast(1)), isArabic),
                                    icon = Icons.Default.Speed,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 9. Privacy & GPL Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = strings.privacyLocalNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // 10. Per-Manga Detailed Stats Title & Items (Part 3: Deep Analytics)
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            SectionHeaderTitle(title = strings.mangaStatisticsTitle, icon = Icons.Default.FormatListNumbered)
                        }
                    }

                    if (mangaStats.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EmptySectionCard(message = strings.noStatsYet)
                            }
                        }
                    } else {
                        items(mangaStats, key = { it.mangaKey }) { manga ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                PerMangaStatCard(
                                    stats = manga,
                                    isArabic = isArabic,
                                    onClick = {
                                        onNavigateToMangaDetails(manga.sourceId, manga.mangaUrl, manga.mangaTitle, manga.mangaCover)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userProfile.username) }
        var tempBio by remember { mutableStateOf(userProfile.bio) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(strings.editProfile, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(strings.username) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text(strings.bio) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        StatisticsManager.updateUserProfile(tempName.ifBlank { "Manga Reader" }, tempBio, userProfile.avatarId)
                        showEditProfileDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Reset Stats Dialog
    if (showClearStatsDialog) {
        AlertDialog(
            onDismissRequest = { showClearStatsDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.clearStats, fontWeight = FontWeight.Bold) },
            text = { Text(strings.clearStatsConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        StatisticsManager.clearAllStats()
                        showClearStatsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.clear)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearStatsDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun ProfileHeroCard(
    userProfile: com.abht.manga_dt.models.UserProfileData,
    heroBannerCover: String,
    rankTitle: String,
    rankProgress: Float,
    streakDays: Int,
    streakDaysLabel: String,
    totalTimeSeconds: Long,
    totalChaptersRead: Int,
    totalMangaCount: Int,
    strings: AppStrings,
    isArabic: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (heroBannerCover.isNotBlank()) {
                AsyncImage(
                    model = heroBannerCover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .blur(16.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (heroBannerCover.isNotBlank()) 20.dp else 4.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.surfaceContainerHigh),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = userProfile.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = userProfile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(rankTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFF6D00).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6D00), modifier = Modifier.size(16.dp))
                            Text("$streakDays $streakDaysLabel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFF6D00))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Quick Capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickStatCapsule(
                        label = strings.totalReadingTime,
                        value = StatisticsManager.formatDuration(totalTimeSeconds, isArabic),
                        icon = Icons.Default.Timer
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    QuickStatCapsule(
                        label = strings.chaptersRead,
                        value = "$totalChaptersRead",
                        icon = Icons.AutoMirrored.Filled.MenuBook
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    QuickStatCapsule(
                        label = strings.mangaRead,
                        value = "$totalMangaCount",
                        icon = Icons.Default.CollectionsBookmark
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Rank Level Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = strings.rank, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(text = "${(rankProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { rankProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentlyReadingSpotlightCard(
    item: HistoryEntry,
    strings: AppStrings,
    onCardClick: () -> Unit,
    onResumeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(width = 68.dp, height = 96.dp)
            ) {
                AsyncImage(
                    model = item.mangaCover,
                    contentDescription = item.mangaTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.mangaTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.chapterTitle.isNotBlank()) {
                    Text(
                        text = item.chapterTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (item.lastPage > 0) {
                    Text(
                        text = "${strings.pageFormat(item.lastPage)} / ${item.totalPages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onResumeClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.resumeReading, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TasteDnaCard(
    topGenres: List<Pair<String, Int>>,
    strings: AppStrings
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (topGenres.isNotEmpty()) {
                val maxCount = topGenres.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                topGenres.take(5).forEach { (genre, count) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = genre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "$count ${strings.chapters}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (count.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = strings.noStatsYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyActivityCard(
    weeklyActivity: List<DayActivity>,
    isArabic: Boolean,
    strings: AppStrings
) {
    var showPagesMode by remember { mutableStateOf(false) }

    val totalWeeklyMinutes = weeklyActivity.sumOf { it.minutes }
    val totalWeeklyPages = weeklyActivity.sumOf { it.pages }

    val maxVal = if (showPagesMode) {
        weeklyActivity.maxOfOrNull { it.pages.toLong() }?.coerceAtLeast(10L) ?: 10L
    } else {
        weeklyActivity.maxOfOrNull { it.minutes }?.coerceAtLeast(10L) ?: 10L
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row with Title and Stats Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = strings.weeklyActivity,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "إجمالي الأسبوع: $totalWeeklyMinutes د • $totalWeeklyPages صفحة"
                            else "Weekly: ${totalWeeklyMinutes}m • $totalWeeklyPages pages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Switch between Minutes and Pages
                FilledTonalButton(
                    onClick = { showPagesMode = !showPagesMode },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (showPagesMode) (if (isArabic) "صفحات" else "Pages") else (if (isArabic) "دقائق" else "Minutes"),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Chart Bars Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyActivity.forEach { day ->
                    val value = if (showPagesMode) day.pages.toLong() else day.minutes
                    val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f)
                    val unitLabel = if (showPagesMode) (if (isArabic) "ص" else "p") else (if (isArabic) "د" else "m")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Value label on top
                        if (value > 0) {
                            Text(
                                text = "$value$unitLabel",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.height(3.dp))
                        } else {
                            Spacer(Modifier.height(16.dp))
                        }

                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.52f)
                                .fillMaxHeight(ratio * 0.70f)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (day.isToday && value > 0) {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    } else if (value > 0) {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                )
                        )

                        Spacer(Modifier.height(6.dp))

                        // Day name
                        Text(
                            text = if (isArabic) day.dayNameAr else day.dayNameEn,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date number
                        Text(
                            text = day.dateKey.takeLast(2),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (day.isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                        )

                        // Today dot indicator
                        if (day.isToday) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStatCapsule(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SectionHeaderTitle(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ShowcaseMangaCard(
    title: String,
    coverUrl: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.width(115.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptySectionCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReadingVibeCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PerMangaStatCard(
    stats: MangaReadingStats,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(width = 54.dp, height = 76.dp)
            ) {
                AsyncImage(
                    model = stats.mangaCover,
                    contentDescription = stats.mangaTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stats.mangaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (stats.sourceId.isNotBlank()) {
                    Text(
                        text = stats.sourceId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = StatisticsManager.formatDuration(stats.totalTimeSeconds, isArabic),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Text(
                                text = "${stats.chaptersRead.size} ${if (isArabic) "فصل" else "ch"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (stats.totalPagesRead > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${stats.totalPagesRead} ${if (isArabic) "صفحة" else "p"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
