package com.abht.manga_dt

import com.abht.manga_dt.data.MangaSourceManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicDesktopTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun testGetAvailableSources() = runBlocking {
        val manager = MangaSourceManager()
        val sources = manager.getAvailableSources()
        println("Sources count: ${sources.size}")
        sources.take(10).forEach { println("Source: ${it.name} (${it.id}) -> ${it.iconUrl}") }
        assertTrue(sources.isNotEmpty())
    }

    @Test
    fun testGetPopularManga() = runBlocking {
        val manager = MangaSourceManager()
        val sources = manager.getAvailableSources()
        println("Testing first available source: ${sources.first().id}")
        try {
            val mangaList = manager.getPopularManga(sources.first().id, 1)
            println("Manga count: ${mangaList.size}")
        } catch (e: Throwable) {
            println("Caught error for first source: $e")
            e.printStackTrace()
        }
    }

    @Test
    fun testInspectMangaParserSource() {
        org.koitharu.kotatsu.parsers.model.MangaParserSource.entries.take(15).forEach {
            println("Source: name=${it.name}, title=${it.title}, locale=${it.locale}, contentType=${it.contentType}, isBroken=${it.isBroken}")
        }
    }

    @Test
    fun testInspectMangaParserMethods() {
        val methods = org.koitharu.kotatsu.parsers.MangaParser::class.java.methods
        methods.forEach { println("Parser method: ${it.name}(${it.parameterTypes.map { p -> p.simpleName }.joinToString(", ")}) -> ${it.returnType.simpleName}") }
    }

    @Test
    fun testFetchRealMangaAndPages() = runBlocking {
        val manager = MangaSourceManager()
        val sources = manager.getAvailableSources()
        val englishSource = sources.firstOrNull { it.id == "MANGADEX" || it.id == "MANGAKAKALOT" || it.name.contains("Dex", ignoreCase = true) || it.locale == "en" } ?: sources.first()
        println("Testing real manga fetch with source: ${englishSource.name} (${englishSource.id})")
        val mangaList = manager.getPopularManga(englishSource.id, 1)
        println("Fetched ${mangaList.size} manga")
        if (mangaList.isNotEmpty()) {
            val firstManga = mangaList.first()
            println("First Manga: id=${firstManga.id}, title=${firstManga.title}, cover=${firstManga.thumbnailUrl}")
        }
    }

    @Test
    fun testRealPageImageDownload() = runBlocking {
        val manager = MangaSourceManager()
        val sources = manager.getAvailableSources()
        val source = sources.first { !it.isBroken && it.locale == "en" }
        println("Using source: ${source.name} (${source.id})")
        val popular = manager.getPopularManga(source.id, 1)
        println("Found ${popular.size} manga")
        val manga = popular.first()
        println("Testing manga: ${manga.title} url=${manga.url}")
        val details = manager.getMangaDetails(source.id, manga.url)
        println("Details chapters: ${details?.chapters?.size}")
        val firstChapter = details?.chapters?.firstOrNull()
        if (firstChapter != null) {
            println("Testing chapter: ${firstChapter.title} url=${firstChapter.url}")
            val pages = manager.getPages(source.id, firstChapter.url)
            println("Pages count: ${pages.size}")
            if (pages.isNotEmpty()) {
                val pageUrl = pages.first().url
                println("Testing download of page: $pageUrl")
                val client = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()
                val response = client.newCall(request).execute()
                println("Response code: ${response.code}, Content-Type: ${response.header("Content-Type")}, Body size: ${response.body?.bytes()?.size}")
            }
        }
    }
}