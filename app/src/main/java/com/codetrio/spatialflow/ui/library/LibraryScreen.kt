package com.codetrio.spatialflow.ui.library

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.codetrio.spatialflow.R
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.explore.ExploreItem
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import com.codetrio.spatialflow.viewmodel.AccountViewModel
import com.codetrio.spatialflow.data.innertube.AccountManager
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

enum class LibTab(val label: String, val subtitle: String, val icon: ImageVector) {
    LIBRARY("Library", "Your music collection", Icons.Rounded.LibraryMusic),
    PLAYLISTS("Playlists", "Your mixes", Icons.Rounded.QueueMusic),
    SONGS("Songs", "All tracks", Icons.Rounded.MusicNote),
    ARTISTS("Artists", "By artist", Icons.Rounded.Person),
    ALBUMS("Albums", "By album", Icons.Rounded.Album)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerSharedViewModel,
    accountViewModel: AccountViewModel,
    onEditSong: (SongItem) -> Unit,
    onNavigateToExplore: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onConnectClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val context = LocalContext.current

    val isLoggedIn = remember(accountViewModel.userProfile) { AccountManager.isLoggedIn(context) }

    val songList by viewModel.localSongs.collectAsStateWithLifecycle(emptyList())
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val syncedPlaylists by accountViewModel.playlists.collectAsStateWithLifecycle()
    val syncedArtists by accountViewModel.artists.collectAsStateWithLifecycle()
    val syncedAlbums by accountViewModel.albums.collectAsStateWithLifecycle()
    val syncedSongs by accountViewModel.songs.collectAsStateWithLifecycle()

    val localPlaylists by viewModel.localPlaylistsFlow.collectAsStateWithLifecycle()

    val tabs = LibTab.entries.toList()
    var sel by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = sel) { tabs.size }

    LaunchedEffect(sel) { pagerState.animateScrollToPage(sel) }
    LaunchedEffect(pagerState.currentPage) { sel = pagerState.currentPage }

    val maxH = 90.dp
    val maxHPx = with(density) { maxH.toPx() }
    var off by remember { mutableFloatStateOf(0f) }
    val nsc = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(a: androidx.compose.ui.geometry.Offset, s: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (a.y < 0) {
                    val n = off + a.y
                    val o = off
                    off = n.coerceIn(-maxHPx, 0f)
                    return androidx.compose.ui.geometry.Offset(0f, off - o)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(c: androidx.compose.ui.geometry.Offset, a: androidx.compose.ui.geometry.Offset, s: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (a.y > 0) {
                    val n = off + a.y
                    val o = off
                    off = n.coerceIn(-maxHPx, 0f)
                    return androidx.compose.ui.geometry.Offset(0f, off - o)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    val hh = maxH + with(density) { off.toDp() }
    val prog = (1f + (off / maxHPx)).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .align(Alignment.TopCenter)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                listOf(scheme.primaryContainer.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                    }
                }
        )
        Column(Modifier.fillMaxSize().nestedScroll(nsc)) {
            Column(Modifier.fillMaxWidth().height(hh).graphicsLayer { alpha = prog }) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tabs[sel].label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = tabs[sel].subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs.size) { i ->
                    val isSel = i == sel
                    val int = remember { MutableInteractionSource() }
                    val press by int.collectIsPressedAsState()
                    val sc by animateFloatAsState(
                        if (press) 0.92f else if (isSel) 1.05f else 1f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
                        label = "ts"
                    )
                    val bg by animateColorAsState(
                        if (isSel) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.5f),
                        spring(stiffness = Spring.StiffnessMedium),
                        label = "tbg"
                    )
                    val ct by animateColorAsState(
                        if (isSel) scheme.onPrimary else scheme.onSurfaceVariant,
                        spring(stiffness = Spring.StiffnessMedium),
                        label = "tct"
                    )
                    Row(
                        Modifier
                            .graphicsLayer { scaleX = sc; scaleY = sc }
                            .clip(CircleShape)
                            .background(bg)
                            .clickable(int, null) { sel = i }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = tabs[i].icon, contentDescription = tabs[i].label, tint = ct, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = tabs[i].label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = ct)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    LibTab.LIBRARY -> {
                        LibMixTab(
                            localSongs = songList,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isLoggedIn = isLoggedIn,
                            viewModel = viewModel,
                            onConnectClick = onConnectClick
                        )
                    }
                    LibTab.PLAYLISTS -> {
                        val mappedLocal = localPlaylists.map {
                            ExploreItem(id = "local_${it.id}", title = it.title, subtitle = "Local Custom Playlist", artworkUrl = null)
                        }
                        val mappedSynced = syncedPlaylists.map {
                            ExploreItem(id = it.playlistId, title = it.title, subtitle = it.songCount ?: "Playlist", artworkUrl = it.thumbnailUrl)
                        }
                        val allPlaylists = mappedLocal + mappedSynced

                        if (allPlaylists.isEmpty()) {
                            if (!isLoggedIn) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                    ConnectYTMCard(onConnectClick = onConnectClick)
                                    Spacer(Modifier.height(16.dp))
                                    LibPlaceholder("Playlists", Icons.Rounded.QueueMusic)
                                }
                            } else {
                                LibPlaceholder("Playlists", Icons.Rounded.QueueMusic)
                            }
                        } else {
                            PlaylistsContent(playlists = allPlaylists, onPlaylistClick = onPlaylistClick)
                        }
                    }
                    LibTab.SONGS -> {
                        val mappedSyncedSongs = syncedSongs.map {
                            SongItem.createOnlineSong(
                                it.videoId, it.title, it.artist, "https://music.youtube.com/watch?v=${it.videoId}",
                                it.durationMs ?: 0L, it.thumbnailUrl, it.artistId
                            )
                        }
                        val combinedSongs = songList + mappedSyncedSongs
                        
                        if (combinedSongs.isEmpty()) {
                            if (!isLoggedIn) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                    ConnectYTMCard(onConnectClick = onConnectClick)
                                    Spacer(Modifier.height(16.dp))
                                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                        Text(text = "No songs found", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "No songs found", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            SongsContent(
                                songs = combinedSongs,
                                currentSongId = currentSong?.videoId,
                                isPlaying = isPlaying,
                                onSongClick = { song, index ->
                                    viewModel.setSongList(combinedSongs)
                                    viewModel.playSongAtIndex(index)
                                },
                                onMoreOptions = onEditSong
                            )
                        }
                    }
                    LibTab.ARTISTS -> {
                        val mappedArtists = syncedArtists.map {
                            ExploreItem(id = it.browseId, title = it.title, subtitle = "Synced Artist", artworkUrl = it.thumbnailUrl)
                        }
                        if (mappedArtists.isEmpty()) {
                            if (!isLoggedIn) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                    ConnectYTMCard(onConnectClick = onConnectClick)
                                    Spacer(Modifier.height(16.dp))
                                    LibPlaceholder("Artists", Icons.Rounded.Person)
                                }
                            } else {
                                LibPlaceholder("Artists", Icons.Rounded.Person)
                            }
                        } else {
                            ArtistsContent(artists = mappedArtists, onArtistClick = onArtistClick)
                        }
                    }
                    LibTab.ALBUMS -> {
                        val mappedAlbums = syncedAlbums.map {
                            ExploreItem(id = it.browseId, title = it.title, subtitle = it.artists.joinToString { it.name }, artworkUrl = it.thumbnailUrl)
                        }
                        if (mappedAlbums.isEmpty()) {
                            if (!isLoggedIn) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                                    ConnectYTMCard(onConnectClick = onConnectClick)
                                    Spacer(Modifier.height(16.dp))
                                    LibPlaceholder("Albums", Icons.Rounded.Album)
                                }
                            } else {
                                LibPlaceholder("Albums", Icons.Rounded.Album)
                            }
                        } else {
                            AlbumsContent(albums = mappedAlbums, onAlbumClick = onAlbumClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibPlaceholder(t: String, ic: ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = ic, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(text = "$t will appear here", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibMixTab(
    localSongs: List<SongItem>,
    currentSong: SongItem?,
    isPlaying: Boolean,
    isLoggedIn: Boolean,
    viewModel: PlayerSharedViewModel,
    onConnectClick: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        if (!isLoggedIn) {
            item {
                ConnectYTMCard(onConnectClick = onConnectClick)
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.setSongList(localSongs)
                        viewModel.toggleShuffle()
                        viewModel.playNextSong()
                    },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
                FilledTonalButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Favorite, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Favorites")
                }
            }
        }
        if (localSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Your Songs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            items(localSongs.take(20), key = { it.videoId ?: it.hashCode().toString() }) { song ->
                LibSongRow(song, song.videoId == currentSong?.videoId, isPlaying) {
                    viewModel.setSongList(localSongs)
                    viewModel.playSongAtIndex(localSongs.indexOf(song))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun LibSongRow(song: SongItem, isCur: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tr = updateTransition(isCur, label = "hl")
    val sc by tr.animateColor(transitionSpec = { tween(400) }, label = "sc") { if (it) scheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent }
    val tc by tr.animateColor(transitionSpec = { tween(400) }, label = "tc") { if (it) scheme.primary else scheme.onSurface }
    val cr by tr.animateDp(transitionSpec = { tween(400) }, label = "cr") { if (it) 24.dp else 16.dp }
    val ac by tr.animateDp(transitionSpec = { tween(400) }, label = "ac") { if (it) 22.dp else 12.dp }
    Surface(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(cr))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cr),
        color = sc,
        tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ac))
                    .background(scheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(song.getAlbumArtUri(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = tc, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isCur && isPlaying) Box(Modifier.size(5.dp).clip(CircleShape).background(scheme.primary))
        }
    }
}

@Composable
fun ConnectYTMCard(onConnectClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTL = CardCorners.RadiusDp.dp,
            cornerRadiusTR = CardCorners.RadiusDp.dp,
            cornerRadiusBL = CardCorners.RadiusDp.dp,
            cornerRadiusBR = CardCorners.RadiusDp.dp,
            smoothnessAsPercentTL = CardCorners.Smoothness,
            smoothnessAsPercentTR = CardCorners.Smoothness,
            smoothnessAsPercentBL = CardCorners.Smoothness,
            smoothnessAsPercentBR = CardCorners.Smoothness
        ),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.Link, null, tint = scheme.primary, modifier = Modifier.size(36.dp))
            Text("Connect YouTube Music", style = MaterialTheme.typography.titleMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            Text(
                "Link your account to sync your playlists, liked songs, albums, and artists.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                shape = CircleShape
            ) {
                Text("Connect Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
