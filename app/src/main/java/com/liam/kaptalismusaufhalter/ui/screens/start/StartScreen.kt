package com.liam.kaptalismusaufhalter.ui.screens.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.ui.components.ProgressRing
import com.liam.kaptalismusaufhalter.ui.components.StatTile
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StartScreen(
    viewModel: StartViewModel = viewModel(),
    onWishClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currency = remember(state.total) { formatCurrency(state.total) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progress = state.nextStage?.let { next ->
                    val prevThreshold = state.stage.threshold
                    val span = (next.threshold - prevThreshold).coerceAtLeast(1.0)
                    ((state.total - prevThreshold) / span).toFloat()
                } ?: 1f

                ProgressRing(progress = progress) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currency, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Stufe ${state.stage.name}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                state.nextStage?.let {
                    Text(
                        "Nächste Stufe: ${it.name} bei ${formatCurrency(it.threshold)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Nicht ausgegeben",
                    value = currency,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Reift gerade",
                    value = state.ripeningPreview.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.ripeningPreview.isNotEmpty()) {
            item {
                Text("Reift gerade", style = MaterialTheme.typography.titleMedium)
            }
            items(state.ripeningPreview) { wish ->
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onWishClick(wish.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(wish.name, modifier = Modifier.weight(1f))
                        Text(formatCurrency(wish.price))
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    return format.format(value)
}
