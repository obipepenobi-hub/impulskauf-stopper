package com.liam.kaptalismusaufhalter.guard

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Grabs a product "photo" for a ripened wish by cropping the live screen instead of trying to
 * pull a specific ImageView's bitmap out of the accessibility tree (there's no general API for
 * that). [AccessibilityService.takeScreenshot] only exists from Android 11 (API 30) - on older
 * devices wishes created via the overlay simply have no photo, same as manually entered ones.
 */
object ScreenshotCapture {

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun capture(service: AccessibilityService): Bitmap? = suspendCancellableCoroutine { cont ->
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                service.mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    @SuppressLint("NewApi")
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                        result.hardwareBuffer.close()
                        val bitmap = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        if (cont.isActive) cont.resume(bitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            )
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    /**
     * Crops a horizontal strip around [titleBounds] rather than a tight box around just the
     * text - cart/checkout rows commonly show a product thumbnail to one side of the title at
     * the same height, so a full-width band centered on the title's row tends to include it
     * regardless of the shop's exact layout. Falls back to the full screenshot when no bounds
     * were found (e.g. the manual-entry fallback popup).
     */
    fun saveCropped(context: Context, bitmap: Bitmap, titleBounds: Rect?): String? {
        return try {
            val target = if (titleBounds != null && titleBounds.height() > 0) {
                val verticalPadding = (titleBounds.height() * 1.5).toInt().coerceAtLeast(60)
                Rect(
                    0,
                    (titleBounds.top - verticalPadding).coerceAtLeast(0),
                    bitmap.width,
                    (titleBounds.bottom + verticalPadding).coerceAtMost(bitmap.height)
                )
            } else {
                Rect(0, 0, bitmap.width, bitmap.height)
            }
            if (target.width() <= 0 || target.height() <= 0) return null

            val cropped = Bitmap.createBitmap(bitmap, target.left, target.top, target.width(), target.height())
            val dir = File(context.filesDir, "wish_images").apply { mkdirs() }
            val file = File(dir, "wish_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
