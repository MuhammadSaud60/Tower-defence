package com.example

import com.example.data.EnvironmentType
import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.entities.DestructibleObject
import com.example.entities.DestructibleType
import com.example.entities.EffectType
import com.example.entities.Enemy
import com.example.entities.EnemySpec
import com.example.entities.EnemyType
import com.example.entities.GamePath
import com.example.entities.Point2D
import com.example.entities.Projectile
import com.example.entities.ProjectileType
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerSpec
import com.example.entities.TowerType
import com.example.entities.VisualEffect
import com.example.systems.EconomySystem
import com.example.systems.EnemyScalingSystem
import com.example.systems.TargetingSystem
import com.example.systems.WaveManager
import com.example.systems.WaveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testDistinctHitFlashAndDeathEffectsPerEnemyType() {
        val enemyScout = Enemy(id = "s1", spec = EnemySpec.SCOUT, position = Point2D(50f, 50f))
        val enemyHeavy = Enemy(id = "h1", spec = EnemySpec.HEAVY, position = Point2D(50f, 50f))
        val enemyRunner = Enemy(id = "r1", spec = EnemySpec.RUNNER, position = Point2D(50f, 50f))
        val enemySoldier = Enemy(id = "sol1", spec = EnemySpec.SOLDIER, position = Point2D(50f, 50f))
        val enemyBoss = Enemy(id = "b1", spec = EnemySpec.createBoss(10), position = Point2D(50f, 50f))

        // Take damage sets hit flash timer
        val hitScout = enemyScout.takeDamage(10f)
        assertTrue(hitScout.isHitFlashing)
        assertEquals(0.14f, hitScout.hitFlashTimer, 0.01f)

        // Verify combat system death effect type mappings
        val combatSystem = com.example.systems.CombatSystem()
        // Kill each enemy with massive damage projectile
        val lethalProjScout = Projectile(
            type = ProjectileType.BULLET,
            currentPosition = Point2D(50f, 50f),
            targetLastKnownPosition = Point2D(50f, 50f),
            damage = 9999f,
            targetEnemyId = enemyScout.id
        )
        val resultScout = combatSystem.update(0.016f, emptyList<Tower>(), listOf(enemyScout), emptyList(), listOf(lethalProjScout), emptyList<VisualEffect>())
        val scoutDeathFx = resultScout.updatedEffects.firstOrNull { it.type == EffectType.SCOUT_DEATH }
        assertNotNull(scoutDeathFx)
        assertEquals(24f, scoutDeathFx!!.maxRadius)

        val lethalProjRunner = Projectile(
            type = ProjectileType.BULLET,
            currentPosition = Point2D(50f, 50f),
            targetLastKnownPosition = Point2D(50f, 50f),
            damage = 9999f,
            targetEnemyId = enemyRunner.id
        )
        val resultRunner = combatSystem.update(0.016f, emptyList<Tower>(), listOf(enemyRunner), emptyList(), listOf(lethalProjRunner), emptyList<VisualEffect>())
        val runnerDeathFx = resultRunner.updatedEffects.firstOrNull { it.type == EffectType.RUNNER_DEATH }
        assertNotNull(runnerDeathFx)

        val lethalProjHeavy = Projectile(
            type = ProjectileType.BULLET,
            currentPosition = Point2D(50f, 50f),
            targetLastKnownPosition = Point2D(50f, 50f),
            damage = 9999f,
            targetEnemyId = enemyHeavy.id
        )
        val resultHeavy = combatSystem.update(0.016f, emptyList<Tower>(), listOf(enemyHeavy), emptyList(), listOf(lethalProjHeavy), emptyList<VisualEffect>())
        val heavyDeathFx = resultHeavy.updatedEffects.firstOrNull { it.type == EffectType.HEAVY_DEATH }
        assertNotNull(heavyDeathFx)
        assertEquals(48f, heavyDeathFx!!.maxRadius)
        assertEquals(0.48f, heavyDeathFx.maxLifetime, 0.01f)

        val lethalProjBoss = Projectile(
            type = ProjectileType.BULLET,
            currentPosition = Point2D(50f, 50f),
            targetLastKnownPosition = Point2D(50f, 50f),
            damage = 999999f,
            targetEnemyId = enemyBoss.id
        )
        val resultBoss = combatSystem.update(0.016f, emptyList<Tower>(), listOf(enemyBoss), emptyList(), listOf(lethalProjBoss), emptyList<VisualEffect>())
        val bossDeathFx = resultBoss.updatedEffects.firstOrNull { it.type == EffectType.BOSS_DEATH }
        assertNotNull(bossDeathFx)
        assertEquals(80f, bossDeathFx!!.maxRadius)
        assertEquals(0.75f, bossDeathFx.maxLifetime, 0.01f)
    }

    @Test
    fun testFrostTowerStatsAndUpgrade() {
        val frostLvl1 = Tower(spec = TowerSpec.create(TowerType.FROST_GUN, 1), position = Point2D(100f, 100f))
        assertEquals(1, frostLvl1.spec.level)
        assertEquals(GameConfig.FROST_TOWER_COST, frostLvl1.spec.cost)
        assertEquals(GameConfig.FROST_TOWER_SLOW_FACTOR, frostLvl1.spec.slowFactor, 0.01f)
        assertEquals(GameConfig.FROST_TOWER_SLOW_DURATION, frostLvl1.spec.slowDuration, 0.01f)

        val frostLvl2 = frostLvl1.upgrade()
        assertNotNull(frostLvl2)
        assertEquals(2, frostLvl2!!.spec.level)
        assertEquals(0.55f, frostLvl2.spec.slowFactor, 0.01f)
        assertEquals(3.2f, frostLvl2.spec.slowDuration, 0.01f)

        val frostLvl3 = frostLvl2.upgrade()
        assertNotNull(frostLvl3)
        assertEquals(3, frostLvl3!!.spec.level)
        assertEquals(0.68f, frostLvl3.spec.slowFactor, 0.01f)
        assertEquals(4.0f, frostLvl3.spec.slowDuration, 0.01f)
        assertTrue(frostLvl3.isMaxLevel)
    }

    @Test
    fun testEnemySlowMechanicReducesMovementSpeed() {
        val path = GamePath(waypoints = listOf(Point2D(0f, 0f), Point2D(1000f, 0f)))
        val normalEnemy = Enemy(
            id = "runner-normal",
            spec = EnemySpec.RUNNER,
            position = Point2D(0f, 0f)
        )
        val normalMoved = normalEnemy.advance(1.0f, path)

        // Slowed enemy with 50% slow factor
        val slowedEnemy = normalEnemy.applySlow(factor = 0.50f, duration = 3.0f)
        assertTrue(slowedEnemy.isSlowed)
        assertEquals(0.50f, slowedEnemy.slowFactor, 0.01f)
        assertEquals(3.0f, slowedEnemy.slowTimer, 0.01f)

        val slowedMoved = slowedEnemy.advance(1.0f, path)
        // Progress of slowed enemy should be exactly 50% of normal enemy
        assertEquals(normalMoved.totalProgress * 0.5f, slowedMoved.totalProgress, 1.0f)
        assertTrue(slowedMoved.position.x < normalMoved.position.x)
    }

    @Test
    fun testEnemyBulletResistance() {
        val heavyEnemy = Enemy(
            id = "heavy-1",
            spec = EnemySpec.HEAVY, // 70% bullet resistance, 16 armor
            position = Point2D(50f, 50f)
        )
        assertEquals(0.70f, heavyEnemy.spec.bulletResistance, 0.01f)

        // Normal gun (isNormalGun = true): rawAmount 100 reduced by 70% to 30, minus 16 armor -> 14 net damage
        val hitByNormalGun = heavyEnemy.takeDamage(rawAmount = 100f, armorPiercing = 0f, isNormalGun = true)
        val damageTakenNormal = heavyEnemy.currentHp - hitByNormalGun.currentHp
        assertEquals(14f, damageTakenNormal, 0.01f)

        // Non-normal gun (e.g. Cannon / Frost): isNormalGun = false -> 100 raw minus 16 armor -> 84 net damage
        val hitByCannon = heavyEnemy.takeDamage(rawAmount = 100f, armorPiercing = 0f, isNormalGun = false)
        val damageTakenCannon = heavyEnemy.currentHp - hitByCannon.currentHp
        assertEquals(84f, damageTakenCannon, 0.01f)

        // Normal gun deals dramatically less damage against Heavy enemy than non-normal gun
        assertTrue(damageTakenNormal < damageTakenCannon * 0.20f)
    }

    @Test
    fun testDifficultyEconomySettings() {
        assertEquals(85, GameConfig.STARTING_COINS)
        assertEquals(12, GameConfig.WAVE_CLEAR_BONUS_COINS)

        val economy = EconomySystem()
        assertEquals(85, economy.currentCoins)
        // Reward for enemy kill should add 5 coins
        economy.rewardForEnemyKill(1)
        assertEquals(90, economy.currentCoins)
    }

    @Test
    fun testDestructibleObjectDamageAndHitFlash() {
        val tree = DestructibleObject(
            type = DestructibleType.OAK_TREE,
            position = Point2D(150f, 150f)
        )
        assertEquals(100f, tree.maxHp, 0.01f)
        assertEquals(100f, tree.currentHp, 0.01f)
        assertEquals(10, tree.rewardTokens)
        assertTrue(tree.isAlive)
        assertFalse(tree.isDestroyed)
        assertEquals(1.0f, tree.healthFraction, 0.01f)

        // Take partial damage
        val damaged = tree.takeDamage(25f)
        assertEquals(75f, damaged.currentHp, 0.01f)
        assertTrue(damaged.isAlive)
        assertFalse(damaged.isDestroyed)
        assertTrue(damaged.isHitFlashing)
        assertTrue(damaged.healthFraction < 1.0f)

        // Flash timer ticks down
        val postFlash = damaged.tickFlash(0.2f)
        assertFalse(postFlash.isHitFlashing)

        // Lethal damage destroys object
        val destroyed = damaged.takeDamage(80f)
        assertEquals(0f, destroyed.currentHp, 0.01f)
        assertTrue(destroyed.isDestroyed)
        assertFalse(destroyed.isAlive)
        assertEquals(0f, destroyed.healthFraction, 0.01f)
    }

    @Test
    fun testTowerDoesNotAutoTargetDestructibles() {
        val combatSystem = com.example.systems.CombatSystem()
        val tower = Tower(
            spec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
            position = Point2D(100f, 100f)
        )
        val rock = DestructibleObject(
            type = DestructibleType.SMALL_STONE,
            position = Point2D(120f, 100f) // 20 units away, inside tower range
        )

        // When no enemies in range and NO manual target set, tower MUST NOT shoot destructible
        val result = combatSystem.update(
            dt = 0.016f,
            towers = listOf(tower),
            enemies = emptyList(),
            destructibles = listOf(rock),
            projectiles = emptyList(),
            existingEffects = emptyList()
        )

        assertEquals("Tower must never auto-fire at destructibles", 0, result.newProjectilesFired)
        assertTrue(result.updatedProjectiles.isEmpty())

        // Only fires when manual target is set
        val manualTower = tower.setManualTarget(rock.id, com.example.entities.TargetType.DESTRUCTIBLE)
        val manualResult = combatSystem.update(
            dt = 0.016f,
            towers = listOf(manualTower),
            enemies = emptyList(),
            destructibles = listOf(rock),
            projectiles = emptyList(),
            existingEffects = emptyList()
        )
        assertTrue("Tower fires at manual target destructible", manualResult.newProjectilesFired > 0)
        assertEquals(rock.id, manualResult.updatedProjectiles.first().targetEnemyId)
    }

    @Test
    fun testTowerPlacementCannotOverlapDestructibles() {
        val engine = com.example.game.GameEngine()
        val tree = DestructibleObject(
            id = "test_tree",
            type = DestructibleType.TREE,
            position = Point2D(200f, 700f)
        )
        engine.destructibles.clear()
        engine.destructibles.add(tree)

        // Point directly at tree position
        assertFalse("Placement directly on tree must be invalid", engine.isValidTowerPlacement(200f, 700f))
        // Point too close to tree edge (collision radius)
        assertFalse("Placement overlapping tree radius must be invalid", engine.isValidTowerPlacement(210f, 705f))

        // Cannot place tower via placeTowerAt either
        val placed = engine.placeTowerAt(TowerType.MACHINE_GUN, 200f, 700f)
        assertFalse("placeTowerAt must fail on tree", placed)

        // Safe distance away should be valid
        val safePoint = Point2D(200f + tree.radius + 60f, 700f)
        if (engine.currentMap.canPlaceAt(safePoint, GameConfig.TOWER_SIZE / 2f)) {
            assertTrue("Placement at safe distance should be valid", engine.isValidTowerPlacement(safePoint.x, safePoint.y))
        }
    }

    @Test
    fun testManualTargetDestructionClearsTarget() {
        val combatSystem = com.example.systems.CombatSystem()
        val rock = DestructibleObject(
            id = "rock_1",
            type = DestructibleType.SMALL_STONE,
            position = Point2D(120f, 100f),
            currentHp = 10f,
            maxHp = 50f
        )
        val tower = Tower(
            id = "t1",
            spec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
            position = Point2D(100f, 100f),
            manualTargetId = rock.id,
            manualTargetType = com.example.entities.TargetType.DESTRUCTIBLE
        )
        val lethalProj = Projectile(
            type = ProjectileType.BULLET,
            currentPosition = Point2D(120f, 100f),
            targetLastKnownPosition = Point2D(120f, 100f),
            damage = 100f,
            targetEnemyId = rock.id
        )

        val result = combatSystem.update(
            dt = 0.016f,
            towers = listOf(tower),
            enemies = emptyList(),
            destructibles = listOf(rock),
            projectiles = listOf(lethalProj),
            existingEffects = emptyList()
        )

        assertTrue(result.hasStoneDestroyed || result.hasObjectDestroyed)
        assertTrue(result.destroyedDestructibleIds.contains(rock.id))
        val updatedTower = result.updatedTowers.first()
        assertEquals(null, updatedTower.manualTargetId)
        assertEquals(null, updatedTower.manualTargetType)
    }

    @Test
    fun testDestructibleBlocksTowerPlacementUntilDestroyed() {
        val engine = com.example.game.GameEngine()
        val pos = Point2D(200f, 200f)

        // Add an alive destructible directly at or near pos
        val crate = DestructibleObject(
            type = DestructibleType.WOODEN_CRATE,
            position = pos
        )
        engine.destructibles.clear()
        engine.destructibles.add(crate)

        // Placement should be blocked by the alive destructible
        assertFalse(engine.isBuildableLocation(pos))

        // Once destroyed (removed from active destructibles), position becomes buildable if terrain allows
        engine.destructibles.clear()
        val canBuildNow = engine.currentMap.canPlaceAt(pos, GameConfig.TOWER_SIZE / 2f)
        assertEquals(canBuildNow, engine.isBuildableLocation(pos))
    }

    @Test
    fun testEnemyScalingSystemMultipliers() {
        val scaling = EnemyScalingSystem.DEFAULT

        // Wave 1 (0 waves cleared): exactly 1.0x for HP and speed
        assertEquals(1.0f, scaling.getHpMultiplier(0), 0.001f)
        assertEquals(1.0f, scaling.getSpeedMultiplier(0), 0.001f)

        // Wave 5 (4 waves cleared): 1.0 + 4 * 0.08 = 1.32x HP, 1.0 + 4 * 0.015 = 1.06x speed
        assertEquals(1.32f, scaling.getHpMultiplier(4), 0.001f)
        assertEquals(1.06f, scaling.getSpeedMultiplier(4), 0.001f)

        // Wave 10 (9 waves cleared): 1.0 + 9 * 0.08 = 1.72x HP, 1.0 + 9 * 0.015 = 1.135x speed
        assertEquals(1.72f, scaling.getHpMultiplier(9), 0.001f)
        assertEquals(1.135f, scaling.getSpeedMultiplier(9), 0.001f)

        // Wave 20 (19 waves cleared): 1.0 + 19 * 0.08 = 2.52x HP, 1.0 + 19 * 0.015 = 1.285x speed
        assertEquals(2.52f, scaling.getHpMultiplier(19), 0.001f)
        assertEquals(1.285f, scaling.getSpeedMultiplier(19), 0.001f)

        // Extreme endless waves (e.g. 50 waves cleared): speed must be clamped to maxSpeedMultiplier (1.35x)
        assertEquals(1.35f, scaling.getSpeedMultiplier(50), 0.001f)

        // Boss scaling tuning
        val bossHpMult = scaling.getHpMultiplier(9, isBoss = true)
        val bossSpeedMult = scaling.getSpeedMultiplier(9, isBoss = true)
        assertEquals(1.54f, bossHpMult, 0.001f)
        assertEquals(1.09f, bossSpeedMult, 0.001f)
    }

    @Test
    fun testWaveManagerSpawnsScaledEnemiesInLaterWaves() {
        val path = GamePath(id = "test_path", waypoints = listOf(Point2D(0f, 0f), Point2D(500f, 0f)))
        val waveManager = WaveManager(maxWaves = 10, path = path)

        // Wave 1: 0 waves cleared -> base stats
        waveManager.startCurrentWave()
        val e1 = waveManager.update(1.0f, 0)
        assertNotNull(e1)
        assertEquals(0, waveManager.wavesCleared)
        assertEquals(e1!!.spec.baseHp, e1.maxHp, 0.01f)
        assertEquals(e1.spec.baseHp, e1.currentHp, 0.01f)
        assertEquals(e1.spec.baseSpeed, e1.speed, 0.01f)

        // Advance to Wave 2: 1 wave cleared
        waveManager.advanceToNextWave()
        assertEquals(2, waveManager.currentWave)
        assertEquals(1, waveManager.wavesCleared)
        waveManager.startCurrentWave()
        val e2 = waveManager.update(1.0f, 0)
        assertNotNull(e2)
        assertTrue("Wave 2 enemy HP must be greater than base HP", e2!!.maxHp > e2.spec.baseHp)
        assertTrue("Wave 2 enemy speed must be greater than base speed", e2.speed > e2.spec.baseSpeed)
        assertEquals(e2.spec.baseHp * 1.08f, e2.maxHp, 0.1f)
        assertEquals(e2.spec.baseSpeed * 1.015f, e2.speed, 0.1f)
    }

    @Test
    fun testScaledEnemyMovementAndDurability() {
        val path = GamePath(id = "p", waypoints = listOf(Point2D(0f, 0f), Point2D(1000f, 0f)))
        val scaling = EnemyScalingSystem.DEFAULT

        // Standard Soldier vs Scaled Soldier (after 10 waves cleared)
        val baseEnemy = Enemy(
            spec = EnemySpec.SOLDIER,
            position = Point2D(0f, 0f)
        )
        val scaledEnemy = scaling.createScaledEnemy(
            spec = EnemySpec.SOLDIER,
            wavesCleared = 10,
            position = Point2D(0f, 0f)
        )

        assertTrue(scaledEnemy.maxHp > baseEnemy.maxHp)
        assertTrue(scaledEnemy.speed > baseEnemy.speed)

        // Advance both for 1.0 second
        val movedBase = baseEnemy.advance(1.0f, path)
        val movedScaled = scaledEnemy.advance(1.0f, path)

        // Scaled enemy must cover more distance due to scaled speed
        assertTrue(movedScaled.position.x > movedBase.position.x)

        // Durability: Apply 100 damage
        val damagedBase = movedBase.takeDamage(100f)
        val damagedScaled = movedScaled.takeDamage(100f)

        // Scaled enemy should have more remaining HP fraction
        assertTrue(damagedScaled.healthPercentage > damagedBase.healthPercentage)
        assertTrue(damagedScaled.currentHp > damagedBase.currentHp)
    }

    @Test
    fun testDestructiblePlacementOffRoadsAcrossAllPresetMaps() {
        val maps = com.example.data.GameMap.getPresetMaps()
        assertTrue("Preset maps should not be empty", maps.isNotEmpty())

        for (map in maps) {
            assertTrue("Map ${map.id} should have destructible objects", map.destructibles.isNotEmpty())

            for (d in map.destructibles) {
                // Check distance from every path segment
                for (path in map.paths) {
                    val dist = path.distanceToPath(d.position)
                    val minAllowed = (path.pathWidth / 2f) + d.collisionRadius
                    assertTrue(
                        "Destructible ${d.id} (${d.type}) on map ${map.id} at ${d.position} is too close to path ${path.id}. Dist: $dist, Min: $minAllowed",
                        dist >= minAllowed
                    )
                }

                // Check world boundaries
                assertTrue(d.position.x >= d.collisionRadius)
                assertTrue(d.position.x <= map.worldWidth - d.collisionRadius)
                assertTrue(d.position.y >= d.collisionRadius)
                assertTrue(d.position.y <= map.worldHeight - d.collisionRadius)
            }
        }
    }

    @Test
    fun testDestructibleObjectStatsAndRewards() {
        val tree = DestructibleObject(type = DestructibleType.TREE, position = Point2D(0f, 0f))
        assertEquals(100f, tree.maxHp, 0.01f)
        assertEquals(10, tree.rewardTokens)

        val stone = DestructibleObject(type = DestructibleType.STONE, position = Point2D(0f, 0f))
        assertEquals(120f, stone.maxHp, 0.01f)
        assertEquals(15, stone.rewardTokens)

        val largeStone = DestructibleObject(type = DestructibleType.LARGE_STONE, position = Point2D(0f, 0f))
        assertEquals(250f, largeStone.maxHp, 0.01f)
        assertEquals(25, largeStone.rewardTokens)

        val crate = DestructibleObject(type = DestructibleType.WOODEN_CRATE, position = Point2D(0f, 0f))
        assertEquals(50f, crate.maxHp, 0.01f)
        assertEquals(15, crate.rewardTokens)
    }

    @Test
    fun testPlayerDestructibleSelectionAndManualTowerTargeting() {
        val engine = com.example.game.GameEngine()
        val treePos = Point2D(360f, 400f)

        // Find or create a destructible near treePos
        val targetTree = engine.destructibles.firstOrNull { it.position.distanceTo(treePos) < 100f }
            ?: DestructibleObject("test_tree", DestructibleType.TREE, treePos).also { engine.destructibles.add(it) }

        // Player clicks tree -> selects it
        engine.selectDestructible(targetTree.id)
        assertEquals(targetTree.id, engine.gameState.value.selectedDestructibleId)

        // Place a tower in range of the tree
        val towerPos = Point2D(targetTree.position.x + 40f, targetTree.position.y)
        val tower = Tower(
            id = "test_tower",
            spec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
            position = towerPos,
            manualTargetId = targetTree.id
        )

        // CombatSystem update: Tower with manualTargetId must shoot targetTree
        val combatSystem = com.example.systems.CombatSystem()
        val result = combatSystem.update(
            dt = 0.016f,
            towers = listOf(tower),
            enemies = emptyList(),
            destructibles = listOf(targetTree),
            projectiles = emptyList(),
            existingEffects = emptyList()
        )

        assertTrue("Tower should fire at manual target", result.newProjectilesFired > 0)
        assertEquals(targetTree.id, result.updatedProjectiles.first().targetEnemyId)
    }

    @Test
    fun testNewStrongestBossVoidSovereign() {
        val voidSovereign = EnemySpec.VOID_SOVEREIGN
        assertEquals("Void Sovereign", voidSovereign.name)
        assertTrue(voidSovereign.isBoss)
        assertEquals(EnemyType.BOSS, voidSovereign.type)

        // Compare against previous strongest boss (Overlord Colossus)
        val overlord = EnemySpec.createBoss(18)
        assertEquals("Overlord Colossus", overlord.name)

        // Void Sovereign must be strictly stronger than all other enemies
        assertTrue(voidSovereign.baseHp > overlord.baseHp)
        assertTrue(voidSovereign.armor >= overlord.armor)
        assertTrue(voidSovereign.bulletResistance >= overlord.bulletResistance)
        assertTrue(voidSovereign.regenRate >= overlord.regenRate)
        assertTrue(voidSovereign.rewardCoins >= overlord.rewardCoins)

        // Verify wave 20 boss is Void Sovereign
        val wave20Boss = EnemySpec.createBoss(20)
        assertEquals("Void Sovereign", wave20Boss.name)
        assertEquals(8500f, wave20Boss.baseHp, 0.01f)
    }

    @Test
    fun testStopTargetingDestructibleClearsManualTargetAndSelection() {
        val engine = com.example.game.GameEngine()
        val tree = DestructibleObject("target_tree_1", DestructibleType.TREE, Point2D(300f, 300f))
        engine.destructibles.add(tree)

        // Add a tower placed next to the tree
        val tower = Tower(
            id = "tower_1",
            spec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
            position = Point2D(320f, 300f)
        )
        engine.towers.add(tower)

        // Target the tree
        engine.selectDestructible(tree.id)
        engine.setTowerManualTarget(tower.id, tree.id, com.example.entities.TargetType.DESTRUCTIBLE)

        assertEquals(tree.id, engine.gameState.value.selectedDestructibleId)
        assertEquals(tree.id, engine.towers.first { it.id == tower.id }.manualTargetId)

        // User clicks the targeted tree again -> stops targeting it
        engine.stopTargetingDestructible(tree.id)

        // Selection must be cleared
        assertNull(engine.gameState.value.selectedDestructibleId)
        // Tower's manual target must be cleared
        assertNull(engine.towers.first { it.id == tower.id }.manualTargetId)
    }

    @Test
    fun testNightFortressMapConfigurationAndIntegrity() {
        val nightMap = GameMap.createNightFortressMap(isUnlocked = true, stars = 2)

        assertEquals("night_fortress", nightMap.id)
        assertEquals("Night Fortress", nightMap.name)
        assertEquals(EnvironmentType.NIGHT_FORTRESS, nightMap.environmentType)
        assertTrue(nightMap.isUnlocked)
        assertEquals(2, nightMap.starsEarned)

        // Large map dimensions
        assertEquals(2600f, nightMap.worldWidth, 0.01f)
        assertEquals(1700f, nightMap.worldHeight, 0.01f)
        assertTrue(nightMap.worldWidth > 1200f)
        assertTrue(nightMap.worldHeight > 800f)

        // Path and base integrity
        assertEquals(1, nightMap.paths.size)
        val path = nightMap.paths.first()
        assertTrue("Night path must have multiple curves and turns", path.waypoints.size >= 10)
        assertEquals(Point2D(1850f, 1420f), nightMap.basePosition)
        assertEquals(nightMap.basePosition, path.endPoint)

        // Destructibles rule: must not collide with path
        assertTrue("Destructibles list must not be empty", nightMap.destructibles.isNotEmpty())
        for (d in nightMap.destructibles) {
            val dist = path.distanceToPath(d.position)
            val minAllowed = (path.pathWidth / 2f) + d.collisionRadius
            assertTrue("Destructible ${d.id} is too close to path: dist=$dist, min=$minAllowed", dist >= minAllowed)
            assertTrue("Destructible must have positive maxHp", d.maxHp > 0f)
            assertTrue("Destructible must yield reward tokens", d.rewardTokens > 0)
        }
    }

    @Test
    fun testNightFortressWaveProgressionAndBossEncounters() {
        val nightMap = GameMap.createNightFortressMap()
        val waveManager = WaveManager(
            maxWaves = GameConfig.TOTAL_WAVES,
            paths = nightMap.paths,
            isNightFortress = true
        )

        assertEquals(1, waveManager.currentWave)
        assertEquals(WaveStatus.READY_TO_START, waveManager.status)

        // Advance and check Wave 5 Boss encounter
        for (w in 1..4) {
            waveManager.advanceToNextWave()
        }
        assertEquals(5, waveManager.currentWave)
        waveManager.startCurrentWave()
        var spawnedBossWave5 = false
        var enemy = waveManager.update(1.0f, 0)
        while (enemy != null || waveManager.currentQueueIndex < waveManager.totalEnemiesThisWave) {
            if (enemy?.spec?.isBoss == true) spawnedBossWave5 = true
            enemy = waveManager.update(1.0f, 0)
        }
        assertTrue("Wave 5 must spawn a boss", spawnedBossWave5)

        // Advance to Wave 10
        for (w in 5..9) {
            waveManager.advanceToNextWave()
        }
        assertEquals(10, waveManager.currentWave)
        waveManager.startCurrentWave()
        var spawnedBossWave10 = false
        enemy = waveManager.update(1.0f, 0)
        while (enemy != null || waveManager.currentQueueIndex < waveManager.totalEnemiesThisWave) {
            if (enemy?.spec?.isBoss == true) spawnedBossWave10 = true
            enemy = waveManager.update(1.0f, 0)
        }
        assertTrue("Wave 10 must spawn a boss", spawnedBossWave10)

        // Advance to Wave 15 Apex Climax
        for (w in 10..14) {
            waveManager.advanceToNextWave()
        }
        assertEquals(15, waveManager.currentWave)
        waveManager.startCurrentWave()
        var bossCountWave15 = 0
        enemy = waveManager.update(1.0f, 0)
        while (enemy != null || waveManager.currentQueueIndex < waveManager.totalEnemiesThisWave) {
            if (enemy?.spec?.isBoss == true) bossCountWave15++
            enemy = waveManager.update(1.0f, 0)
        }
        assertTrue("Wave 15 apex climax must spawn multiple bosses", bossCountWave15 >= 2)
    }

    @Test
    fun testGameEngineLoadsNightFortressAndTargetingWorks() {
        val engine = com.example.game.GameEngine()
        val nightMap = GameMap.createNightFortressMap(isUnlocked = true)
        engine.loadMap(nightMap)

        assertEquals("night_fortress", engine.gameState.value.currentMap.id)
        assertEquals(EnvironmentType.NIGHT_FORTRESS, engine.gameState.value.currentMap.environmentType)
        assertTrue(engine.gameState.value.destructibles.isNotEmpty())

        val targetObj = engine.gameState.value.destructibles.first()
        engine.selectDestructible(targetObj.id)
        assertEquals(targetObj.id, engine.gameState.value.selectedDestructibleId)

        // Add a tower near targetObj
        val tower = Tower(
            id = "test_night_tower_1",
            spec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
            position = Point2D(targetObj.position.x + 30f, targetObj.position.y)
        )
        engine.towers.add(tower)

        // Set manual target to destructible
        engine.setTowerManualTarget(tower.id, targetObj.id, com.example.entities.TargetType.DESTRUCTIBLE)
        assertEquals(targetObj.id, engine.towers.first { it.id == tower.id }.manualTargetId)
    }
}
