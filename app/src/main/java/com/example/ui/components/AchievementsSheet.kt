package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AchievementBadge
import com.example.util.AchievementManager
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val totalChapters by viewModel.totalChaptersRead.collectAsState()
    val streakDays by viewModel.readingStreakDays.collectAsState()
    val totalMinutes by viewModel.totalReadingMinutes.collectAsState()
    val downloadedMangaIds by viewModel.downloadedMangaIds.collectAsState()
    val surpriseCount by viewModel.surpriseRollsCount.collectAsState()
    val shelvesJson by viewModel.customShelvesJson.collectAsState()

    val shelvesCount = remember(shelvesJson) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
            (com.google.gson.Gson().fromJson<Map<String, List<String>>>(shelvesJson, type) ?: emptyMap()).size
        } catch (e: Exception) {
            0
        }
    }

    val badges = remember(totalChapters, streakDays, totalMinutes, downloadedMangaIds.size, surpriseCount, shelvesCount) {
        AchievementManager.computeBadges(
            totalChaptersRead = totalChapters,
            readingStreakDays = streakDays,
            totalReadingMinutes = totalMinutes,
            offlineCount = downloadedMangaIds.size,
            surpriseRollsCount = surpriseCount,
            shelvesCount = shelvesCount
        )
    }

    val unlockedCount = badges.count { it.isUnlocked }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ThemeSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("achievements_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏆", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Reading Milestones & Badges",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "$unlockedCount of ${badges.size} Badges Unlocked",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemePrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ThemeSurfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖 $totalChapters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Chapters Read", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(ThemeOutline.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥 $streakDays Days", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Daily Streak", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                    }
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(ThemeOutline.copy(alpha = 0.3f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏳ ${totalMinutes}m", color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Time Immersed", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(badges) { badge ->
                    val animatedProgress by animateFloatAsState(targetValue = badge.progress, label = "badgeProgress")

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (badge.isUnlocked) Color(0xFF1E1E28) else ThemeSurfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(
                            1.dp,
                            if (badge.isUnlocked) ThemePrimary.copy(alpha = 0.6f) else ThemeOutline.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (badge.isUnlocked) ThemePrimary.copy(alpha = 0.2f) else ThemeSurfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    badge.icon,
                                    fontSize = 22.sp,
                                    modifier = Modifier.then(
                                        if (!badge.isUnlocked) Modifier else Modifier
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked) Color.White else ThemeOnSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )

                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ThemeOnSurfaceVariant,
                                    fontSize = 10.sp
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.height(28.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (badge.isUnlocked) ThemePrimary else Color(0xFFFF9800),
                                trackColor = ThemeSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = badge.progressText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (badge.isUnlocked) ThemePrimary else ThemeOnSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
