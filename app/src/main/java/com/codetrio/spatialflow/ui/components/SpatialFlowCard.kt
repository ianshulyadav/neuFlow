package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.ui.theme.CardCorners
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@Composable
fun SpatialFlowCard(onClick: (() -> Unit)? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = remember { AbsoluteSmoothCornerShape(CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness, CardCorners.RadiusDp.dp, CardCorners.Smoothness) }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = shape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp), content = content)
    } else {
        Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = shape, colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerHigh), elevation = CardDefaults.cardElevation(0.dp), content = content)
    }
}
