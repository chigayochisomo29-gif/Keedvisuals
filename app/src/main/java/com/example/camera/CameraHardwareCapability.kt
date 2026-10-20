package com.example.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import android.util.Rational

/**
 * Encapsulates the hardware capabilities of a camera lens detected via Camera2 CameraCharacteristics.
 * Ensures the app never fakes manual controls that the device hardware cannot support.
 */
data class CameraHardwareCapability(
    val cameraId: String,
    val isBackFacing: Boolean,
    val hardwareLevel: String,
    val hardwareLevelInt: Int,
    val supportsManualSensor: Boolean,
    val supportsManualPostProcessing: Boolean,
    val isoRange: Range<Int>?,
    val exposureTimeRangeNanos: Range<Long>?,
    val exposureCompensationRange: Range<Int>?,
    val exposureCompensationStep: Rational?,
    val supportsManualFocus: Boolean,
    val minFocusDistanceDiopters: Float,
    val supportedAfModes: List<Int>,
    val supportedAwbModes: List<Int>,
    val supportsFlash: Boolean,
    val maxDigitalZoom: Float,
    val supportsVideoStabilization: Boolean,
    val supportedFpsRanges: List<Range<Int>>,
    val supportedVideoQualities: List<VideoQualityOption> = listOf(VideoQualityOption.FHD_1080P, VideoQualityOption.HD_720P, VideoQualityOption.SD_480P),
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
) {
    val supportsEvCompensation: Boolean
        get() = exposureCompensationRange != null &&
                exposureCompensationRange.lower != exposureCompensationRange.upper

    val evStepValue: Float
        get() = if (exposureCompensationStep != null && exposureCompensationStep.denominator != 0) {
            exposureCompensationStep.numerator.toFloat() / exposureCompensationStep.denominator.toFloat()
        } else {
            0.333f
        }

    companion object {
        fun inspect(context: Context, preferBack: Boolean = true): CameraHardwareCapability {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) return createFallback("0", preferBack)

            return try {
                val cameraIds = cameraManager.cameraIdList
                var targetId = if (preferBack) "0" else "1"

                for (id in cameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (preferBack && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        targetId = id
                        break
                    } else if (!preferBack && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        targetId = id
                        break
                    }
                }

                val characteristics = cameraManager.getCameraCharacteristics(targetId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val isBack = facing == CameraCharacteristics.LENS_FACING_BACK

                val hwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    ?: CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

                val hwLevelName = when (hwLevel) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN ($hwLevel)"
                }

                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?: intArrayOf()
                val supportsManualSensor = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
                val supportsManualPostProcessing = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)

                val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val expRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

                val compRange = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                val compStep = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)

                val minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
                val supportsManualFocus = minFocus > 0f

                val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toList() ?: emptyList()
                val awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toList() ?: emptyList()

                val flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 4.0f

                val stabModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.toList() ?: emptyList()
                val supportsStab = stabModes.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON)

                val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.toList() ?: emptyList()

                CameraHardwareCapability(
                    cameraId = targetId,
                    isBackFacing = isBack,
                    hardwareLevel = hwLevelName,
                    hardwareLevelInt = hwLevel,
                    supportsManualSensor = supportsManualSensor,
                    supportsManualPostProcessing = supportsManualPostProcessing,
                    isoRange = isoRange,
                    exposureTimeRangeNanos = expRange,
                    exposureCompensationRange = compRange,
                    exposureCompensationStep = compStep,
                    supportsManualFocus = supportsManualFocus,
                    minFocusDistanceDiopters = minFocus,
                    supportedAfModes = afModes,
                    supportedAwbModes = awbModes,
                    supportsFlash = flash,
                    maxDigitalZoom = zoom,
                    supportsVideoStabilization = supportsStab,
                    supportedFpsRanges = fpsRanges
                )
            } catch (e: Exception) {
                createFallback(if (preferBack) "0" else "1", preferBack)
            }
        }

        private fun createFallback(id: String, preferBack: Boolean): CameraHardwareCapability {
            return CameraHardwareCapability(
                cameraId = id,
                isBackFacing = preferBack,
                hardwareLevel = "LIMITED (Helio G37)",
                hardwareLevelInt = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                supportsManualSensor = false,
                supportsManualPostProcessing = false,
                isoRange = Range(100, 1600),
                exposureTimeRangeNanos = Range(1_000_000L, 500_000_000L),
                exposureCompensationRange = Range(-6, 6),
                exposureCompensationStep = Rational(1, 3),
                supportsManualFocus = false,
                minFocusDistanceDiopters = 0f,
                supportedAfModes = listOf(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO, CameraMetadata.CONTROL_AF_MODE_AUTO),
                supportedAwbModes = listOf(CameraMetadata.CONTROL_AWB_MODE_AUTO, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT, CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
                supportsFlash = true,
                maxDigitalZoom = 4.0f,
                supportsVideoStabilization = false,
                supportedFpsRanges = listOf(Range(15, 30), Range(30, 30))
            )
        }
    }
}
