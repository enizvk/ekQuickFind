package com.ekquickfind.core.search.quick

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class CalculatorTest {
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12*8+3 | 99",
            "15% of 240 | 36",
            "2+2 | 4",
            "2 + 3 * 4 | 14",
            "(2 + 3) * 4 | 20",
            "10 / 4 | 2.5",
            "2^10 | 1024",
            "2^3^2 | 512",
            "-3 + 5 | 2",
            "-(2+3) | -5",
            "1.5 * 1.5 | 2.25",
            "7 × 6 | 42",
            "84 ÷ 2 | 42",
            "10 − 3 | 7",
            "200 + 10% | 220",
            "200 - 10% | 180",
            "50% * 8 | 4",
            "0.1 + 0.2 | 0.3",
            "1/3 | 0.3333333333",
            "1e3 + 1 | 1001",
        ],
    )
    fun `evaluates expressions`(
        expression: String,
        expected: String,
    ) {
        assertEquals(expected, Calculator.evaluate(expression)?.formatted)
    }

    @ParameterizedTest(name = "\"{0}\" has no answer")
    @ValueSource(strings = ["12*", "1/0", "42", "(5)", "camera", "2 apples + 3", "", "()", "1..2+3", "+", "3 4"])
    fun `returns nothing for invalid or trivial input`(expression: String) {
        assertNull(Calculator.evaluate(expression))
    }
}
