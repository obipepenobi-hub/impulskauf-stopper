package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

/**
 * Small private key/value store for security settings and guard state. Deliberately NOT part of
 * the Room database: security flags are read synchronously from places that can't wait for a
 * query (the overlay window being created, the activity window flags), and keeping them out of
 * the database means no schema migration is ever involved. Also not included in backups - see
 * backup_rules.xml / data_extraction_rules.xml, which only include the database.
 */
class SecurityPrefs(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** FLAG_SECURE on the popup and the app window: blocks screenshots / screen recording of them. */
    var screenProtection: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_PROTECTION, value).apply()

    /**
     * Whether the app may take a screenshot of the purchase screen to keep a product photo. Off =
     * the app never captures a single screen pixel of other apps (names and prices are still read
     * from the accessibility tree, which is how the popup is triggered at all).
     */
    var capturePhotos: Boolean
        get() = prefs.getBoolean(KEY_CAPTURE_PHOTOS, true)
        set(value) = prefs.edit().putBoolean(KEY_CAPTURE_PHOTOS, value).apply()

    /** Whether the background guard scans the device and raises warnings. */
    var guardEnabled: Boolean
        get() = prefs.getBoolean(KEY_GUARD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GUARD_ENABLED, value).apply()

    var lastScanAt: Long
        get() = prefs.getLong(KEY_LAST_SCAN_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCAN_AT, value).apply()

    var lastSeenEventAt: Long
        get() = prefs.getLong(KEY_LAST_SEEN_EVENT_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SEEN_EVENT_AT, value).apply()

    var lastTapjackAlertAt: Long
        get() = prefs.getLong(KEY_LAST_TAPJACK_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_TAPJACK_AT, value).apply()

    var encryptionFallbackLogged: Boolean
        get() = prefs.getBoolean(KEY_ENCRYPTION_FALLBACK_LOGGED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENCRYPTION_FALLBACK_LOGGED, value).apply()

    var accessibilityWasEnabled: Boolean
        get() = prefs.getBoolean(KEY_A11Y_WAS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_A11Y_WAS_ENABLED, value).apply()

    var accessibilityLostSince: Long
        get() = prefs.getLong(KEY_A11Y_LOST_SINCE, 0L)
        set(value) = prefs.edit().putLong(KEY_A11Y_LOST_SINCE, value).apply()

    var overlayLostSince: Long
        get() = prefs.getLong(KEY_OVERLAY_LOST_SINCE, 0L)
        set(value) = prefs.edit().putLong(KEY_OVERLAY_LOST_SINCE, value).apply()

    var overlayWasGranted: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_WAS_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_WAS_GRANTED, value).apply()

    fun loadGuardState(): GuardState = GuardState(
        initialized = prefs.getBoolean(KEY_GUARD_INITIALIZED, false),
        knownKeys = stringSet(KEY_KNOWN),
        trustedKeys = stringSet(KEY_TRUSTED),
        alertedKeys = stringSet(KEY_ALERTED)
    )

    fun saveGuardState(state: GuardState) {
        prefs.edit()
            .putBoolean(KEY_GUARD_INITIALIZED, state.initialized)
            .putStringSet(KEY_KNOWN, state.knownKeys)
            .putStringSet(KEY_TRUSTED, state.trustedKeys)
            .putStringSet(KEY_ALERTED, state.alertedKeys)
            .apply()
    }

    fun setTrusted(key: String, trusted: Boolean) {
        val current = loadGuardState()
        saveGuardState(
            current.copy(trustedKeys = if (trusted) current.trustedKeys + key else current.trustedKeys - key)
        )
    }

    /**
     * Secret that only this app's own notification PendingIntents carry. MainActivity is exported
     * (it has to be, it's the launcher), so without this any other app could fire an intent at it
     * with arbitrary extras and drive the app's navigation.
     */
    fun navToken(): String = synchronized(TOKEN_LOCK) {
        prefs.getString(KEY_NAV_TOKEN, null)?.let { return it }
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val token = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_NAV_TOKEN, token).apply()
        token
    }

    private fun stringSet(key: String): Set<String> = prefs.getStringSet(key, null)?.toSet() ?: emptySet()

    companion object {
        // SecurityPrefs is instantiated freely (service, activity, worker...) - the lock that keeps
        // two threads from generating two different tokens has to be shared between instances.
        private val TOKEN_LOCK = Any()

        const val FILE = "security_prefs"
        private const val KEY_SCREEN_PROTECTION = "screen_protection"
        private const val KEY_GUARD_ENABLED = "guard_enabled"
        private const val KEY_CAPTURE_PHOTOS = "capture_photos"
        private const val KEY_LAST_SCAN_AT = "last_scan_at"
        private const val KEY_LAST_SEEN_EVENT_AT = "last_seen_event_at"
        private const val KEY_LAST_TAPJACK_AT = "last_tapjack_alert_at"
        private const val KEY_ENCRYPTION_FALLBACK_LOGGED = "encryption_fallback_logged"
        private const val KEY_A11Y_WAS_ENABLED = "a11y_was_enabled"
        private const val KEY_OVERLAY_WAS_GRANTED = "overlay_was_granted"
        private const val KEY_A11Y_LOST_SINCE = "a11y_lost_since"
        private const val KEY_OVERLAY_LOST_SINCE = "overlay_lost_since"
        private const val KEY_GUARD_INITIALIZED = "guard_initialized"
        private const val KEY_KNOWN = "guard_known_keys"
        private const val KEY_TRUSTED = "guard_trusted_keys"
        private const val KEY_ALERTED = "guard_alerted_keys"
        private const val KEY_NAV_TOKEN = "nav_token"
    }
}
