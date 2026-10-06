package com.liam.kaptalismusaufhalter.guard

import android.content.Context
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.SideEffect
import androidx.test.core.app.ApplicationProvider
import com.liam.kaptalismusaufhalter.security.EventKind
import com.liam.kaptalismusaufhalter.security.SecurityLog
import com.liam.kaptalismusaufhalter.security.SecurityPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWindowManagerImpl

/** The popup window is the part of the app other apps would attack - check it is really locked down. */
@RunWith(RobolectricTestRunner::class)
class OverlayControllerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun addedViews(): List<View> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return Shadow.extract<ShadowWindowManagerImpl>(windowManager).views
    }

    @Test
    fun `popup window is secure and filters obscured touches when screen protection is on`() {
        SecurityPrefs(context).screenProtection = true
        val controller = OverlayController(context)

        controller.show { }

        assertTrue(controller.isShowing())
        val root = addedViews().single()
        assertTrue("root must be the guarded container", root is GuardedFrameLayout)
        assertTrue(root.filterTouchesWhenObscured)
        val flags = (root.layoutParams as WindowManager.LayoutParams).flags
        assertTrue("FLAG_SECURE must be set", flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, (root.layoutParams as WindowManager.LayoutParams).type)

        controller.hide()
        assertFalse(controller.isShowing())
        assertTrue(addedViews().isEmpty())
    }

    @Test
    fun `popup window is not secure when the user turned screen protection off`() {
        SecurityPrefs(context).screenProtection = false
        val controller = OverlayController(context)

        controller.show { }

        val flags = (addedViews().single().layoutParams as WindowManager.LayoutParams).flags
        assertEquals(0, flags and WindowManager.LayoutParams.FLAG_SECURE)
        // tapjacking protection is not optional
        assertTrue(addedViews().single().filterTouchesWhenObscured)
        controller.hide()
    }

    @Test
    fun `the compose content actually composes inside the guarded container`() {
        // The popup used to be the window's root ComposeView; it now sits inside a container, and
        // Compose looks up its lifecycle on the window's root view - this catches that wiring breaking.
        val controller = OverlayController(context)
        var composed = false

        controller.show { SideEffect { composed = true } }
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue("content never composed", composed)
        controller.hide()
    }

    @Test
    fun `the back key closes the popup - the escape if a covering window blocks its buttons`() {
        val controller = OverlayController(context)
        var backs = 0
        controller.show(onBackPressed = { backs++; controller.hide() }) { }
        val root = addedViews().single()

        assertTrue(root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)))
        assertEquals("only the key release counts", 0, backs)
        assertTrue(root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)))

        assertEquals(1, backs)
        assertFalse(controller.isShowing())
    }

    @Test
    fun `other keys are not swallowed`() {
        val controller = OverlayController(context)
        controller.show(onBackPressed = { }) { }
        val root = addedViews().single()

        // not handled by the container itself (an empty popup has nothing that consumes it)
        assertFalse(root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)))
        controller.hide()
    }

    @Test
    fun `showing twice does not stack windows`() {
        val controller = OverlayController(context)
        controller.show { }
        controller.show { }

        assertEquals(1, addedViews().size)
        controller.hide()
    }

    @Test
    fun `touches through an obscuring window are reported, normal touches are not`() {
        val layout = GuardedFrameLayout(context)
        var reports = 0
        layout.onObscuredTouch = { reports++ }

        layout.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN, obscured = false))
        assertEquals(0, reports)

        layout.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN, obscured = true))
        assertEquals(1, reports)

        // only the initial DOWN of a gesture counts, not every MOVE/UP of the same touch
        layout.dispatchTouchEvent(touch(MotionEvent.ACTION_MOVE, obscured = true))
        layout.dispatchTouchEvent(touch(MotionEvent.ACTION_UP, obscured = true))
        assertEquals(1, reports)
    }

    @Test
    fun `an obscured touch on the real popup ends up in the security log`() {
        val controller = OverlayController(context)
        controller.show { }
        val root = addedViews().single()

        root.dispatchTouchEvent(touch(MotionEvent.ACTION_DOWN, obscured = true))

        assertTrue(SecurityLog(context).all().any { it.kind == EventKind.TAPJACKING })
        controller.hide()
    }

    private fun touch(action: Int, obscured: Boolean): MotionEvent {
        val properties = arrayOf(MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        })
        val coordinates = arrayOf(MotionEvent.PointerCoords().apply {
            x = 5f
            y = 5f
            pressure = 1f
            size = 1f
        })
        return MotionEvent.obtain(
            0L, 0L, action, 1, properties, coordinates, 0, 0, 1f, 1f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN,
            if (obscured) MotionEvent.FLAG_WINDOW_IS_OBSCURED else 0
        )
    }
}
