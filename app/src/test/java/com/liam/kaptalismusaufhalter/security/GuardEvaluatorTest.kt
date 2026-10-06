package com.liam.kaptalismusaufhalter.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The warning rules: what counts as "new", what is alerted exactly once, what must stay silent. */
class GuardEvaluatorTest {

    private val own = "com.liam.kaptalismusaufhalter"

    private fun a11y(
        pkg: String,
        installer: String? = null,
        system: Boolean = false
    ) = SecurityCandidate(
        kind = ComponentKind.ACCESSIBILITY_SERVICE,
        componentId = "$pkg/.Service",
        packageName = pkg,
        label = pkg,
        isSystemApp = system,
        installer = installer,
        capabilities = setOf(Capability.READ_SCREEN)
    )

    private fun keyboard(pkg: String, installer: String? = "com.android.vending") = SecurityCandidate(
        kind = ComponentKind.INPUT_METHOD,
        componentId = "$pkg/.Ime",
        packageName = pkg,
        label = pkg,
        isSystemApp = false,
        installer = installer,
        capabilities = setOf(Capability.READ_TYPING)
    )

    private val talkback = a11y("com.google.android.marvin.talkback", system = true)
    private val trojan = a11y("com.evil.screenspy") // sideloaded -> HIGH
    private val storeApp = a11y("com.nice.autofill", installer = "com.android.vending") // MEDIUM

    private fun evaluate(current: List<SecurityCandidate>, state: GuardState, failed: Set<ComponentKind> = emptySet()) =
        GuardEvaluator.evaluate(current, state, own, failed)

    @Test
    fun `first scan only alerts for high risk items and records everything as baseline`() {
        val result = evaluate(listOf(talkback, storeApp, trojan), GuardState())

        assertEquals(listOf(trojan.key), result.alerts.map { it.candidate.key })
        assertEquals(AlertReason.HIGH_RISK_EXISTING, result.alerts.single().reason)
        assertTrue(result.newState.initialized)
        assertEquals(setOf(talkback.key, storeApp.key, trojan.key), result.newState.knownKeys)
    }

    @Test
    fun `an unchanged device stays silent on the next scan`() {
        val first = evaluate(listOf(talkback, trojan), GuardState())
        val second = evaluate(listOf(talkback, trojan), first.newState)

        assertTrue(second.alerts.isEmpty())
    }

    @Test
    fun `a newly enabled component alerts exactly once`() {
        val baseline = evaluate(listOf(talkback), GuardState()).newState

        val second = evaluate(listOf(talkback, trojan), baseline)
        assertEquals(listOf(AlertReason.NEW_COMPONENT), second.alerts.map { it.reason })
        assertEquals(RiskLevel.HIGH, second.alerts.single().assessment.level)

        val third = evaluate(listOf(talkback, trojan), second.newState)
        assertTrue(third.alerts.isEmpty())
    }

    @Test
    fun `even a low risk new component is reported so it reaches the log`() {
        val baseline = evaluate(listOf(talkback), GuardState()).newState
        val result = evaluate(listOf(talkback, keyboard("com.nice.keyboard")), baseline)

        assertEquals(1, result.alerts.size)
        assertEquals(RiskLevel.LOW, result.alerts.single().assessment.level)
    }

    @Test
    fun `turning a component off and on again alerts again`() {
        val baseline = evaluate(listOf(talkback), GuardState()).newState
        val withTrojan = evaluate(listOf(talkback, trojan), baseline).newState
        val gone = evaluate(listOf(talkback), withTrojan).newState
        val back = evaluate(listOf(talkback, trojan), gone)

        assertEquals(1, back.alerts.size)
        assertEquals(AlertReason.NEW_COMPONENT, back.alerts.single().reason)
    }

    @Test
    fun `trusted components never alert`() {
        val state = GuardState(initialized = true, knownKeys = setOf(talkback.key), trustedKeys = setOf(trojan.key))
        assertTrue(evaluate(listOf(talkback, trojan), state).alerts.isEmpty())

        val firstRun = evaluate(listOf(trojan), GuardState(trustedKeys = setOf(trojan.key)))
        assertTrue(firstRun.alerts.isEmpty())
    }

    @Test
    fun `the app itself is never reported`() {
        val self = a11y(own)
        val baseline = evaluate(listOf(talkback), GuardState()).newState
        val result = evaluate(listOf(talkback, self), baseline)

        assertTrue(result.alerts.isEmpty())
        assertFalse(self.key in result.newState.knownKeys)
    }

    @Test
    fun `a failed scan source keeps its previous state so nothing re-alerts when it recovers`() {
        val baseline = evaluate(listOf(talkback, trojan), GuardState()).newState

        // accessibility scan failed: no accessibility candidates came back
        val whileFailing = evaluate(emptyList(), baseline, failed = setOf(ComponentKind.ACCESSIBILITY_SERVICE))
        assertTrue(whileFailing.alerts.isEmpty())
        assertTrue(trojan.key in whileFailing.newState.knownKeys)
        assertTrue(trojan.key in whileFailing.newState.alertedKeys)

        val recovered = evaluate(listOf(talkback, trojan), whileFailing.newState)
        assertTrue(recovered.alerts.isEmpty())
    }

    @Test
    fun `without the failure guard a vanished source would look like a fresh install`() {
        val baseline = evaluate(listOf(talkback, trojan), GuardState()).newState
        val emptied = evaluate(emptyList(), baseline).newState // wrongly treated as "all gone"
        val back = evaluate(listOf(talkback, trojan), emptied)

        assertEquals(1, back.alerts.count { it.candidate.key == trojan.key })
    }
}
