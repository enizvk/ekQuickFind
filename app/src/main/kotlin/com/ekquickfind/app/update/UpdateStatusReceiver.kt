package com.ekquickfind.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives the package installer's result for an update session. */
@AndroidEntryPoint
class UpdateStatusReceiver : BroadcastReceiver() {
    @Inject lateinit var updates: Updates

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        updates.onInstallStatus(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE), confirm)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION || confirm == null) return
        // In front of the owner the dialog can open now; in the background a notification waits for a tap.
        if (updates.uiVisible) {
            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            UpdateNotifier.confirm(context, confirm)
        }
    }
}
