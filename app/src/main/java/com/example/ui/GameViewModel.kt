package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameMap
import com.example.data.ProgressionManager
import com.example.data.ProgressionReward
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerType
import com.example.game.GameEngine
import com.example.game.GameState
import com.example.game.GameStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val gameEngine = GameEngine()
    val progressionManager = ProgressionManager(application)
    val gameState: StateFlow<GameState> = gameEngine.gameState

    var lastVictoryReward: ProgressionReward? by mutableStateOf(null)
        private set

    var isGameActive by mutableStateOf(false)
        private set

    fun updateGameSessionActive(active: Boolean) {
        if (isGameActive == active) return
        isGameActive = active
        gameEngine.audioPlayer.isInBattlefield = active
        if (active) {
            gameEngine.resume()
        } else {
            gameEngine.pause()
        }
    }

    init {
        gameEngine.progressionManager = progressionManager
        gameEngine.audioPlayer.isInBattlefield = false
        gameEngine.pause()

        viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = currentTime
                if (isGameActive) {
                    try {
                        gameEngine.update(dt)
                    } catch (e: Throwable) {
                        android.util.Log.e("GameViewModel", "Exception safely handled in game loop", e)
                    }
                }
                delay(16L)
            }
        }

        viewModelScope.launch {
            var recordedGameOverStats = false
            gameState.collect { state ->
                if (state.gameStatus == GameStatus.VICTORY && lastVictoryReward == null) {
                    lastVictoryReward = handleVictoryProgression()
                } else if (state.gameStatus == GameStatus.GAME_OVER && !recordedGameOverStats) {
                    recordedGameOverStats = true
                    progressionManager.recordCombatStats(
                        enemiesKilled = gameEngine.getEnemiesKilledTotal(),
                        bossesKilled = gameEngine.getBossesKilledTotal(),
                        destructiblesCleared = gameEngine.getDestructiblesClearedTotal(),
                        towersPlaced = gameEngine.getTowersPlacedTotal(),
                        damageDealt = gameEngine.getDamageDealtTotal()
                    )
                } else if (state.gameStatus == GameStatus.PLAYING || state.gameStatus == GameStatus.PREPARATION) {
                    recordedGameOverStats = false
                    if (lastVictoryReward != null && state.currentWave <= 1) {
                        lastVictoryReward = null
                    }
                }
            }
        }
    }

    fun startWave() = gameEngine.startWave()
    fun selectTowerToBuild(type: TowerType) = gameEngine.selectTowerToBuild(type)
    fun cancelTowerBuild() = gameEngine.cancelTowerBuild()
    fun updatePlacementPreview(virtualX: Float, virtualY: Float) = gameEngine.updatePlacementPreview(virtualX, virtualY)
    fun tryPlaceTower(virtualX: Float, virtualY: Float) = gameEngine.tryPlaceTower(virtualX, virtualY)
    fun selectBuildPosition(point: com.example.entities.Point2D?) = gameEngine.selectBuildPosition(point)
    fun placeTowerAt(type: TowerType, virtualX: Float, virtualY: Float) = gameEngine.placeTowerAt(type, virtualX, virtualY)
    fun isBuildableLocation(point: com.example.entities.Point2D) = gameEngine.isBuildableLocation(point)
    fun isValidTowerPlacement(worldX: Float, worldY: Float, towerRadius: Float = com.example.data.GameConfig.TOWER_SIZE / 2f) =
        gameEngine.isValidTowerPlacement(worldX, worldY, towerRadius)
    fun clearSelection() {
        gameEngine.clearSelection()
    }
    fun selectDestructible(id: String?) = gameEngine.selectDestructible(id)
    fun stopTargetingDestructible(id: String) = gameEngine.stopTargetingDestructible(id)
    fun setTowerManualTarget(towerId: String, targetId: String?, type: com.example.entities.TargetType? = null) =
        gameEngine.setTowerManualTarget(towerId, targetId, type)
    fun clearTowerManualTarget(towerId: String) = gameEngine.clearTowerManualTarget(towerId)
    fun showNotice(message: String) = gameEngine.showNotice(message)
    fun invalidTargetFeedback() = gameEngine.audioPlayer.invalidPlacement()
    fun selectExistingTower(tower: Tower?) = gameEngine.selectExistingTower(tower)
    fun upgradeSelectedTower() = gameEngine.upgradeSelectedTower()
    fun sellSelectedTower() = gameEngine.sellSelectedTower()
    fun setStrategyForSelectedTower(strategy: TargetingStrategy) = gameEngine.setStrategyForSelectedTower(strategy)
    fun pause() = gameEngine.pause()
    fun resume() = gameEngine.resume()
    fun toggleSpeed() = gameEngine.toggleSpeed()
    fun setGameSpeed(speed: Float) = gameEngine.setGameSpeed(speed)
    fun continueBattle(): Boolean = gameEngine.continueBattle(0.20f)

    fun preloadRewardedAd(context: android.content.Context) {
        com.example.ads.AdManager.getInstance().preloadRewardedAd(context)
    }

    fun showRewardedReviveAd(
        activity: android.app.Activity,
        onRewardGranted: () -> Unit = {},
        onClosedWithoutReward: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        com.example.ads.AdManager.getInstance().showRewardedAd(
            activity = activity,
            onUserEarnedReward = {
                val revived = continueBattle()
                if (revived) {
                    onRewardGranted()
                }
            },
            onAdClosedWithoutReward = {
                onClosedWithoutReward()
            },
            onAdFailedToShow = { error ->
                onFailure(error)
            }
        )
    }

    fun restart() {
        lastVictoryReward = null
        gameEngine.restart()
        if (isGameActive) {
            gameEngine.audioPlayer.isInBattlefield = true
            gameEngine.resume()
        }
    }

    fun loadMap(map: GameMap) {
        lastVictoryReward = null
        gameEngine.loadMap(map)
        if (isGameActive) {
            gameEngine.audioPlayer.isInBattlefield = true
            gameEngine.resume()
        }
    }

    fun handleVictoryProgression(): ProgressionReward? {
        val state = gameState.value
        val mapId = state.currentMap.id

        // Persist combat stats to lifetime progression
        progressionManager.recordCombatStats(
            enemiesKilled = gameEngine.getEnemiesKilledTotal(),
            bossesKilled = gameEngine.getBossesKilledTotal(),
            destructiblesCleared = gameEngine.getDestructiblesClearedTotal(),
            towersPlaced = gameEngine.getTowersPlacedTotal(),
            damageDealt = gameEngine.getDamageDealtTotal()
        )

        val previousStars = progressionManager.getStarsForMap(mapId)
        val basePercent = if (state.maxBaseHp > 0) state.baseHp.toFloat() / state.maxBaseHp else 1f

        val reward = progressionManager.recordBattleVictory(
            mapId = mapId,
            starsEarned = state.starsEarned,
            enemiesKilled = gameEngine.getEnemiesKilledTotal(),
            bossesKilled = gameEngine.getBossesKilledTotal(),
            finalBaseHpPercent = basePercent,
            destructiblesDestroyed = gameEngine.getDestructiblesClearedTotal(),
            towersPlaced = gameEngine.getTowersPlacedTotal(),
            damageDealt = gameEngine.getDamageDealtTotal()
        )

        progressionManager.saveStarsForMap(mapId, state.starsEarned)
        progressionManager.recordWaveReached(mapId, state.currentWave)
        when (mapId) {
            "green_valley", "map_1_valley" -> progressionManager.unlockMap("desert_outpost")
            "desert_outpost", "map_2_canyon" -> progressionManager.unlockMap("forest_pass")
            "forest_pass", "map_3_crossroads" -> progressionManager.unlockMap("split_routes")
            "split_routes", "map_4_split" -> progressionManager.unlockMap("canyon_tunnel")
            "canyon_tunnel", "map_4_tunnel" -> progressionManager.unlockMap("the_crossing")
            "the_crossing", "map_6_crossing" -> progressionManager.unlockMap("map_5_loop")
            "map_5_loop", "dragons_coil" -> progressionManager.unlockMap("snow_outpost")
            "snow_outpost" -> progressionManager.unlockMap("frozen_valley")
            "frozen_valley", "snow_valley", "map_8_snow" -> progressionManager.unlockMap("ice_mountain")
            "ice_mountain" -> progressionManager.unlockMap("frozen_fortress")
            "frozen_fortress" -> progressionManager.unlockMap("arctic_base")
            "arctic_base" -> progressionManager.unlockMap("night_fortress")
            "night_fortress", "map_9_night" -> progressionManager.unlockMap("eclipse_frontier")
            "eclipse_frontier", "solstice_frontier" -> progressionManager.unlockMap("storm_twin_bastion")
            "storm_twin_bastion", "tempest_bastion" -> progressionManager.unlockMap("cloudy_dense_forest")
            "cloudy_dense_forest", "cloudy_forest" -> progressionManager.unlockMap("snow_summit_descent")
            "snow_summit_descent", "frostpeak_descent" -> progressionManager.unlockMap("desert_dune_bastion")
        }

        return reward
    }

    fun getNextMap(): GameMap? {
        val currentId = gameState.value.currentMap.id
        return when (currentId) {
            "green_valley", "map_1_valley" -> GameMap.createDesertOutpostMap(isUnlocked = true, stars = progressionManager.getStarsForMap("desert_outpost"))
            "desert_outpost", "map_2_canyon" -> GameMap.createForestPassMap(isUnlocked = true, stars = progressionManager.getStarsForMap("forest_pass"))
            "forest_pass", "map_3_crossroads" -> GameMap.createSplitRoutesMap(isUnlocked = true, stars = progressionManager.getStarsForMap("split_routes"))
            "split_routes", "map_4_split" -> GameMap.createCanyonTunnelMap(isUnlocked = true, stars = progressionManager.getStarsForMap("canyon_tunnel"))
            "canyon_tunnel", "map_4_tunnel" -> GameMap.createTheCrossingMap(isUnlocked = true, stars = progressionManager.getStarsForMap("the_crossing"))
            "the_crossing", "map_6_crossing" -> GameMap.createDragonsCoilMap(isUnlocked = true, stars = progressionManager.getStarsForMap("map_5_loop"))
            "map_5_loop", "dragons_coil" -> GameMap.createSnowOutpostMap(isUnlocked = true, stars = progressionManager.getStarsForMap("snow_outpost"))
            "snow_outpost" -> GameMap.createFrozenValleyMap(isUnlocked = true, stars = progressionManager.getStarsForMap("frozen_valley"))
            "frozen_valley", "snow_valley", "map_8_snow" -> GameMap.createIceMountainMap(isUnlocked = true, stars = progressionManager.getStarsForMap("ice_mountain"))
            "ice_mountain" -> GameMap.createFrozenFortressMap(isUnlocked = true, stars = progressionManager.getStarsForMap("frozen_fortress"))
            "frozen_fortress" -> GameMap.createArcticBaseMap(isUnlocked = true, stars = progressionManager.getStarsForMap("arctic_base"))
            "arctic_base" -> GameMap.createNightFortressMap(isUnlocked = true, stars = progressionManager.getStarsForMap("night_fortress"))
            "night_fortress", "map_9_night" -> GameMap.createEclipseFrontierMap(isUnlocked = true, stars = progressionManager.getStarsForMap("eclipse_frontier"))
            "eclipse_frontier", "solstice_frontier" -> GameMap.createTempestBastionMap(isUnlocked = true, stars = progressionManager.getStarsForMap("storm_twin_bastion"))
            "storm_twin_bastion", "tempest_bastion" -> GameMap.createCloudyForestMap(isUnlocked = true, stars = progressionManager.getStarsForMap("cloudy_dense_forest"))
            "cloudy_dense_forest", "cloudy_forest" -> GameMap.createSnowSummitMap(isUnlocked = true, stars = progressionManager.getStarsForMap("snow_summit_descent"))
            "snow_summit_descent", "frostpeak_descent" -> GameMap.createDesertDuneBastionMap(isUnlocked = true, stars = progressionManager.getStarsForMap("desert_dune_bastion"))
            "desert_dune_bastion", "dune_storm_stronghold" -> GameMap.createEmeraldTwinPassMap(isUnlocked = true, stars = progressionManager.getStarsForMap("emerald_twin_pass"))
            "emerald_twin_pass", "emerald_serpent_pass" -> GameMap.createForestRingBastionMap(isUnlocked = true, stars = progressionManager.getStarsForMap("forest_ring_bastion"))
            "forest_ring_bastion", "sylvan_ring_sanctuary" -> GameMap.createFrozenPassMap(isUnlocked = true, stars = progressionManager.getStarsForMap("frozen_pass"))
            "frozen_pass" -> GameMap.createObsidianCrossfireMap(isUnlocked = true, stars = progressionManager.getStarsForMap("obsidian_crossfire"))
            "obsidian_crossfire" -> GameMap.createTempestRavineMap(isUnlocked = true, stars = progressionManager.getStarsForMap("tempest_ravine"))
            "tempest_ravine" -> GameMap.createEclipseCitadelMap(isUnlocked = true, stars = progressionManager.getStarsForMap("eclipse_citadel"))
            "eclipse_citadel" -> GameMap.createApexDragonSanctumMap(isUnlocked = true, stars = progressionManager.getStarsForMap("apex_dragon_sanctum"))
            else -> null
        }
    }

    fun toggleWeatherRain() {
        gameEngine.toggleWeatherRain()
    }

    fun loadNextMap() {
        val next = getNextMap()
        if (next != null) {
            loadMap(next)
        } else {
            restart()
        }
    }
}
