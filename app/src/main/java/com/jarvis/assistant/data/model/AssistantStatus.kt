package com.jarvis.assistant.data.model

/**
 * High-level state machine representing JARVIS core states.
 */
enum class AssistantStatus(val label: String, val description: String) {
    IDLE("IDLE", "System standing by for vocal or typed commands"),
    LISTENING("LISTENING", "Streaming audio and transcribing speech"),
    THINKING("THINKING", "Synthesizing response through Gemini neural model"),
    SPEAKING("SPEAKING", "Synthesizing voice response through TextToSpeech"),
    EXECUTING("EXECUTING", "Performing Android OS system directive")
}
