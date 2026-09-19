package com.example.systems

import com.example.entities.Enemy
import com.example.entities.EnemySpec
import com.example.entities.Point2D
import kotlin.math.min

/**
 * System that scales enemy health and movement speed dynamically based on
 * the number of waves cleared, ensuring later stages provide escalating challenge.
 */
class EnemyScalingSystem(
    val hpScalePerWave: Float = DEFAULT_HP_SCALE_PER_WAVE,
    val speedScalePerWave: Float = DEFAULT_SPEED_SCALE_PER_WAVE,
    val maxSpeedMultiplier: Float = DEFAULT_MAX_SPEED_MULTIPLIER,
    val bossHpScalePerWave: Float = DEFAULT_BOSS_HP_SCALE_PER_WAVE,
    val bossSpeedScalePerWave: Float = DEFAULT_BOSS_SPEED_SCALE_PER_WAVE,
    val maxBossSpeedMultiplier: Float = DEFAULT_MAX_BOSS_SPEED_MULTIPLIER
) {
    companion object {
        const val DEFAULT_HP_SCALE_PER_WAVE = 0.08f // +8% HP per wave cleared
        const val DEFAULT_SPEED_SCALE_PER_WAVE = 0.015f // +1.5% Speed per wave cleared
        const val DEFAULT_MAX_SPEED_MULTIPLIER = 1.35f // Cap at +35% movement speed for regular enemies
        const val DEFAULT_BOSS_HP_SCALE_PER_WAVE = 0.06f // +6% HP per wave cleared for bosses
        const val DEFAULT_BOSS_SPEED_SCALE_PER_WAVE = 0.010f // +1% Speed per wave cleared for bosses
        const val DEFAULT_MAX_BOSS_SPEED_MULTIPLIER = 1.20f // Cap at +20% movement speed for bosses

        val DEFAULT = EnemyScalingSystem()
    }

    /**
     * Calculates the health multiplier based on waves cleared.
     * At 0 waves cleared (Wave 1), multiplier is 1.0.
     */
    fun getHpMultiplier(wavesCleared: Int, isBoss: Boolean = false): Float {
        val safeCleared = wavesCleared.coerceAtLeast(0)
        val rate = if (isBoss) bossHpScalePerWave else hpScalePerWave
        return 1.0f + (safeCleared * rate)
    }

    /**
     * Calculates the speed multiplier based on waves cleared, clamped by the maximum allowed multiplier.
     * At 0 waves cleared (Wave 1), multiplier is 1.0.
     */
    fun getSpeedMultiplier(wavesCleared: Int, isBoss: Boolean = false): Float {
        val safeCleared = wavesCleared.coerceAtLeast(0)
        val rate = if (isBoss) bossSpeedScalePerWave else speedScalePerWave
        val maxMult = if (isBoss) maxBossSpeedMultiplier else maxSpeedMultiplier
        return min(maxMult, 1.0f + (safeCleared * rate))
    }

    /**
     * Computes the scaled HP for an enemy.
     */
    fun calculateScaledHp(baseHp: Float, wavesCleared: Int, isBoss: Boolean = false): Float {
        return baseHp * getHpMultiplier(wavesCleared, isBoss)
    }

    /**
     * Computes the scaled speed for an enemy.
     */
    fun calculateScaledSpeed(baseSpeed: Float, wavesCleared: Int, isBoss: Boolean = false): Float {
        return baseSpeed * getSpeedMultiplier(wavesCleared, isBoss)
    }

    /**
     * Creates an Enemy instance with scaled HP and speed attributes based on waves cleared.
     */
    fun createScaledEnemy(
        spec: EnemySpec,
        wavesCleared: Int,
        pathIndex: Int = 0,
        position: Point2D,
        headingAngle: Float = 0f
    ): Enemy {
        val scaledHp = calculateScaledHp(spec.baseHp, wavesCleared, spec.isBoss)
        val scaledSpeed = calculateScaledSpeed(spec.baseSpeed, wavesCleared, spec.isBoss)
        return Enemy(
            spec = spec,
            pathIndex = pathIndex,
            maxHp = scaledHp,
            currentHp = scaledHp,
            speed = scaledSpeed,
            position = position,
            headingAngle = headingAngle
        )
    }
}
