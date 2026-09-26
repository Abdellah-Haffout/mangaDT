package com.abht.manga_dt.data

import androidx.compose.runtime.mutableStateMapOf
import com.abht.manga_dt.models.Chapter
import com.abht.manga_dt.models.Manga
import com.abht.manga_dt.models.MangaStatus

object OfflineMangaManager {
    private const val REGISTRY_KEY = "mangadt_offline_manga_registry_v1"
    private const val MANGA_PREFIX = "offline_manga_"

    // In-memory cache backed by persistent disk storage
    val cachedMangaMap = mutableStateMapOf<String, Manga>()
    private val savedKeys = mutableSetOf<String>()

    init {
        loadRegistry()
    }

    private fun getStorageKey(sourceId: String, mangaUrl: String, title: String = ""): String {
        val cleanSrc = sourceId.trim().lowercase()
        val cleanUrl = mangaUrl.trim().ifBlank { title.trim().lowercase() }
        return "$cleanSrc::$cleanUrl"
    }

    private fun loadRegistry() {
        try {
            val registryRaw = SettingsStorage.getString(REGISTRY_KEY, "")
            if (registryRaw.isNotBlank()) {
                val keys = registryRaw.split(";;;;;").filter { it.isNotBlank() }
                savedKeys.addAll(keys)
                for (key in keys) {
                    val raw = SettingsStorage.getString("$MANGA_PREFIX$key", "")
                    if (raw.isNotBlank()) {
                        val manga = deserializeManga(raw)
                        if (manga != null) {
                            cachedMangaMap[key] = manga
                            // Also mirror into MangaDataCache for seamless compatibility
                            MangaDataCache.cachedMangaDetails[key] = manga
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveRegistry() {
        try {
            val registryRaw = savedKeys.joinToString(";;;;;")
            SettingsStorage.setString(REGISTRY_KEY, registryRaw)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Persistently saves manga metadata and its chapter list for offline use.
     */
    fun saveManga(manga: Manga) {
        if (manga.title.isBlank()) return
        val key = getStorageKey(manga.source, manga.url, manga.title)
        
        // Merge with existing chapters if the new manga has fewer chapters (e.g. partial load)
        val existing = cachedMangaMap[key]
        val mergedChapters = if (existing != null && manga.chapters.isEmpty() && existing.chapters.isNotEmpty()) {
            existing.chapters
        } else if (existing != null && existing.chapters.isNotEmpty() && manga.chapters.isNotEmpty()) {
            val newUrls = manga.chapters.map { it.url }.toSet()
            manga.chapters + existing.chapters.filterNot { it.url in newUrls }
        } else {
            manga.chapters
        }

        val toSave = manga.copy(
            chapters = mergedChapters,
            thumbnailUrl = manga.thumbnailUrl.ifBlank { existing?.thumbnailUrl.orEmpty() },
            description = manga.description?.ifBlank { existing?.description } ?: existing?.description,
            author = manga.author?.ifBlank { existing?.author } ?: existing?.author
        )

        cachedMangaMap[key] = toSave
        MangaDataCache.cachedMangaDetails[key] = toSave
        savedKeys.add(key)

        try {
            val serialized = serializeManga(toSave)
            SettingsStorage.setString("$MANGA_PREFIX$key", serialized)
            saveRegistry()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Look up offline manga by source ID and manga URL (or title fallback).
     */
    fun getManga(sourceId: String, mangaUrl: String, title: String = ""): Manga? {
        val key = getStorageKey(sourceId, mangaUrl, title)
        val inMap = cachedMangaMap[key]
        if (inMap != null) return inMap

        // Try title match fallback
        if (title.isNotBlank()) {
            val byTitle = cachedMangaMap.values.firstOrNull {
                it.title.equals(title, ignoreCase = true) || (it.source.equals(sourceId, ignoreCase = true) && it.title.equals(title, ignoreCase = true))
            }
            if (byTitle != null) return byTitle
        }

        // Try load from SettingsStorage directly
        val raw = SettingsStorage.getString("$MANGA_PREFIX$key", "")
        if (raw.isNotBlank()) {
            val loaded = deserializeManga(raw)
            if (loaded != null) {
                cachedMangaMap[key] = loaded
                MangaDataCache.cachedMangaDetails[key] = loaded
                return loaded
            }
        }
        return null
    }

    fun getAllOfflineManga(): List<Manga> {
        return cachedMangaMap.values.toList()
    }

    // --- Serialization Helpers ---

    private fun serializeManga(m: Manga): String {
        // Delimiters:
        // Section: ~~~MANGA_SEC~~~
        // Fields: |||||
        // Tags: ,,,,,
        // Chapter Fields: ^^^^^
        // Chapters: ;;;;;
        val fields = listOf(
            m.id,
            m.title,
            m.thumbnailUrl,
            m.author.orEmpty(),
            m.description.orEmpty(),
            m.status.name,
            m.source,
            m.url,
            m.altTitle.orEmpty(),
            m.rating.toString(),
            m.isNsfw.toString(),
            m.tags.joinToString(",,,,,"),
            m.publicUrl,
            m.latestChapter.orEmpty(),
            m.updatedAt.orEmpty(),
            m.views.orEmpty(),
            m.releaseYear.orEmpty(),
            m.mangaType.orEmpty(),
            m.ratingCount?.toString().orEmpty()
        ).joinToString("|||||")

        val chaptersSerialized = m.chapters.joinToString(";;;;;") { ch ->
            listOf(
                ch.id,
                ch.mangaId,
                ch.title,
                ch.chapterNumber.toString(),
                ch.uploadDate?.toString().orEmpty(),
                ch.scanlator.orEmpty(),
                ch.url
            ).joinToString("^^^^^")
        }

        return "$fields~~~MANGA_SEC~~~$chaptersSerialized"
    }

    private fun deserializeManga(raw: String): Manga? {
        return try {
            val sections = raw.split("~~~MANGA_SEC~~~")
            if (sections.isEmpty()) return null
            val fields = sections[0].split("|||||")
            if (fields.size < 11) return null

            val tagsList = if (fields.size > 11 && fields[11].isNotBlank()) {
                fields[11].split(",,,,,").filter { it.isNotBlank() }
            } else emptyList()

            val chaptersList = if (sections.size > 1 && sections[1].isNotBlank()) {
                sections[1].split(";;;;;").mapNotNull { chStr ->
                    val chParts = chStr.split("^^^^^")
                    if (chParts.size >= 7) {
                        Chapter(
                            id = chParts[0],
                            mangaId = chParts[1],
                            title = chParts[2],
                            chapterNumber = chParts[3].toFloatOrNull() ?: 0f,
                            uploadDate = chParts[4].toLongOrNull(),
                            scanlator = chParts[5].ifBlank { null },
                            url = chParts[6]
                        )
                    } else null
                }
            } else emptyList()

            Manga(
                id = fields[0],
                title = fields[1],
                thumbnailUrl = fields[2],
                author = fields[3].ifBlank { null },
                description = fields[4].ifBlank { null },
                status = runCatching { MangaStatus.valueOf(fields[5]) }.getOrDefault(MangaStatus.UNKNOWN),
                source = fields[6],
                url = fields[7],
                chapters = chaptersList,
                altTitle = fields.getOrNull(8)?.ifBlank { null },
                rating = fields.getOrNull(9)?.toFloatOrNull() ?: 0f,
                isNsfw = fields.getOrNull(10)?.toBooleanStrictOrNull() ?: false,
                tags = tagsList,
                publicUrl = fields.getOrNull(12).orEmpty(),
                latestChapter = fields.getOrNull(13)?.ifBlank { null },
                updatedAt = fields.getOrNull(14)?.ifBlank { null },
                views = fields.getOrNull(15)?.ifBlank { null },
                releaseYear = fields.getOrNull(16)?.ifBlank { null },
                mangaType = fields.getOrNull(17)?.ifBlank { null },
                ratingCount = fields.getOrNull(18)?.toIntOrNull()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
