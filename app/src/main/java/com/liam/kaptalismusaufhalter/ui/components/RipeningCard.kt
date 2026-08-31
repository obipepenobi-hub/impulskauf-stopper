package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.data.Wish
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2700
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.ColorText
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RipeningCard(wish: Wish, hourlyWage: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val total = (wish.unlockAt - wish.createdAt).coerceAtLeast(1)
    val elapsed = (now - wish.createdAt).coerceIn(0, total)
    val progress = elapsed.toFloat() / total.toFloat()
    val remaining = (wish.unlockAt - now).coerceAtLeast(0)
    val hours = calcWorkHours(wish.price, hourlyWage)

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    wish.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    "${formatCurrency(wish.price)} · ${"%.1f".format(hours)} Std",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorNeutral700
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(9.dp)
                        .background(ColorText.copy(alpha = 0.09f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(9.dp)
                            .background(ColorAccent2, RoundedCornerShape(50))
                    )
                }
                Text(
                    text = if (remaining <= 0) "reif" else formatRemaining(remaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorAccent2700,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 74.dp)
                )
            }
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
