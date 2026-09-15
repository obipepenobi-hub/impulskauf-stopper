package com.liam.kaptalismusaufhalter.ui.screens.reift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.data.Wish
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.components.formatRemaining
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2600
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent700
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral300
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.ColorText
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont

@Composable
fun ReiftScreen(
    viewModel: ReiftViewModel = viewModel(),
    onWishClick: (Long) -> Unit
) {
    val wishes by viewModel.pendingWishes.collectAsState()
    val hourlyWage by viewModel.hourlyWage.collectAsState()
    val totalOnTheLine = wishes.sumOf { it.price }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text("Wartet gerade", style = TextStyle(fontFamily = HeadingFont, fontSize = 26.sp))
                if (wishes.isNotEmpty()) {
                    Text(
                        "${wishes.size} ${if (wishes.size == 1) "Wunsch" else "Wünsche"} · ${formatCurrency(totalOnTheLine)} auf der Kippe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorNeutral700,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            QuickAddCard(onAdd = viewModel::quickAdd)
        }

        if (wishes.isEmpty()) {
            item {
                Text(
                    "Nichts wartet gerade. Leg oben einen Wunsch an.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorNeutral700
                )
            }
        } else {
            items(wishes) { wish ->
                ReiftItemCard(wish = wish, hourlyWage = hourlyWage, onClick = { onWishClick(wish.id) })
            }
        }
    }
}

@Composable
private fun ReiftItemCard(wish: Wish, hourlyWage: Double, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val total = (wish.unlockAt - wish.createdAt).coerceAtLeast(1)
    val elapsed = (now - wish.createdAt).coerceIn(0, total)
    val progress = elapsed.toFloat() / total.toFloat()
    val remaining = (wish.unlockAt - now).coerceAtLeast(0)
    val ready = remaining <= 0
    val hours = calcWorkHours(wish.price, hourlyWage)
    val shopLabel = shopLabelFor(wish.linkUrl)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(28.dp))
            .clickable(enabled = ready, onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(ColorNeutral300, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FOTO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ColorNeutral700)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wish.name,
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 17.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        shopLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = ColorNeutral600,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatCurrency(wish.price), style = TextStyle(fontFamily = HeadingFont, fontSize = 17.sp))
                    Text(
                        "${"%.1f".format(hours).replace(".", ",")} Std",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorAccent700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(ColorText.copy(alpha = 0.09f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(10.dp)
                            .background(if (ready) ColorAccent else ColorAccent2600, RoundedCornerShape(50))
                    )
                }
                Text(
                    text = if (ready) "bereit" else formatRemaining(remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ready) ColorAccent else ColorAccent2600
                )
            }
        }
    }
}

private fun shopLabelFor(linkUrl: String?): String {
    if (linkUrl.isNullOrBlank()) return "manuell eingetragen"
    return try {
        val uri = android.net.Uri.parse(linkUrl)
        uri.host?.removePrefix("www.") ?: "manuell eingetragen"
    } catch (e: Exception) {
        "manuell eingetragen"
    }
}

@Composable
private fun QuickAddCard(onAdd: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(28.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Impuls gerade?", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp))
        QuickField(value = name, onValueChange = { name = it }, placeholder = "Was möchtest du kaufen?")
        QuickField(
            value = priceText,
            onValueChange = { priceText = it },
            placeholder = "Preis (€)",
            keyboardType = KeyboardType.Decimal
        )
        val price = priceText.replace(",", ".").toDoubleOrNull()
        val enabled = name.isNotBlank() && price != null && price > 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) ColorAccent else ColorAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable(enabled = enabled) {
                    onAdd(name.trim(), price!!)
                    name = ""
                    priceText = ""
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Warten lassen", style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp), color = ColorBg)
        }
    }
}

@Composable
private fun QuickField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBg, RoundedCornerShape(50))
            .border(1.dp, ColorText.copy(alpha = 0.16f), RoundedCornerShape(50))
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ColorText, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(ColorAccent),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = ColorNeutral600)
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}
