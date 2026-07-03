package com.codetrio.spatialflow.data.lyrics

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════
// LYRICS DATA MODELS
// ═══════════════════════════════════════════════════

data class Lyrics(
    val synced: List<SyncedLine>? = null,
    val plain: List<String>? = null,
    val areFromRemote: Boolean = false,
    val source: String? = null
)

data class SyncedLine(
    val time: Int, // milliseconds
    val line: String,
    val words: List<SyncedWord>? = null,
    val translation: String? = null,
    val romanization: String? = null
)

data class SyncedWord(
    val time: Long, // milliseconds
    val word: String,
    val startsNewWord: Boolean = true
)

// LrcLib API response models
data class LrcLibSearchResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    @SerializedName("plainLyrics") val plainLyrics: String? = null,
    @SerializedName("syncedLyrics") val syncedLyrics: String? = null
)

data class LrcLibSearchResult(
    val id: Long,
    val name: String,
    @SerializedName("artistName") val artistName: String? = null,
    @SerializedName("albumName") val albumName: String? = null
)

sealed class LyricsSearchResult {
    data class Found(val record: LrcLibSearchResult) : LyricsSearchResult()
    data class NotFound(val query: String) : LyricsSearchResult()
    data class Error(val query: String, val message: String) : LyricsSearchResult()
}

// UI state for search dialog
sealed interface LyricsSearchUiState {
    data object Idle : LyricsSearchUiState
    data object Loading : LyricsSearchUiState
    data class PickResult(val results: List<LyricsSearchResult.Found>) : LyricsSearchUiState
    data class NotFound(val message: String) : LyricsSearchUiState
    data class Error(val message: String) : LyricsSearchUiState
    data object Success : LyricsSearchUiState
}
