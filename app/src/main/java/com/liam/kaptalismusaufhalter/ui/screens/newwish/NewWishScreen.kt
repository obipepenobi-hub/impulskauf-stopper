package com.liam.kaptalismusaufhalter.ui.screens.newwish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.ui.components.BackHeader
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.components.formatFreeAt
import com.liam.kaptalismusaufhalter.ui.components.formatWaitLabel
import com.liam.kaptalismusaufhalter.ui.components.formatWorkPhrase
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2100
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2200
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent2800
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.ColorText
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont

@Composable
fun NewWishScreen(
    viewModel: NewWishViewModel = viewModel(),
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    LaunchedEffect(saved) {
        if (saved) {
            viewModel.resetSaved()
            onSaved()
        }
    }

    val price = priceText.replace(",", ".").toDoubleOrNull()
    val hasPrice = price != null && price > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BackHeader("Neuer Wunsch", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LabeledField("Was willst du haben?") {
                PillTextField(value = name, onValueChange = { name = it }, placeholder = "z. B. Rucksack, 30 l")
            }
            LabeledField("Was kostet es?") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    PillTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        placeholder = "0",
                        keyboardType = KeyboardType.Decimal,
                        trailingPadding = 44.dp
                    )
                    Text(
                        "€",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ColorNeutral600,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                    )
                }
            }
            LabeledField("Wo hast du's gesehen? (optional)") {
                PillTextField(value = link, onValueChange = { link = it }, placeholder = "Link oder Laden")
            }
        }

        if (hasPrice && price != null) {
            val workHours = viewModel.workHoursFor(price)
            val waitHours = viewModel.waitHoursFor(price)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorAccent2100, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Text(
                    "UMGERECHNET",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorAccent2800
                )
                Text(
                    "${"%.1f".format(workHours).replace(".", ",")} Arbeitsstunden",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 30.sp),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "${formatCurrency(price)} bei ${"%.2f".format(settings.hourlyWage).replace(".", ",")} € pro Stunde. Das ist ${formatWorkPhrase(workHours)}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorAccent2800,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ColorAccent2800.copy(alpha = 0.2f))
                        .padding(vertical = 16.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ColorAccent2200, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.HourglassBottom, contentDescription = null, tint = ColorAccent2800)
                    }
                    Column {
                        Text(
                            "Reifezeit ${formatWaitLabel(waitHours)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "frei ab ${formatFreeAt(waitHours)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorAccent2800
                        )
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        Text(
            "Kein Verbot — nur eine Pause. Wenn du's danach noch willst, kauf es mit ruhigem Gewissen.",
            style = MaterialTheme.typography.bodySmall,
            color = ColorNeutral600
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (hasPrice && name.isNotBlank()) ColorAccent else ColorAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                .clickable(enabled = hasPrice && name.isNotBlank()) {
                    viewModel.save(name.trim(), price ?: 0.0, link)
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Reifen lassen", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp), color = ColorBg)
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ColorText.copy(alpha = 0.7f))
        content()
    }
}

@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingPadding: androidx.compose.ui.unit.Dp = 14.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(50))
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
                .padding(start = 14.dp, end = trailingPadding, top = 14.dp, bottom = 14.dp)
        )
    }
}
