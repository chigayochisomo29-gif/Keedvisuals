package com.example.util

import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

object TimecodeFormatter {
    /**
     * Formats elapsed milliseconds into SMPTE-like timecode (HH:MM:SS:FF where FF is estimated frame count)
     */
    fun formatTimecode(durationMs: Long, fps: Int = 30): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val frames = ((durationMs % 1000) * fps / 1000).toInt()
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frames)
    }

    /**
     * Formats simple recording time MM:SS
     */
    fun formatSimpleTimer(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Formats shutter speed nanos to fractional seconds string (e.g. 1/50, 1/120)
     */
    fun formatShutterSpeed(exposureTimeNanos: Long): String {
        if (exposureTimeNanos <= 0) return "AUTO"
        val seconds = exposureTimeNanos / 1_000_000_000.0
        val denominator = (1.0 / seconds).toInt()
        return if (denominator > 0) "1/$denominator" else "${String.format(Locale.US, "%.1f", seconds)}s"
    }

    /**
     * Calculates shutter angle for cinema reference (e.g. 180° rule)
     */
    fun calculateShutterAngle(exposureTimeNanos: Long, fps: Int): String {
        if (exposureTimeNanos <= 0 || fps <= 0) return "180.0°"
        val seconds = exposureTimeNanos / 1_000_000_000.0
        val angle = seconds * fps * 360.0
        return String.format(Locale.US, "%.1f°", angle.coerceIn(10.0, 360.0))
    }
}

object StorageUtils {
    /**
     * Calculates free space on external/primary storage in gigabytes
     */
    fun getFreeSpaceGb(): Double {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable / (1024.0 * 1024.0 * 1024.0)
        } catch (_: Exception) {
            0.0
        }
    }

    /**
     * Estimates remaining recording minutes based on bitrate (assumes ~15-20 Mbps for 1080p, ~8 Mbps for 720p)
     */
    fun estimateRemainingTimeMinutes(freeGb: Double, is1080p: Boolean): Int {
        val mbPerMinute = if (is1080p) 140.0 else 70.0 // ~18.6 Mbps vs ~9.3 Mbps
        val freeMb = freeGb * 1024.0
        return (freeMb / mbPerMinute).toInt().coerceAtLeast(0)
    }

    fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1000) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
