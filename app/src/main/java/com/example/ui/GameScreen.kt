package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
    onNavigateToResearch: () -> Unit = {},
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
            onPlacementDrag = { vx, vy ->
                viewModel.updatePlacementPreview(vx, vy)
            },
            onCanvasTap = { vx, vy ->
                val tapPoint = Point2D(vx, vy)

                // 0. Active Weapon Placement Mode (Priority 0)
                if (gameState.isBuildingTower) {
                    val spec = gameState.selectedTowerSpec
                    if (spec != null) {
                        if (viewModel.isValidTowerPlacement(vx, vy)) {
                            viewModel.placeTowerAt(spec.type, vx, vy)
                        } else {
                            viewModel.showNotice("Blocked: Cannot deploy on obstacles, roads, or water!")
                            viewModel.invalidTargetFeedback()
                        }
                    }
                    return@GameCanvas
                }

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
            progressionManager = viewModel.progressionManager,
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
                onToggleSpeed = { viewModel.toggleSpeed() },
                onSetSpeed = { speed -> viewModel.setGameSpeed(speed) }
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
                    countdownSeconds = ceil(gameState.preparationCountdown).toInt().coerceAtLeast(1),
                    isBossWave = gameState.isBossWave,
                    upcomingBossName = gameState.upcomingBossName
                )
            }
        }

        // 4. Boss Health Banner (when an active boss is engaged on the battlefield)
        val activeBoss = gameState.activeBoss
        if (activeBoss != null && activeBoss.isAlive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
            ) {
                ActiveBossHealthBanner(boss = activeBoss)
            }
        } else if (gameState.isBossWave && gameState.gameStatus == GameStatus.PLAYING && gameState.enemiesRemaining > 0 && gameState.placementNotice == null) {
            // Early Boss Wave Alert before the boss unit emerges
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 46.dp)
            ) {
                BossIncomingAlertBanner(bossName = gameState.upcomingBossName ?: "BOSS")
            }
        }

        // 5. Placement Notice / Wave Transition Alert Banner
        if (gameState.placementNotice != null && gameState.gameStatus != GameStatus.PREPARATION && activeBoss == null) {
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

        // 6. Bottom Tactical Tower Selection Deck & Weapon Preview (visible when game is running/prep)
        if (gameState.gameStatus == GameStatus.PLAYING || gameState.gameStatus == GameStatus.PREPARATION) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tower Preview Panel (active when placing a tower)
                if (gameState.isBuildingTower && gameState.selectedTowerSpec != null) {
                    val spec = gameState.selectedTowerSpec!!
                    TowerPreviewPanel(
                        type = spec.type,
                        cost = spec.cost,
                        playerCoins = gameState.coins,
                        onCancel = { viewModel.cancelTowerBuild() }
                    )
                }

                // Premium Mobile Strategy Weapon Shop Deck
                TowerShopDeck(
                    playerCoins = gameState.coins,
                    selectedType = if (gameState.isBuildingTower) gameState.selectedTowerSpec?.type else null,
                    progressionManager = viewModel.progressionManager,
                    onSelectTower = { type ->
                        if (gameState.isBuildingTower && gameState.selectedTowerSpec?.type == type) {
                            viewModel.cancelTowerBuild()
                        } else {
                            viewModel.selectTowerToBuild(type)
                        }
                    }
                )
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
                onMainMenu = onNavigateToMapSelect,
                onUpgradeTowers = onNavigateToResearch
            )
        }

        // Victory Modal Dialog
        if (gameState.gameStatus == GameStatus.VICTORY) {
            val nextMap = viewModel.getNextMap()
            VictoryDialog(
                gameState = gameState,
                hasNextLevel = nextMap != null,
                progressionReward = viewModel.lastVictoryReward,
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
 * Displays "TACTICAL DEPLOYMENT PHASE" countdown at the beginning of each mission.
 */
@Composable
private fun PreparationCountdownBanner(
    countdownSeconds: Int,
    isBossWave: Boolean = false,
    upcomingBossName: String? = null
) {
    val borderColor = if (isBossWave) Color(0xFFF43F5E) else Color(0xFF38BDF8)
    val titleColor = if (isBossWave) Color(0xFFF43F5E) else Color(0xFF38BDF8)
    val numberColor = if (isBossWave) Color(0xFFFF4D4D) else Color(0xFFFFD166)

    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    if (isBossWave) listOf(Color(0xF02A0812), Color(0xF0120307))
                    else listOf(Color(0xF00F172A), Color(0xF0070B14))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .border(
                0.5.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("preparation_countdown_banner")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isBossWave) "⚠️ BOSS WAVE INCOMING!" else "PREPARATION PHASE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$countdownSeconds",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = numberColor
            )
            Text(
                text = if (isBossWave) "TARGET: ${(upcomingBossName ?: "APEX BOSS").uppercase()} • FORTIFY CHOKEPOINTS"
                       else "DEPLOY DEFENSE TURRETS ON EMPTY GROUND",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBossWave) Color(0xFFFDA4AF) else Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ActiveBossHealthBanner(
    boss: com.example.entities.Enemy,
    modifier: Modifier = Modifier
) {
    val hpPercent = (boss.currentHp.toFloat() / boss.maxHp.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xF0240810), Color(0xF0120208))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, Color(0xFFF43F5E), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("active_boss_health_banner")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(min = 220.dp, max = 340.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Boss Threat",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = boss.spec.name.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "${boss.currentHp} / ${boss.maxHp} HP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDA4AF)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Health Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFF3B0B14), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = hpPercent)
                        .height(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFF43F5E), Color(0xFFFB7185))
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun BossIncomingAlertBanner(
    bossName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF02A0812),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E)),
        modifier = modifier.testTag("boss_incoming_alert")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Boss Threat",
                tint = Color(0xFFF43F5E),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "BOSS WAVE: ${bossName.uppercase()}",
                color = Color(0xFFFFD166),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun GameHudBar(
    gameState: GameState,
    onPauseClick: () -> Unit,
    onToggleSpeed: () -> Unit,
    onSetSpeed: (Float) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFA0B132B), Color(0xF2070D1A))
                )
            )
            .border(
                width = 1.dp,
                color = Color(0x3838BDF8),
                shape = RectangleShape
            )
            .testTag("game_hud_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ==========================================
            // Left Group: Base Health & Gold Coins
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Base Fortress Integrity Capsule
                val isHpLow = gameState.base.currentHp <= 5
                val hpRatio = (gameState.base.currentHp.toFloat() / gameState.base.maxHp.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (isHpLow) Color(0xFFEF4444) else Color(0xFF10B981),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Base HP",
                            tint = if (isHpLow) Color(0xFFEF4444) else Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Mini tactical health bar
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .background(Color(0xFF334155), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(hpRatio)
                                    .height(5.dp)
                                    .background(
                                        if (isHpLow) Color(0xFFEF4444) else Color(0xFF10B981),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${gameState.base.currentHp}/${gameState.base.maxHp}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isHpLow) Color(0xFFF87171) else Color(0xFF6EE7B7)
                        )
                    }
                }

                // 2. Gold Coins Capsule
                Box(
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEAB308), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Gold Coins",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${gameState.coins}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE68A),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // ==========================================
            // Center Group: Wave Telemetry & Foes
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Wave Capsule Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (gameState.isBossWave) Color(0xE62A0812) else Color(0xE60F172A),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (gameState.isBossWave) Color(0xFFF43F5E) else Color(0x6638BDF8),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (gameState.isBossWave) "BOSS" else "WAVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBossWave) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${gameState.currentWave}/${gameState.maxWaves}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gameState.isBossWave) Color(0xFFFF4D4D) else Color(0xFF38BDF8)
                        )
                        if (gameState.isBossWave) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF43F5E), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SKULL",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Foes / Enemy Count Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0x66F43F5E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CrisisAlert,
                            contentDescription = "Foes",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${gameState.enemiesRemaining}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDA4AF)
                        )
                    }
                }
            }

            // ==========================================
            // Right Group: Speed Control (1x/2x/3x) & Pause
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Game-Style Segmented Speed Control (1x, 2x, 3x)
                Row(
                    modifier = Modifier
                        .background(Color(0xE60B1220), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                        .testTag("speed_toggle_button"),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSpeed = gameState.gameSpeedMultiplier
                    listOf(1f, 2f, 3f).forEach { speed ->
                        val isSelected = (currentSpeed == speed)
                        val targetBg = if (isSelected) Color(0xFF0284C7) else Color.Transparent
                        val targetBorder = if (isSelected) Color(0xFF38BDF8) else Color.Transparent
                        val targetText = if (isSelected) Color.White else Color(0xFF94A3B8)

                        val animatedBg by animateColorAsState(targetValue = targetBg, label = "speed_bg")
                        val animatedBorder by animateColorAsState(targetValue = targetBorder, label = "speed_border")
                        val animatedText by animateColorAsState(targetValue = targetText, label = "speed_text")

                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .widthIn(min = 28.dp)
                                .background(animatedBg, RoundedCornerShape(6.dp))
                                .border(1.dp, animatedBorder, RoundedCornerShape(6.dp))
                                .clickable { onSetSpeed(speed) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed.toInt()}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = animatedText
                            )
                        }
                    }
                }

                // Tactical Pause Button with game styling and press feedback
                val pauseInteractionSource = remember { MutableInteractionSource() }
                val isPausePressed by pauseInteractionSource.collectIsPressedAsState()
                val pauseScale by animateFloatAsState(
                    targetValue = if (isPausePressed) 0.92f else 1f,
                    animationSpec = tween(50),
                    label = "pause_scale"
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(pauseScale)
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = pauseInteractionSource,
                            indication = null,
                            onClick = onPauseClick
                        )
                        .testTag("pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

