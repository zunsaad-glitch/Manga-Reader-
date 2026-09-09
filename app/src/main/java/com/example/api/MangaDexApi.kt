package com.example.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class MangaListResponse(
    @SerializedName("data") val data: List<MangaData> = emptyList()
)

data class SingleMangaResponse(
    @SerializedName("data") val data: MangaData? = null
)

data class MangaData(
    @SerializedName("id") val id: String = "",
    @SerializedName("attributes") val attributes: MangaAttributes? = null,
    @SerializedName("relationships") val relationships: List<Relationship>? = null
) {
    fun getAuthorName(): String? {
        val authorRel = relationships?.firstOrNull { it.type == "author" || it.type == "artist" }
        return authorRel?.attributes?.name
    }

    fun getAuthorId(): String? {
        val authorRel = relationships?.firstOrNull { it.type == "author" || it.type == "artist" }
        return authorRel?.id
    }

    fun getCoverImageUrl(): String? {
        val coverRel = relationships?.firstOrNull { it.type == "cover_art" }
        val fileName = coverRel?.attributes?.fileName
        return when {
            fileName == null -> null
            fileName.startsWith("http://") || fileName.startsWith("https://") -> fileName
            else -> "https://uploads.mangadex.org/covers/$id/$fileName.256.jpg"
        }
    }
}

data class Relationship(
    @SerializedName("id") val id: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("attributes") val attributes: RelationshipAttributes? = null
)

data class RelationshipAttributes(
    @SerializedName("fileName") val fileName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("biography") val biography: Map<String, String>? = null,
    @SerializedName("twitter") val twitter: String? = null,
    @SerializedName("pixiv") val pixiv: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("weibo") val weibo: String? = null
)

data class MangaAttributes(
    @SerializedName("title") val title: Map<String, String>? = emptyMap(),
    @SerializedName("altTitles") val altTitles: List<Map<String, String>>? = null,
    @SerializedName("description") val description: Map<String, String>? = null,
    @SerializedName("originalLanguage") val originalLanguage: String? = null,
    @SerializedName("contentRating") val contentRating: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("tags") val tags: List<TagData>? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("latestUploadedChapter") val latestUploadedChapter: String? = null,
    @SerializedName("lastChapter") val lastChapter: String? = null
)

data class ChapterListResponse(
    @SerializedName("data") val data: List<ChapterData> = emptyList(),
    @SerializedName("limit") val limit: Int = 0,
    @SerializedName("offset") val offset: Int = 0,
    @SerializedName("total") val total: Int = 0
)

data class ChapterData(
    @SerializedName("id") val id: String = "",
    @SerializedName("type") val type: String = "chapter",
    @SerializedName("attributes") val attributes: ChapterAttributes? = null
)

data class ChapterAttributes(
    @SerializedName("volume") val volume: String? = null,
    @SerializedName("chapter") val chapter: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("translatedLanguage") val translatedLanguage: String? = null,
    @SerializedName("externalUrl") val externalUrl: String? = null,
    @SerializedName("pages") val pages: Int? = null,
    @SerializedName("publishAt") val publishAt: String? = null,
    @SerializedName("readableAt") val readableAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

fun ChapterData.getFormattedReleaseDate(): String {
    val rawDate = attributes?.publishAt
        ?: attributes?.readableAt
        ?: attributes?.createdAt
        ?: attributes?.updatedAt
        ?: return ""
    return formatMangaDexDate(rawDate)
}

fun ChapterData.getShortReleaseDate(): String {
    val rawDate = attributes?.publishAt
        ?: attributes?.readableAt
        ?: attributes?.createdAt
        ?: return ""
    return try {
        val clean = rawDate.trim()
        if (clean.contains("T")) {
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val parsed = isoFormat.parse(clean.substringBefore("+").substringBefore("Z"))
            if (parsed != null) {
                java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(parsed)
            } else clean.substringBefore("T")
        } else {
            clean.substringBefore("T")
        }
    } catch (_: Exception) {
        rawDate.substringBefore("T")
    }
}

fun formatMangaDexDate(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return ""
    return try {
        val clean = rawDate.trim()
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val parsed = if (clean.contains("T")) {
            isoFormat.parse(clean.substringBefore("+").substringBefore("Z"))
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(clean)
        }
        if (parsed != null) {
            val now = System.currentTimeMillis()
            val diff = now - parsed.time
            val days = diff / (1000 * 60 * 60 * 24)
            val hours = diff / (1000 * 60 * 60)
            val minutes = diff / (1000 * 60)
            val displayFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
            val formattedDate = displayFormat.format(parsed)

            if (diff >= 0 && days < 7) {
                when {
                    minutes < 60 -> if (minutes <= 1) "Just now" else "$minutes min ago ($formattedDate)"
                    hours < 24 -> "$hours hr ago ($formattedDate)"
                    days == 1L -> "Yesterday ($formattedDate)"
                    days in 2..6 -> "$days days ago ($formattedDate)"
                    else -> formattedDate
                }
            } else {
                formattedDate
            }
        } else {
            clean.substringBefore("T")
        }
    } catch (_: Exception) {
        rawDate.substringBefore("T")
    }
}

data class AtHomeServerResponse(
    @SerializedName("baseUrl") val baseUrl: String = "",
    @SerializedName("chapter") val chapter: ChapterNode? = null
)

data class ChapterNode(
    @SerializedName("hash") val hash: String = "",
    @SerializedName("data") val data: List<String> = emptyList(),
    @SerializedName("dataSaver") val dataSaver: List<String>? = null
)

data class SingleAuthorResponse(
    @SerializedName("data") val data: AuthorData? = null
)

data class AuthorListResponse(
    @SerializedName("data") val data: List<AuthorData> = emptyList()
)

data class AuthorData(
    @SerializedName("id") val id: String = "",
    @SerializedName("attributes") val attributes: AuthorAttributes? = null
)

data class AuthorAttributes(
    @SerializedName("name") val name: String = "",
    @SerializedName("biography") val biography: Map<String, String>? = null,
    @SerializedName("twitter") val twitter: String? = null,
    @SerializedName("pixiv") val pixiv: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("weibo") val weibo: String? = null,
    @SerializedName("youtube") val youtube: String? = null
)

data class TagListResponse(
    @SerializedName("data") val data: List<TagData> = emptyList()
)

data class TagData(
    @SerializedName("id") val id: String = "",
    @SerializedName("attributes") val attributes: TagAttributes? = null
)

data class TagAttributes(
    @SerializedName("name") val name: Map<String, String>? = emptyMap()
)

interface MangaDexApi {
    @GET("manga")
    suspend fun getMangaList(
        @Query("ids[]") ids: List<String>? = null,
        @Query("title") title: String? = null,
        @Query("authors[]") authors: List<String>? = null,
        @Query("originalLanguage[]") originalLanguages: List<String>? = null,
        @Query("availableTranslatedLanguage[]") availableTranslatedLanguage: List<String>? = null,
        @Query("hasAvailableChapters") hasAvailableChapters: Boolean? = true,
        @Query("contentRating[]") contentRatings: List<String>,
        @Query("includedTags[]") includedTags: List<String>? = null,
        @Query("includedTagsMode") includedTagsMode: String = "AND",
        @Query("status[]") status: List<String>? = null,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("order[followedCount]") orderFollowedCount: String? = null,
        @Query("order[rating]") orderRating: String? = null,
        @Query("order[latestUploadedChapter]") orderLatestUploadedChapter: String? = null,
        @Query("order[createdAt]") orderCreatedAt: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): MangaListResponse

    @GET("manga/{id}")
    suspend fun getMangaById(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist")
    ): SingleMangaResponse

    @GET("author")
    suspend fun searchAuthors(
        @Query("name") name: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("order[name]") orderName: String? = null
    ): AuthorListResponse

    @GET("author/{id}")
    suspend fun getAuthorById(
        @Path("id") id: String
    ): SingleAuthorResponse

    @GET("manga/tag")
    suspend fun getTags(): TagListResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaChapters(
        @Path("id") mangaId: String,
        @Query("translatedLanguage[]") translatedLanguage: List<String>? = null,
        @Query("contentRating[]") contentRatings: List<String> = listOf("safe", "suggestive", "erotica", "pornographic"),
        @Query("order[chapter]") order: String = "asc",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("includeExternalUrl") includeExternalUrl: Int = 0,
        @Query("includeFutureUpdates") includeFutureUpdates: Int = 0,
        @Query("includeEmptyPages") includeEmptyPages: Int = 0
    ): ChapterListResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getChapterServer(
        @Path("chapterId") chapterId: String
    ): AtHomeServerResponse
}
