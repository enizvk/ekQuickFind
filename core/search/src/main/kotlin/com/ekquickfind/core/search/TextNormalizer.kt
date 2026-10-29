package com.ekquickfind.core.search

import java.text.Normalizer
import java.util.Locale

object TextNormalizer {
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    /** Lowercase and strip accents without touching whitespace; used per character by [MatchTarget]. */
    internal fun normalizeChunk(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)

    /** Lowercase, strip accents, collapse whitespace. */
    fun normalize(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .replace(whitespace, " ")
            .trim()
}
