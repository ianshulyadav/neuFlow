package com.codetrio.spatialflow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.player.QueueSongCard
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QueueBottomSheet : BottomSheetDialogFragment() {
    private val vm: PlayerSharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val songList by vm.songList.collectAsStateWithLifecycle()
                val currentSongIndex by vm.currentSongIndex.collectAsStateWithLifecycle()
                QueueBottomSheetContent(
                    songList = songList,
                    currentSongIndex = currentSongIndex,
                    onDismiss = { dismiss() },
                    onPlaySongAtIndex = { vm.playSongAtIndex(it) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let {
            it.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            it.behavior.skipCollapsed = true
        }
    }
}

@Composable
fun QueueBottomSheetContent(songList: List<SongItem>, currentSongIndex: Int, onDismiss: () -> Unit, onPlaySongAtIndex: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(scheme.surfaceContainerHigh)) {
        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(36.dp).height(5.dp).clip(RoundedCornerShape(50)).background(scheme.onSurfaceVariant.copy(alpha = 0.4f)))
        }
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = GoogleSansRounded,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            itemsIndexed(songList, key = { i, s -> "${s.videoId}_$i" }) { index, song ->
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                    QueueSongCard(
                        song = song,
                        isCurrent = index == currentSongIndex,
                        isPlaying = index == currentSongIndex,
                        onTap = { onPlaySongAtIndex(index) },
                        onRemove = { }
                    )
                }
            }
        }
    }
}
