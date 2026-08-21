package com.abht.manga_dt.data

import java.io.File

actual class FileBackupEngine actual constructor() {

    actual fun getDefaultBackupDirectory(): String {
        val userHome = System.getProperty("user.home") ?: "."
        val downloads = File(userHome, "Downloads")
        val backupDir = if (downloads.exists() && downloads.isDirectory) {
            File(downloads, "MangaDT_Backups")
        } else {
            File(userHome, "MangaDT_Backups")
        }
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir.absolutePath
    }

    actual fun saveBackupToFile(filename: String, content: String): Result<String> {
        return try {
            val dir = File(getDefaultBackupDirectory())
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            file.writeText(content, Charsets.UTF_8)
            Result.success(file.absolutePath)
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
        return try {
            val dir = File(getDefaultBackupDirectory())
            if (!dir.exists()) return emptyList()
            val files = dir.listFiles { f -> f.isFile && (f.name.endsWith(".json") || f.name.contains("mangadt_backup")) } ?: emptyArray()
            files.sortedByDescending { it.lastModified() }.map {
                BackupFileInfo(
                    name = it.name,
                    path = it.absolutePath,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified()
                )
            }
        } catch (_: Exception) {
            emptyList()
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
