package com.example.api

import com.example.data.model.GenericGalleryDetailResponse
import com.example.data.model.GenericSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GenericGalleryApi {

    /**
     * Search galleries with query and optional page number.
     * Response schema: { "results": [{ "id", "title", "coverUrl", "tags", "pageCount", "source" }] }
     */
    @GET("api/v1/search")
    suspend fun searchGalleries(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<GenericSearchResponse>

    /**
     * Fetch gallery details including all page URLs.
     * Response schema: { "id", "title", "pages": ["url1", "url2"], "artist", "tags" }
     */
    @GET("api/v1/gallery/{id}")
    suspend fun getGalleryDetail(
        @Path("id") id: String
    ): Response<GenericGalleryDetailResponse>
}
