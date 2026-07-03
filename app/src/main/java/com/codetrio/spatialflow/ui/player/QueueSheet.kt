package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.R
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import kotlinx.coroutines.launch

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

@Composable
fun QueueSheet(
    queue: List<SongItem>, currentSongId: String?, isPlaying: Boolean,
    repeatMode: Int, isShuffleOn: Boolean,
    sleepTimerMode: PlayerSharedViewModel.SleepTimerMode = PlayerSharedViewModel.SleepTimerMode.OFF,
    onDismiss: () -> Unit, onPlaySong: (SongItem, Int) -> Unit, onRemoveSong: (String) -> Unit,
    onToggleShuffle: () -> Unit, onToggleLoopMode: () -> Unit, onShowSleepTimerDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme; val density = LocalDensity.current; val haptic = LocalHapticFeedback.current
    val currentSongIndex = queue.indexOfFirst { it.videoId == currentSongId }
    val trayBg = scheme.surfaceContainer; val trayInactiveBtn = scheme.surfaceVariant; val trayActiveBtn = scheme.primary
    val trayInactiveCt = scheme.onSurfaceVariant; val trayActiveCt = scheme.onPrimary

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Queue", style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close", tint = scheme.onSurfaceVariant) }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 6.dp, bottom = 12.dp)) {
                itemsIndexed(queue, key = { i, s -> "${s.videoId}_$i" }) { index, song ->
                    QueueSongCard(song = song, isCurrent = index == currentSongIndex, isPlaying = isPlaying && index == currentSongIndex, onTap = { onPlaySong(song, index) }, onRemove = { onRemoveSong(song.videoId ?: "") })
                }
            }
        }

        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), trayBg) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 18.dp, bottom = 14.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TrayBtn(R.drawable.ic_shuffle, "Shuffle", isShuffleOn, trayActiveBtn, trayInactiveBtn, trayActiveCt, trayInactiveCt, onToggleShuffle, haptic)
                val loopActive = repeatMode != PlayerSharedViewModel.REPEAT_OFF; val loopIcon = if (repeatMode == PlayerSharedViewModel.REPEAT_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                TrayBtn(loopIcon, "Repeat", loopActive, trayActiveBtn, trayInactiveBtn, trayActiveCt, trayInactiveCt, onToggleLoopMode, haptic)
                val timerActive = sleepTimerMode != PlayerSharedViewModel.SleepTimerMode.OFF
                TrayBtn(R.drawable.ic_timer, "Timer", timerActive, trayActiveBtn, trayInactiveBtn, trayActiveCt, trayInactiveCt, onShowSleepTimerDialog, haptic)
            }
        }
    }
}

@Composable
private fun RowScope.TrayBtn(iconRes: Int, desc: String, active: Boolean, activeBg: Color, inactiveBg: Color, activeCt: Color, inactiveCt: Color, onClick: () -> Unit, haptic: HapticFeedback) {
    val src = remember { MutableInteractionSource() }; val pressed by src.collectIsPressedAsState()
    val corner by animateDpAsState(if (pressed) 12.dp else 28.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "tc")
    Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }, modifier = Modifier.weight(1f).fillMaxHeight(), interactionSource = src, shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.buttonColors(containerColor = if (active) activeBg else inactiveBg, contentColor = if (active) activeCt else inactiveCt), contentPadding = PaddingValues(0.dp)
    ) { Icon(painterResource(iconRes), desc, Modifier.size(22.dp)) }
}

@Composable
fun QueueSongCard(song: SongItem, isCurrent: Boolean, isPlaying: Boolean, onTap: () -> Unit, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme; val density = LocalDensity.current; val haptic = LocalHapticFeedback.current
    val corner by animateDpAsState(if (isCurrent) 28.dp else 20.dp, tween(350, easing = FastOutSlowInEasing), label = "qcr")
    val albCorner by animateDpAsState(if (isCurrent) 26.dp else 12.dp, tween(350, easing = FastOutSlowInEasing), label = "qac")
    val bg by animateColorAsState(if (isCurrent) scheme.primaryContainer.copy(alpha = 0.25f) else scheme.surfaceContainerLowest, tween(300), label = "qbg")
    val tc by animateColorAsState(if (isCurrent) scheme.primary else scheme.onSurface, tween(300), label = "qtc")

    val swipeOff = remember { Animatable(0f) }; var itemW by remember { mutableStateOf(0f) }
    val dismissScope = rememberCoroutineScope(); val threshold = with(density) { 100.dp.toPx() }

    Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp).onGloballyPositioned { itemW = it.size.width.toFloat() }) {
        if ((-swipeOff.value).coerceAtLeast(0f) > 0f) {
            Box(Modifier.fillMaxSize().padding(end = 10.dp).clip(RoundedCornerShape(corner)).background(scheme.errorContainer), contentAlignment = Alignment.CenterEnd) {
                Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Remove", tint = scheme.onErrorContainer, modifier = Modifier.padding(end = 14.dp).size(22.dp))
            }
        }
        Surface(
            Modifier.graphicsLayer { translationX = swipeOff.value }.clip(RoundedCornerShape(corner))
                .pointerInput(Unit) { detectHorizontalDragGestures(onDragStart = {}, onHorizontalDrag = { c, d -> c.consume(); dismissScope.launch { swipeOff.snapTo((swipeOff.value + d).coerceIn(-itemW, 0f)) } }, onDragEnd = { dismissScope.launch { if (-swipeOff.value > threshold) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); swipeOff.animateTo(-itemW, tween(180)); onRemove() } else swipeOff.animateTo(0f, spring()) } }, onDragCancel = { dismissScope.launch { swipeOff.animateTo(0f, spring()) } }) }.clickable { onTap() },
            RoundedCornerShape(corner), bg, tonalElevation = 0.dp
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(albCorner)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) { AsyncImage(song.getAlbumArtUri(), song.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                Column(Modifier.weight(1f)) {
                    Text(text = song.title, style = MaterialTheme.typography.titleSmall, color = tc, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                    Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = if (isCurrent) scheme.primary.copy(alpha = 0.75f) else scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isCurrent && isPlaying) Box(Modifier.size(5.dp).clip(CircleShape).background(scheme.primary))
            }
        }
    }
}

