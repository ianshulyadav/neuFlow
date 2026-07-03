package com.codetrio.spatialflow.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.components.SongListItem
import com.codetrio.spatialflow.ui.explore.AlbumCard
import com.codetrio.spatialflow.ui.explore.ExploreItem

@Composable
fun SongsContent(
    songs: List<SongItem>, currentSongId: String?, isPlaying: Boolean,
    onSongClick: (SongItem, Int) -> Unit, onMoreOptions: (SongItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("All Songs") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primary, labelColor = MaterialTheme.colorScheme.onPrimary))
                AssistChip(onClick = {}, label = { Text("Liked") }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), labelColor = MaterialTheme.colorScheme.onSurfaceVariant))
                AssistChip(onClick = {}, label = { Text("Downloaded") })
            }
        }
        items(songs, key = { it.videoId ?: it.hashCode().toString() }) { song ->
            SongListItem(song, song.videoId == currentSongId, isPlaying && song.videoId == currentSongId, true, { onSongClick(song, songs.indexOf(song)) }, { onMoreOptions(song) })
        }
    }
}

@Composable
fun AlbumsContent(albums: List<ExploreItem>, onAlbumClick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(GridCells.Fixed(2), modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(albums, key = { it.id }) { album -> AlbumCard(album.title, album.subtitle, album.artworkUrl, { onAlbumClick(album.id) }) }
    }
}

@Composable
fun ArtistsContent(artists: List<ExploreItem>, onArtistClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(artists, key = { it.id }) { artist ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onArtistClick(artist.id) }, RoundedCornerShape(16.dp), Color.Transparent, tonalElevation = 0.dp) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(50.dp).clip(CircleShape).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (artist.artworkUrl != null) coil.compose.AsyncImage(artist.artworkUrl, artist.title, Modifier.fillMaxSize())
                        else Icon(imageVector = Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(28.dp), tint = scheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f)) { Text(artist.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface); Text(artist.subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                    Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
fun PlaylistsContent(playlists: List<ExploreItem>, onPlaylistClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onPlaylistClick(playlist.id) }, RoundedCornerShape(16.dp), Color.Transparent, tonalElevation = 0.dp) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (playlist.artworkUrl != null) coil.compose.AsyncImage(playlist.artworkUrl, playlist.title, Modifier.fillMaxSize())
                        else Icon(imageVector = Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(28.dp), tint = scheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f)) { Text(playlist.title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface); Text(playlist.subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant) }
                    Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}
