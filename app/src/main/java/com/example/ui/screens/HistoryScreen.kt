package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Search
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
import com.example.data.db.ReadingHistoryEntity
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    viewModel: MainViewModel,
    onNavigateToMangaDetail: (String) -> Unit,
    onNavigateToReader: (String) -> Unit
) {
    val historyList by viewModel.readingHistory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredList = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            val q = searchQuery.trim().lowercase()
            historyList.filter {
                it.title.lowercase().contains(q) ||
                        (it.lastChapterTitle?.lowercase()?.contains(q) == true) ||
                        (it.lastChapterNumber?.lowercase()?.contains(q) == true)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Reading History?", color = ThemeOnSurface) },
            text = { Text("This will remove all reading progress and history records.", color = ThemeOnSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history_btn")
                ) {
                    Text("Clear All", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = ThemeOnSurfaceVariant)
                }
            },
            containerColor = ThemeSurface
        )
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = ThemePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reading History",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnSurface
                            )
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_all_history_btn")
                        ) {
                            Icon(
                                Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = ThemeOnSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground)
        ) {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = ThemePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.AutoStories,
                                    contentDescription = null,
                                    tint = ThemePrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No Reading History Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Start reading any manga to automatically track your progress and resume anytime.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ThemeOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.currentTab.value = AppTab.HOME },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                        ) {
                            Text("Discover Manga", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Search Bar
                    if (historyList.size > 3) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search history...", color = ThemeOnSurfaceVariant, fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, contentDescription = null, tint = ThemeOnSurfaceVariant)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePrimary,
                                    unfocusedBorderColor = ThemeSurfaceVariant,
                                    focusedContainerColor = ThemeSurface,
                                    unfocusedContainerColor = ThemeSurface,
                                    focusedTextColor = ThemeOnSurface,
                                    unfocusedTextColor = ThemeOnSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            )
                        }
                    }

                    // Continue Reading Hero Card (Most Recent)
                    val mostRecent = historyList.firstOrNull()
                    if (mostRecent != null && searchQuery.isBlank()) {
                        item {
                            HistoryHeroCard(
                                history = mostRecent,
                                onResume = {
                                    if (!mostRecent.lastChapterId.isNullOrBlank()) {
                                        onNavigateToReader(mostRecent.lastChapterId)
                                    } else {
                                        onNavigateToMangaDetail(mostRecent.mangaId)
                                    }
                                },
                                onOpenDetail = { onNavigateToMangaDetail(mostRecent.mangaId) }
                            )
                        }

                        item {
                            Text(
                                text = "RECENTLY READ (${filteredList.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ThemePrimary,
                                    letterSpacing = 1.2.sp
                                ),
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                    }

                    // History Items
                    items(filteredList, key = { it.mangaId }) { history ->
                        HistoryItemCard(
                            history = history,
                            onResume = {
                                if (!history.lastChapterId.isNullOrBlank()) {
                                    onNavigateToReader(history.lastChapterId)
                                } else {
                                    onNavigateToMangaDetail(history.mangaId)
                                }
                            },
                            onOpenDetail = { onNavigateToMangaDetail(history.mangaId) },
                            onDelete = { viewModel.deleteHistoryItem(history.mangaId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryHeroCard(
    history: ReadingHistoryEntity,
    onResume: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResume() }
            .testTag("history_hero_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Gradient accent background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ThemePrimary.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (history.coverUrl != null) {
                    AsyncImage(
                        model = history.coverUrl,
                        contentDescription = history.title,
                        modifier = Modifier
                            .width(80.dp)
                            .height(115.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenDetail() },
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ThemePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "CONTINUE READING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemePrimary,
                                fontSize = 9.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val chapterLabel = when {
                        !history.lastChapterNumber.isNullOrBlank() -> "Chapter ${history.lastChapterNumber}"
                        !history.lastChapterTitle.isNullOrBlank() -> history.lastChapterTitle
                        else -> "Latest Read"
                    }

                    Text(
                        text = chapterLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ThemeOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    history: ReadingHistoryEntity,
    onResume: () -> Unit,
    onOpenDetail: () -> Unit,
    onDelete: () -> Unit
) {
    val relativeTime = remember(history.lastReadTimestamp) {
        DateUtils.getRelativeTimeSpanString(
            history.lastReadTimestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResume() }
            .testTag("history_item_${history.mangaId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSurfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (history.coverUrl != null) {
                AsyncImage(
                    model = history.coverUrl,
                    contentDescription = history.title,
                    modifier = Modifier
                        .width(60.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenDetail() },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(84.dp)
                        .background(ThemeSurfaceVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = ThemeOnSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = history.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val chapterText = when {
                    !history.lastChapterNumber.isNullOrBlank() -> "Chapter ${history.lastChapterNumber}"
                    !history.lastChapterTitle.isNullOrBlank() -> history.lastChapterTitle
                    else -> "Chapter Read"
                }

                Text(
                    text = chapterText,
                    style = MaterialTheme.typography.bodySmall.copy(color = ThemePrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ThemeOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_history_${history.mangaId}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = ThemeOnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
