package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContentRating
import com.example.model.ContentRatingPreset
import com.example.model.Manga
import com.example.model.MangaSource
import com.example.ui.components.MangaCard
import com.example.ui.components.SourceTabBar
import com.example.ui.theme.*

@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedSource: MangaSource,
    onSourceSelect: (MangaSource) -> Unit,
    selectedTag: String?,
    availableTags: List<String>,
    onTagSelect: (String) -> Unit,
    selectedContentRatings: Set<String>,
    contentRatingPreset: ContentRatingPreset,
    onRatingPresetSelect: (ContentRatingPreset) -> Unit,
    onToggleRating: (String) -> Unit,
    searchResults: List<Manga>,
    bookmarkedIds: Set<String>,
    isLoading: Boolean,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = true,
    onLoadMore: () -> Unit = {},
    onMangaClick: (Manga) -> Unit,
    onBookmarkToggle: (String) -> Unit,
    onClearFilters: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    // Smoothly scroll to top when source or tag changes
    LaunchedEffect(selectedSource, selectedTag) {
        gridState.scrollToItem(0)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore, isLoadingMore, hasMore) {
        if (shouldLoadMore && !isLoadingMore && hasMore) {
            onLoadMore()
        }
    }

    val adultThemes = listOf(
        "Incest", "Harem", "Doujinshi", "Gore", "Sexual Violence", "Crossdressing", "Gyaru", "Monster Girls", "Psychological"
    )

    val standardGenres = availableTags.filterNot { adultThemes.contains(it) || it == "All" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Top Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text("Search title, author, or keyword...", color = TextTertiary, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonAccent)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NeonAccent,
                    unfocusedBorderColor = DarkSurfaceElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Filter Toggle Button
            IconButton(
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier
                    .background(
                        if (showAdvancedFilters) NeonAccent else DarkSurface,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filters",
                    tint = if (showAdvancedFilters) Color.White else NeonAccent
                )
            }
        }

        // Search Results & Filter Controls
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_results_grid")
        ) {
            // Source Switcher
            item(span = { GridItemSpan(maxLineSpan) }) {
                SourceTabBar(
                    selectedSource = selectedSource,
                    onSourceSelect = onSourceSelect
                )
            }

            // Explicit Content Filter Section (Primary Request)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeonAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Content Rating & Explicit Filter",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Filter out or include 18+",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset filter chips (All vs Safe vs Explicit)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ContentRatingPreset.values().forEach { preset ->
                                val isSelected = contentRatingPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) NeonAccent else DarkSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onRatingPresetSelect(preset) }
                                ) {
                                    Text(
                                        text = preset.label,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    )
                                }
                            }
                        }

                        // Detailed check chips for individual ratings
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ContentRating.values().forEach { ratingItem ->
                                val isChecked = selectedContentRatings.contains(ratingItem.apiValue)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isChecked) DarkSurfaceElevated else DarkBackground,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleRating(ratingItem.apiValue) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isChecked) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (ratingItem == ContentRating.PORNOGRAPHIC || ratingItem == ContentRating.EROTICA) NeonAccent else NeonCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                        }
                                        Text(
                                            text = ratingItem.label,
                                            color = if (isChecked) TextPrimary else TextTertiary,
                                            fontSize = 10.sp,
                                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Explicit & Adult Tags Section (Incest, Harem, Doujinshi, Gore, etc.)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Adult & Mature Themes",
                                color = AmberGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedTag != null && selectedTag != "All") {
                            TextButton(
                                onClick = { onTagSelect("All") },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Tag (${selectedTag})", color = NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(adultThemes) { tag ->
                            val isSelected = tag.equals(selectedTag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonAccent else DarkSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTagSelect(tag) }
                                    .testTag("search_tag_${tag.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standard Popular Genres Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Popular Genres",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        item {
                            val isAll = selectedTag.isNullOrBlank() || selectedTag == "All"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAll) NeonCyan else DarkSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTagSelect("All") }
                            ) {
                                Text(
                                    text = "All",
                                    color = if (isAll) Color.Black else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        items(standardGenres) { tag ->
                            val isSelected = tag.equals(selectedTag, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonCyan else DarkSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onTagSelect(tag) }
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.Black else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLoading) "Searching titles..." else "${searchResults.size} titles found",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (searchQuery.isNotBlank() || (selectedTag != null && selectedTag != "All") || contentRatingPreset != ContentRatingPreset.ALL) {
                        TextButton(
                            onClick = onClearFilters,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Reset All Filters", color = NeonCyan, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Loading Indicator
            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonAccent, modifier = Modifier.size(36.dp))
                    }
                }
            } else if (searchResults.isEmpty()) {
                // Helpful Empty State with clear actions
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 40.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = NeonAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No titles found matching criteria",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try including 18+ content rating, clearing the search query, or selecting a different tag.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onClearFilters,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAccent)
                            ) {
                                Text("Reset Filters & Show All", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Results List (Safe unique key)
            itemsIndexed(searchResults, key = { index, manga -> "${manga.id}_$index" }) { _, manga ->
                Box(modifier = Modifier.padding(4.dp)) {
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
            if (searchResults.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(searchResults.size, hasMore) {
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
                                    text = "Loading more titles...",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else if (hasMore) {
                            Button(
                                onClick = onLoadMore,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NeonAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Load More Results",
                                    color = NeonAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
