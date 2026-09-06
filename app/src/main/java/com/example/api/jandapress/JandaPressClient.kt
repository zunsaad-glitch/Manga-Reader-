package com.example.api.jandapress

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object JandaPressClient {

    const val DEFAULT_PRIMARY_BASE_URL = "https://jandapress.xyz/"
    const val DEFAULT_FALLBACK_BASE_URL = "https://janda-api.vercel.app/"
    const val DEFAULT_SECONDARY_BASE_URL = "https://api.jandapress.com/"

    @Volatile
    private var customBaseUrl: String? = null

    @Volatile
    private var cachedService: JandaPressService? = null

    private val okHttpClient: OkHttpClient by lazy {
        com.example.api.NetworkClient.getClient()
    }

    fun getService(baseUrl: String? = null): JandaPressService {
        val effectiveUrl = baseUrl ?: customBaseUrl ?: DEFAULT_PRIMARY_BASE_URL
        val formattedUrl = if (effectiveUrl.endsWith("/")) effectiveUrl else "$effectiveUrl/"

        val current = cachedService
        if (current != null && customBaseUrl == baseUrl) {
            return current
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(JandaPressService::class.java)
        cachedService = service
        return service
    }

    fun setCustomBaseUrl(url: String?) {
        customBaseUrl = url
        cachedService = null
    }

    fun getCustomBaseUrl(): String? = customBaseUrl

    fun isConfigured(): Boolean = !customBaseUrl.isNullOrBlank()
}

