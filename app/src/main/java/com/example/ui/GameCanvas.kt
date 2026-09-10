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

    // Soft drop shadow
    drawCircle(
        color = Color(0x50000000),
        radius = baseR * 1.05f,
        center = Offset(center.x + 3f, center.y + 5f)
    )

    // Fortified stone base platform with bevel
    drawCircle(color = Color(0xFF334155), radius = baseR, center = center)
    drawCircle(color = Color(0xFF475569), radius = baseR * 0.90f, center = center)
    drawCircle(color = Color(0xFF1E293B), radius = baseR, center = center, style = Stroke(width = 2.5f))

    // 4 Corner heavy rivet plates
    for (i in 0..3) {
        val ang = (i * Math.PI / 2.0 + Math.PI / 4.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.76f)
        val by = center.y + sin(ang) * (baseR * 0.76f)
        drawCircle(color = Color(0xFF0F172A), radius = 3.5f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.2f, center = Offset(bx - 0.5f, by - 0.5f))
    }

    // Rotating Turret Top & Barrels
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Swivel ring mount
        drawCircle(color = Color(0xFF0F172A), radius = 16f, center = center)
        drawCircle(color = Color(0xFF64748B), radius = 14f, center = center)
        drawCircle(color = Color(0xFF334155), radius = 14f, center = center, style = Stroke(width = 2f))

        val recoil = tower.recoilFraction * 6f

        when (tower.spec.level) {
            1 -> {
                // Level 1: Twin gunmetal barrels
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y - 7f),
                    size = Size(26f, 4.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y + 2.5f),
                    size = Size(26f, 4.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                // Highlights
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 4f - recoil, center.y - 6f), size = Size(20f, 1.5f))
                drawRect(color = Color(0xFF64748B), topLeft = Offset(center.x + 4f - recoil, center.y + 3.5f), size = Size(20f, 1.5f))
                // Brass muzzle rings
                drawRect(color = Color(0xFFFFD166), topLeft = Offset(center.x + 24f - recoil, center.y - 7.5f), size = Size(4f, 5.5f))
                drawRect(color = Color(0xFFFFD166), topLeft = Offset(center.x + 24f - recoil, center.y + 2f), size = Size(4f, 5.5f))
            }
            2 -> {
                // Level 2: Extended heavy twin barrels with cooling jacket vents
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y - 8f),
                    size = Size(32f, 5.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x + 2f - recoil, center.y + 2.5f),
                    size = Size(32f, 5.5f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Circular cooling perforations
                for (h in 0..2) {
                    drawCircle(color = Color(0xFF475569), radius = 1.5f, center = Offset(center.x + 12f + h * 6f - recoil, center.y - 5.2f))
                    drawCircle(color = Color(0xFF475569), radius = 1.5f, center = Offset(center.x + 12f + h * 6f - recoil, center.y + 5.2f))
                }
                // Dual side ammo drums
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x - 6f, center.y - 17f), size = Size(13f, 7f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x - 6f, center.y + 10f), size = Size(13f, 7f), cornerRadius = CornerRadius(2f, 2f))
                // Heavy muzzle brakes
                drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + 30f - recoil, center.y - 9f), size = Size(5f, 7.5f))
                drawRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + 30f - recoil, center.y + 1.5f), size = Size(5f, 7.5f))
            }
            else -> {
                // Level 3: Gatling Vulcan Rotary Cluster
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 7f, center.y - 16f),
                    size = Size(14f, 32f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(center.x - 7f, center.y - 16f),
                    size = Size(14f, 32f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 2f)
                )
                // 4-barrel rotary Gatling array
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 2f - recoil, center.y - 8f), size = Size(36f, 3.5f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 2f - recoil, center.y - 3f), size = Size(37f, 4f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 2f - recoil, center.y + 2f), size = Size(37f, 4f))
                drawRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 2f - recoil, center.y + 6.5f), size = Size(36f, 3.5f))
                // Golden barrel clamps
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 22f - recoil, center.y - 9f), size = Size(5f, 18f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFFF59E0B), topLeft = Offset(center.x + 34f - recoil, center.y - 9f), size = Size(4f, 18f), cornerRadius = CornerRadius(2f, 2f))
                // Golden ammo belt
                for (b in 0..2) {
                    drawRect(color = Color(0xFFFFD166), topLeft = Offset(center.x - 12f + b * 4f, center.y + 11f + b * 2f), size = Size(3f, 6f))
                }
            }
        }

        // Turret Center Dome
        drawCircle(color = Color(0xFF0284C7), radius = 13f, center = center)
        drawCircle(color = Color(0xFF38BDF8), radius = 9f, center = Offset(center.x - 2f, center.y - 2f))
        drawCircle(color = Color(0xFFFFFFFF), radius = 3.5f, center = Offset(center.x - 3f, center.y - 3f))

        // Recoil Muzzle Flash (Vibrant cartoon starburst on firing)
        if (tower.isFiring) {
            val muzzleTipX = center.x + (if (tower.spec.level == 1) 28f else if (tower.spec.level == 2) 35f else 38f) - recoil
            drawCircle(color = Color(0xFFFFD166), radius = 10f, center = Offset(muzzleTipX + 4f, center.y))
            drawCircle(color = Color(0xFFFFFFFF), radius = 5f, center = Offset(muzzleTipX + 4f, center.y))
            drawLine(Color(0xFFFFE066), Offset(muzzleTipX - 2f, center.y), Offset(muzzleTipX + 16f, center.y), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFE066), Offset(muzzleTipX + 4f, center.y - 10f), Offset(muzzleTipX + 4f, center.y + 10f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawCannonTower(center: Offset, tower: Tower) {
    val baseR = tower.spec.size * 0.48f

    // Soft drop shadow
    drawCircle(
        color = Color(0x50000000),
        radius = baseR * 1.05f,
        center = Offset(center.x + 4f, center.y + 6f)
    )

    // Rugged chiseled granite foundation
    drawCircle(color = Color(0xFF1E293B), radius = baseR, center = center)
    drawCircle(color = Color(0xFF334155), radius = baseR * 0.90f, center = center)
    drawCircle(color = Color(0xFF475569), radius = baseR * 0.75f, center = center)

    // Corner bolted iron brackets
    for (i in 0..3) {
        val ang = (i * Math.PI / 2.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.78f)
        val by = center.y + sin(ang) * (baseR * 0.78f)
        drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(bx - 3.5f, by - 3.5f), size = Size(7f, 7f), cornerRadius = CornerRadius(2f, 2f))
        drawCircle(color = Color(0xFF0F172A), radius = 1.5f, center = Offset(bx, by))
    }

    // Rotating Turret Top & Barrels
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Mechanical swivel turntable
        drawCircle(color = Color(0xFF0F172A), radius = 17f, center = center)
        drawCircle(color = Color(0xFF475569), radius = 15f, center = center)

        // Heavy trunnion side brackets
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 10f, center.y - 15f), size = Size(13f, 6f), cornerRadius = CornerRadius(2f, 2f))
        drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x - 10f, center.y + 9f), size = Size(13f, 6f), cornerRadius = CornerRadius(2f, 2f))
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.5f, center = Offset(center.x - 3f, center.y - 12f))
        drawCircle(color = Color(0xFFCBD5E1), radius = 2.5f, center = Offset(center.x - 3f, center.y + 12f))

        val recoil = tower.recoilFraction * 9f

        when (tower.spec.level) {
            1 -> {
                // Level 1: Classic heavy mortar barrel
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 2f - recoil, center.y - 9f),
                    size = Size(32f, 18f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Specular highlight
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x - 2f - recoil, center.y - 6f), size = Size(28f, 3f))
                // Brass breech ring
                drawRoundRect(color = Color(0xFFD97706), topLeft = Offset(center.x + 8f - recoil, center.y - 10f), size = Size(4f, 20f), cornerRadius = CornerRadius(2f, 2f))
                // Flared muzzle lip
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 27f - recoil, center.y - 11f), size = Size(7f, 22f), cornerRadius = CornerRadius(3f, 3f))
                drawRoundRect(color = Color(0xFFE2E8F0), topLeft = Offset(center.x + 27f - recoil, center.y - 11f), size = Size(7f, 22f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(width = 1.5f))
            }
            2 -> {
                // Level 2: Extended Siege Howitzer with hydraulic pistons
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 4f - recoil, center.y - 10f),
                    size = Size(39f, 20f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRect(color = Color(0xFF475569), topLeft = Offset(center.x - 4f - recoil, center.y - 7f), size = Size(36f, 3.5f))
                // Top & bottom hydraulic recoil pistons
                drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(center.x - 2f - recoil, center.y - 13f), size = Size(18f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF64748B), topLeft = Offset(center.x - 2f - recoil, center.y + 9f), size = Size(18f, 4f), cornerRadius = CornerRadius(2f, 2f))
                // Double reinforced blast collar
                drawRoundRect(color = Color(0xFFCBD5E1), topLeft = Offset(center.x + 20f - recoil, center.y - 11.5f), size = Size(5f, 23f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x + 33f - recoil, center.y - 12f), size = Size(8f, 24f), cornerRadius = CornerRadius(3f, 3f))
            }
            else -> {
                // Level 3: Devastator Mortar with glowing thermal vents
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x - 6f - recoil, center.y - 12f),
                    size = Size(44f, 24f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Molten thermal exhaust vents
                for (v in 0..2) {
                    drawRect(color = Color(0xFFF97316), topLeft = Offset(center.x + 6f + v * 8f - recoil, center.y - 4f), size = Size(5f, 8f))
                }
                // Angled blast shield deflectors
                drawRoundRect(color = Color(0xFFDC2626), topLeft = Offset(center.x - 8f, center.y - 18f), size = Size(12f, 36f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFFFFD166), topLeft = Offset(center.x - 8f, center.y - 18f), size = Size(12f, 36f), cornerRadius = CornerRadius(4f, 4f), style = Stroke(width = 2f))
                // Spiked colossal bore
                drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(center.x + 35f - recoil, center.y - 14f), size = Size(10f, 28f), cornerRadius = CornerRadius(4f, 4f))
                drawRoundRect(color = Color(0xFFF97316), topLeft = Offset(center.x + 35f - recoil, center.y - 14f), size = Size(10f, 28f), cornerRadius = CornerRadius(4f, 4f), style = Stroke(width = 2f))
            }
        }

        // Turret Breech Cap
        drawCircle(color = Color(0xFFDC2626), radius = 10f, center = Offset(center.x - 4f, center.y))
        drawCircle(color = Color(0xFFFFFFFF), radius = 3.5f, center = Offset(center.x - 6f, center.y - 2f))

        // Cannon Firing Effect: Massive Fireball Puff & Smoke
        if (tower.isFiring) {
            val muzzleTipX = center.x + (if (tower.spec.level == 1) 34f else if (tower.spec.level == 2) 41f else 45f) - recoil
            drawCircle(color = Color(0xFF64748B).copy(alpha = 0.7f), radius = 16f, center = Offset(muzzleTipX + 8f, center.y))
            drawCircle(color = Color(0xFFEA580C), radius = 13f, center = Offset(muzzleTipX + 5f, center.y))
            drawCircle(color = Color(0xFFFACC15), radius = 8f, center = Offset(muzzleTipX + 4f, center.y))
            drawCircle(color = Color(0xFFFFFFFF), radius = 4f, center = Offset(muzzleTipX + 3f, center.y))
        }
    }
}

private fun DrawScope.drawRapidFireTower(center: Offset, tower: Tower, time: Float) {
    val baseR = tower.spec.size * 0.48f

    // Soft drop shadow
    drawCircle(
        color = Color(0x50000000),
        radius = baseR * 1.05f,
        center = Offset(center.x + 3f, center.y + 5f)
    )

    // High-tech obsidian base with glowing runic energy nodes
    drawCircle(color = Color(0xFF1E1B4B), radius = baseR, center = center)
    drawCircle(color = Color(0xFF312E81), radius = baseR * 0.88f, center = center)
    drawCircle(color = Color(0xFF6366F1), radius = baseR * 0.88f, center = center, style = Stroke(width = 2f))

    for (i in 0..5) {
        val ang = (i * Math.PI / 3.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.74f)
        val by = center.y + sin(ang) * (baseR * 0.74f)
        drawCircle(color = Color(0xFF38BDF8), radius = 2.2f, center = Offset(bx, by))
    }

    // Rotating Turret Top & Barrels
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Swivel chassis
        drawCircle(color = Color(0xFF4C1D95), radius = 16f, center = center)
        drawCircle(color = Color(0xFFA855F7), radius = 15f, center = center, style = Stroke(width = 2f))

        // Central Pulsing Energy Core
        val pulse = (sin(time * 8f) * 0.15f + 0.85f)
        drawCircle(color = Color(0x66A855F7), radius = 12f * pulse, center = center)
        drawCircle(color = Color(0xFFC084FC), radius = 8f * pulse, center = center)
        drawCircle(color = Color(0xFFFFFFFF), radius = 3.5f, center = center)

        when (tower.spec.level) {
            1 -> {
                // Level 1: Dual forward crystal prongs with spark
                drawRoundRect(
                    color = Color(0xFF6B21A8),
                    topLeft = Offset(center.x + 2f, center.y - 9f),
                    size = Size(26f, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = Color(0xFF6B21A8),
                    topLeft = Offset(center.x + 2f, center.y + 5f),
                    size = Size(26f, 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Glowing cyan focus tips
                drawCircle(color = Color(0xFF38BDF8), radius = 3.5f, center = Offset(center.x + 28f, center.y - 7f))
                drawCircle(color = Color(0xFF38BDF8), radius = 3.5f, center = Offset(center.x + 28f, center.y + 7f))
                drawLine(Color(0xFFE0E7FF), Offset(center.x + 28f, center.y - 7f), Offset(center.x + 28f, center.y + 7f), strokeWidth = 1.5f)
            }
            2 -> {
                // Level 2: Triple plasma prongs & floating capacitors
                drawRoundRect(color = Color(0xFF581C87), topLeft = Offset(center.x + 2f, center.y - 10f), size = Size(28f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF581C87), topLeft = Offset(center.x + 5f, center.y - 2f), size = Size(32f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF581C87), topLeft = Offset(center.x + 2f, center.y + 6f), size = Size(28f, 4f), cornerRadius = CornerRadius(2f, 2f))
                // Cyan focus lenses
                drawCircle(color = Color(0xFF22D3EE), radius = 3.5f, center = Offset(center.x + 30f, center.y - 8f))
                drawCircle(color = Color(0xFF38BDF8), radius = 4.5f, center = Offset(center.x + 37f, center.y))
                drawCircle(color = Color(0xFF22D3EE), radius = 3.5f, center = Offset(center.x + 30f, center.y + 8f))
                // Side capacitors
                drawRoundRect(color = Color(0xFF0284C7), topLeft = Offset(center.x - 5f, center.y - 17f), size = Size(10f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF0284C7), topLeft = Offset(center.x - 5f, center.y + 11f), size = Size(10f, 6f), cornerRadius = CornerRadius(2f, 2f))
            }
            else -> {
                // Level 3: Quad ornate gold & electric indigo focus spikes
                drawRoundRect(color = Color(0xFF3B0764), topLeft = Offset(center.x + 2f, center.y - 12f), size = Size(32f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF3B0764), topLeft = Offset(center.x + 6f, center.y - 5f), size = Size(36f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF3B0764), topLeft = Offset(center.x + 6f, center.y + 1f), size = Size(36f, 4f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF3B0764), topLeft = Offset(center.x + 2f, center.y + 8f), size = Size(32f, 4f), cornerRadius = CornerRadius(2f, 2f))
                // Gold trim
                drawRoundRect(color = Color(0xFFFFD166), topLeft = Offset(center.x + 22f, center.y - 13f), size = Size(4f, 26f), cornerRadius = CornerRadius(2f, 2f))
                // Orbiting energy sparkles
                for (o in 0..2) {
                    val orbAng = time * 6f + o * (Math.PI * 2.0 / 3.0).toFloat()
                    val ox = center.x + cos(orbAng) * 22f
                    val oy = center.y + sin(orbAng) * 22f
                    drawCircle(color = Color(0xFF67E8F9), radius = 3f, center = Offset(ox, oy))
                    drawCircle(color = Color(0xFFFFFFFF), radius = 1.5f, center = Offset(ox, oy))
                }
            }
        }

        // Firing Burst Effect
        if (tower.isFiring) {
            val tipX = center.x + (if (tower.spec.level == 1) 30f else 38f)
            drawCircle(color = Color(0x88C084FC), radius = 13f, center = Offset(tipX, center.y))
            drawCircle(color = Color(0xFF22D3EE), radius = 7f, center = Offset(tipX, center.y))
            drawCircle(color = Color(0xFFFFFFFF), radius = 3.5f, center = Offset(tipX, center.y))
        }
    }
}

private fun DrawScope.drawEnemies(enemies: List<Enemy>, time: Float) {
    for (enemy in enemies) {
        if (!enemy.isAlive || enemy.reachedBase) continue

        // Wobble walking animation
        val wobbleY = sin(enemy.animWobbleTime) * 2.2f
        val center = Offset(enemy.position.x, enemy.position.y + wobbleY)

        // Drop shadow
        drawOval(
            color = Color(0x40000000),
            topLeft = Offset(center.x - enemy.spec.radius * 0.85f, center.y + enemy.spec.radius * 0.45f),
            size = Size(enemy.spec.radius * 1.7f, enemy.spec.radius * 0.75f)
        )

        when (enemy.spec.type) {
            EnemyType.SCOUT -> {
                // Cartoon flying bug/bat: flapping wings, warm amber body, glossy eyes, antennae
                val wingAngle = sin(enemy.animWobbleTime * 2.5f) * 12f
                // Translucent flapping wings
                drawRoundRect(
                    color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                    topLeft = Offset(center.x - 12f, center.y - 15f + wingAngle),
                    size = Size(9f, 15f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                    topLeft = Offset(center.x - 12f, center.y + 1f - wingAngle),
                    size = Size(9f, 15f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Amber Body
                drawOval(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(center.x - enemy.spec.radius, center.y - enemy.spec.radius * 0.75f),
                    size = Size(enemy.spec.radius * 2f, enemy.spec.radius * 1.5f)
                )
                drawOval(
                    color = Color(0xFFD97706),
                    topLeft = Offset(center.x - enemy.spec.radius, center.y - enemy.spec.radius * 0.75f),
                    size = Size(enemy.spec.radius * 2f, enemy.spec.radius * 1.5f),
                    style = Stroke(width = 2f)
                )
                // Belly Stripes
                drawLine(Color(0xFF78350F), Offset(center.x - 3f, center.y - 6f), Offset(center.x - 3f, center.y + 6f), strokeWidth = 2f)
                drawLine(Color(0xFF78350F), Offset(center.x + 2f, center.y - 5f), Offset(center.x + 2f, center.y + 5f), strokeWidth = 2f)
                // Large glossy cartoon eyes
                drawCircle(Color.White, radius = 5.5f, center = Offset(center.x + 8f, center.y - 4f))
                drawCircle(Color.White, radius = 5.5f, center = Offset(center.x + 8f, center.y + 4f))
                drawCircle(Color(0xFF0F172A), radius = 3.2f, center = Offset(center.x + 9.5f, center.y - 4f))
                drawCircle(Color(0xFF0F172A), radius = 3.2f, center = Offset(center.x + 9.5f, center.y + 4f))
                drawCircle(Color.White, radius = 1.2f, center = Offset(center.x + 10f, center.y - 5f))
                drawCircle(Color.White, radius = 1.2f, center = Offset(center.x + 10f, center.y + 3f))
                // Antennae with golden tips
                drawLine(Color(0xFF78350F), Offset(center.x + 6f, center.y - 6f), Offset(center.x + 14f, center.y - 11f), strokeWidth = 1.5f)
                drawLine(Color(0xFF78350F), Offset(center.x + 6f, center.y + 6f), Offset(center.x + 14f, center.y + 11f), strokeWidth = 1.5f)
                drawCircle(Color(0xFFFDE047), radius = 2f, center = Offset(center.x + 14f, center.y - 11f))
                drawCircle(Color(0xFFFDE047), radius = 2f, center = Offset(center.x + 14f, center.y + 11f))
            }
            EnemyType.SOLDIER -> {
                // Armored goblin/grunt: stout teal body, steel domed helmet with golden crest, fierce eyes, shield
                drawCircle(color = Color(0xFF0D9488), radius = enemy.spec.radius, center = center)
                drawCircle(color = Color(0xFF115E59), radius = enemy.spec.radius, center = center, style = Stroke(width = 2.5f))
                // Helmet with golden crest
                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(center.x - enemy.spec.radius * 0.95f, center.y - enemy.spec.radius * 1.05f),
                    size = Size(enemy.spec.radius * 1.9f, enemy.spec.radius * 1.15f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - enemy.spec.radius * 0.95f, center.y - enemy.spec.radius * 1.05f),
                    size = Size(enemy.spec.radius * 1.9f, enemy.spec.radius * 1.15f),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 2f)
                )
                // Golden helmet spike
                drawLine(Color(0xFFFFD166), Offset(center.x, center.y - enemy.spec.radius * 1.05f), Offset(center.x, center.y - enemy.spec.radius * 1.45f), strokeWidth = 3f, cap = StrokeCap.Round)
                // Visor rim
                drawRect(color = Color(0xFF334155), topLeft = Offset(center.x - enemy.spec.radius * 0.9f, center.y - 2f), size = Size(enemy.spec.radius * 1.8f, 3.5f))
                // Determined eyes
                drawCircle(Color.White, radius = 4.5f, center = Offset(center.x + 4f, center.y + 4f))
                drawCircle(Color.White, radius = 4.5f, center = Offset(center.x - 4f, center.y + 4f))
                drawCircle(Color(0xFF0F172A), radius = 2.5f, center = Offset(center.x + 5f, center.y + 4f))
                drawCircle(Color(0xFF0F172A), radius = 2.5f, center = Offset(center.x - 3f, center.y + 4f))
                // Buckler shield on side
                drawCircle(Color(0xFF92400E), radius = 6.5f, center = Offset(center.x - enemy.spec.radius + 2f, center.y + 2f))
                drawCircle(Color(0xFFD97706), radius = 3.5f, center = Offset(center.x - enemy.spec.radius + 2f, center.y + 2f))
            }
            EnemyType.HEAVY -> {
                // Hulking horned iron golem: wide trapezoidal body, curved horns, glowing eye slit
                drawRoundRect(
                    color = Color(0xFF991B1B),
                    topLeft = Offset(center.x - enemy.spec.radius, center.y - enemy.spec.radius),
                    size = Size(enemy.spec.radius * 2f, enemy.spec.radius * 2f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFF450A0A),
                    topLeft = Offset(center.x - enemy.spec.radius, center.y - enemy.spec.radius),
                    size = Size(enemy.spec.radius * 2f, enemy.spec.radius * 2f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 3f)
                )
                // Massive curved iron horns
                drawLine(Color(0xFFCBD5E1), Offset(center.x - enemy.spec.radius + 3f, center.y - enemy.spec.radius + 5f), Offset(center.x - enemy.spec.radius - 8f, center.y - enemy.spec.radius - 9f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(Color(0xFFCBD5E1), Offset(center.x + enemy.spec.radius - 3f, center.y - enemy.spec.radius + 5f), Offset(center.x + enemy.spec.radius + 8f, center.y - enemy.spec.radius - 9f), strokeWidth = 5f, cap = StrokeCap.Round)
                // Spiked shoulder plates
                drawCircle(color = Color(0xFF334155), radius = 6f, center = Offset(center.x - enemy.spec.radius + 2f, center.y))
                drawCircle(color = Color(0xFF334155), radius = 6f, center = Offset(center.x + enemy.spec.radius - 2f, center.y))
                // Visor slit with glowing orange eye slit
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 12f, center.y - 7f), size = Size(24f, 7f), cornerRadius = CornerRadius(3f, 3f))
                drawRect(color = Color(0xFFF97316), topLeft = Offset(center.x - 8f, center.y - 5f), size = Size(16f, 3f))
                // Steel chest plate rivets
                drawCircle(color = Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x - 6f, center.y + 8f))
                drawCircle(color = Color(0xFFE2E8F0), radius = 2f, center = Offset(center.x + 6f, center.y + 8f))
            }
            EnemyType.RUNNER -> {
                // Electric cyan speedster: streamlined raptor body, swept-back crest, afterimages
                drawCircle(color = Color(0x330284C7), radius = enemy.spec.radius * 0.7f, center = Offset(center.x - 12f, center.y))
                drawCircle(color = Color(0x550284C7), radius = enemy.spec.radius * 0.85f, center = Offset(center.x - 6f, center.y))
                // Streamlined aerodynamic body
                drawOval(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(center.x - enemy.spec.radius * 1.1f, center.y - enemy.spec.radius * 0.75f),
                    size = Size(enemy.spec.radius * 2.2f, enemy.spec.radius * 1.5f)
                )
                drawOval(
                    color = Color(0xFF0369A1),
                    topLeft = Offset(center.x - enemy.spec.radius * 1.1f, center.y - enemy.spec.radius * 0.75f),
                    size = Size(enemy.spec.radius * 2.2f, enemy.spec.radius * 1.5f),
                    style = Stroke(width = 2.5f)
                )
                // Swept-back electric fins
                drawLine(Color(0xFF38BDF8), Offset(center.x - 2f, center.y - 6f), Offset(center.x - 16f, center.y - 12f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(Color(0xFF38BDF8), Offset(center.x - 2f, center.y + 6f), Offset(center.x - 16f, center.y + 12f), strokeWidth = 3f, cap = StrokeCap.Round)
                // Sharp glowing electric eyes
                drawRoundRect(color = Color(0xFFFACC15), topLeft = Offset(center.x + 4f, center.y - 4f), size = Size(8f, 3.5f), cornerRadius = CornerRadius(1.5f, 1.5f))
                drawCircle(Color.White, radius = 1.5f, center = Offset(center.x + 9f, center.y - 2f))
            }
            EnemyType.BOSS -> {
                // Colossal Legendary War Titan
                val bossAura = (sin(time * 4f) * 4f) + enemy.spec.radius + 10f
                drawCircle(color = Color(0x44E11D48), radius = bossAura, center = center)
                drawCircle(
                    color = Color(0x88E11D48),
                    radius = bossAura,
                    center = center,
                    style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                )
                // Colossal dragon/mech armor
                drawCircle(color = Color(0xFF4C0519), radius = enemy.spec.radius, center = center)
                drawCircle(color = Color(0xFF881337), radius = enemy.spec.radius * 0.85f, center = center)
                drawCircle(color = Color(0xFFFFD166), radius = enemy.spec.radius, center = center, style = Stroke(width = 3.5f))
                // Grand 5-Spike Golden Crown on top
                for (s in -2..2) {
                    val spikeX = center.x + (s * 10f)
                    val spikeLen = if (s == 0) 18f else if (Math.abs(s) == 1) 14f else 10f
                    drawLine(Color(0xFFFFD166), Offset(spikeX, center.y - enemy.spec.radius + 4f), Offset(spikeX, center.y - enemy.spec.radius - spikeLen), strokeWidth = 4f, cap = StrokeCap.Round)
                    drawCircle(Color(0xFFE11D48), radius = 2.5f, center = Offset(spikeX, center.y - enemy.spec.radius - spikeLen))
                }
                // Pulsing central power core
                val corePulse = (sin(time * 6f) * 0.2f + 0.8f)
                drawCircle(color = Color(0xFFFF0055), radius = enemy.spec.radius * 0.35f * corePulse, center = center)
                drawCircle(color = Color(0xFFFDE047), radius = enemy.spec.radius * 0.20f * corePulse, center = center)
                drawCircle(color = Color.White, radius = enemy.spec.radius * 0.10f, center = center)
                // Glowing slit eyes under heavy brow
                drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(center.x - 14f, center.y - 14f), size = Size(28f, 8f), cornerRadius = CornerRadius(3f, 3f))
                drawRect(color = Color(0xFFEF4444), topLeft = Offset(center.x - 10f, center.y - 12f), size = Size(8f, 3.5f))
                drawRect(color = Color(0xFFEF4444), topLeft = Offset(center.x + 2f, center.y - 12f), size = Size(8f, 3.5f))
            }
        }

        // Hit Reaction Flash Overlay (White-yellow flash on damage)
        if (enemy.isHitFlashing) {
            drawCircle(color = Color(0xEEFFFFFF), radius = enemy.spec.radius * 1.05f, center = center)
            drawCircle(color = Color(0xFFFFE066), radius = enemy.spec.radius * 0.90f, center = center)
        }

        // Boss Shield Phase Aura
        if (enemy.spec.isBoss && enemy.isShielded) {
            val pulse = (sin(time * 8f) * 3f)
            drawCircle(
                color = Color(0x44F59E0B),
                radius = enemy.spec.radius + 12f + pulse,
                center = center
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = enemy.spec.radius + 10f + pulse,
                center = center,
                style = Stroke(width = 3.5f)
            )
        }

        // Health Bars (World-Space)
        if (enemy.spec.isBoss) {
            // World-Space Boss Health Bar directly above the boss
            val bossBarWidth = 54f
            val bossBarHeight = 6.5f
            val bossBarTop = center.y - enemy.spec.radius - 22f
            val bossBarLeft = center.x - bossBarWidth / 2f

            // Outer dark container
            drawRoundRect(
                color = Color(0xEE0F172A),
                topLeft = Offset(bossBarLeft - 1.5f, bossBarTop - 1.5f),
                size = Size(bossBarWidth + 3f, bossBarHeight + 3f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            // Golden boss frame
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(bossBarLeft - 1.5f, bossBarTop - 1.5f),
                size = Size(bossBarWidth + 3f, bossBarHeight + 3f),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = 1f)
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
            // Small golden skull / crown indicator dot above
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 2.5f,
                center = Offset(center.x, bossBarTop - 4f)
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

private fun DrawScope.drawProjectiles(projectiles: List<Projectile>) {
    for (p in projectiles) {
        if (p.isHit || p.isExpired) continue

        val center = Offset(p.currentPosition.x, p.currentPosition.y)

        when (p.type) {
            ProjectileType.BULLET -> {
                // Cartoon tracer bullet: golden glow, white core, motion streak
                drawCircle(color = Color(0xFFFFD166), radius = 4.5f, center = center)
                drawCircle(color = Color.White, radius = 2.5f, center = center)
            }
            ProjectileType.CANNONBALL -> {
                // Heavy cast iron cannonball with specular shine & shadow
                drawCircle(color = Color(0x50000000), radius = 7f, center = Offset(center.x + 3f, center.y + 4f))
                drawCircle(color = Color(0xFF1E293B), radius = 7f, center = center)
                drawCircle(color = Color(0xFF475569), radius = 3.5f, center = Offset(center.x - 2f, center.y - 2f))
                drawCircle(color = Color.White, radius = 1.5f, center = Offset(center.x - 2.5f, center.y - 2.5f))
            }
            ProjectileType.PLASMA_BOLT -> {
                // Pulsing electric energy bolt
                drawCircle(color = Color(0x558B5CF6), radius = 10f, center = center)
                drawCircle(color = Color(0xFFA855F7), radius = 6f, center = center)
                drawCircle(color = Color(0xFF22D3EE), radius = 3.5f, center = center)
                drawCircle(color = Color.White, radius = 2f, center = center)
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
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                // Outer expanding smoke cloud puffs
                drawCircle(
                    color = Color(0xFF64748B).copy(alpha = alpha * 0.6f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 6f * alpha)
                )
                // Fire core
                drawCircle(
                    color = Color(0xFFEA580C).copy(alpha = alpha),
                    radius = currentR * 0.70f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = alpha),
                    radius = currentR * 0.40f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.18f,
                    center = center
                )
                // Flying cartoon spark stars
                for (s in 0..3) {
                    val sAng = s * (Math.PI / 2.0).toFloat() + fx.progress * 2f
                    val sDist = currentR * 0.85f
                    val sx = center.x + cos(sAng) * sDist
                    val sy = center.y + sin(sAng) * sDist
                    drawCircle(color = Color(0xFFFFD166).copy(alpha = alpha), radius = 3f * alpha, center = Offset(sx, sy))
                }
            }
            EffectType.HIT_SPARK -> {
                val currentR = fx.maxRadius * fx.progress
                // 4-Point Cartoon Star Spark
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    start = Offset(center.x - currentR, center.y),
                    end = Offset(center.x + currentR, center.y),
                    strokeWidth = 3f * alpha,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    start = Offset(center.x, center.y - currentR),
                    end = Offset(center.x, center.y + currentR),
                    strokeWidth = 3f * alpha,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.4f,
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
