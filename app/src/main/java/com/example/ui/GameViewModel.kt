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
                val dt = ((currentTime - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                lastTime = currentTime
                gameEngine.update(dt)
                delay(16L)
            }
        }
    }

    fun startWave() = gameEngine.startWave()
    fun selectTowerToBuild(type: TowerType) = gameEngine.selectTowerToBuild(type)
    fun cancelTowerBuild() = gameEngine.cancelTowerBuild()
    fun updatePlacementPreview(virtualX: Float, virtualY: Float) = gameEngine.updatePlacementPreview(virtualX, virtualY)
    fun tryPlaceTower(virtualX: Float, virtualY: Float) = gameEngine.tryPlaceTower(virtualX, virtualY)
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
        if (mapId == "green_valley") {
            progressionManager.unlockMap("desert_outpost")
        }
        if (progressionManager.getTotalStars() >= 3) {
            progressionManager.unlockMap("forest_pass")
        }
    }
}
