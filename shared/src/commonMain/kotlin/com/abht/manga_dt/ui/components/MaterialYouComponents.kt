package com.abht.manga_dt.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.models.HistoryEntry
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus

/**
 * Modes of Search available in Manga DT
 */
enum class HomeSearchMode(val icon: ImageVector) {
    MANGA(Icons.Default.Public),
    LOCAL(Icons.Default.CollectionsBookmark),
    SOURCES(Icons.Default.Explore);

    @Composable
    fun getLabel(): String = when (this) {
        MANGA -> Strings.current.onlineMangaMode
        LOCAL -> Strings.current.localLibraryMode
        SOURCES -> Strings.current.sourcesMode
    }
}

/**
 * Material 3 Expressive Pill Search Bar with Mode Selector Integration
 */
@Composable
fun MaterialYouSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchMode: HomeSearchMode,
    onSearchModeChange: (HomeSearchMode) -> Unit,
    onClear: () -> Unit = { onQueryChange("") },
    modifier: Modifier = Modifier
) {
    val strings = Strings.current
    val placeholderText = when (searchMode) {
        HomeSearchMode.MANGA -> strings.searchManga
        HomeSearchMode.LOCAL -> strings.searchLocal
        HomeSearchMode.SOURCES -> strings.searchSources
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                searchMode.icon,
                contentDescription = searchMode.getLabel(),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = placeholderText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = strings.clear,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Search Mode Switcher Chips
 */
@Composable
fun MaterialYouSearchModeChips(
    selectedMode: HomeSearchMode,
    onModeSelected: (HomeSearchMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeSearchMode.values().forEach { mode ->
            val isSelected = selectedMode == mode
            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = mode.getLabel(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        mode.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * Material 3 Tonal Category Capsule
 */
@Composable
fun MaterialYouCategoryCapsule(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
    ).value

    val textColor = animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ).value

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}

/**
 * Material 3 Filter Dropdown Pill
 */
@Composable
fun MaterialYouFilterDropdownPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showDropDownArrow: Boolean = true
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showDropDownArrow) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun MaterialYouMangaPosterCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = AppSettings.homeCardCornersDp,
    showSourceBadge: Boolean = AppSettings.homeShowSourceBadge,
    showRatingBadge: Boolean = AppSettings.homeShowRatingBadge
) {
    val strings = Strings.current
    val cornerRadius = cornerRadiusDp.dp

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(cornerRadius)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isMangaNsfw = manga.isNsfw || AppSettings.isMangaNsfw(manga.isNsfw, manga.tags)
            val shouldShieldCover = isMangaNsfw && AppSettings.nsfwBlurCovers

            // 1. High Resolution Cover Artwork
            AsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = rememberVectorPainter(Icons.Default.BrokenImage)
            )

            // NSFW Privacy Shield overlay if enabled
            if (shouldShieldCover) {
                NsfwCoverShield(modifier = Modifier.fillMaxSize())
            }

            // 2. High Legibility Dark Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // 3. Status Badge (Top End) - ONLY shown if status is known
            if (manga.status != MangaStatus.UNKNOWN) {
                val (statusBg, statusFg, statusText) = when (manga.status) {
                    MangaStatus.ONGOING -> Triple(Color(0xFF2E7D32).copy(alpha = 0.90f), Color.White, strings.statusOngoing)
                    MangaStatus.COMPLETED -> Triple(Color(0xFF1565C0).copy(alpha = 0.90f), Color.White, strings.statusCompleted)
                    MangaStatus.ON_HOLD -> Triple(Color(0xFFEF6C00).copy(alpha = 0.90f), Color.White, strings.statusOnHold)
                    MangaStatus.DROPPED -> Triple(Color(0xFFC62828).copy(alpha = 0.90f), Color.White, strings.statusDropped)
                    MangaStatus.UNKNOWN -> Triple(Color.Transparent, Color.Transparent, "")
                }

                Surface(
                    shape = RoundedCornerShape(topEnd = cornerRadius, bottomStart = 8.dp),
                    color = statusBg,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 4. Manga Metadata
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Title
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showSourceBadge || (showRatingBadge && manga.rating > 0f)) {
                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (showSourceBadge) {
                            Text(
                                text = manga.source,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        // Real Rating Badge
                        if (showRatingBadge && manga.rating > 0f) {
                            val ratingDisplay = if (manga.rating > 10f) {
                                "${((manga.rating / 10f) * 10).toInt() / 10f}"
                            } else {
                                "${((manga.rating) * 10).toInt() / 10f}"
                            }

                            Spacer(Modifier.width(6.dp))

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF5C518),
                                modifier = Modifier.height(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 5.dp)
                                ) {
                                    Text(
                                        text = "⭐ $ratingDisplay",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.Black
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

/**
 * Material 3 Detailed Manga List Card (for List Layout Mode)
 */
@Composable
fun MaterialYouMangaListCard(
    manga: Manga,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = AppSettings.homeCardCornersDp
) {
    val cornerRadius = cornerRadiusDp.dp

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Thumbnail
            Card(
                shape = RoundedCornerShape(cornerRadius.coerceAtMost(10.dp)),
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = rememberVectorPainter(Icons.Default.BrokenImage)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!manga.author.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source Pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = manga.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (manga.rating > 0f) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF5C518).copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "⭐ ${manga.rating}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 Continue Reading Card for Shortcuts
 */
@Composable
fun MaterialYouContinueReadingCard(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onResumeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = Strings.current

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .width(200.dp)
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = entry.mangaCover,
                contentDescription = entry.mangaTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp)),
                error = rememberVectorPainter(Icons.Default.BrokenImage)
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.mangaTitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = strings.chapterFormat(entry.chapterTitle.replace(Regex("(?i)chapter\\s*"), "").trim()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (entry.lastPage > 1) {
                    Text(
                        text = strings.pageFormat(entry.lastPage),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                onClick = { onResumeClick?.invoke() ?: onClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = strings.resumeReading,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Section Header
 */
@Composable
fun MaterialYouSectionHeader(
    title: String,
    badgeCount: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$badgeCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Discreet Privacy & 18+ Shield Overlay for sensitive manga covers
 */
@Composable
fun NsfwCoverShield(
    modifier: Modifier = Modifier,
    badgeText: String = "18+"
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = "18+ Sensitive Content",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
