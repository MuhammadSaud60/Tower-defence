package com.example.entities

import com.example.data.GameConfig
import java.util.UUID

enum class DestructibleType(
    val displayName: String,
    val maxHp: Float,
    val rewardTokens: Int,
    val radius: Float
) {
    // Primary types explicitly specified
    TREE("Tree", 100f, 10, 28f),
    STONE("Stone", 120f, 15, 24f),
    LARGE_STONE("Large Stone", 250f, 25, 36f),

    // Archetype variants with full compatibility
    OAK_TREE("Oak Tree", 100f, 10, 30f),
    PINE_TREE("Pine Tree", 100f, 10, 28f),
    SMALL_STONE("Small Rock", 100f, 10, 22f),
    LARGE_BOULDER("Large Stone", 250f, 25, 36f),

    // Wooden Crates / Barrels
    WOODEN_CRATE("Supply Crate", 50f, 15, 24f),
    REINFORCED_CRATE("Reinforced Crate", 100f, 20, 28f);

    val isTree: Boolean get() = this == TREE || this == OAK_TREE || this == PINE_TREE
    val isStone: Boolean get() = this == STONE || this == LARGE_STONE || this == SMALL_STONE || this == LARGE_BOULDER
}

/**
 * Natural environment object situated in the battlefield.
 * Can be targeted and damaged by player defenses, rewarding coins upon destruction.
 */
data class DestructibleObject(
    val id: String = UUID.randomUUID().toString(),
    val type: DestructibleType,
    val position: Point2D,
    val maxHp: Float = type.maxHp,
    val currentHp: Float = maxHp,
    val rewardTokens: Int = type.rewardTokens,
    val radius: Float = type.radius,
    val hitFlashTimer: Float = 0f
) {
    // Explicit requested property names & aliases
    val health: Float get() = currentHp
    val maximumHealth: Float get() = maxHp
    val rewardValue: Int get() = rewardTokens
    val collisionRadius: Float get() = radius
    val isDestroyed: Boolean get() = currentHp <= 0f
    val isAlive: Boolean get() = currentHp > 0f
    val healthFraction: Float get() = (currentHp / maxHp).coerceIn(0f, 1f)
    val isHitFlashing: Boolean get() = hitFlashTimer > 0f

    fun takeDamage(damage: Float): DestructibleObject {
        val newHp = (currentHp - damage).coerceAtLeast(0f)
        return copy(
            currentHp = newHp,
            hitFlashTimer = 0.15f
        )
    }

    fun tickFlash(dt: Float): DestructibleObject {
        return if (hitFlashTimer > 0f) {
            copy(hitFlashTimer = (hitFlashTimer - dt).coerceAtLeast(0f))
        } else {
            this
        }
    }
}
