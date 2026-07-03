package com.codetrio.spatialflow.ui.settings

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAppearance: () -> Unit, onAudio: () -> Unit, onLibrary: () -> Unit, onAbout: () -> Unit) {
    val s = MaterialTheme.colorScheme
    Scaffold(containerColor = s.background, topBar = { TopAppBar(title = { Text("Settings", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { SettingsSecHdr("Appearance") }
            item { SettingsCard {
                SettingsRow(Icons.Rounded.ColorLens, "Theme Mode", "Change the look of the app", s.primaryContainer, s.primary, onAppearance)
                SettingsDiv()
                SettingsRow(Icons.Rounded.AudioFile, "Audio Settings", "Modify play quality & settings", s.secondaryContainer, s.secondary, onAudio)
                SettingsDiv()
                SettingsRow(Icons.Rounded.LibraryMusic, "Library Settings", "Manage scan paths & database", s.tertiaryContainer, s.tertiary, onLibrary)
            }}
            item { SettingsSecHdr("About") }
            item { SettingsCard {
                SettingsRow(Icons.Rounded.Info, "About the app", "App details & developer credits", s.surfaceVariant, s.onSurfaceVariant, onAbout)
                SettingsDiv()
                SettingsRow(Icons.Rounded.FavoriteBorder, "Rate the app", "Share feedback", s.errorContainer, s.error, {})
            }}
            item { SettingsSecHdr("Data") }
            item { SettingsCard { SettingsRow(Icons.Rounded.DeleteForever, "Clear all data", "Reset everything", s.errorContainer, s.error, destructive = true, onClick = {}) }}
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
@Composable fun SettingsSecHdr(t: String) { Text(text = t.uppercase(), style = MaterialTheme.typography.labelMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)) }
@Composable fun SettingsCard(c: @Composable ColumnScope.() -> Unit) {
    val sh = AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness)
    Card(Modifier.fillMaxWidth().padding(horizontal=16.dp), sh, CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp)) { Column(Modifier.padding(vertical=4.dp)) { c() } }
}
@Composable fun SettingsRow(ic: ImageVector, l: String, sub: String, bg: Color, tint: Color, onClick: () -> Unit, destructive: Boolean = false) {
    val s = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(imageVector = ic, contentDescription = l, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = l, style = MaterialTheme.typography.bodyLarge, color = if(destructive) s.error else s.onSurface, fontFamily = GoogleSansRounded)
            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = s.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = s.onSurfaceVariant.copy(alpha=0.5f))
    }
}
@Composable fun SettingsTgl(ic: ImageVector, l: String, sub: String, bg: Color, tint: Color, v: Boolean, on: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(imageVector = ic, contentDescription = l, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = l, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontFamily = GoogleSansRounded)
            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(v, on)
    }
}
@Composable fun SettingsDiv() { HorizontalDivider(Modifier.padding(start=68.dp,end=16.dp), thickness=0.5.dp, color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f)) }
