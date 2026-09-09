package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.*
import com.example.data.SettingsRepository
import com.example.ui.components.AchievementsSheet
import com.example.ui.components.AdvancedTagMatrixDialog
import com.example.ui.components.PanelBookmarksSheet
import com.example.ui.components.QuestsAndRankSheet
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.LibraryFilter
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MangaCategory
import com.example.viewmodel.MangaSortOrder
import com.example.viewmodel.SearchMode
import com.example.viewmodel.SearchSource
import kotlinx.coroutines.launch

@Composable
fun ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = ThemePrimary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .padding(16.dp)
                .size(52.dp)
                .testTag("jump_to_top_button")
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Jump to Top",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
fun SurpriseRouletteDialog(
    manga: MangaData?,
    onDismiss: () -> Unit,
    onReroll: () -> Unit,
    onRead: (String) -> Unit
) {
    if (manga == null) return
    val title = manga.attributes?.title?.get("en")
        ?: manga.attributes?.title?.values?.firstOrNull()
        ?: "Mystery Title"
    val coverUrl = manga.getCoverImageUrl()
    val desc = manga.attributes?.description?.get("en")
        ?: manga.attributes?.description?.values?.firstOrNull()
        ?: "An exceptional hand-picked recommendation selected specially for you."
    val rating = manga.attributes?.contentRating ?: "safe"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎲", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Surprise Discovery", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(130.dp)
                        .height(180.dp)
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(ThemeSurfaceVariant))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (rating != "safe") {
                    Surface(
                        color = if (rating == "pornographic" || rating == "erotica") Color(0xFFE53935) else Color(0xFFFF9800),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = rating.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onRead(manga.id)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
            ) {
                Text("Read Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReroll) {
                    Text("🎲 Roll Again", color = ThemeSecondary, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = ThemeOnSurfaceVariant)
                }
            }
        },
        containerColor = ThemeSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickChapterPeekSheet(
    viewModel: MainViewModel,
    onNavigateToReader: (String) -> Unit,
    onNavigateToChapters: (String) -> Unit
) {
    val quickManga by viewModel.quickPeekManga.collectAsState()
    val chapters by viewModel.quickPeekChapters.collectAsState()
    val isLoading by viewModel.isQuickPeekLoading.collectAsState()

    if (quickManga != null) {
        val manga = quickManga!!
        val title = manga.attributes?.title?.get("en")
            ?: manga.attributes?.title?.values?.firstOrNull()
            ?: "Manga"
        val coverUrl = manga.getCoverImageUrl()
        val rating = manga.attributes?.contentRating ?: "safe"

        ModalBottomSheet(
            onDismissRequest = { viewModel.closeQuickPeek() },
            containerColor = ThemeSurface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(60.dp, 84.dp)
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(coverUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (rating != "safe") {
                                Surface(
                                    color = if (rating == "pornographic" || rating == "erotica") Color(0xFFE53935) else Color(0xFFFF9800),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = rating.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }
                            }
                            Text(
                                text = "Quick Chapters",
                                style = MaterialTheme.typography.bodySmall.copy(color = ThemeSecondary, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                    IconButton(onClick = {
                        val id = manga.id
                        viewModel.closeQuickPeek()
                        onNavigateToChapters(id)
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "View Details", tint = ThemePrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ThemeOutline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemePrimary)
                    }
                } else if (chapters.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No chapter feeds found.", color = ThemeOnSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chapters) { ch ->
                            val chNum = ch.attributes?.chapter ?: "1"
                            val chTitle = ch.attributes?.title ?: "Chapter $chNum"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.closeQuickPeek()
                                        onNavigateToReader(ch.id)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Chapter $chNum",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        if (chTitle.isNotBlank() && chTitle != "Chapter $chNum") {
                                            Text(
                                                chTitle,
                                                style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val releaseDate = ch.getFormattedReleaseDate()
                                        if (releaseDate.isNotBlank()) {
                                            Text(
                                                "📅 $releaseDate",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF80CBC4),
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.closeQuickPeek()
                                            onNavigateToReader(ch.id)
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                                    ) {
                                        Text("Read", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MangaListScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToAuthor: (String) -> Unit = {},
    onNavigateToNhDetail: (String) -> Unit = {},
    onNavigateToNhArtist: (String) -> Unit = {},
    onNavigateToJandaDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToReader: (String) -> Unit = {},
    onNavigateToMillionDollar: () -> Unit = {}
) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        containerColor = ThemeBackground,
        bottomBar = {
            NavigationBar(
                containerColor = ThemeNavBar,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.HOME,
                    onClick = { viewModel.setTab(AppTab.HOME) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimary,
                        selectedTextColor = ThemePrimary,
                        unselectedIconColor = ThemeOnSurfaceVariant,
                        unselectedTextColor = ThemeOnSurfaceVariant,
                        indicatorColor = ThemePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SEARCH,
                    onClick = { viewModel.setTab(AppTab.SEARCH) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    label = { Text("Search", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimary,
                        selectedTextColor = ThemePrimary,
                        unselectedIconColor = ThemeOnSurfaceVariant,
                        unselectedTextColor = ThemeOnSurfaceVariant,
                        indicatorColor = ThemePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_search")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.LIBRARY,
                    onClick = { viewModel.setTab(AppTab.LIBRARY) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.LIBRARY) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Library"
                        )
                    },
                    label = { Text("Library", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimary,
                        selectedTextColor = ThemePrimary,
                        unselectedIconColor = ThemeOnSurfaceVariant,
                        unselectedTextColor = ThemeOnSurfaceVariant,
                        indicatorColor = ThemePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_library")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.HISTORY,
                    onClick = { viewModel.setTab(AppTab.HISTORY) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimary,
                        selectedTextColor = ThemePrimary,
                        unselectedIconColor = ThemeOnSurfaceVariant,
                        unselectedTextColor = ThemeOnSurfaceVariant,
                        indicatorColor = ThemePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ThemePrimary,
                        selectedTextColor = ThemePrimary,
                        unselectedIconColor = ThemeOnSurfaceVariant,
                        unselectedTextColor = ThemeOnSurfaceVariant,
                        indicatorColor = ThemePrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground)
        ) {
            when (currentTab) {
                AppTab.HOME -> HomeScreenContent(viewModel, onNavigateToChapters, onNavigateToAuthor, onNavigateToMillionDollar)
                AppTab.SEARCH -> SearchScreenContent(viewModel, onNavigateToChapters, onNavigateToAuthor)
                AppTab.LIBRARY -> LibraryScreenContent(viewModel, onNavigateToChapters, onNavigateToAuthor)
                AppTab.HISTORY -> HistoryScreenContent(viewModel, onNavigateToChapters, onNavigateToReader)
                AppTab.SETTINGS -> SettingsTabContent(viewModel)
            }

            QuickChapterPeekSheet(
                viewModel = viewModel,
                onNavigateToReader = onNavigateToReader,
                onNavigateToChapters = onNavigateToChapters
            )
        }
    }
}

// ======================== DOUJINSHI & ADULT MANGA HUB ========================

@Composable
fun DoujinshiHubContainer(
    onNavigateToNhDetail: (String) -> Unit,
    onNavigateToJandaDetail: (String, String) -> Unit
) {
    var selectedSource by remember { mutableStateOf(0) } // 0 = JandaPress, 1 = nHentai

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {
        // Top Source Switcher
        Surface(
            color = ThemeSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedSource == 0) Color(0xFF6366F1) else ThemeBackground,
                    border = BorderStroke(1.dp, if (selectedSource == 0) Color(0xFF6366F1) else ThemeSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSource = 0 }
                        .testTag("hub_switch_jandapress")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Dns,
                            contentDescription = "JandaPress",
                            tint = if (selectedSource == 0) Color.White else ThemeOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "JandaPress API",
                            color = if (selectedSource == 0) Color.White else ThemeOnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedSource == 1) ThemePrimary else ThemeBackground,
                    border = BorderStroke(1.dp, if (selectedSource == 1) ThemePrimary else ThemeSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSource = 1 }
                        .testTag("hub_switch_nhentai")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "nHentai",
                            tint = if (selectedSource == 1) Color.White else ThemeOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "nHentai Direct",
                            color = if (selectedSource == 1) Color.White else ThemeOnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedSource == 0) {
                JandaPressContent(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onNavigateToDetail = onNavigateToJandaDetail
                )
            } else {
                NhSearchContent(
                    viewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onNavigateToDetail = onNavigateToNhDetail
                )
            }
        }
    }
}

// ======================== HOME SCREEN CONTENT ========================

@Composable
fun HomeScreenContent(
    viewModel: MainViewModel,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToAuthor: (String) -> Unit = {},
    onNavigateToMillionDollar: () -> Unit = {}
) {
    val mangas by viewModel.mangas.collectAsState()
    val featuredManga by viewModel.featuredManga.collectAsState()
    val topRatedMangas by viewModel.topRatedMangas.collectAsState()
    val latestMangas by viewModel.latestMangas.collectAsState()
    val allFreshMangas by viewModel.allFreshMangas.collectAsState()
    val freshMangas = remember(allFreshMangas, latestMangas) {
        if (allFreshMangas.isNotEmpty()) allFreshMangas else latestMangas.filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
    }
    val mantaList by viewModel.mantaList.collectAsState()
    val manhwaReadList by viewModel.manhwaReadList.collectAsState()
    val ecchiComicsList by viewModel.ecchiComicsList.collectAsState()
    val threeDComicsList by viewModel.threeDComicsList.collectAsState()
    val adultWebtoonsList by viewModel.adultWebtoonsList.collectAsState()
    val adultComicsList by viewModel.adultComicsList.collectAsState()
    val parodyMangasList by viewModel.parodyMangasList.collectAsState()
    val comicsCategoryList by viewModel.comicsCategoryList.collectAsState()
    val doujinshiList by viewModel.doujinshiList.collectAsState()
    val manhwaCategoryList by viewModel.manhwaCategoryList.collectAsState()
    val mangaCategoryList by viewModel.mangaCategoryList.collectAsState()
    val manhuaCategoryList by viewModel.manhuaCategoryList.collectAsState()
    val manhwaToonList by viewModel.manhwaToonList.collectAsState()
    val manhwaToonSort by viewModel.manhwaToonSort.collectAsState()
    val manhwaToonGenre by viewModel.manhwaToonGenre.collectAsState()
    val isManhwaToonLoading by viewModel.isManhwaToonLoading.collectAsState()
    val mangaToonList by viewModel.mangaToonList.collectAsState()
    val mangaToonGenre by viewModel.mangaToonGenre.collectAsState()
    val isMangaToonLoading by viewModel.isMangaToonLoading.collectAsState()
    val cultivationGoatMangas by viewModel.cultivationGoatMangas.collectAsState()
    val goatMangas by viewModel.goatMangas.collectAsState()
    val fullColorMangas by viewModel.fullColorMangas.collectAsState()
    val matureNtrLibrary by viewModel.matureNtrLibrary.collectAsState()
    val overflowMangas by viewModel.overflowMangas.collectAsState()
    val continueReadingList by viewModel.continueReadingList.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedSortOrder by viewModel.selectedSortOrder.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMoreHome.collectAsState()
    val hasMore by viewModel.hasMoreHome.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
    var surpriseManga by remember { mutableStateOf<MangaData?>(null) }
    var showAchievementsSheet by remember { mutableStateOf(false) }
    var showQuestsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showTagMatrixDialog by remember { mutableStateOf(false) }

    var showWarningDialog by remember { mutableStateOf(false) }
    var rememberWarningChoice by remember { mutableStateOf(false) }
    var selectedMangaForWarning by remember { mutableStateOf<MangaData?>(null) }

    fun handleCardClick(manga: MangaData) {
        if (viewModel.shouldShowAgeWarning(manga)) {
            selectedMangaForWarning = manga
            showWarningDialog = true
        } else {
            onNavigateToChapters(manga.id)
        }
    }

    if (showWarningDialog && selectedMangaForWarning != null) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ThemePrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("18+ Content Notice", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        "This title contains mature/adult content (18+). Please confirm you wish to view this series.",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { rememberWarningChoice = !rememberWarningChoice }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberWarningChoice,
                            onCheckedChange = { rememberWarningChoice = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ThemePrimary,
                                uncheckedColor = ThemeOnSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Remember my choice (don't ask again)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeOnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = selectedMangaForWarning
                        viewModel.dismissAgeWarning(rememberWarningChoice)
                        showWarningDialog = false
                        if (m != null) {
                            onNavigateToChapters(m.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                ) {
                    Text("Proceed (18+)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("Cancel", color = ThemeOnSurfaceVariant)
                }
            },
            containerColor = ThemeSurface
        )
    }

    if (surpriseManga != null) {
        SurpriseRouletteDialog(
            manga = surpriseManga,
            onDismiss = { surpriseManga = null },
            onReroll = { surpriseManga = viewModel.getRandomSurprisePick() },
            onRead = { id ->
                val target = (mangas + latestMangas + matureNtrLibrary + adultWebtoonsList).find { it.id == id }
                if (target != null) handleCardClick(target) else onNavigateToChapters(id)
            }
        )
    }

    val popularTags = listOf(
        "All", "🔥 18+ Fresh", "🧊 3D Comics", "Ecchi", "Smut", "GOAT", "Adult 18+", "NTR", "Netorare", "Incest", "Armpit", "Fetish",
        "Doujinshi", "Boys' Love", "Girls' Love", "Reincarnation", "Isekai",
        "Harem", "Romance", "Cultivation", "Martial Arts", "Action", "Adventure", "Fantasy",
        "Drama", "Supernatural", "Psychological", "Slice of Life", "Comedy", "Full Color"
    )

    val categoryCounts = remember(
        mangas, fullColorMangas, adultWebtoonsList, matureNtrLibrary,
        ecchiComicsList, threeDComicsList, goatMangas, adultComicsList,
        parodyMangasList, manhwaCategoryList, mangaCategoryList, manhuaCategoryList,
        comicsCategoryList, doujinshiList, manhwaToonList, mangaToonList, mantaList, manhwaReadList, selectedCategory
    ) {
        mapOf(
            MangaCategory.ALL to mangas.size,
            MangaCategory.MANTA to (if (mantaList.isNotEmpty()) mantaList.size else 16),
            MangaCategory.MANHWAREAD to (if (manhwaReadList.isNotEmpty()) manhwaReadList.size else 15),
            MangaCategory.MANGATOON to (if (mangaToonList.isNotEmpty()) mangaToonList.size else 12),
            MangaCategory.MANHWATOON to (if (manhwaToonList.isNotEmpty()) manhwaToonList.size else 14),
            MangaCategory.FULL_COLOR to fullColorMangas.size,
            MangaCategory.WEBTOONS to maxOf(comicsCategoryList.size, 25),
            MangaCategory.ADULT_WEBTOONS to adultWebtoonsList.size,
            MangaCategory.NTR_18 to matureNtrLibrary.size,
            MangaCategory.ECCHI to ecchiComicsList.size,
            MangaCategory.COMIC_3D to threeDComicsList.size,
            MangaCategory.GOAT to goatMangas.size,
            MangaCategory.ADULT_COMICS to adultComicsList.size,
            MangaCategory.PARODY_18 to parodyMangasList.size,
            MangaCategory.MANHWA to (if (selectedCategory == MangaCategory.MANHWA && mangas.isNotEmpty()) mangas.size else manhwaCategoryList.size),
            MangaCategory.MANGA to (if (selectedCategory == MangaCategory.MANGA && mangas.isNotEmpty()) mangas.size else mangaCategoryList.size),
            MangaCategory.MANHUA to (if (selectedCategory == MangaCategory.MANHUA && mangas.isNotEmpty()) mangas.size else manhuaCategoryList.size),
            MangaCategory.COMICS to comicsCategoryList.size,
            MangaCategory.DOUJINSHI to doujinshiList.size
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("home_scroll_list"),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // App Header & Feature Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ThemePrimary, Color(0xFF9C27B0))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Manga Universe",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "Unlimited Manga, Webtoons & 18+ Vault",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ThemeOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Utility and Feature Pills Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showQuestsSheet = true }
                                .testTag("home_quests_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⚡", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Quests",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFD54F).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showBookmarksSheet = true }
                                .testTag("home_moments_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("📑", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Moments",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFB300).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showAchievementsSheet = true }
                                .testTag("home_achievements_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("🏆", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Badges",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF673AB7).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { surpriseManga = viewModel.getRandomSurprisePick() }
                                .testTag("home_surprise_me_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("🎲", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Surprise",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCE93D8)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFD700).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onNavigateToMillionDollar() }
                                .testTag("home_vip_million_dollar_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("👑", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "VIP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
            }

            // Streamlined Category & Quick Navigation Bar with Content Numbers
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Matrix Trigger
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showTagMatrixDialog = true }
                            .testTag("home_filter_matrix_btn"),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF673AB7).copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.5f)),
                        contentColor = Color(0xFFCE93D8)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("🎛️", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE1BEE7))
                            )
                        }
                    }

                    // Main Category Pills with Live Content Numbers
                    MangaCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        val count = categoryCounts[category] ?: 0
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectCategory(category) }
                                .testTag("category_pill_${category.name.lowercase()}"),
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) ThemePrimary else ThemeSurfaceVariant.copy(alpha = 0.6f),
                            border = if (isSelected) null else BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f)),
                            contentColor = if (isSelected) Color.White else ThemeOnSurfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else ThemePrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) Color.White else ThemePrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // MangaToon Dedicated Live Scraper Controls & Status
            if (selectedCategory == MangaCategory.MANGATOON) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF241512)),
                        border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFF5722).copy(alpha = 0.25f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🎨", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            "MangaToon.mobi Scraper",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBE9E7)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Live direct HTML scraper from mangatoon.mobi",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFFFAB91)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isMangaToonLoading) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (isMangaToonLoading) Color(0xFFFF9800).copy(alpha = 0.6f) else Color(0xFF4CAF50).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        if (isMangaToonLoading) "SCRAPING..." else "LIVE CONNECTED",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = if (isMangaToonLoading) Color(0xFFFFB74D) else Color(0xFF81C784)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Genre Filter Chips
                            Text(
                                "GENRES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A65)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val genres = listOf(
                                    null to "All Manga",
                                    "romance" to "💖 Romance",
                                    "fantasy" to "✨ Fantasy",
                                    "president" to "👔 CEO / President",
                                    "urban" to "🏙️ Urban",
                                    "bl" to "🌸 Boys Love",
                                    "action" to "⚔️ Action",
                                    "historical" to "👑 Historical",
                                    "comedy" to "😂 Comedy",
                                    "horror" to "👻 Horror",
                                    "school" to "🏫 Campus",
                                    "rebirth" to "🔄 Rebirth"
                                )
                                genres.forEach { (genreKey, label) ->
                                    val isGenreSelected = mangaToonGenre == genreKey
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                viewModel.fetchMangaToon(page = 1, genre = genreKey)
                                            },
                                        shape = RoundedCornerShape(50),
                                        color = if (isGenreSelected) Color(0xFFFF5722) else Color(0xFF3E1F18),
                                        border = BorderStroke(1.dp, if (isGenreSelected) Color(0xFFFFCCBC) else Color(0xFFBF360C).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (isGenreSelected) Color.White else Color(0xFFFFCCBC),
                                                fontWeight = if (isGenreSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ManhwaToon Dedicated Live Scraper Controls & Status
            if (selectedCategory == MangaCategory.MANHWATOON) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B112C)),
                        border = BorderStroke(1.dp, Color(0xFFAB47BC).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF9C27B0).copy(alpha = 0.25f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("⚡", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            "ManhwaToon.me Scraper",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF3E5F5)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Live direct HTML scraper from manhwatoon.me",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFCE93D8)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isManhwaToonLoading) Color(0xFFFF9800).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (isManhwaToonLoading) Color(0xFFFF9800).copy(alpha = 0.6f) else Color(0xFF4CAF50).copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        if (isManhwaToonLoading) "SCRAPING..." else "LIVE CONNECTED",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = if (isManhwaToonLoading) Color(0xFFFFB74D) else Color(0xFF81C784)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sort Order Tabs
                            Text(
                                "SORT BY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA68C8)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sorts = listOf(
                                    "latest" to "🔥 Latest",
                                    "trending" to "📈 Trending",
                                    "rating" to "⭐ Top Rated",
                                    "views" to "👀 Most Views",
                                    "new-manga" to "✨ New Releases"
                                )
                                sorts.forEach { (key, label) ->
                                    val isSortSelected = manhwaToonSort == key
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                viewModel.fetchManhwaToon(page = 1, sort = key, genre = manhwaToonGenre)
                                            },
                                        shape = RoundedCornerShape(50),
                                        color = if (isSortSelected) Color(0xFF9C27B0) else Color(0xFF2C1945),
                                        border = BorderStroke(1.dp, if (isSortSelected) Color(0xFFE1BEE7) else Color(0xFF512DA8).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (isSortSelected) Color.White else Color(0xFFD1C4E9),
                                                fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Genre Filter Chips
                            Text(
                                "GENRES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBA68C8)
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val genres = listOf(
                                    null to "All Genres",
                                    "romance" to "💕 Romance",
                                    "action" to "⚔️ Action",
                                    "adult" to "🔞 Adult 18+",
                                    "drama" to "🎭 Drama",
                                    "fantasy" to "🧙 Fantasy",
                                    "comedy" to "😂 Comedy",
                                    "doujinshi" to "🌸 Doujinshi",
                                    "school-life" to "🏫 School Life"
                                )
                                genres.forEach { (key, label) ->
                                    val isGenreSelected = manhwaToonGenre == key
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                viewModel.fetchManhwaToon(page = 1, sort = manhwaToonSort, genre = key)
                                            },
                                        shape = RoundedCornerShape(50),
                                        color = if (isGenreSelected) Color(0xFF673AB7) else Color(0xFF2C1945),
                                        border = BorderStroke(1.dp, if (isGenreSelected) Color(0xFFD1C4E9) else Color(0xFF512DA8).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (isGenreSelected) Color.White else Color(0xFFD1C4E9),
                                                fontWeight = if (isGenreSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discovery Carousels are exclusively shown on the ALL Home page
            if (selectedCategory == MangaCategory.ALL) {
                // Slide of Fresh Releases on top (excluding completed mangas)
                if (freshMangas.isNotEmpty()) {
                    item {
                        FreshReleasesSlider(
                            mangas = freshMangas,
                            onClick = { handleCardClick(it) }
                        )
                    }
                } else if (featuredManga != null) {
                    item {
                        featuredManga?.let { manga ->
                            EditorPickHeroCard(
                                manga = manga,
                                onClick = { handleCardClick(manga) }
                            )
                        }
                    }
                }

        // Greatest of All Time (GOAT) Masterpieces Carousel
        if (goatMangas.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🐐 Greatest of All Time (GOAT)",
                    subtitle = "Berserk, Vagabond, Monster, Lookism, Lord of Mysteries & Legends"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goatMangas) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // 🌈 Full Color & Masterpiece Editions Carousel
        if (fullColorMangas.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🌈 Full Color Manga & Webtoons",
                    subtitle = "Solo Leveling, Secret Class, Bleach Colored, Sinful Lust & Vivid Masterpieces"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fullColorMangas) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Premium 18+ Library (NTR, Incest, Smut & Mature Hits)
        if (matureNtrLibrary.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🖤 Premium 18+ Vault (NTR, Incest & Mature Hits)",
                    subtitle = "Everyday Conversations With My Big Sister, Yanmama, Secret Class & Taboo Masterpieces"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matureNtrLibrary) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Adult 18+ Webtoons Carousel
        if (adultWebtoonsList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🔞 Adult 18+ Webtoons & Manhwa",
                    subtitle = "Secret Class, Boarding Diary, Stepmother Friends, Sinful Lust & Adult Hits"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(adultWebtoonsList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Ecchi & Smut Classics Carousel
        if (ecchiComicsList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🔥 Hot Ecchi, Smut & Harem",
                    subtitle = "High School DxD, To LOVE-Ru, Prison School, Mato Seihei & Top Hits"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ecchiComicsList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // 3D Comics & CG Graphic Novels Carousel
        if (threeDComicsList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🧊 3D Comics & CG Graphic Novels",
                    subtitle = "3D Rendered Webcomics, Stylized CGI, Blender & DAZ Visual Stories"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(threeDComicsList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // 18+ Adult Comics & Graphic Novels Carousel
        if (adultComicsList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "💋 18+ Adult Comics & Graphic Novels",
                    subtitle = "Sunstone, Blood Stain, Alfie, Oglaf, Mirka Andolfo & Erotica Comics"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(adultComicsList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // 18+ Parody & Doujinshi Carousel
        if (parodyMangasList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🎭 18+ Anime & Gaming Parodies",
                    subtitle = "Naruto, Fate/Grand Order, Genshin Impact, One Piece, Bleach & Hololive Parodies"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(parodyMangasList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Comics & Graphic Novels Carousel
        if (comicsCategoryList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "💬 Western & Webcomics Vault",
                    subtitle = "Solo Leveling, Beginning After The End, Tower of God & Full Color"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comicsCategoryList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Doujinshi Vault Carousel
        if (doujinshiList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🌸 Doujinshi & Fan Art Comics",
                    subtitle = "Exclusive uncensored doujinshi collections"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(doujinshiList) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Cultivation GOATs / Special Section
        if (cultivationGoatMangas.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🔥 Cultivation GOATs & Manhwa",
                    subtitle = "Reverend Insanity, Martial Peak & Cultivation Legends"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cultivationGoatMangas) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Top Rated Carousel
        if (topRatedMangas.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "⭐ Top Rated Masterpieces",
                    subtitle = "Highest reader scores globally"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topRatedMangas) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }

        // Overflow & Cult Classics
        if (overflowMangas.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🌶️ Special Selection & Classics",
                    subtitle = "Overflow, Kaiduka & Fan Favorites"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(overflowMangas) { manga ->
                        ContinueReadingCard(manga = manga, onClick = { handleCardClick(manga) })
                    }
                }
            }
        }
        }

        // All Manga Feed
        val displayMangas = when {
            selectedCategory == MangaCategory.MANGATOON && mangaToonList.isNotEmpty() -> mangaToonList
            selectedCategory == MangaCategory.MANHWATOON && manhwaToonList.isNotEmpty() -> manhwaToonList
            else -> mangas
        }
        val isFeedLoading = when (selectedCategory) {
            MangaCategory.MANGATOON -> isMangaToonLoading
            MangaCategory.MANHWATOON -> isManhwaToonLoading
            else -> isLoading
        }

        item {
            SectionHeader(
                title = when (selectedCategory) {
                    MangaCategory.MANGATOON -> "🎨 MangaToon Directory"
                    MangaCategory.MANHWATOON -> "⚡ ManhwaToon Directory"
                    MangaCategory.COMIC_3D -> "🧊 3D Comics & CG Vault"
                    else -> "📚 Explore ${selectedCategory.displayName} Directory"
                },
                subtitle = when (selectedCategory) {
                    MangaCategory.MANGATOON -> "${displayMangas.size} titles scraped live"
                    MangaCategory.MANHWATOON -> "${displayMangas.size} titles scraped live"
                    else -> "${displayMangas.size} titles loaded"
                }
            )
        }

        if (isFeedLoading && displayMangas.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ThemePrimary)
                }
            }
        } else {
            items(displayMangas) { manga ->
                val isBookmarked = bookmarkedIds.contains(manga.id)
                val isFav = favoriteIds.contains(manga.id)
                MangaFeedCard(
                    manga = manga,
                    isBookmarked = isBookmarked,
                    isFavorite = isFav,
                    onToggleBookmark = { viewModel.toggleBookmark(manga.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(manga.id) },
                    onClick = { handleCardClick(manga) },
                    onAuthorClick = {
                        val authorId = manga.getAuthorId()
                        if (authorId != null) onNavigateToAuthor(authorId)
                    },
                    onQuickPeek = { viewModel.openQuickPeek(manga) }
                )
            }

            if (hasMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = ThemePrimary, modifier = Modifier.size(32.dp))
                        } else {
                            Button(
                                onClick = { viewModel.loadMoreMangas() },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Load More Titles", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    ScrollToTopFab(
        visible = showScrollToTop,
        onClick = {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp)
    )

    if (showAchievementsSheet) {
        AchievementsSheet(
            viewModel = viewModel,
            onDismiss = { showAchievementsSheet = false }
        )
    }

    if (showQuestsSheet) {
        QuestsAndRankSheet(
            viewModel = viewModel,
            onDismiss = { showQuestsSheet = false }
        )
    }

    if (showBookmarksSheet) {
        PanelBookmarksSheet(
            viewModel = viewModel,
            onDismiss = { showBookmarksSheet = false }
        )
    }

    if (showTagMatrixDialog) {
        AdvancedTagMatrixDialog(
            viewModel = viewModel,
            onDismiss = { showTagMatrixDialog = false }
        )
    }
}
}

// ======================== COMPONENT CARDS ========================

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeOnSurfaceVariant
            )
        }
    }
}

@Composable
fun FreshReleasesSlider(
    mangas: List<MangaData>,
    onClick: (MangaData) -> Unit
) {
    val activeMangas = remember(mangas) {
        mangas.filter { !it.attributes?.status.equals("completed", ignoreCase = true) }
    }
    if (activeMangas.isEmpty()) return

    val pagerState = rememberPagerState { activeMangas.size }

    // Smooth auto-scroll across fresh releases
    LaunchedEffect(pagerState, activeMangas.size) {
        while (true) {
            kotlinx.coroutines.delay(4500)
            if (activeMangas.isNotEmpty() && !pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % activeMangas.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFF5722),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "FRESH RELEASE ⚡",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Chapters & Drops",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Text(
                text = "${pagerState.currentPage + 1}/${activeMangas.size}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ThemeOnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) { page ->
            val manga = activeMangas[page]
            val title = manga.attributes?.title?.get("en")
                ?: manga.attributes?.title?.values?.firstOrNull()
                ?: "Fresh Manga"
            val coverUrl = manga.getCoverImageUrl()
            val desc = manga.attributes?.description?.get("en")
                ?: manga.attributes?.description?.values?.firstOrNull()
                ?: ""
            val status = manga.attributes?.status?.replaceFirstChar { it.uppercase() } ?: "Ongoing"

            val tagsList = manga.attributes?.tags?.mapNotNull { it.attributes?.name?.get("en") } ?: emptyList()
            val rating = manga.attributes?.contentRating ?: "safe"
            val is18Plus = rating.equals("pornographic", ignoreCase = true) || rating.equals("erotica", ignoreCase = true)

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onClick(manga) }
                    .testTag("fresh_slide_$page"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f),
                                        Color.Black.copy(alpha = 0.95f)
                                    ),
                                    startY = 50f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Badges & Visible Tags Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // API Source Tag
                            val (sourceName, sourceColor) = when {
                                manga.id.startsWith("mta_") -> "🌊 MANTA" to Color(0xFF007AFF)
                                manga.id.startsWith("mwr_") -> "📖 MANHWAREAD" to Color(0xFF7C4DFF)
                                manga.id.startsWith("mto_") -> "🎨 MANGATOON" to Color(0xFFFF4081)
                                manga.id.startsWith("mt_") -> "⚡ MANHWATOON" to Color(0xFF00B0FF)
                                manga.id.startsWith("3d_") -> "🧊 3D COMIC" to Color(0xFF00E676)
                                else -> "📚 MANGADEX" to Color(0xFFFF5722)
                            }
                            Surface(
                                color = sourceColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    sourceName,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                )
                            }

                            Surface(
                                color = Color(0xFFFF5722),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "⚡ FRESH DROP",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                )
                            }

                            // 18+ / Rating Tag
                            if (is18Plus) {
                                Surface(
                                    color = Color(0xFFD32F2F),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "🔞 18+ MATURE",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            } else if (rating.equals("suggestive", ignoreCase = true)) {
                                Surface(
                                    color = Color(0xFFFF9800),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "🔥 SUGGESTIVE",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color.White
                                        )
                                    )
                                }
                            }

                            Surface(
                                color = ThemeSurface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    status,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp,
                                        color = ThemeOnSurface
                                    )
                                )
                            }
                        }

                        // Visible Genre / Keyword Tags (e.g. NTR, Erotica, Romance, Ecchi)
                        if (tagsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tagsList.take(3).forEach { tagName ->
                                    val isTabooTag = tagName.contains("NTR", ignoreCase = true) ||
                                            tagName.contains("Netorare", ignoreCase = true) ||
                                            tagName.contains("Erotica", ignoreCase = true) ||
                                            tagName.contains("Incest", ignoreCase = true) ||
                                            tagName.contains("Smut", ignoreCase = true)
                                    Surface(
                                        color = if (isTabooTag) Color(0xFF7B1FA2).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = tagName,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (desc.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Indicator Dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(mangas.size.coerceAtMost(10)) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFFF5722) else ThemeSurfaceVariant)
                )
            }
        }
    }
}

@Composable
fun EditorPickHeroCard(manga: MangaData, onClick: () -> Unit) {
    val title = manga.attributes?.title?.get("en")
        ?: manga.attributes?.title?.values?.firstOrNull()
        ?: "Featured Manga"
    val coverUrl = manga.getCoverImageUrl()
    val desc = manga.attributes?.description?.get("en")
        ?: manga.attributes?.description?.values?.firstOrNull()
        ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (coverUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f), Color.Black)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    color = ThemePrimary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "EDITOR'S PICK",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueReadingCard(manga: MangaData, onClick: () -> Unit) {
    val title = manga.attributes?.title?.get("en")
        ?: manga.attributes?.title?.values?.firstOrNull()
        ?: "Manga"
    val coverUrl = manga.getCoverImageUrl()
    val isFullColor = manga.attributes?.tags?.any { tag ->
        val name = tag.attributes?.name?.get("en") ?: ""
        name.equals("Full Color", ignoreCase = true) || name.equals("Official Colored", ignoreCase = true)
    } ?: (title.contains("Full Color", ignoreCase = true) || title.contains("Colored", ignoreCase = true))

    Card(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(ThemeSurfaceVariant))
                }

                if (isFullColor) {
                    Surface(
                        color = Color(0xFF00C853),
                        shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp, topEnd = 0.dp, bottomStart = 0.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "COLOR",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Color.White),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MangaFeedCard(
    manga: MangaData,
    isBookmarked: Boolean,
    isFavorite: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit = {},
    onQuickPeek: (() -> Unit)? = null
) {
    val title = manga.attributes?.title?.get("en")
        ?: manga.attributes?.title?.values?.firstOrNull()
        ?: "Unknown Manga"
    val coverUrl = manga.getCoverImageUrl()
    val desc = manga.attributes?.description?.get("en")
        ?: manga.attributes?.description?.values?.firstOrNull()
        ?: ""
    val authorName = manga.getAuthorName()
    val rating = manga.attributes?.contentRating ?: "safe"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("manga_card_${manga.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp, 125.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(ThemeSurfaceVariant))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (authorName != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "by $authorName",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeSecondary,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            modifier = Modifier.clickable { onAuthorClick() }
                        )
                    }

                    if (desc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (sourceName, sourceColor) = when {
                            manga.id.startsWith("mta_") -> "MANTA" to Color(0xFF2979FF)
                            manga.id.startsWith("mwr_") -> "MANHWAREAD" to Color(0xFF7C4DFF)
                            manga.id.startsWith("mto_") -> "MANGATOON" to Color(0xFFFF4081)
                            manga.id.startsWith("mt_") -> "MANHWATOON" to Color(0xFF00B0FF)
                            manga.id.startsWith("3d_") -> "3D COMIC" to Color(0xFF00E676)
                            else -> "MANGADEX" to Color(0xFFFF6D00)
                        }
                        Surface(
                            color = sourceColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, sourceColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = sourceName,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = sourceColor
                                )
                            )
                        }

                        val isFullColor = manga.attributes?.tags?.any { tag ->
                            val name = tag.attributes?.name?.get("en") ?: ""
                            name.equals("Full Color", ignoreCase = true) || name.equals("Official Colored", ignoreCase = true)
                        } ?: (title.contains("Full Color", ignoreCase = true) || title.contains("Colored", ignoreCase = true))

                        if (isFullColor) {
                            Surface(
                                color = Color(0xFF00C853),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "COLOR",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        if (rating != "safe") {
                            Surface(
                                color = if (rating == "pornographic" || rating == "erotica") Color(0xFFE53935) else Color(0xFFFF9800),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = rating.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        val displayTags = manga.attributes?.tags?.mapNotNull {
                            it.attributes?.name?.get("en") ?: it.attributes?.name?.values?.firstOrNull()
                        }?.filter {
                            !it.equals("Manhwa", true) &&
                            !it.equals("Webtoon", true) &&
                            !it.equals("Full Color", true) &&
                            !it.equals("Official Colored", true)
                        }?.take(2) ?: emptyList()

                        displayTags.forEach { tagName ->
                            val isMatureTag = tagName.contains("18+") || tagName.contains("Erotica") || tagName.contains("Adult")
                            Surface(
                                color = if (isMatureTag) Color(0xFFD32F2F).copy(alpha = 0.2f) else ThemeSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tagName,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isMatureTag) Color(0xFFFF8A80) else ThemeOnSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onQuickPeek != null) {
                            IconButton(
                                onClick = onQuickPeek,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "Quick Peek",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) ThemePrimary else ThemeOnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFE91E63) else ThemeOnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ======================== SEARCH SCREEN CONTENT ========================

@Composable
fun SearchScreenContent(
    viewModel: MainViewModel,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToAuthor: (String) -> Unit
) {
    val searchMangas by viewModel.searchMangas.collectAsState()
    val authorResults by viewModel.authorSearchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isSearchingMore by viewModel.isSearchingMore.collectAsState()
    val hasMoreSearch by viewModel.hasMoreSearch.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val searchSource by viewModel.searchSource.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= searchMangas.size - 4 && hasMoreSearch && !isSearchingMore && !isSearching) {
                    viewModel.loadMoreSearch()
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "Search & Discover",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field"),
                placeholder = {
                    Text(
                        when (searchMode) {
                            SearchMode.TITLE -> "Search manga, manhwa title..."
                            SearchMode.AUTHOR -> "Search creator, author, artist..."
                            SearchMode.TAG -> "Search genres (e.g. Action, Isekai)..."
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ThemePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = ThemeOutline.copy(alpha = 0.4f),
                    focusedContainerColor = ThemeSurface,
                    unfocusedContainerColor = ThemeSurface
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Modes Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchMode.values().forEach { mode ->
                    val isSelected = searchMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateSearchMode(mode) }
                            .testTag("search_mode_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ThemePrimary else ThemeSurfaceVariant,
                        contentColor = if (isSelected) Color.White else ThemeOnSurfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Source Filter Chips
            if (searchMode != SearchMode.AUTHOR) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchSource.values().toList()) { source ->
                        val isSelected = searchSource == source
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ThemePrimary else ThemeSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateSearchSource(source) }
                                .testTag("search_source_${source.name.lowercase()}")
                        ) {
                            Text(
                                text = source.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else ThemeOnSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ThemePrimary)
                }
            } else if (searchMode == SearchMode.AUTHOR) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (authorResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No authors found.", color = ThemeOnSurfaceVariant)
                            }
                        }
                    } else {
                        items(authorResults) { author ->
                            AuthorSearchCard(author = author, onClick = { onNavigateToAuthor(author.id) })
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchMangas.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when (searchSource) {
                                            SearchSource.MANTA -> "No Manta titles found for \"$searchQuery\"."
                                            SearchSource.MANHWAREAD -> "No ManhwaRead titles found for \"$searchQuery\"."
                                            SearchSource.MANGATOON -> "No MangaToon titles found for \"$searchQuery\"."
                                            SearchSource.MANHWATOON -> "No ManhwaToon titles found for \"$searchQuery\"."
                                            else -> "No manga titles found."
                                        },
                                        color = ThemeOnSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (searchSource == SearchSource.MANTA) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Text("Browse All Manta", color = ThemePrimary)
                                        }
                                    }
                                    if (searchSource == SearchSource.MANHWAREAD) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Text("Browse All ManhwaRead", color = ThemePrimary)
                                        }
                                    }
                                    if (searchSource == SearchSource.MANGATOON) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Text("Browse All MangaToon", color = ThemePrimary)
                                        }
                                    }
                                    if (searchSource == SearchSource.MANHWATOON) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Text("Browse All ManhwaToon", color = ThemePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(searchMangas) { manga ->
                            val isBookmarked = bookmarkedIds.contains(manga.id)
                            val isFav = favoriteIds.contains(manga.id)
                            MangaFeedCard(
                                manga = manga,
                                isBookmarked = isBookmarked,
                                isFavorite = isFav,
                                onToggleBookmark = { viewModel.toggleBookmark(manga.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(manga.id) },
                                onClick = { onNavigateToChapters(manga.id) },
                                onAuthorClick = {
                                    val authorId = manga.getAuthorId()
                                    if (authorId != null) onNavigateToAuthor(authorId)
                                },
                                onQuickPeek = { viewModel.openQuickPeek(manga) }
                            )
                        }
                        if (isSearchingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = ThemePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        ScrollToTopFab(
            visible = showScrollToTop,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun AuthorSearchCard(author: AuthorData, onClick: () -> Unit) {
    val name = author.attributes?.name ?: "Unknown Creator"
    val bio = author.attributes?.biography?.get("en")
        ?: author.attributes?.biography?.values?.firstOrNull()
        ?: "Manga / Manhwa Creator"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ThemeSecondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = ThemeSecondary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ThemeOnSurfaceVariant)
        }
    }
}

// ======================== LIBRARY SCREEN CONTENT ========================

@Composable
fun LibraryScreenContent(
    viewModel: MainViewModel,
    onNavigateToChapters: (String) -> Unit,
    onNavigateToAuthor: (String) -> Unit
) {
    val libraryMangas by viewModel.libraryMangas.collectAsState()
    val filter by viewModel.selectedLibraryFilter.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryFilter.values().forEach { f ->
                    val isSelected = filter == f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setLibraryFilter(f) }
                            .testTag("lib_filter_${f.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ThemePrimary else ThemeSurfaceVariant,
                        contentColor = if (isSelected) Color.White else ThemeOnSurfaceVariant
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = f.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (libraryMangas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = ThemeOnSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            when (filter) {
                                LibraryFilter.OFFLINE -> "No offline downloads yet."
                                LibraryFilter.FAVORITES -> "No favorite titles yet."
                                LibraryFilter.ALL -> "No bookmarked titles yet."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = ThemeOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            when (filter) {
                                LibraryFilter.OFFLINE -> "Download chapters to read anytime, anywhere without an internet connection."
                                LibraryFilter.FAVORITES -> "Heart manga titles to save them to your favorites."
                                LibraryFilter.ALL -> "Bookmark or favorite manga to access them quickly here."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeOnSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(libraryMangas) { manga ->
                        val isBookmarked = bookmarkedIds.contains(manga.id)
                        val isFav = favoriteIds.contains(manga.id)
                        MangaFeedCard(
                            manga = manga,
                            isBookmarked = isBookmarked,
                            isFavorite = isFav,
                            onToggleBookmark = { viewModel.toggleBookmark(manga.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(manga.id) },
                            onClick = { onNavigateToChapters(manga.id) },
                            onAuthorClick = {
                                val authorId = manga.getAuthorId()
                                if (authorId != null) onNavigateToAuthor(authorId)
                            },
                            onQuickPeek = { viewModel.openQuickPeek(manga) }
                        )
                    }
                }
            }
        }

        ScrollToTopFab(
            visible = showScrollToTop,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

// ======================== SETTINGS TAB CONTENT ========================

@Composable
fun SettingsTabContent(viewModel: MainViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val readerMode by viewModel.readerMode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isClearingCache by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        // Appearance
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "APPEARANCE & THEME",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThemeSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System")
                        modes.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = themeMode.equals(key, ignoreCase = true),
                                onClick = { viewModel.setThemeMode(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        }

        // Reader Mode
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "READER MODE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThemeSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf("VERTICAL" to "Webtoon (Vertical)", "HORIZONTAL" to "Paged (Horizontal)")
                        modes.forEachIndexed { index, (key, label) ->
                            SegmentedButton(
                                selected = readerMode.equals(key, ignoreCase = true),
                                onClick = { viewModel.setReaderMode(key) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Storage & Cache
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "STORAGE & CACHE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThemeSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isClearingCache = true
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        coil.Coil.imageLoader(context).diskCache?.clear()
                                        coil.Coil.imageLoader(context).memoryCache?.clear()
                                        java.io.File(context.cacheDir, "image_cache").deleteRecursively()
                                        java.io.File(context.cacheDir, "network_image_cache").deleteRecursively()
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Image cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            android.widget.Toast.makeText(context, "Cache cleanup completed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } finally {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isClearingCache = false
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear Image Cache", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White))
                            Text("Frees temporary downloaded page memory", style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant))
                        }
                        if (isClearingCache) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ThemePrimary)
                        } else {
                            Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = ThemePrimary)
                        }
                    }
                }
            }
        }

        // App Information
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ABOUT APPLICATION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ThemeSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", style = MaterialTheme.typography.bodyMedium, color = ThemeOnSurfaceVariant)
                        Text("v${com.example.BuildConfig.VERSION_NAME} (Pro)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Content Catalog", style = MaterialTheme.typography.bodyMedium, color = ThemeOnSurfaceVariant)
                        Text("Manga, Manhwa & 18+ Unrestricted", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ThemePrimary)
                    }
                }
            }
        }
    }
}
