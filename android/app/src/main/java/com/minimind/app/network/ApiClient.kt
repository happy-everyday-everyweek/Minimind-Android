package com.minimind.app.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "http://127.0.0.1:8000"

    private var baseUrl = DEFAULT_BASE_URL

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private var _retrofit: Retrofit? = null

    private val retrofit: Retrofit
        get() {
            if (_retrofit == null || _retrofit?.baseUrl().toString() != "$baseUrl/") {
                _retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            }
            return _retrofit!!
        }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun fetchOkHttpClient(): OkHttpClient = okHttpClient

    fun updateBaseUrl(url: String) {
        if (url != baseUrl) {
            baseUrl = url.trimEnd('/')
            _retrofit = null
        }
    }

    fun getBaseUrl(): String = baseUrl
}
