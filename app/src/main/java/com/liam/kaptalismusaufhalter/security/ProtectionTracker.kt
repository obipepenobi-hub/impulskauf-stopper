package com.liam.kaptalismusaufhalter.security

/**
 * Decides when "the access this app depends on disappeared" is worth a warning. Android briefly
 * reports a service as not enabled while an app is being updated or restarted, so a single sighting
 * is not enough: the access must still be missing on a later check at least [GRACE_MS] afterwards.
 */
object ProtectionTracker {

    const val GRACE_MS = 2 * 60 * 1000L

    data class State(val wasEnabled: Boolean, val lostSince: Long)

    data class Step(val state: State, val raiseAlert: Boolean)

    fun step(state: State, nowEnabled: Boolean, now: Long): Step = when {
        nowEnabled -> Step(State(wasEnabled = true, lostSince = 0L), raiseAlert = false)
        // never was on, or already reported: nothing to say
        !state.wasEnabled -> Step(state, raiseAlert = false)
        // first time we notice it is gone: wait and see
        state.lostSince == 0L -> Step(state.copy(lostSince = now), raiseAlert = false)
        now - state.lostSince >= GRACE_MS -> Step(State(wasEnabled = false, lostSince = 0L), raiseAlert = true)
        else -> Step(state, raiseAlert = false)
    }
}
