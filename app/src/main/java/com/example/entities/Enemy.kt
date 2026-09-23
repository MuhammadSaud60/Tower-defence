package com.example.entities

import com.example.data.GameConfig
import java.util.UUID

enum class EnemyType {
    SCOUT,
    SOLDIER,
    HEAVY,
    RUNNER,
    SHIELD,
    FLYING,
    HEALER,
    SUMMONER,
    STEALTH,
    BOSS
}

/**
 * Archetype definition for enemies.
 */
data class EnemySpec(
    val type: EnemyType = EnemyType.SOLDIER,
    val name: String = "Soldier",
    val baseHp: Float = 120f,
    val armor: Float = 4f,
    val baseSpeed: Float = 110f,
    val rewardCoins: Int = 6,
    val baseDamage: Int = GameConfig.DAMAGE_PER_ENEMY,
    val radius: Float = 22f,
    val primaryColorHex: Long = 0xFF4CAF50L,
    val secondaryColorHex: Long = 0xFF2E7D32L,
    val isBoss: Boolean = false,
    val regenRate: Float = 0f,
    val bulletResistance: Float = 0f,
    val isFlying: Boolean = false,
    val maxArmorHp: Float = 0f,
    val maxShieldHp: Float = 0f,
    val abilities: List<AbilityDefinition> = emptyList()
) {
    companion object {
        val SCOUT = EnemySpec(
            type = EnemyType.SCOUT,
            name = "Scout",
            baseHp = 45f,
            armor = 0f,
            baseSpeed = 175f,
            rewardCoins = 4,
            baseDamage = 6,
            radius = 16f,
            primaryColorHex = 0xFFFFB703L, // Warm Golden Amber
            secondaryColorHex = 0xFFFB8500L,
            bulletResistance = 0f
        )

        val SOLDIER = EnemySpec(
            type = EnemyType.SOLDIER,
            name = "Soldier",
            baseHp = 120f,
            armor = 4f,
            baseSpeed = 110f,
            rewardCoins = 6,
            baseDamage = 10,
            radius = 20f,
            primaryColorHex = 0xFF2A9D8FL, // Forest Teal
            secondaryColorHex = 0xFF264653L,
            bulletResistance = 0.25f
        )

        val HEAVY = EnemySpec(
            type = EnemyType.HEAVY,
            name = "Heavy",
            baseHp = 380f,
            armor = 16f,
            baseSpeed = 65f,
            rewardCoins = 14,
            baseDamage = 20,
            radius = 28f,
            primaryColorHex = 0xFFE76F51L, // Rust Brick / Crimson Iron
            secondaryColorHex = 0xFF9A031EL,
            bulletResistance = 0.70f, // 70% deflection against normal guns while armor intact
            maxArmorHp = 180f,
            abilities = listOf(AbilityDefinition.armorProtection(maxArmorHp = 180f, bulletDeflection = 0.70f))
        )

        val RUNNER = EnemySpec(
            type = EnemyType.RUNNER,
            name = "Runner",
            baseHp = 40f,
            armor = 1f,
            baseSpeed = 220f,
            rewardCoins = 5,
            baseDamage = 8,
            radius = 15f,
            primaryColorHex = 0xFF00B4D8L, // Electric Cyan
            secondaryColorHex = 0xFF0077B6L,
            bulletResistance = 0f,
            abilities = listOf(AbilityDefinition.speedBurst(cooldown = 5.2f, duration = 1.8f, warningDuration = 0.45f, speedMultiplierBonus = 0.65f))
        )

        val SHIELD = EnemySpec(
            type = EnemyType.SHIELD,
            name = "Shield Vanguard",
            baseHp = 150f,
            armor = 2f,
            baseSpeed = 95f,
            rewardCoins = 10,
            baseDamage = 10,
            radius = 22f,
            primaryColorHex = 0xFF0284C7L, // Azure Cyan
            secondaryColorHex = 0xFF0369A1L,
            maxShieldHp = 180f,
            abilities = listOf(AbilityDefinition.energyShield(maxShieldHp = 180f, regenDelay = 3.2f, regenRate = 50f))
        )

        val FLYING = EnemySpec(
            type = EnemyType.FLYING,
            name = "Sky Drake",
            baseHp = 95f,
            armor = 1f,
            baseSpeed = 135f,
            rewardCoins = 8,
            baseDamage = 10,
            radius = 20f,
            primaryColorHex = 0xFF6366F1L, // Indigo Violet
            secondaryColorHex = 0xFF4338CAL,
            isFlying = true
        )

        val HEALER = EnemySpec(
            type = EnemyType.HEALER,
            name = "Field Medic",
            baseHp = 90f,
            armor = 0f,
            baseSpeed = 100f,
            rewardCoins = 9,
            baseDamage = 6,
            radius = 18f,
            primaryColorHex = 0xFF10B981L, // Emerald Mint
            secondaryColorHex = 0xFF059669L,
            abilities = listOf(AbilityDefinition.healingAura(cooldown = 2.0f, healAmount = 25f, auraRadius = 125f))
        )

        val SUMMONER = EnemySpec(
            type = EnemyType.SUMMONER,
            name = "Void Summoner",
            baseHp = 250f,
            armor = 6f,
            baseSpeed = 75f,
            rewardCoins = 16,
            baseDamage = 15,
            radius = 25f,
            primaryColorHex = 0xFF8B5CF6L, // Mystic Purple
            secondaryColorHex = 0xFFEC4899L,
            abilities = listOf(AbilityDefinition.summonMinions(cooldown = 7.5f, warningDuration = 0.8f, minionsPerSummon = 2, maxTotalMinions = 6))
        )

        val STEALTH = EnemySpec(
            type = EnemyType.STEALTH,
            name = "Shadow Infiltrator",
            baseHp = 80f,
            armor = 0f,
            baseSpeed = 160f,
            rewardCoins = 9,
            baseDamage = 8,
            radius = 16f,
            primaryColorHex = 0xFF475569L, // Phantom Slate
            secondaryColorHex = 0xFF1E293BL,
            abilities = listOf(AbilityDefinition.temporaryCloak(cooldown = 6.0f, duration = 3.0f, warningDuration = 0.4f))
        )

        val MINION_SCOUT = EnemySpec(
            type = EnemyType.SCOUT,
            name = "Void Minion",
            baseHp = 30f,
            armor = 0f,
            baseSpeed = 170f,
            rewardCoins = 1,
            baseDamage = 4,
            radius = 13f,
            primaryColorHex = 0xFFA855F7L,
            secondaryColorHex = 0xFF7E22CEL
        )

        val ICE_MINION = EnemySpec(
            type = EnemyType.SCOUT,
            name = "Ice Minion",
            baseHp = 60f,
            armor = 2f,
            baseSpeed = 150f,
            rewardCoins = 2,
            baseDamage = 5,
            radius = 14f,
            primaryColorHex = 0xFF38BDF8L,
            secondaryColorHex = 0xFF0284C7L
        )

        val VOID_SOVEREIGN = EnemySpec(
            type = EnemyType.BOSS,
            name = "Void Sovereign",
            baseHp = 8500f,
            armor = 50f,
            baseSpeed = 34f,
            rewardCoins = 350,
            baseDamage = 30,
            radius = 44f,
            primaryColorHex = 0xFF7928CAL, // Dark Nether Violet / Void Amethyst
            secondaryColorHex = 0xFFFF0080L, // Electric Magenta / Void Nebula Flare
            isBoss = true,
            regenRate = 35f,
            bulletResistance = 0.82f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.82f),
                AbilityDefinition.iceShockwave(cooldown = 7.5f, radius = 210f),
                AbilityDefinition.summonIceMinions(cooldown = 8.0f)
            )
        )

        fun createBoss(wave: Int): EnemySpec {
            val (name, hp, armor, regen, speed, coins, resistance) = when {
                wave <= 5 -> Tuple7("Iron Golem", 1200f, 24f, 8f, 48f, 60, 0.60f)
                wave <= 10 -> Tuple7("Siege Titan", 2200f, 30f, 14f, 44f, 100, 0.65f)
                wave <= 15 -> Tuple7("Dreadnought", 3600f, 36f, 20f, 40f, 160, 0.70f)
                wave < 20 -> Tuple7("Overlord Colossus", 5200f, 42f, 28f, 36f, 240, 0.75f)
                else -> Tuple7("Void Sovereign", 8500f, 50f, 35f, 34f, 350, 0.82f)
            }
            return EnemySpec(
                type = EnemyType.BOSS,
                name = name,
                baseHp = hp,
                armor = armor,
                baseSpeed = speed,
                rewardCoins = coins,
                baseDamage = if (name == "Void Sovereign") 30 else GameConfig.DAMAGE_PER_BOSS,
                radius = if (name == "Void Sovereign") 44f else 38f,
                primaryColorHex = if (name == "Void Sovereign") 0xFF7928CAL else 0xFF8338ECL,
                secondaryColorHex = if (name == "Void Sovereign") 0xFFFF0080L else 0xFFFF006EL,
                isBoss = true,
                regenRate = regen,
                bulletResistance = resistance,
                abilities = listOf(
                    AbilityDefinition.iceArmor(resistance),
                    AbilityDefinition.iceShockwave(cooldown = 8.5f, radius = 190f),
                    AbilityDefinition.summonIceMinions(cooldown = 9.0f)
                )
            )
        }

        // ========================================================
        // SNOW WORLD SPECIALIZED BOSS VARIATIONS
        // ========================================================

        /**
         * Ice Golem: Slow, heavily armored juggernaut that deflects 70-75% ballistic bullets.
         */
        fun createIceGolem(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Ice Golem" else "Frost Colossus",
            baseHp = if (tier == 1) 1500f else 3800f,
            armor = if (tier == 1) 28f else 38f,
            baseSpeed = 42f,
            rewardCoins = if (tier == 1) 85 else 190,
            baseDamage = 25,
            radius = 42f,
            primaryColorHex = 0xFF0284C7L, // Deep Glacier Cyan
            secondaryColorHex = 0xFFE0F2FEL, // Frosted Crystal White
            isBoss = true,
            regenRate = 12f,
            bulletResistance = 0.75f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.75f),
                AbilityDefinition.iceShockwave(cooldown = if (tier == 1) 8.5f else 7.0f, radius = 200f),
                AbilityDefinition.summonIceMinions(cooldown = 8.5f)
            )
        )

        /**
         * Frozen Commander: Tactical vanguard with active shield regeneration and high speed.
         */
        fun createFrozenCommander(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Frozen Commander" else "Glacial Warlord",
            baseHp = if (tier == 1) 2000f else 4200f,
            armor = if (tier == 1) 24f else 34f,
            baseSpeed = 52f,
            rewardCoins = if (tier == 1) 110 else 220,
            baseDamage = 25,
            radius = 40f,
            primaryColorHex = 0xFF4338CAL, // Royal Indigo Glacier
            secondaryColorHex = 0xFF38BDF8L, // Electric Cryo Aura
            isBoss = true,
            regenRate = 30f,
            bulletResistance = 0.65f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.65f),
                AbilityDefinition.iceShockwave(cooldown = 8.0f, radius = 190f),
                AbilityDefinition.summonIceMinions(cooldown = 8.0f)
            )
        )

        /**
         * Ice Beast: Swift, agile predatory boss rushing past frontline turrets.
         */
        fun createIceBeast(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Ice Beast" else "Ice Beast Alpha",
            baseHp = if (tier == 1) 1200f else 2600f,
            armor = if (tier == 1) 14f else 22f,
            baseSpeed = if (tier == 1) 128f else 145f,
            rewardCoins = if (tier == 1) 75 else 165,
            baseDamage = 20,
            radius = 35f,
            primaryColorHex = 0xFF06B6D4L, // Neon Blizzard Cyan
            secondaryColorHex = 0xFFF43F5EL, // Feral Crimson Frost
            isBoss = true,
            regenRate = 14f,
            bulletResistance = 0.40f,
            abilities = listOf(
                AbilityDefinition.speedBurst(cooldown = 6.0f, duration = 2.0f, speedMultiplierBonus = 0.5f),
                AbilityDefinition.iceShockwave(cooldown = 9.0f, radius = 175f)
            )
        )

        /**
         * Arctic Machine: Massive mechanical ice-crawler siege engine with high armor and titanium hull.
         */
        fun createArcticMachine(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Arctic Machine" else "Polar Dreadnought",
            baseHp = if (tier == 1) 5200f else 8200f,
            armor = if (tier == 1) 40f else 48f,
            baseSpeed = 35f,
            rewardCoins = if (tier == 1) 240 else 380,
            baseDamage = 35,
            radius = 45f,
            primaryColorHex = 0xFF334155L, // Gunmetal Titanium Slate
            secondaryColorHex = 0xFF0EA5E9L, // Cryo Core Reactor Blue
            isBoss = true,
            regenRate = 22f,
            bulletResistance = 0.80f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.80f),
                AbilityDefinition.iceShockwave(cooldown = 7.5f, radius = 220f),
                AbilityDefinition.summonIceMinions(cooldown = 7.5f)
            )
        )

        /**
         * Void Arctic Overlord: Apex endgame polar sovereign with overwhelming health and aurora shields.
         */
        fun createArcticOverlord(): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = "Void Arctic Overlord",
            baseHp = 9800f,
            armor = 52f,
            baseSpeed = 34f,
            rewardCoins = 450,
            baseDamage = 40,
            radius = 48f,
            primaryColorHex = 0xFF4F46E5L, // Cosmic Dark Frost
            secondaryColorHex = 0xFF00F5D4L, // Polar Aurora Flare
            isBoss = true,
            regenRate = 42f,
            bulletResistance = 0.85f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.85f),
                AbilityDefinition.iceShockwave(cooldown = 6.5f, radius = 240f),
                AbilityDefinition.summonIceMinions(cooldown = 7.0f)
            )
        )

        /**
         * Dune Golem: Ancient sandstone juggernaut formed from hardened desert sediment.
         */
        fun createDuneGolem(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Dune Golem" else "Ancient Sand Golem",
            baseHp = if (tier == 1) 1600f else 3600f,
            armor = if (tier == 1) 24f else 34f,
            baseSpeed = 46f,
            rewardCoins = if (tier == 1) 90 else 200,
            baseDamage = 25,
            radius = 42f,
            primaryColorHex = 0xFFD4A373L, // Sandstone Ochre
            secondaryColorHex = 0xFFBC6C25L, // Terra Cotta
            isBoss = true,
            regenRate = 12f,
            bulletResistance = 0.65f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.65f),
                AbilityDefinition.iceShockwave(cooldown = 8.0f, radius = 190f)
            )
        )

        /**
         * Sandstorm Titan: Colossal desert brute wielding spinning whirlwind sand defenses.
         */
        fun createSandstormTitan(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Sandstorm Titan" else "Dune Colossus",
            baseHp = if (tier == 1) 2400f else 4800f,
            armor = if (tier == 1) 30f else 40f,
            baseSpeed = 42f,
            rewardCoins = if (tier == 1) 120 else 240,
            baseDamage = 25,
            radius = 44f,
            primaryColorHex = 0xFFE76F51L, // Desert Terracotta
            secondaryColorHex = 0xFFF4A261L, // Radiant Sand Glow
            isBoss = true,
            regenRate = 18f,
            bulletResistance = 0.70f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.70f),
                AbilityDefinition.iceShockwave(cooldown = 7.5f, radius = 210f)
            )
        )

        /**
         * Desert Warlord: Swift armored nomad commander charging across dunes with speed bursts.
         */
        fun createDesertWarlord(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Desert Warlord" else "Nomad Warmaster",
            baseHp = if (tier == 1) 2000f else 4400f,
            armor = if (tier == 1) 22f else 32f,
            baseSpeed = 58f,
            rewardCoins = if (tier == 1) 110 else 230,
            baseDamage = 25,
            radius = 40f,
            primaryColorHex = 0xFFCA8A04L, // Golden Dune Amber
            secondaryColorHex = 0xFFB45309L, // Deep Bronze
            isBoss = true,
            regenRate = 22f,
            bulletResistance = 0.60f,
            abilities = listOf(
                AbilityDefinition.speedBurst(cooldown = 6.0f, duration = 2.0f, speedMultiplierBonus = 0.45f),
                AbilityDefinition.iceShockwave(cooldown = 8.5f, radius = 185f)
            )
        )

        /**
         * Solar Dreadnought: Sunfire-forged desert siege fortress with heavy blast armor plating.
         */
        fun createSolarDreadnought(tier: Int = 1): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = if (tier == 1) "Solar Dreadnought" else "Sunfire Colossus",
            baseHp = if (tier == 1) 4200f else 7200f,
            armor = if (tier == 1) 38f else 48f,
            baseSpeed = 36f,
            rewardCoins = if (tier == 1) 220 else 360,
            baseDamage = 35,
            radius = 46f,
            primaryColorHex = 0xFF9A3412L, // Obsidian Sun Rust
            secondaryColorHex = 0xFFEA580CL, // Solar Blaze Flare
            isBoss = true,
            regenRate = 26f,
            bulletResistance = 0.78f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.78f),
                AbilityDefinition.iceShockwave(cooldown = 7.0f, radius = 220f)
            )
        )

        /**
         * Dune Apex Overlord: Supreme pharaoh sovereign of the Great Sand Sea with impenetrable armor and crushing shocks.
         */
        fun createDuneApexOverlord(): EnemySpec = EnemySpec(
            type = EnemyType.BOSS,
            name = "Dune Apex Overlord",
            baseHp = 8800f,
            armor = 50f,
            baseSpeed = 34f,
            rewardCoins = 400,
            baseDamage = 40,
            radius = 48f,
            primaryColorHex = 0xFF78350FL, // Ancient Pharaoh Bronze
            secondaryColorHex = 0xFFFBBF24L, // Blinding Solar Crown
            isBoss = true,
            regenRate = 38f,
            bulletResistance = 0.82f,
            abilities = listOf(
                AbilityDefinition.iceArmor(0.82f),
                AbilityDefinition.iceShockwave(cooldown = 6.5f, radius = 230f)
            )
        )
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
)

/**
 * Individual enemy instance traveling on the battlefield.
 */
data class Enemy(
    val id: String = UUID.randomUUID().toString(),
    val spec: EnemySpec = EnemySpec.SOLDIER,
    val pathIndex: Int = 0,
    val maxHp: Float = spec.baseHp,
    val currentHp: Float = maxHp,
    val speed: Float = spec.baseSpeed,
    val position: Point2D = Point2D(-40f, 180f),
    val currentSegmentIndex: Int = 0,
    val distanceOnSegment: Float = 0f,
    val totalProgress: Float = 0f,
    val isAlive: Boolean = true,
    val reachedBase: Boolean = false,
    val animWobbleTime: Float = 0f,
    val hitFlashTimer: Float = 0f,
    val isShielded: Boolean = false,
    val shieldTimer: Float = 0f,
    val shieldCooldown: Float = 8.0f,
    val isInTunnel: Boolean = false,
    val headingAngle: Float = 0f,
    val slowTimer: Float = 0f,
    val slowFactor: Float = 0f,

    // --- Enemy Ability & Boss Mechanic Runtime States ---
    val bossPhase: Int = 1,
    val bossPhaseTransitionTimer: Float = 0f,
    val currentArmorHp: Float = spec.maxArmorHp,
    val currentShieldHp: Float = spec.maxShieldHp,
    val shieldRegenDelayTimer: Float = 0f,
    val isSpeedBurstActive: Boolean = false,
    val speedBurstTimer: Float = 0f,
    val speedBurstCooldown: Float = 3.5f,
    val speedBurstWarningTimer: Float = 0f,
    val isStealthed: Boolean = false,
    val stealthTimer: Float = 0f,
    val stealthCooldown: Float = 4.0f,
    val stealthWarningTimer: Float = 0f,
    val healingCooldown: Float = 2.0f,
    val summonCooldown: Float = 5.0f,
    val summonWarningTimer: Float = 0f,
    val summonsCount: Int = 0,
    val shockwaveCooldown: Float = 7.0f,
    val shockwaveWarningTimer: Float = 0f,
    val footstepTimer: Float = 0f,
    val hasFootstepStomp: Boolean = false,
    val hasArmorBrokenJustNow: Boolean = false,
    val hasShieldBrokenJustNow: Boolean = false,
    val hasPhaseChangedJustNow: Boolean = false,
    val hasHealPulseJustNow: Boolean = false,
    val hasShockwaveJustNow: Boolean = false,
    val hasSummonJustNow: Boolean = false
) {
    val healthPercentage: Float get() = (currentHp / maxHp).coerceIn(0f, 1f)
    val isHitFlashing: Boolean get() = hitFlashTimer > 0f
    val isSlowed: Boolean get() = slowTimer > 0f
    val isArmorBroken: Boolean get() = spec.maxArmorHp > 0f && currentArmorHp <= 0f
    val isEnergyShieldActive: Boolean get() = spec.maxShieldHp > 0f && currentShieldHp > 0f
    val armorFraction: Float get() = if (spec.maxArmorHp > 0f) (currentArmorHp / spec.maxArmorHp).coerceIn(0f, 1f) else 0f
    val shieldFraction: Float get() = if (spec.maxShieldHp > 0f) (currentShieldHp / spec.maxShieldHp).coerceIn(0f, 1f) else 0f
    val isEnraged: Boolean get() = spec.isBoss && bossPhase == 3

    fun takeDamage(rawAmount: Float, armorPiercing: Float = 0f, isNormalGun: Boolean = false): Enemy {
        var dmgRemaining = rawAmount
        var newShieldHp = currentShieldHp
        var shieldJustBroke = false
        var newShieldDelay = shieldRegenDelayTimer

        // 1. Energy Shield absorbs damage first
        if (isEnergyShieldActive) {
            newShieldDelay = 3.2f // Reset regeneration delay upon taking damage
            if (dmgRemaining <= newShieldHp) {
                newShieldHp -= dmgRemaining
                dmgRemaining = 0f
            } else {
                dmgRemaining -= newShieldHp
                newShieldHp = 0f
                shieldJustBroke = true
            }
        }

        if (dmgRemaining <= 0f) {
            return copy(
                currentShieldHp = newShieldHp,
                shieldRegenDelayTimer = newShieldDelay,
                hasShieldBrokenJustNow = shieldJustBroke,
                hitFlashTimer = 0.14f
            )
        }

        // 2. Armor and Damage Reduction
        var newArmorHp = currentArmorHp
        var armorJustBroke = false
        val effectiveRaw: Float
        val effectiveArmor: Float

        if (spec.type == EnemyType.HEAVY) {
            if (!isArmorBroken) {
                // Armor takes structural wear from incoming fire
                val armorWear = dmgRemaining * (if (armorPiercing > 0.3f) 1.2f else 0.5f)
                newArmorHp = (currentArmorHp - armorWear).coerceAtLeast(0f)
                if (newArmorHp <= 0f) {
                    armorJustBroke = true
                }
                effectiveRaw = if (isNormalGun) dmgRemaining * (1f - spec.bulletResistance) else dmgRemaining
                effectiveArmor = (spec.armor * (1f - armorPiercing.coerceIn(0f, 0.9f))).coerceAtLeast(0f)
            } else {
                // Armor plates shattered! Takes unreduced ballistic damage
                effectiveRaw = dmgRemaining
                effectiveArmor = 0f
            }
        } else {
            effectiveRaw = if (isNormalGun && spec.bulletResistance > 0f) {
                dmgRemaining * (1f - spec.bulletResistance)
            } else {
                dmgRemaining
            }
            val baseArmor = if (isShielded) (spec.armor + 25f) else spec.armor
            effectiveArmor = (baseArmor * (1f - armorPiercing.coerceIn(0f, 0.9f))).coerceAtLeast(0f)
        }

        val minFloor = if (isNormalGun && spec.bulletResistance >= 0.5f) 0.04f else 0.15f
        val netDamage = kotlin.math.max(effectiveRaw * minFloor, effectiveRaw - effectiveArmor)
        val newHp = currentHp - netDamage

        // 3. Boss Phase transitions: Phase 1 (>60%), Phase 2 (25%-60%), Phase 3 (<=25% Enraged)
        var newBossPhase = bossPhase
        var phaseTransitionJustNow = false
        var newPhaseTimer = bossPhaseTransitionTimer

        if (spec.isBoss && newHp > 0f) {
            if (bossPhase == 1 && newHp <= maxHp * 0.60f) {
                newBossPhase = 2
                phaseTransitionJustNow = true
                newPhaseTimer = 1.0f
            } else if (bossPhase <= 2 && newHp <= maxHp * 0.25f) {
                newBossPhase = 3
                phaseTransitionJustNow = true
                newPhaseTimer = 1.0f
            }
        }

        return copy(
            currentHp = newHp,
            isAlive = newHp > 0f,
            currentShieldHp = newShieldHp,
            shieldRegenDelayTimer = newShieldDelay,
            currentArmorHp = newArmorHp,
            bossPhase = newBossPhase,
            bossPhaseTransitionTimer = newPhaseTimer,
            hasShieldBrokenJustNow = shieldJustBroke,
            hasArmorBrokenJustNow = armorJustBroke,
            hasPhaseChangedJustNow = phaseTransitionJustNow,
            hitFlashTimer = 0.14f
        )
    }

    fun applySlow(factor: Float, duration: Float): Enemy {
        val newFactor = kotlin.math.max(slowFactor, factor.coerceIn(0.1f, 0.85f))
        val newTimer = kotlin.math.max(slowTimer, duration)
        return copy(
            slowFactor = newFactor,
            slowTimer = newTimer
        )
    }

    fun heal(amount: Float): Enemy {
        if (!isAlive || currentHp >= maxHp) return this
        val restoredHp = kotlin.math.min(maxHp, currentHp + amount)
        return copy(currentHp = restoredHp)
    }

    /**
     * Advances the enemy along the path waypoints by distance = speed * dt.
     * Updates ability cooldowns, warnings, and active states.
     */
    fun advance(dt: Float, path: GamePath): Enemy {
        if (!isAlive || reachedBase) return this

        // Reset single-frame event flags
        var newHp = currentHp
        var newPhaseTimer = (bossPhaseTransitionTimer - dt).coerceAtLeast(0f)

        // 1. Energy Shield regeneration (when not attacked for shieldRegenDelayTimer)
        var newShieldHp = currentShieldHp
        var newShieldDelay = (shieldRegenDelayTimer - dt).coerceAtLeast(0f)
        if (spec.maxShieldHp > 0f && newShieldHp < spec.maxShieldHp && newShieldDelay <= 0f) {
            newShieldHp = kotlin.math.min(spec.maxShieldHp, newShieldHp + 50f * dt)
        }

        // 2. Boss Legacy Shield cycle & HP regeneration
        var newShielded = isShielded
        var newShieldTimer = shieldTimer
        var newShieldCooldown = shieldCooldown

        if (spec.isBoss) {
            if (spec.regenRate > 0f && newHp < maxHp) {
                newHp = kotlin.math.min(maxHp, newHp + spec.regenRate * dt)
            }

            if (newShielded) {
                newShieldTimer -= dt
                if (newShieldTimer <= 0f) {
                    newShielded = false
                    newShieldTimer = 0f
                    newShieldCooldown = 8.5f
                }
            } else {
                newShieldCooldown -= dt
                if (newShieldCooldown <= 0f) {
                    newShielded = true
                    newShieldTimer = 3.5f
                }
            }
        }

        // 3. Runner Speed Burst ability cycle
        var newSpeedBurstActive = isSpeedBurstActive
        var newSpeedBurstTimer = speedBurstTimer
        var newSpeedBurstCooldown = speedBurstCooldown
        var newSpeedBurstWarning = speedBurstWarningTimer

        if (spec.type == EnemyType.RUNNER) {
            if (newSpeedBurstActive) {
                newSpeedBurstTimer -= dt
                if (newSpeedBurstTimer <= 0f) {
                    newSpeedBurstActive = false
                    newSpeedBurstCooldown = 5.2f
                }
            } else if (newSpeedBurstWarning > 0f) {
                newSpeedBurstWarning -= dt
                if (newSpeedBurstWarning <= 0f) {
                    newSpeedBurstActive = true
                    newSpeedBurstTimer = 1.8f
                }
            } else {
                newSpeedBurstCooldown -= dt
                if (newSpeedBurstCooldown <= 0f) {
                    newSpeedBurstWarning = 0.45f
                }
            }
        }

        // 4. Stealth Cloak ability cycle
        var newIsStealthed = isStealthed
        var newStealthTimer = stealthTimer
        var newStealthCooldown = stealthCooldown
        var newStealthWarning = stealthWarningTimer

        if (spec.type == EnemyType.STEALTH) {
            if (newIsStealthed) {
                newStealthTimer -= dt
                if (newStealthTimer <= 0f) {
                    newIsStealthed = false
                    newStealthCooldown = 6.0f
                }
            } else if (newStealthWarning > 0f) {
                newStealthWarning -= dt
                if (newStealthWarning <= 0f) {
                    newIsStealthed = true
                    newStealthTimer = 3.2f
                }
            } else {
                newStealthCooldown -= dt
                if (newStealthCooldown <= 0f) {
                    newStealthWarning = 0.4f
                }
            }
        }

        // 5. Healer Healing Aura cycle
        var newHealingCooldown = healingCooldown
        var triggerHeal = false
        if (spec.type == EnemyType.HEALER) {
            newHealingCooldown -= dt
            if (newHealingCooldown <= 0f) {
                newHealingCooldown = 2.0f
                triggerHeal = true
            }
        }

        // 6. Summoner Minions cycle
        var newSummonCooldown = summonCooldown
        var newSummonWarning = summonWarningTimer
        var newSummonsCount = summonsCount
        var triggerSummon = false

        if (spec.type == EnemyType.SUMMONER) {
            if (newSummonsCount < 3) {
                if (newSummonWarning > 0f) {
                    newSummonWarning -= dt
                    if (newSummonWarning <= 0f) {
                        triggerSummon = true
                        newSummonsCount++
                        newSummonCooldown = 7.5f
                    }
                } else {
                    newSummonCooldown -= dt
                    if (newSummonCooldown <= 0f) {
                        newSummonWarning = 0.8f
                    }
                }
            }
        }

        // 7. Boss mechanics: Footstep stomps, Shockwave, and Minion Summons
        var newFootstepTimer = footstepTimer
        var triggerFootstep = false
        var newShockwaveCooldown = shockwaveCooldown
        var newShockwaveWarning = shockwaveWarningTimer
        var triggerShockwave = false

        if (spec.isBoss) {
            // Heavy walking stomp
            newFootstepTimer += dt
            val stompInterval = if (bossPhase == 3) 0.55f else 0.75f
            if (newFootstepTimer >= stompInterval) {
                newFootstepTimer = 0f
                triggerFootstep = true
            }

            // Phase 2 & 3: Ice Shockwave
            if (bossPhase >= 2) {
                val swInterval = if (bossPhase == 3) 5.5f else 8.5f
                if (newShockwaveWarning > 0f) {
                    newShockwaveWarning -= dt
                    if (newShockwaveWarning <= 0f) {
                        triggerShockwave = true
                        newShockwaveCooldown = swInterval
                    }
                } else {
                    newShockwaveCooldown -= dt
                    if (newShockwaveCooldown <= 0f) {
                        newShockwaveWarning = 1.0f
                    }
                }

                // Phase 2 & 3: Summon Ice Minions
                val summonInterval = if (bossPhase == 3) 7.5f else 9.5f
                if (newSummonWarning > 0f) {
                    newSummonWarning -= dt
                    if (newSummonWarning <= 0f) {
                        triggerSummon = true
                        newSummonCooldown = summonInterval
                    }
                } else {
                    newSummonCooldown -= dt
                    if (newSummonCooldown <= 0f) {
                        newSummonWarning = 0.8f
                    }
                }
            }
        }

        // 8. Speed calculation and path traversal
        val burstMult = if (newSpeedBurstActive) 1.65f else 1.0f
        val enrageMult = if (spec.isBoss && bossPhase == 3) 1.25f else 1.0f
        val effectiveSlow = if (slowTimer > 0f) slowFactor else 0f
        val slowMult = (1f - effectiveSlow).coerceIn(0.20f, 1f)
        val moveDist = speed * burstMult * enrageMult * slowMult * dt

        val updatedSlowTimer = (slowTimer - dt).coerceAtLeast(0f)
        val updatedSlowFactor = if (updatedSlowTimer <= 0f) 0f else slowFactor
        val updatedFlashTimer = (hitFlashTimer - dt).coerceAtLeast(0f)

        var newSegmentIndex = currentSegmentIndex
        var newDistanceOnSegment = distanceOnSegment + moveDist

        while (newSegmentIndex < path.waypoints.size - 1) {
            val segmentLen = path.segmentLengths[newSegmentIndex]
            if (newDistanceOnSegment <= segmentLen) {
                break
            }
            newDistanceOnSegment -= segmentLen
            newSegmentIndex++
        }

        if (newSegmentIndex >= path.waypoints.size - 1) {
            return copy(
                position = path.endPoint,
                currentSegmentIndex = path.waypoints.size - 1,
                distanceOnSegment = 0f,
                totalProgress = path.totalPathLength,
                reachedBase = true,
                isAlive = false,
                hitFlashTimer = 0f,
                slowTimer = 0f,
                slowFactor = 0f
            )
        }

        val p1 = path.waypoints[newSegmentIndex]
        val p2 = path.waypoints[newSegmentIndex + 1]
        val segLen = path.segmentLengths[newSegmentIndex]
        val fraction = if (segLen > 0.001f) newDistanceOnSegment / segLen else 0f

        val currentPos = Point2D(
            x = p1.x + (p2.x - p1.x) * fraction,
            y = p1.y + (p2.y - p1.y) * fraction
        )

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val targetAngle = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat()
        val angleDiff = (targetAngle - headingAngle + 540f) % 360f - 180f
        val newAngle = headingAngle + angleDiff * kotlin.math.min(1f, dt * 14f)

        val progress = path.calculateProgressDistance(newSegmentIndex, newDistanceOnSegment)

        return copy(
            currentHp = newHp,
            position = currentPos,
            currentSegmentIndex = newSegmentIndex,
            distanceOnSegment = newDistanceOnSegment,
            totalProgress = progress,
            animWobbleTime = animWobbleTime + dt * 8f * (if (newSpeedBurstActive) 1.6f else 1.0f),
            hitFlashTimer = updatedFlashTimer,
            isShielded = newShielded,
            shieldTimer = newShieldTimer,
            shieldCooldown = newShieldCooldown,
            headingAngle = newAngle,
            slowTimer = updatedSlowTimer,
            slowFactor = updatedSlowFactor,
            currentShieldHp = newShieldHp,
            shieldRegenDelayTimer = newShieldDelay,
            bossPhaseTransitionTimer = newPhaseTimer,
            isSpeedBurstActive = newSpeedBurstActive,
            speedBurstTimer = newSpeedBurstTimer,
            speedBurstCooldown = newSpeedBurstCooldown,
            speedBurstWarningTimer = newSpeedBurstWarning,
            isStealthed = newIsStealthed,
            stealthTimer = newStealthTimer,
            stealthCooldown = newStealthCooldown,
            stealthWarningTimer = newStealthWarning,
            healingCooldown = newHealingCooldown,
            summonCooldown = newSummonCooldown,
            summonWarningTimer = newSummonWarning,
            summonsCount = newSummonsCount,
            shockwaveCooldown = newShockwaveCooldown,
            shockwaveWarningTimer = newShockwaveWarning,
            footstepTimer = newFootstepTimer,
            hasFootstepStomp = triggerFootstep,
            hasArmorBrokenJustNow = false,
            hasShieldBrokenJustNow = false,
            hasPhaseChangedJustNow = false,
            hasHealPulseJustNow = triggerHeal,
            hasShockwaveJustNow = triggerShockwave,
            hasSummonJustNow = triggerSummon
        )
    }
}
