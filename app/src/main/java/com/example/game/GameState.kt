package com.example.game

import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.entities.Base
import com.example.entities.DestructibleObject
import com.example.entities.Enemy
import com.example.entities.Point2D
import com.example.entities.Projectile
import com.example.entities.Tower
import com.example.entities.TowerSpec
import com.example.entities.VisualEffect
import com.example.systems.WaveStatus

enum class GameStatus {
    MENU,
    PREPARATION,
    PLAYING,
    PAUSED,
    WAVE_COMPLETE,
    GAME_OVER,
    VICTORY;

    companion object {
        // Compatibility alias for WAVE_TRANSITION
        val WAVE_TRANSITION = WAVE_COMPLETE
    }
}

/**
 * Immutable snapshot of the entire game world state.
 */
data class GameState(
    val gameStatus: GameStatus = GameStatus.PREPARATION,
    val preparationCountdown: Float = 5.0f,
    val selectedBuildPos: Point2D? = null,
    val currentMap: GameMap = GameMap.createGreenValleyMap(),
    val base: Base = Base(position = currentMap.basePosition),
    val coins: Int = GameConfig.STARTING_COINS,
    val totalCoinsEarned: Int = GameConfig.STARTING_COINS,
    val currentWave: Int = 1,
    val maxWaves: Int = GameConfig.TOTAL_WAVES,
    val wavesCleared: Int = 0,
    val enemyHpMultiplier: Float = 1.0f,
    val enemySpeedMultiplier: Float = 1.0f,
    val waveStatus: WaveStatus = WaveStatus.READY_TO_START,
    val enemiesRemaining: Int = 0,
    val nextWaveCountdown: Float = 0f,
    val towers: List<Tower> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val destructibles: List<DestructibleObject> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val effects: List<VisualEffect> = emptyList(),
    val isBuildingTower: Boolean = false,
    val selectedTowerSpec: TowerSpec? = null,
    val previewPlacementPos: Point2D? = null,
    val isValidPlacement: Boolean = false,
    val selectedExistingTower: Tower? = null,
    val selectedDestructibleId: String? = null,
    val placementNotice: String? = null,
    val enemiesKilledTotal: Int = 0,
    val gameSpeedMultiplier: Float = 1.0f,
    val activeBoss: Enemy? = null,
    val isBossWave: Boolean = false,
    val upcomingBossName: String? = null,
    val starsEarned: Int = 0,
    val finalScore: Int = 0,
    val gameTime: Float = 0f,
    val tutorialRecommendedPlot: Point2D? = null,
    val screenShakeIntensity: Float = 0f
) {
    val selectedDestructible: DestructibleObject?
        get() = destructibles.firstOrNull { it.id == selectedDestructibleId && it.isAlive }

    val baseHp: Int get() = base.currentHp
    val maxBaseHp: Int get() = base.maxHp
}
