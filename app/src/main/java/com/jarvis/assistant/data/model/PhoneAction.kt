package com.jarvis.assistant.data.model

enum class ActionSafetyLevel {
    SAFE_READ,       // Harmless info request (battery, time, volume level)
    BENIGN_ACTION,   // Safe reversible action (flashlight toggle, open settings screen)
    SENSITIVE_ACTION // Sensitive action (e.g. create notification, external app launch with arguments, file access)
}

data class PhoneAction(
    val name: String,
    val arguments: Map<String, Any?> = emptyMap(),
    val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.BENIGN_ACTION,
    val userFriendlyDescription: String = ""
)
