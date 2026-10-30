package com.ekquickfind.providers.sites

/** A site as the matcher sees it: its keyword ("yt") and words that also point to it ("youtube"). */
data class SiteKey(
    val siteId: String,
    val keyword: String,
    val triggers: List<String> = emptyList(),
)

/** [text] is the search for the site; [byKeyword] says it was the keyword, not a matching word. */
data class KeywordMatch(
    val siteId: String,
    val text: String,
    val byKeyword: Boolean,
)

/** The site keyword at the start of the bar, [end] characters long (with its separator), for colouring it. */
data class KeywordPrefix(
    val siteId: String,
    val end: Int,
)

/**
 * Finds a site shortcut in what was typed. The keyword must be the first word, then the user's separator if they
 * set one (optional, it may touch the keyword) and a space: "yt: cats", "yt - cats", "yt cats". The space after the
 * separator keeps names like "yt-dlp" normal searches. A matching word works at the start or the end
 * ("youtube cats", "sofia weather"), or alone ("weather").
 */
object KeywordQuery {
    fun parse(
        raw: String,
        sites: List<SiteKey>,
        separator: String,
    ): KeywordMatch? {
        val typed = raw.trim()
        if (typed.isEmpty()) return null
        sites.firstNotNullOfOrNull { site -> byKeyword(typed, site, separator) }?.let { return it }
        return sites.firstNotNullOfOrNull { site -> byTrigger(typed, site) }
    }

    /**
     * The keyword while it's being typed: shown as soon as a space follows it (or the separator and a space), before
     * the search itself is typed. Matching words aren't keywords, so they don't count.
     */
    fun prefix(
        raw: String,
        sites: List<SiteKey>,
        separator: String,
    ): KeywordPrefix? =
        sites.firstNotNullOfOrNull { site ->
            val keyword = site.keyword.trim()
            if (keyword.isEmpty() || !raw.startsWith(keyword, ignoreCase = true)) return@firstNotNullOfOrNull null
            val after = raw.substring(keyword.length)
            val spaced = after.length - after.trimStart().length
            val rest = after.trimStart()
            when {
                separator.isNotEmpty() && rest.startsWith(separator) && rest.removePrefix(separator).startsWithSpace() ->
                    KeywordPrefix(site.siteId, keyword.length + spaced + separator.length)
                after.startsWithSpace() -> KeywordPrefix(site.siteId, keyword.length)
                else -> null
            }
        }

    private fun byKeyword(
        typed: String,
        site: SiteKey,
        separator: String,
    ): KeywordMatch? {
        val keyword = site.keyword.trim()
        if (keyword.isEmpty() || !typed.startsWith(keyword, ignoreCase = true)) return null
        val after = typed.substring(keyword.length)
        val withSeparator = after.trimStart()
        val text =
            when {
                separator.isNotEmpty() && withSeparator.startsWith(separator) && withSeparator.removePrefix(separator).startsWithSpace() ->
                    withSeparator.removePrefix(separator)
                after.startsWithSpace() -> after
                else -> return null
            }.trim()
        return text.takeIf { it.isNotEmpty() }?.let { KeywordMatch(site.siteId, it, byKeyword = true) }
    }

    private fun String.startsWithSpace() = firstOrNull()?.isWhitespace() == true

    private fun byTrigger(
        typed: String,
        site: SiteKey,
    ): KeywordMatch? {
        val words = typed.split(whitespace)
        site.triggers.forEach { trigger ->
            val text =
                when {
                    words.size == 1 && words[0].equals(trigger, ignoreCase = true) -> ""
                    words.first().equals(trigger, ignoreCase = true) -> words.drop(1).joinToString(" ")
                    words.last().equals(trigger, ignoreCase = true) -> words.dropLast(1).joinToString(" ")
                    else -> null
                }
            if (text != null) return KeywordMatch(site.siteId, text, byKeyword = false)
        }
        return null
    }

    private val whitespace = Regex("\\s+")
}
