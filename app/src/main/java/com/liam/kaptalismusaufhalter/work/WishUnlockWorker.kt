package com.liam.kaptalismusaufhalter.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.liam.kaptalismusaufhalter.data.AppDatabase
import java.util.concurrent.TimeUnit

class WishUnlockWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val wishId = inputData.getLong(KEY_WISH_ID, -1L)
        if (wishId == -1L) return Result.failure()

        val dao = AppDatabase.getInstance(applicationContext).wishDao()
        val wish = dao.getById(wishId) ?: return Result.success()

        if (wish.status.name == "PENDING") {
            NotificationHelper.notifyWishReady(applicationContext, wish.id, wish.name)
        }
        return Result.success()
    }

    companion object {
        private const val KEY_WISH_ID = "wish_id"

        fun schedule(context: Context, wishId: Long, unlockAt: Long) {
            val delayMillis = (unlockAt - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<WishUnlockWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_WISH_ID to wishId))
                .addTag(wishTag(wishId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                wishTag(wishId),
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun wishTag(wishId: Long) = "wish_unlock_$wishId"
    }
}
