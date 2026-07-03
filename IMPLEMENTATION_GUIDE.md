# SpatialFlow → PixelPlayer-Quality UI — Complete Implementation Guide

> **For AI Agents: Execute in order. Every instruction is mandatory.**
> **Target:** Make SpatialFlow's UI identical to PixelPlayer's quality.
> **Date:** 2026-07-03

---

## ⚠️ STEP 0: BUILD GRADLE

Edit `app/build.gradle.kts` and ADD:

```kotlin
implementation("io.github.racra:compose-smooth-corner-rect:1.0.0")
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("com.google.android.material:material:1.12.0")
```

Create `res/values/arrays.xml` with Google Fonts certs (see BUILD_CONFIG.md).

---

## ⚠️ STEP 1: VERIFY ALL FILES EXIST

All 23 Kotlin files are in place under `/home/user/SpatialFlow/app/src/main/java/com/codetrio/spatialflow/`.

**Core Theme (6 files):**
- `ui/theme/ColorExtractionEngine.kt` — HCT color extraction with QuantizerCelebi
- `ui/theme/Shape.kt` — AppShapes + CardCorners
- `ui/theme/Type.kt` — GoogleSansRounded + Montserrat
- `ui/theme/Color.kt` — Fallback tokens
- `ui/theme/Theme.kt` — Root theme with Crossfade
- `ui/theme/ColorSchemeCache.kt` — Two-tier caching

**Shared Components (7 files):**
- `ui/components/SpatialFlowCard.kt` — Universal card
- `ui/components/AutoScrollingText.kt` — Marquee with fade
- `ui/components/GradientHeader.kt` — Page header
- `ui/components/SectionHeader.kt` — Section title
- `ui/components/SectionDivider.kt` — Divider
- `ui/components/SongListItem.kt` — Animated song row
- `ui/components/ActionSheet.kt` — More-options sheet

**Player (5 files):**
- `ui/player/MiniPlayer.kt` — EXACT PixelPlayer clone
- `ui/player/AnimatedPlaybackControls.kt` — EXACT PixelPlayer clone
- `ui/player/LyricsSheet.kt` — Full lyrics system (711 lines)
- `ui/player/WavyMusicSlider.kt` — Scheme-driven slider

**Screens (4 files):**
- `ui/library/LibraryTabContent.kt` — Songs/Albums/Artists/Playlists
- `ui/explore/ExploreScreen.kt` — Wired explore
- `ui/settings/SettingsScreen.kt` — M3E settings
- `ui/onboarding/OnboardingScreen.kt` — Setup cards

**Data + ViewModel (3 files):**
- `data/cache/ColorSchemeDao.kt` — Room cache
- `viewmodel/PlayerSharedViewModel_ColorIntegration.kt` — Wiring reference

---

## ⚠️ STEP 2: WIRE THE VIEWMODEL

Open `viewmodel/PlayerSharedViewModel.kt` and ADD:

```kotlin
// Fields
private val _currentColorScheme = MutableStateFlow<ColorSchemePair?>(null)
val currentColorScheme: StateFlow<ColorSchemePair?> = _currentColorScheme.asStateFlow()
private var currentPaletteStyle = PaletteStyle.default

// Method
fun updateColorsForSong(song: SongItem) {
    viewModelScope.launch(Dispatchers.IO) {
        val uri = song.getAlbumArtUri() ?: run { _currentColorScheme.value = null; return@launch }
        val cacheKey = "${uri}_${currentPaletteStyle.key}"
        ColorSchemeCache.get(cacheKey)?.let { _currentColorScheme.value = it; return@launch }
        val bitmap = ColorSchemeCache.loadBitmapForExtraction(context, uri) ?: run { _currentColorScheme.value = null; return@launch }
        val seed = extractSeedColor(bitmap); val pair = generateColorSchemePair(seed, currentPaletteStyle)
        ColorSchemeCache.put(cacheKey, pair); _currentColorScheme.value = pair; bitmap.recycle()
    }
}

// Call on song change:
// fun onSongChanged(song: SongItem) { currentSong = song; updateColorsForSong(song) }
```

## ⚠️ STEP 3: WIRE MAIN ACTIVITY

```kotlin
val colorScheme by viewModel.currentColorScheme.collectAsStateWithLifecycle()
SpatialFlowTheme(albumColorSchemePair = colorScheme) { /* NavHost */ }
```

---

## ⚠️ UNIVERSAL RULES (ALWAYS FOLLOW)

```
☐ Colors: MaterialTheme.colorScheme.<token> ONLY — never Color(0xFF...)
☐ Elevation: tonalElevation = 0.dp on every Surface/Card
☐ Corners: AppShapes or AbsoluteSmoothCornerShape with CardCorners
☐ Fonts: GoogleSansRounded (via Typography styles)
☐ Gradients: scheme.primaryContainer → Color.Transparent
☐ Dividers: scheme.outlineVariant.copy(alpha = 0.3f)
☐ Icons: tint from scheme tokens
```

### Color Token Quick Reference
| Need | Token |
|---|---|
| Page bg | `scheme.background` |
| Card bg | `scheme.surfaceContainerHigh` |
| Primary text | `scheme.onSurface` |
| Secondary text | `scheme.onSurfaceVariant` |
| Accent | `scheme.primary` |
| Text on accent | `scheme.onPrimary` |
| Divider | `scheme.outlineVariant.copy(alpha = 0.5f)` |
| Placeholder | `scheme.onSurface.copy(alpha = 0.1f)` |
| Error | `scheme.error` |

---

## WHAT MAKES IT PIXELPLAYER-QUALITY

1. HCT color space — perceptually uniform
2. QuantizerCelebi — 128-color intelligent quantization
3. Weighted scoring — population + chroma + hue diversity
4. Representative anchoring — true-to-artwork seed
5. Neutral detection — auto grayscale for B&W art
6. 4 palette styles — Tonal Spot/Vibrant/Expressive/Fruit Salad
7. 50-token ColorScheme — every surface computed
8. AbsoluteSmoothCornerShape — continuous squircle corners
9. Zero tonalElevation — hierarchy via color
10. GoogleSansRounded ROND=100 — soft rounded font
11. Crossfade animation — smooth scheme transitions
12. 3-tier caching — memory→Room→generate
