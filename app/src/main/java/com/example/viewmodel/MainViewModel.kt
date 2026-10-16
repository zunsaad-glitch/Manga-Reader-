package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.Chapter
import com.example.model.ContentRatingPreset
import com.example.model.Manga
import com.example.model.MangaSource
import com.example.model.ReadingProgress
import com.example.repository.MangaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MangaRepository = MangaRepository()
) : ViewModel() {

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

    private val _continueReadingList = MutableStateFlow<List<ReadingProgress>>(emptyList())
    val continueReadingList: StateFlow<List<ReadingProgress>> = _continueReadingList.asStateFlow()

    private val _bookmarkedMangaIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedMangaIds: StateFlow<Set<String>> = _bookmarkedMangaIds.asStateFlow()

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
            contentRatings = _selectedContentRatings.value
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
            contentRatings = _selectedContentRatings.value
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
                    contentRatings = _selectedContentRatings.value
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
                    contentRatings = _selectedContentRatings.value
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
        val current = _bookmarkedMangaIds.value.toMutableSet()
        if (current.contains(mangaId)) {
            current.remove(mangaId)
        } else {
            current.add(mangaId)
        }
        _bookmarkedMangaIds.value = current
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
    }
}
