package com.rsdgt.mihon.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rsdgt.mihon.ui.components.NHentaiImage
import com.rsdgt.mihon.ui.theme.MihonTheme
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    initialManga: SManga,
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    onReadChapterClick: (SChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialManga.url) {
        if (state.manga?.url != initialManga.url) {
            viewModel.loadDetails(initialManga)
        }
    }

    DetailContent(
        state = state,
        initialManga = initialManga,
        onBackClick = onBackClick,
        onReadChapterClick = onReadChapterClick,
        onRetry = { viewModel.retry(initialManga) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailContent(
    state: DetailUiState,
    initialManga: SManga,
    onBackClick: () -> Unit,
    onReadChapterClick: (SChapter) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manga = state.manga ?: initialManga

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = manga.title,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 600.dp

            if (state.isLoading && state.chapters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading manga details...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (state.errorMessage != null && state.chapters.isEmpty()) {
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
            } else {
                if (isWideScreen) {
                    // Two-column layout for tablets / landscape
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left Pane: Cover & Read Button
                        Column(
                            modifier = Modifier
                                .width(280.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ElevatedCard(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.7f)
                            ) {
                                NHentaiImage(
                                    url = manga.thumbnail_url,
                                    contentDescription = manga.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (state.chapters.isNotEmpty()) {
                                Button(
                                    onClick = { onReadChapterClick(state.chapters.first()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Read"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Read Chapter",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Right Pane: Details & Chapter List
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            MangaInfoSection(manga = manga)
                            ChapterListSection(
                                chapters = state.chapters,
                                onReadChapterClick = onReadChapterClick
                            )
                        }
                    }
                } else {
                    // Single-column layout for phones
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .width(130.dp)
                                    .aspectRatio(0.7f)
                            ) {
                                NHentaiImage(
                                    url = manga.thumbnail_url,
                                    contentDescription = manga.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = manga.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (manga.status == SManga.COMPLETED) "COMPLETED" else "ONGOING",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                if (state.chapters.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { onReadChapterClick(state.chapters.first()) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Read"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Read Chapter",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        MangaInfoSection(manga = manga)
                        ChapterListSection(
                            chapters = state.chapters,
                            onReadChapterClick = onReadChapterClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MangaInfoSection(manga: SManga) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Tag list
        if (!manga.genre.isNullOrBlank()) {
            Text(
                text = "Tags & Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                manga.genre?.split(",")?.forEach { tag ->
                    val cleanTag = tag.trim()
                    if (cleanTag.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(cleanTag) }
                        )
                    }
                }
            }
        }

        // Description / Details Card
        if (!manga.description.isNullOrBlank()) {
            Text(
                text = "Gallery Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = manga.description ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChapterListSection(
    chapters: List<SChapter>,
    onReadChapterClick: (SChapter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Chapters (${chapters.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (chapters.isEmpty()) {
            Text(
                text = "No chapters available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            chapters.forEach { chapter ->
                ElevatedCard(
                    onClick = { onReadChapterClick(chapter) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (chapter.date_upload > 0L) {
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                                        .format(Date(chapter.date_upload))
                                    Text(
                                        text = "Uploaded: $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { onReadChapterClick(chapter) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Read")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    val sampleManga = SManga.create().apply {
        title = "Pretty Sample NHentai Gallery Title"
        status = SManga.COMPLETED
        genre = "doujinshi, japanese, romance"
        description = "ID: 123456\nPages: 24\nUploaded: 2026-03-01\nScanlator: English Scanlations"
        thumbnail_url = ""
    }
    val sampleChapter = SChapter.create().apply {
        name = "Read Gallery"
        date_upload = System.currentTimeMillis()
    }
    val mockState = DetailUiState(
        isLoading = false,
        manga = sampleManga,
        chapters = listOf(sampleChapter)
    )
    MihonTheme {
        DetailContent(
            state = mockState,
            initialManga = sampleManga,
            onBackClick = {},
            onReadChapterClick = {},
            onRetry = {}
        )
    }
}
