package com.codetrio.spatialflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Central shape system. Every component MUST use these shapes
 * or CardCorners (for AbsoluteSmoothCornerShape cards).
 * NEVER hardcode radii in individual composables.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Card shape constants for AbsoluteSmoothCornerShape (squircle corners).
 * Smoothness = 60 means 60% continuous curve — eliminates the visual
 * "kink" where straight edges meet circular arcs.
 */
object CardCorners {
    const val RadiusDp = 28f
    const val Smoothness = 60
}

val PlayerSheetCollapsedCornerRadius = 32.dp
