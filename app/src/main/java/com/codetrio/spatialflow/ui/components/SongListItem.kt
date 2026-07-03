package com.codetrio.spatialflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.model.SongItem

import androidx.compose.animation.animateColor

@Composable
fun SongListItem(song: SongItem, isCurrentSong: Boolean, isPlaying: Boolean, showAlbumArt: Boolean = true, onClick: () -> Unit, onMoreOptions: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme; val transition = updateTransition(isCurrentSong, label = "hl")
    val surfaceColor by transition.animateColor({ tween(400) }, label = "sc") { if (it) scheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent }
    val textColor by transition.animateColor({ tween(400) }, label = "tc") { if (it) scheme.primary else scheme.onSurface }
    Surface(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), RoundedCornerShape(16.dp), surfaceColor, tonalElevation = 0.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showAlbumArt) { Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) { AsyncImage(song.getAlbumArtUri(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
            Column(Modifier.weight(1f)) { Text(song.title, style = MaterialTheme.typography.titleMedium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal); Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (isCurrentSong && isPlaying) Box(Modifier.size(4.dp).clip(CircleShape).background(scheme.primary))
            if (onMoreOptions != null) IconButton(onClick = onMoreOptions) { Icon(Icons.Rounded.MoreVert, null, tint = scheme.onSurfaceVariant) }
        }
    }
}
