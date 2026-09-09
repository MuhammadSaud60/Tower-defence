package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Lifecycle safety: pause game when app moves to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top HUD Bar (Fixed height and layout)
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

            // 2. Full-Screen Battlefield Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameCanvas(
                    gameState = gameState,
                    onCanvasTap = { vx, vy ->
                        val tapPoint = Point2D(vx, vy)
                        // 1. Check if tapped on existing tower
                        val tappedTower = gameState.towers.firstOrNull {
                            it.position.distanceTo(tapPoint) <= it.spec.size * 0.95f
                        }
                        if (tappedTower != null) {
                            viewModel.selectBuildPosition(null)
                            viewModel.selectExistingTower(tappedTower)
                        } else {
                            // 2. Check if tapped empty buildable ground
                            if (viewModel.isBuildableLocation(tapPoint)) {
                                viewModel.selectExistingTower(null)
                                viewModel.selectBuildPosition(tapPoint)
                            } else {
                                // 3. Tapped unbuildable area (road, water, rocks, base) -> close all menus
                                viewModel.clearSelection()
                            }
                        }
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
                    onCloseBuildMenu = { viewModel.selectBuildPosition(null) }
                )

                // 3. 5-Second Preparation Phase Countdown Banner ("GET READY")
                if (gameState.gameStatus == GameStatus.PREPARATION) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
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
                            .padding(top = 12.dp)
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
            }
        }

        // Pause Modal Dialog
        if (gameState.gameStatus == GameStatus.PAUSED) {
            PauseDialog(
                onResume = { viewModel.resume() },
                onRestart = { viewModel.restart() },
                onMainMenu = onNavigateToMainMenu
            )
        }

        // Game Over Modal Dialog
        if (gameState.gameStatus == GameStatus.GAME_OVER) {
            GameOverDialog(
                gameStatus = gameState.gameStatus,
                currentWave = gameState.currentWave,
                totalWaves = gameState.maxWaves,
                coinsEarned = gameState.totalCoinsEarned,
                enemiesKilled = gameState.enemiesKilledTotal,
                onRestart = { viewModel.restart() },
                onMainMenu = onNavigateToMainMenu
            )
        }

        // Victory Modal Dialog
        if (gameState.gameStatus == GameStatus.VICTORY) {
            VictoryDialog(
                gameState = gameState,
                onPlayAgain = { viewModel.restart() },
                onMapSelection = onNavigateToMapSelect
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
    Card(
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Base Health Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Base Health",
                        tint = if (gameState.base.healthPercentage > 0.4f) Color(0xFF22C55E) else Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(gameState.base.healthPercentage * 100).toInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Coins Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.coins}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD166)
                    )
                }

                // Wave Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WAVE ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${gameState.currentWave}/${gameState.maxWaves}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Enemies Remaining
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ENEMIES: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${gameState.enemiesRemaining}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E)
                    )
                }

                // Speed and Pause controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleSpeed,
                        modifier = Modifier.size(36.dp).testTag("speed_toggle_button")
                    ) {
                        Text(
                            text = "${gameState.gameSpeedMultiplier.toInt()}x",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (gameState.gameSpeedMultiplier > 1f) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                        )
                    }

                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

