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
import com.example.api.nhapi.NhGallery
import com.example.ui.theme.*
import com.example.viewmodel.NhApiViewModel
import com.example.viewmodel.NhSubTab
import com.example.viewmodel.NhUiState

private val POPULAR_TAGS = listOf(
    "3d", "ecchi", "3d comic", "hanpatsu", "urakan", "sinful lust", "big sister", "doujinshi", "romance", "vanilla",
    "full color", "english", "japanese", "milf", "schoolgirl", "maid", "cosplay", "yuri", "erotica"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NhSearchScreen(
    viewModel: NhApiViewModel = viewModel(),
    onNavigateToDetail: (String) -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "NH Gallery Explorer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnSurface
                            )
                        )
                        Text(
                            "Browse, search tags or 6-digit magic codes",
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
                            val randomId = viewModel.getRandomCuratedId()
                            onNavigateToDetail(randomId)
                        },
                        modifier = Modifier.testTag("nh_random_btn")
                    ) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = "Random Gallery",
                            tint = ThemePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ThemeBackground)
        ) {
            NhSearchContent(
                viewModel = viewModel,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NhSearchContent(
    viewModel: NhApiViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val currentSubTab by viewModel.currentSubTab.collectAsState()
    val state by viewModel.searchState.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val savedIds by viewModel.savedIds.collectAsState()
    val savedGalleries by viewModel.savedGalleries.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf(query) }

    LaunchedEffect(query) {
        if (inputText != query) {
            inputText = query
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Sub-tabs Row (Popular, Latest, Tags, Saved)
        PrimaryTabRow(
            selectedTabIndex = currentSubTab.ordinal,
            containerColor = ThemeSurface,
            contentColor = ThemePrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = currentSubTab == NhSubTab.POPULAR,
                onClick = { viewModel.selectSubTab(NhSubTab.POPULAR) },
                text = { Text("🔥 Popular", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nh_subtab_popular")
            )
            Tab(
                selected = currentSubTab == NhSubTab.LATEST,
                onClick = { viewModel.selectSubTab(NhSubTab.LATEST) },
                text = { Text("✨ Latest", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nh_subtab_latest")
            )
            Tab(
                selected = currentSubTab == NhSubTab.TAGS,
                onClick = { viewModel.selectSubTab(NhSubTab.TAGS) },
                text = { Text("🏷️ Tags", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.testTag("nh_subtab_tags")
            )
            Tab(
                selected = currentSubTab == NhSubTab.SAVED,
                onClick = { viewModel.selectSubTab(NhSubTab.SAVED) },
                text = {
                    Text(
                        if (savedIds.isNotEmpty()) "❤️ (${savedIds.size})" else "❤️ Saved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier.testTag("nh_subtab_saved")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar & Random Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    viewModel.updateSearchQuery(it)
                },
                placeholder = { Text("Search tag, title, or 6-digit ID...", color = ThemeOnSurfaceVariant, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = ThemePrimary)
                },
                trailingIcon = {
                    if (inputText.isNotBlank()) {
                        IconButton(onClick = {
                            inputText = ""
                            viewModel.updateSearchQuery("")
                            viewModel.selectSubTab(NhSubTab.POPULAR)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ThemeOnSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = ThemeSurfaceVariant,
                    focusedContainerColor = ThemeSurface,
                    unfocusedContainerColor = ThemeSurface,
                    focusedTextColor = ThemeOnSurface,
                    unfocusedTextColor = ThemeOnSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    val q = inputText.trim()
                    if (q.all { it.isDigit() } && q.length in 4..7) {
                        onNavigateToDetail(q)
                    } else {
                        viewModel.search(q)
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("nh_search_input")
            )

            // Random button
            FilledTonalButton(
                onClick = {
                    val randomId = viewModel.getRandomCuratedId()
                    onNavigateToDetail(randomId)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ThemePrimary.copy(alpha = 0.15f),
                    contentColor = ThemePrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = "Random", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Dice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Tag Filter Chips Row (Only shown on TAGS subtab or when a tag is active)
        if (currentSubTab == NhSubTab.TAGS || selectedTag != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EXPLORE TAGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemePrimary,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    POPULAR_TAGS.forEach { tag ->
                        val isSelected = selectedTag.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.selectTag(tag)
                            },
                            label = {
                                Text(
                                    tag.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = ThemeSurface,
                                labelColor = ThemeOnSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) ThemePrimary else ThemeSurfaceVariant
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Content Area
        if (currentSubTab == NhSubTab.SAVED) {
            // Saved Galleries View
            if (savedGalleries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = ThemePrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Saved Galleries Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tap the bookmark icon on any gallery card to save it here for fast offline reading.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ThemeOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(savedGalleries, key = { it.id }) { gallery ->
                        NhGalleryGridCard(
                            gallery = gallery,
                            isSaved = savedIds.contains(gallery.id),
                            onToggleSave = { viewModel.toggleSaved(gallery.id) },
                            onClick = { onNavigateToDetail(gallery.id) }
                        )
                    }
                }
            }
        } else {
            when (val s = state) {
                is NhUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ThemePrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading galleries...",
                                color = ThemeOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                is NhUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(s.data, key = { it.id }) { gallery ->
                            NhGalleryGridCard(
                                gallery = gallery,
                                isSaved = savedIds.contains(gallery.id),
                                onToggleSave = { viewModel.toggleSaved(gallery.id) },
                                onClick = { onNavigateToDetail(gallery.id) }
                            )
                        }

                        // Load More Button
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        color = ThemePrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.loadMore() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = ThemePrimary
                                        ),
                                        border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(44.dp)
                                    ) {
                                        Text("Load More", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                is NhUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Failed to load galleries",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ThemeOnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (currentSubTab == NhSubTab.POPULAR) {
                                        viewModel.loadPopular()
                                    } else if (currentSubTab == NhSubTab.LATEST) {
                                        viewModel.loadLatest()
                                    } else {
                                        viewModel.search(inputText.ifBlank { "english" })
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                is NhUiState.Idle -> {
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
}

@Composable
fun NhGalleryGridCard(
    gallery: NhGallery,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("gallery_card_${gallery.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, ThemeSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(ThemeSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(gallery.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = gallery.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient shadow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 100f
                            )
                        )
                )

                // Top badges row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ID badge (magic number)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.85f)
                    ) {
                        Text(
                            "#${gallery.id}",
                            color = ThemePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Bookmark / Save Action Button
                    if (onToggleSave != null) {
                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) ThemePrimary else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Page count badge
                if (gallery.pageCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            "${gallery.pageCount}P",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = gallery.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ThemeOnSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!gallery.artist.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "By ${gallery.artist}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemePrimary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (gallery.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gallery.tags.take(3).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemeOnSurfaceVariant,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
