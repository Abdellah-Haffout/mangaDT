package com.abht.manga_dt.ui.theme

import androidx.compose.ui.graphics.Color
import coil3.Image

actual fun extractDominantColor(image: Image): Color? {
    return runCatching {
        val bitmapImage = image as? coil3.BitmapImage ?: return null
        val bitmap = bitmapImage.bitmap
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val stepX = (width / 64).coerceAtLeast(1)
        val stepY = (height / 64).coerceAtLeast(1)

        val numBins = 4096
        val counts = IntArray(numBins)
        val sumR = LongArray(numBins)
        val sumG = LongArray(numBins)
        val sumB = LongArray(numBins)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val argb = bitmap.getColor(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = (argb ushr 16) and 0xFF
                val g = (argb ushr 8) and 0xFF
                val b = argb and 0xFF

                val rBin = r ushr 4
                val gBin = g ushr 4
                val bBin = b ushr 4
                val binIndex = (rBin shl 8) or (gBin shl 4) or bBin

                counts[binIndex]++
                sumR[binIndex] += r
                sumG[binIndex] += g
                sumB[binIndex] += b
            }
        }

        var maxCount = 0
        var dominantBin = -1

        var maxCountAll = 0
        var dominantBinAll = -1

        for (i in 0 until numBins) {
            val count = counts[i]
            if (count > maxCountAll) {
                maxCountAll = count
                dominantBinAll = i
            }

            if (count > 0) {
                val r = (sumR[i] / count).toInt()
                val g = (sumG[i] / count).toInt()
                val b = (sumB[i] / count).toInt()
                val isExtremeBlack = r < 16 && g < 16 && b < 16
                val isExtremeWhite = r > 242 && g > 242 && b > 242

                if (!isExtremeBlack && !isExtremeWhite && count > maxCount) {
                    maxCount = count
                    dominantBin = i
                }
            }
        }

        val bestBin = if (dominantBin != -1) dominantBin else dominantBinAll
        if (bestBin != -1 && counts[bestBin] > 0) {
            val count = counts[bestBin].toFloat()
            val finalR = (sumR[bestBin] / count) / 255f
            val finalG = (sumG[bestBin] / count) / 255f
            val finalB = (sumB[bestBin] / count) / 255f
            Color(finalR, finalG, finalB)
        } else {
            null
        }
    }.getOrNull()
}
