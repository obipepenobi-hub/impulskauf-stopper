package com.liam.kaptalismusaufhalter.ui.screens.security

import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.security.RiskLevel
import com.liam.kaptalismusaufhalter.security.SecurityEvent
import com.liam.kaptalismusaufhalter.security.Severity
import com.liam.kaptalismusaufhalter.ui.components.BackHeader
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont

private val ColorDanger = Color(0xFFB3261E)

@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Warnings are delivered as notifications - if those are blocked, say so instead of letting the
    // user believe they are protected. Re-checked when returning from the system settings.
    var notificationsEnabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { BackHeader("Sicherheit", onBack) }

        item {
            SectionCard {
                Text(
                    if (state.guardEnabled) "Wächter aktiv" else "Wächter ausgeschaltet",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 18.sp)
                )
                Text(
                    if (state.lastScanAt > 0) {
                        "Zuletzt geprüft: " + DateUtils.getRelativeTimeSpanString(
                            state.lastScanAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                        )
                    } else {
                        "Noch nicht geprüft."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
                if (!notificationsEnabled) {
                    Text(
                        "Benachrichtigungen sind ausgeschaltet – Warnungen siehst du dann nur hier im Protokoll.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorDanger
                    )
                    Text(
                        "Benachrichtigungen einschalten",
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 14.sp),
                        color = ColorAccent,
                        modifier = Modifier.clickable { openNotificationSettings(context) }
                    )
                }
                Text(
                    text = if (state.scanning) "Prüfe …" else "Jetzt prüfen",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp),
                    color = ColorAccent,
                    modifier = Modifier.clickable(enabled = !state.scanning, onClick = viewModel::refresh)
                )
            }
        }

        item {
            SectionCard {
                ToggleRow(
                    title = "Screenshot-Schutz",
                    description = "Das Popup und die App-Fenster lassen sich nicht mehr per Screenshot, " +
                        "Bildschirmaufnahme oder Übertragung abgreifen. Schalte das aus, wenn du selbst " +
                        "einen Screenshot der App machen willst.",
                    checked = state.screenProtection,
                    onCheckedChange = viewModel::setScreenProtection
                )
                ToggleRow(
                    title = "Produktfotos aufnehmen",
                    description = "Speichert beim Popup einen kleinen, verschlüsselten Ausschnitt des " +
                        "Kauf-Bildschirms als Foto. Ausgeschaltet nimmt die App nie ein Bild von " +
                        "fremden Apps auf – der Produktname und Preis werden trotzdem erkannt.",
                    checked = state.capturePhotos,
                    onCheckedChange = viewModel::setCapturePhotos
                )
                ToggleRow(
                    title = "Warnungen bei verdächtigen Apps",
                    description = "Meldet sofort, wenn eine App neuen Zugriff auf Bildschirm, Tastatur " +
                        "oder Benachrichtigungen bekommt, wenn ein anderes Fenster das Popup überdeckt " +
                        "oder ein Update abgelehnt wurde.",
                    checked = state.guardEnabled,
                    onCheckedChange = viewModel::setGuardEnabled
                )
            }
        }

        item {
            SectionCard {
                Text("Das ist immer aktiv", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp))
                listOf(
                    "Produktfotos sind mit dem Android-Schlüsselspeicher verschlüsselt, liegen nur im privaten App-Speicher und sind von jedem Backup ausgenommen.",
                    "Fotos und Vorschläge werden gelöscht, sobald ein Wunsch abgebrochen oder entschieden wird.",
                    "Seiten mit Passwortfeld werden nie gelesen und nie fotografiert.",
                    "Klicks werden blockiert, wenn ein anderes Fenster das Popup überdeckt (Tapjacking).",
                    "Updates: nur von diesem GitHub-Release über HTTPS, privat zwischengespeichert, Signatur wird vor der Installation geprüft.",
                    "Fremde Apps können die App nicht über Intents fernsteuern."
                ).forEach { line ->
                    Text("•  $line", style = MaterialTheme.typography.bodySmall, color = ColorNeutral700)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Geräte-Check", style = TextStyle(fontFamily = HeadingFont, fontSize = 18.sp))
                Text(
                    "Apps, die gerade Bildschirm, Tastatur oder Benachrichtigungen lesen dürfen. " +
                        "Kennst du eine App nicht, deaktiviere sie in den Einstellungen und " +
                        "deinstalliere sie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
                if (state.failedKinds.isNotEmpty()) {
                    Text(
                        "Nicht lesbar: " + state.failedKinds.joinToString { it.label } +
                            " – das Ergebnis ist unvollständig.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorDanger
                    )
                }
            }
        }

        if (state.components.isEmpty() && !state.scanning) {
            item {
                Text(
                    "Keine zusätzlichen Zugriffe gefunden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorNeutral600
                )
            }
        }
        items(state.components, key = { it.candidate.key }) { row ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.candidate.label, style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp))
                        Text(
                            "${row.candidate.kind.label} · ${row.candidate.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorNeutral600
                        )
                    }
                    Text(
                        if (row.trusted) "vertraut" else "Risiko: ${row.assessment.level.label}",
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 13.sp),
                        color = if (row.trusted) ColorNeutral600 else riskColor(row.assessment.level)
                    )
                }
                Text(
                    row.assessment.reasons.joinToString(" ") { it.trimEnd('.') + "." },
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(
                        "Einstellungen öffnen",
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 13.sp),
                        color = ColorAccent,
                        modifier = Modifier.clickable { openSettings(context, row.candidate.kind.settingsAction) }
                    )
                    Text(
                        if (row.trusted) "Vertrauen entziehen" else "Als vertrauenswürdig markieren",
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 13.sp),
                        color = ColorNeutral700,
                        modifier = Modifier.clickable { viewModel.toggleTrust(row.candidate.key) }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Protokoll", style = TextStyle(fontFamily = HeadingFont, fontSize = 18.sp))
                if (state.events.isNotEmpty()) {
                    Text(
                        "Löschen",
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 14.sp),
                        color = ColorAccent,
                        modifier = Modifier.clickable(onClick = viewModel::clearLog)
                    )
                }
            }
        }
        if (state.events.isEmpty()) {
            item {
                Text(
                    "Keine Auffälligkeiten aufgezeichnet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorNeutral600
                )
            }
        }
        items(state.events, key = { it.id }) { event -> EventCard(event) }

        item {
            SectionCard {
                Text("Gespeicherte Produktfotos", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp))
                Text(
                    "Löscht alle von der App aufgenommenen Produktfotos sofort. Die Wünsche selbst " +
                        "bleiben erhalten, nur ohne Bild.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
                Text(
                    if (state.photosDeleted) "Alle Fotos gelöscht." else "Alle Fotos jetzt löschen",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp),
                    color = if (state.photosDeleted) ColorNeutral600 else ColorDanger,
                    modifier = Modifier.clickable(enabled = !state.photosDeleted, onClick = viewModel::deleteAllPhotos)
                )
            }
        }

        item {
            Text(
                "Grenzen: Auf einem gerooteten Handy, oder wenn du selbst einer App die Bedienungshilfen " +
                    "oder eine Bildschirmaufnahme erlaubst, kann keine App das verhindern – deshalb warnt " +
                    "der Wächter, sobald so etwas Neues auftaucht. Installiere nur Apps aus vertrauenswürdigen Quellen.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorNeutral600
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = ColorNeutral700)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ColorAccent)
        )
    }
}

@Composable
private fun EventCard(event: SecurityEvent) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                severityLabel(event.severity),
                style = TextStyle(fontFamily = HeadingFont, fontSize = 12.sp),
                color = severityColor(event.severity)
            )
            Text(
                DateUtils.getRelativeTimeSpanString(event.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = ColorNeutral600
            )
        }
        Text(event.title, style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp))
        Text(event.details, style = MaterialTheme.typography.bodySmall, color = ColorNeutral700)
    }
}

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.HIGH -> ColorDanger
    RiskLevel.MEDIUM -> ColorAccent
    RiskLevel.LOW -> ColorAccent2
}

private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> ColorDanger
    Severity.WARNING -> ColorAccent
    Severity.INFO -> ColorNeutral600
}

private fun severityLabel(severity: Severity): String = when (severity) {
    Severity.CRITICAL -> "DRINGEND"
    Severity.WARNING -> "WARNUNG"
    Severity.INFO -> "HINWEIS"
}

private fun openSettings(context: android.content.Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
