package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
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
import com.example.data.GameConfig
import com.example.entities.Point2D
import com.example.entities.TowerType
import com.example.game.GameStatus
import com.example.game.GameState
import com.example.systems.WaveStatus

/**
 * Main gameplay screen containing the top HUD with Boss Bar, 2D Canvas battlefield,
 * placement notification banners, tower selector, and upgrade overlay.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToMainMenu: () -> Unit,
    onNavigateToMapSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()

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
            // 1. Top HUD Bar
            GameHudBar(
                gameState = gameState,
                onPauseClick = { viewModel.pause() },
                onToggleSpeed = { viewModel.toggleSpeed() }
            )

            // Boss Health Bar Banner (Visible when boss is active)
            if (gameState.activeBoss != null) {
                BossHealthBanner(gameState.activeBoss!!)
            }

            // 2. Battlefield Canvas Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                GameCanvas(
                    gameState = gameState,
                    onCanvasTap = { vx, vy ->
                        if (gameState.isBuildingTower) {
                            viewModel.tryPlaceTower(vx, vy)
                        } else {
                            // Check if tapped on existing tower
                            val tappedTower = gameState.towers.firstOrNull {
                                it.position.distanceTo(Point2D(vx, vy)) <= it.spec.size * 0.9f
                            }
                            viewModel.selectExistingTower(tappedTower)
                        }
                    },
                    onPlacementDrag = { vx, vy ->
                        viewModel.updatePlacementPreview(vx, vy)
                    }
                )

                // Placement Notice Alert Banner
                if (gameState.placementNotice != null) {
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

            // 3. Selected Tower Upgrade & Control Overlay
            if (gameState.selectedExistingTower != null) {
                TowerUpgradePanel(
                    tower = gameState.selectedExistingTower!!,
                    playerCoins = gameState.coins,
                    onUpgrade = { viewModel.upgradeSelectedTower() },
                    onSell = { viewModel.sellSelectedTower() },
                    onStrategyChange = { strategy -> viewModel.setStrategyForSelectedTower(strategy) },
                    onClose = { viewModel.selectExistingTower(null) }
                )
            } else {
                // 4. Bottom Tactical Controls Panel (Towers store & Wave dispatch)
                GameControlPanel(
                    gameState = gameState,
                    onSelectTower = { type -> viewModel.selectTowerToBuild(type) },
                    onCancelBuild = { viewModel.cancelTowerBuild() },
                    onStartWave = { viewModel.startWave() }
                )
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

@Composable
private fun BossHealthBanner(boss: com.example.entities.Enemy) {
    Surface(
        color = Color(0xDD831843),
        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Boss Alert",
                tint = Color(0xFFFDE047),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "BOSS: ${boss.spec.name.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${boss.currentHp.toInt()} / ${boss.maxHp.toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { boss.healthPercentage },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFFE11D48),
                    trackColor = Color(0xFF4C0519)
                )
            }
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
                // Coins Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.coins}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Wave Counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WAVE ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${gameState.currentWave}/${gameState.maxWaves}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                // Enemies Remaining
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ENEMIES: ",
                        fontSize = 12.sp,
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

            // Base Health Progress Indicator Bar
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Base HP",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BASE: ${gameState.base.currentHp}/${gameState.base.maxHp}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = { gameState.base.healthPercentage },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp),
                    color = when {
                        gameState.base.healthPercentage > 0.5f -> Color(0xFF10B981)
                        gameState.base.healthPercentage > 0.25f -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    trackColor = Color(0xFF334155)
                )
            }
        }
    }
}

@Composable
private fun GameControlPanel(
    gameState: GameState,
    onSelectTower: (TowerType) -> Unit,
    onCancelBuild: () -> Unit,
    onStartWave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (gameState.isBuildingTower) {
                // Active building indicator banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3338BDF8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Text(
                                text = "Placing ${gameState.selectedTowerSpec?.name} • Tap grass to build",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancelBuild,
                        modifier = Modifier.size(32.dp).testTag("cancel_build_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Placement",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Tower Selection Strip & Wave Dispatch Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3 Tower Cards
                TowerChoiceButton(
                    type = TowerType.MACHINE_GUN,
                    name = "Gunner",
                    cost = GameConfig.MG_TOWER_COST,
                    playerCoins = gameState.coins,
                    isSelected = gameState.isBuildingTower && gameState.selectedTowerSpec?.type == TowerType.MACHINE_GUN,
                    onClick = { onSelectTower(TowerType.MACHINE_GUN) },
                    modifier = Modifier.weight(1f)
                )

                TowerChoiceButton(
                    type = TowerType.CANNON,
                    name = "Cannon",
                    cost = GameConfig.CANNON_TOWER_COST,
                    playerCoins = gameState.coins,
                    isSelected = gameState.isBuildingTower && gameState.selectedTowerSpec?.type == TowerType.CANNON,
                    onClick = { onSelectTower(TowerType.CANNON) },
                    modifier = Modifier.weight(1f)
                )

                TowerChoiceButton(
                    type = TowerType.RAPID_FIRE,
                    name = "Blaster",
                    cost = GameConfig.RAPID_TOWER_COST,
                    playerCoins = gameState.coins,
                    isSelected = gameState.isBuildingTower && gameState.selectedTowerSpec?.type == TowerType.RAPID_FIRE,
                    onClick = { onSelectTower(TowerType.RAPID_FIRE) },
                    modifier = Modifier.weight(1f)
                )

                // Wave Action Button
                WaveActionButton(
                    gameState = gameState,
                    onStartWave = onStartWave,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
private fun TowerChoiceButton(
    type: TowerType,
    name: String,
    cost: Int,
    playerCoins: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = playerCoins >= cost
    val borderCol = if (isSelected) Color(0xFF38BDF8) else Color.Transparent
    val bgCol = if (canAfford) Color(0xFF334155) else Color(0xFF1E293B)

    Button(
        onClick = onClick,
        enabled = canAfford,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgCol,
            disabledContainerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF64748B)
        ),
        modifier = modifier
            .height(56.dp)
            .border(2.dp, borderCol, RoundedCornerShape(10.dp))
            .testTag("build_${type.name.lowercase()}_button")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$cost 🪙",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                color = if (canAfford) Color(0xFFFFD166) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun WaveActionButton(
    gameState: GameState,
    onStartWave: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (gameState.gameStatus) {
        GameStatus.WAVE_COMPLETE, GameStatus.MENU -> {
            val label = if (gameState.currentWave == 1 && gameState.waveStatus == WaveStatus.READY_TO_START) {
                "START WAVE 1"
            } else {
                "NEW WAVE"
            }
            Button(
                onClick = onStartWave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = modifier.height(56.dp).testTag("start_wave_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = label,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        GameStatus.PLAYING -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f)),
                modifier = modifier.height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFF43F5E), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FIGHTING",
                        color = Color(0xFFF43F5E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        GameStatus.PAUSED -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0F172A),
                modifier = modifier.height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PAUSED",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        GameStatus.GAME_OVER -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF450A0A),
                modifier = modifier.height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("DEFEAT", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        GameStatus.VICTORY -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF064E3B),
                modifier = modifier.height(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("VICTORY", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
