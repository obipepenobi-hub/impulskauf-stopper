package com.liam.kaptalismusaufhalter.security

/**
 * Rates how dangerous a component would be *if* it were malicious. The deciding signals are the
 * ones a user can't see in Android's settings list: where the app came from (sideloaded apps
 * are where nearly all screen-stealing trojans live) and how powerful the granted capability is.
 */
object RiskAssessor {

    // Installers that run their own malware scanning / review. Anything else (a browser, the raw
    // package installer, adb, a file manager) counts as sideloaded.
    internal val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.sec.android.app.samsungapps",
        "com.huawei.appmarket",
        "com.amazon.venezia",
        "com.xiaomi.mipicks",
        "com.xiaomi.market",
        "com.heytap.market",
        "com.oppo.market",
        "com.vivo.appstore",
        "org.fdroid.fdroid"
    )

    fun assess(candidate: SecurityCandidate): RiskAssessment {
        if (candidate.isSystemApp) {
            return RiskAssessment(RiskLevel.LOW, listOf("Teil des Systems / vorinstalliert"))
        }

        val trustedStore = candidate.installer in TRUSTED_INSTALLERS
        val reasons = mutableListOf<String>()

        if (candidate.capabilities.isNotEmpty()) {
            reasons += "Darf: " + candidate.capabilities.joinToString(", ") { it.description }
        }
        reasons += when {
            trustedStore -> "Aus einem App-Store installiert"
            candidate.installer == null -> "Nicht über einen App-Store installiert (Quelle unbekannt)"
            else -> "Installiert über „${candidate.installer}“ – kein bekannter App-Store"
        }

        val level = when (candidate.kind) {
            ComponentKind.ACCESSIBILITY_SERVICE -> if (trustedStore) RiskLevel.MEDIUM else RiskLevel.HIGH
            ComponentKind.NOTIFICATION_LISTENER -> if (trustedStore) RiskLevel.MEDIUM else RiskLevel.HIGH
            ComponentKind.DEVICE_ADMIN -> if (trustedStore) RiskLevel.MEDIUM else RiskLevel.HIGH
            // A keyboard from a store is something people install on purpose all the time; only a
            // sideloaded one is alarming.
            ComponentKind.INPUT_METHOD -> if (trustedStore) RiskLevel.LOW else RiskLevel.HIGH
        }
        return RiskAssessment(level, reasons)
    }
}

/** Decodes [android.accessibilityservice.AccessibilityServiceInfo.getCapabilities] bits. */
internal fun decodeAccessibilityCapabilities(bits: Int): Set<Capability> {
    val result = linkedSetOf<Capability>()
    // CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT
    if (bits and 0x01 != 0) result += Capability.READ_SCREEN
    // CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS
    if (bits and 0x08 != 0) result += Capability.KEY_EVENTS
    // CAPABILITY_CAN_PERFORM_GESTURES
    if (bits and 0x20 != 0) result += Capability.GESTURES
    // CAPABILITY_CAN_TAKE_SCREENSHOT
    if (bits and 0x80 != 0) result += Capability.TAKE_SCREENSHOT
    return result
}
