package com.ekquickfind.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

private val brand = Color(0xFF3D5AFE)
internal val lightFallback = lightColorScheme(primary = brand, secondary = Color(0xFF5C6BC0), tertiary = Color(0xFF00897B))
internal val darkFallback = darkColorScheme(primary = Color(0xFFB6C2FF), secondary = Color(0xFFBEC6FF), tertiary = Color(0xFF80CBC4))

@Composable
fun ekColorScheme(
    dark: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    val context = LocalContext.current
    return when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> darkFallback
        else -> lightFallback
    }
}

/** Hue of a colour in degrees (0..360). */
fun Color.hue(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val d = max - min
    if (d == 0f) return 0f
    val h =
        when (max) {
            red -> ((green - blue) / d).mod(6f)
            green -> (blue - red) / d + 2f
            else -> (red - green) / d + 4f
        }
    return h * 60f
}

private fun Color.saturationHsl(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val l = (max + min) / 2f
    return if (max == min) 0f else (max - min) / (1f - abs(2f * l - 1f))
}

/**
 * Re-tints the accent roles from one [seed] colour, keeping everything else (surfaces, text, errors).
 * Lightness levels follow Material's tones: pale accents on dark, deep accents on light. Null keeps [this].
 */
fun ColorScheme.withAccent(
    seed: Color?,
    dark: Boolean,
): ColorScheme {
    if (seed == null) return this
    val h = seed.hue()
    val s = seed.saturationHsl().coerceIn(0.35f, 0.9f)

    fun tone(
        lightness: Float,
        saturation: Float = s,
    ) = Color.hsl(h, saturation.coerceIn(0f, 1f), lightness)
    return if (dark) {
        copy(
            primary = tone(0.74f),
            onPrimary = tone(0.14f),
            primaryContainer = tone(0.30f),
            onPrimaryContainer = tone(0.90f),
            inversePrimary = tone(0.40f),
            secondaryContainer = tone(0.27f, s * 0.45f),
            onSecondaryContainer = tone(0.90f, s * 0.5f),
            surfaceTint = tone(0.74f),
        )
    } else {
        copy(
            primary = tone(0.38f),
            onPrimary = Color.White,
            primaryContainer = tone(0.90f),
            onPrimaryContainer = tone(0.12f),
            inversePrimary = tone(0.78f),
            secondaryContainer = tone(0.88f, s * 0.5f),
            onSecondaryContainer = tone(0.14f, s * 0.5f),
            surfaceTint = tone(0.38f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EkQuickFindTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    reduceMotion: Boolean = false,
    /** Seed for the accent roles; null keeps the wallpaper or default accent. */
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = ekColorScheme(dark, dynamicColor).withAccent(accent, dark),
        // Reduce motion swaps bouncy springs for the calmer standard scheme.
        motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
        content = content,
    )
}
