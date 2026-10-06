package com.liam.kaptalismusaufhalter

import androidx.work.testing.WorkManagerTestInitHelper

/** The real app class, but with WorkManager initialised the way unit tests need (no manifest providers run). */
class TestImpulskaufApp : ImpulskaufApp() {
    override fun onCreate() {
        WorkManagerTestInitHelper.initializeTestWorkManager(this)
        super.onCreate()
    }
}
