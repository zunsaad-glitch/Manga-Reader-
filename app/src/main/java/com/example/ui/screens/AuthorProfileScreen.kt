package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.AuthorData
import com.example.api.MangaData
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorProfileScreen(
    authorId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToChapters: (String) -> Unit
) {
    val authorProfile by viewModel.selectedAuthorProfile.collectAsState()
    val authorWorks by viewModel.authorWorks.collectAsState()
    val isLoading by viewModel.isAuthorLoading.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Popular") }

    LaunchedEffect(authorId) {
        viewModel.fetchAuthorProfile(authorId)
    }

    val authorName = authorProfile?.attributes?.name ?: "Artist"
    val filteredWorks = remember(authorWorks, selectedFilter, selectedSort) {
        var list = authorWorks
        if (selectedFilter == "English") {
            list = list.filter { it.attributes?.originalLanguage == "en" || it.attributes?.title?.containsKey("en") == true }
        } else if (selectedFilter == "Japanese") {
            list = list.filter { it.attributes?.originalLanguage == "ja" || it.attributes?.title?.containsKey("ja") == true }
        }
        if (selectedSort == "Newest") {
            list = list.reversed()
        }
        list
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "artist: ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = ThemeOnSurfaceVariant
                            )
                        )
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("author_profile_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground)
        ) {
            if (isLoading && authorProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ThemePrimary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Banner & Artist Meta (nHentai layout benchmark)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val initial = authorName.take(1).uppercase()

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ThemePrimary, ThemeSecondary)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (initial.isNotBlank()) initial else "A",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ThemePrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "ARTIST",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = ThemePrimary
                                            )
                                        )
                                    }

                                    Text(
                                        text = authorName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "${authorWorks.size} galleries / works uploaded",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ThemeOnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                val bio = authorProfile?.attributes?.biography?.get("en")
                                    ?: authorProfile?.attributes?.biography?.values?.firstOrNull()
                                if (!bio.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = ThemeOutline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = bio,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            lineHeight = 18.sp,
                                            color = ThemeOnSurfaceVariant
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val twitter = authorProfile?.attributes?.twitter
                                if (!twitter.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThemeSurfaceVariant
                                    ) {
                                        Text(
                                            text = "@$twitter",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemePrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Filter & Sort Bar
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Language Tabs
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("All", "English", "Japanese").forEach { lang ->
                                    val isSelected = selectedFilter == lang
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFilter = lang },
                                        label = {
                                            Text(
                                                lang,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThemePrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = ThemeSurfaceVariant,
                                            labelColor = ThemeOnSurfaceVariant
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // Sort Order
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Popular", "Newest").forEach { sort ->
                                    val isSelected = selectedSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSort = sort },
                                        label = {
                                            Text(
                                                sort,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThemeSecondary,
                                            selectedLabelColor = Color.White,
                                            containerColor = ThemeSurfaceVariant,
                                            labelColor = ThemeOnSurfaceVariant
                                        ),
                                        border = null,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Galleries / Works Count Header
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "GALLERIES (${filteredWorks.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = ThemePrimary
                            )
                        )
                    }

                    // Empty state
                    if (filteredWorks.isEmpty() && !isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.MenuBook,
                                        contentDescription = null,
                                        tint = ThemeOnSurfaceVariant,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No galleries match the selected filter",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ThemeOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // NHentai Benchmark Grid Cards
                        items(filteredWorks, key = { it.id }) { manga ->
                            NhArtistGalleryCard(
                                manga = manga,
                                isBookmarked = bookmarkedIds.contains(manga.id),
                                isFavorite = favoriteIds.contains(manga.id),
                                onClick = { onNavigateToChapters(manga.id) },
                                onToggleBookmark = { viewModel.toggleBookmark(manga.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(manga.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NhArtistGalleryCard(
    manga: MangaData,
    isBookmarked: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val title = manga.attributes?.title?.get("en")
        ?: manga.attributes?.title?.values?.firstOrNull()
        ?: "Gallery #${manga.id.take(6)}"
    val coverUrl = manga.getCoverImageUrl()
    val rating = manga.attributes?.contentRating ?: "safe"
    val is18Plus = rating.equals("pornographic", ignoreCase = true) || rating.equals("erotica", ignoreCase = true)
    val lang = manga.attributes?.originalLanguage?.uppercase() ?: "EN"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("nh_artist_card_${manga.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.25f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .background(ThemeSurfaceVariant)
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

                // Top Language / 18+ Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "[$lang]",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        )
                    }

                    if (is18Plus) {
                        Surface(
                            color = Color(0xFFD32F2F).copy(alpha = 0.9f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "18+",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                // Bottom Gradient Overlay with Page / Chapter Hint
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
            }

            // Card Caption & Title (nHentai style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 32.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val firstTag = manga.attributes?.tags?.firstOrNull()?.attributes?.name?.get("en") ?: "Doujin"
                    Surface(
                        color = ThemeSurfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = firstTag,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = ThemeOnSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) ThemePrimary else ThemeOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFE91E63) else ThemeOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
