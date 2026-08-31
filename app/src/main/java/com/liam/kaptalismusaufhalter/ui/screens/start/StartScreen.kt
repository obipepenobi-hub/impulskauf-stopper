package com.liam.kaptalismusaufhalter.ui.screens.start

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.ui.components.PiggyIcon
import com.liam.kaptalismusaufhalter.ui.components.ProgressRing
import com.liam.kaptalismusaufhalter.ui.components.RipeningCard
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent100
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2100
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2800
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent700
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent800
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont

@Composable
fun StartScreen(
    viewModel: StartViewModel = viewModel(),
    onWishClick: (Long) -> Unit,
    onSeeAllClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val now = System.currentTimeMillis()
    val readyWish = state.ripeningPreview.firstOrNull { it.unlockAt <= now }
    val ripeningNotReady = state.ripeningPreview.filter { it.unlockAt > now }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("DEIN SPARSCHWEIN", style = MaterialTheme.typography.labelLarge, color = ColorNeutral600)
                    Text("Moin!", style = MaterialTheme.typography.titleLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(ColorAccent2100, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Stufe ${stageNumber(state.stage.name)} · ${state.stage.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorAccent2800
                        )
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Tune, contentDescription = "Einstellungen", tint = ColorNeutral700)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val progress = state.nextStage?.let { next ->
                    val span = (next.threshold - state.stage.threshold).coerceAtLeast(1.0)
                    ((state.total - state.stage.threshold) / span).toFloat()
                } ?: 1f

                ProgressRing(progress = progress) {
                    PiggyIcon()
                }

                Column {
                    Text(formatCurrency(state.total), style = MaterialTheme.typography.headlineMedium)
                    if (state.workHours > 0) {
                        Text(
                            "= ${"%.1f".format(state.workHours)} Arbeitsstunden,\ndie du nicht hergegeben hast.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorNeutral700,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    state.nextStage?.let {
                        val remaining = (it.threshold - state.total).coerceAtLeast(0.0)
                        Text(
                            "Noch ${formatCurrency(remaining)} bis ${it.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorAccent700,
                            modifier = Modifier.padding(top = 9.dp)
                        )
                    }
                }
            }
        }

        readyWish?.let { wish ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorAccent, RoundedCornerShape(26.dp))
                        .clickable { onWishClick(wish.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(ColorBg.copy(alpha = 0.26f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.HourglassBottom, contentDescription = null, tint = ColorBg)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${wish.name} ist reif",
                            style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp),
                            color = ColorBg
                        )
                        Text(
                            "Wartezeit vorbei — jetzt entscheiden",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorBg.copy(alpha = 0.85f)
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ColorBg)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text("Reift gerade", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Alle ${state.pendingCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }

        if (ripeningNotReady.isEmpty() && readyWish == null) {
            item {
                Text(
                    "Nichts reift gerade.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorNeutral700
                )
            }
        } else {
            items(ripeningNotReady) { wish ->
                // Not ready yet - tapping mirrors the design's behavior of landing on the Reift list,
                // not the decision screen (which only opens for wishes whose wait time is up).
                RipeningCard(wish = wish, hourlyWage = state.hourlyWage, onClick = onSeeAllClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                StatTileDesign(
                    value = state.skippedCount.toString(),
                    label = "Käufe verhindert",
                    background = ColorAccent2100,
                    textColor = ColorAccent2800,
                    modifier = Modifier.weight(1f)
                )
                StatTileDesign(
                    value = "%.1f".format(state.workHours).replace(".", ","),
                    label = "Stunden zurückgeholt",
                    background = ColorAccent100,
                    textColor = ColorAccent800,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatTileDesign(
    value: String,
    label: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(background, RoundedCornerShape(24.dp))
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Column {
            Text(value, style = TextStyle(fontFamily = HeadingFont, fontSize = 26.sp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = textColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun stageNumber(name: String): Int =
    when (name) {
        "Ferkel" -> 1
        "Sparferkel" -> 2
        "Prachtsau" -> 3
        "Goldschwein" -> 4
        "Zuchtlegende" -> 5
        else -> 1
    }
