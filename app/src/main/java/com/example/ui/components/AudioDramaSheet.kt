package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDramaSheet(
    mangaTitle: String,
    chapterTitle: String,
    synopsis: String?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val isLoading by viewModel.audioDramaIsLoading.collectAsState()
    val isPlaying by viewModel.audioDramaManager.isPlaying.collectAsState()
    val spokenScript by viewModel.audioDramaScript.collectAsState()

    var speechRate by remember { mutableStateOf(1.0f) }

    LaunchedEffect(Unit) {
        if (spokenScript == null && !isLoading) {
            viewModel.generateAndPlayAudioDrama(
                mangaTitle = mangaTitle,
                chapterTitle = chapterTitle,
                synopsis = synopsis,
                speechRate = speechRate
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13111C),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("audio_drama_sheet")
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF007F), Color(0xFF7928CA))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎙️", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "AI Audio Drama & Narration",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "$mangaTitle • $chapterTitle",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeOnSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Narration Script Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1B2E))
                    .border(BorderStroke(1.dp, Color(0xFF3B2D54)), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF007F),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Directing voice actors & writing cinematic script...",
                            color = ThemeOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else if (spokenScript != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = spokenScript ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tap 'Narrate Chapter' to begin.", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speech Speed Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Speed:", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.85f to "0.8x", 1.0f to "1.0x", 1.25f to "1.2x", 1.5f to "1.5x").forEach { (speed, label) ->
                        val isSelected = speechRate == speed
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF007F) else Color(0xFF2B263F),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { speechRate = speed }
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ThemeOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Re-generate button
                OutlinedButton(
                    onClick = {
                        viewModel.generateAndPlayAudioDrama(
                            mangaTitle = mangaTitle,
                            chapterTitle = chapterTitle,
                            synopsis = synopsis,
                            speechRate = speechRate
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF7928CA))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Script", color = Color.White, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Play / Stop Master Button
                Button(
                    onClick = {
                        if (isPlaying) {
                            viewModel.stopAudioDrama()
                        } else {
                            if (spokenScript != null) {
                                viewModel.audioDramaManager.speak(spokenScript ?: "", speechRate = speechRate)
                            } else {
                                viewModel.generateAndPlayAudioDrama(
                                    mangaTitle = mangaTitle,
                                    chapterTitle = chapterTitle,
                                    synopsis = synopsis,
                                    speechRate = speechRate
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFE53935) else Color(0xFFFF007F)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("audio_drama_toggle_btn")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isPlaying) "Stop Narration" else "Play Audio Drama",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
