package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaTranslatorSheet(
    mangaTitle: String,
    pageNumber: Int,
    pageImageUrl: String?,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var sourceLanguage by remember { mutableStateOf("Japanese / RAW") }
    var targetLanguage by remember { mutableStateOf("English") }
    var customDialogueText by remember { mutableStateOf("") }

    val isLoading by viewModel.translatorIsLoading.collectAsState()
    val translationResult by viewModel.translatorResult.collectAsState()

    LaunchedEffect(Unit) {
        if (translationResult == null) {
            viewModel.translatePageContent(
                mangaTitle = mangaTitle,
                pageNumber = pageNumber,
                pageImageUrl = pageImageUrl,
                sourceLang = sourceLanguage,
                targetLang = targetLanguage,
                rawText = customDialogueText
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearTranslatorResult()
            onDismiss()
        },
        containerColor = ThemeSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.testTag("manga_translator_sheet")
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
                                    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "AI Manga & SFX Translator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            "$mangaTitle • Page $pageNumber",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ThemeOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.clearTranslatorResult()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Language Selector Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ThemeSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Source: $sourceLanguage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ThemePrimary)
                    Text("Target: $targetLanguage", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 340.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ThemeBackground.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00C9FF),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Gemini is analyzing dialogue bubbles, SFX & nuances...",
                            color = ThemeOnSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else if (translationResult != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = translationResult ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tap 'Translate Page' to start.", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Optional raw text input field for manual bubble transcription
            OutlinedTextField(
                value = customDialogueText,
                onValueChange = { customDialogueText = it },
                placeholder = { Text("Optional: Paste specific bubble text or SFX (e.g., ドキドキ / 쾅)", fontSize = 11.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C9FF),
                    unfocusedBorderColor = ThemeOutline.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().testTag("translator_custom_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        viewModel.translatePageContent(
                            mangaTitle = mangaTitle,
                            pageNumber = pageNumber,
                            pageImageUrl = pageImageUrl,
                            sourceLang = sourceLanguage,
                            targetLang = targetLanguage,
                            rawText = customDialogueText
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9FF))
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Translate / Re-scan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (!translationResult.isNullOrBlank() && !isLoading) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Translation", translationResult)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Translation copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
