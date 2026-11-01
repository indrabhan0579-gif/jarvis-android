package com.jarvis.assistant.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecution: ActionExecutionResult? = null,
    val isPendingConfirmation: Boolean = false,
    val pendingAction: PhoneAction? = null
)

data class ActionExecutionResult(
    val actionName: String,
    val isSuccess: Boolean,
    val message: String,
    val outputData: Map<String, String> = emptyMap(),
    val requiresManualIntervention: Boolean = false
)
