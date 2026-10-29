package com.ekquickfind.app.search

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

/** Most-used apps over the last week, only when the user granted usage access. */
class UsageSuggestions(
    private val context: Context,
) {
    fun hasAccess(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun topPackages(limit: Int): List<String> {
        if (!hasAccess()) return emptyList()
        val now = System.currentTimeMillis()
        val stats =
            context
                .getSystemService(
                    UsageStatsManager::class.java,
                ).queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - WEEK_MS, now)
        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .mapValues { (_, s) -> s.sumOf { it.totalTimeInForeground } }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(limit * 2)
    }

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
