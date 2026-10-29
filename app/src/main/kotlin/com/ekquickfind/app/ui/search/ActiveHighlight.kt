package com.ekquickfind.app.ui.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ekquickfind.app.ui.theme.LocalSurfaceStyle
import com.ekquickfind.data.prefs.ActiveItemStyle
import com.ekquickfind.data.prefs.BarAnimation
import com.ekquickfind.data.prefs.ItemAnimation

private const val SELECT_MS = 240

private const val LIFT_MS = 160
private const val LIFT_SCALE = 1.03f

/** The search bar glow an item animation runs around the item's border, if any. */
internal fun ItemAnimation.borderGlow(): BarAnimation? =
    when (this) {
        ItemAnimation.Settle, ItemAnimation.Lift -> null
        ItemAnimation.LightSweep -> BarAnimation.LightSweep
        ItemAnimation.ColourPulse -> BarAnimation.ColourPulse
        ItemAnimation.ShimmerRing -> BarAnimation.ShimmerRing
        ItemAnimation.Breathe -> BarAnimation.Breathe
        ItemAnimation.TwinLights -> BarAnimation.TwinLights
    }

/** Corner radius that rounds a chip's ends fully, like CircleShape. */
internal val PILL_CORNER = 100.dp
private val PINK = Color(0xFFF28BB5)

private class HighlightLook(
    val fill: Brush,
    val line: Brush,
    val lineWidth: Dp,
    val glow: Color,
    val glowRadius: Dp,
    val glowOffsetY: Dp,
)

/**
 * Marks the active result in the chosen style and plays the item animation when it becomes active.
 * Put it before any clip: the line is drawn inside the bounds, so it's equally thick all the way round.
 * The glow is kept within the list's 8 dp padding so the list edge doesn't cut it off.
 */
@Composable
internal fun Modifier.activeHighlight(
    selected: Boolean,
    cornerRadius: Dp,
): Modifier {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val style = LocalSurfaceStyle.current
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (style.itemAnimation != null) tween(SELECT_MS, easing = FastOutSlowInEasing) else snap(),
        label = "active-highlight",
    )
    val visible by remember { derivedStateOf { progress > 0f } }
    if (!visible) return this
    // Lift: the item rises a touch and settles back as it becomes active.
    val lift = remember { Animatable(1f) }
    if (style.itemAnimation == ItemAnimation.Lift) {
        LaunchedEffect(Unit) {
            lift.animateTo(LIFT_SCALE, tween(LIFT_MS, easing = FastOutSlowInEasing))
            lift.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val look =
        remember(style.activeItem, colors.primary, colors.tertiary) {
            val accent = colors.primary
            when (style.activeItem) {
                ActiveItemStyle.TintedCard ->
                    HighlightLook(
                        fill = Brush.linearGradient(listOf(accent.copy(alpha = 0.20f), colors.tertiary.copy(alpha = 0.12f))),
                        // A soft accent edge, lighter than Soft glow's line.
                        line = SolidColor(accent.copy(alpha = 0.35f)),
                        lineWidth = 1.dp,
                        glow = accent.copy(alpha = 0.22f),
                        glowRadius = 10.dp,
                        glowOffsetY = 3.dp,
                    )
                ActiveItemStyle.SoftGlow ->
                    HighlightLook(
                        fill = SolidColor(accent.copy(alpha = 0.08f)),
                        line = SolidColor(accent.copy(alpha = 0.85f)),
                        lineWidth = 1.5.dp,
                        glow = accent.copy(alpha = 0.32f),
                        glowRadius = 10.dp,
                        glowOffsetY = 0.dp,
                    )
                ActiveItemStyle.Gradient ->
                    HighlightLook(
                        fill = SolidColor(colors.tertiary.copy(alpha = 0.07f)),
                        line = Brush.linearGradient(listOf(accent, colors.tertiary, PINK)),
                        lineWidth = 1.5.dp,
                        glow = colors.tertiary.copy(alpha = 0.3f),
                        glowRadius = 10.dp,
                        glowOffsetY = 0.dp,
                    )
            }
        }
    return this
        .then(
            if (style.itemAnimation == ItemAnimation.Lift) {
                Modifier.graphicsLayer {
                    scaleX = lift.value
                    scaleY = lift.value
                }
            } else {
                Modifier
            },
        ).openingGlow(style.itemAnimation?.borderGlow(), cornerRadius)
        .dropShadow(shape) {
            radius = look.glowRadius.toPx()
            offset = Offset(0f, look.glowOffsetY.toPx())
            color = look.glow
            alpha = progress
        }.drawWithCache {
            val width = look.lineWidth.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            // Inset by half the line so the stroke stays inside the bounds and nothing clips it.
            val lineOutline = shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
            onDrawWithContent {
                val p = progress
                // A slight grow-in reads as the highlight settling on the item.
                scale(0.97f + 0.03f * p) {
                    drawOutline(outline, look.fill, alpha = p)
                }
                drawContent()
                translate(width / 2, width / 2) {
                    drawOutline(lineOutline, look.line, alpha = p, style = Stroke(width))
                }
            }
        }
}
