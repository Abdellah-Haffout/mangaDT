package com.abht.manga_dt.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.Color
import coil3.BitmapImage
import coil3.DrawableImage
import coil3.Image

actual fun extractDominantColor(image: Image): Color? {
    return runCatching {
        var isTempCopy = false
        val softwareBitmap: Bitmap = when (image) {
            is BitmapImage -> {
                val raw = image.bitmap
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
                    isTempCopy = true
                    raw.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                } else {
                    raw
                }
            }
            is DrawableImage -> {
                val d = image.drawable
                if (d is BitmapDrawable) {
                    val raw = d.bitmap
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
                        isTempCopy = true
                        raw.copy(Bitmap.Config.ARGB_8888, false) ?: return null
                    } else {
                        raw
                    }
                } else {
                    isTempCopy = true
                    val w = d.intrinsicWidth.takeIf { it > 0 } ?: 64
                    val h = d.intrinsicHeight.takeIf { it > 0 } ?: 64
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    d.setBounds(0, 0, w, h)
                    d.draw(canvas)
                    bmp
                }
            }
            else -> {
                // Fallback for any other custom Coil Image type
                runCatching {
                    val method = image.javaClass.methods.firstOrNull { it.name == "getBitmap" || it.name == "getDrawable" }
                    val obj = method?.invoke(image)
                    when (obj) {
                        is Bitmap -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && obj.config == Bitmap.Config.HARDWARE) {
                                isTempCopy = true
                                obj.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                obj
                            }
                        }
                        is BitmapDrawable -> {
                            val raw = obj.bitmap
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
                                isTempCopy = true
                                raw.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                raw
                            }
                        }
                        is Drawable -> {
                            isTempCopy = true
                            val w = obj.intrinsicWidth.takeIf { it > 0 } ?: 64
                            val h = obj.intrinsicHeight.takeIf { it > 0 } ?: 64
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            obj.setBounds(0, 0, w, h)
                            obj.draw(canvas)
                            bmp
                        }
                        else -> null
                    }
                }.getOrNull() ?: return null
            }
        }

        val width = softwareBitmap.width
        val height = softwareBitmap.height
        if (width <= 0 || height <= 0) {
            if (isTempCopy) softwareBitmap.recycle()
            return null
        }

        // Sample pixels across the entire image
        val stepX = (width / 64).coerceAtLeast(1)
        val stepY = (height / 64).coerceAtLeast(1)

        // 4-bit per channel color quantization (16x16x16 = 4096 bins)
        val numBins = 4096
        val counts = IntArray(numBins)
        val sumR = LongArray(numBins)
        val sumG = LongArray(numBins)
        val sumB = LongArray(numBins)

        for (x in 0 until width step stepX) {
            for (y in 0 until height step stepY) {
                val pixel = softwareBitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                // Quantize each channel to 4 bits (0..15)
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

        if (isTempCopy) {
            softwareBitmap.recycle()
        }

        // Find the color bucket with the largest share (majority of pixels)
        // First preference: non-extreme pixels (ignoring pure black margins < 16 or pure white borders > 242)
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
