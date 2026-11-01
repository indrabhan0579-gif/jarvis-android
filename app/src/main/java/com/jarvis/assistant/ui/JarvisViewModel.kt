package com.jarvis.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.assistant.actions.PhoneActionManager
import com.jarvis.assistant.ai.GeminiAiService
import com.jarvis.assistant.ai.GeminiResponse
import com.jarvis.assistant.ai.JarvisTools
import com.jarvis.assistant.data.model.ActionExecutionResult
import com.jarvis.assistant.data.model.ActionSafetyLevel
import com.jarvis.assistant.data.model.AssistantStatus
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.PhoneAction
import com.jarvis.assistant.data.repository.ConversationRepository
import com.jarvis.assistant.permissions.PermissionItem
import com.jarvis.assistant.permissions.PermissionManager
import com.jarvis.assistant.voice.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConversationRepository(application)
    private val actionManager = PhoneActionManager(application)
    private val permissionManager = PermissionManager(application)
    val geminiService = GeminiAiService(application)

    private val _status = MutableStateFlow(AssistantStatus.IDLE)
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _pendingAction = MutableStateFlow<PhoneAction?>(null)
    val pendingAction: StateFlow<PhoneAction?> = _pendingAction.asStateFlow()

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    private val _currentModel = MutableStateFlow("gemini-3.8-flash")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    private var voiceAssistant: VoiceAssistantManager? = null

    init {
        _messages.value = repository.loadMessages()
        refreshPermissions()

        voiceAssistant = VoiceAssistantManager(
            context = application,
            onStatusChanged = { newStatus -> _status.value = newStatus },
            onSpeechRecognized = { transcript -> onUserSubmittedText(transcript) },
            onError = { errorMsg ->
                addSystemMessage(errorMsg)
                _status.value = AssistantStatus.IDLE
            }
        )
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.getPermissionsState()
    }

    fun startListening() {
        voiceAssistant?.startListening()
    }

    fun stopListening() {
        voiceAssistant?.stopListening()
    }

    fun stopSpeaking() {
        voiceAssistant?.stopSpeaking()
    }

    fun repeatLastResponse() {
        voiceAssistant?.repeatLastResponse()
    }

    fun onUserSubmittedText(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = text)
        appendMessage(userMsg)

        _status.value = AssistantStatus.THINKING

        viewModelScope.launch {
            when (val response = geminiService.sendMessage(text)) {
                is GeminiResponse.TextSuccess -> {
                    val reply = response.message
                    appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = reply))
                    voiceAssistant?.speak(reply)
                }
                is GeminiResponse.ToolCallRequested -> {
                    handleToolCall(response.toolName, response.arguments)
                }
                is GeminiResponse.Error -> {
                    _status.value = AssistantStatus.IDLE
                    appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = response.userFriendlyMessage))
                    voiceAssistant?.speak(response.userFriendlyMessage)
                }
            }
        }
    }

    private fun handleToolCall(toolName: String, args: Map<String, Any?>) {
        val safetyLevel = JarvisTools.getSafetyLevel(toolName)
        val action = PhoneAction(name = toolName, arguments = args, safetyLevel = safetyLevel)

        if (safetyLevel == ActionSafetyLevel.SENSITIVE_ACTION) {
            _pendingAction.value = action
            _status.value = AssistantStatus.IDLE
            addSystemMessage("Safety protocol: Confirmation required to execute '${toolName}'.")
            return
        }

        // Direct execution for safe tools
        executeTool(action)
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        executeTool(action)
    }

    fun cancelPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        addSystemMessage("Directive '${action.name}' aborted by user.")
        _status.value = AssistantStatus.IDLE
    }

    private fun executeTool(action: PhoneAction) {
        _status.value = AssistantStatus.EXECUTING

        viewModelScope.launch {
            val result: ActionExecutionResult = when (action.name) {
                JarvisTools.TOOL_GET_BATTERY -> actionManager.getBatteryStatus()
                JarvisTools.TOOL_GET_DATE_TIME -> actionManager.getDateTime()
                JarvisTools.TOOL_GET_VOLUME -> actionManager.getMediaVolume()
                JarvisTools.TOOL_SET_VOLUME -> {
                    val pct = (action.arguments["volume_percent"] as? Number)?.toInt() ?: 50
                    actionManager.setMediaVolume(pct)
                }
                JarvisTools.TOOL_TOGGLE_FLASHLIGHT -> {
                    val enable = (action.arguments["enable"] as? Boolean) ?: true
                    actionManager.toggleFlashlight(enable)
                }
                JarvisTools.TOOL_OPEN_APP -> {
                    val appName = (action.arguments["app_name"] as? String) ?: ""
                    actionManager.openInstalledApp(appName)
                }
                JarvisTools.TOOL_OPEN_WIFI_SETTINGS -> actionManager.openWifiSettings()
                JarvisTools.TOOL_OPEN_BLUETOOTH_SETTINGS -> actionManager.openBluetoothSettings()
                JarvisTools.TOOL_OPEN_DISPLAY_SETTINGS -> actionManager.openDisplaySettings()
                JarvisTools.TOOL_OPEN_SOUND_SETTINGS -> actionManager.openSoundSettings()
                JarvisTools.TOOL_OPEN_NOTIFICATION_SETTINGS -> actionManager.openNotificationSettings()
                JarvisTools.TOOL_OPEN_APP_PERMISSIONS -> actionManager.openAppPermissions()
                JarvisTools.TOOL_CREATE_REMINDER -> {
                    val title = (action.arguments["title"] as? String) ?: "JARVIS Alert"
                    val details = (action.arguments["details"] as? String) ?: "Scheduled reminder."
                    actionManager.createReminderNotification(title, details)
                }
                else -> ActionExecutionResult(action.name, false, "Unknown or unsupported tool '${action.name}'")
            }

            // Record action in conversation log
            val actionMessage = ChatMessage(
                sender = MessageSender.SYSTEM,
                text = result.message,
                actionExecution = result
            )
            appendMessage(actionMessage)

            // Feed output back to Gemini to complete conversational loop
            val outputData = mutableMapOf<String, Any>(
                "status" to if (result.isSuccess) "success" else "failed",
                "message" to result.message
            )
            outputData.putAll(result.outputData)

            _status.value = AssistantStatus.THINKING
            when (val followUp = geminiService.sendToolResponse(action.name, outputData)) {
                is GeminiResponse.TextSuccess -> {
                    appendMessage(ChatMessage(sender = MessageSender.JARVIS, text = followUp.message))
                    voiceAssistant?.speak(followUp.message)
                }
                else -> {
                    _status.value = AssistantStatus.IDLE
                    voiceAssistant?.speak(result.message)
                }
            }
        }
    }

    private fun appendMessage(message: ChatMessage) {
        val updated = _messages.value + message
        _messages.value = updated
        repository.saveMessages(updated)
    }

    private fun addSystemMessage(text: String) {
        appendMessage(ChatMessage(sender = MessageSender.SYSTEM, text = text))
    }

    fun clearHistory() {
        repository.clearMessages()
        geminiService.resetChat()
        _messages.value = repository.getInitialGreeting()
    }

    fun setModel(model: String) {
        _currentModel.value = model
        geminiService.updateModelConfig(model)
    }

    fun openAppSettings() {
        actionManager.openAppPermissions()
    }

    override fun onCleared() {
        super.onCleared()
        voiceAssistant?.shutdown()
    }
}
