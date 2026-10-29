package com.ekquickfind.app.ui.settings

import kotlin.math.roundToInt

internal fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> = toMutableList().apply { add(to, removeAt(from)) }

/** Row the dragged item lands on after moving [offsetPx] from [from]; it snaps once it passes half a row. */
internal fun dragTarget(
    from: Int,
    offsetPx: Float,
    rowHeightPx: Int,
    count: Int,
): Int = (from + (offsetPx / rowHeightPx).roundToInt()).coerceIn(0, count - 1)
