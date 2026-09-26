package com.abht.manga_dt.data

expect class PlatformDownloadEngine() {
    fun getDownloadsDirectory(): String
    suspend fun downloadPageToFile(url: String, targetFilePath: String, headers: Map<String, String> = emptyMap()): Boolean
    fun deleteDirectory(dirPath: String): Boolean
    fun getDirectorySize(dirPath: String): Long
    fun fileExists(filePath: String): Boolean
    fun getFileUri(filePath: String): String
    fun packageDirectoryToZip(dirPath: String): ByteArray?
    fun extractZipToDirectory(zipBytes: ByteArray, targetDirPath: String): List<String>
}
