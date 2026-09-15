package com.liam.kaptalismusaufhalter.ui.screens.decision

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liam.kaptalismusaufhalter.domain.calcWorkHours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.liam.kaptalismusaufhalter.ui.components.formatCurrency
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent300
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent400
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent900
import com.liam.kaptalismusaufhalter.ui.theme.ColorDecisionBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorDecisionText
import com.liam.kaptalismusaufhalter.ui.theme.ColorDecisionText300
import com.liam.kaptalismusaufhalter.ui.theme.ColorDecisionText400
import com.liam.kaptalismusaufhalter.ui.theme.HeadingFont
import java.util.concurrent.TimeUnit

@Composable
fun DecisionScreen(
    wishId: Long,
    onDone: () -> Unit
) {
    val viewModel: DecisionViewModel = viewModel(factory = DecisionViewModel.factory(wishId))
    val wish by viewModel.wish.collectAsState()
    val decided by viewModel.decided.collectAsState()
    val piggyTotal by viewModel.piggyTotal.collectAsState()
    val hourlyWage by viewModel.hourlyWage.collectAsState()

    LaunchedEffect(decided) {
        if (decided) onDone()
    }

    val current = wish ?: return
    val hours = calcWorkHours(current.price, hourlyWage)
    val waitedDays = TimeUnit.MILLISECONDS.toDays(
        (System.currentTimeMillis() - current.createdAt).coerceAtLeast(0)
    ).coerceAtLeast(1)

    var thumbnail by remember(current.imageUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(current.imageUrl) {
        thumbnail = current.imageUrl?.let { path ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorDecisionBg)
            .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmallPiggyGlyph()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "IM SCHWEIN",
                    style = MaterialTheme.typography.labelLarge,
                    color = ColorDecisionText400
                )
                Text(
                    formatCurrency(piggyTotal),
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 20.sp, color = ColorDecisionText),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(ColorDecisionText.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text("Wartezeit vorbei", style = MaterialTheme.typography.bodySmall, color = ColorDecisionText300)
            }
        }

        Column {
            Text(
                "Willst du es\nimmer noch?",
                style = TextStyle(fontFamily = HeadingFont, fontSize = 32.sp, lineHeight = 36.sp, color = ColorDecisionText)
            )
            Text(
                "Du hast $waitedDays ${if (waitedDays == 1L) "Tag" else "Tage"} gewartet. Es gibt kein falsches Ergebnis — beides ist okay.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorDecisionText300,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorDecisionText.copy(alpha = 0.08f), RoundedCornerShape(30.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ColorDecisionText.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = thumbnail
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        Text("FOTO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ColorDecisionText300, textAlign = TextAlign.Center)
                    }
                }
                Column {
                    Text(current.name, style = TextStyle(fontFamily = HeadingFont, fontSize = 20.sp, color = ColorDecisionText))
                    current.linkUrl?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = ColorDecisionText400,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorDecisionText.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp)
                ) {
                    Text(formatCurrency(current.price), style = TextStyle(fontFamily = HeadingFont, fontSize = 24.sp, color = ColorDecisionText))
                    Text("Preis", style = MaterialTheme.typography.labelSmall, color = ColorDecisionText400, modifier = Modifier.padding(top = 3.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorAccent400.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp)
                ) {
                    Text("%.1f".format(hours).replace(".", ","), style = TextStyle(fontFamily = HeadingFont, fontSize = 24.sp, color = ColorAccent400))
                    Text("Arbeitsstunden", style = MaterialTheme.typography.labelSmall, color = ColorAccent300, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorAccent400, RoundedCornerShape(32.dp))
                .clickable { viewModel.decide(bought = false) }
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Spa, contentDescription = null, tint = ColorAccent900)
                Text(
                    "Doch nicht gekauft",
                    style = TextStyle(fontFamily = HeadingFont, fontSize = 19.sp, color = ColorAccent900)
                )
            }
        }
        Text(
            "${formatCurrency(current.price)} fliegen dann ins Sparschwein",
            style = MaterialTheme.typography.bodySmall,
            color = ColorDecisionText300,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ColorDecisionText.copy(alpha = 0.28f), RoundedCornerShape(50))
                .clickable { viewModel.decide(bought = true) }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Gekauft", style = TextStyle(fontFamily = HeadingFont, fontSize = 15.sp, color = ColorDecisionText))
        }
    }
}

@Composable
private fun SmallPiggyGlyph() {
    Box(modifier = Modifier.size(width = 52.dp, height = 42.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .offset(x = 6.dp, y = 0.dp)
                .rotate(-16f)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 2.dp, bottomStart = 6.dp))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .offset(x = 52.dp - 6.dp - 14.dp, y = 0.dp)
                .rotate(16f)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 6.dp, bottomStart = 2.dp))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 36.dp)
                .offset(y = 6.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 15.dp, bottomStart = 15.dp))
                .background(ColorAccent300)
        )
        Box(
            modifier = Modifier
                .size(width = 19.dp, height = 13.dp)
                .offset(x = (52.dp - 19.dp) / 2, y = 42.dp - 4.dp - 13.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .offset(x = 13.dp, y = 42.dp - 19.dp - 4.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .offset(x = 52.dp - 13.dp - 4.dp, y = 42.dp - 19.dp - 4.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )
    }
}
