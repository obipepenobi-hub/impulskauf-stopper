package com.liam.kaptalismusaufhalter.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.liam.kaptalismusaufhalter.security.SecurityGuard
import java.util.concurrent.TimeUnit

/**
 * Safety net for the instant detection inside the accessibility service: if that service isn't
 * running (never enabled, or killed by the system) a new screen-reading app would otherwise go
 * unnoticed until the app is next opened.
 */
class SecurityScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SecurityGuard.scan(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "security_scan"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SecurityScanWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
