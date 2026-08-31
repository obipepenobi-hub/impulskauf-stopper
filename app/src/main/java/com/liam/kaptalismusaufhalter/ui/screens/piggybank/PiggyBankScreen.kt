package com.liam.kaptalismusaufhalter.ui.screens.piggybank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.data.PiggyEntryWithWish
import com.liam.kaptalismusaufhalter.ui.components.ProgressRing
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PiggyBankScreen(viewModel: PiggyBankViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progress = state.nextStage?.let { next ->
                    val span = (next.threshold - state.stage.threshold).coerceAtLeast(1.0)
                    ((state.total - state.stage.threshold) / span).toFloat()
                } ?: 1f

                ProgressRing(progress = progress) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            formatCurrency(state.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Stufe ${state.stage.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Text("Verlauf", style = MaterialTheme.typography.titleMedium)
        }

        items(state.history) { entry ->
            HistoryRow(entry)
        }
    }
}

@Composable
private fun HistoryRow(entry: PiggyEntryWithWish) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(entry.wishName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(formatCurrency(entry.amount), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)
