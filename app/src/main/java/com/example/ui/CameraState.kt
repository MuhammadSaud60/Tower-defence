package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.entities.Point2D

/**
 * Authoritative 2D World Camera state for horizontal landscape battlefield.
 * Maintains camera center position (in world coordinates) and zoom level.
 * Features fast, responsive 1:1 camera drag in world space and deterministic boundary clamping.
 */
class CameraState(
    initialZoom: Float = 0.90f,
    initialX: Float = 650f,
    initialY: Float = 500f
) {
    companion object {
        // Sensible zoom levels as specified in Section 5
        val ZOOM_LEVELS = floatArrayOf(0.50f, 0.60f, 0.70f, 0.80f, 0.90f, 1.00f, 1.15f, 1.30f, 1.50f)
        const val MIN_ZOOM = 0.45f
        const val MAX_ZOOM = 1.50f
    }

    var zoom by mutableFloatStateOf(initialZoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
    var cameraX by mutableFloatStateOf(initialX)
    var cameraY by mutableFloatStateOf(initialY)

    /**
     * Calculates the minimum zoom required so the map completely covers the viewport without void borders.
     */
    fun getMinCoverZoom(viewportWidth: Float, viewportHeight: Float, worldWidth: Float, worldHeight: Float): Float {
        if (worldWidth <= 0f || worldHeight <= 0f || viewportWidth <= 0f || viewportHeight <= 0f) return 0.55f
        val coverX = viewportWidth / worldWidth
        val coverY = viewportHeight / worldHeight
        return maxOf(0.45f, maxOf(coverX, coverY))
    }

    /**
     * Zooms in to the next higher preset zoom level (Section 5).
     */
    fun zoomIn(viewportWidth: Float, viewportHeight: Float, worldWidth: Float, worldHeight: Float) {
        val nextZoom = ZOOM_LEVELS.firstOrNull { it > zoom + 0.03f } ?: MAX_ZOOM
        zoom = nextZoom.coerceIn(getMinCoverZoom(viewportWidth, viewportHeight, worldWidth, worldHeight), MAX_ZOOM)
        clamp(viewportWidth, viewportHeight, worldWidth, worldHeight)
    }

    /**
     * Zooms out to the next lower preset zoom level (Section 5).
     */
    fun zoomOut(viewportWidth: Float, viewportHeight: Float, worldWidth: Float, worldHeight: Float) {
        val minLimit = getMinCoverZoom(viewportWidth, viewportHeight, worldWidth, worldHeight)
        val prevZoom = ZOOM_LEVELS.lastOrNull { it < zoom - 0.03f && it >= minLimit - 0.02f } ?: minLimit
        zoom = maxOf(minLimit, prevZoom)
        clamp(viewportWidth, viewportHeight, worldWidth, worldHeight)
    }

    /**
     * Camera Drag Implementation (Section 8, 9, 10):
     * Directly and responsively translates screen finger delta into world space:
     * deltaWorldX = deltaScreenX / zoom
     * deltaWorldY = deltaScreenY / zoom
     * Moves camera instantly so the battlefield follows the finger 1:1 naturally.
     */
    fun panBy(
        dxPx: Float,
        dyPx: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        worldWidth: Float,
        worldHeight: Float
    ) {
        if (zoom <= 0f) return
        val deltaWorldX = dxPx / zoom
        val deltaWorldY = dyPx / zoom
        cameraX -= deltaWorldX
        cameraY -= deltaWorldY
        clamp(viewportWidth, viewportHeight, worldWidth, worldHeight)
    }

    /**
     * Camera Bounds Clamping (Section 11):
     * Calculates the visible world dimensions based on viewportWidth / zoom and viewportHeight / zoom.
     * Clamps the camera so the visible viewport never goes outside [0..worldWidth, 0..worldHeight].
     * If the visible world dimension is larger than the map, centers the camera on that axis.
     */
    fun clamp(
        viewportWidth: Float,
        viewportHeight: Float,
        worldWidth: Float,
        worldHeight: Float
    ) {
        if (viewportWidth <= 0f || viewportHeight <= 0f || zoom <= 0f) return

        val visibleW = viewportWidth / zoom
        val visibleH = viewportHeight / zoom

        cameraX = if (worldWidth >= visibleW) {
            val halfW = visibleW / 2f
            cameraX.coerceIn(halfW, worldWidth - halfW)
        } else {
            worldWidth / 2f
        }

        cameraY = if (worldHeight >= visibleH) {
            val halfH = visibleH / 2f
            cameraY.coerceIn(halfH, worldHeight - halfH)
        } else {
            worldHeight / 2f
        }
    }

    /**
     * World-to-Screen Transformation (Section 17):
     * screenX = (worldX - cameraX) * zoom + viewportCenterX
     * screenY = (worldY - cameraY) * zoom + viewportCenterY
     */
    fun worldToScreen(
        worldX: Float,
        worldY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Offset {
        val sx = (worldX - cameraX) * zoom + viewportWidth / 2f
        val sy = (worldY - cameraY) * zoom + viewportHeight / 2f
        return Offset(sx, sy)
    }

    /**
     * Screen-to-World Transformation (Section 18):
     * worldX = (screenX - viewportCenterX) / zoom + cameraX
     * worldY = (screenY - viewportCenterY) / zoom + cameraY
     */
    fun screenToWorld(
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Point2D {
        val wx = (screenX - viewportWidth / 2f) / zoom + cameraX
        val wy = (screenY - viewportHeight / 2f) / zoom + cameraY
        return Point2D(wx, wy)
    }

    /**
     * Focuses or resets camera to a specific world position (Section 14).
     */
    fun resetTo(
        targetX: Float,
        targetY: Float,
        targetZoom: Float = 0.90f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        worldWidth: Float = 0f,
        worldHeight: Float = 0f
    ) {
        zoom = targetZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        cameraX = targetX
        cameraY = targetY
        if (viewportWidth > 0f && viewportHeight > 0f && worldWidth > 0f && worldHeight > 0f) {
            clamp(viewportWidth, viewportHeight, worldWidth, worldHeight)
        }
    }
}
