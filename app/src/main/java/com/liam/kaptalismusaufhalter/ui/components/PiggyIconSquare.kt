package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent300
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent400
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent900

/** The compact piggy face used on the Sparschwein screen header card. */
@Composable
fun PiggyIconSquare(modifier: Modifier = Modifier) {
    val w = 78.dp
    val h = 64.dp
    Box(modifier = modifier.size(width = w, height = h)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .offset(x = 10.dp, y = 0.dp)
                .rotate(-16f)
                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomEnd = 2.dp, bottomStart = 7.dp))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(18.dp)
                .offset(x = w - 10.dp - 18.dp, y = 0.dp)
                .rotate(16f)
                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomEnd = 7.dp, bottomStart = 2.dp))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(width = w, height = 54.dp)
                .offset(y = h - 54.dp)
                .clip(RoundedCornerShape(topStart = 27.dp, topEnd = 27.dp, bottomEnd = 22.dp, bottomStart = 22.dp))
                .background(ColorAccent300)
        )
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 20.dp)
                .offset(x = (w - 28.dp) / 2, y = h - 6.dp - 20.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .offset(x = 19.dp, y = h - 29.dp - 5.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )
        Box(
            modifier = Modifier
                .size(5.dp)
                .offset(x = w - 19.dp - 5.dp, y = h - 29.dp - 5.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )
    }
}
