package com.example.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { AppTheme { AppNav() } }
    }
}

@Composable
fun AppNav(vm: MusicViewModel = viewModel()) {
    val nav = rememberNavController()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBars = route == "home" || route == "search"

    fun openTab(target: String) {
        nav.navigate(target) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun playAndOpen(list: List<Song>, index: Int) {
        vm.play(list, index)
        nav.navigate("player")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (showBars) {
                Column {
                    vm.currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = vm.isPlaying,
                            onToggle = { vm.togglePlay() },
                            onClick = { nav.navigate("player") }
                        )
                    }
                    AppBottomBar(selected = route ?: "home", onSelect = { openTab(it) })
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    sections = vm.sections,
                    loading = vm.loading,
                    error = vm.error,
                    onRetry = { vm.loadSongs() },
                    onSongClick = { list, index -> playAndOpen(list, index) }
                )
            }
            composable("search") {
                SearchScreen(
                    results = vm.searchResults,
                    searching = vm.searching,
                    error = vm.searchError,
                    onQueryChange = { vm.onQueryChange(it) },
                    onSearch = { vm.search(it) },
                    onSongClick = { list, index -> playAndOpen(list, index) }
                )
            }
            composable("player") {
                vm.currentSong?.let { song ->
                    PlayerScreen(
                        song = song,
                        isPlaying = vm.isPlaying,
                        onToggle = { vm.togglePlay() },
                        onNext = { vm.next() },
                        onPrevious = { vm.previous() },
                        position = vm.position,
                        duration = vm.duration,
                        onSeek = { vm.seekTo(it) },
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
