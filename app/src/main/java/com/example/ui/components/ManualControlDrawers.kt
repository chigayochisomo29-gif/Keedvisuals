package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.AspectRatioGuide
import com.example.camera.CinemaUiState
import com.example.camera.CinematographyToolsConfig
import com.example.camera.FrameRateOption
import com.example.camera.ManualControlSettings
import com.example.camera.WhiteBalancePreset
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.TimecodeFormatter
import java.util.Locale

import androidx.compose.ui.draw.rotate
import com.example.util.LocalUiRotation

@Composable
fun ManualControlDrawers(
    activeDrawer: ActiveDrawer,
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit,
    onUpdateFps: (FrameRateOption) -> Unit,
    onUpdateTools: (CinematographyToolsConfig) -> Unit,
    onCloseDrawer: () -> Unit,
    onToggleFocusWheel: () -> Unit = {},
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = activeDrawer != ActiveDrawer.NONE,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.92f))
                .border(0.8.dp, Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("manual_control_drawer")
        ) {
            when (activeDrawer) {
                ActiveDrawer.FPS -> FpsDrawerContent(
                    selectedFps = uiState.targetFps,
                    onSelectFps = {
                        onUpdateFps(it)
                        onCloseDrawer()
                    }
                )
                ActiveDrawer.SHUTTER -> ShutterDrawerContent(
                    uiState = uiState,
                    onUpdateManualSettings = onUpdateManualSettings
                )
                ActiveDrawer.ISO -> IsoDrawerContent(
                    uiState = uiState,
                    onUpdateManualSettings = onUpdateManualSettings
                )
                ActiveDrawer.WHITE_BALANCE -> WhiteBalanceDrawerContent(
                    selectedPreset = uiState.manualSettings.whiteBalancePreset,
                    onSelectPreset = {
                        onUpdateManualSettings(uiState.manualSettings.copy(whiteBalancePreset = it))
                    }
                )
                ActiveDrawer.EV -> EvCompensationDrawerContent(
                    uiState = uiState,
                    onUpdateManualSettings = onUpdateManualSettings
                )
                ActiveDrawer.FOCUS -> FocusDrawerContent(
                    uiState = uiState,
                    onUpdateManualSettings = onUpdateManualSettings,
                    onUpdateTools = onUpdateTools,
                    onToggleFocusWheel = onToggleFocusWheel
                )
                ActiveDrawer.ZOOM -> ZoomDrawerContent(
                    uiState = uiState,
                    onUpdateManualSettings = onUpdateManualSettings
                )
                ActiveDrawer.TOOLS -> ToolsDrawerContent(
                    tools = uiState.toolsConfig,
                    onUpdateTools = onUpdateTools
                )
                ActiveDrawer.NONE -> Unit
            }
        }
    }
}

@Composable
private fun FpsDrawerContent(
    selectedFps: FrameRateOption,
    onSelectFps: (FrameRateOption) -> Unit
) {
    Column {
        Text(
            text = "PROJECT FRAME RATE",
            color = CinemaAmber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrameRateOption.values().forEach { fps ->
                val isSelected = fps == selectedFps
                DrawerChip(
                    label = "${fps.fps} FPS",
                    subLabel = fps.label.split(" ")[1].replace("(", "").replace(")", ""),
                    isSelected = isSelected,
                    accentColor = CinemaAmber,
                    onClick = { onSelectFps(fps) }
                )
            }
        }
    }
}

@Composable
private fun ShutterDrawerContent(
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit
) {
    val capability = uiState.capability
    val settings = uiState.manualSettings
    val supportsManual = capability?.supportsManualSensor == true

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SHUTTER SPEED & ANGLE",
                color = CinemaCyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (!supportsManual) {
                Text(
                    text = "HARDWARE LIMITED (Helio G37)",
                    color = CinemaRed,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (!supportsManual) {
            Text(
                text = "Manual sensor exposure time requires FULL Camera2 hardware support. Helio G37 driver runs in continuous Auto Exposure mode.",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto Shutter Chip
            DrawerChip(
                label = "AUTO",
                subLabel = "Continuous",
                isSelected = !settings.manualShutterEnabled,
                accentColor = CinemaCyan,
                onClick = {
                    onUpdateManualSettings(settings.copy(manualShutterEnabled = false))
                }
            )

            // Shutter Speeds
            val shutterOptions = listOf(
                1_000_000_000L / 24L to "1/24 (360°)",
                1_000_000_000L / 48L to "1/48 (180°)",
                1_000_000_000L / 50L to "1/50 (Cinema)",
                1_000_000_000L / 60L to "1/60 (180°)",
                1_000_000_000L / 120L to "1/120 (Action)",
                1_000_000_000L / 250L to "1/250",
                1_000_000_000L / 500L to "1/500",
                1_000_000_000L / 1000L to "1/1000"
            )

            shutterOptions.forEach { (nanos, label) ->
                val isSelected = settings.manualShutterEnabled && settings.shutterNanos == nanos
                DrawerChip(
                    label = TimecodeFormatter.formatShutterSpeed(nanos),
                    subLabel = label.split(" ").getOrElse(1) { "" },
                    isSelected = isSelected,
                    accentColor = CinemaCyan,
                    enabled = supportsManual,
                    onClick = {
                        onUpdateManualSettings(
                            settings.copy(
                                manualShutterEnabled = true,
                                manualIsoEnabled = true,
                                shutterNanos = nanos
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun IsoDrawerContent(
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit
) {
    val capability = uiState.capability
    val settings = uiState.manualSettings
    val supportsManual = capability?.supportsManualSensor == true

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ISO SENSITIVITY",
                color = CinemaAmber,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (!supportsManual) {
                Text(
                    text = "HARDWARE LIMITED (Auto ISO Active)",
                    color = CinemaRed,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DrawerChip(
                label = "AUTO",
                subLabel = "Native",
                isSelected = !settings.manualIsoEnabled,
                accentColor = CinemaAmber,
                onClick = {
                    onUpdateManualSettings(settings.copy(manualIsoEnabled = false))
                }
            )

            val isoSteps = listOf(100, 200, 400, 800, 1600, 3200)
            isoSteps.forEach { iso ->
                val isSelected = settings.manualIsoEnabled && settings.isoValue == iso
                DrawerChip(
                    label = "ISO $iso",
                    subLabel = if (iso <= 400) "Base" else "Gain",
                    isSelected = isSelected,
                    accentColor = CinemaAmber,
                    enabled = supportsManual,
                    onClick = {
                        onUpdateManualSettings(
                            settings.copy(
                                manualIsoEnabled = true,
                                isoValue = iso
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WhiteBalanceDrawerContent(
    selectedPreset: WhiteBalancePreset,
    onSelectPreset: (WhiteBalancePreset) -> Unit
) {
    Column {
        Text(
            text = "WHITE BALANCE & COLOR TEMPERATURE",
            color = CinemaAmber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WhiteBalancePreset.values().forEach { preset ->
                val isSelected = preset == selectedPreset
                DrawerChip(
                    label = preset.label,
                    subLabel = "${preset.kelvin}K",
                    isSelected = isSelected,
                    accentColor = CinemaAmber,
                    onClick = { onSelectPreset(preset) }
                )
            }
        }
    }
}

@Composable
private fun EvCompensationDrawerContent(
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit
) {
    val capability = uiState.capability
    val settings = uiState.manualSettings
    val range = capability?.exposureCompensationRange ?: android.util.Range(-6, 6)
    val step = capability?.evStepValue ?: 0.333f

    val currentEv = settings.exposureCompensationIndex * step
    val currentEvStr = if (currentEv >= 0f) "+${String.format(Locale.US, "%.1f", currentEv)}" else String.format(Locale.US, "%.1f", currentEv)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPOSURE COMPENSATION: $currentEvStr EV",
                color = CinemaCyan,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RESET TO 0.0",
                color = CinemaAmber,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        onUpdateManualSettings(settings.copy(exposureCompensationIndex = 0))
                    }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${(range.lower * step).toInt()} EV",
                color = TextDim,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )

            Slider(
                value = settings.exposureCompensationIndex.toFloat(),
                onValueChange = { newIdx ->
                    onUpdateManualSettings(settings.copy(exposureCompensationIndex = newIdx.toInt()))
                },
                valueRange = range.lower.toFloat()..range.upper.toFloat(),
                steps = (range.upper - range.lower - 1).coerceAtLeast(0),
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CinemaCyan,
                    activeTrackColor = CinemaCyan,
                    inactiveTrackColor = CinemaSurfaceElevated
                )
            )

            Text(
                text = "+${(range.upper * step).toInt()} EV",
                color = TextDim,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FocusDrawerContent(
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit,
    onUpdateTools: (CinematographyToolsConfig) -> Unit,
    onToggleFocusWheel: () -> Unit = {}
) {
    val capability = uiState.capability
    val settings = uiState.manualSettings
    val tools = uiState.toolsConfig
    val supportsManualFocus = capability?.supportsManualFocus != false
    val maxDiopters = capability?.minFocusDistanceDiopters?.coerceAtLeast(10f) ?: 10f
    val rot = LocalUiRotation.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.rotate(rot)
            ) {
                Text(
                    text = if (settings.manualFocusEnabled) "MANUAL FOCUS" else "AUTOFOCUS (CONTINUOUS AF-C)",
                    color = CinemaCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                if (settings.manualFocusEnabled) {
                    Text(
                        text = "• ${formatDioptersToDistance(settings.focusDistanceDiopters)}",
                        color = TextWhite,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!supportsManualFocus) {
                Text(
                    text = "FIXED FOCUS HARDWARE",
                    color = CinemaRed,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.rotate(rot)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            DrawerChip(
                label = "AF-C",
                subLabel = "Continuous",
                isSelected = !settings.manualFocusEnabled,
                accentColor = CinemaCyan,
                onClick = {
                    onUpdateManualSettings(settings.copy(manualFocusEnabled = false))
                }
            )

            DrawerChip(
                label = "MANUAL",
                subLabel = "Slider",
                isSelected = settings.manualFocusEnabled,
                accentColor = CinemaCyan,
                enabled = supportsManualFocus,
                onClick = {
                    onUpdateManualSettings(settings.copy(manualFocusEnabled = true))
                }
            )

            DrawerChip(
                label = "PEAKING",
                subLabel = if (tools.focusPeakingEnabled) tools.focusPeakingColor.label else "Off",
                isSelected = tools.focusPeakingEnabled,
                accentColor = Color(tools.focusPeakingColor.hexColor),
                onClick = {
                    onUpdateTools(tools.copy(focusPeakingEnabled = !tools.focusPeakingEnabled))
                }
            )

            DrawerChip(
                label = "RAIL",
                subLabel = if (uiState.isFocusWheelVisible) "Wheel ON" else "Wheel",
                isSelected = uiState.isFocusWheelVisible,
                accentColor = CinemaCyan,
                onClick = onToggleFocusWheel
            )

            if (settings.manualFocusEnabled && supportsManualFocus) {
                Slider(
                    value = settings.focusDistanceDiopters,
                    onValueChange = { newDist ->
                        onUpdateManualSettings(settings.copy(focusDistanceDiopters = newDist))
                    },
                    valueRange = 0f..maxDiopters,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_focus_drawer_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CinemaCyan,
                        activeTrackColor = CinemaCyan,
                        inactiveTrackColor = CinemaSurfaceElevated
                    )
                )

                Text(
                    text = formatDioptersToDistance(settings.focusDistanceDiopters),
                    color = TextWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Distance Marks Bar when manual focus is active
        if (settings.manualFocusEnabled && supportsManualFocus) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    0.0f to "INF ∞",
                    0.5f to "2.0m",
                    1.0f to "1.0m",
                    2.0f to "0.5m",
                    10.0f to "MACRO"
                ).forEach { (diopt, label) ->
                    val isCurrent = kotlin.math.abs(settings.focusDistanceDiopters - diopt) < 0.25f
                    DrawerChip(
                        label = label,
                        subLabel = "${String.format(Locale.US, "%.1f", diopt)}D",
                        isSelected = isCurrent,
                        accentColor = CinemaCyan,
                        onClick = {
                            onUpdateManualSettings(settings.copy(focusDistanceDiopters = diopt.coerceIn(0f, maxDiopters)))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomDrawerContent(
    uiState: CinemaUiState,
    onUpdateManualSettings: (ManualControlSettings) -> Unit
) {
    val settings = uiState.manualSettings
    val maxZoom = uiState.capability?.maxDigitalZoom?.coerceAtMost(8f) ?: 4f

    Column {
        Text(
            text = "DIGITAL ZOOM / CROP: ${String.format(Locale.US, "%.1f", settings.zoomRatio)}x",
            color = TextWhite,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f).filter { it <= maxZoom }.forEach { z ->
                DrawerChip(
                    label = "${String.format(Locale.US, "%.1f", z)}x",
                    subLabel = if (z == 1.0f) "Wide" else "Tele",
                    isSelected = kotlin.math.abs(settings.zoomRatio - z) < 0.1f,
                    accentColor = TextWhite,
                    onClick = {
                        onUpdateManualSettings(settings.copy(zoomRatio = z))
                    }
                )
            }

            Slider(
                value = settings.zoomRatio,
                onValueChange = { newZoom ->
                    onUpdateManualSettings(settings.copy(zoomRatio = newZoom))
                },
                valueRange = 1.0f..maxZoom,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = TextWhite,
                    activeTrackColor = CinemaAmber,
                    inactiveTrackColor = CinemaSurfaceElevated
                )
            )
        }
    }
}

@Composable
private fun ToolsDrawerContent(
    tools: CinematographyToolsConfig,
    onUpdateTools: (CinematographyToolsConfig) -> Unit
) {
    Column {
        Text(
            text = "CINEMATOGRAPHY MONITORING TOOLS",
            color = CinemaAmber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DrawerChip(
                label = "GRID",
                subLabel = if (tools.gridEnabled) "Rule 1/3" else "Off",
                isSelected = tools.gridEnabled,
                accentColor = CinemaAmber,
                onClick = { onUpdateTools(tools.copy(gridEnabled = !tools.gridEnabled)) }
            )

            DrawerChip(
                label = "CROSSHAIR",
                subLabel = if (tools.centerCrosshairEnabled) "Center" else "Off",
                isSelected = tools.centerCrosshairEnabled,
                accentColor = CinemaCyan,
                onClick = { onUpdateTools(tools.copy(centerCrosshairEnabled = !tools.centerCrosshairEnabled)) }
            )

            DrawerChip(
                label = "SAFE AREA",
                subLabel = if (tools.safeAreaEnabled) "90%/80%" else "Off",
                isSelected = tools.safeAreaEnabled,
                accentColor = CinemaAmber,
                onClick = { onUpdateTools(tools.copy(safeAreaEnabled = !tools.safeAreaEnabled)) }
            )

            DrawerChip(
                label = "SCOPE MASK",
                subLabel = tools.aspectRatioGuide.label,
                isSelected = tools.aspectRatioGuide != AspectRatioGuide.NONE,
                accentColor = CinemaGreen,
                onClick = {
                    val next = when (tools.aspectRatioGuide) {
                        AspectRatioGuide.NONE -> AspectRatioGuide.RATIO_2_35_1
                        AspectRatioGuide.RATIO_2_35_1 -> AspectRatioGuide.RATIO_16_9
                        AspectRatioGuide.RATIO_16_9 -> AspectRatioGuide.RATIO_9_16
                        AspectRatioGuide.RATIO_9_16 -> AspectRatioGuide.NONE
                    }
                    onUpdateTools(tools.copy(aspectRatioGuide = next))
                }
            )

            DrawerChip(
                label = "HISTOGRAM",
                subLabel = if (tools.histogramEnabled) "Luma" else "Off",
                isSelected = tools.histogramEnabled,
                accentColor = CinemaCyan,
                onClick = { onUpdateTools(tools.copy(histogramEnabled = !tools.histogramEnabled)) }
            )

            DrawerChip(
                label = "VU METER",
                subLabel = if (tools.audioMeterEnabled) "On" else "Off",
                isSelected = tools.audioMeterEnabled,
                accentColor = CinemaGreen,
                onClick = { onUpdateTools(tools.copy(audioMeterEnabled = !tools.audioMeterEnabled)) }
            )

            DrawerChip(
                label = "PEAKING",
                subLabel = if (tools.focusPeakingEnabled) tools.focusPeakingColor.label else "Off",
                isSelected = tools.focusPeakingEnabled,
                accentColor = Color(tools.focusPeakingColor.hexColor),
                onClick = { onUpdateTools(tools.copy(focusPeakingEnabled = !tools.focusPeakingEnabled)) }
            )

            DrawerChip(
                label = "CLEAN FEED",
                subLabel = "Hide HUD",
                isSelected = tools.cleanMonitoringMode,
                accentColor = CinemaRed,
                onClick = { onUpdateTools(tools.copy(cleanMonitoringMode = true)) }
            )
        }
    }
}

@Composable
private fun DrawerChip(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    accentColor: Color,
    enabled: Boolean = true,
    rotationDegrees: Float = LocalUiRotation.current,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                if (isSelected) accentColor.copy(alpha = 0.2f) else CinemaSurface,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.rotate(rotationDegrees),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (!enabled) TextDim else if (isSelected) accentColor else TextWhite,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (!enabled) "N/A" else subLabel,
                color = if (!enabled) CinemaRed.copy(alpha = 0.6f) else TextDim,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
