package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MangaSource
import com.example.ui.theme.*

@Composable
fun SourceTabBar(
    selectedSource: MangaSource,
    onSourceSelect: (MangaSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val sources = listOf(
        MangaSource.ALL,
        MangaSource.MANGADEX,
        MangaSource.MANHWATOON,
        MangaSource.MANGATOON,
        MangaSource.MANTA
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sources) { source ->
            val isSelected = source == selectedSource
            val activeColor = when (source) {
                MangaSource.MANGADEX -> MangaDexColor
                MangaSource.MANHWATOON -> ManhwaToonColor
                MangaSource.MANGATOON -> MangaToonColor
                MangaSource.MANTA -> MantaColor
                MangaSource.ALL -> NeonAccent
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) activeColor else DarkSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSourceSelect(source) }
                    .testTag("source_tab_${source.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.displayName,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
