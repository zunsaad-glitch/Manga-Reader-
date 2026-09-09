package com.example.repository

import android.util.Log
import com.example.api.ChapterAttributes
import com.example.api.ChapterData
import com.example.api.MangaAttributes
import com.example.api.MangaData
import com.example.api.Relationship
import com.example.api.RelationshipAttributes
import com.example.api.TagAttributes
import com.example.api.TagData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Repository for scraping and streaming clean, official comics & webtoons
 * from MangaToon (https://mangatoon.mobi/).
 */
object MangaToonRepository {
    private const val TAG = "MangaToonRepo"
    const val BASE_URL = "https://mangatoon.mobi"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // In-memory caches for snappy UI
    private val mangaCache = ConcurrentHashMap<String, MangaData>()
    private val chapterCache = ConcurrentHashMap<String, List<ChapterData>>()
    private val pagesCache = ConcurrentHashMap<String, List<String>>()
    private val relatedCache = ConcurrentHashMap<String, List<MangaData>>()

    fun isMangaToonId(id: String): Boolean {
        return id.startsWith("mto_") || id.contains("mangatoon.mobi")
    }

    fun isMangaToonChapterId(chapterId: String): Boolean {
        return chapterId.startsWith("mto_") && chapterId.contains("_ep_")
    }

    fun extractContentId(idOrUrl: String): String {
        if (idOrUrl.contains("content_id=")) {
            val cid = idOrUrl.substringAfter("content_id=").substringBefore("&")
            if (cid.isNotBlank()) return cid
        }
        if (idOrUrl.startsWith("http")) {
            val trimmed = idOrUrl.trimEnd('/')
            if (trimmed.contains("/watch/")) {
                val parts = trimmed.substringAfter("/watch/").split("/")
                if (parts.isNotEmpty()) return parts[0]
            }
            val last = trimmed.substringAfterLast("/")
            if (last.all { it.isDigit() }) return last
        }
        val clean = idOrUrl.removePrefix("mto_")
        val epIdx = clean.indexOf("_ep_")
        return if (epIdx != -1) clean.substring(0, epIdx) else clean
    }

    fun extractEpisodeId(chapterIdOrUrl: String): String {
        if (chapterIdOrUrl.contains("/watch/")) {
            val parts = chapterIdOrUrl.substringAfter("/watch/").split("/")
            if (parts.size >= 2) return parts[1]
        }
        val epIdx = chapterIdOrUrl.indexOf("_ep_")
        return if (epIdx != -1) chapterIdOrUrl.substring(epIdx + 4) else chapterIdOrUrl.removePrefix("mto_")
    }

    fun getRelatedMangas(mangaId: String): List<MangaData> {
        val cid = extractContentId(mangaId)
        return relatedCache[cid] ?: relatedCache["mto_$cid"] ?: emptyList()
    }

    fun cleanTitle(raw: String): String {
        return raw
            .replace(Regex("""<[^>]+>"""), " ")
            .replace(Regex("""\s*(?:-\s*MangaToon|-\s*Manga Toon)\s*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Fetch comic listings from genre or popular directory
     */
    suspend fun getMangaList(
        page: Int = 1,
        genre: String? = null
    ): List<MangaData> = withContext(Dispatchers.IO) {
        try {
            val url = if (!genre.isNullOrBlank()) {
                val cleanGenre = genre.lowercase().trim().replace(" ", "-")
                "$BASE_URL/en/genre/comic?page=$page&tag=$cleanGenre"
            } else {
                "$BASE_URL/en/genre/comic?page=$page"
            }

            Log.d(TAG, "Fetching MangaToon list: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/en")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "MangaToon HTTP ${response.code} for $url")
                return@withContext if (page == 1) getCuratedSnapshot() else emptyList()
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)
            val mangas = parseListingCards(doc)
            if (mangas.isNotEmpty()) {
                mangas.forEach { mangaCache[it.id] = it }
                return@withContext mangas
            }

            if (page == 1) getCuratedSnapshot() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MangaToon list", e)
            if (page == 1) getCuratedSnapshot() else emptyList()
        }
    }

    /**
     * Search MangaToon comics by query
     */
    suspend fun searchManga(query: String, page: Int = 1): List<MangaData> = withContext(Dispatchers.IO) {
        val qTrimmed = query.trim()
        if (qTrimmed.isEmpty()) {
            return@withContext getCuratedSnapshot()
        }

        try {
            val encodedQuery = URLEncoder.encode(qTrimmed, "UTF-8")
            val url = "$BASE_URL/en/search?word=$encodedQuery"
            Log.d(TAG, "Searching MangaToon: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/en")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "MangaToon search failed ${response.code}")
                return@withContext searchFallback(qTrimmed)
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)
            val results = parseSearchResults(doc)

            if (results.isNotEmpty()) {
                results.forEach { mangaCache[it.id] = it }
                return@withContext results
            }

            searchFallback(qTrimmed)
        } catch (e: Exception) {
            Log.e(TAG, "Error in MangaToon search", e)
            searchFallback(qTrimmed)
        }
    }

    private fun searchFallback(query: String): List<MangaData> {
        val qLower = query.lowercase().trim()
        val all = (mangaCache.values + getCuratedSnapshot()).distinctBy { it.id }
        return all.filter { manga ->
            val title = manga.attributes?.title?.values?.firstOrNull()?.lowercase().orEmpty()
            val desc = manga.attributes?.description?.values?.firstOrNull()?.lowercase().orEmpty()
            val tags = manga.attributes?.tags?.mapNotNull { it.attributes?.name?.values?.firstOrNull()?.lowercase() }.orEmpty()
            title.contains(qLower) || desc.contains(qLower) || tags.any { it.contains(qLower) }
        }
    }

    /**
     * Parse comic cards from /genre/comic listing
     */
    private fun parseListingCards(doc: Document): List<MangaData> {
        val list = mutableListOf<MangaData>()
        val seenIds = mutableSetOf<String>()

        val cardAnchors = doc.select("a[href*='content_id=']")
        for (anchor in cardAnchors) {
            val href = anchor.attr("href")
            val cid = extractContentId(href)
            if (cid.isBlank() || seenIds.contains(cid)) continue
            seenIds.add(cid)

            val imgEl = anchor.selectFirst("img")
            var coverUrl = imgEl?.attr("data-src")?.ifEmpty { null }
                ?: imgEl?.attr("src")?.ifEmpty { null }
                ?: ""
            if (coverUrl.startsWith("//")) coverUrl = "https:$coverUrl"
            if (coverUrl.startsWith("/")) coverUrl = "$BASE_URL$coverUrl"

            val titleEl = anchor.selectFirst(".content-title, .content-title-2, .comics-title")
            var title = titleEl?.text()?.trim() ?: imgEl?.attr("alt")?.trim() ?: ""
            if (title.isBlank()) title = "Comic #$cid"
            title = cleanTitle(title)

            val tagEls = anchor.select(".content-tags span, .tags span, .comics-type span")
            val tagList = mutableListOf<TagData>()
            for (t in tagEls) {
                val rawTags = t.text().split("/", "&nbsp;/&nbsp;", "·", ",")
                for (rt in rawTags) {
                    val cleanTag = rt.trim()
                    if (cleanTag.isNotBlank() && cleanTag.length in 2..25 && !cleanTag.matches(Regex("""\d+[kKmM]?"""))) {
                        tagList.add(
                            TagData(
                                id = "mto_tag_${cleanTag.lowercase().replace(" ", "_")}",
                                attributes = TagAttributes(
                                    name = mapOf("en" to cleanTag)
                                )
                            )
                        )
                    }
                }
            }

            val manga = MangaData(
                id = "mto_$cid",
                attributes = MangaAttributes(
                    title = mapOf("en" to title),
                    description = mapOf("en" to "Read $title officially on MangaToon. High-quality webtoon and manga series with full color chapters."),
                    status = "ongoing",
                    contentRating = "safe",
                    tags = tagList.distinctBy { it.id },
                    originalLanguage = "en"
                ),
                relationships = listOf(
                    Relationship(
                        id = "mto_cover_$cid",
                        type = "cover_art",
                        attributes = RelationshipAttributes(
                            fileName = coverUrl
                        )
                    )
                )
            )
            list.add(manga)
        }
        return list
    }

    /**
     * Parse search result cards from /en/search
     */
    private fun parseSearchResults(doc: Document): List<MangaData> {
        val list = mutableListOf<MangaData>()
        val seenIds = mutableSetOf<String>()

        val items = doc.select(".recommend-item, .search-item, .item, a[href*='content_id=']")
        for (item in items) {
            val anchor = if (item.tagName() == "a") item else item.selectFirst("a[href*='content_id=']")
            val href = anchor?.attr("href") ?: ""
            val cid = extractContentId(href)
            if (cid.isBlank() || seenIds.contains(cid)) continue
            seenIds.add(cid)

            val imgEl = item.selectFirst("img")
            var coverUrl = imgEl?.attr("data-src")?.ifEmpty { null }
                ?: imgEl?.attr("src")?.ifEmpty { null }
                ?: ""
            if (coverUrl.startsWith("//")) coverUrl = "https:$coverUrl"
            if (coverUrl.startsWith("/")) coverUrl = "$BASE_URL$coverUrl"

            val titleEl = item.selectFirst(".recommend-comics-title, .comics-title, .content-title, h2, h3")
            var title = titleEl?.text()?.trim() ?: imgEl?.attr("alt")?.trim() ?: ""
            if (title.isBlank()) title = "Comic #$cid"
            title = cleanTitle(title)

            val typeEl = item.selectFirst(".comics-type, .content-tags, .tags")
            val tagList = mutableListOf<TagData>()
            if (typeEl != null) {
                val tagStrings = typeEl.text().split("/", "&nbsp;/&nbsp;", "·", ",")
                for (ts in tagStrings) {
                    val cleanTag = ts.trim()
                    if (cleanTag.isNotBlank() && cleanTag.length in 2..25 && !cleanTag.matches(Regex("""\d+[kKmM]?"""))) {
                        tagList.add(
                            TagData(
                                id = "mto_tag_${cleanTag.lowercase().replace(" ", "_")}",
                                attributes = TagAttributes(
                                    name = mapOf("en" to cleanTag)
                                )
                            )
                        )
                    }
                }
            }

            val manga = MangaData(
                id = "mto_$cid",
                attributes = MangaAttributes(
                    title = mapOf("en" to title),
                    description = mapOf("en" to "Read $title on MangaToon. Enjoy official high-definition full-color webtoon episodes."),
                    status = "ongoing",
                    contentRating = "safe",
                    tags = tagList.distinctBy { it.id },
                    originalLanguage = "en"
                ),
                relationships = listOf(
                    Relationship(
                        id = "mto_cover_$cid",
                        type = "cover_art",
                        attributes = RelationshipAttributes(
                            fileName = coverUrl
                        )
                    )
                )
            )
            list.add(manga)
        }
        return list
    }

    /**
     * Fetch complete manga detail with synopsis, authors, and genres
     */
    suspend fun getMangaDetails(mangaId: String): MangaData? = withContext(Dispatchers.IO) {
        val cid = extractContentId(mangaId)
        val fullId = "mto_$cid"

        mangaCache[fullId]?.let { cached ->
            if (cached.attributes?.description?.get("en")?.length ?: 0 > 100) {
                return@withContext cached
            }
        }

        try {
            val url = "$BASE_URL/en/detail/$cid"
            Log.d(TAG, "Fetching MangaToon detail: $url")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/en")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Detail fetch failed ${response.code} for $url")
                return@withContext mangaCache[fullId] ?: getCuratedSnapshot().find { it.id == fullId }
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, response.request.url.toString())

            // Title
            val titleEl = doc.selectFirst("h1, .detail-title, .detail-name")
            var title = titleEl?.text()?.trim().orEmpty()
            if (title.isBlank()) title = doc.title().substringBefore("-").trim()
            title = cleanTitle(title)

            // Cover
            val coverEl = doc.selectFirst(".detail-img img, .detail-image img, img.detail-cover")
            var coverUrl = coverEl?.attr("data-src")?.ifEmpty { null }
                ?: coverEl?.attr("src")?.ifEmpty { null }
                ?: ""
            if (coverUrl.startsWith("//")) coverUrl = "https:$coverUrl"
            if (coverUrl.startsWith("/")) coverUrl = "$BASE_URL$coverUrl"

            // Description / Synopsis
            val descEl = doc.selectFirst(".detail-description-all, .detail-description-short, .detail-description, .detail-intro")
            val desc = descEl?.text()?.trim()
                ?.replace(Regex("""^(?:Bully\s*×\s*|Introduction:\s*)"""), "")
                ?: "Read $title on MangaToon. Enjoy official full-color webtoon chapters."

            // Tags / Categories
            val tags = mutableListOf<TagData>()
            val tagElements = doc.select(".detail-tags-item span, .detail-tags span, .detail-info span")
            for (te in tagElements) {
                val raw = te.text().trim()
                if (raw.startsWith("Author Name:", ignoreCase = true) || raw.startsWith("Author:", ignoreCase = true)) {
                    continue
                }
                val splitTags = raw.split("/", ",", "·")
                for (st in splitTags) {
                    val cTag = st.trim()
                    if (cTag.isNotBlank() && cTag.length in 2..25 && !cTag.matches(Regex("""\d+[kKmM]?"""))) {
                        tags.add(
                            TagData(
                                id = "mto_tag_${cTag.lowercase().replace(" ", "_")}",
                                attributes = TagAttributes(
                                    name = mapOf("en" to cTag)
                                )
                            )
                        )
                    }
                }
            }

            // Author / Artist
            val authorText = doc.select(".detail-author, .author, .detail-tags-info").text()
            val authorClean = if (authorText.contains("Author Name:")) {
                authorText.substringAfter("Author Name:").substringBefore("\n").trim()
            } else if (authorText.contains("Author:")) {
                authorText.substringAfter("Author:").substringBefore("\n").trim()
            } else "MangaToon Official"

            val mangaData = MangaData(
                id = fullId,
                attributes = MangaAttributes(
                    title = mapOf("en" to title),
                    description = mapOf("en" to desc),
                    status = "ongoing",
                    contentRating = "safe",
                    tags = tags.distinctBy { it.id },
                    originalLanguage = "en"
                ),
                relationships = listOf(
                    Relationship(
                        id = "mto_cover_$cid",
                        type = "cover_art",
                        attributes = RelationshipAttributes(
                            fileName = coverUrl
                        )
                    ),
                    Relationship(
                        id = "mto_author_$cid",
                        type = "author",
                        attributes = RelationshipAttributes(
                            name = authorClean
                        )
                    )
                )
            )

            mangaCache[fullId] = mangaData

            // Parse chapters as well while we have the document
            val parsedChapters = parseChaptersFromDoc(doc, cid)
            if (parsedChapters.isNotEmpty()) {
                chapterCache[fullId] = parsedChapters
            }

            mangaData
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MangaToon detail", e)
            mangaCache[fullId] ?: getCuratedSnapshot().find { it.id == fullId }
        }
    }

    /**
     * Scrape chapter list for a MangaToon series
     */
    suspend fun getChapters(mangaId: String): List<ChapterData> = withContext(Dispatchers.IO) {
        val cid = extractContentId(mangaId)
        val fullId = "mto_$cid"

        chapterCache[fullId]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        try {
            val url = "$BASE_URL/en/detail/$cid"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/en")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed fetching chapters HTTP ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, response.request.url.toString())
            val chapters = parseChaptersFromDoc(doc, cid)
            if (chapters.isNotEmpty()) {
                chapterCache[fullId] = chapters
            }
            chapters
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MangaToon chapters", e)
            emptyList()
        }
    }

    fun getCachedChapterCount(mangaId: String): Int {
        val cid = extractContentId(mangaId)
        return chapterCache["mto_$cid"]?.size ?: chapterCache[mangaId]?.size ?: 0
    }

    /**
     * Parse episodes from detail page DOM
     */
    private fun parseChaptersFromDoc(doc: Document, cid: String): List<ChapterData> {
        val list = mutableListOf<ChapterData>()
        val seenEpIds = mutableSetOf<String>()

        val watchLinks = doc.select("a[href*='/en/watch/$cid/']")
        for ((index, anchor) in watchLinks.withIndex()) {
            val href = anchor.attr("href")
            val epId = extractEpisodeId(href)
            if (epId.isBlank() || seenEpIds.contains(epId)) continue
            seenEpIds.add(epId)

            val rawText = anchor.text().replace(Regex("""&#x[0-9a-fA-F]+;"""), " ").trim()
            val epMatch = Regex("""Episode\s*(\d+)""", RegexOption.IGNORE_CASE).find(rawText)
            val epNumStr = epMatch?.groupValues?.get(1) ?: (index + 1).toString()
            val dateMatch = Regex("""(\d{4}-\d{2}-\d{2})""").find(rawText)
            val dateStr = dateMatch?.groupValues?.get(1) ?: "2024-01-01"

            val title = if (rawText.contains("Episode", ignoreCase = true)) {
                "Episode $epNumStr"
            } else if (rawText.contains("READ EP", ignoreCase = true)) {
                "Episode 1"
            } else {
                "Episode ${index + 1}"
            }

            list.add(
                ChapterData(
                    id = "mto_${cid}_ep_$epId",
                    type = "chapter",
                    attributes = ChapterAttributes(
                        title = title,
                        volume = null,
                        chapter = epNumStr,
                        pages = 0,
                        translatedLanguage = "en",
                        publishAt = "${dateStr}T00:00:00+00:00",
                        readableAt = "${dateStr}T00:00:00+00:00"
                    )
                )
            )
        }

        // Sort chapters chronologically (Episode 1 first)
        list.sortBy { it.attributes?.chapter?.toFloatOrNull() ?: 0f }
        return list
    }

    /**
     * Scrape page images for a chapter / episode
     */
    suspend fun getChapterImages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        pagesCache[chapterId]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val cid = extractContentId(chapterId)
        val epId = extractEpisodeId(chapterId)
        val url = "$BASE_URL/en/watch/$cid/$epId"

        try {
            Log.d(TAG, "Fetching MangaToon episode images: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/en/detail/$cid")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Episode fetch failed ${response.code} for $url")
                return@withContext emptyList()
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)

            val pageUrls = mutableListOf<String>()
            val imgElements = doc.select("img")

            for (img in imgElements) {
                val src = img.attr("data-src").ifEmpty { img.attr("src") }.trim()
                if (src.isBlank()) continue

                val lower = src.lowercase()
                val isComicImage = (lower.contains("mangatoon.mobi") || lower.contains("pic-aliyun")) &&
                        (lower.endsWith(".jpg") || lower.endsWith(".webp") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                                lower.contains("/watermark/") || lower.contains("/ps/")) &&
                        !lower.contains("logo") && !lower.contains("avatar") && !lower.contains("banner")

                if (isComicImage) {
                    var fullUrl = src
                    if (fullUrl.startsWith("//")) fullUrl = "https:$fullUrl"
                    if (!pageUrls.contains(fullUrl)) {
                        pageUrls.add(fullUrl)
                    }
                }
            }

            Log.d(TAG, "Found ${pageUrls.size} images for MangaToon episode $chapterId")
            if (pageUrls.isNotEmpty()) {
                pagesCache[chapterId] = pageUrls
            }
            pageUrls
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching MangaToon chapter images", e)
            emptyList()
        }
    }

    /**
     * Curated snapshot of top popular MangaToon series for instant loading & resilience
     */
    fun getCuratedSnapshot(): List<MangaData> {
        return listOf(
            createCuratedManga(
                cid = "5",
                title = "Tales of Demons and Gods",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/5e495.jpg",
                desc = "Nie Li, the strongest Demon Spiritualist in his past life standing at the pinnacle of the martial world, lost his life during the battle with the Sage Emperor and the six deity ranked beasts. His soul was brought back to when he was still 13 years old.",
                tags = listOf("Action", "Fantasy", "Martial Arts", "Reincarnation", "Shounen")
            ),
            createCuratedManga(
                cid = "3727959",
                title = "Only Love",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/37279597b3e.webp",
                desc = "Bully × The first love of many, an interpretation of the deepest love. Reborn as a high school sophomore, Qin Chuan made a vow to protect the gentle girl who was once brutally hurt.",
                tags = listOf("Romance", "School Life", "Drama", "Rebirth")
            ),
            createCuratedManga(
                cid = "1477993",
                title = "School Hunk Is A Girl",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/1477993.jpg",
                desc = "Disguised as a handsome young boy, the female lead enters the elite high school to clear her family's name and ends up capturing the hearts of millions as the campus heartthrob.",
                tags = listOf("Romance", "Gender Bender", "Comedy", "School Life", "Sweet")
            ),
            createCuratedManga(
                cid = "6121145",
                title = "Indulgent Gambit",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/6121145b589.webp-posternew6",
                desc = "In an intricate game of romance and business ambition, two powerful individuals find themselves falling into a web of unstoppable passion and hidden secrets.",
                tags = listOf("Romance", "CEO", "Urban Romance", "Drama")
            ),
            createCuratedManga(
                cid = "38471",
                title = "Against the Gods",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/38471.jpg",
                desc = "Possessing the Sky Poison Pearl, Yun Che wakes up in the body of a crippled youth. With determination and heavenly treasures, he defies the gods and walks the path of supreme conquest.",
                tags = listOf("Action", "Fantasy", "Cultivation", "Adventure")
            ),
            createCuratedManga(
                cid = "1005142",
                title = "Bow to Your Queen",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/1005142.jpg",
                desc = "An invincible modern assassin transmigrates into a fragile, bullied princess. Now she turns the empire upside down and makes everyone bow before her royal majesty.",
                tags = listOf("Fantasy", "Historical", "Strong Female Lead", "Transmigration")
            ),
            createCuratedManga(
                cid = "2026",
                title = "Mortals of the Doom",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/20268.jpg",
                desc = "When the world crumbles into apocalyptic chaos and mutant beasts emerge, ordinary humans fight tooth and nail for survival, discovering dormant superpowers.",
                tags = listOf("Action", "Horror", "Sci-Fi", "Apocalypse", "Supernatural")
            ),
            createCuratedManga(
                cid = "546539",
                title = "Cruel Love",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/546539.jpg",
                desc = "A misunderstanding tore them apart years ago, but fate brings them back together in the corporate battlefield where vengeance turns into bittersweet devotion.",
                tags = listOf("Romance", "Drama", "Angst", "CEO")
            ),
            createCuratedManga(
                cid = "1035971",
                title = "Salad Days",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/1035971.jpg",
                desc = "A passionate ballet dancer and a tough young boxer support each other's dreams through sweat, hardships, and youthful warmth.",
                tags = listOf("Youth", "Sports", "Slice of Life", "School Life")
            ),
            createCuratedManga(
                cid = "1478034",
                title = "Marry One Get One Free",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/1478034.jpg",
                desc = "She thought she was simply adopting an adorable genius child, only to find out that the child comes with an overbearing billionaire father attached.",
                tags = listOf("Romance", "Comedy", "Family", "Sweet", "Urban")
            ),
            createCuratedManga(
                cid = "2727893",
                title = "The Zombie & The Vampire",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/2727893.jpg",
                desc = "A refined vampire gentleman crosses paths with a quirky zombie girl who doesn't conform to any undead rules. A hilarious supernatural adventure begins.",
                tags = listOf("Comedy", "Supernatural", "Vampire", "Fantasy")
            ),
            createCuratedManga(
                cid = "2120",
                title = "Resurrecting Queen",
                cover = "https://cn-e-pic-aliyun.mangatoon.mobi/cartoon-posters/2120.jpg",
                desc = "Betrayed by the one she loved, the queen of the entertainment industry is murdered, only to awaken in the body of an innocent rookie starlet ready for sweet revenge.",
                tags = listOf("Revenge", "Showbiz", "Drama", "Romance")
            )
        )
    }

    private fun createCuratedManga(
        cid: String,
        title: String,
        cover: String,
        desc: String,
        tags: List<String>
    ): MangaData {
        return MangaData(
            id = "mto_$cid",
            attributes = MangaAttributes(
                title = mapOf("en" to title),
                description = mapOf("en" to desc),
                status = "ongoing",
                contentRating = "safe",
                tags = tags.map { t ->
                    TagData(
                        id = "mto_tag_${t.lowercase().replace(" ", "_")}",
                        attributes = TagAttributes(
                            name = mapOf("en" to t)
                        )
                    )
                },
                originalLanguage = "en"
            ),
            relationships = listOf(
                Relationship(
                    id = "mto_cover_$cid",
                    type = "cover_art",
                    attributes = RelationshipAttributes(
                        fileName = cover
                    )
                )
            )
        )
    }
}
