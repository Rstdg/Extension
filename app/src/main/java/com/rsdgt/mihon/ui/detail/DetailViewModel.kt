package com.rsdgt.mihon.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rsdgt.mihon.data.repository.MangaRepository
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = false,
    val manga: SManga? = null,
    val chapters: List<SChapter> = emptyList(),
    val errorMessage: String? = null
)

class DetailViewModel(
    private val repository: MangaRepository = MangaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadDetails(manga: SManga) {
        _uiState.update { it.copy(isLoading = true, manga = manga, errorMessage = null) }
        viewModelScope.launch {
            val detailsResult = repository.getMangaDetails(manga)
            val chaptersResult = repository.getChapterList(manga)

            if (detailsResult.isSuccess && chaptersResult.isSuccess) {
                val updatedManga = detailsResult.getOrThrow()
                val chapters = chaptersResult.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        manga = updatedManga,
                        chapters = chapters,
                        errorMessage = null
                    )
                }
            } else {
                val err = detailsResult.exceptionOrNull()?.localizedMessage
                    ?: chaptersResult.exceptionOrNull()?.localizedMessage
                    ?: "Failed to load manga details"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun retry(manga: SManga) {
        loadDetails(manga)
    }
}
