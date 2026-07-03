package com.codetrio.spatialflow.ui.theme

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Observes a SharedPreferences key as Compose State.
 * Re-composes whenever the value changes.
 */
@Composable
fun SharedPreferences.observeKey(key: String, default: String): State<String> {
    val state = remember { mutableStateOf(getString(key, default) ?: default) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getString(key, default) ?: default
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        onDispose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
fun SharedPreferences.observeKey(key: String, default: Boolean): State<Boolean> {
    val state = remember { mutableStateOf(getBoolean(key, default)) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getBoolean(key, default)
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        onDispose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
fun SharedPreferences.observeKey(key: String, default: Int): State<Int> {
    val state = remember { mutableStateOf(getInt(key, default)) }
    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                state.value = prefs.getInt(key, default)
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        onDispose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
