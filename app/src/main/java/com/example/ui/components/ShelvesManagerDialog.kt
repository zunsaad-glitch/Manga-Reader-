package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun ShelvesManagerDialog(
    mangaId: String,
    mangaTitle: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val shelvesJson by viewModel.customShelvesJson.collectAsState()
    var newShelfName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    val shelvesMap = remember(shelvesJson) {
        try {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
            com.google.gson.Gson().fromJson<Map<String, List<String>>>(shelvesJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = ThemePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Add to Shelf", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(mangaTitle, color = ThemeOnSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (shelvesMap.isEmpty()) {
                    Text(
                        "No custom shelves created yet. Create one below to organize your library!",
                        color = ThemeOnSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shelvesMap.keys.toList()) { shelfName ->
                            val mangaList = shelvesMap[shelfName] ?: emptyList()
                            val isInShelf = mangaList.contains(mangaId)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isInShelf) ThemePrimary.copy(alpha = 0.2f) else ThemeSurfaceVariant,
                                border = if (isInShelf) BorderStroke(1.dp, ThemePrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.toggleMangaInShelf(shelfName, mangaId) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📁", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(shelfName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("${mangaList.size} titles", color = ThemeOnSurfaceVariant, fontSize = 10.sp)
                                        }
                                    }
                                    Checkbox(
                                        checked = isInShelf,
                                        onCheckedChange = { viewModel.toggleMangaInShelf(shelfName, mangaId) },
                                        colors = CheckboxDefaults.colors(checkedColor = ThemePrimary)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isCreatingNew) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newShelfName,
                            onValueChange = { newShelfName = it },
                            placeholder = { Text("e.g. 🔥 Top Tier Romance", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeOutline.copy(alpha = 0.4f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f).testTag("new_shelf_input")
                        )
                        Button(
                            onClick = {
                                if (newShelfName.isNotBlank()) {
                                    viewModel.createShelf(newShelfName.trim())
                                    viewModel.toggleMangaInShelf(newShelfName.trim(), mangaId)
                                    newShelfName = ""
                                    isCreatingNew = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                        ) {
                            Text("Add")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemePrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Create New Shelf")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
            ) {
                Text("Done", color = Color.White)
            }
        },
        containerColor = ThemeSurface
    )
}
