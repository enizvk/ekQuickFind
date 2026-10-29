package com.ekquickfind.providers.apps

import com.ekquickfind.data.AppLabelDao
import com.ekquickfind.data.AppLabelEntity

interface AppLabelCache {
    suspend fun read(): List<AppEntry>

    suspend fun write(apps: List<AppEntry>)
}

class RoomAppLabelCache(
    private val dao: AppLabelDao,
) : AppLabelCache {
    override suspend fun read() =
        dao.all().map {
            AppEntry(it.packageName, it.activityName, it.label, it.userSerial, it.profileBadge, it.isSystemApp)
        }

    override suspend fun write(apps: List<AppEntry>) =
        dao.replaceAll(
            apps.map { AppLabelEntity(it.packageName, it.activityName, it.userSerial, it.label, it.profileBadge, it.isSystemApp) },
        )
}
