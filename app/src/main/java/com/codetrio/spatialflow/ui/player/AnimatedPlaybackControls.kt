package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private enum class Btn { NONE, PREV, PP, NEXT }

/**
 * EXACT PIXELPLAYER ANIMATED PLAYBACK CONTROLS.
 *
 * Features:
 * - Play/Pause button uses AbsoluteSmoothCornerShape with animated corner radius
 *   (60dp when paused/playing, 26dp when the opposite — the "morphing pill" effect)
 * - Weight animation: the pressed button expands to 1.1x, others compress to 0.65x
 * - Button lock: skip buttons lock for 600ms to prevent double-tap issues
 * - Delayed visual sync: play state waits 220ms before updating icon after release
 * - Previous/Next: CircleShape background
 *
 * All colors come from parameters — pass scheme tokens from the parent.
 */
@Composable
fun AnimatedPlaybackControls(
    isPlayingProvider: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp,
    baseWeight: Float = 1f,
    expansionWeight: Float = 1.1f,
    compressionWeight: Float = 0.65f,
    pressAnimationSpec: AnimationSpec<Float> = tween(200, easing = FastOutSlowInEasing),
    releaseDelay: Long = 220L,
    playPauseCornerPlaying: Dp = 60.dp,
    playPauseCornerPaused: Dp = 26.dp,
    colorPreviousButton: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorNextButton: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorPlayPause: Color = MaterialTheme.colorScheme.primary,
    tintPlayPauseIcon: Color = MaterialTheme.colorScheme.onPrimary,
    tintPreviousIcon: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    tintNextIcon: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    playPauseIconSize: Dp = 36.dp,
    iconSize: Dp = 32.dp,
) {
    val isPlaying = isPlayingProvider()
    var lastClicked by remember { mutableStateOf<Btn?>(null) }
    var clickTrigger by remember { mutableStateOf(0) }
    val latestIsPlaying by rememberUpdatedState(isPlayingProvider)
    val isPlayPauseLocked = lastClicked == Btn.NEXT || lastClicked == Btn.PREV
    var playPauseVisual by remember { mutableStateOf(isPlaying) }
    var pendingVisual by remember { mutableStateOf<Boolean?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val defaultSpatialDpSpec = remember { MotionScheme.expressive().defaultSpatialSpec<Dp>() }

    // ── RELEASE TIMER ──
    LaunchedEffect(lastClicked, clickTrigger) {
        if (lastClicked != null) {
            delay(when (lastClicked) { Btn.NEXT, Btn.PREV -> 600L; else -> releaseDelay })
            lastClicked = null
        }
    }

    // ── VISUAL STATE SYNC (delayed on pause) ──
    LaunchedEffect(isPlaying) {
        if (isPlaying) { pendingVisual = true; return@LaunchedEffect }
        if (!latestIsPlaying()) { delay(releaseDelay); if (!latestIsPlaying()) pendingVisual = false }
    }
    LaunchedEffect(isPlayPauseLocked, pendingVisual) {
        if (!isPlayPauseLocked) pendingVisual?.let { playPauseVisual = it; pendingVisual = null }
    }

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        Row(
            Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun weightFor(b: Btn) = when (lastClicked) { b -> expansionWeight; null -> baseWeight; else -> compressionWeight }

            // ── PREVIOUS ──
            val prevWeight by animateFloatAsState(weightFor(Btn.PREV), pressAnimationSpec, label = "pw")
            Box(
                Modifier.weight(prevWeight).fillMaxHeight().clip(CircleShape).background(colorPreviousButton)
                    .clickable { lastClicked = Btn.PREV; clickTrigger++; scope.launch { delay(180); onPrevious() } },
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(iconSize), tint = tintPreviousIcon) }

            // ── PLAY/PAUSE (animated smooth corners!) ──
            val ppWeight by animateFloatAsState(weightFor(Btn.PP), pressAnimationSpec, label = "ppw")
            val ppCorner by animateDpAsState(
                if (!playPauseVisual) playPauseCornerPlaying else playPauseCornerPaused,
                defaultSpatialDpSpec, label = "ppc"
            )
            Box(
                Modifier.weight(ppWeight).fillMaxHeight().graphicsLayer {
                    clip = true
                    shape = AbsoluteSmoothCornerShape(
                        cornerRadiusTL = ppCorner, smoothnessAsPercentTR = 60,
                        cornerRadiusBL = ppCorner, smoothnessAsPercentTL = 60,
                        cornerRadiusTR = ppCorner, smoothnessAsPercentBL = 60,
                        cornerRadiusBR = ppCorner, smoothnessAsPercentBR = 60
                    )
                }.background(colorPlayPause)
                    .clickable {
                        lastClicked = Btn.PP; clickTrigger++
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playPauseVisual) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playPauseVisual) "Pause" else "Play",
                    modifier = Modifier.size(playPauseIconSize),
                    tint = tintPlayPauseIcon
                )
            }

            // ── NEXT ──
            val nextWeight by animateFloatAsState(weightFor(Btn.NEXT), pressAnimationSpec, label = "nw")
            Box(
                Modifier.weight(nextWeight).fillMaxHeight().clip(CircleShape).background(colorNextButton)
                    .clickable { lastClicked = Btn.NEXT; clickTrigger++; scope.launch { delay(180); onNext() } },
                contentAlignment = Alignment.Center
            ) { Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(iconSize), tint = tintNextIcon) }
        }
    }
}
