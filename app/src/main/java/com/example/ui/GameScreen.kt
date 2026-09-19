package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.entities.Point2D
import com.example.game.GameStatus
import com.example.game.GameState
import kotlin.math.ceil

/**
 * Main gameplay screen containing:
 * - Top HUD with Base Health, Coins, Wave counter, Speed multiplier, and Pause
 * - Boss Health Banner (when boss is active)
 * - 5-Second Preparation Phase Countdown ("GET READY")
 * - Full-height 2D battlefield Canvas
 * - Contextual circular/radial tower menus (Build on empty plot, Upgrade on tower)
 * - Pause, Game Over, and Victory dialogs
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToMainMenu: () -> Unit,
    onNavigateToMapSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showExitConfirmation by remember { mutableStateOf(false) }

    // Android Back Button Handling with strict priority order:
    // 1. Exit Confirmation open -> closes popup and continues game
    // 2. Victory or Defeat popup open -> closes popup / returns to level selection (home)
    // 3. Tower upgrade or build menu open -> closes menu first
    // 4. Pause menu open -> closes pause menu and resumes
    // 5. Normal gameplay -> pauses game and shows "Exit current mission?" confirmation
    val isResultPopupOpen = gameState.gameStatus == GameStatus.VICTORY || gameState.gameStatus == GameStatus.GAME_OVER
    val isTowerOrBuildMenuOpen = gameState.selectedExistingTower != null ||
            gameState.selectedBuildPos != null ||
            gameState.isBuildingTower ||
            gameState.selectedDestructibleId != null
    val isPauseMenuOpen = gameState.gameStatus == GameStatus.PAUSED && !showExitConfirmation

    BackHandler(enabled = true) {
        when {
            showExitConfirmation -> {
                showExitConfirmation = false
                viewModel.resume()
            }
            isResultPopupOpen -> {
                onNavigateToMapSelect()
            }
            isTowerOrBuildMenuOpen -> {
                viewModel.clearSelection()
                viewModel.selectBuildPosition(null)
                viewModel.selectExistingTower(null)
                viewModel.cancelTowerBuild()
                viewModel.selectDestructible(null)
            }
            isPauseMenuOpen -> {
                viewModel.resume()
            }
            else -> {
                viewModel.pause()
                showExitConfirmation = true
            }
        }
    }

    val context = LocalContext.current

    // Immersive Mode: hide navigation and status bar during game time
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            val activity = context as? Activity
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Lifecycle safety: pause game when app moves to background & re-hide system bars on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val activity = context as? Activity
                activity?.window?.let { window ->
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                }
            } else if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(gameState.gameStatus) {
        if (gameState.gameStatus == GameStatus.VICTORY) {
            viewModel.handleVictoryProgression()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Full-Screen Battlefield Canvas Area (Uses entire available screen)
        GameCanvas(
            gameState = gameState,
            onCanvasTap = { vx, vy ->
                val tapPoint = Point2D(vx, vy)
                val currentlySelectedTower = gameState.selectedExistingTower

                // 1. Existing Tower (Priority 1)
                val tappedTower = gameState.towers
                    .minByOrNull { it.position.distanceTo(tapPoint) }
                    ?.takeIf { it.position.distanceTo(tapPoint) <= maxOf(it.spec.size * 0.95f, 40f) }

                if (tappedTower != null) {
                    viewModel.selectBuildPosition(null)
                    val activeDestId = gameState.selectedDestructibleId
                    if (activeDestId != null) {
                        val dest = gameState.destructibles.firstOrNull { it.id == activeDestId && it.isAlive }
                        if (dest != null && tappedTower.isObjectInRange(dest.position, dest.radius)) {
                            viewModel.setTowerManualTarget(tappedTower.id, dest.id, com.example.entities.TargetType.DESTRUCTIBLE)
                        } else {
                            viewModel.selectDestructible(null)
                        }
                    } else {
                        viewModel.selectDestructible(null)
                    }
                    viewModel.selectExistingTower(tappedTower)
                    return@GameCanvas
                }

                // 2. Enemy (Priority 2)
                val tappedEnemy = gameState.enemies
                    .filter { it.isAlive && !it.reachedBase }
                    .minByOrNull { it.position.distanceTo(tapPoint) }
                    ?.takeIf { it.position.distanceTo(tapPoint) <= maxOf(it.spec.radius * 1.6f, 38f) }

                if (tappedEnemy != null) {
                    if (currentlySelectedTower != null) {
                        if (currentlySelectedTower.isEnemyInRange(tappedEnemy.position)) {
                            viewModel.setTowerManualTarget(currentlySelectedTower.id, tappedEnemy.id, com.example.entities.TargetType.ENEMY)
                            viewModel.selectDestructible(null)
                        } else {
                            viewModel.showNotice("${tappedEnemy.spec.name} is out of range!")
                            viewModel.invalidTargetFeedback()
                        }
                    }
                    return@GameCanvas
                }

                // 3. Destructible Object (Tree/Stone/Crate) (Priority 3)
                val tappedDestructible = gameState.destructibles
                    .filter { it.isAlive }
                    .minByOrNull { it.position.distanceTo(tapPoint) }
                    ?.takeIf { it.position.distanceTo(tapPoint) <= maxOf(it.radius * 1.5f, 42f) }

                if (tappedDestructible != null) {
                    viewModel.selectBuildPosition(null)
                    val isAlreadyTargeted = gameState.selectedDestructibleId == tappedDestructible.id ||
                            gameState.towers.any { it.manualTargetId == tappedDestructible.id }

                    if (isAlreadyTargeted) {
                        viewModel.stopTargetingDestructible(tappedDestructible.id)
                        return@GameCanvas
                    }

                    viewModel.selectDestructible(tappedDestructible.id)

                    val towerToTarget = if (currentlySelectedTower != null && currentlySelectedTower.isObjectInRange(tappedDestructible.position, tappedDestructible.radius)) {
                        currentlySelectedTower
                    } else {
                        gameState.towers
                            .filter { it.isObjectInRange(tappedDestructible.position, tappedDestructible.radius) }
                            .minByOrNull { it.position.distanceTo(tappedDestructible.position) }
                    }

                    if (towerToTarget != null) {
                        viewModel.selectExistingTower(towerToTarget)
                        viewModel.setTowerManualTarget(towerToTarget.id, tappedDestructible.id, com.example.entities.TargetType.DESTRUCTIBLE)
                    } else if (currentlySelectedTower != null) {
                        viewModel.showNotice("${tappedDestructible.type.displayName} is out of range!")
                        viewModel.invalidTargetFeedback()
                    } else {
                        viewModel.showNotice("${tappedDestructible.type.displayName} - Select a tower to target it")
                    }
                    return@GameCanvas
                }

                // 4. Empty Buildable Ground (Priority 4)
                if (viewModel.isValidTowerPlacement(tapPoint.x, tapPoint.y)) {
                    viewModel.selectExistingTower(null)
                    viewModel.selectDestructible(null)
                    viewModel.selectBuildPosition(tapPoint)
                    return@GameCanvas
                }

                // 5. Empty Ground / Unbuildable terrain (Priority 5)
                viewModel.clearSelection()
            },
            onUpgradeTower = { viewModel.upgradeSelectedTower() },
            onSellTower = { viewModel.sellSelectedTower() },
            onStrategyChange = { strategy -> viewModel.setStrategyForSelectedTower(strategy) },
            onDeselectTower = { viewModel.selectExistingTower(null) },
            onSelectBuildTower = { type ->
                val pos = gameState.selectedBuildPos
                if (pos != null) {
                    viewModel.placeTowerAt(type, pos.x, pos.y)
                }
            },
            onCloseBuildMenu = { viewModel.selectBuildPosition(null) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Fixed Top HUD Bar touching screen borders directly (World moves underneath it)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            GameHudBar(
                gameState = gameState,
                onPauseClick = { viewModel.pause() },
                onToggleSpeed = { viewModel.toggleSpeed() }
            )

            // Level 1 First Gun Tutorial (Only shown in Level 1 until completed or skipped)
            Level1TutorialBanner(
                gameState = gameState,
                progressionManager = viewModel.progressionManager
            )
        }

        // 3. 5-Second Preparation Phase Countdown Banner ("GET READY")
        if (gameState.gameStatus == GameStatus.PREPARATION) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                PreparationCountdownBanner(
                    countdownSeconds = ceil(gameState.preparationCountdown).toInt().coerceAtLeast(1)
                )
            }
        }

        // 4. Placement Notice / Wave Transition Alert Banner
        if (gameState.placementNotice != null && gameState.gameStatus != GameStatus.PREPARATION) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Text(
                        text = gameState.placementNotice.orEmpty(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Pause Modal Dialog
        if (gameState.gameStatus == GameStatus.PAUSED && !showExitConfirmation) {
            PauseDialog(
                onResume = { viewModel.resume() },
                onRestart = { viewModel.restart() },
                onMainMenu = onNavigateToMapSelect
            )
        }

        // Defeat / Game Over Modal Dialog
        if (gameState.gameStatus == GameStatus.GAME_OVER) {
            GameOverDialog(
                gameStatus = gameState.gameStatus,
                currentWave = gameState.currentWave,
                totalWaves = gameState.maxWaves,
                coinsEarned = gameState.totalCoinsEarned,
                enemiesKilled = gameState.enemiesKilledTotal,
                onRestart = { viewModel.restart() },
                onMainMenu = onNavigateToMapSelect
            )
        }

        // Victory Modal Dialog
        if (gameState.gameStatus == GameStatus.VICTORY) {
            val nextMap = viewModel.getNextMap()
            VictoryDialog(
                gameState = gameState,
                hasNextLevel = nextMap != null,
                onNextLevel = { viewModel.loadNextMap() },
                onRetry = { viewModel.restart() },
                onHome = onNavigateToMapSelect
            )
        }

        // Exit Mission Confirmation Dialog (triggered via Android back button)
        if (showExitConfirmation) {
            ExitMissionDialog(
                onContinue = {
                    showExitConfirmation = false
                    viewModel.resume()
                },
                onExit = {
                    showExitConfirmation = false
                    onNavigateToMapSelect()
                }
            )
        }
    }
}

/**
 * 5-Second Preparation Phase Banner.
 * Displays "GET READY" countdown at the beginning of each mission.
 */
@Composable
private fun PreparationCountdownBanner(
    countdownSeconds: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE0B132B),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        shadowElevation = 10.dp,
        modifier = Modifier.testTag("preparation_countdown_banner")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "GET READY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$countdownSeconds",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD166)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap empty ground to place towers",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun GameHudBar(
    gameState: GameState,
    onPauseClick: () -> Unit,
    onToggleSpeed: () -> Unit
) {
    Surface(
        shape = RectangleShape,
        color = Color(0xF50B132B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_hud_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Tokens & Wave Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Tokens / Gold Capsule Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xD90F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFD166)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Tokens",
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${gameState.coins}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD166),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // 2. Wave Capsule Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xD90F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "WAVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${gameState.currentWave}/${gameState.maxWaves}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8)
                        )
                        if (gameState.wavesCleared > 0) {
                            val hpBonus = ((gameState.enemyHpMultiplier - 1f) * 100f).toInt()
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0x33EF4444),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x88EF4444))
                            ) {
                                Text(
                                    text = "+$hpBonus% HP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Group: Enemies Remaining, Speed Multiplier, and Pause Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 3. Enemies Remaining Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xD90F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x55F43F5E)),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "ENEMIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${gameState.enemiesRemaining}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }

                // 4. Speed Multiplier Capsule Toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (gameState.gameSpeedMultiplier > 1f) Color(0x400284C7) else Color(0xD90F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (gameState.gameSpeedMultiplier > 1f) Color(0xFF38BDF8) else Color(0x44475569)
                    ),
                    modifier = Modifier
                        .clickable(onClick = onToggleSpeed)
                        .testTag("speed_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${gameState.gameSpeedMultiplier.toInt()}X",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.gameSpeedMultiplier > 1f) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }
                }

                // 5. Pause Button (Circular action button)
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xD90F172A), CircleShape)
                        .border(1.dp, Color(0x44475569), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

