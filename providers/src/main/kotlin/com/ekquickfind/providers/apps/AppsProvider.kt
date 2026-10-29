package com.ekquickfind.providers.apps

import com.ekquickfind.core.search.HiddenResults
import com.ekquickfind.core.search.HideAction
import com.ekquickfind.core.search.ProviderId
import com.ekquickfind.core.search.Query
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.ResultId
import com.ekquickfind.core.search.ResultResolver
import com.ekquickfind.core.search.SearchProvider
import com.ekquickfind.core.search.SearchResult
import com.ekquickfind.core.search.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class AppsProvider(
    private val index: AppIndex,
    private val hidden: HiddenResults,
) : SearchProvider,
    ResultResolver {
    override val id = ID
    override val section = Section.Apps

    // Until the index is built the Apps section shows a loading row.
    override val showsLoadingIndicator = true

    override fun search(query: Query): Flow<List<SearchResult>> =
        flow {
            val ready = index.state.filterIsInstance<AppIndexState.Ready>().first()
            val hiddenIds = hidden.ids()
            val q = query.normalized
            val apps =
                ready.apps.mapNotNull { app ->
                    val id = resultId(app.entry)
                    if (id in hiddenIds) return@mapNotNull null
                    // Package names are a power-user path; rank them just below a label match.
                    val score =
                        listOfNotNull(QueryMatcher.score(q, app.label), QueryMatcher.score(q, app.packageName)?.times(0.9f)).maxOrNull()
                            ?: return@mapNotNull null
                    appResult(app.entry, id, score)
                }
            val shortcuts =
                ready.shortcuts.mapNotNull { s ->
                    val score = QueryMatcher.score(q, s.label) ?: return@mapNotNull null
                    shortcutResult(s.entry, score * 0.9f)
                }
            emit((apps + shortcuts).sortedByDescending { it.score }.take(MAX_RESULTS))
        }

    override suspend fun resolve(id: ResultId): SearchResult? {
        val ready = index.state.filterIsInstance<AppIndexState.Ready>().first()
        ready.apps.firstOrNull { resultId(it.entry) == id }?.let { return appResult(it.entry, id, 1f) }
        return ready.shortcuts
            .firstOrNull { shortcutId(it.entry) == id }
            ?.let { shortcutResult(it.entry, 1f) }
    }

    /** Personal-profile launcher apps for the given packages, in order; used for "Suggested". */
    suspend fun forPackages(packages: List<String>): List<SearchResult> {
        val ready = index.state.filterIsInstance<AppIndexState.Ready>().first()
        val hiddenIds = hidden.ids()
        return packages.mapNotNull { pkg ->
            ready.apps
                .firstOrNull { it.entry.packageName == pkg && it.entry.profileBadge == null }
                ?.let { appResult(it.entry, resultId(it.entry), 1f) }
                ?.takeIf { it.id !in hiddenIds }
        }
    }

    private fun appResult(
        app: AppEntry,
        id: ResultId,
        score: Float,
    ): SearchResult {
        val canUninstall = !app.isSystemApp && app.profileBadge == null
        return SearchResult(
            id = id,
            providerId = ID,
            section = section,
            title = app.label,
            icon = ResultIcon.PackageIcon(app.packageName, app.activityName, app.userSerial),
            score = score,
            badge = app.profileBadge,
            primaryAction = LaunchAppAction(app.packageName, app.activityName, app.userSerial),
            secondaryActions =
                listOfNotNull(
                    AppInfoAction(app.packageName, app.activityName, app.userSerial),
                    UninstallAction(app.packageName).takeIf { canUninstall },
                    HideAction(id),
                ),
        )
    }

    private fun shortcutResult(
        s: ShortcutEntry,
        score: Float,
    ) = SearchResult(
        id = shortcutId(s),
        providerId = ID,
        section = section,
        title = "${s.label} · ${s.appLabel}",
        icon = ResultIcon.ShortcutIcon(s.packageName, s.shortcutId, s.userSerial),
        score = score,
        badge = s.profileBadge,
        primaryAction = LaunchShortcutAction(s.packageName, s.shortcutId, s.userSerial),
    )

    companion object {
        val ID = ProviderId("apps")
        private const val MAX_RESULTS = 30

        fun resultId(app: AppEntry) = ResultId("apps:${app.packageName}/${app.activityName}@${app.userSerial}")

        private fun shortcutId(s: ShortcutEntry) = ResultId("apps:shortcut:${s.packageName}/${s.shortcutId}@${s.userSerial}")
    }
}
