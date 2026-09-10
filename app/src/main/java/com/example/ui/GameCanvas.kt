package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DecorationType
import com.example.data.EnvironmentType
import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.data.MapDecoration
import com.example.data.TunnelRegion
import com.example.entities.Base
import com.example.entities.EffectType
import com.example.entities.Enemy
import com.example.entities.EnemyType
import com.example.entities.GamePath
import com.example.entities.Point2D
import com.example.entities.Projectile
import com.example.entities.ProjectileType
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import com.example.entities.TowerType
import com.example.entities.VisualEffect
import com.example.game.GameState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2D Cartoon / Natural Mobile Game Canvas Battlefield Renderer.
 * Features rolling green hills, dirt trail with pebbles, serene lake,
 * lush trees, rocks, cartoon defense turrets, animated enemies,
 * projectiles, explosion effects, and placement preview.
 *
 * Implements a real 2D camera system with smooth drag panning,
 * animated zoom via floating corner controls, boundary clamping,
 * and aligned radial menus.
 */
@Composable
fun GameCanvas(
    gameState: GameState,
    onCanvasTap: (Float, Float) -> Unit,
    onPlacementDrag: ((Float, Float) -> Unit)? = null,
    onUpgradeTower: (() -> Unit)? = null,
    onSellTower: (() -> Unit)? = null,
    onStrategyChange: ((TargetingStrategy) -> Unit)? = null,
    onDeselectTower: (() -> Unit)? = null,
    onSelectBuildTower: ((TowerType) -> Unit)? = null,
    onCloseBuildMenu: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        val currentMap = gameState.currentMap
        val worldWidth = currentMap.worldWidth
        val worldHeight = currentMap.worldHeight

        // Dedicated Authoritative 2D Camera Architecture for Landscape Battlefield
        val cameraState = remember(currentMap.id) {
            CameraState(
                initialZoom = currentMap.defaultZoom,
                initialX = currentMap.startingCameraCenter.x,
                initialY = currentMap.startingCameraCenter.y
            )
        }

        // Clamp camera position when viewport dimensions are measured or map changes
        LaunchedEffect(currentMap.id, maxWidthPx, maxHeightPx) {
            if (maxWidthPx > 0f && maxHeightPx > 0f) {
                cameraState.clamp(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
            }
        }

        val composePaths = remember(currentMap.paths) {
            currentMap.paths.map { path ->
                val p = Path()
                val waypoints = path.waypoints
                if (waypoints.isNotEmpty()) {
                    p.moveTo(waypoints[0].x, waypoints[0].y)
                    for (i in 1 until waypoints.size) {
                        p.lineTo(waypoints[i].x, waypoints[i].y)
                    }
                }
                path to p
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentMap.id, maxWidthPx, maxHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        var isDragging = false
                        val touchSlop = viewConfiguration.touchSlop
                        var totalDragDistance = 0f
                        val startPos = down.position
                        var prevPos = down.position
                        var prevDistance = -1f

                        do {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.size >= 2) {
                                // Multi-touch pinch to zoom
                                val p0 = activePointers[0]
                                val p1 = activePointers[1]
                                val currentDist = (p0.position - p1.position).getDistance()
                                if (prevDistance > 0f && currentDist > 10f) {
                                    val zoomFactor = currentDist / prevDistance
                                    if (zoomFactor in 0.75f..1.35f) {
                                        val minLimit = cameraState.getMinCoverZoom(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
                                        cameraState.zoom = (cameraState.zoom * zoomFactor).coerceIn(minLimit, CameraState.MAX_ZOOM)
                                        cameraState.clamp(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
                                    }
                                }
                                prevDistance = currentDist
                                isDragging = true
                                p0.consume()
                                p1.consume()
                            } else if (activePointers.size == 1) {
                                prevDistance = -1f
                                val pointer = activePointers[0]
                                val currentPos = pointer.position
                                val delta = currentPos - prevPos
                                totalDragDistance += delta.getDistance()

                                if (totalDragDistance > touchSlop) {
                                    isDragging = true
                                }

                                if (isDragging) {
                                    cameraState.panBy(
                                        dxPx = delta.x,
                                        dyPx = delta.y,
                                        viewportWidth = maxWidthPx,
                                        viewportHeight = maxHeightPx,
                                        worldWidth = worldWidth,
                                        worldHeight = worldHeight
                                    )
                                    pointer.consume()
                                }
                                prevPos = currentPos
                            }
                        } while (event.changes.any { it.pressed })

                        // Gesture completed: single tap only if finger did not drag
                        if (!isDragging && totalDragDistance < touchSlop) {
                            val worldPt = cameraState.screenToWorld(
                                screenX = startPos.x,
                                screenY = startPos.y,
                                viewportWidth = maxWidthPx,
                                viewportHeight = maxHeightPx
                            )
                            if (worldPt.x in 0f..worldWidth && worldPt.y in 0f..worldHeight) {
                                onCanvasTap(worldPt.x, worldPt.y)
                            }
                        }
                    }
                }
        ) {
            // Fill background with environment-matched terrain color to eliminate dark void borders
            val envBaseColor = when (currentMap.environmentType) {
                EnvironmentType.GREEN_VALLEY -> Color(0xFF4C8C2B)
                EnvironmentType.DESERT_CANYON -> Color(0xFFC49A6C)
                EnvironmentType.FOREST_CROSSROADS -> Color(0xFF264E21)
                EnvironmentType.OBSIDIAN_TUNNEL -> Color(0xFF1E293B)
                EnvironmentType.DRAGON_COIL -> Color(0xFF332F2B)
            }
            drawRect(color = envBaseColor, size = size)

            // True 2D Camera Transformation
            withTransform({
                translate(
                    left = maxWidthPx / 2f - cameraState.cameraX * cameraState.zoom,
                    top = maxHeightPx / 2f - cameraState.cameraY * cameraState.zoom
                )
                scale(scaleX = cameraState.zoom, scaleY = cameraState.zoom, pivot = Offset.Zero)
            }) {
                // 1. Natural Terrain spanning the full world dimensions
                drawNaturalTerrain(currentMap)

                // 2. Water pond if present
                drawWaterPond(currentMap, gameState.gameTime)

                // 3. Roads across all paths
                for ((gamePath, composeP) in composePaths) {
                    drawDirtRoad(composeP, gamePath, currentMap.environmentType)
                }

                // 4. Ground Decorations (Trees, Rocks, Bushes, Crystals)
                drawDecorations(currentMap.decorations)

                // 5. Entrance Cave / Spawn Gate for each path
                for (sp in currentMap.spawnPoints) {
                    drawSpawnGate(sp, worldWidth, worldHeight)
                }

                // 6. Tunnel entrances/exits if present
                if (gameState.currentMap.tunnelRegion != null) {
                    drawTunnelPortals(gameState.currentMap.tunnelRegion!!)
                }

                // 7. Player Castle Fortress Base
                drawCastleBase(gameState.base)

                // 8. Placed Defense Towers
                drawTowers(gameState.towers, gameState.selectedExistingTower, gameState.gameTime)

                // 9. Active Enemies with animations
                drawEnemies(gameState.enemies, gameState.gameTime)

                // 10. Tunnel Mountain Canopy (Drawn OVER enemies so enemies pass under it!)
                if (gameState.currentMap.tunnelRegion != null) {
                    drawTunnelCavernCanopy(gameState.currentMap.tunnelRegion!!, gameState.gameTime)
                }

                // 11. Ballistic Projectiles
                drawProjectiles(gameState.projectiles)

                // 12. Combat Particles & Explosions
                drawVisualEffects(gameState.effects)

                // 13. Tower Placement Preview (Legacy dragging)
                if (gameState.isBuildingTower) {
                    drawPlacementPreview(gameState)
                }

                // 14. Level 1 Tutorial Beacon (Glowing beacon on recommended build plot)
                if (gameState.currentMap.id == "green_valley" && gameState.towers.isEmpty() && gameState.selectedBuildPos == null) {
                    val plotPos = TUTORIAL_RECOMMENDED_PLOT
                    val pulse = sin(gameState.gameTime * 4.5f) * 6f
                    drawCircle(
                        color = Color(0x440284C7),
                        radius = GameConfig.TOWER_SIZE * 1.05f + pulse,
                        center = Offset(plotPos.x, plotPos.y)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = GameConfig.TOWER_SIZE * 0.9f,
                        center = Offset(plotPos.x, plotPos.y),
                        style = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), gameState.gameTime * 20f))
                    )
                    // Downward beacon arrow
                    val arrowY = plotPos.y - GameConfig.TOWER_SIZE * 1.15f - (sin(gameState.gameTime * 6f) * 6f)
                    val arrowPath = Path().apply {
                        moveTo(plotPos.x - 12f, arrowY - 16f)
                        lineTo(plotPos.x + 12f, arrowY - 16f)
                        lineTo(plotPos.x, arrowY)
                        close()
                    }
                    drawPath(arrowPath, Color(0xFFFACC15))
                }

                // 15. Selected Empty Build Plot Marker
                if (gameState.selectedBuildPos != null) {
                    val pos = gameState.selectedBuildPos
                    drawCircle(
                        color = Color(0x3338BDF8),
                        radius = GameConfig.TOWER_SIZE * 0.9f,
                        center = Offset(pos.x, pos.y)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = GameConfig.TOWER_SIZE * 0.9f,
                        center = Offset(pos.x, pos.y),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }
        }

        // Compact Circular Camera Zoom Controls (Section 4: 36-40 dp each, stacked vertically)
        val minCoverZoom = cameraState.getMinCoverZoom(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
        val canZoomOut = cameraState.zoom > minCoverZoom + 0.02f
        val canZoomIn = cameraState.zoom < CameraState.MAX_ZOOM - 0.02f

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "−" = Zoom Out Button
            Surface(
                shape = CircleShape,
                color = Color(0xEE0F172A),
                border = BorderStroke(1.5.dp, if (canZoomOut) Color(0x8838BDF8) else Color(0x3364748B)),
                shadowElevation = 4.dp,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(
                    onClick = {
                        cameraState.zoomOut(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
                    },
                    enabled = canZoomOut,
                    modifier = Modifier.fillMaxSize().testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = if (canZoomOut) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // "+" = Zoom In Button
            Surface(
                shape = CircleShape,
                color = Color(0xEE0F172A),
                border = BorderStroke(1.5.dp, if (canZoomIn) Color(0x8838BDF8) else Color(0x3364748B)),
                shadowElevation = 4.dp,
                modifier = Modifier.size(38.dp)
            ) {
                IconButton(
                    onClick = {
                        cameraState.zoomIn(maxWidthPx, maxHeightPx, worldWidth, worldHeight)
                    },
                    enabled = canZoomIn,
                    modifier = Modifier.fillMaxSize().testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = if (canZoomIn) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 1. Mobile Game Style Radial Tower Menu around selected tower
        val selectedTower = gameState.selectedExistingTower
        if (selectedTower != null && !gameState.isBuildingTower && gameState.selectedBuildPos == null) {
            val towerScreenPos = cameraState.worldToScreen(
                worldX = selectedTower.position.x,
                worldY = selectedTower.position.y,
                viewportWidth = maxWidthPx,
                viewportHeight = maxHeightPx
            )

            TowerRadialMenu(
                tower = selectedTower,
                screenX = towerScreenPos.x,
                screenY = towerScreenPos.y,
                maxWidthPx = maxWidthPx,
                maxHeightPx = maxHeightPx,
                playerCoins = gameState.coins,
                onUpgrade = { onUpgradeTower?.invoke() },
                onSell = { onSellTower?.invoke() },
                onStrategyChange = { strat -> onStrategyChange?.invoke(strat) },
                onClose = { onDeselectTower?.invoke() }
            )
        }

        // 2. Mobile Game Style Radial Tower Build Menu on empty buildable area
        val buildPos = gameState.selectedBuildPos
        if (buildPos != null) {
            val buildScreenPos = cameraState.worldToScreen(
                worldX = buildPos.x,
                worldY = buildPos.y,
                viewportWidth = maxWidthPx,
                viewportHeight = maxHeightPx
            )

            TowerBuildRadialMenu(
                buildPos = buildPos,
                screenX = buildScreenPos.x,
                screenY = buildScreenPos.y,
                maxWidthPx = maxWidthPx,
                maxHeightPx = maxHeightPx,
                playerCoins = gameState.coins,
                highlightGunner = (gameState.currentMap.id == "green_valley" && gameState.towers.isEmpty()),
                onSelectTower = { type -> onSelectBuildTower?.invoke(type) },
                onClose = { onCloseBuildMenu?.invoke() }
            )
        }
    }
}

/**
 * Draws natural terrain tailored for each map's distinct environment.
 */
private fun DrawScope.drawNaturalTerrain(map: GameMap) {
    val arenaSize = Size(map.worldWidth, map.worldHeight)

    when (map.environmentType) {
        EnvironmentType.GREEN_VALLEY -> {
            // Deep base meadow green
            drawRect(color = Color(0xFF5D9A38), size = arenaSize)

            // Layered rolling hill waves (organic natural curves)
            drawHillLayer(Color(0xFF6AA743), 180f, 320f, map.worldWidth)
            drawHillLayer(Color(0xFF538C2F), 460f, 620f, map.worldWidth)
            drawHillLayer(Color(0xFF6AA743), 780f, 960f, map.worldWidth)
            drawHillLayer(Color(0xFF538C2F), 1100f, 1260f, map.worldWidth)

            // Scattered cartoon grass patches
            drawGrassTufts()
        }
        EnvironmentType.DESERT_CANYON -> {
            // Warm sandstone canyon base
            drawRect(color = Color(0xFFD4A373), size = arenaSize)
            drawHillLayer(Color(0xFFBC6C25), 180f, 340f, map.worldWidth)
            drawHillLayer(Color(0xFFCC8B46), 460f, 640f, map.worldWidth)
            drawHillLayer(Color(0xFFBC6C25), 780f, 960f, map.worldWidth)
            drawHillLayer(Color(0xFFDDA15E), 1100f, 1280f, map.worldWidth)
            drawSandRipples()
        }
        EnvironmentType.FOREST_CROSSROADS -> {
            // Dense woodland deep green
            drawRect(color = Color(0xFF2D5A27), size = arenaSize)
            drawHillLayer(Color(0xFF244820), 160f, 340f, map.worldWidth)
            drawHillLayer(Color(0xFF35692E), 520f, 700f, map.worldWidth)
            drawHillLayer(Color(0xFF244820), 880f, 1060f, map.worldWidth)
            drawHillLayer(Color(0xFF35692E), 1200f, 1380f, map.worldWidth)
            drawForestFoliage()
        }
        EnvironmentType.OBSIDIAN_TUNNEL -> {
            // Dark volcanic subterranean rock
            drawRect(color = Color(0xFF1E293B), size = arenaSize)
            drawHillLayer(Color(0xFF0F172A), 180f, 360f, map.worldWidth)
            drawHillLayer(Color(0xFF334155), 580f, 760f, map.worldWidth)
            drawHillLayer(Color(0xFF0F172A), 980f, 1160f, map.worldWidth)
            drawCaveFloorGlow()
        }
        EnvironmentType.DRAGON_COIL -> {
            // Ancient weathered stone bedrock with central plateau
            drawRect(color = Color(0xFF3F3B37), size = arenaSize)
            drawHillLayer(Color(0xFF524A42), 200f, 380f, map.worldWidth)
            drawHillLayer(Color(0xFF6A5F54), 600f, 780f, map.worldWidth)
            drawHillLayer(Color(0xFF524A42), 1000f, 1180f, map.worldWidth)
            drawCentralPlateau()
        }
    }
}

private fun DrawScope.drawHillLayer(color: Color, startY: Float, endY: Float, worldWidth: Float) {
    val path = Path().apply {
        moveTo(0f, startY)
        val seg = worldWidth / 4f
        cubicTo(
            seg * 0.9f, startY - 45f,
            seg * 1.6f, startY + 55f,
            seg * 2.5f, startY - 30f
        )
        cubicTo(
            seg * 3.0f, startY - 70f,
            seg * 3.5f, startY + 20f,
            worldWidth, startY
        )
        lineTo(worldWidth, endY)
        cubicTo(
            seg * 2.7f, endY + 40f,
            seg * 1.3f, endY - 40f,
            0f, endY
        )
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawGrassTufts() {
    val tufts = listOf(
        Point2D(100f, 250f), Point2D(460f, 80f), Point2D(860f, 220f),
        Point2D(200f, 440f), Point2D(600f, 400f), Point2D(900f, 540f),
        Point2D(100f, 800f), Point2D(480f, 780f), Point2D(840f, 920f),
        Point2D(340f, 1080f), Point2D(700f, 1300f), Point2D(140f, 1150f)
    )
    for (t in tufts) {
        drawCircle(color = Color(0x334B8027), radius = 10f, center = Offset(t.x, t.y))
        drawLine(
            color = Color(0xFF4B8027),
            start = Offset(t.x, t.y),
            end = Offset(t.x - 5f, t.y - 12f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4B8027),
            start = Offset(t.x, t.y),
            end = Offset(t.x + 1f, t.y - 15f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4B8027),
            start = Offset(t.x, t.y),
            end = Offset(t.x + 7f, t.y - 11f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawWaterPond(map: GameMap, time: Float) {
    val center = map.waterPondCenter ?: return
    val r = map.waterPondRadius
    if (r <= 0f) return

    val pondOffset = Offset(center.x, center.y)

    // Sandy / earthen shore rim
    drawCircle(color = Color(0xFFB0824E), radius = r + 14f, center = pondOffset)
    drawCircle(color = Color(0xFFC79E68), radius = r + 8f, center = pondOffset)

    // Deep water body
    drawCircle(color = Color(0xFF286591), radius = r, center = pondOffset)
    drawCircle(color = Color(0xFF3880B5), radius = r * 0.85f, center = pondOffset)
    drawCircle(color = Color(0xFF4A98D3), radius = r * 0.6f, center = pondOffset)

    // Water ripple ring animated with game time
    val rippleProgress = (time * 0.5f) % 1f
    drawCircle(
        color = Color(0x55FFFFFF).copy(alpha = (1f - rippleProgress) * 0.4f),
        radius = r * (0.3f + rippleProgress * 0.6f),
        center = pondOffset,
        style = Stroke(width = 2.5f)
    )

    // Lily pads
    drawCircle(color = Color(0xFF1E5E3A), radius = 12f, center = Offset(center.x - 22f, center.y - 16f))
    drawCircle(color = Color(0xFF1E5E3A), radius = 10f, center = Offset(center.x + 24f, center.y + 12f))
    // Pink water flower
    drawCircle(color = Color(0xFFFF85A1), radius = 4f, center = Offset(center.x - 22f, center.y - 16f))
}

private fun DrawScope.drawSandRipples() {
    val ripples = listOf(
        Point2D(140f, 260f), Point2D(520f, 120f), Point2D(840f, 260f),
        Point2D(240f, 480f), Point2D(660f, 420f), Point2D(880f, 580f),
        Point2D(160f, 840f), Point2D(520f, 820f), Point2D(840f, 960f),
        Point2D(300f, 1120f), Point2D(720f, 1260f)
    )
    for (r in ripples) {
        drawLine(
            color = Color(0x33A76C20),
            start = Offset(r.x - 22f, r.y),
            end = Offset(r.x + 22f, r.y + 4f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawForestFoliage() {
    val leafPatches = listOf(
        Point2D(120f, 220f), Point2D(460f, 100f), Point2D(820f, 240f),
        Point2D(220f, 460f), Point2D(580f, 380f), Point2D(860f, 520f),
        Point2D(140f, 820f), Point2D(460f, 760f), Point2D(820f, 900f),
        Point2D(280f, 1140f), Point2D(680f, 1280f)
    )
    for (p in leafPatches) {
        drawCircle(color = Color(0x331B3A17), radius = 12f, center = Offset(p.x, p.y))
        drawCircle(color = Color(0xFF1E461A), radius = 4f, center = Offset(p.x - 3f, p.y - 2f))
        drawCircle(color = Color(0xFF38662A), radius = 3.5f, center = Offset(p.x + 4f, p.y + 3f))
    }
}

private fun DrawScope.drawCaveFloorGlow() {
    val crystals = listOf(
        Point2D(160f, 260f), Point2D(840f, 220f), Point2D(240f, 520f),
        Point2D(820f, 780f), Point2D(180f, 920f), Point2D(780f, 1180f)
    )
    for (c in crystals) {
        drawCircle(color = Color(0x33A855F7), radius = 14f, center = Offset(c.x, c.y))
        drawCircle(color = Color(0xFFC084FC), radius = 3.5f, center = Offset(c.x, c.y))
        drawCircle(color = Color(0xFFE9D5FF), radius = 1.5f, center = Offset(c.x, c.y))
    }
}

private fun DrawScope.drawCentralPlateau() {
    val cx = 520f
    val cy = 660f
    val r = 160f

    // Elevated plateau cliff shadow
    drawCircle(color = Color(0x60000000), radius = r + 18f, center = Offset(cx + 8f, cy + 14f))
    // Stone cliff wall
    drawCircle(color = Color(0xFF292524), radius = r + 8f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF44403C), radius = r + 4f, center = Offset(cx, cy))
    // Plateau upper grass / earth surface
    drawCircle(color = Color(0xFF57534E), radius = r, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF78716C), radius = r * 0.9f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF292524), radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))

    // Carved runic dragon emblem on high-ground
    drawCircle(
        color = Color(0xFFD6D3D1),
        radius = r * 0.6f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
    )
    drawCircle(color = Color(0xFFF59E0B), radius = 7f, center = Offset(cx, cy))
}

private fun DrawScope.drawTunnelPortals(tunnel: TunnelRegion) {
    // Entrance Portal
    val ent = tunnel.entrance
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(ent.x - 48f, ent.y - 30f),
        size = Size(96f, 52f),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = Color(0xFF020617),
        topLeft = Offset(ent.x - 38f, ent.y - 20f),
        size = Size(76f, 42f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    // Timber beams
    drawRect(color = Color(0xFF78350F), topLeft = Offset(ent.x - 44f, ent.y - 26f), size = Size(8f, 48f))
    drawRect(color = Color(0xFF78350F), topLeft = Offset(ent.x + 36f, ent.y - 26f), size = Size(8f, 48f))
    drawRect(color = Color(0xFF92400E), topLeft = Offset(ent.x - 44f, ent.y - 26f), size = Size(88f, 8f))

    // Exit Portal
    val ex = tunnel.exit
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(ex.x - 48f, ex.y - 22f),
        size = Size(96f, 52f),
        cornerRadius = CornerRadius(16f, 16f)
    )
    drawRoundRect(
        color = Color(0xFF020617),
        topLeft = Offset(ex.x - 38f, ex.y - 12f),
        size = Size(76f, 42f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    // Exit Timber beams
    drawRect(color = Color(0xFF78350F), topLeft = Offset(ex.x - 44f, ex.y - 18f), size = Size(8f, 48f))
    drawRect(color = Color(0xFF78350F), topLeft = Offset(ex.x + 36f, ex.y - 18f), size = Size(8f, 48f))
    drawRect(color = Color(0xFF92400E), topLeft = Offset(ex.x - 44f, ex.y - 18f), size = Size(88f, 8f))
}

private fun DrawScope.drawTunnelCavernCanopy(tunnel: TunnelRegion, time: Float) {
    val width = tunnel.boundsRight - tunnel.boundsLeft
    val height = tunnel.boundsBottom - tunnel.boundsTop
    val left = tunnel.boundsLeft
    val top = tunnel.boundsTop

    // Heavy mountain drop shadow cast to bottom right
    drawRoundRect(
        color = Color(0x77000000),
        topLeft = Offset(left + 12f, top + 14f),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f)
    )

    // Solid rocky mountain mass covering the tunnel
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f)
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(left + 6f, top + 6f),
        size = Size(width - 12f, height - 12f),
        cornerRadius = CornerRadius(20f, 20f)
    )

    // Craggy mountain ridges and stone strata
    for (step in 1..4) {
        val yOffset = top + (height / 5f) * step
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(left + 14f, yOffset),
            end = Offset(left + width - 14f, yOffset + 12f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF475569),
            start = Offset(left + 14f, yOffset - 2f),
            end = Offset(left + width - 14f, yOffset + 10f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }

    // Glowing purple mountain crystal clusters on the rock roof
    val pulse = (sin(time * 3f) * 0.15f + 0.85f)
    drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.6f * pulse), radius = 18f * pulse, center = Offset(left + 45f, top + 55f))
    drawCircle(color = Color(0xFFC084FC), radius = 8f, center = Offset(left + 45f, top + 55f))

    drawCircle(color = Color(0xFFA855F7).copy(alpha = 0.6f * pulse), radius = 18f * pulse, center = Offset(left + width - 45f, top + height - 55f))
    drawCircle(color = Color(0xFFC084FC), radius = 8f, center = Offset(left + width - 45f, top + height - 55f))

    // Carved runic warning emblem on center of mountain
    drawCircle(color = Color(0xFF0F172A), radius = 24f, center = Offset(left + width / 2f, top + height / 2f))
    drawCircle(color = Color(0xFFE2E8F0), radius = 20f, center = Offset(left + width / 2f, top + height / 2f), style = Stroke(width = 2f))
}

private fun DrawScope.drawDirtRoad(composePath: Path, path: GamePath, env: EnvironmentType) {
    val (shadowColor, shoulderColor, roadColor, wagonColor) = when (env) {
        EnvironmentType.GREEN_VALLEY -> listOf(Color(0xFF4D381E), Color(0xFF966838), Color(0xFFC59556), Color(0x447A5328))
        EnvironmentType.DESERT_CANYON -> listOf(Color(0xFF5C3317), Color(0xFFA0522D), Color(0xFFD28242), Color(0x447A3818))
        EnvironmentType.FOREST_CROSSROADS -> listOf(Color(0xFF2B1D0C), Color(0xFF5C4022), Color(0xFF8B6538), Color(0x443D2810))
        EnvironmentType.OBSIDIAN_TUNNEL -> listOf(Color(0xFF020617), Color(0xFF1E293B), Color(0xFF334155), Color(0x4464748B))
        EnvironmentType.DRAGON_COIL -> listOf(Color(0xFF1C1917), Color(0xFF44403C), Color(0xFF78716C), Color(0x44A8A29E))
    }

    // 1. Road edge shadow
    drawPath(
        path = composePath,
        color = shadowColor,
        style = Stroke(
            width = path.pathWidth + 16f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 2. Earth shoulder
    drawPath(
        path = composePath,
        color = shoulderColor,
        style = Stroke(
            width = path.pathWidth + 6f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 3. Main sunlit dirt surface
    drawPath(
        path = composePath,
        color = roadColor,
        style = Stroke(
            width = path.pathWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 4. Center worn wagon tracks
    drawPath(
        path = composePath,
        color = wagonColor,
        style = Stroke(
            width = 6f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f))
        )
    )
}

private fun DrawScope.drawDecorations(decorations: List<MapDecoration>) {
    for (d in decorations) {
        val center = Offset(d.position.x, d.position.y)

        // Ground drop shadow
        drawCircle(
            color = Color(0x40000000),
            radius = d.size * 0.45f,
            center = Offset(center.x + 4f, center.y + d.size * 0.3f)
        )

        when (d.type) {
            DecorationType.OAK_TREE -> {
                // Brown trunk
                drawRect(
                    color = Color(0xFF6B4226),
                    topLeft = Offset(center.x - 7f, center.y - 6f),
                    size = Size(14f, d.size * 0.5f)
                )
                // Layered round green foliage
                drawCircle(color = Color(0xFF2D5A27), radius = d.size * 0.45f, center = center)
                drawCircle(color = Color(0xFF3F7D37), radius = d.size * 0.38f, center = Offset(center.x - 4f, center.y - 6f))
                drawCircle(color = Color(0xFF5A9E4B), radius = d.size * 0.26f, center = Offset(center.x - 8f, center.y - 12f))
            }
            DecorationType.PINE_TREE -> {
                // Trunk
                drawRect(
                    color = Color(0xFF5A3825),
                    topLeft = Offset(center.x - 5f, center.y + 10f),
                    size = Size(10f, 18f)
                )
                // Triangular pine tiers
                drawPineTier(center.x, center.y + 12f, d.size * 0.42f, Color(0xFF1E4620))
                drawPineTier(center.x, center.y - 4f, d.size * 0.34f, Color(0xFF2B5B2E))
                drawPineTier(center.x, center.y - 18f, d.size * 0.25f, Color(0xFF3B7A3E))
            }
            DecorationType.BOULDER -> {
                // Rock body
                drawRoundRect(
                    color = Color(0xFF5A6268),
                    topLeft = Offset(center.x - d.size * 0.5f, center.y - d.size * 0.35f),
                    size = Size(d.size, d.size * 0.7f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Rock highlight
                drawRoundRect(
                    color = Color(0xFF7E878E),
                    topLeft = Offset(center.x - d.size * 0.4f, center.y - d.size * 0.28f),
                    size = Size(d.size * 0.5f, d.size * 0.35f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            DecorationType.BUSH -> {
                drawCircle(color = Color(0xFF2E632A), radius = d.size * 0.5f, center = center)
                drawCircle(color = Color(0xFF438A3D), radius = d.size * 0.4f, center = Offset(center.x - 2f, center.y - 3f))
                // Red berries
                drawCircle(color = Color(0xFFE63946), radius = 2.5f, center = Offset(center.x - 6f, center.y - 4f))
                drawCircle(color = Color(0xFFE63946), radius = 2.5f, center = Offset(center.x + 5f, center.y + 2f))
            }
            DecorationType.FLOWER_PATCH -> {
                drawCircle(color = Color(0x66438A3D), radius = d.size * 0.5f, center = center)
                val flowerColors = listOf(Color(0xFFFFB703), Color(0xFFE63946), Color(0xFFFFFFFF), Color(0xFF9D4EDD))
                for (i in 0..4) {
                    val angle = i * 1.25f
                    val fx = center.x + cos(angle) * (d.size * 0.3f)
                    val fy = center.y + sin(angle) * (d.size * 0.3f)
                    drawCircle(color = flowerColors[i % flowerColors.size], radius = 3.5f, center = Offset(fx, fy))
                    drawCircle(color = Color(0xFFFFD166), radius = 1.2f, center = Offset(fx, fy))
                }
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawPineTier(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - radius * 1.3f)
        lineTo(cx + radius, cy)
        lineTo(cx - radius, cy)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawSpawnGate(start: Point2D, worldWidth: Float, worldHeight: Float) {
    val gateX = if (start.x < 0f) 24f else if (start.x > worldWidth) worldWidth - 24f else start.x
    val gateY = if (start.y < 0f) 24f else if (start.y > worldHeight) worldHeight - 24f else start.y
    val center = Offset(gateX, gateY)
    // Dark portal entry
    drawCircle(color = Color(0xFF1E293B), radius = 32f, center = center)
    drawCircle(color = Color(0xFF475569), radius = 32f, center = center, style = Stroke(width = 6f))
    drawCircle(color = Color(0xFF0F172A), radius = 22f, center = center)

    // Red warning torch glow
    drawCircle(color = Color(0xFFFF5722), radius = 5f, center = Offset(center.x - 14f, center.y - 18f))
    drawCircle(color = Color(0xFFFF5722), radius = 5f, center = Offset(center.x + 14f, center.y - 18f))
}

private fun DrawScope.drawCastleBase(base: Base) {
    val left = base.position.x - base.width / 2f
    val top = base.position.y - base.height / 2f

    // Section 16: Temporary Base Position Debug Marker
    drawCircle(
        color = Color(0x77EF4444),
        radius = base.width * 0.72f,
        center = Offset(base.position.x, base.position.y),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 3f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    )
    drawCircle(
        color = Color(0xFFEF4444),
        radius = 6f,
        center = Offset(base.position.x, base.position.y)
    )

    // Castle shadow
    drawRoundRect(
        color = Color(0x55000000),
        topLeft = Offset(left + 8f, top + 10f),
        size = Size(base.width, base.height),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // Fortress stone walls
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(left, top),
        size = Size(base.width, base.height),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(left + 4f, top + 4f),
        size = Size(base.width - 8f, base.height - 8f),
        cornerRadius = CornerRadius(10f, 10f)
    )

    // Crenellations (Castle battlements on top)
    val battlementCount = 5
    val bWidth = base.width / (battlementCount * 2 - 1)
    for (i in 0 until battlementCount) {
        val bx = left + (i * 2) * bWidth
        drawRect(
            color = Color(0xFF475569),
            topLeft = Offset(bx, top - 10f),
            size = Size(bWidth, 12f)
        )
    }

    // Wooden Portcullis Gate
    drawRoundRect(
        color = Color(0xFF5C3D2E),
        topLeft = Offset(base.position.x - 18f, top + base.height - 38f),
        size = Size(36f, 36f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Iron bars
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(base.position.x - 8f, top + base.height - 38f),
        end = Offset(base.position.x - 8f, top + base.height - 2f),
        strokeWidth = 2.5f
    )
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(base.position.x + 8f, top + base.height - 38f),
        end = Offset(base.position.x + 8f, top + base.height - 2f),
        strokeWidth = 2.5f
    )

    // Blue Defender Flag
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(base.position.x, top - 10f),
        end = Offset(base.position.x, top - 32f),
        strokeWidth = 3f
    )
    val flagPath = Path().apply {
        moveTo(base.position.x, top - 32f)
        lineTo(base.position.x + 22f, top - 24f)
        lineTo(base.position.x, top - 16f)
        close()
    }
    drawPath(flagPath, Color(0xFF3B82F6))

    // Base Health Bar
    val barWidth = base.width
    val barHeight = 9f
    val barTop = top - 44f

    drawRoundRect(
        color = Color(0xCC000000),
        topLeft = Offset(left, barTop),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )
    val hpColor = when {
        base.healthPercentage > 0.5f -> Color(0xFF22C55E)
        base.healthPercentage > 0.25f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    drawRoundRect(
        color = hpColor,
        topLeft = Offset(left, barTop),
        size = Size(barWidth * base.healthPercentage, barHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

private fun DrawScope.drawTowers(towers: List<Tower>, selectedTower: Tower?, gameTime: Float) {
    for (tower in towers) {
        val center = Offset(tower.position.x, tower.position.y)
        val isSelected = selectedTower?.id == tower.id

        // Selection Attack Range Indicator
        if (isSelected) {
            drawCircle(
                color = Color(0x2238BDF8),
                radius = tower.spec.range,
                center = center
            )
            drawCircle(
                color = Color(0xCC38BDF8),
                radius = tower.spec.range,
                center = center,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            )
        }

        when (tower.spec.type) {
            TowerType.MACHINE_GUN -> drawMachineGunTower(center, tower)
            TowerType.CANNON -> drawCannonTower(center, tower)
            TowerType.RAPID_FIRE -> drawRapidFireTower(center, tower, gameTime)
        }

        // Level Stars Indicator
        for (lvl in 1..tower.spec.level) {
            val starX = center.x - ((tower.spec.level - 1) * 9f) + ((lvl - 1) * 18f)
            val starCenter = Offset(starX, center.y + tower.spec.size * 0.42f)
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 4.5f,
                center = starCenter
            )
            drawCircle(
                color = Color(0xFFFFD166),
                radius = 3.5f,
                center = starCenter
            )
            drawCircle(
                color = Color.White,
                radius = 1.5f,
                center = Offset(starCenter.x - 1f, starCenter.y - 1f)
            )
        }
    }
}

private fun DrawScope.drawMachineGunTower(center: Offset, tower: Tower) {
    val baseR = tower.spec.size * 0.48f

    // 1. Soft ground drop shadow underneath
    drawOval(
        color = Color(0x45000000),
        topLeft = Offset(center.x - baseR * 1.05f + 4f, center.y - baseR * 0.75f + 6f),
        size = Size(baseR * 2.1f, baseR * 1.6f)
    )

    // 2. Heavy fortified circular steel pedestal base with double bevel and dark border
    drawCircle(color = Color(0xFF1E293B), radius = baseR, center = center)
    drawCircle(color = Color(0xFF334155), radius = baseR - 2.5f, center = center)
    drawCircle(color = Color(0xFF475569), radius = baseR * 0.85f, center = center)
    drawCircle(color = Color(0xFF64748B), radius = baseR * 0.82f, center = Offset(center.x - 1.5f, center.y - 1.5f), style = Stroke(width = 1.5f))

    // 3. Hexagonal steel perimeter bolts
    for (i in 0..5) {
        val ang = (i * Math.PI / 3.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.88f)
        val by = center.y + sin(ang) * (baseR * 0.88f)
        drawCircle(color = Color(0xFF0F172A), radius = 3f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFCBD5E1), radius = 1.8f, center = Offset(bx - 0.6f, by - 0.6f))
    }

    // 4. Rotating Turret Mechanism & Weapon Assembly
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Geared turntable ring with traverse teeth
        drawCircle(color = Color(0xFF0F172A), radius = 17f, center = center)
        drawCircle(color = Color(0xFF475569), radius = 15f, center = center)
        for (g in 0..7) {
            val gang = (g * Math.PI / 4.0).toFloat()
            val gx = center.x + cos(gang) * 14.5f
            val gy = center.y + sin(gang) * 14.5f
            drawCircle(color = Color(0xFF1E293B), radius = 1.5f, center = Offset(gx, gy))
        }

        val recoil = tower.recoilFraction * 7f

        when (tower.spec.level) {
            1 -> {
                // Level 1: Sturdy twin machine gun barrels with side olive ammo box
                // Side ammo box (military olive-drab steel canister)
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x - 8f, center.y - 17f),
                    size = Size(13f, 8f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = Color(0xFF166534), // Olive green
                    topLeft = Offset(center.x - 7f, center.y - 16f),
                    size = Size(11f, 6f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                // Ammo box latch & lid crease
                drawRect(color = Color(0xFF14532D), topLeft = Offset(center.x - 7f, center.y - 13.5f), size = Size(11f, 1.5f))
                // Linked brass ammo belt entering breech
                for (b in 0..2) {
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 6f + b * 3.5f, center.y - 10f), size = Size(2.2f, 4f))
                }

                // Twin gunmetal barrels
                val barrelLength = 26f
                // Top barrel
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y - 7f),
                    size = Size(barrelLength, 4.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x + 3f - recoil, center.y - 6f), size = Size(barrelLength - 4f, 1.5f))
                // Bottom barrel
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y + 2.5f),
                    size = Size(barrelLength, 4.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x + 3f - recoil, center.y + 3.5f), size = Size(barrelLength - 4f, 1.5f))

                // Muzzle flash hider / compensator tips
                drawRect(color = Color(0xFF94A3B8), topLeft = Offset(center.x + barrelLength - 1f - recoil, center.y - 7.5f), size = Size(3.5f, 5.5f))
                drawRect(color = Color(0xFF94A3B8), topLeft = Offset(center.x + barrelLength - 1f - recoil, center.y + 2f), size = Size(3.5f, 5.5f))

                // Armored receiver housing block
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 12f, center.y - 9.5f),
                    size = Size(17f, 19f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(center.x - 10f, center.y - 7.5f),
                    size = Size(13f, 15f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            2 -> {
                // Level 2: Extended heavy twin barrels with perforated cooling jackets + front mantlet shield
                // Dual side ammo boxes (olive green)
                drawRoundRect(color = Color(0xFF166534), topLeft = Offset(center.x - 7f, center.y - 18f), size = Size(12f, 7f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF166534), topLeft = Offset(center.x - 7f, center.y + 11f), size = Size(12f, 7f), cornerRadius = CornerRadius(2f, 2f))
                // Linked brass ammo feeds
                for (b in 0..2) {
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 5f + b * 3f, center.y - 11f), size = Size(2f, 3.5f))
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 5f + b * 3f, center.y + 7.5f), size = Size(2f, 3.5f))
                }

                // Front curved gun shield / mantlet armor
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x - 4f, center.y - 14f),
                    size = Size(9f, 28f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(center.x - 3f, center.y - 12.5f),
                    size = Size(6f, 25f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Heavy extended twin barrels (length 33px)
                val barrelLength = 33f
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y - 8f), size = Size(barrelLength, 5.5f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y + 2.5f), size = Size(barrelLength, 5.5f), cornerRadius = CornerRadius(2f, 2f))
                // Highlights
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 5f - recoil, center.y - 7f), size = Size(barrelLength - 7f, 1.8f))
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 5f - recoil, center.y + 3.5f), size = Size(barrelLength - 7f, 1.8f))

                // Perforated barrel cooling sleeve vents
                for (h in 0..2) {
                    drawCircle(color = Color(0xFF1E293B), radius = 1.4f, center = Offset(center.x + 12f + h * 6f - recoil, center.y - 5.2f))
                    drawCircle(color = Color(0xFF1E293B), radius = 1.4f, center = Offset(center.x + 12f + h * 6f - recoil, center.y + 5.2f))
                }

                // Slotted muzzle brakes
                drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + barrelLength - 1f - recoil, center.y - 9f), size = Size(5f, 7.5f))
                drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + barrelLength - 1f - recoil, center.y + 1.5f), size = Size(5f, 7.5f))

                // Receiver body
                drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 13f, center.y - 11f), size = Size(18f, 22f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 11f, center.y - 9f), size = Size(14f, 18f), cornerRadius = CornerRadius(3f, 3f))
            }
            else -> {
                // Level 3: Quad-barrel Gatling Vulcan Rotary Turret
                // Dual high-capacity ammo drums
                drawCircle(color = Color(0xFF0F172A), radius = 7f, center = Offset(center.x - 4f, center.y - 16f))
                drawCircle(color = Color(0xFF166534), radius = 5.5f, center = Offset(center.x - 4f, center.y - 16f))
                drawCircle(color = Color(0xFF0F172A), radius = 7f, center = Offset(center.x - 4f, center.y + 16f))
                drawCircle(color = Color(0xFF166534), radius = 5.5f, center = Offset(center.x - 4f, center.y + 16f))

                // Flexible linked brass ammo chute entering the receiver
                for (b in 0..3) {
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 9f + b * 3f, center.y - 12f + b * 1f), size = Size(2.2f, 4f))
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 9f + b * 3f, center.y + 8f - b * 1f), size = Size(2.2f, 4f))
                }

                // Rotary barrel cluster (4 heavy steel barrels)
                val barrelLength = 37f
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y - 8f), size = Size(barrelLength, 3.5f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y - 3f), size = Size(barrelLength + 1f, 3.8f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y + 2f), size = Size(barrelLength + 1f, 3.8f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 3f - recoil, center.y + 6.5f), size = Size(barrelLength, 3.5f))

                // Steel barrel highlights
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 5f - recoil, center.y - 2f), size = Size(barrelLength - 8f, 1.5f))
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 5f - recoil, center.y + 3f), size = Size(barrelLength - 8f, 1.5f))

                // Golden/brass barrel retaining brackets/clamps
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x + 20f - recoil, center.y - 9.5f), size = Size(4.5f, 19f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x + 34f - recoil, center.y - 9.5f), size = Size(4f, 19f), cornerRadius = CornerRadius(1.5f, 1.5f))

                // Heavy angular armored receiver
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 14f, center.y - 12f), size = Size(20f, 24f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 12f, center.y - 10f), size = Size(16f, 20f), cornerRadius = CornerRadius(3f, 3f))
            }
        }

        // Turret gunner cupola / center optics dome with glint
        drawCircle(color = Color(0xFF0F172A), radius = 12f, center = center)
        drawCircle(color = Color(0xFF334155), radius = 10f, center = center)
        drawCircle(color = Color(0xFF475569), radius = 7f, center = Offset(center.x - 1.5f, center.y - 1.5f))
        drawCircle(color = Color(0xFFFFFFFF), radius = 2.5f, center = Offset(center.x - 3f, center.y - 3f))

        // Muzzle Flash on Firing: Sharp cartoon 8-point golden starburst + ejecting shell casing
        if (tower.isFiring) {
            val tipX = center.x + (if (tower.spec.level == 1) 28f else if (tower.spec.level == 2) 35f else 39f) - recoil
            // Ejecting brass shell casing to the side
            val casingX = center.x + 2f
            val casingY = center.y - 12f - (tower.recoilFraction * 6f)
            drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(casingX, casingY), size = Size(4f, 2.5f), cornerRadius = CornerRadius(0.8f, 0.8f))

            // Starburst flash at muzzle tip
            drawCircle(color = Color(0xFFFFD166), radius = 11f, center = Offset(tipX + 3f, center.y))
            drawCircle(color = Color(0xFFFFFFFF), radius = 5.5f, center = Offset(tipX + 3f, center.y))
            // 4 Spikes
            drawLine(Color(0xFFFEF08A), Offset(tipX - 4f, center.y), Offset(tipX + 16f, center.y), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFEF08A), Offset(tipX + 3f, center.y - 10f), Offset(tipX + 3f, center.y + 10f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Diagonal spikes
            drawLine(Color(0xFFF59E0B), Offset(tipX - 2f, center.y - 6f), Offset(tipX + 8f, center.y + 6f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF59E0B), Offset(tipX - 2f, center.y + 6f), Offset(tipX + 8f, center.y - 6f), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawCannonTower(center: Offset, tower: Tower) {
    val baseR = tower.spec.size * 0.48f

    // 1. Soft ground drop shadow underneath
    drawOval(
        color = Color(0x55000000),
        topLeft = Offset(center.x - baseR * 1.1f + 5f, center.y - baseR * 0.8f + 8f),
        size = Size(baseR * 2.2f, baseR * 1.7f)
    )

    // 2. Heavy fortified octagonal stone and riveted iron foundation
    drawCircle(color = Color(0xFF0F172A), radius = baseR, center = center)
    drawCircle(color = Color(0xFF1E293B), radius = baseR - 2.5f, center = center)
    drawCircle(color = Color(0xFF334155), radius = baseR * 0.86f, center = center)

    // 4 Corner heavy iron bracket plates with central bolts
    for (i in 0..3) {
        val ang = (i * Math.PI / 2.0 + Math.PI / 4.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.80f)
        val by = center.y + sin(ang) * (baseR * 0.80f)
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(bx - 4.5f, by - 4.5f),
            size = Size(9f, 9f),
            cornerRadius = CornerRadius(2.5f, 2.5f)
        )
        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(bx - 3.5f, by - 3.5f),
            size = Size(7f, 7f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawCircle(color = Color(0xFF0F172A), radius = 1.8f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFE2E8F0), radius = 1f, center = Offset(bx - 0.5f, by - 0.5f))
    }

    // 3. Rotating Turret Mechanism & Cannon Assembly
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Heavy mechanical swivel turntable
        drawCircle(color = Color(0xFF0F172A), radius = 19f, center = center)
        drawCircle(color = Color(0xFF475569), radius = 16.5f, center = center)
        drawCircle(color = Color(0xFF1E293B), radius = 16.5f, center = center, style = Stroke(width = 2f))

        // Heavy trunnion side brackets (where the cannon pivots)
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 11f, center.y - 17f), size = Size(14f, 7f), cornerRadius = CornerRadius(2.5f, 2.5f))
        drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 10f, center.y - 16f), size = Size(12f, 5f), cornerRadius = CornerRadius(2f, 2f))
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.5f, center = Offset(center.x - 4f, center.y - 13.5f))

        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 11f, center.y + 10f), size = Size(14f, 7f), cornerRadius = CornerRadius(2.5f, 2.5f))
        drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 10f, center.y + 11f), size = Size(12f, 5f), cornerRadius = CornerRadius(2f, 2f))
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.5f, center = Offset(center.x - 4f, center.y + 13.5f))

        // Recoil displacement (substantial backward pushback!)
        val recoil = tower.recoilFraction * 12f

        when (tower.spec.level) {
            1 -> {
                // Level 1: Heavy Siege Mortar Barrel
                // Recoil cylinder atop breech
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 8f, center.y - 3f), size = Size(18f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRect(color = Color(0xFF94A3B8), topLeft = Offset(center.x - 6f - recoil * 0.5f, center.y - 1.5f), size = Size(12f, 3f))

                // Heavy tapered barrel body (thick cast iron)
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x - 2f - recoil, center.y - 9.5f),
                    size = Size(33f, 19f),
                    cornerRadius = CornerRadius(3.5f, 3.5f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 1f - recoil, center.y - 8f),
                    size = Size(30f, 16f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                // Top cylindrical specular cartoon highlight
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x - 1f - recoil, center.y - 6f), size = Size(27f, 3f))
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x - 1f - recoil, center.y - 5f), size = Size(24f, 1.2f))

                // Brass breech reinforcement band
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x + 8f - recoil, center.y - 10.5f), size = Size(4.5f, 21f), cornerRadius = CornerRadius(2f, 2f))

                // Flared cast-iron muzzle collar & dark bore opening
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 28f - recoil, center.y - 11.5f), size = Size(7.5f, 23f), cornerRadius = CornerRadius(3.5f, 3.5f))
                drawRoundRect(color = Color(0xFF475569), topLeft = Offset(center.x + 29f - recoil, center.y - 10f), size = Size(4.5f, 20f), cornerRadius = CornerRadius(2.5f, 2.5f))
                drawOval(color = Color(0xFF0F172A), topLeft = Offset(center.x + 33f - recoil, center.y - 8f), size = Size(2.5f, 16f))
            }
            2 -> {
                // Level 2: Extended Siege Howitzer with dual hydraulic recoil damping cylinders
                // Upper & lower hydraulic damping cylinders with sliding piston rods
                val pistonSlide = recoil * 0.7f
                // Top cylinder
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 4f, center.y - 14f), size = Size(18f, 5f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF475569), topLeft = Offset(center.x - 3f, center.y - 13f), size = Size(16f, 3f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(center.x + 8f - pistonSlide, center.y - 12.5f), size = Size(10f, 2f))

                // Bottom cylinder
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 4f, center.y + 9f), size = Size(18f, 5f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF475569), topLeft = Offset(center.x - 3f, center.y + 10f), size = Size(16f, 3f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(center.x + 8f - pistonSlide, center.y + 10.5f), size = Size(10f, 2f))

                // Extended heavy barrel (length 41px)
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 4f - recoil, center.y - 10.5f), size = Size(41f, 21f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 3f - recoil, center.y - 9f), size = Size(38f, 18f), cornerRadius = CornerRadius(3f, 3f))
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x - 3f - recoil, center.y - 7f), size = Size(35f, 3.5f))
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x - 3f - recoil, center.y - 6f), size = Size(31f, 1.5f))

                // Steel reinforcement collars
                drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 14f - recoil, center.y - 11.5f), size = Size(4.5f, 23f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 33f - recoil, center.y - 12.5f), size = Size(8.5f, 25f), cornerRadius = CornerRadius(3.5f, 3.5f))
                drawRoundRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + 34f - recoil, center.y - 11f), size = Size(5.5f, 22f), cornerRadius = CornerRadius(2.5f, 2.5f))
            }
            else -> {
                // Level 3: Devastator Siege Bombard with heavy angled side blast shields & colossal rifled bore
                // Angled heavy steel side blast shields
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 10f, center.y - 19f), size = Size(13f, 38f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFF334155), topLeft = Offset(center.x - 9f, center.y - 17.5f), size = Size(10f, 35f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 9f, center.y - 17.5f), size = Size(10f, 35f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 1.5f))

                // Massive bombard barrel (length 46px)
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 6f - recoil, center.y - 12.5f), size = Size(46f, 25f), cornerRadius = CornerRadius(4.5f, 4.5f))
                drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 5f - recoil, center.y - 11f), size = Size(43f, 22f), cornerRadius = CornerRadius(3.5f, 3.5f))
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x - 4f - recoil, center.y - 8f), size = Size(40f, 4f))
                drawRect(color = Color(0xFF94A3B8), topLeft = Offset(center.x - 4f - recoil, center.y - 7f), size = Size(35f, 1.8f))

                // Triple heavy reinforcement ribs
                for (r in 0..2) {
                    drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x + 8f + r * 10f - recoil, center.y - 13.5f), size = Size(4f, 27f), cornerRadius = CornerRadius(1.8f, 1.8f))
                }

                // Colossal spiked muzzle ring
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 37f - recoil, center.y - 15f), size = Size(10f, 30f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 38f - recoil, center.y - 14f), size = Size(7f, 28f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 2f))
            }
        }

        // Breech block counterweight dome
        drawCircle(color = Color(0xFF0F172A), radius = 11f, center = Offset(center.x - 5f, center.y))
        drawCircle(color = Color(0xFF334155), radius = 9f, center = Offset(center.x - 5f, center.y))
        drawCircle(color = Color(0xFF94A3B8), radius = 3f, center = Offset(center.x - 7f, center.y - 2f))

        // Massive Artillery Firing Animation: Expanding Fireball Blast & Billowing Smoke
        if (tower.isFiring) {
            val tipX = center.x + (if (tower.spec.level == 1) 34f else if (tower.spec.level == 2) 42f else 47f) - recoil
            // Expanding grey smoke puffs
            drawCircle(color = Color(0x9964748B), radius = 18f, center = Offset(tipX + 10f, center.y - 4f))
            drawCircle(color = Color(0x9994A3B8), radius = 15f, center = Offset(tipX + 12f, center.y + 5f))

            // Fire core layers
            drawCircle(color = Color(0xFFEA580C), radius = 14f, center = Offset(tipX + 6f, center.y))
            drawCircle(color = Color(0xFFFACC15), radius = 9f, center = Offset(tipX + 5f, center.y))
            drawCircle(color = Color(0xFFFFFFFF), radius = 4.5f, center = Offset(tipX + 4f, center.y))
            // Lateral blast flames
            drawLine(Color(0xFFF59E0B), Offset(tipX + 2f, center.y - 13f), Offset(tipX + 12f, center.y - 5f), strokeWidth = 3.5f, cap = StrokeCap.Round)
            drawLine(Color(0xFFF59E0B), Offset(tipX + 2f, center.y + 13f), Offset(tipX + 12f, center.y + 5f), strokeWidth = 3.5f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawRapidFireTower(center: Offset, tower: Tower, time: Float) {
    val baseR = tower.spec.size * 0.48f

    // 1. Ground drop shadow
    drawOval(
        color = Color(0x45000000),
        topLeft = Offset(center.x - baseR * 1.05f + 4f, center.y - baseR * 0.75f + 6f),
        size = Size(baseR * 2.1f, baseR * 1.6f)
    )

    // 2. High-precision mechanical gunmetal base with amber hazard accents
    drawCircle(color = Color(0xFF18181B), radius = baseR, center = center)
    drawCircle(color = Color(0xFF27272A), radius = baseR - 2.5f, center = center)
    drawCircle(color = Color(0xFF3F3F46), radius = baseR * 0.85f, center = center)

    // Precision bearing ring
    drawCircle(
        color = Color(0xFF71717A),
        radius = baseR * 0.82f,
        center = center,
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
    )

    // 4 Corner hazard warning accents (amber/black striped corners)
    for (i in 0..3) {
        val ang = (i * Math.PI / 2.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.82f)
        val by = center.y + sin(ang) * (baseR * 0.82f)
        drawCircle(color = Color(0xFF09090B), radius = 3.5f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFF59E0B), radius = 2.2f, center = Offset(bx - 0.5f, by - 0.5f))
    }

    // 3. Rotating Mechanical Turret Housing & Autocannon Assembly
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // High-speed traverse collar
        drawCircle(color = Color(0xFF09090B), radius = 17f, center = center)
        drawCircle(color = Color(0xFF27272A), radius = 15f, center = center)
        drawCircle(color = Color(0xFFF59E0B), radius = 14.5f, center = center, style = Stroke(width = 1.5f))

        // Snappy rapid vibration shudder
        val recoil = (tower.recoilFraction * 4.5f)

        when (tower.spec.level) {
            1 -> {
                // Level 1: Quad-barrel light autocannon cluster (2x2 squared steel barrels)
                // Left & right ammo chutes
                drawRoundRect(color = Color(0xFF18181B), topLeft = Offset(center.x - 6f, center.y - 16f), size = Size(11f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF18181B), topLeft = Offset(center.x - 6f, center.y + 10f), size = Size(11f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 5f, center.y - 15f), size = Size(9f, 1.5f))
                drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 5f, center.y + 13.5f), size = Size(9f, 1.5f))

                // 4 parallel autocannon barrels (2 upper, 2 lower)
                val barrelLength = 26f
                drawRect(color = Color(0xFF09090B), topLeft = Offset(center.x + 2f - recoil, center.y - 7.5f), size = Size(barrelLength, 3f))
                drawRect(color = Color(0xFF27272A), topLeft = Offset(center.x + 2f - recoil, center.y - 3.5f), size = Size(barrelLength, 3f))
                drawRect(color = Color(0xFF27272A), topLeft = Offset(center.x + 2f - recoil, center.y + 0.5f), size = Size(barrelLength, 3f))
                drawRect(color = Color(0xFF09090B), topLeft = Offset(center.x + 2f - recoil, center.y + 4.5f), size = Size(barrelLength, 3f))

                // Square muzzle clamp & flash hider
                drawRoundRect(color = Color(0xFF71717A), topLeft = Offset(center.x + barrelLength - 2f - recoil, center.y - 8.5f), size = Size(4.5f, 17f), cornerRadius = CornerRadius(1.5f, 1.5f))

                // Low-profile angular housing
                drawRoundRect(color = Color(0xFF18181B), topLeft = Offset(center.x - 11f, center.y - 9.5f), size = Size(16f, 19f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(color = Color(0xFF27272A), topLeft = Offset(center.x - 9f, center.y - 7.5f), size = Size(12f, 15f), cornerRadius = CornerRadius(2f, 2f))
            }
            2 -> {
                // Level 2: 6-barrel micro-rotary gatling cluster with central spindle drive
                // Side drum magazines (amber trimmed gunmetal drums)
                drawCircle(color = Color(0xFF09090B), radius = 6.5f, center = Offset(center.x - 3f, center.y - 15.5f))
                drawCircle(color = Color(0xFF3F3F46), radius = 5f, center = Offset(center.x - 3f, center.y - 15.5f))
                drawCircle(color = Color(0xFFF59E0B), radius = 2.5f, center = Offset(center.x - 3f, center.y - 15.5f))

                drawCircle(color = Color(0xFF09090B), radius = 6.5f, center = Offset(center.x - 3f, center.y + 15.5f))
                drawCircle(color = Color(0xFF3F3F46), radius = 5f, center = Offset(center.x - 3f, center.y + 15.5f))
                drawCircle(color = Color(0xFFF59E0B), radius = 2.5f, center = Offset(center.x - 3f, center.y + 15.5f))

                // 6-barrel micro rotary cluster
                val barrelLength = 32f
                drawRect(color = Color(0xFF09090B), topLeft = Offset(center.x + 3f - recoil, center.y - 8f), size = Size(barrelLength, 2.5f))
                drawRect(color = Color(0xFF27272A), topLeft = Offset(center.x + 3f - recoil, center.y - 5f), size = Size(barrelLength, 2.5f))
                drawRect(color = Color(0xFF3F3F46), topLeft = Offset(center.x + 3f - recoil, center.y - 2f), size = Size(barrelLength, 2.5f))
                drawRect(color = Color(0xFF3F3F46), topLeft = Offset(center.x + 3f - recoil, center.y + 1f), size = Size(barrelLength, 2.5f))
                drawRect(color = Color(0xFF27272A), topLeft = Offset(center.x + 3f - recoil, center.y + 4f), size = Size(barrelLength, 2.5f))
                drawRect(color = Color(0xFF09090B), topLeft = Offset(center.x + 3f - recoil, center.y + 7f), size = Size(barrelLength, 2.5f))

                // Front spindle disk & retaining collar
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 20f - recoil, center.y - 9f), size = Size(4f, 18f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRoundRect(color = Color(0xFF71717A), topLeft = Offset(center.x + barrelLength - 2f - recoil, center.y - 9.5f), size = Size(4.5f, 19f), cornerRadius = CornerRadius(1.5f, 1.5f))

                // Housing with heat sink radiator fins
                drawRoundRect(color = Color(0xFF18181B), topLeft = Offset(center.x - 12f, center.y - 11f), size = Size(18f, 22f), cornerRadius = CornerRadius(3.5f, 3.5f))
                drawRoundRect(color = Color(0xFF27272A), topLeft = Offset(center.x - 10f, center.y - 9f), size = Size(14f, 18f), cornerRadius = CornerRadius(2.5f, 2.5f))
                for (f in 0..2) {
                    drawRect(color = Color(0xFF52525B), topLeft = Offset(center.x - 8f + f * 4f, center.y - 7f), size = Size(2f, 14f))
                }
            }
            else -> {
                // Level 3: 8-barrel Heavy Rotary Storm Battery
                // Dual high-capacity helical drum magazines
                drawRoundRect(color = Color(0xFF09090B), topLeft = Offset(center.x - 8f, center.y - 19f), size = Size(15f, 8f), cornerRadius = CornerRadius(2.5f, 2.5f))
                drawRoundRect(color = Color(0xFF3F3F46), topLeft = Offset(center.x - 7f, center.y - 18f), size = Size(13f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 7f, center.y - 15f), size = Size(13f, 1.5f))

                drawRoundRect(color = Color(0xFF09090B), topLeft = Offset(center.x - 8f, center.y + 11f), size = Size(15f, 8f), cornerRadius = CornerRadius(2.5f, 2.5f))
                drawRoundRect(color = Color(0xFF3F3F46), topLeft = Offset(center.x - 7f, center.y + 12f), size = Size(13f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 7f, center.y + 13.5f), size = Size(13f, 1.5f))

                // 8-barrel storm battery assembly
                val barrelLength = 36f
                for (b in 0..7) {
                    val byOffset = -10.5f + b * 3f
                    val bColor = if (b == 0 || b == 7) Color(0xFF09090B) else if (b % 2 == 0) Color(0xFF27272A) else Color(0xFF3F3F46)
                    drawRect(color = bColor, topLeft = Offset(center.x + 3f - recoil, center.y + byOffset), size = Size(barrelLength, 2.2f))
                }

                // Twin reinforced barrel clamps & flash compensators
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 18f - recoil, center.y - 11.5f), size = Size(4.5f, 23f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 30f - recoil, center.y - 11.5f), size = Size(4.5f, 23f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawRoundRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + barrelLength - 1f - recoil, center.y - 12f), size = Size(4.5f, 24f), cornerRadius = CornerRadius(1.5f, 1.5f))

                // Reinforced armored traverse housing
                drawRoundRect(color = Color(0xFF09090B), topLeft = Offset(center.x - 14f, center.y - 13f), size = Size(20f, 26f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFF27272A), topLeft = Offset(center.x - 12f, center.y - 11f), size = Size(16f, 22f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x - 12f, center.y - 11f), size = Size(16f, 22f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 1.5f))
            }
        }

        // Turret optics sensor dome with amber tracking ring
        drawCircle(color = Color(0xFF09090B), radius = 11f, center = center)
        drawCircle(color = Color(0xFF27272A), radius = 9f, center = center)
        drawCircle(color = Color(0xFFF59E0B), radius = 6f, center = Offset(center.x - 1f, center.y - 1f))
        drawCircle(color = Color(0xFFFFFFFF), radius = 2f, center = Offset(center.x - 2f, center.y - 2f))

        // Firing Animation: Tight, snappy alternating muzzle micro-flash
        if (tower.isFiring) {
            val tipX = center.x + (if (tower.spec.level == 1) 28f else if (tower.spec.level == 2) 34f else 38f) - recoil
            val flashOffset = if ((time * 100).toInt() % 2 == 0) -3f else 3f
            drawCircle(color = Color(0xFFFB923C), radius = 7f, center = Offset(tipX + 2f, center.y + flashOffset))
            drawCircle(color = Color(0xFFFDE047), radius = 4f, center = Offset(tipX + 2f, center.y + flashOffset))
            drawCircle(color = Color(0xFFFFFFFF), radius = 2f, center = Offset(tipX + 2f, center.y + flashOffset))
            drawLine(Color(0xFFFEF08A), Offset(tipX - 1f, center.y + flashOffset), Offset(tipX + 9f, center.y + flashOffset), strokeWidth = 2f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEnemies(enemies: List<Enemy>, time: Float) {
    for (enemy in enemies) {
        if (!enemy.isAlive || enemy.reachedBase) continue

        // Wobble walking animation
        val wobbleY = sin(enemy.animWobbleTime) * 1.5f
        val center = Offset(enemy.position.x, enemy.position.y + wobbleY)

        // 1. Ground Drop Shadow (World-space coordinates)
        val shadowRadius = enemy.spec.radius
        val shadowWidth = if (enemy.spec.isBoss) shadowRadius * 2.3f else shadowRadius * 1.8f
        val shadowHeight = if (enemy.spec.isBoss) shadowRadius * 1.2f else shadowRadius * 0.75f
        val shadowYOffset = if (enemy.spec.isBoss) shadowRadius * 0.45f else shadowRadius * 0.40f
        drawOval(
            color = Color(0x4D000000),
            topLeft = Offset(center.x - shadowWidth / 2f, center.y + shadowYOffset - shadowHeight / 2f),
            size = Size(shadowWidth, shadowHeight)
        )

        // 2. Character Sprite facing movement direction
        rotate(degrees = enemy.headingAngle, pivot = center) {
            when (enemy.spec.type) {
                EnemyType.SCOUT -> drawScoutEnemy(center, enemy, time)
                EnemyType.SOLDIER -> drawSoldierEnemy(center, enemy, time)
                EnemyType.HEAVY -> drawHeavyEnemy(center, enemy, time)
                EnemyType.RUNNER -> drawRunnerEnemy(center, enemy, time)
                EnemyType.BOSS -> drawBossEnemy(center, enemy, time)
            }
        }

        // 3. Boss Shield Phase Barrier
        if (enemy.spec.isBoss && enemy.isShielded) {
            val pulse = sin(time * 8f) * 3f
            drawCircle(
                color = Color(0x33F59E0B),
                radius = enemy.spec.radius * 1.25f + pulse,
                center = center
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = enemy.spec.radius * 1.20f + pulse,
                center = center,
                style = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), time * 25f))
            )
        }

        // 4. World-Space Health Bars (Drawn horizontally upright directly above the unit)
        if (enemy.spec.isBoss) {
            // World-Space Boss Health Bar directly above the boss
            val bossBarWidth = 64f
            val bossBarHeight = 7.5f
            val bossBarTop = center.y - enemy.spec.radius * 1.35f - 24f
            val bossBarLeft = center.x - bossBarWidth / 2f

            // Outer dark container
            drawRoundRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(bossBarLeft - 2f, bossBarTop - 2f),
                size = Size(bossBarWidth + 4f, bossBarHeight + 4f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // Golden boss frame
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(bossBarLeft - 2f, bossBarTop - 2f),
                size = Size(bossBarWidth + 4f, bossBarHeight + 4f),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = 1.5f)
            )
            // Crimson track background
            drawRoundRect(
                color = Color(0xFF450A0A),
                topLeft = Offset(bossBarLeft, bossBarTop),
                size = Size(bossBarWidth, bossBarHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Health Fill
            val hpColor = when {
                enemy.healthPercentage > 0.5f -> Color(0xFFEF4444)
                enemy.healthPercentage > 0.25f -> Color(0xFFF97316)
                else -> Color(0xFFDC2626)
            }
            if (enemy.healthPercentage > 0f) {
                drawRoundRect(
                    color = hpColor,
                    topLeft = Offset(bossBarLeft, bossBarTop),
                    size = Size((bossBarWidth * enemy.healthPercentage).coerceAtLeast(1f), bossBarHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Top highlight gloss
                drawRoundRect(
                    color = Color(0x55FFFFFF),
                    topLeft = Offset(bossBarLeft, bossBarTop),
                    size = Size(bossBarWidth * enemy.healthPercentage, bossBarHeight * 0.45f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
            // Golden skull / crown indicator dot above
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 3f,
                center = Offset(center.x, bossBarTop - 5f)
            )
        } else if (enemy.healthPercentage < 1.0f) {
            // Standard Enemy Health Bar (Only displayed when damaged to keep screen clean)
            val barWidth = (enemy.spec.radius * 2f).coerceAtLeast(32f)
            val barHeight = 4.5f
            val barTop = center.y - enemy.spec.radius - 12f
            val barLeft = center.x - barWidth / 2f

            drawRoundRect(
                color = Color(0xCC0F172A),
                topLeft = Offset(barLeft - 1f, barTop - 1f),
                size = Size(barWidth + 2f, barHeight + 2f),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
            val hpColor = when {
                enemy.healthPercentage > 0.5f -> Color(0xFF22C55E)
                enemy.healthPercentage > 0.25f -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
            drawRoundRect(
                color = hpColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth * enemy.healthPercentage, barHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

/**
 * 1. Scout Enemy: Nimble, light goblin scout.
 * Forward lean, animated running legs, canvas courier backpack, leather jerkin,
 * pointed ears, skullcap with goggles, keen eyes, and carved scout sling/dagger.
 */
private fun DrawScope.drawScoutEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 1.8f)

    // Running Legs (stepping forward and back along the movement X axis)
    val bootColor = Color(0xFF451A03)
    val legGreen = Color(0xFF15803D)
    // Left Leg
    val leftLegX = center.x - 2f + stride * 7f
    val leftLegY = center.y - 7f
    drawLine(legGreen, Offset(center.x - 2f, center.y - 5f), Offset(leftLegX, leftLegY), strokeWidth = 3f, cap = StrokeCap.Round)
    drawOval(bootColor, topLeft = Offset(leftLegX - 2f, leftLegY - 2.5f), size = Size(6f, 4f))
    // Right Leg
    val rightLegX = center.x - 2f - stride * 7f
    val rightLegY = center.y + 7f
    drawLine(legGreen, Offset(center.x - 2f, center.y + 5f), Offset(rightLegX, rightLegY), strokeWidth = 3f, cap = StrokeCap.Round)
    drawOval(bootColor, topLeft = Offset(rightLegX - 2f, rightLegY - 1.5f), size = Size(6f, 4f))

    // Canvas Courier Backpack strapped behind torso (-X side)
    drawRoundRect(
        color = Color(0xFF92400E),
        topLeft = Offset(center.x - 12f, center.y - 5f),
        size = Size(7f, 10f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Bedroll tied to backpack top
    drawRoundRect(
        color = Color(0xFFD97706),
        topLeft = Offset(center.x - 13f, center.y - 6f),
        size = Size(4f, 12f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    // Rope ties
    drawLine(Color(0xFFFEF3C7), Offset(center.x - 12f, center.y - 3f), Offset(center.x - 9f, center.y - 3f), strokeWidth = 1f)
    drawLine(Color(0xFFFEF3C7), Offset(center.x - 12f, center.y + 3f), Offset(center.x - 9f, center.y + 3f), strokeWidth = 1f)

    // Torso / Leather Jerkin (leaning forward into movement)
    drawRoundRect(
        color = Color(0xFFB45309),
        topLeft = Offset(center.x - 6f, center.y - 6f),
        size = Size(10f, 12f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Cross strap & belt
    drawLine(Color(0xFF78350F), Offset(center.x - 4f, center.y - 5f), Offset(center.x + 2f, center.y + 5f), strokeWidth = 2f)
    drawCircle(Color(0xFFF59E0B), radius = 1.5f, center = Offset(center.x, center.y))

    // Arms & Equipment
    // Left Arm (swinging back with fist)
    val leftArmX = center.x - 1f - stride * 4f
    drawLine(legGreen, Offset(center.x, center.y - 5f), Offset(leftArmX, center.y - 9f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF16A34A), radius = 2f, center = Offset(leftArmX, center.y - 9f))
    // Right Arm (swinging forward with carved wooden sling / scout dagger)
    val rightArmX = center.x + 4f + stride * 4f
    drawLine(legGreen, Offset(center.x + 1f, center.y + 5f), Offset(rightArmX, center.y + 8f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Scout Dagger / Sling
    drawLine(Color(0xFF78350F), Offset(rightArmX, center.y + 8f), Offset(rightArmX + 6f, center.y + 9f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(Color(0xFFE2E8F0), Offset(rightArmX + 4f, center.y + 9f), Offset(rightArmX + 9f, center.y + 9f), strokeWidth = 1.8f, cap = StrokeCap.Round)

    // Head / Face (+X direction)
    val headCenter = Offset(center.x + 5f, center.y)
    // Pointed Goblin Ears extending laterally
    val leftEar = Path().apply {
        moveTo(headCenter.x - 1f, headCenter.y - 4f)
        lineTo(headCenter.x - 3f, headCenter.y - 10f)
        lineTo(headCenter.x + 2f, headCenter.y - 4f)
        close()
    }
    drawPath(leftEar, Color(0xFF15803D))
    val rightEar = Path().apply {
        moveTo(headCenter.x - 1f, headCenter.y + 4f)
        lineTo(headCenter.x - 3f, headCenter.y + 10f)
        lineTo(headCenter.x + 2f, headCenter.y + 4f)
        close()
    }
    drawPath(rightEar, Color(0xFF15803D))

    // Head base
    drawCircle(Color(0xFF16A34A), radius = 5.5f, center = headCenter)
    // Leather Scout Skullcap
    drawArc(
        color = Color(0xFF78350F),
        startAngle = 100f,
        sweepAngle = 160f,
        useCenter = true,
        topLeft = Offset(headCenter.x - 5.5f, headCenter.y - 5.5f),
        size = Size(11f, 11f)
    )
    // Scout Goggles perched on cap
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(headCenter.x + 1f, headCenter.y - 4f),
        size = Size(3f, 8f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(Color(0xFF38BDF8), radius = 1.4f, center = Offset(headCenter.x + 2.5f, headCenter.y - 2f))
    drawCircle(Color(0xFF38BDF8), radius = 1.4f, center = Offset(headCenter.x + 2.5f, headCenter.y + 2f))

    // Keen Cartoon Eyes looking forward
    drawCircle(Color.White, radius = 2.2f, center = Offset(headCenter.x + 4f, headCenter.y - 2.2f))
    drawCircle(Color.White, radius = 2.2f, center = Offset(headCenter.x + 4f, headCenter.y + 2.2f))
    drawCircle(Color(0xFF0F172A), radius = 1.2f, center = Offset(headCenter.x + 4.8f, headCenter.y - 2.2f))
    drawCircle(Color(0xFF0F172A), radius = 1.2f, center = Offset(headCenter.x + 4.8f, headCenter.y + 2.2f))
    drawCircle(Color.White, radius = 0.5f, center = Offset(headCenter.x + 5.1f, headCenter.y - 2.6f))
    drawCircle(Color.White, radius = 0.5f, center = Offset(headCenter.x + 5.1f, headCenter.y + 1.8f))

    // Hit Reaction Flash
    if (enemy.isHitFlashing) {
        drawCircle(Color(0xEEFFFFFF), radius = 14f, center = center)
        drawCircle(Color(0xFFFFD166), radius = 10f, center = center)
    }
}

/**
 * 2. Soldier Enemy: Disciplined vanguard foot soldier / royal infantry.
 * Iron kettle helm with golden ridge comb, shadowed visor, steel cuirass over emerald surcoat,
 * steel kite shield on left arm, steel broadsword extending forward on right arm, armored marching greaves.
 */
private fun DrawScope.drawSoldierEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 1.0f)

    // Armored Marching Legs & Sabatons
    val steelGreave = Color(0xFF475569)
    val bootIron = Color(0xFF1E293B)
    // Left Leg
    val leftLegX = center.x - 3f + stride * 6f
    val leftLegY = center.y - 7f
    drawLine(steelGreave, Offset(center.x - 3f, center.y - 5f), Offset(leftLegX, leftLegY), strokeWidth = 4f, cap = StrokeCap.Round)
    drawRoundRect(bootIron, topLeft = Offset(leftLegX - 2f, leftLegY - 2.5f), size = Size(7f, 5f), cornerRadius = CornerRadius(2f, 2f))
    // Right Leg
    val rightLegX = center.x - 3f - stride * 6f
    val rightLegY = center.y + 7f
    drawLine(steelGreave, Offset(center.x - 3f, center.y + 5f), Offset(rightLegX, rightLegY), strokeWidth = 4f, cap = StrokeCap.Round)
    drawRoundRect(bootIron, topLeft = Offset(rightLegX - 2f, rightLegY - 2.5f), size = Size(7f, 5f), cornerRadius = CornerRadius(2f, 2f))

    // Torso / Emerald Surcoat Base
    drawRoundRect(
        color = Color(0xFF0D9488),
        topLeft = Offset(center.x - 7f, center.y - 8f),
        size = Size(13f, 16f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Steel Cuirass (Breastplate)
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(center.x - 5f, center.y - 7f),
        size = Size(10f, 14f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF64748B),
        topLeft = Offset(center.x - 4f, center.y - 6f),
        size = Size(8f, 12f),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 1.2f)
    )
    // Leather Utility Belt with Brass Buckle
    drawRect(Color(0xFF78350F), topLeft = Offset(center.x - 6f, center.y - 7f), size = Size(3f, 14f))
    drawRect(Color(0xFFF59E0B), topLeft = Offset(center.x - 6f, center.y - 2f), size = Size(3f, 4f))

    // Left Arm & Steel Kite / Heater Shield
    val shieldCenter = Offset(center.x - 1f, center.y - 12f)
    // Shield Body (Pointed heraldic shield)
    val shieldPath = Path().apply {
        moveTo(shieldCenter.x - 7f, shieldCenter.y - 5f)
        lineTo(shieldCenter.x + 8f, shieldCenter.y - 5f)
        lineTo(shieldCenter.x + 9f, shieldCenter.y + 2f)
        lineTo(shieldCenter.x, shieldCenter.y + 7f)
        lineTo(shieldCenter.x - 7f, shieldCenter.y + 2f)
        close()
    }
    drawPath(shieldPath, Color(0xFF0F172A))
    // Shield Face
    val shieldInner = Path().apply {
        moveTo(shieldCenter.x - 5.5f, shieldCenter.y - 3.5f)
        lineTo(shieldCenter.x + 6.5f, shieldInnerY(shieldCenter.y - 3.5f))
        lineTo(shieldCenter.x + 7f, shieldCenter.y + 1f)
        lineTo(shieldCenter.x, shieldCenter.y + 5.5f)
        lineTo(shieldCenter.x - 5.5f, shieldCenter.y + 1f)
        close()
    }
    drawPath(shieldInner, Color(0xFF0D9488))
    // Golden Shield Boss / Emblem
    drawCircle(Color(0xFFF59E0B), radius = 2.2f, center = shieldCenter)
    drawCircle(Color(0xFFFEF3C7), radius = 1f, center = shieldCenter)

    // Right Arm & Polished Steel Broadsword
    val swordArmX = center.x + 2f + stride * 3f
    val swordHandY = center.y + 10f
    // Arm
    drawLine(Color(0xFF334155), Offset(center.x, center.y + 6f), Offset(swordArmX, swordHandY), strokeWidth = 3.5f, cap = StrokeCap.Round)
    // Sword Crossguard & Pommel
    drawLine(Color(0xFFF59E0B), Offset(swordArmX, swordHandY - 4f), Offset(swordArmX, swordHandY + 4f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(Color(0xFF78350F), Offset(swordArmX - 3f, swordHandY), Offset(swordArmX, swordHandY), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Steel Blade extending forward
    val bladePath = Path().apply {
        moveTo(swordArmX, swordHandY - 1.5f)
        lineTo(swordArmX + 13f, swordHandY - 1f)
        lineTo(swordArmX + 17f, swordHandY)
        lineTo(swordArmX + 13f, swordHandY + 1f)
        lineTo(swordArmX, swordHandY + 1.5f)
        close()
    }
    drawPath(bladePath, Color(0xFFE2E8F0))
    drawLine(Color.White, Offset(swordArmX + 1f, swordHandY), Offset(swordArmX + 15f, swordHandY), strokeWidth = 1f)

    // Helmet & Head (+X forward)
    val headPos = Offset(center.x + 4f, center.y)
    // Domed Kettle Helm
    drawCircle(Color(0xFF475569), radius = 6.5f, center = headPos)
    drawCircle(Color(0xFF1E293B), radius = 6.5f, center = headPos, style = Stroke(width = 1.5f))
    // Golden Comb / Crest Ridge on helmet top
    drawLine(Color(0xFFF59E0B), Offset(headPos.x - 5f, headPos.y), Offset(headPos.x + 6f, headPos.y), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Visor Brim Rim
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(headPos.x + 1f, headPos.y - 5.5f),
        size = Size(4f, 11f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    // Determined Eyes peering under visor
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headPos.x + 3f, headPos.y - 3.5f),
        size = Size(2.5f, 7f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(Color.White, radius = 1.2f, center = Offset(headPos.x + 4.5f, headPos.y - 1.8f))
    drawCircle(Color.White, radius = 1.2f, center = Offset(headPos.x + 4.5f, headPos.y + 1.8f))

    // Hit Reaction Flash
    if (enemy.isHitFlashing) {
        drawCircle(Color(0xEEFFFFFF), radius = 16f, center = center)
        drawCircle(Color(0xFF67E8F9), radius = 12f, center = center)
    }
}

private fun shieldInnerY(y: Float): Float = y

/**
 * 3. Heavy Enemy: Hulking Ironclad Juggernaut (1.35x scale).
 * Massive trapezoidal bulk, spiked iron pauldrons flaring outward, bolted dark-crimson cuirass,
 * horned iron greathelm with glowing ember eye slit, colossal two-handed spiked iron slab war maul.
 */
private fun DrawScope.drawHeavyEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 0.6f)

    // Colossal Spiked Sabatons & Greaves (heavy lumbering footsteps)
    val ironDark = Color(0xFF0F172A)
    val plateSteel = Color(0xFF334155)
    // Left Foot
    val leftFootX = center.x - 6f + stride * 7f
    val leftFootY = center.y - 14f
    drawRoundRect(ironDark, topLeft = Offset(leftFootX - 4f, leftFootY - 4f), size = Size(12f, 8f), cornerRadius = CornerRadius(3f, 3f))
    drawRoundRect(plateSteel, topLeft = Offset(leftFootX - 3f, leftFootY - 3f), size = Size(10f, 6f), cornerRadius = CornerRadius(2f, 2f))
    // Left boot forward spike
    drawLine(Color(0xFFCBD5E1), Offset(leftFootX + 8f, leftFootY), Offset(leftFootX + 11f, leftFootY), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Right Foot
    val rightFootX = center.x - 6f - stride * 7f
    val rightFootY = center.y + 14f
    drawRoundRect(ironDark, topLeft = Offset(rightFootX - 4f, rightFootY - 4f), size = Size(12f, 8f), cornerRadius = CornerRadius(3f, 3f))
    drawRoundRect(plateSteel, topLeft = Offset(rightFootX - 3f, rightFootY - 3f), size = Size(10f, 6f), cornerRadius = CornerRadius(2f, 2f))
    // Right boot forward spike
    drawLine(Color(0xFFCBD5E1), Offset(rightFootX + 8f, rightFootY), Offset(rightFootX + 11f, rightFootY), strokeWidth = 2.5f, cap = StrokeCap.Round)

    // Colossal Spiked Shoulder Pauldrons (Flaring far out laterally)
    // Left Pauldron
    val leftPauldronPos = Offset(center.x - 1f, center.y - 18f)
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(leftPauldronPos.x - 7f, leftPauldronPos.y - 6f),
        size = Size(14f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF7F1D1D),
        topLeft = Offset(leftPauldronPos.x - 5f, leftPauldronPos.y - 4f),
        size = Size(10f, 8f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Pauldron Iron Spikes
    drawLine(Color(0xFFCBD5E1), Offset(leftPauldronPos.x + 3f, leftPauldronPos.y - 4f), Offset(leftPauldronPos.x + 10f, leftPauldronPos.y - 9f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(leftPauldronPos.x - 3f, leftPauldronPos.y - 5f), Offset(leftPauldronPos.x - 5f, leftPauldronPos.y - 11f), strokeWidth = 3f, cap = StrokeCap.Round)

    // Right Pauldron
    val rightPauldronPos = Offset(center.x - 1f, center.y + 18f)
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(rightPauldronPos.x - 7f, rightPauldronPos.y - 6f),
        size = Size(14f, 12f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF7F1D1D),
        topLeft = Offset(rightPauldronPos.x - 5f, rightPauldronPos.y - 4f),
        size = Size(10f, 8f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Pauldron Iron Spikes
    drawLine(Color(0xFFCBD5E1), Offset(rightPauldronPos.x + 3f, rightPauldronPos.y + 4f), Offset(rightPauldronPos.x + 10f, rightPauldronPos.y + 9f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(rightPauldronPos.x - 3f, rightPauldronPos.y + 5f), Offset(rightPauldronPos.x - 5f, rightPauldronPos.y + 11f), strokeWidth = 3f, cap = StrokeCap.Round)

    // Hulking Torso (Layered Rust-Crimson Iron Armor Cuirass)
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - 12f, center.y - 14f),
        size = Size(20f, 28f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color(0xFF991B1B),
        topLeft = Offset(center.x - 10f, center.y - 12f),
        size = Size(16f, 24f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Bolted Cross Girders & Iron Rivets
    drawLine(Color(0xFF334155), Offset(center.x - 9f, center.y - 10f), Offset(center.x + 5f, center.y + 10f), strokeWidth = 3.5f)
    drawLine(Color(0xFF334155), Offset(center.x - 9f, center.y + 10f), Offset(center.x + 5f, center.y - 10f), strokeWidth = 3.5f)
    drawCircle(Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x - 6f, center.y - 7f))
    drawCircle(Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x - 6f, center.y + 7f))
    drawCircle(Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x + 3f, center.y - 7f))
    drawCircle(Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x + 3f, center.y + 7f))

    // Colossal Spiked Iron Slab War Maul (Held forward across both armored gauntlets)
    val maulHaftX = center.x + 5f + stride * 3f
    // Heavy iron haft
    drawLine(Color(0xFF1E293B), Offset(maulHaftX - 8f, center.y - 8f), Offset(maulHaftX + 16f, center.y - 8f), strokeWidth = 4f, cap = StrokeCap.Round)
    // Massive Spiked Slab Hammer Head
    val hammerHeadX = maulHaftX + 16f
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(hammerHeadX - 4f, center.y - 16f),
        size = Size(9f, 16f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(hammerHeadX - 3f, center.y - 14f),
        size = Size(7f, 12f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    // Spikes on hammer head face
    drawLine(Color(0xFFCBD5E1), Offset(hammerHeadX + 5f, center.y - 13f), Offset(hammerHeadX + 9f, center.y - 13f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(hammerHeadX + 5f, center.y - 9f), Offset(hammerHeadX + 9f, center.y - 9f), strokeWidth = 2.5f, cap = StrokeCap.Round)

    // Horned Iron Greathelm & Head (+X forward)
    val headPos = Offset(center.x + 5f, center.y)
    // Curved Iron Battle Horns flaring from helmet
    val leftHorn = Path().apply {
        moveTo(headPos.x - 2f, headPos.y - 6f)
        cubicTo(headPos.x - 4f, headPos.y - 12f, headPos.x + 4f, headPos.y - 16f, headPos.x + 10f, headPos.y - 14f)
        lineTo(headPos.x + 1f, headPos.y - 7f)
        close()
    }
    drawPath(leftHorn, Color(0xFFCBD5E1))
    val rightHorn = Path().apply {
        moveTo(headPos.x - 2f, headPos.y + 6f)
        cubicTo(headPos.x - 4f, headPos.y + 12f, headPos.x + 4f, headPos.y + 16f, headPos.x + 10f, headPos.y + 14f)
        lineTo(headPos.x + 1f, headPos.y + 7f)
        close()
    }
    drawPath(rightHorn, Color(0xFFCBD5E1))

    // Greathelm Dome
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(headPos.x - 5f, headPos.y - 8f),
        size = Size(12f, 16f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(headPos.x - 3f, headPos.y - 6f),
        size = Size(8f, 12f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // Horizontal Visor Grille with Glowing Ember/Orange Eye Slit
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headPos.x + 3f, headPos.y - 6f),
        size = Size(3.5f, 12f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    drawRoundRect(
        color = Color(0xFFF97316),
        topLeft = Offset(headPos.x + 4.5f, headPos.y - 4.5f),
        size = Size(2f, 9f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(Color(0xFFFDE047), radius = 1f, center = Offset(headPos.x + 5.5f, headPos.y - 2.5f))
    drawCircle(Color(0xFFFDE047), radius = 1f, center = Offset(headPos.x + 5.5f, headPos.y + 2.5f))

    // Hit Reaction Flash
    if (enemy.isHitFlashing) {
        drawCircle(Color(0xEEFFFFFF), radius = 24f, center = center)
        drawCircle(Color(0xFFF97316), radius = 18f, center = center)
    }
}

/**
 * 4. Runner Enemy: Shadowblade Windstrider Assassin (0.85x scale, elongated silhouette).
 * Low aerodynamic forward sprint, midnight-cyan stealth garb, waving twin scarf ribbons,
 * wind hood with glowing speed goggles, twin reverse-grip daggers swept back, high-speed scissor legs.
 */
private fun DrawScope.drawRunnerEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 2.5f)

    // High-Velocity Scissor Legs
    val bootColor = Color(0xFF0C4A6E)
    val legColor = Color(0xFF0284C7)
    // Left Leg (extended forward / back)
    val leftLegX = center.x - 4f + stride * 9f
    val leftLegY = center.y - 6f
    drawLine(legColor, Offset(center.x - 3f, center.y - 4f), Offset(leftLegX, leftLegY), strokeWidth = 3f, cap = StrokeCap.Round)
    drawOval(bootColor, topLeft = Offset(leftLegX - 2f, leftLegY - 2f), size = Size(7f, 4f))
    // Winglet on heel
    drawLine(Color(0xFF38BDF8), Offset(leftLegX - 2f, leftLegY), Offset(leftLegX - 6f, leftLegY - 2f), strokeWidth = 1.5f)

    // Right Leg
    val rightLegX = center.x - 4f - stride * 9f
    val rightLegY = center.y + 6f
    drawLine(legColor, Offset(center.x - 3f, center.y + 4f), Offset(rightLegX, rightLegY), strokeWidth = 3f, cap = StrokeCap.Round)
    drawOval(bootColor, topLeft = Offset(rightLegX - 2f, rightLegY - 2f), size = Size(7f, 4f))
    // Winglet on heel
    drawLine(Color(0xFF38BDF8), Offset(rightLegX - 2f, rightLegY), Offset(rightLegX - 6f, rightLegY + 2f), strokeWidth = 1.5f)

    // Dynamic Trailing Scarf / Cowl Ribbons waving behind into the wind (-X direction)
    val ribbonWave = sin(time * 14f) * 3f
    val ribbon1 = Path().apply {
        moveTo(center.x - 6f, center.y - 3f)
        quadraticTo(center.x - 14f, center.y - 5f + ribbonWave, center.x - 22f, center.y - 8f - ribbonWave)
        lineTo(center.x - 20f, center.y - 6f - ribbonWave)
        quadraticTo(center.x - 13f, center.y - 3f + ribbonWave, center.x - 6f, center.y - 1f)
        close()
    }
    drawPath(ribbon1, Color(0xFF38BDF8))

    val ribbon2 = Path().apply {
        moveTo(center.x - 6f, center.y + 3f)
        quadraticTo(center.x - 14f, center.y + 5f - ribbonWave, center.x - 22f, center.y + 8f + ribbonWave)
        lineTo(center.x - 20f, center.y + 6f + ribbonWave)
        quadraticTo(center.x - 13f, center.y + 3f - ribbonWave, center.x - 6f, center.y + 1f)
        close()
    }
    drawPath(ribbon2, Color(0xFF0284C7))

    // Sleek Form-Fitting Body / Ninja Tunic
    drawOval(
        color = Color(0xFF0369A1),
        topLeft = Offset(center.x - 8f, center.y - 6f),
        size = Size(16f, 12f)
    )
    drawOval(
        color = Color(0xFF0284C7),
        topLeft = Offset(center.x - 6f, center.y - 4.5f),
        size = Size(12f, 9f)
    )

    // Swept-Back Arms holding twin reverse-grip razor daggers
    // Left Dagger & Arm
    val leftArmX = center.x - 2f - stride * 3f
    drawLine(Color(0xFF0C4A6E), Offset(center.x, center.y - 5f), Offset(leftArmX, center.y - 9f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Reverse-grip blade pointing backwards
    drawLine(Color(0xFFE0F2FE), Offset(leftArmX, center.y - 9f), Offset(leftArmX - 10f, center.y - 13f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(Color(0xFF38BDF8), Offset(leftArmX - 1f, center.y - 9f), Offset(leftArmX - 7f, center.y - 12f), strokeWidth = 1f)

    // Right Dagger & Arm
    val rightArmX = center.x - 2f + stride * 3f
    drawLine(Color(0xFF0C4A6E), Offset(center.x, center.y + 5f), Offset(rightArmX, center.y + 9f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    // Reverse-grip blade pointing backwards
    drawLine(Color(0xFFE0F2FE), Offset(rightArmX, center.y + 9f), Offset(rightArmX - 10f, center.y + 13f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(Color(0xFF38BDF8), Offset(rightArmX - 1f, center.y + 9f), Offset(rightArmX - 7f, center.y + 12f), strokeWidth = 1f)

    // Aerodynamic Wind Hood & Head (+X forward)
    val headPos = Offset(center.x + 6f, center.y)
    drawOval(Color(0xFF075985), topLeft = Offset(headPos.x - 4f, headPos.y - 4.5f), size = Size(9f, 9f))
    // Glowing Electric Speed Goggles
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(headPos.x + 2f, headPos.y - 3.5f),
        size = Size(2.5f, 7f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(Color.White, radius = 1.2f, center = Offset(headPos.x + 3.2f, headPos.y - 1.8f))
    drawCircle(Color.White, radius = 1.2f, center = Offset(headPos.x + 3.2f, headPos.y + 1.8f))

    // Tiny sprint speed trail lines behind boots
    drawLine(Color(0x6638BDF8), Offset(center.x - 10f, center.y - 6f), Offset(center.x - 18f, center.y - 6f), strokeWidth = 1.5f)
    drawLine(Color(0x6638BDF8), Offset(center.x - 10f, center.y + 6f), Offset(center.x - 18f, center.y + 6f), strokeWidth = 1.5f)

    // Hit Reaction Flash
    if (enemy.isHitFlashing) {
        drawCircle(Color(0xEEFFFFFF), radius = 14f, center = center)
        drawCircle(Color(0xFF38BDF8), radius = 10f, center = center)
    }
}

/**
 * 5. Boss Enemy: The Dread Warlord Overlord / Siege Titan (2.5x+ visual scale).
 * COMPLETELY NON-CIRCULAR, ASYMMETRICAL, INTIMIDATING 2D CARTOON WARLORD SILHOUETTE:
 * - Massive segmented plate cuirass (royal crimson & dark iron) with pulsing chest furnace core & rear exhaust chimneys.
 * - Left Shoulder: Towering Fortress Bulwark Pauldron with triple layered siege armor and forward fortress spikes!
 * - Right Arm: Hydraulic armored gauntlet wielding a colossal Jagged Siege War Axe with glowing runic core!
 * - Regal shredded battle-torn crimson war cape billowing behind the warlord.
 * - Horned dread-helm with towering battle horns, golden crown brow, and glowing demonic eyes.
 * - Armored greaves and spiked siege boots that thunderously stomp the ground.
 */
private fun DrawScope.drawBossEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 0.45f)

    // Billowing Ragged Regal War Cape (-X direction behind the warlord)
    val capeWave = sin(enemy.animWobbleTime * 0.9f) * 4f
    val capePath = Path().apply {
        moveTo(center.x - 8f, center.y - 18f)
        cubicTo(
            center.x - 22f, center.y - 24f + capeWave,
            center.x - 36f, center.y - 26f - capeWave,
            center.x - 44f, center.y - 14f
        )
        lineTo(center.x - 42f, center.y - 4f)
        lineTo(center.x - 46f, center.y + 6f)
        lineTo(center.x - 40f, center.y + 16f)
        cubicTo(
            center.x - 34f, center.y + 24f + capeWave,
            center.x - 20f, center.y + 22f - capeWave,
            center.x - 8f, center.y + 18f
        )
        close()
    }
    drawPath(capePath, Color(0xFF4C0519))
    drawPath(capePath, Color(0xFFF59E0B), style = Stroke(width = 1.8f))

    // Massive Armored Greaves & Spiked Siege Boots
    val bootBase = Color(0xFF0F172A)
    val bootSteel = Color(0xFF1E293B)
    // Left Boot
    val leftBootX = center.x - 8f + stride * 9f
    val leftBootY = center.y - 20f
    drawRoundRect(bootBase, topLeft = Offset(leftBootX - 6f, leftBootY - 7f), size = Size(18f, 14f), cornerRadius = CornerRadius(4f, 4f))
    drawRoundRect(bootSteel, topLeft = Offset(leftBootX - 4f, leftBootY - 5f), size = Size(14f, 10f), cornerRadius = CornerRadius(2.5f, 2.5f))
    // Forward iron spike on left boot
    drawLine(Color(0xFFCBD5E1), Offset(leftBootX + 12f, leftBootY), Offset(leftBootX + 18f, leftBootY), strokeWidth = 3.5f, cap = StrokeCap.Round)

    // Right Boot
    val rightBootX = center.x - 8f - stride * 9f
    val rightBootY = center.y + 20f
    drawRoundRect(bootBase, topLeft = Offset(rightBootX - 6f, rightBootY - 7f), size = Size(18f, 14f), cornerRadius = CornerRadius(4f, 4f))
    drawRoundRect(bootSteel, topLeft = Offset(rightBootX - 4f, rightBootY - 5f), size = Size(14f, 10f), cornerRadius = CornerRadius(2.5f, 2.5f))
    // Forward iron spike on right boot
    drawLine(Color(0xFFCBD5E1), Offset(rightBootX + 12f, rightBootY), Offset(rightBootX + 18f, rightBootY), strokeWidth = 3.5f, cap = StrokeCap.Round)

    // Rear Dual Exhaust Chimneys (behind neck)
    drawRoundRect(Color(0xFF1E293B), topLeft = Offset(center.x - 14f, center.y - 12f), size = Size(6f, 6f), cornerRadius = CornerRadius(1.5f, 1.5f))
    drawRoundRect(Color(0xFF1E293B), topLeft = Offset(center.x - 14f, center.y + 6f), size = Size(6f, 6f), cornerRadius = CornerRadius(1.5f, 1.5f))
    // Tiny puff of dark smoke from chimneys
    val smokeAlpha = (sin(time * 6f) * 0.2f + 0.35f)
    drawCircle(Color(0xFF475569).copy(alpha = smokeAlpha), radius = 3.5f, center = Offset(center.x - 18f, center.y - 12f))
    drawCircle(Color(0xFF475569).copy(alpha = smokeAlpha), radius = 3.5f, center = Offset(center.x - 18f, center.y + 6f))

    // ASYMMETRICAL LEFT SIDE: Towering Fortress Bulwark Pauldron (Spiked Siege Shield Pauldron)
    val leftPauldronPos = Offset(center.x, center.y - 24f)
    val fortressPauldron = Path().apply {
        moveTo(leftPauldronPos.x - 12f, leftPauldronPos.y + 4f)
        lineTo(leftPauldronPos.x - 14f, leftPauldronPos.y - 10f)
        lineTo(leftPauldronPos.x - 4f, leftPauldronPos.y - 16f)
        lineTo(leftPauldronPos.x + 12f, leftPauldronPos.y - 12f)
        lineTo(leftPauldronPos.x + 16f, leftPauldronPos.y + 2f)
        lineTo(leftPauldronPos.x + 6f, leftPauldronPos.y + 6f)
        close()
    }
    drawPath(fortressPauldron, Color(0xFF0F172A))
    // Inner plate
    val fortressInner = Path().apply {
        moveTo(leftPauldronPos.x - 10f, leftPauldronPos.y + 2f)
        lineTo(leftPauldronPos.x - 12f, leftPauldronPos.y - 8f)
        lineTo(leftPauldronPos.x - 3f, leftPauldronPos.y - 13f)
        lineTo(leftPauldronPos.x + 10f, leftPauldronPos.y - 10f)
        lineTo(leftPauldronPos.x + 13f, leftPauldronPos.y)
        close()
    }
    drawPath(fortressInner, Color(0xFF881337))
    drawPath(fortressInner, Color(0xFFF59E0B), style = Stroke(width = 2f))
    // Jagged Fortress Pauldron Spikes
    drawLine(Color(0xFFCBD5E1), Offset(leftPauldronPos.x + 8f, leftPauldronPos.y - 11f), Offset(leftPauldronPos.x + 18f, leftPauldronPos.y - 18f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(leftPauldronPos.x - 2f, leftPauldronPos.y - 14f), Offset(leftPauldronPos.x + 2f, leftPauldronPos.y - 22f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(Color(0xFFCBD5E1), Offset(leftPauldronPos.x - 11f, leftPauldronPos.y - 9f), Offset(leftPauldronPos.x - 18f, leftPauldronPos.y - 15f), strokeWidth = 3.5f, cap = StrokeCap.Round)

    // Segmented Fortress Cuirass / Chestplate (Royal Crimson & Iron Plate)
    val chestPath = Path().apply {
        moveTo(center.x - 14f, center.y - 16f)
        lineTo(center.x + 8f, center.y - 18f)
        lineTo(center.x + 14f, center.y - 10f)
        lineTo(center.x + 14f, center.y + 10f)
        lineTo(center.x + 8f, center.y + 18f)
        lineTo(center.x - 14f, center.y + 16f)
        close()
    }
    drawPath(chestPath, Color(0xFF1E1B4B))
    // Cuirass Inner Plate
    val chestInner = Path().apply {
        moveTo(center.x - 12f, center.y - 14f)
        lineTo(center.x + 6f, center.y - 15f)
        lineTo(center.x + 11f, center.y - 8f)
        lineTo(center.x + 11f, center.y + 8f)
        lineTo(center.x + 6f, center.y + 15f)
        lineTo(center.x - 12f, center.y + 14f)
        close()
    }
    drawPath(chestInner, Color(0xFF881337))
    drawPath(chestInner, Color(0xFFF59E0B), style = Stroke(width = 2f))

    // Pulsing Furnace Core / Runic Heart Vent in chest
    val corePulse = (sin(time * 6f) * 0.25f + 0.75f)
    val corePos = Offset(center.x - 2f, center.y)
    drawCircle(Color(0xFF450A0A), radius = 7f, center = corePos)
    drawCircle(Color(0xFFEF4444), radius = 5.5f * corePulse, center = corePos)
    drawCircle(Color(0xFFFDE047), radius = 3.2f * corePulse, center = corePos)
    drawCircle(Color.White, radius = 1.5f * corePulse, center = corePos)
    // Core iron grille bars
    drawLine(Color(0xFF1E293B), Offset(corePos.x - 6f, corePos.y), Offset(corePos.x + 6f, corePos.y), strokeWidth = 1.8f)
    drawLine(Color(0xFF1E293B), Offset(corePos.x, corePos.y - 6f), Offset(corePos.x, corePos.y + 6f), strokeWidth = 1.8f)

    // Colossal War Belt with Carved Demon/Skull Brass Buckle
    drawRect(Color(0xFF0F172A), topLeft = Offset(center.x - 14f, center.y - 12f), size = Size(4f, 24f))
    drawRoundRect(Color(0xFFD97706), topLeft = Offset(center.x - 15f, center.y - 5f), size = Size(5f, 10f), cornerRadius = CornerRadius(2f, 2f))
    drawCircle(Color(0xFFFEF3C7), radius = 1.5f, center = Offset(center.x - 12.5f, center.y))

    // ASYMMETRICAL RIGHT SIDE: Giant Hydraulic Gauntlet & Colossal Jagged Siege War Axe
    val axeHaftX = center.x + 4f + stride * 5f
    val axeArmY = center.y + 22f
    // Right Shoulder Articulated Joint
    drawCircle(Color(0xFF1E293B), radius = 8f, center = Offset(center.x + 2f, axeArmY))
    drawCircle(Color(0xFFD97706), radius = 5f, center = Offset(center.x + 2f, axeArmY))
    // Hydraulic Arm / Gauntlet gripping axe haft
    drawLine(Color(0xFF334155), Offset(center.x + 2f, axeArmY), Offset(axeHaftX + 8f, axeArmY + 4f), strokeWidth = 6f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF0F172A), radius = 4f, center = Offset(axeHaftX + 8f, axeArmY + 4f))

    // Colossal Forged Siege Cleaver / War Greataxe
    val axeHeadX = axeHaftX + 14f
    val axeHeadY = axeArmY + 4f
    // Reinforced Iron Shaft
    drawLine(Color(0xFF18181B), Offset(axeHeadX - 24f, axeHeadY), Offset(axeHeadX + 16f, axeHeadY), strokeWidth = 4.5f, cap = StrokeCap.Round)
    // Golden pommel & counterweight
    drawCircle(Color(0xFFF59E0B), radius = 3.5f, center = Offset(axeHeadX - 24f, axeHeadY))

    // Colossal Double-Beveled Axe Blade
    val axeBlade = Path().apply {
        moveTo(axeHeadX - 6f, axeHeadY - 14f)
        lineTo(axeHeadX + 14f, axeHeadY - 24f)
        lineTo(axeHeadX + 18f, axeHeadY - 6f)
        lineTo(axeHeadX + 14f, axeHeadY)
        lineTo(axeHeadX + 18f, axeHeadY + 6f)
        lineTo(axeHeadX + 14f, axeHeadY + 24f)
        lineTo(axeHeadX - 6f, axeHeadY + 14f)
        close()
    }
    drawPath(axeBlade, Color(0xFF0F172A))
    // Blade Steel Core
    val axeInner = Path().apply {
        moveTo(axeHeadX - 4f, axeHeadY - 11f)
        lineTo(axeHeadX + 11f, axeHeadY - 19f)
        lineTo(axeHeadX + 14f, axeHeadY - 4f)
        lineTo(axeHeadX + 11f, axeHeadY)
        lineTo(axeHeadX + 14f, axeHeadY + 4f)
        lineTo(axeHeadX + 11f, axeHeadY + 19f)
        lineTo(axeHeadX - 4f, axeHeadY + 11f)
        close()
    }
    drawPath(axeInner, Color(0xFF64748B))
    // Glowing Runic Blade Edge
    drawLine(Color(0xFFF59E0B), Offset(axeHeadX + 12f, axeHeadY - 22f), Offset(axeHeadX + 17f, axeHeadY), strokeWidth = 2f)
    drawLine(Color(0xFFF59E0B), Offset(axeHeadX + 17f, axeHeadY), Offset(axeHeadX + 12f, axeHeadY + 22f), strokeWidth = 2f)
    drawCircle(Color(0xFFFDE047), radius = 2.5f, center = Offset(axeHeadX + 4f, axeHeadY))

    // Horned Warlord Dread-Helm & Head (+X forward)
    val headPos = Offset(center.x + 9f, center.y)

    // Colossal Curved Warlord Battle Horns
    val leftHorn = Path().apply {
        moveTo(headPos.x - 3f, headPos.y - 8f)
        cubicTo(headPos.x - 4f, headPos.y - 18f, headPos.x + 8f, headPos.y - 26f, headPos.x + 18f, headPos.y - 24f)
        lineTo(headPos.x + 4f, headPos.y - 10f)
        close()
    }
    drawPath(leftHorn, Color(0xFFCBD5E1))
    drawPath(leftHorn, Color(0xFF94A3B8), style = Stroke(width = 1.5f))

    val rightHorn = Path().apply {
        moveTo(headPos.x - 3f, headPos.y + 8f)
        cubicTo(headPos.x - 4f, headPos.y + 18f, headPos.x + 8f, headPos.y + 26f, headPos.x + 18f, headPos.y + 24f)
        lineTo(headPos.x + 4f, headPos.y + 10f)
        close()
    }
    drawPath(rightHorn, Color(0xFFCBD5E1))
    drawPath(rightHorn, Color(0xFF94A3B8), style = Stroke(width = 1.5f))

    // Dread-Helm Base
    drawRoundRect(
        color = Color(0xFF1E1B4B),
        topLeft = Offset(headPos.x - 7f, headPos.y - 11f),
        size = Size(16f, 22f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawRoundRect(
        color = Color(0xFF4C0519),
        topLeft = Offset(headPos.x - 5f, headPos.y - 9f),
        size = Size(12f, 18f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // 5-Spike Golden Crown Crest on helmet brow
    val crownX = headPos.x - 2f
    for (s in -2..2) {
        val spikeY = headPos.y + (s * 4f)
        val spikeLen = if (s == 0) 8f else if (Math.abs(s) == 1) 6f else 4f
        drawLine(Color(0xFFF59E0B), Offset(crownX, spikeY), Offset(crownX + spikeLen, spikeY), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFEF4444), radius = 1.2f, center = Offset(crownX + spikeLen, spikeY))
    }

    // Shadowed Visor Slit with Glowing Demonic Eyes
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headPos.x + 5f, headPos.y - 8f),
        size = Size(4f, 16f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    // Piercing Ember/Ruby Eyes
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(headPos.x + 6.5f, headPos.y - 6f),
        size = Size(2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawRoundRect(
        color = Color(0xFFEF4444),
        topLeft = Offset(headPos.x + 6.5f, headPos.y + 2f),
        size = Size(2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(Color(0xFFFDE047), radius = 0.9f, center = Offset(headPos.x + 7.5f, headPos.y - 4f))
    drawCircle(Color(0xFFFDE047), radius = 0.9f, center = Offset(headPos.x + 7.5f, headPos.y + 4f))

    // Hit Reaction Flash & Armor Sheen
    if (enemy.isHitFlashing) {
        drawCircle(Color(0xEEFFFFFF), radius = 38f, center = center)
        drawCircle(Color(0xFFFFD166), radius = 30f, center = center)
    }
}

private fun DrawScope.drawProjectiles(projectiles: List<Projectile>) {
    for (p in projectiles) {
        if (p.isHit || p.isExpired) continue

        val center = Offset(p.currentPosition.x, p.currentPosition.y)
        val dx = p.currentPosition.x - p.prevPosition.x
        val dy = p.currentPosition.y - p.prevPosition.y
        val angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / Math.PI).toFloat()

        when (p.type) {
            ProjectileType.BULLET -> {
                // Cartoon tracer bullet: brass casing, copper jacketed round-nosed bullet, bright tip, motion streak
                rotate(degrees = angle, pivot = center) {
                    // Motion streak behind bullet
                    drawLine(
                        color = Color(0x88F59E0B),
                        start = Offset(center.x - 14f, center.y),
                        end = Offset(center.x - 2f, center.y),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFFEF08A),
                        start = Offset(center.x - 8f, center.y),
                        end = Offset(center.x, center.y),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )

                    // Solid brass / copper physical bullet body
                    drawRoundRect(
                        color = Color(0xFF78350F),
                        topLeft = Offset(center.x - 5f, center.y - 2.5f),
                        size = Size(9f, 5f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(center.x - 4f, center.y - 2f),
                        size = Size(7.5f, 4f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    // Specular highlight line
                    drawRect(
                        color = Color(0xFFFEF08A),
                        topLeft = Offset(center.x - 3f, center.y - 1.5f),
                        size = Size(4f, 1.2f)
                    )
                    // Bright lead/copper nose tip
                    drawCircle(color = Color(0xFFFDE047), radius = 2f, center = Offset(center.x + 3.5f, center.y))
                    drawCircle(color = Color.White, radius = 1.2f, center = Offset(center.x + 3.5f, center.y))
                }
            }
            ProjectileType.CANNONBALL -> {
                // Heavy cast iron cannonball with drop shadow, specular shine, smoke puff trail & sparking fuse
                rotate(degrees = angle, pivot = center) {
                    // Trailing smoke puffs behind cannonball flight vector
                    drawCircle(color = Color(0x4494A3B8), radius = 4.5f, center = Offset(center.x - 15f, center.y))
                    drawCircle(color = Color(0x6664748B), radius = 6f, center = Offset(center.x - 9f, center.y))

                    // Sparking fuse on rear
                    drawCircle(color = Color(0xFFEA580C), radius = 3f, center = Offset(center.x - 7.5f, center.y))
                    drawCircle(color = Color(0xFFFACC15), radius = 1.5f, center = Offset(center.x - 7.5f, center.y))
                }

                // Drop shadow
                drawCircle(color = Color(0x45000000), radius = 8f, center = Offset(center.x + 3f, center.y + 5f))

                // Heavy cast iron sphere with 2D cartoon specular rendering
                drawCircle(color = Color(0xFF0F172A), radius = 8f, center = center)
                drawCircle(color = Color(0xFF1E293B), radius = 7f, center = center)
                drawCircle(color = Color(0xFF334155), radius = 5f, center = Offset(center.x - 2f, center.y - 2f))
                drawCircle(color = Color(0xFF94A3B8), radius = 2.5f, center = Offset(center.x - 3f, center.y - 3f))
                drawCircle(color = Color.White, radius = 1.2f, center = Offset(center.x - 3.5f, center.y - 3.5f))
            }
            ProjectileType.RAPID_SLUG -> {
                // High-velocity kinetic dart / sabot slug with amber tracer trail and distinct fins
                rotate(degrees = angle, pivot = center) {
                    // Tapered amber / orange flame trail
                    drawLine(
                        color = Color(0x66EA580C),
                        start = Offset(center.x - 18f, center.y),
                        end = Offset(center.x - 2f, center.y),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFF59E0B),
                        start = Offset(center.x - 13f, center.y),
                        end = Offset(center.x, center.y),
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFFEF08A),
                        start = Offset(center.x - 7f, center.y),
                        end = Offset(center.x + 2f, center.y),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )

                    // Kinetic tungsten/amber dart body
                    drawRoundRect(
                        color = Color(0xFF18181B),
                        topLeft = Offset(center.x - 6f, center.y - 2.5f),
                        size = Size(11f, 5f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    drawRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(center.x - 5f, center.y - 1.8f),
                        size = Size(7f, 3.6f)
                    )

                    // Stabilizer fin accents
                    drawLine(Color(0xFFF59E0B), Offset(center.x - 6f, center.y - 4f), Offset(center.x - 2f, center.y - 2f), strokeWidth = 1.5f)
                    drawLine(Color(0xFFF59E0B), Offset(center.x - 6f, center.y + 4f), Offset(center.x - 2f, center.y + 2f), strokeWidth = 1.5f)

                    // Needle-sharp tungsten tip
                    drawCircle(color = Color(0xFFFEF08A), radius = 2.2f, center = Offset(center.x + 4.5f, center.y))
                    drawCircle(color = Color.White, radius = 1.2f, center = Offset(center.x + 5f, center.y))
                }
            }
        }
    }
}

private fun DrawScope.drawVisualEffects(effects: List<VisualEffect>) {
    for (fx in effects) {
        val center = Offset(fx.position.x, fx.position.y)
        val alpha = (1f - fx.progress).coerceIn(0f, 1f)

        when (fx.type) {
            EffectType.CANNON_EXPLOSION -> {
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)

                // Ground scorch ring
                drawCircle(
                    color = Color(0x55000000).copy(alpha = alpha * 0.5f),
                    radius = currentR * 0.95f,
                    center = center
                )

                // Billowing expanding smoke puffs at 6 positions
                for (i in 0..5) {
                    val ang = i * (Math.PI / 3.0).toFloat() + fx.progress
                    val dist = currentR * 0.7f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFF64748B).copy(alpha = alpha * 0.6f),
                        radius = currentR * 0.45f,
                        center = Offset(px, py)
                    )
                }

                // Fireball explosion core layers
                drawCircle(
                    color = Color(0xFFEA580C).copy(alpha = alpha),
                    radius = currentR * 0.70f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = alpha),
                    radius = currentR * 0.42f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.20f,
                    center = center
                )

                // Flying cartoon shrapnel debris & spark stars
                for (s in 0..4) {
                    val sAng = s * (Math.PI * 2.0 / 5.0).toFloat() + fx.progress * 3f
                    val sDist = currentR * (0.8f + fx.progress * 0.6f)
                    val sx = center.x + cos(sAng) * sDist
                    val sy = center.y + sin(sAng) * sDist
                    // Metal fleck / star
                    drawCircle(color = Color(0xFF0F172A).copy(alpha = alpha), radius = 2.5f, center = Offset(sx, sy))
                    drawCircle(color = Color(0xFFFFD166).copy(alpha = alpha), radius = 1.8f * alpha, center = Offset(sx - 0.5f, sy - 0.5f))
                }
            }
            EffectType.MG_HIT_SPARK -> {
                val currentR = fx.maxRadius * (0.4f + fx.progress * 0.6f)

                // Snappy 4-point cartoon flash
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    start = Offset(center.x - currentR, center.y),
                    end = Offset(center.x + currentR, center.y),
                    strokeWidth = 2.8f * alpha,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    start = Offset(center.x, center.y - currentR),
                    end = Offset(center.x, center.y + currentR),
                    strokeWidth = 2.8f * alpha,
                    cap = StrokeCap.Round
                )

                // Radiating metal flecks & sparks
                for (s in 0..3) {
                    val ang = (s * Math.PI / 2.0 + Math.PI / 4.0).toFloat() + fx.progress
                    val dist = currentR * 0.9f
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawCircle(color = Color(0xFFF59E0B).copy(alpha = alpha), radius = 2f * alpha, center = Offset(sx, sy))
                }

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.35f,
                    center = center
                )
            }
            EffectType.RAPID_HIT_SPARK -> {
                val currentR = fx.maxRadius * (0.5f + fx.progress * 0.5f)

                // High-frequency 6-spike amber/copper crackle
                for (i in 0..5) {
                    val ang = i * (Math.PI / 3.0).toFloat()
                    val len = currentR * (if (i % 2 == 0) 1.1f else 0.7f)
                    val sx = center.x + cos(ang) * len
                    val sy = center.y + sin(ang) * len
                    drawLine(
                        color = Color(0xFFF59E0B).copy(alpha = alpha),
                        start = center,
                        end = Offset(sx, sy),
                        strokeWidth = 2f * alpha,
                        cap = StrokeCap.Round
                    )
                }

                // Bright point impact center
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = alpha),
                    radius = currentR * 0.45f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.22f,
                    center = center
                )
            }
            EffectType.BOSS_HIT_IMPACT -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)

                // Heavy armored deflection shockwave ring
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.9f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                drawCircle(
                    color = Color(0xFFFFD166).copy(alpha = alpha * 0.6f),
                    radius = currentR * 0.7f,
                    center = center,
                    style = Stroke(width = 2f * alpha)
                )

                // Deflection ricochet sparks bursting sideways
                for (s in 0..5) {
                    val ang = s * (Math.PI / 3.0).toFloat() + fx.progress * 2f
                    val dist = currentR * 1.1f
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawCircle(color = Color(0xFFFDE047).copy(alpha = alpha), radius = 2.5f * alpha, center = Offset(sx, sy))
                }

                // Intense center impact flash
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.35f,
                    center = center
                )
            }
            EffectType.ENEMY_DEATH_POOF -> {
                val currentR = fx.maxRadius * (0.4f + fx.progress * 0.6f)
                for (i in 0..4) {
                    val angle = i * 1.25f
                    val px = center.x + cos(angle) * (currentR * 0.55f)
                    val py = center.y + sin(angle) * (currentR * 0.55f)
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.45f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF94A3B8).copy(alpha = alpha * 0.4f),
                        radius = currentR * 0.45f,
                        center = Offset(px, py),
                        style = Stroke(width = 1.5f)
                    )
                }
                // Floating golden spark upward
                val floatY = center.y - fx.progress * 24f
                drawCircle(color = Color(0xFFFFD166).copy(alpha = alpha), radius = 3.5f, center = Offset(center.x, floatY))
            }
            EffectType.SCOUT_DEATH -> {
                // Light, quick dust pop with tiny scattered satchel/feather fragments
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                for (i in 0..3) {
                    val ang = i * 1.57f + fx.progress * 1.2f
                    val px = center.x + cos(ang) * (currentR * 0.6f)
                    val py = center.y + sin(ang) * (currentR * 0.6f)
                    drawCircle(
                        color = Color(0xFFFEF3C7).copy(alpha = alpha * 0.8f),
                        radius = currentR * 0.35f,
                        center = Offset(px, py)
                    )
                }
                // Tiny scattered bits
                for (s in 0..2) {
                    val ang = s * 2.09f + fx.progress * 3f
                    val dist = currentR * 0.9f
                    drawCircle(
                        color = Color(0xFFB45309).copy(alpha = alpha),
                        radius = 2f * alpha,
                        center = Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist)
                    )
                }
            }
            EffectType.SOLDIER_DEATH -> {
                // Medium combat defeat puff with tumbling steel sparks
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)
                for (i in 0..4) {
                    val ang = i * 1.25f + fx.progress * 0.8f
                    val px = center.x + cos(ang) * (currentR * 0.65f)
                    val py = center.y + sin(ang) * (currentR * 0.65f)
                    drawCircle(
                        color = Color(0xFFCBD5E1).copy(alpha = alpha * 0.75f),
                        radius = currentR * 0.40f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF64748B).copy(alpha = alpha * 0.35f),
                        radius = currentR * 0.40f,
                        center = Offset(px, py),
                        style = Stroke(width = 1.2f)
                    )
                }
                // Tumbling metallic glints
                for (s in 0..3) {
                    val ang = s * 1.57f + fx.progress * 2.5f
                    val dist = currentR * 0.85f
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = alpha),
                        radius = 2.5f * alpha,
                        center = Offset(center.x + cos(ang) * dist, center.y + sin(ang) * dist)
                    )
                }
            }
            EffectType.HEAVY_DEATH -> {
                // Heavy armor shatter: dense dark smoke bursts, fiery orange embers, and flying iron fragments
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)
                // Dark smoke clouds
                for (i in 0..5) {
                    val ang = i * 1.04f + fx.progress * 0.6f
                    val px = center.x + cos(ang) * (currentR * 0.6f)
                    val py = center.y + sin(ang) * (currentR * 0.6f)
                    drawCircle(
                        color = Color(0xFF334155).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.45f,
                        center = Offset(px, py)
                    )
                }
                // Center fiery flash
                drawCircle(
                    color = Color(0xFFEA580C).copy(alpha = alpha),
                    radius = currentR * 0.5f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = alpha),
                    radius = currentR * 0.28f,
                    center = center
                )
                // Flying jagged iron shrapnel
                for (s in 0..4) {
                    val ang = s * 1.25f + fx.progress * 1.8f
                    val dist = currentR * (0.5f + fx.progress * 0.6f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = alpha),
                        topLeft = Offset(sx - 3f, sy - 2f),
                        size = Size(6f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                }
            }
            EffectType.BOSS_DEATH -> {
                // Epic multi-ring shockwave, massive golden-crimson explosion, and radiating champion sparks
                val currentR = fx.maxRadius * (0.25f + fx.progress * 0.75f)

                // Expanding ground shockwave ring
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.7f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 4f * alpha)
                )
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha * 0.5f),
                    radius = currentR * 0.75f,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )

                // Billowing heavy blast dust clouds in 8 directions
                for (i in 0..7) {
                    val ang = i * 0.785f + fx.progress * 0.5f
                    val dist = currentR * 0.55f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFF450A0A).copy(alpha = alpha * 0.8f),
                        radius = currentR * 0.40f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF78350F).copy(alpha = alpha * 0.6f),
                        radius = currentR * 0.28f,
                        center = Offset(px, py)
                    )
                }

                // Core radiant flash
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    radius = currentR * 0.45f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.25f,
                    center = center
                )

                // Flying royal armor shrapnel & golden sparks
                for (s in 0..7) {
                    val ang = s * 0.785f + fx.progress * 2.2f
                    val dist = currentR * (0.6f + fx.progress * 0.5f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFFFFD166).copy(alpha = alpha),
                        radius = 3.5f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.BOSS_ENTRANCE -> {
                // Ground slam entrance shockwave: radial dust clouds, ground cracks, and warning aura
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)

                // Heavy ground impact ring
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha * 0.65f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 4.5f * alpha)
                )
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.45f),
                    radius = currentR * 0.7f,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )

                // Radiating ground cracks
                for (c in 0..5) {
                    val ang = c * 1.047f
                    val endX = center.x + cos(ang) * (currentR * 0.85f)
                    val endY = center.y + sin(ang) * (currentR * 0.85f)
                    drawLine(
                        color = Color(0xFFF97316).copy(alpha = alpha * 0.8f),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.5f * alpha,
                        cap = StrokeCap.Round
                    )
                }

                // Billowing ground dust clouds in 6 radial positions
                for (i in 0..5) {
                    val ang = i * 1.047f + fx.progress * 0.4f
                    val dist = currentR * 0.65f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = alpha * 0.7f),
                        radius = currentR * 0.38f,
                        center = Offset(px, py)
                    )
                }

                // Center entrance flare
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha * 0.85f),
                    radius = currentR * 0.3f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.15f,
                    center = center
                )
            }
        }
    }
}

private fun DrawScope.drawPlacementPreview(gameState: GameState) {
    val pos = gameState.previewPlacementPos ?: return
    val center = Offset(pos.x, pos.y)
    val spec = gameState.selectedTowerSpec ?: return

    val isValid = gameState.isValidPlacement
    val ringColor = if (isValid) Color(0xFF22C55E) else Color(0xFFEF4444)
    val fillColor = if (isValid) Color(0x3322C55E) else Color(0x33EF4444)

    // Range preview
    drawCircle(color = fillColor, radius = spec.range, center = center)
    drawCircle(
        color = ringColor,
        radius = spec.range,
        center = center,
        style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
    )

    // Tower footprint preview
    drawCircle(color = ringColor.copy(alpha = 0.6f), radius = spec.size / 2f, center = center)
    drawCircle(color = Color.White, radius = spec.size / 2f, center = center, style = Stroke(width = 2.5f))
}
