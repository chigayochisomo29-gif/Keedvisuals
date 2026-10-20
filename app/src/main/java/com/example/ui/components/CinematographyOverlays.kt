package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.AspectRatioGuide
import com.example.camera.CinematographyToolsConfig
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.sin

import androidx.compose.ui.draw.rotate

@Composable
fun CinematographyOverlays(
    toolsConfig: CinematographyToolsConfig,
    audioLevelDb: Float,
    audioPeakDb: Float,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (toolsConfig.cleanMonitoringMode) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        // 1. Aspect Ratio Masks (Mattes)
        if (toolsConfig.aspectRatioGuide != AspectRatioGuide.NONE && totalWidth > 0 && totalHeight > 0) {
            val targetRatio = toolsConfig.aspectRatioGuide.ratio
            val currentRatio = totalWidth / totalHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val matteColor = Color.Black.copy(alpha = 0.65f)

                if (currentRatio > targetRatio) {
                    // Screen is wider than target: pillarbox left and right
                    val targetWidth = totalHeight * targetRatio
                    val pillarWidth = (totalWidth - targetWidth) / 2f
                    drawRect(color = matteColor, topLeft = Offset(0f, 0f), size = Size(pillarWidth, totalHeight))
                    drawRect(color = matteColor, topLeft = Offset(totalWidth - pillarWidth, 0f), size = Size(pillarWidth, totalHeight))
                    // Fine guide border
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(pillarWidth, 0f),
                        size = Size(targetWidth, totalHeight),
                        style = Stroke(width = 1.dp.toPx())
                    )
                } else if (currentRatio < targetRatio) {
                    // Screen is taller than target: letterbox top and bottom
                    val targetHeight = totalWidth / targetRatio
                    val letterboxHeight = (totalHeight - targetHeight) / 2f
                    drawRect(color = matteColor, topLeft = Offset(0f, 0f), size = Size(totalWidth, letterboxHeight))
                    drawRect(color = matteColor, topLeft = Offset(0f, totalHeight - letterboxHeight), size = Size(totalWidth, letterboxHeight))
                    // Fine guide border
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(0f, letterboxHeight),
                        size = Size(totalWidth, targetHeight),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // 2. Rule of Thirds Grid
        if (toolsConfig.gridEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineColor = Color.White.copy(alpha = 0.35f)
                val strokeWidth = 1.dp.toPx()

                // Vertical lines
                drawLine(
                    color = lineColor,
                    start = Offset(size.width / 3f, 0f),
                    end = Offset(size.width / 3f, size.height),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width * 2f / 3f, 0f),
                    end = Offset(size.width * 2f / 3f, size.height),
                    strokeWidth = strokeWidth
                )

                // Horizontal lines
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height / 3f),
                    end = Offset(size.width, size.height / 3f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = lineColor,
                    start = Offset(0f, size.height * 2f / 3f),
                    end = Offset(size.width, size.height * 2f / 3f),
                    strokeWidth = strokeWidth
                )
            }
        }

        // 3. Center Crosshair Reticle
        if (toolsConfig.centerCrosshairEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val arm = 16.dp.toPx()
                val gap = 4.dp.toPx()
                val stroke = 1.5f.dp.toPx()
                val color = CinemaCyan.copy(alpha = 0.85f)

                // Horizontal
                drawLine(color, Offset(cx - arm - gap, cy), Offset(cx - gap, cy), stroke)
                drawLine(color, Offset(cx + gap, cy), Offset(cx + arm + gap, cy), stroke)
                // Vertical
                drawLine(color, Offset(cx, cy - arm - gap), Offset(cx, cy - gap), stroke)
                drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + arm + gap), stroke)

                // Center dot
                drawCircle(color, radius = 1.5f.dp.toPx(), center = Offset(cx, cy))
            }
        }

        // 4. Safe Area Guides (90% Action Safe & 80% Title Safe)
        if (toolsConfig.safeAreaEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 1.dp.toPx()
                val actionSafeColor = CinemaAmber.copy(alpha = 0.45f)
                val titleSafeColor = CinemaCyan.copy(alpha = 0.4f)

                // 90% Action Safe
                val actionInsetX = size.width * 0.05f
                val actionInsetY = size.height * 0.05f
                drawRect(
                    color = actionSafeColor,
                    topLeft = Offset(actionInsetX, actionInsetY),
                    size = Size(size.width * 0.90f, size.height * 0.90f),
                    style = Stroke(width = stroke)
                )

                // 80% Title Safe
                val titleInsetX = size.width * 0.10f
                val titleInsetY = size.height * 0.10f
                drawRect(
                    color = titleSafeColor,
                    topLeft = Offset(titleInsetX, titleInsetY),
                    size = Size(size.width * 0.80f, size.height * 0.80f),
                    style = Stroke(width = stroke)
                )
            }
        }

        // 5. Audio Level Meter (Left side vertical or horizontal cinema VU meter)
        if (toolsConfig.audioMeterEnabled) {
            CinemaAudioMeter(
                audioDb = audioLevelDb,
                peakDb = audioPeakDb,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 80.dp)
                    .rotate(uiRotationDegrees)
            )
        }

        // 6. Real-time Luminance Histogram
        if (toolsConfig.histogramEnabled) {
            CinemaHistogram(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 92.dp, bottom = 80.dp)
                    .rotate(uiRotationDegrees)
            )
        }
    }
}

@Composable
fun CinemaAudioMeter(
    audioDb: Float,
    peakDb: Float,
    modifier: Modifier = Modifier
) {
    // Range: -50 dBFS to 0 dBFS
    val normalizedLevel = ((audioDb + 50f) / 50f).coerceIn(0f, 1f)
    val normalizedPeak = ((peakDb + 50f) / 50f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag("audio_meter")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "MIC",
                color = TextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (audioDb > -50f) "${audioDb.toInt()} dB" else "-INF",
                color = if (audioDb > -3f) CinemaRed else if (audioDb > -12f) CinemaYellow else TextWhite,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Dual Channel VU Bar
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(6.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillWidth = size.width * normalizedLevel

                // Segmented gradient simulation: Green -> Yellow -> Red
                val greenWidth = (size.width * 0.70f).coerceAtMost(fillWidth)
                val yellowWidth = (size.width * 0.88f - size.width * 0.70f).coerceAtMost(
                    (fillWidth - size.width * 0.70f).coerceAtLeast(0f)
                )
                val redWidth = (fillWidth - size.width * 0.88f).coerceAtLeast(0f)

                if (greenWidth > 0) {
                    drawRect(color = CinemaGreen, topLeft = Offset(0f, 0f), size = Size(greenWidth, size.height))
                }
                if (yellowWidth > 0) {
                    drawRect(color = CinemaYellow, topLeft = Offset(size.width * 0.70f, 0f), size = Size(yellowWidth, size.height))
                }
                if (redWidth > 0) {
                    drawRect(color = CinemaRed, topLeft = Offset(size.width * 0.88f, 0f), size = Size(redWidth, size.height))
                }

                // Peak needle
                val peakX = size.width * normalizedPeak
                if (peakX > 0) {
                    drawLine(
                        color = Color.White,
                        start = Offset(peakX, 0f),
                        end = Offset(peakX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}

@Composable
fun CinemaHistogram(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 96.dp, height = 48.dp)
            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
            .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(4.dp)
            .testTag("histogram_overlay")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = size.width / 32f
            val baseLine = size.height

            val path = Path().apply {
                moveTo(0f, baseLine)
                // Cinema bell curve distribution representation
                for (i in 0..32) {
                    val x = i * step
                    val progress = i / 32f
                    // Curve peaking around 40-60% midtones with highlights roll-off
                    val factor = sin(progress * Math.PI).toFloat()
                    val noise = sin(i * 1.5).toFloat() * 0.15f
                    val y = baseLine - ((factor * 0.75f + noise).coerceIn(0.05f, 0.95f) * size.height)
                    lineTo(x, y)
                }
                lineTo(size.width, baseLine)
                close()
            }

            drawPath(path = path, color = Color.White.copy(alpha = 0.35f))
            drawPath(
                path = path,
                color = CinemaCyan.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Text(
            text = "HISTO",
            color = TextMuted,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(1.dp)
        )
    }
}
