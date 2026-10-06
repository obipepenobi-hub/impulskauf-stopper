package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecurityStorageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun event(id: Long, severity: Severity = Severity.WARNING, timestamp: Long = id) = SecurityEvent(
        id = id,
        timestamp = timestamp,
        severity = severity,
        kind = EventKind.NEW_COMPONENT,
        title = "Titel $id",
        details = "Details mit „Sonderzeichen“ und \"Anführungszeichen\" $id",
        packageName = "com.example",
        settingsAction = "android.settings.ACCESSIBILITY_SETTINGS"
    )

    // --- SecurityPrefs -------------------------------------------------------------------

    @Test
    fun `protection is on by default`() {
        val prefs = SecurityPrefs(context)
        assertTrue(prefs.screenProtection)
        assertTrue(prefs.guardEnabled)
        assertTrue(prefs.capturePhotos)
    }

    @Test
    fun `settings persist`() {
        SecurityPrefs(context).screenProtection = false
        SecurityPrefs(context).guardEnabled = false

        assertFalse(SecurityPrefs(context).screenProtection)
        assertFalse(SecurityPrefs(context).guardEnabled)
    }

    @Test
    fun `guard state round trips and trust can be toggled`() {
        val prefs = SecurityPrefs(context)
        prefs.saveGuardState(GuardState(true, setOf("A:1", "B:2"), setOf("A:1"), setOf("B:2")))

        val loaded = SecurityPrefs(context).loadGuardState()
        assertTrue(loaded.initialized)
        assertEquals(setOf("A:1", "B:2"), loaded.knownKeys)
        assertEquals(setOf("A:1"), loaded.trustedKeys)
        assertEquals(setOf("B:2"), loaded.alertedKeys)

        prefs.setTrusted("B:2", true)
        assertEquals(setOf("A:1", "B:2"), prefs.loadGuardState().trustedKeys)
        prefs.setTrusted("A:1", false)
        assertEquals(setOf("B:2"), prefs.loadGuardState().trustedKeys)
    }

    @Test
    fun `nav token is random, long and stable`() {
        val first = SecurityPrefs(context).navToken()
        val second = SecurityPrefs(context).navToken()

        assertEquals(first, second)
        assertEquals(32, first.length)
        assertTrue(first.all { it in "0123456789abcdef" })
    }

    // --- IntentGuard ---------------------------------------------------------------------

    @Test
    fun `only intents carrying the secret token are trusted`() {
        val token = IntentGuard.tokenFor(context)

        val good = Intent().putExtra(IntentGuard.EXTRA_NAV_TOKEN, token)
        val wrong = Intent().putExtra(IntentGuard.EXTRA_NAV_TOKEN, token.reversed().ifEmpty { "x" } + "0")
        val missing = Intent().putExtra("wish_id", 1L)

        assertTrue(IntentGuard.isTrusted(context, good))
        assertFalse(IntentGuard.isTrusted(context, wrong))
        assertFalse(IntentGuard.isTrusted(context, missing))
        assertFalse(IntentGuard.isTrusted(context, Intent().putExtra(IntentGuard.EXTRA_NAV_TOKEN, "")))
    }

    // --- SecurityLog ---------------------------------------------------------------------

    @Test
    fun `log keeps newest first and survives special characters`() {
        val log = SecurityLog(context)
        log.add(event(1))
        log.add(event(2))

        val all = SecurityLog(context).all()
        assertEquals(listOf(2L, 1L), all.map { it.id })
        assertEquals(event(2), all.first())
    }

    @Test
    fun `null optional fields round trip`() {
        val bare = event(5).copy(packageName = null, settingsAction = null)
        SecurityLog(context).add(bare)

        assertEquals(bare, SecurityLog(context).all().single())
    }

    @Test
    fun `log is capped so it cannot grow without limit`() {
        val log = SecurityLog(context)
        repeat(SecurityLog.MAX_EVENTS + 25) { log.add(event(it.toLong() + 1)) }

        val all = log.all()
        assertEquals(SecurityLog.MAX_EVENTS, all.size)
        assertEquals((SecurityLog.MAX_EVENTS + 25).toLong(), all.first().id)
    }

    @Test
    fun `clear empties the log`() {
        val log = SecurityLog(context)
        log.add(event(1))
        log.clear()
        assertTrue(log.all().isEmpty())
    }

    @Test
    fun `a corrupted log reads as empty instead of crashing`() {
        SecurityPrefs(context).prefs.edit().putString("events_json", "{not json").apply()
        assertTrue(SecurityLog(context).all().isEmpty())
    }

    @Test
    fun `unseen count ignores older events and plain info`() {
        val log = SecurityLog(context)
        log.add(event(1, Severity.CRITICAL, timestamp = 100))
        log.add(event(2, Severity.WARNING, timestamp = 300))
        log.add(event(3, Severity.INFO, timestamp = 400))

        assertEquals(1, log.unseenCount(lastSeenAt = 200))
        assertEquals(2, log.unseenCount(lastSeenAt = 0))
    }

    @Test
    fun `event ids are unique and increasing even within the same millisecond`() {
        val log = SecurityLog(context)
        val a = log.nextId(now = 1000)
        val b = log.nextId(now = 1000)
        val c = log.nextId(now = 1000)

        assertNotEquals(a, b)
        assertTrue(b > a && c > b)
    }
}
