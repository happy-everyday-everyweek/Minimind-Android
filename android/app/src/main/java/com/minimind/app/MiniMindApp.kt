package com.minimind.app

import android.app.Application
import com.minimind.app.backend.BackendManager
import com.minimind.app.data.ActivityRepository
import com.minimind.app.data.PreferencesManager
import com.minimind.app.network.ApiClient

class MiniMindApp : Application() {

    val backendManager by lazy { BackendManager(this) }
    val activityRepository by lazy { ActivityRepository() }
    val preferencesManager by lazy { PreferencesManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MiniMindApp
            private set
    }
}
