package com.codetrio.spatialflow.ui.explore

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetrio.spatialflow.MainActivity
import com.codetrio.spatialflow.model.SongItem
import com.codetrio.spatialflow.ui.components.SectionHeader
import com.codetrio.spatialflow.ui.theme.GoogleSansRounded
import com.codetrio.spatialflow.viewmodel.ExploreViewModel
import com.codetrio.spatialflow.viewmodel.PlayerSharedViewModel
import com.codetrio.spatialflow.model.toSongItem
import com.codetrio.spatialflow.viewmodel.DetailType
import com.codetrio.spatialflow.data.innertube.SearchFilter
import com.codetrio.spatialflow.data.innertube.SearchItem
import com.codetrio.spatialflow.data.innertube.OnlineSong
import com.codetrio.spatialflow.data.innertube.OnlineAlbum
import com.codetrio.spatialflow.data.innertube.OnlineArtist
import com.codetrio.spatialflow.data.innertube.OnlinePlaylist
import com.codetrio.spatialflow.data.innertube.AccountManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    playerSharedViewModel: PlayerSharedViewModel,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.playbackTriggerEvent.collect {
            val currentOnlineSong = viewModel.uiState.value.currentOnlineSong
            val onlineQueue = viewModel.uiState.value.onlineQueue
            val currentIndex = viewModel.uiState.value.currentOnlineIndex
            if (currentOnlineSong != null && onlineQueue.isNotEmpty()) {
                val songItems = onlineQueue.map { it.toSongItem() }
                playerSharedViewModel.setSongList(songItems)
                playerSharedViewModel.playSongAtIndex(currentIndex)
            }
        }
    }

    // Back handler to pop detail stack or close search
    androidx.activity.compose.BackHandler(enabled = uiState.detailStack.isNotEmpty() || uiState.searchQuery.isNotEmpty() || uiState.isSearching) {
        if (uiState.detailStack.isNotEmpty()) {
            viewModel.popDetailStack()
        } else if (uiState.searchQuery.isNotEmpty() || uiState.isSearching) {
            viewModel.clearSearch()
            viewModel.resetToHome()
        }
    }

    var showPlaylistAddDialogForSong by remember { mutableStateOf<SongItem?>(null) }
    var showCreditsDialogForSong by remember { mutableStateOf<OnlineSong?>(null) }
    var activeSongMenu by remember { mutableStateOf<OnlineSong?>(null) }

    // Handle song credit dialog
    showCreditsDialogForSong?.let { song ->
        SongCreditsDialog(song = song, onDismiss = { showCreditsDialogForSong = null })
    }

    // Handle song option bottom sheet
    activeSongMenu?.let { song ->
        val isPinned = uiState.pinnedSpeedDialIds.contains(song.videoId)
        OnlineSongBottomSheet(
            song = song,
            isPinned = isPinned,
            onDismissRequest = { activeSongMenu = null },
            playerSharedViewModel = playerSharedViewModel,
            exploreViewModel = viewModel,
            onPlaylistAddClick = { songItem ->
                showPlaylistAddDialogForSong = songItem
            },
            onViewCreditsClick = { onlineSong ->
                showCreditsDialogForSong = onlineSong
            }
        )
    }

    // Playlist Add Dialog
    showPlaylistAddDialogForSong?.let { songItem ->
        val localPlaylists by playerSharedViewModel.localPlaylistsFlow.collectAsStateWithLifecycle()
        var showCreateDialog by remember { mutableStateOf(false) }

        if (showCreateDialog) {
            var newTitle by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Playlist", fontFamily = GoogleSansRounded) },
                text = {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                playerSharedViewModel.createLocalPlaylist(newTitle)
                                showCreateDialog = false
                            }
                        }
                    ) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showPlaylistAddDialogForSong = null },
                title = { Text("Add to Playlist", fontFamily = GoogleSansRounded) },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        item {
                            ListItem(
                                headlineContent = { Text("Create new playlist...", fontWeight = FontWeight.Bold) },
                                leadingContent = { Icon(Icons.Rounded.Add, null) },
                                modifier = Modifier.clickable { showCreateDialog = true }
                            )
                        }
                        items(localPlaylists) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.title) },
                                modifier = Modifier.clickable {
                                    playerSharedViewModel.addSongToLocalPlaylist(playlist.id, songItem)
                                    showPlaylistAddDialogForSong = null
                                }
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPlaylistAddDialogForSong = null }) { Text("Cancel") }
                }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().background(scheme.background)) {
        val currentDetail = uiState.detailStack.lastOrNull()
        
        Crossfade(targetState = currentDetail, label = "ExploreNavigation") { detailType ->
            when (detailType) {
                DetailType.ALBUM -> {
                    uiState.albumDetail?.let { albumPage ->
                        AlbumDetailView(
                            albumPage = albumPage,
                            currentOnlineSong = uiState.currentOnlineSong,
                            isLoadingStream = uiState.isLoadingStream,
                            onBack = { viewModel.popDetailStack() },
                            onSongClick = { song, queue, index ->
                                viewModel.playOnlineSongWithQueue(song, queue, index)
                            },
                            onSongMenuClick = { song -> activeSongMenu = song },
                            onStartRadioClick = { videoId -> viewModel.startRadio(videoId) }
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                DetailType.PLAYLIST -> {
                    uiState.playlistDetail?.let { playlistPage ->
                        PlaylistDetailView(
                            playlistPage = playlistPage,
                            currentOnlineSong = uiState.currentOnlineSong,
                            isLoadingStream = uiState.isLoadingStream,
                            onBack = { viewModel.popDetailStack() },
                            onSongClick = { song, queue, index ->
                                viewModel.playOnlineSongWithQueue(song, queue, index)
                            },
                            onSongMenuClick = { song -> activeSongMenu = song },
                            onStartRadioClick = { videoId -> viewModel.startRadio(videoId) }
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                DetailType.ARTIST -> {
                    uiState.artistDetail?.let { artistPage ->
                        ArtistDetailView(
                            artistPage = artistPage,
                            currentOnlineSong = uiState.currentOnlineSong,
                            isSubscribed = artistPage.artist.isSubscribed,
                            onBack = { viewModel.popDetailStack() },
                            onSongClick = { song, queue, index ->
                                viewModel.playOnlineSongWithQueue(song, queue, index)
                            },
                            onAlbumClick = { album -> viewModel.loadAlbum(album.browseId) },
                            onPlaylistClick = { playlist -> viewModel.loadPlaylist(playlist.playlistId) },
                            onArtistClick = { artist -> viewModel.loadArtist(artist.browseId) },
                            onSongMenuClick = { song -> activeSongMenu = song },
                            onSubscribeClick = { channelId -> viewModel.subscribeToArtist(channelId) },
                            onStartRadioClick = { videoId -> viewModel.startRadio(videoId) },
                            onSectionClick = { browseId, params, title ->
                                viewModel.loadSectionDetails(browseId, params, title)
                            }
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                DetailType.SECTION -> {
                    uiState.sectionDetail?.let { section ->
                        SectionDetailView(
                            section = section,
                            currentOnlineSong = uiState.currentOnlineSong,
                            isLoadingStream = uiState.isLoadingStream,
                            onBack = { viewModel.popDetailStack() },
                            onSongClick = { song, queue, index ->
                                viewModel.playOnlineSongWithQueue(song, queue, index)
                            },
                            onAlbumClick = { album -> viewModel.loadAlbum(album.browseId) },
                            onPlaylistClick = { playlist -> viewModel.loadPlaylist(playlist.playlistId) },
                            onArtistClick = { artist -> viewModel.loadArtist(artist.browseId) },
                            onSongMenuClick = { song -> activeSongMenu = song },
                            onStartRadioClick = { videoId -> viewModel.startRadio(videoId) }
                        )
                    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                null -> {
                    if (uiState.isSearching || uiState.searchQuery.isNotEmpty()) {
                        SearchLayout(
                            query = uiState.searchQuery,
                            suggestions = uiState.suggestions,
                            searchResults = uiState.searchResults,
                            isSearching = uiState.isSearching,
                            currentOnlineSong = uiState.currentOnlineSong,
                            isLoadingStream = uiState.isLoadingStream,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearchSubmit = { viewModel.search(it) },
                            onBackClick = {
                                viewModel.clearSearch()
                                viewModel.resetToHome()
                            },
                            onSongClick = { song ->
                                viewModel.playOnlineSongWithQueue(song, listOf(song), 0)
                            },
                            onAlbumClick = { album -> viewModel.loadAlbum(album.browseId) },
                            onPlaylistClick = { playlist -> viewModel.loadPlaylist(playlist.playlistId) },
                            onArtistClick = { artist -> viewModel.loadArtist(artist.browseId) },
                            onSongMenuClick = { song -> activeSongMenu = song }
                        )
                    } else {
                        Scaffold(
                            containerColor = scheme.background,
                            topBar = {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.35f), Color.Transparent)))
                                ) {
                                    Spacer(Modifier.statusBarsPadding())
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Explore", style = MaterialTheme.typography.headlineLarge, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                viewModel.setSearchQuery("")
                                            }
                                        ) {
                                            Icon(Icons.Rounded.Search, "Search", tint = scheme.onSurface, modifier = Modifier.size(26.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val mainAct = context as? MainActivity
                                                if (mainAct != null) {
                                                    if (AccountManager.isLoggedIn(context)) {
                                                        mainAct.navigateToSettings()
                                                    } else {
                                                        mainAct.navigateToGoogleSignIn()
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Rounded.AccountCircle, "Account", tint = scheme.onSurface, modifier = Modifier.size(26.dp))
                                        }
                                    }
                                }
                            }
                        ) { paddingValues ->
                            if (uiState.isLoadingHome && uiState.homeSections.isEmpty()) {
                                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                                    HomeFeedSkeleton()
                                }
                            } else {
                                PullToRefreshBox(
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = { viewModel.refreshHomeFeed() },
                                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                                ) {
                                    LazyColumn(
                                        contentPadding = PaddingValues(bottom = 120.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                            val userName = remember { AccountManager.getAccountName(context) }
                                            WelcomeGreetingBanner(userName = userName)
                                        }
                                        
                                        items(uiState.homeSections, key = { it.title }) { section ->
                                            HomeSectionRow(
                                                section = section,
                                                currentOnlineSong = uiState.currentOnlineSong,
                                                onSongClick = { song ->
                                                    viewModel.playOnlineSongWithQueue(song, section.items.filterIsInstance<SearchItem.Song>().map { it.song }, section.items.filterIsInstance<SearchItem.Song>().indexOfFirst { it.song.videoId == song.videoId }.coerceAtLeast(0))
                                                },
                                                onAlbumClick = { album -> viewModel.loadAlbum(album.browseId) },
                                                onArtistClick = { artist -> viewModel.loadArtist(artist.browseId) },
                                                onPlaylistClick = { playlist -> viewModel.loadPlaylist(playlist.playlistId) },
                                                onSongMenuClick = { song -> activeSongMenu = song },
                                                onSectionClick = { browseId, params, title ->
                                                    viewModel.loadSectionDetails(browseId, params, title)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongCreditsDialog(song: OnlineSong, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Song Info & Credits", fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Title: ${song.title}", style = MaterialTheme.typography.bodyMedium)
                Text("Artist: ${song.artist}", style = MaterialTheme.typography.bodyMedium)
                song.albumName?.let { Text("Album: $it", style = MaterialTheme.typography.bodyMedium) }
                Text("Duration: ${formatDuration(song.durationMs ?: 0L)}", style = MaterialTheme.typography.bodyMedium)
                Text("Source: YouTube Music", style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun SearchLayout(
    query: String,
    suggestions: List<String>,
    searchResults: List<SearchItem>,
    isSearching: Boolean,
    currentOnlineSong: OnlineSong?,
    isLoadingStream: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onBackClick: () -> Unit,
    onSongClick: (OnlineSong) -> Unit,
    onAlbumClick: (OnlineAlbum) -> Unit,
    onPlaylistClick: (OnlinePlaylist) -> Unit,
    onArtistClick: (OnlineArtist) -> Unit,
    onSongMenuClick: (OnlineSong) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = scheme.background,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerHigh)
            ) {
                Spacer(Modifier.statusBarsPadding())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, "Back")
                    }
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search songs, albums, artists...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Rounded.Close, "Clear")
                                }
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearchSubmit(query) }
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults) { item ->
                        SearchResultItem(
                            item = item,
                            isCurrentlyPlaying = when (item) {
                                is SearchItem.Song -> item.song.videoId == currentOnlineSong?.videoId
                                else -> false
                            },
                            isLoading = when (item) {
                                is SearchItem.Song -> isLoadingStream && item.song.videoId == currentOnlineSong?.videoId
                                else -> false
                            },
                            onSongMenuClick = onSongMenuClick,
                            onClick = {
                                when (item) {
                                    is SearchItem.Song -> onSongClick(item.song)
                                    is SearchItem.Album -> onAlbumClick(item.album)
                                    is SearchItem.Playlist -> onPlaylistClick(item.playlist)
                                    is SearchItem.Artist -> onArtistClick(item.artist)
                                }
                            }
                        )
                    }
                }
            } else if (suggestions.isNotEmpty() && query.isNotEmpty()) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = { Icon(Icons.Rounded.Search, null) },
                            modifier = Modifier.clickable {
                                onQueryChange(suggestion)
                                onSearchSubmit(suggestion)
                            }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Type to start searching", color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class ExploreItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: Any?
)
