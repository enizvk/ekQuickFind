package com.ekquickfind.providers.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class LauncherAppsSource(
    context: Context,
) : AppsSource {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    override fun loadApps(): List<AppEntry> =
        launcherApps.profiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val badge = badgeFor(user)
            launcherApps.getActivityList(null, user).map { info ->
                val flags = info.applicationInfo.flags
                AppEntry(
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    label = info.label.toString(),
                    userSerial = serial,
                    profileBadge = badge,
                    isSystemApp = flags and ApplicationInfo.FLAG_SYSTEM != 0 && flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0,
                )
            }
        }

    override fun hasShortcutAccess(): Boolean = runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    override fun loadShortcuts(): List<ShortcutEntry> {
        if (!hasShortcutAccess()) return emptyList()
        val query =
            LauncherApps.ShortcutQuery().setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
            )
        return launcherApps.profiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val badge = badgeFor(user)
            val shortcuts: List<ShortcutInfo> = runCatching { launcherApps.getShortcuts(query, user) }.getOrNull().orEmpty()
            shortcuts.filter { it.isEnabled }.map { s ->
                ShortcutEntry(
                    packageName = s.`package`,
                    shortcutId = s.id,
                    label = (s.shortLabel ?: s.longLabel ?: s.id).toString(),
                    appLabel = appLabel(s.`package`, user),
                    userSerial = serial,
                    profileBadge = badge,
                )
            }
        }
    }

    override fun changes(): Flow<Unit> =
        callbackFlow {
            val callback =
                object : LauncherApps.Callback() {
                    override fun onPackageRemoved(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackageAdded(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackageChanged(
                        packageName: String,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackagesAvailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) {
                        trySend(Unit)
                    }

                    override fun onPackagesUnavailable(
                        packageNames: Array<out String>,
                        user: UserHandle,
                        replacing: Boolean,
                    ) {
                        trySend(Unit)
                    }

                    override fun onShortcutsChanged(
                        packageName: String,
                        shortcuts: MutableList<ShortcutInfo>,
                        user: UserHandle,
                    ) {
                        trySend(Unit)
                    }
                }
            launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
            awaitClose { launcherApps.unregisterCallback(callback) }
        }.conflate()

    private fun appLabel(
        packageName: String,
        user: UserHandle,
    ): String =
        launcherApps
            .getActivityList(packageName, user)
            .firstOrNull()
            ?.label
            ?.toString() ?: packageName

    private fun badgeFor(user: UserHandle): String? {
        if (user == Process.myUserHandle()) return null
        // Profile types are readable from Android 15 (private space); before that it's a work profile.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return "Work"
        val type = runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()
        return when (type) {
            UserManager.USER_TYPE_PROFILE_PRIVATE -> "Private"
            UserManager.USER_TYPE_PROFILE_CLONE -> "Clone"
            else -> "Work"
        }
    }
}
