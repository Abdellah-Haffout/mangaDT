package com.abht.manga_dt.ui.theme

import androidx.compose.ui.graphics.Color
import coil3.Image
import kotlin.math.abs

actual fun extractDominantColor(image: Image): Color? {
    return runCatching {
        val bitmapImage = image as? coil3.BitmapImage ?: return null
        val bitmap = bitmapImage.bitmap
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val stepX = (width / 24).coerceAtLeast(1)
        val stepY = (height / 24).coerceAtLeast(1)

        var maxScore = -1f
        var bestColor: Color? = null

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val argb = bitmap.getColor(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF

                val color = Color(r / 255f, g / 255f, b / 255f)
                val (_, s, l) = rgbToHsl(color)

                if (l in 0.15f..0.85f && s > 0.15f) {
                    val score = s * (1f - abs(l - 0.5f))
                    if (score > maxScore) {
                        maxScore = score
                        bestColor = color
                    }
                }
            }
        }
        bestColor
    }.getOrNull()
}
