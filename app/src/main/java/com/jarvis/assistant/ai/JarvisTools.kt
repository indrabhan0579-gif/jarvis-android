package com.jarvis.assistant.ai

import com.google.ai.client.generativeai.type.defineFunction
import com.google.ai.client.generativeai.type.Schema
import com.google.ai.client.generativeai.type.Tool
import com.jarvis.assistant.data.model.ActionSafetyLevel

/**
 * Explicit allowlist of phone actions supported by JARVIS on Android 13.
 * AI is strictly prevented from executing arbitrary shell commands or undeclared actions.
 */
object JarvisTools {

    // Allowlist of safe function names
    const val TOOL_GET_BATTERY = "get_battery_status"
    const val TOOL_GET_DATE_TIME = "get_date_time"
    const val TOOL_GET_VOLUME = "get_media_volume"
    const val TOOL_SET_VOLUME = "set_media_volume"
    const val TOOL_TOGGLE_FLASHLIGHT = "toggle_flashlight"
    const val TOOL_OPEN_APP = "open_installed_app"
    const val TOOL_OPEN_WIFI_SETTINGS = "open_wifi_settings"
    const val TOOL_OPEN_BLUETOOTH_SETTINGS = "open_bluetooth_settings"
    const val TOOL_OPEN_DISPLAY_SETTINGS = "open_display_settings"
    const val TOOL_OPEN_SOUND_SETTINGS = "open_sound_settings"
    const val TOOL_OPEN_NOTIFICATION_SETTINGS = "open_notification_settings"
    const val TOOL_OPEN_APP_PERMISSIONS = "open_app_permissions"
    const val TOOL_CREATE_REMINDER = "create_reminder_notification"
    const val TOOL_PICK_FILE = "pick_user_document"

    val ALLOWLISTED_TOOL_NAMES = setOf(
        TOOL_GET_BATTERY,
        TOOL_GET_DATE_TIME,
        TOOL_GET_VOLUME,
        TOOL_SET_VOLUME,
        TOOL_TOGGLE_FLASHLIGHT,
        TOOL_OPEN_APP,
        TOOL_OPEN_WIFI_SETTINGS,
        TOOL_OPEN_BLUETOOTH_SETTINGS,
        TOOL_OPEN_DISPLAY_SETTINGS,
        TOOL_OPEN_SOUND_SETTINGS,
        TOOL_OPEN_NOTIFICATION_SETTINGS,
        TOOL_OPEN_APP_PERMISSIONS,
        TOOL_CREATE_REMINDER,
        TOOL_PICK_FILE
    )

    fun getSafetyLevel(toolName: String): ActionSafetyLevel {
        return when (toolName) {
            TOOL_GET_BATTERY, TOOL_GET_DATE_TIME, TOOL_GET_VOLUME -> ActionSafetyLevel.SAFE_READ
            TOOL_CREATE_REMINDER, TOOL_PICK_FILE -> ActionSafetyLevel.SENSITIVE_ACTION
            else -> ActionSafetyLevel.BENIGN_ACTION
        }
    }

    /**
     * Gemini FunctionDeclarations for the official Google GenAI Android SDK.
     */
    fun createToolDeclarations(): Tool {
        val getBattery = defineFunction(
            name = TOOL_GET_BATTERY,
            description = "Get current battery level percentage, charging state, and power saver mode.",
            parameters = emptyList()
        )

        val getDateTime = defineFunction(
            name = TOOL_GET_DATE_TIME,
            description = "Get current device date, time, day of the week, and timezone.",
            parameters = emptyList()
        )

        val getVolume = defineFunction(
            name = TOOL_GET_VOLUME,
            description = "Read current media volume percentage and maximum supported stream volume.",
            parameters = emptyList()
        )

        val setVolume = defineFunction(
            name = TOOL_SET_VOLUME,
            description = "Adjust media volume level to a target percentage between 0 and 100.",
            parameters = listOf(
                Schema.int(
                    name = "volume_percent",
                    description = "Target volume percentage from 0 (mute) to 100 (maximum)."
                )
            )
        )

        val toggleFlashlight = defineFunction(
            name = TOOL_TOGGLE_FLASHLIGHT,
            description = "Turn device hardware camera flashlight/torch on or off.",
            parameters = listOf(
                Schema.bool(
                    name = "enable",
                    description = "True to turn on flashlight, False to turn off."
                )
            )
        )

        val openApp = defineFunction(
            name = TOOL_OPEN_APP,
            description = "Open an installed Android application by name (e.g., YouTube, Camera, Calculator, Clock, Maps).",
            parameters = listOf(
                Schema.str(
                    name = "app_name",
                    description = "Common name of the app to launch."
                )
            )
        )

        val openWifiSettings = defineFunction(
            name = TOOL_OPEN_WIFI_SETTINGS,
            description = "Open Android Wi-Fi configuration settings.",
            parameters = emptyList()
        )

        val openBluetoothSettings = defineFunction(
            name = TOOL_OPEN_BLUETOOTH_SETTINGS,
            description = "Open Android Bluetooth connections settings.",
            parameters = emptyList()
        )

        val openDisplaySettings = defineFunction(
            name = TOOL_OPEN_DISPLAY_SETTINGS,
            description = "Open Android Display and brightness settings.",
            parameters = emptyList()
        )

        val openSoundSettings = defineFunction(
            name = TOOL_OPEN_SOUND_SETTINGS,
            description = "Open Android Sound and vibration settings.",
            parameters = emptyList()
        )

        val openNotificationSettings = defineFunction(
            name = TOOL_OPEN_NOTIFICATION_SETTINGS,
            description = "Open Android Notification settings screen.",
            parameters = emptyList()
        )

        val openAppPermissions = defineFunction(
            name = TOOL_OPEN_APP_PERMISSIONS,
            description = "Open JARVIS app permission details in Android System Settings.",
            parameters = emptyList()
        )

        val createReminder = defineFunction(
            name = TOOL_CREATE_REMINDER,
            description = "Post a reminder notification to the Android notification drawer.",
            parameters = listOf(
                Schema.str(name = "title", description = "Reminder or alert title."),
                Schema.str(name = "details", description = "Details or description of the reminder.")
            )
        )

        val pickFile = defineFunction(
            name = TOOL_PICK_FILE,
            description = "Open Android Storage Access Framework document picker to let the user select a file.",
            parameters = listOf(
                Schema.str(
                    name = "mime_type",
                    description = "Filter MIME type, default '*/*' or 'image/*' or 'application/pdf'."
                )
            )
        )

        return Tool(
            listOf(
                getBattery,
                getDateTime,
                getVolume,
                setVolume,
                toggleFlashlight,
                openApp,
                openWifiSettings,
                openBluetoothSettings,
                openDisplaySettings,
                openSoundSettings,
                openNotificationSettings,
                openAppPermissions,
                createReminder,
                pickFile
            )
        )
    }
}
