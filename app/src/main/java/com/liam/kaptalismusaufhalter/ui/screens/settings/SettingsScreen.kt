package com.liam.kaptalismusaufhalter.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.BuildConfig
import com.liam.kaptalismusaufhalter.data.toWaitTiers
import com.liam.kaptalismusaufhalter.update.UpdateAvailableDialog
import com.liam.kaptalismusaufhalter.update.UpdateChecker
import com.liam.kaptalismusaufhalter.update.UpdateInfo
import com.liam.kaptalismusaufhalter.update.UpdateInstaller
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var wageText by remember(settings.hourlyWage) { mutableStateOf(settings.hourlyWage.toString()) }
    var checking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var checkedOnce by remember { mutableStateOf(false) }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onDownload = {
                UpdateInstaller.download(context, info)
                updateInfo = null
            },
            onDismiss = { updateInfo = null }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = wageText,
            onValueChange = { wageText = it },
            label = { Text("Stundenlohn (netto, €)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = {
                wageText.replace(",", ".").toDoubleOrNull()?.let { viewModel.updateHourlyWage(it) }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stundenlohn speichern")
        }

        HorizontalDivider()

        Text("Wartezeit-Staffel", style = MaterialTheme.typography.titleMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                settings.waitTimeConfig.toWaitTiers().forEach { tier ->
                    val label = if (tier.maxPrice != null) "bis %.0f €".format(tier.maxPrice) else "darüber"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        Text("${tier.waitHours} Std")
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = {
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
            },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (checking) "Suche läuft …" else "Nach Updates suchen")
        }
        if (checkedOnce && updateInfo == null && !checking) {
            Text("Du hast die neueste Version.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
