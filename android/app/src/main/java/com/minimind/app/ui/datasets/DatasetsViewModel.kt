package com.minimind.app.ui.datasets

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.network.ApiClient
import com.minimind.app.network.model.DatasetInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DatasetsViewModel : ViewModel() {

    private val _datasets = MutableStateFlow<List<DatasetInfo>>(emptyList())
    val datasets: StateFlow<List<DatasetInfo>> = _datasets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _previewData = MutableStateFlow<List<String>?>(null)
    val previewData: StateFlow<List<String>?> = _previewData.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    init {
        loadDatasets()
    }

    fun loadDatasets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getDatasets()
                }
                _datasets.value = response.datasets
            } catch (e: Exception) {
                _error.value = "加载数据集列表失败: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun previewDataset(datasetId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.previewDataset(datasetId)
                }
                _previewData.value = response.samples
            } catch (e: Exception) {
                _error.value = "预览数据集失败: ${e.message}"
            }
        }
    }

    fun deleteDataset(datasetId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.apiService.deleteDataset(datasetId)
                }
                loadDatasets()
            } catch (e: Exception) {
                _error.value = "删除数据集失败: ${e.message}"
            }
        }
    }

    fun clearPreview() {
        _previewData.value = null
    }

    fun uploadDataset(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _error.value = null
            try {
                val context = MiniMindApp.instance
                val fileName = withContext(Dispatchers.IO) {
                    getFileNameFromUri(context, uri)
                }
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("无法读取文件")
                }
                withContext(Dispatchers.IO) {
                    val fileBody = bytes.toRequestBody("application/octet-stream".toMediaType())
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .build()
                    val request = Request.Builder()
                        .url("${ApiClient.getBaseUrl()}/api/datasets/upload")
                        .post(multipartBody)
                        .build()
                    val response = ApiClient.fetchOkHttpClient().newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("上传失败: HTTP ${response.code}")
                    }
                }
                loadDatasets()
            } catch (e: Exception) {
                _error.value = "上传数据集失败: ${e.message}"
            }
            _isUploading.value = false
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "dataset.jsonl"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
