package com.minimind.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimind.app.MiniMindApp
import com.minimind.app.backend.BackendManager
import com.minimind.app.data.ActivityRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val app = MiniMindApp.instance
    private val backendManager = app.backendManager
    private val activityRepository = app.activityRepository

    val backendStatus: StateFlow<BackendManager.BackendStatus> = backendManager.status
    val recentActivities = activityRepository.activities

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    init {
        checkBackendStatus()
    }

    fun checkBackendStatus() {
        viewModelScope.launch {
            backendManager.checkStatus()
        }
    }

    fun reinitialize() {
        viewModelScope.launch {
            _isInitializing.value = true
            backendManager.initialize()
            _isInitializing.value = false
        }
    }
}
