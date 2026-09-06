package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GenericGalleryRepository
import com.example.data.model.GenericGalleryDetailResponse
import com.example.data.model.GenericSearchResultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenericSearchUiState {
    data object Idle : GenericSearchUiState
    data object Loading : GenericSearchUiState
    data class Success(val items: List<GenericSearchResultItem>, val query: String) : GenericSearchUiState
    data class Error(val message: String) : GenericSearchUiState
}

sealed interface GenericDetailUiState {
    data object Idle : GenericDetailUiState
    data object Loading : GenericDetailUiState
    data class Success(val gallery: GenericGalleryDetailResponse) : GenericDetailUiState
    data class Error(val message: String) : GenericDetailUiState
}

class GenericGalleryViewModel(
    private val repository: GenericGalleryRepository = GenericGalleryRepository()
) : ViewModel() {

    private val _searchState = MutableStateFlow<GenericSearchUiState>(GenericSearchUiState.Idle)
    val searchState: StateFlow<GenericSearchUiState> = _searchState.asStateFlow()

    private val _detailState = MutableStateFlow<GenericDetailUiState>(GenericDetailUiState.Idle)
    val detailState: StateFlow<GenericDetailUiState> = _detailState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _serverUrl = MutableStateFlow("https://api.example.com/")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        repository.updateBaseUrl(url)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun search(query: String = _searchQuery.value, page: Int = 1) {
        if (query.isBlank()) return
        _searchQuery.value = query
        _searchState.value = GenericSearchUiState.Loading

        viewModelScope.launch {
            repository.searchGalleries(query, page)
                .onSuccess { results ->
                    _searchState.value = GenericSearchUiState.Success(results, query)
                }
                .onFailure { error ->
                    _searchState.value = GenericSearchUiState.Error(error.localizedMessage ?: "Unknown search error")
                }
        }
    }

    fun loadGalleryDetail(id: String) {
        _detailState.value = GenericDetailUiState.Loading
        viewModelScope.launch {
            repository.getGalleryDetail(id)
                .onSuccess { detail ->
                    _detailState.value = GenericDetailUiState.Success(detail)
                }
                .onFailure { error ->
                    _detailState.value = GenericDetailUiState.Error(error.localizedMessage ?: "Failed to load gallery")
                }
        }
    }

    fun resetDetailState() {
        _detailState.value = GenericDetailUiState.Idle
    }
}
