package com.example.aether.ui

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.aether.data.MusicRepository
import com.example.aether.model.Song
import com.example.aether.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(_songs, _searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.artist.contains(query, ignoreCase = true) 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    fun loadSongs() {
        viewModelScope.launch {
            _songs.value = repository.getLocalSongs()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun initController(context: Context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val currentSongId = mediaItem?.mediaId?.toLongOrNull()
                    _currentSong.value = _songs.value.find { it.id == currentSongId }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                }
            })
            // Sincronizar estados iniciales
            mediaController?.let {
                _repeatMode.value = it.repeatMode
                _shuffleModeEnabled.value = it.shuffleModeEnabled
            }
            startProgressUpdate()
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                _playbackProgress.value = mediaController?.currentPosition ?: 0L
                delay(1000)
            }
        }
    }

    fun playSong(song: Song) {
        val controller = mediaController ?: return
        
        // Cargar lista actual al controlador si no está
        if (controller.mediaItemCount == 0) {
            val mediaItems = _songs.value.map { s ->
                MediaItem.Builder()
                    .setMediaId(s.id.toString())
                    .setUri(s.contentUri)
                    .build()
            }
            controller.setMediaItems(mediaItems)
        }

        val index = _songs.value.indexOfFirst { it.id == song.id }
        if (index != -1) {
            controller.seekTo(index, 0)
            controller.prepare()
            controller.play()
        }
        _currentSong.value = song
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
    }

    fun toggleShuffleMode() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun skipNext() {
        mediaController?.seekToNext()
    }

    fun skipPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    override fun onCleared() {
        super.onCleared()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
