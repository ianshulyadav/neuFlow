package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientHeader(title: String, startColor: Color = MaterialTheme.colorScheme.primaryContainer, endColor: Color = MaterialTheme.colorScheme.surface, onBackClick: () -> Unit, expandedHeight: androidx.compose.ui.unit.Dp = 160.dp, scrollBehavior: TopAppBarScrollBehavior? = null, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val grad = remember(startColor, endColor) { Brush.verticalGradient(listOf(startColor, endColor)) }
    LargeTopAppBar(
        title = { Text(title, color = scheme.onPrimaryContainer, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp)) },
        expandedHeight = expandedHeight, modifier = modifier.background(brush = grad), scrollBehavior = scrollBehavior,
        navigationIcon = { IconButton(onClick = onBackClick, colors = IconButtonDefaults.iconButtonColors(containerColor = scheme.onPrimaryContainer.copy(alpha = 0.15f))) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = scheme.onPrimaryContainer) } },
        colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent, titleContentColor = scheme.onPrimaryContainer, navigationIconContentColor = scheme.onPrimaryContainer)
    )
}
