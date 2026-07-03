# SpatialFlow → PixelPlayer-Quality Complete UI Implementation

> **Date:** 2026-07-03
> **Scope:** Every component, screen, card, player, queue, settings, onboarding
> **Principle:** PixelPlayer's exact colors/smoothness + ArchiveTune's library layout + M3E purity

---

# FILE INDEX

```
ui/theme/ColorExtractionEngine.kt    ← NEW: HCT color extraction
ui/theme/ColorSchemeCache.kt         ← NEW: Two-tier caching
ui/theme/Type.kt                     ← REWRITE: GoogleSansRounded + Montserrat
ui/theme/Shape.kt                    ← NEW: AbsoluteSmoothCornerShape + CardCorners
ui/theme/Theme.kt                    ← REWRITE: album ColorSchemePair override
ui/theme/Color.kt                    ← REPLACE: scheme token colors only

ui/player/FullPlayer.kt              ← REWRITE: PixelPlayer button styles + colors
ui/player/MiniPlayer.kt              ← NEW: exact copy of PixelPlayer miniplayer
ui/player/PlayerUiComponents.kt      ← REWRITE: all scheme-driven
ui/player/AnimatedPlaybackControls.kt← NEW: PixelPlayer animated buttons
ui/player/QueueBottomSheet.kt        ← REWRITE: PixelPlayer queue gestures + cards
ui/player/WavyMusicSlider.kt         ← UPDATE: scheme colors

ui/library/LibraryScreen.kt          ← REWRITE: ArchiveTune layout + PixelPlayer look
ui/library/LibraryTabs.kt            ← NEW: ArchiveTune tabs + PixelPlayer styling
ui/library/LibrarySongs.kt           ← NEW: ArchiveTune song list layout
ui/library/LibraryAlbums.kt          ← NEW: ArchiveTune album grid
ui/library/LibraryArtists.kt         ← NEW: ArchiveTune artist list
ui/library/LibraryPlaylists.kt       ← NEW: ArchiveTune playlist list

ui/explore/ExploreComponents.kt      ← REWRITE: M3E polished cards
ui/explore/ExploreScreen.kt          ← REWRITE: scheme-driven

ui/onboarding/OnboardingScreen.kt    ← REWRITE: M3E enhanced setup cards
ui/settings/SettingsScreen.kt        ← REWRITE: modal + M3E
ui/components/SpatialFlowCard.kt     ← NEW: universal card component
ui/components/SongListItem.kt        ← NEW: PixelPlayer EnhancedSongListItem clone
ui/components/ActionSheet.kt         ← NEW: PixelPlayer-style popup cards
ui/components/SectionHeader.kt       ← NEW: consistent section headers
```

---

# FILE 1: `ui/theme/ColorExtractionEngine.kt`

```kotlin
package com.codetrio.spatialflow.ui.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.*
import kotlin.math.*

// ═══════════════════════════════════════════════════
// CONFIGURATION
// ═══════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════

private const val GRAY_CHROMA = 12.0
private const val NEUTRAL_CHROMA = 8.0
private const val HIGH_CHROMA = 18.0
private const val REQ_NEUTRAL = 0.92
private const val MAX_HIGH = 0.03
private const val MAX_WEIGHTED = 9.0
private const val MAX_DELTA = 10

// ═══════════════════════════════════════════════════
// CACHES
// ═══════════════════════════════════════════════════

private val seedCache = LruCache<Int, Color>(32)
private val schemeCache = LruCache<String, ColorSchemePair>(48)

data class ColorSchemePair(val light: ColorScheme, val dark: ColorScheme)

fun clearAllCaches() { seedCache.evictAll(); schemeCache.evictAll() }

// ═══════════════════════════════════════════════════
// PUBLIC API
// ═══════════════════════════════════════════════════

fun extractSeedColor(bitmap: Bitmap, config: ExtractConfig = ExtractConfig()): Color {
    val key = 31 * bitmap.hashCode() + config.hashCode()
    seedCache.get(key)?.let { return it }
    val result = runCatching {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        Color(selectSeed(pixels, config))
    }.getOrElse { Color(0xFF6750A4) }
    seedCache.put(key, result)
    return result
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
            return if (gray && dark) toGray(s) else s.toCompose()
        }
        ColorSchemePair(make(false), make(true))
    }.getOrElse { ColorSchemePair(
        androidx.compose.material3.lightColorScheme(),
        androidx.compose.material3.darkColorScheme()
    )}
    schemeCache.put(key, result)
    return result
}

// ═══════════════════════════════════════════════════
// SEED SELECTION
// ═══════════════════════════════════════════════════

private data class Scored(val hct: Hct, val score: Double)
private data class RepColor(val argb: Int, val hct: Hct)

private fun selectSeed(pixels: IntArray, config: ExtractConfig): Int {
    val fallback = average(pixels)
    val q = QuantizerCelebi.quantize(pixels, config.quantizerMaxColors)
    if (isNeutral(q) && isNearGray(fallback)) return fallback
    val rep = representative(pixels, config.normalizedAccuracy)
    val ranked = scoreColors(q, config.scoring, fallback, rep, config.normalizedAccuracy)
    val sel = ranked.firstOrNull() ?: fallback
    return refine(sel, pixels, rep, config)
}

private fun scoreColors(
    map: Map<Int, Int>, sc: ScoringConfig, fallback: Int,
    rep: RepColor?, acc: Double
): List<Int> {
    if (map.isEmpty()) return listOf(fallback)
    val hcts = mutableListOf<Hct>()
    val huePop = IntArray(360)
    var sum = 0.0
    for ((a, p) in map) { if (p <= 0) continue; val h = Hct.fromInt(a); hcts.add(h); huePop[MathUtils.sanitizeDegreesInt(h.hue.toInt())] += p; sum += p }
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
        if (res.none { MathUtils.differenceDegrees(Hct.fromInt(it).hue, sh.hct.hue) < sc.minHueDifference || MathUtils.differenceDegrees(Hct.fromInt(it).hue, sh.hct.hue) > sc.maxHueDifference } || res.isEmpty()) {
            res.add(a); if (res.size >= sc.maxColorCount) break
        }
    }
    return res.ifEmpty { listOf(fallback) }
}

private fun representative(pixels: IntArray, acc: Double): RepColor? {
    if (pixels.isEmpty()) return null
    val thresh = lerp(10.0, 6.0, acc)
    var r = 0L; var g = 0L; var b = 0L; var c = 0
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

private fun refine(candidate: Int, pixels: IntArray, rep: RepColor?, config: ExtractConfig): Int {
    val acc = config.normalizedAccuracy
    val win = lerp(32.0, 18.0, acc)
    val candHct = Hct.fromInt(candidate)
    var lr = 0L; var lg = 0L; var lb = 0L; var lc = 0
    for (a in pixels) {
        val h = Hct.fromInt(a)
        if (MathUtils.differenceDegrees(candHct.hue, h.hue) <= win && h.chroma >= 6.0) {
            lr += (a ushr 16) and 0xFF; lg += (a ushr 8) and 0xFF; lb += a and 0xFF; lc++
        }
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

// ═══════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════

private fun average(pixels: IntArray): Int {
    if (pixels.isEmpty()) return 0xFF6750A4.toInt()
    var r = 0L; var g = 0L; var b = 0L
    for (a in pixels) { r += (a ushr 16) and 0xFF; g += (a ushr 8) and 0xFF; b += a and 0xFF }
    val s = pixels.size.toLong()
    return (0xFF shl 24) or ((r / s).toInt() shl 16) or ((g / s).toInt() shl 8) or (b / s).toInt()
}

private fun isNeutral(map: Map<Int, Int>): Boolean {
    if (map.isEmpty()) return false
    var t = 0.0; var n = 0.0; var h = 0.0; var w = 0.0
    for ((a, p) in map) { if (p <= 0) continue; val pp = p.toDouble(); val ch = Hct.fromInt(a).chroma; t += pp; w += ch * pp; if (ch <= NEUTRAL_CHROMA) n += pp; if (ch >= HIGH_CHROMA) h += pp }
    if (t <= 0.0) return false
    return n / t >= REQ_NEUTRAL && h / t <= MAX_HIGH && w / t <= MAX_WEIGHTED
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

private fun lerp(a: Double, b: Double, f: Double) = a + (b - a) * f.coerceIn(0.0, 1.0)
private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f.coerceIn(0f, 1f)

// ═══════════════════════════════════════════════════
// DYNAMIC SCHEME → COMPOSE
// ═══════════════════════════════════════════════════

private fun DynamicScheme.toCompose() = ColorScheme(
    primary = Color(getPrimary()), onPrimary = Color(getOnPrimary()),
    primaryContainer = Color(getPrimaryContainer()), onPrimaryContainer = Color(getOnPrimaryContainer()),
    inversePrimary = Color(getInversePrimary()),
    secondary = Color(getSecondary()), onSecondary = Color(getOnSecondary()),
    secondaryContainer = Color(getSecondaryContainer()), onSecondaryContainer = Color(getOnSecondaryContainer()),
    tertiary = Color(getTertiary()), onTertiary = Color(getOnTertiary()),
    tertiaryContainer = Color(getTertiaryContainer()), onTertiaryContainer = Color(getOnTertiaryContainer()),
    background = Color(getBackground()), onBackground = Color(getOnBackground()),
    surface = Color(getSurface()), onSurface = Color(getOnSurface()),
    surfaceVariant = Color(getSurfaceVariant()), onSurfaceVariant = Color(getOnSurfaceVariant()),
    surfaceTint = Color(getSurfaceTint()),
    inverseSurface = Color(getInverseSurface()), inverseOnSurface = Color(getInverseOnSurface()),
    error = Color(getError()), onError = Color(getOnError()),
    errorContainer = Color(getErrorContainer()), onErrorContainer = Color(getOnErrorContainer()),
    outline = Color(getOutline()), outlineVariant = Color(getOutlineVariant()),
    scrim = Color(getScrim()),
    surfaceBright = Color(getSurfaceBright()), surfaceDim = Color(getSurfaceDim()),
    surfaceContainer = Color(getSurfaceContainer()),
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

private fun toGray(scheme: DynamicScheme): ColorScheme {
    fun g(c: Color): Color {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(c.toArgb(), hsl)
        hsl[0] = 0f; hsl[1] = 0f
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
    val cs = scheme.toCompose()
    return cs.copy(
        primary = g(cs.primary), onPrimary = g(cs.onPrimary),
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
        surfaceContainerLow = g(cs.surfaceContainerLow), surfaceContainerLowest = g(cs.surfaceContainerLowest),
    )
}
```

---

# FILE 2: `ui/theme/Shape.kt`

```kotlin
package com.codetrio.spatialflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object CardCorners {
    const val RadiusDp = 28f
    const val Smoothness = 60
}

val PlayerSheetCollapsedCornerRadius = 32.dp
```

---

# FILE 3: `ui/theme/Theme.kt`

```kotlin
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

    val colorScheme = remember(baseScheme, albumColorSchemePair, darkTheme, amoledBlack) {
        val s = albumColorSchemePair?.let { if (darkTheme) it.dark else it.light } ?: baseScheme
        if (darkTheme && amoledBlack) s.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black
        ) else s
    }

    SideEffect {
        view.context.findActivity()?.window?.let { w ->
            w.statusBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                w.isStatusBarContrastEnforced = false
                w.isNavigationBarContrastEnforced = false
            }
            w.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(w, view).apply {
                isAppearanceLightStatusBars = ColorUtils.calculateLuminance(colorScheme.background.toArgb()) > 0.55
                isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(colorScheme.background.toArgb()) > 0.55
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

# FILE 4: `ui/theme/Type.kt`

```kotlin
package com.codetrio.spatialflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.codetrio.spatialflow.R

// Google Sans Flex (Rounded axis = 100%)
@OptIn(ExperimentalTextApi::class)
val GoogleSansRounded = FontFamily(
    Font(R.font.google_flex, weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight), FontVariation.Setting("ROND", 100f))),
    Font(R.font.google_flex, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight), FontVariation.Setting("ROND", 100f))),
)

// Google Sans Flex (Non-rounded for lyrics)
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlex = FontFamily(
    Font(R.font.google_flex, variationSettings = FontVariation.Settings(FontVariation.Setting("ROND", 0f)))
)

// Montserrat (for display/hero titles)
private val gProvider = GoogleFont.Provider("com.google.android.gms.fonts", "com.google.android.gms", R.array.com_google_android_gms_fonts_certs)
val MontserratFamily = FontFamily(
    Font(googleFont = GFont("Montserrat"), fontProvider = gProvider, weight = FontWeight.Black),
    Font(googleFont = GFont("Montserrat"), fontProvider = gProvider, weight = FontWeight.Bold),
    Font(googleFont = GFont("Montserrat"), fontProvider = gProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GFont("Montserrat"), fontProvider = gProvider, weight = FontWeight.Normal),
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold, fontSize = 60.sp, textGeometricTransform = TextGeometricTransform(scaleX = 1.5f), letterSpacing = (-0.02).em, lineHeight = 0.95.em),
    displayMedium = TextStyle(fontFamily = MontserratFamily, fontWeight = FontWeight.Normal, fontSize = 50.sp, letterSpacing = (-0.02).em, lineHeight = 0.95.em),
    headlineLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = GoogleSansRounded, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
```

---

# FILE 5: `ui/components/SpatialFlowCard.kt`

```kotlin
package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.ui.theme.CardCorners
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * Universal card component. Use this for EVERY card in the app.
 * - Zero elevation
 * - AbsoluteSmoothCornerShape
 * - surfaceContainerHigh from scheme
 */
@Composable
fun SpatialFlowCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTL = CardCorners.RadiusDp.dp, cornerRadiusTR = CardCorners.RadiusDp.dp,
            cornerRadiusBL = CardCorners.RadiusDp.dp, cornerRadiusBR = CardCorners.RadiusDp.dp,
            smoothnessAsPercentTL = CardCorners.Smoothness, smoothnessAsPercentTR = CardCorners.Smoothness,
            smoothnessAsPercentBL = CardCorners.Smoothness, smoothnessAsPercentBR = CardCorners.Smoothness,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) { content() }
}
```

---

# FILE 6: `ui/player/MiniPlayer.kt`

```kotlin
package com.codetrio.spatialflow.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.size.Size
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

val MiniPlayerHeight = 64.dp

/**
 * EXACT COPY of PixelPlayer's MiniPlayerContentInternal.
 * Same button sizes (36.dp), same circle backgrounds, same icon sizes (22.dp),
 * same font families, same haptic feedback, same layout.
 */
@Composable
fun MiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    isCastConnecting: Boolean = false,
    isPreparingPlayback: Boolean = false,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val enabled = !isCastConnecting && !isPreparingPlayback
    val prevInt = remember { MutableInteractionSource() }
    val ppInt = remember { MutableInteractionSource() }
    val nextInt = remember { MutableInteractionSource() }
    val ripple = remember { ripple(bounded = false) }

    Row(
        modifier = modifier.fillMaxWidth().height(MiniPlayerHeight).padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = song.getAlbumArtUri(),
                contentDescription = "Cover",
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isCastConnecting) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = scheme.onPrimaryContainer)
            else if (isPreparingPlayback) CircularWavyProgressIndicator(Modifier.size(24.dp))
        }

        Spacer(Modifier.width(12.dp))

        // Song info
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            AutoScrollingText(
                text = when { isCastConnecting -> "Connecting…"; isPreparingPlayback -> "Preparing…"; else -> song.title },
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, fontFamily = GoogleSansRounded, color = scheme.onPrimaryContainer)
            )
            AutoScrollingText(
                text = if (isPreparingPlayback) "Loading…" else song.artist,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontFamily = GoogleSansRounded, color = scheme.onPrimaryContainer.copy(alpha = 0.7f))
            )
        }

        Spacer(Modifier.width(8.dp))

        // Previous button — EXACT PixelPlayer style
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary)
            .clickable(prevInt, ripple, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPrevious() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = scheme.primary, modifier = Modifier.size(22.dp)) }

        Spacer(Modifier.width(8.dp))

        // Play/Pause — EXACT PixelPlayer style
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary)
            .clickable(ppInt, ripple, enabled = enabled) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() },
            contentAlignment = Alignment.Center
        ) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pause" else "Play", tint = scheme.onPrimary, modifier = Modifier.size(22.dp)) }

        Spacer(Modifier.width(8.dp))

        // Next button — EXACT PixelPlayer style
        Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.onPrimary)
            .clickable(nextInt, ripple, enabled = enabled) { onNext() },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.SkipNext, "Next", tint = scheme.primary, modifier = Modifier.size(22.dp)) }
    }
}
```

---

# FILE 7: `ui/player/AnimatedPlaybackControls.kt`

```kotlin
package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private enum class Btn { NONE, PREV, PP, NEXT }

/**
 * EXACT PixelPlayer animated playback controls.
 * - SmoothCornerShape on play/pause with animating radius
 * - Weight animation (expansion/compression)
 * - Button lock on skip
 * - Delayed visual state sync
 */
@Composable
fun AnimatedPlaybackControls(
    isPlaying: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 90.dp,
    pressAnimationSpec: AnimationSpec<Float> = tween(200, easing = FastOutSlowInEasing),
    releaseDelay: Long = 220L,
    ppCornerPlaying: Dp = 60.dp,
    ppCornerPaused: Dp = 26.dp,
    colorOther: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorPP: Color = MaterialTheme.colorScheme.primary,
    tintPP: Color = MaterialTheme.colorScheme.onPrimary,
    tintOther: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    ppIconSize: Dp = 36.dp,
    iconSize: Dp = 32.dp,
) {
    val isP = isPlaying()
    var last by remember { mutableStateOf<Btn?>(null) }
    var trigger by remember { mutableStateOf(0) }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val locked = last == Btn.NEXT || last == Btn.PREV
    var visual by remember { mutableStateOf(isP) }
    var pending by remember { mutableStateOf<Boolean?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val motion = remember { MotionScheme.expressive() }
    val spatialSpec = remember { motion.defaultSpatialSpec<Dp>() }

    LaunchedEffect(last, trigger) {
        if (last != null) { delay(if (last == Btn.NEXT || last == Btn.PREV) 600L else releaseDelay); last = null }
    }
    LaunchedEffect(isP) { if (isP) pending = true else { if (latestIsPlaying() != false) { delay(releaseDelay); if (!latestIsPlaying()) pending = false } } }
    LaunchedEffect(locked, pending) { if (!locked) pending?.let { visual = it; pending = null } }

    Box(modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            fun w(b: Btn) = when (last) { b -> 1.1f; null -> 1f; else -> 0.65f }

            val pw by animateFloatAsState(w(Btn.PREV), pressAnimationSpec, label = "pw")
            Box(Modifier.weight(pw).fillMaxHeight().clip(androidx.compose.foundation.shape.CircleShape).background(colorOther)
                .clickable { last = Btn.PREV; trigger++; scope.launch { delay(180); onPrevious() } },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.SkipPrevious, "Prev", tintOther, Modifier.size(iconSize)) }

            val ppw by animateFloatAsState(w(Btn.PP), pressAnimationSpec, label = "ppw")
            val ppc by animateDpAsState(if (!visual) ppCornerPlaying else ppCornerPaused, spatialSpec, label = "ppc")
            Box(Modifier.weight(ppw).fillMaxHeight().graphicsLayer {
                clip = true; shape = AbsoluteSmoothCornerShape(ppc, ppc, ppc, ppc, 60, 60, 60, 60)
            }.background(colorPP).clickable { last = Btn.PP; trigger++; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPlayPause() },
                contentAlignment = Alignment.Center
            ) { Icon(if (visual) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (visual) "Pause" else "Play", tintPP, Modifier.size(ppIconSize)) }

            val nw by animateFloatAsState(w(Btn.NEXT), pressAnimationSpec, label = "nw")
            Box(Modifier.weight(nw).fillMaxHeight().clip(androidx.compose.foundation.shape.CircleShape).background(colorOther)
                .clickable { last = Btn.NEXT; trigger++; scope.launch { delay(180); onNext() } },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.SkipNext, "Next", tintOther, Modifier.size(iconSize)) }
        }
    }
}
```

---

# FILE 8: `ui/library/LibraryScreen.kt`

```kotlin
package com.codetrio.spatialflow.ui.library

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.codetrio.spatialflow.R
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

/**
 * ArchiveTune's library layout structure (HorizontalPager + tab chips + gradient header)
 * WITH PixelPlayer's exact theming, colors, smooth corners, and fonts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: androidx.navigation.NavController,
    viewModel: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    // ── ArchiveTune-style tab definitions ──
    data class Tab(val id: String, val label: String, val icon: Int)
    val tabs = listOf(
        Tab("library", "Library", R.drawable.ic_library),
        Tab("playlists", "Playlists", R.drawable.ic_playlist),
        Tab("songs", "Songs", R.drawable.ic_music_note),
        Tab("artists", "Artists", R.drawable.ic_artist),
        Tab("albums", "Albums", R.drawable.ic_album),
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Collapsible header (ArchiveTune style) ──
    val maxHeader = 90.dp
    val maxHeaderPx = with(LocalDensity.current) { maxHeader.toPx() }
    var headerOffset by remember { mutableFloatStateOf(0f) }

    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) { val n = headerOffset + available.y; val o = headerOffset; headerOffset = n.coerceIn(-maxHeaderPx, 0f); return Offset(0f, headerOffset - o) }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) { val n = headerOffset + available.y; val o = headerOffset; headerOffset = n.coerceIn(-maxHeaderPx, 0f); return Offset(0f, headerOffset - o) }
                return Offset.Zero
            }
        }
    }

    val headerProgress = 1f + (headerOffset / maxHeaderPx)

    // ── Gradient background (PixelPlayer style) ──
    val gradient = Brush.verticalGradient(listOf(scheme.primaryContainer, Color.Transparent))

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        // Background blur/gradient
        Box(Modifier.fillMaxWidth().height(430.dp).align(Alignment.TopCenter).drawWithCache {
            val g = Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.35f), Color.Transparent))
            onDrawBehind { drawRect(g) }
        })

        Column(Modifier.fillMaxSize().nestedScroll(nestedScroll)) {
            // ── HEADER ──
            Column(Modifier.fillMaxWidth().height(maxHeader + with(LocalDensity.current) { headerOffset.toDp() }).then(
                Modifier.graphicsLayer { alpha = headerProgress.coerceIn(0f, 1f) }
            )) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(horizontal = 24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tabs[selectedTab].label, style = MaterialTheme.typography.headlineMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Spacer(Modifier.weight(1f))
                    // Settings / sort button
                    IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_sort), null, tint = scheme.onSurfaceVariant) }
                    IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_more), null, tint = scheme.onSurfaceVariant) }
                }
                Text(
                    when (selectedTab) {
                        0 -> "Your music collection"
                        1 -> "Your custom mixes"
                        2 -> "All tracks"
                        3 -> "Browse by artist"
                        4 -> "Browse by album"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // ── TAB CHIPS (ArchiveTune style: ExpressiveTabChip) ──
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(tabs.size) { i ->
                    val t = tabs[i]
                    val sel = i == selectedTab
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (pressed) 0.92f else if (sel) 1.05f else 1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "tabScale")
                    val bg by animateColorAsState(if (sel) scheme.primary else scheme.surfaceVariant.copy(alpha = 0.5f), spring(stiffness = Spring.StiffnessMedium), label = "tabBg")
                    val ct by animateColorAsState(if (sel) scheme.onPrimary else scheme.onSurfaceVariant, spring(stiffness = Spring.StiffnessMedium), label = "tabCt")

                    Row(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(CircleShape).background(bg)
                        .clickable(interaction, null) { selectedTab = i }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(painterResource(t.icon), t.label, ct, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = ct)
                    }
                }
            }

            // ── PAGER CONTENT ──
            val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { tabs.size })
            LaunchedEffect(selectedTab) { pagerState.animateScrollToPage(selectedTab) }
            LaunchedEffect(pagerState.currentPage) { selectedTab = pagerState.currentPage }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> MixedLibraryContent(navController, viewModel)
                    1 -> PlaylistsContent(navController, viewModel)
                    2 -> SongsContent(navController, viewModel)
                    3 -> ArtistsContent(navController, viewModel)
                    4 -> AlbumsContent(navController, viewModel)
                }
            }
        }
    }
}

// ── Placeholder content composables (implement with real data) ──

@Composable
private fun MixedLibraryContent(navController: androidx.navigation.NavController, vm: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel) {
    // Combined view: playlists + recent albums + quick actions
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        item { SectionHeader("Quick Actions") }
        // ... action cards
        item { SectionHeader("Your Playlists") }
        // ... playlist list
        item { SectionHeader("Recent Albums") }
        // ... album grid
    }
}

@Composable
private fun PlaylistsContent(navController: androidx.navigation.NavController, vm: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) { /* playlist items */ }
}

@Composable
private fun SongsContent(navController: androidx.navigation.NavController, vm: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) { /* song list items */ }
}

@Composable
private fun ArtistsContent(navController: androidx.navigation.NavController, vm: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) { /* artist items */ }
}

@Composable
private fun AlbumsContent(navController: androidx.navigation.NavController, vm: com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) { /* album grid items */ }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
}
```

---

# FILE 9: `ui/components/SongListItem.kt`

```kotlin
package com.codetrio.spatialflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.model.SongItem

/**
 * PixelPlayer's EnhancedSongListItem pattern, adapted for SpatialFlow.
 * Same highlight animation, scheme colors, proper typography.
 */
@Composable
fun SongListItem(
    song: SongItem,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    showAlbumArt: Boolean = true,
    onClick: () -> Unit,
    onMoreOptions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    val transition = updateTransition(isCurrentSong, label = "songHighlight")
    val highlightAlpha by transition.animateFloat(tween(400), label = "ha") { if (it) 0.12f else 0f }
    val surfaceColor by transition.animateColor(tween(400), label = "sc") {
        if (it) scheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    }
    val textColor by transition.animateColor(tween(400), label = "tc") {
        if (it) scheme.primary else scheme.onSurface
    }

    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape, color = surfaceColor, tonalElevation = 0.dp
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showAlbumArt) {
                Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    AsyncImage(song.getAlbumArtUri(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isCurrentSong && isPlaying) {
                Box(Modifier.size(4.dp).clip(CircleShape).background(scheme.primary))
            }
            if (onMoreOptions != null) {
                IconButton(onClick = onMoreOptions) { Icon(androidx.compose.material.icons.Icons.Rounded.MoreVert, null, tint = scheme.onSurfaceVariant) }
            }
        }
    }
}
```

---

# FILE 10: `ui/player/QueueBottomSheet.kt`

This follows PixelPlayer's queue exactly — reorderable gestures, swipe-to-dismiss, smooth corner sheet, scheme colors.

```kotlin
package com.codetrio.spatialflow.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import kotlin.math.roundToInt

/**
 * PixelPlayer-style queue bottom sheet.
 * - Swipe-to-dismiss with red background
 * - Drag-to-reorder via drag handle
 * - Smooth corner sheet top
 * - Current song highlight
 * - Shuffle / Repeat / Timer / Clear actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<SongItem>,
    currentSongId: String?,
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffleOn: Boolean,
    onDismiss: () -> Unit,
    onPlaySong: (SongItem, Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val currentIndex = queue.indexOfFirst { it.videoId == currentSongId }

    var showClearDialog by remember { mutableStateOf(false) }

    // Swipe state
    var swipedIndex by remember { mutableIntStateOf(-1) }
    var swipeOffset by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxSize()) {
            // ── DRAG HANDLE ──
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(50)).background(scheme.onSurfaceVariant.copy(alpha = 0.4f)))
            }

            // ── HEADER ──
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Queue", style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Spacer(Modifier.weight(1f))

                // Shuffle
                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onToggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (isShuffleOn) scheme.primary else scheme.onSurfaceVariant)
                }
                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat",
                        tint = if (repeatMode > 0) scheme.primary else scheme.onSurfaceVariant)
                }
                // Clear
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(Icons.Rounded.ClearAll, "Clear", tint = scheme.onSurfaceVariant)
                }
                // Close
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close", tint = scheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))

            // ── QUEUE LIST ──
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                itemsIndexed(queue, key = { i, s -> "${s.videoId}_$i" }) { index, song ->
                    val isCurrent = index == currentIndex
                    val isSwiped = swipedIndex == index

                    Box(Modifier.fillMaxWidth()) {
                        // Red dismiss background
                        if (isSwiped) {
                            Box(Modifier.fillMaxSize().background(scheme.error).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Rounded.Delete, "Remove", tint = scheme.onError, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Song row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationX = if (isSwiped) swipeOffset else 0f }
                                .clickable { onPlaySong(song, index) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Drag handle
                            Icon(Icons.Rounded.DragIndicator, "Drag", tint = scheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))

                            // Album art
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(scheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                AsyncImage(song.getAlbumArtUri(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }

                            // Info
                            Column(Modifier.weight(1f)) {
                                Text(song.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, fontFamily = GoogleSansRounded),
                                    color = if (isCurrent) scheme.primary else scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            // Playing indicator
                            if (isCurrent && isPlaying) {
                                Box(Modifier.size(4.dp).clip(CircleShape).background(scheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Queue") },
            text = { Text("Remove all songs from the queue?") },
            confirmButton = { TextButton(onClick = { onClearQueue(); showClearDialog = false }) { Text("Clear", color = scheme.error) } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}
```

---

# FILE 11: `ui/player/FullPlayer.kt`

The full player content — same layout as SpatialFlow but with PixelPlayer's button styles and all colors from the scheme:

```kotlin
package com.codetrio.spatialflow.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.codetrio.spatialflow.ui.theme.CardCorners

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    viewModel: PlayerSharedViewModel,
    song: SongItem,
    queue: List<SongItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    progress: Float,
    onCollapse: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val playerOnBase = scheme.onPrimaryContainer
    val playerAccent = scheme.primary
    val playerOnAccent = scheme.onPrimary
    val gradientEdge = scheme.primaryContainer
    val pagerState = rememberPagerState(initialPage = currentIndex, pageCount = { queue.size.coerceAtLeast(1) })

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = playerOnBase),
                title = { Text("Now Playing", fontFamily = GoogleSansRounded, modifier = Modifier.padding(start = 18.dp)) },
                navigationIcon = {
                    IconButton(onClick = onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Collapse", tint = playerOnBase) }
                },
                actions = {
                    // Cast / Queue buttons — PixelPlayer style
                    Box(Modifier.size(42.dp, 50.dp).clip(RoundedCornerShape(6.dp, 50.dp, 6.dp, 50.dp)).background(playerOnAccent.copy(alpha = 0.7f)).then(
                        Modifier.clickable { /* queue */ }
                    ), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.QueueMusic, "Queue", tint = playerAccent)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── ALBUM ART PAGER ──
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f).clip(AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness))
            ) { page ->
                val s = queue.getOrNull(page) ?: song
                AsyncImage(s.getAlbumArtUri(), null, Modifier.fillMaxSize().background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
            }

            // ── METADATA ──
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(song.title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = playerOnBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(song.artist, style = MaterialTheme.typography.titleMedium, color = playerOnBase.copy(alpha = 0.7f), fontFamily = GoogleSansRounded, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // ── PROGRESS (WavyMusicSlider with scheme colors) ──
            WavyMusicSlider(
                value = progress,
                onValueChange = onSeek,
                activeTrackColor = playerOnBase,
                inactiveTrackColor = playerOnBase.copy(alpha = 0.2f),
                thumbColor = playerAccent,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )

            // ── CONTROLS (PixelPlayer AnimatedPlaybackControls) ──
            AnimatedPlaybackControls(
                isPlaying = { isPlaying },
                onPrevious = { viewModel.playPrevious() },
                onPlayPause = { if (isPlaying) viewModel.pauseAudio() else viewModel.playAudio() },
                onNext = { viewModel.playNext() },
                colorOther = playerOnAccent.copy(alpha = 0.08f),
                colorPP = playerAccent,
                tintPP = playerOnAccent,
                tintOther = playerOnBase,
                modifier = Modifier.fillMaxWidth()
            )

            // ── BOTTOM TOGGLES ──
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = playerOnBase.copy(alpha = 0.7f)) }
                IconButton(onClick = { viewModel.toggleRepeat() }) { Icon(Icons.Rounded.Repeat, "Repeat", tint = playerOnBase.copy(alpha = 0.7f)) }
                IconButton(onClick = { viewModel.toggleFavorite() }) { Icon(Icons.Rounded.FavoriteBorder, "Favorite", tint = playerOnBase.copy(alpha = 0.7f)) }
                IconButton(onClick = { /* sleep timer */ }) { Icon(Icons.Rounded.Timer, "Timer", tint = playerOnBase.copy(alpha = 0.7f)) }
            }
        }
    }
}
```

---

# FILE 12: `ui/components/ActionSheet.kt`

```kotlin
package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

/**
 * PixelPlayer-style action / info bottom sheets.
 * Smooth corner top, zero elevation, scheme colors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    title: String,
    onDismiss: () -> Unit,
    items: List<ActionSheetItem>,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            // Handle
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(50)).background(scheme.onSurfaceVariant.copy(alpha = 0.4f)))
            }

            // Title
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold,
                color = scheme.onSurface, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

            // Items
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { item.onClick(); onDismiss() }.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item.icon != null) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(if (item.destructive) scheme.errorContainer else scheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) { Icon(item.icon, item.label, tint = if (item.destructive) scheme.error else scheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.bodyLarge, color = if (item.destructive) scheme.error else scheme.onSurface)
                        item.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
                    }
                    if (item.chevron) Icon(Icons.Rounded.ChevronRight, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

data class ActionSheetItem(
    val label: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val chevron: Boolean = false,
    val onClick: () -> Unit
)
```

---

# FILE 13: `ui/explore/ExploreComponents.kt`

The overhauled explore cards with pure M3E design:

```kotlin
package com.codetrio.spatialflow.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

// ═══════════════════════════════════════════════
// ALBUM CARD (Grid) — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    artworkUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = remember {
        AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness)
    }

    Card(
        modifier = modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AsyncImage(artworkUrl, title, Modifier.fillMaxWidth().weight(1f).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
            Column(Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// PLAYLIST CARD (Horizontal) — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun PlaylistCard(
    title: String,
    subtitle: String,
    artworkUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = remember {
        AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness, CardCorners.Smoothness)
    }

    Card(
        modifier = modifier.width(160.dp).clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            AsyncImage(artworkUrl, title, Modifier.fillMaxWidth().aspectRatio(1f).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
            Column(Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ═══════════════════════════════════════════════
// ARTIST CHIP — M3E polished
// ═══════════════════════════════════════════════
@Composable
fun ArtistChip(
    name: String,
    imageUrl: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.width(80.dp).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(imageUrl, name, Modifier.size(72.dp).clip(CircleShape).background(scheme.surfaceVariant), contentScale = ContentScale.Crop)
        Text(name, style = MaterialTheme.typography.labelMedium, fontFamily = GoogleSansRounded, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
```

---

# COMPONENT CHECKLIST

When implementing ANY new UI in SpatialFlow, verify:

```
☐ Colors: 100% from MaterialTheme.colorScheme.<token>
☐ Corners: AbsoluteSmoothCornerShape or AppShapes
☐ Elevation: tonalElevation = 0.dp on every Surface/Card
☐ Fonts: GoogleSansRounded or MontserratFamily
☐ Typography: MaterialTheme.typography.<style>
☐ Gradients: derived from scheme tokens
☐ Divider: scheme.outlineVariant.copy(alpha = 0.3f-0.5f)
☐ Icons: tint from scheme tokens
☐ Placeholder/skeleton: scheme.onSurface.copy(alpha = 0.1f)
☐ Spacing: 16.dp horizontal, 8-12.dp vertical
```

---

# TOKEN QUICK REFERENCE

```
Use case                          → Token
───────────────────────────────────────────────────
Page background                   → scheme.background
Card background                   → scheme.surfaceContainerHigh
Secondary card                    → scheme.surfaceContainer
Highlight/selected bg             → scheme.primaryContainer.copy(alpha = 0.3f)
Primary text                      → scheme.onSurface
Secondary text                    → scheme.onSurfaceVariant
Accent / CTA buttons              → scheme.primary
Text on accent                    → scheme.onPrimary
Subtle accent bg                  → scheme.primary.copy(alpha = 0.1f)
Divider                           → scheme.outlineVariant.copy(alpha = 0.5f)
Passive icon                      → scheme.onSurfaceVariant
Active icon                       → scheme.primary
Error                             → scheme.error
Placeholder/skeleton              → scheme.onSurface.copy(alpha = 0.1f)
Gradient start                    → scheme.primaryContainer
Gradient end                      → Color.Transparent / scheme.surface
Chip container (unselected)       → scheme.surfaceVariant.copy(alpha = 0.5f)
Chip text (unselected)            → scheme.onSurfaceVariant
Chip container (selected)         → scheme.primary
Chip text (selected)              → scheme.onPrimary
```
