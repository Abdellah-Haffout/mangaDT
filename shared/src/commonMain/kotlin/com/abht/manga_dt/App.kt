package com.abht.manga_dt

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.abht.manga_dt.ui.MainScaffold
import com.abht.manga_dt.ui.screens.MangaDetailsScreen
import com.abht.manga_dt.ui.screens.ProfileStatsScreen
import com.abht.manga_dt.ui.screens.ReaderScreen
import com.abht.manga_dt.ui.screens.SourceDetailsScreen

private fun String.toRouteHex(): String {
    if (this.isEmpty()) return "empty"
    val hexChars = "0123456789abcdef"
    val bytes = this.encodeToByteArray()
    val result = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        val i = b.toInt() and 0xFF
        result.append(hexChars[i ushr 4])
        result.append(hexChars[i and 0x0F])
    }
    return result.toString()
}

private fun String?.fromRouteHex(): String {
    if (this == null || this == "empty") return ""
    return runCatching {
        val bytes = ByteArray(this.length / 2) { i ->
            this.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        bytes.decodeToString()
    }.getOrDefault(this)
}

@Composable
@Preview
fun App() {
    val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (com.abht.manga_dt.data.AppSettings.themeMode) {
        com.abht.manga_dt.data.ThemeMode.SYSTEM -> systemInDark
        com.abht.manga_dt.data.ThemeMode.DARK -> true
        com.abht.manga_dt.data.ThemeMode.LIGHT -> false
    }
    val preset = com.abht.manga_dt.data.AppSettings.themePreset
    val isAmoled = com.abht.manga_dt.data.AppSettings.amoledBlack && darkTheme

    val baseColorScheme = com.abht.manga_dt.ui.theme.rememberAnimatedPresetColorScheme(
        preset = preset,
        isDark = darkTheme,
        isAmoled = isAmoled
    )

    MaterialTheme(colorScheme = baseColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpanded = maxWidth > 600.dp
                val rootNavController = rememberNavController()

            NavHost(
                navController = rootNavController,
                startDestination = "main"
            ) {
                // Deep Analytics Screen
                composable("deep_analytics") {
                    com.abht.manga_dt.ui.screens.DeepAnalyticsScreen(
                        onBack = { rootNavController.popBackStack() },
                        onNavigateToMangaDetails = { sourceId, mangaUrl, title, cover ->
                            val mangaUrlHex = mangaUrl.ifBlank { title }.toRouteHex()
                            val titleHex = title.toRouteHex()
                            val coverHex = cover.toRouteHex()
                            rootNavController.navigate("manga_details/$sourceId/$mangaUrlHex/$titleHex/$coverHex")
                        }
                    )
                }

                // Profile & Detailed Statistics Screen
                composable("profile_stats") {
                    ProfileStatsScreen(
                        onBack = { rootNavController.popBackStack() },
                        onNavigateToMangaDetails = { sourceId, mangaUrl, title, cover ->
                            val mangaUrlHex = mangaUrl.ifBlank { title }.toRouteHex()
                            val titleHex = title.toRouteHex()
                            val coverHex = cover.toRouteHex()
                            rootNavController.navigate("manga_details/$sourceId/$mangaUrlHex/$titleHex/$coverHex")
                        },
                        onNavigateToReader = { entry ->
                            val chapterUrlHex = entry.chapterUrl.toRouteHex()
                            val mangaTitleHex = entry.mangaTitle.toRouteHex()
                            val chapterTitleHex = entry.chapterTitle.toRouteHex()
                            val mangaUrlHex = entry.mangaUrl.toRouteHex()
                            val page = entry.lastPage.coerceAtLeast(1)
                            val offset = entry.scrollOffset.coerceAtLeast(0)
                            rootNavController.navigate("reader/${entry.sourceId}/$chapterUrlHex/$mangaTitleHex/$chapterTitleHex/$page/$offset/$mangaUrlHex")
                        },
                        onNavigateToDeepAnalytics = { rootNavController.navigate("deep_analytics") }
                    )
                }

                // Settings Screen (Full-Screen Dedicated Destination)
                composable("settings") {
                    com.abht.manga_dt.ui.screens.SettingsScreen(
                        onBackClick = { rootNavController.popBackStack() },
                        onNavigateToProfileStats = { rootNavController.navigate("deep_analytics") },
                        onNavigateToSync = { rootNavController.navigate("sync") },
                        onNavigateToDownloads = { rootNavController.navigate("downloads") }
                    )
                }

                // Local Network Sync Screen
                composable("sync") {
                    com.abht.manga_dt.ui.screens.SyncScreen(
                        onBackClick = { rootNavController.popBackStack() }
                    )
                }

                // Downloads Screen (Dedicated Full-Screen Destination)
                composable("downloads") {
                    com.abht.manga_dt.ui.screens.DownloadManagerScreen(
                        onBack = { rootNavController.popBackStack() },
                        onNavigateToMangaDetails = { sourceId, mangaUrl, title, cover ->
                            val mangaUrlHex = mangaUrl.ifBlank { title }.toRouteHex()
                            val titleHex = title.toRouteHex()
                            val coverHex = cover.toRouteHex()
                            rootNavController.navigate("manga_details/$sourceId/$mangaUrlHex/$titleHex/$coverHex")
                        },
                        onNavigateToReader = { info ->
                            val chapterUrlHex = info.chapterUrl.toRouteHex()
                            val mangaTitleHex = info.mangaTitle.toRouteHex()
                            val chapterTitleHex = info.chapterTitle.toRouteHex()
                            val mangaUrlHex = info.mangaUrl.toRouteHex()
                            rootNavController.navigate("reader/${info.sourceId}/$chapterUrlHex/$mangaTitleHex/$chapterTitleHex/1/0/$mangaUrlHex")
                        }
                    )
                }

                // Main Screen containing the 5 Tabs & BottomNavigation/Sidebar
                composable("main") {
                    MainScaffold(
                        isExpanded = isExpanded,
                        onNavigateToSource = { source ->
                            rootNavController.navigate("source_details/${source.id}")
                        },
                        onNavigateToSettings = {
                            rootNavController.navigate("settings")
                        },
                        onNavigateToProfileStats = {
                            rootNavController.navigate("deep_analytics")
                        },
                        onNavigateToSync = {
                            rootNavController.navigate("sync")
                        },
                        onNavigateToDownloads = {
                            rootNavController.navigate("downloads")
                        },
                        onNavigateToMangaDetails = { sourceId, mangaUrl, title, cover ->
                            val mangaUrlHex = mangaUrl.ifBlank { title }.toRouteHex()
                            val titleHex = title.toRouteHex()
                            val coverHex = cover.toRouteHex()
                            rootNavController.navigate("manga_details/$sourceId/$mangaUrlHex/$titleHex/$coverHex")
                        },
                        onNavigateToReader = { mangaId, chapterId ->
                            rootNavController.navigate("reader/dummy/empty/${mangaId.toRouteHex()}/${chapterId.toRouteHex()}")
                        },
                        onNavigateToHistoryItem = { entry ->
                            val chapterUrlHex = entry.chapterUrl.toRouteHex()
                            val mangaTitleHex = entry.mangaTitle.toRouteHex()
                            val chapterTitleHex = entry.chapterTitle.toRouteHex()
                            val mangaUrlHex = entry.mangaUrl.toRouteHex()
                            val page = entry.lastPage.coerceAtLeast(1)
                            val offset = entry.scrollOffset.coerceAtLeast(0)
                            rootNavController.navigate("reader/${entry.sourceId}/$chapterUrlHex/$mangaTitleHex/$chapterTitleHex/$page/$offset/$mangaUrlHex")
                        }
                    )
                }

                // Dedicated Separate Full-Screen Destination: Source Manga List
                composable(
                    route = "source_details/{sourceId}",
                    arguments = listOf(navArgument("sourceId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val sourceId = backStackEntry.savedStateHandle.get<String>("sourceId") ?: ""
                    SourceDetailsScreen(
                        sourceId = sourceId,
                        onBackClick = { rootNavController.popBackStack() },
                        onMangaClick = { manga ->
                            val mangaUrlHex = manga.url.ifBlank { manga.id }.toRouteHex()
                            val titleHex = manga.title.toRouteHex()
                            val coverHex = manga.thumbnailUrl.toRouteHex()
                            rootNavController.navigate("manga_details/$sourceId/$mangaUrlHex/$titleHex/$coverHex")
                        }
                    )
                }

                // Dedicated Separate Full-Screen Destination: Manga Details & Chapter List
                composable(
                    route = "manga_details/{sourceId}/{mangaUrlHex}/{titleHex}/{coverHex}",
                    arguments = listOf(
                        navArgument("sourceId") { type = NavType.StringType },
                        navArgument("mangaUrlHex") { type = NavType.StringType },
                        navArgument("titleHex") { type = NavType.StringType },
                        navArgument("coverHex") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sourceId = backStackEntry.savedStateHandle.get<String>("sourceId") ?: ""
                    val mangaUrl = backStackEntry.savedStateHandle.get<String>("mangaUrlHex").fromRouteHex()
                    val title = backStackEntry.savedStateHandle.get<String>("titleHex").fromRouteHex()
                    val cover = backStackEntry.savedStateHandle.get<String>("coverHex").fromRouteHex()

                    MangaDetailsScreen(
                        sourceId = sourceId,
                        mangaUrl = mangaUrl,
                        initialTitle = title,
                        initialCover = cover,
                        onBackClick = { rootNavController.popBackStack() },
                        onChapterClick = { chapter, page, offset ->
                            com.abht.manga_dt.data.HistoryManager.addOrUpdateHistory(
                                com.abht.manga_dt.models.HistoryEntry(
                                    mangaTitle = title,
                                    mangaCover = cover,
                                    sourceId = sourceId,
                                    chapterTitle = chapter.title,
                                    chapterUrl = chapter.url,
                                    mangaUrl = mangaUrl,
                                    lastPage = page,
                                    scrollOffset = offset,
                                    timestamp = com.abht.manga_dt.data.currentTimeMillis()
                                )
                            )
                            val chapterUrlHex = chapter.url.toRouteHex()
                            val mangaTitleHex = title.toRouteHex()
                            val chapterTitleHex = chapter.title.toRouteHex()
                            val mangaUrlHex = mangaUrl.toRouteHex()
                            rootNavController.navigate("reader/$sourceId/$chapterUrlHex/$mangaTitleHex/$chapterTitleHex/$page/$offset/$mangaUrlHex")
                        },
                        onMangaClick = { srcId, mUrl, mTitle, mCover ->
                            val mUrlHex = mUrl.ifBlank { mTitle }.toRouteHex()
                            val tHex = mTitle.toRouteHex()
                            val cHex = mCover.toRouteHex()
                            rootNavController.navigate("manga_details/$srcId/$mUrlHex/$tHex/$cHex")
                        }
                    )
                }

                // Dedicated Separate Full-Screen Destination: Kotatsu-Style Reader
                composable(
                    route = "reader/{sourceId}/{chapterUrlHex}/{mangaTitleHex}/{chapterTitleHex}/{initialPage}/{scrollOffset}/{mangaUrlHex}",
                    arguments = listOf(
                        navArgument("sourceId") { type = NavType.StringType },
                        navArgument("chapterUrlHex") { type = NavType.StringType },
                        navArgument("mangaTitleHex") { type = NavType.StringType },
                        navArgument("chapterTitleHex") { type = NavType.StringType },
                        navArgument("initialPage") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("scrollOffset") { type = NavType.IntType; defaultValue = 0 },
                        navArgument("mangaUrlHex") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val sourceId = backStackEntry.savedStateHandle.get<String>("sourceId") ?: ""
                    val chapterUrl = backStackEntry.savedStateHandle.get<String>("chapterUrlHex").fromRouteHex()
                    val mangaTitle = backStackEntry.savedStateHandle.get<String>("mangaTitleHex").fromRouteHex().ifBlank { "Manga" }
                    val chapterTitle = backStackEntry.savedStateHandle.get<String>("chapterTitleHex").fromRouteHex().ifBlank { "Chapter" }
                    val initialPage = backStackEntry.savedStateHandle.get<Int>("initialPage") ?: 1
                    val scrollOffset = backStackEntry.savedStateHandle.get<Int>("scrollOffset") ?: 0
                    val mangaUrl = backStackEntry.savedStateHandle.get<String>("mangaUrlHex").fromRouteHex()

                    ReaderScreen(
                        title = mangaTitle,
                        chapterTitle = chapterTitle,
                        sourceId = sourceId,
                        chapterUrl = chapterUrl,
                        initialPage = initialPage,
                        initialScrollOffset = scrollOffset,
                        mangaUrl = mangaUrl,
                        onBack = { rootNavController.popBackStack() },
                        onChapterChange = { newChapter ->
                            val newChapterUrlHex = newChapter.url.toRouteHex()
                            val newTitleHex = mangaTitle.toRouteHex()
                            val newChapterTitleHex = newChapter.title.toRouteHex()
                            val newMangaUrlHex = mangaUrl.toRouteHex()
                            rootNavController.navigate("reader/$sourceId/$newChapterUrlHex/$newTitleHex/$newChapterTitleHex/1/0/$newMangaUrlHex") {
                                popUpTo("reader/{sourceId}/{chapterUrlHex}/{mangaTitleHex}/{chapterTitleHex}/{initialPage}/{scrollOffset}/{mangaUrlHex}") {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                // Fallback 4-arg reader route for backwards compatibility
                composable(
                    route = "reader/{sourceId}/{chapterUrlHex}/{mangaTitleHex}/{chapterTitleHex}",
                    arguments = listOf(
                        navArgument("sourceId") { type = NavType.StringType },
                        navArgument("chapterUrlHex") { type = NavType.StringType },
                        navArgument("mangaTitleHex") { type = NavType.StringType },
                        navArgument("chapterTitleHex") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val sourceId = backStackEntry.savedStateHandle.get<String>("sourceId") ?: ""
                    val chapterUrl = backStackEntry.savedStateHandle.get<String>("chapterUrlHex").fromRouteHex()
                    val mangaTitle = backStackEntry.savedStateHandle.get<String>("mangaTitleHex").fromRouteHex().ifBlank { "Manga" }
                    val chapterTitle = backStackEntry.savedStateHandle.get<String>("chapterTitleHex").fromRouteHex().ifBlank { "Chapter" }

                    ReaderScreen(
                        title = mangaTitle,
                        chapterTitle = chapterTitle,
                        sourceId = sourceId,
                        chapterUrl = chapterUrl,
                        initialPage = 1,
                        initialScrollOffset = 0,
                        onBack = { rootNavController.popBackStack() }
                    )
                }
            }
        }
    }
}
}

