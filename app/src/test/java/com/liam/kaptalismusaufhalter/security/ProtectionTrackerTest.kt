package com.liam.kaptalismusaufhalter.security

import com.liam.kaptalismusaufhalter.security.ProtectionTracker.GRACE_MS
import com.liam.kaptalismusaufhalter.security.ProtectionTracker.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionTrackerTest {

    private val t0 = 1_000_000L

    @Test
    fun `access that is on stays quiet and is remembered`() {
        val step = ProtectionTracker.step(State(false, 0), nowEnabled = true, now = t0)
        assertEquals(State(true, 0), step.state)
        assertFalse(step.raiseAlert)
    }

    @Test
    fun `access that was never on never alerts`() {
        val step = ProtectionTracker.step(State(false, 0), nowEnabled = false, now = t0)
        assertFalse(step.raiseAlert)
        assertEquals(State(false, 0), step.state)
    }

    @Test
    fun `the first sighting of lost access only starts the clock`() {
        val step = ProtectionTracker.step(State(true, 0), nowEnabled = false, now = t0)
        assertFalse(step.raiseAlert)
        assertEquals(State(true, t0), step.state)
    }

    @Test
    fun `a quick recovery cancels the pending alert`() {
        val lost = ProtectionTracker.step(State(true, 0), nowEnabled = false, now = t0).state
        val back = ProtectionTracker.step(lost, nowEnabled = true, now = t0 + 10_000)
        assertFalse(back.raiseAlert)
        assertEquals(State(true, 0), back.state)
    }

    @Test
    fun `still missing before the grace period ends does not alert`() {
        val lost = State(true, t0)
        val step = ProtectionTracker.step(lost, nowEnabled = false, now = t0 + GRACE_MS - 1)
        assertFalse(step.raiseAlert)
        assertEquals(lost, step.state)
    }

    @Test
    fun `still missing after the grace period alerts exactly once`() {
        val step = ProtectionTracker.step(State(true, t0), nowEnabled = false, now = t0 + GRACE_MS)
        assertTrue(step.raiseAlert)
        assertEquals(State(false, 0), step.state)

        val next = ProtectionTracker.step(step.state, nowEnabled = false, now = t0 + GRACE_MS + 60_000)
        assertFalse(next.raiseAlert)
    }

    @Test
    fun `turning access back on after an alert re-arms the tracker`() {
        val alerted = State(false, 0)
        val on = ProtectionTracker.step(alerted, nowEnabled = true, now = t0)
        assertEquals(State(true, 0), on.state)

        val lostAgain = ProtectionTracker.step(on.state, nowEnabled = false, now = t0 + 1)
        assertEquals(t0 + 1, lostAgain.state.lostSince)
    }
}
