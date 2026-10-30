package com.ekquickfind.data.history

import com.ekquickfind.core.search.LaunchStats
import com.ekquickfind.core.search.RankingSignals
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.SignalsSource
import com.ekquickfind.data.prefs.MAX_RECENTS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Everything the app remembers about use, all on device. Also the engine's ranking signal source. */
class HistoryRepository(
    private val dao: HistoryDao,
    /** Name of the connected headphones or car, if any; saved with each launch for suggestions. */
    private val audioDevice: () -> String? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) : SignalsSource {
    val pins: Flow<List<Pin>> = dao.pins()
    val recentResults: Flow<List<RecentResult>> = dao.recentResults(MAX_RECENTS)

    override suspend fun snapshot(): RankingSignals =
        RankingSignals(
            launches = dao.launchStats().associate { ResultId(it.resultId) to LaunchStats(it.count, it.lastAt) },
            pinned =
                dao
                    .pins()
                    .first()
                    .map { ResultId(it.resultId) }
                    .toSet(),
            now = clock(),
        )

    suspend fun recordLaunch(result: SearchResult) {
        if (!result.isRecordable) return
        dao.insertLaunch(
            LaunchEvent(resultId = result.id.value, providerId = result.providerId.value, at = clock(), audioDevice = audioDevice()),
        )
    }

    suspend fun launchesSince(since: Long): List<LaunchEvent> = dao.launchesSince(since)

    suspend fun pin(result: SearchResult) {
        if (!result.isRecordable) return
        dao.pin(Pin(result.id.value, result.providerId.value, dao.nextPinPosition()))
    }

    suspend fun unpin(id: ResultId) = dao.unpin(id.value)

    suspend fun forgetResult(id: ResultId) = dao.forgetResult(id.value)

    /** Clears launches (recent items and learned ranking); pins are kept on purpose. */
    suspend fun clear() = dao.clearLaunches()

    suspend fun prune() = dao.pruneLaunchesBefore(clock() - RETENTION_MS)

    private companion object {
        const val RETENTION_MS = 90L * 24 * 60 * 60 * 1000
    }
}
