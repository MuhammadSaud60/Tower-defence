package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameMap
import com.example.data.ProgressionManager
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerType
import com.example.game.GameEngine
import com.example.game.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val gameEngine = GameEngine()
    val progressionManager = ProgressionManager(application)
    val gameState: StateFlow<GameState> = gameEngine.gameState

    init {
        viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val currentTime = System.nanoTime()
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastTime = currentTime
                try {
                    gameEngine.update(dt)
                } catch (e: Throwable) {
                    android.util.Log.e("GameViewModel", "Exception safely handled in game loop", e)
                }
                delay(16L)
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
    fun restart() = gameEngine.restart()

    fun loadMap(map: GameMap) {
        gameEngine.loadMap(map)
    }

    fun handleVictoryProgression() {
        val state = gameState.value
        val mapId = state.currentMap.id
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
        }
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
            else -> null
        }
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
