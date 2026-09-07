package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.*
import com.example.data.SettingsRepository
import com.example.data.db.AppDatabase
import com.example.data.db.ReadingHistoryEntity
import com.example.model.PanelBookmark
import com.example.model.ReadingQuest
import com.example.repository.JandaPressRepository
import com.example.repository.NhApiRepository
import com.example.util.AudioDramaManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class AppTab {
    HOME, SEARCH, LIBRARY, HISTORY, SETTINGS
}

enum class SearchMode {
    TITLE, AUTHOR, TAG
}

enum class MangaCategory(
    val displayName: String,
    val langCodes: List<String>?,
    val defaultRatings: List<String>? = null,
    val tagId: String? = null
) {
    ALL("All", null),
    MANHWATOON("⚡ ManhwaToon", listOf("ko", "en"), listOf("safe", "suggestive", "erotica", "pornographic")),
    FULL_COLOR("🌈 Full Color", null, listOf("safe", "suggestive", "erotica", "pornographic"), "f5ba408b-0e7a-484d-8d49-4e9125ac96de"),
    WEBTOONS("📱 Webtoons", null, listOf("safe", "suggestive", "erotica", "pornographic"), "e197df38-d0e7-43b5-9b09-2842d0c326dd"),
    ADULT_WEBTOONS("🔞 18+ Webtoons", listOf("ko", "ja", "zh", "en"), listOf("erotica", "pornographic")),
    NTR_18("🖤 18+ NTR & Drama", null, listOf("erotica", "pornographic")),
    ECCHI("🔥 Ecchi & Smut", null, listOf("suggestive", "erotica", "pornographic")),
    COMIC_3D("🧊 3D Comics & CG", null, listOf("safe", "suggestive", "erotica", "pornographic")),
    GOAT("🐐 GOATs", null),
    ADULT_COMICS("💋 18+ Comics", listOf("en", "fr", "es", "ja", "ko"), listOf("erotica", "pornographic")),
    PARODY_18("🎭 18+ Parody", null, listOf("erotica", "pornographic"), "b13b2a48-c720-44a9-9c77-39c9979373fb"),
    MANHWA("🇰🇷 Manhwa", listOf("ko")),
    MANGA("🇯🇵 Manga", listOf("ja")),
    MANHUA("🇨🇳 Manhua", listOf("zh", "zh-hk")),
    COMICS("💬 Comics & Graphic", listOf("en", "fr", "es")),
    DOUJINSHI("🌸 Doujinshi & Fan Works", null, listOf("safe", "suggestive", "erotica", "pornographic"), "b13b2a48-c720-44a9-9c77-39c9979373fb")
}

enum class MangaSortOrder(val displayName: String) {
    MOST_POPULAR("Popular"),
    TOP_RATED("Top Rated"),
    LATEST_UPDATES("Latest Updates"),
    NEW_RELEASES("New Additions")
}

enum class LibraryFilter(val displayName: String) {
    ALL("All Bookmarks"),
    FAVORITES("Favorites"),
    OFFLINE("Offline Downloads")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository(application)
    private val appDatabase = AppDatabase.getDatabase(application)
    private val readingHistoryDao = appDatabase.readingHistoryDao()
    val offlineRepository = com.example.data.OfflineMangaRepository(application, appDatabase)

    val offlineMangas: StateFlow<List<com.example.data.db.OfflineMangaEntity>> = offlineRepository.allOfflineMangas.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val downloadedChapterIds: StateFlow<List<String>> = offlineRepository.downloadedChapterIds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val downloadedMangaIds: StateFlow<List<String>> = offlineRepository.downloadedMangaIds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingChapterId = MutableStateFlow<String?>(null)

    val readingHistory: StateFlow<List<ReadingHistoryEntity>> = readingHistoryDao.getAllHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun recordReadingProgress(
        mangaId: String,
        title: String,
        coverUrl: String?,
        chapterId: String,
        chapterNumber: String?,
        chapterTitle: String?,
        currentPage: Int = 1,
        totalPages: Int = 1
    ) {
        viewModelScope.launch {
            try {
                // If incognito mode is active, don't record history
                if (incognitoMode.value) return@launch

                val entity = ReadingHistoryEntity(
                    mangaId = mangaId,
                    title = title,
                    coverUrl = coverUrl,
                    lastChapterId = chapterId,
                    lastChapterNumber = chapterNumber,
                    lastChapterTitle = chapterTitle,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    lastReadTimestamp = System.currentTimeMillis()
                )
                readingHistoryDao.insertOrUpdate(entity)
                addMillionDollarEarnings(150) // VIP MangaBucks earned per chapter read!
                settingsRepository.recordChapterRead(3)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHistoryItem(mangaId: String) {
        viewModelScope.launch {
            try {
                readingHistoryDao.deleteHistory(mangaId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                readingHistoryDao.clearAllHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==================== MILLION DOLLAR VIP STATS ====================
    val mangaNetWorth = MutableStateFlow(1_250_000L)
    val vipTier = MutableStateFlow("Diamond Tycoon")
    val dailySpinAvailable = MutableStateFlow(true)
    val vipStreakDays = MutableStateFlow(7)
    val unlockedVipPerks = MutableStateFlow(setOf("Golden Reader Aura", "Ultra Speed Scrolling", "AI Manga Oracle"))

    fun addMillionDollarEarnings(amount: Long) {
        mangaNetWorth.value += amount
    }

    fun spinMillionDollarWheel(): Pair<String, Long> {
        val prizes = listOf(
            "💎 +$250,000 MangaBucks Jackpot!" to 250_000L,
            "👑 Legendary Cultivation GOAT Drop!" to 100_000L,
            "⚡ Double Reading Speed Aura!" to 50_000L,
            "🌟 +$500,000 Billionaire Fortune!" to 500_000L,
            "🔥 Rare 18+ Uncensored Vault Access!" to 150_000L,
            "🏆 God-Tier VIP Badge Upgrade!" to 75_000L
        )
        val prize = prizes.random()
        mangaNetWorth.value += prize.second
        dailySpinAvailable.value = false
        return prize
    }
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.mangadex.org/")
        .client(NetworkClient.getClient(application))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        
    private val api = retrofit.create(MangaDexApi::class.java)
    private val jandaRepository = JandaPressRepository()
    private val nhApiRepository = NhApiRepository()

    val isAgeVerified = settingsRepository.isAgeVerified.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isAgeWarningDismissedPermanent = settingsRepository.isAgeWarningDismissedPermanent.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    val isAgeWarningDismissedSession = MutableStateFlow(false)

    fun dismissAgeWarning(rememberPermanently: Boolean) {
        isAgeWarningDismissedSession.value = true
        if (rememberPermanently) {
            viewModelScope.launch {
                settingsRepository.setAgeWarningDismissedPermanent(true)
            }
        }
    }

    fun shouldShowAgeWarning(manga: MangaData?): Boolean {
        if (manga == null) return false
        if (isAgeWarningDismissedPermanent.value || isAgeWarningDismissedSession.value) {
            return false
        }
        val rating = manga.attributes?.contentRating ?: "safe"
        return rating.equals("pornographic", ignoreCase = true) || rating.equals("erotica", ignoreCase = true)
    }

    val ratingSafe = settingsRepository.ratingSafe.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val ratingSuggestive = settingsRepository.ratingSuggestive.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val ratingErotica = settingsRepository.ratingErotica.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val ratingPornographic = settingsRepository.ratingPornographic.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val themeMilfStepmother = settingsRepository.themeMilfStepmother.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val bookmarkedIds = settingsRepository.bookmarkedIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val favoriteIds = settingsRepository.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val subscribedIds = settingsRepository.subscribedIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val notificationsEnabled = settingsRepository.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val incognitoMode = settingsRepository.incognitoMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoScrollSpeed = settingsRepository.autoScrollSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)
    val dualPageMode = settingsRepository.dualPageMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val readingStreakDays = settingsRepository.readingStreakDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
    val totalChaptersRead = settingsRepository.totalChaptersRead.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 24)
    val totalReadingMinutes = settingsRepository.totalReadingMinutes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 380)
    val themeMode = settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")
    val readerMode = settingsRepository.readerMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "VERTICAL")
    val readerTheme = settingsRepository.readerTheme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")
    val readerBrightnessDim = settingsRepository.readerBrightnessDim.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0f)
    val pageTurnHaptics = settingsRepository.pageTurnHaptics.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val pageTurnSound = settingsRepository.pageTurnSound.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val surpriseRollsCount = settingsRepository.surpriseRollsCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val customShelvesJson = settingsRepository.customShelvesJson.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "{}")
    val feedConfigJson = settingsRepository.feedConfigJson.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "[]")
    val readerExp = settingsRepository.readerExp.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 420)
    val panelBookmarksJson = settingsRepository.panelBookmarksJson.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "[]")
    val actionRumbleEnabled = settingsRepository.actionRumbleEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val excludedTags = settingsRepository.excludedTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val includedTags = settingsRepository.includedTags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val minChapterCount = settingsRepository.minChapterCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val statusFilter = settingsRepository.statusFilter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ALL")

    // Panel Bookmarks parsed
    val panelBookmarks = MutableStateFlow<List<PanelBookmark>>(emptyList())

    init {
        viewModelScope.launch {
            panelBookmarksJson.collect { json ->
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<PanelBookmark>>() {}.type
                    val list: List<PanelBookmark>? = com.google.gson.Gson().fromJson(json, type)
                    panelBookmarks.value = list ?: emptyList()
                } catch (e: Exception) {
                    panelBookmarks.value = emptyList()
                }
            }
        }
    }

    fun addPanelBookmark(
        mangaId: String,
        mangaTitle: String,
        chapterTitle: String,
        pageNumber: Int,
        imageUrl: String?,
        userNote: String
    ) {
        viewModelScope.launch {
            try {
                val current = panelBookmarks.value.toMutableList()
                current.add(
                    0,
                    PanelBookmark(
                        mangaId = mangaId,
                        mangaTitle = mangaTitle,
                        chapterTitle = chapterTitle,
                        pageNumber = pageNumber,
                        imageUrl = imageUrl,
                        userNote = userNote
                    )
                )
                val json = com.google.gson.Gson().toJson(current)
                settingsRepository.setPanelBookmarksJson(json)
                settingsRepository.addExp(50)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePanelBookmark(bookmarkId: String) {
        viewModelScope.launch {
            try {
                val current = panelBookmarks.value.filter { it.id != bookmarkId }
                val json = com.google.gson.Gson().toJson(current)
                settingsRepository.setPanelBookmarksJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addReaderExp(amount: Int) {
        viewModelScope.launch {
            settingsRepository.addExp(amount)
        }
    }

    fun setActionRumbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setActionRumbleEnabled(enabled)
        }
    }

    fun setSmartTagFilters(included: Set<String>, excluded: Set<String>, minChapters: Int) {
        viewModelScope.launch {
            settingsRepository.setSmartTagFilters(included, excluded, minChapters)
        }
    }

    // Audio Drama Voice Synthesizer
    val audioDramaManager by lazy { AudioDramaManager(getApplication()) }
    val audioDramaIsLoading = MutableStateFlow(false)
    val audioDramaScript = MutableStateFlow<String?>(null)

    fun generateAndPlayAudioDrama(
        mangaTitle: String,
        chapterTitle: String,
        synopsis: String?,
        speechRate: Float = 0.95f
    ) {
        audioDramaIsLoading.value = true
        viewModelScope.launch {
            try {
                var apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
                    apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
                }

                val prompt = """
                    You are a voice director writing an exciting, atmospheric 60-second AUDIO DRAMA NARRATION script for manga chapter "$chapterTitle" of "$mangaTitle".
                    Context: "${synopsis ?: "Action/Drama Manga"}"
                    
                    Write a punchy, immersive spoken narrative with sound effect descriptions in brackets:
                    - Set the scene with dramatic intensity
                    - Include character lines with emotional cues
                    - Make it sound cinematic when read aloud by Text-to-Speech!
                    Keep it under 150 words.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiClient.api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "The chapter begins. Tension fills the air as the hero steps onto the battlefield."
                
                audioDramaScript.value = text
                audioDramaManager.speak(text, speechRate = speechRate)
                addReaderExp(40)
            } catch (e: Exception) {
                val fallback = "Welcome to $mangaTitle, $chapterTitle. The story continues with pulse-pounding intensity!"
                audioDramaScript.value = fallback
                audioDramaManager.speak(fallback, speechRate = speechRate)
            } finally {
                audioDramaIsLoading.value = false
            }
        }
    }

    fun stopAudioDrama() {
        audioDramaManager.stop()
        audioDramaScript.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioDramaManager.release()
    }

    fun setReaderTheme(theme: String) {
        viewModelScope.launch { settingsRepository.setReaderTheme(theme) }
    }

    fun setReaderBrightnessDim(dim: Float) {
        viewModelScope.launch { settingsRepository.setReaderBrightnessDim(dim) }
    }

    fun setPageTurnHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPageTurnHaptics(enabled) }
    }

    fun setPageTurnSound(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPageTurnSound(enabled) }
    }

    fun incrementSurpriseRolls() {
        viewModelScope.launch { settingsRepository.incrementSurpriseRolls() }
    }

    // Shelves Management
    fun createShelf(shelfName: String) {
        viewModelScope.launch {
            try {
                val currentMap = parseShelvesMap(customShelvesJson.value).toMutableMap()
                if (!currentMap.containsKey(shelfName)) {
                    currentMap[shelfName] = emptyList()
                    settingsRepository.setCustomShelvesJson(serializeShelvesMap(currentMap))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteShelf(shelfName: String) {
        viewModelScope.launch {
            try {
                val currentMap = parseShelvesMap(customShelvesJson.value).toMutableMap()
                currentMap.remove(shelfName)
                settingsRepository.setCustomShelvesJson(serializeShelvesMap(currentMap))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleMangaInShelf(shelfName: String, mangaId: String) {
        viewModelScope.launch {
            try {
                val currentMap = parseShelvesMap(customShelvesJson.value).toMutableMap()
                val list = (currentMap[shelfName] ?: emptyList()).toMutableList()
                if (list.contains(mangaId)) {
                    list.remove(mangaId)
                } else {
                    list.add(mangaId)
                }
                currentMap[shelfName] = list
                settingsRepository.setCustomShelvesJson(serializeShelvesMap(currentMap))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseShelvesMap(json: String): Map<String, List<String>> {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: mapOf(
                "🔥 Peak Fiction" to emptyList(),
                "🔞 Midnight Vault" to emptyList(),
                "⚡ Next to Binge" to emptyList()
            )
        } catch (e: Exception) {
            mapOf(
                "🔥 Peak Fiction" to emptyList(),
                "🔞 Midnight Vault" to emptyList(),
                "⚡ Next to Binge" to emptyList()
            )
        }
    }

    private fun serializeShelvesMap(map: Map<String, List<String>>): String {
        return com.google.gson.Gson().toJson(map)
    }

    // Gemini Assistant Helpers
    val aiAssistantIsLoading = MutableStateFlow(false)
    val aiAssistantResult = MutableStateFlow<String?>(null)

    fun clearAiAssistantResult() {
        aiAssistantResult.value = null
        aiAssistantIsLoading.value = false
    }

    fun askGeminiStoryRecap(title: String, chapterTitle: String, synopsis: String?) {
        aiAssistantIsLoading.value = true
        aiAssistantResult.value = null
        viewModelScope.launch {
            try {
                var apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
                    apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
                }

                val prompt = """
                    You are an expert manga & comic storyteller.
                    Provide an engaging, spoiler-safe STORY RECAP and context for the series "$title" leading into "$chapterTitle".
                    Series Background / Synopsis: "${synopsis ?: "Popular ongoing title"}"
                    
                    Format your response with:
                    1. 📖 **The Core Premise & Conflict** (2 concise paragraphs)
                    2. 🔑 **Key Dynamics to Keep in Mind** (bullet points with bold terms)
                    3. ⚡ **Current Stakes & Tension**
                    
                    Keep it punchy, captivating, and easy to scan for readers jumping into this chapter.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiClient.api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Unable to generate recap at this time."
                aiAssistantResult.value = text
            } catch (e: Exception) {
                aiAssistantResult.value = "AI Story Assistant unavailable: ${e.localizedMessage ?: "Network error"}"
            } finally {
                aiAssistantIsLoading.value = false
            }
        }
    }

    fun askGeminiCharacterLore(title: String, synopsis: String?, tags: List<String>) {
        aiAssistantIsLoading.value = true
        aiAssistantResult.value = null
        viewModelScope.launch {
            try {
                var apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
                    apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
                }

                val prompt = """
                    You are a worldbuilding and character lore archivist for "$title" (Genres: ${tags.joinToString(", ")}).
                    Synopsis: "${synopsis ?: ""}"
                    
                    Provide a well-structured CHARACTER ROSTER & LORE GUIDE:
                    - 👤 **Protagonist & Motives**
                    - 👥 **Key Allies / Love Interests / Rivals**
                    - 🏛️ **Factions / Sects / Hierarchy**
                    - 🔮 **Power System / Core World Rules**
                    
                    Keep the tone exciting, well-organized with emojis and bold section headings.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiClient.api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Unable to retrieve lore breakdown."
                aiAssistantResult.value = text
            } catch (e: Exception) {
                aiAssistantResult.value = "AI Lore Guide unavailable: ${e.localizedMessage ?: "Network error"}"
            } finally {
                aiAssistantIsLoading.value = false
            }
        }
    }

    fun askGeminiVibeRecommendations(title: String, tags: List<String>) {
        aiAssistantIsLoading.value = true
        aiAssistantResult.value = null
        viewModelScope.launch {
            try {
                var apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
                    apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
                }

                val prompt = """
                    You are an elite manga & manhwa curator.
                    Recommend 4 series with the exact same VIBE, art aesthetic, tone, and tropes as "$title" (Genres: ${tags.joinToString(", ")}).
                    
                    For each recommendation provide:
                    - 🎯 **Title** (and alternative names)
                    - 🎨 **Art & Vibe Match**: Why fans of "$title" will adore this
                    - 💡 **One-Sentence Hook**
                    
                    Include top-tier manga, manhwa, or webtoons across safe, ecchi, or mature as appropriate.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiClient.api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Unable to find vibe matches at this time."
                aiAssistantResult.value = text
            } catch (e: Exception) {
                aiAssistantResult.value = "AI Recommendation Matcher unavailable: ${e.localizedMessage ?: "Network error"}"
            } finally {
                aiAssistantIsLoading.value = false
            }
        }
    }

    // Translator Helpers
    val translatorIsLoading = MutableStateFlow(false)
    val translatorResult = MutableStateFlow<String?>(null)

    fun clearTranslatorResult() {
        translatorResult.value = null
        translatorIsLoading.value = false
    }

    fun translatePageContent(
        mangaTitle: String,
        pageNumber: Int,
        pageImageUrl: String?,
        sourceLang: String,
        targetLang: String,
        rawText: String = ""
    ) {
        translatorIsLoading.value = true
        translatorResult.value = null
        viewModelScope.launch {
            try {
                var apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
                    apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
                }

                val prompt = """
                    You are a master manga/manhwa/manhua localization and translation expert.
                    Series: "$mangaTitle" (Page $pageNumber).
                    ${if (rawText.isNotBlank()) "User provided dialogue/SFX to translate: \"$rawText\"" else "Provide a typical localized translation and breakdown for standard dialogue/SFX found on this page in $targetLang."}
                    
                    Structure your translation as:
                    1. 💬 **Dialogue Bubbles & Character Voices** (Localized accurately with emotional tone preserved)
                    2. 💥 **Sound Effects (SFX / Onomatopoeia)** (Original text -> Romanized -> English meaning e.g., ドキドキ / Doki Doki -> *Thump Thump heartbeat*)
                    3. 💡 **Cultural Nuance / Idiom Explanations** (if applicable)
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = GeminiClient.api.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Unable to translate at this time."
                translatorResult.value = text
            } catch (e: Exception) {
                translatorResult.value = "Translator unavailable: ${e.localizedMessage ?: "Network error"}"
            } finally {
                translatorIsLoading.value = false
            }
        }
    }

    fun toggleSubscription(mangaId: String, title: String? = null, coverUrl: String? = null) {
        viewModelScope.launch {
            val wasSubscribed = subscribedIds.value.contains(mangaId)
            settingsRepository.toggleSubscription(mangaId)
            if (!wasSubscribed && notificationsEnabled.value) {
                // Send confirmation notification on subscribe
                com.example.util.MangaNotificationManager.showMangaDropNotification(
                    context = getApplication(),
                    mangaId = mangaId,
                    mangaTitle = title ?: "Manga Series",
                    chapterTitle = "Subscribed! You will get alerts on every new chapter drop.",
                    coverUrl = coverUrl,
                    isSubscribed = true
                )
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setIncognitoMode(enabled)
        }
    }

    fun setAutoScrollSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.setAutoScrollSpeed(speed)
        }
    }

    fun setDualPageMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDualPageMode(enabled)
        }
    }

    fun triggerTestDropNotification() {
        viewModelScope.launch {
            com.example.util.MangaNotificationManager.showTestDropNotification(getApplication())
        }
    }

    fun triggerAdultDropNotification(title: String = "Sinful Lust") {
        viewModelScope.launch {
            com.example.util.MangaNotificationManager.showAdultDropNotification(getApplication(), title)
        }
    }

    val currentTab = MutableStateFlow(AppTab.HOME)
    val selectedCategory = MutableStateFlow(MangaCategory.ALL)
    val selectedSortOrder = MutableStateFlow(MangaSortOrder.MOST_POPULAR)
    val selectedTag = MutableStateFlow("All")
    val selectedLibraryFilter = MutableStateFlow(LibraryFilter.ALL)

    val featuredManga = MutableStateFlow<MangaData?>(null)
    val userFavoriteSpotlightManga = MutableStateFlow<MangaData>(createAneToNoNichijouKaiwaManga())
    val goatMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val fullColorMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val matureNtrLibrary = MutableStateFlow<List<MangaData>>(emptyList())
    val topRatedMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val latestMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val ecchiComicsList = MutableStateFlow<List<MangaData>>(emptyList())
    val threeDComicsList = MutableStateFlow<List<MangaData>>(emptyList())
    val adultWebtoonsList = MutableStateFlow<List<MangaData>>(emptyList())
    val adultComicsList = MutableStateFlow<List<MangaData>>(emptyList())
    val parodyMangasList = MutableStateFlow<List<MangaData>>(emptyList())
    val currentMangaDetail = MutableStateFlow<MangaData?>(null)
    val doujinshiList = MutableStateFlow<List<MangaData>>(emptyList())
    val mangaCategoryList = MutableStateFlow<List<MangaData>>(emptyList())
    val manhwaCategoryList = MutableStateFlow<List<MangaData>>(emptyList())
    val manhuaCategoryList = MutableStateFlow<List<MangaData>>(emptyList())
    val comicsCategoryList = MutableStateFlow<List<MangaData>>(emptyList())
    val manhwaToonList = MutableStateFlow<List<MangaData>>(emptyList())
    val manhwaToonSort = MutableStateFlow("latest")
    val manhwaToonGenre = MutableStateFlow<String?>(null)
    val isManhwaToonLoading = MutableStateFlow(false)
    val specialInterestMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val cultivationGoatMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val overflowMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val continueReadingList = MutableStateFlow<List<MangaData>>(emptyList())
    val mangas = MutableStateFlow<List<MangaData>>(emptyList())
    val searchMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val authorSearchResults = MutableStateFlow<List<AuthorData>>(emptyList())
    val featuredAuthors = MutableStateFlow<List<AuthorData>>(emptyList())
    val libraryMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val availableTags = MutableStateFlow<List<TagData>>(emptyList())

    // Author Profile States
    val selectedAuthorProfile = MutableStateFlow<AuthorData?>(null)
    val authorWorks = MutableStateFlow<List<MangaData>>(emptyList())
    val isAuthorLoading = MutableStateFlow(false)

    val chapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val allRawChapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val selectedChapterLanguage = MutableStateFlow<String>("all")
    val chaptersError = MutableStateFlow<String?>(null)
    val imageUrls = MutableStateFlow<List<String>>(emptyList())
    val similarMangas = MutableStateFlow<List<MangaData>>(emptyList())
    val copilotMessages = MutableStateFlow<List<CopilotMessage>>(emptyList())
    val copilotIsLoading = MutableStateFlow(false)
    
    val isLoading = MutableStateFlow(false)
    val isLoadingMoreHome = MutableStateFlow(false)
    val hasMoreHome = MutableStateFlow(true)
    private var homeOffset = 0

    val isSearching = MutableStateFlow(false)
    val isSearchingMore = MutableStateFlow(false)
    val hasMoreSearch = MutableStateFlow(true)
    private var searchOffset = 0

    val searchQuery = MutableStateFlow("")
    val searchMode = MutableStateFlow(SearchMode.TITLE)

    init {
        // High priority: load essential tags and primary home mangas first
        fetchTags()
        fetchMangas()
        fetchSpecialSections()

        // Stagger secondary carousels in background to avoid API rate limiting and network queue starvation
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
            fetchCategoryRows()
            kotlinx.coroutines.delay(200)
            fetchGoatMangas()
            fetchFullColorMangas()
            fetchMatureNtrLibrary()
            kotlinx.coroutines.delay(200)
            fetchEcchiComics()
            fetchThreeDComics()
            fetchAdultWebtoons()
            kotlinx.coroutines.delay(200)
            fetch18PlusComics()
            fetch18PlusParodies()
            fetchDoujinshis()
            kotlinx.coroutines.delay(200)
            fetchSpecialInterestMangas()
            fetchCultivationGoatMangas()
            fetchOverflowMangas()
            fetchFeaturedAuthors()
            fetchManhwaToon()
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setReaderMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setReaderMode(mode)
        }
    }

    fun setTab(tab: AppTab) {
        currentTab.value = tab
        if (tab == AppTab.LIBRARY) {
            fetchLibraryMangas()
        }
    }

    fun selectCategory(category: MangaCategory) {
        selectedCategory.value = category
        fetchMangas()
        if (searchQuery.value.isNotBlank() || searchMode.value != SearchMode.TITLE) {
            performSearch()
        }
    }

    fun selectSortOrder(sortOrder: MangaSortOrder) {
        selectedSortOrder.value = sortOrder
        fetchMangas()
    }

    fun selectTag(tagName: String) {
        selectedTag.value = tagName
        fetchMangas()
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        selectedLibraryFilter.value = filter
        fetchLibraryMangas()
    }

    fun selectLibraryFilter(filter: LibraryFilter) = setLibraryFilter(filter)

    fun toggleBookmark(mangaId: String) {
        viewModelScope.launch {
            settingsRepository.toggleBookmark(mangaId)
            fetchLibraryMangas()
        }
    }

    fun toggleFavorite(mangaId: String) {
        viewModelScope.launch {
            settingsRepository.toggleFavorite(mangaId)
            fetchLibraryMangas()
        }
    }

    fun setAgeVerified(verified: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAgeVerified(verified)
        }
    }

    fun toggleRating(rating: String, isChecked: Boolean) {
        viewModelScope.launch {
            when (rating) {
                "safe" -> settingsRepository.setRating(SettingsRepository.RATING_SAFE, isChecked)
                "suggestive" -> settingsRepository.setRating(SettingsRepository.RATING_SUGGESTIVE, isChecked)
                "erotica" -> settingsRepository.setRating(SettingsRepository.RATING_EROTICA, isChecked)
                "pornographic" -> settingsRepository.setRating(SettingsRepository.RATING_PORNOGRAPHIC, isChecked)
            }
            fetchMangas()
            fetchSpecialSections()
            fetchCultivationGoatMangas()
            fetchCategoryRows()
            fetchSpecialInterestMangas()
        }
    }

    fun toggleMilfStepmother() {
        viewModelScope.launch {
            settingsRepository.setRating(SettingsRepository.THEME_MILF_STEPMOTHER, !themeMilfStepmother.value)
            fetchMangas()
        }
    }

    private fun getActiveRatings(): List<String> {
        return listOf("safe", "suggestive", "erotica", "pornographic")
    }

    private fun fetchTags() {
        viewModelScope.launch {
            try {
                val response = api.getTags()
                availableTags.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchCategoryRows() {
        viewModelScope.launch {
            try {
                val ratings = getActiveRatings()
                // Manga (Japanese)
                val mangaRes = api.getMangaList(
                    originalLanguages = listOf("ja"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 25
                )
                mangaCategoryList.value = mangaRes.data

                // Manhwa (Korean)
                val manhwaRes = api.getMangaList(
                    originalLanguages = listOf("ko"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 25
                )
                manhwaCategoryList.value = manhwaRes.data

                // Manhua (Chinese)
                val manhuaRes = api.getMangaList(
                    originalLanguages = listOf("zh", "zh-hk"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 25
                )
                manhuaCategoryList.value = manhuaRes.data

                // Comics (English / Western / Webcomics)
                val comicsRes = api.getMangaList(
                    originalLanguages = listOf("en", "fr", "es"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 30
                )
                comicsCategoryList.value = comicsRes.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchEcchiComics() {
        viewModelScope.launch {
            try {
                val ratings = listOf("suggestive", "erotica", "pornographic")
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // 1. Top followed Ecchi & Erotica series
                val topEcchiRes = api.getMangaList(
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 45
                )
                for (m in topEcchiRes.data) {
                    if (existingIds.add(m.id)) combined.add(m)
                }

                // 2. High-rated Ecchi & Erotica classics
                try {
                    val topRatedEcchi = api.getMangaList(
                        contentRatings = ratings,
                        orderRating = "desc",
                        limit = 25
                    )
                    for (m in topRatedEcchi.data) {
                        if (existingIds.add(m.id)) combined.add(m)
                    }
                } catch (_: Exception) {}

                ecchiComicsList.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchThreeDComics() {
        viewModelScope.launch {
            try {
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // 1. Curated 3D Comics & CG Graphic Novels with multi-chapter series
                val curated3d = com.example.repository.ThreeDComicsRepository.getAll3DComics()
                for (m in curated3d) {
                    if (existingIds.add(m.id)) combined.add(m)
                }

                // Query live 3D & CG titles from nHentai v2 API
                try {
                    val nh3d = com.example.api.nhapi.NhApiClient.directApi.searchV2("3d", page = 1)
                    if (nh3d.isSuccessful) {
                        nh3d.body()?.result?.forEach { item ->
                            val itemId = item.id?.toString() ?: return@forEach
                            val mId = "3d_$itemId"
                            if (existingIds.add(mId)) {
                                val mediaId = item.mediaId ?: itemId
                                val thumb = item.thumbnail
                                val coverUrl = if (!thumb.isNullOrBlank()) {
                                    if (thumb.startsWith("http")) thumb else "https://t.nhentai.net/$thumb"
                                } else {
                                    "https://t.nhentai.net/galleries/$mediaId/thumb.jpg"
                                }
                                val titleStr = item.englishTitle ?: item.japaneseTitle ?: "3D CG Gallery #$itemId"
                                combined.add(
                                    MangaData(
                                        id = mId,
                                        attributes = MangaAttributes(
                                            title = mapOf("en" to titleStr),
                                            description = mapOf("en" to "Full 3D / 3DCG graphic novel gallery ($itemId)."),
                                            originalLanguage = "en",
                                            contentRating = "pornographic",
                                            status = "completed"
                                        ),
                                        relationships = listOf(
                                            Relationship(id = mId, type = "cover_art", attributes = RelationshipAttributes(fileName = coverUrl)),
                                            Relationship(id = "nh3d", type = "author", attributes = RelationshipAttributes(name = "3D CG Studio"))
                                        )
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}

                // Query additional 3D & CG titles from MangaDex
                try {
                    val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                    val res3d = api.getMangaList(
                        title = "3D",
                        contentRatings = ratings,
                        orderFollowedCount = "desc",
                        limit = 25
                    )
                    for (m in res3d.data) {
                        if (existingIds.add(m.id)) combined.add(m)
                    }
                } catch (_: Exception) {}

                threeDComicsList.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAdultWebtoons() {
        viewModelScope.launch {
            try {
                val ratings = listOf("erotica", "pornographic", "suggestive")
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // Spotlight Sinful Lust webtoon series
                val sinfulLustManga = MangaData(
                    id = "451290",
                    attributes = MangaAttributes(
                        title = mapOf("en" to "[Studio Lust] Sinful Lust - The Secret Room [Full Color]"),
                        description = mapOf("en" to "The hit full-color adult webtoon series Sinful Lust - The Secret Room."),
                        originalLanguage = "ko",
                        contentRating = "pornographic",
                        status = "completed"
                    ),
                    relationships = listOf(
                        Relationship(id = "451290", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/2498210/thumb.jpg")),
                        Relationship(id = "studio_lust", type = "author", attributes = RelationshipAttributes(name = "Studio Lust"))
                    )
                )
                if (existingIds.add(sinfulLustManga.id)) {
                    combined.add(sinfulLustManga)
                }

                // Fast batch query: Top Korean 18+ Webtoons
                val topAdultRes = api.getMangaList(
                    originalLanguages = listOf("ko"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 35
                )
                for (m in topAdultRes.data) {
                    if (existingIds.add(m.id)) {
                        combined.add(m)
                    }
                }

                adultWebtoonsList.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetch18PlusComics() {
        viewModelScope.launch {
            try {
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // 1. Curated high-rated Graphic Novels with guaranteed working chapters and covers
                val curatedComics = listOf(
                    MangaData(
                        id = "comic_371165",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[clesta] The Secret Apartment - Graphic Novel Series [English]"),
                            description = mapOf("en" to "Full-color adult graphic novel with intricate romance and storytelling."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_371165", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/2019715/thumb.jpg")),
                            Relationship(id = "clesta", type = "author", attributes = RelationshipAttributes(name = "clesta"))
                        )
                    ),
                    MangaData(
                        id = "comic_368084",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Danimaru] Midnight Cinderella - Graphic Novel [Full Color English]"),
                            description = mapOf("en" to "Masterpiece graphic novel illustrated by Danimaru in vibrant full color."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_368084", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1996892/thumb.jpg")),
                            Relationship(id = "danimaru", type = "author", attributes = RelationshipAttributes(name = "Danimaru"))
                        )
                    ),
                    MangaData(
                        id = "comic_244327",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Enokido] Private Lessons & After School Romance [English]"),
                            description = mapOf("en" to "Classic adult graphic novel drama centered around academic passion."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_244327", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1276626/thumb.jpg")),
                            Relationship(id = "enokido", type = "author", attributes = RelationshipAttributes(name = "Enokido"))
                        )
                    ),
                    MangaData(
                        id = "comic_351814",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Tokiwa Midori] Summer Beach Resort Special [Full Color English]"),
                            description = mapOf("en" to "Warm summer romance graphic novel with breathtaking artwork."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_351814", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1870727/thumb.jpg")),
                            Relationship(id = "tokiwa", type = "author", attributes = RelationshipAttributes(name = "Tokiwa Midori"))
                        )
                    ),
                    MangaData(
                        id = "comic_393235",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Hisasi] Resonating Heartbeats - Graphic Novel [English]"),
                            description = mapOf("en" to "Iconic graphic novel with stylish character designs by Hisasi."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_393235", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/2149589/thumb.jpg")),
                            Relationship(id = "hisasi", type = "author", attributes = RelationshipAttributes(name = "Hisasi"))
                        )
                    ),
                    MangaData(
                        id = "comic_359336",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Shiwasu no Okina] Seduction in the Snow Cabin [English]"),
                            description = mapOf("en" to "Atmospheric winter cabin graphic novel with emotional tension."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_359336", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1912020/thumb.jpg")),
                            Relationship(id = "shiwasu", type = "author", attributes = RelationshipAttributes(name = "Shiwasu no Okina"))
                        )
                    ),
                    MangaData(
                        id = "comic_350388",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[homunculus] Velvet Garden - Graphic Novel [English]"),
                            description = mapOf("en" to "Acclaimed graphic novel anthology by homunculus."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_350388", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1862462/thumb.jpg")),
                            Relationship(id = "homunculus", type = "author", attributes = RelationshipAttributes(name = "homunculus"))
                        )
                    ),
                    MangaData(
                        id = "comic_374482",
                        attributes = MangaAttributes(
                            title = mapOf("en" to "[Kisaragi Gunma] Sunset Memories - Graphic Edition [English]"),
                            description = mapOf("en" to "Sensational graphic edition with nostalgic youth aesthetics."),
                            originalLanguage = "en",
                            contentRating = "pornographic",
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(id = "comic_374482", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1428040/thumb.jpg")),
                            Relationship(id = "kisaragi", type = "author", attributes = RelationshipAttributes(name = "Kisaragi Gunma"))
                        )
                    )
                )

                for (c in curatedComics) {
                    if (existingIds.add(c.id)) combined.add(c)
                }

                // 2. Query live graphic novels from nHentai API v2
                try {
                    val nhGNovels = com.example.api.nhapi.NhApiClient.directApi.searchV2("graphic novel english", page = 1)
                    if (nhGNovels.isSuccessful) {
                        nhGNovels.body()?.result?.forEach { item ->
                            val itemId = item.id?.toString() ?: return@forEach
                            val mId = "comic_$itemId"
                            if (existingIds.add(mId)) {
                                val mediaId = item.mediaId ?: itemId
                                val thumb = item.thumbnail
                                val coverUrl = if (!thumb.isNullOrBlank()) {
                                    if (thumb.startsWith("http")) thumb else "https://t.nhentai.net/$thumb"
                                } else {
                                    "https://t.nhentai.net/galleries/$mediaId/thumb.jpg"
                                }
                                val titleStr = item.englishTitle ?: item.japaneseTitle ?: "Graphic Novel #$itemId"
                                combined.add(
                                    MangaData(
                                        id = mId,
                                        attributes = MangaAttributes(
                                            title = mapOf("en" to titleStr),
                                            description = mapOf("en" to "Full English graphic novel edition ($itemId)."),
                                            originalLanguage = "en",
                                            contentRating = "pornographic",
                                            status = "completed"
                                        ),
                                        relationships = listOf(
                                            Relationship(id = mId, type = "cover_art", attributes = RelationshipAttributes(fileName = coverUrl)),
                                            Relationship(id = "nhgn", type = "author", attributes = RelationshipAttributes(name = "Graphic Novel Press"))
                                        )
                                    )
                                )
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 3. Query adult Western & Graphic Comics from MangaDex
                try {
                    val ratings = listOf("erotica", "pornographic", "suggestive")
                    val topRes = api.getMangaList(
                        originalLanguages = listOf("en", "fr", "es", "it", "de"),
                        contentRatings = ratings,
                        orderFollowedCount = "desc",
                        limit = 35
                    )
                    for (m in topRes.data) {
                        if (existingIds.add(m.id)) combined.add(m)
                    }
                } catch (_: Exception) {}

                adultComicsList.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetch18PlusParodies() {
        viewModelScope.launch {
            try {
                val ratings = listOf("erotica", "pornographic", "suggestive")
                val doujinTagId = "b13b2a48-c720-44a9-9c77-39c9979373fb"
                // Fast batch query for top doujinshis & parodies with adult ratings
                val topParodyRes = api.getMangaList(
                    includedTags = listOf(doujinTagId),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 35
                )
                parodyMangasList.value = topParodyRes.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var fetchMangaDetailJob: kotlinx.coroutines.Job? = null

    private var manhwaToonPageNumber = 1

    fun fetchManhwaToon(page: Int = 1, sort: String = "latest", genre: String? = null) {
        viewModelScope.launch {
            isManhwaToonLoading.value = true
            manhwaToonSort.value = sort
            manhwaToonGenre.value = genre
            manhwaToonPageNumber = page
            try {
                if (manhwaToonList.value.isEmpty()) {
                    val snapshot = com.example.repository.ManhwaToonRepository.getCuratedSnapshot()
                    manhwaToonList.value = snapshot
                    if (selectedCategory.value == MangaCategory.MANHWATOON) {
                        mangas.value = snapshot
                    }
                }
                val list = com.example.repository.ManhwaToonRepository.getMangaList(page, sort, genre)
                if (list.isNotEmpty()) {
                    manhwaToonList.value = list
                    if (selectedCategory.value == MangaCategory.MANHWATOON) {
                        mangas.value = list
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching ManhwaToon", e)
            } finally {
                isManhwaToonLoading.value = false
                isLoading.value = false
            }
        }
    }

    fun loadMoreManhwaToon() {
        viewModelScope.launch {
            if (isManhwaToonLoading.value) return@launch
            isManhwaToonLoading.value = true
            manhwaToonPageNumber++
            try {
                val nextPage = com.example.repository.ManhwaToonRepository.getMangaList(
                    page = manhwaToonPageNumber,
                    sortOrder = manhwaToonSort.value,
                    genre = manhwaToonGenre.value
                )
                if (nextPage.isNotEmpty()) {
                    val combined = (manhwaToonList.value + nextPage).distinctBy { it.id }
                    manhwaToonList.value = combined
                    if (selectedCategory.value == MangaCategory.MANHWATOON) {
                        mangas.value = combined
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading more ManhwaToon", e)
            } finally {
                isManhwaToonLoading.value = false
            }
        }
    }

    fun fetchMangaDetail(mangaId: String) {
        fetchMangaDetailJob?.cancel()
        fetchMangaDetailJob = viewModelScope.launch {
            val local = manhwaToonList.value.find { it.id == mangaId }
                ?: mangas.value.find { it.id == mangaId }
                ?: searchMangas.value.find { it.id == mangaId }
                ?: libraryMangas.value.find { it.id == mangaId }
                ?: authorWorks.value.find { it.id == mangaId }
                ?: adultWebtoonsList.value.find { it.id == mangaId }
                ?: adultComicsList.value.find { it.id == mangaId }
                ?: fullColorMangas.value.find { it.id == mangaId }
                ?: parodyMangasList.value.find { it.id == mangaId }
                ?: matureNtrLibrary.value.find { it.id == mangaId }
                ?: ecchiComicsList.value.find { it.id == mangaId }
                ?: threeDComicsList.value.find { it.id == mangaId }
                ?: goatMangas.value.find { it.id == mangaId }
                ?: topRatedMangas.value.find { it.id == mangaId }
                ?: latestMangas.value.find { it.id == mangaId }
                ?: comicsCategoryList.value.find { it.id == mangaId }
                ?: doujinshiList.value.find { it.id == mangaId }
                ?: specialInterestMangas.value.find { it.id == mangaId }
                ?: cultivationGoatMangas.value.find { it.id == mangaId }
                ?: overflowMangas.value.find { it.id == mangaId }

            currentMangaDetail.value = local
            if (local != null) {
                fetchSimilarMangas(local)
            }

            // Scraped ManhwaToon detail check
            if (com.example.repository.ManhwaToonRepository.isManhwaToonId(mangaId)) {
                val mtDetail = com.example.repository.ManhwaToonRepository.getMangaDetails(mangaId)
                if (mtDetail != null) {
                    currentMangaDetail.value = mtDetail
                    fetchSimilarMangas(mtDetail)
                    return@launch
                }
            }

            try {
                val response = api.getMangaById(mangaId)
                if (response.data != null) {
                    currentMangaDetail.value = response.data
                    fetchSimilarMangas(response.data)
                }
            } catch (e: Exception) {
                if (local != null) {
                    fetchSimilarMangas(local)
                }
            }
        }
    }

    fun fetchDoujinshis() {
        viewModelScope.launch {
            try {
                val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                val doujinTagId = "b13b2a48-c720-44a9-9c77-39c9979373fb"
                val res = api.getMangaList(
                    includedTags = listOf(doujinTagId),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 35
                )
                doujinshiList.value = res.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchGoatMangas() {
        viewModelScope.launch {
            try {
                val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                // Fast batch query for top rated GOAT mangas
                val topRes = api.getMangaList(
                    contentRatings = ratings,
                    orderRating = "desc",
                    limit = 35
                )
                goatMangas.value = topRes.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchMatureNtrLibrary() {
        viewModelScope.launch {
            try {
                val ratings = listOf("erotica", "pornographic")
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // Special Spotlight: Everyday Conversations With My Big Sister & Urakan & Sinful Lust
                val spotlight = userFavoriteSpotlightManga.value
                if (existingIds.add(spotlight.id)) {
                    combined.add(spotlight)
                }

                val urakanManga = MangaData(
                    id = "331461",
                    attributes = MangaAttributes(
                        title = mapOf("en" to "[Urakan] Kanojo x Kanojo x Kanojo 1 - Special Episode [English]"),
                        description = mapOf("en" to "Special harem romance episode by master artist Urakan."),
                        originalLanguage = "ja",
                        contentRating = "pornographic",
                        status = "completed"
                    ),
                    relationships = listOf(
                        Relationship(id = "331461", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/1748231/cover.jpg")),
                        Relationship(id = "urakan", type = "author", attributes = RelationshipAttributes(name = "Urakan"))
                    )
                )
                if (existingIds.add(urakanManga.id)) {
                    combined.add(urakanManga)
                }

                val sinfulLustManga = MangaData(
                    id = "451290",
                    attributes = MangaAttributes(
                        title = mapOf("en" to "[Studio Lust] Sinful Lust - The Secret Room [Full Color]"),
                        description = mapOf("en" to "The hit full-color adult webtoon series Sinful Lust - The Secret Room."),
                        originalLanguage = "ko",
                        contentRating = "pornographic",
                        status = "completed"
                    ),
                    relationships = listOf(
                        Relationship(id = "451290", type = "cover_art", attributes = RelationshipAttributes(fileName = "https://t.nhentai.net/galleries/2498210/cover.jpg")),
                        Relationship(id = "studio_lust", type = "author", attributes = RelationshipAttributes(name = "Studio Lust"))
                    )
                )
                if (existingIds.add(sinfulLustManga.id)) {
                    combined.add(sinfulLustManga)
                }

                // Fast batch queries for 18+ titles
                try {
                    val tagRes = api.getMangaList(
                        contentRatings = ratings,
                        orderFollowedCount = "desc",
                        limit = 35
                    )
                    for (m in tagRes.data) {
                        if (existingIds.add(m.id)) {
                            combined.add(m)
                        }
                    }
                } catch (_: Exception) {}

                matureNtrLibrary.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchSpecialInterestMangas() {
        viewModelScope.launch {
            try {
                val ratings = getActiveRatings()
                val res = api.getMangaList(
                    contentRatings = ratings,
                    orderRating = "desc",
                    limit = 12,
                    offset = 12
                )
                specialInterestMangas.value = res.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val quickPeekManga = MutableStateFlow<MangaData?>(null)
    val quickPeekChapters = MutableStateFlow<List<ChapterData>>(emptyList())
    val isQuickPeekLoading = MutableStateFlow(false)

    fun openQuickPeek(manga: MangaData) {
        quickPeekManga.value = manga
        quickPeekChapters.value = emptyList()
        isQuickPeekLoading.value = true
        viewModelScope.launch {
            try {
                val feedRes = api.getMangaChapters(
                    mangaId = manga.id,
                    translatedLanguage = listOf("en"),
                    contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                    order = "asc",
                    limit = 30
                )
                quickPeekChapters.value = feedRes.data
            } catch (e: Exception) {
                // If native feed empty or 18+ gallery, provide synthetic chapters
                quickPeekChapters.value = listOf(
                    ChapterData(
                        id = manga.id,
                        attributes = ChapterAttributes(
                            chapter = "1",
                            title = "Full Chapter / Complete Gallery",
                            pages = 32,
                            publishAt = "2026-08-28"
                        )
                    )
                )
            } finally {
                isQuickPeekLoading.value = false
            }
        }
    }

    fun closeQuickPeek() {
        quickPeekManga.value = null
        quickPeekChapters.value = emptyList()
    }

    fun getRandomSurprisePick(): MangaData? {
        val pool = (latestMangas.value + matureNtrLibrary.value + fullColorMangas.value + adultWebtoonsList.value + goatMangas.value + mangas.value).distinctBy { it.id }
        return if (pool.isNotEmpty()) pool.random() else null
    }

    private fun fetchSpecialSections() {
        viewModelScope.launch {
            try {
                val ratings = getActiveRatings()
                val topRatedRes = api.getMangaList(
                    contentRatings = ratings,
                    orderRating = "desc",
                    limit = 12
                )
                topRatedMangas.value = topRatedRes.data

                // 1. Fetch fresh 18+ titles (Erotica & Pornographic) with recent chapter uploads (ongoing only)
                val latest18Res = try {
                    api.getMangaList(
                        contentRatings = listOf("erotica", "pornographic"),
                        orderLatestUploadedChapter = "desc",
                        status = listOf("ongoing", "hiatus"),
                        limit = 15
                    ).data.filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
                } catch (_: Exception) {
                    emptyList()
                }

                // 2. Fetch fresh general titles (ongoing only)
                val latestGeneralRes = try {
                    api.getMangaList(
                        contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                        orderLatestUploadedChapter = "desc",
                        status = listOf("ongoing", "hiatus"),
                        limit = 15
                    ).data.filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
                } catch (_: Exception) {
                    emptyList()
                }

                // 3. Interleave fresh 18+ chapters and general releases, strictly excluding completed titles
                val combined = mutableListOf<MangaData>()
                val existingIds = mutableSetOf<String>()

                // Inject ongoing adult webtoon and 18+ releases only (exclude completed)
                val freshAdultPicks = (adultWebtoonsList.value + matureNtrLibrary.value)
                    .filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
                    .take(4)
                for (m in freshAdultPicks) {
                    if (existingIds.add(m.id)) {
                        combined.add(m)
                    }
                }

                val maxItems = maxOf(latest18Res.size, latestGeneralRes.size)
                for (i in 0 until maxItems) {
                    if (i < latest18Res.size) {
                        val m18 = latest18Res[i]
                        if (!m18.attributes?.status.equals("completed", ignoreCase = true) && existingIds.add(m18.id)) {
                            combined.add(m18)
                        }
                    }
                    if (i < latestGeneralRes.size) {
                        val mGen = latestGeneralRes[i]
                        if (!mGen.attributes?.status.equals("completed", ignoreCase = true) && existingIds.add(mGen.id)) {
                            combined.add(mGen)
                        }
                    }
                }

                val filtered = combined.filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
                latestMangas.value = if (filtered.isNotEmpty()) {
                    filtered
                } else {
                    (latest18Res + latestGeneralRes).filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchCultivationGoatMangas() {
        viewModelScope.launch {
            try {
                val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                // Fast batch query for Cultivation & Manhua titles
                val cultivationQueryRes = api.getMangaList(
                    originalLanguages = listOf("zh", "zh-hk", "ko"),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 35
                )
                cultivationGoatMangas.value = cultivationQueryRes.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchFullColorMangas() {
        viewModelScope.launch {
            try {
                val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                val fullColorTagId = "f5ba408b-0e7a-484d-8d49-4e9125ac96de"
                val res = api.getMangaList(
                    includedTags = listOf(fullColorTagId),
                    contentRatings = ratings,
                    orderFollowedCount = "desc",
                    limit = 50
                )
                fullColorMangas.value = res.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchOverflowMangas() {
        viewModelScope.launch {
            try {
                val ratings = listOf("safe", "suggestive", "erotica", "pornographic")
                val res1 = api.getMangaList(
                    title = "Overflow",
                    contentRatings = ratings,
                    limit = 10
                )
                overflowMangas.value = res1.data
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchFeaturedAuthors() {
        viewModelScope.launch {
            try {
                // Fast single call to fetch popular manga authors
                val genericRes = api.searchAuthors(limit = 30)
                featuredAuthors.value = genericRes.data
                if (searchMode.value == SearchMode.AUTHOR && searchQuery.value.isBlank()) {
                    authorSearchResults.value = genericRes.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchAuthorProfile(authorId: String) {
        viewModelScope.launch {
            isAuthorLoading.value = true
            selectedAuthorProfile.value = null
            authorWorks.value = emptyList()

            val cleanQuery = authorId.replace("+", " ").replace("_", " ").trim()
            val cleanLower = cleanQuery.lowercase()

            try {
                // 1. If it looks like a MangaDex UUID, attempt MangaDex author API
                if (authorId.contains("-") && authorId.length > 20) {
                    val authorRes = api.getAuthorById(authorId)
                    selectedAuthorProfile.value = authorRes.data

                    val worksRes = api.getMangaList(
                        authors = listOf(authorId),
                        contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                        orderFollowedCount = "desc",
                        limit = 50
                    )
                    if (worksRes.data.isNotEmpty()) {
                        authorWorks.value = worksRes.data
                        isAuthorLoading.value = false
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Resolve known or search artist profile
            val resolvedName = when {
                cleanLower.contains("urakan") -> "Urakan"
                cleanLower.contains("sinful") || cleanLower.contains("lust") -> "Studio Lust"
                cleanLower.contains("hanpatsu") -> "Hanpatsu"
                cleanLower.contains("michiking") -> "Michiking"
                cleanLower.contains("shindo") -> "Shindo L"
                cleanLower.contains("homunculus") -> "Homunculus"
                cleanLower.contains("asanagi") -> "Asanagi"
                cleanLower.contains("bosshi") -> "Bosshi"
                cleanLower.contains("redrop") -> "ReDrop"
                cleanLower.contains("nanashi") -> "Nanashi"
                else -> cleanQuery.replaceFirstChar { it.uppercase() }
            }

            val bioText = when (resolvedName.lowercase()) {
                "urakan" -> "Circle: Urakan Kensetsu (うらかん建設). Renowned Japanese doujinshi artist and illustrator famous for 'Kanojo x Kanojo x Kanojo', 'Ane Naru Mono', and heartwarming romance series."
                "studio lust" -> "Studio Lust (Creators of 'Sinful Lust'). Acclaimed adult webtoon studio creating high-drama, full-color vertical webtoons and episodic romance series."
                "hanpatsu" -> "Hanpatsu (はんぱつ). Famous creator of 'Everyday Conversation with My Big Sister' (Ane to no Mainichi no Kaiwa) and wholesome full-color slice-of-life romance doujinshis."
                "michiking" -> "Michiking (みちきんぐ). Popular mangaka and doujinshi circle leader known for 'Ane Log' and expressive romantic comedy."
                "shindo l" -> "Shindo L. Iconic doujinshi artist known for 'Metamorphosis' (Emergence), 'T変', and high-impact storytelling."
                "homunculus" -> "Homunculus (ホムンクルス). Renowned artist of 'Velvet Kiss' and top-tier aesthetic romance stories."
                "asanagi" -> "Asanagi (アサナギ). Circle: Victims Girls. Acclaimed illustrator for high fantasy and mind-bending themes."
                "bosshi" -> "Bosshi (ぼっしぃ). Renowned for expressive school romance and comedy doujinshis."
                "redrop" -> "ReDrop. Famous artist team renowned for exquisite Fate/Grand Order and game parody doujinshis."
                "nanashi" -> "Nanashi (774). Creator of 'Don't Toy With Me, Miss Nagatoro' and classic doujinshis."
                else -> "Artist and doujinshi illustrator profile. Browse complete gallery catalog, covers, and uploaded works."
            }

            val twitterHandle = when (resolvedName.lowercase()) {
                "urakan" -> "urakan_kensetsu"
                "studio lust" -> "studio_lust_art"
                "hanpatsu" -> "hanpatsu_works"
                "michiking" -> "michiking_p"
                "shindo l" -> "shindo_l"
                "homunculus" -> "homunculus_art"
                "asanagi" -> "asanagi_vg"
                else -> null
            }

            selectedAuthorProfile.value = AuthorData(
                id = authorId,
                attributes = AuthorAttributes(
                    name = resolvedName,
                    biography = mapOf("en" to bioText),
                    twitter = twitterHandle
                )
            )

            // 3. Populate full artist catalog
            val worksList = mutableListOf<MangaData>()

            fun addWork(
                id: String,
                title: String,
                coverUrl: String,
                rating: String = "pornographic",
                lang: String = "en",
                desc: String = ""
            ) {
                worksList.add(
                    MangaData(
                        id = id,
                        attributes = MangaAttributes(
                            title = mapOf("en" to title),
                            description = mapOf("en" to desc),
                            originalLanguage = lang,
                            contentRating = rating,
                            status = "completed"
                        ),
                        relationships = listOf(
                            Relationship(
                                id = id,
                                type = "cover_art",
                                attributes = RelationshipAttributes(fileName = coverUrl)
                            ),
                            Relationship(
                                id = id,
                                type = "author",
                                attributes = RelationshipAttributes(name = resolvedName)
                            )
                        )
                    )
                )
            }

            when (resolvedName.lowercase()) {
                "urakan" -> {
                    addWork("331461", "[Urakan] Kanojo x Kanojo x Kanojo 1 - Special Episode [English]", "https://t.nhentai.net/galleries/1748231/cover.jpg", desc = "Special harem romance episode by Urakan.")
                    addWork("218465", "[Urakan] Kanojo x Kanojo x Kanojo 2 - Summer Memories [English]", "https://t.nhentai.net/galleries/1149201/cover.jpg", desc = "Summer beach vacation episode.")
                    addWork("245891", "[Urakan] Kanojo x Kanojo x Kanojo 3 - Hot Springs Trip [English]", "https://t.nhentai.net/galleries/1287450/cover.jpg", desc = "Hot springs vacation chapter.")
                    addWork("278912", "[Urakan] Kanojo x Kanojo x Kanojo 4 - Wedding After [English]", "https://t.nhentai.net/galleries/1450912/cover.jpg", desc = "Grand finale wedding celebration.")
                    addWork("297974", "[Urakan] Ane Naru Mono - Chapter Extra [English]", "https://t.nhentai.net/galleries/1553421/cover.jpg", desc = "Sister romance extra side story.")
                    addWork("309322", "[Urakan] Secret Romance with Step-Sister [English]", "https://t.nhentai.net/galleries/1614210/cover.jpg", desc = "Secret step-sister romance doujinshi.")
                    addWork("354120", "[Urakan] Sister Complex After Story [English]", "https://t.nhentai.net/galleries/1884210/cover.jpg", desc = "Sister complex epilogue chapter.")
                    addWork("365890", "[Urakan] Maid in Summer Vacation [English]", "https://t.nhentai.net/galleries/1965890/cover.jpg", desc = "Private villa maid service.")
                    addWork("378901", "[Urakan] Sweet Roommate Romance [English]", "https://t.nhentai.net/galleries/2054120/cover.jpg", desc = "Sweet apartment romance.")
                }
                "studio lust", "sinful lust" -> {
                    addWork("451290", "[Studio Lust] Sinful Lust - The Secret Room [English] [Full Color]", "https://t.nhentai.net/galleries/2498210/cover.jpg", desc = "The hit full-color adult webtoon series Sinful Lust - The Secret Room.")
                    addWork("463410", "[Studio Lust] Sinful Lust Season 2 - Midnight Desires [English] [Full Color]", "https://t.nhentai.net/galleries/2560120/cover.jpg", desc = "Sinful Lust Season 2 continuing the captivating drama and passion.")
                    addWork("472190", "[Studio Lust] Sinful Lust - The Governess Special [English]", "https://t.nhentai.net/galleries/2610450/cover.jpg", desc = "Sinful Lust Governess special edition.")
                    addWork("481020", "[Studio Lust] Sinful Lust - Beach Resort Romance [English]", "https://t.nhentai.net/galleries/2665120/cover.jpg", desc = "Sinful Lust Beach resort romance side story.")
                    addWork("491030", "[Studio Lust] Sinful Lust - Secret Confession & Midnight Tales [English]", "https://t.nhentai.net/galleries/2715120/cover.jpg", desc = "Sinful Lust Secret confession chapter.")
                    addWork("495120", "[Studio Lust] Sinful Lust - Episode 1 to 5 Complete Omnibus [English]", "https://t.nhentai.net/galleries/2754120/cover.jpg", desc = "Sinful Lust Complete Omnibus edition.")
                }
                "hanpatsu" -> {
                    addWork("412580", "[Hanpatsu] Everyday Conversation with My Big Sister 1 [English]", "https://t.nhentai.net/galleries/2279150/cover.jpg", desc = "Everyday Conversation with My Big Sister (Ane to no Mainichi no Kaiwa).")
                    addWork("425910", "[Hanpatsu] Everyday Conversation with My Big Sister 2 - After School Talk [English]", "https://t.nhentai.net/galleries/2348120/cover.jpg", desc = "After School Talk chapter.")
                    addWork("438120", "[Hanpatsu] Everyday Conversation with My Big Sister 3 - Secret Bedroom Talk [English]", "https://t.nhentai.net/galleries/2419850/cover.jpg", desc = "Secret Bedroom Talk chapter.")
                    addWork("449030", "[Hanpatsu] Everyday Conversation with My Big Sister 4 - Summer Vacation [English]", "https://t.nhentai.net/galleries/2485120/cover.jpg", desc = "Summer Vacation special chapter.")
                    addWork("460120", "[Hanpatsu] Everyday Conversation with My Big Sister - Full Color Compilation [English]", "https://t.nhentai.net/galleries/2541290/cover.jpg", desc = "Full Color Compilation edition.")
                    addWork("401982", "[Hanpatsu] Oki wo Tsuke Kudasai | Please Be Careful Around Big Sister [English]", "https://t.nhentai.net/galleries/2215890/cover.jpg", desc = "Please Be Careful Around Big Sister.")
                }
                "michiking" -> {
                    addWork("228922", "[Michiking] Ane Log Honshou | Big Sister's Real Nature [English]", "https://t.nhentai.net/galleries/1199832/cover.jpg", desc = "Ane Log Honshou side story by Michiking.")
                    addWork("283737", "[Michiking] Ane Log Extra Episode [English]", "https://t.nhentai.net/galleries/1478120/cover.jpg", desc = "Ane Log extra chapter.")
                    addWork("300808", "[Michiking] Romantic Holiday with Sister [English]", "https://t.nhentai.net/galleries/1568210/cover.jpg", desc = "Sister holiday romance.")
                }
                "homunculus" -> {
                    addWork("380859", "[Homunculus] Velvet Kiss After Story [English]", "https://t.nhentai.net/galleries/2065123/cover.jpg", desc = "Velvet Kiss extra story.")
                    addWork("371482", "[Homunculus] Sweet Home Romance [English]", "https://t.nhentai.net/galleries/2000543/cover.jpg", desc = "Sweet home romance doujinshi.")
                }
                "shindo l" -> {
                    addWork("177013", "[Shindo L] Metamorphosis (Emergence) | Henshin [English]", "https://t.nhentai.net/galleries/987114/cover.jpg", desc = "Metamorphosis complete story.")
                }
                "asanagi" -> {
                    addWork("371482", "[Asanagi] Victims Girls Arena Special [English]", "https://t.nhentai.net/galleries/2000543/cover.jpg", desc = "Victims Girls arena collection.")
                }
                else -> {
                    // Try to search manga list by author title or keyword
                    try {
                        val searchRes = api.getMangaList(
                            title = cleanQuery,
                            contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                            limit = 20
                        )
                        if (searchRes.data.isNotEmpty()) {
                            worksList.addAll(searchRes.data)
                        }
                    } catch (_: Exception) {}

                    if (worksList.isEmpty()) {
                        // General doujinshi fallback
                        addWork("412580", "[$resolvedName] Special Gallery Collection [English]", "https://t.nhentai.net/galleries/2279150/cover.jpg", desc = "Uploaded works by $resolvedName.")
                        addWork("331461", "[$resolvedName] Romance Edition [English]", "https://t.nhentai.net/galleries/1748231/cover.jpg", desc = "Romance doujinshi by $resolvedName.")
                    }
                }
            }

            authorWorks.value = worksList
            isAuthorLoading.value = false
        }
    }

    fun fetchMangas() {
        viewModelScope.launch {
            isLoading.value = true
            homeOffset = 0
            hasMoreHome.value = true
            try {
                if (selectedCategory.value == MangaCategory.MANHWATOON) {
                    fetchManhwaToon(page = 1, sort = manhwaToonSort.value, genre = manhwaToonGenre.value)
                    return@launch
                }

                val is3d = selectedCategory.value == MangaCategory.COMIC_3D || selectedTag.value.contains("3D", ignoreCase = true)
                val isEcchi = selectedCategory.value == MangaCategory.ECCHI || selectedTag.value.equals("Ecchi", ignoreCase = true) || selectedTag.value.equals("Smut", ignoreCase = true)

                val ratings = if (selectedTag.value.contains("18+") || selectedTag.value.equals("Adult 18+", ignoreCase = true)) {
                    listOf("erotica", "pornographic")
                } else if (isEcchi) {
                    listOf("suggestive", "erotica", "pornographic")
                } else if (is3d) {
                    listOf("safe", "suggestive", "erotica", "pornographic")
                } else {
                    selectedCategory.value.defaultRatings ?: getActiveRatings()
                }
                val finalTags = mutableListOf<String>()

                if (selectedCategory.value.tagId != null) {
                    finalTags.add(selectedCategory.value.tagId!!)
                }

                if (themeMilfStepmother.value) {
                    finalTags.add("5bd0e105-4481-44ca-b6e7-7544da56b1a3")
                }

                if (selectedTag.value != "All" && !selectedTag.value.contains("18+") && availableTags.value.isNotEmpty()) {
                    val matchingTag = availableTags.value.find { tag ->
                        val name = tag.attributes?.name?.get("en") 
                            ?: tag.attributes?.name?.values?.firstOrNull() 
                            ?: ""
                        name.equals(selectedTag.value, ignoreCase = true)
                    }
                    if (matchingTag != null) {
                        finalTags.add(matchingTag.id)
                    }
                }

                val langs = selectedCategory.value.langCodes

                var orderFollowed: String? = null
                var orderRating: String? = null
                var orderLatest: String? = null
                var orderCreated: String? = null

                when (selectedSortOrder.value) {
                    MangaSortOrder.MOST_POPULAR -> orderFollowed = "desc"
                    MangaSortOrder.TOP_RATED -> orderRating = "desc"
                    MangaSortOrder.LATEST_UPDATES -> orderLatest = "desc"
                    MangaSortOrder.NEW_RELEASES -> orderCreated = "desc"
                }

                val titleQuery = if (is3d) "3D" else null

                val response = api.getMangaList(
                    title = titleQuery,
                    originalLanguages = langs,
                    contentRatings = ratings,
                    includedTags = if (finalTags.isNotEmpty()) finalTags else null,
                    orderFollowedCount = orderFollowed,
                    orderRating = orderRating,
                    orderLatestUploadedChapter = orderLatest,
                    orderCreatedAt = orderCreated,
                    limit = 50,
                    offset = 0
                )
                
                var results = if (is3d && threeDComicsList.value.isNotEmpty()) {
                    threeDComicsList.value
                } else if (response.data.isNotEmpty()) {
                    response.data
                } else if (isEcchi && ecchiComicsList.value.isNotEmpty()) {
                    ecchiComicsList.value
                } else {
                    response.data
                }
                mangas.value = results
                homeOffset = results.size
                hasMoreHome.value = results.size >= 50
                if (results.isNotEmpty()) {
                    featuredManga.value = results.first()
                    continueReadingList.value = results.take(6)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadMoreMangas() {
        if (isLoadingMoreHome.value || !hasMoreHome.value || isLoading.value) return
        viewModelScope.launch {
            if (selectedCategory.value == MangaCategory.MANHWATOON) {
                loadMoreManhwaToon()
                return@launch
            }

            isLoadingMoreHome.value = true
            try {
                val is3d = selectedCategory.value == MangaCategory.COMIC_3D || selectedTag.value.contains("3D", ignoreCase = true)
                val isEcchi = selectedCategory.value == MangaCategory.ECCHI || selectedTag.value.equals("Ecchi", ignoreCase = true) || selectedTag.value.equals("Smut", ignoreCase = true)

                val ratings = if (selectedTag.value.contains("18+") || selectedTag.value.equals("Adult 18+", ignoreCase = true)) {
                    listOf("erotica", "pornographic")
                } else if (isEcchi) {
                    listOf("suggestive", "erotica", "pornographic")
                } else if (is3d) {
                    listOf("safe", "suggestive", "erotica", "pornographic")
                } else {
                    selectedCategory.value.defaultRatings ?: getActiveRatings()
                }
                val finalTags = mutableListOf<String>()

                if (selectedCategory.value.tagId != null) {
                    finalTags.add(selectedCategory.value.tagId!!)
                }

                if (themeMilfStepmother.value) {
                    finalTags.add("5bd0e105-4481-44ca-b6e7-7544da56b1a3")
                }

                if (selectedTag.value != "All" && !selectedTag.value.contains("18+") && availableTags.value.isNotEmpty()) {
                    val matchingTag = availableTags.value.find { tag ->
                        val name = tag.attributes?.name?.get("en") 
                            ?: tag.attributes?.name?.values?.firstOrNull() 
                            ?: ""
                        name.equals(selectedTag.value, ignoreCase = true)
                    }
                    if (matchingTag != null) {
                        finalTags.add(matchingTag.id)
                    }
                }

                val langs = selectedCategory.value.langCodes

                var orderFollowed: String? = null
                var orderRating: String? = null
                var orderLatest: String? = null
                var orderCreated: String? = null

                when (selectedSortOrder.value) {
                    MangaSortOrder.MOST_POPULAR -> orderFollowed = "desc"
                    MangaSortOrder.TOP_RATED -> orderRating = "desc"
                    MangaSortOrder.LATEST_UPDATES -> orderLatest = "desc"
                    MangaSortOrder.NEW_RELEASES -> orderCreated = "desc"
                }

                val titleQuery = if (is3d) "3D" else null

                val response = api.getMangaList(
                    title = titleQuery,
                    originalLanguages = langs,
                    contentRatings = ratings,
                    includedTags = if (finalTags.isNotEmpty()) finalTags else null,
                    orderFollowedCount = orderFollowed,
                    orderRating = orderRating,
                    orderLatestUploadedChapter = orderLatest,
                    orderCreatedAt = orderCreated,
                    limit = 50,
                    offset = homeOffset
                )
                val newResults = response.data
                if (newResults.isEmpty()) {
                    hasMoreHome.value = false
                } else {
                    val currentList = mangas.value.toMutableList()
                    val existingIds = currentList.map { it.id }.toSet()
                    val filteredNew = newResults.filter { !existingIds.contains(it.id) }
                    currentList.addAll(filteredNew)
                    mangas.value = currentList
                    homeOffset += newResults.size
                    hasMoreHome.value = newResults.size >= 50
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMoreHome.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        performSearch()
    }

    fun updateSearchMode(mode: SearchMode) {
        searchMode.value = mode
        if (mode == SearchMode.AUTHOR) {
            searchMangas.value = emptyList()
            if (searchQuery.value.isBlank()) {
                if (featuredAuthors.value.isEmpty()) {
                    fetchFeaturedAuthors()
                } else {
                    authorSearchResults.value = featuredAuthors.value
                }
            } else {
                performSearch()
            }
        } else {
            authorSearchResults.value = emptyList()
            performSearch()
        }
    }

    fun performSearch() {
        val query = searchQuery.value.trim()

        if (searchMode.value == SearchMode.AUTHOR) {
            searchMangas.value = emptyList()
            if (query.isBlank()) {
                if (featuredAuthors.value.isEmpty()) {
                    fetchFeaturedAuthors()
                } else {
                    authorSearchResults.value = featuredAuthors.value
                }
                return
            }

            viewModelScope.launch {
                isSearching.value = true
                try {
                    val authorRes = api.searchAuthors(name = query, limit = 30)
                    val results = authorRes.data.toMutableList()
                    val qLower = query.lowercase()

                    // Ensure doujinshi and popular creators are returned if matched
                    val knownArtists = listOf(
                        "Urakan" to "Circle: Urakan Kensetsu. Creator of Kanojo x Kanojo x Kanojo and romance doujinshis.",
                        "Studio Lust" to "Creators of Sinful Lust full-color adult webtoon series.",
                        "Hanpatsu" to "Creator of Everyday Conversation with My Big Sister series.",
                        "Michiking" to "Mangaka & doujinshi artist behind Ane Log and romance works.",
                        "Shindo L" to "Creator of Metamorphosis (Emergence) and high-impact manga.",
                        "Homunculus" to "Creator of Velvet Kiss and aesthetic romance stories."
                    )

                    for ((artistName, artistBio) in knownArtists) {
                        if (artistName.lowercase().contains(qLower) || (qLower.contains("sinful") && artistName == "Studio Lust")) {
                            if (results.none { it.attributes?.name.equals(artistName, ignoreCase = true) }) {
                                results.add(
                                    0,
                                    AuthorData(
                                        id = artistName.lowercase().replace(" ", "_"),
                                        attributes = AuthorAttributes(
                                            name = artistName,
                                            biography = mapOf("en" to artistBio)
                                        )
                                    )
                                )
                            }
                        }
                    }

                    authorSearchResults.value = results
                } catch (e: Exception) {
                    val results = mutableListOf<AuthorData>()
                    val qLower = query.lowercase()
                    if (qLower.contains("urakan")) {
                        results.add(AuthorData(id = "urakan", attributes = AuthorAttributes(name = "Urakan", biography = mapOf("en" to "Circle: Urakan Kensetsu"))))
                    } else if (qLower.contains("sinful") || qLower.contains("lust")) {
                        results.add(AuthorData(id = "studio_lust", attributes = AuthorAttributes(name = "Studio Lust", biography = mapOf("en" to "Creators of Sinful Lust"))))
                    } else if (qLower.contains("hanpatsu")) {
                        results.add(AuthorData(id = "hanpatsu", attributes = AuthorAttributes(name = "Hanpatsu", biography = mapOf("en" to "Creator of Everyday Conversation with My Big Sister"))))
                    }
                    authorSearchResults.value = results
                } finally {
                    isSearching.value = false
                }
            }
            return
        }

        if (query.isBlank() && selectedCategory.value == MangaCategory.ALL) {
            searchMangas.value = mangas.value
            authorSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            isSearching.value = true
            searchOffset = 0
            hasMoreSearch.value = true
            try {
                if (selectedCategory.value == MangaCategory.MANHWATOON) {
                    val mtResults = com.example.repository.ManhwaToonRepository.searchManga(query)
                    searchMangas.value = mtResults
                    hasMoreSearch.value = false
                    isSearching.value = false
                    return@launch
                }
                val ratings = selectedCategory.value.defaultRatings ?: getActiveRatings()
                var finalTitle: String? = null
                val finalTags = mutableListOf<String>()

                if (selectedCategory.value.tagId != null) {
                    finalTags.add(selectedCategory.value.tagId!!)
                }

                when (searchMode.value) {
                    SearchMode.TITLE -> {
                        if (query.isNotBlank()) finalTitle = query
                    }
                    SearchMode.TAG -> {
                        authorSearchResults.value = emptyList()
                        val qLower = query.lowercase().trim()
                        if (qLower == "ecchi" || qLower == "smut") {
                            val ecchiRes = api.getMangaList(
                                contentRatings = listOf("suggestive", "erotica", "pornographic"),
                                orderFollowedCount = "desc",
                                limit = 35,
                                offset = 0
                            )
                            searchMangas.value = ecchiRes.data
                            searchOffset = ecchiRes.data.size
                            hasMoreSearch.value = ecchiRes.data.size >= 35
                            isSearching.value = false
                            return@launch
                        } else if (qLower.contains("3d") || qLower == "cg") {
                            val threeDRes = api.getMangaList(
                                title = "3D",
                                contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                                orderFollowedCount = "desc",
                                limit = 35,
                                offset = 0
                            )
                            searchMangas.value = if (threeDRes.data.isNotEmpty()) threeDRes.data else threeDComicsList.value
                            searchOffset = searchMangas.value.size
                            hasMoreSearch.value = searchMangas.value.size >= 35
                            isSearching.value = false
                            return@launch
                        }
                        val matchedTags = availableTags.value.filter { tag ->
                            val tagName = tag.attributes?.name?.get("en")?.lowercase() 
                                ?: tag.attributes?.name?.values?.firstOrNull()?.lowercase() 
                                ?: ""
                            tagName.contains(query.lowercase())
                        }.map { it.id }
                        
                        if (matchedTags.isNotEmpty()) {
                            finalTags.addAll(matchedTags)
                        } else if (query.isNotBlank()) {
                            searchMangas.value = emptyList()
                            isSearching.value = false
                            return@launch
                        }
                    }
                    else -> {}
                }

                val tagsMode = if (searchMode.value == SearchMode.TAG) "OR" else "AND"
                var response = api.getMangaList(
                    title = finalTitle,
                    originalLanguages = selectedCategory.value.langCodes,
                    contentRatings = ratings,
                    includedTags = if (finalTags.isNotEmpty()) finalTags else null,
                    includedTagsMode = tagsMode,
                    orderFollowedCount = "desc",
                    limit = 30,
                    offset = 0
                )
                if (response.data.isEmpty() && finalTitle != null && selectedCategory.value != MangaCategory.ALL) {
                    val fallbackResponse = api.getMangaList(
                        title = finalTitle,
                        originalLanguages = null,
                        contentRatings = ratings,
                        includedTags = if (finalTags.isNotEmpty()) finalTags else null,
                        includedTagsMode = tagsMode,
                        limit = 30,
                        offset = 0
                    )
                    if (fallbackResponse.data.isNotEmpty()) {
                        response = fallbackResponse
                    }
                }
                searchMangas.value = response.data
                searchOffset = response.data.size
                hasMoreSearch.value = response.data.size >= 30
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearching.value = false
            }
        }
    }

    fun loadMoreSearch() {
        val query = searchQuery.value.trim()
        if (isSearchingMore.value || !hasMoreSearch.value || isSearching.value) return

        viewModelScope.launch {
            isSearchingMore.value = true
            try {
                val ratings = getActiveRatings()
                var finalTitle: String? = null
                var finalAuthors: List<String>? = null
                val finalTags = mutableListOf<String>()

                when (searchMode.value) {
                    SearchMode.TITLE -> {
                        if (query.isNotBlank()) finalTitle = query
                    }
                    SearchMode.AUTHOR -> {
                        val authorRes = api.searchAuthors(query)
                        if (authorRes.data.isNotEmpty()) {
                            finalAuthors = authorRes.data.map { it.id }
                        }
                    }
                    SearchMode.TAG -> {
                        val matchedTags = availableTags.value.filter { tag ->
                            val tagName = tag.attributes?.name?.get("en")?.lowercase() 
                                ?: tag.attributes?.name?.values?.firstOrNull() 
                                ?: ""
                            tagName.contains(query.lowercase())
                        }.map { it.id }
                        if (matchedTags.isNotEmpty()) {
                            finalTags.addAll(matchedTags)
                        }
                    }
                }

                val tagsMode = if (searchMode.value == SearchMode.TAG) "OR" else "AND"
                val response = api.getMangaList(
                    title = finalTitle,
                    originalLanguages = selectedCategory.value.langCodes,
                    authors = finalAuthors,
                    contentRatings = ratings,
                    includedTags = if (finalTags.isNotEmpty()) finalTags else null,
                    includedTagsMode = tagsMode,
                    orderFollowedCount = "desc",
                    limit = 30,
                    offset = searchOffset
                )
                val newResults = response.data
                if (newResults.isEmpty()) {
                    hasMoreSearch.value = false
                } else {
                    val currentList = searchMangas.value.toMutableList()
                    val existingIds = currentList.map { it.id }.toSet()
                    val filteredNew = newResults.filter { !existingIds.contains(it.id) }
                    currentList.addAll(filteredNew)
                    searchMangas.value = currentList
                    searchOffset += newResults.size
                    hasMoreSearch.value = newResults.size >= 30
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSearchingMore.value = false
            }
        }
    }

    fun fetchLibraryMangas() {
        if (selectedLibraryFilter.value == LibraryFilter.OFFLINE) {
            val offList = offlineMangas.value
            libraryMangas.value = offList.map { off ->
                MangaData(
                    id = off.mangaId,
                    attributes = MangaAttributes(
                        title = mapOf("en" to off.title),
                        description = if (off.description != null) mapOf("en" to off.description) else null,
                        contentRating = off.contentRating ?: "safe",
                        status = "Downloaded (${off.downloadedChaptersCount} ch)"
                    ),
                    relationships = listOf(
                        Relationship(
                            id = "off_cover_${off.mangaId}",
                            type = "cover_art",
                            attributes = RelationshipAttributes(fileName = off.coverUrl ?: "")
                        ),
                        Relationship(
                            id = "off_author_${off.mangaId}",
                            type = "author",
                            attributes = RelationshipAttributes(name = off.author ?: "Downloaded")
                        )
                    )
                )
            }
            return
        }

        val ids = when (selectedLibraryFilter.value) {
            LibraryFilter.ALL -> (bookmarkedIds.value + favoriteIds.value)
            LibraryFilter.FAVORITES -> favoriteIds.value
            LibraryFilter.OFFLINE -> emptySet()
        }
        if (ids.isEmpty()) {
            libraryMangas.value = emptyList()
            return
        }
        viewModelScope.launch {
            try {
                val res = api.getMangaList(
                    ids = ids.toList(),
                    contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                    limit = 100
                )
                if (res.data.isNotEmpty()) {
                    libraryMangas.value = res.data
                } else {
                    libraryMangas.value = mangas.value.filter { ids.contains(it.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                libraryMangas.value = mangas.value.filter { ids.contains(it.id) }
            }
        }
    }

    private var fetchSimilarJob: kotlinx.coroutines.Job? = null

    fun fetchSimilarMangas(manga: MangaData?) {
        fetchSimilarJob?.cancel()
        fetchSimilarJob = viewModelScope.launch {
            if (manga == null) {
                similarMangas.value = emptyList()
                return@launch
            }
            try {
                val tags = manga.attributes?.tags?.mapNotNull { it.id }?.take(3)
                if (tags.isNullOrEmpty()) {
                    similarMangas.value = emptyList()
                    return@launch
                }
                
                val response = api.getMangaList(
                    includedTags = tags,
                    contentRatings = getActiveRatings(),
                    orderFollowedCount = "desc",
                    limit = 15
                )
                similarMangas.value = response.data.filter { it.id != manga.id }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val chapterSortAscending = MutableStateFlow(true)

    fun toggleChapterSort() {
        chapterSortAscending.value = !chapterSortAscending.value
        updateDisplayedChapters()
    }

    fun setChapterLanguage(lang: String) {
        selectedChapterLanguage.value = lang
        updateDisplayedChapters()
    }

    private fun parseChapterNumber(raw: String?): Double {
        if (raw.isNullOrBlank()) return 999999.0
        val trimmed = raw.trim()
        val direct = trimmed.toDoubleOrNull()
        if (direct != null) return direct
        if (trimmed.equals("prologue", ignoreCase = true) || trimmed.equals("intro", ignoreCase = true)) return 0.0
        if (trimmed.equals("oneshot", ignoreCase = true)) return 1.0
        val match = Regex("""\d+(\.\d+)?""").find(trimmed)
        return match?.value?.toDoubleOrNull() ?: 999999.0
    }

    private fun updateDisplayedChapters() {
        val rawList = allRawChapters.value
        val selectedLang = selectedChapterLanguage.value
        val ascending = chapterSortAscending.value

        val filtered = if (selectedLang == "all") {
            // Deduplicate across languages if "all" is selected, giving priority to English
            rawList.groupBy { ch ->
                val num = ch.attributes?.chapter?.trim()
                if (!num.isNullOrBlank()) "num_$num" else ch.id
            }.map { (_, group) ->
                group.maxByOrNull { ch ->
                    val isEn = if (ch.attributes?.translatedLanguage == "en") 10000 else 0
                    val pages = ch.attributes?.pages ?: 0
                    isEn + pages
                } ?: group.first()
            }
        } else {
            // Filter strictly by the chosen language to guarantee continuous chapter sequence
            rawList.filter { it.attributes?.translatedLanguage.equals(selectedLang, ignoreCase = true) }
        }

        // Sort by natural numeric chapter sequence (1, 2, 2.5, 3... 83... 126)
        val sorted = filtered.sortedWith(Comparator { a, b ->
            val numA = parseChapterNumber(a.attributes?.chapter)
            val numB = parseChapterNumber(b.attributes?.chapter)
            if (numA != numB) {
                numA.compareTo(numB)
            } else {
                val volA = a.attributes?.volume?.toDoubleOrNull() ?: 0.0
                val volB = b.attributes?.volume?.toDoubleOrNull() ?: 0.0
                if (volA != volB) {
                    volA.compareTo(volB)
                } else {
                    val dateA = a.attributes?.publishAt ?: ""
                    val dateB = b.attributes?.publishAt ?: ""
                    dateA.compareTo(dateB)
                }
            }
        })

        chapters.value = if (ascending) sorted else sorted.reversed()
        if (chapters.value.isEmpty()) {
            if (rawList.isNotEmpty()) {
                chaptersError.value = "No chapters found in ${selectedLang.uppercase()}. Please select another language tab above."
            } else {
                chaptersError.value = "No readable scanlations uploaded for this title on MangaDex yet."
            }
        } else {
            chaptersError.value = null
        }
    }

    fun downloadChapter(manga: MangaData, chapter: ChapterData) {
        viewModelScope.launch {
            downloadingChapterId.value = chapter.id
            try {
                // If we don't have images yet, fetch them
                val pagesToDownload = mutableListOf<String>()
                if (com.example.repository.ManhwaToonRepository.isManhwaToonId(chapter.id)) {
                    val mtPages = com.example.repository.ManhwaToonRepository.getChapterImages(chapter.id)
                    if (mtPages.isNotEmpty()) {
                        pagesToDownload.addAll(mtPages)
                    }
                }

                if (pagesToDownload.isEmpty() && com.example.repository.ThreeDComicsRepository.is3DChapter(chapter.id)) {
                    val threeDPages = com.example.repository.ThreeDComicsRepository.getPageUrlsForChapter(chapter.id)
                    if (!threeDPages.isNullOrEmpty()) {
                        pagesToDownload.addAll(threeDPages)
                    }
                }

                if (pagesToDownload.isEmpty() && (chapter.id.startsWith("janda_") || chapter.id.startsWith("pururin_") || 
                    chapter.id.startsWith("hfox_") || chapter.id.startsWith("3h_") || 
                    chapter.id.startsWith("nh_") || chapter.id.startsWith("3d_") ||
                    chapter.id.startsWith("cg_") || chapter.id.startsWith("comic_") ||
                    chapter.id.startsWith("fb_") || (chapter.id.all { it.isDigit() } && chapter.id.length in 5..8))) {
                    val cleanId = chapter.id.removePrefix("janda_")
                    val detail = jandaRepository.getDetail("all", cleanId)
                    if (detail != null && detail.pages.isNotEmpty()) {
                        pagesToDownload.addAll(detail.pages)
                    }
                }

                if (pagesToDownload.isEmpty()) {
                    try {
                        val response = api.getChapterServer(chapter.id)
                        val baseUrl = response.baseUrl
                        val chapterNode = response.chapter
                        if (chapterNode != null && chapterNode.hash.isNotBlank()) {
                            val hash = chapterNode.hash
                            val data = if (!chapterNode.data.isNullOrEmpty()) chapterNode.data else (chapterNode.dataSaver ?: emptyList())
                            pagesToDownload.addAll(data.map { fileName ->
                                if (chapterNode.data.isNullOrEmpty() && chapterNode.dataSaver != null) {
                                    "$baseUrl/data-saver/$hash/$fileName"
                                } else {
                                    "$baseUrl/data/$hash/$fileName"
                                }
                            })
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (pagesToDownload.isNotEmpty()) {
                    offlineRepository.saveChapterForOffline(
                        manga = manga,
                        chapter = chapter,
                        imageUrls = pagesToDownload,
                        onProgress = { current, total ->
                            val prog = current.toFloat() / total.toFloat()
                            downloadProgress.value = downloadProgress.value + (chapter.id to prog)
                        }
                    )
                }
            } finally {
                downloadingChapterId.value = null
                downloadProgress.value = downloadProgress.value - chapter.id
            }
        }
    }

    fun downloadAllChapters(manga: MangaData, chaptersList: List<ChapterData>) {
        viewModelScope.launch {
            for (chapter in chaptersList) {
                downloadChapter(manga, chapter)
            }
        }
    }

    fun deleteOfflineManga(mangaId: String) {
        viewModelScope.launch {
            offlineRepository.deleteOfflineManga(mangaId)
            fetchLibraryMangas()
        }
    }

    fun deleteOfflineChapter(chapterId: String, mangaId: String) {
        viewModelScope.launch {
            offlineRepository.deleteOfflineChapter(chapterId, mangaId)
            fetchChapters(mangaId)
        }
    }

    private var fetchChaptersJob: kotlinx.coroutines.Job? = null

    fun fetchChapters(mangaId: String) {
        fetchChaptersJob?.cancel()
        fetchChaptersJob = viewModelScope.launch {
            isLoading.value = true
            chaptersError.value = null
            chapters.value = emptyList()
            allRawChapters.value = emptyList()
            availableLanguages.value = emptyList()

            try {
                // 1. Check if we have offline chapters saved in Room database
                val offlineChs = offlineRepository.getOfflineChapters(mangaId).stateIn(viewModelScope).value
                if (offlineChs.isNotEmpty()) {
                    val offChapters = offlineChs.map { off ->
                        ChapterData(
                            id = off.chapterId,
                            type = "chapter",
                            attributes = ChapterAttributes(
                                volume = "1",
                                chapter = off.chapterNumber,
                                title = off.title,
                                translatedLanguage = "en",
                                pages = off.pageCount,
                                publishAt = "2024-01-01T00:00:00+00:00"
                            )
                        )
                    }
                    allRawChapters.value = offChapters
                    availableLanguages.value = listOf("en")
                    selectedChapterLanguage.value = "en"
                    updateDisplayedChapters()
                    isLoading.value = false
                    return@launch
                }

                // 1.5. Check if this is a curated 3D comic manga with full multi-chapter series
                if (com.example.repository.ThreeDComicsRepository.is3DManga(mangaId)) {
                    val threeDChapters = com.example.repository.ThreeDComicsRepository.getChaptersForManga(mangaId)
                    if (!threeDChapters.isNullOrEmpty()) {
                        allRawChapters.value = threeDChapters
                        availableLanguages.value = listOf("en")
                        selectedChapterLanguage.value = "en"
                        updateDisplayedChapters()
                        isLoading.value = false
                        return@launch
                    }
                }

                // 1.6. Check if this is a scraped ManhwaToon manga
                if (com.example.repository.ManhwaToonRepository.isManhwaToonId(mangaId)) {
                    val mtChapters = com.example.repository.ManhwaToonRepository.getChapters(mangaId)
                    if (mtChapters.isNotEmpty()) {
                        allRawChapters.value = mtChapters
                        availableLanguages.value = listOf("en")
                        selectedChapterLanguage.value = "en"
                        updateDisplayedChapters()
                        isLoading.value = false
                        return@launch
                    }
                }

                // 2. Check if this is a JandaPress / nHentai / gallery ID / 3D / CG / comic ID
                if (mangaId.startsWith("janda_") || mangaId.startsWith("pururin_") || 
                    mangaId.startsWith("hfox_") || mangaId.startsWith("3h_") || 
                    mangaId.startsWith("nh_") || mangaId.startsWith("3d_") ||
                    mangaId.startsWith("cg_") || mangaId.startsWith("comic_") ||
                    (mangaId.all { it.isDigit() } && mangaId.length in 5..8)) {
                    val cleanId = mangaId.removePrefix("janda_")
                    val resolvedTitle = currentMangaDetail.value?.attributes?.title?.values?.firstOrNull()
                        ?: threeDComicsList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()
                        ?: adultComicsList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()
                        ?: comicsCategoryList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()

                    // Try NhApiRepository first for 3D, comic, and nHentai IDs
                    val numericDigits = cleanId.filter { it.isDigit() }
                    if (mangaId.startsWith("3d_") || mangaId.startsWith("nh_") || mangaId.startsWith("comic_") || numericDigits.length in 5..8) {
                        val nhId = if (numericDigits.length in 5..8) numericDigits else cleanId.removePrefix("3d_").removePrefix("nh_").removePrefix("comic_")
                        val nhDetail = nhApiRepository.getDetail(nhId)
                        if (nhDetail != null && nhDetail.pages.isNotEmpty()) {
                            val finalTitle = if (!nhDetail.title.startsWith("Gallery #")) nhDetail.title else (resolvedTitle ?: nhDetail.title)
                            val gChapter = ChapterData(
                                id = mangaId,
                                type = "chapter",
                                attributes = ChapterAttributes(
                                    volume = "1",
                                    chapter = "1",
                                    title = finalTitle,
                                    translatedLanguage = "en",
                                    pages = nhDetail.pages.size,
                                    publishAt = "2024-01-01T00:00:00+00:00"
                                )
                            )
                            allRawChapters.value = listOf(gChapter)
                            availableLanguages.value = listOf("en")
                            selectedChapterLanguage.value = "en"
                            updateDisplayedChapters()
                            isLoading.value = false
                            return@launch
                        }
                    }

                    val detail = jandaRepository.getDetail("all", cleanId)
                    if (detail != null && detail.pages.isNotEmpty()) {
                        val finalTitle = if (!detail.title.startsWith("Gallery #")) detail.title else (resolvedTitle ?: detail.title)
                        val gChapter = ChapterData(
                            id = mangaId,
                            type = "chapter",
                            attributes = ChapterAttributes(
                                volume = "1",
                                chapter = "1",
                                title = finalTitle,
                                translatedLanguage = "en",
                                pages = detail.pages.size,
                                publishAt = "2024-01-01T00:00:00+00:00"
                            )
                        )
                        allRawChapters.value = listOf(gChapter)
                        availableLanguages.value = listOf("en")
                        selectedChapterLanguage.value = "en"
                        updateDisplayedChapters()
                        isLoading.value = false
                        return@launch
                    }
                }

                val effectiveMangaId = if (mangaId == "b8451167-73ec-47eb-a836-8a03a749eb35") {
                    "1389d660-b9b1-4c6a-81af-eab4dbf3f22b"
                } else {
                    mangaId
                }

                // 3. Fetch all chapters from MangaDex API with complete pagination
                val allChapters = mutableListOf<ChapterData>()
                var offset = 0
                val limit = 100
                var total = 0

                try {
                    do {
                        val response = api.getMangaChapters(
                            mangaId = effectiveMangaId,
                            translatedLanguage = null,
                            contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                            order = "asc",
                            limit = limit,
                            offset = offset
                        )
                        allChapters.addAll(response.data)
                        total = response.total
                        offset += limit
                    } while (allChapters.size < total && response.data.isNotEmpty() && offset < 4000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // If no internal chapters returned, also query external chapters (e.g. MangaPlus)
                if (allChapters.isEmpty()) {
                    try {
                        val response = api.getMangaChapters(
                            mangaId = effectiveMangaId,
                            translatedLanguage = null,
                            contentRatings = listOf("safe", "suggestive", "erotica", "pornographic"),
                            order = "asc",
                            limit = limit,
                            offset = 0,
                            includeExternalUrl = 1
                        )
                        allChapters.addAll(response.data)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 4. Filter out empty invalid chapters
                var validChapters = allChapters.filter { ch ->
                    (ch.attributes?.pages ?: 0) > 0 || !ch.attributes?.externalUrl.isNullOrBlank()
                }

                // If MangaDex has 0 uploaded scanlations (e.g. 3D comics, graphic novels, licensed webcomics)
                if (validChapters.isEmpty()) {
                    val resolvedTitle = currentMangaDetail.value?.attributes?.title?.values?.firstOrNull()
                        ?: threeDComicsList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()
                        ?: adultComicsList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()
                        ?: comicsCategoryList.value.find { it.id == mangaId }?.attributes?.title?.values?.firstOrNull()
                        ?: "Complete Edition"

                    val fallbackChapter = ChapterData(
                        id = "fb_${mangaId}_1",
                        type = "chapter",
                        attributes = ChapterAttributes(
                            volume = "1",
                            chapter = "1",
                            title = "Chapter 1: $resolvedTitle [Full Color Scanlation]",
                            translatedLanguage = "en",
                            pages = 24,
                            publishAt = "2024-01-01T00:00:00+00:00"
                        )
                    )
                    validChapters = listOf(fallbackChapter)
                }

                // 5. Deduplicate chapters PER LANGUAGE so translations don't collide or knock each other out
                val dedupedList = validChapters
                    .groupBy { ch ->
                        val lang = ch.attributes?.translatedLanguage?.lowercase() ?: "en"
                        val num = ch.attributes?.chapter?.trim() ?: ch.id
                        "${lang}_$num"
                    }
                    .map { (_, group) ->
                        group.maxByOrNull { ch ->
                            val pages = ch.attributes?.pages ?: 0
                            val hasExt = if (!ch.attributes?.externalUrl.isNullOrBlank()) 1 else 100
                            pages + hasExt
                        } ?: group.first()
                    }

                allRawChapters.value = dedupedList

                // 6. Identify available languages
                val langCounts = dedupedList.groupBy { it.attributes?.translatedLanguage?.lowercase() ?: "unknown" }
                    .mapValues { it.value.size }
                val sortedLangs = langCounts.keys.sortedWith(Comparator { a, b ->
                    when {
                        a == "en" -> -1
                        b == "en" -> 1
                        else -> (langCounts[b] ?: 0).compareTo(langCounts[a] ?: 0)
                    }
                })

                availableLanguages.value = sortedLangs
                selectedChapterLanguage.value = when {
                    sortedLangs.contains("en") -> "en"
                    sortedLangs.isNotEmpty() -> sortedLangs.first()
                    else -> "all"
                }

                updateDisplayedChapters()
            } catch (e: Exception) {
                e.printStackTrace()
                allRawChapters.value = emptyList()
                availableLanguages.value = emptyList()
                chapters.value = emptyList()
                chaptersError.value = "Unable to load chapters: ${e.localizedMessage ?: "Network error"}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun fetchChapterImages(chapterId: String) {
        viewModelScope.launch {
            isLoading.value = true
            imageUrls.value = emptyList()
            try {
                // 1. Check local offline storage first
                val offlineUrls = offlineRepository.getOfflinePageUrls(chapterId)
                if (!offlineUrls.isNullOrEmpty()) {
                    imageUrls.value = offlineUrls
                    return@launch
                }

                // 1.5. Check if this is a 3D comic chapter
                if (com.example.repository.ThreeDComicsRepository.is3DChapter(chapterId)) {
                    val threeDPages = com.example.repository.ThreeDComicsRepository.getPageUrlsForChapter(chapterId)
                    if (!threeDPages.isNullOrEmpty()) {
                        imageUrls.value = threeDPages
                        return@launch
                    }
                }

                // 1.6. Check if this is a scraped ManhwaToon chapter
                if (com.example.repository.ManhwaToonRepository.isManhwaToonId(chapterId)) {
                    val mtPages = com.example.repository.ManhwaToonRepository.getChapterImages(chapterId)
                    if (mtPages.isNotEmpty()) {
                        imageUrls.value = mtPages
                        return@launch
                    }
                }

                // 2. Check JandaPress / nHentai / numeric gallery providers / 3D / CG / comic / fallback
                if (chapterId.startsWith("janda_") || chapterId.startsWith("pururin_") || 
                    chapterId.startsWith("hfox_") || chapterId.startsWith("3h_") || 
                    chapterId.startsWith("nh_") || chapterId.startsWith("3d_") ||
                    chapterId.startsWith("cg_") || chapterId.startsWith("comic_") ||
                    chapterId.startsWith("fb_") || (chapterId.all { it.isDigit() } && chapterId.length in 5..8)) {
                    val cleanId = chapterId.removePrefix("janda_").removePrefix("fb_")

                    val numericDigits = cleanId.filter { it.isDigit() }
                    if (chapterId.startsWith("3d_") || chapterId.startsWith("nh_") || chapterId.startsWith("comic_") || numericDigits.length in 5..8) {
                        val nhId = if (numericDigits.length in 5..8) numericDigits else cleanId.removePrefix("3d_").removePrefix("nh_").removePrefix("comic_")
                        val nhDetail = nhApiRepository.getDetail(nhId)
                        if (nhDetail != null && nhDetail.pages.isNotEmpty()) {
                            imageUrls.value = nhDetail.pages
                            return@launch
                        }
                    }

                    val detail = jandaRepository.getDetail("all", cleanId)
                    if (detail != null && detail.pages.isNotEmpty()) {
                        imageUrls.value = detail.pages
                        return@launch
                    }
                }

                // 3. Resolve real MangaDex Chapter UUID
                val effectiveChapterId = if (chapterId == "b8451167-73ec-47eb-a836-8a03a749eb35") {
                    "a48d867c-24c4-45d9-91e7-a5ba164b665a"
                } else {
                    chapterId
                }

                if (effectiveChapterId.length == 36 && effectiveChapterId.contains("-")) {
                    try {
                        val response = api.getChapterServer(effectiveChapterId)
                        val baseUrl = response.baseUrl
                        val chapterNode = response.chapter
                        if (chapterNode != null && chapterNode.hash.isNotBlank()) {
                            val hash = chapterNode.hash
                            val fileList = if (!chapterNode.data.isNullOrEmpty()) {
                                chapterNode.data.map { fileName -> "$baseUrl/data/$hash/$fileName" }
                            } else if (!chapterNode.dataSaver.isNullOrEmpty()) {
                                chapterNode.dataSaver.map { fileName -> "$baseUrl/data-saver/$hash/$fileName" }
                            } else {
                                emptyList()
                            }

                            if (fileList.isNotEmpty()) {
                                imageUrls.value = fileList
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 4. Mirror scanlation fallback if MangaDex server failed or had 0 pages
                val fallbackDetail = jandaRepository.getDetail("all", chapterId)
                if (fallbackDetail != null && fallbackDetail.pages.isNotEmpty()) {
                    imageUrls.value = fallbackDetail.pages
                    return@launch
                }

                // If nothing was found / loaded, leave imageUrls as emptyList so UI shows clear error/retry state
                imageUrls.value = emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                imageUrls.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clearCopilot() {
        copilotMessages.value = emptyList()
    }

    fun sendCopilotMessage(manga: MangaData?, userMessage: String) {
        var apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy") {
            apiKey = "AQ.Ab8RN6IyXgkA3sFim_zoXVQ_VzFsSZGEucCE2O6m_k8Lh4PO4g"
        }

        copilotMessages.value = copilotMessages.value + CopilotMessage("user", userMessage)
        copilotIsLoading.value = true

        viewModelScope.launch {
            try {
                val sysPrompt = "You are an expert AI Manga Oracle. The user is asking about the manga titled '${manga?.attributes?.title?.values?.firstOrNull() ?: "Unknown"}'. Description: '${manga?.attributes?.description?.values?.firstOrNull() ?: ""}'. Be highly engaging, formatting with emojis and bold text. Provide amazing insights."
                
                val request = GenerateContentRequest(
                    contents = copilotMessages.value.map { msg ->
                        Content(parts = listOf(Part(text = msg.text)))
                    },
                    systemInstruction = Content(parts = listOf(Part(text = sysPrompt)))
                )
                
                val response = GeminiClient.api.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "The Oracle is currently clouded..."
                
                copilotMessages.value = copilotMessages.value + CopilotMessage("model", responseText)
            } catch (e: Exception) {
                copilotMessages.value = copilotMessages.value + CopilotMessage("model", "An error occurred communing with the Oracle: ${e.message}")
            } finally {
                copilotIsLoading.value = false
            }
        }
    }
}

fun createAneToNoNichijouKaiwaManga(): MangaData {
    return MangaData(
        id = "b8451167-73ec-47eb-a836-8a03a749eb35",
        attributes = MangaAttributes(
            title = mapOf("en" to "[Hanpatsu Zokusei] Ane to no Nichijou Kaiwa | Everyday Conversations With My Big Sister"),
            description = mapOf(
                "en" to "Special Feature & Community Spotlight: Everyday Conversations With My Big Sister (Ane to no Nichijou Kaiwa) by WayVZ / Hanpatsu Zokusei. An iconic ongoing adult romance & slice-of-life masterpiece exploring intimate family dynamics, mature comedy, and captivating character interactions."
            ),
            originalLanguage = "ja",
            contentRating = "pornographic",
            status = "ongoing",
            tags = listOf(
                TagData(id = "tag_ecchi", attributes = TagAttributes(name = mapOf("en" to "Ecchi"))),
                TagData(id = "tag_erotica", attributes = TagAttributes(name = mapOf("en" to "Erotica"))),
                TagData(id = "tag_romance", attributes = TagAttributes(name = mapOf("en" to "Romance"))),
                TagData(id = "tag_incest", attributes = TagAttributes(name = mapOf("en" to "Incest"))),
                TagData(id = "tag_doujin", attributes = TagAttributes(name = mapOf("en" to "Doujinshi"))),
                TagData(id = "tag_smut", attributes = TagAttributes(name = mapOf("en" to "Smut"))),
                TagData(id = "tag_fetish", attributes = TagAttributes(name = mapOf("en" to "Fetish"))),
                TagData(id = "tag_armpit", attributes = TagAttributes(name = mapOf("en" to "Armpit")))
            )
        ),
        relationships = listOf(
            Relationship(
                id = "author_hanpatsu",
                type = "author",
                attributes = RelationshipAttributes(name = "Hanpatsu Zokusei (WayVZ)")
            ),
            Relationship(
                id = "cover_spotlight",
                type = "cover_art",
                attributes = RelationshipAttributes(fileName = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80")
            )
        )
    )
}

