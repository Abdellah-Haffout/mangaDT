package com.abht.manga_dt.data

actual class PlatformDownloadEngine actual constructor() {
    actual fun getDownloadsDirectory(): String = "manga_downloads"
    actual suspend fun downloadPageToFile(url: String, targetFilePath: String, headers: Map<String, String>): Boolean = false
    actual fun deleteDirectory(dirPath: String): Boolean = true
    actual fun getDirectorySize(dirPath: String): Long = 0L
    actual fun fileExists(filePath: String): Boolean = false
    actual fun getFileUri(filePath: String): String = filePath
    actual fun packageDirectoryToZip(dirPath: String): ByteArray? = null
    actual fun extractZipToDirectory(zipBytes: ByteArray, targetDirPath: String): List<String> = emptyList()
}
