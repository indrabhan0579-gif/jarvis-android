package com.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.jarvis.assistant.data.model.AssistantStatus
import java.util.Locale

class VoiceAssistantManager(
    private val context: Context,
    private val onStatusChanged: (AssistantStatus) -> Unit,
    private val onSpeechRecognized: (String) -> Unit,
    private val onError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val TAG = "JarvisVoiceManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var lastSpokenResponse: String = ""

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onStatusChanged(AssistantStatus.LISTENING)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        onStatusChanged(AssistantStatus.THINKING)
                    }

                    override fun onError(error: Int) {
                        onStatusChanged(AssistantStatus.IDLE)
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_CLIENT -> "Client speech error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Standing by."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                            else -> "Speech recognition error ($error)."
                        }
                        Log.w(TAG, "SpeechRecognizer error: $errorMsg")
                        onError(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            onSpeechRecognized(text)
                        } else {
                            onStatusChanged(AssistantStatus.IDLE)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            Log.w(TAG, "Speech recognition is not supported on this device.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.getDefault())
            }
            // JARVIS pitch and cadence
            textToSpeech?.setPitch(0.92f)
            textToSpeech?.setSpeechRate(1.02f)

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStatusChanged(AssistantStatus.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    onStatusChanged(AssistantStatus.IDLE)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onStatusChanged(AssistantStatus.IDLE)
                }
            })
            isTtsReady = true
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine")
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        // Stop TTS if speaking
        stopSpeaking()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            speechRecognizer?.startListening(intent)
            onStatusChanged(AssistantStatus.LISTENING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            onError("Could not start voice recognition: ${e.message}")
            onStatusChanged(AssistantStatus.IDLE)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            onStatusChanged(AssistantStatus.IDLE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listening", e)
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w(TAG, "TTS engine not ready")
            return
        }
        lastSpokenResponse = text
        textToSpeech?.stop()
        val utteranceId = "JARVIS_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        onStatusChanged(AssistantStatus.IDLE)
    }

    fun repeatLastResponse() {
        if (lastSpokenResponse.isNotEmpty()) {
            speak(lastSpokenResponse)
        } else {
            speak("I have not spoken any statements yet, Sir.")
        }
    }

    fun shutdown() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down speech engines", e)
        }
    }
}
