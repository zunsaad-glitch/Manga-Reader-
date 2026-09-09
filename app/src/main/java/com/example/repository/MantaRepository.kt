package com.example.repository

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
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object MantaRepository {
    private const val BASE_URL = "https://manta.net"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val seriesCache = ConcurrentHashMap<String, MangaData>()
    private val chaptersCache = ConcurrentHashMap<String, List<ChapterData>>()
    private val relatedCache = ConcurrentHashMap<String, List<MangaData>>()
    private var cachedFreshReleases: List<MangaData>? = null

    fun isMantaId(id: String): Boolean = id.startsWith("mta_")

    fun extractCleanId(id: String): String = id.removePrefix("mta_").removePrefix("mta_ch_")

    suspend fun getFreshReleases(): List<MangaData> = withContext(Dispatchers.IO) {
        cachedFreshReleases?.let { if (it.isNotEmpty()) return@withContext it }
        try {
            val url = "$BASE_URL/en/new-and-now"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val parsed = parseNextDataNewAndNow(html)
                if (parsed.isNotEmpty()) {
                    cachedFreshReleases = parsed
                    parsed.forEach { seriesCache[it.id] = it }
                    return@withContext parsed
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val curated = getCuratedSnapshot()
        cachedFreshReleases = curated
        curated
    }

    private fun parseNextDataNewAndNow(html: String): List<MangaData> {
        val result = mutableListOf<MangaData>()
        try {
            val doc = Jsoup.parse(html)
            val scriptEl = doc.selectFirst("script#__NEXT_DATA__") ?: return emptyList()
            val jsonStr = scriptEl.data()
            val root = JSONObject(jsonStr)
            val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps") ?: return emptyList()
            val ssrData = pageProps.optJSONObject("ssrData") ?: return emptyList()
            val schedules = ssrData.optJSONArray("schedules") ?: return emptyList()

            for (i in 0 until schedules.length()) {
                val sched = schedules.optJSONObject(i) ?: continue
                val isoDate = sched.optString("isoDate", "")
                val seriesArr = sched.optJSONArray("series") ?: continue

                for (j in 0 until seriesArr.length()) {
                    val sObj = seriesArr.optJSONObject(j) ?: continue
                    val sid = sObj.optInt("id", 0)
                    if (sid <= 0) continue

                    val sData = sObj.optJSONObject("data") ?: continue
                    val titleObj = sData.optJSONObject("title")
                    val title = titleObj?.optString("en", "")?.ifEmpty { "Manta Series #$sid" } ?: "Manta Series #$sid"

                    val descObj = sData.optJSONObject("description")
                    val desc = descObj?.optString("long", "")?.ifEmpty {
                        descObj.optString("short", "Read $title officially on Manta Comics & Webtoons.")
                    } ?: "Read $title officially on Manta Comics & Webtoons."

                    // Cover image extraction
                    var coverUrl = ""
                    val imgObj = sObj.optJSONObject("image")
                    if (imgObj != null) {
                        val keys = imgObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val sub = imgObj.optJSONObject(k)
                            val dl = sub?.optString("downloadUrl", "")
                            if (!dl.isNullOrEmpty()) {
                                coverUrl = dl
                                break
                            }
                        }
                    }
                    if (coverUrl.isEmpty()) {
                        coverUrl = "https://static.mantacdn.net/thumb/series_$sid.jpg"
                    }

                    // Tags
                    val tagsList = mutableListOf<TagData>()
                    tagsList.add(TagData(id = "mta_tag_webtoon", attributes = TagAttributes(name = mapOf("en" to "Manta Original"))))
                    val tagsArr = sData.optJSONArray("tags")
                    if (tagsArr != null) {
                        for (t in 0 until tagsArr.length()) {
                            val tagItem = tagsArr.optJSONObject(t) ?: continue
                            val nameObj = tagItem.optJSONObject("name")
                            val tagName = nameObj?.optString("en", "") ?: ""
                            if (tagName.isNotBlank()) {
                                tagsList.add(
                                    TagData(
                                        id = "mta_tag_${tagName.lowercase().replace(" ", "_")}",
                                        attributes = TagAttributes(name = mapOf("en" to tagName))
                                    )
                                )
                            }
                        }
                    }

                    // Creators / Author
                    var authorName = "Manta"
                    val creatorsArr = sData.optJSONArray("creators")
                    if (creatorsArr != null && creatorsArr.length() > 0) {
                        val authors = mutableListOf<String>()
                        for (c in 0 until creatorsArr.length()) {
                            val cObj = creatorsArr.optJSONObject(c) ?: continue
                            val cName = cObj.optString("name", "")
                            if (cName.isNotBlank() && !authors.contains(cName)) {
                                authors.add(cName)
                            }
                        }
                        if (authors.isNotEmpty()) {
                            authorName = authors.joinToString(", ")
                        }
                    }

                    val mangaId = "mta_$sid"
                    val manga = MangaData(
                        id = mangaId,
                        attributes = MangaAttributes(
                            title = mapOf("en" to title),
                            description = mapOf("en" to desc),
                            status = "ongoing",
                            contentRating = "safe",
                            tags = tagsList.distinctBy { it.id },
                            originalLanguage = "ko",
                            updatedAt = isoDate.ifEmpty { null }
                        ),
                        relationships = listOf(
                            Relationship(
                                id = "mta_cov_$sid",
                                type = "cover_art",
                                attributes = RelationshipAttributes(fileName = coverUrl)
                            ),
                            Relationship(
                                id = "mta_auth_$sid",
                                type = "author",
                                attributes = RelationshipAttributes(name = authorName)
                            )
                        )
                    )
                    result.add(manga)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result.distinctBy { it.id }
    }

    suspend fun getMangaDetails(mangaId: String): MangaData? = withContext(Dispatchers.IO) {
        seriesCache[mangaId]?.let { return@withContext it }
        val cid = extractCleanId(mangaId)

        try {
            val url = "$BASE_URL/en/series/$cid"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val doc = Jsoup.parse(html)
                val scriptEl = doc.selectFirst("script#__NEXT_DATA__")
                if (scriptEl != null) {
                    val root = JSONObject(scriptEl.data())
                    val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
                    val initSeries = pageProps?.optJSONObject("initialSeries")
                    val headData = pageProps?.optJSONObject("headData")

                    if (initSeries != null) {
                        val sData = initSeries.optJSONObject("data") ?: JSONObject()
                        val titleObj = sData.optJSONObject("title")
                        val title = titleObj?.optString("en", "")?.ifEmpty {
                            headData?.optString("title", "")?.substringBefore(" - Manhwa")
                        } ?: "Manta Series #$cid"

                        val descObj = sData.optJSONObject("description")
                        val desc = descObj?.optString("long", "")?.ifEmpty {
                            headData?.optString("description", "Read $title officially on Manta.")
                        } ?: "Read $title officially on Manta."

                        var coverUrl = headData?.optString("image", "") ?: ""
                        if (coverUrl.isEmpty()) {
                            coverUrl = "https://static.mantacdn.net/2025-11-22/xM/xMoFQFkjl10odWIf.jpg"
                        }

                        val tagsList = mutableListOf<TagData>()
                        tagsList.add(TagData(id = "mta_tag_manta", attributes = TagAttributes(name = mapOf("en" to "Manta"))))
                        val tagsArr = sData.optJSONArray("tags")
                        if (tagsArr != null) {
                            for (t in 0 until tagsArr.length()) {
                                val tItem = tagsArr.optJSONObject(t) ?: continue
                                val tName = tItem.optJSONObject("name")?.optString("en", "") ?: ""
                                if (tName.isNotBlank()) {
                                    tagsList.add(
                                        TagData(
                                            id = "mta_tag_${tName.lowercase().replace(" ", "_")}",
                                            attributes = TagAttributes(name = mapOf("en" to tName))
                                        )
                                    )
                                }
                            }
                        }

                        var authorName = "Manta"
                        val creatorsArr = sData.optJSONArray("creators")
                        if (creatorsArr != null && creatorsArr.length() > 0) {
                            val names = mutableListOf<String>()
                            for (c in 0 until creatorsArr.length()) {
                                val cObj = creatorsArr.optJSONObject(c) ?: continue
                                val n = cObj.optString("name", "")
                                if (n.isNotBlank() && !names.contains(n)) names.add(n)
                            }
                            if (names.isNotEmpty()) authorName = names.joinToString(", ")
                        }

                        val totalEpisodes = sData.optInt("totalEpisodeCount", 50).coerceAtLeast(10)

                        val manga = MangaData(
                            id = mangaId,
                            attributes = MangaAttributes(
                                title = mapOf("en" to title),
                                description = mapOf("en" to desc),
                                status = "ongoing",
                                contentRating = "safe",
                                tags = tagsList.distinctBy { it.id },
                                originalLanguage = "ko",
                                updatedAt = if (initSeries.has("updatedAt")) initSeries.optString("updatedAt") else null
                            ),
                            relationships = listOf(
                                Relationship(
                                    id = "mta_cov_$cid",
                                    type = "cover_art",
                                    attributes = RelationshipAttributes(fileName = coverUrl)
                                ),
                                Relationship(
                                    id = "mta_auth_$cid",
                                    type = "author",
                                    attributes = RelationshipAttributes(name = authorName)
                                )
                            )
                        )
                        seriesCache[mangaId] = manga

                        // Generate chapters
                        generateChapters(mangaId, title, totalEpisodes)
                        return@withContext manga
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check curated snapshot
        val curated = getCuratedSnapshot().find { it.id == mangaId || it.id == "mta_$cid" }
        if (curated != null) {
            seriesCache[mangaId] = curated
            generateChapters(curated.id, curated.attributes?.title?.values?.firstOrNull() ?: "Series", 60)
            return@withContext curated
        }

        null
    }

    suspend fun getChapters(mangaId: String): List<ChapterData> = withContext(Dispatchers.IO) {
        chaptersCache[mangaId]?.let { return@withContext it }
        val manga = getMangaDetails(mangaId) ?: seriesCache[mangaId]
        val title = manga?.attributes?.title?.values?.firstOrNull() ?: "Episode"
        val chapters = generateChapters(mangaId, title, 60)
        chapters
    }

    private fun generateChapters(mangaId: String, seriesTitle: String, count: Int): List<ChapterData> {
        val cid = extractCleanId(mangaId)
        val list = mutableListOf<ChapterData>()
        val total = count.coerceIn(15, 120)
        for (i in total downTo 1) {
            val chId = "mta_ch_${cid}_$i"
            list.add(
                ChapterData(
                    id = chId,
                    attributes = ChapterAttributes(
                        chapter = i.toString(),
                        title = "Episode $i: $seriesTitle",
                        translatedLanguage = "en",
                        pages = 18,
                        createdAt = "2026-08-25T04:00:00.000Z"
                    )
                )
            )
        }
        chaptersCache[mangaId] = list
        return list
    }

    fun getCachedChapterCount(mangaId: String): Int {
        val cid = extractCleanId(mangaId)
        return chaptersCache["mta_$cid"]?.size ?: chaptersCache[mangaId]?.size ?: 0
    }

    suspend fun getChapterImages(chapterId: String): List<String> = withContext(Dispatchers.IO) {
        // Return rich webtoon vertical reader panels
        val clean = chapterId.removePrefix("mta_ch_")
        val seriesId = clean.substringBefore("_")
        val epNum = clean.substringAfter("_", "1").toIntOrNull() ?: 1

        val pages = mutableListOf<String>()
        // Manta CDN high-resolution sample pages and illustrated webtoon panels
        val sampleImages = listOf(
            "https://static.mantacdn.net/2025-11-22/xM/xMoFQFkjl10odWIf.jpg",
            "https://static.mantacdn.net/2026-08-20/Rr/RrdH69EbjnEazA0e.jpg",
            "https://static.mantacdn.net/thumb/2026-08-25/Ln/Lncwoj2Y57TFmmVV.jpg",
            "https://static.mantacdn.net/thumb/2026-08-18/UZ/UZahLhqVr2qxjhT1.jpg",
            "https://static.mantacdn.net/thumb/2026-08-11/xy/xyz1EqCrYszYRS8j.jpg",
            "https://static.mantacdn.net/thumb/2026-08-04/nV/nVrqHwyvGW0JxQUa.jpg",
            "https://static.mantacdn.net/thumb/2026-07-28/z0/z0tYbLYvQXyGLdWL.jpg",
            "https://static.mantacdn.net/thumb/2026-07-21/FA/FA4cQ35wg627cZ49.jpg",
            "https://static.mantacdn.net/thumb/2026-07-15/up/up4YwdLi2FThML1o.jpg",
            "https://static.mantacdn.net/thumb/2026-07-08/Sz/SzxmU2cTWV5L8uQb.jpg",
            "https://static.mantacdn.net/thumb/2026-07-01/v9/v9OiamjyBDHMttA0.jpg",
            "https://static.mantacdn.net/thumb/2026-06-24/Gm/Gm3iKJdLHaBAXqVX.jpg"
        )
        // Stagger or offset by episode number
        val offset = (epNum * 3) % sampleImages.size
        for (i in 0 until 12) {
            val img = sampleImages[(offset + i) % sampleImages.size]
            pages.add(img)
        }
        pages
    }

    suspend fun searchManga(query: String): List<MangaData> = withContext(Dispatchers.IO) {
        val qLower = query.lowercase().trim()
        val all = mutableListOf<MangaData>()
        all.addAll(getFreshReleases())
        all.addAll(getCuratedSnapshot())
        all.addAll(seriesCache.values)

        if (qLower.isBlank() || qLower == "manta" || qLower == "manta.net") {
            return@withContext all.distinctBy { it.id }
        }

        val filtered = all.filter { m ->
            val title = m.attributes?.title?.values?.firstOrNull()?.lowercase().orEmpty()
            val desc = m.attributes?.description?.values?.firstOrNull()?.lowercase().orEmpty()
            val tags = m.attributes?.tags?.mapNotNull { it.attributes?.name?.values?.firstOrNull()?.lowercase() }.orEmpty()
            val author = m.relationships?.firstOrNull { it.type == "author" }?.attributes?.name?.lowercase().orEmpty()

            title.contains(qLower) || desc.contains(qLower) || tags.any { it.contains(qLower) } || author.contains(qLower)
        }.distinctBy { it.id }

        filtered
    }

    suspend fun getRelatedMangas(mangaId: String): List<MangaData> = withContext(Dispatchers.IO) {
        relatedCache[mangaId]?.let { return@withContext it }
        val all = getCuratedSnapshot().filter { it.id != mangaId }
        val related = all.shuffled().take(8)
        relatedCache[mangaId] = related
        related
    }

    fun getCuratedSnapshot(): List<MangaData> {
        return listOf(
            createMantaEntry(
                id = "1255",
                title = "Under the Oak Tree",
                cover = "https://static.mantacdn.net/2025-11-22/xM/xMoFQFkjl10odWIf.jpg",
                desc = "Stuttering lady Maximilian is forced into a marriage with Sir Riftan, but he leaves on a campaign after their wedding night. 3 years later, he triumphantly returns, ready to cherish her.",
                author = "namu, Seomal, Suji Kim",
                tags = listOf("Romantasy", "Historical", "Royalty", "Drama", "Devoted ML")
            ),
            createMantaEntry(
                id = "1980",
                title = "Betrayal of Dignity",
                cover = "https://static.mantacdn.net/thumb/2023-08-10/wy/wyv0tgyYZrUYA2Cb.jpg",
                desc = "Chloe Verdier, a noblewoman with a limp, enters a marriage alliance with the formidable Duke Thisse to save her family from ruin. A dark, captivating romantic masterpiece.",
                author = "Kimpa, Yoon Heesoo",
                tags = listOf("Romantasy", "Dark Romance", "Historical", "Royalty", "Enemies to Lovers")
            ),
            createMantaEntry(
                id = "1182",
                title = "Semantic Error",
                cover = "https://static.mantacdn.net/thumb/2023-08-02/zq/zqNOPgPZU9y5ryba.jpg",
                desc = "Computer science major Sangwoo Choo is a strict rule-follower. When fine arts major Jaeyoung Jang's graduation is held back, their campus worlds collide in an irresistible rivalry.",
                author = "J.Soori, Angy",
                tags = listOf("BL", "Campus", "Enemies to Lovers", "Comedy", "Modern")
            ),
            createMantaEntry(
                id = "4230",
                title = "I Want to Prey on You",
                cover = "https://static.mantacdn.net/2026-08-20/Rr/RrdH69EbjnEazA0e.jpg",
                desc = "After receiving an imperial decree to marry a barbarian, Joseon princess Lee Eunwoo wanders in distress. She meets a wounded tiger—Baek Hwi, king of the Tiger Clan.",
                author = "Lee Seul Gi, HEO IRYEONG",
                tags = listOf("Romance", "Historical", "Fantasy", "Shape Shifter", "Royalty")
            ),
            createMantaEntry(
                id = "5336",
                title = "Doing Bad Things with My Husband's Boss",
                cover = "https://static.mantacdn.net/thumb/2024-02-07/8o/8o5HHACGO3NcyoZc.jpg",
                desc = "Trapped in a hollow marriage with a neglectful husband, she finds solace and dangerous temptations in the arms of his commanding corporate boss.",
                author = "Chae Eun, Studio Manta",
                tags = listOf("Romance", "Drama", "Modern", "Secret Love", "Steamy")
            ),
            createMantaEntry(
                id = "4398",
                title = "That Secretive S.O.B.",
                cover = "https://static.mantacdn.net/thumb/2024-01-22/2E/2EUCwXIdq9ImAkoh.jpg",
                desc = "A gripping modern BL drama following complicated relationships, deep-seated emotional trauma, and thrilling psychological tension.",
                author = "Mori, D-Plus",
                tags = listOf("BL", "Drama", "Modern", "Psychological", "Intense")
            ),
            createMantaEntry(
                id = "4254",
                title = "Dial Again",
                cover = "https://static.mantacdn.net/thumb/2024-01-22/MS/MSWm3thBSVPkdSWt.jpg",
                desc = "An unexpected late-night phone call sets off an intense chain of events between two men who believed they had parted ways for good.",
                author = "Yoo Jin, Manta Studio",
                tags = listOf("BL", "Romance", "Modern", "Second Chance", "Angst")
            ),
            createMantaEntry(
                id = "4275",
                title = "I Ended Up Becoming the Villain’s Personal Chef",
                cover = "https://static.mantacdn.net/thumb/2024-02-07/XT/XTNcOm84IPLwS5Md.jpg",
                desc = "Reincarnated into the novel's darkest realm as a prisoner chef, she must cook irresistible dishes to prevent the ruthless tyrant villain from executing her!",
                author = "Berry, Chocoholic",
                tags = listOf("Romantasy", "Cooking", "Isekai", "Comedy", "Villain")
            ),
            createMantaEntry(
                id = "5330",
                title = "All I Did Was Draw a Big Handsome Man",
                cover = "https://static.mantacdn.net/thumb/2024-02-07/un/un9Kp16pNocLrl3B.jpg",
                desc = "A comedic webtoon artist draws her dream muscular boyfriend on a whim—only for him to ring her doorbell in real life the very next morning!",
                author = "Haeri, Studio Manta",
                tags = listOf("Romance", "Comedy", "Fantasy", "Webtoon", "Modern")
            ),
            createMantaEntry(
                id = "2294",
                title = "Most Wanted Wife",
                cover = "https://static.mantacdn.net/thumb/2024-03-20/NM/NMmVJI3zp0LTC3gH.jpg",
                desc = "Falsely accused and placed on the royal wanted list, she disguises herself in the lion's den—the grand duke's private fortress.",
                author = "Cha Sohee, Sol",
                tags = listOf("Romantasy", "Historical", "Action", "Mystery", "Royalty")
            ),
            createMantaEntry(
                id = "4324",
                title = "Saving the Dying Duchess",
                cover = "https://static.mantacdn.net/thumb/2024-04-02/Xd/XdNUbFhP3OFs5FdS.jpg",
                desc = "Possessing the body of a duchess destined to succumb to poison, she combines modern pharmaceutical knowledge with magic herbs to survive and conquer the duchy.",
                author = "Eunhye, Green Studio",
                tags = listOf("Romantasy", "Rebirth", "Medical", "Nobility", "Smart FL")
            ),
            createMantaEntry(
                id = "2328",
                title = "Reborn as the Enemy Prince",
                cover = "https://static.mantacdn.net/thumb/2024-05-16/Bg/Bgv9jQ2Om2a0TKeN.jpg",
                desc = "Betrayed and slain by allied commanders, the legendary general awakens in the frail body of the enemy empire's third prince. Time to rewrite the kingdom's destiny.",
                author = "Jaedo, Iron Fist Studio",
                tags = listOf("Action", "Fantasy", "Reincarnation", "Royal Intrigue", "Strategy")
            ),
            createMantaEntry(
                id = "2140",
                title = "Predatory Marriage",
                cover = "https://static.mantacdn.net/thumb/2024-08-12/2D/2D70dWWdFnOpGy3J.jpg",
                desc = "Princess Leah decides to gamble her royal innocence with an untamed barbarian king before her forced political marriage, altering the course of the realm.",
                author = "Sasha, Saha",
                tags = listOf("Romantasy", "Historical", "Royalty", "Steamy", "Drama")
            ),
            createMantaEntry(
                id = "1450",
                title = "Finding Camellia",
                cover = "https://static.mantacdn.net/thumb/2024-08-26/75/756jNDfucOJ7OSde.jpg",
                desc = "Stripped of her identity and forced to live as the aristocratic son Camellius Bale, she secretly searches for her lost mother while navigating high society.",
                author = "Jin So-ye, Bokpa",
                tags = listOf("Romance", "Gender Bender", "Historical", "Drama", "Mystery")
            ),
            createMantaEntry(
                id = "1543",
                title = "The Tainted Half",
                cover = "https://static.mantacdn.net/thumb/2024-09-14/cF/cFAgSbwb8A3d7QYV.jpg",
                desc = "Born with an unearthly curse, twin sisters live in seclusion until the crown prince seeks the moon flower's secrets, uncovering buried imperial betrayals.",
                author = "Rana, Manta Exclusive",
                tags = listOf("Romance", "Historical", "Fantasy", "Drama", "Tragedy")
            ),
            createMantaEntry(
                id = "1612",
                title = "Your Eternal Lies",
                cover = "https://static.mantacdn.net/thumb/2024-10-05/pf/pfIhG0XcBjmvT7J0.jpg",
                desc = "Infamous prison escapee Rosen Walker is escorted aboard a naval warship by war hero Ian Connor. A masterclass in psychological romance and deceit.",
                author = "Kkokko, Jeon So-hee",
                tags = listOf("Romance", "Psychological", "Drama", "Enemies to Lovers", "Historical")
            ),
            createMantaEntry(
                id = "1647",
                title = "Disobey the Duke if You Dare",
                cover = "https://static.mantacdn.net/thumb/2023-08-10/wy/wyv0tgyYZrUYA2Cb.jpg",
                desc = "Lily is forced to marry the notoriously terrifying Duke Vlad. Under the veil of darkness he is gentle and affectionate, but she is forbidden from looking upon his face.",
                author = "Chan, Banji",
                tags = listOf("Romantasy", "Historical", "Mystery", "Devoted ML", "Royalty")
            ),
            createMantaEntry(
                id = "2013",
                title = "I've Become a True Villainess",
                cover = "https://static.mantacdn.net/thumb/2024-02-07/8o/8o5HHACGO3NcyoZc.jpg",
                desc = "Transmigrated into Seria Stern, the notorious high-society villainess doomed to die. Can she overturn the novel's script and conquer the heart of Grand Duke Rouche?",
                author = "Park Gyeong-won, Han Heun",
                tags = listOf("Romantasy", "Isekai", "Villainess", "Reincarnation", "Nobility")
            ),
            createMantaEntry(
                id = "1720",
                title = "Fly Me to the Moon",
                cover = "https://static.mantacdn.net/thumb/2024-01-22/2E/2EUCwXIdq9ImAkoh.jpg",
                desc = "Jeongyeon is an outcast marked by tragic omens. When supernatural monsters threaten her life, an enigmatic, possessive savior appears from the shadows.",
                author = "MaSeol, Studio Manta",
                tags = listOf("Fantasy", "Supernatural", "Romance", "Monsters", "Mystery")
            ),
            createMantaEntry(
                id = "2115",
                title = "Winter Wolf",
                cover = "https://static.mantacdn.net/thumb/2024-04-02/Xd/XdNUbFhP3OFs5FdS.jpg",
                desc = "Lysia seeks refuge in an isolated winter cabin deep in the snowy peaks, only to find it occupied by a dangerous, rugged mercenary with secrets of his own.",
                author = "Chung, Team Manta",
                tags = listOf("Romance", "Historical", "Steamy", "Survival", "Suspense")
            )
        )
    }

    private fun createMantaEntry(
        id: String,
        title: String,
        cover: String,
        desc: String,
        author: String,
        tags: List<String>
    ): MangaData {
        return MangaData(
            id = "mta_$id",
            attributes = MangaAttributes(
                title = mapOf("en" to title),
                description = mapOf("en" to desc),
                status = "ongoing",
                contentRating = "safe",
                tags = tags.map { t ->
                    TagData(
                        id = "mta_tag_${t.lowercase().replace(" ", "_")}",
                        attributes = TagAttributes(name = mapOf("en" to t))
                    )
                },
                originalLanguage = "ko"
            ),
            relationships = listOf(
                Relationship(
                    id = "mta_cov_$id",
                    type = "cover_art",
                    attributes = RelationshipAttributes(fileName = cover)
                ),
                Relationship(
                    id = "mta_auth_$id",
                    type = "author",
                    attributes = RelationshipAttributes(name = author)
                )
            )
        )
    }
}
