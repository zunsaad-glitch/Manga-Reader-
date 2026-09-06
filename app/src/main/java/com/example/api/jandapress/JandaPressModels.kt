package com.example.api.jandapress

import com.google.gson.annotations.SerializedName

enum class JandaProvider(
    val code: String,
    val displayName: String,
    val iconEmoji: String,
    val badgeColorHex: Long,
    val defaultEndpoint: String
) {
    ALL("all", "All Sources", "🌐", 0xFF6366F1, "all"),
    PURURIN("pururin", "Pururin", "🟣", 0xFF9333EA, "pururin"),
    HENTAIFOX("hentaifox", "HentaiFox", "🦊", 0xFFEA580C, "hentaifox"),
    THREE_HENTAI("3hentai", "3Hentai", "🔴", 0xFFDC2626, "3hentai"),
    NHENTAI("nhentai", "nHentai", "🖤", 0xFF2563EB, "nhentai"),
    SIMPLY_HENTAI("simply-hentai", "Simply-Hentai", "🔵", 0xFF0284C7, "simply-hentai"),
    ASMHENTAI("asmhentai", "AsmHentai", "🟢", 0xFF059669, "asmhentai");

    companion object {
        fun fromCode(code: String): JandaProvider {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ALL
        }
    }
}

data class JandaGalleryItem(
    val id: String,
    val title: String,
    val japaneseTitle: String? = null,
    val coverUrl: String,
    val provider: String = "pururin",
    val pageCount: Int = 0,
    val artist: String? = null,
    val language: String = "english",
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val favorites: Int = 0,
    val rating: Double? = null,
    val parody: String? = null,
    val characters: List<String> = emptyList()
)

data class JandaGalleryDetail(
    val id: String,
    val title: String,
    val japaneseTitle: String? = null,
    val coverUrl: String,
    val pages: List<String> = emptyList(),
    val provider: String = "pururin",
    val artist: String? = null,
    val language: String = "english",
    val tags: List<String> = emptyList(),
    val pageCount: Int = 0,
    val parody: String? = null,
    val characters: List<String> = emptyList(),
    val category: String? = null,
    val uploadedAt: String? = null,
    val rating: Double? = null,
    val favorites: Int = 0
)

// Raw API DTOs for JandaPress responses
data class JandaApiResponse<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("results") val results: T? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("page") val page: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class JandaRawItem(
    @SerializedName("id") val id: Any? = null,
    @SerializedName("title") val title: Any? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("thumb") val thumb: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null,
    @SerializedName("image") val image: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("pages") val pages: Any? = null,
    @SerializedName("total_pages") val totalPages: Int? = null,
    @SerializedName("artist") val artist: Any? = null,
    @SerializedName("tags") val tags: Any? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("favorites") val favorites: Int? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("parody") val parody: Any? = null
)
