package com.example.entities

import com.example.data.GameConfig
import kotlin.math.max

/**
 * The player's main base fortress that must be protected.
 */
data class Base(
    val position: Point2D = Point2D(820f, 1220f),
    val width: Float = GameConfig.BASE_WIDTH,
    val height: Float = GameConfig.BASE_HEIGHT,
    val maxHp: Int = GameConfig.BASE_MAX_HP,
    val currentHp: Int = GameConfig.BASE_MAX_HP
) {
    val isDestroyed: Boolean get() = currentHp <= 0

    val healthPercentage: Float get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)

    fun takeDamage(amount: Int): Base {
        val newHp = max(0, currentHp - amount)
        return copy(currentHp = newHp)
    }

    fun isPointInside(point: Point2D): Boolean {
        val halfW = width / 2f
        val halfH = height / 2f
        return point.x >= position.x - halfW &&
                point.x <= position.x + halfW &&
                point.y >= position.y - halfH &&
                point.y <= position.y + halfH
    }
}
