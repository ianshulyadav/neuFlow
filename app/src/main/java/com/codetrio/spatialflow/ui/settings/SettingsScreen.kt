package com.codetrio.spatialflow.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.ui.theme.CardCorners
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsCard {
                    SettingsRow(Icons.Rounded.Palette, "Theme", "Dark / Light / System", scheme.primaryContainer, scheme.onPrimaryContainer, onNavigateToAppearance)
                    SettingsDivider()
                    SettingsRow(Icons.Rounded.TextFields, "Font", "System default", scheme.secondaryContainer, scheme.onSecondaryContainer, {})
                    SettingsDivider()
                    SettingsToggleRow(Icons.Rounded.DarkMode, "AMOLED Black", "Pure black for OLED", scheme.tertiaryContainer, scheme.onTertiaryContainer, false, {})
                }
            }
            item { SettingsSectionHeader("Audio") }
            item {
                SettingsCard {
                    SettingsRow(Icons.Rounded.Equalizer, "Equalizer", "Customize sound", scheme.primaryContainer, scheme.onPrimaryContainer, onNavigateToAudio)
                    SettingsDivider()
                    SettingsRow(Icons.Rounded.VolumeUp, "Volume Normalization", "Consistent volume", scheme.secondaryContainer, scheme.onSecondaryContainer, {})
                    SettingsDivider()
                    SettingsRow(Icons.Rounded.SurroundSound, "Spatial Audio", "Immersive sound", scheme.tertiaryContainer, scheme.onTertiaryContainer, {})
                }
            }
            item { SettingsSectionHeader("Library") }
            item {
                SettingsCard {
                    SettingsRow(Icons.Rounded.Storage, "Storage", "Manage library", scheme.primaryContainer, scheme.onPrimaryContainer, onNavigateToLibrary)
                    SettingsDivider()
                    SettingsRow(Icons.Rounded.Sync, "Auto-sync", "Stay updated", scheme.secondaryContainer, scheme.onSecondaryContainer, {})
                    SettingsDivider()
                    SettingsToggleRow(Icons.Rounded.Download, "Wi-Fi only", "Save mobile data", scheme.tertiaryContainer, scheme.onTertiaryContainer, true, {})
                }
            }
            item { SettingsSectionHeader("About") }
            item {
                SettingsCard {
                    SettingsRow(Icons.Rounded.Info, "About SpatialFlow", "Version 1.0.0", scheme.primaryContainer, scheme.onPrimaryContainer, onNavigateToAbout)
                    SettingsDivider()
                    SettingsRow(Icons.Rounded.FavoriteBorder, "Rate the app", "Share feedback", scheme.errorContainer, scheme.error, {})
                }
            }
            item { SettingsSectionHeader("Data") }
            item {
                SettingsCard {
                    SettingsRow(Icons.Rounded.DeleteForever, "Clear all data", "Reset everything", scheme.errorContainer, scheme.error, destructive = true, onClick = {})
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness)
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape, CardDefaults.cardColors(scheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
fun SettingsRow(icon: ImageVector, label: String, subtitle: String, iconBg: Color, iconTint: Color, onClick: () -> Unit, destructive: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = iconTint) }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) scheme.error else scheme.onSurface, fontFamily = GoogleSansRounded)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun SettingsToggleRow(icon: ImageVector, label: String, subtitle: String, iconBg: Color, iconTint: Color, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = iconTint) }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontFamily = GoogleSansRounded)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onToggle)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 68.dp, end = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}
