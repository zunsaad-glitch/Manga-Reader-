package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Manga
import com.example.model.MangaSource
import com.example.model.MangaVariety
import com.example.model.ReadingProgress
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mangas: List<Manga>,
    selectedSource: MangaSource,
    selectedVariety: MangaVariety = MangaVariety.ALL_POPULAR,
    selectedTag: String?,
    availableTags: List<String>,
    continueReadingList: List<ReadingProgress>,
    bookmarkedIds: Set<String>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onSourceSelect: (MangaSource) -> Unit,
    onVarietySelect: (MangaVariety) -> Unit = {},
    onTagSelect: (String) -> Unit,
    onMangaClick: (Manga) -> Unit,
    onBookmarkToggle: (String) -> Unit,
    onContinueReadingClick: (ReadingProgress) -> Unit,
    onLoadMore: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Smoothly reset scroll position to top whenever provider, variety or tag changes
    LaunchedEffect(selectedSource, selectedVariety, selectedTag) {
        gridState.scrollToItem(0)
    }

    // Predictive infinite scroll: trigger prefetching when user scrolls near the bottom of loaded mangas
    val shouldLoadMore by remember(mangas.size) {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            mangas.isNotEmpty() && totalItems > 8 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore, isLoadingMore, hasMore) {
        if (shouldLoadMore && !isLoadingMore && hasMore) {
            onLoadMore()
        }
    }

    // Scroll to top button visibility
    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 4 }
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("manga_home_grid")
        ) {
            // App Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader(
                    onSearchClick = onSearchClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Featured Hero Slider
            val heroManga = mangas.firstOrNull { it.isEditorPick || it.isTrending } ?: mangas.firstOrNull()
            if (heroManga != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        HeroSlider(
                            manga = heroManga,
                            isBookmarked = bookmarkedIds.contains(heroManga.id),
                            onReadClick = { onMangaClick(heroManga) },
                            onBookmarkClick = { onBookmarkToggle(heroManga.id) },
                            onTagClick = onTagSelect
                        )
                    }
                }
            }

            // Continue Reading Shelf
            if (continueReadingList.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContinueReadingShelf(
                        readingList = continueReadingList,
                        onItemClick = onContinueReadingClick,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }

            // Source Switcher Tabs
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Browse by Provider",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    SourceTabBar(
                        selectedSource = selectedSource,
                        onSourceSelect = onSourceSelect
                    )
                }
            }

            // Enriched Variety & Category Selector
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "Explore Varieties & Rankings",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    VarietyTabBar(
                        selectedVariety = selectedVariety,
                        onVarietySelect = onVarietySelect
                    )
                }
            }

            // Interactive Tag Chips Filter
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTag.isNullOrBlank() || selectedTag == "All") "Popular Genres & Tags" else "Filtered by: $selectedTag",
                            color = if (selectedTag != null && selectedTag != "All") NeonAccent else TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (selectedTag != null && selectedTag != "All") {
                            TextButton(
                                onClick = { onTagSelect("All") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Clear Filter", color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    }

                    TagFilterRow(
                        tags = availableTags,
                        selectedTag = selectedTag,
                        onTagSelect = onTagSelect
                    )
                }
            }

            // Section Title for Grid Feed
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            !selectedTag.isNullOrBlank() && selectedTag != "All" -> "$selectedTag Titles"
                            selectedSource == MangaSource.ALL -> "Discover All Titles"
                            else -> "${selectedSource.displayName} Titles"
                        },
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${mangas.size} loaded",
                        color = TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            // Loading Initial State
            if (isLoading && mangas.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonAccent, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // Empty State
            if (!isLoading && mangas.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No mangas found for \"$selectedTag\"",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try picking a different genre or clearing your filter.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onTagSelect("All") },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset All Filters")
                        }
                    }
                }
            }

            // Manga Cards Grid Items (unique key by ID and index to safely avoid duplicate crashes)
            itemsIndexed(mangas, key = { index, manga -> "${manga.id}_$index" }) { _, manga ->
                Box(modifier = Modifier.padding(8.dp)) {
                    MangaCard(
                        manga = manga,
                        isBookmarked = bookmarkedIds.contains(manga.id),
                        onCardClick = { onMangaClick(manga) },
                        onBookmarkClick = { onBookmarkToggle(manga.id) },
                        onTagClick = onTagSelect
                    )
                }
            }

            // Seamless Bottom Infinite Scroll Section
            if (mangas.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Trigger immediate fetch when this footer enters view
                    LaunchedEffect(mangas.size, hasMore) {
                        if (!isLoadingMore && hasMore) {
                            onLoadMore()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingMore) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = NeonAccent,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Loading more ${selectedSource.displayName}...",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (hasMore) {
                            // Button fallback in case auto-scroll needs a manual kick
                            Button(
                                onClick = onLoadMore,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("load_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NeonAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Load More Titles",
                                    color = NeonAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = "You have browsed all available titles in ${if (!selectedTag.isNullOrBlank() && selectedTag != "All") selectedTag else selectedSource.displayName}!",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Floating Scroll-to-Top Button
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        gridState.animateScrollToItem(0)
                    }
                },
                containerColor = NeonAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun HomeHeader(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "MangaVerse",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Infinite Reader & Multi-Source Explorer",
                color = TextTertiary,
                fontSize = 12.sp
            )
        }

        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(44.dp)
                .background(DarkSurfaceElevated, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = NeonAccent
            )
        }
    }
}
