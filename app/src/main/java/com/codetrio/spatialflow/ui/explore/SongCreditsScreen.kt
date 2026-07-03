package com.codetrio.spatialflow.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCreditsScreen(
    song: SongItem,
    onBack: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Song Info & Credits", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(36.dp), tint = scheme.onPrimaryContainer)
                    }
                    Column {
                        Text(song.title, style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                        Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    }
                }
            }

            // Info rows
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    InfoItem(icon = Icons.Rounded.MusicNote, title = "Title", value = song.title)
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = scheme.outlineVariant.copy(alpha = 0.4f))
                    InfoItem(icon = Icons.Rounded.Person, title = "Artist", value = song.artist)
                    if (song.albumId > 0) {
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = scheme.outlineVariant.copy(alpha = 0.4f))
                        InfoItem(icon = Icons.Rounded.Info, title = "Album ID", value = song.albumId.toString())
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = scheme.outlineVariant.copy(alpha = 0.4f))
                    InfoItem(
                        icon = Icons.Rounded.Schedule,
                        title = "Duration",
                        value = formatDuration(song.duration)
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = scheme.outlineVariant.copy(alpha = 0.4f))
                    InfoItem(
                        icon = Icons.Rounded.Folder,
                        title = "Location",
                        value = song.path ?: song.contentUri.toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(icon: ImageVector, title: String, value: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(20.dp), tint = scheme.onSurfaceVariant)
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = scheme.primary, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
