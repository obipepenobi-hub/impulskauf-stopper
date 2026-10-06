package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.provider.Settings
import com.liam.kaptalismusaufhalter.guard.ImpulskaufAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Entry point of the warning system. Everything that can detect something suspicious reports
 * through here, so the rules for logging vs. notifying live in one place: events are ALWAYS
 * logged; the notification is only posted when the user hasn't switched the guard off.
 */
object SecurityGuard {

    data class ScanSummary(
        val candidates: List<SecurityCandidate>,
        val failedKinds: Set<ComponentKind>,
        val alertCount: Int
    )

    private val lock = Any()
    private const val TAPJACK_ALERT_GAP_MS = 10 * 60 * 1000L

    suspend fun scan(context: Context): ScanSummary? = withContext(Dispatchers.IO) { scanBlocking(context) }

    /** Runs a full scan, raises alerts for anything new/dangerous and updates the remembered state. */
    fun scanBlocking(context: Context): ScanSummary? {
        val app = context.applicationContext
        val prefs = SecurityPrefs(app)
        return try {
            synchronized(lock) {
                val outcome = SecurityScanner.scan(app)
                val result = GuardEvaluator.evaluate(
                    current = outcome.candidates,
                    state = prefs.loadGuardState(),
                    ownPackage = app.packageName,
                    failedKinds = outcome.failedKinds
                )
                prefs.saveGuardState(result.newState)

                val log = SecurityLog(app)
                result.alerts.forEach { alert ->
                    raise(app, AlertFormatter.toEvent(alert, log.nextId(), System.currentTimeMillis()))
                }
                checkOwnProtection(app, prefs)
                prefs.lastScanAt = System.currentTimeMillis()

                ScanSummary(outcome.candidates, outcome.failedKinds, result.alerts.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Adds [event] to the log and - unless the user switched the guard off - warns them. */
    fun raise(context: Context, event: SecurityEvent) {
        val app = context.applicationContext
        SecurityLog(app).add(event)
        if (SecurityPrefs(app).guardEnabled) SecurityNotifier.notify(app, event)
    }

    fun reportTapjacking(context: Context) {
        val app = context.applicationContext
        val prefs = SecurityPrefs(app)
        val now = System.currentTimeMillis()
        // Touch events arrive in bursts - one alert per window is plenty.
        synchronized(lock) {
            if (now - prefs.lastTapjackAlertAt < TAPJACK_ALERT_GAP_MS) return
            prefs.lastTapjackAlertAt = now
        }
        raise(app, AlertFormatter.tapjackingEvent(SecurityLog(app).nextId(now), now))
    }

    fun reportUpdateRejected(context: Context, reason: String) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        raise(app, AlertFormatter.updateRejectedEvent(SecurityLog(app).nextId(now), now, reason))
    }

    fun reportUnexpectedLaunch(context: Context, caller: String?) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        raise(app, AlertFormatter.unexpectedLaunchEvent(SecurityLog(app).nextId(now), now, caller))
    }

    /**
     * Something silently switching off a security app is itself a classic malware move - so if
     * the accessibility/overlay access this app relies on disappears, say so instead of just
     * quietly no longer showing popups. [ProtectionTracker] keeps a brief system hiccup (e.g. during
     * an app update) from looking like an attack.
     */
    private fun checkOwnProtection(app: Context, prefs: SecurityPrefs) {
        val log = SecurityLog(app)
        val now = System.currentTimeMillis()

        val accessibility = ProtectionTracker.step(
            ProtectionTracker.State(prefs.accessibilityWasEnabled, prefs.accessibilityLostSince),
            nowEnabled = ImpulskaufAccessibilityService.isEnabled(app),
            now = now
        )
        prefs.accessibilityWasEnabled = accessibility.state.wasEnabled
        prefs.accessibilityLostSince = accessibility.state.lostSince
        if (accessibility.raiseAlert) {
            raise(app, AlertFormatter.protectionDisabledEvent(log.nextId(now), now, "Bedienungshilfen-Zugriff"))
        }

        val overlay = ProtectionTracker.step(
            ProtectionTracker.State(prefs.overlayWasGranted, prefs.overlayLostSince),
            nowEnabled = Settings.canDrawOverlays(app),
            now = now
        )
        prefs.overlayWasGranted = overlay.state.wasEnabled
        prefs.overlayLostSince = overlay.state.lostSince
        if (overlay.raiseAlert) {
            raise(app, AlertFormatter.protectionDisabledEvent(log.nextId(now + 1), now, "„Über anderen Apps anzeigen“"))
        }
    }
}
