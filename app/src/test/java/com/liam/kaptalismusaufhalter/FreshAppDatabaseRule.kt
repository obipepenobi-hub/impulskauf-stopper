package com.liam.kaptalismusaufhalter

import com.liam.kaptalismusaufhalter.data.AppDatabase
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * AppDatabase is a process-wide singleton. In production there is one Application for the whole
 * process, but Robolectric creates a fresh Application per test inside one JVM, so a singleton
 * created by an earlier test would point at that test's already-torn-down data directory.
 */
class FreshAppDatabaseRule : TestWatcher() {
    override fun starting(description: Description?) = reset()
    override fun finished(description: Description?) = reset()

    private fun reset() {
        val field = AppDatabase::class.java.getDeclaredField("instance").apply { isAccessible = true }
        (field.get(null) as? AppDatabase)?.let { runCatching { it.close() } }
        field.set(null, null)
    }
}
