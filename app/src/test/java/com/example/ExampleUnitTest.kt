package com.example

import com.example.data.GameConfig
import com.example.entities.Enemy
import com.example.entities.EnemySpec
import com.example.entities.EnemyType
import com.example.entities.GamePath
import com.example.entities.Point2D
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerSpec
import com.example.entities.TowerType
import com.example.systems.EconomySystem
import com.example.systems.TargetingSystem
import com.example.systems.WaveManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTowerTypesAndUpgrades() {
        val mg = Tower(spec = TowerSpec.create(TowerType.MACHINE_GUN, 1), position = Point2D(100f, 100f))
        assertEquals(1, mg.spec.level)
        assertEquals(GameConfig.MG_TOWER_COST, mg.spec.cost)

        val mg2 = mg.upgrade()
        assertNotNull(mg2)
        assertEquals(2, mg2!!.spec.level)
        assertTrue(mg2.spec.damage > mg.spec.damage)

        val mg3 = mg2.upgrade()
        assertNotNull(mg3)
        assertEquals(3, mg3!!.spec.level)
        assertTrue(mg3.isMaxLevel)
    }

    @Test
    fun testTowerSellingRefund() {
        val cannon = Tower(
            spec = TowerSpec.create(TowerType.CANNON, 1),
            position = Point2D(200f, 200f),
            totalCoinsInvested = 100
        )
        // 70% refund of 100 is 70
        assertEquals(70, cannon.sellRefundCoins)
    }

    @Test
    fun testEconomySystem() {
        val economy = EconomySystem(120)
        assertTrue(economy.canAfford(100))
        assertFalse(economy.canAfford(150))

        assertTrue(economy.spend(40))
        assertEquals(80, economy.currentCoins)

        economy.addCoins(25)
        assertEquals(105, economy.currentCoins)
    }

    @Test
    fun testTargetingStrategies() {
        val targeting = TargetingSystem()
        val tower = Tower(
            spec = TowerSpec(range = 300f),
            position = Point2D(0f, 0f),
            targetingStrategy = TargetingStrategy.FIRST
        )

        val e1 = Enemy(
            id = "e1",
            spec = EnemySpec.SCOUT,
            position = Point2D(50f, 0f),
            totalProgress = 100f,
            currentHp = 30f
        )
        val e2 = Enemy(
            id = "e2",
            spec = EnemySpec.HEAVY,
            position = Point2D(100f, 0f),
            totalProgress = 50f,
            currentHp = 200f
        )

        // FIRST strategy picks e1 (higher totalProgress 100 > 50)
        val targetFirst = targeting.findTarget(tower.setStrategy(TargetingStrategy.FIRST), listOf(e1, e2))
        assertEquals("e1", targetFirst?.id)

        // LAST strategy picks e2 (lower totalProgress 50 < 100)
        val targetLast = targeting.findTarget(tower.setStrategy(TargetingStrategy.LAST), listOf(e1, e2))
        assertEquals("e2", targetLast?.id)

        // STRONGEST strategy picks e2 (higher HP 200 > 30)
        val targetStrongest = targeting.findTarget(tower.setStrategy(TargetingStrategy.STRONGEST), listOf(e1, e2))
        assertEquals("e2", targetStrongest?.id)

        // CLOSEST strategy picks e1 (50 distance < 100 distance)
        val targetClosest = targeting.findTarget(tower.setStrategy(TargetingStrategy.CLOSEST), listOf(e1, e2))
        assertEquals("e1", targetClosest?.id)
    }

    @Test
    fun testWaveManager20WavesAndBosses() {
        val path = GamePath()
        val waveManager = WaveManager(maxWaves = 20, path = path)
        assertEquals(20, waveManager.maxWaves)
        assertEquals(1, waveManager.currentWave)

        // Boss encounters at wave 5, 10, 15, 20
        val boss5 = EnemySpec.createBoss(5)
        assertTrue(boss5.isBoss)
        assertEquals(EnemyType.BOSS, boss5.type)

        val boss20 = EnemySpec.createBoss(20)
        assertTrue(boss20.isBoss)
        assertTrue(boss20.baseHp > boss5.baseHp)
    }

    @Test
    fun testStartWaveEngineTransition() {
        val engine = com.example.game.GameEngine()
        assertEquals(com.example.game.GameStatus.WAVE_COMPLETE, engine.gameState.value.gameStatus)

        engine.startWave()
        assertEquals(com.example.game.GameStatus.PLAYING, engine.gameState.value.gameStatus)
        assertEquals(com.example.systems.WaveStatus.SPAWNING, engine.gameState.value.waveStatus)
    }
}
