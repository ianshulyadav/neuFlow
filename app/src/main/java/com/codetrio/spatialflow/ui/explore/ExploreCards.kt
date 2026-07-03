package com.codetrio.spatialflow.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

// ═══════════════════════════════════════════════
// ALBUM CARD (Grid) — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    artworkUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = remember {
        AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().aspectRatio(1f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AsyncImage(artworkUrl, title, Modifier.fillMaxWidth().weight(1f).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
            Column(Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// PLAYLIST CARD (Horizontal) — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun PlaylistCard(
    title: String,
    subtitle: String,
    artworkUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = remember {
        AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness)
    }

    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AsyncImage(artworkUrl, title, Modifier.fillMaxWidth().aspectRatio(1f).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
            Column(Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// ARTIST CHIP — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun ArtistChip(
    name: String,
    imageUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.width(80.dp).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(imageUrl, name, Modifier.size(72.dp).clip(CircleShape).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
        Text(name, style = MaterialTheme.typography.labelMedium, fontFamily = GoogleSansRounded, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
