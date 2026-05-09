package com.minimind.app.network.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val message: String,
    @SerializedName("model_id") val modelId: String,
    val temperature: Float = 0.7f,
    @SerializedName("top_p") val topP: Float = 0.9f,
    @SerializedName("max_tokens") val maxTokens: Int = 512,
    @SerializedName("open_thinking") val openThinking: Boolean = false
)

data class ChatResponse(
    val reply: String,
    @SerializedName("reasoning_content") val reasoningContent: String? = null
)

data class ModelInfo(
    val id: String,
    val name: String,
    val size: Long,
    val source: String
)

data class StreamToken(
    val token: String,
    @SerializedName("reasoning_content") val reasoningContent: String? = null,
    val done: Boolean = false
)
