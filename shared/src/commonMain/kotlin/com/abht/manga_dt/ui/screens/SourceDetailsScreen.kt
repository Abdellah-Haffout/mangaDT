package com.abht.manga_dt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.ui.components.CaptchaWebViewDialog
import com.abht.manga_dt.ui.components.MangaItem
import com.abht.manga_dt.ui.models.LayoutMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceDetailsScreen(
    sourceId: String,
    onBackClick: () -> Unit,
    onMangaClick: (Manga) -> Unit
) {
    val strings = Strings.current
    val sourceManager = remember { MangaSourceManager() }
    val scope = rememberCoroutineScope()
    val cached = com.abht.manga_dt.data.MangaDataCache.cachedPopularManga[sourceId] ?: emptyList()
    var mangaList by remember(sourceId) { mutableStateOf(cached) }
    var isLoading by remember(sourceId) { mutableStateOf(cached.isEmpty()) }
    var page by remember(sourceId) { mutableStateOf(1) }
    var canLoadMore by remember(sourceId) { mutableStateOf(true) }
    var isSearching by remember(sourceId) { mutableStateOf(false) }
    var searchQuery by remember(sourceId) { mutableStateOf("") }
    var sourceName by remember(sourceId) { mutableStateOf(sourceId) }
    var errorMessage by remember(sourceId) { mutableStateOf<String?>(null) }
    var layoutMode by remember { mutableStateOf(AppSettings.browseLayoutMode) }
    var showCaptchaDialog by remember { mutableStateOf(false) }

    val sourceUrl = remember(sourceId) {
        sourceManager.getSourceBaseUrl(sourceId)
    }

    fun isCaptchaOrCloudflare(error: String?): Boolean {
        if (error == null) return false
        val lower = error.lowercase()
        return lower.contains("403") || 
               lower.contains("503") || 
               lower.contains("cloudflare") || 
               lower.contains("turnstile") || 
               lower.contains("captcha") || 
               lower.contains("challenge") || 
               lower.contains("ddos") || 
               lower.contains("just a moment") || 
               lower.contains("cf-chl") || 
               lower.contains("protection") || 
               lower.contains("access denied") || 
               lower.contains("forbidden")
    }

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMessage = null
            canLoadMore = true
            try {
                sourceName = sourceManager.getAvailableSources().find { it.id.equals(sourceId, ignoreCase = true) }?.name ?: sourceId
                val results = sourceManager.getPopularManga(sourceId, 1)
                mangaList = results
                page = 1
                if (results.isEmpty()) {
                    canLoadMore = false
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sourceId) {
        sourceName = sourceManager.getAvailableSources().find { it.id.equals(sourceId, ignoreCase = true) }?.name ?: sourceId
        if (mangaList.isEmpty()) {
            refresh()
        }
    }

    fun loadMore() {
        if (isLoading || isSearching || !canLoadMore) return
        scope.launch {
            isLoading = true
            try {
                val nextBatch = sourceManager.getPopularManga(sourceId, page + 1)
                if (nextBatch.isNotEmpty()) {
                    mangaList = mangaList + nextBatch
                    page++
                } else {
                    canLoadMore = false
                }
            } catch (e: Exception) {
                // Ignore pagination fail
            } finally {
                isLoading = false
            }
        }
    }

    fun performSearch(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            isSearching = false
            refresh()
            return
        }
        isSearching = true
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val results = sourceManager.searchManga(sourceId, query)
                mangaList = results
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sourceName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                    }
                },
                actions = {
                    var showSearchField by remember { mutableStateOf(false) }
                    if (showSearchField) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { performSearch(it) },
                            placeholder = { Text(strings.searchManga) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            trailingIcon = {
                                IconButton(onClick = { showSearchField = false; performSearch("") }) {
                                    Icon(Icons.Default.Search, contentDescription = "Close")
                                }
                            }
                        )
                    } else {
                        // Quick Webview / Captcha button
                        IconButton(onClick = { showCaptchaDialog = true }) {
                            Icon(Icons.Default.Security, contentDescription = strings.solveCaptcha, tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            layoutMode = when (layoutMode) {
                                LayoutMode.COMFORTABLE -> LayoutMode.COMPACT
                                LayoutMode.COMPACT -> LayoutMode.LIST
                                LayoutMode.LIST -> LayoutMode.COMFORTABLE
                            }
                        }) {
                            val icon = when (layoutMode) {
                                LayoutMode.COMFORTABLE -> Icons.Default.GridView
                                LayoutMode.COMPACT -> Icons.Default.ViewCompact
                                LayoutMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                            }
                            Icon(icon, contentDescription = "Toggle Layout")
                        }
                        IconButton(onClick = { showSearchField = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (mangaList.isEmpty() && isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (isCaptchaOrCloudflare(errorMessage)) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                                modifier = Modifier.size(40.dp)
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

                    OutlinedButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.retry)
                    }
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                    Button(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.retry)
                    }
                }
            } else if (mangaList.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.noMangaFound,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(strings.retry)
                    }
                }
            } else {
                val gridState = rememberLazyGridState()
                
                LazyVerticalGrid(
                    columns = when (layoutMode) {
                        LayoutMode.COMFORTABLE -> GridCells.Adaptive(120.dp)
                        LayoutMode.COMPACT -> GridCells.Adaptive(100.dp)
                        LayoutMode.LIST -> GridCells.Fixed(1)
                    },
                    state = gridState,
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(mangaList) { manga ->
                        MangaItem(manga = manga, layoutMode = layoutMode, onClick = { onMangaClick(manga) })
                    }
                    
                    if (!isSearching && canLoadMore && mangaList.isNotEmpty()) {
                        item {
                            LaunchedEffect(mangaList.size) {
                                loadMore()
                            }
                        }
                    }
                }
            }
        }
    }

    // Captcha / Cloudflare WebView Modal
    if (showCaptchaDialog) {
        CaptchaWebViewDialog(
            url = sourceUrl,
            title = sourceName,
            onDismiss = { showCaptchaDialog = false },
            onSolved = {
                showCaptchaDialog = false
                refresh()
            }
        )
    }
}
