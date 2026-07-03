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
import coil.size.Size

/** Height of the miniplayer bar at the bottom of screens */
val MiniPlayerHeight = 64.dp

/**
 * EXACT PIXELPLAYER MINIPLAYER CLONE.
 *
 * Layout: [44dp album art circle] [title + artist] [36dp prev] [36dp play/pause] [36dp next]
 *
 * Button styling:
 *   Previous/Next: 36dp circle, onPrimary background, primary icon, 22dp icon size
 *   Play/Pause:    36dp circle, primary background, onPrimary icon, 22dp icon size
 *   Haptics:       TextHandleMove on press
 *   Ripple:        unbounded circular
 */
@Composable
fun MiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    isCastConnecting: Boolean = false,
    isPreparingPlayback: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canScroll: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val enabled = !isCastConnecting && !isPreparingPlayback

    val prevInteraction = remember { MutableInteractionSource() }
    val ppInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val indication = remember { ripple(bounded = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── ALBUM ART (44dp circle, exact PixelPlayer size) ──
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.getAlbumArtUri(),
                contentDescription = "Cover",
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isCastConnecting) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = scheme.onPrimaryContainer)
            } else if (isPreparingPlayback) {
                CircularWavyProgressIndicator(Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.width(12.dp))

        // ── SONG INFO ──
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            AutoScrollingText(
                text = when {
                    isCastConnecting -> "Connecting to device…"
                    isPreparingPlayback -> "Preparing playback…"
                    else -> song.title
                },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp, fontFamily = GoogleSansRounded,
                    color = scheme.onPrimaryContainer
                ),
                gradientEdgeColor = scheme.primaryContainer,
                canScroll = canScroll
            )
            AutoScrollingText(
                text = if (isPreparingPlayback) "Loading audio…" else song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp, fontFamily = GoogleSansRounded,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
                ),
                gradientEdgeColor = scheme.primaryContainer,
                canScroll = canScroll
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── PREVIOUS (onPrimary bg, primary icon, 36dp, 22dp icon) ──
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary)
                .clickable(prevInteraction, indication, enabled = enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPrevious()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SkipPrevious, "Previous", tint = scheme.primary, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(8.dp))

        // ── PLAY/PAUSE (primary bg, onPrimary icon, 36dp, 22dp icon) ──
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(scheme.primary)
                .clickable(ppInteraction, indication, enabled = enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (isPlaying) "Pause" else "Play",
                tint = scheme.onPrimary, modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── NEXT (onPrimary bg, primary icon, 36dp, 22dp icon) ──
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary)
                .clickable(nextInteraction, indication, enabled = enabled) {
                    onNext()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SkipNext, "Next", tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
    }
}
