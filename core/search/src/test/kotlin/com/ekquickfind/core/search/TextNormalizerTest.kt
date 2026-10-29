package com.ekquickfind.core.search

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TextNormalizerTest {
    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(
        "Café, cafe",
        "'  Google    Maps ', google maps",
        "ÉCOLE, ecole",
        "Zürich, zurich",
        "'tab\tand\nnewline', tab and newline",
        "'', ''",
    )
    fun `normalises case, accents and whitespace`(
        input: String,
        expected: String,
    ) {
        assertEquals(expected, TextNormalizer.normalize(input))
    }
}
