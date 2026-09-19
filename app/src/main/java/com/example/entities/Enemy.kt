package com.example.entities

import com.example.data.GameConfig
import java.util.UUID

enum class EnemyType {
    SCOUT,
    SOLDIER,
    HEAVY,
    RUNNER,
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
    val bulletResistance: Float = 0f
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
            bulletResistance = 0.70f // 70% deflection against normal guns
        )

        val RUNNER = EnemySpec(
            type = EnemyType.RUNNER,
            name = "Runner",
            baseHp = 40f,
            armor = 1f,
            baseSpeed = 235f,
            rewardCoins = 5,
            baseDamage = 8,
            radius = 15f,
            primaryColorHex = 0xFF00B4D8L, // Electric Cyan
            secondaryColorHex = 0xFF0077B6L,
            bulletResistance = 0f
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
            bulletResistance = 0.82f
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
                bulletResistance = resistance
            )
        }
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
    val slowFactor: Float = 0f
) {
    val healthPercentage: Float get() = (currentHp / maxHp).coerceIn(0f, 1f)
    val isHitFlashing: Boolean get() = hitFlashTimer > 0f
    val isSlowed: Boolean get() = slowTimer > 0f

    fun takeDamage(rawAmount: Float, armorPiercing: Float = 0f, isNormalGun: Boolean = false): Enemy {
        val effectiveRaw = if (isNormalGun && spec.bulletResistance > 0f) {
            rawAmount * (1f - spec.bulletResistance)
        } else {
            rawAmount
        }
        val baseArmor = if (isShielded) (spec.armor + 25f) else spec.armor
        val effectiveArmor = (baseArmor * (1f - armorPiercing.coerceIn(0f, 0.9f))).coerceAtLeast(0f)
        val minFloor = if (isNormalGun && spec.bulletResistance >= 0.5f) 0.04f else 0.15f
        val netDamage = kotlin.math.max(effectiveRaw * minFloor, effectiveRaw - effectiveArmor)
        val newHp = currentHp - netDamage
        return copy(
            currentHp = newHp,
            isAlive = newHp > 0f,
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

    /**
     * Advances the enemy along the path waypoints by distance = speed * dt.
     */
    fun advance(dt: Float, path: GamePath): Enemy {
        if (!isAlive || reachedBase) return this

        // Boss mechanics: Shield phase cycle and health regeneration
        var newShielded = isShielded
        var newShieldTimer = shieldTimer
        var newShieldCooldown = shieldCooldown
        var newHp = currentHp

        if (spec.isBoss) {
            // Regeneration
            if (spec.regenRate > 0f && newHp < maxHp) {
                newHp = kotlin.math.min(maxHp, newHp + spec.regenRate * dt)
            }

            // Shield phase
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

        val effectiveSlow = if (slowTimer > 0f) slowFactor else 0f
        val speedMultiplier = (1f - effectiveSlow).coerceIn(0.20f, 1f)
        val moveDist = speed * speedMultiplier * dt
        val updatedSlowTimer = (slowTimer - dt).coerceAtLeast(0f)
        val updatedSlowFactor = if (updatedSlowTimer <= 0f) 0f else slowFactor

        var newSegmentIndex = currentSegmentIndex
        var newDistanceOnSegment = distanceOnSegment + moveDist
        val updatedFlashTimer = (hitFlashTimer - dt).coerceAtLeast(0f)

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
        // Smooth angle rotation to prevent abrupt snapping on sharp path turns
        val angleDiff = (targetAngle - headingAngle + 540f) % 360f - 180f
        val newAngle = headingAngle + angleDiff * kotlin.math.min(1f, dt * 14f)

        val progress = path.calculateProgressDistance(newSegmentIndex, newDistanceOnSegment)

        return copy(
            currentHp = newHp,
            position = currentPos,
            currentSegmentIndex = newSegmentIndex,
            distanceOnSegment = newDistanceOnSegment,
            totalProgress = progress,
            animWobbleTime = animWobbleTime + dt * 8f,
            hitFlashTimer = updatedFlashTimer,
            isShielded = newShielded,
            shieldTimer = newShieldTimer,
            shieldCooldown = newShieldCooldown,
            headingAngle = newAngle,
            slowTimer = updatedSlowTimer,
            slowFactor = updatedSlowFactor
        )
    }
}
