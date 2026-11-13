package com.rsdgt.mihon.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rsdgt.mihon.ui.catalog.CatalogScreen
import com.rsdgt.mihon.ui.catalog.CatalogViewModel
import com.rsdgt.mihon.ui.detail.DetailScreen
import com.rsdgt.mihon.ui.detail.DetailViewModel
import com.rsdgt.mihon.ui.reader.ReaderScreen
import com.rsdgt.mihon.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

sealed interface Screen {
    data object Catalog : Screen
    data class Detail(val manga: SManga) : Screen
    data class Reader(val chapter: SChapter, val mangaTitle: String) : Screen
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    catalogViewModel: CatalogViewModel = viewModel(),
    detailViewModel: DetailViewModel = viewModel(),
    readerViewModel: ReaderViewModel = viewModel()
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Catalog) }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLast()
    }

    val currentScreen = backStack.lastOrNull() ?: Screen.Catalog

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition",
        modifier = modifier
    ) { targetScreen ->
        when (targetScreen) {
            is Screen.Catalog -> {
                CatalogScreen(
                    viewModel = catalogViewModel,
                    onMangaClick = { manga ->
                        backStack.add(Screen.Detail(manga))
                    }
                )
            }

            is Screen.Detail -> {
                DetailScreen(
                    initialManga = targetScreen.manga,
                    viewModel = detailViewModel,
                    onBackClick = { backStack.removeLast() },
                    onReadChapterClick = { chapter ->
                        backStack.add(Screen.Reader(chapter, targetScreen.manga.title))
                    }
                )
            }

            is Screen.Reader -> {
                ReaderScreen(
                    chapter = targetScreen.chapter,
                    mangaTitle = targetScreen.mangaTitle,
                    viewModel = readerViewModel,
                    onBackClick = { backStack.removeLast() }
                )
            }
        }
    }
}
