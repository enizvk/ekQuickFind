package com.ekquickfind.app.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import com.ekquickfind.app.R
import com.ekquickfind.app.search.SearchActivity
import com.ekquickfind.core.search.ResultIcon
import com.ekquickfind.core.search.SearchResult

/** Mirrors pinned results into launcher shortcuts (long-press the ekQuickFind icon). */
class ShortcutsPublisher(
    private val context: Context,
) {
    private val manager = context.getSystemService(ShortcutManager::class.java)

    fun publish(pinned: List<SearchResult>) {
        val shortcuts =
            pinned.take(MAX_DYNAMIC).mapIndexed { rank, result ->
                ShortcutInfo
                    .Builder(context, "pin:${result.id.value}")
                    .setShortLabel(result.title)
                    .setLongLabel(result.subtitle?.let { "${result.title} · $it" } ?: result.title)
                    .setIcon(iconFor(result))
                    .setRank(rank)
                    .setIntent(
                        Intent(ACTION_OPEN_RESULT)
                            .setComponent(ComponentName(context, SearchActivity::class.java))
                            .putExtra(EXTRA_RESULT_ID, result.id.value)
                            .putExtra(EXTRA_PROVIDER_ID, result.providerId.value),
                    ).build()
            }
        runCatching { manager.dynamicShortcuts = shortcuts }
    }

    private fun iconFor(result: SearchResult): Icon {
        val app = result.icon as? ResultIcon.PackageIcon
        val drawable =
            app?.let {
                runCatching {
                    val launcherApps = context.getSystemService(LauncherApps::class.java)
                    val component = ComponentName(it.packageName, it.activityName ?: return@runCatching null)
                    launcherApps.resolveActivity(Intent().setComponent(component), Process.myUserHandle())?.getIcon(0)
                }.getOrNull()
            }
        return drawable?.let { Icon.createWithBitmap(it.toBitmap(ICON_PX, ICON_PX)) }
            ?: Icon.createWithResource(context, R.drawable.ic_shortcut_search)
    }

    companion object {
        const val ACTION_OPEN_RESULT = "com.ekquickfind.app.OPEN_RESULT"
        const val ACTION_OPEN_SETTINGS = "com.ekquickfind.app.OPEN_SETTINGS"
        const val EXTRA_RESULT_ID = "result_id"
        const val EXTRA_PROVIDER_ID = "provider_id"

        // Launchers show about four or five; two are taken by the static shortcuts.
        private const val MAX_DYNAMIC = 3
        private const val ICON_PX = 192
    }
}
