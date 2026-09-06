package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
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
import coil.Coil
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.api.*
import com.example.ui.theme.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.components.AiStoryAssistantSheet
import com.example.ui.components.AudioDramaSheet
import com.example.ui.components.MangaTranslatorSheet
import com.example.ui.components.PanelBookmarkDialog
import com.example.ui.components.QuoteCardGeneratorDialog
import com.example.ui.components.ReaderSettingsSheet
import com.example.util.AmbientSoundPlayer
import com.example.util.AmbientSoundType
import com.example.util.ChapterExporter
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ReaderMode {
    VERTICAL_WEBTOON,
    HORIZONTAL_PAGED
}

fun buildReaderImageRequest(context: android.content.Context, url: String, reloadKey: Int = 0): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(url)
        .crossfade(true)
    if (reloadKey > 0) {
        builder.memoryCachePolicy(coil.request.CachePolicy.WRITE_ONLY)
            .diskCachePolicy(coil.request.CachePolicy.WRITE_ONLY)
            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
    }
    if (url.contains("nhentai") || url.contains("pururin") || url.contains("hentaifox") || url.contains("3hentai")) {
        builder.addHeader("Referer", "https://nhentai.net/")
    } else if (url.contains("mangadex")) {
        builder.addHeader("Referer", "https://mangadex.org/")
    }
    return builder.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterReaderScreen(
    chapterId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var currentChapterId by remember(chapterId) { mutableStateOf(chapterId) }
    val chapters by viewModel.chapters.collectAsState()
    val imageUrls by viewModel.imageUrls.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val incognitoMode by viewModel.incognitoMode.collectAsState()

    // Themes & Display Settings
    val readerTheme by viewModel.readerTheme.collectAsState()
    val readerBrightnessDim by viewModel.readerBrightnessDim.collectAsState()
    val pageTurnHaptics by viewModel.pageTurnHaptics.collectAsState()
    val pageTurnSound by viewModel.pageTurnSound.collectAsState()

    val canvasBgColor = remember(readerTheme) {
        when (readerTheme) {
            "OLED" -> Color(0xFF000000)
            "DARK" -> Color(0xFF141419)
            "SEPIA" -> Color(0xFF261D17)
            "EINK" -> Color(0xFF1E1E1E)
            "ROSE" -> Color(0xFF24151B)
            else -> Color(0xFF000000)
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var showChapterSheet by remember { mutableStateOf(false) }
    var showAmbienceDialog by remember { mutableStateOf(false) }
    var showExportProgress by remember { mutableStateOf(false) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var showTranslatorSheet by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var showQuoteCardDialog by remember { mutableStateOf(false) }
    var showAudioDramaSheet by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var showToolsMenu by remember { mutableStateOf(false) }
    var isDualPageMode by remember { mutableStateOf(false) }

    // Auto-Scroll State
    var isAutoScrolling by remember { mutableStateOf(false) }
    var autoScrollSpeedMultiplier by remember { mutableStateOf(1f) } // 1x, 2x, 3x

    var readerMode by remember { mutableStateOf(ReaderMode.VERTICAL_WEBTOON) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentChapter = remember(chapters, currentChapterId) {
        chapters.find { it.id == currentChapterId }
    }

    val downloadedChapterIds by viewModel.downloadedChapterIds.collectAsState()
    val isOfflineChapter = remember(currentChapterId, downloadedChapterIds, imageUrls) {
        downloadedChapterIds.contains(currentChapterId) || imageUrls.firstOrNull()?.startsWith("file://") == true
    }

    val currentIndex = remember(chapters, currentChapterId) {
        chapters.indexOfFirst { it.id == currentChapterId }
    }

    val prevChapter = remember(chapters, currentIndex) {
        if (currentIndex in 1 until chapters.size) chapters[currentIndex - 1] else null
    }

    val nextChapter = remember(chapters, currentIndex) {
        if (currentIndex >= 0 && currentIndex < chapters.size - 1) chapters[currentIndex + 1] else null
    }

    LaunchedEffect(currentChapterId) {
        viewModel.fetchChapterImages(currentChapterId)
    }

    // Smart Page Preloader: Pre-buffer 4 pages ahead in background for instant rendering
    LaunchedEffect(imageUrls) {
        if (imageUrls.isNotEmpty()) {
            val loader = Coil.imageLoader(context)
            imageUrls.take(6).forEach { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    // Preload next batch on scroll
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleItemIndex) {
        if (imageUrls.isNotEmpty() && firstVisibleItemIndex + 2 < imageUrls.size) {
            val loader = Coil.imageLoader(context)
            val nextBatch = imageUrls.drop(firstVisibleItemIndex + 1).take(4)
            nextBatch.forEach { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .build()
                loader.enqueue(request)
            }
        }
    }

    // Auto-Scroll Loop
    LaunchedEffect(isAutoScrolling, autoScrollSpeedMultiplier, readerMode) {
        if (isAutoScrolling && readerMode == ReaderMode.VERTICAL_WEBTOON) {
            while (isActive && isAutoScrolling) {
                val scrollAmount = 2.2f * autoScrollSpeedMultiplier
                listState.scrollBy(scrollAmount)
                delay(16) // ~60fps smooth glide
            }
        }
    }

    // Clean up ambient sound on leave
    DisposableEffect(Unit) {
        onDispose {
            AmbientSoundPlayer.stop()
        }
    }

    val currentMangaDetail by viewModel.currentMangaDetail.collectAsState()
    val rawNum = currentChapter?.attributes?.chapter?.trim()
    val chTitle = currentChapter?.attributes?.title?.trim() ?: ""
    val mangaTitle = currentMangaDetail?.attributes?.title?.values?.firstOrNull()
        ?: currentMangaDetail?.attributes?.title?.get("en")
        ?: "Manga"
    val mangaSynopsis = currentMangaDetail?.attributes?.description?.get("en")
        ?: currentMangaDetail?.attributes?.description?.values?.firstOrNull()
    val mangaTags = currentMangaDetail?.attributes?.tags?.mapNotNull {
        it.attributes?.name?.get("en") ?: it.attributes?.name?.values?.firstOrNull()
    } ?: emptyList()
    val resolvedMangaId = currentMangaDetail?.id?.takeIf { it.isNotBlank() }
        ?: currentChapter?.id
        ?: chapterId

    val displayHeading = when {
        !rawNum.isNullOrBlank() -> "Ch. $rawNum"
        chTitle.isNotBlank() -> chTitle
        else -> "Chapter"
    }

    Scaffold(
        containerColor = canvasBgColor,
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = {
                        Column(modifier = Modifier.padding(start = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayHeading,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isOfflineChapter) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF4CAF50).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "OFFLINE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF81C784),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (incognitoMode) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF9C27B0).copy(alpha = 0.35f)
                                    ) {
                                        Text(
                                            text = "🕵️ INCOGNITO",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFFE1BEE7),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                val currentReleaseDate = currentChapter?.getShortReleaseDate() ?: ""
                                if (currentReleaseDate.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF00897B).copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "📅 $currentReleaseDate",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF80CBC4),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (chTitle.isNotBlank() && chTitle != displayHeading) "$mangaTitle • $chTitle" else mangaTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reader_back_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        // Switch reading mode
                        IconButton(
                            onClick = {
                                isAutoScrolling = false
                                readerMode = if (readerMode == ReaderMode.VERTICAL_WEBTOON) {
                                    ReaderMode.HORIZONTAL_PAGED
                                } else {
                                    ReaderMode.VERTICAL_WEBTOON
                                }
                            },
                            modifier = Modifier.testTag("toggle_reader_mode_btn")
                        ) {
                            Icon(
                                if (readerMode == ReaderMode.VERTICAL_WEBTOON) Icons.Default.ViewDay else Icons.Default.ViewCarousel,
                                contentDescription = "Switch Mode",
                                tint = Color.White
                            )
                        }

                        // Display Settings Button
                        IconButton(
                            onClick = { showReaderSettingsSheet = true },
                            modifier = Modifier.testTag("reader_settings_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Reader Display Settings", tint = Color.White)
                        }

                        // More Reader Tools Overflow Menu
                        Box {
                            IconButton(
                                onClick = { showToolsMenu = true },
                                modifier = Modifier.testTag("reader_more_tools_btn")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Tools", tint = Color.White)
                            }

                            DropdownMenu(
                                expanded = showToolsMenu,
                                onDismissRequest = { showToolsMenu = false },
                                modifier = Modifier.background(ThemeSurfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🎙️ AI Audio Drama & Voice", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showAudioDramaSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFFFF007F), modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_audio_drama_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("📑 Bookmark Moment & Note", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showBookmarkDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_bookmark_moment_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("✨ Gemini Lore & Recap", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showAiAssistantSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFD1C4E9), modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_ai_assistant_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("🌐 AI Page & SFX Translator", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showTranslatorSheet = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Translate, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_translator_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("🖼️ Quote Card Generator", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showQuoteCardDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.CropOriginal, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_quote_card_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("🎧 Ambient Soundscapes", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        showAmbienceDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                             if (AmbientSoundPlayer.currentSound != AmbientSoundType.NONE) Icons.Default.MusicNote else Icons.Default.VolumeMute,
                                            contentDescription = null,
                                            tint = if (AmbientSoundPlayer.currentSound != AmbientSoundType.NONE) ThemePrimary else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("reader_ambience_btn")
                                )

                                DropdownMenuItem(
                                    text = { Text("📦 Export CBZ / Share", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        showToolsMenu = false
                                        if (imageUrls.isNotEmpty()) {
                                            showExportProgress = true
                                            scope.launch {
                                                val cbzFile = ChapterExporter.exportChapterToCbz(
                                                    context = context,
                                                    mangaTitle = mangaTitle,
                                                    chapterTitle = displayHeading,
                                                    imageUrls = imageUrls
                                                )
                                                showExportProgress = false
                                                if (cbzFile != null) {
                                                    ChapterExporter.shareCbzFile(context, cbzFile, "$mangaTitle - $displayHeading")
                                                } else {
                                                    Toast.makeText(context, "Export failed. Please check network or storage.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("reader_cbz_export_btn")
                                )

                                if (chapters.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("📑 Chapter List", color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            showToolsMenu = false
                                            showChapterSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        },
                                        modifier = Modifier.testTag("open_chapter_drawer_btn")
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.85f)
                    )
                )
            }
        },
        bottomBar = {
            if (showControls && imageUrls.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().testTag("reader_bottom_bar")
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Chapter
                            IconButton(
                                onClick = {
                                    prevChapter?.let { currentChapterId = it.id }
                                },
                                enabled = prevChapter != null,
                                modifier = Modifier.testTag("prev_chapter_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Chapter",
                                    tint = if (prevChapter != null) Color.White else Color.Gray
                                )
                            }

                            // Auto-Scroll Play/Pause & Speed Dial Pill (In Webtoon Mode)
                            if (readerMode == ReaderMode.VERTICAL_WEBTOON) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isAutoScrolling) ThemePrimary.copy(alpha = 0.25f) else ThemeSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isAutoScrolling) ThemePrimary else ThemeOutline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { isAutoScrolling = !isAutoScrolling }
                                        .testTag("auto_scroll_toggle_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Auto Scroll",
                                            tint = if (isAutoScrolling) ThemePrimary else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isAutoScrolling) "Auto: ${autoScrollSpeedMultiplier.toInt()}x" else "Auto-Scroll",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAutoScrolling) ThemePrimary else Color.White
                                        )
                                    }
                                }
                            }

                            // Dual-Page Spread Toggle (In Horizontal Mode)
                            if (readerMode == ReaderMode.HORIZONTAL_PAGED) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isDualPageMode) ThemePrimary.copy(alpha = 0.25f) else ThemeSurfaceVariant,
                                    border = BorderStroke(1.dp, if (isDualPageMode) ThemePrimary else ThemeOutline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { isDualPageMode = !isDualPageMode }
                                ) {
                                    Text(
                                        text = if (isDualPageMode) "📖 Dual-Page" else "📄 Single",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDualPageMode) ThemePrimary else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Chapter Picker Button in Bottom Bar
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = ThemeSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showChapterSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatListNumbered, null, tint = ThemePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        displayHeading,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "(${imageUrls.size}p)",
                                        fontSize = 11.sp,
                                        color = ThemeOnSurfaceVariant
                                    )
                                }
                            }

                            // Next Chapter
                            IconButton(
                                onClick = {
                                    nextChapter?.let { currentChapterId = it.id }
                                },
                                enabled = nextChapter != null,
                                modifier = Modifier.testTag("next_chapter_btn")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Chapter",
                                    tint = if (nextChapter != null) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .clickable {
                    if (isAutoScrolling) {
                        isAutoScrolling = false // Tap anywhere to pause auto-scroll
                    } else {
                        showControls = !showControls
                    }
                }
        ) {
            if (isLoading && imageUrls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ThemePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Buffering chapter pages in HD...", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                    }
                }
            } else if (imageUrls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = ThemePrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Unable to load chapter pages",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "The scanlation host might be slow or this chapter has no uploaded image server nodes.",
                            color = ThemeOnSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.fetchChapterImages(currentChapterId) },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                                modifier = Modifier.testTag("retry_chapter_images_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", color = Color.White)
                            }
                            if (chapters.size > 1) {
                                OutlinedButton(
                                    onClick = { showChapterSheet = true },
                                    border = BorderStroke(1.dp, ThemePrimary),
                                    modifier = Modifier.testTag("choose_another_chapter_btn")
                                ) {
                                    Text("Other Chapters", color = ThemePrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                val context = LocalContext.current
                val readerImageLoader = remember(context) { com.example.api.NetworkClient.getImageLoader(context) }

                if (readerMode == ReaderMode.VERTICAL_WEBTOON) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(imageUrls) { index, url ->
                            var reloadKey by remember(url) { mutableStateOf(0) }
                            var loadError by remember(url, reloadKey) { mutableStateOf(false) }

                            val imageRequest = remember(url, reloadKey) {
                                buildReaderImageRequest(context, url, reloadKey)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 350.dp)
                                    .wrapContentHeight()
                            ) {
                                SubcomposeAsyncImage(
                                    model = imageRequest,
                                    imageLoader = readerImageLoader,
                                    contentDescription = "Page ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    contentScale = ContentScale.FillWidth,
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(350.dp)
                                                .background(ThemeSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(
                                                    color = ThemePrimary,
                                                    modifier = Modifier.size(32.dp),
                                                    strokeWidth = 2.5.dp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "Loading page ${index + 1}...",
                                                    color = ThemeOnSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    },
                                    onError = { loadError = true },
                                    onSuccess = { loadError = false },
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .background(ThemeSurfaceVariant.copy(alpha = 0.5f))
                                                .clickable { reloadKey++ },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "Tap to Reload",
                                                    tint = ThemePrimary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "Page ${index + 1} - Tap to Retry",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Tap to refresh image connection",
                                                    color = ThemeOnSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Bottom navigation card at the end of vertical webtoon
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("End of $displayHeading", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (prevChapter != null) {
                                        OutlinedButton(
                                            onClick = { currentChapterId = prevChapter.id },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                        ) {
                                            Text("← Previous")
                                        }
                                    }
                                    if (nextChapter != null) {
                                        Button(
                                            onClick = { currentChapterId = nextChapter.id },
                                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                                        ) {
                                            Text("Next Chapter →", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Paged Horizontal Mode (Single or Dual Page Spreads)
                    val pageStep = if (isDualPageMode) 2 else 1
                    val totalPagesCount = if (isDualPageMode) (imageUrls.size + 1) / 2 else imageUrls.size
                    val pagerState = rememberPagerState(pageCount = { totalPagesCount })

                    // Haptic pulse feedback on page turns
                    LaunchedEffect(pagerState.currentPage) {
                        if (pageTurnHaptics) {
                            try {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { spreadIndex ->
                            if (isDualPageMode) {
                                val leftIndex = spreadIndex * 2
                                val rightIndex = leftIndex + 1

                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (leftIndex < imageUrls.size) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = buildReaderImageRequest(context, imageUrls[leftIndex]),
                                                imageLoader = readerImageLoader,
                                                contentDescription = "Page ${leftIndex + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                    if (rightIndex < imageUrls.size) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = buildReaderImageRequest(context, imageUrls[rightIndex]),
                                                imageLoader = readerImageLoader,
                                                contentDescription = "Page ${rightIndex + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AsyncImage(
                                        model = buildReaderImageRequest(context, imageUrls[spreadIndex]),
                                        imageLoader = readerImageLoader,
                                        contentDescription = "Page ${spreadIndex + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        // Page floating indicator
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = if (isDualPageMode) {
                                    val currentP = (pagerState.currentPage * 2 + 1).coerceAtMost(imageUrls.size)
                                    "Pages $currentP-${(currentP + 1).coerceAtMost(imageUrls.size)} / ${imageUrls.size}"
                                } else {
                                    "${pagerState.currentPage + 1} / ${imageUrls.size}"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Floating Auto-Scroll Speed Stepper (When active)
            AnimatedVisibility(
                visible = isAutoScrolling,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Speed:", fontSize = 11.sp, color = ThemeOnSurfaceVariant)
                        listOf(1f, 2f, 3f, 4f).forEach { spd ->
                            val isSelected = autoScrollSpeedMultiplier == spd
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) ThemePrimary else Color.Transparent,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { autoScrollSpeedMultiplier = spd }
                            ) {
                                Text(
                                    "${spd.toInt()}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else ThemeOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Exporting CBZ Loading Dialog
            if (showExportProgress) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = ThemePrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Packaging Chapter into CBZ...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "Creating offline comic bundle",
                                    color = ThemeOnSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Ambience Soundscapes Dialog
            if (showAmbienceDialog) {
                AlertDialog(
                    onDismissRequest = { showAmbienceDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Headphones, null, tint = ThemePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reading Soundscapes", color = Color.White)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Select relaxing background ambience while reading:",
                                color = ThemeOnSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            AmbientSoundType.values().forEach { sound ->
                                val isSelected = AmbientSoundPlayer.currentSound == sound
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) ThemePrimary.copy(alpha = 0.2f) else ThemeSurfaceVariant,
                                    border = if (isSelected) BorderStroke(1.dp, ThemePrimary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            AmbientSoundPlayer.play(sound)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sound.icon, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                sound.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) ThemePrimary else Color.White
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = ThemePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAmbienceDialog = false }) {
                            Text("Done", color = ThemePrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = ThemeSurface
                )
            }

            // Chapter Quick Navigation Bottom Sheet
            if (showChapterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showChapterSheet = false },
                    containerColor = ThemeSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Select Chapter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chapters) { ch ->
                                val chNum = ch.attributes?.chapter?.trim()
                                val chTitle = ch.attributes?.title?.trim() ?: ""
                                val isCurrent = ch.id == currentChapterId
                                val name = when {
                                    !chNum.isNullOrBlank() -> "Chapter $chNum"
                                    chTitle.isNotBlank() -> chTitle
                                    else -> "Chapter"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) ThemePrimary.copy(alpha = 0.2f) else ThemeSurfaceVariant,
                                    border = if (isCurrent) BorderStroke(1.dp, ThemePrimary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            currentChapterId = ch.id
                                            showChapterSheet = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                name,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCurrent) ThemePrimary else Color.White
                                            )
                                            if (chTitle.isNotBlank() && chTitle != name) {
                                                Text(chTitle, fontSize = 11.sp, color = ThemeOnSurfaceVariant)
                                            }
                                            val chDate = ch.getFormattedReleaseDate()
                                            if (chDate.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("📅 $chDate", fontSize = 10.sp, color = Color(0xFF80CBC4))
                                            }
                                        }
                                        if (isCurrent) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = ThemePrimary)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Screen Night Dimmer Filter Overlay
            if (readerBrightnessDim > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = readerBrightnessDim))
                )
            }

            // Reader Settings Sheet
            if (showReaderSettingsSheet) {
                ReaderSettingsSheet(
                    viewModel = viewModel,
                    isDualPageMode = isDualPageMode,
                    onToggleDualPage = { isDualPageMode = it },
                    onDismiss = { showReaderSettingsSheet = false }
                )
            }

            // Gemini AI Story Assistant Sheet
            if (showAiAssistantSheet) {
                AiStoryAssistantSheet(
                    mangaTitle = mangaTitle,
                    chapterTitle = displayHeading,
                    synopsis = mangaSynopsis,
                    tags = mangaTags,
                    viewModel = viewModel,
                    onDismiss = { showAiAssistantSheet = false }
                )
            }

            // AI Manga Page & SFX Translator Sheet
            if (showTranslatorSheet) {
                val pageIndex = listState.firstVisibleItemIndex + 1
                val currentImg = imageUrls.getOrNull(pageIndex - 1) ?: imageUrls.firstOrNull()
                MangaTranslatorSheet(
                    mangaTitle = mangaTitle,
                    pageNumber = pageIndex,
                    pageImageUrl = currentImg,
                    viewModel = viewModel,
                    onDismiss = { showTranslatorSheet = false }
                )
            }

            // Panel Snipping & Quote Card Generator Dialog
            if (showQuoteCardDialog) {
                val pageIndex = listState.firstVisibleItemIndex
                val currentImageUrl = imageUrls.getOrNull(pageIndex) ?: imageUrls.firstOrNull()
                QuoteCardGeneratorDialog(
                    mangaTitle = mangaTitle,
                    chapterTitle = displayHeading,
                    imageUrl = currentImageUrl,
                    onDismiss = { showQuoteCardDialog = false }
                )
            }

            // AI Dynamic Audio Drama & Narration Sheet
            if (showAudioDramaSheet) {
                AudioDramaSheet(
                    mangaTitle = mangaTitle,
                    chapterTitle = displayHeading,
                    synopsis = mangaSynopsis,
                    viewModel = viewModel,
                    onDismiss = { showAudioDramaSheet = false }
                )
            }

            // Bookmark Moment & Panel Journal Dialog
            if (showBookmarkDialog) {
                val pageIndex = listState.firstVisibleItemIndex + 1
                val currentImg = imageUrls.getOrNull(pageIndex - 1) ?: imageUrls.firstOrNull()
                PanelBookmarkDialog(
                    mangaId = resolvedMangaId,
                    mangaTitle = mangaTitle,
                    chapterTitle = displayHeading,
                    pageNumber = pageIndex,
                    imageUrl = currentImg,
                    viewModel = viewModel,
                    onDismiss = { showBookmarkDialog = false }
                )
            }
        }
    }
}

