package com.jarvis.assistant.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.data.model.ActionExecutionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhoneActionManager(private val context: Context) {

    private val TAG = "JarvisActionManager"
    private val CHANNEL_ID = "jarvis_alerts_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "JARVIS Subsystem Alerts"
            val descriptionText = "Notifications and reminders scheduled by JARVIS"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Reads battery status using BatteryManager intent.
     */
    fun getBatteryStatus(): ActionExecutionResult {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct: Float = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()) else 0f

            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSave = powerManager?.isPowerSaveMode ?: false

            val message = "Battery level is at ${batteryPct.toInt()}%. Status: ${if (isCharging) "Charging" else "Discharging"}${if (isPowerSave) " (Power Saver ON)" else ""}."
            
            ActionExecutionResult(
                actionName = "get_battery_status",
                isSuccess = true,
                message = message,
                outputData = mapOf(
                    "battery_percent" to batteryPct.toInt().toString(),
                    "is_charging" to isCharging.toString(),
                    "power_save_mode" to isPowerSave.toString()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery", e)
            ActionExecutionResult("get_battery_status", false, "Failed to read battery status: ${e.message}")
        }
    }

    /**
     * Reads current date, time, and timezone.
     */
    fun getDateTime(): ActionExecutionResult {
        val now = Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val tzFormat = SimpleDateFormat("z", Locale.getDefault())

        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now)
        val tzStr = tzFormat.format(now)

        return ActionExecutionResult(
            actionName = "get_date_time",
            isSuccess = true,
            message = "The current time is $timeStr on $dateStr ($tzStr).",
            outputData = mapOf("time" to timeStr, "date" to dateStr, "timezone" to tzStr)
        )
    }

    /**
     * Reads media stream volume.
     */
    fun getMediaVolume(): ActionExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val pct = if (maxVol > 0) (currentVol * 100 / maxVol) else 0

            ActionExecutionResult(
                actionName = "get_media_volume",
                isSuccess = true,
                message = "Media volume is currently at $pct% (level $currentVol of $maxVol).",
                outputData = mapOf("percent" to pct.toString(), "current" to currentVol.toString(), "max" to maxVol.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting volume", e)
            ActionExecutionResult("get_media_volume", false, "Unable to read volume level: ${e.message}")
        }
    }

    /**
     * Adjusts media volume within valid boundaries.
     */
    fun setMediaVolume(percent: Int): ActionExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clampedPct = percent.coerceIn(0, 100)
            val targetLevel = (clampedPct * maxVol / 100).coerceIn(0, maxVol)

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, AudioManager.FLAG_SHOW_UI)

            ActionExecutionResult(
                actionName = "set_media_volume",
                isSuccess = true,
                message = "Media volume set to $clampedPct%.",
                outputData = mapOf("new_percent" to clampedPct.toString(), "stream_level" to targetLevel.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            ActionExecutionResult("set_media_volume", false, "Unable to adjust volume: ${e.message}")
        }
    }

    /**
     * Toggles hardware camera flashlight/torch.
     */
    fun toggleFlashlight(enable: Boolean): ActionExecutionResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ActionExecutionResult("toggle_flashlight", false, "CameraManager subsystem unavailable.")

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                return ActionExecutionResult("toggle_flashlight", false, "Hardware flashlight not detected on this device.")
            }

            cameraManager.setTorchMode(cameraId, enable)
            val state = if (enable) "engaged (ON)" else "disengaged (OFF)"
            ActionExecutionResult(
                actionName = "toggle_flashlight",
                isSuccess = true,
                message = "Flashlight has been $state.",
                outputData = mapOf("state" to enable.toString())
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException toggling torch", e)
            ActionExecutionResult("toggle_flashlight", false, "Hardware camera busy or permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight toggle error", e)
            ActionExecutionResult("toggle_flashlight", false, "Failed to toggle flashlight: ${e.message}")
        }
    }

    /**
     * Opens an installed application by common name.
     */
    fun openInstalledApp(appName: String): ActionExecutionResult {
        val pm = context.packageManager
        val query = appName.trim().lowercase()

        // Common app package alias map
        val commonPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "camera" to "com.google.android.GoogleCamera",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "calendar" to "com.google.android.calendar",
            "photos" to "com.google.android.apps.photos",
            "files" to "com.google.android.apps.nbu.files"
        )

        try {
            var targetIntent: Intent? = null

            if (commonPackages.containsKey(query)) {
                targetIntent = pm.getLaunchIntentForPackage(commonPackages[query]!!)
            }

            if (targetIntent == null) {
                // Search installed launchable packages
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val pkgAppsList = pm.queryIntentActivities(mainIntent, 0)
                val match = pkgAppsList.firstOrNull {
                    it.loadLabel(pm).toString().lowercase().contains(query)
                }
                if (match != null) {
                    targetIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
                }
            }

            if (targetIntent != null) {
                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(targetIntent)
                return ActionExecutionResult("open_installed_app", true, "Launching $appName.")
            } else {
                return ActionExecutionResult(
                    actionName = "open_installed_app",
                    isSuccess = false,
                    message = "Could not locate an installed app matching '$appName'.",
                    requiresManualIntervention = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $appName", e)
            return ActionExecutionResult("open_installed_app", false, "Failed to launch app: ${e.message}")
        }
    }

    /**
     * Opens settings screens. Android 10+ restricts toggling Wi-Fi/Bluetooth directly from apps,
     * so modern Android guidelines require opening the official system intent.
     */
    fun openSettingsScreen(action: String, title: String): ActionExecutionResult {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionExecutionResult(
                actionName = "open_settings",
                isSuccess = true,
                message = "Opened $title settings. Android requires toggling this setting manually.",
                requiresManualIntervention = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings $action", e)
            ActionExecutionResult("open_settings", false, "Could not open $title settings: ${e.message}")
        }
    }

    fun openWifiSettings() = openSettingsScreen(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi")
    fun openBluetoothSettings() = openSettingsScreen(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth")
    fun openDisplaySettings() = openSettingsScreen(Settings.ACTION_DISPLAY_SETTINGS, "Display")
    fun openSoundSettings() = openSettingsScreen(Settings.ACTION_SOUND_SETTINGS, "Sound")
    fun openNotificationSettings() = openSettingsScreen(Settings.ACTION_NOTIFICATION_SETTINGS, "Notification")

    /**
     * Opens Application Details in Android System Settings.
     */
    fun openAppPermissions(): ActionExecutionResult {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionExecutionResult("open_app_permissions", true, "Opened JARVIS application permission settings.")
        } catch (e: Exception) {
            ActionExecutionResult("open_app_permissions", false, "Failed to open permissions: ${e.message}")
        }
    }

    /**
     * Creates a reminder or alert notification (requires POST_NOTIFICATIONS on Android 13).
     */
    fun createReminderNotification(title: String, details: String): ActionExecutionResult {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Check Android 13 permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPerm) {
                    return ActionExecutionResult(
                        actionName = "create_reminder_notification",
                        isSuccess = false,
                        message = "Notification permission is not granted on Android 13. Please grant permission.",
                        requiresManualIntervention = true
                    )
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(details)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())

            ActionExecutionResult(
                actionName = "create_reminder_notification",
                isSuccess = true,
                message = "Reminder created: '$title' posted to notifications.",
                outputData = mapOf("title" to title, "details" to details)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Notification post failed", e)
            ActionExecutionResult("create_reminder_notification", false, "Failed to schedule notification: ${e.message}")
        }
    }
}
