package com.liam.kaptalismusaufhalter.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liam.kaptalismusaufhalter.BuildConfig
import com.liam.kaptalismusaufhalter.update.UpdateChecker
import java.util.concurrent.TimeUnit

/**
 * Runs once a day in the background so a new release is noticed even if the app isn't
 * opened - the in-app check in MainActivity only fires while the app is actually running.
 * One request/day is nowhere near GitHub's 60/h unauthenticated rate limit.
 */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val info = UpdateChecker.checkForUpdate(
            BuildConfig.UPDATE_REPO_OWNER,
            BuildConfig.UPDATE_REPO_NAME,
            BuildConfig.VERSION_NAME
        ) ?: return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_VERSION, null)
        if (lastNotified == info.versionName) return Result.success()

        NotificationHelper.notifyUpdateAvailable(applicationContext, info.versionName)
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, info.versionName).apply()
        return Result.success()
    }

    companion object {
        private const val PREFS = "update_check_worker"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private const val UNIQUE_WORK_NAME = "update_check"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
