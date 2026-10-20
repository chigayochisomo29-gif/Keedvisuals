package com.example

import com.example.camera.FocusPeakingColor
import com.example.camera.VideoQualityOption
import com.example.ui.components.formatDioptersToDistance
import com.example.util.TimecodeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyTimecodeFormatting() {
    val formatted = TimecodeFormatter.formatTimecode(65432L, 30)
    // 65432 ms = 1 min 5 sec 432 ms -> 00:01:05:12 (432 * 30 / 1000 = 12)
    assertEquals("00:01:05:12", formatted)
  }

  @Test
  fun verifyShutterSpeedFormatting() {
    val shutterNanos = 1_000_000_000L / 50L
    val shutter = TimecodeFormatter.formatShutterSpeed(shutterNanos)
    assertEquals("1/50", shutter)
  }

  @Test
  fun verifyVideoQualityOptions() {
    assertEquals(1920, VideoQualityOption.FHD_1080P.width)
    assertEquals(1080, VideoQualityOption.FHD_1080P.height)
    assertEquals(1280, VideoQualityOption.HD_720P.width)
  }

  @Test
  fun verifyDiopterDistanceFormatting() {
    val inf = formatDioptersToDistance(0.0f)
    assertEquals("INF ∞", inf)

    val oneMeter = formatDioptersToDistance(1.0f)
    assertEquals("1.00 m", oneMeter)

    val halfMeter = formatDioptersToDistance(2.0f)
    assertEquals("50 cm", halfMeter)

    val macro = formatDioptersToDistance(10.0f)
    assertEquals("0.1m MACRO", macro)
  }

  @Test
  fun verifyFocusPeakingColors() {
    val green = FocusPeakingColor.GREEN
    val red = FocusPeakingColor.RED
    val cyan = FocusPeakingColor.CYAN

    assertEquals("Green", green.label)
    assertEquals(0xFF00E676, green.hexColor)
    assertEquals("Red", red.label)
    assertEquals("Cyan", cyan.label)
  }
}
