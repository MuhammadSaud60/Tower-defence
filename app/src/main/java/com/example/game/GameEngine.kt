package com.example.game

import com.example.audio.AndroidAudioPlayer
import com.example.audio.AudioPlayer
import com.example.audio.GameSound
import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.data.EnvironmentType
import com.example.entities.Base
import com.example.entities.DestructibleObject
import com.example.entities.EffectType
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
    val audioPlayer: AudioPlayer = AndroidAudioPlayer.getInstance()
) {
    internal var currentMap: GameMap = GameMap.createGreenValleyMap()
    private var waveManager = WaveManager(
        maxWaves = currentMap.totalWaves,
        paths = currentMap.paths,
        isSnowValley = currentMap.environmentType == EnvironmentType.SNOW_VALLEY,
        isNightFortress = currentMap.environmentType == EnvironmentType.NIGHT_FORTRESS,
        mapId = currentMap.id
    )
    private val combatSystem = CombatSystem()
    private val economySystem = EconomySystem()

    private var base = Base(position = currentMap.basePosition)
    internal val towers = mutableListOf<Tower>()
    private val enemies = mutableListOf<Enemy>()
    internal val destructibles = mutableListOf<DestructibleObject>().apply { addAll(currentMap.destructibles) }
    private val projectiles = mutableListOf<Projectile>()
    private val visualEffects = mutableListOf<VisualEffect>()

    init {
        audioPlayer.updateMapAmbience(currentMap.environmentType)
    }

    private var gameStatus = GameStatus.PREPARATION
    private var preparationCountdown = 5.0f
    private var stateBeforePause: GameStatus = GameStatus.PREPARATION
    private var selectedBuildPos: Point2D? = null
    private var isBuildingTower = false
    private var selectedTowerSpec: TowerSpec? = null
    private var previewPlacementPos: Point2D? = null
    private var isValidPlacement = false
    private var selectedExistingTower: Tower? = null
    private var selectedDestructibleId: String? = null
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

            if (preparationCountdown <= 0f) {
                preparationCountdown = 0f
                gameStatus = GameStatus.PLAYING
                waveManager.startCurrentWave()
                audioPlayer.playSound(GameSound.WAVE_START)
            }
            processCombatUpdate(clampedDt)
            publishState()
            return
        }

        // 3. Handle WAVE_COMPLETE state (automatic delay between waves, no pause)
        if (gameStatus == GameStatus.WAVE_COMPLETE) {
            waveTransitionTimer -= clampedDt
            val nextWaveNum = waveManager.currentWave + 1
            val nextWavesCleared = waveManager.currentWave
            val hpBonusPct = ((waveManager.scalingSystem.getHpMultiplier(nextWavesCleared) - 1f) * 100f).toInt()
            if (waveTransitionTimer > 1.2f) {
                placementNotice = "WAVE ${waveManager.currentWave} COMPLETE! +${GameConfig.WAVE_CLEAR_BONUS_COINS}🪙"
            } else {
                if (hpBonusPct > 0) {
                    placementNotice = "WAVE $nextWaveNum INCOMING (Threat +$hpBonusPct% HP)..."
                } else {
                    placementNotice = "WAVE $nextWaveNum INCOMING..."
                }
            }

            if (waveTransitionTimer <= 0f) {
                val advanced = waveManager.advanceToNextWave()
                if (advanced) {
                    gameStatus = GameStatus.PLAYING
                    placementNotice = null
                    audioPlayer.playSound(GameSound.WAVE_START)
                } else {
                    gameStatus = GameStatus.VICTORY
                    audioPlayer.playSound(GameSound.VICTORY)
                    publishState()
                    return
                }
            }
            processCombatUpdate(clampedDt)
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
                visualEffects.add(
                    VisualEffect(
                        type = EffectType.BOSS_ENTRANCE,
                        position = newEnemy.position,
                        maxLifetime = 0.85f,
                        maxRadius = 85f
                    )
                )
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
        processCombatUpdate(clampedDt)

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

    private fun processCombatUpdate(clampedDt: Float) {
        val combatResult = combatSystem.update(
            clampedDt,
            towers,
            enemies,
            destructibles,
            projectiles,
            visualEffects
        )

        towers.clear()
        towers.addAll(combatResult.updatedTowers)

        enemies.clear()
        enemies.addAll(combatResult.updatedEnemies.filter { it.isAlive && !it.reachedBase })

        destructibles.clear()
        destructibles.addAll(combatResult.updatedDestructibles.filter { it.isAlive })

        // Clear manual target if selected destructible was destroyed
        if (selectedDestructibleId != null && (destructibles.none { it.id == selectedDestructibleId } || combatResult.destroyedDestructibleIds.contains(selectedDestructibleId))) {
            selectedDestructibleId = null
        }

        // Synchronize selectedExistingTower state
        if (selectedExistingTower != null) {
            selectedExistingTower = towers.firstOrNull { it.id == selectedExistingTower?.id }
        }

        projectiles.clear()
        projectiles.addAll(combatResult.updatedProjectiles)

        visualEffects.clear()
        visualEffects.addAll(combatResult.updatedEffects)

        // Discrete weapon fire sound events
        if (combatResult.firedTowerTypes.contains(TowerType.MACHINE_GUN)) {
            audioPlayer.machineGunFire()
        }
        if (combatResult.firedTowerTypes.contains(TowerType.CANNON)) {
            audioPlayer.cannonFire()
        }
        if (combatResult.firedTowerTypes.contains(TowerType.RAPID_FIRE)) {
            audioPlayer.rapidFire()
        }
        if (combatResult.firedTowerTypes.contains(TowerType.FROST_GUN)) {
            audioPlayer.frostFire()
        }

        // Discrete impact sound events
        if (combatResult.hasCannonImpact) {
            audioPlayer.cannonImpact()
        }
        if (combatResult.hasFrostImpact) {
            audioPlayer.frostImpact()
        }
        if (combatResult.hasBossImpact) {
            audioPlayer.bossImpact()
        }
        if (combatResult.hasHeavyEnemyHit) {
            audioPlayer.heavyEnemyHit()
        } else if (combatResult.hasSmallEnemyHit || combatResult.hasEnemyHit) {
            audioPlayer.enemyHit()
        }

        // Destructibles sound events
        if (combatResult.hasTreeDestroyed) {
            audioPlayer.treeDestroyed()
        } else if (combatResult.hasStoneDestroyed) {
            audioPlayer.stoneDestroyed()
        } else if (combatResult.hasObjectDestroyed) {
            audioPlayer.objectDestroy()
        } else if (combatResult.hasTreeHit) {
            audioPlayer.treeHit()
        } else if (combatResult.hasStoneHit) {
            audioPlayer.stoneHit()
        } else if (combatResult.hasObjectHit) {
            audioPlayer.objectHit()
        }

        if (combatResult.coinsEarned > 0) {
            economySystem.addCoins(combatResult.coinsEarned)
            audioPlayer.coinReward()
        }
        if (combatResult.enemiesKilled > 0) {
            enemiesKilledTotal += combatResult.enemiesKilled
            audioPlayer.enemyDeath()
        }

        if (combatResult.bossDefeated) {
            showNotice("BOSS DEFEATED! Massive Coin Reward!")
            audioPlayer.bossDefeated()
        }
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

    @Synchronized
    fun selectDestructible(id: String?) {
        selectedDestructibleId = id
        publishState()
    }

    @Synchronized
    fun setTowerManualTarget(towerId: String, targetId: String?, type: com.example.entities.TargetType? = null): Boolean {
        val idx = towers.indexOfFirst { it.id == towerId }
        if (idx < 0) return false
        val tower = towers[idx]
        if (targetId != null) {
            val destObj = destructibles.firstOrNull { it.id == targetId && it.isAlive }
            val enemyObj = enemies.firstOrNull { it.id == targetId && it.isAlive }
            val inRange = when {
                destObj != null -> tower.isObjectInRange(destObj.position, destObj.radius)
                enemyObj != null -> tower.isEnemyInRange(enemyObj.position)
                else -> false
            }
            if (!inRange) {
                showNotice("Target is out of range!")
                audioPlayer.invalidPlacement()
                return false
            }
            val resolvedType = type ?: if (destObj != null) com.example.entities.TargetType.DESTRUCTIBLE else com.example.entities.TargetType.ENEMY
            val updated = tower.setManualTarget(targetId, resolvedType)
            towers[idx] = updated
            if (selectedExistingTower?.id == towerId) {
                selectedExistingTower = updated
            }
            if (destObj != null) {
                selectedDestructibleId = destObj.id
                showNotice("Targeting ${destObj.type.displayName}!")
            } else if (enemyObj != null) {
                showNotice("Targeting ${enemyObj.spec.name}!")
            }
        } else {
            val updated = tower.clearManualTarget()
            towers[idx] = updated
            if (selectedExistingTower?.id == towerId) {
                selectedExistingTower = updated
            }
        }
        publishState()
        return true
    }

    @Synchronized
    fun clearTowerManualTarget(towerId: String): Boolean {
        return setTowerManualTarget(towerId, null)
    }

    @Synchronized
    fun stopTargetingDestructible(id: String) {
        if (selectedDestructibleId == id) {
            selectedDestructibleId = null
        }
        for (i in towers.indices) {
            if (towers[i].manualTargetId == id) {
                val cleared = towers[i].clearManualTarget()
                towers[i] = cleared
                if (selectedExistingTower?.id == cleared.id) {
                    selectedExistingTower = cleared
                }
            }
        }
        val dest = destructibles.firstOrNull { it.id == id }
        val name = dest?.type?.displayName ?: "Object"
        showNotice("Stopped targeting $name")
        publishState()
    }

    @Synchronized
    fun clearSelection() {
        selectedExistingTower = null
        selectedBuildPos = null
        selectedDestructibleId = null
        isBuildingTower = false
        selectedTowerSpec = null
        previewPlacementPos = null
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
            audioPlayer.invalidPlacement()
            return false
        }

        economySystem.spend(cost)
        val upgraded = tower.upgrade() ?: return false

        val idx = towers.indexOfFirst { it.id == tower.id }
        if (idx != -1) {
            towers[idx] = upgraded
            selectedExistingTower = upgraded
            showNotice("${upgraded.spec.name} upgraded!")
            audioPlayer.towerUpgraded()
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
        audioPlayer.towerSold()
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

    /**
     * Single source of truth for tower placement validation across all systems:
     * - Normal tower placement
     * - Radial build preview / validation
     * - Tutorial build marker beacon
     * - Preview / ghost tower
     */
    fun isValidTowerPlacement(
        worldX: Float,
        worldY: Float,
        towerRadius: Float = GameConfig.TOWER_SIZE / 2f
    ): Boolean {
        val point = Point2D(worldX, worldY)
        // 1. Check boundary and impassable map tiles (road, water, base)
        if (!currentMap.canPlaceAt(point, towerRadius)) {
            return false
        }
        // 2. Existing towers
        val tooCloseToOther = towers.any {
            it.position.distanceTo(point) < GameConfig.MIN_DISTANCE_BETWEEN_TOWERS
        }
        if (tooCloseToOther) {
            return false
        }
        // 3. Destructible environment objects (Trees, Stones, Rocks, Crates)
        val overlapsDestructible = destructibles.any {
            it.isAlive && it.position.distanceTo(point) < (towerRadius + it.radius + 6f)
        }
        if (overlapsDestructible) {
            return false
        }
        return true
    }

    /**
     * Finds the nearest valid empty buildable location around a preferred position.
     * Used by the Level 1 tutorial marker to dynamically ensure it points to a valid spot.
     */
    fun findNearestValidBuildLocation(
        preferred: Point2D = Point2D(340f, 400f),
        towerRadius: Float = GameConfig.TOWER_SIZE / 2f
    ): Point2D {
        if (isValidTowerPlacement(preferred.x, preferred.y, towerRadius)) {
            return preferred
        }
        for (r in 15..600 step 15) {
            for (deg in 0 until 360 step 15) {
                val rad = Math.toRadians(deg.toDouble())
                val candX = (preferred.x + kotlin.math.cos(rad) * r).toFloat()
                val candY = (preferred.y + kotlin.math.sin(rad) * r).toFloat()
                if (isValidTowerPlacement(candX, candY, towerRadius)) {
                    return Point2D(candX, candY)
                }
            }
        }
        return preferred
    }

    fun isBuildableLocation(point: Point2D): Boolean {
        return isValidTowerPlacement(point.x, point.y, GameConfig.TOWER_SIZE / 2f)
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
        val radius = spec.size / 2f
        val targetPoint = Point2D(virtualX, virtualY)

        val obstacle = destructibles.firstOrNull {
            it.isAlive && it.position.distanceTo(targetPoint) < (radius + it.radius + 6f)
        }
        if (obstacle != null) {
            showNotice("Cannot build: Blocked by ${obstacle.type.displayName}!")
            audioPlayer.invalidPlacement()
            return false
        }

        if (!isValidTowerPlacement(virtualX, virtualY, radius)) {
            showNotice("Cannot build here! Check paths, water, or obstacles.")
            audioPlayer.invalidPlacement()
            return false
        }

        if (!economySystem.spend(spec.cost)) {
            showNotice("Not enough coins! Need ${spec.cost}🪙")
            audioPlayer.invalidPlacement()
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
        audioPlayer.towerPlaced()
        publishState()
        return true
    }

    fun validatePlacement(virtualX: Float, virtualY: Float): Pair<Boolean, String?> {
        val spec = selectedTowerSpec ?: TowerSpec.create(TowerType.MACHINE_GUN, 1)
        val radius = spec.size / 2f
        val targetPoint = Point2D(virtualX, virtualY)

        if (!currentMap.canPlaceAt(targetPoint, radius)) {
            return false to "Cannot build here! Check road, water, or base."
        }

        val tooCloseToOther = towers.any {
            it.position.distanceTo(targetPoint) < GameConfig.MIN_DISTANCE_BETWEEN_TOWERS
        }
        if (tooCloseToOther) {
            return false to "Too close to another tower!"
        }

        val obstacle = destructibles.firstOrNull {
            it.isAlive && it.position.distanceTo(targetPoint) < (radius + it.radius + 6f)
        }
        if (obstacle != null) {
            return false to "Blocked by ${obstacle.type.displayName}!"
        }

        if (!economySystem.canAfford(spec.cost)) {
            return false to "Not enough coins! Need ${spec.cost}🪙"
        }

        return true to null
    }

    fun tryPlaceTower(virtualX: Float, virtualY: Float): Boolean {
        val (isValid, reason) = validatePlacement(virtualX, virtualY)
        if (!isValid) {
            if (reason != null) showNotice(reason)
            audioPlayer.invalidPlacement()
            return false
        }

        val spec = selectedTowerSpec ?: TowerSpec.create(TowerType.MACHINE_GUN, 1)
        val targetPoint = Point2D(virtualX, virtualY)

        if (!economySystem.spend(spec.cost)) {
            showNotice("Not enough coins! Need ${spec.cost}🪙")
            audioPlayer.invalidPlacement()
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
        audioPlayer.towerPlaced()
        publishState()
        return true
    }

    fun loadMap(map: GameMap) {
        currentMap = map
        audioPlayer.updateMapAmbience(map.environmentType)
        restart(isNewMission = true)
    }

    @Synchronized
    fun pause() {
        if (gameStatus != GameStatus.PAUSED && gameStatus != GameStatus.GAME_OVER && gameStatus != GameStatus.VICTORY) {
            stateBeforePause = gameStatus
            changeGameState(GameStatus.PAUSED)
            audioPlayer.pauseAmbienceAndMusic()
        }
    }

    @Synchronized
    fun resume() {
        if (gameStatus == GameStatus.PAUSED) {
            changeGameState(stateBeforePause)
            audioPlayer.resumeAmbienceAndMusic()
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
        destructibles.clear()
        destructibles.addAll(currentMap.destructibles)
        projectiles.clear()
        visualEffects.clear()
        waveManager = WaveManager(
            maxWaves = currentMap.totalWaves,
            paths = currentMap.paths,
            isSnowValley = currentMap.environmentType == EnvironmentType.SNOW_VALLEY,
            isNightFortress = currentMap.environmentType == EnvironmentType.NIGHT_FORTRESS,
            mapId = currentMap.id
        )
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
        selectedDestructibleId = null
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

    fun showNotice(message: String) {
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
        val tutPlot = if (currentMap.id == "green_valley" && towers.isEmpty()) {
            findNearestValidBuildLocation(Point2D(340f, 400f), GameConfig.TOWER_SIZE / 2f)
        } else null

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
            wavesCleared = waveManager.wavesCleared,
            enemyHpMultiplier = waveManager.currentHpMultiplier,
            enemySpeedMultiplier = waveManager.currentSpeedMultiplier,
            waveStatus = waveManager.status,
            enemiesRemaining = enemies.size,
            nextWaveCountdown = waveManager.nextWaveCountdown,
            towers = towers.toList(),
            enemies = enemies.toList(),
            destructibles = destructibles.toList(),
            projectiles = projectiles.toList(),
            effects = visualEffects.toList(),
            isBuildingTower = isBuildingTower,
            selectedTowerSpec = selectedTowerSpec,
            previewPlacementPos = previewPlacementPos,
            isValidPlacement = isValidPlacement,
            selectedExistingTower = selectedExistingTower,
            selectedDestructibleId = selectedDestructibleId,
            placementNotice = placementNotice,
            enemiesKilledTotal = enemiesKilledTotal,
            gameSpeedMultiplier = gameSpeedMultiplier,
            activeBoss = activeBoss,
            isBossWave = waveManager.isBossWave(waveManager.currentWave),
            upcomingBossName = waveManager.getUpcomingBossName(waveManager.currentWave),
            starsEarned = stars,
            finalScore = score,
            gameTime = gameTime,
            tutorialRecommendedPlot = tutPlot
        )
    }
}
