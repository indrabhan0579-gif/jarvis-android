package com.jarvis.assistant.ai

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FunctionCallPart
import com.google.ai.client.generativeai.type.FunctionResponsePart
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.jarvis.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class GeminiResponse {
    data class TextSuccess(val message: String) : GeminiResponse()
    data class ToolCallRequested(val toolName: String, val arguments: Map<String, Any?>) : GeminiResponse()
    data class Error(val userFriendlyMessage: String, val isKeyMissing: Boolean = false) : GeminiResponse()
}

class GeminiAiService(private val context: Context) {

    private val TAG = "JarvisGeminiAI"
    private var activeModel: GenerativeModel? = null
    private var activeChat: Chat? = null
    private var currentModelName = "gemini-3.8-flash"

    private val systemPrompt = """
        You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), an elite AI assistant.
        Your tone is polite, concise, British-accented, intelligent, and confident.
        Address the user respectfully as 'Sir' or 'Ma'am'.
        You have direct integration with Android phone capabilities via tools.
        Always execute tools when asked to check battery, time, adjust volume, toggle flashlight, open apps,
        open phone settings, or schedule reminders.
        Never fabricate actions or claim a phone setting changed without tool confirmation.
        Keep verbal replies brief (1-3 sentences) suitable for TextToSpeech.
    """.trimIndent()

    init {
        initModel()
    }

    private fun getStoredApiKey(): String {
        val prefs = context.getSharedPreferences("jarvis_config", Context.MODE_PRIVATE)
        val userConfiguredKey = prefs.getString("custom_api_key", null)?.trim()
        if (!userConfiguredKey.isNullOrEmpty()) {
            return userConfiguredKey
        }
        return BuildConfig.GEMINI_API_KEY
    }

    fun updateModelConfig(modelName: String) {
        currentModelName = modelName
        initModel()
    }

    private fun initModel() {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
            activeModel = null
            activeChat = null
            return
        }

        try {
            val tool = JarvisTools.createToolDeclarations()
            activeModel = GenerativeModel(
                modelName = currentModelName,
                apiKey = apiKey,
                systemInstruction = content { text(systemPrompt) },
                tools = listOf(tool),
                requestOptions = RequestOptions(apiVersion = "v1beta")
            )
            activeChat = activeModel?.startChat()
            Log.d(TAG, "Initialized Gemini model: $currentModelName")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Gemini model", e)
            activeModel = null
            activeChat = null
        }
    }

    suspend fun sendMessage(userInput: String): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext GeminiResponse.Error(
                "Gemini API key is not configured. Please add your key in Settings or local.properties.",
                isKeyMissing = true
            )
        }

        if (activeChat == null || activeModel == null) {
            initModel()
        }

        val chat = activeChat ?: return@withContext GeminiResponse.Error(
            "Neural subsystem not initialized. Check your network or API key configuration."
        )

        try {
            val response = chat.sendMessage(userInput)
            val functionCalls = response.functionCalls

            if (functionCalls.isNotEmpty()) {
                val call = functionCalls.first()
                val toolName = call.name
                
                // Security check against allowlist
                if (!JarvisTools.ALLOWLISTED_TOOL_NAMES.contains(toolName)) {
                    Log.w(TAG, "AI attempted to invoke non-allowlisted tool: $toolName")
                    return@withContext GeminiResponse.Error(
                        "Security protocol: Directive '$toolName' was rejected by Android security sandbox."
                    )
                }

                // Convert arguments map
                val args = call.args.mapValues { (_, value) -> value as Any? }
                return@withContext GeminiResponse.ToolCallRequested(toolName, args)
            }

            val text = response.text?.trim()
            if (!text.isNullOrEmpty()) {
                return@withContext GeminiResponse.TextSuccess(text)
            } else {
                return@withContext GeminiResponse.TextSuccess("Directive acknowledged, Sir.")
            }

        } catch (e: InvalidAPIKeyException) {
            Log.e(TAG, "Invalid API key detected")
            return@withContext GeminiResponse.Error("The provided Gemini API Key is invalid. Please verify it in Settings.", isKeyMissing = true)
        } catch (e: PromptBlockedException) {
            return@withContext GeminiResponse.Error("The prompt was blocked by safety filters.")
        } catch (e: Exception) {
            Log.e(TAG, "Gemini communication failure: ${e.message}")
            val msg = when {
                e.message?.contains("429", ignoreCase = true) == true -> "API rate limit reached. Standing by briefly."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "Network unreachable. Please check your Wi-Fi or cellular connection."
                else -> "An error occurred during neural synthesis: ${e.localizedMessage ?: "Unknown failure"}"
            }
            return@withContext GeminiResponse.Error(msg)
        }
    }

    /**
     * Sends the outcome of a phone action back to Gemini so it can generate the conversational answer.
     */
    suspend fun sendToolResponse(toolName: String, output: Map<String, Any>): GeminiResponse = withContext(Dispatchers.IO) {
        val chat = activeChat ?: return@withContext GeminiResponse.Error("Chat session lost.")
        try {
            val jsonObject = JSONObject(output)
            val response = chat.sendMessage(
                content("user") {
                    part(FunctionResponsePart(toolName, jsonObject))
                }
            )
            val text = response.text?.trim() ?: "Action executed successfully, Sir."
            GeminiResponse.TextSuccess(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error returning tool result to model", e)
            GeminiResponse.TextSuccess("Executed directive '$toolName' successfully, Sir.")
        }
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext Pair(false, "API Key missing.")
        }
        try {
            val testModel = GenerativeModel(
                modelName = currentModelName,
                apiKey = apiKey
            )
            val res = testModel.generateContent("Ping. Answer with one word: Connected.")
            val text = res.text?.trim() ?: ""
            if (text.isNotEmpty()) {
                Pair(true, "Connected successfully ($currentModelName online)")
            } else {
                Pair(false, "Empty response from Gemini endpoint")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection test failed")
        }
    }

    fun resetChat() {
        activeChat = activeModel?.startChat()
    }
}
