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
    val baseHp: Float = 80f,
    val baseSpeed: Float = 120f,
    val rewardCoins: Int = 12,
    val baseDamage: Int = GameConfig.DAMAGE_PER_ENEMY,
    val radius: Float = 22f,
    val primaryColorHex: Long = 0xFF4CAF50L,
    val secondaryColorHex: Long = 0xFF2E7D32L,
    val isBoss: Boolean = false
) {
    companion object {
        val SCOUT = EnemySpec(
            type = EnemyType.SCOUT,
            name = "Scout",
            baseHp = 35f,
            baseSpeed = 180f,
            rewardCoins = 8,
            baseDamage = 8,
            radius = 16f,
            primaryColorHex = 0xFFFFB703L, // Warm Golden Amber
            secondaryColorHex = 0xFFFB8500L
        )

        val SOLDIER = EnemySpec(
            type = EnemyType.SOLDIER,
            name = "Soldier",
            baseHp = 85f,
            baseSpeed = 120f,
            rewardCoins = 12,
            baseDamage = 10,
            radius = 20f,
            primaryColorHex = 0xFF2A9D8FL, // Forest Teal
            secondaryColorHex = 0xFF264653L
        )

        val HEAVY = EnemySpec(
            type = EnemyType.HEAVY,
            name = "Heavy",
            baseHp = 260f,
            baseSpeed = 70f,
            rewardCoins = 25,
            baseDamage = 20,
            radius = 28f,
            primaryColorHex = 0xFFE76F51L, // Rust Brick / Crimson Iron
            secondaryColorHex = 0xFF9A031EL
        )

        val RUNNER = EnemySpec(
            type = EnemyType.RUNNER,
            name = "Runner",
            baseHp = 28f,
            baseSpeed = 230f,
            rewardCoins = 10,
            baseDamage = 8,
            radius = 15f,
            primaryColorHex = 0xFF00B4D8L, // Electric Cyan
            secondaryColorHex = 0xFF0077B6L
        )

        fun createBoss(wave: Int): EnemySpec {
            val (name, hpMultiplier) = when (wave) {
                5 -> "Iron Golem" to 1.0f
                10 -> "Siege Titan" to 1.8f
                15 -> "Dreadnought" to 2.8f
                else -> "Overlord Colossus" to 4.2f
            }
            return EnemySpec(
                type = EnemyType.BOSS,
                name = name,
                baseHp = 600f * hpMultiplier,
                baseSpeed = 55f,
                rewardCoins = (100 * hpMultiplier).toInt(),
                baseDamage = GameConfig.DAMAGE_PER_BOSS,
                radius = 38f,
                primaryColorHex = 0xFF8338ECL, // Regal Royal Purple
                secondaryColorHex = 0xFFFF006EL,
                isBoss = true
            )
        }
    }
}

/**
 * Individual enemy instance traveling on the battlefield.
 */
data class Enemy(
    val id: String = UUID.randomUUID().toString(),
    val spec: EnemySpec = EnemySpec.SOLDIER,
    val maxHp: Float = spec.baseHp,
    val currentHp: Float = spec.baseHp,
    val position: Point2D = Point2D(-40f, 180f),
    val currentSegmentIndex: Int = 0,
    val distanceOnSegment: Float = 0f,
    val totalProgress: Float = 0f,
    val isAlive: Boolean = true,
    val reachedBase: Boolean = false,
    val animWobbleTime: Float = 0f,
    val hitFlashTimer: Float = 0f
) {
    val healthPercentage: Float get() = (currentHp / maxHp).coerceIn(0f, 1f)
    val isHitFlashing: Boolean get() = hitFlashTimer > 0f

    fun takeDamage(amount: Float): Enemy {
        val newHp = currentHp - amount
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
            position = currentPos,
            currentSegmentIndex = newSegmentIndex,
            distanceOnSegment = newDistanceOnSegment,
            totalProgress = progress,
            animWobbleTime = animWobbleTime + dt * 8f,
            hitFlashTimer = updatedFlashTimer
        )
    }
}
