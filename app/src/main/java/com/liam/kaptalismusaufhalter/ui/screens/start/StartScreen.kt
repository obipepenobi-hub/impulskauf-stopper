package com.liam.kaptalismusaufhalter.ui.screens.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.R
import com.liam.kaptalismusaufhalter.ui.components.ProgressRing
import com.liam.kaptalismusaufhalter.ui.components.RipeningCard
import com.liam.kaptalismusaufhalter.ui.components.StatTile
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency

@Composable
fun StartScreen(
    viewModel: StartViewModel = viewModel(),
    onWishClick: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "DEIN SPARSCHWEIN",
                    style = MaterialTheme.typography.labelLarge
                )

                val progress = state.nextStage?.let { next ->
                    val prevThreshold = state.stage.threshold
                    val span = (next.threshold - prevThreshold).coerceAtLeast(1.0)
                    ((state.total - prevThreshold) / span).toFloat()
                } ?: 1f

                ProgressRing(progress = progress) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Text(
                    formatCurrency(state.total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Stufe ${state.stage.name}", style = MaterialTheme.typography.bodyMedium)
                if (state.workHours > 0) {
                    Text(
                        "= ${"%.1f".format(state.workHours)} Arbeitsstunden, die du nicht hergegeben hast.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                state.nextStage?.let {
                    val remaining = (it.threshold - state.total).coerceAtLeast(0.0)
                    Text(
                        "Noch ${formatCurrency(remaining)} bis ${it.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Nicht ausgegeben",
                    value = formatCurrency(state.total),
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
                RipeningCard(wish = wish, onClick = { onWishClick(wish.id) })
            }
        }
    }
}
