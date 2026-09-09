package com.example.entities

import java.util.UUID

enum class EffectType {
    CANNON_EXPLOSION,
    HIT_SPARK,
    ENEMY_DEATH_POOF
}

data class VisualEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val position: Point2D,
    val maxLifetime: Float,
    val currentLifetime: Float = 0f,
    val maxRadius: Float
) {
    val progress: Float get() = (currentLifetime / maxLifetime).coerceIn(0f, 1f)
    val isFinished: Boolean get() = currentLifetime >= maxLifetime

    fun advance(dt: Float): VisualEffect {
        return copy(currentLifetime = currentLifetime + dt)
    }
}
