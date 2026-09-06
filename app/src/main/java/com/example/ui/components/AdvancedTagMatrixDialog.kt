package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

val POPULAR_TAGS = listOf(
    "Full Color", "Ecchi", "Smut", "3D", "3D Comic", "Harem", "Action", "Adventure", "Romance", "Comedy", "Fantasy", "Isekai", "Sci-Fi",
    "Drama", "Horror", "Mystery", "Psychological", "Supernatural", "Slice of Life",
    "School Life", "Martial Arts", "Tragedy", "Gore"
)

val MIN_CHAPTER_OPTIONS = listOf(0 to "Any", 10 to "10+ Ch", 30 to "30+ Ch", 50 to "50+ Ch", 100 to "100+ Ch")

@Composable
fun AdvancedTagMatrixDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentIncluded by viewModel.includedTags.collectAsState()
    val currentExcluded by viewModel.excludedTags.collectAsState()
    val currentMinChapters by viewModel.minChapterCount.collectAsState()

    var included by remember { mutableStateOf(currentIncluded.toMutableSet()) }
    var excluded by remember { mutableStateOf(currentExcluded.toMutableSet()) }
    var minChapters by remember { mutableStateOf(currentMinChapters) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
            border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("tag_matrix_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = ThemePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Smart Tag & Filter Matrix",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ThemeOnSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Minimum Chapters Filter
                Text("Minimum Chapter Count (Binge Filter):", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MIN_CHAPTER_OPTIONS) { (count, label) ->
                        val isSelected = minChapters == count
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF9800) else ThemeSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { minChapters = count }
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else ThemeOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Included Tags (Green / Check)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✅ Include Tags (Must Match):", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowTagRow(
                    tags = POPULAR_TAGS,
                    selectedTags = included,
                    activeColor = Color(0xFF2E7D32),
                    onToggle = { tag ->
                        if (included.contains(tag)) {
                            included.remove(tag)
                        } else {
                            included.add(tag)
                            excluded.remove(tag) // Cannot be both
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Excluded Tags (Red / Cross)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❌ Exclude Tags (Never Show):", color = Color(0xFFE57373), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowTagRow(
                    tags = POPULAR_TAGS,
                    selectedTags = excluded,
                    activeColor = Color(0xFFC62828),
                    onToggle = { tag ->
                        if (excluded.contains(tag)) {
                            excluded.remove(tag)
                        } else {
                            excluded.add(tag)
                            included.remove(tag) // Cannot be both
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            included.clear()
                            excluded.clear()
                            minChapters = 0
                        }
                    ) {
                        Text("Reset All", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.setSmartTagFilters(
                                included = included,
                                excluded = excluded,
                                minChapters = minChapters
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("apply_tag_matrix_btn")
                    ) {
                        Text("Apply Filters", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowTagRow(
    tags: List<String>,
    selectedTags: Set<String>,
    activeColor: Color,
    onToggle: (String) -> Unit
) {
    // Simple horizontal scrollable row with wrap
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tags) { tag ->
            val isSelected = selectedTags.contains(tag)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) activeColor else ThemeSurfaceVariant.copy(alpha = 0.4f),
                border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onToggle(tag) }
            ) {
                Text(
                    text = tag,
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else ThemeOnSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
