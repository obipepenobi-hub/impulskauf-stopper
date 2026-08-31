package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.ui.theme.ColorAccent
import com.liam.kaptalismusaufhalter.ui.theme.ColorBg
import com.liam.kaptalismusaufhalter.ui.theme.ColorText

/**
 * A filled ring (conic-split disc with a cutout hole), matching the design's
 * `conic-gradient(from 180deg, accent 0 p%, track p% 100%)` piggy-progress ring —
 * not a thin stroked arc.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    holeSize: Dp = 116.dp,
    holeColor: Color = ColorBg,
    centerContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val diameter = kotlin.math.min(this.size.width, this.size.height)
            val topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            val sweep = 360f * progress.coerceIn(0f, 1f)

            // track (remainder)
            drawArc(
                color = ColorText.copy(alpha = 0.10f),
                startAngle = 90f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            // progress wedge, starting at the bottom (CSS conic-gradient "from 180deg")
            drawArc(
                color = ColorAccent,
                startAngle = 90f,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }
        Box(
            modifier = Modifier
                .size(holeSize)
                .background(holeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            centerContent()
        }
    }
}
