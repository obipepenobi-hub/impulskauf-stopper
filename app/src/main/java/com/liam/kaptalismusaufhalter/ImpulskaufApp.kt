package com.liam.kaptalismusaufhalter

import android.app.Application
import com.liam.kaptalismusaufhalter.data.AppDatabase
import com.liam.kaptalismusaufhalter.data.WishRepository
import com.liam.kaptalismusaufhalter.work.NotificationHelper
import com.liam.kaptalismusaufhalter.work.UpdateCheckWorker

class ImpulskaufApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val wishRepository: WishRepository by lazy {
        WishRepository(this, database.wishDao(), database.settingsDao())
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        UpdateCheckWorker.schedulePeriodic(this)
    }
}
