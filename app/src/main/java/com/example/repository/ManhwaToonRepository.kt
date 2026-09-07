package com.example.repository

import android.util.Log
import com.example.api.ChapterAttributes
import com.example.api.ChapterData
import com.example.api.MangaAttributes
import com.example.api.MangaData
import com.example.api.Relationship
import com.example.api.RelationshipAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ManhwaToonRepository {
    private const val TAG = "ManhwaToonRepo"
    const val BASE_URL = "https://www.manhwatoon.me"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // In-memory cache for fast responsive UI
    private val mangaCache = ConcurrentHashMap<String, MangaData>()
    private val chapterCache = ConcurrentHashMap<String, List<ChapterData>>()
    private val pagesCache = ConcurrentHashMap<String, List<String>>()

    fun isManhwaToonId(id: String): Boolean {
        return id.startsWith("mt_") || id.contains("manhwatoon.me")
    }

    fun isManhwaToonChapterId(chapterId: String): Boolean {
        return chapterId.startsWith("mt_") && chapterId.contains("_ch_")
    }

    fun extractSlug(idOrUrl: String): String {
        if (idOrUrl.startsWith("http")) {
            val trimmed = idOrUrl.trimEnd('/')
            val parts = trimmed.split("/")
            val idx = parts.indexOf("manhwa")
            if (idx != -1 && idx + 1 < parts.size) {
                return parts[idx + 1]
            }
            return parts.last()
        }
        val clean = idOrUrl.removePrefix("mt_")
        val chIdx = clean.indexOf("_ch_")
        return if (chIdx != -1) clean.substring(0, chIdx) else clean
    }

    fun extractChapterSlug(chapterIdOrUrl: String): String {
        if (chapterIdOrUrl.startsWith("http")) {
            val trimmed = chapterIdOrUrl.trimEnd('/')
            return trimmed.substringAfterLast("/")
        }
        val chIdx = chapterIdOrUrl.indexOf("_ch_")
        return if (chIdx != -1) chapterIdOrUrl.substring(chIdx + 4) else chapterIdOrUrl.removePrefix("mt_")
    }

    /**
     * Fetch manga list from directory, orderby, or genre
     */
    suspend fun getMangaList(
        page: Int = 1,
        sortOrder: String = "latest", // "latest", "trending", "rating", "views", "new-manga", "alphabet"
        genre: String? = null
    ): List<MangaData> = withContext(Dispatchers.IO) {
        try {
            val url = when {
                !genre.isNullOrBlank() -> {
                    val cleanGenre = genre.lowercase().trim().replace(" ", "-")
                    if (page > 1) "$BASE_URL/manhwa-genre/$cleanGenre/page/$page/"
                    else "$BASE_URL/manhwa-genre/$cleanGenre/"
                }
                page > 1 -> "$BASE_URL/manga/page/$page/?m_orderby=$sortOrder"
                else -> "$BASE_URL/manga/?m_orderby=$sortOrder"
            }

            Log.d(TAG, "Fetching ManhwaToon list: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed response ${response.code} for $url")
                return@withContext if (page == 1) getCuratedSnapshot() else emptyList()
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)
            val mangas = parseMangaCards(doc)
            if (mangas.isNotEmpty()) {
                mangas.forEach { mangaCache[it.id] = it }
                return@withContext mangas
            }

            if (page == 1) getCuratedSnapshot() else emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ManhwaToon list", e)
            if (page == 1) getCuratedSnapshot() else emptyList()
        }
    }

    /**
     * Live search on manhwatoon.me
     */
    suspend fun searchManga(query: String, page: Int = 1): List<MangaData> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = if (page > 1) {
                "$BASE_URL/page/$page/?s=$encodedQuery&post_type=wp-manga"
            } else {
                "$BASE_URL/?s=$encodedQuery&post_type=wp-manga"
            }

            Log.d(TAG, "Searching ManhwaToon: $url")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)
            val results = parseMangaCards(doc)
            results.forEach { mangaCache[it.id] = it }
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error searching ManhwaToon for $query", e)
            emptyList()
        }
    }

    /**
     * Get detailed metadata for a specific manhwa
     */
    suspend fun getMangaDetails(slugOrId: String): MangaData? = withContext(Dispatchers.IO) {
        val slug = extractSlug(slugOrId)
        val mangaId = "mt_$slug"

        mangaCache[mangaId]?.let { cached ->
            // If already has description, return cached
            if (!cached.attributes?.description?.values?.firstOrNull().isNullOrBlank()) {
                return@withContext cached
            }
        }

        try {
            val url = "$BASE_URL/manhwa/$slug/"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext mangaCache[mangaId]

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, BASE_URL)

            val title = doc.select(".post-title h1").text().trim().ifEmpty {
                doc.select("meta[property=og:title]").attr("content").removeSuffix(" - ManhwaToon").trim()
            }.ifEmpty { slug.replace("-", " ").capitalizeWords() }

            val coverUrl = doc.select("meta[property=og:image]").attr("content").trim().ifEmpty {
                extractImageSrc(doc.selectFirst(".summary_image img"))
            }

            val desc = doc.select(".summary__content, .description-summary, .manga-excerpt").text().trim()
            val author = doc.select(".author-content a, .artist-content a").text().trim().ifEmpty { "ManhwaToon Artist" }
            val genres = doc.select(".genres-content a").map { it.text().trim() }
            val statusText = doc.select(".post-status .summary-content").text().trim().lowercase()
            val status = if (statusText.contains("complete")) "completed" else "ongoing"

            val manga = MangaData(
                id = mangaId,
                attributes = MangaAttributes(
                    title = mapOf("en" to title),
                    description = mapOf("en" to desc),
                    originalLanguage = "ko",
                    contentRating = if (genres.any { it.contains("Adult", true) || it.contains("Mature", true) }) "pornographic" else "suggestive",
                    status = status
                ),
                relationships = listOfNotNull(
                    Relationship(
                        id = mangaId,
                        type = "cover_art",
                        attributes = RelationshipAttributes(fileName = coverUrl)
                    ),
                    Relationship(
                        id = "author_$slug",
                        type = "author",
                        attributes = RelationshipAttributes(name = author)
                    )
                )
            )

            mangaCache[mangaId] = manga
            manga
        } catch (e: Exception) {
            Log.e(TAG, "Error getting manga details for $slug", e)
            mangaCache[mangaId]
        }
    }

    /**
     * Get chapters for a manhwa
     */
    suspend fun getChapters(mangaIdOrSlug: String): List<ChapterData> = withContext(Dispatchers.IO) {
        val slug = extractSlug(mangaIdOrSlug)
        val mangaId = "mt_$slug"

        chapterCache[mangaId]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val chapters = mutableListOf<ChapterData>()
        try {
            // 1. First try AJAX endpoint (standard for Madara theme)
            val ajaxUrl = "$BASE_URL/manhwa/$slug/ajax/chapters/"
            val request = Request.Builder()
                .url(ajaxUrl)
                .post("".toRequestBody(null))
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/manhwa/$slug/")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            val response = httpClient.newCall(request).execute()
            var html = ""
            if (response.isSuccessful) {
                html = response.body?.string().orEmpty()
            }

            // Fallback: fetch main page if AJAX returned empty
            if (html.isBlank() || !html.contains("wp-manga-chapter")) {
                val pageReq = Request.Builder()
                    .url("$BASE_URL/manhwa/$slug/")
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "$BASE_URL/")
                    .build()
                val pageResp = httpClient.newCall(pageReq).execute()
                if (pageResp.isSuccessful) {
                    html = pageResp.body?.string().orEmpty()
                }
            }

            if (html.isNotBlank()) {
                val doc = Jsoup.parse(html, BASE_URL)
                val elements = doc.select("li.wp-manga-chapter")
                for (el in elements) {
                    val aTag = el.selectFirst("a") ?: continue
                    val chHref = aTag.attr("abs:href").trim()
                    if (chHref.isEmpty()) continue

                    val chSlug = chHref.trimEnd('/').substringAfterLast('/')
                    val chTitle = aTag.text().trim()
                    val dateStr = el.select(".chapter-release-date, span.post-on, i").text().trim().ifEmpty {
                        aTag.attr("title").ifEmpty { "Recent" }
                    }

                    // Extract chapter number from text or slug
                    val chNumMatch = Regex("""(?:chapter|ch\.?)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(chTitle)
                        ?: Regex("""chapter-(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE).find(chSlug)
                    val chNum = chNumMatch?.groupValues?.get(1) ?: "1"

                    val chapterId = "mt_${slug}_ch_${chSlug}"
                    chapters.add(
                        ChapterData(
                            id = chapterId,
                            type = "chapter",
                            attributes = ChapterAttributes(
                                volume = "1",
                                chapter = chNum,
                                title = chTitle,
                                translatedLanguage = "en",
                                pages = 0,
                                publishAt = dateStr
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chapters for $slug", e)
        }

        if (chapters.isNotEmpty()) {
            chapterCache[mangaId] = chapters
        }
        chapters
    }

    /**
     * Get image URLs for reading a chapter
     */
    suspend fun getChapterImages(chapterIdOrUrl: String): List<String> = withContext(Dispatchers.IO) {
        pagesCache[chapterIdOrUrl]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val slug = extractSlug(chapterIdOrUrl)
        val chSlug = extractChapterSlug(chapterIdOrUrl)
        val chapterUrl = if (chapterIdOrUrl.startsWith("http")) {
            chapterIdOrUrl
        } else {
            "$BASE_URL/manhwa/$slug/$chSlug/"
        }

        val images = mutableListOf<String>()
        try {
            Log.d(TAG, "Fetching chapter reader images: $chapterUrl")
            val request = Request.Builder()
                .url(chapterUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "$BASE_URL/manhwa/$slug/")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Reader request failed with code ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body?.string().orEmpty()
            val doc = Jsoup.parse(html, chapterUrl)

            // Madara readers place images inside .reading-content or .page-break
            val imgElements = doc.select(".reading-content img, .page-break img, .wp-manga-chapter-img, div.text-left img")
            for (img in imgElements) {
                val src = extractImageSrc(img)
                if (src.isNotBlank() && src.startsWith("http") && !src.contains("dflazy.jpg") && !images.contains(src)) {
                    images.add(src)
                }
            }

            Log.d(TAG, "Extracted ${images.size} reader images for $chSlug")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chapter images for $chapterUrl", e)
        }

        if (images.isNotEmpty()) {
            pagesCache[chapterIdOrUrl] = images
        }
        images
    }

    /**
     * Extract clean image URL from Element supporting data-src, data-srcset, src
     */
    private fun extractImageSrc(img: Element?): String {
        if (img == null) return ""
        val dataSrc = img.attr("data-src").trim()
        if (dataSrc.startsWith("http") && !dataSrc.contains("dflazy.jpg")) return dataSrc

        val dataSrcset = img.attr("data-srcset").trim()
        if (dataSrcset.isNotBlank()) {
            // Pick the highest resolution URL in srcset (e.g. "url1 110w, url2 175w")
            val parts = dataSrcset.split(",").map { it.trim() }
            val best = parts.lastOrNull()?.split(" ")?.firstOrNull()?.trim().orEmpty()
            if (best.startsWith("http")) return best
        }

        val src = img.attr("src").trim()
        if (src.startsWith("http") && !src.contains("dflazy.jpg")) return src
        return ""
    }

    /**
     * Parse HTML cards from search or directory pages
     */
    private fun parseMangaCards(doc: Document): List<MangaData> {
        val results = mutableListOf<MangaData>()
        val seenSlugs = mutableSetOf<String>()

        // Look for cards: .page-item-detail, .c-tabs-item__content, .item-thumb
        val cardElements = doc.select(".page-item-detail, .c-tabs-item__content, .row.c-tabs-item__content, .col-6.col-md-3, .col-6.col-md-2")

        for (el in cardElements) {
            val aTag = el.selectFirst("h3 a, .post-title a, .tab-thumb a, .item-thumb a") ?: continue
            val href = aTag.attr("abs:href").trim()
            if (!href.contains("/manhwa/")) continue

            val slug = extractSlug(href)
            if (slug.isEmpty() || !seenSlugs.add(slug)) continue

            val title = aTag.text().trim().ifEmpty {
                aTag.attr("title").trim()
            }.ifEmpty {
                el.selectFirst(".post-title")?.text()?.trim().orEmpty()
            }.ifEmpty { slug.replace("-", " ").capitalizeWords() }

            val imgEl = el.selectFirst("img")
            val coverUrl = extractImageSrc(imgEl).ifEmpty {
                "https://cdn.manhwatoon.me/$slug.webp"
            }

            val rating = el.select(".score, .total_votes").text().trim()
            val latestCh = el.select(".chapter-item a, .list-chapter a").firstOrNull()?.text()?.trim()
            val desc = if (!latestCh.isNullOrEmpty()) "Latest: $latestCh" else "Korean Manhwa & Webtoon"

            val mangaId = "mt_$slug"
            results.add(
                MangaData(
                    id = mangaId,
                    attributes = MangaAttributes(
                        title = mapOf("en" to title),
                        description = mapOf("en" to desc),
                        originalLanguage = "ko",
                        contentRating = "suggestive",
                        status = "ongoing"
                    ),
                    relationships = listOf(
                        Relationship(
                            id = mangaId,
                            type = "cover_art",
                            attributes = RelationshipAttributes(fileName = coverUrl)
                        ),
                        Relationship(
                            id = "author_$slug",
                            type = "author",
                            attributes = RelationshipAttributes(name = "ManhwaToon")
                        )
                    )
                )
            )
        }
        return results
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Curated snapshot of popular manhwas on ManhwaToon for instant zero-latency preview
     */
    fun getCuratedSnapshot(): List<MangaData> {
        val curated = listOf(
            Triple("a-savage-proposal", "A Savage Proposal", "https://cdn.manhwatoon.me/a-savage-proposal-38187.webp"),
            Triple("excuse-me-this-is-my-room-uncensored", "Excuse Me, This Is My Room [Uncensored]", "https://cdn.manhwatoon.me/WP-manga/data/manga_6a378323a0a6f/cover.webp"),
            Triple("theres-no-such-thing-as-a-bad-hero-in-the-world", "There’s No Such Thing As A Bad Hero In The World", "https://cdn.manhwatoon.me/theres-no-such-thing-as-a-bad-hero-in-the-world-15874.webp"),
            Triple("freelancer", "Freelancer", "https://cdn.manhwatoon.me/freelancer-36946.webp"),
            Triple("beautiful-days-raw", "Beautiful Days [Full Color]", "https://cdn.manhwatoon.me/beautiful-days-raw-29753.webp"),
            Triple("beware-of-the-villainess-manhwa", "Beware of the Villainess! Manhwa", "https://cdn.manhwatoon.me/beware-of-the-villainess-manhwa-37012.webp"),
            Triple("the-dead-queens-second-life", "The Dead Queen’s Second Life", "https://cdn.manhwatoon.me/the-dead-queens-second-life-38150.webp"),
            Triple("the-wind-mage", "The Wind Mage", "https://cdn.manhwatoon.me/the-wind-mage-37880.webp"),
            Triple("the-youngest-daughter-of-the-sichuan-tang-family-was-kidnapped", "The Youngest Daughter of the Sichuan Tang Family Was Kidnapped", "https://cdn.manhwatoon.me/the-youngest-daughter-of-the-sichuan-tang-family-was-kidnapped-38165.webp"),
            Triple("where-did-all-the-men-go", "Where Did All the Men Go?", "https://cdn.manhwatoon.me/where-did-all-the-men-go-38012.webp"),
            Triple("mesmerizing-ghost-doctor", "Mesmerizing Ghost Doctor", "https://cdn.manhwatoon.me/mesmerizing-ghost-doctor-36890.webp"),
            Triple("get-out", "Get Out!", "https://cdn.manhwatoon.me/get-out-37540.webp"),
            Triple("hooked-on-you", "Hooked On You", "https://cdn.manhwatoon.me/hooked-on-you-37620.webp"),
            Triple("i-owe-a-billion-dollars-and-i-am-forced-to-become-a-worker-for-an-evil-god", "I Owe A Billion Dollars And I Am Forced to Become A Worker For An Evil God", "https://cdn.manhwatoon.me/i-owe-a-billion-dollars-and-i-am-forced-to-become-a-worker-for-an-evil-god-37910.webp")
        )

        return curated.map { (slug, title, cover) ->
            val mangaId = "mt_$slug"
            MangaData(
                id = mangaId,
                attributes = MangaAttributes(
                    title = mapOf("en" to title),
                    description = mapOf("en" to "Read Korean manhwa and webtoons free on ManhwaToon."),
                    originalLanguage = "ko",
                    contentRating = "suggestive",
                    status = "ongoing"
                ),
                relationships = listOf(
                    Relationship(
                        id = mangaId,
                        type = "cover_art",
                        attributes = RelationshipAttributes(fileName = cover)
                    ),
                    Relationship(
                        id = "author_$slug",
                        type = "author",
                        attributes = RelationshipAttributes(name = "ManhwaToon")
                    )
                )
            )
        }
    }
}
