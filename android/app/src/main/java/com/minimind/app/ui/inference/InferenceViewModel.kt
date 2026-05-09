package com.minimind.app.ui.inference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.data.ActivityRecord
import com.minimind.app.network.ApiClient
import com.minimind.app.network.WebSocketClient
import com.minimind.app.network.WebSocketCallback
import com.minimind.app.network.model.ModelInfo
import com.minimind.app.network.model.StreamToken
import com.minimind.app.network.model.TrainingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val reasoningContent: String? = null,
    val isStreaming: Boolean = false
)

class InferenceViewModel : ViewModel() {

    private val webSocketClient = WebSocketClient()
    private val activityRepository = MiniMindApp.instance.activityRepository

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _models = MutableStateFlow<List<ModelInfo>>(emptyList())
    val models: StateFlow<List<ModelInfo>> = _models.asStateFlow()

    private val _selectedModelId = MutableStateFlow("")
    val selectedModelId: StateFlow<String> = _selectedModelId.asStateFlow()

    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _topP = MutableStateFlow(0.9f)
    val topP: StateFlow<Float> = _topP.asStateFlow()

    private val _maxTokens = MutableStateFlow(512)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _openThinking = MutableStateFlow(false)
    val openThinking: StateFlow<Boolean> = _openThinking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        webSocketClient.setCallback(object : WebSocketCallback {
            override fun onConnected() {
                _isConnected.value = true
            }

            override fun onMessage(text: String) {}

            override fun onToken(token: StreamToken) {
                val currentMessages = _messages.value.toMutableList()
                val lastMessage = currentMessages.lastOrNull()
                if (lastMessage != null && !lastMessage.isUser && lastMessage.isStreaming) {
                    val updated = lastMessage.copy(
                        content = lastMessage.content + token.token,
                        reasoningContent = if (token.reasoningContent != null) {
                            (lastMessage.reasoningContent ?: "") + token.reasoningContent
                        } else {
                            lastMessage.reasoningContent
                        }
                    )
                    currentMessages[currentMessages.lastIndex] = updated
                    _messages.value = currentMessages
                }
                if (token.done) {
                    val currentMsgs = _messages.value.toMutableList()
                    val last = currentMsgs.lastOrNull()
                    if (last != null && !last.isUser && last.isStreaming) {
                        currentMsgs[currentMsgs.lastIndex] = last.copy(isStreaming = false)
                        _messages.value = currentMsgs
                    }
                    _isLoading.value = false
                }
            }

            override fun onTrainingStatus(status: TrainingStatus) {}

            override fun onError(error: Throwable) {
                _isConnected.value = false
                _isLoading.value = false
            }

            override fun onDisconnected() {
                _isConnected.value = false
            }
        })
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getModels()
                }
                _models.value = response.models
                if (_selectedModelId.value.isEmpty() && response.models.isNotEmpty()) {
                    _selectedModelId.value = response.models.first().id
                }
            } catch (e: Exception) {
                _models.value = listOf(
                    ModelInfo("default", "MiniMind-Default", 0L, "local")
                )
                _selectedModelId.value = "default"
            }
        }
    }

    fun selectModel(modelId: String) {
        _selectedModelId.value = modelId
    }

    fun updateTemperature(value: Float) {
        _temperature.value = value
    }

    fun updateTopP(value: Float) {
        _topP.value = value
    }

    fun updateMaxTokens(value: Int) {
        _maxTokens.value = value
    }

    fun updateOpenThinking(value: Boolean) {
        _openThinking.value = value
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(content = text, isUser = true)
        val assistantMessage = ChatMessage(
            content = "",
            isUser = false,
            isStreaming = true
        )
        _messages.value = _messages.value + listOf(userMessage, assistantMessage)
        _isLoading.value = true

        if (!_isConnected.value) {
            webSocketClient.connectInference(ApiClient.getBaseUrl())
        }

        val request = mapOf(
            "message" to text,
            "model_id" to _selectedModelId.value,
            "temperature" to _temperature.value,
            "top_p" to _topP.value,
            "max_tokens" to _maxTokens.value,
            "open_thinking" to _openThinking.value
        )
        webSocketClient.sendChatRequest(request)

        activityRepository.addActivity(
            ActivityRecord(
                id = java.util.UUID.randomUUID().toString(),
                type = "inference",
                title = "推理对话: ${text.take(30)}",
                status = "completed"
            )
        )
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
}
