package com.codetrio.spatialflow.viewmodel

/*
 * Color integration reference for PlayerSharedViewModel.
 * Copy these fields and methods into the EXISTING PlayerSharedViewModel class.
 *
 * ── ADD THESE FIELDS ──
 * private val _currentColorScheme = MutableStateFlow<ColorSchemePair?>(null)
 * val currentColorScheme: StateFlow<ColorSchemePair?> = _currentColorScheme.asStateFlow()
 * private var currentPaletteStyle = PaletteStyle.default
 *
 * ── ADD THIS METHOD ──
 * fun updateColorsForSong(song: SongItem) {
 *     viewModelScope.launch(Dispatchers.IO) {
 *         val uri = song.getAlbumArtUri() ?: run { _currentColorScheme.value = null; return@launch }
 *         val cacheKey = "\${uri}_\${currentPaletteStyle.key}"
 *         ColorSchemeCache.get(cacheKey)?.let { _currentColorScheme.value = it; return@launch }
 *         val bitmap = ColorSchemeCache.loadBitmapForExtraction(context, uri)
 *             ?: run { _currentColorScheme.value = null; return@launch }
 *         val seed = extractSeedColor(bitmap)
 *         val pair = generateColorSchemePair(seed, currentPaletteStyle)
 *         ColorSchemeCache.put(cacheKey, pair)
 *         _currentColorScheme.value = pair
 *         bitmap.recycle()
 *     }
 * }
 *
 * ── IN MainActivity / Root Composable ──
 * val colorScheme by viewModel.currentColorScheme.collectAsStateWithLifecycle()
 * SpatialFlowTheme(albumColorSchemePair = colorScheme) { NavHost(...) }
 */
