package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SettingsRepository
import com.example.ui.components.ReadingInsightsSheet
import com.example.util.MangaNotificationManager
import com.example.viewmodel.MainViewModel
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val readerMode by viewModel.settingsRepository.readerMode.collectAsState(initial = "VERTICAL")
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val incognitoMode by viewModel.incognitoMode.collectAsState()
    val subscribedIds by viewModel.subscribedIds.collectAsState()
    val streakDays by viewModel.readingStreakDays.collectAsState()
    val chaptersRead by viewModel.totalChaptersRead.collectAsState()
    val totalMinutes by viewModel.totalReadingMinutes.collectAsState()
    val actionRumbleEnabled by viewModel.actionRumbleEnabled.collectAsState()
    val readerExp by viewModel.readerExp.collectAsState()

    var isClearingCache by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 1. READING INSIGHTS & STATS HERO CARD ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showInsightsSheet = true }
                    .testTag("settings_insights_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "READING INSIGHTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "View Stats →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 $streakDays Days", style = MaterialTheme.typography.titleMedium)
                            Text("Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚 $chaptersRead Ch.", style = MaterialTheme.typography.titleMedium)
                            Text("Read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔔 ${subscribedIds.size}", style = MaterialTheme.typography.titleMedium)
                            Text("Subscribed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // === 2. NOTIFICATIONS & ALERTS ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_notifications_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "NOTIFICATIONS & ALERTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsRowItem(
                        icon = Icons.Filled.Notifications,
                        title = "New Manga Drops & Releases",
                        subtitle = "Receive instant push alerts when new chapters or titles drop",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                modifier = Modifier.testTag("notifications_master_switch")
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

                    SettingsRowItem(
                        icon = Icons.Filled.BookmarkAdded,
                        title = "Subscribed Releases (${subscribedIds.size})",
                        subtitle = "Alerts triggered exclusively for bookmarked favorites",
                        trailing = {
                            Text(
                                if (subscribedIds.isEmpty()) "None" else "${subscribedIds.size} active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

                    SettingsRowItem(
                        icon = Icons.Filled.Send,
                        title = "Send Test Drop Alert",
                        subtitle = "Verify push notification channels on this device",
                        onClick = {
                            scope.launch {
                                MangaNotificationManager.showMangaDropNotification(
                                    context = context,
                                    mangaId = "test_drop",
                                    mangaTitle = "Solo Leveling: Ragnarok",
                                    chapterTitle = "Ch. 182",
                                    coverUrl = null,
                                    isSubscribed = false
                                )
                                Toast.makeText(context, "Test notification dispatched!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // === 3. PRIVACY & INCOGNITO ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_privacy_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PRIVACY & INCOGNITO",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsRowItem(
                        icon = Icons.Filled.VisibilityOff,
                        title = "Incognito Mode",
                        subtitle = "Pause reading history and streak tracking while enabled",
                        trailing = {
                            Switch(
                                checked = incognitoMode,
                                onCheckedChange = { viewModel.setIncognitoMode(it) },
                                modifier = Modifier.testTag("incognito_switch")
                            )
                        }
                    )
                }
            }

            // === 4. READER PREFERENCES ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_reader_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "READER PREFERENCES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Reading Mode", style = MaterialTheme.typography.titleSmall)
                    Text("Select your preferred layout for chapters and galleries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = readerMode == "VERTICAL",
                            onClick = {
                                scope.launch { viewModel.settingsRepository.setReaderMode("VERTICAL") }
                            },
                            label = { Text("Vertical Webtoon") },
                            leadingIcon = { Icon(Icons.Filled.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f).testTag("reader_mode_vertical")
                        )
                        FilterChip(
                            selected = readerMode == "HORIZONTAL",
                            onClick = {
                                scope.launch { viewModel.settingsRepository.setReaderMode("HORIZONTAL") }
                            },
                            label = { Text("Paged LTR") },
                            leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f).testTag("reader_mode_horizontal")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsRowItem(
                        icon = Icons.Filled.Vibration,
                        title = "Action Rumble & Haptic Feedback",
                        subtitle = "Tactile feedback pulses during page transitions and action scenes",
                        trailing = {
                            Switch(
                                checked = actionRumbleEnabled,
                                onCheckedChange = { viewModel.setActionRumbleEnabled(it) },
                                modifier = Modifier.testTag("action_rumble_switch")
                            )
                        }
                    )
                }
            }

            // === 5. STORAGE & CACHE ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_storage_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "STORAGE & CACHE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsRowItem(
                        icon = Icons.Filled.DeleteSweep,
                        title = "Clear Image Cache",
                        subtitle = "Purges temporary page image caches to free memory",
                        onClick = {
                            isClearingCache = true
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    coil.Coil.imageLoader(context).diskCache?.clear()
                                    coil.Coil.imageLoader(context).memoryCache?.clear()
                                    File(context.cacheDir, "image_cache").deleteRecursively()
                                    File(context.cacheDir, "network_image_cache").deleteRecursively()
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "Image cache cleared successfully", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "Cache cleanup completed", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isClearingCache = false
                                    }
                                }
                            }
                        },
                        trailing = {
                            if (isClearingCache) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }

            // === 6. APP INFO & SOURCES ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("settings_info_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "APP INFORMATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsRowItem(
                        icon = Icons.Filled.Hub,
                        title = "Content Sources",
                        subtitle = "MangaDex API • nHentai API • JandaPress Mirror"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))

                    SettingsRowItem(
                        icon = Icons.Filled.Layers,
                        title = "Version",
                        subtitle = "2.5.0 (Build 2026.08)"
                    )
                }
            }
        }
    }

    if (showInsightsSheet) {
        ReadingInsightsSheet(
            streakDays = streakDays,
            totalChapters = chaptersRead,
            totalMinutes = totalMinutes,
            incognitoMode = incognitoMode,
            onToggleIncognito = { viewModel.setIncognitoMode(it) },
            onDismiss = { showInsightsSheet = false }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            trailing()
        }
    }
}

