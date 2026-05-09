package com.minimind.app.backend

import android.content.Context
import android.content.Intent
import com.minimind.app.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class BackendManager(private val context: Context) {

    enum class BackendStatus {
        UNKNOWN, CHECKING, ONLINE, OFFLINE, ERROR
    }

    private val _status = MutableStateFlow(BackendStatus.UNKNOWN)
    val status: StateFlow<BackendStatus> = _status.asStateFlow()

    private val prefs = context.getSharedPreferences("backend_manager", Context.MODE_PRIVATE)

    suspend fun checkStatus() {
        _status.value = BackendStatus.CHECKING
        try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.checkHealth()
            }
            _status.value = if (response["status"] == "ok") {
                BackendStatus.ONLINE
            } else {
                BackendStatus.OFFLINE
            }
        } catch (e: Exception) {
            _status.value = BackendStatus.OFFLINE
        }
    }

    suspend fun initialize(): Result<String> {
        return try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.initialize()
            }
            _status.value = BackendStatus.ONLINE
            Result.success(response["message"] ?: "初始化成功")
        } catch (e: Exception) {
            _status.value = BackendStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun restart(): Result<String> {
        return try {
            val response = withContext(Dispatchers.IO) {
                ApiClient.apiService.restart()
            }
            _status.value = BackendStatus.ONLINE
            Result.success(response["message"] ?: "重启成功")
        } catch (e: Exception) {
            _status.value = BackendStatus.ERROR
            Result.failure(e)
        }
    }

    fun setOffline() {
        _status.value = BackendStatus.OFFLINE
    }

    fun startLinuxInstance(): Result<String> {
        return try {
            val packageManager = context.packageManager
            val termuxIntent = packageManager.getLaunchIntentForPackage("com.termux")
            if (termuxIntent == null) {
                return Result.failure(Exception("Termux 未安装，请先安装 Termux 应用"))
            }
            termuxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(termuxIntent)
            runTermuxCommand("cd ~/minimind && bash init.sh")
            Result.success("Linux 实例启动中")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isFirstLaunch(): Boolean {
        val isFirst = prefs.getBoolean("first_launch", true)
        if (isFirst) {
            prefs.edit().putBoolean("first_launch", false).apply()
        }
        return isFirst
    }

    suspend fun ensureBackendRunning(): Result<String> {
        checkStatus()
        if (_status.value == BackendStatus.ONLINE) {
            return Result.success("后端服务已运行")
        }
        if (isFirstLaunch()) {
            startLinuxInstance()
        } else {
            runTermuxCommand("cd ~/minimind && python serve_openai_api.py &")
        }
        repeat(30) {
            delay(2000)
            checkStatus()
            if (_status.value == BackendStatus.ONLINE) {
                return Result.success("后端服务已就绪")
            }
        }
        return Result.failure(Exception("后端服务启动超时"))
    }

    fun runTermuxCommand(command: String) {
        val intent = Intent("com.termux.execute_command")
        intent.putExtra("com.termux.execute_command", command)
        intent.setPackage("com.termux")
        context.sendBroadcast(intent)
    }
}
