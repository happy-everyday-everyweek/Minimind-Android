package com.minimind.app.network

import com.google.gson.Gson
import com.minimind.app.network.model.StreamToken
import com.minimind.app.network.model.TrainingStatus
import okhttp3.*

interface WebSocketCallback {
    fun onConnected()
    fun onMessage(text: String)
    fun onToken(token: StreamToken)
    fun onTrainingStatus(status: TrainingStatus)
    fun onError(error: Throwable)
    fun onDisconnected()
}

class WebSocketClient(
    private val client: OkHttpClient = OkHttpClient()
) {
    private var webSocket: WebSocket? = null
    private var callback: WebSocketCallback? = null
    private val gson = Gson()

    fun setCallback(callback: WebSocketCallback) {
        this.callback = callback
    }

    fun connectInference(baseUrl: String) {
        disconnect()
        val wsUrl = baseUrl.replace("http", "ws") + "/ws/chat"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                callback?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val token = gson.fromJson(text, StreamToken::class.java)
                    callback?.onToken(token)
                } catch (e: Exception) {
                    callback?.onMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                callback?.onError(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                callback?.onDisconnected()
            }
        })
    }

    fun connectTraining(baseUrl: String, taskId: String) {
        disconnect()
        val wsUrl = baseUrl.replace("http", "ws") + "/ws/training/$taskId"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                callback?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val status = gson.fromJson(text, TrainingStatus::class.java)
                    callback?.onTrainingStatus(status)
                } catch (e: Exception) {
                    callback?.onMessage(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                callback?.onError(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                callback?.onDisconnected()
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun sendChatRequest(request: Map<String, Any>) {
        val json = gson.toJson(request)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }

    fun isConnected(): Boolean = webSocket != null
}
