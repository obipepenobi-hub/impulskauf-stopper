package com.liam.kaptalismusaufhalter.ui.screens.reift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.ui.components.RipeningCard

@Composable
fun ReiftScreen(
    viewModel: ReiftViewModel = viewModel(),
    onWishClick: (Long) -> Unit
) {
    val wishes by viewModel.pendingWishes.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            QuickAddCard(onAdd = viewModel::quickAdd)
        }

        if (wishes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Nichts reift gerade.", style = MaterialTheme.typography.titleMedium)
                    Text("Leg oben oder unter „Neu“ einen Wunsch an.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(wishes) { wish ->
                RipeningCard(wish = wish, onClick = { onWishClick(wish.id) })
            }
        }
    }
}

@Composable
private fun QuickAddCard(onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Impuls gerade?", style = MaterialTheme.typography.titleMedium)
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
            Button(
                onClick = {
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    if (name.isNotBlank() && price != null && price > 0) {
                        onAdd(name.trim(), price)
                        name = ""
                        priceText = ""
                    }
                },
                enabled = name.isNotBlank() && priceText.replace(",", ".").toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Einreifen lassen")
            }
        }
    }
}

