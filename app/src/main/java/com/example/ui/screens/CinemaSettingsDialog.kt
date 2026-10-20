package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.camera.CinemaUiState
import com.example.camera.FrameRateOption
import com.example.camera.VideoQualityOption
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

@Composable
fun CinemaSettingsDialog(
    uiState: CinemaUiState,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onSelectFps: (FrameRateOption) -> Unit,
    onToggleAudio: (Boolean) -> Unit,
    onToggleStabilization: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .background(CinemaBlack, RoundedCornerShape(8.dp))
                .border(1.dp, CinemaBorder, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .testTag("cinema_settings_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KEED CINEMA CAMERA",
                            color = CinemaRed,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Video Pipeline & Hardware Capability Inspector",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = TextWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = CinemaBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Settings Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Video Resolution
                    SettingsSection(title = "RECORDING RESOLUTION (H.264 MP4)") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(VideoQualityOption.FHD_1080P, VideoQualityOption.HD_720P, VideoQualityOption.SD_480P).forEach { opt ->
                                val isSelected = opt == uiState.quality
                                val isSupported = uiState.capability?.supportedVideoQualities?.contains(opt) ?: true
                                SelectableChip(
                                    title = opt.label,
                                    subtitle = if (isSupported) "${opt.width}x${opt.height}" else "UNSUPPORTED",
                                    isSelected = isSelected,
                                    accentColor = CinemaAmber,
                                    enabled = isSupported,
                                    modifier = Modifier.weight(1f),
                                    onClick = { if (isSupported) onSelectQuality(opt) }
                                )
                            }
                        }
                    }

                    // 2. Project Frame Rate
                    SettingsSection(title = "CAPTURE FRAME RATE") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FrameRateOption.values().forEach { fps ->
                                val isSelected = fps == uiState.targetFps
                                SelectableChip(
                                    title = "${fps.fps} FPS",
                                    subtitle = fps.label.split(" ")[1].replace("(", "").replace(")", ""),
                                    isSelected = isSelected,
                                    accentColor = CinemaCyan,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectFps(fps) }
                                )
                            }
                        }
                        Text(
                            text = "Note: TECNO Spark 9 Helio G37 hardware natively streams 30 fps. Standard 24/25 fps conform to cinema cadence.",
                            color = TextDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // 3. Audio & Stabilization Toggles
                    SettingsSection(title = "AUDIO & STABILIZATION") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CinemaSurface, RoundedCornerShape(4.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Record Audio (AAC 44.1 kHz)",
                                    color = TextWhite,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.recordAudio) "Microphone capture active" else "Video recorded muted (Silent)",
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = uiState.recordAudio,
                                onCheckedChange = onToggleAudio,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CinemaGreen,
                                    checkedTrackColor = CinemaGreen.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = CinemaSurfaceElevated
                                ),
                                modifier = Modifier.testTag("toggle_audio_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val supportsStab = uiState.capability?.supportsVideoStabilization == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CinemaSurface, RoundedCornerShape(4.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Video EIS Stabilization",
                                    color = if (supportsStab) TextWhite else TextDim,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (supportsStab) "Hardware gyro EIS enabled" else "Unsupported by camera sensor driver",
                                    color = if (supportsStab) TextMuted else CinemaRed.copy(alpha = 0.7f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = uiState.videoStabilization,
                                onCheckedChange = onToggleStabilization,
                                enabled = supportsStab,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CinemaCyan,
                                    checkedTrackColor = CinemaCyan.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // 4. Device Hardware Capabilities Inspector
                    SettingsSection(title = "DEVICE & CAMERA2 HARDWARE INSPECTION") {
                        val cap = uiState.capability
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CinemaSurface, RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HardwareRow(label = "Primary Target Device", value = "TECNO Spark 9 (MediaTek Helio G37)")
                            HardwareRow(label = "Detected Device", value = cap?.deviceModel ?: "${Build.MANUFACTURER} ${Build.MODEL}")
                            HardwareRow(label = "OS Version", value = cap?.androidVersion ?: "Android ${Build.VERSION.RELEASE}")
                            HardwareRow(
                                label = "Camera2 Hardware Level",
                                value = cap?.hardwareLevel ?: "LIMITED",
                                isHighlight = true,
                                highlightColor = if (cap?.hardwareLevel == "FULL" || cap?.hardwareLevel == "LEVEL_3") CinemaGreen else CinemaAmber
                            )
                            HardwareRow(
                                label = "Manual Sensor Exposure",
                                value = if (cap?.supportsManualSensor == true) "SUPPORTED" else "LIMITED (Helio G37 AE active)",
                                isHighlight = true,
                                highlightColor = if (cap?.supportsManualSensor == true) CinemaGreen else CinemaRed
                            )
                            HardwareRow(
                                label = "Manual Focus Distance",
                                value = if (cap?.supportsManualFocus == true) "SUPPORTED (0.0 to ${cap.minFocusDistanceDiopters} diopters)" else "FIXED FOCUS / AF-C",
                                isHighlight = false
                            )
                            HardwareRow(
                                label = "Exposure Compensation",
                                value = if (cap?.supportsEvCompensation == true) "SUPPORTED (${cap.exposureCompensationRange?.lower} to ${cap.exposureCompensationRange?.upper} steps)" else "UNSUPPORTED",
                                isHighlight = true,
                                highlightColor = CinemaGreen
                            )
                            HardwareRow(
                                label = "Storage Movies Directory",
                                value = "Movies/KeedCinemaCamera",
                                isHighlight = false
                            )
                            HardwareRow(
                                label = "Available Space",
                                value = String.format(Locale.US, "%.2f GB (~%d min remaining)", uiState.freeStorageGb, uiState.estimatedMinutesRemaining),
                                isHighlight = true,
                                highlightColor = if (uiState.freeStorageGb > 1.0) CinemaGreen else CinemaRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = CinemaAmber,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SelectableChip(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                if (!enabled) CinemaSurface.copy(alpha = 0.5f) else if (isSelected) accentColor.copy(alpha = 0.2f) else CinemaSurface,
                RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.5.dp,
                color = if (!enabled) Color.Transparent else if (isSelected) accentColor else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (!enabled) TextDim else if (isSelected) accentColor else TextWhite,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = if (!enabled) CinemaRed.copy(alpha = 0.6f) else if (isSelected) TextWhite else TextDim,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HardwareRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = CinemaGreen
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = if (isHighlight) highlightColor else TextWhite,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}
