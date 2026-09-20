package com.example.entities

import com.example.audio.GameSound

enum class AbilityType {
    SPEED_BURST,
    ARMOR_PROTECTION,
    ENERGY_SHIELD,
    HEALING_AURA,
    SUMMON_MINIONS,
    TEMPORARY_CLOAK,
    ICE_ARMOR,
    ICE_SHOCKWAVE,
    SUMMON_ICE_MINIONS
}

enum class ActivationCondition {
    ALWAYS_WHEN_READY,
    PASSIVE,
    PASSIVE_WITH_REGEN,
    NEARBY_DAMAGED_ALLIES,
    BOSS_PHASE_2_OR_3,
    HEALTH_BELOW_THRESHOLD
}

/**
 * Reusable, data-driven definition of an enemy ability.
 * Defines cooldown, trigger condition, visual effect, sound effect, and gameplay values.
 */
data class AbilityDefinition(
    val type: AbilityType,
    val name: String,
    val description: String,
    val cooldown: Float,
    val duration: Float = 0f,
    val warningDuration: Float = 0f,
    val condition: ActivationCondition = ActivationCondition.ALWAYS_WHEN_READY,
    val visualEffect: EffectType? = null,
    val soundEffect: GameSound? = null,
    val value1: Float = 0f, // e.g. speed boost factor (0.65f), heal amount (25f), shockwave radius (190f)
    val value2: Float = 0f, // e.g. aura radius (120f), tower slow duration (3.0f), minion count (2f)
    val maxUses: Int = -1   // -1 = infinite
) {
    companion object {
        fun speedBurst(
            cooldown: Float = 5.2f,
            duration: Float = 1.8f,
            warningDuration: Float = 0.45f,
            speedMultiplierBonus: Float = 0.65f
        ) = AbilityDefinition(
            type = AbilityType.SPEED_BURST,
            name = "Speed Burst",
            description = "Periodically surges forward with high speed",
            cooldown = cooldown,
            duration = duration,
            warningDuration = warningDuration,
            condition = ActivationCondition.ALWAYS_WHEN_READY,
            visualEffect = EffectType.SPEED_BURST_TRAIL,
            soundEffect = GameSound.RUNNER_DASH,
            value1 = speedMultiplierBonus
        )

        fun armorProtection(
            maxArmorHp: Float = 180f,
            bulletDeflection: Float = 0.70f
        ) = AbilityDefinition(
            type = AbilityType.ARMOR_PROTECTION,
            name = "Armor Protection",
            description = "Heavy steel plates reducing ballistic damage until fractured",
            cooldown = 0f,
            condition = ActivationCondition.PASSIVE,
            visualEffect = EffectType.ARMOR_CRACK_BURST,
            soundEffect = GameSound.ARMOR_BREAK,
            value1 = maxArmorHp,
            value2 = bulletDeflection
        )

        fun energyShield(
            maxShieldHp: Float = 200f,
            regenDelay: Float = 3.2f,
            regenRate: Float = 55f
        ) = AbilityDefinition(
            type = AbilityType.ENERGY_SHIELD,
            name = "Energy Shield",
            description = "Absorbs incoming damage and regenerates when out of combat",
            cooldown = regenDelay,
            condition = ActivationCondition.PASSIVE_WITH_REGEN,
            visualEffect = EffectType.SHIELD_SHATTER,
            soundEffect = GameSound.SHIELD_BREAK,
            value1 = maxShieldHp,
            value2 = regenRate
        )

        fun healingAura(
            cooldown: Float = 2.0f,
            healAmount: Float = 25f,
            auraRadius: Float = 125f
        ) = AbilityDefinition(
            type = AbilityType.HEALING_AURA,
            name = "Healing Aura",
            description = "Restores health to wounded comrades nearby",
            cooldown = cooldown,
            condition = ActivationCondition.NEARBY_DAMAGED_ALLIES,
            visualEffect = EffectType.HEAL_WAVE,
            soundEffect = GameSound.HEAL_PULSE,
            value1 = healAmount,
            value2 = auraRadius
        )

        fun summonMinions(
            cooldown: Float = 7.5f,
            warningDuration: Float = 0.8f,
            minionsPerSummon: Int = 2,
            maxTotalMinions: Int = 6
        ) = AbilityDefinition(
            type = AbilityType.SUMMON_MINIONS,
            name = "Summon Minions",
            description = "Opens void portals summoning minion reinforcements",
            cooldown = cooldown,
            warningDuration = warningDuration,
            condition = ActivationCondition.ALWAYS_WHEN_READY,
            visualEffect = EffectType.SUMMON_RIFT,
            soundEffect = GameSound.SUMMON_MINIONS,
            value1 = minionsPerSummon.toFloat(),
            maxUses = maxTotalMinions / minionsPerSummon
        )

        fun temporaryCloak(
            cooldown: Float = 6.5f,
            duration: Float = 3.2f,
            warningDuration: Float = 0.4f
        ) = AbilityDefinition(
            type = AbilityType.TEMPORARY_CLOAK,
            name = "Temporary Cloak",
            description = "Bends light to become invisible to defense turrets",
            cooldown = cooldown,
            duration = duration,
            warningDuration = warningDuration,
            condition = ActivationCondition.ALWAYS_WHEN_READY,
            visualEffect = EffectType.STEALTH_SMOKE,
            soundEffect = GameSound.STEALTH_CLOAK
        )

        fun iceArmor(
            bulletDeflection: Float = 0.75f
        ) = AbilityDefinition(
            type = AbilityType.ICE_ARMOR,
            name = "Ice Armor",
            description = "Glacial frost crystalline carapace deflecting ballistic projectiles",
            cooldown = 0f,
            condition = ActivationCondition.PASSIVE,
            value1 = bulletDeflection
        )

        fun iceShockwave(
            cooldown: Float = 8.5f,
            warningDuration: Float = 1.0f,
            radius: Float = 190f,
            towerSlowDuration: Float = 3.0f
        ) = AbilityDefinition(
            type = AbilityType.ICE_SHOCKWAVE,
            name = "Ice Shockwave",
            description = "Slams the ground with sub-zero energy, temporarily freezing nearby turrets",
            cooldown = cooldown,
            warningDuration = warningDuration,
            condition = ActivationCondition.BOSS_PHASE_2_OR_3,
            visualEffect = EffectType.BOSS_SHOCKWAVE_RING,
            soundEffect = GameSound.BOSS_SHOCKWAVE,
            value1 = radius,
            value2 = towerSlowDuration
        )

        fun summonIceMinions(
            cooldown: Float = 9.0f,
            warningDuration: Float = 0.8f,
            minionsPerSummon: Int = 2
        ) = AbilityDefinition(
            type = AbilityType.SUMMON_ICE_MINIONS,
            name = "Summon Ice Minions",
            description = "Carves ice scouts out of permafrost to rush defenses",
            cooldown = cooldown,
            warningDuration = warningDuration,
            condition = ActivationCondition.BOSS_PHASE_2_OR_3,
            visualEffect = EffectType.SUMMON_RIFT,
            soundEffect = GameSound.SUMMON_MINIONS,
            value1 = minionsPerSummon.toFloat()
        )
    }
}

/**
 * Runtime state of an active or cooling-down ability on an Enemy instance.
 */
data class EnemyAbilityState(
    val definition: AbilityDefinition,
    val cooldownTimer: Float = 0f,
    val activeTimer: Float = 0f,
    val warningTimer: Float = 0f,
    val usesCount: Int = 0
) {
    val isWarning: Boolean get() = warningTimer > 0f
    val isActive: Boolean get() = activeTimer > 0f
    val isReady: Boolean get() = cooldownTimer <= 0f && activeTimer <= 0f && warningTimer <= 0f &&
            (definition.maxUses < 0 || usesCount < definition.maxUses)
}
