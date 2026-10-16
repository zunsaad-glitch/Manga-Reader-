package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Chapter
import com.example.model.Manga
import com.example.model.ReaderOrientation
import com.example.ui.theme.*

@Composable
fun ReaderScreen(
    manga: Manga,
    chapter: Chapter,
    isLoadingPages: Boolean,
    onRetryPages: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var readerMode by remember { mutableStateOf(ReaderOrientation.VERTICAL_WEBTOON) }
    var isControlsVisible by remember { mutableStateOf(true) }

    val verticalListState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { chapter.pages.size.coerceAtLeast(1) })
    val context = LocalContext.current

    // Track active page
    val currentPage = when (readerMode) {
        ReaderOrientation.VERTICAL_WEBTOON -> (verticalListState.firstVisibleItemIndex + 1).coerceAtMost(chapter.pages.size.coerceAtLeast(1))
        ReaderOrientation.HORIZONTAL_PAGE -> (pagerState.currentPage + 1).coerceAtMost(chapter.pages.size.coerceAtLeast(1))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
    ) {
        if (isLoadingPages) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = NeonAccent, modifier = Modifier.size(44.dp))
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Loading chapter from MangaDex Network...",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Connecting to high-speed CDN...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        } else if (chapter.pages.isEmpty()) {
            // Error / Retry state when chapter pages cannot be fetched
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonAccent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Could not load chapter pages",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This chapter might be temporarily unavailable on MangaDex network or restricted by server.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetryPages,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Loading Pages", color = Color.White)
                }
            }
        } else if (readerMode == ReaderOrientation.VERTICAL_WEBTOON) {
            LazyColumn(
                state = verticalListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vertical_reader_list")
            ) {
                itemsIndexed(chapter.pages) { index, pageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("horizontal_reader_pager")
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(chapter.pages[pageIndex])
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top App Bar Controls
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = DarkBackground.copy(alpha = 0.92f),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Reader",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = manga.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = chapter.title,
                                color = NeonCyan,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Mode Toggle (Webtoon vertical vs Page horizontal)
                    IconButton(
                        onClick = {
                            readerMode = if (readerMode == ReaderOrientation.VERTICAL_WEBTOON) {
                                ReaderOrientation.HORIZONTAL_PAGE
                            } else {
                                ReaderOrientation.VERTICAL_WEBTOON
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (readerMode == ReaderOrientation.VERTICAL_WEBTOON) Icons.Default.ViewCarousel else Icons.Default.SwapVert,
                            contentDescription = "Toggle Reading Mode",
                            tint = NeonAccent
                        )
                    }
                }
            }
        }

        // Bottom Page Indicator Pill
        if (chapter.pages.isNotEmpty()) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkBackground.copy(alpha = 0.88f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$currentPage / ${chapter.pages.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
