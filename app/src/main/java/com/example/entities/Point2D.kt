package com.example.entities

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * 2D vector / coordinate point representation for game spatial calculations.
 */
data class Point2D(val x: Float, val y: Float) {

    fun distanceTo(other: Point2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun angleTo(other: Point2D): Float {
        val dx = other.x - x
        val dy = other.y - y
        return (atan2(dy.toDouble(), dx.toDouble()) * (180.0 / Math.PI)).toFloat()
    }

    operator fun plus(other: Point2D): Point2D = Point2D(x + other.x, y + other.y)

    operator fun minus(other: Point2D): Point2D = Point2D(x - other.x, y - other.y)

    operator fun times(scalar: Float): Point2D = Point2D(x * scalar, y * scalar)

    fun length(): Float = hypot(x, y)

    fun normalized(): Point2D {
        val len = length()
        return if (len > 0.0001f) Point2D(x / len, y / len) else Point2D(0f, 0f)
    }

    companion object {
        val Zero = Point2D(0f, 0f)
    }
}
