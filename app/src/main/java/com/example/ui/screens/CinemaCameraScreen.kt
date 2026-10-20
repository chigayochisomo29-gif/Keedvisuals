package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CinemaCameraManager
import com.example.camera.CinemaUiState
import com.example.camera.RecordingStatus
import com.example.camera.VideoQualityOption
import com.example.ui.components.ActiveDrawer
import com.example.ui.components.CinemaActionColumn
import com.example.ui.components.CinemaFocusPuller
import com.example.ui.components.CinemaTopHUD
import com.example.ui.components.CinematographyOverlays
import com.example.ui.components.FocusPeakingOverlay
import com.example.ui.components.FocusReticle
import com.example.ui.components.ManualControlDrawers
import com.example.ui.components.ManualControlsBar
import com.example.ui.theme.CinemaAmber
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.TextWhite
import com.example.util.AudioLevelMonitor
import com.example.util.LocalUiRotation
import com.example.util.StorageUtils
import com.example.util.TimecodeFormatter
import com.example.util.rememberDeviceUiRotation
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.draw.rotate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CinemaCameraScreen(
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val activity = context as? Activity
    val cameraManager = remember { CinemaCameraManager(context) }
    val audioMonitor = remember { AudioLevelMonitor() }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var uiState by remember { mutableStateOf(CinemaUiState()) }
    var activeDrawer by remember { mutableStateOf(ActiveDrawer.NONE) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Tap to focus reticle position
    var tapFocusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var isShutterFlashVisible by remember { mutableStateOf(false) }

    val liveAudioDb by audioMonitor.audioLevelDb.collectAsState()
    val livePeakDb by audioMonitor.peakLevelDb.collectAsState()
    val uiRotationDegrees by rememberDeviceUiRotation()

    // Keep screen on during camera monitoring and recording
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audioMonitor.startMonitoring(context, coroutineScope)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            audioMonitor.stopMonitoring()
            cameraManager.release()
        }
    }

    // Refresh free storage stats periodically
    LaunchedEffect(Unit) {
        while (true) {
            val freeGb = StorageUtils.getFreeSpaceGb()
            val minutes = StorageUtils.estimateRemainingTimeMinutes(
                freeGb,
                uiState.quality.height >= 1080
            )
            uiState = uiState.copy(
                freeStorageGb = freeGb,
                estimatedMinutesRemaining = minutes
            )
            delay(5000)
        }
    }

    // Bind Camera when previewView is created or camera lens flipped
    fun rebindCamera(previewView: PreviewView) {
        cameraManager.initializeCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            isBackCamera = uiState.isBackCamera,
            qualityOption = uiState.quality,
            manualSettings = uiState.manualSettings,
            onCameraBound = { capability ->
                val validatedQuality = if (capability.supportedVideoQualities.contains(uiState.quality)) {
                    uiState.quality
                } else {
                    capability.supportedVideoQualities.firstOrNull() ?: VideoQualityOption.HD_720P
                }
                uiState = uiState.copy(
                    capability = capability,
                    quality = validatedQuality,
                    errorMessage = null
                )
            },
            onError = { err ->
                uiState = uiState.copy(errorMessage = err)
            }
        )
    }

    CompositionLocalProvider(LocalUiRotation provides uiRotationDegrees) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("cinema_camera_screen")
        ) {
        // 1. Physical Camera Live Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                    rebindCamera(this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Tap to Focus and Meter
                            tapFocusPoint = Pair(offset.x, offset.y)
                            previewViewRef?.let { pv ->
                                cameraManager.tapToFocus(pv, offset.x, offset.y)
                            }
                        },
                        onDoubleTap = {
                            // Double tap toggles clean monitoring feed
                            uiState = uiState.copy(
                                toolsConfig = uiState.toolsConfig.copy(
                                    cleanMonitoringMode = !uiState.toolsConfig.cleanMonitoringMode
                                )
                            )
                        }
                    )
                }
        )

        // 2. Cinematography Overlays (Aspect Ratio Mattes, Grid, Safe Areas, VU Meter, Histogram)
        CinematographyOverlays(
            toolsConfig = uiState.toolsConfig,
            audioLevelDb = liveAudioDb,
            audioPeakDb = livePeakDb,
            uiRotationDegrees = uiRotationDegrees,
            modifier = Modifier.fillMaxSize()
        )

        // 2b. Visual Focus Peaking Overlay & Real-time Indicator HUD
        FocusPeakingOverlay(
            toolsConfig = uiState.toolsConfig,
            settings = uiState.manualSettings,
            uiRotationDegrees = uiRotationDegrees,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Tap-to-Focus Reticle
        tapFocusPoint?.let { (x, y) ->
            FocusReticle(
                x = x,
                y = y,
                onAnimationEnd = { tapFocusPoint = null }
            )
        }

        // 4. White Flash Effect on Photo Capture
        AnimatedVisibility(
            visible = isShutterFlashVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)))
        }

        // 5. Clean Feed Exit Overlay Indicator
        if (uiState.toolsConfig.cleanMonitoringMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .rotate(uiRotationDegrees)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .clickable {
                        uiState = uiState.copy(
                            toolsConfig = uiState.toolsConfig.copy(cleanMonitoringMode = false)
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("exit_clean_mode_button")
            ) {
                Text(
                    text = "CLEAN FEED (TAP TO EXIT)",
                    color = CinemaAmber,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Professional Cinema Camera Chrome UI
        if (!uiState.toolsConfig.cleanMonitoringMode) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top HUD Bar
                CinemaTopHUD(
                    uiState = uiState,
                    uiRotationDegrees = uiRotationDegrees,
                    modifier = Modifier.fillMaxWidth()
                )

                // Middle Workspace: Viewfinder + Right Action Column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Central Camera Viewport area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // Real-time Cinema Manual Focus Puller Rail
                        FocusPullerContainer(
                            visible = uiState.isFocusWheelVisible,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                        ) {
                            CinemaFocusPuller(
                                uiState = uiState,
                                onUpdateFocusDistance = { diopters ->
                                    val updated = uiState.manualSettings.copy(
                                        manualFocusEnabled = true,
                                        focusDistanceDiopters = diopters
                                    )
                                    uiState = uiState.copy(manualSettings = updated)
                                    cameraManager.setManualFocusDistance(diopters)
                                },
                                onToggleManualFocus = { enabled ->
                                    val updated = uiState.manualSettings.copy(manualFocusEnabled = enabled)
                                    uiState = uiState.copy(manualSettings = updated)
                                    if (enabled) {
                                        cameraManager.setManualFocusDistance(updated.focusDistanceDiopters)
                                    } else {
                                        cameraManager.restoreAutoFocus()
                                    }
                                },
                                onTogglePeaking = { enabled ->
                                    uiState = uiState.copy(
                                        toolsConfig = uiState.toolsConfig.copy(focusPeakingEnabled = enabled)
                                    )
                                },
                                onSelectPeakingColor = { clr ->
                                    uiState = uiState.copy(
                                        toolsConfig = uiState.toolsConfig.copy(focusPeakingColor = clr)
                                    )
                                },
                                onClose = {
                                    uiState = uiState.copy(isFocusWheelVisible = false)
                                },
                                uiRotationDegrees = uiRotationDegrees
                            )
                        }
                    }

                    // Right Action Column (Record, Photo, Lens, Torch, Gallery, Settings)
                    CinemaActionColumn(
                        uiState = uiState,
                        uiRotationDegrees = uiRotationDegrees,
                        onRecordToggle = {
                            if (uiState.recordingStatus == RecordingStatus.RECORDING) {
                                uiState = uiState.copy(recordingStatus = RecordingStatus.FINALIZING)
                                cameraManager.stopVideoRecording()
                            } else {
                                cameraManager.startVideoRecording(
                                    recordAudio = uiState.recordAudio,
                                    onEvent = { event ->
                                        when (event) {
                                            is VideoRecordEvent.Start -> {
                                                uiState = uiState.copy(
                                                    recordingStatus = RecordingStatus.RECORDING,
                                                    recordedDurationMs = 0L,
                                                    infoMessage = "Recording started: H.264 MP4"
                                                )
                                            }
                                            is VideoRecordEvent.Status -> {
                                                val durMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                                                val bytes = event.recordingStats.numBytesRecorded
                                                uiState = uiState.copy(
                                                    recordedDurationMs = durMs,
                                                    recordedBytes = bytes,
                                                    timecodeString = TimecodeFormatter.formatTimecode(durMs, uiState.targetFps.fps),
                                                    timerString = TimecodeFormatter.formatSimpleTimer(durMs)
                                                )
                                            }
                                            is VideoRecordEvent.Finalize -> {
                                                if (event.hasError()) {
                                                    uiState = uiState.copy(
                                                        recordingStatus = RecordingStatus.IDLE,
                                                        errorMessage = "Recording ended with error code: ${event.error}"
                                                    )
                                                } else {
                                                    val uri = event.outputResults.outputUri
                                                    uiState = uiState.copy(
                                                        recordingStatus = RecordingStatus.IDLE,
                                                        lastCapturedThumbnailUri = uri,
                                                        infoMessage = "Saved to Movies/KeedCinemaCamera"
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        uiState = uiState.copy(
                                            recordingStatus = RecordingStatus.IDLE,
                                            errorMessage = err
                                        )
                                    }
                                )
                            }
                        },
                        onCapturePhoto = {
                            isShutterFlashVisible = true
                            coroutineScope.launch {
                                delay(100)
                                isShutterFlashVisible = false
                            }
                            cameraManager.takeStillPhoto(
                                onSuccess = { uri ->
                                    uiState = uiState.copy(
                                        lastCapturedThumbnailUri = uri,
                                        infoMessage = "Still photo saved to Pictures/KeedCinemaCamera"
                                    )
                                },
                                onError = { err ->
                                    uiState = uiState.copy(errorMessage = err)
                                }
                            )
                        },
                        onFlipCamera = {
                            val newFacing = !uiState.isBackCamera
                            uiState = uiState.copy(isBackCamera = newFacing)
                            previewViewRef?.let { pv -> rebindCamera(pv) }
                        },
                        onToggleTorch = {
                            val newTorch = !uiState.manualSettings.torchEnabled
                            val updated = uiState.manualSettings.copy(torchEnabled = newTorch)
                            uiState = uiState.copy(manualSettings = updated)
                            cameraManager.applyManualSettings(updated)
                        },
                        onOpenTools = {
                            activeDrawer = if (activeDrawer == ActiveDrawer.TOOLS) ActiveDrawer.NONE else ActiveDrawer.TOOLS
                        },
                        onToggleFocusWheel = {
                            val willShow = !uiState.isFocusWheelVisible
                            uiState = uiState.copy(
                                isFocusWheelVisible = willShow,
                                manualSettings = if (willShow && !uiState.manualSettings.manualFocusEnabled) {
                                    uiState.manualSettings.copy(manualFocusEnabled = true)
                                } else {
                                    uiState.manualSettings
                                },
                                toolsConfig = if (willShow) {
                                    uiState.toolsConfig.copy(focusPeakingEnabled = true)
                                } else {
                                    uiState.toolsConfig
                                }
                            )
                            if (willShow) {
                                cameraManager.setManualFocusDistance(uiState.manualSettings.focusDistanceDiopters)
                            }
                        },
                        onOpenGallery = onNavigateToGallery,
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }

                // Bottom Adjustment Drawers
                ManualControlDrawers(
                    activeDrawer = activeDrawer,
                    uiState = uiState,
                    uiRotationDegrees = uiRotationDegrees,
                    onUpdateManualSettings = { newSettings ->
                        val focusDistChanged = newSettings.focusDistanceDiopters != uiState.manualSettings.focusDistanceDiopters
                        val focusModeChanged = newSettings.manualFocusEnabled != uiState.manualSettings.manualFocusEnabled
                        uiState = uiState.copy(manualSettings = newSettings)
                        if (newSettings.manualFocusEnabled && (focusDistChanged || focusModeChanged)) {
                            cameraManager.setManualFocusDistance(newSettings.focusDistanceDiopters)
                        } else if (!newSettings.manualFocusEnabled && focusModeChanged) {
                            cameraManager.restoreAutoFocus()
                        } else {
                            cameraManager.applyManualSettings(newSettings)
                        }
                    },
                    onUpdateFps = { newFps ->
                        uiState = uiState.copy(targetFps = newFps)
                    },
                    onUpdateTools = { newTools ->
                        uiState = uiState.copy(toolsConfig = newTools)
                    },
                    onToggleFocusWheel = {
                        uiState = uiState.copy(isFocusWheelVisible = !uiState.isFocusWheelVisible)
                    },
                    onCloseDrawer = { activeDrawer = ActiveDrawer.NONE }
                )

                // Bottom Manual Controls Bar
                ManualControlsBar(
                    uiState = uiState,
                    activeDrawer = activeDrawer,
                    uiRotationDegrees = uiRotationDegrees,
                    onSelectDrawer = { activeDrawer = it }
                )
            }
        }

        // Notification / Info / Error Banner
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                containerColor = CinemaRed,
                contentColor = Color.White,
                action = {
                    Text(
                        text = "DISMISS",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { uiState = uiState.copy(errorMessage = null) }
                    )
                }
            ) {
                Text(text = error, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        uiState.infoMessage?.let { info ->
            LaunchedEffect(info) {
                delay(3000)
                uiState = uiState.copy(infoMessage = null)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 45.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = info,
                    color = CinemaAmber,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Full Cinema Video Settings Dialog
        if (showSettingsDialog) {
            CinemaSettingsDialog(
                uiState = uiState,
                onSelectQuality = { q ->
                    uiState = uiState.copy(quality = q)
                    previewViewRef?.let { pv -> rebindCamera(pv) }
                },
                onSelectFps = { fps ->
                    uiState = uiState.copy(targetFps = fps)
                },
                onToggleAudio = { recAudio ->
                    uiState = uiState.copy(recordAudio = recAudio)
                },
                onToggleStabilization = { stab ->
                    uiState = uiState.copy(videoStabilization = stab)
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
        }
    }
}

@Composable
private fun FocusPullerContainer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}
