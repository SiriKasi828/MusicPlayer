package com.example.musicplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ---------- Theme ----------

val AppPurple = Color(0xFF8B5CF6)
val AppBlue = Color(0xFF3B82F6)
val AppDarkBg = Color(0xFF0F0F14)
val AppCardBg = Color(0xFF1B1B24)
val AppMiniBg = Color(0xFF2A2438)
val AppSoftText = Color(0xFFA0A0B0)

private val TopGlow = Brush.verticalGradient(0f to Color(0xFF2A1B4D), 0.5f to AppDarkBg)

private val AppColors = darkColorScheme(
    primary = AppPurple,
    onPrimary = Color.White,
    background = AppDarkBg,
    onBackground = Color(0xFFF2F2F7),
    surface = AppCardBg,
    onSurface = Color(0xFFF2F2F7),
    onSurfaceVariant = AppSoftText
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, content = content)
}

// ---------- Shared pieces ----------

@Composable
fun Cover(url: String, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    val shape = RoundedCornerShape(corner)
    if (url.isBlank()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(listOf(AppPurple, AppBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Couldn't load songs", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(message, textAlign = TextAlign.Center, color = AppSoftText)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp)
    )
}

@Composable
private fun ArtistBubble(name: String, imageUrl: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Cover(imageUrl, Modifier.size(88.dp), corner = 44.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- Home ----------

@Composable
fun HomeScreen(
    sections: List<Section>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit
) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    Box(Modifier.fillMaxSize().background(TopGlow).statusBarsPadding()) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AppPurple
            )

            error != null -> ErrorView(error, onRetry, Modifier.align(Alignment.Center))

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                item {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                    )
                }

                sections.firstOrNull()?.let { first ->
                    first.songs.firstOrNull()?.let { hero ->
                        item {
                            FeaturedCard(
                                song = hero,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) { onSongClick(first.songs, 0) }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            first.songs.drop(1).take(6).chunked(2).forEachIndexed { rowIdx, pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEachIndexed { colIdx, song ->
                                        QuickPick(song, Modifier.weight(1f)) {
                                            onSongClick(first.songs, 1 + rowIdx * 2 + colIdx)
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                val artistSections = sections.filter { it.isArtist }
                if (artistSections.isNotEmpty()) {
                    item {
                        Column {
                            SectionTitle("Popular artists")
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(artistSections, key = { it.title }) { s ->
                                    ArtistBubble(s.title, s.songs.first().artworkUrl) {
                                        onSongClick(s.songs, 0)
                                    }
                                }
                            }
                        }
                    }
                }

                sections.forEach { section ->
                    item(key = section.title) {
                        Column {
                            SectionTitle(if (section.isArtist) "Best of ${section.title}" else section.title)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(section.songs, key = { _, s -> s.id }) { i, s ->
                                    SongCard(s) { onSongClick(section.songs, i) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(song: Song, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Cover(song.artworkUrl, Modifier.fillMaxSize(), corner = 0.dp)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.3f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.72f)
                .padding(18.dp)
        ) {
            Text(
                "FEATURED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC4B5FD),
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(AppPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun QuickPick(song: Song, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(AppCardBg)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.artworkUrl, Modifier.size(56.dp), corner = 0.dp)
        Text(
            song.title,
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Cover(song.artworkUrl, Modifier.size(150.dp), corner = 12.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = AppSoftText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- Search ----------

private class Genre(val name: String, val term: String, val c1: Color, val c2: Color)

private val genres = listOf(
    Genre("Pop", "pop hits", Color(0xFFEC4899), Color(0xFF8B5CF6)),
    Genre("Rock", "classic rock", Color(0xFFEF4444), Color(0xFF7F1D1D)),
    Genre("Hip-Hop", "hip hop hits", Color(0xFFF59E0B), Color(0xFFB45309)),
    Genre("Dance", "dance hits", Color(0xFF06B6D4), Color(0xFF3B82F6)),
    Genre("Chill", "chill", Color(0xFF10B981), Color(0xFF065F46)),
    Genre("Indie", "indie pop", Color(0xFF84CC16), Color(0xFF3F6212)),
    Genre("R&B", "r&b hits", Color(0xFF8B5CF6), Color(0xFF312E81)),
    Genre("Country", "country hits", Color(0xFFD97706), Color(0xFF78350F))
)

@Composable
private fun GenreTile(genre: Genre, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(shape)
            .background(Brush.linearGradient(listOf(genre.c1, genre.c2)))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            genre.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.align(Alignment.BottomEnd).size(44.dp)
        )
    }
}

@Composable
fun SearchScreen(
    results: List<Song>,
    searching: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSongClick: (List<Song>, Int) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val typed = query.trim().length >= 2

    Column(Modifier.fillMaxSize().background(TopGlow).statusBarsPadding()) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChange(it)
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Songs or artists") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onQueryChange("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearch(query)
                focus.clearFocus()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppCardBg,
                unfocusedContainerColor = AppCardBg,
                focusedBorderColor = AppPurple,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = AppPurple
            )
        )

        Box(
            modifier = Modifier.fillMaxWidth().height(14.dp).padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (searching) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppPurple,
                    trackColor = Color.Transparent
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                !typed -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Browse all",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    genres.chunked(2).forEach { pair ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { g ->
                                    GenreTile(g, Modifier.weight(1f)) {
                                        query = g.name
                                        onSearch(g.term)
                                        focus.clearFocus()
                                    }
                                }
                            }
                        }
                    }
                }

                error != null && results.isEmpty() -> Text(
                    error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = AppSoftText,
                    textAlign = TextAlign.Center
                )

                results.isEmpty() -> Spacer(Modifier.fillMaxSize())

                else -> {
                    val q = query.trim()
                    val artists = remember(results, q) {
                        results.filter { it.artist.contains(q, ignoreCase = true) }
                            .distinctBy { it.artist }
                            .take(8)
                    }
                    val sorted = remember(results, q) {
                        results.sortedByDescending { it.artist.contains(q, ignoreCase = true) }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (artists.isNotEmpty()) {
                            item {
                                Text(
                                    "Artists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(artists, key = { it.artist }) { s ->
                                        ArtistBubble(s.artist, s.artworkUrl) {
                                            query = s.artist
                                            onSearch(s.artist)
                                            focus.clearFocus()
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Text(
                                "Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        itemsIndexed(sorted, key = { _, s -> s.id }) { i, song ->
                            SongRow(song) { onSongClick(sorted, i) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongRow(song: Song, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppCardBg)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.artworkUrl, Modifier.size(60.dp), corner = 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AppSoftText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppPurple.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = AppPurple)
        }
    }
}

// ---------- Bottom bar + mini player ----------

@Composable
fun AppBottomBar(selected: String, onSelect: (String) -> Unit) {
    val colors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color.White,
        selectedTextColor = Color.White,
        indicatorColor = AppPurple.copy(alpha = 0.35f),
        unselectedIconColor = AppSoftText,
        unselectedTextColor = AppSoftText
    )
    NavigationBar(containerColor = AppCardBg) {
        NavigationBarItem(
            selected = selected == "home",
            onClick = { onSelect("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = colors
        )
        NavigationBarItem(
            selected = selected == "search",
            onClick = { onSelect("search") },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search") },
            colors = colors
        )
    }
}

@Composable
fun MiniPlayer(song: Song, isPlaying: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(shape)
            .background(AppMiniBg)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(song.artworkUrl, Modifier.size(44.dp), corner = 8.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AppSoftText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ---------- Now Playing ----------

@Composable
fun PlayerScreen(
    song: Song,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1B4D), AppDarkBg)))
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "NOW PLAYING",
                style = MaterialTheme.typography.labelMedium,
                color = AppSoftText,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = 24.dp
            ) {
                Cover(
                    song.artworkUrl,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    corner = 24.dp
                )
            }

            Spacer(Modifier.height(36.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = AppSoftText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(28.dp))

            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableStateOf(0f) }
            val total = duration.coerceAtLeast(1L)
            val fraction = if (dragging) dragValue else position.toFloat() / total
            val current = if (dragging) (dragValue * total).toLong() else position

            Slider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    onSeek((dragValue * total).toLong())
                    dragging = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = AppPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatTime(current),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppSoftText
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (duration > 0) "-" + formatTime(total - current) else "0:00",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppSoftText
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AppPurple)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(java.util.Locale.US, totalSec / 60, totalSec % 60)
}
