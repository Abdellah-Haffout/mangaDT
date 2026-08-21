package com.abht.manga_dt.data

import android.os.Environment
import java.io.File

actual class FileBackupEngine actual constructor() {

    actual fun getDefaultBackupDirectory(): String {
        val ctx = SettingsStorage.appContext
        
        // 1. Primary: App External Files directory (Requires 0 permissions on all Android versions)
        val externalDir = ctx?.getExternalFilesDir("backups")
        if (externalDir != null) {
            if (!externalDir.exists()) externalDir.mkdirs()
            return externalDir.absolutePath
        }

        // 2. Secondary: App Internal Files directory
        val internalDir = ctx?.let { File(it.filesDir, "backups") }
        if (internalDir != null) {
            if (!internalDir.exists()) internalDir.mkdirs()
            return internalDir.absolutePath
        }

        // 3. Fallback: Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDir = if (downloads != null && downloads.exists()) {
            File(downloads, "MangaDT_Backups")
        } else {
            File("/sdcard/Download/MangaDT_Backups")
        }
        if (!backupDir.exists()) {
            runCatching { backupDir.mkdirs() }
        }
        return backupDir.absolutePath
    }

    actual fun saveBackupToFile(filename: String, content: String): Result<String> {
        return try {
            val primaryDir = File(getDefaultBackupDirectory())
            if (!primaryDir.exists()) primaryDir.mkdirs()
            val primaryFile = File(primaryDir, filename)
            primaryFile.writeText(content, Charsets.UTF_8)

            // Try to write a copy to public Downloads if accessible
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloads != null && downloads.exists()) {
                    val publicDir = File(downloads, "MangaDT_Backups")
                    if (!publicDir.exists()) publicDir.mkdirs()
                    val publicFile = File(publicDir, filename)
                    publicFile.writeText(content, Charsets.UTF_8)
                }
            } catch (_: Exception) {
                // Ignore if scoped storage restricts direct public Downloads write
            }

            Result.success(primaryFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun readBackupFromFile(path: String): Result<String> {
        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return Result.failure(Exception("File does not exist: $path"))
            }
            val text = file.readText(Charsets.UTF_8)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun listLocalBackups(): List<BackupFileInfo> {
        val allFiles = mutableListOf<File>()
        val seenPaths = mutableSetOf<String>()

        // 1. App external directory
        val ctx = SettingsStorage.appContext
        val externalDir = ctx?.getExternalFilesDir("backups")
        if (externalDir != null && externalDir.exists()) {
            externalDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.contains("mangadt_backup")) }?.forEach {
                if (seenPaths.add(it.name)) allFiles.add(it)
            }
        }

        // 2. App internal directory
        val internalDir = ctx?.let { File(it.filesDir, "backups") }
        if (internalDir != null && internalDir.exists()) {
            internalDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.contains("mangadt_backup")) }?.forEach {
                if (seenPaths.add(it.name)) allFiles.add(it)
            }
        }

        // 3. Public Downloads directory
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads != null && downloads.exists()) {
                val publicDir = File(downloads, "MangaDT_Backups")
                if (publicDir.exists()) {
                    publicDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".json") || it.name.contains("mangadt_backup")) }?.forEach {
                        if (seenPaths.add(it.name)) allFiles.add(it)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return allFiles.sortedByDescending { it.lastModified() }.map {
            BackupFileInfo(
                name = it.name,
                path = it.absolutePath,
                sizeBytes = it.length(),
                lastModified = it.lastModified()
            )
        }
    }

    actual fun deleteBackupFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }
}
