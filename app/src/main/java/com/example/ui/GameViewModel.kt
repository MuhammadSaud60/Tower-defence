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
    fun clearSelection() {
        gameEngine.selectExistingTower(null)
        gameEngine.selectBuildPosition(null)
    }
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
            "green_valley" -> progressionManager.unlockMap("desert_outpost")
            "desert_outpost" -> progressionManager.unlockMap("forest_pass")
            "forest_pass" -> progressionManager.unlockMap("split_routes")
            "split_routes" -> progressionManager.unlockMap("canyon_tunnel")
            "canyon_tunnel", "map_4_tunnel" -> progressionManager.unlockMap("the_crossing")
            "the_crossing" -> progressionManager.unlockMap("map_5_loop")
        }
    }
}
