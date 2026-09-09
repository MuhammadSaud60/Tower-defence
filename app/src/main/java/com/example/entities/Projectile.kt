package com.example.entities

import com.example.data.GameConfig
import java.util.UUID

enum class ProjectileType {
    BULLET,
    CANNONBALL,
    PLASMA_BOLT
}

/**
 * Ballistic projectile entity moving toward an enemy target.
 */
data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    val type: ProjectileType = ProjectileType.BULLET,
    val currentPosition: Point2D,
    val targetEnemyId: String,
    val targetLastKnownPosition: Point2D,
    val damage: Float,
    val splashRadius: Float = 0f,
    val armorPiercing: Float = 0f,
    val speed: Float = GameConfig.BULLET_SPEED,
    val isHit: Boolean = false,
    val isExpired: Boolean = false,
    val lifetime: Float = 0f,
    val maxLifetime: Float = 3f
) {
    fun advance(dt: Float, currentEnemyPosition: Point2D?): Projectile {
        if (isHit || isExpired) return this

        val newLifetime = lifetime + dt
        if (newLifetime >= maxLifetime) {
            return copy(isExpired = true, lifetime = newLifetime)
        }

        // Aim at enemy current position or last known coordinates
        val destination = currentEnemyPosition ?: targetLastKnownPosition
        val distance = currentPosition.distanceTo(destination)
        val step = speed * dt

        return if (step >= distance || distance <= GameConfig.PROJECTILE_HIT_RADIUS) {
            // Reached target
            copy(
                currentPosition = destination,
                isHit = true,
                lifetime = newLifetime
            )
        } else {
            val fraction = step / distance
            val nextPos = Point2D(
                x = currentPosition.x + (destination.x - currentPosition.x) * fraction,
                y = currentPosition.y + (destination.y - currentPosition.y) * fraction
            )
            copy(
                currentPosition = nextPos,
                targetLastKnownPosition = destination,
                lifetime = newLifetime
            )
        }
    }
}
