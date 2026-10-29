package com.ekquickfind.app.settings

import com.ekquickfind.core.search.MatchTarget
import com.ekquickfind.core.search.QueryMatcher
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.TextNormalizer
import com.ekquickfind.providers.apps.AppEntry
import com.ekquickfind.providers.apps.AppsProvider

data class ExcludableApp(
    val id: String,
    val label: String,
    val badge: String?,
    val icon: ResultIcon,
    val excluded: Boolean,
)

/** Rows for the Excluded apps page: every launchable app with whether it's hidden from search. */
fun excludableApps(
    apps: List<AppEntry>,
    hidden: Set<String>,
    filter: String,
    excludedFirst: Boolean = false,
): List<ExcludableApp> {
    val q = TextNormalizer.normalize(filter)
    val rows =
        apps
            .filter {
                q.isEmpty() ||
                    QueryMatcher.score(
                        q,
                        MatchTarget(it.label),
                    ) != null ||
                    TextNormalizer.normalize(it.label).contains(q)
            }.map { app ->
                val id = AppsProvider.resultId(app).value
                ExcludableApp(
                    id,
                    app.label,
                    app.profileBadge,
                    ResultIcon.PackageIcon(app.packageName, app.activityName, app.userSerial),
                    id in hidden,
                )
            }.sortedBy { it.label.lowercase() }
    return if (excludedFirst) rows.sortedByDescending { it.excluded } else rows
}
