package com.liam.kaptalismusaufhalter.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class WishImageStoreTest {

    private lateinit var context: Context
    private val originalCipher = WishImageStore.cipher

    // The real cipher is bound to the Android Keystore, which doesn't exist on the JVM - same AES-GCM
    // code, test key.
    private val testCipher = AesGcmImageCipher { SecretKeySpec(ByteArray(32) { it.toByte() }, "AES") }

    private val photo = "pretend-this-is-a-jpeg-of-a-checkout-page".toByteArray()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WishImageStore.cipher = testCipher
    }

    @After
    fun tearDown() {
        WishImageStore.cipher = originalCipher
    }

    @Test
    fun `saved photos are encrypted on disk and read back intact`() {
        val path = WishImageStore.save(context, photo)!!

        assertTrue(path.endsWith(".enc"))
        val onDisk = File(path).readBytes()
        assertFalse("plaintext must not be on disk", String(onDisk).contains("checkout-page"))
        assertArrayEquals(photo, WishImageStore.readBytes(context, path))
    }

    @Test
    fun `every save uses a fresh IV so equal photos do not produce equal files`() {
        val a = File(WishImageStore.save(context, photo)!!).readBytes()
        Thread.sleep(2) // distinct file names
        val b = File(WishImageStore.save(context, photo)!!).readBytes()

        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `a tampered file is rejected instead of being decoded`() {
        val path = WishImageStore.save(context, photo)!!
        val file = File(path)
        val bytes = file.readBytes()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 0x01).toByte()
        file.writeBytes(bytes)

        assertNull(WishImageStore.readBytes(context, path))
    }

    @Test
    fun `if encryption is unavailable the photo is still saved privately and the fallback is logged once`() {
        WishImageStore.cipher = object : ImageCipher {
            override fun encrypt(plain: ByteArray): ByteArray = throw IllegalStateException("no keystore")
            override fun decrypt(blob: ByteArray): ByteArray = throw IllegalStateException("no keystore")
        }

        val first = WishImageStore.save(context, photo)!!
        Thread.sleep(2)
        WishImageStore.save(context, photo)

        assertTrue(first.endsWith(".jpg"))
        assertArrayEquals(photo, WishImageStore.readBytes(context, first))
        val notes = SecurityLog(context).all().filter { it.kind == EventKind.INFO }
        assertEquals(1, notes.size)
        assertEquals(Severity.INFO, notes.single().severity)
    }

    @Test
    fun `legacy plain jpegs from older versions are still readable`() {
        val dir = File(context.filesDir, "wish_images").apply { mkdirs() }
        val legacy = File(dir, "wish_123.jpg").apply { writeBytes(photo) }

        assertArrayEquals(photo, WishImageStore.readBytes(context, legacy.absolutePath))
    }

    @Test
    fun `delete removes the photo`() {
        val path = WishImageStore.save(context, photo)!!
        WishImageStore.delete(context, path)

        assertFalse(File(path).exists())
        assertNull(WishImageStore.readBytes(context, path))
    }

    @Test
    fun `delete and read refuse paths outside the photo directory`() {
        val outside = File(context.cacheDir, "precious.txt").apply { writeText("keep me") }
        WishImageStore.save(context, photo) // make sure the store dir exists

        WishImageStore.delete(context, outside.absolutePath)
        assertTrue("file outside the store must survive", outside.exists())
        assertNull(WishImageStore.readBytes(context, outside.absolutePath))

        val traversal = File(context.filesDir, "wish_images/../../cache/precious.txt").path
        WishImageStore.delete(context, traversal)
        assertTrue(outside.exists())
    }

    @Test
    fun `null and blank paths are ignored`() {
        WishImageStore.delete(context, null)
        WishImageStore.delete(context, "")
        assertNull(WishImageStore.readBytes(context, null))
        assertNull(WishImageStore.readBytes(context, "   "))
    }

    @Test
    fun `sweep removes old unreferenced photos but keeps referenced and fresh ones`() {
        val keep = File(WishImageStore.save(context, photo)!!)
        Thread.sleep(2)
        val orphan = File(WishImageStore.save(context, photo)!!)
        Thread.sleep(2)
        val fresh = File(WishImageStore.save(context, photo)!!)
        val twoHoursAgo = System.currentTimeMillis() - 2 * 60 * 60 * 1000L
        keep.setLastModified(twoHoursAgo)
        orphan.setLastModified(twoHoursAgo)

        val removed = WishImageStore.sweepOrphans(context, referencedPaths = setOf(keep.absolutePath))

        assertEquals(1, removed)
        assertTrue(keep.exists())
        assertFalse(orphan.exists())
        assertTrue("a photo captured seconds ago must not be swept", fresh.exists())
    }

    @Test
    fun `delete all empties the store`() {
        WishImageStore.save(context, photo)
        Thread.sleep(2)
        WishImageStore.save(context, photo)

        WishImageStore.deleteAll(context)

        assertEquals(0, File(context.filesDir, "wish_images").listFiles()?.size ?: 0)
        assertNotNull(context.filesDir)
    }
}
