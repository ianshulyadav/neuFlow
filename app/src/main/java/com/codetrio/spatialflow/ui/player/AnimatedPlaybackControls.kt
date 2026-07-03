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
import androidx.compose.material3.*
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

@Composable
fun AnimatedPlaybackControls(
    isPlayingProvider: () -> Boolean, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    modifier: Modifier = Modifier, height: Dp = 90.dp, pressAnimationSpec: AnimationSpec<Float> = tween(200, easing = FastOutSlowInEasing),
    releaseDelay: Long = 220L, ppCornerPlaying: Dp = 60.dp, ppCornerPaused: Dp = 26.dp,
    colorPrev: Color = MaterialTheme.colorScheme.secondaryContainer, colorNext: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorPP: Color = MaterialTheme.colorScheme.primary, tintPP: Color = MaterialTheme.colorScheme.onPrimary,
    tintPrev: Color = MaterialTheme.colorScheme.onSecondaryContainer, tintNext: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    ppIconSize: Dp = 36.dp, iconSize: Dp = 32.dp
) {
    val isP = isPlayingProvider(); var last by remember { mutableStateOf<Btn?>(null) }; var trigger by remember { mutableStateOf(0) }
    val latestIsPlaying by rememberUpdatedState(isPlayingProvider); val locked = last == Btn.NEXT || last == Btn.PREV
    var visual by remember { mutableStateOf(isP) }; var pending by remember { mutableStateOf<Boolean?>(null) }
    val haptic = LocalHapticFeedback.current; val scope = rememberCoroutineScope(); val spatialSpec = remember { MotionScheme.expressive().defaultSpatialSpec<Dp>() }
    LaunchedEffect(last, trigger) { if (last != null) { delay(when (last) { Btn.NEXT, Btn.PREV -> 600L; else -> releaseDelay }); last = null } }
    LaunchedEffect(isP) { if (isP) pending = true else { if (!latestIsPlaying()) { delay(releaseDelay); if (!latestIsPlaying()) pending = false } } }
    LaunchedEffect(locked, pending) { if (!locked) pending?.let { visual = it; pending = null } }
    Box(modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            fun w(b: Btn) = when (last) { b -> 1.1f; null -> 1f; else -> 0.65f }
            val pw by animateFloatAsState(w(Btn.PREV), pressAnimationSpec, label = "pw")
            Box(Modifier.weight(pw).fillMaxHeight().clip(CircleShape).background(colorPrev).clickable { last = Btn.PREV; trigger++; scope.launch { delay(180); onPrevious() } }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SkipPrevious, "Prev", modifier = Modifier.size(iconSize), tint = tintPrev) }
            val ppw by animateFloatAsState(w(Btn.PP), pressAnimationSpec, label = "ppw"); val ppc by animateDpAsState(if (!visual) ppCornerPlaying else ppCornerPaused, spatialSpec, label = "ppc")
            Box(Modifier.weight(ppw).fillMaxHeight().graphicsLayer { clip = true; shape = AbsoluteSmoothCornerShape(ppc, 60) }.background(colorPP).clickable { last = Btn.PP; trigger++; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() }, contentAlignment = Alignment.Center) { Icon(if (visual) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (visual) "Pause" else "Play", modifier = Modifier.size(ppIconSize), tint = tintPP) }
            val nw by animateFloatAsState(w(Btn.NEXT), pressAnimationSpec, label = "nw")
            Box(Modifier.weight(nw).fillMaxHeight().clip(CircleShape).background(colorNext).clickable { last = Btn.NEXT; trigger++; scope.launch { delay(180); onNext() } }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.SkipNext, "Next", modifier = Modifier.size(iconSize), tint = tintNext) }
        }
    }
}
