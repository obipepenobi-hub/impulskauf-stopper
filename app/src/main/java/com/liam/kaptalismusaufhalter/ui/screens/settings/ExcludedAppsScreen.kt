package com.liam.kaptalismusaufhalter.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.ui.components.BackHeader
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600

@Composable
fun ExcludedAppsScreen(onBack: () -> Unit, viewModel: ExcludedAppsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { BackHeader("Ausgeschlossene Apps", onBack) }
        item {
            Text(
                "Der Impulskauf-Stopper löst in diesen Apps nicht aus.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorNeutral600,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )
        }
        if (state.loading) {
            item {
                Text("Lade installierte Apps …", style = MaterialTheme.typography.bodyMedium, color = ColorNeutral600)
            }
        } else {
            items(state.apps, key = { it.packageName }) { app ->
                val excluded = app.packageName in state.excludedPackages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = excluded,
                        onCheckedChange = { viewModel.setExcluded(app.packageName, it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ColorAccent)
                    )
                }
            }
        }
    }
}
