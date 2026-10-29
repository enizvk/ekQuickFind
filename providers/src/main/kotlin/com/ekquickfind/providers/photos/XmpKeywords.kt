package com.ekquickfind.providers.photos

/** Keywords that editing apps (Lightroom, digiKam, Photos) save inside a photo's XMP metadata. */
object XmpKeywords {
    private val subject = Regex("<(?:\\w+:)?subject\\b[^>]*>(.*?)</(?:\\w+:)?subject>", RegexOption.DOT_MATCHES_ALL)
    private val hierarchical =
        Regex("<(?:\\w+:)?hierarchicalSubject\\b[^>]*>(.*?)</(?:\\w+:)?hierarchicalSubject>", RegexOption.DOT_MATCHES_ALL)
    private val item = Regex("<(?:\\w+:)?li\\b[^>]*>(.*?)</(?:\\w+:)?li>", RegexOption.DOT_MATCHES_ALL)
    private val numeric = Regex("&#(x?)([0-9a-fA-F]+);")

    fun parse(xmp: String?): List<String> {
        if (xmp.isNullOrBlank()) return emptyList()
        val flat = subject.findAll(xmp).flatMap { items(it.groupValues[1]) }
        // "Animals|Pets|Dog" keeps its most specific level.
        val nested = hierarchical.findAll(xmp).flatMap { items(it.groupValues[1]) }.map { it.substringAfterLast('|').trim() }
        return (flat + nested).filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.toList()
    }

    private fun items(block: String) = item.findAll(block).map { decode(it.groupValues[1]).trim() }

    private fun decode(text: String) =
        numeric
            .replace(text) { m ->
                val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
                code?.let { String(Character.toChars(it)) } ?: m.value
            }.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
}
