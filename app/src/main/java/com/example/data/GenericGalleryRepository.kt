package com.example.data

import com.example.api.GenericGalleryApi
import com.example.data.model.GenericGalleryDetailResponse
import com.example.data.model.GenericSearchResultItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class GenericGalleryRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var currentBaseUrl: String = "https://api.example.com/"
    private var api: GenericGalleryApi = buildApi(currentBaseUrl)

    private fun buildApi(baseUrl: String): GenericGalleryApi {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "GenericGalleryClient/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GenericGalleryApi::class.java)
    }

    fun updateBaseUrl(newBaseUrl: String) {
        if (newBaseUrl.isNotBlank() && newBaseUrl != currentBaseUrl) {
            currentBaseUrl = newBaseUrl
            api = buildApi(newBaseUrl)
        }
    }

    suspend fun searchGalleries(query: String, page: Int = 1): Result<List<GenericSearchResultItem>> {
        return withContext(dispatcher) {
            try {
                val response = api.searchGalleries(query = query, page = page)
                if (response.isSuccessful) {
                    val results = response.body()?.results ?: emptyList()
                    Result.success(results)
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("HTTP $errorCode: $errorBody"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Network connection failed: ${e.localizedMessage ?: "No Internet"}"))
            } catch (e: Exception) {
                Result.failure(Exception("Failed to fetch search results: ${e.localizedMessage}"))
            }
        }
    }

    suspend fun getGalleryDetail(id: String): Result<GenericGalleryDetailResponse> {
        return withContext(dispatcher) {
            try {
                val response = api.getGalleryDetail(id = id)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty gallery response received from server."))
                    }
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("HTTP $errorCode: $errorBody"))
                }
            } catch (e: IOException) {
                Result.failure(Exception("Network connection failed: ${e.localizedMessage ?: "No Internet"}"))
            } catch (e: Exception) {
                Result.failure(Exception("Failed to fetch gallery details: ${e.localizedMessage}"))
            }
        }
    }
}
