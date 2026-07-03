package com.codetrio.spatialflow.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.codetrio.spatialflow.data.cache.CachedColorScheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ColorSchemeCache {
    private val memoryCache = LruCache<String, ColorSchemePair>(30)
    private val mutex = Mutex()
    private val inProgress = mutableSetOf<String>()
    private val gson = Gson()

    fun get(key: String): ColorSchemePair? = memoryCache.get(key)
    fun put(key: String, v: ColorSchemePair) { memoryCache.put(key, v) }
    fun evictAll() = memoryCache.evictAll()
    suspend fun markInProgress(uri: String): Boolean = mutex.withLock {
        if (inProgress.contains(uri)) false else { inProgress.add(uri); true }
    }
    suspend fun markComplete(uri: String) = mutex.withLock { inProgress.remove(uri) }
    suspend fun loadBitmapForExtraction(ctx: Context, uri: Any): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = ImageRequest.Builder(ctx).data(uri).allowHardware(false)
                .size(Size(128,128)).bitmapConfig(Bitmap.Config.ARGB_8888).build()
            val d = ctx.imageLoader.execute(req).drawable ?: return@withContext null
            Bitmap.createBitmap(
                d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).also { b -> Canvas(b).let { c -> d.setBounds(0,0,c.width,c.height); d.draw(c) } }
        } catch (_: Exception) { null }
    }

    fun serializeColorSchemePair(uri: String, style: String, pair: ColorSchemePair): CachedColorScheme {
        val lightMap = colorSchemeToMap(pair.light)
        val darkMap = colorSchemeToMap(pair.dark)
        return CachedColorScheme(
            uri = uri,
            paletteStyle = style,
            lightSchemeJson = gson.toJson(lightMap),
            darkSchemeJson = gson.toJson(darkMap)
        )
    }

    fun deserializeColorSchemePair(cached: CachedColorScheme): ColorSchemePair {
        val lightType = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type
        val lightMap: Map<String, Int> = gson.fromJson(cached.lightSchemeJson, lightType)
        val darkMap: Map<String, Int> = gson.fromJson(cached.darkSchemeJson, lightType)
        return ColorSchemePair(
            light = mapToColorScheme(lightMap, false),
            dark = mapToColorScheme(darkMap, true)
        )
    }

    private fun colorSchemeToMap(scheme: ColorScheme): Map<String, Int> {
        return mapOf(
            "primary" to scheme.primary.toArgb(),
            "onPrimary" to scheme.onPrimary.toArgb(),
            "primaryContainer" to scheme.primaryContainer.toArgb(),
            "onPrimaryContainer" to scheme.onPrimaryContainer.toArgb(),
            "inversePrimary" to scheme.inversePrimary.toArgb(),
            "secondary" to scheme.secondary.toArgb(),
            "onSecondary" to scheme.onSecondary.toArgb(),
            "secondaryContainer" to scheme.secondaryContainer.toArgb(),
            "onSecondaryContainer" to scheme.onSecondaryContainer.toArgb(),
            "tertiary" to scheme.tertiary.toArgb(),
            "onTertiary" to scheme.onTertiary.toArgb(),
            "tertiaryContainer" to scheme.tertiaryContainer.toArgb(),
            "onTertiaryContainer" to scheme.onTertiaryContainer.toArgb(),
            "background" to scheme.background.toArgb(),
            "onBackground" to scheme.onBackground.toArgb(),
            "surface" to scheme.surface.toArgb(),
            "onSurface" to scheme.onSurface.toArgb(),
            "surfaceVariant" to scheme.surfaceVariant.toArgb(),
            "onSurfaceVariant" to scheme.onSurfaceVariant.toArgb(),
            "surfaceTint" to scheme.surfaceTint.toArgb(),
            "inverseSurface" to scheme.inverseSurface.toArgb(),
            "inverseOnSurface" to scheme.inverseOnSurface.toArgb(),
            "error" to scheme.error.toArgb(),
            "onError" to scheme.onError.toArgb(),
            "errorContainer" to scheme.errorContainer.toArgb(),
            "onErrorContainer" to scheme.onErrorContainer.toArgb(),
            "outline" to scheme.outline.toArgb(),
            "outlineVariant" to scheme.outlineVariant.toArgb(),
            "scrim" to scheme.scrim.toArgb(),
            "surfaceBright" to scheme.surfaceBright.toArgb(),
            "surfaceDim" to scheme.surfaceDim.toArgb(),
            "surfaceContainerLowest" to scheme.surfaceContainerLowest.toArgb(),
            "surfaceContainerLow" to scheme.surfaceContainerLow.toArgb(),
            "surfaceContainer" to scheme.surfaceContainer.toArgb(),
            "surfaceContainerHigh" to scheme.surfaceContainerHigh.toArgb(),
            "surfaceContainerHighest" to scheme.surfaceContainerHighest.toArgb()
        )
    }

    private fun mapToColorScheme(map: Map<String, Int>, isDark: Boolean): ColorScheme {
        val fallback = if (isDark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
        fun getColor(key: String, default: Color): Color {
            return map[key]?.let { Color(it) } ?: default
        }
        return ColorScheme(
            primary = getColor("primary", fallback.primary),
            onPrimary = getColor("onPrimary", fallback.onPrimary),
            primaryContainer = getColor("primaryContainer", fallback.primaryContainer),
            onPrimaryContainer = getColor("onPrimaryContainer", fallback.onPrimaryContainer),
            inversePrimary = getColor("inversePrimary", fallback.inversePrimary),
            secondary = getColor("secondary", fallback.secondary),
            onSecondary = getColor("onSecondary", fallback.onSecondary),
            secondaryContainer = getColor("secondaryContainer", fallback.secondaryContainer),
            onSecondaryContainer = getColor("onSecondaryContainer", fallback.onSecondaryContainer),
            tertiary = getColor("tertiary", fallback.tertiary),
            onTertiary = getColor("onTertiary", fallback.onTertiary),
            tertiaryContainer = getColor("tertiaryContainer", fallback.tertiaryContainer),
            onTertiaryContainer = getColor("onTertiaryContainer", fallback.onTertiaryContainer),
            background = getColor("background", fallback.background),
            onBackground = getColor("onBackground", fallback.onBackground),
            surface = getColor("surface", fallback.surface),
            onSurface = getColor("onSurface", fallback.onSurface),
            surfaceVariant = getColor("surfaceVariant", fallback.surfaceVariant),
            onSurfaceVariant = getColor("onSurfaceVariant", fallback.onSurfaceVariant),
            surfaceTint = getColor("surfaceTint", fallback.surfaceTint),
            inverseSurface = getColor("inverseSurface", fallback.inverseSurface),
            inverseOnSurface = getColor("inverseOnSurface", fallback.inverseOnSurface),
            error = getColor("error", fallback.error),
            onError = getColor("onError", fallback.onError),
            errorContainer = getColor("errorContainer", fallback.errorContainer),
            onErrorContainer = getColor("onErrorContainer", fallback.onErrorContainer),
            outline = getColor("outline", fallback.outline),
            outlineVariant = getColor("outlineVariant", fallback.outlineVariant),
            scrim = getColor("scrim", fallback.scrim),
            surfaceBright = getColor("surfaceBright", fallback.surfaceBright),
            surfaceDim = getColor("surfaceDim", fallback.surfaceDim),
            surfaceContainerLowest = getColor("surfaceContainerLowest", fallback.surfaceContainerLowest),
            surfaceContainerLow = getColor("surfaceContainerLow", fallback.surfaceContainerLow),
            surfaceContainer = getColor("surfaceContainer", fallback.surfaceContainer),
            surfaceContainerHigh = getColor("surfaceContainerHigh", fallback.surfaceContainerHigh),
            surfaceContainerHighest = getColor("surfaceContainerHighest", fallback.surfaceContainerHighest)
        )
    }
}
