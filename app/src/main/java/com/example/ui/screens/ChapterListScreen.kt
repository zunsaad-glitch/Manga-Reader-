package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import com.example.ui.components.AiStoryAssistantSheet
import com.example.ui.components.ShelvesManagerDialog
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.api.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

fun getLanguageDisplayName(langCode: String): String {
    return when (langCode.lowercase()) {
        "en" -> "🇺🇸 English"
        "es", "es-la" -> "🇪🇸 Spanish"
        "pt-br", "pt" -> "🇧🇷 Portuguese"
        "fr" -> "🇫🇷 French"
        "id" -> "🇮🇩 Indonesian"
        "ja" -> "🇯🇵 Japanese"
        "ko" -> "🇰🇷 Korean"
        "zh", "zh-hk", "zh-ro" -> "🇨🇳 Chinese"
        "ru" -> "🇷🇺 Russian"
        "de" -> "🇩🇪 German"
        "it" -> "🇮🇹 Italian"
        "ar" -> "🇸🇦 Arabic"
        "vi" -> "🇻🇳 Vietnamese"
        "th" -> "🇹🇭 Thai"
        "tr" -> "🇹🇷 Turkish"
        "pl" -> "🇵🇱 Polish"
        "hi" -> "🇮🇳 Hindi"
        "tl" -> "🇵🇭 Tagalog"
        "uk" -> "🇺🇦 Ukrainian"
        "all" -> "🌐 All"
        else -> "🏳️ ${langCode.uppercase()}"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChapterListScreen(
    mangaId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToReader: (String) -> Unit,
    onNavigateToAuthor: (String) -> Unit = {},
    onNavigateToManga: (String) -> Unit = {}
) {
    val chapters by viewModel.chapters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val subscribedIds by viewModel.subscribedIds.collectAsState()
    val isBookmarked = bookmarkedIds.contains(mangaId)
    val isFavorite = favoriteIds.contains(mangaId)
    val isSubscribed = subscribedIds.contains(mangaId)

    // Get the manga from all possible lists (currentMangaDetail, mangas, searchMangas, libraryMangas, authorWorks, adult/mature lists)
    val currentMangaDetail by viewModel.currentMangaDetail.collectAsState()
    val allMangas by viewModel.mangas.collectAsState()
    val searchMangas by viewModel.searchMangas.collectAsState()
    val libraryMangas by viewModel.libraryMangas.collectAsState()
    val authorWorks by viewModel.authorWorks.collectAsState()
    val adultWebtoonsList by viewModel.adultWebtoonsList.collectAsState()
    val adultComicsList by viewModel.adultComicsList.collectAsState()
    val parodyMangasList by viewModel.parodyMangasList.collectAsState()
    val matureNtrLibrary by viewModel.matureNtrLibrary.collectAsState()
    val ecchiComicsList by viewModel.ecchiComicsList.collectAsState()
    val threeDComicsList by viewModel.threeDComicsList.collectAsState()
    val goatMangas by viewModel.goatMangas.collectAsState()
    val topRatedMangas by viewModel.topRatedMangas.collectAsState()
    val latestMangas by viewModel.latestMangas.collectAsState()
    val comicsCategoryList by viewModel.comicsCategoryList.collectAsState()
    val doujinshiList by viewModel.doujinshiList.collectAsState()

    val manga = remember(
        mangaId, currentMangaDetail, allMangas, searchMangas, libraryMangas, authorWorks,
        adultWebtoonsList, adultComicsList, parodyMangasList, matureNtrLibrary,
        ecchiComicsList, threeDComicsList, goatMangas, topRatedMangas, latestMangas, comicsCategoryList, doujinshiList
    ) {
        currentMangaDetail?.takeIf { it.id == mangaId }
            ?: allMangas.find { it.id == mangaId }
            ?: searchMangas.find { it.id == mangaId }
            ?: libraryMangas.find { it.id == mangaId }
            ?: authorWorks.find { it.id == mangaId }
            ?: adultWebtoonsList.find { it.id == mangaId }
            ?: adultComicsList.find { it.id == mangaId }
            ?: parodyMangasList.find { it.id == mangaId }
            ?: matureNtrLibrary.find { it.id == mangaId }
            ?: ecchiComicsList.find { it.id == mangaId }
            ?: threeDComicsList.find { it.id == mangaId }
            ?: goatMangas.find { it.id == mangaId }
            ?: topRatedMangas.find { it.id == mangaId }
            ?: latestMangas.find { it.id == mangaId }
            ?: comicsCategoryList.find { it.id == mangaId }
            ?: doujinshiList.find { it.id == mangaId }
    }

    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedChapterLanguage by viewModel.selectedChapterLanguage.collectAsState()
    val allRawChapters by viewModel.allRawChapters.collectAsState()
    val chaptersError by viewModel.chaptersError.collectAsState()
    val sortAscending by viewModel.chapterSortAscending.collectAsState()
    val context = LocalContext.current

    var chapterSearchQuery by remember { mutableStateOf("") }
    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var showShelvesDialog by remember { mutableStateOf(false) }
    var showTopBarMenu by remember { mutableStateOf(false) }

    val displayedChapters = remember(chapters, chapterSearchQuery) {
        if (chapterSearchQuery.isNotBlank()) {
            val q = chapterSearchQuery.trim().lowercase()
            chapters.filter {
                val num = it.attributes?.chapter ?: ""
                val title = it.attributes?.title ?: ""
                num.lowercase().contains(q) || title.lowercase().contains(q)
            }
        } else {
            chapters
        }
    }

    val similarMangas by viewModel.similarMangas.collectAsState()
    val downloadedChapterIds by viewModel.downloadedChapterIds.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadingChapterId by viewModel.downloadingChapterId.collectAsState()

    LaunchedEffect(mangaId) {
        viewModel.fetchMangaDetail(mangaId)
        viewModel.fetchChapters(mangaId)
    }

    LaunchedEffect(manga) {
        if (manga != null) {
            viewModel.fetchSimilarMangas(manga)
        }
    }

    val mangaTitle = manga?.attributes?.title?.get("en")
        ?: manga?.attributes?.title?.values?.firstOrNull()
        ?: "Chapters"

    val authorName = manga?.getAuthorName()
    val authorId = manga?.getAuthorId()
    val coverUrl = manga?.getCoverImageUrl()

    val rawDescription = manga?.attributes?.description?.get("en")
        ?: manga?.attributes?.description?.values?.firstOrNull()

    val cleanSynopsis = remember(rawDescription, mangaTitle, manga) {
        if (!rawDescription.isNullOrBlank()) {
            rawDescription
                .replace(Regex("\\[url=[^\\]]*\\]|\\[/url\\]|\\[b\\]|\\[/b\\]|\\[i\\]|\\[/i\\]|\\[\\*\\]|\\*\\*\\*|---|\\*\\*"), "")
                .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
                .trim()
        } else {
            val format = when (manga?.attributes?.originalLanguage) {
                "ko" -> "manhwa & webtoon series"
                "zh", "zh-hk" -> "manhua comic"
                "en", "fr", "es" -> "graphic novel & comic"
                else -> "manga masterpiece"
            }
            "Read $mangaTitle, an acclaimed $format featuring captivating artwork, storyline, and dynamic characters. Browse and read all available translated chapters below."
        }
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        mangaTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chapter_list_back_btn")
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
                    IconButton(
                        onClick = { viewModel.toggleFavorite(mangaId) },
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF4081) else ThemeOnSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleBookmark(mangaId) },
                        modifier = Modifier.testTag("bookmark_button")
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) ThemePrimary else ThemeOnSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showTopBarMenu = true },
                            modifier = Modifier.testTag("chapter_list_more_menu_btn")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false },
                            modifier = Modifier.background(ThemeSurfaceVariant)
                        ) {
                            DropdownMenuItem(
                                text = { Text("✨ AI Story Lore & Recap", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showTopBarMenu = false
                                    showAiAssistantSheet = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFD1C4E9), modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("ai_story_lore_btn")
                            )

                            DropdownMenuItem(
                                text = { Text("📁 Add to Shelf", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showTopBarMenu = false
                                    showShelvesDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("add_to_shelf_btn")
                            )

                            DropdownMenuItem(
                                text = { Text(if (sortAscending) "⬇️ Sort: Newest First" else "⬆️ Sort: Oldest First", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.toggleChapterSort()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SwapVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("sort_chapters_btn")
                            )

                            DropdownMenuItem(
                                text = { Text(if (isSubscribed) "🔔 Subscribed" else "🔕 Subscribe to Drops", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.toggleSubscription(mangaId, mangaTitle, coverUrl)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isSubscribed) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                        contentDescription = null,
                                        tint = if (isSubscribed) Color(0xFFFFB300) else ThemeOnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("subscribe_button")
                            )
                        }
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
                .background(ThemeBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Info Card
                if (manga != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (coverUrl != null) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = mangaTitle,
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = mangaTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!authorName.isNullOrBlank() && !authorId.isNullOrBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable { onNavigateToAuthor(authorId) }
                                                .testTag("author_pill_${authorId}"),
                                            shape = RoundedCornerShape(50),
                                            color = ThemePrimary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = ThemePrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = authorName,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ThemePrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    val status = manga.attributes?.status?.replaceFirstChar { it.uppercase() } ?: "Ongoing"
                                    val lang = when (manga.attributes?.originalLanguage) {
                                        "ko" -> "Manhwa"
                                        "zh", "zh-hk" -> "Manhua"
                                        else -> "Manga"
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ThemeSecondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = lang,
                                                style = MaterialTheme.typography.labelSmall.copy(color = ThemeSecondary),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ThemeSurfaceVariant
                                        ) {
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.labelSmall.copy(color = ThemeOnSurfaceVariant),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        val rating = manga.attributes?.contentRating
                                        if (rating != null && (rating == "erotica" || rating == "pornographic")) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFD32F2F).copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = if (rating == "pornographic") "18+ Adult" else "18+ Mature",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFFF8A80),
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Synopsis & Story Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("manga_synopsis_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = ThemePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SYNOPSIS & SUMMARY",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = ThemePrimary
                                        )
                                    )
                                }

                                if (cleanSynopsis.length > 200) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = ThemePrimary.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ThemePrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Icon(
                                                if (isSynopsisExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = ThemePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = cleanSynopsis,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 22.sp,
                                    color = ThemeOnSurface.copy(alpha = 0.9f)
                                ),
                                maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Genre and Category Tags
                            val tags = manga?.attributes?.tags?.mapNotNull { tag ->
                                tag.attributes?.name?.get("en") ?: tag.attributes?.name?.values?.firstOrNull()
                            } ?: emptyList()

                            if (tags.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "GENRES & THEMES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = ThemeOnSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tags.take(12).forEach { tagName ->
                                        val isMatureTag = tagName.contains("18+") || tagName.contains("Erotica") || tagName.contains("Doujinshi") || tagName.contains("Smut")
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isMatureTag) Color(0xFFD32F2F).copy(alpha = 0.15f) else ThemeSurfaceVariant,
                                            border = if (isMatureTag) BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f)) else null
                                        ) {
                                            Text(
                                                text = tagName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (isMatureTag) Color(0xFFFF8A80) else ThemeOnSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Similar Content Suggestions
                if (similarMangas.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✨ SIMILAR & RECOMMENDED TITLES",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp,
                                        color = ThemeSecondary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(similarMangas) { simManga ->
                                    val simTitle = simManga.attributes?.title?.get("en")
                                        ?: simManga.attributes?.title?.values?.firstOrNull()
                                        ?: "Manga"
                                    val simCover = simManga.getCoverImageUrl()
                                    Card(
                                        modifier = Modifier
                                            .width(115.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onNavigateToManga(simManga.id)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                                        border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f))
                                    ) {
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                                                if (simCover != null) {
                                                    AsyncImage(
                                                        model = simCover,
                                                        contentDescription = simTitle,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(modifier = Modifier.fillMaxSize().background(ThemeSurfaceVariant))
                                                }
                                            }
                                            Text(
                                                text = simTitle,
                                                modifier = Modifier.padding(6.dp),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Controls, Language Tabs & Search Bar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AVAILABLE CHAPTERS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = ThemePrimary
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (chapters.isNotEmpty()) {
                                    if (manga != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ThemePrimary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .clickable { viewModel.downloadAllChapters(manga, chapters) }
                                                .testTag("download_all_btn")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Download,
                                                    contentDescription = "Download All",
                                                    tint = ThemePrimary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Download All",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = ThemePrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${displayedChapters.size}/${chapters.size} ch",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeOnSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ThemeSurfaceVariant,
                                        modifier = Modifier.clickable { viewModel.toggleChapterSort() }
                                    ) {
                                        Text(
                                            text = if (sortAscending) "↑ 1 to N" else "↓ N to 1",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ThemePrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Language selector chips if multiple languages or scanlations are available
                        if (availableLanguages.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = selectedChapterLanguage == "all"
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isAllSelected) ThemePrimary else ThemeSurfaceVariant,
                                        border = if (isAllSelected) null else BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { viewModel.setChapterLanguage("all") }
                                    ) {
                                        Text(
                                            text = "🌐 All (${allRawChapters.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isAllSelected) Color.Black else Color.White
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                items(availableLanguages) { langCode ->
                                    val isSelected = selectedChapterLanguage.equals(langCode, ignoreCase = true)
                                    val count = allRawChapters.count { it.attributes?.translatedLanguage.equals(langCode, ignoreCase = true) }
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) ThemePrimary else ThemeSurfaceVariant,
                                        border = if (isSelected) null else BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable { viewModel.setChapterLanguage(langCode) }
                                    ) {
                                        Text(
                                            text = "${getLanguageDisplayName(langCode)} ($count)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else Color.White
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (chapters.size > 8 || allRawChapters.size > 8) {
                            OutlinedTextField(
                                value = chapterSearchQuery,
                                onValueChange = { chapterSearchQuery = it },
                                placeholder = { Text("Filter chapter number or title...", color = ThemeOnSurfaceVariant, fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePrimary,
                                    unfocusedBorderColor = ThemeSurfaceVariant,
                                    focusedContainerColor = ThemeSurface,
                                    unfocusedContainerColor = ThemeSurface,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )
                        }
                    }
                }

                if (isLoading && chapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ThemePrimary)
                        }
                    }
                } else if (displayedChapters.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    tint = ThemePrimary,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (chapterSearchQuery.isNotBlank()) {
                                        "No chapters matching '$chapterSearchQuery'"
                                    } else {
                                        chaptersError ?: "No chapters uploaded for this language"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (availableLanguages.isNotEmpty() && selectedChapterLanguage != "all") {
                                        "Try switching to 'All' or another language tab above to view other translated releases."
                                    } else {
                                        "Chapters for this title may be licensed officially or not yet added to scanlation databases."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.fetchChapters(mangaId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Retry",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh Chapters", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(displayedChapters) { chapter ->
                        val rawNum = chapter.attributes?.chapter?.trim()
                        val title = chapter.attributes?.title?.trim() ?: ""
                        val volume = chapter.attributes?.volume?.trim()
                        val lang = chapter.attributes?.translatedLanguage?.uppercase() ?: "EN"
                        val pages = chapter.attributes?.pages ?: 0
                        val externalUrl = chapter.attributes?.externalUrl

                        val displayHeading = when {
                            !rawNum.isNullOrBlank() -> "Chapter $rawNum"
                            title.isNotBlank() -> title
                            !volume.isNullOrBlank() -> "Volume $volume"
                            else -> "Oneshot / Release"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!externalUrl.isNullOrBlank()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(externalUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open external link", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.recordReadingProgress(
                                            mangaId = mangaId,
                                            title = mangaTitle,
                                            coverUrl = coverUrl,
                                            chapterId = chapter.id,
                                            chapterNumber = rawNum,
                                            chapterTitle = if (title.isNotBlank()) title else displayHeading,
                                            currentPage = 1,
                                            totalPages = if (pages > 0) pages else 1
                                        )
                                        onNavigateToReader(chapter.id)
                                    }
                                }
                                .testTag("chapter_item_${chapter.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = displayHeading,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (downloadedChapterIds.contains(chapter.id)) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Downloaded Offline",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (title.isNotBlank() && displayHeading != title) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThemeOnSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ThemeSecondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = lang,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ThemeSecondary,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        if (pages > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ThemeSurfaceVariant
                                            ) {
                                                Text(
                                                    text = "$pages pages",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = ThemeOnSurfaceVariant,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (!externalUrl.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFF9800).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "🌐 Official / External",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFFFB74D),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        val releaseDate = chapter.getFormattedReleaseDate()
                                        if (releaseDate.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF00897B).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "📅 $releaseDate",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF80CBC4),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (downloadedChapterIds.contains(chapter.id)) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Offline",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF4CAF50),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDownloading = downloadingChapterId == chapter.id || downloadProgress.containsKey(chapter.id)
                                    val isDownloaded = downloadedChapterIds.contains(chapter.id)

                                    if (isDownloading) {
                                        val prog = downloadProgress[chapter.id] ?: 0f
                                        Box(
                                            modifier = Modifier.size(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { prog.coerceIn(0.05f, 1f) },
                                                modifier = Modifier.size(24.dp),
                                                color = ThemePrimary,
                                                strokeWidth = 2.5.dp
                                            )
                                        }
                                    } else if (isDownloaded) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteOfflineChapter(chapter.id, mangaId)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.DeleteOutline,
                                                contentDescription = "Delete Download",
                                                tint = ThemeOnSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else if (pages > 0 && manga != null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.downloadChapter(manga, chapter)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("download_ch_${chapter.id}")
                                        ) {
                                            Icon(
                                                Icons.Outlined.Download,
                                                contentDescription = "Download Chapter",
                                                tint = ThemePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (!externalUrl.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFFF9800).copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Open Link",
                                                    tint = Color(0xFFFFB74D),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "OPEN",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFB74D),
                                                        fontSize = 10.sp
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = ThemePrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "READ",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = ThemePrimary,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAiAssistantSheet) {
                val tagList = manga?.attributes?.tags?.mapNotNull { it.attributes?.name?.get("en") } ?: emptyList()
                AiStoryAssistantSheet(
                    mangaTitle = mangaTitle,
                    chapterTitle = "Overview & Lore",
                    synopsis = cleanSynopsis,
                    tags = tagList,
                    viewModel = viewModel,
                    onDismiss = { showAiAssistantSheet = false }
                )
            }

            if (showShelvesDialog) {
                ShelvesManagerDialog(
                    mangaId = mangaId,
                    mangaTitle = mangaTitle,
                    viewModel = viewModel,
                    onDismiss = { showShelvesDialog = false }
                )
            }
        }
    }
}
