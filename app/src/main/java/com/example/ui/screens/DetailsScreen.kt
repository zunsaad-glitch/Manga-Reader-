package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Chapter
import com.example.model.Manga
import com.example.ui.components.SourceBadge
import com.example.ui.theme.*

@Composable
fun DetailsScreen(
    manga: Manga,
    chapters: List<Chapter>,
    isChaptersLoading: Boolean,
    isBookmarked: Boolean,
    isAscending: Boolean = true,
    onToggleOrder: () -> Unit = {},
    onBackClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSynopsisExpanded by remember { mutableStateOf(false) }

    val displayedChapters = remember(chapters, isAscending) {
        if (isAscending) {
            chapters.sortedBy { it.chapterNumber }
        } else {
            chapters.sortedByDescending { it.chapterNumber }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header Backdrop
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        DarkBackground.copy(alpha = 0.4f),
                                        DarkBackground.copy(alpha = 0.85f),
                                        DarkBackground
                                    )
                                )
                            )
                    )

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkBackground.copy(alpha = 0.75f), CircleShape)
                                .testTag("details_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .background(DarkBackground.copy(alpha = 0.75f), CircleShape)
                                .testTag("details_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) NeonAccent else Color.White
                            )
                        }
                    }

                    // Floating Poster & Main Info inside header
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Poster Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier.size(105.dp, 145.dp)
                        ) {
                            AsyncImage(
                                model = manga.coverUrl,
                                contentDescription = manga.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title & Metadata
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SourceBadge(source = manga.source)
                                if (manga.contentRating == "pornographic" || manga.contentRating == "erotica") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE53935)
                                    ) {
                                        Text(
                                            text = if (manga.contentRating == "pornographic") "18+ EXPLICIT" else "MATURE",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = manga.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Author: ${manga.author}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f".format(manga.rating),
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "•  ${manga.status}",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Clickable Genres Row (Tags feature)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    manga.genres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTagClick(genre) }
                                .testTag("details_tag_$genre")
                        ) {
                            Text(
                                text = genre,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Synopsis Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Synopsis",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = manga.synopsis,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = if (isSynopsisExpanded) 50 else 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        TextButton(
                            onClick = { isSynopsisExpanded = !isSynopsisExpanded },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (isSynopsisExpanded) "Show Less" else "Read More",
                                color = NeonAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Chapters Header with Sort Order Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isChaptersLoading) "Loading Chapters..." else "Chapters (${chapters.size})",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isChaptersLoading && chapters.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleOrder() }
                                .testTag("toggle_chapter_sequence_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Change Sequence Order",
                                    tint = NeonAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isAscending) "Ch. 1 → Last (Ascending)" else "Latest → Ch. 1 (Descending)",
                                    color = NeonAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isChaptersLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonAccent, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chapters available yet for this title.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Chapters List sorted by user's chosen sequence
            items(displayedChapters, key = { it.id }) { chapter ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChapterClick(chapter) }
                        .testTag("chapter_item_${chapter.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chapter.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val pageText = if (chapter.pages.isNotEmpty()) "${chapter.pages.size} pages • " else ""
                            Text(
                                text = "$pageText${chapter.releaseDate}",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Read chapter",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom CTA Button: Start Reading (starts at Chapter 1 / lowest chapter number)
        val firstChapter = chapters.minByOrNull { it.chapterNumber } ?: chapters.firstOrNull()
        if (firstChapter != null) {
            Surface(
                color = DarkSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { onChapterClick(firstChapter) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_reading_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Reading Ch. ${firstChapter.chapterNumber.toInt()}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
