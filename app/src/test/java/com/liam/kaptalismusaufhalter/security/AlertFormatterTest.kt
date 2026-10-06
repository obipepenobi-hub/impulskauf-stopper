package com.liam.kaptalismusaufhalter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertFormatterTest {

    private fun alert(level: RiskLevel, reason: AlertReason = AlertReason.NEW_COMPONENT) = GuardAlert(
        candidate = SecurityCandidate(
            kind = ComponentKind.ACCESSIBILITY_SERVICE,
            componentId = "com.evil/.Spy",
            packageName = "com.evil",
            label = "Taschenlampe Pro",
            isSystemApp = false,
            installer = null,
            capabilities = setOf(Capability.TAKE_SCREENSHOT)
        ),
        assessment = RiskAssessment(level, listOf("Darf: Screenshots aufnehmen", "Nicht über einen App-Store installiert (Quelle unbekannt)")),
        reason = reason
    )

    @Test
    fun `severity follows the risk level`() {
        assertEquals(Severity.CRITICAL, AlertFormatter.toEvent(alert(RiskLevel.HIGH), 1, 1).severity)
        assertEquals(Severity.WARNING, AlertFormatter.toEvent(alert(RiskLevel.MEDIUM), 1, 1).severity)
        assertEquals(Severity.INFO, AlertFormatter.toEvent(alert(RiskLevel.LOW), 1, 1).severity)
    }

    @Test
    fun `a new high risk component names the app, the package, the danger and what to do`() {
        val event = AlertFormatter.toEvent(alert(RiskLevel.HIGH), id = 7, now = 1000)

        assertEquals(EventKind.NEW_COMPONENT, event.kind)
        assertTrue(event.title.contains("Taschenlampe Pro"))
        assertTrue(event.title.contains("Bedienungshilfe"))
        assertTrue(event.details.contains("com.evil"))
        assertTrue(event.details.contains("Screenshots aufnehmen"))
        assertTrue(event.details.contains("Risiko: hoch"))
        assertTrue(event.details.contains("deinstallieren"))
        assertEquals("android.settings.ACCESSIBILITY_SETTINGS", event.settingsAction)
        assertEquals("com.evil", event.packageName)
        assertEquals(7L, event.id)
        assertEquals(1000L, event.timestamp)
    }

    @Test
    fun `an existing risky component is worded differently from a new one`() {
        val event = AlertFormatter.toEvent(alert(RiskLevel.HIGH, AlertReason.HIGH_RISK_EXISTING), 1, 1)
        assertEquals(EventKind.HIGH_RISK_COMPONENT, event.kind)
        assertTrue(event.title.startsWith("Riskante"))
    }

    @Test
    fun `low risk items do not tell the user to uninstall anything`() {
        val event = AlertFormatter.toEvent(alert(RiskLevel.LOW), 1, 1)
        assertFalse(event.details.contains("deinstallieren"))
    }

    @Test
    fun `the special events are critical or warning and carry an explanation`() {
        val tapjack = AlertFormatter.tapjackingEvent(1, 1)
        assertEquals(Severity.CRITICAL, tapjack.severity)
        assertEquals(EventKind.TAPJACKING, tapjack.kind)

        val update = AlertFormatter.updateRejectedEvent(1, 1, "Die Signatur passt nicht.")
        assertEquals(Severity.CRITICAL, update.severity)
        assertTrue(update.details.contains("Die Signatur passt nicht."))
        assertTrue(update.details.contains("nichts installiert"))

        val disabled = AlertFormatter.protectionDisabledEvent(1, 1, "Bedienungshilfen-Zugriff")
        assertEquals(Severity.WARNING, disabled.severity)
        assertNotNull(disabled.settingsAction)

        val launch = AlertFormatter.unexpectedLaunchEvent(1, 1, "android-app://com.evil")
        assertTrue(launch.details.contains("com.evil"))
        assertFalse(AlertFormatter.unexpectedLaunchEvent(1, 1, null).details.contains("Absender"))
    }
}
