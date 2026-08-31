package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent300
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent400
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent800
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent900

/**
 * The piggy face illustration from the design, reproduced as layered shapes
 * (the design itself builds it out of plain divs, not an image asset).
 */
@Composable
fun PiggyIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 90.dp, height = 74.dp)) {
        // ears
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(x = 12.dp, y = 0.dp)
                .rotate(-16f)
                .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomEnd = 3.dp, bottomStart = 9.dp))
                .background(ColorAccent400)
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(x = 90.dp - 12.dp - 22.dp, y = 0.dp)
                .rotate(16f)
                .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomEnd = 9.dp, bottomStart = 3.dp))
                .background(ColorAccent400)
        )

        // body
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 64.dp)
                .offset(y = 10.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 26.dp, bottomStart = 26.dp))
                .background(ColorAccent300)
        )

        // nose bridge
        Box(
            modifier = Modifier
                .size(width = 30.dp, height = 6.dp)
                .offset(x = (90.dp - 30.dp) / 2, y = 14.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorAccent800.copy(alpha = 0.3f))
        )

        // eyes
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(x = 22.dp, y = 74.dp - 34.dp - 6.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .offset(x = 90.dp - 22.dp - 6.dp, y = 74.dp - 34.dp - 6.dp)
                .clip(CircleShape)
                .background(ColorAccent900)
        )

        // snout
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 24.dp)
                .offset(x = (90.dp - 34.dp) / 2, y = 74.dp - 8.dp - 24.dp)
                .clip(RoundedCornerShape(50))
                .background(ColorAccent400),
            contentAlignment = Alignment.Center
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ColorAccent800.copy(alpha = 0.5f))
                )
                Box(modifier = Modifier.size(width = 7.dp, height = 1.dp))
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ColorAccent800.copy(alpha = 0.5f))
                )
            }
        }
    }
}
