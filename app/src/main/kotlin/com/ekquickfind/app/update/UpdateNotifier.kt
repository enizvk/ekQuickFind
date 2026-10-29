package com.ekquickfind.app.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.ekquickfind.app.R
import com.ekquickfind.app.shortcuts.ShortcutsPublisher

/** Update notices for when the app isn't on screen; silently skipped if notifications aren't allowed. */
object UpdateNotifier {
    private const val CHANNEL = "updates"
    private const val ID = 7001

    fun confirm(
        context: Context,
        confirm: Intent,
    ) = post(
        context,
        "ekQuickFind update ready",
        "Tap to install",
        PendingIntent.getActivity(context, 1, confirm, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
    )

    fun available(
        context: Context,
        release: Release,
    ) = post(
        context,
        "ekQuickFind ${release.versionName} is available",
        "Open settings to install",
        PendingIntent.getActivity(
            context,
            2,
            Intent(ShortcutsPublisher.ACTION_OPEN_SETTINGS).setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        ),
    )

    // Permission is checked right below; only the direct build declares it, and only that build has an update source.
    @SuppressLint("NotificationPermission")
    private fun post(
        context: Context,
        title: String,
        text: String,
        tap: PendingIntent,
    ) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_DEFAULT))
        val notification =
            Notification
                .Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_tile_search)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build()
        manager.notify(ID, notification)
    }
}
