package com.liam.kaptalismusaufhalter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.liam.kaptalismusaufhalter.ui.theme.Primary
import com.liam.kaptalismusaufhalter.ui.theme.TrackColor
import kotlin.math.min

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 160.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 16.dp,
    centerContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = min(this.size.width, this.size.height) - stroke
            val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = TrackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = Primary,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        centerContent()
    }
}
