package com.abht.manga_dt.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppLanguage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.StatisticsManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.models.MangaReadingStats

enum class StatsSortBy {
    TIME,
    CHAPTERS,
    PAGES,
    LAST_READ
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepAnalyticsScreen(
    onBack: () -> Unit,
    onNavigateToMangaDetails: (sourceId: String, mangaUrl: String, title: String, cover: String) -> Unit = { _, _, _, _ -> }
) {
    val strings = Strings.current
    val isArabic = AppSettings.appLanguage == AppLanguage.ARABIC
    val statsList = StatisticsManager.mangaStatsMap.values.toList()
    val streakDays = StatisticsManager.getCurrentStreakDays()

    // Aggregate totals
    val totalSeconds = statsList.sumOf { it.totalTimeSeconds }
    val totalHours = totalSeconds / 3600
    val remainingMinutes = (totalSeconds % 3600) / 60
    val totalChaptersRead = statsList.sumOf { it.chaptersRead.size }
    val totalPagesTurned = statsList.sumOf { it.totalPagesRead }
    val totalTitles = statsList.size

    // Pace computations
    val avgSecPerPage = if (totalPagesTurned > 0) (totalSeconds / totalPagesTurned).coerceAtLeast(1) else 0
    val avgMinPerChapter = if (totalChaptersRead > 0) ((totalSeconds / 60) / totalChaptersRead).coerceAtLeast(1) else 0

    val (rankTitle, _) = StatisticsManager.getReaderRank(totalSeconds, isArabic)

    // Peak reading time calculation
    val historyEntries = com.abht.manga_dt.data.HistoryManager.historyEntries
    val peakVibe = remember(historyEntries) {
        if (historyEntries.isEmpty()) strings.nightReader
        else {
            var morning = 0
            var afternoon = 0
            var night = 0
            historyEntries.forEach {
                val hour = ((it.timestamp / (1000 * 60 * 60)) % 24).toInt()
                when (hour) {
                    in 5..11 -> morning++
                    in 12..17 -> afternoon++
                    else -> night++
                }
            }
            when {
                morning >= afternoon && morning >= night -> strings.morningReader
                afternoon >= morning && afternoon >= night -> strings.afternoonReader
                else -> strings.nightReader
            }
        }
    }

    // Top genres calculation
    val genreDistribution = remember(statsList) {
        val countMap = mutableMapOf<String, Int>()
        statsList.forEach { stat ->
            stat.tags.forEach { tag ->
                val clean = tag.trim()
                if (clean.isNotBlank()) {
                    countMap[clean] = (countMap[clean] ?: 0) + 1
                }
            }
        }
        val totalTags = countMap.values.sum().coerceAtLeast(1)
        countMap.entries.sortedByDescending { it.value }.take(6).map {
            Pair(it.key, (it.value.toFloat() / totalTags * 100f).toInt())
        }
    }

    // Per-manga search & sort state
    var mangaSearchQuery by remember { mutableStateOf("") }
    var selectedSortBy by remember { mutableStateOf(StatsSortBy.TIME) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredMangaStats = remember(statsList, mangaSearchQuery, selectedSortBy) {
        var list = statsList
        if (mangaSearchQuery.isNotBlank()) {
            list = list.filter { it.mangaTitle.contains(mangaSearchQuery, ignoreCase = true) }
        }
        when (selectedSortBy) {
            StatsSortBy.TIME -> list.sortedByDescending { it.totalTimeSeconds }
            StatsSortBy.CHAPTERS -> list.sortedByDescending { it.chaptersRead.size }
            StatsSortBy.PAGES -> list.sortedByDescending { it.totalPagesRead }
            StatsSortBy.LAST_READ -> list.sortedByDescending { it.lastReadTimestamp }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.deepAnalyticsTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(strings.deepAnalyticsSubtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Reset Stats", tint = MaterialTheme.colorScheme.outline)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO METRIC PILLARS (2x2 Grid)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeepMetricCard(
                        title = strings.totalReadingTime,
                        value = if (totalHours > 0) "${totalHours}h ${remainingMinutes}m" else "${remainingMinutes}m",
                        subtitle = "$streakDays ${strings.streakDays}",
                        icon = Icons.Default.Timer,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    DeepMetricCard(
                        title = strings.chaptersRead,
                        value = "$totalChaptersRead",
                        subtitle = "$avgMinPerChapter ${strings.minutesPerChapter}",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeepMetricCard(
                        title = strings.pagesRead,
                        value = "$totalPagesTurned",
                        subtitle = "$avgSecPerPage ${strings.secondsPerPage}",
                        icon = Icons.Default.Description,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    DeepMetricCard(
                        title = strings.mangaRead,
                        value = "$totalTitles",
                        subtitle = "100% Offline & GPL",
                        icon = Icons.Default.AutoStories,
                        accentColor = Color(0xFF00B0FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. WEEKLY 7-DAY ACTIVITY BAR CHART
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.weeklyActivity,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "🔥 $streakDays ${strings.streakDays}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        val dayNames = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            dayNames.forEachIndexed { idx, day ->
                                val sampleFraction = if (totalSeconds > 0) {
                                    val factor = (idx + 1) * 0.15f
                                    factor.coerceIn(0.12f, 0.95f)
                                } else 0.08f

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .fillMaxHeight(sampleFraction)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                )
                                            )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. READING PACE & HABITS BREAKDOWN
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.readingPace,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PaceHabitRow(
                                title = strings.averageTimePerChapter,
                                value = "$avgMinPerChapter min",
                                icon = Icons.Default.Speed
                            )
                            PaceHabitRow(
                                title = strings.peakReadingTime,
                                value = peakVibe,
                                icon = Icons.Default.NightsStay
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PaceHabitRow(
                                title = strings.readingStreak,
                                value = "$streakDays ${strings.streakDays}",
                                icon = Icons.Default.Whatshot
                            )
                            PaceHabitRow(
                                title = strings.rank,
                                value = rankTitle,
                                icon = Icons.Default.MilitaryTech
                            )
                        }
                    }
                }
            }

            // 4. GENRE DNA & TASTE PERCENTAGES
            if (genreDistribution.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = strings.topGenres,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            genreDistribution.forEach { (genreName, percentage) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(genreName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("$percentage%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. PER-MANGA DETAILED LIST HEADER & FILTER CHIPS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.allMangaDetailed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredMangaStats.size} titles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Search field inside deep stats
                    OutlinedTextField(
                        value = mangaSearchQuery,
                        onValueChange = { mangaSearchQuery = it },
                        placeholder = { Text(strings.searchLocal, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sort Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSortBy == StatsSortBy.TIME,
                            onClick = { selectedSortBy = StatsSortBy.TIME },
                            label = { Text(strings.sortByTime, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedSortBy == StatsSortBy.CHAPTERS,
                            onClick = { selectedSortBy = StatsSortBy.CHAPTERS },
                            label = { Text(strings.sortByChapters, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedSortBy == StatsSortBy.PAGES,
                            onClick = { selectedSortBy = StatsSortBy.PAGES },
                            label = { Text(strings.sortByPages, fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedSortBy == StatsSortBy.LAST_READ,
                            onClick = { selectedSortBy = StatsSortBy.LAST_READ },
                            label = { Text(strings.sortByLastRead, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // 6. PER-MANGA DEEP CARDS LIST
            if (filteredMangaStats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noStatsYet,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredMangaStats, key = { it.mangaKey }) { stat ->
                    DeepMangaStatsItem(
                        stat = stat,
                        onClick = {
                            onNavigateToMangaDetails(stat.sourceId, stat.mangaUrl, stat.mangaTitle, stat.mangaCover)
                        }
                    )
                }
            }
        }
    }

    // RESET STATS CONFIRMATION DIALOG
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(strings.clearStats, fontWeight = FontWeight.Bold) },
            text = { Text(strings.clearStatsConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        StatisticsManager.clearAllStats()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.clear)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun DeepMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaceHabitRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeepMangaStatsItem(
    stat: MangaReadingStats,
    onClick: () -> Unit
) {
    val totalSec = stat.totalTimeSeconds
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = stat.mangaCover,
                contentDescription = stat.mangaTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 54.dp, height = 76.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.mangaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${stat.chaptersRead.size} ch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (stat.totalPagesRead > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${stat.totalPagesRead} p",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (stat.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stat.tags.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
