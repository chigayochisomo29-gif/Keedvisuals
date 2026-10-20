package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaColorScheme = darkColorScheme(
    primary = CinemaRed,
    onPrimary = Color.White,
    primaryContainer = CinemaRed.copy(alpha = 0.2f),
    onPrimaryContainer = CinemaRedBright,
    secondary = CinemaAmber,
    onSecondary = Color.Black,
    secondaryContainer = CinemaAmber.copy(alpha = 0.15f),
    onSecondaryContainer = CinemaAmber,
    tertiary = CinemaCyan,
    onTertiary = Color.Black,
    background = CinemaBlack,
    onBackground = TextWhite,
    surface = CinemaCharcoal,
    onSurface = TextWhite,
    surfaceVariant = CinemaSurface,
    onSurfaceVariant = TextMuted,
    outline = CinemaBorder,
    outlineVariant = CinemaBorder.copy(alpha = 0.5f)
)

@Composable
fun KeedCinemaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography,
        content = content
    )
}
