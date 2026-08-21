package com.abht.manga_dt.data

import com.abht.manga_dt.models.Manga
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.model.MangaSource as KotatsuMangaSource
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.SortOrder
import okhttp3.Response
import java.awt.image.BufferedImage

class JvmMangaLoaderContext(
    override val httpClient: OkHttpClient,
    override val cookieJar: CookieJar
) : MangaLoaderContext() {
    @Deprecated("Use evaluateJs with baseUrl and timeout")
    override suspend fun evaluateJs(script: String): String? = null
    override suspend fun evaluateJs(baseUrl: String, script: String, timeout: Long): String? = null
    
    override fun getConfig(source: KotatsuMangaSource): MangaSourceConfig = object : MangaSourceConfig {
        override fun <T> get(key: org.koitharu.kotatsu.parsers.config.ConfigKey<T>): T = key.defaultValue
    }
    
    override fun getDefaultUserAgent(): String = APP_USER_AGENT
    
    override fun redrawImageResponse(response: Response, redraw: (image: Bitmap) -> Bitmap): Response = response
    
    override fun createBitmap(width: Int, height: Int): Bitmap {
        return JvmBitmap(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB))
    }
}

class JvmBitmap(val image: BufferedImage) : Bitmap {
    override val width: Int get() = image.width
    override val height: Int get() = image.height
    override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
        val g = image.createGraphics()
        g.drawImage(
            (sourceBitmap as JvmBitmap).image,
            dst.left, dst.top, dst.right, dst.bottom,
            src.left, src.top, src.right, src.bottom,
            null
        )
        g.dispose()
    }
}

actual class MangaSourceManager actual constructor() {
    private val client = OkHttpClient.Builder()
        .cookieJar(AppCookieJar)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", APP_USER_AGENT)
                .build()
            chain.proceed(req)
        }
        .build()
    private val context = JvmMangaLoaderContext(client, AppCookieJar)

    actual fun getSourceBaseUrl(sourceId: String): String {
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull() ?: return "https://google.com"
        return try {
            val parser = context.newParserInstance(parserSource)
            val domain = parser.domain
            if (domain.startsWith("http://") || domain.startsWith("https://")) domain else "https://$domain"
        } catch (e: Exception) {
            "https://${parserSource.name.lowercase().replace('_', '-')}.com"
        }
    }

    private fun isNsfwManga(manga: Manga): Boolean {
        if (manga.isNsfw) return true
        val nsfwTags = setOf("hentai", "adult", "pornographic", "smut", "erotica", "18+", "nsfw")
        return manga.tags.any { tag -> nsfwTags.contains(tag.lowercase().trim()) }
    }

    actual suspend fun getAvailableSources(): List<MangaSource> {
        val isNsfw = AppSettings.isNsfwAllowed
        val allSources = MangaParserSource.entries.map { 
            MangaSource(
                id = it.name, 
                name = it.title,
                iconUrl = "https://www.google.com/s2/favicons?sz=64&domain=${it.name.lowercase().replace('_', '-')}.com",
                locale = it.locale,
                contentType = it.contentType.name,
                isBroken = it.isBroken
            )
        }.sortedBy { it.name.lowercase() }

        val filtered = if (!isNsfw) {
            allSources.filter { it.contentType != "HENTAI" && !it.name.contains("hentai", ignoreCase = true) && !it.id.contains("hentai", ignoreCase = true) }
        } else {
            allSources
        }
        MangaDataCache.cachedSources = filtered
        return filtered
    }

    actual suspend fun searchManga(sourceId: String, query: String): List<Manga> {
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull() ?: return emptyList()
        return try {
            val parser = context.newParserInstance(parserSource)
            val filter = MangaListFilter(query = query)
            val result = parser.getList(0, SortOrder.RELEVANCE, filter)
            val mapped = result.map { mapManga(it) }
            val isNsfw = AppSettings.isNsfwAllowed
            if (!isNsfw) mapped.filterNot { isNsfwManga(it) } else mapped
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun getPopularManga(sourceId: String, page: Int): List<Manga> {
        if (page == 1 && MangaDataCache.cachedPopularManga.containsKey(sourceId)) {
            val cached = MangaDataCache.cachedPopularManga[sourceId]
            if (!cached.isNullOrEmpty()) {
                val isNsfw = AppSettings.isNsfwAllowed
                return if (!isNsfw) cached.filterNot { isNsfwManga(it) } else cached
            }
        }
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull() ?: return emptyList()
        return try {
            val parser = context.newParserInstance(parserSource)
            val pageSize = 20
            val offset = (page - 1) * pageSize
            val result = parser.getList(offset, SortOrder.POPULARITY, MangaListFilter.EMPTY)
            val mapped = result.map { mapManga(it) }
            val isNsfw = AppSettings.isNsfwAllowed
            val filtered = if (!isNsfw) mapped.filterNot { isNsfwManga(it) } else mapped
            if (page == 1 && filtered.isNotEmpty()) {
                MangaDataCache.cachedPopularManga[sourceId] = filtered
            }
            filtered
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun getMangaDetails(sourceId: String, mangaUrl: String): Manga? {
        val cacheKey = "$sourceId::$mangaUrl"
        MangaDataCache.cachedMangaDetails[cacheKey]?.let { return it }
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull() ?: return null
        return try {
            val parser = context.newParserInstance(parserSource)
            val dummyManga = org.koitharu.kotatsu.parsers.model.Manga(
                id = 0L,
                title = "",
                altTitle = null,
                url = mangaUrl,
                publicUrl = mangaUrl,
                rating = 0f,
                isNsfw = false,
                coverUrl = null,
                tags = emptySet(),
                state = null,
                author = null,
                source = parserSource
            )
            val details = parser.getDetails(dummyManga)
            val chapters = details.chapters?.map { c ->
                com.abht.manga_dt.models.Chapter(
                    id = c.id.toString(),
                    mangaId = mangaUrl,
                    title = c.title.orEmpty().ifBlank { "Chapter ${c.number}" },
                    chapterNumber = c.number,
                    uploadDate = c.uploadDate,
                    scanlator = c.scanlator,
                    url = c.url
                )
            } ?: emptyList()
            val mapped = mapManga(details).copy(chapters = chapters, url = details.url)
            MangaDataCache.cachedMangaDetails[cacheKey] = mapped
            mapped
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun getPages(sourceId: String, chapterUrl: String): List<com.abht.manga_dt.models.ReaderPage> {
        val cacheKey = "$sourceId::$chapterUrl"
        MangaDataCache.cachedPages[cacheKey]?.let { return it }
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull() ?: return emptyList()
        return try {
            val parser = context.newParserInstance(parserSource)
            val dummyChapter = org.koitharu.kotatsu.parsers.model.MangaChapter(
                id = 0L,
                title = "",
                number = 1f,
                volume = 0,
                url = chapterUrl,
                scanlator = null,
                uploadDate = 0L,
                branch = null,
                source = parserSource
            )
            val pages = parser.getPages(dummyChapter)
            val mapped = pages.mapIndexed { index, p ->
                val directUrl = try {
                    parser.getPageUrl(p)
                } catch (e: Exception) {
                    p.url
                }
                com.abht.manga_dt.models.ReaderPage(
                    url = directUrl.ifBlank { p.url },
                    pageNumber = index + 1
                )
            }
            if (mapped.isNotEmpty()) {
                MangaDataCache.cachedPages[cacheKey] = mapped
            }
            mapped
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    actual suspend fun testSource(sourceId: String): String? {
        val parserSource = runCatching { MangaParserSource.valueOf(sourceId) }.getOrNull()
            ?: return "Invalid source ID: $sourceId"
        return try {
            val parser = context.newParserInstance(parserSource)
            parser.getList(0, SortOrder.POPULARITY, MangaListFilter.EMPTY)
            null
        } catch (e: Exception) {
            e.message ?: e.toString()
        }
    }

    private fun mapManga(kotatsuManga: org.koitharu.kotatsu.parsers.model.Manga): Manga {
        val authorsList = kotatsuManga.authors.filter { it.isNotBlank() }
        val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else kotatsuManga.author
        return Manga(
            id = kotatsuManga.id.toString(),
            title = kotatsuManga.title,
            thumbnailUrl = kotatsuManga.largeCoverUrl ?: kotatsuManga.coverUrl ?: "",
            author = authorName,
            description = kotatsuManga.description,
            status = mapStatus(kotatsuManga.state),
            source = kotatsuManga.source.name,
            url = kotatsuManga.url,
            altTitle = kotatsuManga.altTitle,
            rating = kotatsuManga.rating,
            isNsfw = kotatsuManga.isNsfw,
            tags = kotatsuManga.tags.map { it.title }.filter { it.isNotBlank() },
            publicUrl = kotatsuManga.publicUrl.ifBlank { kotatsuManga.url }
        )
    }

    private fun mapStatus(state: org.koitharu.kotatsu.parsers.model.MangaState?): com.abht.manga_dt.models.MangaStatus {
        return when (state) {
            org.koitharu.kotatsu.parsers.model.MangaState.ONGOING -> com.abht.manga_dt.models.MangaStatus.ONGOING
            org.koitharu.kotatsu.parsers.model.MangaState.FINISHED -> com.abht.manga_dt.models.MangaStatus.COMPLETED
            org.koitharu.kotatsu.parsers.model.MangaState.ABANDONED -> com.abht.manga_dt.models.MangaStatus.DROPPED
            org.koitharu.kotatsu.parsers.model.MangaState.PAUSED -> com.abht.manga_dt.models.MangaStatus.ON_HOLD
            else -> com.abht.manga_dt.models.MangaStatus.UNKNOWN
        }
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual object SettingsStorage {
    private val prefs = runCatching {
        java.util.prefs.Preferences.userRoot().node("com/abht/manga_dt/settings")
    }.getOrNull()
    private val inMemory = mutableMapOf<String, String>()

    actual fun getString(key: String, defaultValue: String): String {
        return prefs?.get(key, defaultValue) ?: inMemory.getOrDefault(key, defaultValue)
    }

    actual fun setString(key: String, value: String) {
        prefs?.put(key, value)
        inMemory[key] = value
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: inMemory[key]?.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun setBoolean(key: String, value: Boolean) {
        prefs?.putBoolean(key, value)
        inMemory[key] = value.toString()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return prefs?.getInt(key, defaultValue) ?: inMemory[key]?.toIntOrNull() ?: defaultValue
    }

    actual fun setInt(key: String, value: Int) {
        prefs?.putInt(key, value)
        inMemory[key] = value.toString()
    }

    actual fun getStringSet(key: String): Set<String> {
        val raw = getString(key, "")
        if (raw.isBlank()) return emptySet()
        return raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    actual fun setStringSet(key: String, values: Set<String>) {
        setString(key, values.joinToString("|||"))
    }
}
