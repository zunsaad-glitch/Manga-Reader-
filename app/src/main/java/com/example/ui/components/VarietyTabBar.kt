package com.example.ui.components

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
import com.example.model.MangaVariety
import com.example.ui.theme.*

@Composable
fun VarietyTabBar(
    selectedVariety: MangaVariety,
    onVarietySelect: (MangaVariety) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MangaVariety.values()) { variety ->
            val isSelected = variety == selectedVariety
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) NeonAccent else DarkSurface,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceElevated) else null,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onVarietySelect(variety) }
                    .testTag("variety_chip_${variety.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = variety.badge,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = variety.displayName,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
