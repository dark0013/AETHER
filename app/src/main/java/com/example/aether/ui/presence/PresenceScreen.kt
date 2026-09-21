package com.example.aether.ui.presence

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aether.ui.AetherSettingsButton
import com.example.aether.ui.MusicViewModel
import com.example.aether.ui.formatDuration
import com.example.aether.ui.skin.LocalSkin
import com.example.aether.ui.skin.SkinVisualizer
import com.example.aether.ui.skin.rememberPlayButtonScale

@Composable
fun PresenceScreen(
    viewModel: MusicViewModel,
    onOpenLibrary: () -> Unit,
    onOpenSessionMap: () -> Unit,
    onImportFolder: () -> Unit,
    onImportFiles: () -> Unit,
    skinViewModel: com.example.aether.ui.skin.SkinViewModel
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val isChromeVisible by viewModel.isChromeVisible.collectAsState()
    val isRitualMode by viewModel.isRitualMode.collectAsState()
    val tape by viewModel.currentTape.collectAsState()
    val marks by viewModel.currentMarks.collectAsState()
    val profile by viewModel.currentProfile.collectAsState()
    val audioFeatures by viewModel.realtimeAudioFeatures.collectAsState()
    
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val skin = LocalSkin.current
    val ritualAmber = Color(0xFFE8A87C)

    DisposableEffect(isPlaying, isRitualMode) {
        view.keepScreenOn = isPlaying || isRitualMode
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(isRitualMode) {
        if (isRitualMode) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skin.colors.background)
            .presenceGestures(
                isRitualMode = isRitualMode,
                onTap = { viewModel.toggleChrome() },
                onDoubleTap = { viewModel.togglePlayPause() },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.addMark()
                },
                onVolumeDelta = { viewModel.adjustVolume(it) },
                onFlickNext = { viewModel.skipNext() },
                onFlickPrevious = { viewModel.skipPrevious() },
                onPinchOpenSession = onOpenSessionMap,
                onSwipeFromLeftEdge = onOpenLibrary
            )
    ) {
        SkinVisualizer(
            features = audioFeatures,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        val overlayAlpha by animateFloatAsState(
            targetValue = if (isRitualMode) 0.55f else 0.0f,
            animationSpec = tween(500),
            label = "ritual_overlay"
        )
        if (isRitualMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = overlayAlpha * 0.25f),
                                Color.Black.copy(alpha = overlayAlpha * 0.4f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .border(
                        1.5.dp,
                        ritualAmber.copy(alpha = 0.55f),
                        RoundedCornerShape(skin.cardRadius)
                    )
            )
        }

        // 2. Density Tape (Always visible but sutil)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DensityTapeWidget(
                tape = tape,
                marks = marks,
                progress = progress,
                duration = currentSong?.duration ?: 0L,
                onSeek = { viewModel.seekTo(it, snapToOnset = true) }
            )
            if (!isChromeVisible && !isRitualMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Desliza para controlar",
                    style = MaterialTheme.typography.labelSmall,
                    color = skin.colors.onBackground.copy(alpha = 0.45f)
                )
            }
        }

        AnimatedVisibility(
            visible = isRitualMode,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Anchor,
                        contentDescription = null,
                        tint = ritualAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RITUAL",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            ),
                            color = ritualAmber
                        )
                        Text(
                            text = "Esta canción no avanza",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.toggleRitualMode() }) {
                        Text("Salir", color = ritualAmber, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = if (isRitualMode) 72.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenLibrary) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = "Biblioteca",
                    tint = skin.colors.onBackground
                )
            }
            AetherSettingsButton(
                onImportFolder = onImportFolder,
                onImportFiles = onImportFiles,
                skinViewModel = skinViewModel,
                tint = skin.colors.onBackground
            )
            Text(
                text = "AETHER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                ),
                color = skin.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
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
            IconButton(onClick = { viewModel.toggleRitualMode() }) {
                Icon(
                    Icons.Rounded.Anchor,
                    contentDescription = "Ritual",
                    tint = if (isRitualMode) ritualAmber else skin.colors.onBackground
                )
            }
        }

        // 3. Chrome efímero (título / play)
        AnimatedVisibility(
            visible = isChromeVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Center info
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.62f)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = currentSong?.title ?: "AETHER",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = skin.colors.onBackground,
                        maxLines = 2
                    )
                    Text(
                        text = when {
                            isRitualMode -> "Anclada · una sola canción"
                            currentSong != null -> currentSong!!.artist
                            else -> "Pulsa play para empezar"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRitualMode) ritualAmber.copy(alpha = 0.9f) else skin.colors.onBackground.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val playScale = rememberPlayButtonScale(isPlaying)
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer(scaleX = playScale, scaleY = playScale)
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
                    color = skin.colors.onBackground.copy(alpha = 0.5f),
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
