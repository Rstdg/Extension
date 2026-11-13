package com.rsdgt.mihon.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsdgt.mihon.ui.components.NHentaiImage
import com.rsdgt.mihon.ui.theme.MihonTheme
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    chapter: SChapter,
    mangaTitle: String,
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(chapter.url) {
        viewModel.loadPages(chapter)
    }

    ReaderContent(
        state = state,
        mangaTitle = mangaTitle,
        chapterName = chapter.name,
        onBackClick = onBackClick,
        onPageSelected = viewModel::onPageSelected,
        onRetry = { viewModel.retry(chapter) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderContent(
    state: ReaderUiState,
    mangaTitle: String,
    chapterName: String,
    onBackClick: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showControls by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading pages...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetry) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                state.pages.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pages found for this chapter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }

                else -> {
                    val pagerState = rememberPagerState(
                        initialPage = state.currentPage,
                        pageCount = { state.pages.size }
                    )

                    LaunchedEffect(pagerState.currentPage) {
                        onPageSelected(pagerState.currentPage)
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val page = state.pages[pageIndex]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showControls = !showControls
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            NHentaiImage(
                                url = page.imageUrl,
                                contentDescription = "Page ${page.index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Top Bar Overlay
                    AnimatedVisibility(
                        visible = showControls,
                        enter = slideInVertically { -it },
                        exit = slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = mangaTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        color = Color.White
                                    )
                                    Text(
                                        text = chapterName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBackClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    }

                    // Bottom Bar Controls Overlay
                    AnimatedVisibility(
                        visible = showControls,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            contentColor = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Page ${pagerState.currentPage + 1} of ${state.pages.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (state.pages.size > 1) {
                                    Slider(
                                        value = pagerState.currentPage.toFloat(),
                                        onValueChange = { value ->
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(value.toInt())
                                            }
                                        },
                                        valueRange = 0f..(state.pages.size - 1).toFloat(),
                                        steps = (state.pages.size - 2).coerceAtLeast(0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReaderScreenPreview() {
    val sampleChapter = SChapter.create().apply {
        name = "Read Gallery"
    }
    val samplePages = listOf(
        Page(0, imageUrl = ""),
        Page(1, imageUrl = "")
    )
    val mockState = ReaderUiState(
        isLoading = false,
        pages = samplePages,
        currentPage = 0
    )
    MihonTheme {
        ReaderContent(
            state = mockState,
            mangaTitle = "Sample Gallery",
            chapterName = sampleChapter.name,
            onBackClick = {},
            onPageSelected = {},
            onRetry = {}
        )
    }
}
