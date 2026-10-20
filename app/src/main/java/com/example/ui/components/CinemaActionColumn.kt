package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camera.CinemaUiState
import com.example.camera.RecordingStatus
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaRedBright
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

import androidx.compose.ui.draw.rotate

@Composable
fun CinemaActionColumn(
    uiState: CinemaUiState,
    onRecordToggle: () -> Unit,
    onCapturePhoto: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onOpenTools: () -> Unit,
    onToggleFocusWheel: () -> Unit = {},
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (uiState.toolsConfig.cleanMonitoringMode) return

    val isRecording = uiState.recordingStatus == RecordingStatus.RECORDING

    val pulseTransition = rememberInfiniteTransition(label = "rec_button_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.85f))
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("cinema_action_column"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Actions: Settings, Tools & Focus Puller
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Cinema Settings",
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp).rotate(uiRotationDegrees)
                )
            }

            // Cinematography Tools Drawer Button
            IconButton(
                onClick = onOpenTools,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("tools_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Monitoring Tools",
                    tint = CinemaAmber,
                    modifier = Modifier.size(20.dp).rotate(uiRotationDegrees)
                )
            }

            // Dedicated Manual Focus Puller Rail Toggle
            IconButton(
                onClick = onToggleFocusWheel,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("focus_puller_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Focus Puller Rail",
                    tint = if (uiState.isFocusWheelVisible || uiState.manualSettings.manualFocusEnabled) CinemaCyan else TextMuted,
                    modifier = Modifier.size(20.dp).rotate(uiRotationDegrees)
                )
            }

            // Torch Toggle (if supported)
            if (uiState.capability?.supportsFlash == true) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("torch_button")
                ) {
                    Icon(
                        imageVector = if (uiState.manualSettings.torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Camera Torch",
                        tint = if (uiState.manualSettings.torchEnabled) CinemaAmber else TextMuted,
                        modifier = Modifier.size(20.dp).rotate(uiRotationDegrees)
                    )
                }
            }
        }

        // Center Primary Action: RECORD VIDEO BUTTON
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Cinema Red Recording Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(if (isRecording) pulseScale else 1f)
                    .border(
                        width = 3.dp,
                        color = if (isRecording) CinemaRedBright else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .padding(5.dp)
                    .clickable(onClick = onRecordToggle)
                    .testTag("record_video_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    // Recording Active: Rounded Red Square (Stop Trigger)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(uiRotationDegrees)
                            .background(CinemaRed, RoundedCornerShape(4.dp))
                    )
                } else {
                    // Standby: Solid Red Circle
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(CinemaRed, CircleShape)
                    )
                }
            }

            // Still Photo Shutter Button (secondary)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .padding(3.dp)
                    .clickable(onClick = onCapturePhoto)
                    .testTag("capture_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        // Bottom Actions: Switch Camera & Gallery
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Flip Front/Rear Camera
            IconButton(
                onClick = onFlipCamera,
                enabled = !isRecording,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera Lens",
                    tint = if (isRecording) TextMuted.copy(alpha = 0.4f) else TextWhite,
                    modifier = Modifier.size(20.dp).rotate(uiRotationDegrees)
                )
            }

            // Media Gallery Thumbnail Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenGallery)
                    .testTag("gallery_button"),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.lastCapturedThumbnailUri != null) {
                    AsyncImage(
                        model = uiState.lastCapturedThumbnailUri,
                        contentDescription = "Last Recorded Media",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).rotate(uiRotationDegrees)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Local Media Gallery",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp).rotate(uiRotationDegrees)
                    )
                }
            }
        }
    }
}
