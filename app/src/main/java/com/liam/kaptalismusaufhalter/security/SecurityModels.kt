package com.liam.kaptalismusaufhalter.security

/** Kinds of system-level "can see/steal what you do" components the guard keeps an eye on. */
enum class ComponentKind(val label: String, val settingsAction: String) {
    ACCESSIBILITY_SERVICE("Bedienungshilfe", "android.settings.ACCESSIBILITY_SETTINGS"),
    NOTIFICATION_LISTENER("Benachrichtigungs-Zugriff", "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
    INPUT_METHOD("Tastatur", "android.settings.INPUT_METHOD_SETTINGS"),
    DEVICE_ADMIN("Geräteadministrator", "android.settings.SECURITY_SETTINGS")
}

/** What a component is able to do - the reason it matters if it turns out to be malicious. */
enum class Capability(val description: String) {
    READ_SCREEN("Bildschirminhalte lesen"),
    TAKE_SCREENSHOT("Screenshots aufnehmen"),
    KEY_EVENTS("Tastatureingaben mitlesen"),
    GESTURES("Eingaben für dich ausführen"),
    READ_NOTIFICATIONS("alle Benachrichtigungen lesen (z. B. Bestätigungscodes)"),
    READ_TYPING("alles mitlesen, was du tippst (auch Passwörter)"),
    MANAGE_DEVICE("das Gerät verwalten (sperren, löschen)")
}

enum class RiskLevel(val label: String) { LOW("niedrig"), MEDIUM("mittel"), HIGH("hoch") }

data class RiskAssessment(val level: RiskLevel, val reasons: List<String>)

/**
 * One enabled component found on the device. [componentId] is stable across scans (flattened
 * component name, or the package name where the platform only exposes that).
 */
data class SecurityCandidate(
    val kind: ComponentKind,
    val componentId: String,
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean,
    val installer: String?,
    val capabilities: Set<Capability>
) {
    val key: String get() = "${kind.name}:$componentId"
}

enum class Severity { INFO, WARNING, CRITICAL }

enum class EventKind {
    NEW_COMPONENT,
    HIGH_RISK_COMPONENT,
    TAPJACKING,
    UPDATE_REJECTED,
    PROTECTION_DISABLED,
    UNEXPECTED_LAUNCH,
    INFO
}

data class SecurityEvent(
    val id: Long,
    val timestamp: Long,
    val severity: Severity,
    val kind: EventKind,
    val title: String,
    val details: String,
    val packageName: String? = null,
    val settingsAction: String? = null
)

enum class AlertReason { NEW_COMPONENT, HIGH_RISK_EXISTING }

data class GuardAlert(
    val candidate: SecurityCandidate,
    val assessment: RiskAssessment,
    val reason: AlertReason
)

/** What the guard remembered from previous scans - persisted by [SecurityPrefs]. */
data class GuardState(
    val initialized: Boolean = false,
    val knownKeys: Set<String> = emptySet(),
    val trustedKeys: Set<String> = emptySet(),
    val alertedKeys: Set<String> = emptySet()
)

data class GuardResult(val alerts: List<GuardAlert>, val newState: GuardState)
