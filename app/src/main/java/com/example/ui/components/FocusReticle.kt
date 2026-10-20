package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan

@Composable
fun FocusReticle(
    x: Float,
    y: Float,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(x, y) {
        scale.snapTo(1.4f)
        alpha.snapTo(1f)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        kotlinx.coroutines.delay(1200)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 400)
        )
        onAnimationEnd()
    }

    if (alpha.value > 0.01f) {
        val boxSizeDp = 64.dp
        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        (x - (boxSizeDp.toPx() * scale.value) / 2f).toInt(),
                        (y - (boxSizeDp.toPx() * scale.value) / 2f).toInt()
                    )
                }
                .size(boxSizeDp * scale.value)
        ) {
            Canvas(modifier = Modifier.size(boxSizeDp * scale.value)) {
                val strokeWidth = 1.5.dp.toPx()
                val reticleColor = CinemaCyan.copy(alpha = alpha.value)
                val cornerLength = size.width * 0.22f

                // Outer focus square corners
                // Top-left
                drawLine(reticleColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                drawLine(reticleColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
                // Top-right
                drawLine(reticleColor, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                drawLine(reticleColor, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
                // Bottom-left
                drawLine(reticleColor, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                drawLine(reticleColor, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
                // Bottom-right
                drawLine(reticleColor, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                drawLine(reticleColor, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)

                // Subtle center dot
                drawCircle(
                    color = CinemaAmber.copy(alpha = alpha.value),
                    radius = 2.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }
    }
}
