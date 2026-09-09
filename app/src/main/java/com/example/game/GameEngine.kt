package com.example.game

import com.example.audio.AndroidAudioPlayer
import com.example.audio.AudioPlayer
import com.example.audio.GameSound
import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.entities.Base
import com.example.entities.Enemy
import com.example.entities.Point2D
import com.example.entities.Projectile
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerSpec
import com.example.entities.TowerType
import com.example.entities.VisualEffect
import com.example.systems.CombatSystem
import com.example.systems.EconomySystem
import com.example.systems.WaveManager
import com.example.systems.WaveStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.min

/**
 * Core game engine coordinating maps, waves, combat, tower placement, upgrades, and audio.
 */
class GameEngine(
    val audioPlayer: AudioPlayer = AndroidAudioPlayer()
) {
    private var currentMap: GameMap = GameMap.createGreenValleyMap()
    private var waveManager = WaveManager(maxWaves = GameConfig.TOTAL_WAVES, paths = currentMap.paths)
    private val combatSystem = CombatSystem()
    private val economySystem = EconomySystem()

    private var base = Base(position = currentMap.basePosition)
    private val towers = mutableListOf<Tower>()
    private val enemies = mutableListOf<Enemy>()
    private val projectiles = mutableListOf<Projectile>()
    private val visualEffects = mutableListOf<VisualEffect>()

    private var gameStatus = GameStatus.PREPARATION
    private var preparationCountdown = 5.0f
    private var stateBeforePause: GameStatus = GameStatus.PREPARATION
    private var selectedBuildPos: Point2D? = null
    private var isBuildingTower = false
    private var selectedTowerSpec: TowerSpec? = null
    private var previewPlacementPos: Point2D? = null
    private var isValidPlacement = false
    private var selectedExistingTower: Tower? = null
    private var placementNotice: String? = null
    private var placementNoticeTimer = 0f
    private var waveTransitionTimer = 0f
    private var enemiesKilledTotal = 0
    private var gameSpeedMultiplier = 1.0f
    private var gameTime = 0f
    private var lastWaveRewarded = 0

    init {
        // Wave starts after preparation countdown reaches 0
    }

    private val _gameState = MutableStateFlow(buildState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    fun changeGameState(newStatus: GameStatus) {
        if (gameStatus == newStatus) return
        gameStatus = newStatus
        publishState()
    }

    @Synchronized
    fun update(dt: Float) {
        if (gameStatus == GameStatus.PAUSED || gameStatus == GameStatus.GAME_OVER || gameStatus == GameStatus.VICTORY) {
            return
        }

        val clampedDt = min(dt, 0.05f) * gameSpeedMultiplier
        gameTime += clampedDt

        // 1. Game Over Check
        if (base.isDestroyed) {
            changeGameState(GameStatus.GAME_OVER)
            audioPlayer.playSound(GameSound.GAME_OVER)
            return
        }

        // 2. PREPARATION Phase (Exactly 5 seconds countdown before wave starts)
        if (gameStatus == GameStatus.PREPARATION) {
            preparationCountdown -= clampedDt
            // Advance visual effects if any
            val updatedEffects = visualEffects
                .map { it.advance(clampedDt) }
                .filter { !it.isFinished }
            visualEffects.clear()
            visualEffects.addAll(updatedEffects)

            if (preparationCountdown <= 0f) {
                preparationCountdown = 0f
                gameStatus = GameStatus.PLAYING
                waveManager.startCurrentWave()
                audioPlayer.playSound(GameSound.WAVE_START)
            }
            publishState()
            return
        }

        // 3. Handle WAVE_COMPLETE state (automatic delay between waves, no pause)
        if (gameStatus == GameStatus.WAVE_COMPLETE) {
            waveTransitionTimer -= clampedDt
            val nextWaveNum = waveManager.currentWave + 1
            if (waveTransitionTimer > 1.2f) {
                placementNotice = "WAVE ${waveManager.currentWave} COMPLETE! +${GameConfig.WAVE_CLEAR_BONUS_COINS}🪙"
            } else {
                placementNotice = "WAVE $nextWaveNum INCOMING..."
            }

            // Keep visual effects updating smoothly
            val updatedEffects = visualEffects
                .map { it.advance(clampedDt) }
                .filter { !it.isFinished }
            visualEffects.clear()
            visualEffects.addAll(updatedEffects)

            if (waveTransitionTimer <= 0f) {
                val advanced = waveManager.advanceToNextWave()
                if (advanced) {
                    gameStatus = GameStatus.PLAYING
                    placementNotice = null
                    audioPlayer.playSound(GameSound.WAVE_START)
                } else {
                    gameStatus = GameStatus.VICTORY
                    audioPlayer.playSound(GameSound.VICTORY)
                }
            }
            publishState()
            return
        }

        // Update notice banner timer for other notices (e.g. boss warning)
        if (placementNoticeTimer > 0f) {
            placementNoticeTimer -= dt
            if (placementNoticeTimer <= 0f && gameStatus != GameStatus.WAVE_COMPLETE) {
                placementNotice = null
            }
        }

        // 4. Victory Check
        if (waveManager.status == WaveStatus.ALL_WAVES_CLEARED && enemies.isEmpty()) {
            changeGameState(GameStatus.VICTORY)
            audioPlayer.playSound(GameSound.VICTORY)
            return
        }

        // 5. Wave completion check -> Enter WAVE_COMPLETE automatically!
        if ((waveManager.status == WaveStatus.WAVE_CLEARED ||
                (waveManager.currentQueueIndex >= waveManager.totalEnemiesThisWave && waveManager.totalEnemiesThisWave > 0)) &&
            enemies.isEmpty()
        ) {
            if (waveManager.currentWave >= waveManager.maxWaves) {
                changeGameState(GameStatus.VICTORY)
                audioPlayer.playSound(GameSound.VICTORY)
                return
            }
            if (waveManager.currentWave > lastWaveRewarded) {
                lastWaveRewarded = waveManager.currentWave
                economySystem.addCoins(GameConfig.WAVE_CLEAR_BONUS_COINS)
            }
            gameStatus = GameStatus.WAVE_COMPLETE
            waveTransitionTimer = 2.5f
            placementNotice = "WAVE ${waveManager.currentWave} COMPLETE! +${GameConfig.WAVE_CLEAR_BONUS_COINS}🪙"
            audioPlayer.playSound(GameSound.WAVE_CLEAR)
            publishState()
            return
        }

        // 6. Wave progression: spawn enemy if ready
        val prevEnemyCount = enemies.size
        val newEnemy = waveManager.update(clampedDt, prevEnemyCount, autoStartNextWave = false)
        if (newEnemy != null) {
            enemies.add(newEnemy)
            if (newEnemy.spec.isBoss) {
                audioPlayer.playSound(GameSound.BOSS_APPEARANCE)
                showNotice("WARNING: ${newEnemy.spec.name} has arrived!")
            }
        }

        // 7. Advance enemies along path
        val activeEnemies = mutableListOf<Enemy>()
        for (enemy in enemies) {
            val assignedPath = currentMap.getPath(enemy.pathIndex)
            val advanced = enemy.advance(clampedDt, assignedPath)
            val inTunnel = currentMap.isPointInTunnel(advanced.position)
            val updated = if (advanced.isInTunnel != inTunnel) advanced.copy(isInTunnel = inTunnel) else advanced

            if (updated.reachedBase) {
                base = base.takeDamage(updated.spec.baseDamage)
                audioPlayer.playSound(GameSound.ENEMY_HIT)
                if (base.isDestroyed) {
                    changeGameState(GameStatus.GAME_OVER)
                    audioPlayer.playSound(GameSound.GAME_OVER)
                    break
                }
            } else if (updated.isAlive) {
                activeEnemies.add(updated)
            }
        }
        enemies.clear()
        enemies.addAll(activeEnemies)

        // 8. Combat Update
        val combatResult = combatSystem.update(
            clampedDt,
            towers,
            enemies,
            projectiles,
            visualEffects
        )

        towers.clear()
        towers.addAll(combatResult.updatedTowers)

        enemies.clear()
        enemies.addAll(combatResult.updatedEnemies.filter { it.isAlive && !it.reachedBase })

        projectiles.clear()
        projectiles.addAll(combatResult.updatedProjectiles)

        visualEffects.clear()
        visualEffects.addAll(combatResult.updatedEffects)

        if (combatResult.newProjectilesFired > 0) {
            audioPlayer.playSound(GameSound.TOWER_FIRE_BULLET)
        }

        if (combatResult.coinsEarned > 0) {
            economySystem.addCoins(combatResult.coinsEarned)
        }
        if (combatResult.enemiesKilled > 0) {
            enemiesKilledTotal += combatResult.enemiesKilled
            audioPlayer.playSound(GameSound.ENEMY_DEATH)
        }

        if (combatResult.bossDefeated) {
            showNotice("BOSS DEFEATED! Massive Coin Reward!")
        }

        // Instant check if last enemy was eliminated during this combat tick
        if (waveManager.currentQueueIndex >= waveManager.totalEnemiesThisWave &&
            waveManager.totalEnemiesThisWave > 0 &&
            enemies.isEmpty()
        ) {
            if (waveManager.currentWave >= waveManager.maxWaves) {
                changeGameState(GameStatus.VICTORY)
                audioPlayer.playSound(GameSound.VICTORY)
                return
            }
            if (waveManager.currentWave > lastWaveRewarded) {
                lastWaveRewarded = waveManager.currentWave
                economySystem.addCoins(GameConfig.WAVE_CLEAR_BONUS_COINS)
            }
            gameStatus = GameStatus.WAVE_COMPLETE
            waveTransitionTimer = 2.5f
            placementNotice = "WAVE ${waveManager.currentWave} COMPLETE! +${GameConfig.WAVE_CLEAR_BONUS_COINS}🪙"
            audioPlayer.playSound(GameSound.WAVE_CLEAR)
            publishState()
            return
        }

        // Keep selected tower reference updated with latest stats
        if (selectedExistingTower != null) {
            selectedExistingTower = towers.find { it.id == selectedExistingTower?.id }
        }

        publishState()
    }

    fun startWave() {
        if (gameStatus == GameStatus.PAUSED) {
            gameStatus = GameStatus.PLAYING
            publishState()
        } else if (gameStatus == GameStatus.WAVE_TRANSITION) {
            waveManager.advanceToNextWave()
            gameStatus = GameStatus.PLAYING
            waveTransitionTimer = 0f
            placementNotice = null
            audioPlayer.playSound(GameSound.WAVE_START)
            publishState()
        } else if (waveManager.status == WaveStatus.READY_TO_START) {
            waveManager.startCurrentWave()
            gameStatus = GameStatus.PLAYING
            audioPlayer.playSound(GameSound.WAVE_START)
            publishState()
        } else if (waveManager.status == WaveStatus.WAVE_CLEARED) {
            if (waveManager.currentWave < waveManager.maxWaves) {
                waveManager.advanceToNextWave()
                gameStatus = GameStatus.PLAYING
                audioPlayer.playSound(GameSound.WAVE_START)
                publishState()
            }
        }
    }

    fun selectTowerToBuild(type: TowerType) {
        val spec = TowerSpec.create(type, 1)
        if (!economySystem.canAfford(spec.cost)) {
            showNotice("Need ${spec.cost} coins for ${spec.name}!")
            return
        }
        isBuildingTower = true
        selectedTowerSpec = spec
        selectedExistingTower = null
        previewPlacementPos = null
        isValidPlacement = false
        publishState()
    }

    fun cancelTowerBuild() {
        isBuildingTower = false
        selectedTowerSpec = null
        previewPlacementPos = null
        publishState()
    }

    fun updatePlacementPreview(virtualX: Float, virtualY: Float) {
        if (!isBuildingTower) return
        val pos = Point2D(virtualX, virtualY)
        previewPlacementPos = pos
        val (valid, _) = validatePlacement(virtualX, virtualY)
        isValidPlacement = valid
        publishState()
    }

    fun selectExistingTower(tower: Tower?) {
        selectedExistingTower = tower
        isBuildingTower = false
        selectedTowerSpec = null
        publishState()
    }

    fun upgradeSelectedTower(): Boolean {
        val tower = selectedExistingTower ?: return false
        if (tower.isMaxLevel) {
            showNotice("Tower is already at MAX level!")
            return false
        }
        val cost = tower.spec.upgradeCost
        if (!economySystem.canAfford(cost)) {
            showNotice("Need $cost coins to upgrade!")
            return false
        }

        economySystem.spend(cost)
        val upgraded = tower.upgrade() ?: return false

        val idx = towers.indexOfFirst { it.id == tower.id }
        if (idx != -1) {
            towers[idx] = upgraded
            selectedExistingTower = upgraded
            showNotice("${upgraded.spec.name} upgraded!")
            audioPlayer.playSound(GameSound.TOWER_FIRE_CANNON)
            publishState()
            return true
        }
        return false
    }

    fun sellSelectedTower(): Boolean {
        val tower = selectedExistingTower ?: return false
        val refund = tower.sellRefundCoins
        economySystem.addCoins(refund)
        towers.removeAll { it.id == tower.id }
        selectedExistingTower = null
        showNotice("Tower sold for +$refund coins!")
        audioPlayer.playSound(GameSound.ENEMY_DEATH)
        publishState()
        return true
    }

    fun setStrategyForSelectedTower(strategy: TargetingStrategy) {
        val tower = selectedExistingTower ?: return
        val updated = tower.setStrategy(strategy)
        val idx = towers.indexOfFirst { it.id == tower.id }
        if (idx != -1) {
            towers[idx] = updated
            selectedExistingTower = updated
            publishState()
        }
    }

    fun isBuildableLocation(point: Point2D): Boolean {
        val radius = GameConfig.TOWER_SIZE / 2f
        if (!currentMap.canPlaceAt(point, radius)) return false
        val tooCloseToOther = towers.any {
            it.position.distanceTo(point) < GameConfig.MIN_DISTANCE_BETWEEN_TOWERS
        }
        return !tooCloseToOther
    }

    @Synchronized
    fun selectBuildPosition(point: Point2D?) {
        selectedExistingTower = null
        selectedBuildPos = point
        publishState()
    }

    @Synchronized
    fun placeTowerAt(type: TowerType, virtualX: Float, virtualY: Float): Boolean {
        val spec = TowerSpec.create(type, 1)
        val targetPoint = Point2D(virtualX, virtualY)

        if (!isBuildableLocation(targetPoint)) {
            showNotice("Cannot build here! Check paths, water, or obstacles.")
            return false
        }

        if (!economySystem.spend(spec.cost)) {
            showNotice("Not enough coins! Need ${spec.cost}🪙")
            return false
        }

        val newTower = Tower(
            spec = spec,
            position = targetPoint,
            totalCoinsInvested = spec.cost
        )
        towers.add(newTower)
        selectedBuildPos = null
        selectedTowerSpec = null
        isBuildingTower = false
        showNotice("${spec.name} deployed!")
        audioPlayer.playSound(GameSound.TOWER_FIRE_CANNON)
        publishState()
        return true
    }

    fun validatePlacement(virtualX: Float, virtualY: Float): Pair<Boolean, String?> {
        val spec = selectedTowerSpec ?: TowerSpec.create(TowerType.MACHINE_GUN, 1)
        val targetPoint = Point2D(virtualX, virtualY)

        if (!currentMap.canPlaceAt(targetPoint, spec.size / 2f)) {
            return false to "Cannot build here! Check road, water, or base."
        }

        val tooCloseToOther = towers.any {
            it.position.distanceTo(targetPoint) < GameConfig.MIN_DISTANCE_BETWEEN_TOWERS
        }
        if (tooCloseToOther) {
            return false to "Too close to another tower!"
        }

        if (!economySystem.canAfford(spec.cost)) {
            return false to "Not enough coins! Need ${spec.cost}"
        }

        return true to null
    }

    fun tryPlaceTower(virtualX: Float, virtualY: Float): Boolean {
        val (isValid, reason) = validatePlacement(virtualX, virtualY)
        if (!isValid) {
            if (reason != null) showNotice(reason)
            return false
        }

        val spec = selectedTowerSpec ?: TowerSpec.create(TowerType.MACHINE_GUN, 1)
        val targetPoint = Point2D(virtualX, virtualY)

        if (!economySystem.spend(spec.cost)) {
            showNotice("Not enough coins! Need ${spec.cost}")
            return false
        }

        val newTower = Tower(
            spec = spec,
            position = targetPoint,
            totalCoinsInvested = spec.cost
        )
        towers.add(newTower)
        isBuildingTower = false
        selectedTowerSpec = null
        previewPlacementPos = null
        showNotice("${spec.name} deployed!")
        audioPlayer.playSound(GameSound.TOWER_FIRE_CANNON)
        publishState()
        return true
    }

    fun loadMap(map: GameMap) {
        currentMap = map
        restart(isNewMission = true)
    }

    @Synchronized
    fun pause() {
        if (gameStatus != GameStatus.PAUSED && gameStatus != GameStatus.GAME_OVER && gameStatus != GameStatus.VICTORY) {
            stateBeforePause = gameStatus
            changeGameState(GameStatus.PAUSED)
        }
    }

    @Synchronized
    fun resume() {
        if (gameStatus == GameStatus.PAUSED) {
            changeGameState(stateBeforePause)
        }
    }

    fun toggleSpeed() {
        gameSpeedMultiplier = when (gameSpeedMultiplier) {
            1.0f -> 2.0f
            2.0f -> 3.0f
            else -> 1.0f
        }
        publishState()
    }

    @Synchronized
    fun restart(isNewMission: Boolean = true) {
        base = Base(position = currentMap.basePosition)
        towers.clear()
        enemies.clear()
        projectiles.clear()
        visualEffects.clear()
        waveManager = WaveManager(maxWaves = GameConfig.TOTAL_WAVES, paths = currentMap.paths)
        economySystem.reset()
        if (isNewMission) {
            gameStatus = GameStatus.PREPARATION
            preparationCountdown = 5.0f
            stateBeforePause = GameStatus.PREPARATION
        } else {
            gameStatus = GameStatus.PLAYING
            preparationCountdown = 0f
            waveManager.startCurrentWave()
            stateBeforePause = GameStatus.PLAYING
        }
        selectedBuildPos = null
        isBuildingTower = false
        selectedTowerSpec = null
        previewPlacementPos = null
        isValidPlacement = false
        selectedExistingTower = null
        placementNotice = null
        placementNoticeTimer = 0f
        waveTransitionTimer = 0f
        enemiesKilledTotal = 0
        gameSpeedMultiplier = 1.0f
        gameTime = 0f
        lastWaveRewarded = 0
        audioPlayer.playSound(GameSound.WAVE_START)
        publishState()
    }

    private fun showNotice(message: String) {
        placementNotice = message
        placementNoticeTimer = 2.0f
        publishState()
    }

    private fun publishState() {
        _gameState.value = buildState()
    }

    private fun buildState(): GameState {
        val baseHpPercent = base.healthPercentage
        val stars = when {
            baseHpPercent >= 0.8f -> 3
            baseHpPercent >= 0.4f -> 2
            baseHpPercent > 0f -> 1
            else -> 0
        }
        val score = (economySystem.totalCoinsEarned * 10) + (enemiesKilledTotal * 25) + (stars * 500)
        val activeBoss = enemies.find { it.spec.isBoss && it.isAlive }

        return GameState(
            gameStatus = gameStatus,
            preparationCountdown = preparationCountdown,
            selectedBuildPos = selectedBuildPos,
            currentMap = currentMap,
            base = base,
            coins = economySystem.currentCoins,
            totalCoinsEarned = economySystem.totalCoinsEarned,
            currentWave = waveManager.currentWave,
            maxWaves = waveManager.maxWaves,
            waveStatus = waveManager.status,
            enemiesRemaining = enemies.size,
            nextWaveCountdown = waveManager.nextWaveCountdown,
            towers = towers.toList(),
            enemies = enemies.toList(),
            projectiles = projectiles.toList(),
            effects = visualEffects.toList(),
            isBuildingTower = isBuildingTower,
            selectedTowerSpec = selectedTowerSpec,
            previewPlacementPos = previewPlacementPos,
            isValidPlacement = isValidPlacement,
            selectedExistingTower = selectedExistingTower,
            placementNotice = placementNotice,
            enemiesKilledTotal = enemiesKilledTotal,
            gameSpeedMultiplier = gameSpeedMultiplier,
            activeBoss = activeBoss,
            starsEarned = stars,
            finalScore = score,
            gameTime = gameTime
        )
    }
}
