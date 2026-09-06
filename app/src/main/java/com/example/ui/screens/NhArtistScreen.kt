package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.api.nhapi.NhGallery
import com.example.repository.NhApiRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NhArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NhApiRepository() }
    val scope = rememberCoroutineScope()

    var galleries by remember { mutableStateOf<List<NhGallery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Popular") }
    var isFollowing by remember { mutableStateOf(false) }

    val cleanName = remember(artistName) {
        artistName.replace("+", " ").replace("_", " ").trim()
    }

    LaunchedEffect(cleanName) {
        isLoading = true
        scope.launch {
            try {
                val results = repository.getArtistGalleries(cleanName)
                galleries = results
            } catch (_: Exception) {
                galleries = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    val filteredGalleries = remember(galleries, selectedLanguage, selectedSort) {
        var list = galleries
        if (selectedLanguage == "English") {
            list = list.filter { it.language.equals("english", ignoreCase = true) || it.title.contains("english", ignoreCase = true) }
        } else if (selectedLanguage == "Japanese") {
            list = list.filter { it.language.equals("japanese", ignoreCase = true) || it.title.contains("japanese", ignoreCase = true) }
        }

        if (selectedSort == "Popular") {
            list.sortedByDescending { it.favorites }
        } else if (selectedSort == "Pages") {
            list.sortedByDescending { it.pageCount }
        } else {
            list
        }
    }

    Scaffold(
        containerColor = ThemeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "artist: ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = ThemeOnSurfaceVariant
                            )
                        )
                        Text(
                            text = cleanName.lowercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemePrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nh_artist_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isFollowing = !isFollowing }) {
                        Icon(
                            if (isFollowing) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite Artist",
                            tint = if (isFollowing) Color(0xFFE91E63) else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ThemeBackground)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ThemePrimary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Breadcrumbs Header (nHentai layout replica: Home > Artists > Urakan)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "Home",
                                color = ThemeOnSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onBack() }
                            )
                            Text(" › ", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                            Text(
                                "Artists",
                                color = ThemeOnSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { onBack() }
                            )
                            Text(" › ", color = ThemeOnSurfaceVariant, fontSize = 12.sp)
                            Text(
                                cleanName,
                                color = ThemePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 2. Hero Artist Banner Card
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                            border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(ThemePrimary, Color(0xFF9C27B0))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cleanName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "artist: ",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = ThemeOnSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = cleanName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = ThemePrimary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        "${galleries.size} doujinshis / galleries",
                                        color = ThemePrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val artistBio = when (cleanName.lowercase()) {
                                    "urakan" -> "Circle: Urakan Kensetsu | Famous works: Kanojo x Kanojo x Kanojo, Ane Naru Mono, Secret Romance with Step-Sister. Renowned for high-color and romance artwork."
                                    "shindo l" -> "Famous works: Metamorphosis (Emergence), T変. Known for intense drama and iconic storytelling."
                                    "homunculus" -> "Famous works: Velvet Kiss, After Story. Known for detailed aesthetic romance erotica."
                                    "asanagi" -> "Circle: Victims Girls. Known for high fantasy and mind break themes."
                                    "michiking" -> "Famous works: Ane Log, Extra Stories. Renowned for comedy and vanilla themes."
                                    else -> "Verified nHentai artist profile. Discover full high-definition doujinshi galleries, covers, and preview pages."
                                }

                                Text(
                                    text = artistBio,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ThemeOnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { isFollowing = !isFollowing },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) ThemeSurfaceVariant else ThemePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        if (isFollowing) Icons.Filled.Check else Icons.Filled.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isFollowing) ThemePrimary else Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isFollowing) "Following Artist" else "Follow Artist",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isFollowing) ThemePrimary else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 3. Filter & Sort Row
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Galleries (${filteredGalleries.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )

                                // Sort Selector
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Popular", "Recent", "Pages").forEach { sortOption ->
                                        FilterChip(
                                            selected = selectedSort == sortOption,
                                            onClick = { selectedSort = sortOption },
                                            label = { Text(sortOption, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ThemePrimary,
                                                selectedLabelColor = Color.White,
                                                containerColor = ThemeSurface,
                                                labelColor = ThemeOnSurfaceVariant
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = ThemeSurfaceVariant,
                                                selectedBorderColor = ThemePrimary,
                                                enabled = true,
                                                selected = selectedSort == sortOption
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Language Selector
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Language:", fontSize = 11.sp, color = ThemeOnSurfaceVariant)
                                listOf("All", "English", "Japanese").forEach { lang ->
                                    val count = when (lang) {
                                        "All" -> galleries.size
                                        "English" -> galleries.count { it.language.equals("english", true) || it.title.contains("english", true) }
                                        else -> galleries.count { it.language.equals("japanese", true) }
                                    }
                                    SuggestionChip(
                                        onClick = { selectedLanguage = lang },
                                        label = { Text("$lang ($count)", fontSize = 10.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (selectedLanguage == lang) ThemePrimary.copy(alpha = 0.2f) else ThemeSurface,
                                            labelColor = if (selectedLanguage == lang) ThemePrimary else ThemeOnSurfaceVariant
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            borderColor = if (selectedLanguage == lang) ThemePrimary else ThemeSurfaceVariant,
                                            enabled = true
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Authentic Gallery Grid Items
                    if (filteredGalleries.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "No galleries match the filter",
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemeOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredGalleries, key = { it.id }) { gallery ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToDetail(gallery.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = ThemeSurface),
                                border = BorderStroke(1.dp, ThemeOutline.copy(alpha = 0.2f))
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.7f)
                                            .background(Color(0xFF1E1E2C))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(gallery.coverUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = gallery.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Magic ID Pill
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                "#${gallery.id}",
                                                color = ThemePrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Page count badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ThemePrimary.copy(alpha = 0.85f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                "${gallery.pageCount}P",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = gallery.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = ThemeOnSurface
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = gallery.artist ?: cleanName,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = ThemePrimary
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (gallery.favorites > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Favorite,
                                                        contentDescription = null,
                                                        tint = Color(0xFFE91E63),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        "${gallery.favorites}",
                                                        color = ThemeOnSurfaceVariant,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
