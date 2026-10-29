package com.ekquickfind.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File

/** Installs a downloaded APK over this app through the system package installer. */
class UpdateInstaller(
    private val context: Context,
) {
    /** The owner has to allow "Install unknown apps" for ekQuickFind once. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Blocking; the result arrives later in [UpdateStatusReceiver]. */
    fun install(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                // Android skips the confirmation when it can (an app updating itself); otherwise it asks once.
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val status = Intent(context, UpdateStatusReceiver::class.java)
            // Mutable so the installer can add its status extras; the intent is explicit, so that's allowed.
            val pending =
                PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    status,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            session.commit(pending.intentSender)
        }
    }
}
