package com.abht.manga_dt.data

import java.util.Locale

actual fun isSystemLanguageArabic(): Boolean {
    return try {
        Locale.getDefault().language.startsWith("ar", ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}
