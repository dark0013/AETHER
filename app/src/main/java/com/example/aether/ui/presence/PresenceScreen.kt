package com.example.aether.ui.presence

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aether.ui.MusicViewModel
import com.example.aether.ui.formatDuration
import kotlin.math.abs

@Composable
fun PresenceScreen(
    viewModel: MusicViewModel,
    onOpenLibrary: () -> Unit,
    onOpenSessionMap: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val isChromeVisible by viewModel.isChromeVisible.collectAsState()
    val isRitualMode by viewModel.isRitualMode.collectAsState()
    val tape by viewModel.currentTape.collectAsState()
    val marks by viewModel.currentMarks.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val audioFeatures by viewModel.realtimeAudioFeatures.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.toggleChrome() },
                    onDoubleTap = { viewModel.togglePlayPause() },
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.addMark() 
                    }
                )
            }
            .pointerInput(Unit) {
                var totalDragX = 0f
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { 
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        
                        // Vertical drag for volume
                        if (abs(totalDragY) > abs(totalDragX)) {
                            val sensitivity = 0.005f
                            viewModel.setVolume(volume - (dragAmount.y * sensitivity))
                        }
                    },
                    onDragEnd = {
                        // Horizontal flick threshold
                        if (abs(totalDragX) > 300f && abs(totalDragX) > abs(totalDragY)) {
                            if (totalDragX > 0) viewModel.skipNext() else viewModel.skipPrevious()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom < 0.8f) { // Pinch out (alejar)
                        onOpenSessionMap()
                    }
                }
            }
    ) {
        // 0. Shader Background
        PresenceVisualizer(
            features = audioFeatures,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = if (isRitualMode) 0.7f else 1f)
        )

        // 1. Background Artwork (blended)
        if (currentSong != null) {
            AsyncImage(
                model = currentSong!!.albumArtUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (isRitualMode) 0.2f else 0.3f // Reduced alpha to blend with shader
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // 2. Density Tape (Always visible but sutil)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 16.dp)
        ) {
            DensityTapeWidget(
                tape = tape,
                marks = marks,
                progress = progress,
                duration = currentSong?.duration ?: 0L,
                onSeek = { viewModel.seekTo(it, snapToOnset = true) }
            )
        }

        // 3. Chrome Efímero
        AnimatedVisibility(
            visible = isChromeVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenLibrary) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library", tint = Color.White)
                    }

                    IconButton(onClick = { viewModel.toggleRitualMode() }) {
                        Icon(
                            Icons.Rounded.Anchor,
                            contentDescription = "Ritual",
                            tint = if (isRitualMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    
                    if (profile?.bpm != null && profile!!.bpm!! > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${profile!!.bpm!!.toInt()} BPM",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Center info
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong?.title ?: "AETHER",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White,
                        maxLines = 2
                    )
                    Text(
                        text = currentSong?.artist ?: "Selecciona una canción",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Simple Play/Pause in center chrome
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Bottom time
                Text(
                    text = "${formatDuration(progress)} / ${formatDuration(currentSong?.duration ?: 0L)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    // Trigger auto-hide timer when chrome becomes visible
    LaunchedEffect(isChromeVisible) {
        if (isChromeVisible) {
            viewModel.showChrome() // This starts the timer
        }
    }
}
