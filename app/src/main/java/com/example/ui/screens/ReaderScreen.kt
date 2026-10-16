package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.model.Chapter
import com.example.model.Manga
import com.example.model.ReaderOrientation
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    manga: Manga,
    chapter: Chapter,
    allChapters: List<Chapter> = emptyList(),
    isLoadingPages: Boolean,
    isAscending: Boolean = true,
    onToggleOrder: () -> Unit = {},
    onChapterChange: (Chapter) -> Unit = {},
    onRetryPages: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var readerMode by remember { mutableStateOf(ReaderOrientation.VERTICAL_WEBTOON) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var showChapterSheet by remember { mutableStateOf(false) }

    val verticalListState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { chapter.pages.size.coerceAtLeast(1) })
    val context = LocalContext.current

    // Zoom state for page reader
    var pageZoom by remember { mutableFloatStateOf(1f) }

    // Sequential chapter lookup: previous and next
    val prevChapter = remember(allChapters, chapter) {
        allChapters.filter { it.chapterNumber < chapter.chapterNumber }.maxByOrNull { it.chapterNumber }
    }
    val nextChapter = remember(allChapters, chapter) {
        allChapters.filter { it.chapterNumber > chapter.chapterNumber }.minByOrNull { it.chapterNumber }
    }

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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        var isImageLoading by remember { mutableStateOf(true) }
                        var isImageError by remember { mutableStateOf(false) }

                        if (isImageLoading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NeonAccent,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Page ${index + 1}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(pageUrl)
                                .crossfade(true)
                                .listener(
                                    onStart = { isImageLoading = true; isImageError = false },
                                    onSuccess = { _, _ -> isImageLoading = false },
                                    onError = { _, _ -> isImageLoading = false; isImageError = true }
                                )
                                .build(),
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )

                        if (isImageError) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NeonAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Failed to load page ${index + 1}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // End-of-chapter Completion Banner for seamless binge-reading
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Finished Chapter ${chapter.chapterNumber.toInt()}!",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${manga.title} • ${chapter.title}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            if (nextChapter != null) {
                                Button(
                                    onClick = { onChapterChange(nextChapter) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("read_next_chapter_btn")
                                ) {
                                    Text("Read Next: Chapter ${nextChapter.chapterNumber.toInt()}", color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Text(
                                    text = "You've caught up with the latest chapter!",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = onBackClick,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Back to Manga Details", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        } else {
            // Horizontal Pager Reader with Double-Tap Zoom
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("horizontal_reader_pager")
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    pageZoom = if (pageZoom > 1f) 1f else 2f
                                },
                                onTap = {
                                    isControlsVisible = !isControlsVisible
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(chapter.pages[pageIndex])
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = pageZoom
                                scaleY = pageZoom
                            }
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
                color = DarkBackground.copy(alpha = 0.94f),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = chapter.title,
                                color = NeonCyan,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Chapter Drawer/List Button
                        if (allChapters.isNotEmpty()) {
                            IconButton(onClick = { showChapterSheet = true }) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = "Chapter List",
                                    tint = Color.White
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
        }

        // Bottom Chapter & Page Navigation Bar
        if (chapter.pages.isNotEmpty()) {
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DarkBackground.copy(alpha = 0.92f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Chapter Button
                        IconButton(
                            onClick = { prevChapter?.let { onChapterChange(it) } },
                            enabled = prevChapter != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Chapter",
                                tint = if (prevChapter != null) NeonAccent else Color.Gray.copy(alpha = 0.4f)
                            )
                        }

                        // Page Indicator Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$currentPage / ${chapter.pages.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        // Next Chapter Button
                        IconButton(
                            onClick = { nextChapter?.let { onChapterChange(it) } },
                            enabled = nextChapter != null
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Chapter",
                                tint = if (nextChapter != null) NeonAccent else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // Quick In-Reader Chapter Picker Bottom Sheet
        if (showChapterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChapterSheet = false },
                containerColor = DarkSurface,
                scrimColor = Color.Black.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val sortedChapters = remember(allChapters, isAscending) {
                        if (isAscending) allChapters.sortedBy { it.chapterNumber } else allChapters.sortedByDescending { it.chapterNumber }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters (${allChapters.size})",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleOrder() }
                                    .testTag("reader_toggle_order_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = NeonAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAscending) "Ch. 1 → Last" else "Latest → 1",
                                        color = NeonAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(onClick = { showChapterSheet = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(sortedChapters, key = { it.id }) { ch ->
                            val isCurrent = ch.id == chapter.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) NeonAccent.copy(alpha = 0.18f) else DarkSurfaceElevated,
                                border = if (isCurrent) ButtonDefaults.outlinedButtonBorder else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showChapterSheet = false
                                        if (!isCurrent) {
                                            onChapterChange(ch)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ch.title,
                                        color = if (isCurrent) NeonAccent else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (isCurrent) {
                                        Text("Reading", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
}
