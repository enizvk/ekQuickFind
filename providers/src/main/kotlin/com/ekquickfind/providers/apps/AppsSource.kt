package com.ekquickfind.providers.apps

import kotlinx.coroutines.flow.Flow

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userSerial: Long,
    /** "Work", "Private" etc. for non-personal profiles, null for the personal profile. */
    val profileBadge: String?,
    val isSystemApp: Boolean,
)

data class ShortcutEntry(
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val appLabel: String,
    val userSerial: Long,
    val profileBadge: String?,
)

/** Everything the apps provider needs from the system, behind one seam so it can be faked in tests. */
interface AppsSource {
    /** Launchable activities across every profile the app can see. Slow: call off the main thread. */
    fun loadApps(): List<AppEntry>

    /** Shortcut access is only granted to the default launcher, so this is usually false. */
    fun hasShortcutAccess(): Boolean

    fun loadShortcuts(): List<ShortcutEntry>

    /** Emits whenever packages or shortcuts change in any profile. */
    fun changes(): Flow<Unit>
}
