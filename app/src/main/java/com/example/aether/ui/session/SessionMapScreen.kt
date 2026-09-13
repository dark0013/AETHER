package com.example.aether.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aether.ui.MusicViewModel
import com.example.aether.ui.SessionItem
import com.example.aether.ui.presence.DensityTapeWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionMapScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val sessionItems by viewModel.sessionItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa de Sesión", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sessionItems) { item ->
                SessionMapItem(
                    item = item,
                    onClick = {
                        viewModel.playSong(item.song)
                        onBack()
                    }
                )
            }
        }
    }
}

@Composable
fun SessionMapItem(
    item: SessionItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = item.song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Mini Tape
        DensityTapeWidget(
            tape = item.tape,
            progress = 0, // In map, we don't show real-time progress for all items
            duration = item.song.duration,
            onSeek = {}, // Map tapes are static
            modifier = Modifier.height(24.dp)
        )
    }
}
