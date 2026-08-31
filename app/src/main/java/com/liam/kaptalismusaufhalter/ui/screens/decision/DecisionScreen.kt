package com.liam.kaptalismusaufhalter.ui.screens.decision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DecisionScreen(
    wishId: Long,
    onDone: () -> Unit
) {
    val viewModel: DecisionViewModel = viewModel(factory = DecisionViewModel.factory(wishId))
    val wish by viewModel.wish.collectAsState()
    val decided by viewModel.decided.collectAsState()

    LaunchedEffect(decided) {
        if (decided) onDone()
    }

    val current = wish ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Zeit für eine Entscheidung", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
        Text(current.name, style = MaterialTheme.typography.titleLarge)
        Text(
            NumberFormat.getCurrencyInstance(Locale.GERMANY).format(current.price),
            style = MaterialTheme.typography.titleMedium
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.decide(bought = true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Gekauft")
            }
            Button(
                onClick = { viewModel.decide(bought = false) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Doch nicht gekauft")
            }
        }
    }
}
