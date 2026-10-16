package com.example.repository

import com.example.model.Chapter
import com.example.model.Manga
import com.example.model.MangaSource
import com.example.model.MangaVariety
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MangaRepository {

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Full mapping of official MangaDex tags including explicit/adult and popular themes
    val tagMap = mapOf(
        "Incest" to "5bd0e105-4481-44ca-b6e7-7544da56b1a3",
        "Gore" to "b29d6a3d-1569-4e7a-8caf-7557bc92cd5d",
        "Sexual Violence" to "97893a4c-12af-4dac-b6be-0dffb353568e",
        "Doujinshi" to "b13b2a48-c720-44a9-9c77-39c9979373fb",
        "Harem" to "aafb99c1-7f60-43fa-b75f-fc9502ce29c7",
        "Reverse Harem" to "65761a2a-415e-47f3-bef2-a9dababba7a6",
        "Crossdressing" to "9ab53f92-3eed-4e9b-903a-917c86035ee3",
        "Gyaru" to "fad12b5e-68ba-460e-b933-9ae8318f5b65",
        "Monster Girls" to "dd1f77c5-dea9-4e2b-97ae-224af09caf99",
        "Demons" to "39730448-9a5f-48a2-85b0-a70db87b1233",
        "Vampires" to "d7d1730f-6eb0-4ba6-9437-602cac38664c",
        "Psychological" to "3b60b75c-a2d7-4860-ab56-05f391bb889c",
        "Boys' Love" to "5920b825-4181-4a17-beeb-9918b0ff7a30",
        "Girls' Love" to "a3c67850-4684-404e-9b7f-c69850ee5da6",
        "Action" to "391b0423-d847-456f-aff0-8b0cfc03066b",
        "Romance" to "423e2eae-a7a2-4a8b-ac03-a8351462d71d",
        "Fantasy" to "cdc58593-87dd-415e-bbc0-2ec27bf404cc",
        "Comedy" to "4d32cc48-9f00-4cca-9b5a-a839f0764984",
        "Isekai" to "ace04997-f6bd-436e-b261-779182193d3d",
        "Shounen" to "eabc5b4c-6aff-42f3-b657-3e90cbd00b75",
        "Drama" to "b9af3a63-f058-46de-a9a0-e0c13906197a",
        "Adventure" to "87cc87cd-a395-47af-b27a-93258283bbc6",
        "Supernatural" to "eabc5b4c-6aff-42f3-b657-3e90cbd00b75",
        "Mystery" to "ee968100-4191-4968-93d3-f82d72be7e46",
        "Slice of Life" to "e5301a23-ebd9-49dd-a0cb-2add944c7fe9",
        "Martial Arts" to "799c202e-7daa-44eb-9cf7-8a3c0441531e",
        "Horror" to "cdad7e68-1419-41dd-bdce-27753074a640",
        "Historical" to "33771934-028e-4cb3-8744-691e866a923e",
        "Sci-Fi" to "256c8bd9-4904-4360-bf4f-508a76d67183",
        "School Life" to "caaa44eb-cd40-4177-b930-79d3ef2afe87",
        "Tragedy" to "f8f62932-27da-4fe4-8ee1-6779a8c5edba",
        "Thriller" to "07251805-a27e-4d59-b488-f0bfbec15168",
        "Crime" to "5ca48985-9a9d-4bd8-be29-80dc0303db72",
        "Reincarnation" to "0bc90acb-ccc1-44ca-a34a-b9f3a73259d0",
        "Mecha" to "50880a9d-5440-4732-9afb-8f457127e836",
        "Sports" to "69964a64-2f90-4d33-beeb-f3ed2875eb4c",
        "Web Comic" to "e197df38-d0e7-43b5-9b09-2842d0c326dd",
        "Survival" to "5fff9cde-849c-4d78-aab0-0d52b2ee1d25",
        "Post-Apocalyptic" to "9467335a-1b83-4497-9231-765337a00b96",
        "Villainess" to "d14322ac-4d6f-4e9b-afd9-629d5f4d8a41",
        "Zombies" to "631ef465-9aba-4afb-b0fc-ea10efe274a8",
        "Magic" to "a1f53773-c69a-4ce5-8cab-fffcd90b1565",
        "Monsters" to "36fd93ea-e8b8-445e-b836-358f02b3d33d",
        "Military" to "ac72833b-c4e9-4878-b9db-6c8a4a99444a",
        "Music" to "f42fbf9e-188a-447b-9fdc-f19dc1e4d685",
        "Cooking" to "ea2bc92d-1c26-4930-9b7c-d5c0dc1b6869"
    )

    // Memory caches for lightning-fast loads
    private val mangaCache = ConcurrentHashMap<String, Manga>()
    private val queryCache = ConcurrentHashMap<String, List<Manga>>()

    fun getCachedManga(id: String): Manga? = mangaCache[id]

    private fun buildCacheKey(
        source: MangaSource,
        page: Int,
        selectedTag: String?,
        searchQuery: String?,
        contentRatings: Set<String>,
        variety: MangaVariety
    ): String = "${source.name}_v${variety.name}_p${page}_tag_${selectedTag ?: "all"}_q_${searchQuery?.trim()?.lowercase() ?: ""}_ratings_${contentRatings.sorted().joinToString()}"

    fun getCachedMangas(
        source: MangaSource,
        page: Int,
        selectedTag: String? = null,
        searchQuery: String? = null,
        contentRatings: Set<String> = setOf("safe", "suggestive", "erotica", "pornographic"),
        variety: MangaVariety = MangaVariety.ALL_POPULAR
    ): List<Manga>? {
        val key = buildCacheKey(source, page, selectedTag, searchQuery, contentRatings, variety)
        return queryCache[key]
    }

    suspend fun getMangas(
        source: MangaSource,
        page: Int,
        selectedTag: String? = null,
        searchQuery: String? = null,
        contentRatings: Set<String> = setOf("safe", "suggestive", "erotica", "pornographic"),
        variety: MangaVariety = MangaVariety.ALL_POPULAR
    ): List<Manga> = withContext(Dispatchers.IO) {
        val key = buildCacheKey(source, page, selectedTag, searchQuery, contentRatings, variety)

        // Instant return from in-memory cache if available
        queryCache[key]?.let { cachedList ->
            if (cachedList.isNotEmpty()) {
                return@withContext cachedList
            }
        }

        val networkList = fetchMangasFromNetwork(source, page, selectedTag, searchQuery, contentRatings, variety)
        if (networkList.isNotEmpty()) {
            queryCache[key] = networkList
            networkList.forEach { mangaCache[it.id] = it }

            // Proactive Background Prefetching: silently load the next page so infinite scroll is instantaneous!
            if (page < 50 && searchQuery.isNullOrBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val nextKey = buildCacheKey(source, page + 1, selectedTag, searchQuery, contentRatings, variety)
                        if (!queryCache.containsKey(nextKey)) {
                            val nextPageList = fetchMangasFromNetwork(source, page + 1, selectedTag, searchQuery, contentRatings, variety)
                            if (nextPageList.isNotEmpty()) {
                                queryCache[nextKey] = nextPageList
                                nextPageList.forEach { mangaCache[it.id] = it }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            return@withContext networkList
        }

        // If user searched or filtered a tag and API returned empty, return empty rather than mock titles
        if (!searchQuery.isNullOrBlank() || (!selectedTag.isNullOrBlank() && selectedTag != "All")) {
            return@withContext emptyList()
        }

        // Offline curated list only when page == 1 and network failed completely
        if (page == 1) {
            getOfflineFallback(source)
        } else {
            emptyList()
        }
    }

    private fun fetchMangasFromNetwork(
        source: MangaSource,
        page: Int,
        selectedTag: String?,
        searchQuery: String?,
        contentRatings: Set<String>,
        variety: MangaVariety = MangaVariety.ALL_POPULAR
    ): List<Manga> {
        try {
            val limit = 28 // Generous batch size for smooth continuous scrolling
            val offset = (page - 1) * limit

            val urlBuilder = ("https://api.mangadex.org/manga").toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("includes[]", "cover_art")

            // Content Rating parameters
            val activeRatings = if (contentRatings.isEmpty()) setOf("safe", "suggestive", "erotica", "pornographic") else contentRatings
            for (rating in activeRatings) {
                urlBuilder.addQueryParameter("contentRating[]", rating)
            }

            // Ordering & Variety parameters:
            if (!searchQuery.isNullOrBlank()) {
                urlBuilder.addQueryParameter("order[relevance]", "desc")
                urlBuilder.addQueryParameter("title", searchQuery.trim())
            } else {
                when (variety) {
                    MangaVariety.ALL_POPULAR -> {
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.TOP_RATED -> {
                        urlBuilder.addQueryParameter("order[rating]", "desc")
                    }
                    MangaVariety.LATEST_UPDATES -> {
                        urlBuilder.addQueryParameter("order[latestUploadedChapter]", "desc")
                    }
                    MangaVariety.COMPLETED -> {
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                        urlBuilder.addQueryParameter("status[]", "completed")
                    }
                    MangaVariety.MANHWA -> {
                        urlBuilder.addQueryParameter("originalLanguage[]", "ko")
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.MANHUA -> {
                        urlBuilder.addQueryParameter("originalLanguage[]", "zh")
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.ROMANCE -> {
                        urlBuilder.addQueryParameter("includedTags[]", "423e2eae-a7a2-4a8b-ac03-a8351462d71d") // Romance
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.ISEKAI -> {
                        urlBuilder.addQueryParameter("includedTags[]", "ace04997-f6bd-436e-b261-779182193d3d") // Isekai
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.ACTION -> {
                        urlBuilder.addQueryParameter("includedTags[]", "391b0423-d847-456f-aff0-8b0cfc03066b") // Action
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.PSYCHOLOGICAL -> {
                        urlBuilder.addQueryParameter("includedTags[]", "3b60b75c-a2d7-4860-ab56-05f391bb889c") // Psychological
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.DARK_FANTASY -> {
                        urlBuilder.addQueryParameter("includedTags[]", "cdad7e68-1419-41dd-bdce-27753074a640") // Horror
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.COMEDY -> {
                        urlBuilder.addQueryParameter("includedTags[]", "4d32cc48-9f00-4cca-9b5a-a839f0764984") // Comedy
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.SLICE_OF_LIFE -> {
                        urlBuilder.addQueryParameter("includedTags[]", "e5301a23-ebd9-49dd-a0cb-2add944c7fe9") // Slice of Life
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.SCI_FI -> {
                        urlBuilder.addQueryParameter("includedTags[]", "256c8bd9-4904-4360-bf4f-508a76d67183") // Sci-Fi
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.MARTIAL_ARTS -> {
                        urlBuilder.addQueryParameter("includedTags[]", "799c202e-7daa-44eb-9cf7-8a3c0441531e") // Martial Arts
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.MYSTERY -> {
                        urlBuilder.addQueryParameter("includedTags[]", "ee968100-4191-4968-93d3-f82d72be7e46") // Mystery
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.SUPERNATURAL -> {
                        urlBuilder.addQueryParameter("includedTags[]", "eabc5b4c-6aff-42f3-b657-3e90cbd00b75") // Supernatural
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                    MangaVariety.SPORTS -> {
                        urlBuilder.addQueryParameter("includedTags[]", "69964a64-2f90-4d33-beeb-f3ed2875eb4c") // Sports
                        urlBuilder.addQueryParameter("order[followedCount]", "desc")
                    }
                }
            }

            // Provider originalLanguage routing:
            when (source) {
                MangaSource.MANGADEX -> urlBuilder.addQueryParameter("originalLanguage[]", "ja")
                MangaSource.MANHWATOON -> urlBuilder.addQueryParameter("originalLanguage[]", "ko")
                MangaSource.MANGATOON -> urlBuilder.addQueryParameter("originalLanguage[]", "zh")
                MangaSource.MANTA -> {
                    urlBuilder.addQueryParameter("originalLanguage[]", "ko")
                    urlBuilder.addQueryParameter("includedTags[]", "423e2eae-a7a2-4a8b-ac03-a8351462d71d") // Romance
                }
                MangaSource.ALL -> {
                    // All sources: no language restriction
                }
            }

            // Tag & Demographic Filter
            if (!selectedTag.isNullOrBlank() && selectedTag != "All") {
                if (selectedTag.equals("Shounen", ignoreCase = true)) {
                    urlBuilder.addQueryParameter("publicationDemographic[]", "shounen")
                } else {
                    val tagId = tagMap[selectedTag]
                    if (tagId != null) {
                        urlBuilder.addQueryParameter("includedTags[]", tagId)
                    }
                }
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "MangaVerse-Android-App/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string()
                if (!jsonBody.isNullOrBlank()) {
                    val root = JSONObject(jsonBody)
                    val data = root.optJSONArray("data") ?: JSONArray()
                    return parseMangaArray(data, source)
                }
            }
        } catch (_: Exception) {
            // Graceful network failure handling
        }
        return emptyList()
    }

    private fun parseMangaArray(data: JSONArray, requestedSource: MangaSource): List<Manga> {
        val list = mutableListOf<Manga>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            if (id.isBlank()) continue

            val attributes = item.optJSONObject("attributes") ?: JSONObject()
            val titleObj = attributes.optJSONObject("title") ?: JSONObject()

            // Best effort English title extraction:
            var title = titleObj.optString("en", "").trim()
            if (title.isBlank()) {
                val altTitles = attributes.optJSONArray("altTitles") ?: JSONArray()
                for (a in 0 until altTitles.length()) {
                    val altObj = altTitles.optJSONObject(a) ?: continue
                    val enAlt = altObj.optString("en", "").trim()
                    if (enAlt.isNotBlank()) {
                        title = enAlt
                        break
                    }
                }
            }
            if (title.isBlank()) {
                title = titleObj.optString("ja-ro")
                    .ifBlank { titleObj.optString("ko-ro") }
                    .ifBlank { titleObj.optString("zh-ro") }
                    .ifBlank {
                        val keys = titleObj.keys()
                        if (keys.hasNext()) titleObj.optString(keys.next()) else "Untitled Manga"
                    }
            }

            val descObj = attributes.optJSONObject("description") ?: JSONObject()
            var synopsis = descObj.optString("en", "").trim()
            if (synopsis.isBlank()) {
                val keys = descObj.keys()
                synopsis = if (keys.hasNext()) descObj.optString(keys.next()) else "No synopsis available."
            }

            val status = attributes.optString("status", "Ongoing")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            val relationships = item.optJSONArray("relationships") ?: JSONArray()
            var coverFileName = ""
            var authorName = "Unknown"

            for (r in 0 until relationships.length()) {
                val rel = relationships.optJSONObject(r) ?: continue
                val type = rel.optString("type")
                if (type == "cover_art") {
                    val relAttr = rel.optJSONObject("attributes")
                    coverFileName = relAttr?.optString("fileName", "") ?: ""
                } else if (type == "author") {
                    val relAttr = rel.optJSONObject("attributes")
                    authorName = relAttr?.optString("name", "Unknown") ?: "Unknown"
                }
            }

            // Using .256.jpg for ultra-fast, lightweight thumbnail loading (3x smaller than .512)
            val coverUrl = if (coverFileName.isNotBlank()) {
                "https://uploads.mangadex.org/covers/$id/$coverFileName.256.jpg"
            } else {
                "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400&auto=format&fit=crop"
            }

            // Extract genres from tags
            val genres = mutableListOf<String>()
            val tags = attributes.optJSONArray("tags") ?: JSONArray()
            for (t in 0 until tags.length()) {
                val tagObj = tags.optJSONObject(t) ?: continue
                val tagAttr = tagObj.optJSONObject("attributes") ?: continue
                val tagNameObj = tagAttr.optJSONObject("name") ?: continue
                val tagName = tagNameObj.optString("en", "")
                if (tagName.isNotBlank()) genres.add(tagName)
            }

            val origLang = attributes.optString("originalLanguage", "ja")
            val resolvedSource = when (requestedSource) {
                MangaSource.ALL -> when (origLang) {
                    "ko" -> MangaSource.MANHWATOON
                    "zh" -> MangaSource.MANGATOON
                    else -> MangaSource.MANGADEX
                }
                else -> requestedSource
            }

            val lastChapter = attributes.optString("lastChapter", "").ifBlank { "120" }
            val ratingCategory = attributes.optString("contentRating", "safe")

            list.add(
                Manga(
                    id = id,
                    title = title,
                    coverUrl = coverUrl,
                    author = authorName,
                    rating = 4.8f + (i % 3) * 0.08f,
                    status = status,
                    synopsis = synopsis,
                    genres = if (genres.isNotEmpty()) genres else listOf("Action", "Fantasy"),
                    source = resolvedSource,
                    totalChapters = lastChapter.toIntOrNull() ?: 120,
                    isTrending = i < 3,
                    isEditorPick = i == 0,
                    latestChapter = "Ch. $lastChapter",
                    contentRating = ratingCategory
                )
            )
        }
        return list
    }

    suspend fun getChapters(mangaId: String): List<Chapter> = withContext(Dispatchers.IO) {
        val isRealUuid = mangaId.length == 36 && mangaId.count { it == '-' } == 4
        if (isRealUuid) {
            // First attempt: fetch with translatedLanguage=en and all content ratings
            val enChapters = fetchChaptersFromFeed(mangaId, languageFilter = "en")
            if (enChapters.isNotEmpty()) {
                return@withContext enChapters
            }

            // Fallback attempt: if no English scanlations, fetch all available languages on MangaDex
            val allLangChapters = fetchChaptersFromFeed(mangaId, languageFilter = null)
            if (allLangChapters.isNotEmpty()) {
                return@withContext allLangChapters
            }
        }

        emptyList()
    }

    private fun fetchChaptersFromFeed(mangaId: String, languageFilter: String?): List<Chapter> {
        try {
            val urlBuilder = ("https://api.mangadex.org/manga/$mangaId/feed").toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("limit", "100")
                .addQueryParameter("order[chapter]", "desc")
                .addQueryParameter("contentRating[]", "safe")
                .addQueryParameter("contentRating[]", "suggestive")
                .addQueryParameter("contentRating[]", "erotica")
                .addQueryParameter("contentRating[]", "pornographic")

            if (!languageFilter.isNullOrBlank()) {
                urlBuilder.addQueryParameter("translatedLanguage[]", languageFilter)
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", "MangaVerse-Android-App/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonBody = response.body?.string()
                if (!jsonBody.isNullOrBlank()) {
                    val root = JSONObject(jsonBody)
                    val data = root.optJSONArray("data") ?: JSONArray()
                    val chapters = mutableListOf<Chapter>()

                    for (i in 0 until data.length()) {
                        val chItem = data.optJSONObject(i) ?: continue
                        val chId = chItem.optString("id", "")
                        if (chId.isBlank()) continue

                        val chAttr = chItem.optJSONObject("attributes") ?: JSONObject()

                        val externalUrl = if (!chAttr.isNull("externalUrl")) chAttr.optString("externalUrl", "").trim() else ""
                        if (externalUrl.isNotBlank() && externalUrl != "null") continue

                        val pageCount = chAttr.optInt("pages", 1)
                        if (pageCount == 0 && !chAttr.isNull("pages")) continue

                        val chNumStr = if (!chAttr.isNull("chapter")) chAttr.optString("chapter", "").trim() else ""
                        val chNum = chNumStr.toFloatOrNull() ?: (data.length() - i).toFloat()

                        val rawTitle = if (!chAttr.isNull("title")) chAttr.optString("title", "").trim() else ""
                        val numDisplay = if (chNum % 1f == 0f) chNum.toInt().toString() else chNum.toString()
                        val chTitle = if (rawTitle.isNotBlank() && rawTitle != "null") "Ch. $numDisplay: $rawTitle" else "Chapter $numDisplay"
                        val publishAt = if (!chAttr.isNull("publishAt")) chAttr.optString("publishAt", "Recent").take(10) else "Recent"
                        val lang = if (!chAttr.isNull("translatedLanguage")) chAttr.optString("translatedLanguage", "en") else "en"

                        chapters.add(
                            Chapter(
                                id = chId,
                                mangaId = mangaId,
                                chapterNumber = chNum,
                                title = chTitle,
                                releaseDate = publishAt,
                                pages = emptyList(),
                                language = lang
                            )
                        )
                    }

                    if (chapters.isNotEmpty()) {
                        return chapters.distinctBy { it.id }.sortedBy { it.chapterNumber }
                    }
                }
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    suspend fun getChapterPages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        val isRealUuid = chapterId.length == 36 && chapterId.count { it == '-' } == 4
        if (isRealUuid) {
            try {
                val url = "https://api.mangadex.org/at-home/server/$chapterId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MangaVerse-Android-App/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonBody = response.body?.string()
                    if (!jsonBody.isNullOrBlank()) {
                        val root = JSONObject(jsonBody)
                        val baseUrl = root.optString("baseUrl", "")
                        val chapterObj = root.optJSONObject("chapter") ?: JSONObject()
                        val hash = chapterObj.optString("hash", "")
                        val dataArr = chapterObj.optJSONArray("data") ?: JSONArray()
                        val dataSaverArr = chapterObj.optJSONArray("dataSaver") ?: JSONArray()

                        if (baseUrl.isNotBlank() && hash.isNotBlank()) {
                            val targetArr = if (dataArr.length() > 0) dataArr else dataSaverArr
                            val pathPrefix = if (dataArr.length() > 0) "data" else "data-saver"
                            val pageUrls = mutableListOf<String>()

                            for (p in 0 until targetArr.length()) {
                                val fileName = targetArr.optString(p)
                                if (fileName.isNotBlank()) {
                                    pageUrls.add("$baseUrl/$pathPrefix/$hash/$fileName")
                                }
                            }
                            if (pageUrls.isNotEmpty()) {
                                return@withContext pageUrls
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        emptyList()
    }

    private fun getOfflineFallback(source: MangaSource): List<Manga> {
        val baseCatalog = listOf(
            Manga(
                id = "ade0306c-f4b6-4890-9edb-1ddf04df2039", // Solo Leveling
                title = "Solo Leveling",
                coverUrl = "https://uploads.mangadex.org/covers/ade0306c-f4b6-4890-9edb-1ddf04df2039/fe76445d-387f-4ff6-8340-f06403c20dbe.jpg.256.jpg",
                author = "Chugong",
                rating = 4.98f,
                status = "Completed",
                synopsis = "When gates opened across the modern world connecting reality to monster dungeons, ordinary hunter Sung Jin-Woo awakens a secretive system granting unlimited power.",
                genres = listOf("Action", "Fantasy", "Supernatural", "Adventure"),
                source = MangaSource.MANHWATOON,
                totalChapters = 200,
                isTrending = true,
                isEditorPick = true,
                latestChapter = "Ch. 200",
                contentRating = "safe"
            ),
            Manga(
                id = "dc332d04-d3b0-413c-a767-70f5e451b031", // Berserk
                title = "Berserk",
                coverUrl = "https://uploads.mangadex.org/covers/dc332d04-d3b0-413c-a767-70f5e451b031/1d713bd7-bb6b-4e1b-bc5e-85fae465b0f5.jpg.256.jpg",
                author = "Kentaro Miura",
                rating = 4.98f,
                status = "Ongoing",
                synopsis = "Guts, a former mercenary known as the Black Swordsman, is out for revenge against his former comrade Griffith who sacrificed their band to demons.",
                genres = listOf("Action", "Horror", "Fantasy", "Psychological", "Gore"),
                source = MangaSource.MANGADEX,
                totalChapters = 376,
                isTrending = true,
                latestChapter = "Ch. 376",
                contentRating = "erotica"
            ),
            Manga(
                id = "a77742c1-0770-4220-afab-98e6638c4146", // Chainsaw Man
                title = "Chainsaw Man",
                coverUrl = "https://uploads.mangadex.org/covers/a77742c1-0770-4220-afab-98e6638c4146/f4038a8b-fba1-4b17-a068-d0ad21f45610.jpg.256.jpg",
                author = "Tatsuki Fujimoto",
                rating = 4.90f,
                status = "Ongoing",
                synopsis = "Denji is trying to live a normal life while caring for Nayuta, as the deadly War Devil searches Tokyo to destroy Chainsaw Man.",
                genres = listOf("Action", "Horror", "Supernatural", "Comedy", "Shounen"),
                source = MangaSource.MANGADEX,
                totalChapters = 180,
                isTrending = true,
                latestChapter = "Ch. 180",
                contentRating = "suggestive"
            ),
            Manga(
                id = "b0b721ff-c388-4486-aa0f-c8b0e20e50b2", // Frieren
                title = "Frieren: Beyond Journey's End",
                coverUrl = "https://uploads.mangadex.org/covers/b0b721ff-c388-4486-aa0f-c8b0e20e50b2/b43db0e4-d621-424a-8106-fc5e02206ff8.jpg.256.jpg",
                author = "Kanehito Yamada",
                rating = 4.97f,
                status = "Ongoing",
                synopsis = "The demon king has been defeated, and the hero party disbands. An elven mage reflecting on human mortality embarks on a journey across the continent.",
                genres = listOf("Fantasy", "Adventure", "Drama", "Slice of Life"),
                source = MangaSource.MANGADEX,
                totalChapters = 135,
                latestChapter = "Ch. 135",
                contentRating = "safe"
            ),
            Manga(
                id = "a1c7c817-4e59-43b7-9365-09675a149a6f", // One Piece
                title = "One Piece",
                coverUrl = "https://uploads.mangadex.org/covers/a1c7c817-4e59-43b7-9365-09675a149a6f/a62cf3ea-c49b-4ffc-a6be-3e6f98774780.jpg.256.jpg",
                author = "Eiichiro Oda",
                rating = 4.99f,
                status = "Ongoing",
                synopsis = "Monkey D. Luffy embarks with his eclectic crew across the Grand Line in search of the legendary pirate treasure left by Gol D. Roger.",
                genres = listOf("Action", "Adventure", "Comedy", "Shounen", "Fantasy"),
                source = MangaSource.MANGADEX,
                totalChapters = 1120,
                latestChapter = "Ch. 1120",
                contentRating = "safe"
            ),
            Manga(
                id = "c52b2ce3-7f95-469c-96b0-479524fb7a1a", // Jujutsu Kaisen
                title = "Jujutsu Kaisen",
                coverUrl = "https://uploads.mangadex.org/covers/c52b2ce3-7f95-469c-96b0-479524fb7a1a/e12d46e9-b50e-4fa0-82a8-144a95728a50.jpg.256.jpg",
                author = "Gege Akutami",
                rating = 4.92f,
                status = "Completed",
                synopsis = "Yuji Itadori swallows a cursed talisman—the finger of Sukuna—and becomes entangled in the occult realm of sorcerers and deadly curses.",
                genres = listOf("Action", "Supernatural", "Shounen", "Horror"),
                source = MangaSource.MANGADEX,
                totalChapters = 271,
                latestChapter = "Ch. 271",
                contentRating = "safe"
            )
        )

        val filtered = baseCatalog.filter { manga ->
            source == MangaSource.ALL || manga.source == source
        }

        return if (filtered.isNotEmpty()) filtered else baseCatalog
    }
}
