package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.MainMenuScreen
import com.example.ui.MapSelectionScreen
import com.example.ui.SettingsDialog
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    MAIN_MENU,
    MAP_SELECT,
    GAME
}

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TowerDefenseApp(gameViewModel = gameViewModel)
            }
        }
    }
}

@Composable
fun TowerDefenseApp(gameViewModel: GameViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN_MENU) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        when (currentScreen) {
            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onPlayClick = {
                        currentScreen = AppScreen.MAP_SELECT
                    },
                    onSettingsClick = {
                        showSettingsDialog = true
                    }
                )
            }

            AppScreen.MAP_SELECT -> {
                MapSelectionScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onSelectMap = { selectedMap ->
                        gameViewModel.loadMap(selectedMap)
                        currentScreen = AppScreen.GAME
                    },
                    onBackToMenu = {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                )
            }

            AppScreen.GAME -> {
                GameScreen(
                    viewModel = gameViewModel,
                    onNavigateToMainMenu = {
                        gameViewModel.pause()
                        currentScreen = AppScreen.MAIN_MENU
                    },
                    onNavigateToMapSelect = {
                        gameViewModel.pause()
                        currentScreen = AppScreen.MAP_SELECT
                    }
                )
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
