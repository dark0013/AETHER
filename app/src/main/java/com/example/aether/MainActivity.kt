package com.example.aether

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aether.data.MusicRepository
import com.example.aether.ui.MusicApp
import com.example.aether.ui.MusicViewModel
import com.example.aether.ui.PermissionScreen
import com.example.aether.ui.theme.AETHERTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AETHERTheme {
                val context = LocalContext.current
                val repository = remember { MusicRepository(context) }
                val viewModel: MusicViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MusicViewModel(repository) as T
                        }
                    }
                )

                var hasPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_MEDIA_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasPermission = isGranted
                    if (isGranted) {
                        viewModel.loadSongs()
                    }
                }

                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                LaunchedEffect(hasPermission) {
                    if (hasPermission) {
                        viewModel.loadSongs()
                        viewModel.initController(context.applicationContext)
                    }
                }

                if (hasPermission) {
                    MusicApp(viewModel = viewModel)
                } else {
                    PermissionScreen(onRequestAccess = { launcher.launch(permission) })
                }
            }
        }
    }
}
