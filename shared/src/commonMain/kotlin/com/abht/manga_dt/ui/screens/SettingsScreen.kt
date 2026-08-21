package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abht.manga_dt.data.AppLanguage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.BackupFileInfo
import com.abht.manga_dt.data.BackupManager
import com.abht.manga_dt.data.BackupPreview
import com.abht.manga_dt.data.RestoreMode
import com.abht.manga_dt.data.Strings
import com.abht.manga_dt.data.ThemeMode
import com.abht.manga_dt.data.ThemePreset
import com.abht.manga_dt.ui.models.LayoutMode
import com.abht.manga_dt.ui.models.ReaderBackground
import com.abht.manga_dt.ui.models.ReaderScaleMode
import com.abht.manga_dt.ui.models.ReadingMode

enum class SettingsCategory(
    val titleEn: String,
    val titleAr: String,
    val descEn: String,
    val descAr: String,
    val icon: ImageVector
) {
    APPEARANCE(
        "Appearance", "المظهر والتصميم",
        "Theme, Color scheme, Language, List mode", "السمة، نظام الألوان، اللغة، نمط العرض",
        Icons.Default.Palette
    ),
    SOURCES(
        "Manga sources", "مصادر المانجا",
        "Pinned sources, 18+ NSFW content", "المصادر المثبتة، محتوى 18+ للبالغين",
        Icons.Default.CollectionsBookmark
    ),
    READER(
        "Reader settings", "إعدادات القارئ",
        "Read mode, Scale mode, Switch pages", "نمط القراءة، ملاءمة الشاشة، تقليب الصفحات",
        Icons.Default.AutoStories
    ),
    STORAGE(
        "Storage and network", "التخزين والشبكة",
        "Storage usage, Proxy, Image cache", "استهلاك الذاكرة، الذاكرة المؤقتة للصور",
        Icons.Default.Sync
    ),
    DOWNLOADS(
        "Downloads", "التنزيلات",
        "Downloads folder, Download only via Wi-Fi", "مجلد التنزيل، التنزيل عبر Wi-Fi فقط",
        Icons.Default.FileDownload
    ),
    UPDATES(
        "Check for new chapters", "التحديثات التلقائية",
        "Look for updates, Notifications settings", "البحث عن فصول جديدة، إعدادات التنبيهات",
        Icons.Default.RssFeed
    ),
    SERVICES(
        "Services & Tracking", "الخدمات والمزامنة",
        "Suggestions, Synchronization, Tracking", "المزامنة السحابية، التتبع والخدمات",
        Icons.Default.Extension
    ),
    BACKUP(
        "Backup and restore", "النسخ الاحتياطي والاستعادة",
        "Create or restore a backup, Periodic backups", "إنشاء أو استعادة نسخة احتياطية",
        Icons.Default.History
    ),
    ABOUT(
        "About", "حول التطبيق",
        "Version 1.0.0 (Kotatsu Multiplatform)", "الإصدار 1.0.0 (محرك كوتاتسو)",
        Icons.Default.Info
    );

    @Composable
    fun getTitle(): String = if (AppSettings.appLanguage == AppLanguage.ARABIC) titleAr else titleEn

    @Composable
    fun getDesc(): String = if (AppSettings.appLanguage == AppLanguage.ARABIC) descAr else descEn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToProfileStats: () -> Unit = {},
    onNavigateToSync: () -> Unit = {}
) {
    val strings = Strings.current
    var selectedCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }
    var mobileCurrentSubScreen by remember { mutableStateOf<SettingsCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWideScreen = maxWidth >= 720.dp

        if (isWideScreen) {
            // WIDE SCREEN DUAL-PANE LAYOUT
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Categories List (Width ~ 320dp)
                Surface(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        TopAppBar(
                            title = {
                                if (isSearching) {
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(strings.searchLocal, fontSize = 14.sp) },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(strings.settings, fontWeight = FontWeight.Bold)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearching = !isSearching; if (!isSearching) searchQuery = "" }) {
                                    Icon(if (isSearching) Icons.Default.Clear else Icons.Default.Search, contentDescription = "Search")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )

                        // Categories List
                        val filteredCategories = remember(searchQuery, AppSettings.appLanguage) {
                            if (searchQuery.isBlank()) SettingsCategory.values().toList()
                            else SettingsCategory.values().filter {
                                it.titleEn.contains(searchQuery, ignoreCase = true) ||
                                it.titleAr.contains(searchQuery, ignoreCase = true) ||
                                it.descEn.contains(searchQuery, ignoreCase = true) ||
                                it.descAr.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredCategories) { category ->
                                val isSelected = selectedCategory == category
                                Surface(
                                    onClick = {
                                        if (category == SettingsCategory.SERVICES) {
                                            onNavigateToSync()
                                        } else {
                                            selectedCategory = category
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            category.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = category.getTitle(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = category.getDesc(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Pane: Active Category Details
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = { fadeIn() togetherWith fadeOut() }
                    ) { category ->
                        CategoryDetailContent(category = category, onNavigateToSync = onNavigateToSync)
                    }
                }
            }
        } else {
            // MOBILE / PORTRAIT SINGLE-PANE LAYOUT
            if (mobileCurrentSubScreen == null) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(strings.settings, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearching = !isSearching }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        )
                    }
                ) { padding ->
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize()
                    ) {
                        items(SettingsCategory.values()) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (category == SettingsCategory.SERVICES) {
                                            onNavigateToSync()
                                        } else {
                                            mobileCurrentSubScreen = category
                                        }
                                    }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category.getTitle(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = category.getDesc(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            } else {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(mobileCurrentSubScreen!!.getTitle(), fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { mobileCurrentSubScreen = null }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                        CategoryDetailContent(category = mobileCurrentSubScreen!!, onNavigateToSync = onNavigateToSync)
                    }
                }
            }
        }
    }
}

/**
 * Category Detail Content Component with Modal Selection Dialogs
 */
@Composable
fun CategoryDetailContent(
    category: SettingsCategory,
    onNavigateToSync: () -> Unit = {}
) {
    val strings = Strings.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLayoutDialog by remember { mutableStateOf(false) }
    var showReadingModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (category) {
            SettingsCategory.APPEARANCE -> {
                Text(
                    text = strings.colorScheme,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Theme Presets Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemePreset.values().forEach { preset ->
                        ThemePresetPreviewCard(
                            preset = preset,
                            isSelected = AppSettings.themePreset == preset,
                            onClick = { AppSettings.updateThemePreset(preset) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Theme Mode (Opens Dialog on Click)
                val currentThemeModeLabel = when (AppSettings.themeMode) {
                    ThemeMode.SYSTEM -> strings.themeSystem
                    ThemeMode.DARK -> strings.themeDark
                    ThemeMode.LIGHT -> strings.themeLight
                }
                SettingsRowClickable(
                    title = strings.theme,
                    subtitle = currentThemeModeLabel,
                    icon = when (AppSettings.themeMode) {
                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        ThemeMode.DARK -> Icons.Default.DarkMode
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                    },
                    onClick = { showThemeDialog = true }
                )

                // AMOLED Pure Black
                SettingsRowSwitch(
                    title = strings.blackAmoled,
                    subtitle = strings.blackAmoledDesc,
                    icon = Icons.Default.Contrast,
                    checked = AppSettings.amoledBlack,
                    onCheckedChange = { AppSettings.setAmoled(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // App Language (Opens Dialog on Click)
                SettingsRowClickable(
                    title = strings.appLanguage,
                    subtitle = AppSettings.appLanguage.nativeName,
                    icon = Icons.Default.Language,
                    onClick = { showLanguageDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Browse Layout Mode (Opens Dialog on Click)
                val currentLayoutLabel = when (AppSettings.browseLayoutMode) {
                    LayoutMode.LIST -> strings.layoutList
                    LayoutMode.COMFORTABLE -> strings.layoutComfortable
                    LayoutMode.COMPACT -> strings.layoutCompact
                }

                SettingsRowClickable(
                    title = strings.defaultBrowseLayout,
                    subtitle = currentLayoutLabel,
                    icon = Icons.Default.GridView,
                    onClick = { showLayoutDialog = true }
                )
            }

            SettingsCategory.SOURCES -> {
                SettingsRowSwitch(
                    title = strings.nsfwContent,
                    subtitle = strings.nsfwContentDesc,
                    icon = Icons.Default.Warning,
                    checked = AppSettings.isNsfwAllowed,
                    onCheckedChange = { AppSettings.setNsfw(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Text(
                    text = "${strings.pinnedSourcesTitle} (${AppSettings.pinnedSourceIds.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (AppSettings.pinnedSourceIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            AppSettings.pinnedSourceIds.forEach { AppSettings.togglePinnedSource(it) }
                        }
                    ) {
                        Text(strings.clearAllPinned)
                    }
                } else {
                    Text(
                        text = strings.noPinnedSources,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            SettingsCategory.READER -> {
                val currentReadingModeLabel = when (AppSettings.readingMode) {
                    ReadingMode.WEBTOON -> strings.readingModeWebtoon
                    ReadingMode.VERTICAL_PAGED -> strings.readingModeVerticalPaged
                    ReadingMode.LTR -> strings.readingModeLTR
                    ReadingMode.RTL -> strings.readingModeRTL
                }

                val currentBgLabel = when (AppSettings.readerBackground) {
                    ReaderBackground.BLACK -> strings.readerBgBlack
                    ReaderBackground.DARK_GRAY -> strings.readerBgDarkGray
                    ReaderBackground.WHITE -> strings.readerBgWhite
                }

                val currentScaleLabel = when (AppSettings.readerScaleMode) {
                    ReaderScaleMode.FIT_WIDTH -> strings.readerScaleFitWidth
                    ReaderScaleMode.FIT_SCREEN -> strings.readerScaleFitScreen
                    ReaderScaleMode.FIT_HEIGHT -> strings.readerScaleFitHeight
                    ReaderScaleMode.ORIGINAL -> strings.readerScaleOriginal
                }

                SettingsRowClickable(
                    title = strings.defaultReadingMode,
                    subtitle = currentReadingModeLabel,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { showReadingModeDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 4.dp))

                SettingsRowSwitch(
                    title = strings.pageIndicator,
                    subtitle = strings.pageIndicatorDesc,
                    icon = Icons.Default.Filter1,
                    checked = AppSettings.showPageNumberPill,
                    onCheckedChange = { AppSettings.updateShowPageNumberPill(it) }
                )

                SettingsRowSwitch(
                    title = strings.keepScreenOn,
                    subtitle = strings.keepScreenOnDesc,
                    icon = Icons.Default.BrightnessMedium,
                    checked = AppSettings.readerKeepScreenOn,
                    onCheckedChange = { AppSettings.updateReaderKeepScreenOn(it) }
                )

                SettingsRowSwitch(
                    title = strings.cropBorders,
                    subtitle = strings.cropBordersDesc,
                    icon = Icons.Default.Crop,
                    checked = AppSettings.readerCropBorders,
                    onCheckedChange = { AppSettings.updateReaderCropBorders(it) }
                )
            }

            SettingsCategory.STORAGE -> {
                var cacheCleared by remember { mutableStateOf(false) }

                SettingsRowClickable(
                    title = strings.clearCache,
                    subtitle = if (cacheCleared) strings.clearCacheSuccess else strings.storageUsageDesc,
                    icon = Icons.Default.CleaningServices,
                    onClick = {
                        com.abht.manga_dt.data.MangaDataCache.cachedPopularManga.clear()
                        com.abht.manga_dt.data.MangaDataCache.cachedMangaDetails.clear()
                        com.abht.manga_dt.data.MangaDataCache.cachedPages.clear()
                        cacheCleared = true
                    }
                )
            }

            SettingsCategory.SERVICES -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.localSync, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(strings.localSyncDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToSync,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.localSync)
                        }
                    }
                }
            }

            SettingsCategory.BACKUP -> {
                BackupSettingsContent()
            }

            SettingsCategory.ABOUT -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(strings.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(strings.versionText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = strings.autoConfigured,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // --- SELECTION DIALOGS ---

    // 1. Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(strings.selectThemeTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> strings.themeSystem
                            ThemeMode.DARK -> strings.themeDark
                            ThemeMode.LIGHT -> strings.themeLight
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                AppSettings.updateThemeMode(mode)
                                showThemeDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = AppSettings.themeMode == mode,
                                onClick = {
                                    AppSettings.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text(strings.cancel) } }
        )
    }

    // 2. Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.selectLanguageTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                AppSettings.setLanguage(lang)
                                showLanguageDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = AppSettings.appLanguage == lang, onClick = { AppSettings.setLanguage(lang); showLanguageDialog = false })
                            Spacer(Modifier.width(10.dp))
                            Text(lang.nativeName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.cancel) } }
        )
    }

    // 3. Layout Selection Dialog
    if (showLayoutDialog) {
        AlertDialog(
            onDismissRequest = { showLayoutDialog = false },
            title = { Text(strings.selectLayoutTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        LayoutMode.LIST to strings.layoutList,
                        LayoutMode.COMFORTABLE to strings.layoutComfortable,
                        LayoutMode.COMPACT to strings.layoutCompact
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                AppSettings.setBrowseLayout(mode)
                                showLayoutDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = AppSettings.browseLayoutMode == mode, onClick = { AppSettings.setBrowseLayout(mode); showLayoutDialog = false })
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLayoutDialog = false }) { Text(strings.cancel) } }
        )
    }

    // 4. Reading Mode Selection Dialog
    if (showReadingModeDialog) {
        AlertDialog(
            onDismissRequest = { showReadingModeDialog = false },
            title = { Text(strings.selectReadingModeTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(
                        ReadingMode.WEBTOON to strings.readingModeWebtoon,
                        ReadingMode.VERTICAL_PAGED to strings.readingModeVerticalPaged,
                        ReadingMode.LTR to strings.readingModeLTR,
                        ReadingMode.RTL to strings.readingModeRTL
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                AppSettings.setReaderMode(mode)
                                showReadingModeDialog = false
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = AppSettings.readingMode == mode, onClick = { AppSettings.setReaderMode(mode); showReadingModeDialog = false })
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showReadingModeDialog = false }) { Text(strings.cancel) } }
        )
    }
}

/**
 * Standard Settings Clickable Row (Title + Value Subtitle)
 */
@Composable
fun SettingsRowClickable(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Standard Settings Switch Row
 */
@Composable
fun SettingsRowSwitch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Theme Preset Card (Screenshot 2 Design Replica)
 */
@Composable
fun ThemePresetPreviewCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = Color(preset.primaryColor)
    val accentDotColor = Color(preset.accentDotColor)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isSelected) BorderStroke(2.dp, primaryColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier
                .width(88.dp)
                .height(116.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top: "Abc" + Checkmark
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Abc",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Surface(
                                shape = CircleShape,
                                color = primaryColor,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Middle: Mini UI lines
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = primaryColor.copy(alpha = 0.65f),
                        modifier = Modifier.width(36.dp).height(5.dp)
                    ) {}
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.width(48.dp).height(5.dp)
                    ) {}

                    Spacer(Modifier.weight(1f))

                    // Bottom End: Accent Dot Circle
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = CircleShape,
                            color = accentDotColor,
                            modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                        ) {}
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = preset.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BackupSettingsContent() {
    val appStrings = Strings.current
    var backupList by remember { mutableStateOf(BackupManager.listBackups()) }
    var lastCreatedPath by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    var showPasteJsonDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }

    var pendingPreviewAndJson by remember { mutableStateOf<Pair<BackupPreview, String>?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var deleteCandidatePath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. CREATE BACKUP CARD
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appStrings.createBackup, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(appStrings.createBackupDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val result = BackupManager.createBackup()
                        if (result.isSuccess) {
                            lastCreatedPath = result.getOrNull()
                            statusMessage = appStrings.backupCreatedSuccess
                            isSuccessStatus = true
                            backupList = BackupManager.listBackups()
                        } else {
                            statusMessage = "Error: ${result.exceptionOrNull()?.message}"
                            isSuccessStatus = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(appStrings.createBackup)
                }

                if (lastCreatedPath != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(appStrings.backupSavedAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(lastCreatedPath ?: "", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // 2. RESTORE FROM DEVICE STORAGE (FILE PICKER)
        val filePickerLauncher = com.abht.manga_dt.ui.components.rememberFilePicker { content, _ ->
            val preview = BackupManager.inspectBackup(content)
            if (preview != null) {
                pendingPreviewAndJson = Pair(preview, content)
            } else {
                statusMessage = appStrings.invalidBackupFormat
                isSuccessStatus = false
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(appStrings.selectFileFromDevice, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(appStrings.selectFileFromDeviceDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = filePickerLauncher,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(appStrings.restoreBackup, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 3. RESTORE VIA PASTE JSON
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(appStrings.pasteJsonBackup, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(appStrings.restoreBackupDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }

                FilledTonalButton(
                    onClick = { showPasteJsonDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(appStrings.restoreBackup, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // 4. STATUS MESSAGE BANNER
        if (statusMessage != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccessStatus) Color(0xFF00C853).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isSuccessStatus) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSuccessStatus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // 4. LOCALLY SAVED BACKUP FILES LIST
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appStrings.localBackupsList,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { backupList = BackupManager.listBackups() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (backupList.isEmpty()) {
                    Text(
                        text = appStrings.noLocalBackupsFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        backupList.forEach { fileInfo ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                                        Column {
                                            Text(fileInfo.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text("${fileInfo.sizeBytes / 1024} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                val contentRes = BackupManager.readBackup(fileInfo.path)
                                                if (contentRes.isSuccess) {
                                                    val raw = contentRes.getOrNull() ?: ""
                                                    val preview = BackupManager.inspectBackup(raw)
                                                    if (preview != null) {
                                                        pendingPreviewAndJson = Pair(preview, raw)
                                                    } else {
                                                        statusMessage = appStrings.invalidBackupFormat
                                                        isSuccessStatus = false
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(appStrings.restoreBackup, fontSize = 11.sp)
                                        }

                                        IconButton(
                                            onClick = { deleteCandidatePath = fileInfo.path },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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

    // --- PASTE JSON DIALOG ---
    if (showPasteJsonDialog) {
        AlertDialog(
            onDismissRequest = { showPasteJsonDialog = false },
            title = { Text(appStrings.pasteJsonBackup, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pastedJsonText,
                    onValueChange = { pastedJsonText = it },
                    placeholder = { Text(appStrings.pasteJsonPlaceholder, fontSize = 12.sp) },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val preview = BackupManager.inspectBackup(pastedJsonText)
                        if (preview != null) {
                            pendingPreviewAndJson = Pair(preview, pastedJsonText)
                            showPasteJsonDialog = false
                        } else {
                            statusMessage = appStrings.invalidBackupFormat
                            isSuccessStatus = false
                        }
                    },
                    enabled = pastedJsonText.isNotBlank()
                ) {
                    Text(appStrings.ok)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteJsonDialog = false }) {
                    Text(appStrings.cancel)
                }
            }
        )
    }

    // --- BACKUP PREVIEW & RESTORE MODE SELECTION MODAL ---
    if (pendingPreviewAndJson != null) {
        val (preview, rawJson) = pendingPreviewAndJson!!

        AlertDialog(
            onDismissRequest = { pendingPreviewAndJson = null },
            title = { Text(appStrings.backupPreviewTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Preview Stats Grid
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("📱 ${preview.deviceName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                            Text("📚 ${appStrings.backupLibraryItems}: ${preview.libraryCount}", style = MaterialTheme.typography.bodySmall)
                            Text("📖 ${appStrings.backupHistoryEntries}: ${preview.historyCount}", style = MaterialTheme.typography.bodySmall)
                            Text("📊 ${appStrings.backupStatsEntries}: ${preview.statsCount}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(appStrings.restoreModeTitle, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Mode 1: Merge
                    Surface(
                        onClick = { selectedRestoreMode = RestoreMode.MERGE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedRestoreMode == RestoreMode.MERGE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (selectedRestoreMode == RestoreMode.MERGE) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedRestoreMode == RestoreMode.MERGE, onClick = { selectedRestoreMode = RestoreMode.MERGE })
                                Spacer(Modifier.width(6.dp))
                                Text(appStrings.restoreModeMerge, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(appStrings.restoreModeMergeDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 32.dp))
                        }
                    }

                    // Mode 2: Full Overwrite
                    Surface(
                        onClick = { selectedRestoreMode = RestoreMode.OVERWRITE },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedRestoreMode == RestoreMode.OVERWRITE) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, if (selectedRestoreMode == RestoreMode.OVERWRITE) MaterialTheme.colorScheme.error else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedRestoreMode == RestoreMode.OVERWRITE, onClick = { selectedRestoreMode = RestoreMode.OVERWRITE })
                                Spacer(Modifier.width(6.dp))
                                Text(appStrings.restoreModeOverwrite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(appStrings.restoreModeOverwriteDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 32.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = BackupManager.restoreBackup(rawJson, selectedRestoreMode)
                        statusMessage = res.message
                        isSuccessStatus = res.success
                        pendingPreviewAndJson = null
                    }
                ) {
                    Text(appStrings.restoreBackup)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPreviewAndJson = null }) {
                    Text(appStrings.cancel)
                }
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (deleteCandidatePath != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidatePath = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(appStrings.clear, fontWeight = FontWeight.Bold) },
            text = { Text(appStrings.deleteBackupConfirm) },
            confirmButton = {
                Button(
                    onClick = {
                        BackupManager.deleteBackup(deleteCandidatePath!!)
                        deleteCandidatePath = null
                        backupList = BackupManager.listBackups()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(appStrings.clear)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidatePath = null }) {
                    Text(appStrings.cancel)
                }
            }
        )
    }
}
