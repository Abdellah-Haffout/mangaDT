package com.abht.manga_dt.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import coil3.Image
import kotlin.math.abs

expect fun extractDominantColor(image: Image): Color?

fun rgbToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    var h = 0f
    var s = 0f

    if (max != min) {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h *= 60f
    }
    return floatArrayOf(h, s, l)
}

fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
    val hNorm = (h % 360f + 360f) % 360f
    val sClamped = s.coerceIn(0f, 1f)
    val lClamped = l.coerceIn(0f, 1f)

    val c = (1f - abs(2f * lClamped - 1f)) * sClamped
    val x = c * (1f - abs((hNorm / 60f) % 2f - 1f))
    val m = lClamped - c / 2f

    val (rPrime, gPrime, bPrime) = when ((hNorm / 60f).toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = (rPrime + m).coerceIn(0f, 1f),
        green = (gPrime + m).coerceIn(0f, 1f),
        blue = (bPrime + m).coerceIn(0f, 1f),
        alpha = alpha
    )
}

fun generateSeedFromMetadata(title: String, coverUrl: String): Color {
    val key = "$title::$coverUrl"
    var hash = 0
    for (ch in key) {
        hash = (hash * 31 + ch.code) and 0x7FFFFFFF
    }
    val hue = (hash % 360).toFloat()
    val sat = 0.65f + ((hash ushr 8) % 25) / 100f
    val light = 0.50f + ((hash ushr 16) % 15) / 100f
    return hslToColor(hue, sat, light)
}

fun createDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    val (h, s, _) = rgbToHsl(seedColor)
    val vibrantS = s.coerceAtLeast(0.48f)

    return if (isDark) {
        val primary = hslToColor(h, vibrantS.coerceAtLeast(0.68f), 0.76f)
        val onPrimary = Color(0xFF0D0E11)
        val primaryContainer = hslToColor(h, 0.55f, 0.24f)
        val onPrimaryContainer = hslToColor(h, 0.45f, 0.90f)

        val secondary = hslToColor((h + 20f) % 360f, 0.40f, 0.74f)
        val onSecondary = Color(0xFF101114)
        val secondaryContainer = hslToColor((h + 20f) % 360f, 0.38f, 0.22f)
        val onSecondaryContainer = hslToColor((h + 20f) % 360f, 0.35f, 0.90f)

        val tertiary = hslToColor((h + 50f) % 360f, 0.48f, 0.76f)
        val onTertiary = Color(0xFF101114)
        val tertiaryContainer = hslToColor((h + 50f) % 360f, 0.40f, 0.22f)
        val onTertiaryContainer = hslToColor((h + 50f) % 360f, 0.40f, 0.92f)

        val background = if (isAmoled) Color.Black else hslToColor(h, 0.12f, 0.08f)
        val onBackground = Color(0xFFE6E1E5)
        val surface = if (isAmoled) Color.Black else hslToColor(h, 0.12f, 0.10f)
        val onSurface = Color(0xFFE6E1E5)
        val surfaceVariant = hslToColor(h, 0.18f, 0.18f)
        val onSurfaceVariant = hslToColor(h, 0.15f, 0.80f)

        val surfaceContainerLowest = if (isAmoled) Color.Black else hslToColor(h, 0.10f, 0.06f)
        val surfaceContainerLow = if (isAmoled) Color(0xFF080808) else hslToColor(h, 0.12f, 0.11f)
        val surfaceContainer = if (isAmoled) Color(0xFF101010) else hslToColor(h, 0.14f, 0.14f)
        val surfaceContainerHigh = if (isAmoled) Color(0xFF181818) else hslToColor(h, 0.16f, 0.17f)
        val surfaceContainerHighest = if (isAmoled) Color(0xFF222222) else hslToColor(h, 0.18f, 0.21f)

        val outline = hslToColor(h, 0.18f, 0.50f)
        val outlineVariant = hslToColor(h, 0.15f, 0.28f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        val primary = hslToColor(h, vibrantS.coerceAtLeast(0.72f), 0.38f)
        val onPrimary = Color.White
        val primaryContainer = hslToColor(h, 0.60f, 0.90f)
        val onPrimaryContainer = hslToColor(h, 0.75f, 0.14f)

        val secondary = hslToColor((h + 20f) % 360f, 0.40f, 0.40f)
        val onSecondary = Color.White
        val secondaryContainer = hslToColor((h + 20f) % 360f, 0.40f, 0.88f)
        val onSecondaryContainer = hslToColor((h + 20f) % 360f, 0.55f, 0.14f)

        val tertiary = hslToColor((h + 50f) % 360f, 0.50f, 0.42f)
        val onTertiary = Color.White
        val tertiaryContainer = hslToColor((h + 50f) % 360f, 0.45f, 0.90f)
        val onTertiaryContainer = hslToColor((h + 50f) % 360f, 0.60f, 0.14f)

        val background = hslToColor(h, 0.15f, 0.98f)
        val onBackground = Color(0xFF1B1B1F)
        val surface = hslToColor(h, 0.15f, 0.98f)
        val onSurface = Color(0xFF1B1B1F)
        val surfaceVariant = hslToColor(h, 0.18f, 0.90f)
        val onSurfaceVariant = hslToColor(h, 0.20f, 0.30f)

        val surfaceContainerLowest = Color.White
        val surfaceContainerLow = hslToColor(h, 0.12f, 0.96f)
        val surfaceContainer = hslToColor(h, 0.15f, 0.94f)
        val surfaceContainerHigh = hslToColor(h, 0.18f, 0.91f)
        val surfaceContainerHighest = hslToColor(h, 0.20f, 0.87f)

        val outline = hslToColor(h, 0.18f, 0.55f)
        val outlineVariant = hslToColor(h, 0.15f, 0.78f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }
}

@Composable
fun rememberAnimatedDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    val targetScheme = remember(seedColor, isDark, isAmoled) {
        createDynamicColorScheme(seedColor, isDark, isAmoled)
    }

    val primary = animateColorAsState(targetScheme.primary, tween(350)).value
    val primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(350)).value
    val onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(350)).value
    val secondary = animateColorAsState(targetScheme.secondary, tween(350)).value
    val secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(350)).value
    val onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(350)).value
    val tertiary = animateColorAsState(targetScheme.tertiary, tween(350)).value
    val tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, tween(350)).value
    val onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, tween(350)).value
    val background = animateColorAsState(targetScheme.background, tween(350)).value
    val surface = animateColorAsState(targetScheme.surface, tween(350)).value
    val surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, tween(350)).value
    val onSurface = animateColorAsState(targetScheme.onSurface, tween(350)).value
    val onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(350)).value
    val surfaceContainer = animateColorAsState(targetScheme.surfaceContainer, tween(350)).value
    val surfaceContainerLow = animateColorAsState(targetScheme.surfaceContainerLow, tween(350)).value
    val surfaceContainerHigh = animateColorAsState(targetScheme.surfaceContainerHigh, tween(350)).value
    val outline = animateColorAsState(targetScheme.outline, tween(350)).value
    val outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(350)).value

    return targetScheme.copy(
        primary = primary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = surfaceContainer,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerHigh = surfaceContainerHigh,
        outline = outline,
        outlineVariant = outlineVariant
    )
}
