package com.ekquickfind.core.search.quick

import java.math.BigDecimal
import java.math.RoundingMode

internal fun formatNumber(
    value: Double,
    maxDecimals: Int,
): String {
    val rounded = BigDecimal.valueOf(value).setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
    val plain = rounded.toPlainString()
    return if (plain == "-0") "0" else plain
}
