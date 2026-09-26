package com.abht.manga_dt

import com.abht.manga_dt.data.AppSettings
import com.abht.manga_dt.data.MangaSourceManager
import com.abht.manga_dt.data.mangasource.BuiltinMangaSources
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

        // Verify all manga-source sources are present
        val mangaSourceIds = BuiltinMangaSources.getAllSources().map { it.id }
        mangaSourceIds.forEach { id ->
            val found = sources.any { it.id.equals(id, ignoreCase = true) }
            println("Checking manga-source '$id': found=$found")
            assertTrue(found, "Expected source $id to be available in MangaSourceManager")
        }
    }

    @Test
    fun testMangaSourceSearchAndDetails() = runBlocking {
        val manager = MangaSourceManager()
        
        // 1. Test Mangapill
        try {
            val pillResults = manager.searchManga("mangapill", "Naruto")
            println("Mangapill search results count: ${pillResults.size}")
            if (pillResults.isNotEmpty()) {
                val first = pillResults.first()
                println("Mangapill first manga: ${first.title} -> url: ${first.url}")
                assertTrue(first.title.isNotBlank())

                val details = manager.getMangaDetails("mangapill", first.url)
                if (details != null) {
                    println("Mangapill details: ${details.title}, chapters count: ${details.chapters.size}")
                    if (details.chapters.isNotEmpty()) {
                        val firstCh = details.chapters.first()
                        val pages = manager.getPages("mangapill", firstCh.url)
                        println("Mangapill pages count: ${pages.size}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Mangapill network test error: ${e.message}")
        }

        // 2. Test 3asq
        try {
            val asqResults = manager.searchManga("3asq", "One Piece")
            println("3asq search results count: ${asqResults.size}")
            if (asqResults.isNotEmpty()) {
                val first = asqResults.first()
                println("3asq first manga: ${first.title} -> url: ${first.url}")
                assertTrue(first.title.isNotBlank())

                val details = manager.getMangaDetails("3asq", first.url)
                if (details != null) {
                    println("3asq details: ${details.title}, chapters count: ${details.chapters.size}")
                    if (details.chapters.isNotEmpty()) {
                        val firstCh = details.chapters.first()
                        val pages = manager.getPages("3asq", firstCh.url)
                        println("3asq pages count: ${pages.size}")
                    }
                }
            }
        } catch (e: Exception) {
            println("3asq network test error: ${e.message}")
        }

        // 3. Test MGRead (New Source)
        try {
            val mgreadResults = manager.searchManga("mgread", "Solo")
            println("MGRead search results count: ${mgreadResults.size}")
            if (mgreadResults.isNotEmpty()) {
                val first = mgreadResults.first()
                println("MGRead first manga: ${first.title} -> url: ${first.url}")
                assertTrue(first.title.isNotBlank())
            }
        } catch (e: Exception) {
            println("MGRead network test error: ${e.message}")
        }
    }

    @Test
    fun testProviderToggles() = runBlocking {
        val manager = MangaSourceManager()

        // 1. Both enabled
        AppSettings.updateEnableKotatsuSources(true)
        AppSettings.updateEnableMangaSources(true)
        val allSources = manager.getAvailableSources()
        val hasKotatsu = allSources.any { !BuiltinMangaSources.isMangaSourceId(it.id) }
        val hasMangaSource = allSources.any { BuiltinMangaSources.isMangaSourceId(it.id) }
        assertTrue(hasKotatsu, "Expected Kotatsu sources when enabled")
        assertTrue(hasMangaSource, "Expected MangaSource sources when enabled")

        // 2. Disable Kotatsu -> only MangaSource
        AppSettings.updateEnableKotatsuSources(false)
        val onlyMangaSources = manager.getAvailableSources()
        val hasOnlyMangaSource = onlyMangaSources.all { BuiltinMangaSources.isMangaSourceId(it.id) }
        assertTrue(hasOnlyMangaSource, "Expected only manga-source when Kotatsu is disabled")

        // 3. Restore both
        AppSettings.updateEnableKotatsuSources(true)
        AppSettings.updateEnableMangaSources(true)
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
}