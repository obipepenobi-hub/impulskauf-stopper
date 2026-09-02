package com.liam.kaptalismusaufhalter.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.BuildConfig
import com.liam.kaptalismusaufhalter.data.Strictness
import com.liam.kaptalismusaufhalter.data.toWaitTiers
import com.liam.kaptalismusaufhalter.domain.PIGGY_STAGES
import com.liam.kaptalismusaufhalter.domain.calcWaitHours
import com.liam.kaptalismusaufhalter.guard.ImpulskaufAccessibilityService
import com.liam.kaptalismusaufhalter.ui.components.BackHeader
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.components.formatWaitLabel
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent100
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent700
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral400
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.ColorText
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont
import com.liam.kaptalismusaufhalter.update.UpdateAvailableDialog
import com.liam.kaptalismusaufhalter.update.UpdateChecker
import com.liam.kaptalismusaufhalter.update.UpdateInfo
import com.liam.kaptalismusaufhalter.update.UpdateInstaller
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onExcludedAppsClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Seeded once from the DB value, then left alone - keying this on settings.hourlyWage would
    // reset the field (and the cursor) on every keystroke, since typing a valid value saves it,
    // which changes settings.hourlyWage right back.
    var wageText by remember { mutableStateOf("") }
    var wageInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(settings.hourlyWage) {
        if (!wageInitialized) {
            wageText = settings.hourlyWage.toString().replace(".", ",")
            wageInitialized = true
        }
    }
    var checking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var checkedOnce by remember { mutableStateOf(false) }

    val wage = wageText.replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 } ?: settings.hourlyWage

    // Both permissions are granted outside the app (system settings), so re-check whenever
    // this screen comes back to the foreground instead of only once on first composition.
    var accessibilityEnabled by remember { mutableStateOf(ImpulskaufAccessibilityService.isEnabled(context)) }
    var overlayGranted by remember { mutableStateOf(AndroidSettings.canDrawOverlays(context)) }
    var showAccessibilityExplainer by remember { mutableStateOf(false) }
    var showOverlayExplainer by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityEnabled = ImpulskaufAccessibilityService.isEnabled(context)
                overlayGranted = AndroidSettings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
            info = info,
            downloadProgress = downloadProgress,
            onDownload = {
                downloadProgress = 0f
                scope.launch {
                    UpdateInstaller.download(context, info) { progress ->
                        downloadProgress = progress
                    }.onSuccess { apkFile ->
                        UpdateInstaller.promptInstall(context, apkFile)
                        downloadProgress = null
                        updateInfo = null
                    }.onFailure {
                        downloadProgress = null
                    }
                }
            },
            onDismiss = { updateInfo = null }
        )
    }

    if (showAccessibilityExplainer) {
        PermissionExplainerDialog(
            title = "Bedienungshilfen erlauben?",
            body = "Der Impulskauf-Stopper braucht den Bedienungshilfen-Dienst, um Kauf-" +
                "Bildschirme in anderen Apps zu erkennen (Preis + Kaufen-Button). Android zeigt " +
                "dabei eine Warnung über „volle Kontrolle über dein Gerät“ — das steht bei " +
                "jedem Bedienungshilfen-Dienst so, unabhängig davon, was er tatsächlich tut. Wir " +
                "lesen nur nach diesem Muster mit, speichern und übertragen nichts.\n\nIn der " +
                "nächsten Ansicht: „Impulskauf-Stopper“ suchen und aktivieren.\n\nWeil die App " +
                "nicht aus dem Play Store kommt, blockiert Android den Schalter beim ersten Mal " +
                "eventuell mit „App wurde Zugriff verweigert“. Falls das passiert, tippe unten " +
                "auf „App-Info öffnen“ und erlaube dort „Eingeschränkte Einstellungen zulassen“.",
            onConfirm = {
                showAccessibilityExplainer = false
                context.startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onDismiss = { showAccessibilityExplainer = false },
            secondaryActionLabel = "App-Info öffnen (bei „Zugriff verweigert“)",
            onSecondaryAction = {
                showAccessibilityExplainer = false
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        )
    }

    if (showOverlayExplainer) {
        PermissionExplainerDialog(
            title = "Anzeige über anderen Apps erlauben?",
            body = "Damit der Hinweis wirklich über der anderen App erscheint und du nicht " +
                "versehentlich am Popup vorbei auf „Kaufen“ tippst, braucht die App die " +
                "Berechtigung, über anderen Apps zu zeichnen. Das ist eine separate Berechtigung " +
                "von der Bedienungshilfen-Freigabe — beide werden gebraucht.",
            onConfirm = {
                showOverlayExplainer = false
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
            onDismiss = { showOverlayExplainer = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader("Einstellungen", onBack) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldLabel("Dein Stundenlohn (netto)")
                Box {
                    PillInput(
                        value = wageText,
                        onValueChange = {
                            wageText = it
                            it.replace(",", ".").toDoubleOrNull()?.takeIf { v -> v > 0 }?.let(viewModel::updateHourlyWage)
                        },
                        keyboardType = KeyboardType.Decimal,
                        trailingText = "€ / Std"
                    )
                }
                Text(
                    "Danach rechnet die App jeden Preis um. Aktuell: 100 € = ${"%.1f".format(100.0 / wage).replace(".", ",")} Arbeitsstunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(26.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Impulskauf-Stopper für andere Apps", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp))
                Text(
                    "Erkennt Kauf-Bildschirme in anderen Apps (Preis + Kaufen-Button) und zeigt " +
                        "diesen Hinweis darüber an. Liest dafür Bildschirminhalte anderer Apps mit " +
                        "dem Bedienungshilfen-Dienst nach diesem Muster mit — nichts wird gespeichert " +
                        "oder übertragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
                PermissionRow(
                    label = "Bedienungshilfen-Zugriff",
                    granted = accessibilityEnabled,
                    onClick = { showAccessibilityExplainer = true }
                )
                PermissionRow(
                    label = "Über anderen Apps anzeigen",
                    granted = overlayGranted,
                    onClick = { showOverlayExplainer = true }
                )
                Text(
                    "Ausgeschlossene Apps verwalten",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 14.sp),
                    color = ColorAccent,
                    modifier = Modifier
                        .clickable(onClick = onExcludedAppsClick)
                        .padding(top = 4.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("Wie streng soll die Reifezeit sein?")
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Strictness.entries.forEach { option ->
                        StrictnessOption(
                            option = option,
                            selected = settings.strictnessEnum == option,
                            onClick = { viewModel.updateStrictness(option) }
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(26.dp))
                    .padding(18.dp)
            ) {
                Text(
                    "WARTEZEIT-STAFFEL BEI „${settings.strictnessEnum.label}“",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorNeutral600
                )
                Column(
                    modifier = Modifier.padding(top = 13.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    val tiers = settings.waitTimeConfig.toWaitTiers()
                    val ranges = listOf(20.0 to "bis 20 €", 50.0 to "20–50 €", 150.0 to "50–150 €", 400.0 to "150–400 €", 401.0 to "über 400 €")
                    ranges.forEach { (price, label) ->
                        val hours = calcWaitHours(price, tiers, settings.strictnessEnum.factor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = ColorNeutral700)
                            Text(formatWaitLabel(hours), style = TextStyle(fontFamily = HeadingFont, fontSize = 14.sp))
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("Sparschwein-Stufen")
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    PIGGY_STAGES.forEachIndexed { index, stage ->
                        StageRow(
                            index = index + 1,
                            name = stage.name,
                            threshold = stage.threshold,
                            highlighted = stage == uiState.currentStage
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSurface, RoundedCornerShape(26.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = ColorNeutral700)
                Text(
                    text = if (checking) "Suche läuft …" else "Nach Updates suchen",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp),
                    color = ColorAccent,
                    modifier = Modifier.clickable(enabled = !checking) {
                        checking = true
                        scope.launch {
                            updateInfo = UpdateChecker.checkForUpdate(
                                BuildConfig.UPDATE_REPO_OWNER,
                                BuildConfig.UPDATE_REPO_NAME,
                                BuildConfig.VERSION_NAME
                            )
                            checking = false
                            checkedOnce = true
                        }
                    }
                )
                if (checkedOnce && updateInfo == null && !checking) {
                    Text("Du hast die neueste Version.", style = MaterialTheme.typography.bodySmall, color = ColorNeutral600)
                }
            }
        }
    }
}

@Composable
private fun PermissionExplainerDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBg, RoundedCornerShape(28.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = TextStyle(fontFamily = HeadingFont, fontSize = 20.sp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = ColorNeutral700)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorAccent, RoundedCornerShape(50))
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Zu den Einstellungen", style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp), color = ColorBg)
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Text(
                    secondaryActionLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorAccent,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSecondaryAction)
                        .padding(vertical = 4.dp)
                )
            }
            Text(
                "Abbrechen",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorNeutral600,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = ColorText.copy(alpha = 0.7f))
}

@Composable
private fun PillInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    trailingText: String
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ColorText, fontSize = 16.sp),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(ColorAccent),
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorSurface, RoundedCornerShape(50))
                .border(1.dp, ColorText.copy(alpha = 0.16f), RoundedCornerShape(50))
                .padding(start = 14.dp, end = 60.dp, top = 14.dp, bottom = 14.dp)
        )
        Text(
            trailingText,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorNeutral600,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBg, RoundedCornerShape(50))
            .clickable(enabled = !granted, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (granted) "Erlaubt" else "Aktivieren",
            style = MaterialTheme.typography.bodyMedium,
            color = if (granted) ColorNeutral600 else ColorAccent
        )
    }
}

@Composable
private fun StrictnessOption(option: Strictness, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) ColorAccent100 else ColorSurface, RoundedCornerShape(50))
            .border(1.dp, if (selected) ColorAccent else ColorText.copy(alpha = 0.16f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (selected) ColorAccent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .border(2.dp, if (selected) ColorAccent else ColorNeutral400, CircleShape)
        )
        Column {
            Text(option.label, style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp))
            Text(option.description, style = MaterialTheme.typography.bodySmall, color = ColorNeutral700)
        }
    }
}

@Composable
private fun StageRow(index: Int, name: String, threshold: Double, highlighted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) ColorAccent100 else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(22.dp))
            .padding(horizontal = 15.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Stufe $index",
            style = MaterialTheme.typography.bodySmall,
            color = ColorNeutral600,
            modifier = Modifier.padding(end = 0.dp)
        )
        Text(name, style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp), modifier = Modifier.weight(1f))
        Text(
            "ab ${formatCurrency(threshold)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlighted) ColorAccent700 else ColorNeutral600
        )
    }
}
