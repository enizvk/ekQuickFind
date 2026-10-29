package com.ekquickfind.app.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.ParcelFileDescriptor
import com.ekquickfind.data.EkDatabase
import com.ekquickfind.data.prefs.UserPreferencesSerializer
import java.io.File

/** Reads the backup opt-in straight from the preferences file; the agent runs without the app's DI graph. */
object BackupPolicy {
    fun includeHistory(filesDir: File): Boolean =
        runCatching {
            val file = File(filesDir, "datastore/preferences.json")
            file.exists() && UserPreferencesSerializer.decode(file.readText()).backupHistory
        }.getOrDefault(false)
}

/**
 * Auto Backup with one twist: the rules file always excludes the history database, and this agent
 * adds it back only when the user turned on history backup in settings.
 */
class EkBackupAgent : BackupAgent() {
    override fun onFullBackup(data: FullBackupDataOutput) {
        super.onFullBackup(data)
        if (!BackupPolicy.includeHistory(filesDir)) return
        val db = getDatabasePath(EkDatabase.NAME)
        listOf(db, File("${db.path}-wal"), File("${db.path}-shm")).filter { it.exists() }.forEach { fullBackupFile(it, data) }
    }

    // Key/value backup isn't used; Auto Backup handles everything through onFullBackup.
    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit
}
