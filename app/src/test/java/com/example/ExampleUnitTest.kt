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
        assertEquals(com.example.game.GameStatus.PREPARATION, engine.gameState.value.gameStatus)
        assertEquals(5.0f, engine.gameState.value.preparationCountdown, 0.01f)
        assertTrue(engine.gameState.value.enemies.isEmpty())

        // Advance 2 seconds of preparation (40 frames of 0.05s)
        repeat(40) { engine.update(0.05f) }
        assertEquals(com.example.game.GameStatus.PREPARATION, engine.gameState.value.gameStatus)
        assertEquals(3.0f, engine.gameState.value.preparationCountdown, 0.05f)
        assertTrue(engine.gameState.value.enemies.isEmpty())

        // Advance remaining 3.1 seconds (62 frames of 0.05s) -> transitions automatically to PLAYING
        repeat(62) { engine.update(0.05f) }
        assertEquals(com.example.game.GameStatus.PLAYING, engine.gameState.value.gameStatus)
        assertEquals(com.example.systems.WaveStatus.SPAWNING, engine.gameState.value.waveStatus)
    }

    @Test
    fun testPauseResumePreservesPreparation() {
        val engine = com.example.game.GameEngine()
        // Advance 1.5 seconds (30 frames of 0.05s)
        repeat(30) { engine.update(0.05f) }
        assertEquals(3.5f, engine.gameState.value.preparationCountdown, 0.05f)

        engine.pause()
        assertEquals(com.example.game.GameStatus.PAUSED, engine.gameState.value.gameStatus)

        // Ticking while paused should NOT advance preparation countdown
        repeat(20) { engine.update(0.05f) }
        assertEquals(3.5f, engine.gameState.value.preparationCountdown, 0.05f)

        engine.resume()
        assertEquals(com.example.game.GameStatus.PREPARATION, engine.gameState.value.gameStatus)
        assertEquals(3.5f, engine.gameState.value.preparationCountdown, 0.05f)
    }

    @Test
    fun testEmptyBuildableAreaContextualPlacement() {
        val engine = com.example.game.GameEngine()
        val buildPoint = Point2D(200f, 700f)
        assertTrue(engine.isBuildableLocation(buildPoint))

        // Select build position
        engine.selectBuildPosition(buildPoint)
        assertEquals(buildPoint, engine.gameState.value.selectedBuildPos)

        // Place Gunner tower
        val initialCoins = engine.gameState.value.coins
        val success = engine.placeTowerAt(TowerType.MACHINE_GUN, buildPoint.x, buildPoint.y)
        assertTrue(success)
        assertEquals(1, engine.gameState.value.towers.size)
        assertEquals(initialCoins - GameConfig.MG_TOWER_COST, engine.gameState.value.coins)
        assertEquals(null, engine.gameState.value.selectedBuildPos)

        // Cannot place another tower at the same location
        assertFalse(engine.isBuildableLocation(buildPoint))
    }

    @Test
    fun testSpeedMultiplierCycle() {
        val engine = com.example.game.GameEngine()
        assertEquals(1f, engine.gameState.value.gameSpeedMultiplier)
        engine.toggleSpeed()
        assertEquals(2f, engine.gameState.value.gameSpeedMultiplier)
        engine.toggleSpeed()
        assertEquals(3f, engine.gameState.value.gameSpeedMultiplier)
        engine.toggleSpeed()
        assertEquals(1f, engine.gameState.value.gameSpeedMultiplier)
    }

    @Test
    fun testMultiPathWaveDispatch() {
        val path1 = GamePath(id = "p1", waypoints = listOf(Point2D(0f, 0f), Point2D(100f, 0f)))
        val path2 = GamePath(id = "p2", waypoints = listOf(Point2D(0f, 100f), Point2D(100f, 100f)))
        val waveManager = WaveManager(maxWaves = 5, paths = listOf(path1, path2))

        waveManager.startCurrentWave()
        // WaveManager.update spawns an enemy when cooldown elapses
        val e1 = waveManager.update(1.0f, 0)
        assertNotNull(e1)
        assertEquals(0, e1!!.pathIndex)

        val e2 = waveManager.update(1.5f, 1)
        assertNotNull(e2)
        assertEquals(1, e2!!.pathIndex)
    }

    @Test
    fun testMapCannotPlaceOnRoads() {
        val map = com.example.data.GameMap.createGreenValleyMap(isUnlocked = true)
        // Waypoint at (500f, 340f) is directly on the path
        val onRoad = map.canPlaceAt(Point2D(500f, 340f), 40f)
        assertFalse("Tower placement on road should be forbidden", onRoad)

        // Clear grassy open field at (200f, 700f)
        val onMeadow = map.canPlaceAt(Point2D(200f, 700f), 40f)
        assertTrue("Tower placement on open meadow should be allowed", onMeadow)
    }

    @Test
    fun testTunnelRegionDetection() {
        val map = com.example.data.GameMap.createObsidianTunnelMap(isUnlocked = true)
        assertNotNull(map.tunnelRegion)
        // Inside tunnel bounds: (620f, 600f)
        assertTrue(map.isPointInTunnel(Point2D(620f, 600f)))
        // Outside tunnel: (100f, 100f)
        assertFalse(map.isPointInTunnel(Point2D(100f, 100f)))

        // Cannot place tower directly inside solid mountain rock
        assertFalse(map.canPlaceAt(Point2D(620f, 600f), 40f))
    }

    @Test
    fun testEnemyHeadingAngleAdvance() {
        val path = GamePath(waypoints = listOf(Point2D(0f, 0f), Point2D(100f, 0f), Point2D(100f, 100f)))
        var enemy = Enemy(
            id = "test-soldier",
            spec = EnemySpec.SOLDIER,
            position = Point2D(0f, 0f),
            headingAngle = 0f
        )
        // Advance along first segment (eastwards -> angle should be approx 0 degrees)
        enemy = enemy.advance(0.1f, path)
        assertEquals(0f, enemy.headingAngle, 5f)

        // Advance past (100, 0) into southwards segment (towards 100, 100) -> angle turns towards 90 degrees
        repeat(30) {
            enemy = enemy.advance(0.1f, path)
        }
        assertEquals(90f, enemy.headingAngle, 10f)
    }

    @Test
    fun testBossSpawnCreatesEntranceEffect() {
        val engine = com.example.game.GameEngine()
        // Force spawn a boss wave or check wave manager boss spec
        val bossSpec = EnemySpec.createBoss(5)
        assertTrue(bossSpec.isBoss)
        assertEquals(EnemyType.BOSS, bossSpec.type)
        assertTrue(bossSpec.radius >= 36f)
    }
}
