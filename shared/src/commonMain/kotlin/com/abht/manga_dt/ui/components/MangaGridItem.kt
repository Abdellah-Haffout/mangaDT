package com.abht.manga_dt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus
import com.abht.manga_dt.ui.models.LayoutMode

@Composable
fun MangaItem(
    manga: Manga, 
    layoutMode: LayoutMode,
    onClick: () -> Unit
) {
    when (layoutMode) {
        LayoutMode.COMFORTABLE -> ComfortableGridItem(manga, onClick)
        LayoutMode.COMPACT -> CompactGridItem(manga, onClick)
        LayoutMode.LIST -> ListMangaItem(manga, onClick)
    }
}

@Composable
private fun ComfortableGridItem(manga: Manga, onClick: () -> Unit) {
    val isNsfw = manga.isNsfw || AppSettings.isMangaNsfw(manga.isNsfw, manga.tags)
    val shouldShield = isNsfw && AppSettings.nsfwBlurCovers
    val cornerRadius = AppSettings.homeCardCornersDp.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Gradient for badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                )

                // Bottom Gradient for badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Top-Start: Rating badge
                if (manga.rating > 0f && AppSettings.homeShowRatingBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = formatRating(manga.rating),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-End: Source badge
                if (AppSettings.homeShowSourceBadge && manga.source.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                    ) {
                        Text(
                            text = manga.source.take(12),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom-Start: Latest Chapter / Status Pill
                val bottomBadgeText = when {
                    !manga.latestChapter.isNullOrBlank() -> manga.latestChapter
                    manga.status == MangaStatus.ONGOING -> "Ongoing"
                    manga.status == MangaStatus.COMPLETED -> "Completed"
                    else -> null
                }

                if (!bottomBadgeText.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = bottomBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                if (shouldShield) {
                    NsfwCoverShield(modifier = Modifier.fillMaxSize())
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title
        Text(
            text = manga.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Metadata Subtitle (Author / Manga Type / Status)
        val subtitleInfo = listOfNotNull(
            manga.author?.takeIf { it.isNotBlank() },
            manga.mangaType?.takeIf { it.isNotBlank() },
            manga.views?.takeIf { it.isNotBlank() }?.let { "👁️ $it" },
            manga.updatedAt?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        if (subtitleInfo.isNotBlank()) {
            Text(
                text = subtitleInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactGridItem(manga: Manga, onClick: () -> Unit) {
    val isNsfw = manga.isNsfw || AppSettings.isMangaNsfw(manga.isNsfw, manga.tags)
    val shouldShield = isNsfw && AppSettings.nsfwBlurCovers
    val cornerRadius = AppSettings.homeCardCornersDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top-Start: Rating badge
        if (manga.rating > 0f && AppSettings.homeShowRatingBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(Modifier.width(1.dp))
                    Text(
                        text = formatRating(manga.rating),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        if (shouldShield) {
            NsfwCoverShield(modifier = Modifier.fillMaxSize())
        }

        // Bottom Title & Info Overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.75f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!manga.latestChapter.isNullOrBlank()) {
                    Text(
                        text = manga.latestChapter,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ListMangaItem(manga: Manga, onClick: () -> Unit) {
    val isNsfw = manga.isNsfw || AppSettings.isMangaNsfw(manga.isNsfw, manga.tags)
    val shouldShield = isNsfw && AppSettings.nsfwBlurCovers
    val cornerRadius = (AppSettings.homeCardCornersDp / 2).coerceAtLeast(4).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(72.dp, 96.dp),
            shape = RoundedCornerShape(cornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = manga.thumbnailUrl,
                    contentDescription = manga.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (manga.rating > 0f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(Modifier.width(1.dp))
                            Text(
                                text = formatRating(manga.rating),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                if (shouldShield) {
                    NsfwCoverShield(modifier = Modifier.fillMaxSize())
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Row 1: Title + Source Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (manga.source.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = manga.source,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Row 2: Author / Status / Manga Type
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status indicator dot
                val (statusColor, statusText) = when (manga.status) {
                    MangaStatus.ONGOING -> Color(0xFF4CAF50) to "مستمرة"
                    MangaStatus.COMPLETED -> Color(0xFF2196F3) to "مكتملة"
                    MangaStatus.ON_HOLD -> Color(0xFFFF9800) to "متوقفة"
                    MangaStatus.DROPPED -> Color(0xFFF44336) to "ملغية"
                    MangaStatus.UNKNOWN -> MaterialTheme.colorScheme.outline to null
                }

                if (statusText != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                                color = statusColor
                            )
                        }
                    }
                }

                if (!manga.author.isNullOrBlank()) {
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!manga.mangaType.isNullOrBlank()) {
                    Text(
                        text = "• ${manga.mangaType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Row 3: Latest Chapter / Views / Updated Time
            val metaDetails = listOfNotNull(
                manga.latestChapter?.let { "📖 $it" },
                manga.views?.let { "👁️ $it" },
                manga.updatedAt?.let { "⏱️ $it" }
            ).joinToString("  •  ")

            if (metaDetails.isNotBlank()) {
                Text(
                    text = metaDetails,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 4: Tags / Genres preview (if any)
            if (manga.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    manga.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Deprecated("Use MangaItem with LayoutMode", ReplaceWith("MangaItem(manga, LayoutMode.COMFORTABLE, onClick)"))
@Composable
fun MangaGridItem(manga: Manga, onClick: () -> Unit) {
    ComfortableGridItem(manga, onClick)
}

private fun formatRating(rating: Float): String {
    val r = if (rating > 10f) rating / 10f else rating
    val rounded = kotlin.math.round(r * 10).toInt()
    return "${rounded / 10}.${kotlin.math.abs(rounded % 10)}"
}
