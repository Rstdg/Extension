package com.rsdgt.mihon.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsdgt.mihon.data.repository.MangaRepository
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReaderUiState(
    val isLoading: Boolean = false,
    val pages: List<Page> = emptyList(),
    val currentPage: Int = 0,
    val errorMessage: String? = null
)

class ReaderViewModel(
    private val repository: MangaRepository = MangaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun loadPages(chapter: SChapter) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getPageList(chapter)
            result.fold(
                onSuccess = { pageList ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pages = pageList,
                            currentPage = 0,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.localizedMessage ?: "Failed to load reader pages"
                        )
                    }
                }
            )
        }
    }

    fun onPageSelected(index: Int) {
        _uiState.update { it.copy(currentPage = index) }
    }

    fun retry(chapter: SChapter) {
        loadPages(chapter)
    }
}
