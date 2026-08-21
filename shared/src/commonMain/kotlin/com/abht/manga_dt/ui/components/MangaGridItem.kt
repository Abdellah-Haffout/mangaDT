package com.abht.manga_dt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abht.manga_dt.models.Manga
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
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = manga.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompactGridItem(manga: Manga, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = manga.thumbnailUrl,
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small)
        )
        Surface(
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth(),
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
        ) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.labelSmall,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ListMangaItem(manga: Manga, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .size(60.dp, 80.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            AsyncImage(
                model = manga.thumbnailUrl,
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            manga.author?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Deprecated("Use MangaItem with LayoutMode", ReplaceWith("MangaItem(manga, LayoutMode.COMFORTABLE, onClick)"))
@Composable
fun MangaGridItem(manga: Manga, onClick: () -> Unit) {
    ComfortableGridItem(manga, onClick)
}
