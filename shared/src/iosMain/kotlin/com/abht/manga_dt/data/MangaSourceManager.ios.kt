package com.abht.manga_dt.data

import com.abht.manga_dt.models.Manga

actual class MangaSourceManager actual constructor() {
    actual suspend fun getAvailableSources(): List<MangaSource> = emptyList()
    actual suspend fun searchManga(sourceId: String, query: String): List<Manga> = emptyList()
    actual suspend fun getPopularManga(sourceId: String, page: Int): List<Manga> = emptyList()
    actual suspend fun getMangaDetails(sourceId: String, mangaUrl: String): Manga? = null
    actual suspend fun getPages(sourceId: String, chapterUrl: String): List<com.abht.manga_dt.models.ReaderPage> = emptyList()
    actual suspend fun testSource(sourceId: String): String? = null
}

actual fun currentTimeMillis(): Long = 0L

actual object SettingsStorage {
    private val inMemory = mutableMapOf<String, String>()

    actual fun getString(key: String, defaultValue: String): String = inMemory.getOrDefault(key, defaultValue)
    actual fun setString(key: String, value: String) { inMemory[key] = value }
    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean = inMemory[key]?.toBooleanStrictOrNull() ?: defaultValue
    actual fun setBoolean(key: String, value: Boolean) { inMemory[key] = value.toString() }
    actual fun getInt(key: String, defaultValue: Int): Int = inMemory[key]?.toIntOrNull() ?: defaultValue
    actual fun setInt(key: String, value: Int) { inMemory[key] = value.toString() }
    actual fun getStringSet(key: String): Set<String> {
        val raw = getString(key, "")
        if (raw.isBlank()) return emptySet()
        return raw.split("|||").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    actual fun setStringSet(key: String, values: Set<String>) {
        setString(key, values.joinToString("|||"))
    }
}
