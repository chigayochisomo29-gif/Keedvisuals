package com.example.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
class CinemaCameraManager(
    private val context: Context
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var activeRecording: Recording? = null

    var currentCapability: CameraHardwareCapability? = null
        private set

    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isBackCamera: Boolean,
        qualityOption: VideoQualityOption,
        manualSettings: ManualControlSettings,
        onCameraBound: (CameraHardwareCapability) -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                bindCameraUseCases(
                    provider = provider,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    isBackCamera = isBackCamera,
                    qualityOption = qualityOption,
                    manualSettings = manualSettings,
                    onCameraBound = onCameraBound,
                    onError = onError
                )
            } catch (e: Exception) {
                onError("Failed to initialize camera engine: ${e.localizedMessage ?: "Unknown error"}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        isBackCamera: Boolean,
        qualityOption: VideoQualityOption,
        manualSettings: ManualControlSettings,
        onCameraBound: (CameraHardwareCapability) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            provider.unbindAll()

            val cameraSelector = if (isBackCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Inspect actual hardware capabilities via Camera2 characteristics
            var capability = CameraHardwareCapability.inspect(context, preferBack = isBackCamera)

            // Build Preview
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Query genuine supported video encoder profiles via Recorder.getVideoCapabilities
            val cameraInfo = provider.getCameraInfo(cameraSelector)
            val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
            val camxQualities = videoCapabilities.getSupportedQualities(DynamicRange.SDR)

            val detectedQualities = mutableListOf<VideoQualityOption>()
            if (camxQualities.contains(Quality.FHD)) detectedQualities.add(VideoQualityOption.FHD_1080P)
            if (camxQualities.contains(Quality.HD)) detectedQualities.add(VideoQualityOption.HD_720P)
            if (camxQualities.contains(Quality.SD)) detectedQualities.add(VideoQualityOption.SD_480P)
            if (detectedQualities.isEmpty()) {
                detectedQualities.add(VideoQualityOption.HD_720P)
                detectedQualities.add(VideoQualityOption.SD_480P)
            }

            capability = capability.copy(supportedVideoQualities = detectedQualities)
            currentCapability = capability

            // Build QualitySelector with comprehensive fallback order
            val qualityList = mutableListOf<Quality>()
            if (camxQualities.contains(qualityOption.quality)) {
                qualityList.add(qualityOption.quality)
            }
            qualityList.addAll(camxQualities.filter { it != qualityOption.quality })
            if (qualityList.isEmpty()) {
                qualityList.addAll(listOf(Quality.SD, Quality.HD, Quality.LOWEST))
            }

            val qualitySelector = QualitySelector.fromOrderedList(
                qualityList,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )

            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Build ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            // Bind to lifecycle
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageCapture
            )

            // Apply active manual hardware settings
            applyManualSettings(manualSettings, capability)

            onCameraBound(capability)
        } catch (e: Exception) {
            onError("Camera hardware binding failed: ${e.localizedMessage ?: "Check hardware permissions"}")
        }
    }

    fun applyManualSettings(
        settings: ManualControlSettings,
        capability: CameraHardwareCapability = currentCapability ?: CameraHardwareCapability.inspect(context)
    ) {
        val cam = camera ?: return
        val cameraControl = cam.cameraControl

        try {
            val camera2Control = Camera2CameraControl.from(cameraControl)
            val requestBuilder = CaptureRequestOptions.Builder()

            // ISO & Shutter speed: only apply manual if sensor manual control is genuinely supported
            if (capability.supportsManualSensor && settings.manualIsoEnabled) {
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
                val clampedIso = if (capability.isoRange != null) {
                    settings.isoValue.coerceIn(capability.isoRange.lower, capability.isoRange.upper)
                } else {
                    settings.isoValue
                }
                requestBuilder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)

                if (settings.manualShutterEnabled) {
                    val clampedExposure = if (capability.exposureTimeRangeNanos != null) {
                        settings.shutterNanos.coerceIn(
                            capability.exposureTimeRangeNanos.lower,
                            capability.exposureTimeRangeNanos.upper
                        )
                    } else {
                        settings.shutterNanos
                    }
                    requestBuilder.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        clampedExposure
                    )
                }
            } else {
                // Return to continuous auto exposure
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            // Manual Focus
            if (capability.supportsManualFocus && settings.manualFocusEnabled) {
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                val clampedDistance = settings.focusDistanceDiopters.coerceIn(
                    0f,
                    capability.minFocusDistanceDiopters.coerceAtLeast(10f)
                )
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    clampedDistance
                )
            } else {
                // Return to continuous auto focus
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }

            // White Balance
            if (settings.whiteBalancePreset != WhiteBalancePreset.AUTO &&
                capability.supportedAwbModes.contains(settings.whiteBalancePreset.camera2Mode)
            ) {
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    settings.whiteBalancePreset.camera2Mode
                )
            } else {
                requestBuilder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO
                )
            }

            camera2Control.setCaptureRequestOptions(requestBuilder.build())

            // Exposure compensation (EV)
            if (capability.supportsEvCompensation && capability.exposureCompensationRange != null) {
                val clampedEv = settings.exposureCompensationIndex.coerceIn(
                    capability.exposureCompensationRange.lower,
                    capability.exposureCompensationRange.upper
                )
                cameraControl.setExposureCompensationIndex(clampedEv)
            }

            // Zoom
            val zoom = settings.zoomRatio.coerceIn(1.0f, capability.maxDigitalZoom.coerceAtMost(8.0f))
            cameraControl.setZoomRatio(zoom)

            // Torch
            if (capability.supportsFlash) {
                cameraControl.enableTorch(settings.torchEnabled)
            }
        } catch (_: Exception) {
            // Safe fallback if device driver rejects specific key
        }
    }

    fun setManualFocusDistance(diopters: Float) {
        val cam = camera ?: return
        val capability = currentCapability ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val requestBuilder = CaptureRequestOptions.Builder()
            requestBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            val maxDiopters = if (capability.minFocusDistanceDiopters > 0f) capability.minFocusDistanceDiopters else 10f
            val clampedDistance = diopters.coerceIn(0f, maxDiopters)
            requestBuilder.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                clampedDistance
            )
            camera2Control.setCaptureRequestOptions(requestBuilder.build())
        } catch (_: Exception) {}
    }

    fun restoreAutoFocus() {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val requestBuilder = CaptureRequestOptions.Builder()
            requestBuilder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            camera2Control.setCaptureRequestOptions(requestBuilder.build())
        } catch (_: Exception) {}
    }

    fun tapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val cam = camera ?: return
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        recordAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit,
        onError: (String) -> Unit
    ) {
        val vc = videoCapture ?: run {
            onError("Video capture pipeline is not ready")
            return
        }

        if (activeRecording != null) {
            onError("Recording is already in progress")
            return
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "KEED_$timestamp.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_MOVIES + "/KeedCinemaCamera"
                    )
                }
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }

            val mediaStoreOutput = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()

            var pendingRecording = vc.output.prepareRecording(context, mediaStoreOutput)

            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (recordAudio && hasMicPermission) {
                try {
                    pendingRecording = pendingRecording.withAudioEnabled()
                } catch (_: Throwable) {}
            }

            try {
                activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        activeRecording = null
                    }
                    onEvent(event)
                }
            } catch (startEx: Exception) {
                // If starting with audio failed, fallback to video-only recording
                if (recordAudio) {
                    val fallbackRecording = vc.output.prepareRecording(context, mediaStoreOutput)
                    activeRecording = fallbackRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            activeRecording = null
                        }
                        onEvent(event)
                    }
                } else {
                    throw startEx
                }
            }
        } catch (e: Exception) {
            activeRecording = null
            onError("Could not start recording: ${e.localizedMessage ?: "Storage or encoder failure"}")
        }
    }

    fun stopVideoRecording() {
        try {
            activeRecording?.stop()
            activeRecording = null
        } catch (_: Exception) {}
    }

    fun pauseVideoRecording() {
        try {
            activeRecording?.pause()
        } catch (_: Exception) {}
    }

    fun resumeVideoRecording() {
        try {
            activeRecording?.resume()
        } catch (_: Exception) {}
    }

    fun isRecordingActive(): Boolean = activeRecording != null

    fun takeStillPhoto(
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val ic = imageCapture ?: run {
            onError("Still photo capture is not ready")
            return
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "KEED_$timestamp.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/KeedCinemaCamera"
                    )
                }
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            ic.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val savedUri = outputFileResults.savedUri ?: Uri.EMPTY
                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(savedUri)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        ContextCompat.getMainExecutor(context).execute {
                            onError("Photo capture failed: ${exception.localizedMessage ?: "Unknown error"}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            onError("Still capture initialization failed: ${e.localizedMessage ?: "Unknown"}")
        }
    }

    fun release() {
        try {
            activeRecording?.stop()
            activeRecording = null
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (_: Exception) {}
    }
}
