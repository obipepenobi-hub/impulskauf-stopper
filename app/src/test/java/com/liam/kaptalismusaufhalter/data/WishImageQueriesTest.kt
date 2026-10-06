package com.liam.kaptalismusaufhalter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.liam.kaptalismusaufhalter.security.WishImageStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The DB side of "no screenshot outlives its wish": queries + the startup sweep that uses them. */
@RunWith(RobolectricTestRunner::class)
class WishImageQueriesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun wish(name: String, imagePath: String?) = Wish(
        name = name, price = 10.0, imageUrl = imagePath,
        createdAt = 1, unlockAt = 2, status = WishStatus.PENDING
    )

    @Test
    fun `allImagePaths returns only wishes that have a photo`() = runBlocking {
        db.wishDao().insert(wish("a", "/x/a.enc"))
        db.wishDao().insert(wish("b", null))
        db.wishDao().insert(wish("c", "/x/c.enc"))

        assertEquals(setOf("/x/a.enc", "/x/c.enc"), db.wishDao().allImagePaths().toSet())
    }

    @Test
    fun `clearImagePaths removes every photo reference but keeps the wishes`() = runBlocking {
        val id = db.wishDao().insert(wish("a", "/x/a.enc"))
        db.wishDao().insert(wish("b", "/x/b.enc"))

        db.wishDao().clearImagePaths()

        assertTrue(db.wishDao().allImagePaths().isEmpty())
        assertNull(db.wishDao().getById(id)?.imageUrl)
        assertEquals("a", db.wishDao().getById(id)?.name)
    }

    @Test
    fun `the startup sweep keeps photos of existing wishes and removes the rest`() = runBlocking {
        val dir = File(context.filesDir, "wish_images").apply { mkdirs() }
        val referenced = File(dir, "wish_1.jpg").apply { writeBytes(byteArrayOf(1)) }
        val orphan = File(dir, "wish_2.jpg").apply { writeBytes(byteArrayOf(2)) }
        val longAgo = System.currentTimeMillis() - 3 * 60 * 60 * 1000L
        referenced.setLastModified(longAgo)
        orphan.setLastModified(longAgo)
        db.wishDao().insert(wish("a", referenced.absolutePath))

        WishImageStore.sweepOrphans(context, db.wishDao().allImagePaths().toSet())

        assertTrue(referenced.exists())
        assertFalse(orphan.exists())
    }
}
