package com.rsdgt.mihon.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsdgt.mihon.data.repository.MangaRepository
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val isLoading: Boolean = false,
    val mangas: List<SManga> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val query: String = "",
    val sortIndex: Int = 0, // 0: Newest, 1: Popular, 2: Popular Today, 3: Popular Week
    val errorMessage: String? = null
)

class CatalogViewModel(
    private val repository: MangaRepository = MangaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
    }

    fun onSearch() {
        _uiState.update { it.copy(page = 1) }
        loadCatalog()
    }

    fun onSortSelected(index: Int) {
        if (_uiState.value.sortIndex != index) {
            _uiState.update { it.copy(sortIndex = index, page = 1) }
            loadCatalog()
        }
    }

    fun nextPage() {
        if (_uiState.value.hasNextPage && !_uiState.value.isLoading) {
            _uiState.update { it.copy(page = it.page + 1) }
            loadCatalog()
        }
    }

    fun previousPage() {
        if (_uiState.value.page > 1 && !_uiState.value.isLoading) {
            _uiState.update { it.copy(page = it.page - 1) }
            loadCatalog()
        }
    }

    fun retry() {
        loadCatalog()
    }

    private fun loadCatalog() {
        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (currentState.query.isBlank() && currentState.sortIndex == 0) {
                repository.getLatestManga(currentState.page)
            } else if (currentState.query.isBlank() && currentState.sortIndex == 1) {
                repository.getPopularManga(currentState.page)
            } else {
                repository.searchManga(currentState.page, currentState.query, currentState.sortIndex)
            }

            result.fold(
                onSuccess = { mangasPage ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mangas = mangasPage.mangas,
                            hasNextPage = mangasPage.hasNextPage,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Failed to load catalog"
                        )
                    }
                }
            )
        }
    }
}
