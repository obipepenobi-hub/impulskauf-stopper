package com.liam.kaptalismusaufhalter.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

/** The instant-warning path: a settings change while the protection is running must be noticed. */
@RunWith(RobolectricTestRunner::class)
class SecurityMonitorTest {

    private lateinit var context: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var monitor: SecurityMonitor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        monitor = SecurityMonitor(context, scope)
    }

    @After
    fun tearDown() {
        monitor.stop()
        scope.cancel()
    }

    private fun service(pkg: String, label: String, capabilities: Int, flags: Int = 0): AccessibilityServiceInfo {
        val serviceInfo = ServiceInfo().apply {
            packageName = pkg
            name = "$pkg.Service"
            nonLocalizedLabel = label
            applicationInfo = ApplicationInfo().apply { packageName = pkg; this.flags = flags }
        }
        val resolve = ResolveInfo().apply { this.serviceInfo = serviceInfo; nonLocalizedLabel = label }
        return AccessibilityServiceInfo().also {
            ReflectionHelpers.setField(it, "mResolveInfo", resolve)
            ReflectionHelpers.setField(it, "mCapabilities", capabilities)
        }
    }

    private fun enable(vararg infos: AccessibilityServiceInfo) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabledAccessibilityServiceList(infos.toList())
    }

    /** ContentObserver callbacks run on the main looper, so keep it moving while we wait for the IO scan. */
    private fun awaitUntil(timeoutMs: Long = 8_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(25)
        }
        throw AssertionError("condition not met within ${timeoutMs}ms")
    }

    @Test
    fun `starting the monitor scans immediately`() {
        enable(service("com.google.android.marvin.talkback", "TalkBack", 0x01, ApplicationInfo.FLAG_SYSTEM))

        monitor.start()

        awaitUntil { SecurityPrefs(context).loadGuardState().initialized }
        assertTrue(SecurityPrefs(context).lastScanAt > 0)
    }

    @Test
    fun `a change of the enabled accessibility services triggers a scan and a warning`() {
        enable(service("com.google.android.marvin.talkback", "TalkBack", 0x01, ApplicationInfo.FLAG_SYSTEM))
        monitor.start()
        awaitUntil { SecurityPrefs(context).loadGuardState().initialized }
        assertTrue(SecurityLog(context).all().isEmpty())

        // a trojan gets switched on: the system setting changes and the service list now includes it
        enable(
            service("com.google.android.marvin.talkback", "TalkBack", 0x01, ApplicationInfo.FLAG_SYSTEM),
            service("com.evil.flashlight", "Taschenlampe Pro", 0x81)
        )
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.evil.flashlight/com.evil.flashlight.Service"
        )

        awaitUntil { SecurityLog(context).all().isNotEmpty() }
        val event = SecurityLog(context).all().single()
        assertEquals(Severity.CRITICAL, event.severity)
        assertTrue(event.title.contains("Taschenlampe Pro"))
    }

    @Test
    fun `after stopping, settings changes are no longer watched`() {
        enable(service("com.google.android.marvin.talkback", "TalkBack", 0x01, ApplicationInfo.FLAG_SYSTEM))
        monitor.start()
        awaitUntil { SecurityPrefs(context).loadGuardState().initialized }
        monitor.stop()

        enable(service("com.evil.flashlight", "Taschenlampe Pro", 0x81))
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "com.evil.flashlight/com.evil.flashlight.Service"
        )
        // give an (unexpected) debounced scan more than enough time to show up
        Thread.sleep(2_500)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(SecurityLog(context).all().isEmpty())
    }
}
