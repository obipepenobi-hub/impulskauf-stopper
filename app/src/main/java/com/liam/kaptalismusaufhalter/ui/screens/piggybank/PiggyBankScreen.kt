package com.liam.kaptalismusaufhalter.ui.screens.piggybank

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.data.PiggyEntryWithWish
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import com.liam.kaptalismusaufhalter.ui.components.PiggyIconSquare
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2100
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2200
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2700
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2800
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PiggyBankScreen(viewModel: PiggyBankViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorAccent2100, RoundedCornerShape(32.dp))
                    .padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PiggyIconSquare()
                Column {
                    Text(formatCurrency(state.total), style = TextStyle(fontFamily = HeadingFont, fontSize = 36.sp))
                    Text(
                        "${state.history.size} Einzahlungen · ${"%.1f".format(state.workHours).replace(".", ",")} Std",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorAccent2800,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        item {
            Text("Verlauf", style = MaterialTheme.typography.titleMedium)
        }

        if (state.history.isEmpty()) {
            item {
                Text(
                    "Noch nichts im Sparschwein.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorNeutral600
                )
            }
        } else {
            items(state.history) { entry ->
                HistoryRow(entry, state.hourlyWage)
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: PiggyEntryWithWish, hourlyWage: Double) {
    val hours = calcWorkHours(entry.amount, hourlyWage)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(ColorAccent2200, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = ColorAccent2800)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.wishName, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${SimpleDateFormat("d. MMM yyyy", Locale.GERMANY).format(Date(entry.timestamp))} · ${"%.1f".format(hours).replace(".", ",")} Std",
                style = MaterialTheme.typography.bodySmall,
                color = ColorNeutral600,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Text(
            "+${formatCurrency(entry.amount)}",
            style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp),
            color = ColorAccent2700
        )
    }
}
