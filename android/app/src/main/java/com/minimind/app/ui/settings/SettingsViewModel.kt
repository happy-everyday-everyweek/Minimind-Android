package com.minimind.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.backend.BackendManager
import com.minimind.app.data.PreferencesManager
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.ResourceLimitsRequest
import com.minimind.app.network.model.ResourceLimitsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ResourceLimits(
    val maxCpuPercent: Int = 80,
    val maxMemoryMb: Int = 2048,
    val maxTrainingProcesses: Int = 1
)

class SettingsViewModel : ViewModel() {

    private val preferencesManager = MiniMindApp.instance.preferencesManager
    private val backendManager = MiniMindApp.instance.backendManager

    private val _apiBase = MutableStateFlow("")
    val apiBase: StateFlow<String> = _apiBase.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _apiModel = MutableStateFlow("")
    val apiModel: StateFlow<String> = _apiModel.asStateFlow()

    private val _resourceLimits = MutableStateFlow(ResourceLimits())
    val resourceLimits: StateFlow<ResourceLimits> = _resourceLimits.asStateFlow()

    private val _isSavingLimits = MutableStateFlow(false)
    val isSavingLimits: StateFlow<Boolean> = _isSavingLimits.asStateFlow()

    private val _saveLimitsResult = MutableStateFlow<String?>(null)
    val saveLimitsResult: StateFlow<String?> = _saveLimitsResult.asStateFlow()

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
        loadResourceLimits()
        checkBackendStatus()
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

    fun updateMaxCpuPercent(value: Int) {
        _resourceLimits.value = _resourceLimits.value.copy(maxCpuPercent = value.coerceIn(0, 100))
    }

    fun updateMaxMemoryMb(value: Int) {
        _resourceLimits.value = _resourceLimits.value.copy(maxMemoryMb = value)
    }

    fun updateMaxTrainingProcesses(value: Int) {
        _resourceLimits.value = _resourceLimits.value.copy(maxTrainingProcesses = value)
    }

    private fun loadResourceLimits() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getResourceLimits()
                }
                _resourceLimits.value = ResourceLimits(
                    maxCpuPercent = response.max_cpu_percent,
                    maxMemoryMb = response.max_memory_mb,
                    maxTrainingProcesses = response.max_training_processes
                )
            } catch (_: Exception) {
            }
        }
    }

    fun saveResourceLimits() {
        viewModelScope.launch {
            _isSavingLimits.value = true
            _saveLimitsResult.value = null
            try {
                val limits = _resourceLimits.value
                val request = ResourceLimitsRequest(
                    max_cpu_percent = limits.maxCpuPercent,
                    max_memory_mb = limits.maxMemoryMb,
                    max_training_processes = limits.maxTrainingProcesses
                )
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.updateResourceLimits(request)
                }
                _saveLimitsResult.value = "保存成功"
            } catch (e: Exception) {
                _saveLimitsResult.value = "保存失败: ${e.message}"
            }
            _isSavingLimits.value = false
        }
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
