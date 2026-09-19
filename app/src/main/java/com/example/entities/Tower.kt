package com.example.entities

import com.example.data.GameConfig
import java.util.UUID
import kotlin.math.max

enum class TowerType {
    MACHINE_GUN,
    CANNON,
    RAPID_FIRE,
    FROST_GUN
}

enum class TargetingStrategy {
    FIRST,
    LAST,
    STRONGEST,
    CLOSEST
}

/**
 * Specification for a specific tower archetype at a given level.
 */
data class TowerSpec(
    val type: TowerType = TowerType.MACHINE_GUN,
    val name: String = "Machine Gun",
    val level: Int = 1,
    val cost: Int = GameConfig.MG_TOWER_COST,
    val range: Float = GameConfig.MG_TOWER_RANGE,
    val damage: Float = GameConfig.MG_TOWER_DAMAGE,
    val attackCooldown: Float = GameConfig.MG_TOWER_COOLDOWN,
    val size: Float = GameConfig.TOWER_SIZE,
    val baseColorHex: Long = 0xFF5C677D,
    val turretColorHex: Long = 0xFF0466C8,
    val upgradeCost: Int = 35,
    val splashRadius: Float = 0f,
    val armorPiercing: Float = 0f,
    val slowFactor: Float = 0f,
    val slowDuration: Float = 0f
) {
    val attacksPerSecond: Float get() = if (attackCooldown > 0f) 1f / attackCooldown else 0f

    companion object {
        fun create(type: TowerType, level: Int = 1): TowerSpec {
            return when (type) {
                TowerType.MACHINE_GUN -> when (level) {
                    1 -> TowerSpec(
                        type = TowerType.MACHINE_GUN,
                        name = "Machine Gun",
                        level = 1,
                        cost = GameConfig.MG_TOWER_COST,
                        range = GameConfig.MG_TOWER_RANGE,
                        damage = GameConfig.MG_TOWER_DAMAGE,
                        attackCooldown = GameConfig.MG_TOWER_COOLDOWN,
                        baseColorHex = 0xFF4A5568,
                        turretColorHex = 0xFF3182CE,
                        upgradeCost = 35,
                        armorPiercing = 0f
                    )
                    2 -> TowerSpec(
                        type = TowerType.MACHINE_GUN,
                        name = "Dual MG Mk.II",
                        level = 2,
                        cost = 35,
                        range = 245f,
                        damage = 15f,
                        attackCooldown = 0.18f,
                        baseColorHex = 0xFF2D3748,
                        turretColorHex = 0xFF2B6CB0,
                        upgradeCost = 55,
                        armorPiercing = 0f
                    )
                    else -> TowerSpec(
                        type = TowerType.MACHINE_GUN,
                        name = "Gatling Vulcan Mk.III",
                        level = 3,
                        cost = 55,
                        range = 275f,
                        damage = 24f,
                        attackCooldown = 0.14f,
                        baseColorHex = 0xFF1A202C,
                        turretColorHex = 0xFF1A365D,
                        upgradeCost = 0,
                        armorPiercing = 0f
                    )
                }

                TowerType.CANNON -> when (level) {
                    1 -> TowerSpec(
                        type = TowerType.CANNON,
                        name = "Heavy Cannon",
                        level = 1,
                        cost = GameConfig.CANNON_TOWER_COST,
                        range = GameConfig.CANNON_TOWER_RANGE,
                        damage = GameConfig.CANNON_TOWER_DAMAGE,
                        attackCooldown = GameConfig.CANNON_TOWER_COOLDOWN,
                        baseColorHex = 0xFF744210,
                        turretColorHex = 0xFF975A16,
                        upgradeCost = 80,
                        splashRadius = GameConfig.CANNON_SPLASH_RADIUS,
                        armorPiercing = 0.50f
                    )
                    2 -> TowerSpec(
                        type = TowerType.CANNON,
                        name = "Siege Howitzer Mk.II",
                        level = 2,
                        cost = 80,
                        range = 340f,
                        damage = 120f,
                        attackCooldown = 1.35f,
                        baseColorHex = 0xFF5F370E,
                        turretColorHex = 0xFFB7791F,
                        upgradeCost = 130,
                        splashRadius = 110f,
                        armorPiercing = 0.55f
                    )
                    else -> TowerSpec(
                        type = TowerType.CANNON,
                        name = "Devastator Mortar Mk.III",
                        level = 3,
                        cost = 130,
                        range = 380f,
                        damage = 200f,
                        attackCooldown = 1.2f,
                        baseColorHex = 0xFF442609,
                        turretColorHex = 0xFFD69E2E,
                        upgradeCost = 0,
                        splashRadius = 135f,
                        armorPiercing = 0.60f
                    )
                }

                TowerType.RAPID_FIRE -> when (level) {
                    1 -> TowerSpec(
                        type = TowerType.RAPID_FIRE,
                        name = "Rapid Autocannon",
                        level = 1,
                        cost = GameConfig.RAPID_TOWER_COST,
                        range = GameConfig.RAPID_TOWER_RANGE,
                        damage = GameConfig.RAPID_TOWER_DAMAGE,
                        attackCooldown = GameConfig.RAPID_TOWER_COOLDOWN,
                        baseColorHex = 0xFF27272A,
                        turretColorHex = 0xFFD97706,
                        upgradeCost = 55,
                        armorPiercing = 0f
                    )
                    2 -> TowerSpec(
                        type = TowerType.RAPID_FIRE,
                        name = "Twin Autocannon Mk.II",
                        level = 2,
                        cost = 55,
                        range = 290f,
                        damage = 22f,
                        attackCooldown = 0.07f,
                        baseColorHex = 0xFF18181B,
                        turretColorHex = 0xFFF59E0B,
                        upgradeCost = 90,
                        armorPiercing = 0.10f
                    )
                    else -> TowerSpec(
                        type = TowerType.RAPID_FIRE,
                        name = "Storm Battery Mk.III",
                        level = 3,
                        cost = 90,
                        range = 330f,
                        damage = 35f,
                        attackCooldown = 0.05f,
                        baseColorHex = 0xFF09090B,
                        turretColorHex = 0xFFFBBF24,
                        upgradeCost = 0,
                        armorPiercing = 0.20f
                    )
                }

                TowerType.FROST_GUN -> when (level) {
                    1 -> TowerSpec(
                        type = TowerType.FROST_GUN,
                        name = "Cryo Blaster",
                        level = 1,
                        cost = GameConfig.FROST_TOWER_COST,
                        range = GameConfig.FROST_TOWER_RANGE,
                        damage = GameConfig.FROST_TOWER_DAMAGE,
                        attackCooldown = GameConfig.FROST_TOWER_COOLDOWN,
                        baseColorHex = 0xFF0F172A,
                        turretColorHex = 0xFF06B6D4,
                        upgradeCost = 50,
                        splashRadius = 0f,
                        armorPiercing = 0.10f,
                        slowFactor = GameConfig.FROST_TOWER_SLOW_FACTOR,
                        slowDuration = GameConfig.FROST_TOWER_SLOW_DURATION
                    )
                    2 -> TowerSpec(
                        type = TowerType.FROST_GUN,
                        name = "Glacial Cannon Mk.II",
                        level = 2,
                        cost = 50,
                        range = 265f,
                        damage = 14f,
                        attackCooldown = 0.65f,
                        baseColorHex = 0xFF082F49,
                        turretColorHex = 0xFF0EA5E9,
                        upgradeCost = 80,
                        splashRadius = 60f,
                        armorPiercing = 0.20f,
                        slowFactor = 0.55f,
                        slowDuration = 3.2f
                    )
                    else -> TowerSpec(
                        type = TowerType.FROST_GUN,
                        name = "Blizzard Mortar Mk.III",
                        level = 3,
                        cost = 80,
                        range = 300f,
                        damage = 26f,
                        attackCooldown = 0.55f,
                        baseColorHex = 0xFF042F2E,
                        turretColorHex = 0xFF38BDF8,
                        upgradeCost = 0,
                        splashRadius = 85f,
                        armorPiercing = 0.30f,
                        slowFactor = 0.68f,
                        slowDuration = 4.0f
                    )
                }
            }
        }
    }
}

/**
 * Type of target currently acquired or manually designated for a tower.
 */
enum class TargetType {
    ENEMY,
    DESTRUCTIBLE
}

/**
 * Tower entity placed on the battlefield.
 */
data class Tower(
    val id: String = UUID.randomUUID().toString(),
    val spec: TowerSpec = TowerSpec.create(TowerType.MACHINE_GUN, 1),
    val position: Point2D,
    val cooldownTimer: Float = 0f,
    val rotationAngle: Float = 0f,
    val targetType: TargetType = TargetType.ENEMY,
    val targetId: String? = null,
    val targetEnemyId: String? = targetId,
    val manualTargetId: String? = null,
    val manualTargetType: TargetType? = null,
    val targetingStrategy: TargetingStrategy = TargetingStrategy.FIRST,
    val totalCoinsInvested: Int = spec.cost
) {
    val currentTargetId: String? get() = manualTargetId ?: targetId ?: targetEnemyId
    val currentTargetType: TargetType get() = manualTargetType ?: targetType
    val canAttack: Boolean get() = cooldownTimer <= 0f
    val isMaxLevel: Boolean get() = spec.level >= 3
    val sellRefundCoins: Int get() = (totalCoinsInvested * 0.7f).toInt().coerceAtLeast(10)
    val isFiring: Boolean get() {
        val flashDuration = when (spec.type) {
            TowerType.MACHINE_GUN -> 0.055f
            TowerType.CANNON -> 0.18f
            TowerType.RAPID_FIRE -> 0.038f
            TowerType.FROST_GUN -> 0.12f
        }
        return cooldownTimer > (spec.attackCooldown - flashDuration)
    }

    val recoilFraction: Float get() {
        val window = when (spec.type) {
            TowerType.MACHINE_GUN -> 0.08f
            TowerType.CANNON -> 0.32f
            TowerType.RAPID_FIRE -> 0.05f
            TowerType.FROST_GUN -> 0.16f
        }
        val elapsed = spec.attackCooldown - cooldownTimer
        return if (elapsed in 0f..window) {
            (1f - (elapsed / window)).coerceIn(0f, 1f)
        } else 0f
    }

    fun tickCooldown(dt: Float): Tower {
        val newTimer = max(0f, cooldownTimer - dt)
        return if (newTimer != cooldownTimer) copy(cooldownTimer = newTimer) else this
    }

    fun resetCooldown(): Tower {
        return copy(cooldownTimer = spec.attackCooldown)
    }

    fun aimAt(targetPosition: Point2D, targetId: String?, type: TargetType = TargetType.ENEMY): Tower {
        val angle = position.angleTo(targetPosition)
        return copy(
            rotationAngle = angle,
            targetId = targetId,
            targetEnemyId = targetId,
            targetType = type
        )
    }

    fun clearTarget(): Tower {
        return copy(
            targetId = null,
            targetEnemyId = null,
            targetType = TargetType.ENEMY
        )
    }

    fun setManualTarget(targetId: String?, type: TargetType? = if (targetId != null) TargetType.DESTRUCTIBLE else null): Tower {
        return copy(
            manualTargetId = targetId,
            manualTargetType = type,
            targetId = targetId,
            targetEnemyId = targetId,
            targetType = type ?: TargetType.ENEMY
        )
    }

    fun clearManualTarget(): Tower {
        return copy(
            manualTargetId = null,
            manualTargetType = null,
            targetId = null,
            targetEnemyId = null,
            targetType = TargetType.ENEMY
        )
    }

    fun isEnemyInRange(enemyPosition: Point2D): Boolean {
        return position.distanceTo(enemyPosition) <= spec.range
    }

    fun isObjectInRange(objectPosition: Point2D, objectRadius: Float = 0f): Boolean {
        val dist = position.distanceTo(objectPosition)
        return (dist - objectRadius) <= spec.range
    }

    fun upgrade(): Tower? {
        if (isMaxLevel) return null
        val nextLevel = spec.level + 1
        val upgradedSpec = TowerSpec.create(spec.type, nextLevel)
        return copy(
            spec = upgradedSpec,
            totalCoinsInvested = totalCoinsInvested + spec.upgradeCost
        )
    }

    fun setStrategy(newStrategy: TargetingStrategy): Tower {
        return copy(targetingStrategy = newStrategy)
    }
}
