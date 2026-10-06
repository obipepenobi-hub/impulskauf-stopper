package com.liam.kaptalismusaufhalter.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

/**
 * End-to-end tests of the warning system on a simulated device: real scanner, real evaluator,
 * real log and real notifications - only the list of "enabled" accessibility services is faked.
 */
@RunWith(RobolectricTestRunner::class)
class SecurityGuardTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun accessibilityInfo(
        pkg: String,
        label: String,
        capabilities: Int = 0x01,
        appFlags: Int = 0
    ): AccessibilityServiceInfo {
        val service = ServiceInfo().apply {
            packageName = pkg
            name = "$pkg.Service"
            nonLocalizedLabel = label
            applicationInfo = ApplicationInfo().apply {
                packageName = pkg
                flags = appFlags
                nonLocalizedLabel = label
            }
        }
        val resolve = ResolveInfo().apply {
            serviceInfo = service
            nonLocalizedLabel = label
        }
        return AccessibilityServiceInfo().also {
            ReflectionHelpers.setField(it, "mResolveInfo", resolve)
            ReflectionHelpers.setField(it, "mCapabilities", capabilities)
        }
    }

    private val talkback get() = accessibilityInfo(
        "com.google.android.marvin.talkback", "TalkBack", appFlags = ApplicationInfo.FLAG_SYSTEM
    )
    private val spy get() = accessibilityInfo("com.evil.flashlight", "Taschenlampe Pro", capabilities = 0x01 or 0x80)

    private fun enableServices(vararg infos: AccessibilityServiceInfo) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabledAccessibilityServiceList(infos.toList())
    }

    private fun notifications(): List<Notification> =
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications

    private fun scan() = SecurityGuard.scanBlocking(context)

    @Test
    fun `first scan on a clean device is silent and records the baseline`() {
        enableServices(talkback)

        val summary = scan()

        assertNotNull(summary)
        assertEquals(0, summary!!.alertCount)
        assertTrue(SecurityPrefs(context).loadGuardState().initialized)
        assertTrue(SecurityLog(context).all().isEmpty())
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `enabling a sideloaded screen reader raises a critical alert and a notification`() {
        enableServices(talkback)
        scan()

        enableServices(talkback, spy)
        val summary = scan()

        assertEquals(1, summary!!.alertCount)
        val event = SecurityLog(context).all().single()
        assertEquals(Severity.CRITICAL, event.severity)
        assertEquals(EventKind.NEW_COMPONENT, event.kind)
        assertTrue(event.title.contains("Taschenlampe Pro"))
        assertTrue(event.details.contains("Screenshots aufnehmen"))
        assertEquals("com.evil.flashlight", event.packageName)

        val posted = notifications().single()
        assertEquals(event.title, posted.extras.getString(Notification.EXTRA_TITLE))
        assertNotNull(posted.contentIntent)
        assertEquals(1, posted.actions.size)
    }

    @Test
    fun `the same component does not alert again on later scans`() {
        enableServices(talkback)
        scan()
        enableServices(talkback, spy)
        scan()

        scan()
        scan()

        assertEquals(1, SecurityLog(context).all().size)
        assertEquals(1, notifications().size)
    }

    @Test
    fun `with the guard switched off events are still logged but nothing is posted`() {
        SecurityPrefs(context).guardEnabled = false
        enableServices(talkback)
        scan()

        enableServices(talkback, spy)
        scan()

        assertEquals(1, SecurityLog(context).all().size)
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `a service from the play store is a warning not a critical alert`() {
        val packageManager = context.packageManager
        shadowOf(packageManager).installPackage(
            PackageInfo().apply {
                packageName = "com.nice.autofill"
                applicationInfo = ApplicationInfo().apply { packageName = "com.nice.autofill" }
            }
        )
        shadowOf(packageManager).setInstallSourceInfo("com.nice.autofill", "com.android.vending", "com.android.vending")

        enableServices(talkback)
        scan()
        enableServices(talkback, accessibilityInfo("com.nice.autofill", "Nice Autofill"))
        scan()

        assertEquals(Severity.WARNING, SecurityLog(context).all().single().severity)
    }

    @Test
    fun `components the user marked as trusted stay silent`() {
        enableServices(talkback)
        scan()
        // ComponentName.flattenToShortString() abbreviates the class name when it starts with the package
        val key = "ACCESSIBILITY_SERVICE:com.evil.flashlight/.Service"
        SecurityPrefs(context).setTrusted(key, true)

        enableServices(talkback, spy)
        scan()

        assertTrue(SecurityLog(context).all().isEmpty())
        assertTrue(notifications().isEmpty())
    }

    @Test
    fun `a newly enabled notification listener is found through the system setting`() {
        shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = "com.evil.otpgrabber"
                applicationInfo = ApplicationInfo().apply { packageName = "com.evil.otpgrabber" }
            }
        )
        enableServices(talkback)
        scan()

        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "com.evil.otpgrabber/com.evil.otpgrabber.Listener"
        )
        scan()

        val event = SecurityLog(context).all().single()
        assertEquals(Severity.CRITICAL, event.severity)
        assertTrue(event.title.contains("Benachrichtigungs-Zugriff"))
        assertEquals("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS", event.settingsAction)
    }

    @Test
    fun `losing the accessibility access this app needs is reported after a grace period`() {
        val prefs = SecurityPrefs(context)
        prefs.accessibilityWasEnabled = true
        enableServices(talkback) // our own service is not in the enabled list any more

        scan()
        // first sighting only starts the clock - Android briefly reports the service as off during updates
        assertTrue(SecurityLog(context).all().isEmpty())
        assertTrue(prefs.accessibilityLostSince > 0)

        prefs.accessibilityLostSince = System.currentTimeMillis() - 3 * 60 * 1000L
        scan()

        val event = SecurityLog(context).all().single()
        assertEquals(EventKind.PROTECTION_DISABLED, event.kind)
        assertTrue(event.title.contains("Bedienungshilfen-Zugriff"))
        assertFalse(prefs.accessibilityWasEnabled)

        // reported once - not again on every later scan
        scan()
        assertEquals(1, SecurityLog(context).all().size)
    }

    @Test
    fun `a service list that cannot be read neither alerts nor forgets what was known`() {
        enableServices(talkback, spy)
        scan() // baseline: the spy is already there -> one HIGH_RISK_EXISTING alert
        val alertsAfterBaseline = SecurityLog(context).all().size

        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabledAccessibilityServiceList(null)
        val summary = scan()
        assertTrue(ComponentKind.ACCESSIBILITY_SERVICE in summary!!.failedKinds)

        enableServices(talkback, spy)
        scan()

        assertEquals(alertsAfterBaseline, SecurityLog(context).all().size)
    }

    @Test
    fun `tapjacking is reported once per window, not once per touch`() {
        SecurityGuard.reportTapjacking(context)
        SecurityGuard.reportTapjacking(context)
        SecurityGuard.reportTapjacking(context)

        val events = SecurityLog(context).all()
        assertEquals(1, events.size)
        assertEquals(EventKind.TAPJACKING, events.single().kind)
        assertEquals(1, notifications().size)
    }

    @Test
    fun `a rejected update is logged critical and notified`() {
        SecurityGuard.reportUpdateRejected(context, "Die Signatur des Updates passt nicht zur installierten App.")

        val event = SecurityLog(context).all().single()
        assertEquals(EventKind.UPDATE_REJECTED, event.kind)
        assertEquals(Severity.CRITICAL, event.severity)
        assertEquals(1, notifications().size)
    }

    @Test
    fun `a foreign launch attempt is logged as a warning`() {
        SecurityGuard.reportUnexpectedLaunch(context, "android-app://com.evil")

        val event = SecurityLog(context).all().single()
        assertEquals(EventKind.UNEXPECTED_LAUNCH, event.kind)
        assertTrue(event.details.contains("com.evil"))
    }
}
