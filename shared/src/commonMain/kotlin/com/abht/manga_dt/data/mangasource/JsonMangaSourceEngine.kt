package com.abht.manga_dt.data.mangasource

import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus
import com.abht.manga_dt.models.ReaderPage

object JsonMangaSourceEngine {

    private fun dotAllRegex(pattern: String): Regex {
        val p = if (pattern.startsWith("(?s)")) pattern else "(?s)$pattern"
        return Regex(p)
    }


    fun normalizeUrl(raw: String, baseUrl: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "${baseUrl.trimEnd('/')}$trimmed"
            else -> "${baseUrl.trimEnd('/')}/$trimmed"
        }
    }

    fun unescapeHtml(raw: String): String {
        return raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&#8216;", "‘")
            .replace("&#8217;", "’")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace("&#8230;", "…")
            .replace("&nbsp;", " ")
            .trim()
    }

    fun stripHtmlTags(raw: String): String {
        val cleaned = raw.replace(Regex("<[^>]+>"), " ")
        return unescapeHtml(cleaned).replace(Regex("\\s+"), " ").trim()
    }

    fun mapStatus(statusStr: String?): MangaStatus {
        if (statusStr == null) return MangaStatus.UNKNOWN
        val lower = statusStr.lowercase().trim()
        return when {
            lower.contains("ongoing") || lower.contains("مستمر") || lower.contains("publishing") -> MangaStatus.ONGOING
            lower.contains("completed") || lower.contains("مكتمل") || lower.contains("finished") -> MangaStatus.COMPLETED
            lower.contains("dropped") || lower.contains("متوقف") || lower.contains("abandoned") -> MangaStatus.DROPPED
            lower.contains("on hold") || lower.contains("hiatus") || lower.contains("paused") -> MangaStatus.ON_HOLD
            else -> MangaStatus.UNKNOWN
        }
    }

    fun extractMangaListFromHtml(
        html: String,
        step: RequestStep,
        sourceDef: JsonSourceDefinition
    ): List<Manga> {
        val results = mutableListOf<Manga>()
        val seen = mutableSetOf<String>()

        val itemPattern = step.regex.itemPattern
        if (!itemPattern.isNullOrEmpty()) {
            val blocks = if (itemPattern.startsWith("split:")) {
                val delimiter = itemPattern.removePrefix("split:")
                html.split(delimiter).drop(1)
            } else {
                runCatching {
                    dotAllRegex(itemPattern).findAll(html).map { it.value }.toList()
                }.getOrDefault(emptyList())
            }

            for (block in blocks) {
                if (block.isBlank()) continue

                // 1. Extract ID
                val idPattern = step.regex.idRegex ?: """href="https?://[^/]+/manga/([^/]+)/""""
                val id = runCatching {
                    dotAllRegex(idPattern).find(block)?.let { match ->
                        if (match.groupValues.size > 1) match.groupValues[1] else match.groupValues[0]
                    }?.trim()
                }.getOrNull()

                if (id.isNullOrBlank() || !seen.add(id)) continue

                // 2. Extract Title
                val titlePattern = step.regex.titleRegex ?: """<h[1-6][^>]*>\s*<a [^>]*>([^<]+)</a>"""
                val rawTitle = runCatching {
                    dotAllRegex(titlePattern).find(block)?.let { match ->
                        if (match.groupValues.size > 1) match.groupValues[1] else match.groupValues[0]
                    }?.trim()
                }.getOrNull() ?: id
                val title = stripHtmlTags(rawTitle).ifBlank { id }

                // 3. Extract Cover URL
                val coverPattern = step.regex.coverRegex ?: """<img [^>]*(?:src|data-src)="([^"]+)""""
                val rawCover = runCatching {
                    dotAllRegex(coverPattern).find(block)?.groupValues?.getOrNull(1)?.trim()
                }.getOrNull()
                val coverUrl = if (!rawCover.isNullOrBlank()) normalizeUrl(rawCover, sourceDef.baseUrl) else ""

                // 4. Alt Title
                val altTitlePattern = step.regex.altTitleRegex ?: """mg_alternative.*?<div class="summary-content">\s*([^<]+)"""
                val altTitle = runCatching {
                    dotAllRegex(altTitlePattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                }.getOrNull()

                // 5. Author
                val authorPattern = step.regex.authorRegex ?: """mg_author.*?<div class="summary-content">\s*([^<]+)"""
                val author = runCatching {
                    dotAllRegex(authorPattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                }.getOrNull()

                // 6. Rating
                val ratingPattern = step.regex.ratingRegex ?: """class="score font-meta total_votes">([0-9.]+)"""
                val rating = runCatching {
                    dotAllRegex(ratingPattern).find(block)?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f
                }.getOrDefault(0f)

                // 7. Status
                val statusStr = if (!step.regex.statusRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.statusRegex).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else {
                    runCatching {
                        dotAllRegex("""mg_status.*?<div class="summary-content">\s*([^<]+)""").find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                }
                val status = mapStatus(statusStr)

                // 8. Description
                val descPattern = step.regex.descriptionRegex
                val description = if (!descPattern.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(descPattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else null

                // 9. Genres & Tags
                val genresPattern = step.regex.genresRegex ?: """href="https?://[^/]+/manga-genre/[^/]+/\"[^>]*>([^<]+)</a>"""
                val genres = runCatching {
                    dotAllRegex(genresPattern).findAll(block).mapNotNull {
                        it.groupValues.getOrNull(1)?.let { g -> stripHtmlTags(g) }
                    }.filter { it.isNotBlank() }.toList()
                }.getOrDefault(emptyList())

                val tagsPattern = step.regex.tagsRegex ?: """<span class="manga-title-badges[^"]*">\s*(?:<span class="text">)?([^<]+)(?:</span>)?\s*</span>"""
                val tags = runCatching {
                    dotAllRegex(tagsPattern).findAll(block).mapNotNull {
                        it.groupValues.getOrNull(1)?.let { t -> stripHtmlTags(t) }
                    }.filter { it.isNotBlank() }.toList()
                }.getOrDefault(emptyList())

                val allTags = (genres + tags + sourceDef.tags).distinct()
                val isNsfw = sourceDef.isNsfw || allTags.any { t ->
                    val low = t.lowercase()
                    low.contains("18+") || low.contains("adult") || low.contains("hentai") || low.contains("ecchi")
                } || block.contains("18+")

                // 10. Latest Chapter
                val latestChapter = if (!step.regex.latestChapterRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.latestChapterRegex).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else {
                    val latestChPattern = """(?:class="[^"]*(?:font-meta chapter|chapter-item|latest-chap|version-chap)[^"]*"[^>]*>|href="[^"]*chapter[^"]*"[^>]*>)\s*([^<]+)"""
                    runCatching {
                        dotAllRegex(latestChPattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                }

                // 11. Updated Time & Views
                val updatedAt = if (!step.regex.updatedAtRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.updatedAtRegex).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else {
                    val updatedPattern = """(?:class="[^"]*(?:post-on font-meta|chapter-release-date|timediff)[^"]*"[^>]*>)\s*([^<]+)"""
                    runCatching {
                        dotAllRegex(updatedPattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                }

                val views = if (!step.regex.viewsRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.viewsRegex).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else {
                    val viewsPattern = """(?:class="[^"]*(?:views|post-views)[^"]*"[^>]*>)\s*([^<]+)"""
                    runCatching {
                        dotAllRegex(viewsPattern).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                }

                val mangaType = if (!step.regex.typeRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.typeRegex).find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                    }.getOrNull()
                } else {
                    allTags.firstOrNull { t -> 
                        val l = t.lowercase()
                        l == "manhwa" || l == "manga" || l == "manhua" || l == "webtoon" || l == "comic" || l == "مانجا" || l == "مانهوا" || l == "مانها" || l == "ويب تون"
                    }
                }

                val releaseYear = if (!step.regex.yearRegex.isNullOrBlank()) {
                    runCatching {
                        dotAllRegex(step.regex.yearRegex).find(block)?.groupValues?.getOrNull(1)?.trim()
                    }.getOrNull()
                } else null

                val mangaUrl = if (id.startsWith("http://") || id.startsWith("https://")) {
                    id
                } else {
                    "${sourceDef.baseUrl.trimEnd('/')}/manga/$id/"
                }

                results.add(
                    Manga(
                        id = id,
                        title = title,
                        thumbnailUrl = coverUrl,
                        author = author,
                        description = description,
                        status = status,
                        source = sourceDef.id,
                        url = mangaUrl,
                        altTitle = altTitle,
                        rating = rating,
                        isNsfw = isNsfw,
                        tags = allTags,
                        publicUrl = mangaUrl,
                        latestChapter = latestChapter,
                        updatedAt = updatedAt,
                        views = views,
                        mangaType = mangaType
                    )
                )
            }
        } else if (step.regex.pattern.isNotBlank()) {
            val re = runCatching { dotAllRegex(step.regex.pattern) }.getOrNull()
            if (re != null) {
                for (match in re.findAll(html)) {
                    val id = match.groupValues.getOrNull(step.regex.idGroup)?.trim().orEmpty()
                    if (id.isBlank() || !seen.add(id)) continue

                    val rawTitle = step.regex.titleGroup?.let { match.groupValues.getOrNull(it) }?.trim() ?: id
                    val title = stripHtmlTags(rawTitle).ifBlank { id }

                    val rawCover = step.regex.coverGroup?.let { match.groupValues.getOrNull(it) }?.trim().orEmpty()
                    val coverUrl = if (rawCover.isNotBlank()) normalizeUrl(rawCover, sourceDef.baseUrl) else ""

                    val mangaUrl = if (id.startsWith("http://") || id.startsWith("https://")) {
                        id
                    } else if (id.startsWith("/")) {
                        "${sourceDef.baseUrl.trimEnd('/')}$id"
                    } else {
                        "${sourceDef.baseUrl.trimEnd('/')}/manga/$id"
                    }

                    results.add(
                        Manga(
                            id = id,
                            title = title,
                            thumbnailUrl = coverUrl,
                            author = step.regex.authorGroup?.let { match.groupValues.getOrNull(it)?.let { a -> stripHtmlTags(a) } },
                            description = step.regex.descriptionGroup?.let { match.groupValues.getOrNull(it)?.let { d -> stripHtmlTags(d) } },
                            status = mapStatus(step.regex.statusGroup?.let { match.groupValues.getOrNull(it) }),
                            source = sourceDef.id,
                            url = mangaUrl,
                            altTitle = step.regex.altTitleGroup?.let { match.groupValues.getOrNull(it)?.let { at -> stripHtmlTags(at) } },
                            rating = step.regex.ratingGroup?.let { match.groupValues.getOrNull(it)?.toFloatOrNull() } ?: 0f,
                            isNsfw = sourceDef.isNsfw,
                            tags = sourceDef.tags,
                            publicUrl = mangaUrl
                        )
                    )
                }
            }
        }

        return results
    }

    fun extractMangaDetailsFromHtml(
        html: String,
        mangaIdOrUrl: String,
        sourceDef: JsonSourceDefinition
    ): Manga {
        val cleanHtml = html
            .replace(Regex("(?s)<style.*?</style>"), "")
            .replace(Regex("(?s)<script.*?</script>"), "")

        fun helperField(headings: List<String>): String? {
            for (h in headings) {
                val escaped = Regex.escape(h)
                val pattern = """(?s)<h5>\s*$escaped\s*</h5>[\s\S]*?<div class="summary-content[^"]*">\s*([\s\S]*?)</div>"""
                val found = runCatching {
                    Regex(pattern).find(cleanHtml)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
                }.getOrNull()
                if (!found.isNullOrBlank() && found != "-") return found
            }
            return null
        }

        // Title
        val titleMatch = Regex("""(?s)<div class="post-title[^"]*">\s*(?:<span[^>]*>.*?</span>\s*)*<h1[^>]*>\s*([^<]+)""").find(cleanHtml)
            ?: Regex("""<h1[^>]*>\s*([^<]+)""").find(cleanHtml)
        val title = titleMatch?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }?.ifBlank { null }
            ?: mangaIdOrUrl.substringAfterLast('/').ifBlank { mangaIdOrUrl }

        // Cover
        val coverMatch = Regex("""(?s)<div class="summary_image"[^>]*>[\s\S]*?<img [^>]*(?:src|data-src)="([^"]+)"""").find(cleanHtml)
            ?: Regex("""<img [^>]*(?:src|data-src)="([^"]+)"[^>]*class="[^"]*(?:cover|poster|thumb)[^"]*"""").find(cleanHtml)
            ?: Regex("""<img [^>]*(?:src|data-src)="([^"]+)"""").find(cleanHtml)
        val rawCover = coverMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val coverUrl = if (rawCover.isNotBlank()) normalizeUrl(rawCover, sourceDef.baseUrl) else ""

        // Rating
        val ratingMatch = Regex("""class="score font-meta total_votes">([0-9.]+)""").find(cleanHtml)
            ?: Regex("""id="averagerate">\s*([0-9.]+)""").find(cleanHtml)
        val rating = ratingMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0f

        // Alternative Title
        val altTitle = helperField(listOf("أسماء أخرى", "Alternative", "Alternative Titles", "Other Names"))

        // Author & Artist
        val author = helperField(listOf("الكاتب", "المؤلف", "Author", "Authors"))
        val artist = helperField(listOf("الرسام", "Artist", "Artists"))

        // Status
        val statusStr = helperField(listOf("الحالة", "Status"))
        val status = mapStatus(statusStr)

        // Genres
        val genres = runCatching {
            Regex("""href="https?://[^/]+/manga-genre/[^/]+/\"[^>]*>([^<]+)</a>""").findAll(cleanHtml).mapNotNull {
                it.groupValues.getOrNull(1)?.let { g -> stripHtmlTags(g) }
            }.filter { it.isNotBlank() }.toList()
        }.getOrDefault(emptyList())

        // Badges / Tags
        val tags = runCatching {
            Regex("""<span class="manga-title-badges[^"]*">\s*(?:<span class="text">)?([^<]+)(?:</span>)?\s*</span>""").findAll(cleanHtml).mapNotNull {
                it.groupValues.getOrNull(1)?.let { t -> stripHtmlTags(t) }
            }.filter { it.isNotBlank() }.toList()
        }.getOrDefault(emptyList())

        val allTags = (genres + tags + sourceDef.tags).distinct()
        val isNsfw = sourceDef.isNsfw || allTags.any { t ->
            val low = t.lowercase()
            low.contains("18+") || low.contains("adult") || low.contains("hentai") || low.contains("mature")
        }

        // Description
        val desc1 = Regex("""(?s)<div class="manga-excerpt[^"]*">\s*<p>\s*([\s\S]*?)</p>""").find(cleanHtml)
        val desc2 = Regex("""(?s)<div class="description-summary[^"]*">[\s\S]*?<div class="summary__content[^"]*">\s*<p>\s*([\s\S]*?)</p>""").find(cleanHtml)
        val desc3 = Regex("""(?s)<div class="description-summary[^"]*">[\s\S]*?<div class="summary__content[^"]*">\s*([\s\S]*?)</div>""").find(cleanHtml)
        val desc4 = Regex("""(?s)<p class="text-secondary[^"]*">([\s\S]*?)</p>""").find(cleanHtml)
        val rawDesc = (desc1 ?: desc2 ?: desc3 ?: desc4)?.groupValues?.getOrNull(1)
        val description = rawDesc?.let { stripHtmlTags(it) }?.ifBlank { null }

        // Views, Release Year, Type
        val views = helperField(listOf("المشاهدات", "Views", "Total Views"))
        val releaseYear = helperField(listOf("سنة الإصدار", "تاريخ الإصدار", "Release", "Year", "Published"))
        val mangaType = helperField(listOf("النوع", "Type", "Format")) ?: allTags.firstOrNull { t -> 
            val l = t.lowercase()
            l == "manhwa" || l == "manga" || l == "manhua" || l == "webtoon" || l == "comic" || l == "مانجا" || l == "مانهوا" || l == "مانها" || l == "ويب تون"
        }

        val cleanUrl = normalizeUrl(mangaIdOrUrl, sourceDef.baseUrl)

        return Manga(
            id = mangaIdOrUrl,
            title = title,
            thumbnailUrl = coverUrl,
            author = if (!author.isNullOrBlank()) author else artist,
            description = description,
            status = status,
            source = sourceDef.id,
            url = cleanUrl,
            altTitle = altTitle,
            rating = rating,
            isNsfw = isNsfw,
            tags = allTags,
            publicUrl = cleanUrl,
            views = views,
            releaseYear = releaseYear,
            mangaType = mangaType
        )
    }

    fun extractChaptersFromHtml(
        html: String,
        mangaId: String,
        step: RequestStep,
        sourceDef: JsonSourceDefinition
    ): List<Chapter> {
        val cleanMangaId = mangaId
            .removePrefix(sourceDef.baseUrl)
            .removePrefix("/manga/")
            .removePrefix("manga/")
            .removePrefix("/")
            .removeSuffix("/")
            .trim()

        val pattern = step.regex.pattern.replace("{manga_id}", Regex.escape(cleanMangaId))
        val re = runCatching { dotAllRegex(pattern) }.getOrNull()
            ?: runCatching { Regex("""href="([^"]+)""[^>]*>([\s\S]*?)</a>""") }.getOrNull()

        if (re == null) return emptyList()

        val chapters = mutableListOf<Chapter>()
        val seen = mutableSetOf<String>()

        for (match in re.findAll(html)) {
            val chId = match.groupValues.getOrNull(step.regex.idGroup)?.trim().orEmpty()
            if (chId.isBlank() || !seen.add(chId)) continue

            val rawTitle = step.regex.titleGroup?.let { match.groupValues.getOrNull(it) }?.trim()
            val cleanTitle = rawTitle?.let { stripHtmlTags(it) }?.ifBlank { null }

            // Extract chapter number
            val numStr = Regex("""([0-9]+(?:\.[0-9]+)?)""").find(cleanTitle ?: chId)?.groupValues?.getOrNull(1)
            val chapterNum = numStr?.toFloatOrNull() ?: chId.split('-').lastOrNull()?.toFloatOrNull() ?: (chapters.size + 1).toFloat()

            val title = cleanTitle ?: "Chapter $chapterNum"

            // Look for href attribute in matched text if available
            val hrefInMatch = Regex("""href="([^"]+)"""").find(match.value)?.groupValues?.getOrNull(1)?.trim()

            val chapterUrl = when {
                !hrefInMatch.isNullOrBlank() -> normalizeUrl(hrefInMatch, sourceDef.baseUrl)
                chId.startsWith("http://") || chId.startsWith("https://") -> chId
                chId.startsWith("/") -> normalizeUrl(chId, sourceDef.baseUrl)
                sourceDef.pages.urlTemplate.contains("{chapter_id}") -> {
                    sourceDef.pages.urlTemplate
                        .replace("{base_url}", sourceDef.baseUrl.trimEnd('/'))
                        .replace("{chapter_id}", chId)
                }
                else -> "${sourceDef.baseUrl.trimEnd('/')}/manga/$cleanMangaId/$chId/"
            }

            chapters.add(
                Chapter(
                    id = chId,
                    mangaId = mangaId,
                    title = title,
                    chapterNumber = chapterNum,
                    url = chapterUrl
                )
            )
        }

        return chapters
    }

    fun extractPagesFromHtml(
        html: String,
        step: RequestStep,
        sourceDef: JsonSourceDefinition
    ): List<ReaderPage> {
        val pages = mutableListOf<ReaderPage>()
        val seen = mutableSetOf<String>()

        // 1. Try step regex pattern
        if (step.regex.pattern.isNotBlank()) {
            val re = runCatching { dotAllRegex(step.regex.pattern) }.getOrNull()
            if (re != null) {
                for (match in re.findAll(html)) {
                    val rawUrl = match.groupValues.getOrNull(step.regex.idGroup)?.trim().orEmpty()
                    if (rawUrl.isBlank()) continue

                    val pageUrl = normalizeUrl(rawUrl, sourceDef.baseUrl)
                    if (pageUrl.isBlank() || !seen.add(pageUrl)) continue

                    val lower = pageUrl.lowercase()
                    if (lower.contains("logo") || lower.contains("avatar") || lower.contains("favicon") || lower.contains("banner") || lower.endsWith(".gif")) {
                        continue
                    }

                    pages.add(ReaderPage(url = pageUrl, pageNumber = pages.size + 1))
                }
            }
        }

        // 2. If nothing found or fallback, scan for common reader image patterns
        if (pages.isEmpty()) {
            val fallbackPatterns = listOf(
                """<img [^>]*(?:src|data-src|data-url)="([^"]+)"[^>]*class="[^"]*(?:wp-manga-chapter-img|js-page|page-image)[^"]*"""",
                """<img [^>]*class="[^"]*(?:wp-manga-chapter-img|js-page|page-image)[^"]*"[^>]*(?:src|data-src|data-url)="([^"]+)"""",
                """data-src="([^"]+)"""",
                """data-url="([^"]+)""""
            )

            for (pattern in fallbackPatterns) {
                val re = runCatching { dotAllRegex(pattern) }.getOrNull() ?: continue
                for (match in re.findAll(html)) {
                    val rawUrl = match.groupValues.getOrNull(1)?.trim().orEmpty()
                    if (rawUrl.isBlank()) continue

                    val pageUrl = normalizeUrl(rawUrl, sourceDef.baseUrl)
                    if (pageUrl.isBlank() || !seen.add(pageUrl)) continue

                    val lower = pageUrl.lowercase()
                    if (lower.contains("logo") || lower.contains("avatar") || lower.contains("favicon") || lower.contains("banner") || lower.endsWith(".gif")) {
                        continue
                    }

                    pages.add(ReaderPage(url = pageUrl, pageNumber = pages.size + 1))
                }
                if (pages.isNotEmpty()) break
            }
        }

        return pages
    }
}
