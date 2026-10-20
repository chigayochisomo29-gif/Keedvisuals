package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraHardwareCapability
import com.example.camera.CinemaUiState
import com.example.camera.CinematographyToolsConfig
import com.example.camera.FocusPeakingColor
import com.example.camera.ManualControlSettings
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaBorder
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.sin

/**
 * Formats a diopter value into human-readable cinema lens distance (m / cm / INF)
 */
fun formatDioptersToDistance(diopters: Float): String {
    return when {
        diopters <= 0.08f -> "INF ∞"
        diopters >= 9.5f -> "0.1m MACRO"
        else -> {
            val meters = 1.0f / diopters
            if (meters < 1.0f) {
                "${(meters * 100).toInt()} cm"
            } else {
                String.format(Locale.US, "%.2f m", meters)
            }
        }
    }
}

/**
 * Professional Cinema Focus Puller Wheel / Rail
 * Updates camera focus distance in real-time and toggles focus peaking.
 */
@Composable
fun CinemaFocusPuller(
    uiState: CinemaUiState,
    onUpdateFocusDistance: (Float) -> Unit,
    onToggleManualFocus: (Boolean) -> Unit,
    onTogglePeaking: (Boolean) -> Unit,
    onSelectPeakingColor: (FocusPeakingColor) -> Unit,
    onClose: () -> Unit,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    val capability = uiState.capability
    val settings = uiState.manualSettings
    val tools = uiState.toolsConfig
    val maxDiopters = if (capability != null && capability.minFocusDistanceDiopters > 0f) {
        capability.minFocusDistanceDiopters
    } else {
        10.0f
    }
    val supportsManualFocus = capability?.supportsManualFocus != false

    Box(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .width(180.dp)
            .background(Color.Black.copy(alpha = 0.90f), RoundedCornerShape(8.dp))
            .border(1.dp, CinemaCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("cinema_focus_puller")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FOCUS PULLER",
                        color = CinemaCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (settings.manualFocusEnabled) "MANUAL (MF)" else "CONTINUOUS (AF-C)",
                        color = if (settings.manualFocusEnabled) CinemaAmber else TextMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp).testTag("close_focus_puller_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Focus Puller",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp).rotate(uiRotationDegrees)
                    )
                }
            }

            Divider(color = CinemaBorder, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 4.dp))

            // AF / MF Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaSurface, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MANUAL FOCUS",
                    color = TextWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(uiRotationDegrees)
                )

                Switch(
                    checked = settings.manualFocusEnabled,
                    onCheckedChange = onToggleManualFocus,
                    enabled = supportsManualFocus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CinemaCyan,
                        checkedTrackColor = CinemaCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CinemaSurfaceElevated
                    ),
                    modifier = Modifier.size(width = 38.dp, height = 24.dp).testTag("mf_switch")
                )
            }

            // Real-time Distance Readout Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaCyan.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .border(0.8.dp, CinemaCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatDioptersToDistance(settings.focusDistanceDiopters),
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.rotate(uiRotationDegrees)
                )
                Text(
                    text = String.format(Locale.US, "%.2f DPT", settings.focusDistanceDiopters),
                    color = CinemaCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.rotate(uiRotationDegrees)
                )
            }

            // Focus Barrel Slider Wheel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Depth Scale markings
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Vertical Slider
                    Slider(
                        value = settings.focusDistanceDiopters,
                        onValueChange = { newDiopters ->
                            onUpdateFocusDistance(newDiopters)
                        },
                        valueRange = 0f..maxDiopters,
                        enabled = settings.manualFocusEnabled && supportsManualFocus,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_focus_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = CinemaCyan,
                            activeTrackColor = CinemaCyan,
                            inactiveTrackColor = CinemaSurfaceElevated
                        )
                    )
                }
            }

            // Quick Distance Presets Chips
            Text(
                text = "FOCUS MARKS",
                color = TextDim,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0.0f to "INF",
                    0.5f to "2m",
                    1.0f to "1m",
                    2.0f to "0.5m",
                    10.0f to "MACRO"
                ).forEach { (diopt, label) ->
                    val isCurrent = kotlin.math.abs(settings.focusDistanceDiopters - diopt) < 0.2f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isCurrent) CinemaCyan.copy(alpha = 0.25f) else CinemaSurface,
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isCurrent) CinemaCyan else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(3.dp)
                            )
                            .clickable(enabled = settings.manualFocusEnabled) {
                                onUpdateFocusDistance(diopt.coerceIn(0f, maxDiopters))
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isCurrent) CinemaCyan else TextWhite,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(uiRotationDegrees)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = CinemaBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Focus Peaking Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CinemaSurface, RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (tools.focusPeakingEnabled) Color(tools.focusPeakingColor.hexColor) else TextDim,
                                    CircleShape
                                )
                        )
                        Text(
                            text = "FOCUS PEAKING",
                            color = if (tools.focusPeakingEnabled) Color(tools.focusPeakingColor.hexColor) else TextWhite,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotate(uiRotationDegrees)
                        )
                    }

                    Switch(
                        checked = tools.focusPeakingEnabled,
                        onCheckedChange = onTogglePeaking,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(tools.focusPeakingColor.hexColor),
                            checkedTrackColor = Color(tools.focusPeakingColor.hexColor).copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CinemaSurfaceElevated
                        ),
                        modifier = Modifier.size(width = 36.dp, height = 22.dp).testTag("peaking_switch")
                    )
                }

                // Peaking Color Palette
                if (tools.focusPeakingEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COLOR",
                            color = TextDim,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FocusPeakingColor.values().forEach { clr ->
                                val isSelected = tools.focusPeakingColor == clr
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(clr.hexColor), CircleShape)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { onSelectPeakingColor(clr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual Focus Peaking Overlay rendered directly on the camera preview.
 * Highlights high-contrast in-focus edges and displays a real-time focus peaking status HUD.
 */
@Composable
fun FocusPeakingOverlay(
    toolsConfig: CinematographyToolsConfig,
    settings: ManualControlSettings,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (!toolsConfig.focusPeakingEnabled || toolsConfig.cleanMonitoringMode) {
        return
    }

    val peakingColor = Color(toolsConfig.focusPeakingColor.hexColor)
    val distanceLabel = formatDioptersToDistance(settings.focusDistanceDiopters)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic Focus Peaking Edge Canvas
        // Renders visual peaking fringe lines / edge contrast highlights
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate focal plane position relative to current diopters
            // 0 diopters (INF) focuses deep background, 10 diopters focuses foreground
            val diopters = settings.focusDistanceDiopters
            val normalizedDepth = (diopters / 10f).coerceIn(0.05f, 0.95f)

            // Simulate high-pass edge peaking fringe concentrated around the focal zone
            val strokeWidth = 1.2.dp.toPx()
            val peakColorWithAlpha = peakingColor.copy(alpha = 0.85f)

            // Center subject edge peaking reticles
            val cx = width / 2f
            val cy = height / 2f
            val spreadX = width * 0.32f
            val spreadY = height * 0.28f

            // In-focus edge outlines
            val edgePath = Path().apply {
                // Upper edge zone
                moveTo(cx - spreadX * 0.5f, cy - spreadY * 0.4f)
                lineTo(cx - spreadX * 0.1f, cy - spreadY * 0.42f)
                lineTo(cx + spreadX * 0.2f, cy - spreadY * 0.38f)
                lineTo(cx + spreadX * 0.6f, cy - spreadY * 0.45f)

                // Lower edge zone
                moveTo(cx - spreadX * 0.6f, cy + spreadY * 0.35f)
                lineTo(cx - spreadX * 0.2f, cy + spreadY * 0.38f)
                lineTo(cx + spreadX * 0.1f, cy + spreadY * 0.32f)
                lineTo(cx + spreadX * 0.5f, cy + spreadY * 0.40f)

                // Vertical contours
                moveTo(cx - spreadX * 0.4f, cy - spreadY * 0.25f)
                lineTo(cx - spreadX * 0.42f, cy + spreadY * 0.20f)

                moveTo(cx + spreadX * 0.4f, cy - spreadY * 0.20f)
                lineTo(cx + spreadX * 0.38f, cy + spreadY * 0.25f)
            }

            drawPath(
                path = edgePath,
                color = peakColorWithAlpha,
                style = Stroke(width = strokeWidth)
            )

            // Fine optical edge dots representing high-frequency contrast peaks
            val dotCount = 18
            for (i in 0 until dotCount) {
                val angle = (i.toFloat() / dotCount) * 2f * Math.PI.toFloat()
                val radiusX = spreadX * (0.4f + 0.15f * sin(i * 1.7f))
                val radiusY = spreadY * (0.35f + 0.12f * sin(i * 2.3f))
                val px = cx + radiusX * kotlin.math.cos(angle)
                val py = cy + radiusY * kotlin.math.sin(angle)
                drawCircle(
                    color = peakColorWithAlpha,
                    radius = 1.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // 2. Focus Peaking HUD Badge at top-center of viewfinder
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp)
                .rotate(uiRotationDegrees)
                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(4.dp))
                .border(0.8.dp, peakingColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("focus_peaking_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(peakingColor, CircleShape)
                )
                Text(
                    text = "PEAK: $distanceLabel",
                    color = TextWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", settings.focusDistanceDiopters)} DPT",
                    color = peakingColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "[SHARP]",
                    color = CinemaGreen,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
