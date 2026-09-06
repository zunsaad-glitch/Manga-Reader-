package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class ReaderThemeOption(
    val id: String,
    val name: String,
    val previewColor: Color,
    val icon: String
)

val ReaderThemeOptions = listOf(
    ReaderThemeOption("OLED", "OLED Pitch", Color(0xFF000000), "🌑"),
    ReaderThemeOption("DARK", "Deep Slate", Color(0xFF141419), "🌚"),
    ReaderThemeOption("SEPIA", "Warm Sepia", Color(0xFF261D17), "📜"),
    ReaderThemeOption("EINK", "E-Ink Matte", Color(0xFF1E1E1E), "📰"),
    ReaderThemeOption("ROSE", "Warm Rosé", Color(0xFF24151B), "🌸")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    viewModel: MainViewModel,
    isDualPageMode: Boolean,
    onToggleDualPage: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTheme by viewModel.readerTheme.collectAsState()
    val currentDim by viewModel.readerBrightnessDim.collectAsState()
    val pageHaptics by viewModel.pageTurnHaptics.collectAsState()
    val pageSound by viewModel.pageTurnSound.collectAsState()

    var dimSliderValue by remember(currentDim) { mutableStateOf(currentDim) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ThemeSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("reader_settings_sheet")
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
                    Icon(Icons.Default.Tune, contentDescription = null, tint = ThemePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Reader Display & Comfort",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Reading Canvas Themes
            Text(
                "Canvas Tint & Atmosphere",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReaderThemeOptions.forEach { opt ->
                    val isSelected = currentTheme == opt.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = opt.previewColor,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) ThemePrimary else ThemeOutline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setReaderTheme(opt.id) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(opt.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                opt.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ThemePrimary else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Night Reading Dimmer Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Night Comfort Dimmer",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    "${(dimSliderValue * 100).toInt()}% Dim",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ThemePrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Slider(
                value = dimSliderValue,
                onValueChange = {
                    dimSliderValue = it
                    viewModel.setReaderBrightnessDim(it)
                },
                valueRange = 0f..0.7f,
                colors = SliderDefaults.colors(
                    thumbColor = ThemePrimary,
                    activeTrackColor = ThemePrimary,
                    inactiveTrackColor = ThemeSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("reader_dim_slider")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Page Turn Haptics & Sound FX Toggles
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ThemeSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Haptics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Page-Turn Haptics", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tactile page turning pulse feedback", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = pageHaptics,
                            onCheckedChange = { viewModel.setPageTurnHaptics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ThemePrimary)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ThemeOutline.copy(alpha = 0.2f))

                    // Sound FX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Paper Rustle Sound FX", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Subtle physical page flutter audio", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = pageSound,
                            onCheckedChange = { viewModel.setPageTurnSound(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ThemePrimary)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ThemeOutline.copy(alpha = 0.2f))

                    // Dual Page Spread
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = ThemePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Dual-Page Spread (Tablet)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Authentic 2-page open book layout", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = isDualPageMode,
                            onCheckedChange = { onToggleDualPage(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ThemePrimary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
