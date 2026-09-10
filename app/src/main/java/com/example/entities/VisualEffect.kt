package com.example.entities

import java.util.UUID

enum class EffectType {
    CANNON_EXPLOSION,
    MG_HIT_SPARK,
    RAPID_HIT_SPARK,
    BOSS_HIT_IMPACT,
    ENEMY_DEATH_POOF,
    SCOUT_DEATH,
    SOLDIER_DEATH,
    HEAVY_DEATH,
    BOSS_DEATH,
    BOSS_ENTRANCE;

    companion object {
        @JvmField val HIT_SPARK = MG_HIT_SPARK
    }
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
