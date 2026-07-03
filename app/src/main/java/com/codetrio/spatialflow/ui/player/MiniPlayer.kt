package com.codetrio.spatialflow.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.components.AutoScrollingText
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

val MiniPlayerHeight = 64.dp
val MiniPlayerBottomSpacer = 8.dp

@Composable
fun MiniPlayer(song: SongItem, isPlaying: Boolean, isCastConnecting: Boolean = false, isPreparingPlayback: Boolean = false, onPlayPause: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier, canScroll: Boolean = true) {
    val haptic = LocalHapticFeedback.current; val scheme = MaterialTheme.colorScheme; val enabled = !isCastConnecting && !isPreparingPlayback
    val pI = remember { MutableInteractionSource() }; val ppI = remember { MutableInteractionSource() }; val nI = remember { MutableInteractionSource() }; val ind = remember { ripple(bounded = false) }
    Row(modifier.fillMaxWidth().height(MiniPlayerHeight).padding(start = 10.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(song.getAlbumArtUri(), "Cover", Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            if (isCastConnecting) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = scheme.onPrimaryContainer) else if (isPreparingPlayback) CircularWavyProgressIndicator(Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            AutoScrollingText(when { isCastConnecting -> "Connecting…"; isPreparingPlayback -> "Preparing…"; else -> song.title },
                MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, fontFamily = GoogleSansRounded, color = scheme.onPrimaryContainer), scheme.primaryContainer, canScroll = canScroll)
            AutoScrollingText(if (isPreparingPlayback) "Loading…" else song.artist,
                MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontFamily = GoogleSansRounded, color = scheme.onPrimaryContainer.copy(alpha = 0.7f)), scheme.primaryContainer, canScroll = canScroll)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary).clickable(pI, ind, enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPrevious() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SkipPrevious, "Prev", modifier = Modifier.size(22.dp), tint = scheme.primary) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary).clickable(ppI, ind, enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() }, contentAlignment = Alignment.Center) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", modifier = Modifier.size(22.dp), tint = scheme.onPrimary) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary).clickable(nI, ind, enabled) { onNext() }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(22.dp), tint = scheme.primary) }
    }
}
