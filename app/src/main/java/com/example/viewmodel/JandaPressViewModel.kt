package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.jandapress.JandaGalleryDetail
import com.example.api.jandapress.JandaGalleryItem
import com.example.api.jandapress.JandaPressClient
import com.example.api.jandapress.JandaProvider
import com.example.data.SettingsRepository
import com.example.repository.JandaPressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class JandaSubTab {
    EXPLORE,
    POPULAR,
    LATEST,
    PROVIDERS,
    SAVED
}

sealed class JandaUiState {
    object Idle : JandaUiState()
    object Loading : JandaUiState()
    data class Success(val data: List<JandaGalleryItem>, val page: Int = 1) : JandaUiState()
    data class Error(val message: String) : JandaUiState()
}

sealed class JandaDetailUiState {
    object Loading : JandaDetailUiState()
    data class Success(val detail: JandaGalleryDetail) : JandaDetailUiState()
    data class Error(val message: String) : JandaDetailUiState()
}

class JandaPressViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: JandaPressRepository = JandaPressRepository()
    private val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val _currentProvider = MutableStateFlow(JandaProvider.ALL)
    val currentProvider: StateFlow<JandaProvider> = _currentProvider

    private val _currentSubTab = MutableStateFlow(JandaSubTab.EXPLORE)
    val currentSubTab: StateFlow<JandaSubTab> = _currentSubTab

    private val _searchState = MutableStateFlow<JandaUiState>(JandaUiState.Idle)
    val searchState: StateFlow<JandaUiState> = _searchState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _detailState = MutableStateFlow<JandaDetailUiState>(JandaDetailUiState.Loading)
    val detailState: StateFlow<JandaDetailUiState> = _detailState

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds

    private val _savedGalleries = MutableStateFlow<List<JandaGalleryItem>>(emptyList())
    val savedGalleries: StateFlow<List<JandaGalleryItem>> = _savedGalleries

    val subscribedIds: StateFlow<Set<String>> = settingsRepository.subscribedIds.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        emptySet()
    )

    private val _apiUrl = MutableStateFlow(JandaPressClient.DEFAULT_PRIMARY_BASE_URL)
    val apiUrl: StateFlow<String> = _apiUrl

    init {
        viewModelScope.launch {
            settingsRepository.nhSavedIds.collectLatest { ids ->
                _savedIds.value = ids
                if (_currentSubTab.value == JandaSubTab.SAVED) {
                    loadSavedGalleries(ids)
                }
            }
        }
        loadExplore()
    }

    fun selectProvider(provider: JandaProvider) {
        _currentProvider.value = provider
        when (_currentSubTab.value) {
            JandaSubTab.EXPLORE, JandaSubTab.POPULAR -> loadPopular()
            JandaSubTab.LATEST -> loadLatest()
            JandaSubTab.SAVED -> loadSavedGalleries(_savedIds.value)
            JandaSubTab.PROVIDERS -> {}
        }
    }

    fun selectSubTab(tab: JandaSubTab) {
        _currentSubTab.value = tab
        when (tab) {
            JandaSubTab.EXPLORE -> {
                _selectedTag.value = null
                _searchQuery.value = ""
                loadExplore()
            }
            JandaSubTab.POPULAR -> {
                _selectedTag.value = null
                _searchQuery.value = ""
                loadPopular()
            }
            JandaSubTab.LATEST -> {
                _selectedTag.value = null
                _searchQuery.value = ""
                loadLatest()
            }
            JandaSubTab.PROVIDERS -> {}
            JandaSubTab.SAVED -> {
                loadSavedGalleries(_savedIds.value)
            }
        }
    }

    fun loadExplore() {
        viewModelScope.launch {
            _searchState.value = JandaUiState.Loading
            try {
                val results = repository.getCuratedGalleries(_currentProvider.value)
                _searchState.value = JandaUiState.Success(results, 1)
            } catch (e: Exception) {
                _searchState.value = JandaUiState.Error(e.localizedMessage ?: "Failed to load galleries")
            }
        }
    }

    fun loadPopular(page: Int = 1) {
        viewModelScope.launch {
            _searchState.value = JandaUiState.Loading
            try {
                val results = repository.getPopular(_currentProvider.value, page)
                _searchState.value = JandaUiState.Success(results, page)
            } catch (e: Exception) {
                _searchState.value = JandaUiState.Error(e.localizedMessage ?: "Failed to connect to JandaPress API")
            }
        }
    }

    fun loadLatest(page: Int = 1) {
        viewModelScope.launch {
            _searchState.value = JandaUiState.Loading
            try {
                val results = repository.getRecent(_currentProvider.value, page)
                _searchState.value = JandaUiState.Success(results, page)
            } catch (e: Exception) {
                _searchState.value = JandaUiState.Error(e.localizedMessage ?: "Failed to load recent works")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) {
            loadExplore()
            return
        }

        viewModelScope.launch {
            _searchState.value = JandaUiState.Loading
            try {
                val results = repository.search(query, _currentProvider.value, 1)
                if (results.isNotEmpty()) {
                    _searchState.value = JandaUiState.Success(results, 1)
                } else {
                    _searchState.value = JandaUiState.Error("No galleries found matching '$query'")
                }
            } catch (e: Exception) {
                _searchState.value = JandaUiState.Error(e.localizedMessage ?: "Search error")
            }
        }
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
        _searchQuery.value = tag
        performSearch()
    }

    fun loadDetail(provider: String, id: String) {
        viewModelScope.launch {
            _detailState.value = JandaDetailUiState.Loading
            try {
                val detail = repository.getDetail(provider, id)
                if (detail != null) {
                    _detailState.value = JandaDetailUiState.Success(detail)
                } else {
                    _detailState.value = JandaDetailUiState.Error("Could not retrieve gallery details.")
                }
            } catch (e: Exception) {
                _detailState.value = JandaDetailUiState.Error(e.localizedMessage ?: "Error loading gallery")
            }
        }
    }

    fun getRandomId(): String {
        return repository.getRandomId(_currentProvider.value)
    }

    fun toggleBookmark(id: String) {
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
                    mangaTitle = title ?: "Manga Series",
                    chapterTitle = "Subscribed to drops! Alerts active.",
                    coverUrl = coverUrl,
                    isSubscribed = true
                )
            }
        }
    }

    fun isBookmarked(id: String): Boolean {
        return _savedIds.value.contains(id)
    }

    fun setCustomApiUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotBlank()) {
            JandaPressClient.setCustomBaseUrl(trimmed)
            _apiUrl.value = trimmed
            loadExplore()
        }
    }

    private fun loadSavedGalleries(savedIds: Set<String>) {
        val all = repository.getCuratedGalleries(JandaProvider.ALL)
        val saved = all.filter { savedIds.contains(it.id) || savedIds.contains(it.id.substringAfter("_")) }
        _savedGalleries.value = saved
        _searchState.value = JandaUiState.Success(saved, 1)
    }
}
