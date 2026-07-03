package com.codetrio.spatialflow.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.ContextWrapper
import android.os.Build

import android.content.SharedPreferences

val LocalAlbumColorScheme = staticCompositionLocalOf<ColorSchemePair?> { null }
val LocalSpatialFlowDarkTheme = staticCompositionLocalOf { false }

@Composable
fun <T> SharedPreferences.observeKey(key: String, defaultValue: T): State<T> {
    val state = remember { mutableStateOf(defaultValue) }
    DisposableEffect(this, key) {
        state.value = when (defaultValue) {
            is Boolean -> getBoolean(key, defaultValue) as T
            is String -> getString(key, defaultValue) as T
            is Float -> getFloat(key, defaultValue) as T
            is Int -> getInt(key, defaultValue) as T
            is Long -> getLong(key, defaultValue) as T
            else -> defaultValue
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                state.value = when (defaultValue) {
                    is Boolean -> prefs.getBoolean(key, defaultValue) as T
                    is String -> prefs.getString(key, defaultValue) as T
                    is Float -> prefs.getFloat(key, defaultValue) as T
                    is Int -> prefs.getInt(key, defaultValue) as T
                    is Long -> prefs.getLong(key, defaultValue) as T
                    else -> defaultValue
                }
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return state
}

private tailrec fun android.content.Context.findActivity(): Activity? = when(this){ is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }

@Composable
fun SpatialFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    albumColorSchemePair: ColorSchemePair? = null,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current; val view = LocalView.current
    val baseScheme = remember(darkTheme) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { if(darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
        else { if(darkTheme) darkColorScheme() else lightColorScheme() }}

    val targetScheme = remember(baseScheme, albumColorSchemePair, darkTheme, amoledBlack) {
        val s = albumColorSchemePair?.let { if(darkTheme) it.dark else it.light } ?: baseScheme
        if(darkTheme && amoledBlack) s.copy(background=Color.Black, surface=Color.Black, surfaceContainerLowest=Color.Black, surfaceContainerLow=Color.Black, surfaceContainer=Color.Black) else s}

    Crossfade(targetState=targetScheme, animationSpec=tween(500), label="schemeXfade") { currentScheme ->
        SideEffect {
            view.context.findActivity()?.window?.let { w ->
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { w.isStatusBarContrastEnforced = false; w.isNavigationBarContrastEnforced = false }
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(w, view).apply {
                    isAppearanceLightStatusBars = ColorUtils.calculateLuminance(currentScheme.background.toArgb()) > 0.55
                    isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(currentScheme.background.toArgb()) > 0.55}}}

        CompositionLocalProvider(LocalAlbumColorScheme provides albumColorSchemePair, LocalSpatialFlowDarkTheme provides darkTheme) {
            MaterialTheme(colorScheme=currentScheme, typography=Typography, shapes=AppShapes, content=content)}}}
