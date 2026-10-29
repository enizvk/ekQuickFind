package com.ekquickfind.app.photos

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ekquickfind.app.R
import com.ekquickfind.data.prefs.PreferencesRepository
import com.ekquickfind.providers.photos.PhotoTagIndexer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Tags photos that are new or edited since the last run. The first full run is a foreground job with a
 * progress notification, so it keeps going after the app closes; stopping is fine, the next run continues.
 */
class PhotoTagWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun indexer(): PhotoTagIndexer

        fun prefs(): PreferencesRepository
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        if (!deps
                .prefs()
                .preferences
                .first()
                .photoTags
        ) {
            return Result.success()
        }
        val canRead = applicationContext.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        if (!canRead) return Result.success()
        val foreground = inputData.getBoolean(KEY_FOREGROUND, false) && tryForeground(0, 0)
        var lastShown = 0
        deps.indexer().run { done, total ->
            // Refreshing the notification for every photo would be wasteful.
            if (foreground && (done - lastShown >= NOTIFY_EVERY || done == total)) {
                lastShown = done
                tryForeground(done, total)
            }
        }
        return Result.success()
    }

    // Expedited work asks for this on older paths; keep it in step with the real notification.
    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(0, 0)

    /** False when Android won't allow a foreground job right now (for example started from the background). */
    private suspend fun tryForeground(
        done: Int,
        total: Int,
    ): Boolean =
        try {
            setForeground(foregroundInfo(done, total))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

    private fun foregroundInfo(
        done: Int,
        total: Int,
    ): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Photo tagging", NotificationManager.IMPORTANCE_LOW))
        val notification =
            Notification
                .Builder(applicationContext, CHANNEL)
                .setSmallIcon(R.drawable.ic_tile_search)
                .setContentTitle("Tagging photos")
                .setContentText(if (total > 0) "%,d of %,d".format(done, total) else "Getting ready")
                .setProgress(total, done, total == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        private const val RUN = "photo-tags-run"
        private const val DAILY = "photo-tags-daily"

        // Older name; runs queued under it may be stuck behind a battery constraint.
        private const val LEGACY_NOW = "photo-tags-now"
        private const val KEY_FOREGROUND = "foreground"
        private const val CHANNEL = "photo-tagging"
        private const val NOTIFICATION_ID = 7101
        private const val NOTIFY_EVERY = 25

        /**
         * Tags what's left right away as a foreground job, then keeps up daily while charging.
         * [restart] replaces a queued run (the owner just turned tagging on); otherwise a running one is kept.
         */
        fun start(
            context: Context,
            restart: Boolean = false,
        ) {
            val work = WorkManager.getInstance(context)
            work.cancelUniqueWork(LEGACY_NOW)
            val run =
                OneTimeWorkRequestBuilder<PhotoTagWorker>()
                    // Expedited starts now instead of waiting for a battery-friendly moment.
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(androidx.work.workDataOf(KEY_FOREGROUND to true))
                    .build()
            work.enqueueUniqueWork(RUN, if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, run)
            work.enqueueUniquePeriodicWork(
                DAILY,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PhotoTagWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints(requiresCharging = true, requiresBatteryNotLow = true))
                    .build(),
            )
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(LEGACY_NOW)
                cancelUniqueWork(RUN)
                cancelUniqueWork(DAILY)
            }
        }
    }
}
