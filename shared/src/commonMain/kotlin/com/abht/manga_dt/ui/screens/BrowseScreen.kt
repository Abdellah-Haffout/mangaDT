package com.abht.manga_dt.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.MangaSource
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.mangasource.BuiltinMangaSources
import com.abht.manga_dt.ui.models.LayoutMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    sources: List<MangaSource>,
    onSourceClick: (MangaSource) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val layoutMode = AppSettings.browseLayoutMode
    val isAscending = AppSettings.browseSortAscending
    val selectedLanguage = AppSettings.browseSelectedLanguage
    val selectedContentType = AppSettings.browseSelectedContentType
    val pinnedSourceIds = AppSettings.pinnedSourceIds
    var showOnlyPinned by remember { mutableStateOf(false) }
    var selectedProviderFilter by remember { mutableStateOf("ALL") }

    // Distinct available languages
    val availableLanguages = remember(sources) {
        listOf("ALL") + sources.mapNotNull { it.locale?.uppercase() }.distinct().sorted()
    }

    val isNsfwAllowed = AppSettings.isNsfwAllowed

    // Filtered & Sorted sources
    val processedSources = remember(sources, searchQuery, isAscending, selectedLanguage, selectedContentType, pinnedSourceIds, showOnlyPinned, isNsfwAllowed) {
        var list = sources

        // NSFW / 18+ Filter
        if (!isNsfwAllowed) {
            list = list.filter { it.contentType != "HENTAI" && !it.name.contains("hentai", ignoreCase = true) && !it.id.contains("hentai", ignoreCase = true) }
        }

        // Language Filter
        if (selectedLanguage != "ALL") {
            list = list.filter { it.locale?.equals(selectedLanguage, ignoreCase = true) == true }
        }

        // Content Type Filter
        if (selectedContentType != "ALL") {
            list = list.filter { it.contentType?.equals(selectedContentType, ignoreCase = true) == true }
        }

        // Pinned only filter
        if (showOnlyPinned) {
            list = list.filter { pinnedSourceIds.contains(it.id) }
        }

        // Provider Filter
        if (selectedProviderFilter == "MANGA_SOURCE") {
            list = list.filter { BuiltinMangaSources.isMangaSourceId(it.id) }
        } else if (selectedProviderFilter == "KOTATSU") {
            list = list.filter { !BuiltinMangaSources.isMangaSourceId(it.id) }
        }

        // Search Query Filter
        if (searchQuery.isNotBlank()) {
            list = list.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.id.contains(searchQuery, ignoreCase = true) ||
                (it.locale?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // Sort: Pinned sources first, then by name
        val comparator = if (isAscending) {
            compareByDescending<MangaSource> { pinnedSourceIds.contains(it.id) }
                .thenBy { it.name.lowercase() }
        } else {
            compareByDescending<MangaSource> { pinnedSourceIds.contains(it.id) }
                .thenByDescending { it.name.lowercase() }
        }

        list.sortedWith(comparator)
    }

    Column(modifier = Modifier.fillMaxSize()) {
            // Kotatsu Style Filter Bar (Languages & Pinned & Types & Providers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinned Filter Chip
                FilterChip(
                    selected = showOnlyPinned,
                    onClick = { showOnlyPinned = !showOnlyPinned },
                    label = { 
                        Text("Pinned (${pinnedSourceIds.size})") 
                    },
                    leadingIcon = {
                        Icon(
                            if (showOnlyPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (showOnlyPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                )

                // Provider Filter Chips (if both enabled)
                if (AppSettings.enableKotatsuSources && AppSettings.enableMangaSources) {
                    FilterChip(
                        selected = selectedProviderFilter == "MANGA_SOURCE",
                        onClick = {
                            selectedProviderFilter = if (selectedProviderFilter == "MANGA_SOURCE") "ALL" else "MANGA_SOURCE"
                        },
                        label = { Text("⚡ manga-source") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )

                    FilterChip(
                        selected = selectedProviderFilter == "KOTATSU",
                        onClick = {
                            selectedProviderFilter = if (selectedProviderFilter == "KOTATSU") "ALL" else "KOTATSU"
                        },
                        label = { Text("🌐 Kotatsu") }
                    )
                }

                // Language Chips
                val commonLanguages = listOf("ALL", "EN", "AR", "RU", "JA", "ES", "FR", "PT", "ID", "IT", "DE")
                commonLanguages.filter { lang -> lang == "ALL" || availableLanguages.contains(lang) }.forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang && !showOnlyPinned && selectedProviderFilter == "ALL",
                        onClick = {
                            showOnlyPinned = false
                            AppSettings.setBrowseLanguage(lang)
                        },
                        label = { Text(if (lang == "ALL") "All Langs" else lang) }
                    )
                }

                // Content Type Filter: 18+ (Hentai) Toggle
                FilterChip(
                    selected = selectedContentType == "HENTAI",
                    onClick = {
                        val nextType = if (selectedContentType == "HENTAI") "ALL" else "HENTAI"
                        AppSettings.setBrowseContentType(nextType)
                    },
                    label = { Text("18+ (Hentai)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            if (sources.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (processedSources.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No sources found", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                when (layoutMode) {
                    LayoutMode.LIST -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = if (AppSettings.floatingNavBar) 84.dp else 0.dp)
                        ) {
                            items(
                                items = processedSources,
                                key = { it.id }
                            ) { source ->
                                val isPinned = pinnedSourceIds.contains(source.id)
                                KotatsuSourceListItem(
                                    source = source,
                                    isPinned = isPinned,
                                    onPinToggle = { AppSettings.togglePinnedSource(source.id) },
                                    onClick = { onSourceClick(source) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                    LayoutMode.COMFORTABLE -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = if (AppSettings.floatingNavBar) 84.dp else 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = processedSources,
                                key = { it.id }
                            ) { source ->
                                val isPinned = pinnedSourceIds.contains(source.id)
                                KotatsuSourceComfortableGridItem(
                                    source = source,
                                    isPinned = isPinned,
                                    onPinToggle = { AppSettings.togglePinnedSource(source.id) },
                                    onClick = { onSourceClick(source) }
                                )
                            }
                        }
                    }
                    LayoutMode.COMPACT -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 170.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (AppSettings.floatingNavBar) 84.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = processedSources,
                                key = { it.id }
                            ) { source ->
                                val isPinned = pinnedSourceIds.contains(source.id)
                                KotatsuSourceCompactGridItem(
                                    source = source,
                                    isPinned = isPinned,
                                    onPinToggle = { AppSettings.togglePinnedSource(source.id) },
                                    onClick = { onSourceClick(source) }
                                )
                            }
                        }
                    }
                }
            }
        }
}

/** Kotatsu-Redo Style List Item **/
@Composable
fun KotatsuSourceListItem(
    source: MangaSource,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source Icon Box
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            ) {
                AsyncImage(
                    model = source.iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(MaterialTheme.shapes.small),
                    error = rememberVectorPainter(Icons.Default.Language),
                    fallback = rememberVectorPainter(Icons.Default.Language)
                )
            }

            Spacer(Modifier.width(14.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Language Chip Badge
                    source.locale?.let { locale ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = locale.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Manga-Source Provider Tag
                    if (BuiltinMangaSources.isMangaSourceId(source.id)) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "manga-source",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Content Type Badge (if special)
                    if (source.contentType == "HENTAI") {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "18+",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Broken Indicator
                    if (source.isBroken) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                text = "Broken",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Pin / Favorite Icon Button
            IconButton(onClick = onPinToggle) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/** Kotatsu-Redo Style Comfortable Grid Item **/
@Composable
fun KotatsuSourceComfortableGridItem(
    source: MangaSource,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Language tag top-left
            source.locale?.let { locale ->
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = locale.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Pin icon top-right
            IconButton(
                onClick = onPinToggle,
                modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: Icon & Title
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(44.dp)
                ) {
                    AsyncImage(
                        model = source.iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .clip(MaterialTheme.shapes.small),
                        error = rememberVectorPainter(Icons.Default.Language),
                        fallback = rememberVectorPainter(Icons.Default.Language)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (BuiltinMangaSources.isMangaSourceId(source.id)) {
                    Spacer(Modifier.height(3.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "manga-source",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Kotatsu-Redo Style Compact Grid Item **/
@Composable
fun KotatsuSourceCompactGridItem(
    source: MangaSource,
    isPinned: Boolean,
    onPinToggle: () -> Unit,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = source.iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(26.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                error = rememberVectorPainter(Icons.Default.Language),
                fallback = rememberVectorPainter(Icons.Default.Language)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    source.locale?.let { locale ->
                        Text(
                            text = locale.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (BuiltinMangaSources.isMangaSourceId(source.id)) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "manga-source",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = onPinToggle,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

