package com.example.api.nhapi

import com.google.gson.annotations.SerializedName

// ================= NATIVE NHENTAI API MODELS =================

data class NhNativeSearchResponse(
    @SerializedName("result") val result: List<NhNativeGalleryItem>? = null,
    @SerializedName("num_pages") val numPages: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null
)

data class NhNativeGalleryItem(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("title") val title: NhNativeTitle? = null,
    @SerializedName("images") val images: NhNativeImages? = null,
    @SerializedName("tags") val tags: List<NhNativeTag>? = null,
    @SerializedName("num_pages") val numPages: Int? = null,
    @SerializedName("num_favorites") val numFavorites: Int? = null
)

data class NhNativeTitle(
    @SerializedName("english") val english: String? = null,
    @SerializedName("japanese") val japanese: String? = null,
    @SerializedName("pretty") val pretty: String? = null
)

data class NhNativeImages(
    @SerializedName("pages") val pages: List<NhNativeImageInfo>? = null,
    @SerializedName("cover") val cover: NhNativeImageInfo? = null,
    @SerializedName("thumbnail") val thumbnail: NhNativeImageInfo? = null
)

data class NhNativeImageInfo(
    @SerializedName("t") val t: String? = null, // "j" = jpg, "p" = png, "w" = webp, "g" = gif
    @SerializedName("w") val w: Int? = null,
    @SerializedName("h") val h: Int? = null
)

data class NhNativeTag(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("type") val type: String? = null, // "tag", "artist", "category", "language", "character", "parody", "group"
    @SerializedName("name") val name: String? = null,
    @SerializedName("count") val count: Int? = null
)

// ================= PROXY / GENERIC RESPONSE MODELS =================

data class NhProxySearchResponse(
    @SerializedName("results") val results: List<NhProxyGalleryItem>? = null,
    @SerializedName("result") val result: List<NhProxyGalleryItem>? = null,
    @SerializedName("data") val data: List<NhProxyGalleryItem>? = null
)

data class NhProxyGalleryItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("pageCount") val pageCount: Int? = null,
    @SerializedName("num_pages") val numPages: Int? = null
)

// ================= CUBARI RESPONSE MODELS =================

data class CubariSeriesResponse(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("chapters") val chapters: Map<String, CubariChapterItem>? = null
)

data class CubariChapterItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("volume") val volume: String? = null,
    @SerializedName("groups") val groups: Map<String, List<String>>? = null
)

// ================= UNIFIED APP MODELS =================

data class NhGallery(
    val id: String,
    val title: String,
    val coverUrl: String,
    val tags: List<String> = emptyList(),
    val artist: String? = null,
    val language: String? = null,
    val pageCount: Int = 0,
    val favorites: Int = 0
)

data class NhGalleryDetail(
    val id: String,
    val title: String,
    val japaneseTitle: String? = null,
    val coverUrl: String = "",
    val pages: List<String> = emptyList(),
    val artist: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val parody: String? = null,
    val characters: List<String> = emptyList(),
    val pageCount: Int = 0,
    val favorites: Int = 0
)

// ================= OFFICIAL NHENTAI API V2 MODELS =================

data class NhV2SearchResponse(
    @SerializedName("result") val result: List<NhV2GalleryItem>? = null,
    @SerializedName("num_pages") val numPages: Int? = null,
    @SerializedName("per_page") val perPage: Int? = null
)

data class NhV2GalleryItem(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("english_title") val englishTitle: String? = null,
    @SerializedName("japanese_title") val japaneseTitle: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("thumbnail_width") val thumbnailWidth: Int? = null,
    @SerializedName("thumbnail_height") val thumbnailHeight: Int? = null,
    @SerializedName("num_pages") val numPages: Int? = null,
    @SerializedName("num_favorites") val numFavorites: Int? = null
)

data class NhV2GalleryDetail(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("media_id") val mediaId: String? = null,
    @SerializedName("title") val title: NhNativeTitle? = null,
    @SerializedName("cover") val cover: NhV2ImageObject? = null,
    @SerializedName("thumbnail") val thumbnail: NhV2ImageObject? = null,
    @SerializedName("pages") val pages: List<NhV2PageObject>? = null,
    @SerializedName("num_pages") val numPages: Int? = null,
    @SerializedName("num_favorites") val numFavorites: Int? = null,
    @SerializedName("tags") val tags: List<NhNativeTag>? = null
)

data class NhV2ImageObject(
    @SerializedName("path") val path: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)

data class NhV2PageObject(
    @SerializedName("number") val number: Int? = null,
    @SerializedName("path") val path: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null
)

