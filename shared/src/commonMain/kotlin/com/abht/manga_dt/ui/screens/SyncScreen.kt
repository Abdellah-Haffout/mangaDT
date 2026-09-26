package com.abht.manga_dt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abht.manga_dt.data.AppLanguage
import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.DownloadManager
import com.abht.manga_dt.data.LocalSyncManager
import com.abht.manga_dt.data.Strings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onBackClick: () -> Unit
) {
    val strings = Strings.current
    val isArabic = AppSettings.appLanguage == AppLanguage.ARABIC
    val coroutineScope = rememberCoroutineScope()

    var manualIp by remember { mutableStateOf("") }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var syncResultStatus by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(true) }

    val isServerEnabled = LocalSyncManager.isServerEnabled
    val localIp = LocalSyncManager.localIpAddress
    val deviceName = LocalSyncManager.deviceName
    val serverPort = LocalSyncManager.serverPort
    val discoveredPeers = LocalSyncManager.discoveredPeers
    val isScanning = LocalSyncManager.isScanning
    val isSyncing = LocalSyncManager.isSyncing
    val syncOptions = LocalSyncManager.syncOptions

    // Auto-start server and scan on initial open if not running
    LaunchedEffect(Unit) {
        if (!isServerEnabled) {
            LocalSyncManager.toggleServer(true)
        }
        LocalSyncManager.scanForPeers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.localSync, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            LocalSyncManager.refreshLocalIp()
                            LocalSyncManager.scanForPeers()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. CURRENT DEVICE STATUS CARD
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isServerEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (deviceName.contains("PC", ignoreCase = true) || deviceName.contains("Desktop", ignoreCase = true)) Icons.Default.Laptop
                                            else Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = if (isServerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = deviceName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        IconButton(
                                            onClick = { showEditNameDialog = true },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit name", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Text(
                                        text = "$localIp:$serverPort",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Switch(
                                checked = isServerEnabled,
                                onCheckedChange = { LocalSyncManager.toggleServer(it) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerEnabled) Color(0xFF00C853) else Color(0xFFD50000))
                            )
                            Text(
                                text = if (isServerEnabled) strings.serverRunning else strings.serverStopped,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isServerEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. DISCOVERED DEVICES SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.discoveredDevices,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledTonalButton(
                        onClick = {
                            coroutineScope.launch {
                                LocalSyncManager.scanForPeers()
                            }
                        },
                        enabled = !isScanning,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(strings.scanning, style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.scanDevices, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (discoveredPeers.isEmpty() && !isScanning) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.WifiTetheringOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = strings.noDevicesFound,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(discoveredPeers, key = { "${it.ip}:${it.port}" }) { peer ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
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
                                        Icon(
                                            if (peer.isAndroid) Icons.Default.PhoneAndroid else Icons.Default.Laptop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = peer.deviceName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${peer.ip}:${peer.port}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = LocalSyncManager.syncWithTarget(peer.ip, peer.port)
                                        syncResultStatus = res.message
                                        isSuccessStatus = res.success
                                    }
                                },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.syncNow, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // 3. MANUAL IP CONNECTION CARD
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.manualConnect,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                placeholder = { Text(strings.enterTargetIp, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    if (manualIp.isNotBlank()) {
                                        val ipClean = manualIp.trim().substringBefore(":")
                                        val portClean = manualIp.trim().substringAfter(":", "").toIntOrNull() ?: serverPort
                                        coroutineScope.launch {
                                            val res = LocalSyncManager.syncWithTarget(ipClean, portClean)
                                            syncResultStatus = res.message
                                            isSuccessStatus = res.success
                                        }
                                    }
                                },
                                enabled = manualIp.isNotBlank() && !isSyncing,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(strings.connectAndSync, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 4. SYNC OPTIONS / WHAT TO SYNC
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.syncOptions,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        SyncOptionRow(
                            title = strings.syncLibrary,
                            checked = syncOptions.syncLibrary,
                            onCheckedChange = { LocalSyncManager.syncOptions = syncOptions.copy(syncLibrary = it) },
                            icon = Icons.Default.CollectionsBookmark
                        )
                        SyncOptionRow(
                            title = strings.syncHistory,
                            checked = syncOptions.syncHistory,
                            onCheckedChange = { LocalSyncManager.syncOptions = syncOptions.copy(syncHistory = it) },
                            icon = Icons.AutoMirrored.Filled.MenuBook
                        )
                        SyncOptionRow(
                            title = strings.syncStats,
                            checked = syncOptions.syncStats,
                            onCheckedChange = { LocalSyncManager.syncOptions = syncOptions.copy(syncStats = it) },
                            icon = Icons.Default.Insights
                        )
                        SyncOptionRow(
                            title = strings.syncSettings,
                            checked = syncOptions.syncSettings,
                            onCheckedChange = { LocalSyncManager.syncOptions = syncOptions.copy(syncSettings = it) },
                            icon = Icons.Default.Tune
                        )

                        val downloadedCount = DownloadManager.downloadedChapters.size
                        val totalDownloadedBytes = DownloadManager.getTotalStorageUsed()
                        val downloadsBadge = if (downloadedCount > 0) {
                            "$downloadedCount (${DownloadManager.formatBytes(totalDownloadedBytes)})"
                        } else null

                        SyncOptionRow(
                            title = strings.syncDownloads,
                            subtitle = strings.syncDownloadsDesc,
                            badge = downloadsBadge,
                            checked = syncOptions.syncDownloads,
                            onCheckedChange = { LocalSyncManager.syncOptions = syncOptions.copy(syncDownloads = it) },
                            icon = Icons.Default.FolderZip
                        )
                    }
                }
            }

            // 5. LIVE SYNC FEEDBACK BANNER
            val currentStatus = LocalSyncManager.lastSyncStatus
            if (isSyncing || syncResultStatus != null || !currentStatus.isNullOrBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSyncing) MaterialTheme.colorScheme.primaryContainer
                            else if (isSuccessStatus) Color(0xFF00C853).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = currentStatus?.takeIf { it.startsWith("جاري") || it.startsWith("Transferring") } ?: strings.syncing,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Icon(
                                    if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isSuccessStatus) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = syncResultStatus ?: currentStatus ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSuccessStatus) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // 6. LOCAL PRIVACY BANNER
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = strings.localSyncDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    // Edit Device Name Dialog
    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(deviceName) }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(strings.deviceName, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        LocalSyncManager.updateDeviceName(tempName)
                        showEditNameDialog = false
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun SyncOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String? = null,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (badge != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}
