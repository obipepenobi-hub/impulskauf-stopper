package com.liam.kaptalismusaufhalter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskAssessorTest {

    private fun candidate(
        kind: ComponentKind = ComponentKind.ACCESSIBILITY_SERVICE,
        system: Boolean = false,
        installer: String? = null,
        caps: Set<Capability> = setOf(Capability.READ_SCREEN)
    ) = SecurityCandidate(
        kind = kind,
        componentId = "com.example/.Svc",
        packageName = "com.example",
        label = "Beispiel",
        isSystemApp = system,
        installer = installer,
        capabilities = caps
    )

    @Test
    fun `system components are low risk whatever they can do`() {
        val result = RiskAssessor.assess(
            candidate(system = true, caps = setOf(Capability.TAKE_SCREENSHOT, Capability.KEY_EVENTS))
        )
        assertEquals(RiskLevel.LOW, result.level)
    }

    @Test
    fun `sideloaded accessibility service is high risk`() {
        assertEquals(RiskLevel.HIGH, RiskAssessor.assess(candidate(installer = null)).level)
        assertEquals(
            RiskLevel.HIGH,
            RiskAssessor.assess(candidate(installer = "com.google.android.packageinstaller")).level
        )
    }

    @Test
    fun `store installed accessibility service is medium risk`() {
        assertEquals(RiskLevel.MEDIUM, RiskAssessor.assess(candidate(installer = "com.android.vending")).level)
    }

    @Test
    fun `store keyboard is low but sideloaded keyboard is high`() {
        val store = candidate(ComponentKind.INPUT_METHOD, installer = "com.android.vending", caps = setOf(Capability.READ_TYPING))
        val sideloaded = candidate(ComponentKind.INPUT_METHOD, installer = null, caps = setOf(Capability.READ_TYPING))
        assertEquals(RiskLevel.LOW, RiskAssessor.assess(store).level)
        assertEquals(RiskLevel.HIGH, RiskAssessor.assess(sideloaded).level)
    }

    @Test
    fun `sideloaded notification listener and device admin are high risk`() {
        assertEquals(
            RiskLevel.HIGH,
            RiskAssessor.assess(candidate(ComponentKind.NOTIFICATION_LISTENER, caps = setOf(Capability.READ_NOTIFICATIONS))).level
        )
        assertEquals(
            RiskLevel.HIGH,
            RiskAssessor.assess(candidate(ComponentKind.DEVICE_ADMIN, caps = setOf(Capability.MANAGE_DEVICE))).level
        )
    }

    @Test
    fun `reasons explain the source and the capabilities in plain language`() {
        val unknown = RiskAssessor.assess(candidate(installer = null, caps = setOf(Capability.TAKE_SCREENSHOT)))
        assertTrue(unknown.reasons.any { it.contains("Screenshots aufnehmen") })
        assertTrue(unknown.reasons.any { it.contains("Quelle unbekannt") })

        val browser = RiskAssessor.assess(candidate(installer = "com.android.chrome"))
        assertTrue(browser.reasons.any { it.contains("com.android.chrome") })

        val store = RiskAssessor.assess(candidate(installer = "com.android.vending"))
        assertTrue(store.reasons.any { it.contains("App-Store installiert") })
    }

    @Test
    fun `accessibility capability bits are decoded`() {
        assertEquals(emptySet<Capability>(), decodeAccessibilityCapabilities(0))
        assertEquals(setOf(Capability.READ_SCREEN), decodeAccessibilityCapabilities(0x01))
        assertEquals(
            setOf(Capability.READ_SCREEN, Capability.KEY_EVENTS, Capability.GESTURES, Capability.TAKE_SCREENSHOT),
            decodeAccessibilityCapabilities(0x01 or 0x08 or 0x20 or 0x80)
        )
        // touch exploration (0x02) and magnification (0x10) are not on the danger list
        assertEquals(emptySet<Capability>(), decodeAccessibilityCapabilities(0x02 or 0x10))
    }
}
