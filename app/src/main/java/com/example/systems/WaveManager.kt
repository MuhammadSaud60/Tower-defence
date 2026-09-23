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
    val isNightFortress: Boolean = false,
    val isDayNight: Boolean = false,
    val isTempestRain: Boolean = false,
    val isCloudyForest: Boolean = false,
    val mapId: String = ""
) {
    constructor(
        maxWaves: Int = GameConfig.TOTAL_WAVES,
        path: GamePath,
        scalingSystem: EnemyScalingSystem = EnemyScalingSystem.DEFAULT,
        isSnowValley: Boolean = false,
        isNightFortress: Boolean = false,
        isDayNight: Boolean = false,
        isTempestRain: Boolean = false,
        isCloudyForest: Boolean = false,
        mapId: String = ""
    ) : this(maxWaves, listOf(path), scalingSystem, isSnowValley, isNightFortress, isDayNight, isTempestRain, isCloudyForest, mapId)

    val primaryPath: GamePath get() = paths.first()
    var currentWave: Int = 1
        private set

    val wavesCleared: Int get() = (currentWave - 1).coerceAtLeast(0)
    val currentHpMultiplier: Float get() = scalingSystem.getHpMultiplier(wavesCleared)
    val currentSpeedMultiplier: Float get() = scalingSystem.getSpeedMultiplier(wavesCleared)

    var status: WaveStatus = WaveStatus.READY_TO_START
        private set

    var currentWaveQueue: List<WaveSpawnItem> = emptyList()
        private set
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

    private fun getRosterForWave(wave: Int): List<WaveSpawnItem> {
        return when {
            mapId == "desert_dune_bastion" || mapId == "dune_storm_stronghold" -> generateDesertDuneBastionWaveRoster(wave)
            mapId == "snow_summit_descent" || mapId == "frostpeak_descent" -> generateSnowSummitWaveRoster(wave)
            isCloudyForest || mapId == "cloudy_dense_forest" || mapId == "cloudy_forest" -> generateCloudyForestWaveRoster(wave)
            isTempestRain || mapId == "storm_twin_bastion" || mapId == "tempest_twins" -> generateTempestBastionWaveRoster(wave)
            isDayNight || mapId == "eclipse_frontier" || mapId == "solstice_frontier" -> generateEclipseFrontierWaveRoster(wave)
            mapId == "snow_outpost" -> generateSnowOutpostWaveRoster(wave)
            mapId == "frozen_valley" || mapId == "snow_valley" || (isSnowValley && mapId.isEmpty()) -> generateFrozenValleyWaveRoster(wave)
            mapId == "ice_mountain" -> generateIceMountainWaveRoster(wave)
            mapId == "frozen_fortress" -> generateFrozenFortressWaveRoster(wave)
            mapId == "arctic_base" -> generateArcticBaseWaveRoster(wave)
            isNightFortress || mapId == "night_fortress" -> generateNightFortressWaveRoster(wave)
            else -> generateWaveRoster(wave)
        }
    }

    private fun setupWave(wave: Int) {
        currentWaveQueue = getRosterForWave(wave)
        totalEnemiesThisWave = currentWaveQueue.size
        currentQueueIndex = 0
        enemiesSpawnedThisWave = 0
        spawnCooldown = 0.5f
    }

    /**
     * Checks if a particular wave contains any boss encounter.
     */
    fun isBossWave(wave: Int): Boolean {
        return getRosterForWave(wave).any { it.spec.isBoss }
    }

    /**
     * Returns the name of the upcoming boss for a wave, or null if none.
     * Accurately reflects multi-boss encounters in late climax waves.
     */
    fun getUpcomingBossName(wave: Int): String? {
        val bossItems = getRosterForWave(wave).filter { it.spec.isBoss }
        val distinctBossNames = bossItems.map { it.spec.name }.distinct()
        val totalBosses = bossItems.size
        return when {
            distinctBossNames.isEmpty() -> null
            totalBosses == 1 -> distinctBossNames.first()
            totalBosses == 2 && distinctBossNames.size == 2 -> "${distinctBossNames[0]} & ${distinctBossNames[1]}"
            totalBosses == 2 && distinctBossNames.size == 1 -> "Twin ${distinctBossNames[0]}s"
            totalBosses in 3..4 && distinctBossNames.size <= 3 -> distinctBossNames.joinToString(", ")
            totalBosses >= 10 -> "10 APEX BOSSES: ${distinctBossNames.take(2).joinToString(" & ")} + 8 More!"
            else -> "$totalBosses Bosses: ${distinctBossNames.take(2).joinToString(" & ")} + more"
        }
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
                // Wave 7: First Energy Shield Vanguards + Runner rush
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.2f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.55f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.4f)) }
            }
            8 -> {
                // Wave 8: First Aerial Sky Drakes (Flying) + Heavy brigade
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 1.1f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.5f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
            }
            9 -> {
                // Wave 9: Pre-boss swarm with shields and flyers
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            10 -> {
                // Wave 10: BOSS 2 - Siege Titan with Shield Escorts
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.5f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            11 -> {
                // Wave 11: First Shadow Infiltrators (Stealth) + high-speed rush
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.8f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
            }
            12 -> {
                // Wave 12: First Field Medics (Healing Aura) + Combined arms
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.5f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
            }
            13 -> {
                // Wave 13: Iron march with Shields and Medics
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.4f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.7f)) }
            }
            14 -> {
                // Wave 14: First Void Summoners + Overwhelming tide
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 2.0f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.6f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
            }
            15 -> {
                // Wave 15: BOSS 3 - Dreadnought supported by Summoner and Medics
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 2.0f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            16 -> {
                // Wave 16: Elite stealth, flyers & heavies
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.6f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            17 -> {
                // Wave 17: Relentless assault with all unit classes
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
            }
            18 -> {
                // Wave 18: Vanguard invasion with Summoners & Stealth
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            19 -> {
                // Wave 19: Pre-Climax Blitz with Dreadnought Vanguard
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(14), 2.5f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            else -> {
                // Wave 20: FINAL SHOWDOWN - TRIPLE BOSS ASSAULT: Dreadnought + Overlord Colossus + Void Sovereign!
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.5f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.4f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 4.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    private fun generateSnowValleyWaveRoster(wave: Int): List<WaveSpawnItem> =
        generateFrozenValleyWaveRoster(wave)

    // ========================================================
    // SNOW WORLD LEVEL 1: SNOW OUTPOST (12 Waves)
    // Mini-Boss: Wave 6 (Ice Beast) | Climax Boss: Wave 12 (Ice Golem)
    // Introduces Shield Vanguards, Sky Drakes, and Field Medics
    // ========================================================
    private fun generateSnowOutpostWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()
        when (wave) {
            1 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.1f)) }
            }
            2 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.9f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.1f)) }
            }
            3 -> {
                // Swarm Wave (Fast Runners with Speed Burst)
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.75f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
            }
            4 -> {
                // First Shield Vanguards testing frontier defenses
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.3f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.4f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
            }
            5 -> {
                // Mixed Vanguard
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            6 -> {
                // MINI-BOSS 1: Ice Beast (Fast predator with speed burst ability)
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.5f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.2f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
            }
            7 -> {
                // First Aerial Sky Drakes flying across the snowy pass
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 1.0f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
            }
            8 -> {
                // Speed Blitz with Shield Support
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
            }
            9 -> {
                // First Field Medics healing the armored front lines
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.5f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            10 -> {
                // Siege Column with Medics and Flyers
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
            }
            11 -> {
                // Pre-Climax Swarm
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            else -> {
                // Wave 12: CLIMAX BOSS: Ice Golem (Armored juggernaut ground slam) + Shield & Medic Escorts
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(1), 3.0f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.5f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
        }
        return list
    }

    // ========================================================
    // SNOW WORLD LEVEL 2: FROZEN VALLEY (18 Waves)
    // Mini-Boss: Wave 6 (Ice Beast) | Wave 12 (Frozen Commander) | Climax: Wave 18 (Dual Titan)
    // Introduces Stealth Infiltrators, Shield Phalanxes, and Flying Sky Drakes
    // ========================================================
    private fun generateFrozenValleyWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()
        when (wave) {
            1 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.0f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.2f)) }
            }
            2 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.8f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.0f)) }
            }
            3 -> {
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.3f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.4f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.7f)) }
            }
            4 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
            }
            5 -> {
                // First Shadow Infiltrators cloaking through the snowdrifts
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.75f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.55f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
            }
            6 -> {
                // MINI-BOSS 1: Ice Beast with stealth and speed escorts
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.5f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.2f)) }
            }
            7 -> {
                // Flying Sky Drakes over the frozen valley
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
            }
            8 -> {
                // Speed Blitz with Stealth Infiltrators
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.65f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            9 -> {
                // Field Medics supporting shielded phalanx
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
            }
            10 -> {
                // Stealth Infiltrators + Flying Drake pincer
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.6f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            11 -> {
                // First Void Summoner conjuring minions
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 2.0f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.6f)) }
            }
            12 -> {
                // MINI-BOSS 2: Frozen Commander (Regenerative tactical commander) + Field Medic escorts
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(1), 2.8f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.3f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
            13 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.38f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
            }
            14 -> {
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
            }
            15 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
            16 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            17 -> {
                // Pre-Climax: Combined aerial & stealth assault with Ice Beast Vanguard
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.2f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            else -> {
                // Wave 18: CLIMAX TRIPLE TITAN: Ice Beast Alpha + Glacial Warlord + Frost Colossus!
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.2f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.5f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    // ========================================================
    // SNOW WORLD LEVEL 3: ICE MOUNTAIN (24 Waves)
    // Mini-Boss: Wave 8 (Ice Beast Alpha) | Wave 16 (Frozen Commander) |
    // Dual Boss: Wave 20 | Climax: Wave 24 (Frost Colossus Behemoth)
    // Prominently features Aerial Sky Drakes, Void Summoners, and Field Medics
    // ========================================================
    private fun generateIceMountainWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()
        when (wave) {
            1 -> {
                repeat(7) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 1.0f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 1.1f)) }
            }
            2 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.75f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.9f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.2f)) }
            }
            3 -> {
                // First Mountain Drake squadron (Flying)
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 1.0f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.3f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.6f)) }
            }
            4 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
            }
            5 -> {
                // Stealth Infiltrators scaling peaks
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
            }
            6 -> {
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.3f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
            }
            7 -> {
                // Sky Drake swarm + Speed rush
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            8 -> {
                // MINI-BOSS 1: Ice Beast Alpha with Drake & Shield escort
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.5f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
            }
            9 -> {
                // First Void Summoner conjuring on the peaks
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 2.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.65f)) }
            }
            10 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
            }
            11 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.55f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.85f)) }
            }
            12 -> {
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.7f)) }
            }
            13 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            14 -> {
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.75f)) }
            }
            15 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
            }
            16 -> {
                // MINI-BOSS 2: Frozen Commander (Regen aura) + Field Medics + Flying Drakes
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(1), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
            17 -> {
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.45f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.75f)) }
            }
            18 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.5f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.55f)) }
            }
            19 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.45f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            20 -> {
                // DUAL MINI-BOSSES: Ice Beast Alpha + Glacial Warlord
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
            21 -> {
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.4f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
            22 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.65f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.4f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            23 -> {
                // Wave 23: Dual Boss Incursion before climax
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.4f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
            }
            else -> {
                // Wave 24: APEX CLIMAX: TRIPLE BOSS STRIKE - Ice Beast Alpha + Glacial Warlord + Frost Colossus!
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.6f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.5f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    // ========================================================
    // SNOW WORLD LEVEL 4: FROZEN FORTRESS (24 Waves)
    // Multi-Boss: Wave 10, Wave 18, Wave 23 (Dual) | Climax: Wave 24 (TRIPLE BOSS)
    // Dual-lane fortress assault with full combined arms forces
    // ========================================================
    private fun generateFrozenFortressWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()
        when (wave) {
            in 1..9 -> {
                val scale = wave
                repeat(4 + scale) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(1 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.9f)) }
                repeat(3 + scale) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.55f)) }
            }
            10 -> {
                // BOSS 1: Ice Golem Citadel Breaker with Shield & Medic Escorts
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(1), 2.5f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.3f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.0f)) }
            }
            in 11..17 -> {
                val scale = wave - 10
                repeat(3 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                repeat(4 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.9f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.85f)) }
                repeat(4 + scale) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.55f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.65f)) }
            }
            18 -> {
                // BOSS 2: Glacial Warlord (Summoner Commander) + Healers
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
            }
            in 19..22 -> {
                val scale = wave - 18
                repeat(4 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(4 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.5f)) }
                repeat(8 + scale) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.55f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
            }
            23 -> {
                // DUAL BOSS STRIKE: Ice Beast Alpha + Glacial Warlord
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
            else -> {
                // Wave 24: APEX CLIMAX: TRIPLE BOSS ASSAULT - Ice Beast Alpha + Frost Colossus + Arctic Machine!
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.0f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 3.5f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.5f)) }
            }
        }
        return list
    }

    // ========================================================
    // SNOW WORLD LEVEL 5: ARCTIC BASE (25 Waves)
    // Multi-Boss: Wave 8, 15, 22 (Dual), 24 (Triple) | Ultimate Climax: Wave 25 (QUAD BOSS)
    // The supreme 25-wave campaign endurance mission on the polar shelf
    // ========================================================
    private fun generateArcticBaseWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()
        when (wave) {
            in 1..7 -> {
                val scale = wave
                repeat(4 + scale) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(1 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.1f)) }
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 1.1f)) }
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.85f)) }
                repeat(4 + scale) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.5f)) }
            }
            8 -> {
                // BOSS 1: Ice Beast Pack Leader with aerial hunters
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.5f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.9f)) }
            }
            in 9..14 -> {
                val scale = wave - 8
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.8f)) }
                repeat(2 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.2f)) }
                repeat(4 + scale) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.85f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.8f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
            }
            15 -> {
                // BOSS 2: Glacial Warlord + Medics & Shield phalanx
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.6f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
            }
            in 16..21 -> {
                val scale = wave - 15
                repeat(3 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.5f)) }
                repeat(3 + scale / 2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(8 + scale) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(6 + scale) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8 + scale) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.45f)) }
            }
            22 -> {
                // DUAL BOSS STRIKE: Frost Colossus & Glacial Warlord
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.65f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.5f))
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.1f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
            23 -> {
                // DUAL BOSS INVASION: Ice Beast Alpha & Polar Dreadnought
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 3.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 1.0f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
            24 -> {
                // TRIPLE BOSS ONSLAUGHT: Ice Beast Alpha + Frost Colossus + Glacial Warlord
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.4f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.9f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
            }
            else -> {
                // Wave 25: ULTIMATE CAMPAIGN CLIMAX: QUAD BOSS APEX BATTLE!
                // Ice Beast Alpha + Frost Colossus + Polar Dreadnought + Void Arctic Overlord!
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.4f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.2f))
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.9f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.4f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 4.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.35f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.5f)) }
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
                // Wave 14: Relentless assault force with Vanguard Boss
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 2.5f))
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.4f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.3f)) }
            }
            else -> {
                // Wave 15: APEX CLIMAX - TRIPLE BOSS SIEGE on Night Fortress!
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(14), 2.5f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 4.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
            }
        }
        return list
    }

    /**
     * Eclipse Frontier: Special 20-Wave Day & Night Sector.
     * Waves 1-10: Sunlit Day assault.
     * Waves 11-20: Treacherous Midnight Siege.
     * High Difficulty Rule: From Wave 9 onwards, ONLY Bosses and fast enemies (Runners, Flying, Stealth) spawn!
     * Climax: Waves feature multiple bosses simultaneously, culminating in an extraordinary 10-boss final siege on Wave 20!
     */
    private fun generateEclipseFrontierWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            // ==========================================
            // DAY PHASE (WAVES 1 - 10)
            // ==========================================
            1 -> {
                // Wave 1 (Day): Fast reconnaissance
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.8f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            2 -> {
                // Wave 2 (Day): Shield vanguard & runners
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.7f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.8f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.4f)) }
            }
            3 -> {
                // Wave 3 (Day): Armored push
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.75f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.6f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.38f)) }
            }
            4 -> {
                // Wave 4 (Day): Aerial invasion
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.55f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.7f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            5 -> {
                // Wave 5 (Day): First Day Boss - Iron Golem
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(6), 2.5f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.7f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            6 -> {
                // Wave 6 (Day): Shadow infiltrators & aerial drakes
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.5f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            7 -> {
                // Wave 7 (Day): Void summoners & combat medics
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.2f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.9f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.65f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
            }
            8 -> {
                // Wave 8 (Day Climax): Dual Boss Assault (Siege Titan + Ice Beast)
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.6f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(8), 2.5f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.5f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.6f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
            }

            // =========================================================================
            // HIGH DIFFICULTY SPEC: FROM WAVE 9 ONWARDS, ONLY BOSSES & FAST ENEMIES!
            // ZERO SMALL ENEMIES (NO SOLDIER/GRUNTS) — ONLY RUNNERS, FLYING, STEALTH & MULTI-BOSSES!
            // =========================================================================
            9 -> {
                // Wave 9 (Dusk Transition): Dual Boss + Supersonic Rush
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.38f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.38f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
            }
            10 -> {
                // Wave 10 (High Noon Final Day Wave): Triple Boss Confrontation
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.35f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.5f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(1), 2.2f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.35f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
            }

            // ==========================================
            // NIGHT PHASE (WAVES 11 - 20)
            // ==========================================
            11 -> {
                // Wave 11 (Nightfall Arrives): Triple Midnight Bosses
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.32f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 2.6f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.32f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
            }
            12 -> {
                // Wave 12 (Night): 4 Bosses + Fast Predators
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.32f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 2.6f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.32f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.25f)) }
            }
            13 -> {
                // Wave 13 (Night): 4 Bosses + Fast Swarm
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.30f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.25f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.30f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.25f)) }
            }
            14 -> {
                // Wave 14 (Midnight Rampage): 5 Bosses
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.24f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.8f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.24f)) }
            }
            15 -> {
                // Wave 15 (Midnight Incursion): 5 Bosses with Arctic Siege Machine
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.24f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.6f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.28f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.22f)) }
            }
            16 -> {
                // Wave 16 (Shadow Cataclysm): 6 Bosses
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.26f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.22f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.26f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.2f))
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.22f)) }
            }
            17 -> {
                // Wave 17 (Apex Midnight Siege): 6 Heavy Bosses
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.22f)) }
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.4f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.25f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.2f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.25f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
            }
            18 -> {
                // Wave 18 (Nightfall Annihilation): 7 Bosses
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.2f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.24f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.24f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.2f))
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
            }
            19 -> {
                // Wave 19 (Pre-Apex Cataclysm): 8 Bosses
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.2f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.22f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.22f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 3.8f))
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
            }
            else -> {
                // =========================================================================
                // WAVE 20: THE ULTIMATE 10-BOSS MIDNIGHT APEX CONFRONTATION
                // EXACTLY 10 BOSSES ATTACKING ALONGSIDE HIGH-SPEED AIR & GROUND INTERCEPTORS!
                // =========================================================================
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
                // Boss 1: Agile Cryo Striker
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.22f)) }
                // Boss 2: Cryo Tactical Warlord
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.2f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.22f)) }
                // Boss 3: Swift Stalker
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
                // Boss 4: Frost Juggernaut
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.22f)) }
                // Boss 5: Polar Mechanical Siege Engine
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.22f)) }
                // Boss 6: Vanguard Glacial Commander
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.2f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
                // Boss 7: Heavy Frost Colossus
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.6f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.22f)) }
                // Boss 8: Polar Mechanical Dreadnought
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.0f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.20f)) }
                // Boss 9: Cosmic Void Sovereign
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 3.5f))
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
                // Boss 10: The Apex Polar Legend - Void Arctic Overlord!
                list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 4.2f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.16f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.20f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.20f)) }
            }
        }

        return list
    }

    /**
     * Tempest Bastion: 15 waves under perpetual heavy rain.
     * Dual path assault on North & South Bastion Towers.
     * After Wave 7 (Waves 8-15), ONLY BOSSES attack!
     */
    private fun generateTempestBastionWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            // Wave 1: Vanguard scouts probing both North and South bastion roads
            1 -> {
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.70f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
            }
            // Wave 2: Heavy shield wall advancing under downpour
            2 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.60f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.65f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.38f)) }
            }
            // Wave 3: Armored phalanx & storm infiltrators
            3 -> {
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.60f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
            }
            // Wave 4: Tempest flying drakes soaring through lightning & cloaked shadowstalkers
            4 -> {
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.45f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
            }
            // Wave 5: Dual Iron Golems split between North and South bastion paths
            5 -> {
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f)) // North Bastion Golem
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.60f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f)) // South Bastion Golem
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
            }
            // Wave 6: Dark ritual summoners & battle medics sustaining the storm vanguard
            6 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.80f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.50f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
            }
            // Wave 7: Grand Final Mixed Incursion before all-boss tempest surge
            7 -> {
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.50f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.8f)) // Siege Titan 1
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.40f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.8f)) // Siege Titan 2
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
            }

            // =========================================================================
            // WAVES 8-15: AFTER WAVE 7, ONLY BOSSES ATTACKING!
            // Every single entity spawned has isBoss = true.
            // =========================================================================

            // Wave 8: Dual Dreadnoughts (1 North Path, 1 South Path)
            8 -> {
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 3.2f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 3.2f))
            }

            // Wave 9: Frost Colossus & Glacial Warlord Trio
            9 -> {
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.0f))
            }

            // Wave 10: Quad Iron Golems & Dreadnought Titans
            10 -> {
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.2f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.5f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.2f))
            }

            // Wave 11: Rapid Ice Beast Alphas & Armored Polar Dreadnoughts
            11 -> {
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.2f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 3.2f))
            }

            // Wave 12: Pentad Overlords - 5 Colossi & Commanders
            12 -> {
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 3.0f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 3.0f))
            }

            // Wave 13: Hexa Siege Dreadnoughts - 6 Mechanical Titans
            13 -> {
                repeat(3) {
                    list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 3.0f))
                    list.add(WaveSpawnItem(EnemySpec.createBoss(15), 3.0f))
                }
            }

            // Wave 14: Octo Titan Blitz - 8 Colossi pounding both Bastions
            14 -> {
                repeat(4) {
                    list.add(WaveSpawnItem(EnemySpec.createBoss(18), 2.8f))
                    list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 2.8f))
                }
            }

            // Wave 15: Cataclysmic Tempest Apex - 10 APEX BOSSES CONVERGENCE
            15 -> {
                // Boss 1 & 2: Cosmic Void Sovereign + Polar Void Overlord (North & South gates)
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 3.5f))
                list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 3.5f))
                // Boss 3 & 4: Overlord Colossus + Polar Dreadnought
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 2.8f))
                // Boss 5 & 6: Glacial Commander + Void Sovereign
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.6f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(20), 3.2f))
                // Boss 7 & 8: Polar Void Overlord + Overlord Colossus
                list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 3.2f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(18), 2.8f))
                // Boss 9 & 10: Polar Dreadnought + Glacial Commander
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 2.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.6f))
            }
        }

        return list
    }

    /**
     * Level 16: Cloudy Grove (15 Waves)
     * Overcast dense forest with 3 paths merging at the central junction before heading into the tunnel.
     * Round-robin spawns across North, Center, and South woodland trails.
     */
    private fun generateCloudyForestWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            // Wave 1: Woodland scouts testing all 3 trails
            1 -> {
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.70f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.65f)) }
            }
            // Wave 2: Quick woodland runners & soldiers
            2 -> {
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.60f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.42f)) }
            }
            // Wave 3: Heavy forest stone-carvers & soldiers
            3 -> {
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.65f)) }
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.55f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.38f)) }
            }
            // Wave 4: Shield wall advancing down the 3 paths to merge
            4 -> {
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.60f)) }
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
            }
            // Wave 5: First Forest Goliath Boss leading an armored vanguard
            5 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.50f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(5), 2.4f)) // Ancient Grove Sentinel
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.55f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
            }
            // Wave 6: Flying canopy drakes & stealth shadow-assassins darting for the tunnel
            6 -> {
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.50f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.48f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
            }
            // Wave 7: Forest shamans (Summoners) and woodland healers
            7 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.90f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.85f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
            }
            // Wave 8: Dual Forest Titan Bosses (one on North, one on South, merging at center)
            8 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.55f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(8), 2.2f))
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.55f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(8), 2.2f))
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.35f)) }
            }
            // Wave 9: Aerial dreadnought swarm + heavy phalanx
            9 -> {
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.42f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.50f)) }
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.50f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
            }
            // Wave 10: Triple Boss Convergence! One Boss spawns on each of the 3 paths simultaneously!
            10 -> {
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.0f)) // North Trail Titan
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.0f)) // Center Trail Titan
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 2.0f)) // South Trail Titan
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.70f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.25f)) }
            }
            // Wave 11: High-tier stealth stalkers & high speed sprint
            11 -> {
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.38f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.38f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.45f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.22f)) }
            }
            // Wave 12: Summoner Elders & Regenerating Vanguard
            12 -> {
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.80f)) }
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.75f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 2.2f))
            }
            // Wave 13: Quadruple Colossus assault converging through the tunnel
            13 -> {
                repeat(4) {
                    list.add(WaveSpawnItem(EnemySpec.createBoss(13), 2.2f))
                    repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.45f)) }
                    repeat(4) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.45f)) }
                }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.30f)) }
            }
            // Wave 14: Six Apex Bosses charging across all 3 trails
            14 -> {
                repeat(6) {
                    list.add(WaveSpawnItem(EnemySpec.createBoss(14), 2.0f))
                    repeat(3) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.35f)) }
                }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
            }
            // Wave 15: Grand Final Wave - Overlord of the Ancient Woods & 8 Boss Lieutenants
            15 -> {
                // Initial heavy vanguard
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.40f)) }
                repeat(9) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.40f)) }

                // 8 Boss Lieutenants marching down all 3 paths
                repeat(8) {
                    list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.2f))
                    repeat(2) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.60f)) }
                }

                // Supreme Boss: Ancient Overlord Colossus
                list.add(WaveSpawnItem(EnemySpec.createBoss(19), 3.0f))
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.30f)) }
                repeat(15) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
            }
        }

        return list
    }

    /**
     * Specialized 20-wave mountain descent progression for Frostpeak Descent (Snow Mountains, Day).
     *
     * Exact user requirements:
     * - Total waves: 20
     * - Waves 1 to 5: Fast enemies and small enemies in large amount
     * - Waves 6 to 15: Medium enemies + bosses
     * - Waves 16 to 20: Only bosses in large amount
     */
    private fun generateSnowSummitWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            // =========================================================================
            // FIRST 5 WAVES: Fast enemies & small enemies in large amount
            // =========================================================================
            1 -> {
                // Wave 1: Fast runner vanguard & scouts (32 total units)
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.32f)) }
            }
            2 -> {
                // Wave 2: Fast runners, swift scouts & flying air swarms (48 total units)
                repeat(26) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.30f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.32f)) }
            }
            3 -> {
                // Wave 3: Massive rush of agile runners, scouts & flyers (62 total units)
                repeat(32) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.24f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.26f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.28f)) }
            }
            4 -> {
                // Wave 4: Blistering mountain descent swarm (78 total units)
                repeat(40) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.20f)) }
                repeat(22) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.22f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.25f)) }
            }
            5 -> {
                // Wave 5: Ultimate fast & small enemy swarm avalanche (98 total units)
                repeat(50) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.18f)) }
                repeat(28) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.20f)) }
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.22f)) }
            }

            // =========================================================================
            // NEXT 10 WAVES: Medium enemies + bosses
            // =========================================================================
            6 -> {
                // Wave 6: Medium infantry battalion + Iron Golem Boss
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.45f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.50f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(6), 1.8f))
            }
            7 -> {
                // Wave 7: Shielded lines, summoners + Ice Golem Boss
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.40f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.42f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.50f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(1), 2.0f))
            }
            8 -> {
                // Wave 8: Heavy armored advance + Siege Titan Boss
                repeat(22) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.40f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.40f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.50f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(8), 2.0f))
            }
            9 -> {
                // Wave 9: Fortified medium columns + 2 Bosses (Ice Golem & Commander)
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.40f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.42f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.42f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.48f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(1), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(1), 2.2f))
            }
            10 -> {
                // Wave 10: Mountain mid-climax + 2 Bosses (Siege Titan & Ice Beast)
                repeat(24) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.38f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.40f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.40f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(10), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(1), 2.0f))
            }
            11 -> {
                // Wave 11: Armored juggernauts + 2 Bosses (Frost Colossus & Arctic Machine)
                repeat(26) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.38f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.38f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.40f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 2.0f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(1), 2.2f))
            }
            12 -> {
                // Wave 12: Infiltrating stealth & heavy phalanx + 2 Bosses (Dreadnought & Commander)
                repeat(28) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.35f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.38f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.38f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.40f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createBoss(12), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.2f))
            }
            13 -> {
                // Wave 13: Reinforced mountain assault + 3 Bosses
                repeat(30) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.35f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.38f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.38f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.42f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(13), 2.2f))
            }
            14 -> {
                // Wave 14: Elite mountain battalion + 3 Bosses
                repeat(32) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.35f)) }
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.36f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.38f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.40f)) }
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 2.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(14), 2.2f))
            }
            15 -> {
                // Wave 15: Grand Alpine Vanguard + 4 Bosses
                repeat(36) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.32f)) }
                repeat(24) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.35f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.35f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.38f)) }
                list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.8f))
                list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 2.0f))
                list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 2.0f))
                list.add(WaveSpawnItem(EnemySpec.createBoss(15), 2.5f))
            }

            // =========================================================================
            // LAST 5 WAVES: Only bosses in large amount
            // =========================================================================
            16 -> {
                // Wave 16: 10 Bosses descending the summit (Strictly bosses only!)
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.createBoss(14), 1.8f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.createIceGolem(1), 1.8f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(1), 2.0f)) }
            }
            17 -> {
                // Wave 17: 14 Bosses (Dreadnoughts, Frost Colossi, Ice Beasts)
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.createBoss(16), 1.6f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.6f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.8f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.createFrozenCommander(2), 1.8f)) }
            }
            18 -> {
                // Wave 18: 18 Bosses (Dreadnoughts, Frost Colossi, Arctic Machines)
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.createBoss(17), 1.4f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 1.6f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.createIceBeast(2), 1.6f)) }
            }
            19 -> {
                // Wave 19: 22 Bosses (Colossi, Arctic Overlords, Frost Colossi)
                repeat(7) { list.add(WaveSpawnItem(EnemySpec.createBoss(18), 1.2f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.2f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.createArcticMachine(2), 1.4f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 1.5f)) }
            }
            20 -> {
                // Wave 20: 28 Supreme Bosses (The Ultimate Mountain Boss Avalanche!)
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.createBoss(19), 1.0f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.createIceGolem(2), 1.0f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.createArcticOverlord(), 1.2f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.createBoss(20), 1.5f)) }
            }
        }

        return list
    }

    /**
     * Hard Desert Dune Bastion Wave Roster:
     * - Difficulty: Hard
     * - Total waves: 15
     * - 1st wave: First the boss attacking with small enemies
     * - 2nd to 6 waves: Fastest enemies with medium level enemies
     * - 7 to 11 waves: All type enemies with different bosses
     * - 12 to 15 waves: ONLY bosses in large amount
     */
    private fun generateDesertDuneBastionWaveRoster(wave: Int): List<WaveSpawnItem> {
        val list = mutableListOf<WaveSpawnItem>()

        when (wave) {
            // =========================================================================
            // 1ST WAVE: FIRST THE BOSS ATTACKING WITH SMALL ENEMIES
            // The boss emerges first out of the desert gates, followed by small scouts!
            // =========================================================================
            1 -> {
                // First: The Boss attacking!
                list.add(WaveSpawnItem(EnemySpec.createDuneGolem(1), 3.0f))
                // Followed by small enemies
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.70f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.MINION_SCOUT, 0.55f)) }
            }

            // =========================================================================
            // 2ND TO 6 WAVES: FASTEST ENEMIES WITH MEDIUM LEVEL ENEMIES
            // Fastest: RUNNER, SCOUT
            // Medium level: SOLDIER, SHIELD, FLYING
            // =========================================================================
            2 -> {
                // Wave 2: Quick swarm of desert runners & scouts with soldier frontliners
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.45f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.48f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.55f)) }
            }
            3 -> {
                // Wave 3: Faster sprinters & scouts supported by soldiers and shield vanguards
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.40f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.42f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.50f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.55f)) }
            }
            4 -> {
                // Wave 4: Flying sky drakes soaring over dunes with fastest runners and scouts
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.36f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.38f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.48f)) }
            }
            5 -> {
                // Wave 5: High-speed runners & scouts accompanied by aerial drakes, shield bearers & soldiers
                repeat(20) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.36f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.45f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.42f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.45f)) }
            }
            6 -> {
                // Wave 6: Climax of fastest enemies — high velocity blitz with reinforced medium phalanx
                repeat(24) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.32f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.42f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.40f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.42f)) }
            }

            // =========================================================================
            // 7 TO 11 WAVES: ALL TYPE ENEMIES WITH DIFFERENT BOSSES
            // Enemies: Scout, Soldier, Heavy, Runner, Shield, Flying, Healer, Summoner, Stealth
            // Different Bosses:
            //   Wave 7: Ancient Dune Golem
            //   Wave 8: Sandstorm Titan
            //   Wave 9: Desert Warlord
            //   Wave 10: Solar Dreadnought
            //   Wave 11: Dune Apex Overlord
            // =========================================================================
            7 -> {
                // Wave 7: All types + Boss: Ancient Dune Golem
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.45f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.35f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.45f)) }
                list.add(WaveSpawnItem(EnemySpec.createDuneGolem(2), 2.2f)) // Boss
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.48f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.48f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.45f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.50f)) }
                repeat(2) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.52f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.42f)) }
            }
            8 -> {
                // Wave 8: All types + Boss: Sandstorm Titan
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.42f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.32f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.42f)) }
                list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(1), 2.2f)) // Boss
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.45f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.45f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.42f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.48f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.50f)) }
                repeat(8) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.40f)) }
            }
            9 -> {
                // Wave 9: All types + Boss: Desert Warlord
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.40f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.30f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.40f)) }
                list.add(WaveSpawnItem(EnemySpec.createDesertWarlord(1), 2.0f)) // Boss
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.42f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.42f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.40f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.45f)) }
                repeat(3) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.48f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.38f)) }
            }
            10 -> {
                // Wave 10: All types + Boss: Solar Dreadnought
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.38f)) }
                repeat(16) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.28f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.38f)) }
                list.add(WaveSpawnItem(EnemySpec.createSolarDreadnought(1), 2.2f)) // Boss
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.40f)) }
                repeat(10) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.40f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.38f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.45f)) }
                repeat(4) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.46f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.36f)) }
            }
            11 -> {
                // Wave 11: All types + Boss: Dune Apex Overlord
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SCOUT, 0.36f)) }
                repeat(18) { list.add(WaveSpawnItem(EnemySpec.RUNNER, 0.26f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.SOLDIER, 0.36f)) }
                list.add(WaveSpawnItem(EnemySpec.createDuneApexOverlord(), 2.5f)) // Boss
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.HEAVY, 0.38f)) }
                repeat(12) { list.add(WaveSpawnItem(EnemySpec.SHIELD, 0.38f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.FLYING, 0.36f)) }
                repeat(6) { list.add(WaveSpawnItem(EnemySpec.HEALER, 0.42f)) }
                repeat(5) { list.add(WaveSpawnItem(EnemySpec.SUMMONER, 0.44f)) }
                repeat(14) { list.add(WaveSpawnItem(EnemySpec.STEALTH, 0.34f)) }
            }

            // =========================================================================
            // 12 TO 15 WAVES: ONLY BOSSES IN LARGE AMOUNT
            // Zero regular creeps — strictly colossal waves of bosses storming both paths!
            // =========================================================================
            12 -> {
                // Wave 12: 6 Bosses (Alternating Dune Golems & Sandstorm Titans across both paths)
                list.add(WaveSpawnItem(EnemySpec.createDuneGolem(2), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(1), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createDuneGolem(2), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(1), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createDuneGolem(2), 2.2f))
                list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(1), 2.5f))
            }
            13 -> {
                // Wave 13: 8 Bosses (Dune Golems, Sandstorm Titans, Desert Warlords)
                repeat(2) {
                    list.add(WaveSpawnItem(EnemySpec.createDuneGolem(2), 2.0f))
                    list.add(WaveSpawnItem(EnemySpec.createDesertWarlord(1), 2.0f))
                    list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(1), 2.0f))
                    list.add(WaveSpawnItem(EnemySpec.createDesertWarlord(2), 2.2f))
                }
            }
            14 -> {
                // Wave 14: 12 Bosses (Sandstorm Titans, Desert Warlords, Solar Dreadnoughts)
                repeat(4) {
                    list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(2), 1.8f))
                    list.add(WaveSpawnItem(EnemySpec.createDesertWarlord(2), 1.8f))
                    list.add(WaveSpawnItem(EnemySpec.createSolarDreadnought(1), 2.0f))
                }
            }
            15 -> {
                // Wave 15: 16 Bosses! (The Grand Desert Apex Incursion — Solar Dreadnoughts, Titans, Warlords, Apex Overlords)
                repeat(4) {
                    list.add(WaveSpawnItem(EnemySpec.createSandstormTitan(2), 1.6f))
                    list.add(WaveSpawnItem(EnemySpec.createDesertWarlord(2), 1.6f))
                    list.add(WaveSpawnItem(EnemySpec.createSolarDreadnought(2), 1.8f))
                    list.add(WaveSpawnItem(EnemySpec.createDuneApexOverlord(), 2.2f))
                }
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
