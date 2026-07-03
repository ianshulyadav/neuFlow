package com.codetrio.spatialflow.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.*
import kotlin.math.*

// ═══════════════════════════════════════════════════════
// HCT COLOR EXTRACTION ENGINE
// Core of PixelPlayer's color quality. Uses Google's
// Material Color Utilities (HCT color space, QuantizerCelebi,
// weighted scoring, representative anchoring, local refinement)
// ═══════════════════════════════════════════════════════

data class ScoringConfig(
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

data class ExtractConfig(
    val downscaleMaxDimension: Int = 128,
    val quantizerMaxColors: Int = 128,
    val scoring: ScoringConfig = ScoringConfig(),
    val accuracyLevel: Int = 0
) {
    val normalizedAccuracy: Double
        get() = accuracyLevel.coerceIn(0, 10).toDouble() / 10.0
}

enum class PaletteStyle(val key: String) {
    TONAL_SPOT("tonal_spot"),
    VIBRANT("vibrant"),
    EXPRESSIVE("expressive"),
    FRUIT_SALAD("fruit_salad");
    companion object { val default = TONAL_SPOT }
}

data class ColorSchemePair(val light: ColorScheme, val dark: ColorScheme)

// ── CACHES ──
private val seedCache = LruCache<Int, Color>(32)
private val schemeCache = LruCache<String, ColorSchemePair>(48)

fun clearAllCaches() { seedCache.evictAll(); schemeCache.evictAll() }

// ── CONSTANTS ──
private const val GRAY_CHROMA = 12.0; private const val NEUTRAL_CHROMA = 8.0
private const val HIGH_CHROMA = 18.0; private const val REQ_NEUTRAL = 0.92
private const val MAX_HIGH = 0.03; private const val MAX_WEIGHTED = 9.0
private const val MAX_DELTA = 10

// ═══════════════════════════════════════════════════════
// PUBLIC API — call these from ViewModel
// ═══════════════════════════════════════════════════════

fun extractSeedColor(bitmap: Bitmap, config: ExtractConfig = ExtractConfig()): Color {
    val key = 31 * bitmap.hashCode() + config.hashCode()
    seedCache.get(key)?.let { return it }
    val result = runCatching {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Color(selectSeedArgb(pixels, config))
    }.getOrElse { Color(0xFF6750A4) }
    seedCache.put(key, result); return result
}

fun generateColorSchemePair(seed: Color, style: PaletteStyle = PaletteStyle.default): ColorSchemePair {
    val key = "${seed.toArgb()}_${style.key}"
    schemeCache.get(key)?.let { return it }
    val result = runCatching {
        val hct = Hct.fromInt(seed.toArgb())
        val gray = hct.chroma <= GRAY_CHROMA && isNearGray(seed.toArgb())
        fun make(dark: Boolean): ColorScheme {
            val s = when (style) {
                PaletteStyle.TONAL_SPOT  -> SchemeTonalSpot(hct, dark, 0.0)
                PaletteStyle.VIBRANT     -> SchemeVibrant(hct, dark, 0.0)
                PaletteStyle.EXPRESSIVE  -> SchemeExpressive(hct, dark, 0.0)
                PaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, dark, 0.0)
            }
            return if (gray && dark) grayscaleScheme(s) else s.toComposeScheme()
        }
        ColorSchemePair(make(false), make(true))
    }.getOrElse {
        ColorSchemePair(
            androidx.compose.material3.lightColorScheme(),
            androidx.compose.material3.darkColorScheme()
        )
    }
    schemeCache.put(key, result); return result
}

// ═══════════════════════════════════════════════════════
// SEED SELECTION ALGORITHM
// ═══════════════════════════════════════════════════════

private data class Scored(val hct: Hct, val score: Double)
private data class RepColor(val argb: Int, val hct: Hct)

private fun selectSeedArgb(pixels: IntArray, config: ExtractConfig): Int {
    val fallback = averageArgb(pixels)
    val q = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)
    if (isNeutralArtwork(q) && isNearGray(fallback)) return fallback
    val rep = representativeColor(pixels, config.normalizedAccuracy)
    val ranked = scoreQuantized(q, config.scoring, fallback, rep, config.normalizedAccuracy)
    val sel = ranked.firstOrNull() ?: fallback
    return refineSeed(sel, pixels, rep, config)
}

private fun scoreQuantized(
    map: Map<Int, Int>, sc: ScoringConfig, fallback: Int, rep: RepColor?, acc: Double
): List<Int> {
    if (map.isEmpty()) return listOf(fallback)
    val hcts = mutableListOf<Hct>()
    val huePop = IntArray(360); var sum = 0.0
    for ((a, p) in map) { if (p <= 0) continue; val h = Hct.fromInt(a); hcts.add(h)
        huePop[MathUtils.sanitizeDegreesInt(h.hue.toInt())] += p; sum += p }
    if (sum <= 0.0) return listOf(fallback)

    val excited = DoubleArray(360)
    for (h in 0..359) { var s = 0.0; for (n in h-14..h+15) s += huePop[MathUtils.sanitizeDegreesInt(n)]; excited[h] = s / (sum * 30) }
    val tChroma = rep?.hct?.chroma?.coerceIn(12.0, 72.0) ?: sc.targetChroma

    val scored = hcts.map { h ->
        val prop = (map[h.toInt()] ?: 0).toDouble() / sum
        val ch = h.chroma
        val chScore = if (ch >= sc.cutoffChroma) {
            if (ch >= tChroma) sc.weightChromaAbove * (1.0 - ((ch - tChroma) / 100.0).coerceIn(0.0, 1.0))
            else sc.weightChromaBelow * (ch / tChroma)
        } else 0.0
        val exc = excited[MathUtils.sanitizeDegreesInt(h.hue.toInt())]
        val s = (prop * sc.weightProportion) + chScore - (exc.coerceAtMost(sc.cutoffExcitedProportion) * 0.5)
        Scored(h, s)
    }.sortedByDescending { it.score }

    val res = mutableListOf<Int>()
    for (sh in scored) {
        val a = sh.hct.toInt()
        if (res.none { diff -> val diffHue = MathUtils.differenceDegrees(Hct.fromInt(diff).hue, sh.hct.hue)
                diffHue < sc.minHueDifference || diffHue > sc.maxHueDifference } || res.isEmpty()
        ) { res.add(a); if (res.size >= sc.maxColorCount) break }
    }
    return res.ifEmpty { listOf(fallback) }
}

private fun representativeColor(pixels: IntArray, acc: Double): RepColor? {
    if (pixels.isEmpty()) return null
    val thresh = lerp(10.0, 6.0, acc); var r = 0L; var g = 0L; var b = 0L; var c = 0
    for (a in pixels) {
        if (((a ushr 24) and 0xFF) < 28) continue
        val rr = (a ushr 16) and 0xFF; val gg = (a ushr 8) and 0xFF; val bb = a and 0xFF
        if (rr + gg + bb < 36) continue
        if (Hct.fromInt(a).chroma >= thresh) { r += rr; g += gg; b += bb; c++ }
    }
    if (c <= 0 || c.toDouble() / pixels.size < 0.04) return null
    val argb = (0xFF shl 24) or ((r / c).toInt() shl 16) or ((g / c).toInt() shl 8) or (b / c).toInt()
    return RepColor(argb, Hct.fromInt(argb))
}

private fun refineSeed(candidate: Int, pixels: IntArray, rep: RepColor?, config: ExtractConfig): Int {
    val acc = config.normalizedAccuracy; val win = lerp(32.0, 18.0, acc)
    val candHct = Hct.fromInt(candidate); var lr = 0L; var lg = 0L; var lb = 0L; var lc = 0
    for (a in pixels) {
        val h = Hct.fromInt(a)
        if (MathUtils.differenceDegrees(candHct.hue, h.hue) <= win && h.chroma >= 6.0) {
            lr += (a ushr 16) and 0xFF; lg += (a ushr 8) and 0xFF; lb += a and 0xFF; lc++ }
    }
    val blend = lerp(0.42f, 0.72f, acc.toFloat())
    val refined = if (lc > 0) blendArgb(candidate,
        (0xFF shl 24) or ((lr / lc).toInt() shl 16) or ((lg / lc).toInt() shl 8) or (lb / lc).toInt(), blend
    ) else candidate
    if (rep != null) {
        val hWin = lerp(90.0, 52.0, acc)
        if (MathUtils.differenceDegrees(Hct.fromInt(refined).hue, rep.hct.hue) <= hWin)
            return blendArgb(refined, rep.argb, lerp(0.0f, 0.42f, acc.toFloat()))
    }
    return refined
}

// ── UTILITIES ──
private fun averageArgb(pixels: IntArray): Int {
    if (pixels.isEmpty()) return 0xFF6750A4.toInt()
    var r = 0L; var g = 0L; var b = 0L
    for (a in pixels) { r += (a ushr 16) and 0xFF; g += (a ushr 8) and 0xFF; b += a and 0xFF }
    val s = pixels.size.toLong()
    return (0xFF shl 24) or ((r / s).toInt() shl 16) or ((g / s).toInt() shl 8) or (b / s).toInt()
}

private fun isNeutralArtwork(map: Map<Int, Int>): Boolean {
    if (map.isEmpty()) return false
    var t = 0.0; var n = 0.0; var h = 0.0; var w = 0.0
    for ((a, p) in map) { if (p <= 0) continue; val pp = p.toDouble(); val ch = Hct.fromInt(a).chroma
        t += pp; w += ch * pp; if (ch <= NEUTRAL_CHROMA) n += pp; if (ch >= HIGH_CHROMA) h += pp }
    return t > 0.0 && n / t >= REQ_NEUTRAL && h / t <= MAX_HIGH && w / t <= MAX_WEIGHTED
}

private fun isNearGray(argb: Int): Boolean {
    val r = (argb ushr 16) and 0xFF; val g = (argb ushr 8) and 0xFF; val b = argb and 0xFF
    return maxOf(abs(r - g), abs(g - b), abs(r - b)) <= MAX_DELTA
}

private fun blendArgb(a: Int, b: Int, ratio: Float): Int {
    val inv = 1f - ratio.coerceIn(0f, 1f)
    fun c(s: Int) = ((((a ushr s) and 0xFF) * inv + ((b ushr s) and 0xFF) * ratio).roundToInt().coerceIn(0, 255))
    return (c(24) shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
}
private fun lerp(a: Double, b: Double, f: Double): Double = a + (b - a) * f.coerceIn(0.0, 1.0)
private fun lerp(a: Float, b: Float, f: Float): Float = a + (b - a) * f.coerceIn(0f, 1f)

// ── DYNAMIC SCHEME → COMPOSE COLORSCHEME ──
private fun DynamicScheme.toComposeScheme(): ColorScheme = ColorScheme(
    primary = Color(getPrimary()), onPrimary = Color(getOnPrimary()),
    primaryContainer = Color(getPrimaryContainer()), onPrimaryContainer = Color(getOnPrimaryContainer()),
    inversePrimary = Color(getInversePrimary()), secondary = Color(getSecondary()),
    onSecondary = Color(getOnSecondary()), secondaryContainer = Color(getSecondaryContainer()),
    onSecondaryContainer = Color(getOnSecondaryContainer()), tertiary = Color(getTertiary()),
    onTertiary = Color(getOnTertiary()), tertiaryContainer = Color(getTertiaryContainer()),
    onTertiaryContainer = Color(getOnTertiaryContainer()), background = Color(getBackground()),
    onBackground = Color(getOnBackground()), surface = Color(getSurface()),
    onSurface = Color(getOnSurface()), surfaceVariant = Color(getSurfaceVariant()),
    onSurfaceVariant = Color(getOnSurfaceVariant()), surfaceTint = Color(getSurfaceTint()),
    inverseSurface = Color(getInverseSurface()), inverseOnSurface = Color(getInverseOnSurface()),
    error = Color(getError()), onError = Color(getOnError()),
    errorContainer = Color(getErrorContainer()), onErrorContainer = Color(getOnErrorContainer()),
    outline = Color(getOutline()), outlineVariant = Color(getOutlineVariant()),
    scrim = Color(getScrim()), surfaceBright = Color(getSurfaceBright()),
    surfaceDim = Color(getSurfaceDim()), surfaceContainer = Color(getSurfaceContainer()),
    surfaceContainerHigh = Color(getSurfaceContainerHigh()),
    surfaceContainerHighest = Color(getSurfaceContainerHighest()),
    surfaceContainerLow = Color(getSurfaceContainerLow()),
    surfaceContainerLowest = Color(getSurfaceContainerLowest()),
    primaryFixed = Color(getPrimaryFixed()), primaryFixedDim = Color(getPrimaryFixedDim()),
    onPrimaryFixed = Color(getOnPrimaryFixed()), onPrimaryFixedVariant = Color(getOnPrimaryFixedVariant()),
    secondaryFixed = Color(getSecondaryFixed()), secondaryFixedDim = Color(getSecondaryFixedDim()),
    onSecondaryFixed = Color(getOnSecondaryFixed()), onSecondaryFixedVariant = Color(getOnSecondaryFixedVariant()),
    tertiaryFixed = Color(getTertiaryFixed()), tertiaryFixedDim = Color(getTertiaryFixedDim()),
    onTertiaryFixed = Color(getOnTertiaryFixed()), onTertiaryFixedVariant = Color(getOnTertiaryFixedVariant()),
)

private fun grayscaleScheme(scheme: DynamicScheme): ColorScheme {
    fun g(c: Color): Color {
        val hsl = FloatArray(3); androidx.core.graphics.ColorUtils.colorToHSL(c.toArgb(), hsl)
        hsl[0] = 0f; hsl[1] = 0f; return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
    val cs = scheme.toComposeScheme()
    return cs.copy(primary = g(cs.primary), onPrimary = g(cs.onPrimary),
        primaryContainer = g(cs.primaryContainer), onPrimaryContainer = g(cs.onPrimaryContainer),
        secondary = g(cs.secondary), onSecondary = g(cs.onSecondary),
        secondaryContainer = g(cs.secondaryContainer), onSecondaryContainer = g(cs.onSecondaryContainer),
        tertiary = g(cs.tertiary), onTertiary = g(cs.onTertiary),
        tertiaryContainer = g(cs.tertiaryContainer), onTertiaryContainer = g(cs.onTertiaryContainer),
        background = g(cs.background), onBackground = g(cs.onBackground),
        surface = g(cs.surface), onSurface = g(cs.onSurface),
        surfaceVariant = g(cs.surfaceVariant), onSurfaceVariant = g(cs.onSurfaceVariant),
        surfaceContainer = g(cs.surfaceContainer), surfaceContainerHigh = g(cs.surfaceContainerHigh),
        surfaceContainerHighest = g(cs.surfaceContainerHighest),
        surfaceContainerLow = g(cs.surfaceContainerLow), surfaceContainerLowest = g(cs.surfaceContainerLowest))
}
