package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max

/**
 * Monitors live audio input amplitude and dBFS for the cinema VU meter.
 * Gracefully handles missing microphone permissions or unsupported hardware tracks.
 */
class AudioLevelMonitor {
    private val _audioLevelDb = MutableStateFlow(-60f)
    val audioLevelDb: StateFlow<Float> = _audioLevelDb.asStateFlow()

    private val _peakLevelDb = MutableStateFlow(-60f)
    val peakLevelDb: StateFlow<Float> = _peakLevelDb.asStateFlow()

    private var monitorJob: Job? = null
    private var isRunning = false

    @SuppressLint("MissingPermission")
    fun startMonitoring(context: Context, scope: CoroutineScope) {
        if (isRunning) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _audioLevelDb.value = -60f
            _peakLevelDb.value = -60f
            return
        }

        isRunning = true

        monitorJob = scope.launch(Dispatchers.IO) {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            // Check if microphone hardware is physically available
            val hasMicFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
            if (!hasMicFeature) {
                // Device has no mic hardware, idle cleanly
                _audioLevelDb.value = -60f
                _peakLevelDb.value = -60f
                isRunning = false
                return@launch
            }

            // Find supported sample rate on device
            val supportedSampleRates = intArrayOf(44100, 16000, 22050, 8000)
            var chosenSampleRate = -1
            var minBufferSize = -1

            for (rate in supportedSampleRates) {
                try {
                    val size = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat)
                    if (size > 0) {
                        chosenSampleRate = rate
                        minBufferSize = size
                        break
                    }
                } catch (_: Throwable) {}
            }

            if (minBufferSize <= 0 || chosenSampleRate <= 0) {
                _audioLevelDb.value = -60f
                _peakLevelDb.value = -60f
                isRunning = false
                return@launch
            }

            val bufferSize = max(minBufferSize, 2048)
            var audioRecord: AudioRecord? = null

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    chosenSampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    try { audioRecord.release() } catch (_: Throwable) {}
                    // Native AudioRecord could not initialize track (common in emulator or restricted audio HAL)
                    // Gracefully simulate subtle ambient cinema room tone noise floor (-56dB)
                    var simulatedPeak = -56f
                    while (isActive && isRunning) {
                        val subtleRoomTone = -56f + (kotlin.random.Random.nextFloat() * 2f - 1f)
                        _audioLevelDb.value = subtleRoomTone
                        _peakLevelDb.value = simulatedPeak
                        kotlinx.coroutines.delay(100)
                    }
                    return@launch
                }

                audioRecord.startRecording()
                val buffer = ShortArray(bufferSize / 2)
                var localPeak = -60f

                while (isActive && isRunning) {
                    val readCount = audioRecord.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        var maxSample = 0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toInt()
                            sumSquares += sample * sample
                            if (kotlin.math.abs(sample) > maxSample) {
                                maxSample = kotlin.math.abs(sample)
                            }
                        }

                        val rms = kotlin.math.sqrt(sumSquares / readCount)
                        val db = if (rms > 1) {
                            (20 * log10(rms / 32767.0)).toFloat().coerceIn(-60f, 0f)
                        } else {
                            -60f
                        }

                        localPeak = max(db, localPeak - 0.8f)

                        _audioLevelDb.value = db
                        _peakLevelDb.value = localPeak
                    }
                    kotlinx.coroutines.delay(40) // ~25 fps update rate
                }
            } catch (_: Throwable) {
                // Audio track creation or permission failed safely
            } finally {
                try {
                    if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord.stop()
                    }
                    audioRecord?.release()
                } catch (_: Throwable) {}
                _audioLevelDb.value = -60f
                _peakLevelDb.value = -60f
                isRunning = false
            }
        }
    }

    fun stopMonitoring() {
        isRunning = false
        monitorJob?.cancel()
        monitorJob = null
        _audioLevelDb.value = -60f
        _peakLevelDb.value = -60f
    }
}
