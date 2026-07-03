package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

import androidx.compose.foundation.background

data class ActionSheetItem(val label: String, val subtitle: String? = null, val icon: ImageVector? = null, val destructive: Boolean = false, val chevron: Boolean = false, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(title: String, onDismiss: () -> Unit, items: List<ActionSheetItem>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), scheme.surfaceContainerHigh, tonalElevation = 0.dp) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(50)).background(scheme.onSurfaceVariant.copy(alpha = 0.4f))) }
            Text(text = title, style = MaterialTheme.typography.titleLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            items.forEach { item ->
                Row(Modifier.fillMaxWidth().clickable { item.onClick(); onDismiss() }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (item.icon != null) Box(Modifier.size(40.dp).clip(CircleShape).background(if (item.destructive) scheme.errorContainer else scheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(imageVector = item.icon, contentDescription = item.label, tint = if (item.destructive) scheme.error else scheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f)) { Text(text = item.label, style = MaterialTheme.typography.bodyLarge, color = if (item.destructive) scheme.error else scheme.onSurface); item.subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) } }
                    if (item.chevron) Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
