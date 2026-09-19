package com.example.systems

import com.example.data.GameConfig
import com.example.entities.Enemy
import com.example.entities.EnemySpec
import com.example.entities.EnemyType
import com.example.entities.GamePath
import kotlin.math.max

enum class WaveStatus {
    READY_TO_START,
    SPAWNING,
    IN_PROGRESS,
    WAVE_CLEARED,
    ALL_WAVES_CLEARED
}

data class WaveSpawnItem(
    val spec: EnemySpec,
    val delayBeforeNextSeconds: Float = 1.0f
)

/**
 * Manages the 20-wave escalation, composition, pacing, and boss encounters.
 */
class WaveManager(
    val maxWaves: Int = GameConfig.TOTAL_WAVES,
    val paths: List<GamePath> = listOf(GamePath()),
    val scalingSystem: EnemyScalingSystem = EnemyScalingSystem.DEFAULT,
    val isSnowValley: Boolean = false,
    val isNightFortress: Boolean = false
) {
    constructor(
        maxWaves: Int = GameConfig.TOTAL_WAVES,
        path: GamePath,
        scalingSystem: EnemyScalingSystem = EnemyScalingSystem.DEFAULT,
        isSnowValley: Boolean = false,
        isNightFortress: Boolean = false
    ) : this(maxWaves, listOf(path), scalingSystem, isSnowValley, isNightFortress)

    val primaryPath: GamePath get() = paths.first()
    var currentWave: Int = 1
        private set

    val wavesCleared: Int get() = (currentWave - 1).coerceAtLeast(0)
    val currentHpMultiplier: Float get() = scalingSystem.getHpMultiplier(wavesCleared)
    val currentSpeedMultiplier: Float get() = scalingSystem.getSpeedMultiplier(wavesCleared)

    var status: WaveStatus = WaveStatus.READY_TO_START
        private set

    private var currentWaveQueue: List<WaveSpawnItem> = emptyList()
    var currentQueueIndex: Int = 0
        private set
    private var spawnCooldown: Float = 0f

    var enemiesSpawnedThisWave: Int = 0
        private set

    var totalEnemiesThisWave: Int = 0
        private set

    var nextWaveCountdown: Float = 0f
        private set

    init {
        setupWave(1)
    }

    private fun setupWave(wave: Int) {
        currentWaveQueue = when {
            isNightFortress -> generateNightFortressWaveRoster(wave)
            isSnowValley -> generateSnowValleyWaveRoster(wave)
            else -> generateWaveRoster(wave)
        }
        totalEnemiesThisWave = currentWaveQueue.size
        currentQueueIndex = 0
        enemiesSpawnedThisWave = 0
        spawnCooldown = 0.5f
    }

    private fun generateWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            1 -> {
                // Wave 1: Intro Scouts
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.2f)) }
            }
            2 -> {
                // Wave 2: Scouts + first Soldiers
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.1f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.3f)) }
            }
            3 -> {
                // Wave 3: Mostly Soldiers
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.9f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.2f)) }
            }
            4 -> {
                // Wave 4: First agile Runners
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.8f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.1f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
            }
            5 -> {
                // Wave 5: BOSS 1 - Iron Golem + Escorts
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.2f)) }
            }
            6 -> {
                // Wave 6: First Armored Heavies
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.6f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
            }
            7 -> {
                // Wave 7: Runner rush
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.55f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.4f)) }
            }
            8 -> {
                // Wave 8: Heavy brigade
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.5f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            9 -> {
                // Wave 9: Pre-boss swarm
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            10 -> {
                // Wave 10: BOSS 2 - Siege Titan
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.5f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            11 -> {
                // Wave 11: High-speed infiltration
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
            }
            12 -> {
                // Wave 12: Combined arms
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            13 -> {
                // Wave 13: Iron march
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.4f)) }
            }
            14 -> {
                // Wave 14: Overwhelming tide
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            15 -> {
                // Wave 15: BOSS 3 - Dreadnought
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            16 -> {
                // Wave 16: Elite runners & heavies
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            17 -> {
                // Wave 17: Relentless assault
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
            }
            18 -> {
                // Wave 18: Vanguard invasion
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            19 -> {
                // Wave 19: The Calm before the storm
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.5f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            else -> {
                // Wave 20: FINAL SHOWDOWN - Vanguard Overlord Colossus followed by the APEX BOSS: Void Sovereign!
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 4.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
        }
        return list
    }

    private fun generateSnowValleyWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            1 -> {
                // Wave 1: Fast snow wolf scouts probed with vanguard soldiers
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.0f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.2f)) }
            }
            2 -> {
                // Wave 2: Agile Runners slipping between soldiers
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.8f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
            }
            3 -> {
                // Wave 3: Early armored heavies testing choke points
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.5f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
            }
            4 -> {
                // Wave 4: Tough armored squad
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            5 -> {
                // Wave 5: BOSS 1 (Iron Golem) escorted by rapid runners
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
            }
            6 -> {
                // Wave 6: Heavy vanguard column
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            7 -> {
                // Wave 7: Swift flankers followed by siege heavies
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
            }
            8 -> {
                // Wave 8: Mixed assault battalion
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            9 -> {
                // Wave 9: Shielded siege march
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.6f)) }
            }
            10 -> {
                // Wave 10: BOSS 2 (Shadow Stalker) with heavy escorts
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            11 -> {
                // Wave 11: High-speed blizzard rush
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
            12 -> {
                // Wave 12: Heavy armored assault
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.5f)) }
            }
            13 -> {
                // Wave 13: Mixed blitz
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            14 -> {
                // Wave 14: Overwhelming force
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.4f)) }
            }
            15 -> {
                // Wave 15: BOSS 3 (Mech Behemoth) accompanied by armored guard
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
            16 -> {
                // Wave 16: Relentless assault
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            17 -> {
                // Wave 17: Multi-tiered breach
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.4f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            18 -> {
                // Wave 18: Armored vanguard flood
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            19 -> {
                // Wave 19: The Grand Blizzard Army
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.35f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
            }
            else -> {
                // Wave 20: APEX CLIMAX - Dual Boss Siege in Snow Valley
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 4.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    private fun generateNightFortressWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            1 -> {
                // Wave 1: Shadow Infiltrators - Fast night scouts probing the perimeter
                repeat(7) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.9f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.1f)) }
            }
            2 -> {
                // Wave 2: Agile Runners slipping between soldiers
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
            }
            3 -> {
                // Wave 3: Early armored heavies testing the winding choke points
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.4f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
            }
            4 -> {
                // Wave 4: Mixed assault battalion
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            5 -> {
                // Wave 5: BOSS 1 (Night Warden) escorted by agile shadow runners
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f))
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
            }
            6 -> {
                // Wave 6: High density phalanx
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            7 -> {
                // Wave 7: Shadow Blitz - Rapid runners weaving through heavy shields
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            8 -> {
                // Wave 8: Armored vanguard column
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.6f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
            }
            9 -> {
                // Wave 9: Iron Siege Column
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.55f)) }
            }
            10 -> {
                // Wave 10: BOSS 2 (Nocturnal Colossus) with heavy escorts
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
            }
            11 -> {
                // Wave 11: High-speed stealth rush
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
            12 -> {
                // Wave 12: Heavy armored breach force
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.75f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.45f)) }
            }
            13 -> {
                // Wave 13: Midnight Incursion
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
            14 -> {
                // Wave 14: Relentless assault force
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.4f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            else -> {
                // Wave 15: APEX CLIMAX - Dual Boss Siege on Night Fortress!
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 4.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    fun startCurrentWave() {
        if (status == WaveStatus.READY_TO_START) {
            status = WaveStatus.SPAWNING
            spawnCooldown = 0.3f
            nextWaveCountdown = 0f
        } else if (status == WaveStatus.WAVE_CLEARED) {
            advanceToNextWave()
        }
    }

    fun reset() {
        currentWave = 1
        setupWave(1)
        status = WaveStatus.READY_TO_START
        spawnCooldown = 0f
        nextWaveCountdown = 0f
    }

    fun update(
        dt: Float,
        aliveEnemyCount: Int,
        autoStartNextWave: Boolean = false
    ): Enemy? {
        var spawnedEnemy: Enemy? = null

        when (status) {
            WaveStatus.READY_TO_START -> {}

            WaveStatus.SPAWNING -> {
                spawnCooldown = max(0f, spawnCooldown - dt)
                if (spawnCooldown <= 0f && currentQueueIndex < currentWaveQueue.size) {
                    val item = currentWaveQueue[currentQueueIndex]
                    val pathIdx = currentQueueIndex % paths.size
                    val assignedPath = paths[pathIdx]
                    val p1 = assignedPath.waypoints.getOrNull(0) ?: assignedPath.startPoint
                    val p2 = assignedPath.waypoints.getOrNull(1) ?: p1
                    val initAngle = (kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble()) * 180.0 / Math.PI).toFloat()
                    spawnedEnemy = scalingSystem.createScaledEnemy(
                        spec = item.spec,
                        wavesCleared = wavesCleared,
                        pathIndex = pathIdx,
                        position = assignedPath.startPoint,
                        headingAngle = initAngle
                    )
                    currentQueueIndex++
                    enemiesSpawnedThisWave = currentQueueIndex
                    spawnCooldown = item.delayBeforeNextSeconds

                    if (currentQueueIndex >= currentWaveQueue.size) {
                        status = WaveStatus.IN_PROGRESS
                    }
                }
            }

            WaveStatus.IN_PROGRESS -> {
                if (aliveEnemyCount == 0 && currentQueueIndex >= currentWaveQueue.size) {
                    if (currentWave >= maxWaves) {
                        status = WaveStatus.ALL_WAVES_CLEARED
                    } else {
                        status = WaveStatus.WAVE_CLEARED
                        nextWaveCountdown = 0f
                    }
                }
            }

            WaveStatus.WAVE_CLEARED -> {
                if (autoStartNextWave) {
                    nextWaveCountdown = max(0f, nextWaveCountdown - dt)
                    if (nextWaveCountdown <= 0f) {
                        advanceToNextWave()
                    }
                }
            }

            WaveStatus.ALL_WAVES_CLEARED -> {}
        }

        return spawnedEnemy
    }

    fun advanceToNextWave(): Boolean {
        if (currentWave < maxWaves) {
            currentWave++
            setupWave(currentWave)
            status = WaveStatus.SPAWNING
            spawnCooldown = 0.3f
            nextWaveCountdown = 0f
            return true
        }
        return false
    }
}
