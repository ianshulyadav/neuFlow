# SpatialFlow → PixelPlayer-Quality UI Transformation Guide

## Complete Reference for Every Component, Card, Tab, and Page

---

# PART 0: THE FOUNDATION — UNIVERSAL LAWS

These principles apply to **every single UI element** you design. No exceptions.

```
┌────────────────────────────────────────────────────────────┐
│                    THE 10 COMMANDMENTS                      │
├────────────────────────────────────────────────────────────┤
│  1. NO raw colors — every color comes from the ColorScheme  │
│  2. NO hardcoded radii — use the Shape system               │
│  3. NO hardcoded elevations — tonalElevation = 0.dp         │
│  4. EVERY card gets AbsoluteSmoothCornerShape               │
│  5. EVERY screen responds to album-art ColorScheme          │
│  6. EVERY list item uses the Enhanced transition pattern    │
│  7. EVERY gradient derives from the active palette          │
│  8. EVERY icon tint comes from a scheme token               │
│  9. EVERY text style uses the Typography system             │
│ 10. NO view-level shadow or border — hierarchy = color      │
└────────────────────────────────────────────────────────────┘
```

---

# PART 1: COLOR EXTRACTION — THE HEART

## Step 1.1: Add the Material Color Utilities dependency

```kotlin
// build.gradle.kts (app)
dependencies {
    // Google's Material Color Utilities — HCT color space, QuantizerCelebi, DynamicScheme
    implementation("com.google.android.material:material:1.12.0")
    // Already transitively includes:
    //   com.google.android.material.color.utilities.Hct
    //   com.google.android.material.color.utilities.QuantizerCelebi
    //   com.google.android.material.color.utilities.SchemeTonalSpot
    //   com.google.android.material.color.utilities.SchemeVibrant
    //   com.google.android.material.color.utilities.SchemeExpressive
    //   com.google.android.material.color.utilities.SchemeFruitSalad

    // Smooth corners — the squircle/corner smoothing library
    implementation("io.github.racra:compose-smooth-corner-rect:1.0.0")
}
```

## Step 1.2: Create the Color Extraction Engine

Create a new file: `ui/theme/ColorExtractionEngine.kt`

This replaces the current single-color HSL approach in SpatialFlow's `Theme.kt`.

```kotlin
package com.codetrio.spatialflow.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.*
import kotlin.math.*

// ────────────────────────────────────────────────────────────
// 1. CONFIGURATION
// ────────────────────────────────────────────────────────────

data class ColorScoringConfig(
    val targetChroma: Double = 48.0,
    val weightProportion: Double = 0.7,
    val weightChromaAbove: Double = 0.3,
    val weightChromaBelow: Double = 0.1,
    val cutoffChroma: Double = 5.0,
    val cutoffExcitedProportion: Double = 0.01,
    val maxColorCount: Int = 4,
    val maxHueDifference: Int = 90,
    val minHueDifference: Int = 15
)

data class ColorExtractionConfig(
    val downscaleMaxDimension: Int = 128,
    val quantizerMaxColors: Int = 128,
    val scoring: ColorScoringConfig = ColorScoringConfig(),
    val accuracyLevel: Int = 0  // 0-10: higher = truer to artwork
) {
    val normalizedAccuracy: Double
        get() = accuracyLevel.coerceIn(0, 10).toDouble() / 10.0
}

enum class PaletteStyle(val storageKey: String) {
    TONAL_SPOT("tonal_spot"),
    VIBRANT("vibrant"),
    EXPRESSIVE("expressive"),
    FRUIT_SALAD("fruit_salad");

    companion object {
        val default = TONAL_SPOT
    }
}

// ────────────────────────────────────────────────────────────
// 2. CONSTANTS
// ────────────────────────────────────────────────────────────

private const val GRAYSCALE_CHROMA_THRESHOLD = 12.0
private const val NEUTRAL_PIXEL_CHROMA_THRESHOLD = 8.0
private const val HIGH_CHROMA_THRESHOLD = 18.0
private const val REQUIRED_NEUTRAL_POPULATION = 0.92
private const val MAX_HIGH_CHROMA_POPULATION = 0.03
private const val MAX_WEIGHTED_CHROMA_FOR_NEUTRAL = 9.0
private const val MAX_GRAYSCALE_CHANNEL_DELTA = 10

// ────────────────────────────────────────────────────────────
// 3. CACHE
// ────────────────────────────────────────────────────────────

private val seedColorCache = LruCache<Int, Color>(32)
private val schemeCache = LruCache<String, ColorSchemePair>(48)

data class ColorSchemePair(
    val light: ColorScheme,
    val dark: ColorScheme
)

fun clearColorCaches() {
    seedColorCache.evictAll()
    schemeCache.evictAll()
}

// ────────────────────────────────────────────────────────────
// 4. PUBLIC API
// ────────────────────────────────────────────────────────────

/**
 * Extract a single seed color from a bitmap using QuantizerCelebi
 * + weighted scoring + representative anchoring + local refinement.
 */
fun extractSeedColor(
    bitmap: Bitmap,
    config: ColorExtractionConfig = ColorExtractionConfig()
): Color {
    val cacheKey = 31 * bitmap.hashCode() + config.hashCode()
    seedColorCache.get(cacheKey)?.let { return it }

    val result = runCatching {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Color(selectSeedColorArgb(pixels, config))
    }.getOrElse { Color(0xFF6750A4) } // Fallback M3 purple

    seedColorCache.put(cacheKey, result)
    return result
}

/**
 * Generate a full light+dark ColorScheme pair from a seed color
 * and palette style. This is the single entry point for all theming.
 */
fun generateColorSchemePair(
    seedColor: Color,
    paletteStyle: PaletteStyle = PaletteStyle.default
): ColorSchemePair {
    val cacheKey = "${seedColor.toArgb()}_${paletteStyle.storageKey}"
    schemeCache.get(cacheKey)?.let { return it }

    val result = runCatching {
        val seedArgb = seedColor.toArgb()
        val sourceHct = Hct.fromInt(seedArgb)
        val isGray = sourceHct.chroma <= GRAYSCALE_CHROMA_THRESHOLD && isArgbNearGray(seedArgb)

        fun makeScheme(dark: Boolean): ColorScheme {
            val scheme = when (paletteStyle) {
                PaletteStyle.TONAL_SPOT   -> SchemeTonalSpot(sourceHct, dark, 0.0)
                PaletteStyle.VIBRANT      -> SchemeVibrant(sourceHct, dark, 0.0)
                PaletteStyle.EXPRESSIVE   -> SchemeExpressive(sourceHct, dark, 0.0)
                PaletteStyle.FRUIT_SALAD  -> SchemeFruitSalad(sourceHct, dark, 0.0)
            }
            return if (isGray && dark) grayscaleScheme(scheme) else scheme.toComposeColorScheme()
        }

        ColorSchemePair(light = makeScheme(false), dark = makeScheme(true))
    }.getOrElse {
        // Fallback pair
        ColorSchemePair(
            light = androidx.compose.material3.lightColorScheme(),
            dark = androidx.compose.material3.darkColorScheme()
        )
    }

    schemeCache.put(cacheKey, result)
    return result
}

// ────────────────────────────────────────────────────────────
// 5. SEED SELECTION ALGORITHM
// ────────────────────────────────────────────────────────────

private fun selectSeedColorArgb(
    pixels: IntArray,
    config: ColorExtractionConfig
): Int {
    val fallbackArgb = averageColorArgb(pixels)
    val quantized = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)

    if (isMostlyNeutralArtwork(quantized) && isArgbNearGray(fallbackArgb)) {
        return fallbackArgb
    }

    val representative = calculateRepresentativeColor(pixels, config.normalizedAccuracy)
    val ranked = scoreQuantizedColors(
        quantized, config.scoring, fallbackArgb, representative, config.normalizedAccuracy
    )
    val selected = ranked.firstOrNull() ?: fallbackArgb

    return refineSeedColor(selected, pixels, representative, config)
}

// ────────────────────────────────────────────────────────────
// 6. SCORING
// ────────────────────────────────────────────────────────────

private data class ScoredHct(val hct: Hct, val score: Double)
private data class RepresentativeColor(val argb: Int, val hct: Hct)

private fun scoreQuantizedColors(
    colorsToPopulation: Map<Int, Int>,
    scoring: ColorScoringConfig,
    fallbackArgb: Int,
    representative: RepresentativeColor?,
    accuracy: Double
): List<Int> {
    if (colorsToPopulation.isEmpty()) return listOf(fallbackArgb)

    val colorsHct = mutableListOf<Hct>()
    val huePopulation = IntArray(360)
    var populationSum = 0.0

    for ((argb, pop) in colorsToPopulation) {
        if (pop <= 0) continue
        val hct = Hct.fromInt(argb)
        colorsHct.add(hct)
        huePopulation[MathUtils.sanitizeDegreesInt(hct.hue.toInt())] += pop
        populationSum += pop
    }

    if (populationSum <= 0.0) return listOf(fallbackArgb)

    // Compute hue-excited proportions (neighborhood smoothing)
    val hueExcited = DoubleArray(360)
    for (hue in 0..359) {
        var sum = 0.0
        for (neighbor in hue - 14..hue + 15) {
            sum += huePopulation[MathUtils.sanitizeDegreesInt(neighbor)]
        }
        hueExcited[hue] = sum / (populationSum * 30)
    }

    // Score each color
    val targetChroma = representative?.hct?.chroma?.coerceIn(12.0, 72.0) ?: scoring.targetChroma
    val scored = colorsHct.map { hct ->
        val proportion = (colorsToPopulation[hct.toInt()] ?: 0).toDouble() / populationSum
        val chroma = hct.chroma
        val chromaScore = if (chroma >= scoring.cutoffChroma) {
            if (chroma >= targetChroma) {
                scoring.weightChromaAbove * (1.0 - ((chroma - targetChroma) / 100.0).coerceIn(0.0, 1.0))
            } else {
                scoring.weightChromaBelow * (chroma / targetChroma)
            }
        } else 0.0
        val hue = MathUtils.sanitizeDegreesInt(hct.hue.toInt())
        val excited = hueExcited[hue]
        val score = (proportion * scoring.weightProportion) + chromaScore -
            (excited.coerceAtMost(scoring.cutoffExcitedProportion) * 0.5)
        ScoredHct(hct, score)
    }.sortedByDescending { it.score }

    // Select diverse colors
    val result = mutableListOf<Int>()
    for (scoredHct in scored) {
        val argb = scoredHct.hct.toInt()
        val hue = scoredHct.hct.hue
        val isDiverse = result.none { existing ->
            val existingHue = Hct.fromInt(existing).hue
            val diff = MathUtils.differenceDegrees(hue, existingHue)
            diff < scoring.minHueDifference || diff > scoring.maxHueDifference
        }
        if (isDiverse || result.isEmpty()) {
            result.add(argb)
            if (result.size >= scoring.maxColorCount) break
        }
    }

    return result.ifEmpty { listOf(fallbackArgb) }
}

// ────────────────────────────────────────────────────────────
// 7. REPRESENTATIVE COLOR (pixel-level scanning)
// ────────────────────────────────────────────────────────────

private const val REP_CHROMA_THRESHOLD = 10.0
private const val MIN_REP_PIXEL_RATIO = 0.04

private fun calculateRepresentativeColor(
    pixels: IntArray,
    accuracy: Double
): RepresentativeColor? {
    if (pixels.isEmpty()) return null

    val threshold = lerp(REP_CHROMA_THRESHOLD, 6.0, accuracy)
    var totalR = 0L; var totalG = 0L; var totalB = 0L
    var count = 0

    for (argb in pixels) {
        if (((argb ushr 24) and 0xFF) < 28) continue  // skip transparent
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        if ((r + g + b) < 36) continue  // skip near-black

        val hct = Hct.fromInt(argb)
        if (hct.chroma >= threshold) {
            totalR += r; totalG += g; totalB += b; count++
        }
    }

    if (count <= 0 || count.toDouble() / pixels.size < MIN_REP_PIXEL_RATIO) return null

    val argb = (0xFF shl 24) or
        ((totalR / count).toInt() shl 16) or
        ((totalG / count).toInt() shl 8) or
        (totalB / count).toInt()

    return RepresentativeColor(argb, Hct.fromInt(argb))
}

// ────────────────────────────────────────────────────────────
// 8. REFINEMENT
// ────────────────────────────────────────────────────────────

private fun refineSeedColor(
    candidateArgb: Int,
    pixels: IntArray,
    representative: RepresentativeColor?,
    config: ColorExtractionConfig
): Int {
    val candidateHct = Hct.fromInt(candidateArgb)
    val accuracy = config.normalizedAccuracy

    // Local neighborhood average
    val localHueWindow = lerp(32.0, 18.0, accuracy)
    var localR = 0L; var localG = 0L; var localB = 0L; var localCount = 0

    for (argb in pixels) {
        val hct = Hct.fromInt(argb)
        val hueDiff = MathUtils.differenceDegrees(candidateHct.hue, hct.hue)
        if (hueDiff <= localHueWindow && hct.chroma >= 6.0) {
            localR += (argb ushr 16) and 0xFF
            localG += (argb ushr 8) and 0xFF
            localB += argb and 0xFF
            localCount++
        }
    }

    val localBlend = lerp(0.42f, 0.72f, accuracy.toFloat())
    val refinedArgb = if (localCount > 0) {
        blendArgb(candidateArgb,
            (0xFF shl 24) or
            ((localR / localCount).toInt() shl 16) or
            ((localG / localCount).toInt() shl 8) or
            (localB / localCount).toInt(),
            localBlend
        )
    } else candidateArgb

    // Re-anchor to representative
    if (representative != null) {
        val hueWindow = lerp(90.0, 52.0, accuracy)
        if (MathUtils.differenceDegrees(Hct.fromInt(refinedArgb).hue, representative.hct.hue) <= hueWindow) {
            return blendArgb(refinedArgb, representative.argb, lerp(0.0f, 0.42f, accuracy.toFloat()))
        }
    }

    return refinedArgb
}

// ────────────────────────────────────────────────────────────
// 9. UTILITIES
// ────────────────────────────────────────────────────────────

private fun averageColorArgb(pixels: IntArray): Int {
    if (pixels.isEmpty()) return 0xFF6750A4.toInt()
    var r = 0L; var g = 0L; var b = 0L
    for (argb in pixels) {
        r += (argb ushr 16) and 0xFF
        g += (argb ushr 8) and 0xFF
        b += argb and 0xFF
    }
    val s = pixels.size.toLong()
    return (0xFF shl 24) or
        ((r / s).toInt() shl 16) or
        ((g / s).toInt() shl 8) or
        (b / s).toInt()
}

private fun isMostlyNeutralArtwork(colorsToPopulation: Map<Int, Int>): Boolean {
    if (colorsToPopulation.isEmpty()) return false
    var total = 0.0; var neutral = 0.0; var highChroma = 0.0; var weightedChroma = 0.0
    for ((argb, pop) in colorsToPopulation) {
        if (pop <= 0) continue
        val p = pop.toDouble()
        val chroma = Hct.fromInt(argb).chroma
        total += p; weightedChroma += chroma * p
        if (chroma <= NEUTRAL_PIXEL_CHROMA_THRESHOLD) neutral += p
        if (chroma >= HIGH_CHROMA_THRESHOLD) highChroma += p
    }
    if (total <= 0.0) return false
    return neutral / total >= REQUIRED_NEUTRAL_POPULATION &&
        highChroma / total <= MAX_HIGH_CHROMA_POPULATION &&
        weightedChroma / total <= MAX_WEIGHTED_CHROMA_FOR_NEUTRAL
}

private fun isArgbNearGray(argb: Int): Boolean {
    val r = (argb ushr 16) and 0xFF
    val g = (argb ushr 8) and 0xFF
    val b = argb and 0xFF
    return maxOf(abs(r - g), abs(g - b), abs(r - b)) <= MAX_GRAYSCALE_CHANNEL_DELTA
}

private fun blendArgb(a: Int, b: Int, ratio: Float): Int {
    val inv = 1f - ratio.coerceIn(0f, 1f)
    fun comp(shift: Int) =
        ((((a ushr shift) and 0xFF) * inv + ((b ushr shift) and 0xFF) * ratio)
            .roundToInt().coerceIn(0, 255))
    return (comp(24) shl 24) or (comp(16) shl 16) or (comp(8) shl 8) or comp(0)
}

private fun lerp(start: Double, stop: Double, fraction: Double): Double =
    start + ((stop - start) * fraction.coerceIn(0.0, 1.0))

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + ((stop - start) * fraction.coerceIn(0f, 1f))

// ────────────────────────────────────────────────────────────
// 10. COMPOSE COLORSCHEME CONVERSION
// ────────────────────────────────────────────────────────────

private fun DynamicScheme.toComposeColorScheme(): ColorScheme = ColorScheme(
    primary = Color(getPrimary()),
    onPrimary = Color(getOnPrimary()),
    primaryContainer = Color(getPrimaryContainer()),
    onPrimaryContainer = Color(getOnPrimaryContainer()),
    inversePrimary = Color(getInversePrimary()),
    secondary = Color(getSecondary()),
    onSecondary = Color(getOnSecondary()),
    secondaryContainer = Color(getSecondaryContainer()),
    onSecondaryContainer = Color(getOnSecondaryContainer()),
    tertiary = Color(getTertiary()),
    onTertiary = Color(getOnTertiary()),
    tertiaryContainer = Color(getTertiaryContainer()),
    onTertiaryContainer = Color(getOnTertiaryContainer()),
    background = Color(getBackground()),
    onBackground = Color(getOnBackground()),
    surface = Color(getSurface()),
    onSurface = Color(getOnSurface()),
    surfaceVariant = Color(getSurfaceVariant()),
    onSurfaceVariant = Color(getOnSurfaceVariant()),
    surfaceTint = Color(getSurfaceTint()),
    inverseSurface = Color(getInverseSurface()),
    inverseOnSurface = Color(getInverseOnSurface()),
    error = Color(getError()),
    onError = Color(getOnError()),
    errorContainer = Color(getErrorContainer()),
    onErrorContainer = Color(getOnErrorContainer()),
    outline = Color(getOutline()),
    outlineVariant = Color(getOutlineVariant()),
    scrim = Color(getScrim()),
    surfaceBright = Color(getSurfaceBright()),
    surfaceDim = Color(getSurfaceDim()),
    surfaceContainer = Color(getSurfaceContainer()),
    surfaceContainerHigh = Color(getSurfaceContainerHigh()),
    surfaceContainerHighest = Color(getSurfaceContainerHighest()),
    surfaceContainerLow = Color(getSurfaceContainerLow()),
    surfaceContainerLowest = Color(getSurfaceContainerLowest()),
    primaryFixed = Color(getPrimaryFixed()),
    primaryFixedDim = Color(getPrimaryFixedDim()),
    onPrimaryFixed = Color(getOnPrimaryFixed()),
    onPrimaryFixedVariant = Color(getOnPrimaryFixedVariant()),
    secondaryFixed = Color(getSecondaryFixed()),
    secondaryFixedDim = Color(getSecondaryFixedDim()),
    onSecondaryFixed = Color(getOnSecondaryFixed()),
    onSecondaryFixedVariant = Color(getOnSecondaryFixedVariant()),
    tertiaryFixed = Color(getTertiaryFixed()),
    tertiaryFixedDim = Color(getTertiaryFixedDim()),
    onTertiaryFixed = Color(getOnTertiaryFixed()),
    onTertiaryFixedVariant = Color(getOnTertiaryFixedVariant()),
)

private fun grayscaleScheme(scheme: DynamicScheme): ColorScheme {
    fun gray(c: Color): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(c.toArgb(), hsl)
        hsl[0] = 0f; hsl[1] = 0f
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
    val cs = scheme.toComposeColorScheme()
    return cs.copy(
        primary = gray(cs.primary), onPrimary = gray(cs.onPrimary),
        primaryContainer = gray(cs.primaryContainer), onPrimaryContainer = gray(cs.onPrimaryContainer),
        secondary = gray(cs.secondary), onSecondary = gray(cs.onSecondary),
        secondaryContainer = gray(cs.secondaryContainer), onSecondaryContainer = gray(cs.onSecondaryContainer),
        tertiary = gray(cs.tertiary), onTertiary = gray(cs.onTertiary),
        tertiaryContainer = gray(cs.tertiaryContainer), onTertiaryContainer = gray(cs.onTertiaryContainer),
        background = gray(cs.background), onBackground = gray(cs.onBackground),
        surface = gray(cs.surface), onSurface = gray(cs.onSurface),
        surfaceVariant = gray(cs.surfaceVariant), onSurfaceVariant = gray(cs.onSurfaceVariant),
        inverseSurface = gray(cs.inverseSurface), inverseOnSurface = gray(cs.inverseOnSurface),
        surfaceContainer = gray(cs.surfaceContainer),
        surfaceContainerHigh = gray(cs.surfaceContainerHigh),
        surfaceContainerHighest = gray(cs.surfaceContainerHighest),
        surfaceContainerLow = gray(cs.surfaceContainerLow),
        surfaceContainerLowest = gray(cs.surfaceContainerLowest),
    )
}
```

## Step 1.3: Create the ColorScheme Caching Layer

New file: `ui/theme/ColorSchemeCache.kt`

```kotlin
package com.codetrio.spatialflow.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thread-safe color scheme cache with two-tier storage.
 *
 * Tier 1: In-memory LRU (fast)
 * Tier 2: Room database (persistent) — implement when ready
 * Tier 3: Generate fresh from bitmap (slowest)
 */
object ColorSchemeCache {
    private val memoryCache = LruCache<String, ColorSchemePair>(30)
    private val mutex = Mutex()
    private val inProgress = mutableSetOf<String>()

    fun get(key: String): ColorSchemePair? = memoryCache.get(key)

    fun put(key: String, value: ColorSchemePair) {
        memoryCache.put(key, value)
    }

    fun evictAll() = memoryCache.evictAll()

    suspend fun markInProgress(uri: String): Boolean = mutex.withLock {
        if (inProgress.contains(uri)) false
        else { inProgress.add(uri); true }
    }

    suspend fun markComplete(uri: String) = mutex.withLock {
        inProgress.remove(uri)
    }

    /**
     * Load a small (128×128) bitmap from any URI for color extraction.
     */
    suspend fun loadBitmapForExtraction(
        context: Context,
        uri: Any
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .size(Size(128, 128))
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .build()
            val drawable = context.imageLoader.execute(request).drawable ?: return@withContext null
            Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).also { bmp ->
                Canvas(bmp).let { canvas ->
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }
        } catch (_: Exception) { null }
    }
}
```

---

# PART 2: THEME ARCHITECTURE — SHAPES & TYPOGRAPHY

## Step 2.1: Replace Type.kt

```kotlin
// ui/theme/Type.kt

package com.codetrio.spatialflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codetrio.spatialflow.R

// ── GOOGLE SANS FLEX (Rounded) ──
@OptIn(ExperimentalTextApi::class)
val GoogleSansRounded = FontFamily(
    Font(
        resId = R.font.google_flex,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_flex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_flex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_flex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.Setting("ROND", 100f)
        )
    ),
    Font(
        resId = R.font.google_flex,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.Setting("ROND", 100f)
        )
    ),
)

// ── GOOGLE SANS FLEX (Non-Rounded — for lyrics) ──
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(
        resId = R.font.google_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 0f)
        )
    )
)

// ── MONTSERRAT (for display titles) ──
private val montserratProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val MontserratFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = montserratProvider,
        weight = FontWeight.Black
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = montserratProvider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = montserratProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = montserratProvider,
        weight = FontWeight.Normal
    ),
)

// ── TYPOGRAPHY ──

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSansRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp
    ),
)
```

## Step 2.2: Replace the Shape System

```kotlin
// ui/theme/Shape.kt

package com.codetrio.spatialflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale — consistent across the app.
 * Always use these sizes; never hardcode radii in components.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Card shape constants for AbsoluteSmoothCornerShape usage.
 * Smooth corners eliminate the visual "kink" of regular rounded rects.
 *
 * Usage with racra library:
 *   AbsoluteSmoothCornerShape(
 *       cornerRadiusTL = CardCornerRadius,
 *       smoothnessAsPercentTL = CardSmoothness,
 *       ...
 *   )
 */
object CardCorners {
    const val RadiusDp = 28f
    const val Smoothness = 60   // 0=regular rounded, 100=fully smooth/squircle
}
```

---

# PART 3: THE THEME COMPOSABLE — THE SINGLE SOURCE OF TRUTH

## Step 3.1: Rewrite Theme.kt

This is the most important file. Every screen, card, tab, and component must flow through this:

```kotlin
// ui/theme/Theme.kt

package com.codetrio.spatialflow.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

/**
 * CompositionLocal — any component can read whether album-art theming is active.
 */
val LocalAlbumColorScheme = staticCompositionLocalOf<ColorSchemePair?> { null }
val LocalSpatialFlowDarkTheme = staticCompositionLocalOf { false }

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SpatialFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    albumColorSchemePair: ColorSchemePair? = null,  // ← THE KEY: inject album colors here
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // ── Base scheme: system dynamic (Android 12+) or custom ──
    val baseScheme = remember(darkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    }

    // ── Override with album art if available ──
    val colorScheme = remember(baseScheme, albumColorSchemePair, darkTheme, amoledBlack) {
        val scheme = if (albumColorSchemePair != null) {
            if (darkTheme) albumColorSchemePair.dark else albumColorSchemePair.light
        } else baseScheme

        if (darkTheme && amoledBlack) {
            scheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainer = Color.Black,
            )
        } else scheme
    }

    // ── Transparent status/nav bars ──
    SideEffect {
        view.context.findActivity()?.window?.let { window ->
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars =
                    ColorUtils.calculateLuminance(colorScheme.background.toArgb()) > 0.55
                isAppearanceLightNavigationBars =
                    ColorUtils.calculateLuminance(colorScheme.background.toArgb()) > 0.55
            }
        }
    }

    CompositionLocalProvider(
        LocalAlbumColorScheme provides albumColorSchemePair,
        LocalSpatialFlowDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

---

# PART 4: COMPONENT PATTERNS — EXACTLY HOW TO BUILD EVERYTHING

## Pattern 4.1: The Card (use everywhere)

```kotlin
// components/SpatialFlowCard.kt

@Composable
fun SpatialFlowCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = CardCorners.RadiusDp.dp,
            cornerRadiusTR = CardCorners.RadiusDp.dp,
            cornerRadiusBL = CardCorners.RadiusDp.dp,
            cornerRadiusBR = CardCorners.RadiusDp.dp,
            smoothnessAsPercentTL = CardCorners.Smoothness,
            smoothnessAsPercentTR = CardCorners.Smoothness,
            smoothnessAsPercentBL = CardCorners.Smoothness,
            smoothnessAsPercentBR = CardCorners.Smoothness,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // ← NEVER > 0
        onClick = onClick ?: {}
    ) {
        content()
    }
}
```

## Pattern 4.2: The Song List Item

```kotlin
// components/SongListItem.kt

@Composable
fun SongListItem(
    song: SongItem,           // your model
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    showAlbumArt: Boolean = true,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // ── Animated highlight for current song ──
    val transition = updateTransition(
        targetState = isCurrentSong, label = "songHighlight"
    )
    val highlightAlpha by transition.animateFloat(
        transitionSpec = { tween(400) }, label = "highlightAlpha"
    ) { if (it) 0.12f else 0f }
    val highlightColor by transition.animateColor(
        transitionSpec = { tween(400) }, label = "highlightColor"
    ) {
        if (it) colorScheme.primary else Color.Transparent
    }
    val surfaceColor by transition.animateColor(
        transitionSpec = { tween(400) }, label = "surfaceColor"
    ) {
        if (it) colorScheme.primaryContainer.copy(alpha = 0.3f)
        else Color.Transparent
    }

    val shape = RoundedCornerShape(16.dp)  // AppShapes.medium

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = surfaceColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art — using MaterialTheme tokens for placeholder
            if (showAlbumArt) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))  // AppShapes.small
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = song.getAlbumArtUri(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentSong) colorScheme.primary
                            else colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            // Playing indicator — no hardcoded color
            if (isCurrentSong) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary)
                )
            }
        }
    }
}
```

## Pattern 4.3: The Tab Row

```kotlin
// components/TabRow.kt

@Composable
fun SpatialFlowTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,  // ← transparent: let background show
        contentColor = colorScheme.onSurface,
        divider = {}, // ← NO divider line
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = colorScheme.primary  // ← from scheme, never hardcoded
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold
                                     else FontWeight.Normal,
                        color = if (index == selectedIndex) colorScheme.primary
                                else colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}
```

## Pattern 4.4: The Screen Wrapper (every page uses this)

```kotlin
// components/ScreenWrapper.kt

@Composable
fun SpatialFlowScreen(
    topBar: @Composable () -> Unit = {},
    bottomBarPadding: Dp = 0.dp,
    content: @Composable (PaddingValues) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,  // ← scheme background
        topBar = topBar,
        contentWindowInsets = WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = bottomBarPadding)
        ) {
            content(paddingValues)
        }
    }
}
```

## Pattern 4.5: The Gradient Header

```kotlin
// components/GradientHeader.kt

@Composable
fun GradientHeader(
    title: String,
    startColor: Color = MaterialTheme.colorScheme.primaryContainer,
    endColor: Color = MaterialTheme.colorScheme.surface,
    onBackClick: () -> Unit,
    expandedHeight: Dp = 160.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val gradientBrush = remember(startColor, endColor) {
        Brush.verticalGradient(listOf(startColor, endColor))
    }

    LargeTopAppBar(
        title = {
            Text(
                text = title,
                color = colorScheme.onPrimaryContainer,
                fontFamily = GoogleSansRounded,
                modifier = Modifier.padding(start = 6.dp)
            )
        },
        expandedHeight = expandedHeight,
        modifier = Modifier.background(brush = gradientBrush),
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.primaryContainer
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}
```

---

# PART 5: THE FULL PLAYER REDESIGN

## Step 5.1: Current State → Required Changes

SpatialFlow's current `FullPlayer.kt` has these issues:

| Current Issue | Fix |
|---|---|
| `accentColor: Color` passed as parameter | Use `MaterialTheme.colorScheme` tokens |
| `RoundedCornerShape(50.dp)` hardcoded | Use `AbsoluteSmoothCornerShape` from CardCorners |
| `horizontalScroll` for metadata | Use `basicMarqueeWithFadedEdges` + gradient fades from scheme |
| No album carousel | Add `HorizontalPager` with parallax + prefetch |
| Wavy slider uses hardcoded colors | Inherit from scheme |
| Static background | Dynamic gradient from extracted colors |
| No sheet theme animation | Add `animateColorAsState` for all surface transitions |

## Step 5.2: New FullPlayer Skeleton

```kotlin
// player/FullPlayer.kt — REWRITTEN

@Composable
fun FullPlayerContent(  // ← renamed from FullPlayerScreen for clarity
    song: SongItem,
    queue: List<SongItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    // ... playback state from ViewModel
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalSpatialFlowDarkTheme.current

    // ── All colors from scheme tokens ──
    val playerOnBase = colorScheme.onPrimaryContainer
    val playerAccent = colorScheme.primary
    val playerOnAccent = colorScheme.onPrimary
    val placeholderAlpha = 0.1f
    val gradientEdge = colorScheme.primaryContainer

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AnimatedVisibility(visible = true) {
                TopAppBar(
                    modifier = Modifier.graphicsLayer {
                        // fade topbar with expansion fraction
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = playerOnBase
                    ),
                    title = {
                        Text(
                            "Now Playing",
                            fontFamily = GoogleSansRounded,
                            modifier = Modifier.padding(start = 18.dp)
                        )
                    },
                    actions = {
                        // Cast / Bluetooth chip (see Component 4.3 above)
                        // Queue button
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Album Art Carousel
            AlbumArtCarousel(
                song = song, queue = queue, currentIndex = currentIndex,
                cornerRadius = CardCorners.RadiusDp.dp,
                modifier = Modifier.weight(1f)
            )

            // 2. Song Metadata (marquee + gradient fade)
            SongMetadataSection(
                song = song,
                gradientEdgeColor = gradientEdge,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Wavy Progress Slider
            WavyMusicSlider(
                value = progressFraction,
                onValueChange = { onSeek(it) },
                activeTrackColor = playerOnBase,       // ← from scheme
                inactiveTrackColor = playerOnBase.copy(alpha = 0.2f), // ← derived
                thumbColor = playerAccent,             // ← from scheme
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Transport Controls
            TransportControls(
                isPlaying = isPlaying,
                playPauseContainer = playerAccent,
                playPauseContent = playerOnAccent,
                skipContainer = playerAccent,   // ← filled style
                skipContent = playerOnAccent,   // ← filled style
                onPlayPause = {}, onNext = {}, onPrevious = {}
            )

            // 5. Bottom Toggle Row (shuffle, repeat, like)
            BottomToggleRow(
                colors = BottomToggleColors(
                    container = playerOnAccent.copy(alpha = 0.08f),
                    content = playerOnBase
                )
            )
        }
    }
}

// ── Sub-components (each in its own file) ──

@Composable
private fun AlbumArtCarousel(
    song: SongItem, queue: List<SongItem>, currentIndex: Int,
    cornerRadius: Dp, modifier: Modifier
) { /* HorizontalPager with AbsoluteSmoothCornerShape, prefetch neighbors */ }

@Composable
private fun SongMetadataSection(
    song: SongItem, gradientEdgeColor: Color, modifier: Modifier
) { /* Marquee title + artist chips with gradient fade edges */ }

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    playPauseContainer: Color, playPauseContent: Color,
    skipContainer: Color, skipContent: Color,
    onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Play/Pause: large, filled, AbsoluteSmoothCornerShape
    // Skip: smaller, filled, same corner shape
    // ALL colors from parameters → parameters come from scheme
}
```

---

# PART 6: EXPLORE / HOME PAGE REDESIGN

## Current SpatialFlow Explore Page Issues

- Cards use `CardDefaults.cardColors()` without explicit container colors
- Hardcoded `CircleShape` with `border()` for artist avatars
- `RoundedCornerShape(12.dp)` hardcoded in components
- No gradient headers for sections
- `LazyColumn` items don't respond to album color scheme

## Redesign Patterns

```kotlin
// explore/ExploreComponents.kt — REWRITE KEY COMPONENTS

/** Section header — always colored from scheme, always with proper spacing */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,  // ← from scheme
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/** Artist chip — smooth corners, scheme colors, no hard borders */
@Composable
fun ArtistChip(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp) // AppShapes.medium

    Surface(
        modifier = modifier
            .width(100.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = colorScheme.surfaceContainerHigh,  // ← scheme
        tonalElevation = 0.dp  // ← no shadow
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar: no border, use surfaceVariant as fallback bg
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceVariant),  // ← scheme
                contentScale = ContentScale.Crop
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurface,  // ← scheme
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Album grid card — smooth corners, scheme surface, no elevation */
@Composable
fun AlbumGridCard(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = CardCorners.RadiusDp.dp,
            cornerRadiusTR = CardCorners.RadiusDp.dp,
            cornerRadiusBL = CardCorners.RadiusDp.dp,
            cornerRadiusBR = CardCorners.RadiusDp.dp,
            smoothnessAsPercentTL = CardCorners.Smoothness,
            smoothnessAsPercentTR = CardCorners.Smoothness,
            smoothnessAsPercentBL = CardCorners.Smoothness,
            smoothnessAsPercentBR = CardCorners.Smoothness,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh  // ← scheme
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)  // ← zero
    ) {
        Column {
            AsyncImage(
                model = artworkUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(colorScheme.surfaceVariant),  // ← scheme placeholder
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,  // ← scheme
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,  // ← scheme
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

---

# PART 7: TAB & LIBRARY PAGES

## Library / History / Settings — Universal Rules

Every single list item, grid item, and section:

```kotlin
// ── THE UNIVERSAL TAB PAGE TEMPLATE ──

@Composable
fun LibraryPage(
    // paging / list data
    content: LazyPagingItems<Item>,
    viewModel: PlayerSharedViewModel,
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // Pull-to-refresh with scheme colors
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        // Colors from scheme:
        containerColor = colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 8.dp,
                bottom = 80.dp  // miniplayer clearance
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = content.itemCount,
                key = { content[it]?.id ?: it }
            ) { index ->
                val item = content[index] ?: return@items
                SongListItem(  // ← uses scheme colors internally
                    song = item,
                    isCurrentSong = item.id == currentId,
                    isPlaying = isPlaying && item.id == currentId,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
```

## Section Dividers

```kotlin
// NEVER use HorizontalDivider with a hardcoded color
// ALWAYS use the scheme's outlineVariant at reduced alpha:

@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
```

---

# PART 8: COLOR RESPONSIVENESS PIPELINE

## How Every Screen Gets Album Colors

```
┌──────────────────────────────────────────────────────┐
│                  ViewModel                           │
│  PlayerSharedViewModel.collectCurrentSong()          │
│       │                                               │
│       ▼                                               │
│  ┌─────────────────────────┐                         │
│  │ Current Song's Art URI  │                         │
│  └───────────┬─────────────┘                         │
│              │                                        │
│              ▼                                        │
│  ┌──────────────────────────────┐                    │
│  │ ColorSchemeCache             │                    │
│  │  .get(uri) ──▶ cache hit?    │                    │
│  │  .loadBitmap(uri) ──▶ miss   │                    │
│  │  .extractSeedColor(bitmap)   │                    │
│  │  .generateColorSchemePair()  │                    │
│  │  .put(uri, pair)             │                    │
│  └───────────┬──────────────────┘                    │
│              │                                        │
│              ▼                                        │
│  ┌──────────────────────────────┐                    │
│  │ StateFlow<ColorSchemePair?>  │                    │
│  └───────────┬──────────────────┘                    │
│              │                                        │
│              ▼                                        │
│  SpatialFlowTheme(                                    │
│      albumColorSchemePair = pair                      │
│  )                                                    │
│  ┌──────────────────────────────────────┐             │
│  │  MaterialTheme.colorScheme is NOW    │             │
│  │  the album-art-derived scheme        │             │
│  │                                      │             │
│  │  Every component in the tree:        │             │
│  │    colorScheme.primary               │             │
│  │    colorScheme.surfaceContainer      │             │
│  │    colorScheme.onPrimaryContainer    │             │
│  │    → ALL auto-magically respond      │             │
│  └──────────────────────────────────────┘             │
└──────────────────────────────────────────────────────┘
```

## Integration in PlayerSharedViewModel

```kotlin
// Add to PlayerSharedViewModel.kt:

private val _currentColorScheme = MutableStateFlow<ColorSchemePair?>(null)
val currentColorScheme: StateFlow<ColorSchemePair?> = _currentColorScheme.asStateFlow()

fun updateColorsForSong(song: SongItem) {
    viewModelScope.launch(Dispatchers.IO) {
        val uri = song.getAlbumArtUri()?.toString() ?: run {
            _currentColorScheme.value = null
            return@launch
        }
        // Check cache first
        val cacheKey = "song_${song.videoId}_${PaletteStyle.default.storageKey}"
        ColorSchemeCache.get(cacheKey)?.let {
            _currentColorScheme.value = it
            return@launch
        }
        // Generate
        val bitmap = ColorSchemeCache.loadBitmapForExtraction(context, uri) ?: run {
            _currentColorScheme.value = null
            return@launch
        }
        val seed = extractSeedColor(bitmap)
        val pair = generateColorSchemePair(seed)
        ColorSchemeCache.put(cacheKey, pair)
        _currentColorScheme.value = pair
        bitmap.recycle()
    }
}
```

---

# PART 9: CHECKLIST — DESIGN REVIEW FOR EVERY NEW UI

When designing ANY new screen, component, view — run this checklist:

```
☐ 1. Are ALL colors via MaterialTheme.colorScheme.<token>?
     No Color(0xFF...), no Color.Red, no Color.White, no Color.Black.
     Only: primary, onPrimary, primaryContainer, onPrimaryContainer,
            surface, onSurface, surfaceVariant, onSurfaceVariant,
            surfaceContainerLowest → surfaceContainerHighest,
            background, onBackground, outline, outlineVariant

☐ 2. Is tonalElevation = 0.dp on every Surface/Card?
     (Elevation creates shadow clutter — hierarchy via color, not shadow)

☐ 3. Are corners using AppShapes or AbsoluteSmoothCornerShape?
     Never RoundedCornerShape(14.dp) — use small/medium/large or CardCorners.

☐ 4. Is text using MaterialTheme.typography.<style>?
     Never fontSize = 15.sp — use titleMedium, bodyLarge, labelSmall, etc.

☐ 5. Is the font family GoogleSansRounded or MontserratFamily?
     (Already set in Typography — just use the text styles correctly)

☐ 6. Are gradients derived from scheme tokens?
     Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.surface))

☐ 7. Are dividers using scheme.outlineVariant.copy(alpha = 0.5f)?
     No Color.Gray, no hardcoded hex

☐ 8. Does this component survive the album-art ColorScheme switch?
     (Test: play a song with bright red art, then bright blue art.
      Every element should shift smoothly.)

☐ 9. Is the icon tint from a scheme token?
     tint = colorScheme.onSurface (not Color.White or Color.Black)

☐ 10. Are placeholder/skeleton colors derived?
     colorScheme.onSurface.copy(alpha = 0.1f) — never hardcoded gray

☐ 11. Is there a transition animation on state changes?
     animateFloatAsState / animateColorAsState / updateTransition

☐ 12. Is the padding/spacing consistent?
     16.dp horizontal, 8-12.dp vertical spacing (use Arrangement.spacedBy)

☐ 13. Does the component handle dark AND light themes?
     (Never assume dark-only)

☐ 14. Are scrollbars using the ExpressiveScrollBar pattern?
     (Not ugly default scrollbar)

☐ 15. Is the status bar color handled?
     PixelPlayStatusBarStyle / SideEffect for transparent bars
```

---

# PART 10: MIGRATION ORDER — WHAT TO DO FIRST

```
Phase 1: Foundation (do this FIRST — everything depends on it)
├── 1. Add Material Color Utilities + racra smooth corners to build.gradle
├── 2. Create ui/theme/ColorExtractionEngine.kt
├── 3. Create ui/theme/ColorSchemeCache.kt
├── 4. Rewrite ui/theme/Shape.kt (add AppShapes + CardCorners)
├── 5. Rewrite ui/theme/Type.kt (add GoogleSansRounded + MontserratFamily)
└── 6. Rewrite ui/theme/Theme.kt (add ColorSchemePair override)

Phase 2: Player (the flagship screen)
├── 7. Rewrite FullPlayer.kt with scheme-driven colors
├── 8. Rewrite PlayerUiComponents.kt cards/pills with smooth corners
├── 9. Rewrite ArtworkPager.kt with AbsoluteSmoothCornerShape + prefetch
└── 10. Update PlayerSharedViewModel with color extraction pipeline

Phase 3: Content Screens
├── 11. Rewrite ExploreComponents.kt (all cards, chips, headers)
├── 12. Rewrite LibraryScreen.kt (song list, album grid, artist grid)
├── 13. Rewrite HistoryScreen.kt
└── 14. Rewrite EffectsScreen.kt

Phase 4: Shell & Navigation
├── 15. Add animated status/nav bar in Theme.kt
├── 16. Rewrite OnboardingScreen.kt with gradient headers
├── 17. Rewrite SettingsFragment.kt cards with scheme colors
└── 18. Rewrite QueueBottomSheet.kt

Phase 5: Polish
├── 19. Add AlbumColorAccuracy preference (0-10 slider)
├── 20. Add PaletteStyle preference (TONAL_SPOT/VIBRANT/EXPRESSIVE/FRUIT_SALAD)
├── 21. Add persistent Room cache for ColorSchemePair
└── 22. Crossfade animation between schemes on track change
```

---

# PART 11: QUICK REFERENCE — COLOR TOKEN USAGE

```
When you need a...                     Use this token
───────────────────────────────────────────────────────────
Page background                        colorScheme.background
Card background                        colorScheme.surfaceContainerHigh
Secondary card background              colorScheme.surfaceContainer
Selected/highlighted background        colorScheme.primaryContainer.copy(alpha = 0.3f)
Primary text on backgrounds            colorScheme.onSurface
Secondary text                         colorScheme.onSurfaceVariant
Primary accent / buttons               colorScheme.primary
Text on primary                        colorScheme.onPrimary
Filled button background               colorScheme.primary
Subtle accent background               colorScheme.primary.copy(alpha = 0.1f)
Divider                                colorScheme.outlineVariant.copy(alpha = 0.5f)
Icon tint (passive)                    colorScheme.onSurfaceVariant
Icon tint (active)                     colorScheme.primary
Error                                  colorScheme.error
Success/positive                       colorScheme.tertiary
Skeleton/placeholder                   colorScheme.onSurface.copy(alpha = 0.1f)
Gradient start                         colorScheme.primaryContainer
Gradient end                           colorScheme.surface
Chip container                         colorScheme.secondaryContainer
Chip text                              colorScheme.onSecondaryContainer
Status bar scrim                       colorScheme.background (auto-luminance check)
```

---

# SUMMARY

The difference between SpatialFlow's current "light and plain" feel and PixelPlayer's "clean and premium" feel comes down to:

1. **HCT color space** instead of HSL — perceptually accurate colors
2. **QuantizerCelebi + scoring + refinement** instead of simple averaging
3. **Full 50-token ColorScheme** instead of ~25 tokens
4. **AbsoluteSmoothCornerShape** instead of RoundedCornerShape
5. **Zero tonal elevation** everywhere instead of default shadows
6. **Every color from scheme tokens** instead of hardcoded hex values
7. **Coordinated typography** with rounded font axis + Montserrat display
8. **Caching pipeline** for instant color scheme retrieval

Implement these changes in the order specified in Part 10, and every screen, card, tab, and component in SpatialFlow will feel as polished and clean as PixelPlayer.
