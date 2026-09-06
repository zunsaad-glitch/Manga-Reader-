package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.jandapress.JandaGalleryItem
import com.example.api.jandapress.JandaProvider
import com.example.ui.theme.*
import com.example.viewmodel.JandaDetailUiState
import com.example.viewmodel.JandaPressViewModel
import com.example.viewmodel.JandaSubTab
import com.example.viewmodel.JandaUiState

private val JANDA_POPULAR_TAGS = listOf(
    "3d", "ecchi", "3d comic", "pururin", "hentaifox", "3hentai", "hanpatsu", "urakan", "sinful lust", "big sister",
    "doujinshi", "full color", "romance", "vanilla", "milf", "schoolgirl", "maid", "cosplay", "harem", "yuri"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JandaPressScreen(
    viewModel: JandaPressViewModel = viewModel(),
    onNavigateToDetail: (provider: String, id: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var showApiDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "JandaPress",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnSurface
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "ALL-IN-ONE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF818CF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Multi-provider doujinshi & adult comics hub",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ThemeOnSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val randomId = viewModel.getRandomId()
                            val provider = viewModel.currentProvider.value.code
                            onNavigateToDetail(provider, randomId)
                        },
                        modifier = Modifier.testTag("janda_random_btn")
                    ) {
                        Icon(
                            Icons.Filled.Casino,
                            contentDescription = "Random Gallery",
                            tint = ThemePrimary
                        )
                    }
                    IconButton(
                        onClick = { showApiDialog = true },
                        modifier = Modifier.testTag("janda_api_settings_btn")
                    ) {
                        Icon(
                            Icons.Filled.Dns,
                            contentDescription = "API Settings",
                            tint = ThemeOnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeBackground)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            JandaPressContent(
                viewModel = viewModel,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }

    if (showApiDialog) {
        val currentApiUrl by viewModel.apiUrl.collectAsState()
        var editUrl by remember { mutableStateOf(currentApiUrl) }

        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            title = {
                Text(
                    "JandaPress API Configuration",
                    fontWeight = FontWeight.Bold,
                    color = ThemeOnSurface
                )
            },
            text = {
                Column {
                    Text(
                        "Set custom JandaPress REST API base URL or proxy server:",
                        fontSize = 13.sp,
                        color = ThemeOnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("API Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Supported Providers: Pururin, HentaiFox, 3Hentai, nHentai, Simply-Hentai, AsmHentai",
                        fontSize = 11.sp,
                        color = ThemePrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomApiUrl(editUrl)
                        showApiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) {
                    Text("Cancel", color = ThemeOnSurfaceVariant)
                }
            },
            containerColor = ThemeSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JandaPressContent(
    viewModel: JandaPressViewModel,
    onNavigateToDetail: (provider: String, id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProvider by viewModel.currentProvider.collectAsState()
    val currentTab by viewModel.currentSubTab.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val savedIds by viewModel.savedIds.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeBackground)
    ) {
        // --- 1. Provider Horizontal Selector ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JandaProvider.entries.forEach { provider ->
                val isSelected = currentProvider == provider
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(provider.badgeColorHex) else ThemeSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(provider.badgeColorHex) else ThemeSurfaceVariant
                    ),
                    modifier = Modifier
                        .clickable { viewModel.selectProvider(provider) }
                        .testTag("janda_provider_${provider.code}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(provider.iconEmoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = provider.displayName,
                            color = if (isSelected) Color.White else ThemeOnSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 2. Search Field ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search ${currentProvider.displayName} or tag...",
                        color = ThemeOnSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = ThemeOnSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.updateSearchQuery("")
                            viewModel.loadExplore()
                        }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = ThemeOnSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.performSearch()
                }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = ThemeSurfaceVariant,
                    focusedContainerColor = ThemeSurface,
                    unfocusedContainerColor = ThemeSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("janda_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.performSearch()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier.testTag("janda_search_submit")
            ) {
                Text("Search", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // --- 3. Sub Tabs ---
        TabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = ThemeBackground,
            contentColor = ThemePrimary,
            divider = { HorizontalDivider(color = ThemeSurfaceVariant.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = currentTab == JandaSubTab.EXPLORE,
                onClick = { viewModel.selectSubTab(JandaSubTab.EXPLORE) },
                text = { Text("Explore", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Explore, contentDescription = "Explore", modifier = Modifier.size(16.dp)) },
                selectedContentColor = ThemePrimary,
                unselectedContentColor = ThemeOnSurfaceVariant
            )
            Tab(
                selected = currentTab == JandaSubTab.POPULAR,
                onClick = { viewModel.selectSubTab(JandaSubTab.POPULAR) },
                text = { Text("Popular", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Popular", modifier = Modifier.size(16.dp)) },
                selectedContentColor = ThemePrimary,
                unselectedContentColor = ThemeOnSurfaceVariant
            )
            Tab(
                selected = currentTab == JandaSubTab.LATEST,
                onClick = { viewModel.selectSubTab(JandaSubTab.LATEST) },
                text = { Text("Latest", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.NewReleases, contentDescription = "Latest", modifier = Modifier.size(16.dp)) },
                selectedContentColor = ThemePrimary,
                unselectedContentColor = ThemeOnSurfaceVariant
            )
            Tab(
                selected = currentTab == JandaSubTab.SAVED,
                onClick = { viewModel.selectSubTab(JandaSubTab.SAVED) },
                text = {
                    Text(
                        if (savedIds.isNotEmpty()) "Saved (${savedIds.size})" else "Saved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved", modifier = Modifier.size(16.dp)) },
                selectedContentColor = ThemePrimary,
                unselectedContentColor = ThemeOnSurfaceVariant
            )
        }

        // --- 4. Popular Tags Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            JANDA_POPULAR_TAGS.forEach { tag ->
                val isTagSelected = selectedTag.equals(tag, ignoreCase = true)
                FilterChip(
                    selected = isTagSelected,
                    onClick = { viewModel.selectTag(tag) },
                    label = { Text("#$tag", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ThemePrimary.copy(alpha = 0.2f),
                        selectedLabelColor = ThemePrimary,
                        containerColor = ThemeSurface,
                        labelColor = ThemeOnSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isTagSelected,
                        borderColor = if (isTagSelected) ThemePrimary else ThemeSurfaceVariant
                    )
                )
            }
        }

        // --- 5. Gallery Grid Content ---
        when (val state = searchState) {
            is JandaUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ThemePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Loading JandaPress ${currentProvider.displayName}...",
                            color = ThemeOnSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            is JandaUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = "No results",
                            tint = ThemeOnSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            state.message,
                            color = ThemeOnSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadExplore() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                        ) {
                            Text("Reset & Show All")
                        }
                    }
                }
            }
            is JandaUiState.Success -> {
                if (state.data.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No galleries found.", color = ThemeOnSurfaceVariant)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("janda_gallery_grid")
                    ) {
                        items(state.data, key = { it.id }) { item ->
                            JandaGalleryCard(
                                item = item,
                                isSaved = savedIds.contains(item.id) || savedIds.contains(item.id.substringAfter("_")),
                                onBookmarkToggle = { viewModel.toggleBookmark(item.id) },
                                onClick = { onNavigateToDetail(item.provider, item.id) }
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
            is JandaUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ThemePrimary)
                }
            }
        }
    }
}

@Composable
fun JandaGalleryCard(
    item: JandaGalleryItem,
    isSaved: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val providerInfo = JandaProvider.fromCode(item.provider)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSurfaceVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("janda_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(Color(0xFF1E1E1E))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )

                // Provider Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(providerInfo.badgeColorHex).copy(alpha = 0.9f),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        "${providerInfo.iconEmoji} ${providerInfo.displayName}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Bookmark button
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) ThemePrimary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Page Count pill at bottom
                if (item.pageCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            "${item.pageCount}P",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ThemeOnSurface,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (item.favorites > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites",
                                tint = ThemeOnSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                if (item.favorites >= 1000) "${item.favorites / 1000}k" else "${item.favorites}",
                                fontSize = 10.sp,
                                color = ThemeOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
