package com.liam.kaptalismusaufhalter

import android.app.Application
import com.liam.kaptalismusaufhalter.data.AppDatabase
import com.liam.kaptalismusaufhalter.data.WishRepository
import com.liam.kaptalismusaufhalter.security.WishImageStore
import com.liam.kaptalismusaufhalter.work.NotificationHelper
import com.liam.kaptalismusaufhalter.work.SecurityScanWorker
import com.liam.kaptalismusaufhalter.work.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class ImpulskaufApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val wishRepository: WishRepository by lazy {
        WishRepository(this, database.wishDao(), database.settingsDao())
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        UpdateCheckWorker.schedulePeriodic(this)
        SecurityScanWorker.schedulePeriodic(this)

        // Product screenshots no wish references any more (cancelled/decided wishes from older
        // versions, interrupted saves) are privacy leftovers - sweep them on every start.
        appScope.launch {
            try {
                WishImageStore.sweepOrphans(this@ImpulskaufApp, database.wishDao().allImagePaths().toSet())
            } catch (e: Exception) {
                // best effort - next start tries again
            }
        }
    }
}
