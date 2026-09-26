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

fun createPresetColorScheme(
    preset: com.abht.manga_dt.data.ThemePreset,
    isDark: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    return when (preset) {
        com.abht.manga_dt.data.ThemePreset.NOGUCHI_GOLD -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFCCD4A),
                    onPrimary = Color(0xFF252525),
                    primaryContainer = Color(0xFF4C3C10),
                    onPrimaryContainer = Color(0xFFFFEEB5),
                    secondary = Color(0xFFE54D42),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF6B1814),
                    onSecondaryContainer = Color(0xFFFFDAD6),
                    tertiary = Color(0xFFFFF8E4),
                    onTertiary = Color(0xFF252525),
                    tertiaryContainer = Color(0xFF4A4535),
                    onTertiaryContainer = Color(0xFFFFF8E4),
                    background = if (isAmoled) Color.Black else Color(0xFF1E1D1B),
                    onBackground = Color(0xFFEFEBE4),
                    surface = if (isAmoled) Color.Black else Color(0xFF252525),
                    onSurface = Color(0xFFEFEBE4),
                    surfaceVariant = Color(0xFF383633),
                    onSurfaceVariant = Color(0xFFD4CDC0),
                    surfaceContainerLowest = if (isAmoled) Color.Black else Color(0xFF181716),
                    surfaceContainerLow = if (isAmoled) Color(0xFF0A0A0A) else Color(0xFF201F1D),
                    surfaceContainer = if (isAmoled) Color(0xFF141414) else Color(0xFF2A2927),
                    surfaceContainerHigh = if (isAmoled) Color(0xFF1E1E1E) else Color(0xFF333230),
                    surfaceContainerHighest = if (isAmoled) Color(0xFF2A2A2A) else Color(0xFF3F3E3B),
                    outline = Color(0xFFA39A8B),
                    outlineVariant = Color(0xFF534E45)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7A5800),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFCCD4A),
                    onPrimaryContainer = Color(0xFF261900),
                    secondary = Color(0xFFB3261E),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFDAD6),
                    onSecondaryContainer = Color(0xFF410002),
                    tertiary = Color(0xFF665E40),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFEFE6BE),
                    onTertiaryContainer = Color(0xFF211B04),
                    background = Color(0xFFFFFDF8),
                    onBackground = Color(0xFF1E1D1B),
                    surface = Color(0xFFFFF8E4),
                    onSurface = Color(0xFF1E1D1B),
                    surfaceVariant = Color(0xFFEFE5CD),
                    onSurfaceVariant = Color(0xFF4E4633),
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFFFF6DD),
                    surfaceContainer = Color(0xFFF7EED2),
                    surfaceContainerHigh = Color(0xFFEFE4C4),
                    surfaceContainerHighest = Color(0xFFE5D9B6),
                    outline = Color(0xFF817762),
                    outlineVariant = Color(0xFFD2C6AB)
                )
            }
        }

        com.abht.manga_dt.data.ThemePreset.CYBER_VOLT -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFD0FF00),
                    onPrimary = Color(0xFF0B0C0E),
                    primaryContainer = Color(0xFF3B4800),
                    onPrimaryContainer = Color(0xFFE4FF68),
                    secondary = Color(0xFFA855F7),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF4E008C),
                    onSecondaryContainer = Color(0xFFEEDBFF),
                    tertiary = Color(0xFFFEFFFC),
                    onTertiary = Color(0xFF0B0C0E),
                    tertiaryContainer = Color(0xFF33353D),
                    onTertiaryContainer = Color(0xFFFEFFFC),
                    background = if (isAmoled) Color.Black else Color(0xFF0B0C0E),
                    onBackground = Color(0xFFEDEFEA),
                    surface = if (isAmoled) Color.Black else Color(0xFF121418),
                    onSurface = Color(0xFFEDEFEA),
                    surfaceVariant = Color(0xFF1F232B),
                    onSurfaceVariant = Color(0xFFC7CBD1),
                    surfaceContainerLowest = if (isAmoled) Color.Black else Color(0xFF07080A),
                    surfaceContainerLow = if (isAmoled) Color(0xFF080808) else Color(0xFF0E1013),
                    surfaceContainer = if (isAmoled) Color(0xFF101010) else Color(0xFF181B21),
                    surfaceContainerHigh = if (isAmoled) Color(0xFF181818) else Color(0xFF22262E),
                    surfaceContainerHighest = if (isAmoled) Color(0xFF222222) else Color(0xFF2D323D),
                    outline = Color(0xFF8E9585),
                    outlineVariant = Color(0xFF454B3B)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF556600),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD0FF00),
                    onPrimaryContainer = Color(0xFF181F00),
                    secondary = Color(0xFF7600DA),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFEEDBFF),
                    onSecondaryContainer = Color(0xFF26004D),
                    tertiary = Color(0xFF495B30),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFCBDFAC),
                    onTertiaryContainer = Color(0xFF0E1F00),
                    background = Color(0xFFFCFDF6),
                    onBackground = Color(0xFF171A11),
                    surface = Color(0xFFF7FAEE),
                    onSurface = Color(0xFF171A11),
                    surfaceVariant = Color(0xFFE2E7D1),
                    onSurfaceVariant = Color(0xFF444A39),
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFF2F7E4),
                    surfaceContainer = Color(0xFFEAF1D7),
                    surfaceContainerHigh = Color(0xFFE0E9C7),
                    surfaceContainerHighest = Color(0xFFD5DFB8),
                    outline = Color(0xFF757C68),
                    outlineVariant = Color(0xFFC5CCB6)
                )
            }
        }

        com.abht.manga_dt.data.ThemePreset.NEON_ROSE -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFF096C),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFF6E002B),
                    onPrimaryContainer = Color(0xFFFFD9E2),
                    secondary = Color(0xFF8E9EB0),
                    onSecondary = Color(0xFF192731),
                    secondaryContainer = Color(0xFF2A3843),
                    onSecondaryContainer = Color(0xFFD5E3F5),
                    tertiary = Color(0xFFF2088A),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFF5B0032),
                    onTertiaryContainer = Color(0xFFFFD8E6),
                    background = if (isAmoled) Color.Black else Color(0xFF121C24),
                    onBackground = Color(0xFFECEFF3),
                    surface = if (isAmoled) Color.Black else Color(0xFF192731),
                    onSurface = Color(0xFFECEFF3),
                    surfaceVariant = Color(0xFF273845),
                    onSurfaceVariant = Color(0xFFCAD4DE),
                    surfaceContainerLowest = if (isAmoled) Color.Black else Color(0xFF0C1319),
                    surfaceContainerLow = if (isAmoled) Color(0xFF080808) else Color(0xFF15212A),
                    surfaceContainer = if (isAmoled) Color(0xFF101010) else Color(0xFF1F2F3B),
                    surfaceContainerHigh = if (isAmoled) Color(0xFF181818) else Color(0xFF293C4A),
                    surfaceContainerHighest = if (isAmoled) Color(0xFF222222) else Color(0xFF354B5B),
                    outline = Color(0xFF90A1B0),
                    outlineVariant = Color(0xFF445564)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFB8004C),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFD9E2),
                    onPrimaryContainer = Color(0xFF3F0015),
                    secondary = Color(0xFF4F6172),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFD3E4F6),
                    onSecondaryContainer = Color(0xFF0B1D2C),
                    tertiary = Color(0xFF980054),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFFFD8E6),
                    onTertiaryContainer = Color(0xFF37001C),
                    background = Color(0xFFFCF8FA),
                    onBackground = Color(0xFF1D1B1E),
                    surface = Color(0xFFFAF0F4),
                    onSurface = Color(0xFF1D1B1E),
                    surfaceVariant = Color(0xFFEFE0E5),
                    onSurfaceVariant = Color(0xFF4E4448),
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFF8ECF1),
                    surfaceContainer = Color(0xFFF3E2E9),
                    surfaceContainerHigh = Color(0xFFEBD3DD),
                    surfaceContainerHighest = Color(0xFFDFC2CD),
                    outline = Color(0xFF817378),
                    outlineVariant = Color(0xFFD2C2C7)
                )
            }
        }

        com.abht.manga_dt.data.ThemePreset.MIDNIGHT_SOLAR -> {
            if (isDark) {
                darkColorScheme(
                    primary = Color(0xFFFFD72A),
                    onPrimary = Color(0xFF111B2E),
                    primaryContainer = Color(0xFF554400),
                    onPrimaryContainer = Color(0xFFFFEE93),
                    secondary = Color(0xFFFF5288),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFF6E0D32),
                    onSecondaryContainer = Color(0xFFFFD9E3),
                    tertiary = Color(0xFF789CDB),
                    onTertiary = Color(0xFF0B1832),
                    tertiaryContainer = Color(0xFF182641),
                    onTertiaryContainer = Color(0xFFD3E2FF),
                    background = if (isAmoled) Color.Black else Color(0xFF0D1424),
                    onBackground = Color(0xFFECEEF5),
                    surface = if (isAmoled) Color.Black else Color(0xFF111B2E),
                    onSurface = Color(0xFFECEEF5),
                    surfaceVariant = Color(0xFF22314E),
                    onSurfaceVariant = Color(0xFFCBD2E3),
                    surfaceContainerLowest = if (isAmoled) Color.Black else Color(0xFF080D19),
                    surfaceContainerLow = if (isAmoled) Color(0xFF080808) else Color(0xFF0E1728),
                    surfaceContainer = if (isAmoled) Color(0xFF101010) else Color(0xFF182641),
                    surfaceContainerHigh = if (isAmoled) Color(0xFF181818) else Color(0xFF233455),
                    surfaceContainerHighest = if (isAmoled) Color(0xFF222222) else Color(0xFF2F436B),
                    outline = Color(0xFF8C9AB4),
                    outlineVariant = Color(0xFF3D4B66)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7A5F00),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFFFE077),
                    onPrimaryContainer = Color(0xFF261D00),
                    secondary = Color(0xFFA11746),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFFFD9E3),
                    onSecondaryContainer = Color(0xFF3E0015),
                    tertiary = Color(0xFF385E9A),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFD3E2FF),
                    onTertiaryContainer = Color(0xFF001A41),
                    background = Color(0xFFF9FAFD),
                    onBackground = Color(0xFF171B23),
                    surface = Color(0xFFF0F4FA),
                    onSurface = Color(0xFF171B23),
                    surfaceVariant = Color(0xFFDEE3EF),
                    onSurfaceVariant = Color(0xFF424752),
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFE8EEF8),
                    surfaceContainer = Color(0xFFDFE7F4),
                    surfaceContainerHigh = Color(0xFFD2DEED),
                    surfaceContainerHighest = Color(0xFFC3D2E4),
                    outline = Color(0xFF727883),
                    outlineVariant = Color(0xFFC2C7D3)
                )
            }
        }

        else -> createDynamicColorScheme(Color(preset.primaryColor), isDark, isAmoled)
    }
}

@Composable
fun rememberAnimatedPresetColorScheme(
    preset: com.abht.manga_dt.data.ThemePreset,
    isDark: Boolean,
    isAmoled: Boolean = false
): ColorScheme {
    val targetScheme = remember(preset, isDark, isAmoled) {
        createPresetColorScheme(preset, isDark, isAmoled)
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
