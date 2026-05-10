package com.minimind.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "minimind_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_API_BASE = stringPreferencesKey("api_base")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_API_MODEL = stringPreferencesKey("api_model")
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: "http://127.0.0.1:8000"
    }

    val apiBase: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_BASE] ?: ""
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_KEY] ?: ""
    }

    val apiModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_API_MODEL] ?: ""
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = url
        }
    }

    suspend fun saveApiBase(base: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_BASE] = base
        }
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_KEY] = key
        }
    }

    suspend fun saveApiModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_API_MODEL] = model
        }
    }
}
