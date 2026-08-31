package com.liam.kaptalismusaufhalter.ui.screens.newwish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NewWishScreen(
    viewModel: NewWishViewModel = viewModel(),
    onSaved: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    LaunchedEffect(saved) {
        if (saved) {
            viewModel.resetSaved()
            onSaved()
        }
    }

    val price = priceText.replace(",", ".").toDoubleOrNull()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Neuer Wunsch", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Was möchtest du kaufen?") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Preis (€)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text("Link (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (price != null && price > 0) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val hours = viewModel.workHoursFor(price)
                    val waitHours = viewModel.waitHoursFor(price)
                    Text("Das sind ${"%.1f".format(hours)} Arbeitsstunden für dich")
                    Text("Wartezeit bis zur Entscheidung: ${formatDuration(waitHours)}")
                }
            }
        }

        Button(
            onClick = { viewModel.save(name.trim(), price ?: 0.0, link) },
            enabled = name.isNotBlank() && price != null && price > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wunsch einreifen lassen")
        }
    }
}

private fun formatDuration(hours: Int): String {
    val days = hours / 24
    val remHours = hours % 24
    return if (days > 0) "$days Tg $remHours Std" else "$hours Std"
}
