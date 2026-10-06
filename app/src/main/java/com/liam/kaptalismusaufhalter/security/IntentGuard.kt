package com.liam.kaptalismusaufhalter.security

import android.content.Context
import android.content.Intent
import java.security.MessageDigest

/**
 * MainActivity has to be exported (launcher), so any app on the device can send it an intent with
 * arbitrary extras. Navigation-driving extras (open wish N, open the security page) are therefore
 * only honored when the intent also carries the per-install secret that only this app's own
 * notification PendingIntents know.
 */
object IntentGuard {
    const val EXTRA_NAV_TOKEN = "nav_token"

    fun tokenFor(context: Context): String = SecurityPrefs(context).navToken()

    fun isTrusted(context: Context, intent: Intent): Boolean {
        val presented = intent.getStringExtra(EXTRA_NAV_TOKEN) ?: return false
        return constantTimeEquals(presented, tokenFor(context))
    }

    internal fun constantTimeEquals(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
}
