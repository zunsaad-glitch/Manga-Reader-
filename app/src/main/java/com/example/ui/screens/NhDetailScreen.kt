package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.nhapi.NhGalleryDetail
import com.example.ui.theme.*
import com.example.viewmodel.NhApiViewModel
import com.example.viewmodel.NhDetailUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NhDetailScreen(
    id: String,
    viewModel: NhApiViewModel = viewModel(),
    onBack: () -> Unit,
    onStartReading: (Int) -> Unit = {},
    onArtistClick: ((String) -> Unit)? = null,
    onTagClick: ((String) -> Unit)? = null
) {
    val detailState by viewModel.detailState.collectAsState()
    val savedIds by viewModel.savedIds.collectAsState()
    val subscribedIds by viewModel.subscribedIds.collectAsState()
    val isSaved = savedIds.contains(id)
    val isSubscribed = subscribedIds.contains(id)
    val context = LocalContext.current

    LaunchedEffect(id) {
        viewModel.fetchDetail(id)
    }

    val currentDetail = (detailState as? NhDetailUiState.Success)?.detail
    val topTitle = currentDetail?.title ?: "Gallery #${id}"

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nh_detail_back")) {
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
                        modifier = Modifier.testTag("nh_detail_subscribe_btn")
                    ) {
                        Icon(
                            if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                            contentDescription = "Subscribe to Drops",
                            tint = if (isSubscribed) Color(0xFFFFB300) else ThemeOnSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleSaved(id) },
                        modifier = Modifier.testTag("nh_detail_save_btn")
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save Gallery",
                            tint = if (isSaved) ThemePrimary else ThemeOnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        },
        bottomBar = {
            if (detailState is NhDetailUiState.Success) {
                val detail = (detailState as NhDetailUiState.Success).detail
                Surface(
                    color = ThemeSurface,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, ThemeSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleSubscription(id, detail.title, detail.coverUrl) },
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (isSubscribed) Color(0xFFFFB300).copy(alpha = 0.15f) else ThemeSurfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("nh_subscribe_bottom_btn")
                        ) {
                            Icon(
                                if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                contentDescription = "Subscribe to Drops",
                                tint = if (isSubscribed) Color(0xFFFFB300) else ThemeOnSurface
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleSaved(id) },
                            modifier = Modifier
                                .size(50.dp)
                                .background(ThemeSurfaceVariant, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isSaved) ThemePrimary else ThemeOnSurface
                            )
                        }

                        Button(
                            onClick = { onStartReading(0) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("start_reading_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Read Gallery (${detail.pages.size.coerceAtLeast(detail.pageCount)} Pages)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(ThemeBackground)
        ) {
            when (val s = detailState) {
                is NhDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemePrimary)
                    }
                }
                is NhDetailUiState.Success -> {
                    val detail = s.detail
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hero Cover & Overview
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(ThemeSurfaceVariant)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(detail.coverUrl.ifBlank { detail.pages.firstOrNull() })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = detail.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    ThemeBackground.copy(alpha = 0.85f),
                                                    ThemeBackground
                                                ),
                                                startY = 80f
                                            )
                                        )
                                )
                            }
                        }

                        // Title & Info
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = detail.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeOnSurface
                                    )
                                )

                                if (!detail.japaneseTitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = detail.japaneseTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = ThemeOnSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Metadata Badges
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThemePrimary.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            "#${detail.id}",
                                            color = ThemePrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (!detail.language.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ThemeSurfaceVariant
                                        ) {
                                            Text(
                                                detail.language.replaceFirstChar { it.uppercase() },
                                                color = ThemeOnSurface,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (detail.favorites > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color(0xFFE91E63),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${detail.favorites}",
                                                color = ThemeOnSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Key Details Table
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                                    border = BorderStroke(1.dp, ThemeSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (!detail.artist.isNullOrBlank()) {
                                            DetailRow(
                                                label = "Artist",
                                                value = detail.artist,
                                                isAccent = true,
                                                onClick = {
                                                    onArtistClick?.invoke(detail.artist)
                                                }
                                            )
                                        }
                                        if (!detail.parody.isNullOrBlank()) {
                                            DetailRow("Parody", detail.parody)
                                        }
                                        if (detail.characters.isNotEmpty()) {
                                            DetailRow("Characters", detail.characters.joinToString(", "))
                                        }
                                        DetailRow("Pages", "${detail.pages.size.coerceAtLeast(detail.pageCount)}")
                                    }
                                }

                                // Tags flow
                                if (detail.tags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Tags",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeOnSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        detail.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ThemeSurface,
                                                border = BorderStroke(1.dp, ThemeSurfaceVariant),
                                                modifier = Modifier.clickable {
                                                    if (onTagClick != null) {
                                                        onTagClick(tag)
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    tag,
                                                    color = ThemeOnSurface,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    "Page Previews (${detail.pages.size} Pages)",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeOnSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Page Previews items directly in the grid
                        itemsIndexed(detail.pages, key = { index, _ -> "page_$index" }) { index, pageUrl ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ThemeSurfaceVariant)
                                    .clickable { onStartReading(index) }
                                    .testTag("preview_page_$index")
                            ) {
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
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                is NhDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = ThemeOnSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.fetchDetail(id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isAccent: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ThemeOnSurfaceVariant, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                color = if (isAccent) ThemePrimary else ThemeOnSurface,
                fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            if (onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ThemePrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
