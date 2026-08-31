package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.data.Wish
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RipeningCard(wish: Wish, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val total = (wish.unlockAt - wish.createdAt).coerceAtLeast(1)
    val elapsed = (now - wish.createdAt).coerceIn(0, total)
    val progress = elapsed.toFloat() / total.toFloat()
    val remaining = (wish.unlockAt - now).coerceAtLeast(0)

    Card(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(wish.name, style = MaterialTheme.typography.titleMedium)
                Text(formatCurrency(wish.price), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (remaining <= 0) "Bereit" else "Noch ${formatRemaining(remaining)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatRemaining(millis: Long): String {
    val totalSeconds = millis / 1000
    val days = TimeUnit.SECONDS.toDays(totalSeconds)
    val hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60
    val seconds = totalSeconds % 60
    return when {
        days > 0 -> "$days Tg $hours Std"
        hours > 0 -> "$hours Std $minutes Min"
        minutes > 0 -> "$minutes Min $seconds Sek"
        else -> "$seconds Sek"
    }
}

fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale.GERMANY).format(value)
