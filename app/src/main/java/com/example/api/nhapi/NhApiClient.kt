package com.example.api.nhapi

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NhApiClient {
    private const val DIRECT_BASE_URL = "https://nhentai.net/"
    private const val PROXY_BASE_URL = "https://nhapi.geaux.id/"

    private val okHttpClient: OkHttpClient by lazy {
        com.example.api.NetworkClient.getClient()
    }

    private val gson = GsonBuilder().create()

    val directApi: NhApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DIRECT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(NhApiService::class.java)
    }

    val proxyApi: NhApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PROXY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(NhApiService::class.java)
    }

    val cubariApi: CubariApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://cubari.moe/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CubariApiService::class.java)
    }
}
