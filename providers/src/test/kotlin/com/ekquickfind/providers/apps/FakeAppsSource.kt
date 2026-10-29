package com.ekquickfind.providers.apps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAppsSource(
    var apps: List<AppEntry> = emptyList(),
    var shortcuts: List<ShortcutEntry> = emptyList(),
    var shortcutAccess: Boolean = false,
) : AppsSource {
    val changeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    var loads = 0
        private set

    override fun loadApps(): List<AppEntry> {
        loads++
        return apps
    }

    override fun hasShortcutAccess() = shortcutAccess

    override fun loadShortcuts() = if (shortcutAccess) shortcuts else emptyList()

    override fun changes(): Flow<Unit> = changeEvents
}

fun app(
    label: String,
    pkg: String = "com.example.${label.lowercase().replace(" ", "")}",
    badge: String? = null,
    serial: Long = if (badge == null) 0 else 10,
    system: Boolean = false,
) = AppEntry(pkg, "$pkg.Main", label, serial, badge, system)
