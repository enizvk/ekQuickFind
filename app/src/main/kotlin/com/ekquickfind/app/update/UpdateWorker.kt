package com.ekquickfind.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ekquickfind.data.prefs.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Daily check on Wi-Fi; installs a newer version when "Update automatically" is on. */
class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun updates(): Updates

        fun prefs(): PreferencesRepository
    }

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)
        if (!deps
                .prefs()
                .preferences
                .first()
                .autoUpdate
        ) {
            return Result.success()
        }
        val updates = deps.updates()
        val check = updates.check()
        if (check is UpdateCheck.Available) {
            // Without the install permission the owner has to step in, so just say an update is there.
            if (updates.canInstall()) updates.install(check.release) else UpdateNotifier.available(applicationContext, check.release)
        }
        // Failures wait for tomorrow's run rather than retrying in a loop.
        return Result.success()
    }

    companion object {
        private const val NAME = "update-check"

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                    .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED, requiresBatteryNotLow = true))
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
