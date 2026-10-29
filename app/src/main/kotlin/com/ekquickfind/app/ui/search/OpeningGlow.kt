package com.ekquickfind.app.ui.search

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ekquickfind.data.prefs.BarAnimation
import kotlin.math.min

private val SWEEP_EASING = CubicBezierEasing(0.45f, 0.05f, 0.35f, 1f)
private val VIOLET = Color(0xFFC58AF9)
private val PINK = Color(0xFFF28BB5)
private val AMBER = Color(0xFFFDD663)
private val GREEN = Color(0xFF81C995)

// Pulse brightness over time: breathes twice, then fades out.
private val PULSE_LINE = floatArrayOf(0f, 0.9f, 0.5f, 0.85f, 0.4f, 0f)
private val PULSE_HALO = floatArrayOf(0f, 0.55f, 0.25f, 0.5f, 0.2f, 0f)

// One slow breath in and out.
private val BREATHE_LINE = floatArrayOf(0f, 0.7f, 0f)
private val BREATHE_HALO = floatArrayOf(0f, 0.5f, 0f)

internal fun glowDurationMillis(animation: BarAnimation): Int =
    when (animation) {
        BarAnimation.LightSweep -> 2800
        BarAnimation.ColourPulse -> 3600
        BarAnimation.ShimmerRing -> 3200
        BarAnimation.Breathe -> 2400
        BarAnimation.TwinLights -> 2800
    }

/** Brightness of the line and of its soft halo at [p] (0..1 through the animation). */
internal fun glowAlphas(
    animation: BarAnimation,
    p: Float,
): Pair<Float, Float> =
    when (animation) {
        BarAnimation.LightSweep, BarAnimation.TwinLights -> envelope(p, fadeIn = 0.1f, fadeOut = 0.25f).let { it to it * 0.6f }
        BarAnimation.ColourPulse -> keyframes(p, PULSE_LINE) to keyframes(p, PULSE_HALO)
        BarAnimation.ShimmerRing -> envelope(p, fadeIn = 0.25f, fadeOut = 0.25f).let { it * 0.85f to it * 0.35f }
        BarAnimation.Breathe -> keyframes(p, BREATHE_LINE) to keyframes(p, BREATHE_HALO)
    }

/**
 * A short glow around the search bar when search opens, played once; nothing when [animation] is null.
 * A new [replay] value plays it again (the settings preview). Everything happens in the draw phase,
 * so typing and results aren't slowed down.
 */
@Composable
internal fun Modifier.openingGlow(
    animation: BarAnimation?,
    cornerRadius: Dp,
    replay: Int = 0,
): Modifier {
    if (animation == null) return this
    val accent = MaterialTheme.colorScheme.primary
    val progress = remember { Animatable(0f) }
    LaunchedEffect(animation, replay) {
        progress.snapTo(0f)
        val easing = if (animation == BarAnimation.LightSweep || animation == BarAnimation.TwinLights) SWEEP_EASING else LinearEasing
        progress.animateTo(1f, tween(glowDurationMillis(animation), easing = easing))
    }
    return drawWithCache {
        val line = 1.5.dp.toPx()
        val radius = min(cornerRadius.toPx(), size.height / 2)
        val rect = RectF(line / 2, line / 2, size.width - line / 2, size.height - line / 2)
        val center = Offset(size.width / 2, size.height / 2)
        val shader = glowShader(animation, accent, center, size)
        val matrix = Matrix()
        val soft = animation != BarAnimation.LightSweep && animation != BarAnimation.TwinLights

        fun paint(width: Dp) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = width.toPx()
                color = accent.toArgb()
                this.shader = shader
            }
        val linePaint = paint(1.5.dp)
        val haloPaint =
            paint(if (soft) 6.dp else 4.dp).apply {
                maskFilter = BlurMaskFilter((if (soft) 12.dp else 8.dp).toPx(), BlurMaskFilter.Blur.NORMAL)
            }
        onDrawWithContent {
            val p = progress.value
            if (p >= 1f) {
                drawContent()
                return@onDrawWithContent
            }
            when (animation) {
                BarAnimation.LightSweep -> matrix.setRotate(540f * p, center.x, center.y)
                BarAnimation.TwinLights -> matrix.setRotate(360f * p, center.x, center.y)
                BarAnimation.ShimmerRing -> matrix.setRotate(200f * p, center.x, center.y)
                BarAnimation.ColourPulse -> matrix.setTranslate(size.width * p, 0f)
                BarAnimation.Breathe -> Unit
            }
            shader?.setLocalMatrix(matrix)
            val (lineAlpha, haloAlpha) = glowAlphas(animation, p)
            linePaint.alpha = (255 * lineAlpha).toInt()
            haloPaint.alpha = (255 * haloAlpha).toInt()
            // Halo behind the bar so only its outer part shows; the line on top of the bar's edge.
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, haloPaint) }
            drawContent()
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(rect, radius, radius, linePaint) }
        }
    }
}

/** Colours of each animation; null means plain accent. */
private fun glowShader(
    animation: BarAnimation,
    accent: Color,
    center: Offset,
    size: Size,
): Shader? {
    val tail = listOf(Color.Transparent, accent.copy(alpha = 0.35f), Color.White, VIOLET.copy(alpha = 0.6f), Color.Transparent)
    return when (animation) {
        // A point of light: dark most of the way round, then a short bright tail.
        BarAnimation.LightSweep ->
            SweepGradientShader(center, listOf(Color.Transparent) + tail, listOf(0f, 0.80f, 0.89f, 0.97f, 0.995f, 1f))
        // The same tail twice, half a turn apart.
        BarAnimation.TwinLights ->
            SweepGradientShader(
                center,
                listOf(Color.Transparent) + tail + tail,
                listOf(0f, 0.30f, 0.39f, 0.47f, 0.495f, 0.5f, 0.80f, 0.89f, 0.97f, 0.995f, 1f),
            )
        // Ends on the colour it starts with, so the repeating band has no seam while it drifts.
        BarAnimation.ColourPulse ->
            LinearGradientShader(
                Offset.Zero,
                Offset(size.width, 0f),
                listOf(accent, VIOLET, PINK, AMBER, accent),
                tileMode = TileMode.Repeated,
            )
        BarAnimation.ShimmerRing -> SweepGradientShader(center, listOf(accent, VIOLET, PINK, AMBER, GREEN, accent))
        BarAnimation.Breathe -> null
    }
}

/** Fades in over the first [fadeIn] of the time, holds, fades out over the last [fadeOut]. */
internal fun envelope(
    p: Float,
    fadeIn: Float,
    fadeOut: Float,
): Float =
    when {
        p < fadeIn -> p / fadeIn
        p > 1f - fadeOut -> (1f - p) / fadeOut
        else -> 1f
    }.coerceIn(0f, 1f)

/** Smoothly interpolates evenly spaced [values] at [p] in 0..1. */
internal fun keyframes(
    p: Float,
    values: FloatArray,
): Float {
    val at = p.coerceIn(0f, 1f) * (values.size - 1)
    val i = at.toInt().coerceAtMost(values.size - 2)
    val t = at - i
    val eased = t * t * (3 - 2 * t)
    return values[i] + (values[i + 1] - values[i]) * eased
}
