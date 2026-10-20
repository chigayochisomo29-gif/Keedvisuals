package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CinemaUiState
import com.example.camera.RecordingStatus
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGreen
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.CinemaSurfaceElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale

import androidx.compose.ui.draw.rotate

@Composable
fun CinemaTopHUD(
    uiState: CinemaUiState,
    uiRotationDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (uiState.toolsConfig.cleanMonitoringMode) return

    val isRecording = uiState.recordingStatus == RecordingStatus.RECORDING
    val infiniteTransition = rememberInfiniteTransition(label = "tally_pulse")
    val tallyAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tally_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.85f))
            .border(
                width = 0.8.dp,
                color = if (isRecording) CinemaRed.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("cinema_top_hud"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Branding & Device Info
        Column(modifier = Modifier.rotate(uiRotationDegrees)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "KEED",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "CINEMA",
                    color = CinemaRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${uiState.capability?.deviceModel ?: "TECNO Spark 9"} • ${uiState.capability?.hardwareLevel ?: "LIMITED"}",
                color = TextMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Center: Tally & SMPTE Timecode Counter
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .rotate(uiRotationDegrees)
                .background(
                    if (isRecording) CinemaRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(4.dp)
                )
                .border(
                    width = 0.8.dp,
                    color = if (isRecording) CinemaRed else Color.Transparent,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            // Tally Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isRecording) CinemaRed else CinemaGreen,
                        CircleShape
                    )
                    .alpha(if (isRecording) tallyAlpha else 1f)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (isRecording) "REC" else "STBY",
                color = if (isRecording) CinemaRed else CinemaGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = uiState.timecodeString,
                color = TextWhite,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        // Right: Format & Storage Monitor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.rotate(uiRotationDegrees)
        ) {
            // Video Format Badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${uiState.quality.label.split(" ")[0]} ${uiState.targetFps.fps}p",
                    color = CinemaAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (uiState.recordAudio) "H.264 / AAC" else "H.264 / MUTE",
                    color = TextDim,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Storage Free Space Card
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .background(CinemaSurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f GB", uiState.freeStorageGb),
                    color = if (uiState.freeStorageGb < 1.0) CinemaRed else TextWhite,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "~${uiState.estimatedMinutesRemaining} MIN",
                    color = TextMuted,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
