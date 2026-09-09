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
    val rewardCoins: Int = 12,
    val baseDamage: Int = GameConfig.DAMAGE_PER_ENEMY,
    val radius: Float = 22f,
    val primaryColorHex: Long = 0xFF4CAF50L,
    val secondaryColorHex: Long = 0xFF2E7D32L,
    val isBoss: Boolean = false,
    val regenRate: Float = 0f
) {
    companion object {
        val SCOUT = EnemySpec(
            type = EnemyType.SCOUT,
            name = "Scout",
            baseHp = 45f,
            armor = 0f,
            baseSpeed = 175f,
            rewardCoins = 8,
            baseDamage = 6,
            radius = 16f,
            primaryColorHex = 0xFFFFB703L, // Warm Golden Amber
            secondaryColorHex = 0xFFFB8500L
        )

        val SOLDIER = EnemySpec(
            type = EnemyType.SOLDIER,
            name = "Soldier",
            baseHp = 120f,
            armor = 4f,
            baseSpeed = 110f,
            rewardCoins = 12,
            baseDamage = 10,
            radius = 20f,
            primaryColorHex = 0xFF2A9D8FL, // Forest Teal
            secondaryColorHex = 0xFF264653L
        )

        val HEAVY = EnemySpec(
            type = EnemyType.HEAVY,
            name = "Heavy",
            baseHp = 380f,
            armor = 16f,
            baseSpeed = 65f,
            rewardCoins = 28,
            baseDamage = 20,
            radius = 28f,
            primaryColorHex = 0xFFE76F51L, // Rust Brick / Crimson Iron
            secondaryColorHex = 0xFF9A031EL
        )

        val RUNNER = EnemySpec(
            type = EnemyType.RUNNER,
            name = "Runner",
            baseHp = 40f,
            armor = 1f,
            baseSpeed = 235f,
            rewardCoins = 10,
            baseDamage = 8,
            radius = 15f,
            primaryColorHex = 0xFF00B4D8L, // Electric Cyan
            secondaryColorHex = 0xFF0077B6L
        )

        fun createBoss(wave: Int): EnemySpec {
            val (name, hp, armor, regen, speed, coins) = when (wave) {
                5 -> Tuple6("Iron Golem", 1200f, 24f, 8f, 48f, 150)
                10 -> Tuple6("Siege Titan", 2200f, 30f, 14f, 44f, 250)
                15 -> Tuple6("Dreadnought", 3600f, 36f, 20f, 40f, 380)
                else -> Tuple6("Overlord Colossus", 5200f, 42f, 28f, 36f, 550)
            }
            return EnemySpec(
                type = EnemyType.BOSS,
                name = name,
                baseHp = hp,
                armor = armor,
                baseSpeed = speed,
                rewardCoins = coins,
                baseDamage = GameConfig.DAMAGE_PER_BOSS,
                radius = 38f,
                primaryColorHex = 0xFF8338ECL, // Regal Royal Purple
                secondaryColorHex = 0xFFFF006EL,
                isBoss = true,
                regenRate = regen
            )
        }
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)

/**
 * Individual enemy instance traveling on the battlefield.
 */
data class Enemy(
    val id: String = UUID.randomUUID().toString(),
    val spec: EnemySpec = EnemySpec.SOLDIER,
    val pathIndex: Int = 0,
    val maxHp: Float = spec.baseHp,
    val currentHp: Float = spec.baseHp,
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
    val isInTunnel: Boolean = false
) {
    val healthPercentage: Float get() = (currentHp / maxHp).coerceIn(0f, 1f)
    val isHitFlashing: Boolean get() = hitFlashTimer > 0f

    fun takeDamage(rawAmount: Float, armorPiercing: Float = 0f): Enemy {
        val baseArmor = if (isShielded) (spec.armor + 25f) else spec.armor
        val effectiveArmor = (baseArmor * (1f - armorPiercing.coerceIn(0f, 0.9f))).coerceAtLeast(0f)
        val netDamage = kotlin.math.max(rawAmount * 0.15f, rawAmount - effectiveArmor)
        val newHp = currentHp - netDamage
        return copy(
            currentHp = newHp,
            isAlive = newHp > 0f,
            hitFlashTimer = 0.14f
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

        val moveDist = spec.baseSpeed * dt
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
                hitFlashTimer = 0f
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
            shieldCooldown = newShieldCooldown
        )
    }
}
