package com.abht.manga_dt.data.mangasource

object BuiltinMangaSources {

    val SOURCE_3ASQ = JsonSourceDefinition(
        id = "3asq",
        name = "Manga 3asq (العاشق - عربي)",
        baseUrl = "https://3asq.online",
        languages = listOf("ar"),
        isNsfw = false,
        tags = listOf("arabic", "wp-manga", "translated", "manga", "manhwa"),
        search = RequestStep(
            urlTemplate = "{base_url}/?s={query}&post_type=wp-manga",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"row c-tabs-item__content\">",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<h3 class=\"h4\">\\s*<a [^>]*>([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                altTitleRegex = "(?s)mg_alternative.*?<div class=\"summary-content\">\\s*([^<]+)",
                statusRegex = "(?s)mg_status.*?<div class=\"summary-content\">\\s*([^<]+)",
                authorRegex = "(?s)mg_author.*?<div class=\"summary-content\">\\s*([^<]+)",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "(?s)latest-chap.*?<span class=\"font-meta chapter\"><a [^>]*>([^<]+)</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                genresRegex = "href=\"https?://[^/]+/manga-genre/[^/]+/\"[^>]*>([^<]+)</a>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "(?s)<div class=\"page-item-detail manga\\s*[^\"]*\">(.*?)</div>\\s*<!-- \\.page-item-detail -->",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<div class=\"post-title[^\"]*\">.*?<a href=\"https?://[^/]+/manga/[^/]+/\">([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "<span class=\"chapter font-meta\">\\s*<a[^>]*>\\s*([^<]+)\\s*</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/ajax/chapters/",
            method = "POST",
            regex = RegexExtractor(
                pattern = "href=\"https?://[^/]+/manga/{manga_id}/([^/]+)/\"[^>]*>([\\s\\S]*?)</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "src=\"\\s*([^\"]+)\"[^>]*class=\"[^\"]*wp-manga-chapter-img[^\"]*",
                idGroup = 1
            )
        )
    )

    val SOURCE_MANGALEK = JsonSourceDefinition(
        id = "mangalek",
        name = "Manga-Lek (مانجا ليك - عربي)",
        baseUrl = "https://mangalik.net",
        languages = listOf("ar"),
        isNsfw = false,
        tags = listOf("arabic", "wp-manga", "translated", "manga", "manhwa"),
        search = RequestStep(
            urlTemplate = "{base_url}/?s={query}&post_type=wp-manga",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"row c-tabs-item__content\">",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<h3 class=\"h4\">\\s*<a [^>]*>([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                altTitleRegex = "(?s)mg_alternative.*?<div class=\"summary-content\">\\s*([^<]+)",
                statusRegex = "(?s)mg_status.*?<div class=\"summary-content\">\\s*([^<]+)",
                authorRegex = "(?s)mg_author.*?<div class=\"summary-content\">\\s*([^<]+)",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "(?s)latest-chap.*?<span class=\"font-meta chapter\"><a [^>]*>([^<]+)</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                genresRegex = "href=\"https?://[^/]+/manga-genre/[^/]+/\"[^>]*>([^<]+)</a>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"page-item-detail manga",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<div class=\"post-title[^\"]*\">.*?<a [^>]*>([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "<span class=\"chapter font-meta\">\\s*<a[^>]*>\\s*([^<]+)\\s*</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/ajax/chapters/",
            method = "POST",
            regex = RegexExtractor(
                pattern = "href=\"https?://[^/]+/manga/{manga_id}/([^/]+)/\"[^>]*>([\\s\\S]*?)</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "src=\"\\s*([^\"]+)\"[^>]*class=\"[^\"]*wp-manga-chapter-img[^\"]*",
                idGroup = 1
            )
        )
    )

    val SOURCE_MANGALEKO = JsonSourceDefinition(
        id = "mangaleko",
        name = "MangaLeko (مانجا ليكو - عربي)",
        baseUrl = "https://mangaleko.com",
        languages = listOf("ar"),
        isNsfw = false,
        tags = listOf("arabic", "wp-manga", "translated", "manga", "manhwa"),
        search = RequestStep(
            urlTemplate = "{base_url}/?s={query}&post_type=wp-manga",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "(?s)<div class=\"row c-tabs-item__content\">(.*?)(?=<div class=\"row c-tabs-item__content\"|<div class=\"wp-pagenavi\"|</div>\\s*</div>\\s*</div>\\s*</div>\\s*</div>)",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "<h3 class=\"h4\">\\s*<a [^>]*>([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "latest-chap.*?<span class=\"font-meta chapter\"><a [^>]*>([^<]+)</a>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "(?s)<div class=\"page-item-detail manga\\s*[^\"]*\">(.*?)</div>\\s*<!-- \\.page-item-detail -->",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<div class=\"post-title[^\"]*\">.*?<a href=\"https?://[^/]+/manga/[^/]+/\">([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "<span class=\"chapter font-meta\">\\s*<a[^>]*>\\s*([^<]+)\\s*</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/ajax/chapters/",
            method = "POST",
            regex = RegexExtractor(
                pattern = "href=\"https?://[^/]+/manga/{manga_id}/([^/]+)/\"[^>]*>([\\s\\S]*?)</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "src=\"\\s*([^\"]+)\"[^>]*class=\"[^\"]*wp-manga-chapter-img[^\"]*",
                idGroup = 1
            )
        )
    )

    val SOURCE_MANGAPILL = JsonSourceDefinition(
        id = "mangapill",
        name = "Mangapill (Fast High-Quality Manga)",
        baseUrl = "https://mangapill.com",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "manga", "fast", "scans"),
        search = RequestStep(
            urlTemplate = "{base_url}/search?q={query}",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<figure class=\"w-full h-52",
                idRegex = "href=\"/manga/([^\"\\s]+)\"",
                coverRegex = "data-src=\"([^\"]+)\"",
                titleRegex = "(?s)class=\"[^\"]*font-black[^\"]*\">\\s*([^<]+)</div>",
                genresRegex = "class=\"[^\"]*bg-card[^\"]*\">([^<]+)</div>",
                yearRegex = "bg-orange-500[^\"]*\">([0-9]+)</div>",
                statusRegex = "bg-green-500[^\"]*\">([^<]+)</div>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<figure class=\"w-full h-52",
                idRegex = "href=\"/manga/([^\"\\s]+)\"",
                coverRegex = "data-src=\"([^\"]+)\"",
                titleRegex = "(?s)class=\"[^\"]*font-black[^\"]*\">\\s*([^<]+)</div>",
                genresRegex = "class=\"[^\"]*bg-card[^\"]*\">([^<]+)</div>",
                yearRegex = "bg-orange-500[^\"]*\">([0-9]+)</div>",
                statusRegex = "bg-green-500[^\"]*\">([^<]+)</div>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}",
            method = "GET",
            regex = RegexExtractor(
                pattern = "href=\"/chapters/([^\"]+)\"[^>]*>\\s*(?:Chapter\\s*)?([0-9.]+)\\s*</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/chapters/{chapter_id}",
            method = "GET",
            regex = RegexExtractor(
                pattern = "<img [^>]*class=\"[^\"]*js-page[^\"]*\"[^>]*data-src=\"([^\"]+)\"|data-src=\"([^\"]+)\"[^>]*class=\"[^\"]*js-page[^\"]*\"",
                idGroup = 1
            )
        )
    )

    val SOURCE_LIKEMANGA = JsonSourceDefinition(
        id = "likemanga",
        name = "LikeManga (Global Manhwa)",
        baseUrl = "https://likemanga.ink",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "manhwa", "webtoon"),
        search = RequestStep(
            urlTemplate = "{base_url}/?act=ajax&code=search_manga&keyword={query}",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<li><a href=\"/",
                idRegex = "^([^/]+)/",
                coverRegex = "src=\"([^\"]+)\"",
                titleRegex = "<h3>([^<]+)</h3>",
                latestChapterRegex = "<b>([^<]+)</b>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<li><a href=\"/",
                idRegex = "^([^/]+)/",
                coverRegex = "src=\"([^\"]+)\"",
                titleRegex = "<h3>([^<]+)</h3>",
                latestChapterRegex = "<b>([^<]+)</b>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/{manga_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "href=\"/[^/]+/(chapter-[0-9.]+-[0-9]+)/\"[^>]*>(?:Chapter\\s*)?([0-9.]+)",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "src=\"(https?://[^\"\\s]+/(?:[0-9]+/[0-9]+/[0-9]+\\.[a-zA-Z0-9]+))\"",
                idGroup = 1
            )
        )
    )

    val SOURCE_MANGAREAD = JsonSourceDefinition(
        id = "mangaread",
        name = "MangaRead (English Webtoons & Manhwa)",
        baseUrl = "https://www.mangaread.org",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "wp-manga", "manhwa", "webtoon"),
        search = RequestStep(
            urlTemplate = "{base_url}/?s={query}&post_type=wp-manga",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"row c-tabs-item__content\">",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<h3 class=\"h4\">\\s*<a [^>]*>([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "(?s)latest-chap.*?<span class=\"font-meta chapter\"><a [^>]*>([^<]+)</a>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "(?s)<div class=\"page-item-detail manga\\s*[^\"]*\">(.*?)</div>\\s*<!-- \\.page-item-detail -->",
                idRegex = "href=\"https?://[^/]+/manga/([^/]+)/\"",
                titleRegex = "(?s)<div class=\"post-title[^\"]*\">.*?<a href=\"https?://[^/]+/manga/[^/]+/\">([^<]+)</a>",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                ratingRegex = "class=\"score font-meta total_votes\">([0-9.]+)",
                viewsRegex = "<span class=\"views\"><i class=\"fa fa-eye\"></i>\\s*([^<]+)</span>",
                latestChapterRegex = "<span class=\"chapter font-meta\">\\s*<a[^>]*>\\s*([^<]+)\\s*</a>",
                updatedAtRegex = "<span class=\"timediff\">([^<]+)</span>",
                tagsRegex = "<span class=\"manga-title-badges[^\"]*\">\\s*(?:<span class=\"text\">)?([^<]+)(?:</span>)?\\s*</span>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/ajax/chapters/",
            method = "POST",
            regex = RegexExtractor(
                pattern = "href=\"https://[^/]+/manga/{manga_id}/([^/]+)/\"[^>]*>([\\s\\S]*?)</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "src=\"\\s*([^\"]+)\"[^>]*class=\"[^\"]*wp-manga-chapter-img[^\"]*",
                idGroup = 1
            )
        )
    )

    val SOURCE_MGREAD = JsonSourceDefinition(
        id = "mgread",
        name = "MGRead (Global Manhwa)",
        baseUrl = "https://mgread.io",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "manhwa", "webtoon"),
        search = RequestStep(
            urlTemplate = "{base_url}/?s={query}",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:href=https://mgread.io/manga/",
                idRegex = "^([^/]+)/",
                coverRegex = "src=([^\\s>]+)",
                titleRegex = "(?s)<h2 [^>]*>\\s*<a [^>]*>([^<]+)</a>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:href=https://mgread.io/manga/",
                idRegex = "^([^/]+)/",
                coverRegex = "src=([^\\s>]+)",
                titleRegex = "(?s)<h2 [^>]*>\\s*<a [^>]*>([^<]+)</a>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "href=https://mgread.io/manga/[^/]+/(chapter-[0-9.]+)/",
                idGroup = 1,
                titleGroup = 1
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "(?:data-original-src|src)=(https://mg\\.mgread\\.io/[^\\s>]+)",
                idGroup = 1
            )
        )
    )

    val SOURCE_FANFOX = JsonSourceDefinition(
        id = "fanfox",
        name = "FanFox / MangaFox (English Classic)",
        baseUrl = "https://fanfox.net",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "manga", "classic", "shounen"),
        search = RequestStep(
            urlTemplate = "{base_url}/search?title={query}",
            method = "GET",
            regex = RegexExtractor(
                pattern = "(?s)<a href=\"/manga/([^/]+)/\"[^>]*>\\s*<img class=\"manga-list-4-cover\" src=\"([^\"]+)\"[^>]*>.*?<p class=\"manga-list-4-item-title\">\\s*<a [^>]*>([^<]+)</a>",
                idGroup = 1,
                coverGroup = 2,
                titleGroup = 3
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/releases/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "(?s)<a href=\"/manga/([^/]+)/\"[^>]*>\\s*<img class=\"manga-list-4-cover\" src=\"([^\"]+)\"[^>]*>.*?<p class=\"manga-list-4-item-title\">\\s*<a [^>]*>([^<]+)</a>",
                idGroup = 1,
                coverGroup = 2,
                titleGroup = 3
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}/",
            method = "GET",
            regex = RegexExtractor(
                pattern = "href=\"/manga/[^/]+/c([0-9.]+)/1\\.html\"[^>]*title=\"[^\"]*\">\\s*Ch\\.?([0-9.]+)\\s*</a>",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/manga/{chapter_id}/1.html",
            method = "GET",
            regex = RegexExtractor(
                pattern = "<img [^>]*src=\"([^\"]*store/manga/[^\"]+)",
                idGroup = 1
            )
        )
    )

    val SOURCE_MANGAFIRE = JsonSourceDefinition(
        id = "mangafire",
        name = "MangaFire (mangafire.to)",
        baseUrl = "https://mangafire.to",
        languages = listOf("en"),
        isNsfw = false,
        tags = listOf("english", "manga", "manhwa"),
        search = RequestStep(
            urlTemplate = "{base_url}/filter?keyword={query}",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"unit\">",
                idRegex = "href=\"/manga/([^\"\\s]+)\"",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                titleRegex = "(?s)<a href=\"/manga/[^\"]+\"[^>]*>\\s*([^<]+)\\s*</a>",
                latestChapterRegex = "href=\"/read/[^\"]*chapter-([0-9.]+)\"",
                typeRegex = "<span class=\"type\">([^<]+)</span>"
            )
        ),
        latest = RequestStep(
            urlTemplate = "{base_url}/filter?sort=recently_updated",
            method = "GET",
            regex = RegexExtractor(
                itemPattern = "split:<div class=\"unit\">",
                idRegex = "href=\"/manga/([^\"\\s]+)\"",
                coverRegex = "<img [^>]*(?:src|data-src)=\"([^\"]+)\"",
                titleRegex = "(?s)<a href=\"/manga/[^\"]+\"[^>]*>\\s*([^<]+)\\s*</a>",
                latestChapterRegex = "href=\"/read/[^\"]*chapter-([0-9.]+)\"",
                typeRegex = "<span class=\"type\">([^<]+)</span>"
            )
        ),
        chapters = RequestStep(
            urlTemplate = "{base_url}/manga/{manga_id}",
            method = "GET",
            regex = RegexExtractor(
                pattern = "href=\"/read/[^/]+/(?:en|ar|ja|es|fr)/([^\"]+)\"[^>]*>(?:Chapter\\s*)?([0-9.]+)",
                idGroup = 1,
                titleGroup = 2
            )
        ),
        pages = RequestStep(
            urlTemplate = "{base_url}/read/{chapter_id}",
            method = "GET",
            regex = RegexExtractor(
                pattern = "(?:data-url|src)=\"([^\"]+(?:\\.jpg|\\.jpeg|\\.png|\\.webp)[^\"]*)\"",
                idGroup = 1
            )
        )
    )

    private val BUILTIN_SOURCES = listOf(
        SOURCE_3ASQ,
        SOURCE_MANGALEK,
        SOURCE_MANGALEKO,
        SOURCE_MANGAPILL,
        SOURCE_LIKEMANGA,
        SOURCE_MANGAREAD,
        SOURCE_MGREAD,
        SOURCE_FANFOX,
        SOURCE_MANGAFIRE
    )

    fun getAllSources(): List<JsonSourceDefinition> {
        return BUILTIN_SOURCES
    }

    fun findById(sourceId: String): JsonSourceDefinition? {
        val cleanId = sourceId.removePrefix("ms_").removePrefix("MS_").trim().lowercase()
        return BUILTIN_SOURCES.firstOrNull { 
            it.id.equals(cleanId, ignoreCase = true) ||
            it.id.equals(sourceId, ignoreCase = true) ||
            sourceId.equals("ms_${it.id}", ignoreCase = true)
        }
    }

    fun isMangaSourceId(sourceId: String): Boolean {
        return findById(sourceId) != null
    }
}
