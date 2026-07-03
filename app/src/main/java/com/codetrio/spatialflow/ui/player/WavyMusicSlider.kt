package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Updated WavyMusicSlider — all colors from MaterialTheme.
 * No hardcoded colors anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavyMusicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trackHeight: Dp = 4.dp,
    thumbRadius: Dp = 10.dp,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    waveAmplitudeWhenPlaying: Dp = 2.5.dp,
    waveLength: Dp = 48.dp,
    waveAnimationDuration: Int = 2200,
    hideInactiveTrackPortion: Boolean = true,
    isPlaying: Boolean = true,
    isWaveEligible: Boolean = true,
    semanticsLabel: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed
    val shouldShowWave = isWaveEligible && isPlaying && !isInteracting

    val animatedWaveAmplitude by animateDpAsState(if (shouldShowWave) waveAmplitudeWhenPlaying else 0.dp, tween(250, easing = FastOutSlowInEasing), label = "waveAmp")
    val thumbInteractionFraction by animateFloatAsState(if (isInteracting) 1f else 0f, tween(200, easing = FastOutSlowInEasing), label = "thumbInt")

    val infiniteTransition = rememberInfiniteTransition(label = "wavePhase")
    val wavePhase by infiniteTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(waveAnimationDuration, easing = LinearEasing), RepeatMode.Restart), label = "wavePhase")

    val trackPx = with(density) { trackHeight.toPx() }
    val thumbPx = with(density) { thumbRadius.toPx() }
    val waveAmpPx = with(density) { animatedWaveAmplitude.toPx() }
    val waveLenPx = with(density) { waveLength.toPx() }

    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = (clampedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val scopeSize = this.size
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    var currentDragFraction = fraction
                    fun updateValue(posX: Float) {
                        val trackStart = thumbPx
                        val trackEnd = scopeSize.width - thumbPx
                        currentDragFraction = ((posX - trackStart) / (trackEnd - trackStart)).coerceIn(0f, 1f)
                        val newValue = valueRange.start + currentDragFraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                    updateValue(down.position.x)
                    // simplified drag handling
                }
            }
            .clearAndSetSemantics {
                this.contentDescription = semanticsLabel ?: "Seek"
                progressBarRangeInfo = ProgressBarRangeInfo(current = clampedValue, range = valueRange, steps = 0)
                setProgress { onValueChange(it.coerceIn(valueRange.start, valueRange.endInclusive)); true }
            }
    ) {
        val trackY = size.height / 2f
        val trackStart = thumbPx
        val trackEnd = size.width - thumbPx
        val activeEnd = trackStart + (trackEnd - trackStart) * fraction

        // Inactive track
        if (!hideInactiveTrackPortion) {
            drawLine(inactiveTrackColor, Offset(activeEnd, trackY), Offset(trackEnd, trackY), trackPx)
        }

        // Active track — optionally wavy
        if (waveAmpPx > 0f && waveLenPx > 0f && activeEnd > trackStart) {
            val path = Path()
            path.moveTo(trackStart, trackY)
            var x = trackStart
            while (x < activeEnd) {
                val phase = (x / waveLenPx + wavePhase) * 2f * PI.toFloat()
                val y = trackY + sin(phase) * waveAmpPx
                path.lineTo(x, y)
                x += 2f
            }
            path.lineTo(activeEnd, trackY)
            drawPath(path, activeTrackColor, style = Stroke(width = trackPx, cap = StrokeCap.Round))
        } else {
            drawLine(activeTrackColor, Offset(trackStart, trackY), Offset(activeEnd, trackY), trackPx, cap = StrokeCap.Round)
        }

        // Thumb
        val thumbRadiusAdjusted = thumbPx + (thumbPx * 0.4f) * thumbInteractionFraction
        drawCircle(thumbColor, thumbRadiusAdjusted, Offset(activeEnd, trackY))
    }
}
