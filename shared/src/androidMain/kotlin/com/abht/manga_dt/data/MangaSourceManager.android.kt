package com.abht.manga_dt.data

import com.abht.manga_dt.data.mangasource.BuiltinMangaSources
import com.abht.manga_dt.data.mangasource.JsonMangaSourceEngine
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.bitmap.Bitmap
import org.koitharu.kotatsu.parsers.bitmap.Rect
import org.koitharu.kotatsu.parsers.config.MangaSourceConfig
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaSource as KotatsuMangaSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.net.URLEncoder

class AndroidMangaLoaderContext(
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
        return AndroidBitmap(android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888))
    }
}

class AndroidBitmap(val bitmap: android.graphics.Bitmap) : Bitmap {
    override val width: Int get() = bitmap.width
    override val height: Int get() = bitmap.height
    override fun drawBitmap(sourceBitmap: Bitmap, src: Rect, dst: Rect) {
        val canvas = android.graphics.Canvas(bitmap)
        val srcRect = android.graphics.Rect(src.left, src.top, src.right, src.bottom)
        val dstRect = android.graphics.Rect(dst.left, dst.top, dst.right, dst.bottom)
        canvas.drawBitmap((sourceBitmap as AndroidBitmap).bitmap, srcRect, dstRect, null)
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
    private val context = AndroidMangaLoaderContext(client, AppCookieJar)

    private suspend fun executeHttpRequest(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val reqBuilder = okhttp3.Request.Builder().url(url)
        headers.forEach { (k, v) -> reqBuilder.header(k, v) }
        if (method.equals("POST", ignoreCase = true)) {
            reqBuilder.post(ByteArray(0).toRequestBody())
        } else {
            reqBuilder.get()
        }
        client.newCall(reqBuilder.build()).execute().use { response ->
            response.body?.string().orEmpty()
        }
    }

    actual fun getSourceBaseUrl(sourceId: String): String {
        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return jsonDef.baseUrl
        }

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
        val kotatsuSources = if (AppSettings.enableKotatsuSources) {
            MangaParserSource.entries.map { 
                MangaSource(
                    id = it.name, 
                    name = it.title,
                    iconUrl = "https://www.google.com/s2/favicons?sz=64&domain=${it.name.lowercase().replace('_', '-')}.com",
                    locale = it.locale,
                    contentType = it.contentType.name,
                    isBroken = it.isBroken
                )
            }
        } else emptyList()

        val mangaSources = if (AppSettings.enableMangaSources) {
            BuiltinMangaSources.getAllSources().map { it.toMangaSource() }
        } else emptyList()

        val allSources = (mangaSources + kotatsuSources).sortedBy { it.name.lowercase() }

        val filtered = if (!isNsfw) {
            allSources.filter { it.contentType != "HENTAI" && !it.name.contains("hentai", ignoreCase = true) && !it.id.contains("hentai", ignoreCase = true) }
        } else {
            allSources
        }
        MangaDataCache.cachedSources = filtered
        return filtered
    }

    actual suspend fun searchManga(sourceId: String, query: String): List<Manga> {
        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val targetUrl = jsonDef.search.urlTemplate
                    .replace("{base_url}", jsonDef.baseUrl.trimEnd('/'))
                    .replace("{query}", encodedQuery)
                val html = executeHttpRequest(targetUrl, jsonDef.search.method)
                val list = JsonMangaSourceEngine.extractMangaListFromHtml(html, jsonDef.search, jsonDef)
                val isNsfw = AppSettings.isNsfwAllowed
                if (!isNsfw) list.filterNot { isNsfwManga(it) } else list
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

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

        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return try {
                val targetUrl = when {
                    jsonDef.latest != null && page == 1 -> {
                        jsonDef.latest.urlTemplate.replace("{base_url}", jsonDef.baseUrl.trimEnd('/'))
                    }
                    jsonDef.search.urlTemplate.contains("wp-manga") -> {
                        if (page == 1) "${jsonDef.baseUrl.trimEnd('/')}/?s=&post_type=wp-manga&m_orderby=views"
                        else "${jsonDef.baseUrl.trimEnd('/')}/page/$page/?s=&post_type=wp-manga&m_orderby=views"
                    }
                    jsonDef.latest != null -> {
                        jsonDef.latest.urlTemplate.replace("{base_url}", jsonDef.baseUrl.trimEnd('/')).replace("{page}", page.toString())
                    }
                    else -> {
                        jsonDef.search.urlTemplate.replace("{base_url}", jsonDef.baseUrl.trimEnd('/')).replace("{query}", "").replace("{page}", page.toString())
                    }
                }
                val step = jsonDef.latest ?: jsonDef.search
                val html = executeHttpRequest(targetUrl, step.method)
                val list = JsonMangaSourceEngine.extractMangaListFromHtml(html, step, jsonDef)
                val isNsfw = AppSettings.isNsfwAllowed
                val filtered = if (!isNsfw) list.filterNot { isNsfwManga(it) } else list
                if (page == 1 && filtered.isNotEmpty()) {
                    MangaDataCache.cachedPopularManga[sourceId] = filtered
                }
                filtered
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
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

        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return try {
                val cleanUrl = JsonMangaSourceEngine.normalizeUrl(mangaUrl, jsonDef.baseUrl)
                val mangaId = cleanUrl
                    .removePrefix(jsonDef.baseUrl)
                    .removePrefix("/manga/")
                    .removePrefix("manga/")
                    .removePrefix("/")
                    .removeSuffix("/")
                    .trim()

                val detailsUrl = if (jsonDef.details != null) {
                    jsonDef.details.urlTemplate
                        .replace("{base_url}", jsonDef.baseUrl.trimEnd('/'))
                        .replace("{manga_id}", mangaId)
                } else {
                    cleanUrl
                }
                val detailsHtml = executeHttpRequest(detailsUrl, jsonDef.details?.method ?: "GET")
                val baseManga = JsonMangaSourceEngine.extractMangaDetailsFromHtml(detailsHtml, mangaId.ifBlank { mangaUrl }, jsonDef)

                val chaptersUrl = jsonDef.chapters.urlTemplate
                    .replace("{base_url}", jsonDef.baseUrl.trimEnd('/'))
                    .replace("{manga_id}", mangaId)
                val chaptersHtml = try {
                    executeHttpRequest(chaptersUrl, jsonDef.chapters.method)
                } catch (e: Exception) {
                    detailsHtml
                }
                val chapters = JsonMangaSourceEngine.extractChaptersFromHtml(
                    if (chaptersHtml.isNotBlank()) chaptersHtml else detailsHtml,
                    mangaId,
                    jsonDef.chapters,
                    jsonDef
                )

                val mapped = baseManga.copy(chapters = chapters, url = cleanUrl)
                MangaDataCache.cachedMangaDetails[cacheKey] = mapped
                mapped
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

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

        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return try {
                val cleanChapterUrl = JsonMangaSourceEngine.normalizeUrl(chapterUrl, jsonDef.baseUrl)
                val chapterId = cleanChapterUrl
                    .removePrefix(jsonDef.baseUrl)
                    .removePrefix("/manga/")
                    .removePrefix("/chapters/")
                    .removePrefix("/read/")
                    .removePrefix("/")
                    .removeSuffix("/")
                    .trim()

                val targetUrl = if (chapterUrl.startsWith("http://") || chapterUrl.startsWith("https://")) {
                    chapterUrl
                } else {
                    jsonDef.pages.urlTemplate
                        .replace("{base_url}", jsonDef.baseUrl.trimEnd('/'))
                        .replace("{chapter_id}", chapterId)
                }
                val html = executeHttpRequest(targetUrl, jsonDef.pages.method)
                val pages = JsonMangaSourceEngine.extractPagesFromHtml(html, jsonDef.pages, jsonDef)
                if (pages.isNotEmpty()) {
                    MangaDataCache.cachedPages[cacheKey] = pages
                }
                pages
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

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
        val jsonDef = BuiltinMangaSources.findById(sourceId)
        if (jsonDef != null) {
            return try {
                val list = getPopularManga(sourceId, 1)
                if (list.isEmpty()) "Source returned empty list" else null
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
        }

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
        val firstChapter = kotatsuManga.chapters?.firstOrNull()
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
            publicUrl = kotatsuManga.publicUrl.ifBlank { kotatsuManga.url },
            latestChapter = firstChapter?.let { if (!it.title.isNullOrBlank()) it.title else "Ch. ${it.number}" }
        )
    }

    private fun mapStatus(state: MangaState?): MangaStatus {
        return when (state) {
            MangaState.ONGOING -> MangaStatus.ONGOING
            MangaState.FINISHED -> MangaStatus.COMPLETED
            MangaState.ABANDONED -> MangaStatus.DROPPED
            MangaState.PAUSED -> MangaStatus.ON_HOLD
            else -> MangaStatus.UNKNOWN
        }
    }
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual object SettingsStorage {
    private var prefs: android.content.SharedPreferences? = null
    private val inMemory = mutableMapOf<String, String>()
    var appContext: android.content.Context? = null
        private set

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("mangadt_prefs", android.content.Context.MODE_PRIVATE)
            com.abht.manga_dt.data.AppSettings.reload()
            com.abht.manga_dt.data.LibraryManager.reload()
            com.abht.manga_dt.data.HistoryManager.reload()
            com.abht.manga_dt.data.StatisticsManager.reload()
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: inMemory.getOrDefault(key, defaultValue)
    }

    actual fun setString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
        inMemory[key] = value
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: inMemory[key]?.toBooleanStrictOrNull() ?: defaultValue
    }

    actual fun setBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
        inMemory[key] = value.toString()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return prefs?.getInt(key, defaultValue) ?: inMemory[key]?.toIntOrNull() ?: defaultValue
    }

    actual fun setInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
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
