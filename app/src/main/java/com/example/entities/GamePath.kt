package com.example.entities

import com.example.data.GameConfig
import kotlin.math.max
import kotlin.math.min

/**
 * Defines the fixed enemy traveling path across the battlefield.
 * Provides waypoint progression data and spatial collision checks for tower placement.
 */
class GamePath(
    val id: String = "main",
    val waypoints: List<Point2D> = listOf(
        Point2D(-40f, 180f),
        Point2D(340f, 180f),
        Point2D(340f, 500f),
        Point2D(780f, 500f),
        Point2D(780f, 860f),
        Point2D(220f, 860f),
        Point2D(220f, 1220f),
        Point2D(820f, 1220f)
    ),
    val pathWidth: Float = GameConfig.PATH_WIDTH
) {
    val totalWaypoints: Int get() = waypoints.size

    val startPoint: Point2D get() = waypoints.first()
    val endPoint: Point2D get() = waypoints.last()

    // Pre-calculated segment lengths
    val segmentLengths: List<Float> = waypoints.zipWithNext { a, b -> a.distanceTo(b) }
    val totalPathLength: Float = segmentLengths.sum()

    /**
     * Checks whether a candidate point collides with the path corridor.
     */
    fun isPointOnPath(point: Point2D, margin: Float = pathWidth / 2f): Boolean {
        return distanceToPath(point) < margin
    }

    /**
     * Calculates the minimum perpendicular distance from a point to any segment of the path.
     */
    fun distanceToPath(point: Point2D): Float {
        var minDistance = Float.MAX_VALUE

        for (i in 0 until waypoints.size - 1) {
            val a = waypoints[i]
            val b = waypoints[i + 1]
            val dist = distanceToSegment(point, a, b)
            if (dist < minDistance) {
                minDistance = dist
            }
        }
        return minDistance
    }

    private fun distanceToSegment(p: Point2D, a: Point2D, b: Point2D): Float {
        val abX = b.x - a.x
        val abY = b.y - a.y
        val apX = p.x - a.x
        val apY = p.y - a.y

        val abLenSq = abX * abX + abY * abY
        if (abLenSq == 0f) return p.distanceTo(a)

        // Projection factor clamped to [0, 1]
        val t = max(0f, min(1f, (apX * abX + apY * abY) / abLenSq))
        val projX = a.x + t * abX
        val projY = a.y + t * abY

        val dx = p.x - projX
        val dy = p.y - projY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Computes the total distance traveled from the start given the current segment and segment offset.
     */
    fun calculateProgressDistance(segmentIndex: Int, distanceOnSegment: Float): Float {
        var distance = 0f
        for (i in 0 until min(segmentIndex, segmentLengths.size)) {
            distance += segmentLengths[i]
        }
        return distance + distanceOnSegment
    }
}
