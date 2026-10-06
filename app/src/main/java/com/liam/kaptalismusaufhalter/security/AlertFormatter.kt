package com.liam.kaptalismusaufhalter.security

/** Builds the user-facing German text for a [GuardAlert]. Pure, so the wording is testable. */
object AlertFormatter {

    fun toEvent(alert: GuardAlert, id: Long, now: Long): SecurityEvent {
        val c = alert.candidate
        val level = alert.assessment.level
        val severity = when (level) {
            RiskLevel.HIGH -> Severity.CRITICAL
            RiskLevel.MEDIUM -> Severity.WARNING
            RiskLevel.LOW -> Severity.INFO
        }

        val title = when (alert.reason) {
            AlertReason.NEW_COMPONENT -> "Neu aktiviert: ${c.kind.label} „${c.label}“"
            AlertReason.HIGH_RISK_EXISTING -> "Riskante ${c.kind.label}: „${c.label}“"
        }

        val details = buildString {
            append("„${c.label}“ (${c.packageName}) ")
            append(
                when (alert.reason) {
                    AlertReason.NEW_COMPONENT -> "wurde gerade als ${c.kind.label} aktiviert. "
                    AlertReason.HIGH_RISK_EXISTING -> "ist als ${c.kind.label} aktiv. "
                }
            )
            append("Risiko: ${level.label}. ")
            append(alert.assessment.reasons.joinToString(" ") { it.trimEnd('.') + "." })
            if (level != RiskLevel.LOW) {
                append(" Warst du das nicht selbst? Dann in den Einstellungen sofort deaktivieren und die App deinstallieren.")
            }
        }

        return SecurityEvent(
            id = id,
            timestamp = now,
            severity = severity,
            kind = when (alert.reason) {
                AlertReason.NEW_COMPONENT -> EventKind.NEW_COMPONENT
                AlertReason.HIGH_RISK_EXISTING -> EventKind.HIGH_RISK_COMPONENT
            },
            title = title,
            details = details,
            packageName = c.packageName,
            settingsAction = c.kind.settingsAction
        )
    }

    fun tapjackingEvent(id: Long, now: Long) = SecurityEvent(
        id = id,
        timestamp = now,
        severity = Severity.CRITICAL,
        kind = EventKind.TAPJACKING,
        title = "Popup wurde von einem anderen Fenster überdeckt",
        details = "Während der Impulskauf-Stopper-Hinweis offen war, hat ein anderes Fenster darüber " +
            "gelegen. Die Eingabe wurde zur Sicherheit blockiert. Das kann ein harmloses Overlay sein " +
            "(z. B. eine Chat-Blase oder ein Bildschirmfilter) – oder der Versuch einer App, dich zu " +
            "einem Klick auf „Trotzdem kaufen“ zu verleiten. Prüfe, welche Apps „über anderen Apps " +
            "anzeigen“ dürfen."
    )

    fun updateRejectedEvent(id: Long, now: Long, reason: String) = SecurityEvent(
        id = id,
        timestamp = now,
        severity = Severity.CRITICAL,
        kind = EventKind.UPDATE_REJECTED,
        title = "Update wurde aus Sicherheitsgründen abgelehnt",
        details = "$reason Es wurde nichts installiert. Lade das Update bei Bedarf manuell von der " +
            "offiziellen Release-Seite."
    )

    fun protectionDisabledEvent(id: Long, now: Long, what: String) = SecurityEvent(
        id = id,
        timestamp = now,
        severity = Severity.WARNING,
        kind = EventKind.PROTECTION_DISABLED,
        title = "$what wurde ausgeschaltet",
        details = "Der Impulskauf-Stopper kann ohne diese Freigabe keine Kauf-Bildschirme mehr " +
            "erkennen. Hast du das nicht selbst gemacht, könnte eine andere App versuchen, " +
            "Schutz-Apps abzuschalten.",
        settingsAction = "android.settings.ACCESSIBILITY_SETTINGS"
    )

    fun unexpectedLaunchEvent(id: Long, now: Long, caller: String?) = SecurityEvent(
        id = id,
        timestamp = now,
        severity = Severity.WARNING,
        kind = EventKind.UNEXPECTED_LAUNCH,
        title = "Fremder Aufruf der App abgewiesen",
        details = "Eine andere App hat versucht, einen geschützten Bereich der App direkt zu öffnen" +
            (caller?.let { " (Absender laut System: $it)" } ?: "") + ". Der Aufruf wurde ignoriert."
    )

    fun infoEvent(id: Long, now: Long, title: String, details: String) = SecurityEvent(
        id = id,
        timestamp = now,
        severity = Severity.INFO,
        kind = EventKind.INFO,
        title = title,
        details = details
    )
}
