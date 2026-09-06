package com.example.api.nhapi

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NhApiService {

    // Direct nHentai API
    @GET("api/v2/search")
    suspend fun searchV2(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null
    ): Response<NhV2SearchResponse>

    @GET("api/v2/galleries/{id}")
    suspend fun getGalleryDetailV2(
        @Path("id") id: String
    ): Response<NhV2GalleryDetail>

    @GET("api/galleries/search")
    suspend fun searchDirect(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null
    ): Response<NhNativeSearchResponse>

    @GET("api/galleries/all")
    suspend fun getRecentDirect(
        @Query("page") page: Int = 1
    ): Response<NhNativeSearchResponse>

    @GET("api/gallery/{id}")
    suspend fun getGalleryDetailDirect(
        @Path("id") id: String
    ): Response<NhNativeGalleryItem>

    // Proxy / Mirror API
    @GET("search")
    suspend fun searchProxy(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null
    ): Response<NhProxySearchResponse>

    @GET("gallery/{id}")
    suspend fun getGalleryDetailProxy(
        @Path("id") id: String
    ): Response<NhGalleryDetail>

    @GET("gallery/{id}/pages")
    suspend fun getPagesProxy(
        @Path("id") id: String
    ): Response<List<String>>
}

interface CubariApiService {
    @GET("read/api/nhentai/series/{id}/")
    suspend fun getSeries(
        @Path("id") id: String
    ): Response<CubariSeriesResponse>
}
