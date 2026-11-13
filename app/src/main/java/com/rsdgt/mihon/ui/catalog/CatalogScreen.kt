package com.rsdgt.mihon.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsdgt.mihon.ui.components.MangaCard
import com.rsdgt.mihon.ui.theme.MihonTheme
import eu.kanade.tachiyomi.source.model.SManga

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    onMangaClick: (SManga) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    CatalogContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onSortSelected = viewModel::onSortSelected,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage,
        onRetry = viewModel::retry,
        onMangaClick = onMangaClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogContent(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSortSelected: (Int) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onRetry: () -> Unit,
    onMangaClick: (SManga) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val sortOptions = listOf("Newest", "Popular", "Popular Today", "Popular Week")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NHentai Catalog",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input & Action
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search title, tags, or id:123456...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = {
                            onQueryChange("")
                            onSearch()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    onSearch()
                }),
                shape = MaterialTheme.shapes.large
            )

            // Sort Options Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Sort filter",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                itemsIndexed(sortOptions) { index, option ->
                    FilterChip(
                        selected = state.sortIndex == index,
                        onClick = { onSortSelected(index) },
                        label = { Text(option) }
                    )
                }
            }

            // Main Catalog Content Grid / Loading / Error
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Loading catalog...",
                                    style = MaterialTheme.typography.bodyMedium
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

                    state.mangas.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No galleries found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.mangas) { manga ->
                                MangaCard(
                                    manga = manga,
                                    onMangaClick = onMangaClick
                                )
                            }
                        }
                    }
                }
            }

            // Pagination Controls Bar
            AnimatedVisibility(visible = !state.isLoading && state.errorMessage == null && state.mangas.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onPreviousPage,
                            enabled = state.page > 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Previous Page"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        Text(
                            text = "Page ${state.page}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = onNextPage,
                            enabled = state.hasNextPage
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Next Page"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogScreenPreview() {
    val mockState = CatalogUiState(
        isLoading = false,
        mangas = listOf(
            SManga.create().apply {
                title = "Gallery Title 1"
                status = SManga.COMPLETED
                genre = "doujinshi"
            },
            SManga.create().apply {
                title = "Gallery Title 2"
                status = SManga.COMPLETED
                genre = "japanese"
            }
        ),
        page = 1,
        hasNextPage = true
    )
    MihonTheme {
        CatalogContent(
            state = mockState,
            onQueryChange = {},
            onSearch = {},
            onSortSelected = {},
            onNextPage = {},
            onPreviousPage = {},
            onRetry = {},
            onMangaClick = {}
        )
    }
}
