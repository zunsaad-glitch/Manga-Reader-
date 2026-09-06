package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.*
import com.example.viewmodel.GenericDetailUiState
import com.example.viewmodel.GenericGalleryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericGalleryReaderScreen(
    galleryId: String,
    initialPage: Int = 0,
    viewModel: GenericGalleryViewModel,
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val scope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(galleryId) {
        if (detailState !is GenericDetailUiState.Success) {
            viewModel.loadGalleryDetail(galleryId)
        }
    }

    when (val state = detailState) {
        is GenericDetailUiState.Idle, is GenericDetailUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ThemePrimary)
            }
        }
        is GenericDetailUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text(state.message, color = ThemeOnSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)) {
                        Text("Back")
                    }
                }
            }
        }
        is GenericDetailUiState.Success -> {
            val gallery = state.gallery
            val pages = gallery.pages
            val pageCount = pages.size

            if (pageCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pages in this gallery", color = Color.White)
                }
                return
            }

            val pagerState = rememberPagerState(
                initialPage = initialPage.coerceIn(0, pageCount - 1),
                pageCount = { pageCount }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Main Horizontal Pager for swiping pages
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showControls = !showControls
                        }
                        .testTag("gallery_reader_pager")
                ) { pageIndex ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = pages[pageIndex],
                            contentDescription = "Page ${pageIndex + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = ThemePrimary)
                                }
                            },
                            error = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Page failed to load", color = Color.LightGray)
                                }
                            }
                        )
                    }
                }

                // Top Bar Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                gallery.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.8f))
                    )
                }

                // Bottom Controls & Slider Overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Current page / Total count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page ${pagerState.currentPage + 1} of $pageCount",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage > 0) {
                                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page", tint = Color.White)
                                    }

                                    IconButton(
                                        onClick = {
                                            if (pagerState.currentPage < pageCount - 1) {
                                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                            }
                                        },
                                        enabled = pagerState.currentPage < pageCount - 1
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page", tint = Color.White)
                                    }
                                }
                            }

                            // Slider / Seek bar
                            if (pageCount > 1) {
                                Slider(
                                    value = pagerState.currentPage.toFloat(),
                                    onValueChange = { pageFloat ->
                                        scope.launch { pagerState.scrollToPage(pageFloat.toInt()) }
                                    },
                                    valueRange = 0f..(pageCount - 1).toFloat(),
                                    steps = (pageCount - 2).coerceAtLeast(0),
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

                // Minimal page badge when controls are hidden
                if (!showControls) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $pageCount",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
