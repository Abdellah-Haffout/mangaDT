package com.abht.manga_dt.data.mangasource

import com.abht.manga_dt.data.MangaSource

data class RegexExtractor(
    val pattern: String = "",
    val idGroup: Int = 1,
    val titleGroup: Int? = null,
    val coverGroup: Int? = null,
    val altTitleGroup: Int? = null,
    val authorGroup: Int? = null,
    val artistGroup: Int? = null,
    val descriptionGroup: Int? = null,
    val ratingGroup: Int? = null,
    val viewsGroup: Int? = null,
    val statusGroup: Int? = null,
    val latestChapterGroup: Int? = null,
    val updatedAtGroup: Int? = null,

    // Block-level extraction
    val itemPattern: String? = null,
    val idRegex: String? = null,
    val titleRegex: String? = null,
    val coverRegex: String? = null,
    val altTitleRegex: String? = null,
    val authorRegex: String? = null,
    val artistRegex: String? = null,
    val descriptionRegex: String? = null,
    val ratingRegex: String? = null,
    val viewsRegex: String? = null,
    val statusRegex: String? = null,
    val latestChapterRegex: String? = null,
    val updatedAtRegex: String? = null,
    val genresRegex: String? = null,
    val tagsRegex: String? = null,
    val nsfwRegex: String? = null,
    val typeRegex: String? = null,
    val yearRegex: String? = null
)

data class RequestStep(
    val urlTemplate: String,
    val method: String = "GET",
    val regex: RegexExtractor = RegexExtractor()
)

data class JsonSourceDefinition(
    val id: String,
    val name: String,
    val baseUrl: String,
    val languages: List<String> = listOf("en"),
    val isNsfw: Boolean = false,
    val tags: List<String> = emptyList(),
    val iconUrl: String? = null,
    val userAgent: String? = null,
    val engine: String? = null,
    val search: RequestStep,
    val latest: RequestStep? = null,
    val details: RequestStep? = null,
    val chapters: RequestStep,
    val pages: RequestStep
) {
    fun toMangaSource(): MangaSource {
        val domain = baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        val defaultIcon = "https://www.google.com/s2/favicons?sz=64&domain=$domain"
        return MangaSource(
            id = id,
            name = name,
            iconUrl = iconUrl ?: defaultIcon,
            locale = languages.firstOrNull()?.uppercase() ?: "EN",
            contentType = if (isNsfw) "HENTAI" else "MANGA",
            isBroken = false
        )
    }
}
