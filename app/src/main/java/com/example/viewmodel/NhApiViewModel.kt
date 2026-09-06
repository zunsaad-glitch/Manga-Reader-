package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.nhapi.NhGallery
import com.example.api.nhapi.NhGalleryDetail
import com.example.data.SettingsRepository
import com.example.repository.NhApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NhSubTab {
    POPULAR,
    LATEST,
    TAGS,
    SAVED
}

sealed class NhUiState {
    object Idle : NhUiState()
    object Loading : NhUiState()
    data class Success(val data: List<NhGallery>, val page: Int = 1) : NhUiState()
    data class Error(val message: String) : NhUiState()
}

sealed class NhDetailUiState {
    object Loading : NhDetailUiState()
    data class Success(val detail: NhGalleryDetail) : NhDetailUiState()
    data class Error(val message: String) : NhDetailUiState()
}

class NhApiViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: NhApiRepository = NhApiRepository()
    private val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val _currentSubTab = MutableStateFlow(NhSubTab.POPULAR)
    val currentSubTab: StateFlow<NhSubTab> = _currentSubTab

    private val _searchState = MutableStateFlow<NhUiState>(NhUiState.Idle)
    val searchState: StateFlow<NhUiState> = _searchState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _detailState = MutableStateFlow<NhDetailUiState>(NhDetailUiState.Loading)
    val detailState: StateFlow<NhDetailUiState> = _detailState

    private val _pages = MutableStateFlow<List<String>>(emptyList())
    val pages: StateFlow<List<String>> = _pages

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds

    private val _savedGalleries = MutableStateFlow<List<NhGallery>>(emptyList())
    val savedGalleries: StateFlow<List<NhGallery>> = _savedGalleries

    val subscribedIds: StateFlow<Set<String>> = settingsRepository.subscribedIds.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        emptySet()
    )

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var currentSearchPage = 1

    init {
        viewModelScope.launch {
            settingsRepository.nhSavedIds.collectLatest { ids ->
                _savedIds.value = ids
                if (_currentSubTab.value == NhSubTab.SAVED) {
                    loadSavedGalleries(ids)
                }
            }
        }
        loadPopular()
    }

    fun selectSubTab(tab: NhSubTab) {
        _currentSubTab.value = tab
        when (tab) {
            NhSubTab.POPULAR -> {
                _selectedTag.value = null
                _searchQuery.value = ""
                loadPopular()
            }
            NhSubTab.LATEST -> {
                _selectedTag.value = null
                _searchQuery.value = ""
                loadLatest()
            }
            NhSubTab.TAGS -> {
                if (_selectedTag.value == null) {
                    selectTag("doujinshi")
                }
            }
            NhSubTab.SAVED -> {
                loadSavedGalleries(_savedIds.value)
            }
        }
    }

    fun loadPopular(page: Int = 1, append: Boolean = false) {
        currentSearchPage = page
        viewModelScope.launch {
            if (!append) _searchState.value = NhUiState.Loading else _isLoadingMore.value = true
            try {
                val results = repository.getPopular(page)
                if (append && _searchState.value is NhUiState.Success) {
                    val current = (_searchState.value as NhUiState.Success).data
                    _searchState.value = NhUiState.Success(current + results, page)
                } else {
                    if (results.isNotEmpty()) {
                        _searchState.value = NhUiState.Success(results, page)
                    } else {
                        _searchState.value = NhUiState.Error("No galleries found.")
                    }
                }
            } catch (e: Exception) {
                if (!append) {
                    _searchState.value = NhUiState.Error(e.localizedMessage ?: "Failed to connect to gallery service.")
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun loadLatest(page: Int = 1, append: Boolean = false) {
        currentSearchPage = page
        viewModelScope.launch {
            if (!append) _searchState.value = NhUiState.Loading else _isLoadingMore.value = true
            try {
                val results = repository.getRecent(page)
                if (append && _searchState.value is NhUiState.Success) {
                    val current = (_searchState.value as NhUiState.Success).data
                    _searchState.value = NhUiState.Success(current + results, page)
                } else {
                    if (results.isNotEmpty()) {
                        _searchState.value = NhUiState.Success(results, page)
                    } else {
                        _searchState.value = NhUiState.Error("No recent galleries found.")
                    }
                }
            } catch (e: Exception) {
                if (!append) {
                    _searchState.value = NhUiState.Error(e.localizedMessage ?: "Failed to connect to gallery service.")
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
        _searchQuery.value = tag
        search(tag)
    }

    fun search(query: String, page: Int = 1, append: Boolean = false) {
        val q = query.trim()
        _searchQuery.value = q
        currentSearchPage = page
        viewModelScope.launch {
            if (!append) _searchState.value = NhUiState.Loading else _isLoadingMore.value = true
            try {
                val results = repository.search(if (q.isBlank()) "english" else q, page = page)
                if (append && _searchState.value is NhUiState.Success) {
                    val current = (_searchState.value as NhUiState.Success).data
                    _searchState.value = NhUiState.Success(current + results, page)
                } else {
                    if (results.isNotEmpty()) {
                        _searchState.value = NhUiState.Success(results, page)
                    } else {
                        _searchState.value = NhUiState.Error("No galleries found for '$q'. Try another tag or 6-digit ID.")
                    }
                }
            } catch (e: Exception) {
                if (!append) {
                    _searchState.value = NhUiState.Error(e.localizedMessage ?: "Failed to connect to gallery service.")
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value) return
        val nextPage = currentSearchPage + 1
        when (_currentSubTab.value) {
            NhSubTab.POPULAR -> loadPopular(nextPage, append = true)
            NhSubTab.LATEST -> loadLatest(nextPage, append = true)
            NhSubTab.TAGS, NhSubTab.SAVED -> {
                val q = _searchQuery.value.ifBlank { _selectedTag.value ?: "english" }
                search(q, nextPage, append = true)
            }
        }
    }

    fun toggleSaved(id: String) {
        viewModelScope.launch {
            settingsRepository.toggleNhSaved(id)
        }
    }

    fun toggleSubscription(id: String, title: String? = null, coverUrl: String? = null) {
        viewModelScope.launch {
            val wasSubscribed = subscribedIds.value.contains(id)
            settingsRepository.toggleSubscription(id)
            if (!wasSubscribed) {
                com.example.util.MangaNotificationManager.showMangaDropNotification(
                    context = getApplication(),
                    mangaId = id,
                    mangaTitle = title ?: "Gallery #${id}",
                    chapterTitle = "Subscribed to drops! Alerts active.",
                    coverUrl = coverUrl,
                    isSubscribed = true
                )
            }
        }
    }

    private fun loadSavedGalleries(ids: Set<String>) {
        viewModelScope.launch {
            if (ids.isEmpty()) {
                _savedGalleries.value = emptyList()
                return@launch
            }
            val list = repository.getByIds(ids)
            _savedGalleries.value = list
        }
    }

    fun getRandomCuratedId(): String {
        return repository.getRandomCuratedId()
    }

    fun fetchDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = NhDetailUiState.Loading
            try {
                val result = repository.getDetail(id)
                if (result != null) {
                    _detailState.value = NhDetailUiState.Success(result)
                    _pages.value = result.pages
                } else {
                    _detailState.value = NhDetailUiState.Error("Failed to load gallery details.")
                }
            } catch (e: Exception) {
                _detailState.value = NhDetailUiState.Error(e.localizedMessage ?: "Error loading gallery.")
            }
        }
    }

    fun fetchPages(id: String) {
        viewModelScope.launch {
            try {
                val result = repository.getPages(id)
                _pages.value = result
            } catch (_: Exception) {}
        }
    }
}
