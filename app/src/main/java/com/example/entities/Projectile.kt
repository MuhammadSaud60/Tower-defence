package com.example.entities

import com.example.data.GameConfig
import java.util.UUID

enum class ProjectileType {
    BULLET,
    CANNONBALL,
    RAPID_SLUG,
    CRYO_ORB;

    companion object {
        @JvmField val PLASMA_BOLT = RAPID_SLUG
        @JvmField val FROST_BOLT = CRYO_ORB
    }
}

/**
 * Ballistic projectile entity moving toward an enemy or destructible target.
 */
data class Projectile(
    val id: String = UUID.randomUUID().toString(),
    val type: ProjectileType = ProjectileType.BULLET,
    val currentPosition: Point2D,
    val prevPosition: Point2D = currentPosition,
    val targetId: String = "",
    val targetType: TargetType = TargetType.ENEMY,
    val targetWorldPosition: Point2D = currentPosition,
    val targetEnemyId: String = targetId,
    val targetLastKnownPosition: Point2D = targetWorldPosition,
    val damage: Float,
    val splashRadius: Float = 0f,
    val armorPiercing: Float = 0f,
    val slowFactor: Float = 0f,
    val slowDuration: Float = 0f,
    val speed: Float = GameConfig.BULLET_SPEED,
    val isHit: Boolean = false,
    val isExpired: Boolean = false,
    val lifetime: Float = 0f,
    val maxLifetime: Float = 3f
) {
    fun advance(dt: Float, currentTargetPosition: Point2D? = null): Projectile {
        if (isHit || isExpired) return this

        val newLifetime = lifetime + dt
        if (newLifetime >= maxLifetime) {
            return copy(isExpired = true, lifetime = newLifetime)
        }

        // Aim at target's current world coordinates or destination
        val destination = currentTargetPosition ?: targetWorldPosition
        val distance = currentPosition.distanceTo(destination)
        val step = speed * dt

        return if (step >= distance || distance <= GameConfig.PROJECTILE_HIT_RADIUS) {
            // Reached target
            copy(
                prevPosition = currentPosition,
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
                prevPosition = currentPosition,
                currentPosition = nextPos,
                targetWorldPosition = destination,
                targetLastKnownPosition = destination,
                lifetime = newLifetime
            )
        }
    }
}
