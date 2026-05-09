package com.minimind.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.backend.BackendManager
import com.minimind.app.data.PreferencesManager
import com.minimind.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val preferencesManager = MiniMindApp.instance.preferencesManager
    private val backendManager = MiniMindApp.instance.backendManager

    private val _apiProvider = MutableStateFlow("deepseek")
    val apiProvider: StateFlow<String> = _apiProvider.asStateFlow()

    private val _apiBase = MutableStateFlow("https://api.deepseek.com/v1")
    val apiBase: StateFlow<String> = _apiBase.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _apiModel = MutableStateFlow("deepseek-chat")
    val apiModel: StateFlow<String> = _apiModel.asStateFlow()

    private val _backendStatus = MutableStateFlow(BackendManager.BackendStatus.UNKNOWN)
    val backendStatus: StateFlow<BackendManager.BackendStatus> = _backendStatus.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _isRestarting = MutableStateFlow(false)
    val isRestarting: StateFlow<Boolean> = _isRestarting.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.apiProvider.collect { _apiProvider.value = it }
        }
        viewModelScope.launch {
            preferencesManager.apiBase.collect { _apiBase.value = it }
        }
        viewModelScope.launch {
            preferencesManager.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            preferencesManager.apiModel.collect { _apiModel.value = it }
        }
        viewModelScope.launch {
            backendManager.status.collect { _backendStatus.value = it }
        }
        checkBackendStatus()
    }

    fun updateApiProvider(provider: String) {
        _apiProvider.value = provider
        viewModelScope.launch { preferencesManager.saveApiProvider(provider) }
    }

    fun updateApiBase(base: String) {
        _apiBase.value = base
        viewModelScope.launch { preferencesManager.saveApiBase(base) }
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        viewModelScope.launch { preferencesManager.saveApiKey(key) }
    }

    fun updateApiModel(model: String) {
        _apiModel.value = model
        viewModelScope.launch { preferencesManager.saveApiModel(model) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.testConnection(
                        mapOf(
                            "api_base" to _apiBase.value,
                            "api_key" to _apiKey.value,
                            "model_name" to _apiModel.value
                        )
                    )
                }
                _testResult.value = "连接成功: ${response["message"] ?: "OK"}"
            } catch (e: Exception) {
                _testResult.value = "连接失败: ${e.message}"
            }
            _isTesting.value = false
        }
    }

    fun checkBackendStatus() {
        viewModelScope.launch {
            backendManager.checkStatus()
        }
    }

    fun restartBackend() {
        viewModelScope.launch {
            _isRestarting.value = true
            backendManager.restart()
            _isRestarting.value = false
        }
    }

    fun reinitialize() {
        viewModelScope.launch {
            backendManager.initialize()
        }
    }
}
