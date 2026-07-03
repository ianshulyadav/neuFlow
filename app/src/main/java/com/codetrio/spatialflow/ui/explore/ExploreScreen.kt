package com.codetrio.spatialflow.ui.explore

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.codetrio.spatialflow.ui.components.SectionHeader
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

/**
 * Fully overhauled Explore screen.
 * Connected to existing data sources, all scheme-driven.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onSearchClick: () -> Unit,
    onAccountClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    // Real data — connect these from ViewModel
    quickPicks: List<ExploreItem>,
    trendingAlbums: List<ExploreItem>,
    popularPlaylists: List<ExploreItem>,
    featuredArtists: List<ExploreItem>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    // Collapsible header (ArchiveTune style)
    val maxHeader = 80.dp
    val maxHeaderPx = with(LocalDensity.current) { maxHeader.toPx() }
    var headerOffset by remember { mutableFloatStateOf(0f) }

    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) { val n = headerOffset + available.y; val o = headerOffset; headerOffset = n.coerceIn(-maxHeaderPx, 0f); return Offset(0f, headerOffset - o) }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) { val n = headerOffset + available.y; val o = headerOffset; headerOffset = n.coerceIn(-maxHeaderPx, 0f); return Offset(0f, headerOffset - o) }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(maxHeader + with(LocalDensity.current) { headerOffset.toDp() })
                    .background(Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.35f), Color.Transparent)))
            ) {
                Spacer(Modifier.statusBarsPadding())
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore", style = MaterialTheme.typography.headlineLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Rounded.Search, "Search", tint = scheme.onSurface, modifier = Modifier.size(26.dp))
                    }
                    IconButton(onClick = onAccountClick) {
                        Icon(Icons.Rounded.AccountCircle, "Account", tint = scheme.onSurface, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(nestedScroll)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ── QUICK PICKS ──
                if (quickPicks.isNotEmpty()) {
                    item { SectionHeader("Quick Picks") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(quickPicks, key = { it.id }) { item ->
                                AlbumCard(title = item.title, subtitle = item.subtitle, artworkUrl = item.artworkUrl,
                                    onClick = { onAlbumClick(item.id) }, modifier = Modifier.width(160.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── TRENDING ALBUMS ──
                if (trendingAlbums.isNotEmpty()) {
                    item { SectionHeader("Trending Albums") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(trendingAlbums, key = { it.id }) { item ->
                                AlbumCard(title = item.title, subtitle = item.subtitle, artworkUrl = item.artworkUrl,
                                    onClick = { onAlbumClick(item.id) }, modifier = Modifier.width(160.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── POPULAR PLAYLISTS ──
                if (popularPlaylists.isNotEmpty()) {
                    item { SectionHeader("Popular Playlists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(popularPlaylists, key = { it.id }) { item ->
                                PlaylistCard(title = item.title, subtitle = item.subtitle, artworkUrl = item.artworkUrl,
                                    onClick = { onPlaylistClick(item.id) }, modifier = Modifier.width(160.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── FEATURED ARTISTS ──
                if (featuredArtists.isNotEmpty()) {
                    item { SectionHeader("Featured Artists") }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(featuredArtists, key = { it.id }) { item ->
                                ArtistChip(name = item.title, imageUrl = item.artworkUrl,
                                    onClick = { onArtistClick(item.id) })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/** Data class for explore content — connect to real models from ViewModel */
data class ExploreItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: Any?
)

/**
 * ViewModel-based overload for backward compatibility with MainActivity.
 * Extracts home feed data from ExploreViewModel and delegates to the data-based ExploreScreen.
 */
@Composable
fun ExploreScreen(
    viewModel: com.codetrio.spatialflow.viewmodel.ExploreViewModel,
    playerSharedViewModel: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Helper to map SearchItem to ExploreItem
    fun com.codetrio.spatialflow.data.innertube.SearchItem.toExploreItem(): ExploreItem = when (this) {
        is com.codetrio.spatialflow.data.innertube.SearchItem.Song -> ExploreItem(
            id = song.videoId, title = song.title, subtitle = song.artist, artworkUrl = song.thumbnailUrl
        )
        is com.codetrio.spatialflow.data.innertube.SearchItem.Album -> ExploreItem(
            id = album.browseId, title = album.title, subtitle = album.artists.joinToString { it.name }, artworkUrl = album.thumbnailUrl
        )
        is com.codetrio.spatialflow.data.innertube.SearchItem.Playlist -> ExploreItem(
            id = playlist.playlistId, title = playlist.title, subtitle = playlist.songCount ?: "", artworkUrl = playlist.thumbnailUrl
        )
        is com.codetrio.spatialflow.data.innertube.SearchItem.Artist -> ExploreItem(
            id = artist.browseId, title = artist.title, subtitle = artist.subscriberCount ?: "", artworkUrl = artist.thumbnailUrl
        )
    }

    // Map home sections to ExploreItem lists
    val sections = uiState.homeSections
    val quickPicks = sections.getOrNull(0)?.items?.map { it.toExploreItem() } ?: emptyList()
    val trendingAlbums = sections.getOrNull(1)?.items?.map { it.toExploreItem() } ?: emptyList()
    val popularPlaylists = sections.getOrNull(2)?.items?.map { it.toExploreItem() } ?: emptyList()
    val featuredArtists = sections.getOrNull(3)?.items?.map { it.toExploreItem() } ?: emptyList()

    ExploreScreen(
        onSearchClick = { /* TODO: wire to search nav */ },
        onAccountClick = { /* TODO: wire to account nav */ },
        onAlbumClick = { /* TODO: wire to album detail */ },
        onPlaylistClick = { /* TODO: wire to playlist detail */ },
        onArtistClick = { /* TODO: wire to artist detail */ },
        quickPicks = quickPicks,
        trendingAlbums = trendingAlbums,
        popularPlaylists = popularPlaylists,
        featuredArtists = featuredArtists,
        isRefreshing = uiState.isLoadingHome || uiState.isRefreshing,
        onRefresh = { viewModel.loadHomeFeed() },
        modifier = modifier
    )
}


