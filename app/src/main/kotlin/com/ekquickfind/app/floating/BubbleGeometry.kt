package com.ekquickfind.app.floating

import kotlin.math.roundToInt

/** Where the button rests: an edge and a height from 0 (top) to 1 (bottom). */
data class BubbleSpot(
    val onRight: Boolean,
    val yFraction: Float,
)

/** Screen maths for the floating button; positions are the button's top-left corner in pixels. */
class BubbleGeometry(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val sizePx: Int,
    private val marginPx: Int,
) {
    private val maxY = (screenHeight - sizePx).coerceAtLeast(0)

    fun place(
        onRight: Boolean,
        yFraction: Float,
    ): Pair<Int, Int> {
        val x = if (onRight) screenWidth - sizePx - marginPx else marginPx
        return x to (yFraction.coerceIn(0f, 1f) * maxY).roundToInt()
    }

    /** The edge nearer to where the button was let go. */
    fun snap(
        x: Int,
        y: Int,
    ) = BubbleSpot(
        onRight = x + sizePx / 2 >= screenWidth / 2,
        yFraction = if (maxY == 0) 0f else (y.toFloat() / maxY).coerceIn(0f, 1f),
    )
}

/** A press that hardly moved is a tap, not a drag. */
fun isTap(
    dx: Float,
    dy: Float,
    slopPx: Int,
): Boolean = dx * dx + dy * dy <= slopPx.toFloat() * slopPx
