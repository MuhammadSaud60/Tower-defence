package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ads.AdManager
import com.example.audio.AndroidAudioPlayer
import com.example.ui.AchievementsScreen
import com.example.ui.GameScreen
import com.example.ui.GameViewModel
import com.example.ui.MainMenuScreen
import com.example.ui.MapSelectionScreen
import com.example.ui.PlayerProfileScreen
import com.example.ui.ResearchLabScreen
import com.example.ui.SettingsDialog
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    SPLASH,
    MAIN_MENU,
    MAP_SELECT,
    GAME,
    RESEARCH_LAB,
    PROFILE,
    ACHIEVEMENTS
}

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()
    private var isGameActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAudioPlayer.getInstance(applicationContext)
        AdManager.getInstance().initialize(applicationContext)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TowerDefenseApp(
                    gameViewModel = gameViewModel,
                    onGameActiveChanged = { active ->
                        isGameActive = active
                        if (active) {
                            hideSystemBars()
                        } else {
                            showSystemBars()
                        }
                    }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        AndroidAudioPlayer.getInstance().pauseAmbienceAndMusic()
    }

    override fun onResume() {
        super.onResume()
        if (isGameActive) {
            AndroidAudioPlayer.getInstance().resumeAmbienceAndMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidAudioPlayer.getInstance().release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Maintain sticky immersive mode during game time when window regains focus
        if (hasFocus && isGameActive) {
            hideSystemBars()
        }
    }

    fun hideSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun showSystemBars() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun TowerDefenseApp(
    gameViewModel: GameViewModel,
    onGameActiveChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableStateOf(0L) }

    LaunchedEffect(currentScreen) {
        val isGame = currentScreen == AppScreen.GAME
        gameViewModel.updateGameSessionActive(isGame)
        onGameActiveChanged(isGame)
    }

    // Single click on back button must NEVER close the game:
    // 1. Splash screen: absorb back press
    if (currentScreen == AppScreen.SPLASH) {
        BackHandler { /* Do nothing while splash completes */ }
    }

    // 2. Settings dialog open: close dialog
    if (showSettingsDialog) {
        BackHandler {
            showSettingsDialog = false
        }
    } else if (currentScreen in listOf(AppScreen.MAP_SELECT, AppScreen.RESEARCH_LAB, AppScreen.PROFILE, AppScreen.ACHIEVEMENTS)) {
        // 3. Sub-screens: return to main menu
        BackHandler {
            currentScreen = AppScreen.MAIN_MENU
        }
    } else if (currentScreen == AppScreen.MAIN_MENU) {
        // 4. Main Menu screen: clicking back once does NOT close the game; requires double tap within 2s
        BackHandler {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                (context as? ComponentActivity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                )
            }

            AppScreen.MAIN_MENU -> {
                MainMenuScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onPlayClick = {
                        currentScreen = AppScreen.MAP_SELECT
                    },
                    onSettingsClick = {
                        showSettingsDialog = true
                    },
                    onOpenResearchLab = {
                        currentScreen = AppScreen.RESEARCH_LAB
                    },
                    onOpenProfile = {
                        currentScreen = AppScreen.PROFILE
                    },
                    onOpenAchievements = {
                        currentScreen = AppScreen.ACHIEVEMENTS
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
                    },
                    onNavigateToResearch = {
                        gameViewModel.pause()
                        currentScreen = AppScreen.RESEARCH_LAB
                    }
                )
            }

            AppScreen.RESEARCH_LAB -> {
                ResearchLabScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onBack = {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                )
            }

            AppScreen.PROFILE -> {
                PlayerProfileScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onBack = {
                        currentScreen = AppScreen.MAIN_MENU
                    }
                )
            }

            AppScreen.ACHIEVEMENTS -> {
                AchievementsScreen(
                    progressionManager = gameViewModel.progressionManager,
                    onBack = {
                        currentScreen = AppScreen.MAIN_MENU
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
