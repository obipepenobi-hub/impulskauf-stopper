package com.liam.kaptalismusaufhalter.guard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liam.kaptalismusaufhalter.ImpulskaufApp
import com.liam.kaptalismusaufhalter.data.Settings
import com.liam.kaptalismusaufhalter.data.toWaitTiers
import com.liam.kaptalismusaufhalter.domain.calcWaitHours
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import com.liam.kaptalismusaufhalter.ui.components.PiggyIconSquare
import com.liam.kaptalismusaufhalter.ui.components.formatWaitLabel
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral300
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral600
import com.liam.kaptalismusaufhalter.ui.theme.ColorNeutral700
import com.liam.kaptalismusaufhalter.ui.theme.ColorSurface
import com.liam.kaptalismusaufhalter.ui.theme.ColorText
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont

@Composable
fun ImpulsPopupOverlay(
    detectedPrice: Double?,
    sourceAppLabel: String,
    onRipen: (name: String, price: Double) -> Unit,
    onBuyAnyway: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(Settings()) }
    LaunchedEffect(Unit) {
        settings = (context.applicationContext as ImpulskaufApp).database.settingsDao().get() ?: Settings()
    }

    // No product-name extraction is attempted (too unreliable to guess reliably) - pre-fill
    // with where it came from so entries in Reift/Sparschwein aren't all identically labeled,
    // and so the manual fallback isn't a totally blank field to type into.
    var name by remember { mutableStateOf("Kauf bei $sourceAppLabel") }
    var priceText by remember { mutableStateOf(detectedPrice?.let { "%.2f".format(it).replace(".", ",") } ?: "") }
    val manualPrice = priceText.replace(",", ".").toDoubleOrNull()
    val price = detectedPrice ?: manualPrice

    val workHours = price?.let { calcWorkHours(it, settings.hourlyWage) } ?: 0.0
    val animatedHours by animateFloatAsState(
        targetValue = workHours.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "workHours"
    )
    val progress = if (workHours > 0) (animatedHours / workHours.toFloat()).coerceIn(0f, 1f) else 0f

    val waitHours = price?.let {
        calcWaitHours(it, settings.waitTimeConfig.toWaitTiers(), settings.strictnessEnum.factor)
    } ?: 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorText.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBg, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(28.dp)) { PiggyIconSquare() }
                Text(
                    "IMPULSKAUF-STOPPER",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorNeutral600
                )
            }

            Text(
                "Moment. Willst du das wirklich?",
                style = TextStyle(fontFamily = HeadingFont, fontSize = 24.sp)
            )

            if (detectedPrice != null) {
                Column {
                    Text(
                        "%.1f".format(animatedHours).replace(".", ","),
                        style = TextStyle(fontFamily = HeadingFont, fontSize = 40.sp, color = ColorAccent)
                    )
                    Text(
                        "Arbeitsstunden bei ${"%.0f".format(settings.hourlyWage)} €/Std netto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorNeutral700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SegmentedBar(progress = progress)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kein Preis erkannt — trag kurz nach, was du kaufen willst:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorNeutral700
                    )
                    OverlayField(value = name, onValueChange = { name = it }, placeholder = "Was kaufst du?")
                    OverlayField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        placeholder = "Preis (€)",
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }

            if (waitHours > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorSurface, RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        "Vorschlag: ${formatWaitLabel(waitHours)} reifen lassen — dann entscheidest du nochmal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorNeutral700
                    )
                }
            }

            val canRipen = price != null && price > 0 && (detectedPrice != null || name.isNotBlank())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (canRipen) ColorAccent else ColorAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable(enabled = canRipen) {
                        val finalName = name.ifBlank { "Impulskauf" }
                        price?.let { onRipen(finalName, it) }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Reifen lassen", style = TextStyle(fontFamily = HeadingFont, fontSize = 16.sp), color = ColorBg)
            }
            Text(
                "Trotzdem kaufen",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorNeutral600,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBuyAnyway)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SegmentedBar(progress: Float, segments: Int = 10) {
    val filled = (progress * segments).toInt().coerceIn(0, segments)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        if (index < filled) ColorAccent else ColorNeutral300,
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun OverlayField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorSurface, RoundedCornerShape(50))
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
