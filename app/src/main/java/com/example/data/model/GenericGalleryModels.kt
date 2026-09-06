package com.example.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data contract for generic search results endpoint:
 * { "results": [{ "id": "1", "title": "Sample", "coverUrl": "...", "tags": ["tag1"], "pageCount": 24, "source": "API" }] }
 */
data class GenericSearchResponse(
    @SerializedName("results") val results: List<GenericSearchResultItem> = emptyList()
)

data class GenericSearchResultItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("coverUrl") val coverUrl: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("pageCount") val pageCount: Int = 0,
    @SerializedName("source") val source: String = ""
)

/**
 * Data contract for generic gallery detail endpoint:
 * { "id": "1", "title": "Sample", "pages": ["https://.../1.jpg", "https://.../2.jpg"], "artist": "Artist Name", "tags": ["action", "drama"] }
 */
data class GenericGalleryDetailResponse(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("pages") val pages: List<String> = emptyList(),
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("tags") val tags: List<String> = emptyList()
)
