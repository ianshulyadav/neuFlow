package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.codetrio.spatialflow.R
import com.codetrio.spatialflow.model.SongItem
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun SlidingQueueDrawer(
    isQueueExpanded: Boolean,
    onQueueExpandedChange: (Boolean) -> Unit,
    songList: List<SongItem>,
    currentSongIndex: Int,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    sleepTimerMode: PlayerSharedViewModel.SleepTimerMode,
    onReorderQueue: (Int, Int) -> Unit,
    onPlaySongAtIndex: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLoopMode: () -> Unit,
    onShowSleepTimerDialog: () -> Unit,
    playerBackgroundColor: Int,
    dynamicAccentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val slidingOffset by animateDpAsState(
        targetValue = if (isQueueExpanded) 0.dp else screenHeight + 100.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "QueueSlidingOffset"
    )

    val queueCornerRadius by animateDpAsState(
        targetValue = if (isQueueExpanded) 0.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "QueueCornerRadius"
    )
    val safeCornerRadius = queueCornerRadius.coerceAtLeast(0.dp)

    val queueBgColor = deriveArtworkSurfaceColor(
        sourceColor = Color(playerBackgroundColor),
        isDark = isDark,
        darkLightness = 0.155f,
        lightLightness = 0.835f,
        darkSaturationRange = 0.32f..0.54f,
        lightSaturationRange = 0.30f..0.48f
    )
    val queueTrayBackgroundColor = remember(playerBackgroundColor, isDark) {
        deriveArtworkSurfaceColor(
            sourceColor = Color(playerBackgroundColor),
            isDark = isDark,
            darkLightness = 0.24f,
            lightLightness = 0.73f,
            darkSaturationRange = 0.30f..0.60f,
            lightSaturationRange = 0.24f..0.50f
        )
    }
    val queueTrayInactiveButtonColor = remember(queueTrayBackgroundColor, isDark) {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(queueTrayBackgroundColor.toArgb(), hsl)
        if (hsl[1] < 0.08f) {
            hsl[1] = 0f
        } else {
            hsl[1] = hsl[1].coerceIn(0.24f, 0.55f)
        }
        if (isDark) {
            hsl[2] = 0.33f
        } else {
            hsl[2] = 0.64f
        }
        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
    val queueTrayActiveButtonColor = remember(dynamicAccentColor, isDark) {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(dynamicAccentColor.toArgb(), hsl)
        if (hsl[1] < 0.08f) {
            if (isDark) Color(0xFFE8E8EA) else Color(0xFF1F1E23)
        } else {
            hsl[1] = hsl[1].coerceAtLeast(0.45f)
            hsl[2] = if (isDark) 0.62f else 0.42f
            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        }
    }
    val queueTrayInactiveContentColor = remember(queueTrayInactiveButtonColor) {
        if (androidx.core.graphics.ColorUtils.calculateLuminance(queueTrayInactiveButtonColor.toArgb()) > 0.5) {
            Color(0xFF1C1B1F)
        } else {
            Color.White
        }
    }
    val queueTrayActiveContentColor = remember(queueTrayActiveButtonColor) {
        if (androidx.core.graphics.ColorUtils.calculateLuminance(queueTrayActiveButtonColor.toArgb()) > 0.5) {
            Color(0xFF1C1B1F)
        } else {
            Color.White
        }
    }

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .offset { androidx.compose.ui.unit.IntOffset(0, slidingOffset.roundToPx()) },
        shape = RoundedCornerShape(topStart = safeCornerRadius, topEnd = safeCornerRadius),
        color = queueBgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val boxScope = this
            val lazyListState = rememberLazyListState()
            
            val dragDropState = rememberDragDropState(lazyListState = lazyListState) { from, to ->
                onReorderQueue(from, to)
            }

            // Scroll active track into view on first open
            LaunchedEffect(isQueueExpanded) {
                if (isQueueExpanded && currentSongIndex in songList.indices) {
                    val distance = abs(lazyListState.firstVisibleItemIndex - currentSongIndex)
                    if (distance > 24) {
                        lazyListState.scrollToItem(currentSongIndex)
                    } else {
                        lazyListState.animateScrollToItem(currentSongIndex)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Strip Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Side Grouping
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onQueueExpandedChange(false) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = "Collapse Queue",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            Column {
                                Text(
                                    text = "Playing From",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "QUEUE",
                                    style = MaterialTheme.typography.titleSmallEmphasized,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Right Side
                        Text(
                            text = "${songList.size} tracks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // Queue List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                ) {
                    itemsIndexed(
                        items = songList,
                        key = { index, song -> "${song.id}_$index" },
                        contentType = { _, _ -> "queue-song" }
                    ) { index, song ->
                        val isPlaying = (index == currentSongIndex)
                        val shapes = ListItemDefaults.segmentedShapes(index = index, count = songList.size)
                        val isDragging = index == dragDropState.currentIndexOfDraggedItem
                        val displacement = if (isDragging) dragDropState.elementDisplacement ?: 0f else 0f

                        Box(
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = displacement
                                    if (isDragging) {
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                        ) {
                            QueueListItem(
                                song = song,
                                isPlaying = isPlaying,
                                shapes = shapes,
                                showReorderControls = true,
                                dragDropState = if (isQueueExpanded) dragDropState else null,
                                index = index,
                                onMoveUp = {
                                    if (index > 0) {
                                        onReorderQueue(index, index - 1)
                                    }
                                },
                                onMoveDown = {
                                    if (index < songList.size - 1) {
                                        onReorderQueue(index, index + 1)
                                    }
                                },
                                onClick = {
                                    onPlaySongAtIndex(index)
                                }
                            )
                        }
                    }
                }
            } // Column (header + list)

            // Connected ButtonGroup tray with curved top clip
            Surface(
                modifier = with(boxScope) {
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = queueTrayBackgroundColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 16.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        expandedRatio = 0.3f,
                        overflowIndicator = {}
                    ) {
                        val scope = this

                        // 1. Shuffle Button
                        customItem(
                            buttonGroupContent = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val cornerRadius by animateDpAsState(
                                    targetValue = if (isPressed) 12.dp else 28.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "ShuffleCorner"
                                )
                                androidx.compose.material3.Button(
                                    onClick = {
                                        onToggleShuffle()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = with(scope) {
                                        Modifier
                                            .animateWidth(interactionSource)
                                            .weight(1f)
                                            .height(56.dp)
                                    },
                                    interactionSource = interactionSource,
                                    shape = RoundedCornerShape(cornerRadius),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (isShuffleEnabled) queueTrayActiveButtonColor else queueTrayInactiveButtonColor,
                                        contentColor = if (isShuffleEnabled) queueTrayActiveContentColor else queueTrayInactiveContentColor
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_shuffle),
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            menuContent = {}
                        )

                        // 2. Loop Button
                        customItem(
                            buttonGroupContent = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val cornerRadius by animateDpAsState(
                                    targetValue = if (isPressed) 12.dp else 28.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "LoopCorner"
                                )
                                val loopIcon = if (repeatMode == PlayerSharedViewModel.REPEAT_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat
                                val loopActive = repeatMode != PlayerSharedViewModel.REPEAT_OFF
                                androidx.compose.material3.Button(
                                    onClick = {
                                        onToggleLoopMode()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = with(scope) {
                                        Modifier
                                            .animateWidth(interactionSource)
                                            .weight(1f)
                                            .height(56.dp)
                                    },
                                    interactionSource = interactionSource,
                                    shape = RoundedCornerShape(cornerRadius),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (loopActive) queueTrayActiveButtonColor else queueTrayInactiveButtonColor,
                                        contentColor = if (loopActive) queueTrayActiveContentColor else queueTrayInactiveContentColor
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = loopIcon),
                                        contentDescription = "Repeat Mode",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            menuContent = {}
                        )

                        // 3. Sleep Timer Button
                        customItem(
                            buttonGroupContent = {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                val cornerRadius by animateDpAsState(
                                    targetValue = if (isPressed) 12.dp else 28.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "TimerCorner"
                                )
                                val timerActive = sleepTimerMode != PlayerSharedViewModel.SleepTimerMode.OFF
                                androidx.compose.material3.Button(
                                    onClick = {
                                        onShowSleepTimerDialog()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    modifier = with(scope) {
                                        Modifier
                                            .animateWidth(interactionSource)
                                            .weight(1f)
                                            .height(56.dp)
                                    },
                                    interactionSource = interactionSource,
                                    shape = RoundedCornerShape(cornerRadius),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = if (timerActive) queueTrayActiveButtonColor else queueTrayInactiveButtonColor,
                                        contentColor = if (timerActive) queueTrayActiveContentColor else queueTrayInactiveContentColor
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_timer),
                                        contentDescription = "Sleep Timer",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            menuContent = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueListItem(
    song: SongItem,
    isPlaying: Boolean,
    shapes: ListItemShapes,
    showReorderControls: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    dragDropState: DragDropState? = null,
    index: Int = -1,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            val albumArtModel = remember(song.videoId) { song.getAlbumArtUri() ?: R.drawable.default_album_art }
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = albumArtModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        content = {
            Text(
                text = song.title,
                fontWeight = if (isPlaying) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (showReorderControls) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move Up") },
                            onClick = {
                                showMenu = false
                                onMoveUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Down") },
                            onClick = {
                                showMenu = false
                                onMoveDown()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag to reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .pointerInput(dragDropState) {
                                if (dragDropState == null || index < 0) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val item = dragDropState.lazyListState.layoutInfo.visibleItemsInfo.find { it.index == index }
                                        if (item != null) {
                                            dragDropState.onDragStart(Offset(offset.x, offset.y + item.offset))
                                        }
                                    },
                                    onDragEnd = { dragDropState.onDragInterrupted() },
                                    onDragCancel = { dragDropState.onDragInterrupted() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDropState.onDrag(dragAmount)
                                    }
                                )
                            }
                    )
                }
            }
        },
        shapes = shapes,
        colors = ListItemDefaults.colors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                Color.Transparent
            }
        )
    )
}

class DragDropState(
    val lazyListState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedDistance by mutableFloatStateOf(0f)
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
    private var autoScrollJob: Job? = null
    private var autoScrollDeltaPx: Float = 0f
    private var lastTargetIndex: Int? = null

    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem?.let { currentIndex ->
            lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == currentIndex }
                ?.let { item ->
                    (initiallyDraggedElement?.offset ?: 0).toFloat() + draggedDistance - item.offset
                }
        }

    fun onDragStart(offset: Offset) {
        stopAutoScroll()
        lastTargetIndex = null
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                initiallyDraggedElement = it
                currentIndexOfDraggedItem = it.index
            }
    }

    fun onDragInterrupted() {
        initiallyDraggedElement = null
        currentIndexOfDraggedItem = null
        draggedDistance = 0f
        lastTargetIndex = null
        stopAutoScroll()
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y

        val initialOffset = initiallyDraggedElement?.offset ?: return
        val currentOffset = initialOffset + draggedDistance
        val size = initiallyDraggedElement?.size ?: return
        
        val currentCenter = currentOffset + size / 2f

        val currentIndex = currentIndexOfDraggedItem ?: return
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        
        // Nearest item detection
        val targetItem = visibleItems.minByOrNull { item ->
            kotlin.math.abs((item.offset + item.size / 2f) - currentCenter)
        }

        if (
            targetItem != null && 
            targetItem.index != currentIndex && 
            targetItem.index != lastTargetIndex
        ) {
            lastTargetIndex = targetItem.index

            onMove(currentIndex, targetItem.index)
            currentIndexOfDraggedItem = targetItem.index

            // Re-anchor Dragged Item
            initiallyDraggedElement = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == targetItem.index }
            draggedDistance = 0f
        }
        
        // Use exact visible window limits to prevent jarring scrolling
        val viewportStart = lazyListState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = lazyListState.layoutInfo.viewportEndOffset.toFloat()
        val overscrollThreshold = 80f

        // Smoother delta values
        val scrollDelta = when {
            currentOffset < viewportStart + overscrollThreshold -> -20f
            currentOffset + size > viewportEnd - overscrollThreshold -> 20f
            else -> 0f
        }
        updateAutoScroll(scrollDelta)
    }

    private fun updateAutoScroll(scrollDeltaPx: Float) {
        if (scrollDeltaPx == 0f) {
            stopAutoScroll()
            return
        }
        autoScrollDeltaPx = scrollDeltaPx
        if (autoScrollJob?.isActive == true) return

        autoScrollJob = scope.launch {
            while (currentIndexOfDraggedItem != null) {
                val delta = autoScrollDeltaPx
                if (delta == 0f) break
                lazyListState.scrollBy(delta)
                delay(16)
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollDeltaPx = 0f
        autoScrollJob?.cancel()
        autoScrollJob = null
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        DragDropState(lazyListState, scope, onMove)
    }
    return state
}

fun Modifier.dragContainer(dragDropState: DragDropState, enabled: Boolean): Modifier {
    if (!enabled) return this
    return this.then(
        Modifier.pointerInput(dragDropState) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                onDragEnd = { dragDropState.onDragInterrupted() },
                onDragCancel = { dragDropState.onDragInterrupted() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(dragAmount)
                }
            )
        }
    )
}
