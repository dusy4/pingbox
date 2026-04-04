package com.dusy4.pingbox

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.dusy4.pingbox.ui.navigation.PingBoxApp
import com.dusy4.pingbox.ui.theme.PingBoxTheme

/**
 * Main entry point for the PingBox Android app.
 */
class MainActivity : ComponentActivity() {

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+ (Tiramisu)
        requestNotificationPermissionIfNeeded()

        // Set Compose content
        setContent {
            PingBoxTheme {
                PingBoxApp()
            }
        }
    }

    /**
     * On Android 13+, POST_NOTIFICATIONS is a runtime permission.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
