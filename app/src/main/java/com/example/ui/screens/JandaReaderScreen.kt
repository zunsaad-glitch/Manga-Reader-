package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.api.jandapress.JandaProvider
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemeSurfaceVariant
import com.example.viewmodel.JandaDetailUiState
import com.example.viewmodel.JandaPressViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JandaReaderScreen(
    provider: String,
    id: String,
    initialPage: Int = 0,
    viewModel: JandaPressViewModel = viewModel(),
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var readerMode by remember { mutableStateOf(ReaderMode.VERTICAL_WEBTOON) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(provider, id) {
        if (detailState !is JandaDetailUiState.Success || (detailState as JandaDetailUiState.Success).detail.id != id) {
            viewModel.loadDetail(provider, id)
        }
    }

    val pageList = remember(detailState) {
        if (detailState is JandaDetailUiState.Success) {
            (detailState as JandaDetailUiState.Success).detail.pages
        } else {
            emptyList()
        }
    }

    val galleryTitle = remember(detailState) {
        if (detailState is JandaDetailUiState.Success) {
            (detailState as JandaDetailUiState.Success).detail.title
        } else {
            "Gallery #$id"
        }
    }

    val providerInfo = remember(provider) {
        JandaProvider.fromCode(provider)
    }

    val totalPages = pageList.size
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) {
        totalPages.coerceAtLeast(1)
    }
    val listState = rememberLazyListState()

    val currentPage = if (readerMode == ReaderMode.HORIZONTAL_PAGED) {
        pagerState.currentPage + 1
    } else {
        (listState.firstVisibleItemIndex + 1).coerceAtMost(totalPages)
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    galleryTitle,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(providerInfo.badgeColorHex)
                                ) {
                                    Text(
                                        providerInfo.displayName,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (totalPages > 0) {
                                Text(
                                    "Page $currentPage of $totalPages",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close Reader",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                readerMode = if (readerMode == ReaderMode.VERTICAL_WEBTOON) {
                                    ReaderMode.HORIZONTAL_PAGED
                                } else {
                                    ReaderMode.VERTICAL_WEBTOON
                                }
                            },
                            modifier = Modifier.testTag("toggle_janda_reader_mode_btn")
                        ) {
                            Icon(
                                if (readerMode == ReaderMode.VERTICAL_WEBTOON) Icons.Default.ViewDay else Icons.Default.ViewCarousel,
                                contentDescription = "Toggle Reading Mode",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls && totalPages > 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().testTag("janda_reader_bottom_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Page $currentPage of $totalPages",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ThemeSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (readerMode == ReaderMode.VERTICAL_WEBTOON) "Vertical Webtoon" else "Horizontal Paged",
                                    color = ThemePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = (currentPage - 1).toFloat(),
                            onValueChange = { pageIndex ->
                                val target = pageIndex.toInt().coerceIn(0, totalPages - 1)
                                scope.launch {
                                    if (readerMode == ReaderMode.HORIZONTAL_PAGED) {
                                        pagerState.scrollToPage(target)
                                    } else {
                                        listState.scrollToItem(target)
                                    }
                                }
                            },
                            valueRange = 0f..(totalPages - 1).coerceAtLeast(0).toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            colors = SliderDefaults.colors(
                                thumbColor = ThemePrimary,
                                activeTrackColor = ThemePrimary,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        ) {
            if (pageList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ThemePrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading manga pages...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                when (readerMode) {
                    ReaderMode.HORIZONTAL_PAGED -> {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val pageUrl = pageList.getOrNull(page)
                            JandaZoomableImage(
                                url = pageUrl,
                                contentDescription = "Page ${page + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    ReaderMode.VERTICAL_WEBTOON -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(pageList) { idx, pageUrl ->
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Page ${idx + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(350.dp)
                                                .background(ThemeSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = ThemePrimary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .background(ThemeSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.MenuBook,
                                                    contentDescription = null,
                                                    tint = ThemePrimary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "Page ${idx + 1} loading...",
                                                    color = Color.LightGray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 350.dp)
                                        .wrapContentHeight()
                                )
                            }

                            // Bottom navigation card at the end of vertical webtoon
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "End of Gallery",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onBack,
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                                    ) {
                                        Text("Back to Details", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JandaZoomableImage(
    url: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        if (scale > 1f) {
            val maxOffsetX = (scale - 1f) * 500f
            val maxOffsetY = (scale - 1f) * 800f
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
