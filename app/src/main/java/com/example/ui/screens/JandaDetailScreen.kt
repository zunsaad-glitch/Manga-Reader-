package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.jandapress.JandaGalleryDetail
import com.example.api.jandapress.JandaProvider
import com.example.ui.theme.*
import com.example.viewmodel.JandaDetailUiState
import com.example.viewmodel.JandaPressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JandaDetailScreen(
    provider: String,
    id: String,
    viewModel: JandaPressViewModel = viewModel(),
    onBack: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null,
    onStartReading: (pageIndex: Int) -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val savedIds by viewModel.savedIds.collectAsState()
    val subscribedIds by viewModel.subscribedIds.collectAsState()
    val isSaved = savedIds.contains(id) || savedIds.contains(id.substringAfter("_"))
    val isSubscribed = subscribedIds.contains(id) || subscribedIds.contains(id.substringAfter("_"))

    LaunchedEffect(provider, id) {
        viewModel.loadDetail(provider, id)
    }

    val currentDetail = (detailState as? JandaDetailUiState.Success)?.detail

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gallery Details",
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeOnSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleSubscription(id, currentDetail?.title, currentDetail?.coverUrl) },
                        modifier = Modifier.testTag("janda_detail_subscribe_btn")
                    ) {
                        Icon(
                            if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = "Subscribe to Drops",
                            tint = if (isSubscribed) Color(0xFFFFB300) else ThemeOnSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark(id) },
                        modifier = Modifier.testTag("janda_detail_bookmark_btn")
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) ThemePrimary else ThemeOnSurface
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
            when (val state = detailState) {
                is JandaDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ThemePrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading gallery details...",
                                color = ThemeOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                is JandaDetailUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = "Error",
                                tint = ThemePrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                state.message,
                                color = ThemeOnSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadDetail(provider, id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is JandaDetailUiState.Success -> {
                    JandaDetailContent(
                        detail = state.detail,
                        isSaved = isSaved,
                        isSubscribed = isSubscribed,
                        onBookmarkToggle = { viewModel.toggleBookmark(id) },
                        onSubscribeToggle = { viewModel.toggleSubscription(id, state.detail.title, state.detail.coverUrl) },
                        onArtistClick = onArtistClick,
                        onStartReading = onStartReading
                    )
                }
            }
        }
    }
}

@Composable
fun JandaDetailContent(
    detail: JandaGalleryDetail,
    isSaved: Boolean,
    isSubscribed: Boolean = false,
    onBookmarkToggle: () -> Unit,
    onSubscribeToggle: () -> Unit = {},
    onArtistClick: ((String) -> Unit)?,
    onStartReading: (pageIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val providerInfo = JandaProvider.fromCode(detail.provider)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("janda_detail_view")
    ) {
        // --- 1. HERO HEADER (Span all 3 columns) ---
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover Poster
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ThemeSurfaceVariant)
                            .clickable { onStartReading(0) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(detail.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = detail.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(providerInfo.badgeColorHex),
                            modifier = Modifier
                                .padding(4.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text(
                                "${providerInfo.iconEmoji} ${providerInfo.displayName}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Metadata Info
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnSurface,
                                lineHeight = 20.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!detail.japaneseTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = detail.japaneseTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ThemeOnSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Artist Row
                        if (!detail.artist.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onArtistClick?.invoke(detail.artist) }
                            ) {
                                Text(
                                    "Artist: ",
                                    color = ThemeOnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                Text(
                                    detail.artist,
                                    color = ThemePrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Badges Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeSurfaceVariant
                            ) {
                                Text(
                                    detail.language.uppercase(),
                                    color = ThemeOnSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ThemeSurfaceVariant
                            ) {
                                Text(
                                    "${detail.pages.size.coerceAtLeast(detail.pageCount)} Pages",
                                    color = ThemeOnSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onStartReading(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("janda_read_now_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Read Online", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onSubscribeToggle,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSubscribed) Color(0xFFFFB300) else ThemeSurfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSubscribed) Color(0xFFFFB300).copy(alpha = 0.12f) else Color.Transparent
                        ),
                        modifier = Modifier.height(48.dp).testTag("janda_subscribe_btn")
                    ) {
                        Icon(
                            if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = "Subscribe to Drops",
                            tint = if (isSubscribed) Color(0xFFFFB300) else ThemeOnSurface
                        )
                    }

                    OutlinedButton(
                        onClick = onBookmarkToggle,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSaved) ThemePrimary else ThemeSurfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSaved) ThemePrimary.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) ThemePrimary else ThemeOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tags Section
                if (detail.tags.isNotEmpty()) {
                    Text(
                        "Tags",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OptInRowTags(tags = detail.tags)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                HorizontalDivider(color = ThemeSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "All Pages (${detail.pages.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnSurface
                        )
                    )
                    Text(
                        "Tap any page to jump",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemeOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- 2. THUMBNAIL PAGES GRID ---
        itemsIndexed(detail.pages) { index, pageUrl ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clickable { onStartReading(index) }
                    .testTag("janda_thumbnail_$index")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        shape = RoundedCornerShape(topStart = 6.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            "${index + 1}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OptInRowTags(tags: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.take(8).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ThemeSurfaceVariant.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, ThemeSurfaceVariant)
            ) {
                Text(
                    "#$tag",
                    color = ThemeOnSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
