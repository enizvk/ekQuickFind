package com.ekquickfind.core.search

import kotlin.math.ln
import kotlin.math.pow

data class LaunchStats(
    val count: Int,
    val lastLaunchedAt: Long,
)

/** History-derived inputs for ranking, snapshotted once per query. */
data class RankingSignals(
    val launches: Map<ResultId, LaunchStats> = emptyMap(),
    val pinned: Set<ResultId> = emptySet(),
    val now: Long = System.currentTimeMillis(),
) {
    companion object {
        val None = RankingSignals(now = 0)
    }
}

data class RankedResult(
    val result: SearchResult,
    val score: Float,
)

/**
 * finalScore = textScore * sectionWeight + log1p(launches) * decay * launchWeight + pinBoost.
 * Decay halves every [halfLifeDays]; the history store prunes events older than 90 days.
 */
class Ranker(
    private val halfLifeDays: Double = 30.0,
    private val launchWeight: Float = 0.25f,
    private val pinBoost: Float = 0.5f,
) {
    fun score(
        result: SearchResult,
        signals: RankingSignals,
    ): Float {
        val text = result.score * result.section.weight
        if (result.isFallback || !result.isRecordable) return text
        val launches = signals.launches[result.id]?.let { launchBoost(it, signals.now) } ?: 0f
        val pin = if (result.id in signals.pinned) pinBoost else 0f
        return text + launches + pin
    }

    /** Stable: equal scores keep the order the provider emitted (its own tie-breaks). */
    fun rank(
        results: List<SearchResult>,
        signals: RankingSignals,
    ): List<RankedResult> = results.map { RankedResult(it, score(it, signals)) }.sortedByDescending { it.score }

    internal fun launchBoost(
        stats: LaunchStats,
        now: Long,
    ): Float {
        if (stats.count <= 0) return 0f
        val ageDays = (now - stats.lastLaunchedAt).coerceAtLeast(0) / DAY_MS.toDouble()
        val decay = 0.5.pow(ageDays / halfLifeDays)
        return (ln(1.0 + stats.count) * decay * launchWeight).toFloat()
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
