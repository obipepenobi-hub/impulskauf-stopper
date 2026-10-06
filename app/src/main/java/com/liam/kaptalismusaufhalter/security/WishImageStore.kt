package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ImageCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
}

/** AES-256-GCM; the random IV is stored in front of the ciphertext. The key comes from [keyProvider]. */
class AesGcmImageCipher(private val keyProvider: () -> SecretKey) : ImageCipher {

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.isNotEmpty()) { "Empty blob" }
        val ivLength = blob[0].toInt() and 0xFF
        require(ivLength in 12..16 && blob.size > 1 + ivLength) { "Malformed blob" }
        val iv = blob.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(128, iv))
        return cipher.doFinal(blob, 1 + ivLength, blob.size - 1 - ivLength)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

/** Hardware-backed, non-exportable key from the Android Keystore: copied-off files are useless. */
internal object WishImageKey {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "impulskauf_wish_image_key"

    @Synchronized
    fun get(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}

/**
 * The only place product screenshots (cropped from other apps' screens) are written, read and
 * deleted. They live in private internal storage, are encrypted at rest, are excluded from every
 * backup, and are removed as soon as the wish they belong to is gone.
 *
 * If the Keystore is unavailable on some device, the photo is still saved (plain, but in the
 * app-private directory) rather than silently breaking the feature - the fallback is logged once.
 */
object WishImageStore {
    private const val DIR = "wish_images"
    internal const val ENCRYPTED_SUFFIX = ".enc"

    @Volatile
    internal var cipher: ImageCipher = AesGcmImageCipher { WishImageKey.get() }

    fun save(context: Context, jpegBytes: ByteArray): String? {
        val dir = storeDir(context).apply { mkdirs() }
        val stamp = System.currentTimeMillis()

        val encrypted = try {
            cipher.encrypt(jpegBytes)
        } catch (e: Exception) {
            noteEncryptionFallback(context)
            null
        }
        val file = File(dir, if (encrypted != null) "wish_$stamp$ENCRYPTED_SUFFIX" else "wish_$stamp.jpg")

        return try {
            file.outputStream().use { it.write(encrypted ?: jpegBytes) }
            restrictToOwner(file)
            file.absolutePath
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun decode(context: Context, path: String?): Bitmap? =
        readBytes(context, path)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    /** The plain JPEG bytes of a stored photo (decrypting if needed), or null if missing/tampered. */
    internal fun readBytes(context: Context, path: String?): ByteArray? {
        val file = resolveInsideStore(context, path) ?: return null
        return try {
            val raw = file.readBytes()
            if (file.name.endsWith(ENCRYPTED_SUFFIX)) cipher.decrypt(raw) else raw
        } catch (e: Exception) {
            null
        }
    }

    fun delete(context: Context, path: String?) {
        resolveInsideStore(context, path)?.delete()
    }

    fun deleteAll(context: Context) {
        storeDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Removes files no wish references any more (older than [minAgeMs], so a just-captured one survives). */
    fun sweepOrphans(context: Context, referencedPaths: Set<String>, minAgeMs: Long = 60 * 60 * 1000L): Int {
        val cutoff = System.currentTimeMillis() - minAgeMs
        var removed = 0
        storeDir(context).listFiles()?.forEach { file ->
            if (file.absolutePath !in referencedPaths && file.lastModified() < cutoff && file.delete()) removed++
        }
        return removed
    }

    private fun storeDir(context: Context) = File(context.applicationContext.filesDir, DIR)

    /**
     * Wish.imageUrl comes out of the database - never delete or open a path from there without
     * confirming it really is inside our own store (a restored/tampered DB must not be able to
     * point this at arbitrary files).
     */
    private fun resolveInsideStore(context: Context, path: String?): File? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path).canonicalFile
            val dir = storeDir(context).canonicalFile
            if (file.parentFile == dir && file.isFile) file else null
        } catch (e: Exception) {
            null
        }
    }

    private fun restrictToOwner(file: File) {
        file.setReadable(false, false)
        file.setReadable(true, true)
        file.setWritable(false, false)
        file.setWritable(true, true)
    }

    private fun noteEncryptionFallback(context: Context) {
        val prefs = SecurityPrefs(context)
        if (prefs.encryptionFallbackLogged) return
        prefs.encryptionFallbackLogged = true
        val log = SecurityLog(context)
        val now = System.currentTimeMillis()
        log.add(
            AlertFormatter.infoEvent(
                log.nextId(now), now,
                "Foto-Verschlüsselung nicht verfügbar",
                "Auf diesem Gerät ließ sich der Android-Schlüsselspeicher nicht nutzen. Produktfotos " +
                    "werden deshalb ohne zusätzliche Verschlüsselung gespeichert (weiterhin nur im " +
                    "privaten App-Speicher, nie in einem Backup)."
            )
        )
    }
}
