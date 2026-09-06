package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*

enum class CardFrameStyle(val displayName: String, val gradient: Brush, val textColor: Color, val badgeBg: Color) {
    GOLDEN("✨ Golden Foil", Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500), Color(0xFFFF8C00))), Color(0xFFFFE082), Color(0xFF4A3400)),
    CYBERPUNK("🌌 Cyber Neon", Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF7B2CBF), Color(0xFFF72585))), Color(0xFF80FFEA), Color(0xFF2A0845)),
    MINIMAL_OBSIDIAN("🖤 Minimal Noir", Brush.linearGradient(listOf(Color(0xFF2B2B2B), Color(0xFF141414))), Color(0xFFE0E0E0), Color(0xFF1E1E1E)),
    VINTAGE_SEPIA("☕ Vintage Sepia", Brush.linearGradient(listOf(Color(0xFFD4A373), Color(0xFFCCD5AE))), Color(0xFFFAEDCD), Color(0xFF3B2F2F))
}

@Composable
fun QuoteCardGeneratorDialog(
    mangaTitle: String,
    chapterTitle: String,
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var customQuote by remember { mutableStateOf("“This chapter changed everything.”") }
    var selectedStyle by remember { mutableStateOf(CardFrameStyle.GOLDEN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CropOriginal, contentDescription = null, tint = ThemePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Panel & Quote Card Creator", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card Preview Canvas
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedStyle.gradient)
                        .padding(3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF101014))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Image Thumbnail
                        if (imageUrl != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Card Artwork",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quote Text
                        Text(
                            text = customQuote,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = selectedStyle.textColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Series & Chapter Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = selectedStyle.badgeBg,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "📖 $mangaTitle • $chapterTitle",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Caption Input
                OutlinedTextField(
                    value = customQuote,
                    onValueChange = { customQuote = it },
                    label = { Text("Custom Quote / Thought", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePrimary,
                        unfocusedBorderColor = ThemeOutline.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("custom_quote_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Style Selector
                Text("Select Aesthetic Frame:", color = ThemeOnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CardFrameStyle.values().forEach { style ->
                        val isSelected = selectedStyle == style
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ThemePrimary else ThemeSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedStyle = style }
                        ) {
                            Text(
                                style.displayName.split(" ").first(),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "$customQuote\n\n📖 $mangaTitle - $chapterTitle\nRead on Manga Reader App ✨"
                        )
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Manga Quote Card")
                    context.startActivity(shareIntent)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Card", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ThemeOnSurfaceVariant)
            }
        },
        containerColor = ThemeSurface
    )
}
