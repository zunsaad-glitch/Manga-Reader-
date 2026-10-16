package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Chapter
import com.example.model.Manga
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

enum class MainNavigationTab {
    EXPLORE,
    SEARCH,
    LIBRARY
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure high-performance Coil cache for instant image rendering
        try {
            val imageLoader = coil.ImageLoader.Builder(this)
                .memoryCache {
                    coil.memory.MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    coil.disk.DiskCache.Builder()
                        .directory(cacheDir.resolve("manga_image_cache"))
                        .maxSizeBytes(128L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
            coil.Coil.setImageLoader(imageLoader)
        } catch (_: Exception) {}

        setContent {
            MangaVerseTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    var currentTab by remember { mutableStateOf(MainNavigationTab.EXPLORE) }

    val mangas by viewModel.mangas.collectAsStateWithLifecycle()
    val selectedSource by viewModel.selectedSource.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedContentRatings by viewModel.selectedContentRatings.collectAsStateWithLifecycle()
    val contentRatingPreset by viewModel.contentRatingPreset.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val continueReadingList by viewModel.continueReadingList.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedMangaIds.collectAsStateWithLifecycle()

    val selectedManga by viewModel.selectedManga.collectAsStateWithLifecycle()
    val selectedMangaChapters by viewModel.selectedMangaChapters.collectAsStateWithLifecycle()
    val isChaptersLoading by viewModel.isChaptersLoading.collectAsStateWithLifecycle()
    val activeChapter by viewModel.activeChapter.collectAsStateWithLifecycle()
    val activeReaderManga by viewModel.activeReaderManga.collectAsStateWithLifecycle()
    val isLoadingPages by viewModel.isLoadingPages.collectAsStateWithLifecycle()

    // Android Back button handling
    BackHandler(enabled = activeChapter != null || selectedManga != null || currentTab != MainNavigationTab.EXPLORE) {
        when {
            activeChapter != null -> viewModel.closeReader()
            selectedManga != null -> viewModel.closeMangaDetails()
            currentTab != MainNavigationTab.EXPLORE -> currentTab = MainNavigationTab.EXPLORE
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            // Only show bottom navigation when neither Details nor Reader is actively open
            if (activeChapter == null && selectedManga == null) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.EXPLORE,
                        onClick = { currentTab = MainNavigationTab.EXPLORE },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Explore"
                            )
                        },
                        label = { Text("Explore") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonAccent,
                            indicatorColor = NeonAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_explore")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.SEARCH,
                        onClick = { currentTab = MainNavigationTab.SEARCH },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonAccent,
                            indicatorColor = NeonAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.LIBRARY,
                        onClick = { currentTab = MainNavigationTab.LIBRARY },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NeonAccent,
                            indicatorColor = NeonAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_library")
                    )
                }
            }
        }
    ) { innerPadding ->
        // When Reader is active
        if (activeChapter != null && activeReaderManga != null) {
            ReaderScreen(
                manga = activeReaderManga!!,
                chapter = activeChapter!!,
                isLoadingPages = isLoadingPages,
                onRetryPages = { viewModel.retryActiveChapterPages() },
                onBackClick = { viewModel.closeReader() }
            )
        } else if (selectedManga != null) {
            // When Details Screen is active
            DetailsScreen(
                manga = selectedManga!!,
                chapters = selectedMangaChapters,
                isChaptersLoading = isChaptersLoading,
                isBookmarked = bookmarkedIds.contains(selectedManga!!.id),
                onBackClick = { viewModel.closeMangaDetails() },
                onBookmarkToggle = { viewModel.toggleBookmark(selectedManga!!.id) },
                onChapterClick = { chapter ->
                    viewModel.openReader(chapter, selectedManga!!)
                },
                onTagClick = { tag ->
                    viewModel.closeMangaDetails()
                    viewModel.selectTag(tag)
                    currentTab = MainNavigationTab.EXPLORE
                }
            )
        } else {
            // Main Tabs
            when (currentTab) {
                MainNavigationTab.EXPLORE -> {
                    HomeScreen(
                        mangas = mangas,
                        selectedSource = selectedSource,
                        selectedTag = selectedTag,
                        availableTags = viewModel.availableTags,
                        continueReadingList = continueReadingList,
                        bookmarkedIds = bookmarkedIds,
                        isLoading = isLoading,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        onSourceSelect = { viewModel.selectSource(it) },
                        onTagSelect = { viewModel.selectTag(it) },
                        onMangaClick = { viewModel.openMangaDetails(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onContinueReadingClick = { progress ->
                            val targetManga = mangas.find { it.id == progress.mangaId }
                            if (targetManga != null) {
                                viewModel.openMangaDetails(targetManga)
                            }
                        },
                        onLoadMore = { viewModel.loadMore() },
                        onSearchClick = { currentTab = MainNavigationTab.SEARCH }
                    )
                }

                MainNavigationTab.SEARCH -> {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        selectedSource = selectedSource,
                        onSourceSelect = { viewModel.selectSource(it) },
                        selectedTag = selectedTag,
                        availableTags = viewModel.availableTags,
                        onTagSelect = { viewModel.selectTag(it) },
                        selectedContentRatings = selectedContentRatings,
                        contentRatingPreset = contentRatingPreset,
                        onRatingPresetSelect = { viewModel.setContentRatingPreset(it) },
                        onToggleRating = { viewModel.toggleContentRating(it) },
                        searchResults = mangas,
                        bookmarkedIds = bookmarkedIds,
                        isLoading = isLoading,
                        isLoadingMore = isLoadingMore,
                        hasMore = hasMore,
                        onLoadMore = { viewModel.loadMore() },
                        onMangaClick = { viewModel.openMangaDetails(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onClearFilters = { viewModel.clearAllFilters() },
                        onBackClick = { currentTab = MainNavigationTab.EXPLORE }
                    )
                }

                MainNavigationTab.LIBRARY -> {
                    val bookmarkedMangas = mangas.filter { bookmarkedIds.contains(it.id) }
                    LibraryScreen(
                        bookmarkedMangas = bookmarkedMangas,
                        readingHistory = continueReadingList,
                        onMangaClick = { viewModel.openMangaDetails(it) },
                        onBookmarkToggle = { viewModel.toggleBookmark(it) },
                        onProgressClick = { progress ->
                            val targetManga = mangas.find { it.id == progress.mangaId }
                            if (targetManga != null) {
                                viewModel.openMangaDetails(targetManga)
                            }
                        },
                        onTagClick = { tag ->
                            viewModel.selectTag(tag)
                            currentTab = MainNavigationTab.EXPLORE
                        }
                    )
                }
            }
        }
    }
}
