package com.example.camera

import android.hardware.camera2.CameraMetadata
import android.net.Uri
import androidx.camera.video.Quality

enum class RecordingStatus {
    IDLE,
    RECORDING,
    PAUSED,
    FINALIZING
}

enum class VideoQualityOption(val label: String, val quality: Quality, val width: Int, val height: Int) {
    FHD_1080P("1080p FHD", Quality.FHD, 1920, 1080),
    HD_720P("720p HD", Quality.HD, 1280, 720),
    SD_480P("480p SD", Quality.SD, 720, 480)
}

enum class FrameRateOption(val fps: Int, val label: String) {
    FPS_24(24, "24 fps (Cinema)"),
    FPS_25(25, "25 fps (PAL)"),
    FPS_30(30, "30 fps (Standard)"),
    FPS_60(60, "60 fps (High Speed)")
}

enum class WhiteBalancePreset(val label: String, val kelvin: Int, val camera2Mode: Int) {
    AUTO("AWB", 5000, CameraMetadata.CONTROL_AWB_MODE_AUTO),
    TUNGSTEN("3200K", 3200, CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("4000K", 4000, CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT),
    DAYLIGHT("5600K", 5600, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("6500K", 6500, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    SHADE("7500K", 7500, CameraMetadata.CONTROL_AWB_MODE_SHADE)
}

enum class AspectRatioGuide(val label: String, val ratio: Float) {
    NONE("Off", 0f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_2_35_1("2.35:1 Scope", 2.35f),
    RATIO_9_16("9:16 Vertical", 9f / 16f)
}

enum class FocusPeakingColor(val label: String, val hexColor: Long) {
    GREEN("Green", 0xFF00E676),
    RED("Red", 0xFFFF1744),
    CYAN("Cyan", 0xFF00E5FF)
}

data class CinematographyToolsConfig(
    val gridEnabled: Boolean = true,
    val centerCrosshairEnabled: Boolean = true,
    val safeAreaEnabled: Boolean = false,
    val histogramEnabled: Boolean = true,
    val audioMeterEnabled: Boolean = true,
    val zebraEnabled: Boolean = false,
    val focusPeakingEnabled: Boolean = false,
    val focusPeakingColor: FocusPeakingColor = FocusPeakingColor.GREEN,
    val aspectRatioGuide: AspectRatioGuide = AspectRatioGuide.RATIO_2_35_1,
    val cleanMonitoringMode: Boolean = false
)

data class ManualControlSettings(
    val manualIsoEnabled: Boolean = false,
    val isoValue: Int = 400,
    val manualShutterEnabled: Boolean = false,
    val shutterNanos: Long = 1_000_000_000L / 50L, // 1/50 sec default cinema shutter
    val exposureCompensationIndex: Int = 0,
    val manualFocusEnabled: Boolean = false,
    val focusDistanceDiopters: Float = 2.0f, // 0.5 meter
    val whiteBalancePreset: WhiteBalancePreset = WhiteBalancePreset.AUTO,
    val torchEnabled: Boolean = false,
    val zoomRatio: Float = 1.0f
)

data class CinemaUiState(
    val recordingStatus: RecordingStatus = RecordingStatus.IDLE,
    val recordedDurationMs: Long = 0L,
    val recordedBytes: Long = 0L,
    val timecodeString: String = "00:00:00:00",
    val timerString: String = "00:00",
    val isBackCamera: Boolean = true,
    val quality: VideoQualityOption = VideoQualityOption.FHD_1080P,
    val targetFps: FrameRateOption = FrameRateOption.FPS_30,
    val recordAudio: Boolean = true,
    val videoStabilization: Boolean = false,
    val freeStorageGb: Double = 0.0,
    val estimatedMinutesRemaining: Int = 0,
    val audioLevelDb: Float = -60f,
    val audioPeakDb: Float = -60f,
    val capability: CameraHardwareCapability? = null,
    val manualSettings: ManualControlSettings = ManualControlSettings(),
    val toolsConfig: CinematographyToolsConfig = CinematographyToolsConfig(),
    val lastCapturedThumbnailUri: Uri? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isTakingPhoto: Boolean = false,
    val isFocusWheelVisible: Boolean = false
)
