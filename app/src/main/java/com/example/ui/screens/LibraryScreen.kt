package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Manga
import com.example.model.ReadingProgress
import com.example.ui.components.ContinueReadingCard
import com.example.ui.components.MangaCard
import com.example.ui.theme.*

@Composable
fun LibraryScreen(
    bookmarkedMangas: List<Manga>,
    readingHistory: List<ReadingProgress>,
    onMangaClick: (Manga) -> Unit,
    onBookmarkToggle: (String) -> Unit,
    onProgressClick: (ReadingProgress) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Bookmarks (${bookmarkedMangas.size})", "History (${readingHistory.size})")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Library Header
        Text(
            text = "My Library",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = NeonAccent,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Bookmarks Tab
            if (bookmarkedMangas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No saved bookmarks yet.\nTap the bookmark icon on any manga to save it here!",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bookmarkedMangas, key = { it.id }) { manga ->
                        Box(modifier = Modifier.padding(6.dp)) {
                            MangaCard(
                                manga = manga,
                                isBookmarked = true,
                                onCardClick = { onMangaClick(manga) },
                                onBookmarkClick = { onBookmarkToggle(manga.id) },
                                onTagClick = onTagClick
                            )
                        }
                    }
                }
            }
        } else {
            // History Tab
            if (readingHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reading history recorded.\nStart reading any chapter to track your progress!",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(readingHistory, key = { it.mangaId }) { item ->
                        ContinueReadingCard(
                            item = item,
                            onClick = { onProgressClick(item) }
                        )
                    }
                }
            }
        }
    }
}
