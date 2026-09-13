package com.example.aether.ui

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.aether.analysis.AnalysisScheduler
import com.example.aether.analysis.SimilarityEngine
import com.example.aether.analysis.TransitEngine
import com.example.aether.data.MusicRepository
import com.example.aether.data.db.entities.DensityTapeEntity
import com.example.aether.data.db.entities.MarkEntity
import com.example.aether.data.db.entities.PlaylistSummary
import com.example.aether.data.db.entities.ProfileEntity
import com.example.aether.model.Song
import com.example.aether.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import android.os.Bundle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RealtimeAudioFeatures(
    val energy: Float = 0f,
    val centroid: Float = 0f,
    val flux: Float = 0f,
    val isOnset: Boolean = false
)

data class SessionItem(
    val song: Song,
    val tape: DensityTapeEntity?
)

data class UserNotice(
    val message: String,
    val id: Long = System.currentTimeMillis()
)

class MusicViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    val songs: StateFlow<List<Song>> = repository.getSongsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(songs, _searchQuery) { songs, query ->
        if (query.isBlank()) songs
        else songs.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.artist.contains(query, ignoreCase = true) 
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.getPlaylistsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _activePlaylistName = MutableStateFlow<String?>(null)
    val activePlaylistName: StateFlow<String?> = _activePlaylistName.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentProfile: StateFlow<ProfileEntity?> = _currentSong
        .flatMapLatest { song ->
            if (song != null) repository.getProfileFlow(song.id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTape: StateFlow<DensityTapeEntity?> = _currentSong
        .flatMapLatest { song ->
            if (song != null) repository.getTapeFlow(song.id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMarks: StateFlow<List<MarkEntity>> = _currentSong
        .flatMapLatest { song ->
            if (song != null) repository.getMarksFlow(song.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionItems: StateFlow<List<SessionItem>> = repository.getActiveSessionFlow()
        .flatMapLatest { session ->
            if (session == null || session.trackIds.isBlank()) {
                flowOf(emptyList())
            } else {
                val ids = session.trackIds.split(",").mapNotNull { it.toLongOrNull() }
                flow {
                    val tapesById = repository.getTapesForSongs(ids).associateBy { it.songId }
                    emitAll(
                        songs.map { allSongs ->
                            val songsById = allSongs.associateBy { it.id }
                            ids.mapNotNull { id -> songsById[id] }.map { song ->
                                SessionItem(song, tapesById[song.id])
                            }
                        }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    val realtimeAudioFeatures: StateFlow<RealtimeAudioFeatures> = combine(
        currentTape,
        playbackProgress
    ) { tape, progress ->
        if (tape == null || tape.energyTape.isEmpty()) {
            RealtimeAudioFeatures()
        } else {
            val hopMs = tape.hopMs.toDouble()
            val frameIndex = (progress / hopMs).toInt().coerceIn(0, tape.frameCount - 1)
            
            RealtimeAudioFeatures(
                energy = (tape.energyTape[frameIndex].toInt() and 0xFF) / 255f,
                centroid = (tape.centroidTape[frameIndex].toInt() and 0xFF) / 255f,
                flux = (tape.fluxTape[frameIndex].toInt() and 0xFF) / 255f,
                isOnset = tape.onsetFlags[frameIndex].toInt() == 1
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RealtimeAudioFeatures())

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _currentTransitionPlan = MutableStateFlow<TransitEngine.TransitionPlan?>(null)
    val currentTransitionPlan: StateFlow<TransitEngine.TransitionPlan?> = _currentTransitionPlan.asStateFlow()

    private val _isChromeVisible = MutableStateFlow(true)
    val isChromeVisible: StateFlow<Boolean> = _isChromeVisible.asStateFlow()

    private val _isHighRefreshRate = MutableStateFlow(true)

    private val _isRitualMode = MutableStateFlow(false)
    val isRitualMode: StateFlow<Boolean> = _isRitualMode.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _userNotice = MutableStateFlow<UserNotice?>(null)
    val userNotice: StateFlow<UserNotice?> = _userNotice.asStateFlow()

    private var chromeHideJob: Job? = null
    private var idleSessionJob: Job? = null
    private var currentSessionId: Long? = null

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var analysisScheduler: AnalysisScheduler? = null
    private var audioManager: AudioManager? = null
    private var volumeStepAccumulator = 0f
    private var playFirstWhenControllerReady = false
    private var activePlaylistId: Long? = null
    
    private val playedHistory = mutableListOf<Long>()
    private val maxHistorySize = 8

    fun loadSongs() {
        viewModelScope.launch {
            repository.syncWithMediaStore()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleChrome() {
        _isChromeVisible.value = !_isChromeVisible.value
        if (_isChromeVisible.value) {
            startChromeTimer()
        }
    }

    fun showChrome() {
        _isChromeVisible.value = true
        startChromeTimer()
    }

    fun setHighRefreshRate(enabled: Boolean) {
        _isHighRefreshRate.value = enabled
    }

    private fun startChromeTimer() {
        chromeHideJob?.cancel()
        chromeHideJob = viewModelScope.launch {
            delay(2200)
            _isChromeVisible.value = false
        }
    }

    private suspend fun suggestNextSong(currentSong: Song): Song? {
        val currentProfile = currentProfile.value ?: return null
        val candidates = repository.getReadyProfilesWithSongs()
        
        val suggestion = SimilarityEngine.suggestNext(
            currentSong = currentSong,
            currentProfile = currentProfile,
            candidates = candidates,
            historyIds = playedHistory.toSet()
        )

        // Si hay sugerencia, planificar la transición inmediatamente
        if (suggestion != null) {
            val nextProfile = candidates.find { it.first.id == suggestion.id }?.second
            val currentTape = currentTape.value
            val nextTape = repository.getTapeFlow(suggestion.id).first()

            val plan = TransitEngine.planCrossfade(
                currentDurationMs = currentSong.duration,
                currentTape = currentTape,
                nextTape = nextTape,
                currentProfile = currentProfile,
                nextProfile = nextProfile
            )
            _currentTransitionPlan.value = plan
            sendTransitionPlanToService(plan)
        }

        return suggestion
    }

    private fun sendTransitionPlanToService(plan: TransitEngine.TransitionPlan) {
        val controller = mediaController ?: return
        val args = Bundle().apply {
            putLong("end_point_ms", plan.endPointMs)
            putLong("start_point_ms", plan.startPointMs)
            putLong("fade_duration_ms", plan.fadeDurationMs)
        }
        controller.sendCustomCommand(SessionCommand("SET_TRANSITION_PLAN", Bundle.EMPTY), args)
    }

    fun initController(context: Context) {
        analysisScheduler = AnalysisScheduler.getInstance(context)
        audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
        syncVolumeFromSystem()
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        idleSessionJob?.cancel()
                    } else {
                        scheduleIdleSessionEnd()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    _userNotice.value = UserNotice("No se pudo leer el archivo")
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val currentSongId = mediaItem?.mediaId?.toLongOrNull()
                    val newSong = songs.value.find { it.id == currentSongId }
                    _currentSong.value = newSong
                    
                    // Actualizar historial y Sesión
                    if (newSong != null) {
                        playedHistory.remove(newSong.id)
                        playedHistory.add(newSong.id)
                        if (playedHistory.size > maxHistorySize) {
                            playedHistory.removeAt(0)
                        }

                        viewModelScope.launch {
                            if (currentSessionId == null) {
                                currentSessionId = repository.startSession(if (_isRitualMode.value) "ritual" else "presence")
                            }
                            currentSessionId?.let { repository.appendTrackToSession(it, newSong.id) }
                        }
                    }

                    // Si la canción terminó automáticamente, preparar la siguiente por similitud
                    // EXCEPTO en modo Ritual
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                        newSong != null &&
                        !_isRitualMode.value &&
                        activePlaylistId == null
                    ) {
                        viewModelScope.launch {
                            val nextSuggestion = suggestNextSong(newSong)
                            if (nextSuggestion != null) {
                                val nextMediaItem = MediaItem.Builder()
                                    .setMediaId(nextSuggestion.id.toString())
                                    .setUri(nextSuggestion.contentUri)
                                    .build()
                                
                                val nextIndex = mediaController?.nextMediaItemIndex ?: -1
                                if (nextIndex != -1) {
                                    mediaController?.replaceMediaItem(nextIndex, nextMediaItem)
                                } else {
                                    mediaController?.addMediaItem(nextMediaItem)
                                }
                            }
                        }
                    }
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
                it.volume = 1f
            }
            startProgressUpdate()
            if (playFirstWhenControllerReady) {
                playFirstWhenControllerReady = false
                togglePlayPause()
            }
        }, MoreExecutors.directExecutor())
    }

    fun consumeNotice() {
        _userNotice.value = null
    }

    private fun scheduleIdleSessionEnd() {
        idleSessionJob?.cancel()
        idleSessionJob = viewModelScope.launch {
            delay(IDLE_SESSION_MS)
            currentSessionId?.let { repository.endSession(it) }
            currentSessionId = null
        }
    }

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (true) {
                _playbackProgress.value = mediaController?.currentPosition ?: 0L
                val playing = mediaController?.isPlaying == true
                val delayMs = when {
                    !playing -> 500L
                    _isHighRefreshRate.value -> 32L
                    else -> 250L
                }
                delay(delayMs)
            }
        }
    }

    fun playSong(song: Song) {
        playQueue(songs.value, songs.value.indexOfFirst { it.id == song.id }, playlistId = null, playlistName = null)
    }

    fun playPlaylist(playlistId: Long, startIndex: Int = 0) {
        viewModelScope.launch {
            val playlist = repository.getPlaylist(playlistId) ?: return@launch
            val tracks = repository.getPlaylistSongs(playlistId)
            if (tracks.isEmpty()) {
                _userNotice.value = UserNotice("La playlist está vacía")
                return@launch
            }
            playQueue(tracks, startIndex.coerceIn(0, tracks.lastIndex), playlistId, playlist.name)
        }
    }

    private fun playQueue(
        queue: List<Song>,
        startIndex: Int,
        playlistId: Long?,
        playlistName: String?
    ) {
        val controller = mediaController ?: return
        if (queue.isEmpty() || startIndex < 0) return
        activePlaylistId = playlistId
        _activePlaylistName.value = playlistName
        controller.shuffleModeEnabled = false
        controller.setMediaItems(queue.map { it.toMediaItem() }, startIndex, 0L)
        controller.prepare()
        controller.play()
        val song = queue[startIndex]
        _currentSong.value = song
        analysisScheduler?.boostAnalysis(song.id)
        if (playlistName != null) {
            _userNotice.value = UserNotice("Reproduciendo: $playlistName")
        }
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .build()

    suspend fun loadPlaylistEditor(id: Long?): Pair<String, List<Song>> {
        if (id == null) return "" to emptyList()
        val playlist = repository.getPlaylist(id) ?: return "" to emptyList()
        return playlist.name to repository.getPlaylistSongs(id)
    }

    fun savePlaylist(id: Long?, name: String, songIds: List<Long>, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val savedId = repository.savePlaylist(id, name, songIds)
            _userNotice.value = UserNotice("Playlist guardada")
            onSaved(savedId)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            if (activePlaylistId == id) {
                activePlaylistId = null
                _activePlaylistName.value = null
            }
            repository.deletePlaylist(id)
        }
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null) {
            playFirstWhenControllerReady = true
            playFirstSong()
            return
        }
        if (controller.isPlaying) {
            controller.pause()
            return
        }
        if (controller.mediaItemCount > 0) {
            controller.play()
            return
        }
        playFirstSong()
    }

    private fun playFirstSong() {
        val first = songs.value.firstOrNull()
        if (first != null) {
            playSong(first)
            return
        }
        viewModelScope.launch {
            val song = repository.getLocalSongs().firstOrNull()
            if (song != null) {
                playSong(song)
            } else {
                _userNotice.value = UserNotice("Tu biblioteca está vacía")
            }
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

    fun toggleRitualMode() {
        val nextMode = !_isRitualMode.value
        _isRitualMode.value = nextMode
        if (nextMode) {
            _isChromeVisible.value = true
            startChromeTimer()
            _userNotice.value = UserNotice("Ritual: esta canción no avanza")
        }

        val controller = mediaController ?: return
        val args = Bundle().apply {
            putBoolean("is_ritual", nextMode)
        }
        controller.sendCustomCommand(SessionCommand("SET_RITUAL_MODE", Bundle.EMPTY), args)
    }

    fun skipNext() {
        mediaController?.seekToNext()
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition >= 4_000L) {
            controller.seekTo(0)
        } else {
            controller.seekToPrevious()
        }
    }

    fun setVolume(volume: Float) {
        applySystemVolume((volume.coerceIn(0f, 1f) * streamMax()).roundToInt())
    }

    fun adjustVolume(delta: Float) {
        val max = streamMax()
        volumeStepAccumulator += delta * max
        val steps = volumeStepAccumulator.toInt()
        if (steps == 0) return
        volumeStepAccumulator -= steps
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: return
        applySystemVolume(current + steps)
    }

    private fun streamMax(): Int =
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1

    private fun applySystemVolume(level: Int) {
        val am = audioManager ?: return
        val max = streamMax()
        val target = level.coerceIn(0, max)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (target == current) {
            syncVolumeFromSystem()
            return
        }
        try {
            am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                target,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (_: SecurityException) {
            // Do Not Disturb or policy may block stream writes.
        }
        mediaController?.volume = 1f
        syncVolumeFromSystem()
    }

    private fun syncVolumeFromSystem() {
        val am = audioManager ?: return
        _volume.value = am.getStreamVolume(AudioManager.STREAM_MUSIC) / streamMax().toFloat()
    }

    fun seekTo(position: Long, snapToOnset: Boolean = false) {
        val finalPosition = if (snapToOnset) {
            calculateSnappedPosition(position)
        } else {
            position
        }
        mediaController?.seekTo(finalPosition)
    }

    private fun calculateSnappedPosition(position: Long): Long {
        val tape = currentTape.value ?: return position
        if (tape.onsetFlags.isEmpty()) return position

        val hopMs = tape.hopMs.toDouble()
        val targetFrame = (position / hopMs).toInt().coerceIn(0, tape.frameCount - 1)
        
        // Search window +- 180ms (~2 frames at 93ms)
        val searchRadius = 2
        val start = (targetFrame - searchRadius).coerceAtLeast(0)
        val end = (targetFrame + searchRadius).coerceAtMost(tape.frameCount - 1)
        
        var bestFrame = targetFrame
        var minDiff = Long.MAX_VALUE
        
        for (i in start..end) {
            if (tape.onsetFlags[i] == 1.toByte()) {
                val framePos = (i * hopMs).toLong()
                val diff = abs(framePos - position)
                if (diff < minDiff) {
                    minDiff = diff
                    bestFrame = i
                }
            }
        }
        
        return (bestFrame * hopMs).toLong()
    }

    fun addMark() {
        val song = currentSong.value ?: return
        val pos = playbackProgress.value
        viewModelScope.launch {
            repository.addMark(song.id, pos)
        }
    }

    override fun onCleared() {
        super.onCleared()
        idleSessionJob?.cancel()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    companion object {
        private const val IDLE_SESSION_MS = 30L * 60L * 1000L
    }
}
