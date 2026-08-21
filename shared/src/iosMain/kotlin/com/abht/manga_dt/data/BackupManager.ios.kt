package com.abht.manga_dt.data

actual class FileBackupEngine actual constructor() {
    actual fun getDefaultBackupDirectory(): String = "/Documents/MangaDT_Backups"
    actual fun saveBackupToFile(filename: String, content: String): Result<String> = Result.success("/Documents/MangaDT_Backups/$filename")
    actual fun readBackupFromFile(path: String): Result<String> = Result.failure(Exception("Not supported"))
    actual fun listLocalBackups(): List<BackupFileInfo> = emptyList()
    actual fun deleteBackupFile(path: String): Boolean = false
}
