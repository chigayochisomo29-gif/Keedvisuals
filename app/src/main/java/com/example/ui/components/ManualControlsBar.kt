package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.camera.CinemaUiState
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.util.TimecodeFormatter
import java.util.Locale

import androidx.compose.ui.draw.rotate

enum class ActiveDrawer {
    NONE,
    FPS,
    SHUTTER,
    ISO,
    WHITE_BALANCE,
    EV,
    FOCUS,
    ZOOM,
    TOOLS
}

@Composable
fun ManualControlsBar(
    uiState: CinemaUiState,
    activeDrawer: ActiveDrawer,
    onSelectDrawer: (ActiveDrawer) -> Unit,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (uiState.toolsConfig.cleanMonitoringMode) return

    val capability = uiState.capability
    val manualSettings = uiState.manualSettings

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.90f))
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("manual_controls_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. FPS Button
        CinemaControlButton(
            label = "FPS",
            value = "${uiState.targetFps.fps}",
            subValue = uiState.targetFps.label.split(" ")[1],
            isActive = activeDrawer == ActiveDrawer.FPS,
            accentColor = CinemaAmber,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.FPS) ActiveDrawer.NONE else ActiveDrawer.FPS)
            },
            tag = "control_fps"
        )

        // 2. SHUTTER Speed / Angle Button
        val shutterLabel = if (manualSettings.manualShutterEnabled && (capability?.supportsManualSensor == true)) {
            TimecodeFormatter.formatShutterSpeed(manualSettings.shutterNanos)
        } else {
            "AUTO"
        }
        val shutterAngle = if (manualSettings.manualShutterEnabled && (capability?.supportsManualSensor == true)) {
            TimecodeFormatter.calculateShutterAngle(manualSettings.shutterNanos, uiState.targetFps.fps)
        } else {
            "180°"
        }
        CinemaControlButton(
            label = "SHUTTER",
            value = shutterLabel,
            subValue = shutterAngle,
            isActive = activeDrawer == ActiveDrawer.SHUTTER,
            accentColor = CinemaCyan,
            isDisabled = capability?.supportsManualSensor == false,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.SHUTTER) ActiveDrawer.NONE else ActiveDrawer.SHUTTER)
            },
            tag = "control_shutter"
        )

        // 3. ISO Sensitivity Button
        val isoLabel = if (manualSettings.manualIsoEnabled && (capability?.supportsManualSensor == true)) {
            "${manualSettings.isoValue}"
        } else {
            "AUTO"
        }
        CinemaControlButton(
            label = "ISO",
            value = isoLabel,
            subValue = if (manualSettings.manualIsoEnabled) "MANUAL" else "AUTO",
            isActive = activeDrawer == ActiveDrawer.ISO,
            accentColor = CinemaAmber,
            isDisabled = capability?.supportsManualSensor == false,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.ISO) ActiveDrawer.NONE else ActiveDrawer.ISO)
            },
            tag = "control_iso"
        )

        // 4. WHITE BALANCE Button
        CinemaControlButton(
            label = "WB",
            value = manualSettings.whiteBalancePreset.kelvin.toString() + "K",
            subValue = manualSettings.whiteBalancePreset.label,
            isActive = activeDrawer == ActiveDrawer.WHITE_BALANCE,
            accentColor = CinemaAmber,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.WHITE_BALANCE) ActiveDrawer.NONE else ActiveDrawer.WHITE_BALANCE)
            },
            tag = "control_wb"
        )

        // 5. EXPOSURE COMPENSATION (EV) Button
        val evValue = manualSettings.exposureCompensationIndex * (capability?.evStepValue ?: 0.333f)
        val evString = if (evValue >= 0f) "+${String.format(Locale.US, "%.1f", evValue)}" else String.format(Locale.US, "%.1f", evValue)
        CinemaControlButton(
            label = "EV",
            value = "$evString EV",
            subValue = if (manualSettings.exposureCompensationIndex == 0) "0.0" else "SHIFT",
            isActive = activeDrawer == ActiveDrawer.EV,
            accentColor = CinemaCyan,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.EV) ActiveDrawer.NONE else ActiveDrawer.EV)
            },
            tag = "control_ev"
        )

        // 6. FOCUS (AF/MF) Button
        val focusLabel = if (manualSettings.manualFocusEnabled && (capability?.supportsManualFocus == true)) {
            "MF"
        } else {
            "AF-C"
        }
        CinemaControlButton(
            label = "FOCUS",
            value = focusLabel,
            subValue = if (manualSettings.manualFocusEnabled) "MANUAL" else "AUTO",
            isActive = activeDrawer == ActiveDrawer.FOCUS,
            accentColor = CinemaCyan,
            isDisabled = capability?.supportsManualFocus == false,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.FOCUS) ActiveDrawer.NONE else ActiveDrawer.FOCUS)
            },
            tag = "control_focus"
        )

        // 7. ZOOM Button
        CinemaControlButton(
            label = "LENS",
            value = "${String.format(Locale.US, "%.1f", manualSettings.zoomRatio)}x",
            subValue = if (uiState.isBackCamera) "REAR" else "FRONT",
            isActive = activeDrawer == ActiveDrawer.ZOOM,
            accentColor = TextWhite,
            rotationDegrees = uiRotationDegrees,
            onClick = {
                onSelectDrawer(if (activeDrawer == ActiveDrawer.ZOOM) ActiveDrawer.NONE else ActiveDrawer.ZOOM)
            },
            tag = "control_zoom"
        )
    }
}

@Composable
private fun CinemaControlButton(
    label: String,
    value: String,
    subValue: String,
    isActive: Boolean,
    accentColor: Color,
    isDisabled: Boolean = false,
    rotationDegrees: Float = 0f,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        modifier = Modifier
            .background(
                if (isActive) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isActive) 1.dp else 0.5.dp,
                color = if (isActive) accentColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.rotate(rotationDegrees),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (isDisabled) TextDim else if (isActive) accentColor else TextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                color = if (isDisabled) TextDim else TextWhite,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
            Text(
                text = if (isDisabled) "LIMITED" else subValue,
                color = if (isDisabled) CinemaRed.copy(alpha = 0.7f) else TextDim,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
