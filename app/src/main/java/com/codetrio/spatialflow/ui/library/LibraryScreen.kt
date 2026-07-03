package com.codetrio.spatialflow.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.codetrio.spatialflow.R
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel

enum class LibTab(val label: String, val subtitle: String, val icon: Int) {
    LIBRARY("Library","Your music collection",R.drawable.ic_library_music),
    PLAYLISTS("Playlists","Your mixes",R.drawable.ic_queue_music),
    SONGS("Songs","All tracks",R.drawable.ic_music_note),
    ARTISTS("Artists","By artist",R.drawable.ic_music_note),
    ALBUMS("Albums","By album",R.drawable.ic_library_music)}

@Composable
fun LibraryScreen(viewModel: PlayerSharedViewModel, onEditSong: (SongItem) -> Unit, onNavigateToExplore: () -> Unit) {
    val scheme = MaterialTheme.colorScheme; val density = LocalDensity.current
    val songList by viewModel.songList.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val tabs = LibTab.entries.toList(); var sel by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = sel, pageCount = { tabs.size })
    LaunchedEffect(sel) { pagerState.animateScrollToPage(sel) }
    LaunchedEffect(pagerState.currentPage) { sel = pagerState.currentPage }

    val maxH = 90.dp; val maxHPx = with(density) { maxH.toPx() }; var off by remember { mutableFloatStateOf(0f) }
    val nsc = remember { object : NestedScrollConnection {
        override fun onPreScroll(a: androidx.compose.ui.geometry.Offset, s: NestedScrollSource): androidx.compose.ui.geometry.Offset {
            if(a.y<0){val n=off+a.y;val o=off;off=n.coerceIn(-maxHPx,0f);return androidx.compose.ui.geometry.Offset(0f,off-o)};return androidx.compose.ui.geometry.Offset.Zero}
        override fun onPostScroll(c: androidx.compose.ui.geometry.Offset, a: androidx.compose.ui.geometry.Offset, s: NestedScrollSource): androidx.compose.ui.geometry.Offset {
            if(a.y>0){val n=off+a.y;val o=off;off=n.coerceIn(-maxHPx,0f);return androidx.compose.ui.geometry.Offset(0f,off-o)};return androidx.compose.ui.geometry.Offset.Zero}}}
    val hh = maxH + with(density) { off.toDp() }; val prog = (1f + (off / maxHPx)).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize().background(scheme.background)) {
        Box(Modifier.fillMaxWidth().height(430.dp).align(Alignment.TopCenter).drawWithCache{onDrawBehind{drawRect(Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha=0.45f),Color.Transparent)))}})
        Column(Modifier.fillMaxSize().nestedScroll(nsc)) {
            Column(Modifier.fillMaxWidth().height(hh).graphicsLayer{alpha=prog}) {
                Spacer(Modifier.height(8.dp))
                Text(tabs[sel].label, style = MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold, color=scheme.onBackground, modifier=Modifier.padding(horizontal=24.dp))
                Text(tabs[sel].subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))}
            LazyRow(Modifier.fillMaxWidth().padding(vertical=4.dp), contentPadding=PaddingValues(horizontal=24.dp), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
                items(tabs.size){ i->
                    val isSel=i==sel;val int=remember{MutableInteractionSource()};val press by int.collectIsPressedAsState()
                    val sc by animateFloatAsState(if(press)0.92f else if(isSel)1.05f else 1f,spring(Spring.DampingRatioMediumBouncy,Spring.StiffnessLow),label="ts")
                    val bg by animateColorAsState(if(isSel)scheme.primary else scheme.surfaceVariant.copy(alpha=0.5f),spring(stiffness=Spring.StiffnessMedium),label="tbg")
                    val ct by animateColorAsState(if(isSel)scheme.onPrimary else scheme.onSurfaceVariant,spring(stiffness=Spring.StiffnessMedium),label="tct")
                    Row(Modifier.graphicsLayer{scaleX=sc;scaleY=sc}.clip(CircleShape).background(bg).clickable(int,null){sel=i}.padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
                        Icon(painterResource(tabs[i].icon), tabs[i].label, modifier = Modifier.size(20.dp), tint = ct);Spacer(Modifier.width(8.dp))
                        Text(tabs[i].label, style = MaterialTheme.typography.labelLarge.copy(fontWeight=FontWeight.SemiBold,fontSize=15.sp), color = ct)}}}
            HorizontalPager(state=pagerState,modifier=Modifier.fillMaxSize()){page->when(tabs[page]){
                LibTab.LIBRARY->LibMixTab(songList,currentSong,isPlaying,viewModel)
                LibTab.PLAYLISTS->LibPlaceholder("Playlists",Icons.Rounded.QueueMusic)
                LibTab.SONGS->LibSongsTab(songList,currentSong,isPlaying,viewModel)
                LibTab.ARTISTS->LibPlaceholder("Artists",Icons.Rounded.Person)
                LibTab.ALBUMS->LibPlaceholder("Albums",Icons.Rounded.Album)}}}}
}

@Composable private fun LibPlaceholder(t: String, ic: androidx.compose.ui.graphics.vector.ImageVector) { Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Icon(ic,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.3f));Spacer(Modifier.height(16.dp));Text("$t will appear here",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun LibMixTab(s: List<SongItem>, cs: SongItem?, ip: Boolean, vm: PlayerSharedViewModel) { LazyColumn(contentPadding=PaddingValues(bottom=80.dp)) { item { Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FilledTonalButton({ vm.toggleShuffle(); vm.playNextSong() }, Modifier.weight(1f), shape = CircleShape) { Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Shuffle") }; FilledTonalButton({}, Modifier.weight(1f), shape = CircleShape) { Icon(Icons.Rounded.Favorite, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Favorites") } } }; if (s.isNotEmpty()) { item { Text("Your Songs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) }; items(s.take(20), key = { it.videoId ?: it.hashCode().toString() }) { song -> LibSongRow(song, song.videoId == cs?.videoId, ip) { vm.playSongAtIndex(s.indexOf(song)) } } }; item { Spacer(Modifier.height(16.dp)) } } }

@Composable private fun LibSongsTab(s: List<SongItem>, cs: SongItem?, ip: Boolean, vm: PlayerSharedViewModel) { LazyColumn(contentPadding=PaddingValues(bottom=80.dp)) { item { Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){AssistChip({},{Text("All")},colors=AssistChipDefaults.assistChipColors(containerColor=MaterialTheme.colorScheme.primary,labelColor=MaterialTheme.colorScheme.onPrimary));AssistChip({},{Text("Liked")});AssistChip({},{Text("Downloaded")})}}; if(s.isNotEmpty()) itemsIndexed(s,key={_,x->x.videoId?:x.hashCode().toString()}){i,song->LibSongRow(song,song.videoId==cs?.videoId,ip){vm.playSongAtIndex(i)}} else item{Box(Modifier.fillMaxWidth().height(200.dp),contentAlignment=Alignment.Center){Text("No songs found",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}} } }

@Composable private fun LibSongRow(song: SongItem, isCur: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme; val tr = updateTransition(isCur, label="hl")
    val sc by tr.animateColor({ tween(400) },label="sc"){if(it)scheme.primaryContainer.copy(alpha=0.3f) else Color.Transparent}
    val tc by tr.animateColor({ tween(400) },label="tc"){if(it)scheme.primary else scheme.onSurface}
    val cr by tr.animateDp({ tween(400) },label="cr"){if(it)24.dp else 16.dp}
    val ac by tr.animateDp({ tween(400) },label="ac"){if(it)22.dp else 12.dp}
    Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp).clip(RoundedCornerShape(cr)).clickable(onClick=onClick),RoundedCornerShape(cr),sc,tonalElevation=0.dp){
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(ac)).background(scheme.surfaceVariant),contentAlignment=Alignment.Center){AsyncImage(song.getAlbumArtUri(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}
            Column(Modifier.weight(1f)){Text(song.title,style=MaterialTheme.typography.titleMedium,color=tc,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(isCur)FontWeight.Bold else FontWeight.Normal);Text(song.artist,style=MaterialTheme.typography.bodyMedium,color=scheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)}
            if(isCur&&isPlaying)Box(Modifier.size(5.dp).clip(CircleShape).background(scheme.primary))}}}
