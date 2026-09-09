package com.example.game

import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.entities.Base
import com.example.entities.Enemy
import com.example.entities.Point2D
import com.example.entities.Projectile
import com.example.entities.Tower
import com.example.entities.TowerSpec
import com.example.entities.VisualEffect
import com.example.systems.WaveStatus

enum class GameStatus {
    MENU,
    PLAYING,
    PAUSED,
    WAVE_COMPLETE,
    GAME_OVER,
    VICTORY
}

/**
 * Immutable snapshot of the entire game world state for Version 2.
 */
data class GameState(
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val currentMap: GameMap = GameMap.createGreenValleyMap(),
    val base: Base = Base(position = currentMap.basePosition),
    val coins: Int = GameConfig.STARTING_COINS,
    val totalCoinsEarned: Int = GameConfig.STARTING_COINS,
    val currentWave: Int = 1,
    val maxWaves: Int = GameConfig.TOTAL_WAVES,
    val waveStatus: WaveStatus = WaveStatus.READY_TO_START,
    val enemiesRemaining: Int = 0,
    val nextWaveCountdown: Float = 0f,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val effects: List<VisualEffect> = emptyList(),
    val isBuildingTower: Boolean = false,
    val selectedTowerSpec: TowerSpec? = null,
    val previewPlacementPos: Point2D? = null,
    val isValidPlacement: Boolean = false,
    val selectedExistingTower: Tower? = null,
    val placementNotice: String? = null,
    val enemiesKilledTotal: Int = 0,
    val gameSpeedMultiplier: Float = 1.0f,
    val activeBoss: Enemy? = null,
    val starsEarned: Int = 0,
    val finalScore: Int = 0,
    val gameTime: Float = 0f
)
