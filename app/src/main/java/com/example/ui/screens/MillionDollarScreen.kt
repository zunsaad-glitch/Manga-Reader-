package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val GoldGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFA000),
        Color(0xFFFF8F00),
        Color(0xFFFFE082)
    )
)

private val DiamondGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00E5FF),
        Color(0xFF2979FF),
        Color(0xFF651FFF)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MillionDollarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val netWorth by viewModel.mangaNetWorth.collectAsState()
    val vipTier by viewModel.vipTier.collectAsState()
    val dailySpinAvailable by viewModel.dailySpinAvailable.collectAsState()
    val streakDays by viewModel.vipStreakDays.collectAsState()
    val scope = rememberCoroutineScope()

    var isSpinning by remember { mutableStateOf(false) }
    var wonPrize by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var showPrizeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "gold_glow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val formattedNetWorth = remember(netWorth) {
        NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }.format(netWorth)
    }

    if (showPrizeDialog && wonPrize != null) {
        AlertDialog(
            onDismissRequest = { showPrizeDialog = false },
            title = {
                Text(
                    "🎉 VIP PRIZE WON!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    ),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        wonPrize!!.first,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnSurface
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Your new manga reading net worth has been updated!",
                        style = MaterialTheme.typography.bodySmall.copy(color = ThemeOnSurfaceVariant),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrizeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLAIM REWARD", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ThemeSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👑", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$1,000,000 VIP Club",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("million_dollar_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ThemeOnSurface)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("🔥 $streakDays Day Streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeSurface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Worth Showcase Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("million_dollar_networth_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.7f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Background gold radial ambient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Diamond,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "MANGA READING NET WORTH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        color = Color(0xFFFFD700)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = formattedNetWorth,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                                ) {
                                    Text(
                                        text = "VIP Tier: $vipTier",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00E5FF)
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = ThemePrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Top 0.01% Reader",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ThemePrimary
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Million Dollar Golden Spin Wheel / Gacha
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                    border = BorderStroke(1.dp, ThemeSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "🎰 Million Dollar Daily Gacha",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeOnSurface
                                    )
                                )
                                Text(
                                    "Spin the golden wheel for massive reading jackpot drops",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ThemeOnSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(GoldGradient)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(ThemeSurface)
                                    .rotate(if (isSpinning) rotation else 0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (dailySpinAvailable && !isSpinning) {
                                    isSpinning = true
                                    scope.launch {
                                        delay(1500)
                                        isSpinning = false
                                        val prize = viewModel.spinMillionDollarWheel()
                                        wonPrize = prize
                                        showPrizeDialog = true
                                    }
                                }
                            },
                            enabled = dailySpinAvailable && !isSpinning,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                disabledContainerColor = ThemeSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("million_dollar_spin_btn")
                        ) {
                            if (isSpinning) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spinning Wheel...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else if (dailySpinAvailable) {
                                Text("🎰 SPIN $1,000,000 WHEEL FREE", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Text("✅ SPUN FOR TODAY (Refreshes Daily)", color = ThemeOnSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Million Dollar Perks List
            item {
                Text(
                    "VIP PRIVILEGES & PERKS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemePrimary,
                        letterSpacing = 1.2.sp
                    )
                )
            }

            item {
                VipPerkItem(
                    icon = Icons.Outlined.EmojiEvents,
                    title = "Billionaire Speed Caching",
                    description = "Instantaneous prefetching for all high-resolution manga pages",
                    isActive = true
                )
            }

            item {
                VipPerkItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Manga Oracle Copilot",
                    description = "Gemini AI personalized recommendations and chapter analysis",
                    isActive = true
                )
            }

            item {
                VipPerkItem(
                    icon = Icons.Default.Security,
                    title = "Private 18+ Uncensored Vault",
                    description = "Biometric secured access to all special interest & doujinshi titles",
                    isActive = true
                )
            }

            item {
                VipPerkItem(
                    icon = Icons.Default.AllInclusive,
                    title = "Unlimited Offline Library",
                    description = "Save unlimited chapters directly to local persistent storage",
                    isActive = true
                )
            }
        }
    }
}

@Composable
private fun VipPerkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeSurface),
        border = BorderStroke(1.dp, ThemeSurfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFD700).copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ThemeOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            if (isActive) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                ) {
                    Text(
                        "ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
