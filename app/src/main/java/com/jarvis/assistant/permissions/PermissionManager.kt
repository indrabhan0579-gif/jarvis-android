package com.jarvis.assistant.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean
)

class PermissionManager(private val context: Context) {

    fun getPermissionsState(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone (Required for voice assistant)
        list.add(
            PermissionItem(
                permission = Manifest.permission.RECORD_AUDIO,
                title = "Microphone Access",
                description = "Required to capture and transcribe vocal directives to JARVIS.",
                isGranted = isGranted(Manifest.permission.RECORD_AUDIO),
                isRequired = true
            )
        )

        // 2. Camera (Optional, needed for LED flashlight toggle)
        list.add(
            PermissionItem(
                permission = Manifest.permission.CAMERA,
                title = "Camera / Flashlight",
                description = "Enables JARVIS to toggle the device's hardware LED flashlight on demand.",
                isGranted = isGranted(Manifest.permission.CAMERA),
                isRequired = false
            )
        )

        // 3. Android 13 Notifications (Optional/Recommended)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    title = "Post Notifications (Android 13)",
                    description = "Allows JARVIS to deliver scheduled reminders and background operational alerts.",
                    isGranted = isGranted(Manifest.permission.POST_NOTIFICATIONS),
                    isRequired = false
                )
            )
        }

        return list
    }

    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun areCriticalPermissionsGranted(): Boolean {
        return isGranted(Manifest.permission.RECORD_AUDIO)
    }
}
