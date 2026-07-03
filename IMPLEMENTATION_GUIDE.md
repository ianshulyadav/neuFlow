# SpatialFlow UI Transformation — Complete Implementation Guide

> **For AI Agents: Execute this entire document in order. Every instruction is mandatory.**
> **Target:** Make SpatialFlow's UI identical to PixelPlayer's quality level.
> **Date:** 2026-07-03

---

## ⚠️ CRITICAL: Before Starting

This guide assumes you are operating in the SpatialFlow project at:
```
/home/user/SpatialFlow/
```

All files are in the `app/src/main/java/com/codetrio/spatialflow/` directory.

---

## PART 0: BUILD CONFIGURATION (DO FIRST)

### 0.1 Add Gradle Dependencies

Edit `app/build.gradle.kts` and add:

```kotlin
dependencies {
    // SMOOTH CORNERS — squircle/continuous curve (MANDATORY)
    implementation("io.github.racra:compose-smooth-corner-rect:1.0.0")

    // ROOM — persistent color scheme cache
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // GOOGLE FONTS — Montserrat via Downloadable Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    // COIL — image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // MATERIAL — provides HCT color utilities transitively
    implementation("com.google.android.material:material:1.12.0")
}
```

### 0.2 Add Google Fonts Certificates

Create or update `res/values/arrays.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
</resources>
```

### 0.3 Add Room Entity to Database

In your Room `@Database` class, add `CachedColorScheme::class` to entities and expose `colorSchemeDao()`.

---

## PART 1: CORE THEME FILES (CREATE/OVERWRITE IN ORDER)

### File 1.1: `ui/theme/ColorExtractionEngine.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/ColorExtractionEngine.kt`
**Status:** ✅ ALREADY CREATED (228 lines)
**Purpose:** HCT color extraction with QuantizerCelebi, weighted scoring, neutral detection, grayscale fallback, 4 palette styles.

### File 1.2: `ui/theme/Shape.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/Shape.kt`
**Status:** ✅ ALREADY CREATED (31 lines)
**Purpose:** `AppShapes` (8/12/16/24/32dp) + `CardCorners` (28dp/60% smoothness).

### File 1.3: `ui/theme/Type.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/Type.kt`
**Status:** ✅ ALREADY UPDATED
**Purpose:** `GoogleSansRounded` (ROND=100), `MontserratFamily`, full `Typography`.

### File 1.4: `ui/theme/Color.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/Color.kt`
**Status:** ✅ ALREADY UPDATED
**Purpose:** Fallback color tokens only.

### File 1.5: `ui/theme/Theme.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/Theme.kt`
**Status:** ✅ ALREADY UPDATED
**Purpose:** `SpatialFlowTheme` with `albumColorSchemePair` override, `Crossfade` animation, transparent bars.

### File 1.6: `ui/theme/ColorSchemeCache.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/theme/ColorSchemeCache.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Two-tier caching (memory LRU + Room).

---

## PART 2: SHARED COMPONENTS (CREATE IN ORDER)

### File 2.1: `ui/components/SpatialFlowCard.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/SpatialFlowCard.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Universal card with `AbsoluteSmoothCornerShape`, zero elevation, `surfaceContainerHigh`.

### File 2.2: `ui/components/AutoScrollingText.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/AutoScrollingText.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Marquee text with gradient-faded edges.

### File 2.3: `ui/components/GradientHeader.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/GradientHeader.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Page header with vertical gradient.

### File 2.4: `ui/components/SectionHeader.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/SectionHeader.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Consistent section title.

### File 2.5: `ui/components/SectionDivider.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/SectionDivider.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Consistent divider with `outlineVariant` at 30% alpha.

### File 2.6: `ui/components/SongListItem.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/SongListItem.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Animated highlight song row.

### File 2.7: `ui/components/ActionSheet.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/components/ActionSheet.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Bottom sheet for more-options menus.

---

## PART 3: PLAYER COMPONENTS (CREATE IN ORDER)

### File 3.1: `ui/player/MiniPlayer.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/player/MiniPlayer.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** EXACT PixelPlayer miniplayer clone.
**Key specs:** 44dp album art, 36dp buttons, 22dp icons, onPrimary/primary color inversion, TextHandleMove haptics.

### File 3.2: `ui/player/AnimatedPlaybackControls.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/player/AnimatedPlaybackControls.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** EXACT PixelPlayer animated controls clone.
**Key specs:** AbsoluteSmoothCornerShape on play/pause (60dp↔26dp), weight animation (1.1x/0.65x), 600ms skip lock, 220ms release delay.

### File 3.3: `ui/player/WavyMusicSlider.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/player/WavyMusicSlider.kt`
**Status:** ✅ ALREADY UPDATED
**Purpose:** Scheme-driven wavy progress slider.

### File 3.4: `ui/player/LyricsSheet.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/player/LyricsSheet.kt`
**Status:** ✅ ALREADY CREATED (711 lines)
**Purpose:** EXACT PixelPlayer lyrics sheet clone — immersive mode, animated lyrics, swipe-to-skip, vinyl rotation, fetch dialog, more sheet.

---

## PART 4: SCREEN FILES (CREATE/OVERWRITE)

### File 4.1: `ui/library/LibraryTabContent.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/library/LibraryTabContent.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Songs/Albums/Artists/Playlists tab content.

### File 4.2: `ui/explore/ExploreScreen.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/explore/ExploreScreen.kt`
**Status:** ✅ EXISTS (needs data connection)
**Purpose:** Fully wired explore screen with collapsible header + sections.

### File 4.3: `ui/settings/SettingsScreen.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/settings/SettingsScreen.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** M3E modal settings with smooth corner cards.

### File 4.4: `ui/onboarding/OnboardingScreen.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/ui/onboarding/OnboardingScreen.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** M3E enhanced setup cards with animated transitions.

---

## PART 5: DATA LAYER

### File 5.1: `data/cache/ColorSchemeDao.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/data/cache/ColorSchemeDao.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Room DAO for persistent color scheme storage.

### File 5.2: `data/lyrics/` (all existing)
**Paths:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/data/lyrics/*.kt`
**Status:** ✅ ALL EXIST (LrcLibApi, LrcParser, LyricLine, LyricWord, LyricsRepository, etc.)
**Purpose:** Complete lyrics data layer — API calls, parsing, caching, fetching.

---

## PART 6: VIEWMODEL INTEGRATION

### File 6.1: `viewmodel/PlayerSharedViewModel_ColorIntegration.kt`
**Path:** `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/viewmodel/PlayerSharedViewModel_ColorIntegration.kt`
**Status:** ✅ ALREADY CREATED
**Purpose:** Reference code for injecting color extraction into PlayerSharedViewModel.

---

## PART 7: VIEWMODEL INTEGRATION — THE CRITICAL WIRING

### 7.1 Open `PlayerSharedViewModel.kt` and ADD these fields:

```kotlin
import com.codetrio.spatialflow.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ADD inside the class body:
private val _currentColorScheme = MutableStateFlow<ColorSchemePair?>(null)
val currentColorScheme: StateFlow<ColorSchemePair?> = _currentColorScheme.asStateFlow()
private var currentPaletteStyle = PaletteStyle.default
```

### 7.2 ADD this method to PlayerSharedViewModel:

```kotlin
fun updateColorsForSong(song: SongItem) {
    viewModelScope.launch(Dispatchers.IO) {
        val uri = song.getAlbumArtUri() ?: run {
            _currentColorScheme.value = null
            return@launch
        }
        val cacheKey = "${uri}_${currentPaletteStyle.key}"

        // Memory cache
        ColorSchemeCache.get(cacheKey)?.let {
            _currentColorScheme.value = it
            return@launch
        }

        // Generate
        val bitmap = ColorSchemeCache.loadBitmapForExtraction(context, uri)
            ?: run { _currentColorScheme.value = null; return@launch }
        val seed = extractSeedColor(bitmap)
        val pair = generateColorSchemePair(seed, currentPaletteStyle)

        ColorSchemeCache.put(cacheKey, pair)
        _currentColorScheme.value = pair
        bitmap.recycle()
    }
}
```

### 7.3 CALL `updateColorsForSong()` whenever the current song changes:

```kotlin
// In your song-change handler:
fun onSongChanged(song: SongItem) {
    currentSong = song
    updateColorsForSong(song)
}
```

---

## PART 8: MAIN ACTIVITY WIRING

### 8.1 In `MainActivity.kt`, wrap content with the theme:

```kotlin
val colorScheme by viewModel.currentColorScheme.collectAsStateWithLifecycle()
SpatialFlowTheme(albumColorSchemePair = colorScheme) {
    // Your existing NavHost / content
}
```

---

## PART 9: UNIVERSAL RULES FOR EVERY NEW COMPONENT

When creating ANY new UI element, verify:

```
☐ 1. Colors from MaterialTheme.colorScheme.<token> only — NEVER Color(0xFF...) or Color.Red
☐ 2. tonalElevation = 0.dp on every Surface/Card
☐ 3. Corners from AppShapes or AbsoluteSmoothCornerShape with CardCorners
☐ 4. Text uses MaterialTheme.typography.<style>
☐ 5. Font family is GoogleSansRounded (set in Typography — just use the style)
☐ 6. Gradients from scheme tokens: Brush.verticalGradient(listOf(scheme.primaryContainer, Color.Transparent))
☐ 7. Dividers: HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
☐ 8. Icon tints from scheme tokens
☐ 9. Placeholders: scheme.onSurface.copy(alpha = 0.1f)
☐ 10. Padding: 16.dp horizontal, 8-12.dp vertical
```

### Color Token Reference

| Need | Use |
|---|---|
| Page bg | `scheme.background` |
| Card bg | `scheme.surfaceContainerHigh` |
| Secondary card | `scheme.surfaceContainer` |
| Highlight bg | `scheme.primaryContainer.copy(alpha = 0.3f)` |
| Primary text | `scheme.onSurface` |
| Secondary text | `scheme.onSurfaceVariant` |
| Accent/CTA | `scheme.primary` |
| Text on accent | `scheme.onPrimary` |
| Subtle accent | `scheme.primary.copy(alpha = 0.1f)` |
| Divider | `scheme.outlineVariant.copy(alpha = 0.5f)` |
| Passive icon | `scheme.onSurfaceVariant` |
| Active icon | `scheme.primary` |
| Error | `scheme.error` |
| Placeholder | `scheme.onSurface.copy(alpha = 0.1f)` |
| Gradient start | `scheme.primaryContainer` |
| Gradient end | `Color.Transparent` |
| Chip (unselected) | `scheme.surfaceVariant.copy(alpha = 0.5f)` |
| Chip (selected) | `scheme.primary` |

---

## PART 10: FILE CHECKLIST — VERIFY EVERYTHING EXISTS

Run this command to verify:

```bash
for f in \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/ColorExtractionEngine.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/Shape.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/Type.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/Color.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/Theme.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/theme/ColorSchemeCache.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/SpatialFlowCard.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/AutoScrollingText.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/GradientHeader.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/SectionHeader.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/SectionDivider.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/SongListItem.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/components/ActionSheet.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/player/MiniPlayer.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/player/AnimatedPlaybackControls.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/player/LyricsSheet.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/player/WavyMusicSlider.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/library/LibraryTabContent.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/explore/ExploreScreen.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/settings/SettingsScreen.kt" \
  "app/src/main/java/com/codetrio/spatialflow/ui/onboarding/OnboardingScreen.kt" \
  "app/src/main/java/com/codetrio/spatialflow/data/cache/ColorSchemeDao.kt" \
  "app/src/main/java/com/codetrio/spatialflow/viewmodel/PlayerSharedViewModel_ColorIntegration.kt"; do
  if [ -f "$f" ]; then echo "✅ $f"; else echo "❌ MISSING: $f"; fi
done
```

---

## SUMMARY: WHAT MAKES PIXELPLAYER'S UI SPECIAL

1. **HCT color space** → perceptually uniform colors
2. **QuantizerCelebi** → 128-color intelligent quantization
3. **Weighted scoring** → population + chroma + hue diversity
4. **Representative anchoring** → true-to-artwork seed color
5. **Local refinement** → smooth neighborhood blending
6. **Neutral artwork detection** → auto grayscale for B&W art
7. **4 palette styles** → Tonal Spot / Vibrant / Expressive / Fruit Salad
8. **50-token full ColorScheme** → every surface level computed
9. **AbsoluteSmoothCornerShape** → continuous-corner "squircles"
10. **Zero tonalElevation** → hierarchy through color, not shadow
11. **GoogleSansRounded** → ROND=100 soft rounded font
12. **Montserrat display** → stretched cinematic titles
13. **Crossfade animation** → smooth scheme transitions
14. **3-tier caching** → memory → Room → generate
15. **Every color from tokens** → automatic album-art theming

If every component follows these rules, SpatialFlow will look and feel identical to PixelPlayer.
