package com.example.util

import android.content.Context
import android.view.OrientationEventListener
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.compositionLocalOf

val LocalUiRotation = compositionLocalOf { 0f }

/**
 * Monitors the physical orientation of the device using Android's hardware sensor OrientationEventListener.
 * Maps sensor orientation to 0°, 90°, 180°, or 270° so UI text and icons can rotate to remain readable.
 */
@Composable
fun rememberDeviceUiRotation(): State<Float> {
    val context = LocalContext.current
    val targetAngle = remember { mutableFloatStateOf(0f) }
    val animatedAngle = remember { Animatable(0f) }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                // Map 360-degree sensor to 4 cardinal cinema orientations with 45-degree hysteresis
                val newAngle = when (orientation) {
                    in 45 until 135 -> 270f  // Landscape right
                    in 135 until 225 -> 180f // Reverse portrait
                    in 225 until 315 -> 90f  // Landscape left
                    else -> 0f              // Portrait upright
                }

                if (targetAngle.floatValue != newAngle) {
                    targetAngle.floatValue = newAngle
                }
            }
        }

        if (listener.canDetectOrientation()) {
            listener.enable()
        }

        onDispose {
            listener.disable()
        }
    }

    LaunchedEffect(targetAngle.floatValue) {
        val target = targetAngle.floatValue
        var current = animatedAngle.value

        // Normalize angle difference to take shortest rotation path
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f

        animatedAngle.animateTo(
            targetValue = current + diff,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    return animatedAngle.asState()
}
