package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Chapter
import com.example.model.ContentRatingPreset
import com.example.model.Manga
import com.example.model.MangaSource
import com.example.model.MangaVariety
import com.example.model.ReadingProgress
import com.example.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MangaRepository = MangaRepository()
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mangaverse_prefs", Context.MODE_PRIVATE)

    // Comprehensive list of available tags including requested tags & genres
    val availableTags = listOf(
        "All",
        "Incest",
        "Harem",
        "Doujinshi",
        "Gore",
        "Sexual Violence",
        "Crossdressing",
        "Psychological",
        "Romance",
        "Action",
        "Fantasy",
        "Comedy",
        "Isekai",
        "Shounen",
        "Drama",
        "Adventure",
        "Supernatural",
        "Mystery",
        "Slice of Life",
        "Martial Arts",
        "Horror",
        "Historical",
        "Sci-Fi",
        "School Life",
        "Tragedy",
        "Thriller",
        "Crime",
        "Demons",
        "Vampires",
        "Reincarnation",
        "Boys' Love",
        "Girls' Love",
        "Mecha",
        "Sports",
        "Web Comic",
        "Gyaru",
        "Monster Girls",
        "Survival",
        "Post-Apocalyptic",
        "Villainess",
        "Zombies"
    )

    // Tag sets that typically contain explicit/mature content
    val adultTags = setOf(
        "Incest", "Gore", "Sexual Violence", "Doujinshi", "Harem", "Reverse Harem", "Gyaru", "Crossdressing"
    )

    private val _selectedSource = MutableStateFlow(MangaSource.ALL)
    val selectedSource: StateFlow<MangaSource> = _selectedSource.asStateFlow()

    private val _selectedVariety = MutableStateFlow(MangaVariety.ALL_POPULAR)
    val selectedVariety: StateFlow<MangaVariety> = _selectedVariety.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>("All")
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Default to All Content (including 18+) so user doesn't hit empty results, with easy filter toggles
    private val _selectedContentRatings = MutableStateFlow<Set<String>>(
        setOf("safe", "suggestive", "erotica", "pornographic")
    )
    val selectedContentRatings: StateFlow<Set<String>> = _selectedContentRatings.asStateFlow()

    private val _contentRatingPreset = MutableStateFlow(ContentRatingPreset.ALL)
    val contentRatingPreset: StateFlow<ContentRatingPreset> = _contentRatingPreset.asStateFlow()

    private val _mangas = MutableStateFlow<List<Manga>>(emptyList())
    val mangas: StateFlow<List<Manga>> = _mangas.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Persistent Bookmarks & Reading History
    private val _bookmarkedMangas = MutableStateFlow<Map<String, Manga>>(loadPersistedBookmarks())
    val bookmarkedMangas: StateFlow<Map<String, Manga>> = _bookmarkedMangas.asStateFlow()

    private val _bookmarkedMangaIds = MutableStateFlow<Set<String>>(_bookmarkedMangas.value.keys)
    val bookmarkedMangaIds: StateFlow<Set<String>> = _bookmarkedMangaIds.asStateFlow()

    private val _continueReadingList = MutableStateFlow<List<ReadingProgress>>(loadPersistedReadingHistory())
    val continueReadingList: StateFlow<List<ReadingProgress>> = _continueReadingList.asStateFlow()

    private val _selectedManga = MutableStateFlow<Manga?>(null)
    val selectedManga: StateFlow<Manga?> = _selectedManga.asStateFlow()

    private val _selectedMangaChapters = MutableStateFlow<List<Chapter>>(emptyList())
    val selectedMangaChapters: StateFlow<List<Chapter>> = _selectedMangaChapters.asStateFlow()

    private val _isChaptersLoading = MutableStateFlow(false)
    val isChaptersLoading: StateFlow<Boolean> = _isChaptersLoading.asStateFlow()

    private val _activeChapter = MutableStateFlow<Chapter?>(null)
    val activeChapter: StateFlow<Chapter?> = _activeChapter.asStateFlow()

    private val _activeReaderManga = MutableStateFlow<Manga?>(null)
    val activeReaderManga: StateFlow<Manga?> = _activeReaderManga.asStateFlow()

    private val _isLoadingPages = MutableStateFlow(false)
    val isLoadingPages: StateFlow<Boolean> = _isLoadingPages.asStateFlow()

    // Persistent Chapter sequence order preference (default: Ascending / Ch. 1 -> Last)
    private val _isAscendingChapterOrder = MutableStateFlow(prefs.getBoolean("is_ascending_chapter_order", true))
    val isAscendingChapterOrder: StateFlow<Boolean> = _isAscendingChapterOrder.asStateFlow()

    fun toggleChapterOrder() {
        val newOrder = !_isAscendingChapterOrder.value
        _isAscendingChapterOrder.value = newOrder
        prefs.edit().putBoolean("is_ascending_chapter_order", newOrder).apply()
    }

    fun setChapterOrder(ascending: Boolean) {
        _isAscendingChapterOrder.value = ascending
        prefs.edit().putBoolean("is_ascending_chapter_order", ascending).apply()
    }

    init {
        refreshMangas()
    }

    fun selectSource(source: MangaSource) {
        if (_selectedSource.value == source) return
        _selectedSource.value = source
        _currentPage.value = 1
        _hasMore.value = true

        // Instant Cache Hit for 0ms transition!
        val cached = repository.getCachedMangas(
            source = source,
            page = 1,
            selectedTag = _selectedTag.value,
            searchQuery = _searchQuery.value,
            contentRatings = _selectedContentRatings.value,
            variety = _selectedVariety.value
        )
        if (!cached.isNullOrEmpty()) {
            _mangas.value = cached
            _isLoading.value = false
        } else {
            _mangas.value = emptyList()
            _isLoading.value = true
        }

        refreshMangas()
    }

    fun selectVariety(variety: MangaVariety) {
        if (_selectedVariety.value == variety) return
        _selectedVariety.value = variety
        _currentPage.value = 1
        _hasMore.value = true

        val cached = repository.getCachedMangas(
            source = _selectedSource.value,
            page = 1,
            selectedTag = _selectedTag.value,
            searchQuery = _searchQuery.value,
            contentRatings = _selectedContentRatings.value,
            variety = variety
        )
        if (!cached.isNullOrEmpty()) {
            _mangas.value = cached
            _isLoading.value = false
        } else {
            _mangas.value = emptyList()
            _isLoading.value = true
        }

        refreshMangas()
    }

    fun selectTag(tag: String?) {
        val newTag = if (_selectedTag.value == tag && tag != "All") "All" else (tag ?: "All")
        if (_selectedTag.value == newTag && _mangas.value.isNotEmpty()) return
        _selectedTag.value = newTag
        _currentPage.value = 1
        _hasMore.value = true

        // If an explicit tag is selected, automatically enable erotica and pornographic so MangaDex returns results
        if (adultTags.contains(newTag)) {
            val updated = _selectedContentRatings.value + setOf("erotica", "pornographic")
            _selectedContentRatings.value = updated
            updatePresetFromRatings(updated)
        }

        // Instant Cache Hit for 0ms transition!
        val cached = repository.getCachedMangas(
            source = _selectedSource.value,
            page = 1,
            selectedTag = newTag,
            searchQuery = _searchQuery.value,
            contentRatings = _selectedContentRatings.value,
            variety = _selectedVariety.value
        )
        if (!cached.isNullOrEmpty()) {
            _mangas.value = cached
            _isLoading.value = false
        } else {
            _mangas.value = emptyList()
            _isLoading.value = true
        }

        refreshMangas()
    }

    private var searchJob: Job? = null

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1
        _hasMore.value = true
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                delay(250)
            }
            refreshMangas()
        }
    }

    fun setContentRatingPreset(preset: ContentRatingPreset) {
        _contentRatingPreset.value = preset
        _selectedContentRatings.value = preset.ratings
        _currentPage.value = 1
        _hasMore.value = true
        refreshMangas()
    }

    fun toggleContentRating(rating: String) {
        val current = _selectedContentRatings.value.toMutableSet()
        if (current.contains(rating)) {
            // Keep at least one rating active
            if (current.size > 1) {
                current.remove(rating)
            }
        } else {
            current.add(rating)
        }
        _selectedContentRatings.value = current
        updatePresetFromRatings(current)
        _currentPage.value = 1
        _hasMore.value = true
        refreshMangas()
    }

    private fun updatePresetFromRatings(ratings: Set<String>) {
        _contentRatingPreset.value = when {
            ratings == ContentRatingPreset.ALL.ratings -> ContentRatingPreset.ALL
            ratings == ContentRatingPreset.SAFE_CLEAN.ratings -> ContentRatingPreset.SAFE_CLEAN
            ratings == ContentRatingPreset.EXPLICIT_ONLY.ratings -> ContentRatingPreset.EXPLICIT_ONLY
            else -> ContentRatingPreset.ALL
        }
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedTag.value = "All"
        _selectedSource.value = MangaSource.ALL
        _selectedVariety.value = MangaVariety.ALL_POPULAR
        _selectedContentRatings.value = ContentRatingPreset.ALL.ratings
        _contentRatingPreset.value = ContentRatingPreset.ALL
        _currentPage.value = 1
        _hasMore.value = true
        refreshMangas()
    }

    fun refreshMangas() {
        viewModelScope.launch {
            if (_mangas.value.isEmpty()) {
                _isLoading.value = true
            }
            _currentPage.value = 1
            try {
                val list = repository.getMangas(
                    source = _selectedSource.value,
                    page = 1,
                    selectedTag = _selectedTag.value,
                    searchQuery = _searchQuery.value,
                    contentRatings = _selectedContentRatings.value,
                    variety = _selectedVariety.value
                )
                _mangas.value = list
                _hasMore.value = list.isNotEmpty()
            } catch (e: Exception) {
                // Keep current on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            val nextPage = _currentPage.value + 1
            try {
                val more = repository.getMangas(
                    source = _selectedSource.value,
                    page = nextPage,
                    selectedTag = _selectedTag.value,
                    searchQuery = _searchQuery.value,
                    contentRatings = _selectedContentRatings.value,
                    variety = _selectedVariety.value
                )
                if (more.isNotEmpty()) {
                    val existingIds = _mangas.value.map { it.id }.toSet()
                    val newItems = more.filterNot { existingIds.contains(it.id) }
                    _currentPage.value = nextPage
                    if (newItems.isNotEmpty()) {
                        _mangas.value = _mangas.value + newItems
                    }
                    // Continue infinite scrolling as long as API returns titles
                    _hasMore.value = more.isNotEmpty()
                } else {
                    _hasMore.value = false
                }
            } catch (e: Exception) {
                // Ignore transient paging failure
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun openMangaDetails(manga: Manga) {
        _selectedManga.value = manga
        _isChaptersLoading.value = true
        viewModelScope.launch {
            try {
                val chs = repository.getChapters(manga.id)
                _selectedMangaChapters.value = chs
            } catch (e: Exception) {
                _selectedMangaChapters.value = emptyList()
            } finally {
                _isChaptersLoading.value = false
            }
        }
    }

    fun resumeReading(progress: ReadingProgress) {
        val resolved = _mangas.value.find { it.id == progress.mangaId }
            ?: repository.getCachedManga(progress.mangaId)
            ?: _bookmarkedMangas.value[progress.mangaId]
            ?: Manga(
                id = progress.mangaId,
                title = progress.mangaTitle,
                coverUrl = progress.coverUrl,
                author = "Unknown",
                synopsis = "Reading in progress...",
                genres = listOf("Webtoon"),
                source = progress.source,
                latestChapter = "Ch. ${progress.lastChapter.toInt()}"
            )
        openMangaDetails(resolved)
    }

    fun closeMangaDetails() {
        _selectedManga.value = null
        _selectedMangaChapters.value = emptyList()
        _isChaptersLoading.value = false
    }

    fun openReader(chapter: Chapter, manga: Manga) {
        _activeChapter.value = chapter
        _activeReaderManga.value = manga
        _isLoadingPages.value = true

        saveReadingProgress(
            mangaId = manga.id,
            mangaTitle = manga.title,
            coverUrl = manga.coverUrl,
            source = manga.source,
            chapter = chapter.chapterNumber,
            page = 1,
            totalPages = chapter.pages.size.coerceAtLeast(1)
        )

        viewModelScope.launch {
            try {
                val realPages = repository.getChapterPages(chapter.id)
                if (realPages.isNotEmpty()) {
                    _activeChapter.value = chapter.copy(pages = realPages)
                }
            } catch (e: Exception) {
                // Keep active
            } finally {
                _isLoadingPages.value = false
            }
        }
    }

    fun retryActiveChapterPages() {
        val chapter = _activeChapter.value ?: return
        _isLoadingPages.value = true
        viewModelScope.launch {
            try {
                val realPages = repository.getChapterPages(chapter.id)
                if (realPages.isNotEmpty()) {
                    _activeChapter.value = chapter.copy(pages = realPages)
                }
            } catch (e: Exception) {
                // Error handled in UI
            } finally {
                _isLoadingPages.value = false
            }
        }
    }

    fun closeReader() {
        _activeChapter.value = null
        _activeReaderManga.value = null
        _isLoadingPages.value = false
    }

    fun toggleBookmark(mangaId: String) {
        val manga = _mangas.value.find { it.id == mangaId }
            ?: repository.getCachedManga(mangaId)
            ?: _selectedManga.value
            ?: _bookmarkedMangas.value[mangaId]

        val currentMap = _bookmarkedMangas.value.toMutableMap()
        if (currentMap.containsKey(mangaId)) {
            currentMap.remove(mangaId)
        } else if (manga != null) {
            currentMap[mangaId] = manga
        }
        _bookmarkedMangas.value = currentMap
        _bookmarkedMangaIds.value = currentMap.keys
        persistBookmarks(currentMap)
    }

    fun saveReadingProgress(
        mangaId: String,
        mangaTitle: String,
        coverUrl: String,
        source: MangaSource,
        chapter: Float,
        page: Int,
        totalPages: Int
    ) {
        val existing = _continueReadingList.value.filterNot { it.mangaId == mangaId }
        val updated = listOf(
            ReadingProgress(
                mangaId = mangaId,
                mangaTitle = mangaTitle,
                coverUrl = coverUrl,
                source = source,
                lastChapter = chapter,
                lastPage = page,
                totalPages = totalPages,
                timestamp = System.currentTimeMillis()
            )
        ) + existing
        _continueReadingList.value = updated
        persistReadingHistory(updated)
    }

    private fun loadPersistedBookmarks(): Map<String, Manga> {
        val map = mutableMapOf<String, Manga>()
        try {
            val jsonStr = prefs.getString("bookmarked_mangas_json", null) ?: return emptyMap()
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val genresList = mutableListOf<String>()
                val gArr = obj.optJSONArray("genres")
                if (gArr != null) {
                    for (g in 0 until gArr.length()) {
                        genresList.add(gArr.getString(g))
                    }
                }
                val manga = Manga(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    coverUrl = obj.getString("coverUrl"),
                    author = obj.optString("author", "Unknown"),
                    rating = obj.optDouble("rating", 4.8).toFloat(),
                    status = obj.optString("status", "Ongoing"),
                    synopsis = obj.optString("synopsis", ""),
                    genres = if (genresList.isNotEmpty()) genresList else listOf("Action"),
                    source = try { MangaSource.valueOf(obj.optString("source", "ALL")) } catch (_: Throwable) { MangaSource.ALL },
                    totalChapters = obj.optInt("totalChapters", 100),
                    isTrending = obj.optBoolean("isTrending", false),
                    isEditorPick = obj.optBoolean("isEditorPick", false),
                    latestChapter = obj.optString("latestChapter", "Ch. 1"),
                    contentRating = obj.optString("contentRating", "safe")
                )
                map[manga.id] = manga
            }
        } catch (_: Throwable) {}
        return map
    }

    private fun persistBookmarks(map: Map<String, Manga>) {
        try {
            val arr = JSONArray()
            map.values.forEach { m ->
                val obj = JSONObject()
                obj.put("id", m.id)
                obj.put("title", m.title)
                obj.put("coverUrl", m.coverUrl)
                obj.put("author", m.author)
                obj.put("rating", m.rating.toDouble())
                obj.put("status", m.status)
                obj.put("synopsis", m.synopsis)
                val gArr = JSONArray()
                m.genres.forEach { gArr.put(it) }
                obj.put("genres", gArr)
                obj.put("source", m.source.name)
                obj.put("totalChapters", m.totalChapters)
                obj.put("isTrending", m.isTrending)
                obj.put("isEditorPick", m.isEditorPick)
                obj.put("latestChapter", m.latestChapter)
                obj.put("contentRating", m.contentRating)
                arr.put(obj)
            }
            prefs.edit().putString("bookmarked_mangas_json", arr.toString()).apply()
        } catch (_: Throwable) {}
    }

    private fun loadPersistedReadingHistory(): List<ReadingProgress> {
        val list = mutableListOf<ReadingProgress>()
        try {
            val jsonStr = prefs.getString("reading_history_json", null) ?: return emptyList()
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ReadingProgress(
                        mangaId = obj.getString("mangaId"),
                        mangaTitle = obj.getString("mangaTitle"),
                        coverUrl = obj.getString("coverUrl"),
                        source = try { MangaSource.valueOf(obj.optString("source", "ALL")) } catch (_: Throwable) { MangaSource.ALL },
                        lastChapter = obj.optDouble("lastChapter", 1.0).toFloat(),
                        lastPage = obj.optInt("lastPage", 1),
                        totalPages = obj.optInt("totalPages", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Throwable) {}
        return list
    }

    private fun persistReadingHistory(list: List<ReadingProgress>) {
        try {
            val arr = JSONArray()
            list.take(50).forEach { item ->
                val obj = JSONObject()
                obj.put("mangaId", item.mangaId)
                obj.put("mangaTitle", item.mangaTitle)
                obj.put("coverUrl", item.coverUrl)
                obj.put("source", item.source.name)
                obj.put("lastChapter", item.lastChapter.toDouble())
                obj.put("lastPage", item.lastPage)
                obj.put("totalPages", item.totalPages)
                obj.put("timestamp", item.timestamp)
                arr.put(obj)
            }
            prefs.edit().putString("reading_history_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }
}
