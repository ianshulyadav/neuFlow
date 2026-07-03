package com.codetrio.spatialflow.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AutoScrollingText(text: String, style: TextStyle = MaterialTheme.typography.bodyMedium, gradientEdgeColor: Color = MaterialTheme.colorScheme.primaryContainer, edgeWidth: Dp = 16.dp, canScroll: Boolean = true, modifier: Modifier = Modifier) {
    val m = if (canScroll) modifier.graphicsLayer{compositingStrategy=CompositingStrategy.Offscreen}.drawWithCache{
        val ep=edgeWidth.toPx()
        val lb=Brush.horizontalGradient(listOf(gradientEdgeColor,gradientEdgeColor.copy(alpha=0f)),0f,ep)
        val rb=Brush.horizontalGradient(listOf(gradientEdgeColor.copy(alpha=0f),gradientEdgeColor),size.width-ep,size.width)
        onDrawWithContent{drawContent();drawRect(lb,blendMode=BlendMode.DstIn);drawRect(rb,blendMode=BlendMode.DstIn)}
    }.basicMarquee().padding(horizontal=edgeWidth) else modifier.padding(horizontal=edgeWidth)
    Text(text=text,style=style,maxLines=1,softWrap=false,modifier=m)
}
