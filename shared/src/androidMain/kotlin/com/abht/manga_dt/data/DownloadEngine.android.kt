package com.abht.manga_dt.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

actual class PlatformDownloadEngine actual constructor() {

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(AppCookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    actual fun getDownloadsDirectory(): String {
        val ctx = SettingsStorage.appContext
        val dir = if (ctx != null) {
            ctx.getExternalFilesDir("manga_downloads") ?: File(ctx.filesDir, "manga_downloads")
        } else {
            File(System.getProperty("java.io.tmpdir") ?: "/tmp", "manga_downloads")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir.absolutePath
    }

    actual suspend fun downloadPageToFile(
        url: String,
        targetFilePath: String,
        headers: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext false

        var attempts = 0
        val maxAttempts = 2

        while (attempts < maxAttempts) {
            attempts++
            try {
                val targetFile = File(targetFilePath)
                val parent = targetFile.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }

                // Temp file during download
                val tempFile = File("${targetFilePath}.tmp")
                if (tempFile.exists()) tempFile.delete()

                val requestBuilder = Request.Builder().url(url)
                requestBuilder.addHeader("User-Agent", APP_USER_AGENT)
                requestBuilder.addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
                
                headers.forEach { (k, v) ->
                    if (v.isNotBlank()) {
                        requestBuilder.header(k, v)
                    }
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    android.util.Log.e("DownloadEngine", "Failed to download page: HTTP $code for URL: $url")
                    response.close()
                    if (attempts < maxAttempts) continue
                    return@withContext false
                }

                val body = response.body
                if (body == null) {
                    response.close()
                    if (attempts < maxAttempts) continue
                    return@withContext false
                }

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                response.close()

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (targetFile.exists()) targetFile.delete()
                    val renamed = tempFile.renameTo(targetFile)
                    if (renamed || (targetFile.exists() && targetFile.length() > 0)) {
                        return@withContext true
                    }
                }
                tempFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("DownloadEngine", "Exception downloading page: ${e.message} for URL: $url", e)
                if (attempts >= maxAttempts) {
                    return@withContext false
                }
            }
        }
        false
    }

    actual fun deleteDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (dir.exists()) {
                dir.deleteRecursively()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    actual fun getDirectorySize(dirPath: String): Long {
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) 0L
            else dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            0L
        }
    }

    actual fun fileExists(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.length() > 0
    }

    actual fun getFileUri(filePath: String): String {
        val file = File(filePath)
        val path = file.absolutePath
        return if (path.startsWith("/")) "file://$path" else "file:///$path"
    }

    actual fun packageDirectoryToZip(dirPath: String): ByteArray? {
        return try {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) return null
            val files = dir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: return null
            if (files.isEmpty()) return null

            val baos = java.io.ByteArrayOutputStream()
            val zos = java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(baos))
            zos.setLevel(java.util.zip.Deflater.BEST_SPEED)
            val buffer = ByteArray(16384)
            for (f in files) {
                val entry = java.util.zip.ZipEntry(f.name)
                zos.putNextEntry(entry)
                val fis = java.io.BufferedInputStream(java.io.FileInputStream(f))
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    zos.write(buffer, 0, read)
                }
                fis.close()
                zos.closeEntry()
            }
            zos.close()
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    actual fun extractZipToDirectory(zipBytes: ByteArray, targetDirPath: String): List<String> {
        return try {
            val targetDir = File(targetDirPath)
            if (!targetDir.exists()) targetDir.mkdirs()
            val extractedFiles = mutableListOf<String>()
            val bais = java.io.ByteArrayInputStream(zipBytes)
            val zis = java.util.zip.ZipInputStream(java.io.BufferedInputStream(bais))
            val buffer = ByteArray(16384)
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val cleanName = entry.name.substringAfterLast("/").substringAfterLast("\\")
                    val outFile = File(targetDir, cleanName)
                    val fos = java.io.BufferedOutputStream(java.io.FileOutputStream(outFile))
                    var read: Int
                    while (zis.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                    fos.close()
                    extractedFiles.add(outFile.absolutePath)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            extractedFiles.sorted()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
