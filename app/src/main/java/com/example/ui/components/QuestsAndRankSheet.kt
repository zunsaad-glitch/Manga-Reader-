package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class RankTier(
    val level: Int,
    val title: String,
    val icon: String,
    val minExp: Int,
    val maxExp: Int,
    val auraColor: Color
)

val RANK_TIERS = listOf(
    RankTier(1, "Manga Novice", "🥉", 0, 200, Color(0xFFCD7F32)),
    RankTier(2, "Apprentice Reader", "🥈", 200, 500, Color(0xFFC0C0C0)),
    RankTier(3, "Panel Scholar", "🥇", 500, 1000, Color(0xFFFFD700)),
    RankTier(4, "Lore Seeker", "🔮", 1000, 2200, Color(0xFF9C27B0)),
    RankTier(5, "Binge Master", "⚡", 2200, 4500, Color(0xFF00E5FF)),
    RankTier(6, "Grand Manga Sage", "👑", 4500, 10000, Color(0xFFFF3366))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsAndRankSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalExp by viewModel.readerExp.collectAsState()
    val streakDays by viewModel.readingStreakDays.collectAsState()
    val chaptersRead by viewModel.totalChaptersRead.collectAsState()
    val minutesSpent by viewModel.totalReadingMinutes.collectAsState()

    val currentTier = remember(totalExp) {
        RANK_TIERS.findLast { totalExp >= it.minExp } ?: RANK_TIERS.first()
    }
    val nextTier = remember(currentTier) {
        RANK_TIERS.getOrNull(currentTier.level) ?: currentTier
    }

    val progressFraction = remember(totalExp, currentTier, nextTier) {
        if (currentTier.level == nextTier.level) 1.0f
        else ((totalExp - currentTier.minExp).toFloat() / (nextTier.minExp - currentTier.minExp)).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    // Quests definition
    val quests = remember(chaptersRead, streakDays, totalExp) {
        listOf(
            QuestItem(
                id = "daily_read",
                title = "Daily Chapter Sprint",
                description = "Read at least 1 chapter today",
                rewardExp = 60,
                current = (chaptersRead % 5).coerceAtLeast(1),
                target = 1,
                icon = "⚡"
            ),
            QuestItem(
                id = "streak_master",
                title = "Consistency Streak",
                description = "Maintain a 3+ day reading streak",
                rewardExp = 150,
                current = streakDays,
                target = 3,
                icon = "🔥"
            ),
            QuestItem(
                id = "panel_collector",
                title = "Moment Archivist",
                description = "Save 2 memorable panels in your Journal",
                rewardExp = 100,
                current = 2,
                target = 2,
                icon = "📑"
            ),
            QuestItem(
                id = "lore_scholar",
                title = "AI Lore Seeker",
                description = "Consult the AI Lore Guide on any series",
                rewardExp = 80,
                current = 1,
                target = 1,
                icon = "🔮"
            ),
            QuestItem(
                id = "sound_voyager",
                title = "Atmosphere Aficionado",
                description = "Read with ambient soundscapes or Audio Drama",
                rewardExp = 120,
                current = 1,
                target = 1,
                icon = "🎧"
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13111C),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("quests_rank_sheet")
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
                    Text(currentTier.icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Level ${currentTier.level}: ${currentTier.title}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "$totalExp Total EXP",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Level Progress Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
                border = BorderStroke(1.dp, currentTier.auraColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Next Rank: ${nextTier.title} ${nextTier.icon}",
                            fontSize = 12.sp,
                            color = ThemeOnSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${(nextTier.minExp - totalExp).coerceAtLeast(0)} EXP left",
                            fontSize = 11.sp,
                            color = currentTier.auraColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = currentTier.auraColor,
                        trackColor = Color(0xFF2B263F)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Reading Quick Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatPill("📖 $chaptersRead Chapters")
                        StatPill("⏱️ ${minutesSpent / 60}h ${minutesSpent % 60}m")
                        StatPill("🔥 $streakDays Days")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quests Section Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Active Quests & Challenges",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quests List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quests, key = { it.id }) { quest ->
                    var isClaimed by remember { mutableStateOf(false) }
                    QuestCard(
                        quest = quest,
                        isClaimed = isClaimed,
                        onClaim = {
                            isClaimed = true
                            viewModel.addReaderExp(quest.rewardExp)
                            Toast.makeText(context, "+${quest.rewardExp} EXP Claimed! 🎉", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF2B263F),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

data class QuestItem(
    val id: String,
    val title: String,
    val description: String,
    val rewardExp: Int,
    val current: Int,
    val target: Int,
    val icon: String
)

@Composable
private fun QuestCard(
    quest: QuestItem,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    val isCompleted = quest.current >= quest.target

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
        border = BorderStroke(1.dp, if (isCompleted && !isClaimed) Color(0xFFFFD54F).copy(alpha = 0.6f) else Color(0xFF2B263F)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(quest.icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quest.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                Text(quest.description, color = ThemeOnSurfaceVariant, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("+${quest.rewardExp} EXP", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            if (isClaimed) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50))
                ) {
                    Text(
                        "Claimed ✓",
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else if (isCompleted) {
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Claim", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else {
                Text("${quest.current}/${quest.target}", color = ThemeOnSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}
