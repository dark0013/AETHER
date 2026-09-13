package com.example.aether.ui.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.aether.data.MusicRepository
import com.example.aether.data.db.entities.PlaylistSummary
import com.example.aether.model.Song
import com.example.aether.ui.MusicViewModel
import kotlin.math.roundToInt

private enum class PlaylistStep { Name, Pick, Order }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistListPanel(
    playlists: List<PlaylistSummary>,
    selecting: Boolean,
    selectedIds: Set<Long>,
    onOpen: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onEnterSelect: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Aún no hay playlists",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Crea una, marca canciones y ordénalas arrastrando.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(playlists, key = { it.id }) { playlist ->
            val checked = playlist.id in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selecting) onToggleSelect(playlist.id) else onOpen(playlist.id)
                        },
                        onLongClick = { onEnterSelect(playlist.id) }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selecting) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleSelect(playlist.id) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${playlist.trackCount} ${if (playlist.trackCount == 1) "canción" else "canciones"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!selecting) {
                    IconButton(onClick = { onPlay(playlist.id) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditorScreen(
    viewModel: MusicViewModel,
    playlistId: Long?,
    librarySongs: List<Song>,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Song>() }
    var step by remember { mutableStateOf(if (playlistId == null) PlaylistStep.Name else PlaylistStep.Order) }
    var loaded by remember { mutableStateOf(false) }
    var pickQuery by remember { mutableStateOf("") }
    val playlists by viewModel.playlists.collectAsState()
    val nameTaken = playlists.any {
        it.id != (playlistId ?: -1L) && it.name.trim().equals(name.trim(), ignoreCase = true)
    }
    val nameValid = name.trim().isNotEmpty() && name.trim().length <= MusicRepository.MAX_PLAYLIST_NAME && !nameTaken

    LaunchedEffect(playlistId) {
        val (loadedName, tracks) = viewModel.loadPlaylistEditor(playlistId)
        name = loadedName
        selected.clear()
        selected.addAll(tracks)
        step = if (playlistId == null) PlaylistStep.Name else PlaylistStep.Order
        loaded = true
    }

    val selectedIds = selected.map { it.id }.toSet()
    val title = when (step) {
        PlaylistStep.Name -> "Nueva playlist"
        PlaylistStep.Pick -> "Elige canciones"
        PlaylistStep.Order -> name.ifBlank { "Playlist" }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (step) {
                            PlaylistStep.Pick -> step = if (playlistId == null && selected.isEmpty()) PlaylistStep.Name else PlaylistStep.Order
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    when (step) {
                        PlaylistStep.Name -> {
                            TextButton(
                                onClick = { step = PlaylistStep.Pick },
                                enabled = nameValid
                            ) { Text("Siguiente") }
                        }
                        PlaylistStep.Pick -> {
                            TextButton(onClick = { step = PlaylistStep.Order }) {
                                Text("OK")
                            }
                        }
                        PlaylistStep.Order -> {
                            TextButton(
                                onClick = {
                                    viewModel.savePlaylist(playlistId, name, selected.map { it.id }) { onBack() }
                                },
                                enabled = loaded && nameValid
                            ) { Text("Guardar") }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            when (step) {
                PlaylistStep.Order -> {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(
                            onClick = { step = PlaylistStep.Pick },
                            modifier = Modifier.padding(bottom = 12.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Agregar canciones")
                        }
                        if (selected.isNotEmpty()) {
                            FloatingActionButton(
                                onClick = {
                                    viewModel.savePlaylist(playlistId, name, selected.map { it.id }) {
                                        viewModel.playPlaylist(it)
                                        onBack()
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    ) { padding ->
        when (step) {
            PlaylistStep.Name -> NameStep(
                name = name,
                nameTaken = nameTaken,
                onNameChange = { incoming ->
                    name = incoming.take(MusicRepository.MAX_PLAYLIST_NAME)
                },
                onContinue = { if (nameValid) step = PlaylistStep.Pick },
                modifier = Modifier.padding(padding)
            )
            PlaylistStep.Pick -> PickStep(
                librarySongs = librarySongs,
                selectedIds = selectedIds,
                query = pickQuery,
                onQueryChange = { pickQuery = it },
                onToggle = { song, checked ->
                    if (checked) {
                        if (song.id !in selectedIds) selected.add(song)
                    } else {
                        selected.removeAll { it.id == song.id }
                    }
                },
                onOk = { step = PlaylistStep.Order },
                modifier = Modifier.padding(padding)
            )
            PlaylistStep.Order -> OrderStep(
                songs = selected,
                onMove = { from, to ->
                    if (from != to && from in selected.indices && to in selected.indices) {
                        selected.add(to, selected.removeAt(from))
                    }
                },
                onRemove = { index -> if (index in selected.indices) selected.removeAt(index) },
                onAddMore = { step = PlaylistStep.Pick },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    nameTaken: Boolean,
    onNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmed = name.trim()
    val canContinue = trimmed.isNotEmpty() && trimmed.length <= MusicRepository.MAX_PLAYLIST_NAME && !nameTaken
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "¿Cómo se llama esta playlist?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre") },
            supportingText = {
                Text(
                    when {
                        nameTaken -> "Ya existe una playlist con ese nombre"
                        else -> "${name.length}/${MusicRepository.MAX_PLAYLIST_NAME}"
                    }
                )
            },
            isError = nameTaken,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onContinue, enabled = canContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Elegir canciones")
        }
    }
}

@Composable
private fun PickStep(
    librarySongs: List<Song>,
    selectedIds: Set<Long>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: (Song, Boolean) -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = remember(librarySongs, query) {
        if (query.isBlank()) librarySongs
        else librarySongs.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Marca una a una. Luego pulsa OK.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Buscar") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Text(
            "${selectedIds.size} seleccionadas",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(filtered, key = { it.id }) { song ->
                val checked = song.id in selectedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(song, !checked) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(song, it) })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(
                            song.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (checked) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Button(
            onClick = onOk,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("OK · ver seleccionadas")
        }
    }
}

@Composable
private fun OrderStep(
    songs: MutableList<Song>,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAddMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            "Mantén presionada una canción y arrástrala hasta el puesto que quieras.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onAddMore, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Agregar canciones")
        }
        if (songs.isEmpty()) {
            Text(
                "Aún no hay canciones. Agrega algunas para armar el orden.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    val isDragging = draggingId == song.id
                    Row(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(8.dp))
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .pointerInput(song.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingId = song.id
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        val id = draggingId ?: return@detectDragGesturesAfterLongPress
                                        val from = songs.indexOfFirst { it.id == id }
                                        if (from < 0) return@detectDragGesturesAfterLongPress
                                        dragOffset += amount.y
                                        val shift = (dragOffset / itemHeightPx).roundToInt()
                                        val to = (from + shift).coerceIn(0, songs.lastIndex)
                                        if (to != from) {
                                            onMove(from, to)
                                            dragOffset -= (to - from) * itemHeightPx
                                        }
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Arrastrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${index + 1}",
                            modifier = Modifier.width(28.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(
                                song.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onRemove(songs.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: index) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Quitar")
                        }
                    }
                }
            }
        }
    }
}
