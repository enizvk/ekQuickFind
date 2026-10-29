package com.ekquickfind.core.search

/** A searchable text with its normalised forms computed once, so matching per keystroke stays cheap. */
class MatchTarget(
    val raw: String,
) {
    /** Same result as [TextNormalizer.normalize], built per character so we can map back to [raw]. */
    val normalized: String

    /** rawIndex[i] is the index in [raw] that produced normalized[i]. */
    private val rawIndex: IntArray

    /** Words with their start offset in [normalized]. */
    internal val wordStarts: List<Pair<Int, String>>
    val words: List<String>
    val initials: String

    init {
        val sb = StringBuilder()
        val map = ArrayList<Int>(raw.length)
        var pendingSpace = false
        raw.forEachIndexed { i, c ->
            if (c.isWhitespace()) {
                pendingSpace = sb.isNotEmpty()
                return@forEachIndexed
            }
            val piece = TextNormalizer.normalizeChunk(c.toString())
            if (piece.isEmpty()) return@forEachIndexed
            if (pendingSpace) {
                sb.append(' ')
                map += i
                pendingSpace = false
            }
            piece.forEach {
                sb.append(it)
                map += i
            }
        }
        normalized = sb.toString()
        rawIndex = map.toIntArray()
        wordStarts =
            wordPattern.findAll(normalized).map { it.range.first to it.value }.toList()
        words = wordStarts.map { it.second }
        initials = words.joinToString("") { it.take(1) }
    }

    /** Maps a range on [normalized] to [raw], keeping trailing combining marks of the last character. */
    internal fun toRaw(range: IntRange): IntRange {
        val start = rawIndex[range.first]
        var end = rawIndex[range.last]
        while (end + 1 < raw.length && Character.getType(raw[end + 1]) == Character.NON_SPACING_MARK.toInt()) end++
        return start..end
    }

    private companion object {
        val wordPattern = Regex("[\\p{L}\\p{N}]+")
    }
}

/**
 * Scores how well a normalised query matches a target, or null for no match.
 * Order of strength: exact > prefix > word prefix > all words > initials > substring > one-edit typo.
 * Short queries (under [FUZZY_MIN_LENGTH]) only match starts of words, so one letter doesn't match everything containing it;
 * two letters may also be the initials of a multi-word name ("gm" for Google Maps).
 */
object QueryMatcher {
    private const val EXACT = 1f
    private const val PREFIX = 0.85f
    private const val WORD_PREFIX = 0.7f
    private const val ALL_WORDS = 0.62f
    private const val INITIALS = 0.55f
    private const val SUBSTRING = 0.4f
    private const val TYPO = 0.3f
    private const val TYPO_MIN_LENGTH = 4
    const val FUZZY_MIN_LENGTH = 3
    private const val INITIALS_MIN_LENGTH = 2

    fun score(
        query: String,
        target: MatchTarget,
        exact: Boolean = false,
    ): Float? {
        val text = target.normalized
        if (query.isEmpty() || text.isEmpty()) return null
        // Shorter targets win ties: "Maps" beats "Mapster" for "map".
        val coverage = query.length.toFloat() / text.length

        return when {
            text == query -> EXACT
            // Exact mode: the typed text has to be whole words ("eren" finds "Ali Eren", not "Erenay").
            exact ->
                when (wholeWordIndex(query, text)) {
                    -1 -> null
                    0 -> PREFIX + 0.1f * coverage
                    else -> WORD_PREFIX + 0.05f * coverage
                }
            text.startsWith(query) -> PREFIX + 0.1f * coverage
            target.words.drop(1).any { it.startsWith(query) } -> WORD_PREFIX + 0.05f * coverage
            query.length < FUZZY_MIN_LENGTH -> if (shortInitials(query, target)) INITIALS else null
            matchesAllWords(query, target.words) -> ALL_WORDS
            !query.contains(' ') && target.initials.startsWith(query) -> INITIALS
            text.contains(query) -> SUBSTRING
            query.length >= TYPO_MIN_LENGTH && hasOneEditMatch(query, target) -> TYPO
            else -> null
        }
    }

    /**
     * Raw-text ranges to highlight for [query] in [target], following the same match order as [score].
     * Empty when there's no match.
     */
    fun highlight(
        query: String,
        target: MatchTarget,
        exact: Boolean = false,
    ): List<IntRange> {
        val text = target.normalized
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val ranges: List<IntRange> =
            when {
                exact ->
                    wholeWordIndex(query, text)
                        .takeIf { it >= 0 }
                        ?.let { listOf(it until it + query.length) }
                        .orEmpty()
                text.startsWith(query) -> listOf(0 until query.length)
                else -> {
                    val word = target.wordStarts.drop(1).firstOrNull { it.second.startsWith(query) }
                    when {
                        word != null -> listOf(word.first until word.first + query.length)
                        query.length < FUZZY_MIN_LENGTH ->
                            if (shortInitials(query, target)) {
                                target.wordStarts.take(query.length).map { it.first..it.first }
                            } else {
                                emptyList()
                            }
                        matchesAllWords(query, target.words) -> allWordsRanges(query, target)
                        !query.contains(' ') && target.initials.startsWith(query) ->
                            target.wordStarts.take(query.length).map { it.first..it.first }
                        text.contains(query) -> text.indexOf(query).let { listOf(it until it + query.length) }
                        query.length >= TYPO_MIN_LENGTH -> typoRange(query, target)
                        else -> emptyList()
                    }
                }
            }
        return ranges.filter { !it.isEmpty() }.map(target::toRaw)
    }

    /**
     * The opposite of [highlight]: the parts of the text the user hasn't typed, trimmed of spaces.
     * Used for suggestions, where emphasising the typed part on every row reads as repetition.
     */
    fun completion(
        query: String,
        target: MatchTarget,
    ): List<IntRange> {
        val matched = highlight(query, target)
        if (matched.isEmpty()) return emptyList()
        val raw = target.raw
        val gaps = mutableListOf<IntRange>()
        var start = 0
        matched.sortedBy { it.first }.forEach { m ->
            if (m.first > start) gaps += start until m.first
            start = m.last + 1
        }
        if (start < raw.length) gaps += start until raw.length
        return gaps.mapNotNull { r ->
            var a = r.first
            var b = r.last
            while (a <= b && raw[a].isWhitespace()) a++
            while (b >= a && raw[b].isWhitespace()) b--
            if (a <= b) a..b else null
        }
    }

    /** Where [query] first sits in [text] with no letter or digit right before or after it; -1 if nowhere. */
    private fun wholeWordIndex(
        query: String,
        text: String,
    ): Int {
        var i = text.indexOf(query)
        while (i >= 0) {
            val end = i + query.length
            if ((i == 0 || !text[i - 1].isLetterOrDigit()) && (end == text.length || !text[end].isLetterOrDigit())) return i
            i = text.indexOf(query, i + 1)
        }
        return -1
    }

    private fun shortInitials(
        query: String,
        target: MatchTarget,
    ) = query.length >= INITIALS_MIN_LENGTH && target.words.size > 1 && target.initials.startsWith(query)

    private fun allWordsRanges(
        query: String,
        target: MatchTarget,
    ): List<IntRange> {
        val remaining = target.wordStarts.toMutableList()
        return query.split(' ').mapNotNull { qw ->
            val hit = remaining.firstOrNull { it.second.startsWith(qw) } ?: return@mapNotNull null
            remaining.remove(hit)
            hit.first until hit.first + qw.length
        }
    }

    private fun typoRange(
        query: String,
        target: MatchTarget,
    ): List<IntRange> {
        val candidates = listOf(0 to target.normalized) + target.wordStarts
        for ((start, candidate) in candidates) {
            for (len in query.length - 1..query.length + 1) {
                if (len in 1..candidate.length && withinOneEdit(query, candidate.substring(0, len))) return listOf(start until start + len)
            }
        }
        return emptyList()
    }

    fun bestScore(
        query: String,
        targets: List<MatchTarget>,
        exact: Boolean = false,
    ): Float? = targets.mapNotNull { score(query, it, exact) }.maxOrNull()

    private fun matchesAllWords(
        query: String,
        words: List<String>,
    ): Boolean {
        val queryWords = query.split(' ')
        if (queryWords.size < 2) return false
        val remaining = words.toMutableList()
        return queryWords.all { qw ->
            val hit = remaining.firstOrNull { it.startsWith(qw) } ?: return@all false
            remaining.remove(hit)
        }
    }

    /** True when the query is within one edit of a same-length-ish prefix of the text or one of its words. */
    private fun hasOneEditMatch(
        query: String,
        target: MatchTarget,
    ): Boolean {
        val candidates = sequenceOf(target.normalized) + target.words.asSequence()
        return candidates.any { candidate ->
            (query.length - 1..query.length + 1).any { len ->
                len in 1..candidate.length && withinOneEdit(query, candidate.substring(0, len))
            }
        }
    }

    /** Optimal string alignment distance <= 1 (insert, delete, substitute or swap two neighbours). */
    internal fun withinOneEdit(
        a: String,
        b: String,
    ): Boolean {
        if (kotlin.math.abs(a.length - b.length) > 1) return false
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        if (i == a.length && i == b.length) return true
        return when {
            a.length == b.length ->
                a.substring(i + 1) == b.substring(i + 1) ||
                    (i + 1 < a.length && a[i] == b[i + 1] && a[i + 1] == b[i] && a.substring(i + 2) == b.substring(i + 2))
            a.length > b.length -> a.substring(i + 1) == b.substring(i)
            else -> a.substring(i) == b.substring(i + 1)
        }
    }
}
