package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.ui.screens.CinemaCameraScreen
import com.example.ui.screens.CinemaGalleryScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.theme.KeedCinemaTheme

enum class CinemaDestination {
    CAMERA,
    GALLERY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeedCinemaTheme {
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var currentDestination by remember { mutableStateOf(CinemaDestination.CAMERA) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (!hasCameraPermission) {
                        PermissionScreen(
                            onPermissionsGranted = {
                                hasCameraPermission = true
                            }
                        )
                    } else {
                        when (currentDestination) {
                            CinemaDestination.CAMERA -> {
                                CinemaCameraScreen(
                                    onNavigateToGallery = {
                                        currentDestination = CinemaDestination.GALLERY
                                    }
                                )
                            }
                            CinemaDestination.GALLERY -> {
                                CinemaGalleryScreen(
                                    onNavigateBack = {
                                        currentDestination = CinemaDestination.CAMERA
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
