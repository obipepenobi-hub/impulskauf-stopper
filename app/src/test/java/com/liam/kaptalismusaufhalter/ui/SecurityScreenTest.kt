package com.liam.kaptalismusaufhalter.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import com.liam.kaptalismusaufhalter.FreshAppDatabaseRule
import com.liam.kaptalismusaufhalter.security.EventKind
import com.liam.kaptalismusaufhalter.security.SecurityEvent
import com.liam.kaptalismusaufhalter.security.SecurityLog
import com.liam.kaptalismusaufhalter.security.SecurityPrefs
import com.liam.kaptalismusaufhalter.security.Severity
import com.liam.kaptalismusaufhalter.security.WishImageStore
import com.liam.kaptalismusaufhalter.ui.screens.security.SecurityScreen
import com.liam.kaptalismusaufhalter.ui.screens.security.SecurityViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import java.io.File

/** Renders the real Sicherheit screen on a simulated device and drives it like a user would. */
@RunWith(RobolectricTestRunner::class)
class SecurityScreenTest {

    @get:Rule(order = 0)
    val freshDatabase = FreshAppDatabaseRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun spyService(): AccessibilityServiceInfo {
        val service = ServiceInfo().apply {
            packageName = "com.evil.flashlight"
            name = "com.evil.flashlight.Service"
            nonLocalizedLabel = "Taschenlampe Pro"
            applicationInfo = ApplicationInfo().apply { packageName = "com.evil.flashlight" }
        }
        val resolve = ResolveInfo().apply {
            serviceInfo = service
            nonLocalizedLabel = "Taschenlampe Pro"
        }
        return AccessibilityServiceInfo().also {
            ReflectionHelpers.setField(it, "mResolveInfo", resolve)
            ReflectionHelpers.setField(it, "mCapabilities", 0x01 or 0x80)
        }
    }

    private fun enable(vararg services: AccessibilityServiceInfo) {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        shadowOf(manager).setEnabledAccessibilityServiceList(services.toList())
    }

    /**
     * Shows the screen and waits until the first device scan has finished and been rendered.
     *
     * The ViewModel is created explicitly: the default `viewModel()` factory caches the *first*
     * Application it ever saw in a static, which across Robolectric tests (one fresh Application
     * each) would write to a previous test's preferences. Production has a single Application.
     */
    private fun showScreen() {
        val viewModel = SecurityViewModel(context as Application)
        compose.setContent { SecurityScreen(onBack = {}, viewModel = viewModel) }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Zuletzt geprüft", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    /** Scrolls the lazy list until [text] is composed (items off screen don't exist in the tree). */
    private fun scrollTo(text: String) {
        val list = compose.onNode(hasScrollAction())
        for (index in 0 until 40) {
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            runCatching { list.performScrollToIndex(index) }
            compose.waitForIdle()
        }
        compose.onAllNodesWithText(text, substring = true)[0].assertExists("'$text' never appeared in the list")
    }

    @Test
    fun `shows the status, both switches and what is always protected`() {
        enable()
        showScreen()

        compose.onNodeWithText("Sicherheit").assertExists()
        compose.onNodeWithText("Screenshot-Schutz").assertExists()
        compose.onNodeWithText("Warnungen bei verdächtigen Apps").assertExists()
        scrollTo("Das ist immer aktiv")
        compose.onNodeWithText("Das ist immer aktiv").assertExists()
    }

    @Test
    fun `a risky app on the device is listed with its risk and what it can do`() {
        enable(spyService())
        showScreen()

        compose.waitUntil(5_000) { compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty() }
        scrollTo("Taschenlampe Pro")
        compose.onNodeWithText("Taschenlampe Pro").assertExists()
        compose.onNodeWithText("Risiko: hoch").assertExists()
        // mentioned both in the device row and in the log entry the same scan produced
        compose.onAllNodesWithText("Screenshots aufnehmen", substring = true).onFirst().assertExists()
    }

    @Test
    fun `warns when notifications are blocked because then warnings would go unseen`() {
        enable()
        shadowOf(context.getSystemService(android.app.NotificationManager::class.java)).setNotificationsEnabled(false)
        showScreen()

        compose.onNodeWithText("Benachrichtigungen sind ausgeschaltet", substring = true).assertExists()
        compose.onNodeWithText("Benachrichtigungen einschalten").assertExists()
    }

    @Test
    fun `stays quiet about notifications when they are allowed`() {
        enable()
        showScreen()

        assertTrue(
            compose.onAllNodesWithText("Benachrichtigungen sind ausgeschaltet", substring = true)
                .fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun `the screenshot protection switch persists the setting`() {
        enable()
        showScreen()
        assertTrue(SecurityPrefs(context).screenProtection)

        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.waitForIdle()

        assertFalse(SecurityPrefs(context).screenProtection)
    }

    @Test
    fun `the photo switch turns screenshot capture off`() {
        enable()
        showScreen()
        assertTrue(SecurityPrefs(context).capturePhotos)

        // order on screen: screenshot protection, product photos, warnings
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.waitForIdle()

        assertFalse(SecurityPrefs(context).capturePhotos)
        assertTrue(SecurityPrefs(context).screenProtection)
        assertTrue(SecurityPrefs(context).guardEnabled)
    }

    @Test
    fun `marking an app as trusted silences it`() {
        enable(spyService())
        showScreen()
        scrollTo("Taschenlampe Pro")
        compose.onNodeWithText("Taschenlampe Pro").assertExists()

        scrollTo("Als vertrauenswürdig markieren")
        compose.onNodeWithText("Als vertrauenswürdig markieren").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("vertraut").assertExists()
        assertTrue(
            SecurityPrefs(context).loadGuardState().trustedKeys
                .contains("ACCESSIBILITY_SERVICE:com.evil.flashlight/.Service")
        )
    }

    @Test
    fun `logged events are shown and can be cleared`() {
        enable()
        SecurityLog(context).add(
            SecurityEvent(
                id = 1, timestamp = System.currentTimeMillis(), severity = Severity.CRITICAL,
                kind = EventKind.TAPJACKING, title = "Popup wurde von einem anderen Fenster überdeckt",
                details = "Details"
            )
        )
        showScreen()

        scrollTo("Popup wurde von einem anderen Fenster überdeckt")
        compose.onNodeWithText("Popup wurde von einem anderen Fenster überdeckt").assertExists()
        compose.onNodeWithText("DRINGEND").assertExists()

        scrollTo("Löschen")
        compose.onNodeWithText("Löschen").performClick()
        compose.waitForIdle()

        assertTrue(SecurityLog(context).all().isEmpty())
        compose.onNodeWithText("Keine Auffälligkeiten aufgezeichnet.").assertExists()
    }

    @Test
    fun `deleting all photos removes the files`() {
        enable()
        val dir = File(context.filesDir, "wish_images").apply { mkdirs() }
        val photo = File(dir, "wish_1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        showScreen()

        scrollTo("Alle Fotos jetzt löschen")
        compose.onNodeWithText("Alle Fotos jetzt löschen").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Alle Fotos gelöscht.").fetchSemanticsNodes().isNotEmpty() }

        assertFalse(photo.exists())
        assertNull(WishImageStore.readBytes(context, photo.absolutePath))
    }
}
