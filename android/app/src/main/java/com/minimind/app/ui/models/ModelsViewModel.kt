package com.minimind.app.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.ModelWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelsViewModel : ViewModel() {

    private val _models = MutableStateFlow<List<ModelWeight>>(emptyList())
    val models: StateFlow<List<ModelWeight>> = _models.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getModelWeights()
                }
                _models.value = response.models
            } catch (e: Exception) {
                _error.value = "加载模型列表失败: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.deleteModel(modelId)
                }
                loadModels()
            } catch (e: Exception) {
                _error.value = "删除模型失败: ${e.message}"
            }
        }
    }

    fun exportModel(modelId: String, exportPath: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.exportModel(
                        com.minimind.app.network.model.ModelExportRequest(modelId, exportPath)
                    )
                }
            } catch (e: Exception) {
                _error.value = "导出模型失败: ${e.message}"
            }
        }
    }

    fun downloadModel(url: String, name: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.downloadModel(
                        com.minimind.app.network.model.ModelDownloadRequest(url, name)
                    )
                }
                loadModels()
            } catch (e: Exception) {
                _error.value = "下载模型失败: ${e.message}"
            }
        }
    }
}
