package com.liam.kaptalismusaufhalter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.liam.kaptalismusaufhalter.security.EventKind
import com.liam.kaptalismusaufhalter.security.IntentGuard
import com.liam.kaptalismusaufhalter.security.SecurityLog
import com.liam.kaptalismusaufhalter.security.SecurityPrefs
import com.liam.kaptalismusaufhalter.work.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The real MainActivity in a real lifecycle: is the window locked down, and who may steer it? */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestImpulskaufApp::class)
class MainActivityHardeningTest {

    @get:Rule
    val freshDatabase = FreshAppDatabaseRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun launcherIntent() = Intent(context, MainActivity::class.java)

    private fun isSecure(flags: Int) = flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun `the app window blocks screenshots and obscured touches by default`() {
        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(isSecure(activity.window.attributes.flags))
                assertTrue(activity.window.decorView.filterTouchesWhenObscured)
            }
        }
    }

    @Test
    fun `screenshots are allowed again when the user switches the protection off`() {
        SecurityPrefs(context).screenProtection = false

        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(isSecure(activity.window.attributes.flags))
                // tapjacking protection is not optional
                assertTrue(activity.window.decorView.filterTouchesWhenObscured)
            }
        }
    }

    @Test
    fun `toggling the setting while the app is open takes effect immediately`() {
        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            scenario.onActivity { activity -> assertTrue(isSecure(activity.window.attributes.flags)) }

            SecurityPrefs(context).screenProtection = false
            scenario.onActivity { activity -> assertFalse(isSecure(activity.window.attributes.flags)) }

            SecurityPrefs(context).screenProtection = true
            scenario.onActivity { activity -> assertTrue(isSecure(activity.window.attributes.flags)) }
        }
    }

    @Test
    fun `a navigation intent from another app without the secret is ignored and reported`() {
        val foreign = launcherIntent()
            .putExtra(NotificationHelper.EXTRA_WISH_ID, 1L)
            .putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.evil.app"))

        ActivityScenario.launch<MainActivity>(foreign).use { }

        val events = SecurityLog(context).all().filter { it.kind == EventKind.UNEXPECTED_LAUNCH }
        assertEquals(1, events.size)
        assertTrue(events.single().details.contains("com.evil.app"))
    }

    @Test
    fun `a stale token-less intent that claims to come from the app itself is ignored quietly`() {
        // e.g. a wish-ready notification posted by an older version before the token existed
        val stale = launcherIntent()
            .putExtra(NotificationHelper.EXTRA_WISH_ID, 1L)
            .putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://${context.packageName}"))

        ActivityScenario.launch<MainActivity>(stale).use { }

        assertTrue(SecurityLog(context).all().none { it.kind == EventKind.UNEXPECTED_LAUNCH })
    }

    @Test
    fun `an intent carrying the app's own secret is trusted and raises no alarm`() {
        val own = launcherIntent()
            .putExtra(NotificationHelper.EXTRA_OPEN_SECURITY, true)
            .putExtra(IntentGuard.EXTRA_NAV_TOKEN, IntentGuard.tokenFor(context))
            .putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.evil.app"))

        ActivityScenario.launch<MainActivity>(own).use { }

        assertTrue(SecurityLog(context).all().none { it.kind == EventKind.UNEXPECTED_LAUNCH })
    }
}
