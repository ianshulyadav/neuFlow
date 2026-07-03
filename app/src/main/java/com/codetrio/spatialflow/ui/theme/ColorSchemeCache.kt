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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ColorSchemeData(
    val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
    val inversePrimary: Int, val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
    val tertiary: Int, val onTertiary: Int, val tertiaryContainer: Int, val onTertiaryContainer: Int,
    val background: Int, val onBackground: Int, val surface: Int, val onSurface: Int,
    val surfaceVariant: Int, val onSurfaceVariant: Int, val surfaceTint: Int,
    val inverseSurface: Int, val inverseOnSurface: Int, val error: Int, val onError: Int,
    val errorContainer: Int, val onErrorContainer: Int, val outline: Int, val outlineVariant: Int, val scrim: Int,
    val surfaceBright: Int, val surfaceDim: Int, val surfaceContainer: Int, val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int, val surfaceContainerLow: Int, val surfaceContainerLowest: Int,
    val primaryFixed: Int, val primaryFixedDim: Int, val onPrimaryFixed: Int, val onPrimaryFixedVariant: Int,
    val secondaryFixed: Int, val secondaryFixedDim: Int, val onSecondaryFixed: Int, val onSecondaryFixedVariant: Int,
    val tertiaryFixed: Int, val tertiaryFixedDim: Int, val onTertiaryFixed: Int, val onTertiaryFixedVariant: Int
)

object ColorSchemeCache {
    private val memoryCache = LruCache<String, ColorSchemePair>(30)
    private val mutex = Mutex()
    private val inProgress = mutableSetOf<String>()

    fun get(key: String): ColorSchemePair? = memoryCache.get(key)
    fun put(key: String, value: ColorSchemePair) { memoryCache.put(key, value) }
    fun evictAll() = memoryCache.evictAll()
    suspend fun markInProgress(uri: String): Boolean = mutex.withLock { if (inProgress.contains(uri)) false else { inProgress.add(uri); true } }
    suspend fun markComplete(uri: String) = mutex.withLock { inProgress.remove(uri) }

    suspend fun loadBitmapForExtraction(context: Context, uri: Any): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context).data(uri).allowHardware(false).size(Size(128, 128)).bitmapConfig(Bitmap.Config.ARGB_8888).build()
            val d = context.imageLoader.execute(request).drawable ?: return@withContext null
            Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1), d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { b -> Canvas(b).let { c -> d.setBounds(0, 0, c.width, c.height); d.draw(c) } }
        } catch (_: Exception) { null }
    }

    fun serializeColorScheme(scheme: ColorScheme): String {
        val data = ColorSchemeData(
            primary = scheme.primary.toArgb(),
            onPrimary = scheme.onPrimary.toArgb(),
            primaryContainer = scheme.primaryContainer.toArgb(),
            onPrimaryContainer = scheme.onPrimaryContainer.toArgb(),
            inversePrimary = scheme.inversePrimary.toArgb(),
            secondary = scheme.secondary.toArgb(),
            onSecondary = scheme.onSecondary.toArgb(),
            secondaryContainer = scheme.secondaryContainer.toArgb(),
            onSecondaryContainer = scheme.onSecondaryContainer.toArgb(),
            tertiary = scheme.tertiary.toArgb(),
            onTertiary = scheme.onTertiary.toArgb(),
            tertiaryContainer = scheme.tertiaryContainer.toArgb(),
            onTertiaryContainer = scheme.onTertiaryContainer.toArgb(),
            background = scheme.background.toArgb(),
            onBackground = scheme.onBackground.toArgb(),
            surface = scheme.surface.toArgb(),
            onSurface = scheme.onSurface.toArgb(),
            surfaceVariant = scheme.surfaceVariant.toArgb(),
            onSurfaceVariant = scheme.onSurfaceVariant.toArgb(),
            surfaceTint = scheme.surfaceTint.toArgb(),
            inverseSurface = scheme.inverseSurface.toArgb(),
            inverseOnSurface = scheme.inverseOnSurface.toArgb(),
            error = scheme.error.toArgb(),
            onError = scheme.onError.toArgb(),
            errorContainer = scheme.errorContainer.toArgb(),
            onErrorContainer = scheme.onErrorContainer.toArgb(),
            outline = scheme.outline.toArgb(),
            outlineVariant = scheme.outlineVariant.toArgb(),
            scrim = scheme.scrim.toArgb(),
            surfaceBright = scheme.surfaceBright.toArgb(),
            surfaceDim = scheme.surfaceDim.toArgb(),
            surfaceContainer = scheme.surfaceContainer.toArgb(),
            surfaceContainerHigh = scheme.surfaceContainerHigh.toArgb(),
            surfaceContainerHighest = scheme.surfaceContainerHighest.toArgb(),
            surfaceContainerLow = scheme.surfaceContainerLow.toArgb(),
            surfaceContainerLowest = scheme.surfaceContainerLowest.toArgb(),
            primaryFixed = scheme.primaryFixed.toArgb(),
            primaryFixedDim = scheme.primaryFixedDim.toArgb(),
            onPrimaryFixed = scheme.onPrimaryFixed.toArgb(),
            onPrimaryFixedVariant = scheme.onPrimaryFixedVariant.toArgb(),
            secondaryFixed = scheme.secondaryFixed.toArgb(),
            secondaryFixedDim = scheme.secondaryFixedDim.toArgb(),
            onSecondaryFixed = scheme.onSecondaryFixed.toArgb(),
            onSecondaryFixedVariant = scheme.onSecondaryFixedVariant.toArgb(),
            tertiaryFixed = scheme.tertiaryFixed.toArgb(),
            tertiaryFixedDim = scheme.tertiaryFixedDim.toArgb(),
            onTertiaryFixed = scheme.onTertiaryFixed.toArgb(),
            onTertiaryFixedVariant = scheme.onTertiaryFixedVariant.toArgb()
        )
        return com.google.gson.Gson().toJson(data)
    }

    fun deserializeColorScheme(json: String): ColorScheme {
        val data = com.google.gson.Gson().fromJson(json, ColorSchemeData::class.java)
        return ColorScheme(
            primary = Color(data.primary),
            onPrimary = Color(data.onPrimary),
            primaryContainer = Color(data.primaryContainer),
            onPrimaryContainer = Color(data.onPrimaryContainer),
            inversePrimary = Color(data.inversePrimary),
            secondary = Color(data.secondary),
            onSecondary = Color(data.onSecondary),
            secondaryContainer = Color(data.secondaryContainer),
            onSecondaryContainer = Color(data.onSecondaryContainer),
            tertiary = Color(data.tertiary),
            onTertiary = Color(data.onTertiary),
            tertiaryContainer = Color(data.tertiaryContainer),
            onTertiaryContainer = Color(data.onTertiaryContainer),
            background = Color(data.background),
            onBackground = Color(data.onBackground),
            surface = Color(data.surface),
            onSurface = Color(data.onSurface),
            surfaceVariant = Color(data.surfaceVariant),
            onSurfaceVariant = Color(data.onSurfaceVariant),
            surfaceTint = Color(data.surfaceTint),
            inverseSurface = Color(data.inverseSurface),
            inverseOnSurface = Color(data.inverseOnSurface),
            error = Color(data.error),
            onError = Color(data.onError),
            errorContainer = Color(data.errorContainer),
            onErrorContainer = Color(data.onErrorContainer),
            outline = Color(data.outline),
            outlineVariant = Color(data.outlineVariant),
            scrim = Color(data.scrim),
            surfaceBright = Color(data.surfaceBright),
            surfaceDim = Color(data.surfaceDim),
            surfaceContainer = Color(data.surfaceContainer),
            surfaceContainerHigh = Color(data.surfaceContainerHigh),
            surfaceContainerHighest = Color(data.surfaceContainerHighest),
            surfaceContainerLow = Color(data.surfaceContainerLow),
            surfaceContainerLowest = Color(data.surfaceContainerLowest),
            primaryFixed = Color(data.primaryFixed),
            primaryFixedDim = Color(data.primaryFixedDim),
            onPrimaryFixed = Color(data.onPrimaryFixed),
            onPrimaryFixedVariant = Color(data.onPrimaryFixedVariant),
            secondaryFixed = Color(data.secondaryFixed),
            secondaryFixedDim = Color(data.secondaryFixedDim),
            onSecondaryFixed = Color(data.onSecondaryFixed),
            onSecondaryFixedVariant = Color(data.onSecondaryFixedVariant),
            tertiaryFixed = Color(data.tertiaryFixed),
            tertiaryFixedDim = Color(data.tertiaryFixedDim),
            onTertiaryFixed = Color(data.onTertiaryFixed),
            onTertiaryFixedVariant = Color(data.onTertiaryFixedVariant)
        )
    }

    fun serializeColorSchemePair(uri: String, style: String, pair: ColorSchemePair): CachedColorScheme {
        return CachedColorScheme(
            uri = uri,
            paletteStyle = style,
            lightSchemeJson = serializeColorScheme(pair.light),
            darkSchemeJson = serializeColorScheme(pair.dark)
        )
    }

    fun deserializeColorSchemePair(cached: CachedColorScheme): ColorSchemePair {
        return ColorSchemePair(
            light = deserializeColorScheme(cached.lightSchemeJson),
            dark = deserializeColorScheme(cached.darkSchemeJson)
        )
    }
}
