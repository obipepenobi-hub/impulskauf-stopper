package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lives inside the accessibility service (so it's running whenever the protection is) and reacts
 * the moment the system settings that grant screen/keyboard/notification access change - that's
 * the instant a trojan gets switched on, so the user hears about it immediately instead of at the
 * next periodic scan.
 */
class SecurityMonitor(private val context: Context, private val scope: CoroutineScope) {

    private var debounce: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleScan()
    }

    fun start() {
        val resolver = context.contentResolver
        WATCHED_SETTINGS.forEach { name ->
            try {
                resolver.registerContentObserver(Settings.Secure.getUriFor(name), false, observer)
            } catch (e: Exception) {
                // A single unobservable setting just means the periodic scan covers it.
            }
        }
        scheduleScan(delayMs = 0)
    }

    fun stop() {
        debounce?.cancel()
        try {
            context.contentResolver.unregisterContentObserver(observer)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    private fun scheduleScan(delayMs: Long = DEBOUNCE_MS) {
        debounce?.cancel()
        debounce = scope.launch {
            // Settings changes arrive in several quick writes - wait for them to settle.
            delay(delayMs)
            SecurityGuard.scan(context)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 1_500L

        // Literal names: ENABLED_NOTIFICATION_LISTENERS is not part of the public SDK surface,
        // but the setting itself is world-readable (NotificationManagerCompat reads it too).
        private val WATCHED_SETTINGS = listOf(
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "enabled_notification_listeners",
            "enabled_input_methods",
            "default_input_method"
        )
    }
}
