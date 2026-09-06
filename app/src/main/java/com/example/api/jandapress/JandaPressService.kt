package com.example.api.jandapress

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface JandaPressService {

    // Janda standard endpoints: /{provider}/get?id={id}
    @GET("{provider}/get")
    suspend fun getById(
        @Path("provider") provider: String,
        @Query("id") id: String,
        @Query("key") key: String? = null
    ): Response<ResponseBody>

    // Janda search endpoint: /{provider}/search?key={query}&page={page}
    @GET("{provider}/search")
    suspend fun search(
        @Path("provider") provider: String,
        @Query("key") key: String,
        @Query("page") page: Int = 1
    ): Response<ResponseBody>

    // Janda recent endpoint: /{provider}/recent?page={page}
    @GET("{provider}/recent")
    suspend fun getRecent(
        @Path("provider") provider: String,
        @Query("page") page: Int = 1
    ): Response<ResponseBody>

    // Janda popular endpoint: /{provider}/popular?page={page}
    @GET("{provider}/popular")
    suspend fun getPopular(
        @Path("provider") provider: String,
        @Query("page") page: Int = 1
    ): Response<ResponseBody>

    // Janda random endpoint: /{provider}/random
    @GET("{provider}/random")
    suspend fun getRandom(
        @Path("provider") provider: String
    ): Response<ResponseBody>

    // Generic direct URL fetcher (e.g. for custom proxies / mirrors)
    @GET
    suspend fun fetchDirectUrl(@Url url: String): Response<ResponseBody>
}
