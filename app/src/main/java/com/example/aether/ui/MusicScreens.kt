package com.example.aether.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.aether.R
import com.example.aether.model.Song
import com.example.aether.ui.playlist.PlaylistEditorScreen
import com.example.aether.ui.playlist.PlaylistListPanel
import com.example.aether.ui.presence.PresenceScreen
import com.example.aether.ui.session.SessionMapScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val songs by viewModel.filteredSongs.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentProfile by viewModel.currentProfile.collectAsState()
    val notice by viewModel.userNotice.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeScreen by remember { mutableStateOf("presence") } // "presence", "library", "map", "playlist_edit"
    var showNowPlaying by remember { mutableStateOf(false) }
    var editingPlaylistId by remember { mutableStateOf<Long?>(null) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.importFolder(it) }
    }
    val filesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.importFiles(uris)
    }

    LaunchedEffect(activeScreen) {
        viewModel.setHighRefreshRate(activeScreen == "presence")
    }

    LaunchedEffect(notice) {
        val message = notice?.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeNotice()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Crossfade(targetState = activeScreen, label = "screen_switch") { screen ->
        when (screen) {
            "library" -> {
                LibraryScreen(
                    viewModel = viewModel,
                    songs = songs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    searchQuery = searchQuery,
                    onBack = { activeScreen = "presence" },
                    onShowNowPlaying = { showNowPlaying = true },
                    onEditPlaylist = { id ->
                        editingPlaylistId = id
                        activeScreen = "playlist_edit"
                    },
                    onImportFolder = { folderLauncher.launch(null) },
                    onImportFiles = { filesLauncher.launch(arrayOf("audio/*")) }
                )
            }
            "playlist_edit" -> {
                PlaylistEditorScreen(
                    viewModel = viewModel,
                    playlistId = editingPlaylistId,
                    librarySongs = allSongs,
                    onBack = { activeScreen = "library" }
                )
            }
            "map" -> {
                SessionMapScreen(
                    viewModel = viewModel,
                    onBack = { activeScreen = "presence" }
                )
            }
            else -> {
                PresenceScreen(
                    viewModel = viewModel,
                    onOpenLibrary = { activeScreen = "library" },
                    onOpenSessionMap = { activeScreen = "map" },
                    onImportFolder = { folderLauncher.launch(null) },
                    onImportFiles = { filesLauncher.launch(arrayOf("audio/*")) }
                )
            }
        }
    }

    if (showNowPlaying && currentSong != null) {
        val repeatMode by viewModel.repeatMode.collectAsState()
        val shuffleMode by viewModel.shuffleModeEnabled.collectAsState()

        ModalBottomSheet(
            onDismissRequest = { showNowPlaying = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
        ) {
            NowPlayingScreen(
                song = currentSong!!,
                isPlaying = isPlaying,
                progress = progress,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleMode,
                bpm = currentProfile?.bpm,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.skipPrevious() },
                onNext = { viewModel.skipNext() },
                onSeek = { viewModel.seekTo(it) },
                onToggleRepeat = { viewModel.toggleRepeatMode() },
                onToggleShuffle = { viewModel.toggleShuffleMode() }
            )
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
    )
    }
}

@Composable
fun PermissionScreen(onRequestAccess: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permitir acceso a audio",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AETHER necesita leer tu música local para reproducirla.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestAccess) {
                Text("Permitir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Long,
    searchQuery: String,
    onBack: () -> Unit,
    onShowNowPlaying: () -> Unit,
    onEditPlaylist: (Long?) -> Unit,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            AETHERTopBar(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.onSearchQueryChange(it) },
                onBack = onBack,
                onImportFolder = onImportFolder,
                onImportFiles = onImportFiles
            )
        },
        bottomBar = {
            if (currentSong != null) {
                MiniPlayer(
                    song = currentSong,
                    isPlaying = isPlaying,
                    progress = progress,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onClick = onShowNowPlaying
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { onEditPlaylist(null) }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Nueva playlist")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Música") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Playlists") }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    if (songs.isEmpty()) {
                        EmptyState(
                            isSearch = searchQuery.isNotEmpty(),
                            onScan = { viewModel.loadSongs() },
                            onImportFolder = onImportFolder,
                            onImportFiles = onImportFiles
                        )
                    } else {
                        SongList(
                            songs = songs,
                            currentSongId = currentSong?.id,
                            onSongClick = { viewModel.playSong(it) }
                        )
                    }
                } else {
                    PlaylistListPanel(
                        playlists = playlists,
                        onOpen = { onEditPlaylist(it) },
                        onPlay = { viewModel.playPlaylist(it) },
                        onDelete = { pendingDeleteId = it }
                    )
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Eliminar playlist") },
            text = { Text("Se borrará la lista. Las canciones siguen en tu biblioteca.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlaylist(id)
                    pendingDeleteId = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AETHERTopBar(
    searchQuery: String, 
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AETHER",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onImportFolder) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Importar carpeta")
            }
            IconButton(onClick = onImportFiles) {
                Icon(Icons.Rounded.AudioFile, contentDescription = "Elegir archivos")
            }
        }
        
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = { Text("Buscar música...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                isSelected = song.id == currentSongId,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
fun SongItem(song: Song, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else Color.Transparent, label = "bg"
    )
    val textColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface, label = "text"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = null
            )
            // Placeholder si no hay imagen
            if (song.albumArtUri == null) {
                Text(
                    text = song.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) textColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Long,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column {
            // Fina barra de progreso arriba
            val progressFactor = if (song.duration > 0) progress.toFloat() / song.duration else 0f
            LinearProgressIndicator(
                progress = { progressFactor },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Long,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    bpm: Double?,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Artwork Grande
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    )
                )
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(24.dp), spotColor = MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (song.albumArtUri == null) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (bpm != null && bpm > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${bpm.toInt()} BPM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Slider(
            value = progress.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..song.duration.coerceAtLeast(1).toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatDuration(progress), style = MaterialTheme.typography.bodySmall)
            Text(text = formatDuration(song.duration), style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Controles Principales
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = null, modifier = Modifier.size(40.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }

            IconButton(onClick = onToggleRepeat) {
                val icon = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                    Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                    else -> Icons.Rounded.Repeat
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun EmptyState(
    isSearch: Boolean,
    onScan: (() -> Unit)? = null,
    onImportFolder: (() -> Unit)? = null,
    onImportFiles: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isSearch) Icons.Rounded.SearchOff else Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearch) "No se encontraron resultados" else "Tu biblioteca está vacía",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isSearch) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escanea el teléfono o elige una carpeta/archivos a mano.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (onScan != null) {
                    Button(onClick = onScan) { Text("Escanear") }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (onImportFolder != null) {
                    OutlinedButton(onClick = onImportFolder) { Text("Elegir carpeta") }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (onImportFiles != null) {
                    OutlinedButton(onClick = onImportFiles) { Text("Elegir archivos") }
                }
            }
        }
    }
}

fun formatDuration(duration: Long): String {
    val totalSeconds = duration / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
