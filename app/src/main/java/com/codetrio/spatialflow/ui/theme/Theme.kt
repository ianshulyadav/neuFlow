package com.codetrio.spatialflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.ContextWrapper
import android.os.Build

val LocalAlbumColorScheme = staticCompositionLocalOf<ColorSchemePair?> { null }
val LocalSpatialFlowDarkTheme = staticCompositionLocalOf { false }

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * ROOT THEME — Single entry point for all screens.
 *
 * Features:
 * - Album color scheme crossfade (500ms tween when tracks change)
 * - System dynamic colors on Android 12+
 * - AMOLED black override
 * - Transparent status/nav bars with auto light/dark icons
 */
@Composable
fun SpatialFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    albumColorSchemePair: ColorSchemePair? = null,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val baseScheme = remember(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    val targetScheme = remember(baseScheme, albumColorSchemePair, darkTheme, amoledBlack) {
        val s = albumColorSchemePair?.let { if (darkTheme) it.dark else it.light } ?: baseScheme
        if (darkTheme && amoledBlack) s.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black, surfaceContainer = Color.Black)
        else s
    }

    Crossfade(targetState = targetScheme, animationSpec = tween(500), label = "schemeCrossfade") { currentScheme ->
        SideEffect {
            view.context.findActivity()?.window?.let { w ->
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    w.isStatusBarContrastEnforced = false; w.isNavigationBarContrastEnforced = false
                }
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = ColorUtils.calculateLuminance(currentScheme.background.toArgb()) > 0.55
                    isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(currentScheme.background.toArgb()) > 0.55
                }
            }
        }

        CompositionLocalProvider(
            LocalAlbumColorScheme provides albumColorSchemePair,
            LocalSpatialFlowDarkTheme provides darkTheme,
        ) {
            MaterialTheme(colorScheme = currentScheme, typography = Typography, shapes = AppShapes, content = content)
        }
    }
}
