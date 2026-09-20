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
import com.example.entities.DestructibleObject
import com.example.entities.DestructibleType
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
    progressionManager: com.example.data.ProgressionManager? = null,
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
                .pointerInput(currentMap.id, maxWidthPx, maxHeightPx, gameState.isBuildingTower) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        var isDragging = false
                        val touchSlop = viewConfiguration.touchSlop
                        var totalDragDistance = 0f
                        val startPos = down.position
                        var prevPos = down.position
                        var prevDistance = -1f

                        if (gameState.isBuildingTower) {
                            val startWorldPt = cameraState.screenToWorld(
                                screenX = startPos.x,
                                screenY = startPos.y,
                                viewportWidth = maxWidthPx,
                                viewportHeight = maxHeightPx
                            )
                            onPlacementDrag?.invoke(startWorldPt.x, startWorldPt.y)
                        }

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

                                if (gameState.isBuildingTower) {
                                    val dragWorldPt = cameraState.screenToWorld(
                                        screenX = currentPos.x,
                                        screenY = currentPos.y,
                                        viewportWidth = maxWidthPx,
                                        viewportHeight = maxHeightPx
                                    )
                                    onPlacementDrag?.invoke(dragWorldPt.x, dragWorldPt.y)
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

                        // Gesture completed
                        if (gameState.isBuildingTower) {
                            val worldPt = cameraState.screenToWorld(
                                screenX = prevPos.x,
                                screenY = prevPos.y,
                                viewportWidth = maxWidthPx,
                                viewportHeight = maxHeightPx
                            )
                            if (worldPt.x in 0f..worldWidth && worldPt.y in 0f..worldHeight) {
                                onCanvasTap(worldPt.x, worldPt.y)
                            }
                        } else if (!isDragging && totalDragDistance < touchSlop) {
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
                EnvironmentType.SNOW_VALLEY -> Color(0xFFDCE5EE)
                EnvironmentType.NIGHT_FORTRESS -> Color(0xFF0F172A)
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

                // 4. Ground Decorations (Trees, Rocks, Bushes, Crystals, Snow Piles)
                drawDecorations(currentMap.decorations, currentMap.environmentType)

                // 4.5. Destructible Environment Objects (Natural trees, rocks, wooden crates)
                drawDestructibles(
                    destructibles = gameState.destructibles,
                    selectedDestructibleId = gameState.selectedDestructibleId,
                    towerTargetedIds = gameState.towers.mapNotNull { it.manualTargetId }.toSet(),
                    gameTime = gameState.gameTime,
                    env = currentMap.environmentType
                )

                // 5. Entrance Cave / Spawn Gate for each path
                for (sp in currentMap.spawnPoints) {
                    drawSpawnGate(sp, worldWidth, worldHeight, gameState.gameTime)
                }

                // 6. Tunnel entrances/exits if present
                if (gameState.currentMap.tunnelRegion != null) {
                    drawTunnelPortals(gameState.currentMap.tunnelRegion!!)
                }

                // 6.5. Weather Effects: Lightweight Falling Snow in background (behind towers and enemies)
                if (currentMap.environmentType == EnvironmentType.SNOW_VALLEY) {
                    drawFallingSnowWeatherEffect(worldWidth, worldHeight, gameState.gameTime)
                }
                if (currentMap.environmentType == EnvironmentType.NIGHT_FORTRESS) {
                    drawNightSkyAndAmbientEffects(worldWidth, worldHeight, gameState.gameTime)
                }

                // 7. Player Castle Fortress Base
                drawCastleBase(gameState.base, gameState.gameTime, currentMap.environmentType)

                // 8. Placed Defense Towers
                drawTowers(gameState.towers, gameState.selectedExistingTower, gameState.gameTime, currentMap.environmentType)

                // 9. Active Enemies with animations
                drawEnemies(gameState.enemies, gameState.gameTime, currentMap.environmentType)

                // 10. Tunnel Mountain Canopy (Drawn OVER enemies so enemies pass under it!)
                if (gameState.currentMap.tunnelRegion != null) {
                    drawTunnelCavernCanopy(gameState.currentMap.tunnelRegion!!, gameState.gameTime)
                }

                // 11. Ballistic Projectiles
                drawProjectiles(gameState.projectiles, currentMap.environmentType)

                // 12. Combat Particles & Explosions
                drawVisualEffects(gameState.effects, currentMap.environmentType)

                // 12.5. Floating bioluminescent fireflies for Night Fortress
                if (currentMap.environmentType == EnvironmentType.NIGHT_FORTRESS) {
                    drawNightFirefliesAndGlow(worldWidth, worldHeight, gameState.gameTime)
                }

                // 13. Tower Placement Preview (Legacy dragging)
                if (gameState.isBuildingTower) {
                    drawPlacementPreview(gameState)
                }

                // 14. Level 1 Tutorial Beacon (Glowing beacon on dynamically validated build plot)
                val tutorialPlot = gameState.tutorialRecommendedPlot
                if (tutorialPlot != null && gameState.currentMap.id == "green_valley" && gameState.towers.isEmpty() && gameState.selectedBuildPos == null) {
                    val plotPos = tutorialPlot
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
                progressionManager = progressionManager,
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
        EnvironmentType.SNOW_VALLEY -> {
            // Crisp snow base with cool glacial undertones
            drawRect(color = Color(0xFFE2E8F0), size = arenaSize)

            // Layered rolling snowdrifts and glacial dunes
            drawHillLayer(Color(0xFFCBD5E1), 180f, 360f, map.worldWidth)
            drawHillLayer(Color(0xFFF1F5F9), 520f, 720f, map.worldWidth)
            drawHillLayer(Color(0xFFCBD5E1), 900f, 1100f, map.worldWidth)
            drawHillLayer(Color(0xFFF8FAFC), 1280f, 1480f, map.worldWidth)

            // Distinctive snow terrain details: ice patches, frozen crevices, snowdrifts, and sparkles
            drawSnowTerrainDetails(map.worldWidth, map.worldHeight)
        }
        EnvironmentType.NIGHT_FORTRESS -> {
            // Deep twilight night ground with midnight blue/indigo tones
            drawRect(color = Color(0xFF0A1128), size = arenaSize)

            // Night ridges / rolling dark terrain contours
            drawHillLayer(Color(0xFF0E1A38), 180f, 360f, map.worldWidth)
            drawHillLayer(Color(0xFF14244B), 520f, 720f, map.worldWidth)
            drawHillLayer(Color(0xFF0E1A38), 900f, 1100f, map.worldWidth)
            drawHillLayer(Color(0xFF182B56), 1280f, 1480f, map.worldWidth)

            // Moonlit ground clearings, dark moss, and glowing flora
            drawNightTerrainDetails(map.worldWidth, map.worldHeight)
        }
    }
}

private val SNOWFLAKE_COUNT = 90
private val SNOWFLAKE_SEEDS = Array(SNOWFLAKE_COUNT) { i ->
    val rand = kotlin.random.Random(i * 1337 + 42)
    floatArrayOf(
        rand.nextFloat(), // x seed
        rand.nextFloat(), // y seed
        55f + rand.nextFloat() * 60f, // fall speed px/sec
        0.8f + rand.nextFloat() * 1.5f, // drift speed
        1.5f + rand.nextFloat() * 2.2f, // radius
        0.30f + rand.nextFloat() * 0.40f // alpha
    )
}

private fun DrawScope.drawFallingSnowWeatherEffect(worldWidth: Float, worldHeight: Float, gameTime: Float) {
    for (seed in SNOWFLAKE_SEEDS) {
        val xBase = seed[0] * worldWidth
        val yBase = seed[1] * worldHeight
        val fallSpeed = seed[2]
        val driftSpeed = seed[3]
        val radius = seed[4]
        val alpha = seed[5]

        val y = (yBase + gameTime * fallSpeed) % worldHeight
        val drift = kotlin.math.sin(gameTime * driftSpeed + seed[0] * 10f) * 22f
        val x = (xBase + drift + gameTime * 14f) % worldWidth

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSnowTerrainDetails(worldWidth: Float, worldHeight: Float) {
    // 1. Frozen ice sheets / frozen hollow patches
    val icePatches = listOf(
        Point2D(worldWidth * 0.22f, worldHeight * 0.35f) to Size(140f, 75f),
        Point2D(worldWidth * 0.72f, worldHeight * 0.28f) to Size(160f, 85f),
        Point2D(worldWidth * 0.38f, worldHeight * 0.72f) to Size(180f, 95f),
        Point2D(worldWidth * 0.82f, worldHeight * 0.65f) to Size(150f, 80f)
    )

    for ((pos, sz) in icePatches) {
        val left = pos.x - sz.width / 2f
        val top = pos.y - sz.height / 2f
        // Frost shore
        drawRoundRect(
            color = Color(0x66CBD5E1),
            topLeft = Offset(left - 8f, top - 6f),
            size = Size(sz.width + 16f, sz.height + 12f),
            cornerRadius = CornerRadius(sz.height / 2f, sz.height / 2f)
        )
        // Translucent glassy ice
        drawRoundRect(
            color = Color(0x8893C5FD),
            topLeft = Offset(left, top),
            size = sz,
            cornerRadius = CornerRadius(sz.height / 2f, sz.height / 2f)
        )
        drawRoundRect(
            color = Color(0xAA60A5FA),
            topLeft = Offset(left + 8f, top + 6f),
            size = Size(sz.width - 16f, sz.height - 12f),
            cornerRadius = CornerRadius((sz.height - 12f) / 2f, (sz.height - 12f) / 2f)
        )
        // Ice fracture crack lines
        drawLine(
            color = Color(0xCCFFFFFF),
            start = Offset(left + sz.width * 0.25f, top + sz.height * 0.3f),
            end = Offset(left + sz.width * 0.55f, top + sz.height * 0.55f),
            strokeWidth = 1.8f
        )
        drawLine(
            color = Color(0xCCFFFFFF),
            start = Offset(left + sz.width * 0.55f, top + sz.height * 0.55f),
            end = Offset(left + sz.width * 0.8f, top + sz.height * 0.45f),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xCCFFFFFF),
            start = Offset(left + sz.width * 0.55f, top + sz.height * 0.55f),
            end = Offset(left + sz.width * 0.6f, top + sz.height * 0.85f),
            strokeWidth = 1.2f
        )
        // Gloss glare sheen
        drawOval(
            color = Color(0x66FFFFFF),
            topLeft = Offset(left + sz.width * 0.15f, top + sz.height * 0.18f),
            size = Size(sz.width * 0.45f, sz.height * 0.28f)
        )
    }

    // 2. Curving snowdrift ridges across the valley
    val driftLines = listOf(
        Triple(worldWidth * 0.12f, worldHeight * 0.22f, 160f),
        Triple(worldWidth * 0.45f, worldHeight * 0.18f, 220f),
        Triple(worldWidth * 0.82f, worldHeight * 0.20f, 180f),
        Triple(worldWidth * 0.25f, worldHeight * 0.55f, 200f),
        Triple(worldWidth * 0.65f, worldHeight * 0.52f, 240f),
        Triple(worldWidth * 0.18f, worldHeight * 0.85f, 190f),
        Triple(worldWidth * 0.52f, worldHeight * 0.88f, 210f),
        Triple(worldWidth * 0.85f, worldHeight * 0.82f, 170f)
    )
    for ((dx, dy, len) in driftLines) {
        // Shadow side of snowdrift
        drawLine(
            color = Color(0x4494A3B8),
            start = Offset(dx - len / 2f, dy),
            end = Offset(dx + len / 2f, dy + 12f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        // Crest of pristine white snow
        drawLine(
            color = Color(0xBBFFFFFF),
            start = Offset(dx - len / 2f, dy - 2f),
            end = Offset(dx + len / 2f, dy + 10f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }

    // 3. Crystalline snow sparkles
    val sparkles = listOf(
        Point2D(worldWidth * 0.15f, worldHeight * 0.16f),
        Point2D(worldWidth * 0.35f, worldHeight * 0.28f),
        Point2D(worldWidth * 0.58f, worldHeight * 0.14f),
        Point2D(worldWidth * 0.78f, worldHeight * 0.38f),
        Point2D(worldWidth * 0.28f, worldHeight * 0.62f),
        Point2D(worldWidth * 0.48f, worldHeight * 0.48f),
        Point2D(worldWidth * 0.70f, worldHeight * 0.78f),
        Point2D(worldWidth * 0.88f, worldHeight * 0.52f),
        Point2D(worldWidth * 0.32f, worldHeight * 0.90f),
        Point2D(worldWidth * 0.62f, worldHeight * 0.94f)
    )
    for (sp in sparkles) {
        val spSize = 5f
        // 4-pointed sparkle star
        drawLine(color = Color(0xCCFFFFFF), start = Offset(sp.x - spSize, sp.y), end = Offset(sp.x + spSize, sp.y), strokeWidth = 1.5f)
        drawLine(color = Color(0xCCFFFFFF), start = Offset(sp.x, sp.y - spSize), end = Offset(sp.x, sp.y + spSize), strokeWidth = 1.5f)
        drawCircle(color = Color(0xEEBAE6FD), radius = 1.8f, center = Offset(sp.x, sp.y))
    }
}

private val NIGHT_STAR_COUNT = 75
private val NIGHT_STAR_SEEDS = Array(NIGHT_STAR_COUNT) { i ->
    val rand = kotlin.random.Random(i * 2048 + 99)
    floatArrayOf(
        rand.nextFloat(), // x seed
        rand.nextFloat(), // y seed
        1.0f + rand.nextFloat() * 2.2f, // base radius
        1.5f + rand.nextFloat() * 3.5f, // twinkle speed
        rand.nextFloat() * 6.28f, // phase offset
        if (rand.nextBoolean()) 1f else 0f // 1 = pale gold, 0 = cool cyan/white
    )
}

private val FIREFLY_COUNT = 36
private val FIREFLY_SEEDS = Array(FIREFLY_COUNT) { i ->
    val rand = kotlin.random.Random(i * 4096 + 333)
    floatArrayOf(
        rand.nextFloat(), // x seed
        rand.nextFloat(), // y seed
        0.5f + rand.nextFloat() * 1.5f, // speed x
        0.4f + rand.nextFloat() * 1.2f, // speed y
        1.8f + rand.nextFloat() * 3.0f, // pulse speed
        rand.nextFloat() * 6.28f, // pulse phase
        rand.nextFloat() // color tint
    )
}

private fun DrawScope.drawNightTerrainDetails(worldWidth: Float, worldHeight: Float) {
    // 1. Soft moonlit clearings (diffuse ambient moonlight pools on the grass)
    val moonlitClearings = listOf(
        Point2D(worldWidth * 0.18f, worldHeight * 0.22f) to Size(220f, 130f),
        Point2D(worldWidth * 0.44f, worldHeight * 0.32f) to Size(260f, 150f),
        Point2D(worldWidth * 0.72f, worldHeight * 0.24f) to Size(240f, 140f),
        Point2D(worldWidth * 0.30f, worldHeight * 0.65f) to Size(280f, 160f),
        Point2D(worldWidth * 0.58f, worldHeight * 0.68f) to Size(300f, 170f),
        Point2D(worldWidth * 0.82f, worldHeight * 0.72f) to Size(250f, 140f),
        Point2D(worldWidth * 0.20f, worldHeight * 0.86f) to Size(220f, 130f),
        Point2D(worldWidth * 0.50f, worldHeight * 0.90f) to Size(270f, 150f)
    )

    for ((pos, size) in moonlitClearings) {
        // Outer soft glow
        drawOval(
            color = Color(0x181E3A5F),
            topLeft = Offset(pos.x - size.width * 0.5f, pos.y - size.height * 0.5f),
            size = size
        )
        // Inner moonlit sheen
        drawOval(
            color = Color(0x1F2A4A7F),
            topLeft = Offset(pos.x - size.width * 0.35f, pos.y - size.height * 0.35f),
            size = Size(size.width * 0.7f, size.height * 0.7f)
        )
    }

    // 2. Dark forest moss clusters
    val mossClusters = listOf(
        Point2D(worldWidth * 0.12f, worldHeight * 0.42f),
        Point2D(worldWidth * 0.38f, worldHeight * 0.18f),
        Point2D(worldWidth * 0.64f, worldHeight * 0.45f),
        Point2D(worldWidth * 0.85f, worldHeight * 0.35f),
        Point2D(worldWidth * 0.75f, worldHeight * 0.88f),
        Point2D(worldWidth * 0.14f, worldHeight * 0.75f)
    )
    for (m in mossClusters) {
        drawOval(
            color = Color(0xFF071224),
            topLeft = Offset(m.x - 30f, m.y - 18f),
            size = Size(60f, 36f)
        )
        drawOval(
            color = Color(0xFF0C1B33),
            topLeft = Offset(m.x - 22f, m.y - 12f),
            size = Size(44f, 24f)
        )
    }

    // 3. Bioluminescent night ground flora tufts (spores and glowing micro-plants)
    val glowingTufts = listOf(
        Point2D(worldWidth * 0.16f, worldHeight * 0.28f),
        Point2D(worldWidth * 0.25f, worldHeight * 0.48f),
        Point2D(worldWidth * 0.42f, worldHeight * 0.58f),
        Point2D(worldWidth * 0.55f, worldHeight * 0.22f),
        Point2D(worldWidth * 0.68f, worldHeight * 0.52f),
        Point2D(worldWidth * 0.78f, worldHeight * 0.64f),
        Point2D(worldWidth * 0.88f, worldHeight * 0.82f),
        Point2D(worldWidth * 0.34f, worldHeight * 0.82f),
        Point2D(worldWidth * 0.62f, worldHeight * 0.84f)
    )
    for (gt in glowingTufts) {
        // Soft aura
        drawCircle(color = Color(0x332DD4BF), radius = 10f, center = Offset(gt.x, gt.y))
        // Spore points
        drawCircle(color = Color(0xFF2DD4BF), radius = 2.2f, center = Offset(gt.x - 4f, gt.y - 3f))
        drawCircle(color = Color(0xFF38BDF8), radius = 1.8f, center = Offset(gt.x + 3f, gt.y - 2f))
        drawCircle(color = Color(0xFFFEF08A), radius = 1.5f, center = Offset(gt.x, gt.y + 3f))
        drawCircle(color = Color.White, radius = 0.9f, center = Offset(gt.x - 4f, gt.y - 3f))
    }
}

private fun DrawScope.drawNightSkyAndAmbientEffects(worldWidth: Float, worldHeight: Float, gameTime: Float) {
    // 1. Distant twinkling stars across the night canopy
    for (seed in NIGHT_STAR_SEEDS) {
        val sx = seed[0] * worldWidth
        val sy = seed[1] * worldHeight
        val baseR = seed[2]
        val twinkleSpeed = seed[3]
        val phase = seed[4]
        val isGold = seed[5] > 0.5f

        val twinkle = (sin(gameTime * twinkleSpeed + phase) * 0.5f + 0.5f).coerceIn(0.2f, 1f)
        val r = baseR * (0.6f + 0.4f * twinkle)

        val starColor = if (isGold) {
            Color(0xFFFEF08A).copy(alpha = 0.45f + 0.50f * twinkle)
        } else {
            Color(0xFFBAE6FD).copy(alpha = 0.45f + 0.50f * twinkle)
        }

        drawCircle(color = starColor, radius = r, center = Offset(sx, sy))

        // Tiny cross-glint on brightest stars
        if (twinkle > 0.82f && baseR > 2.0f) {
            val glintLen = r * 2.2f
            drawLine(
                color = Color.White.copy(alpha = 0.6f * twinkle),
                start = Offset(sx - glintLen, sy),
                end = Offset(sx + glintLen, sy),
                strokeWidth = 1.0f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.6f * twinkle),
                start = Offset(sx, sy - glintLen),
                end = Offset(sx, sy + glintLen),
                strokeWidth = 1.0f
            )
        }
    }

    // 2. Distant Crescent Moon in the upper sky background
    val moonX = worldWidth * 0.88f
    val moonY = 120f
    val moonR = 30f

    // Soft moon aura
    drawCircle(color = Color(0x18FEF08A), radius = moonR * 3.5f, center = Offset(moonX, moonY))
    drawCircle(color = Color(0x33BAE6FD), radius = moonR * 2.0f, center = Offset(moonX, moonY))
    drawCircle(color = Color(0x55FEF08A), radius = moonR * 1.3f, center = Offset(moonX, moonY))

    // Bright golden-silver moon body
    drawCircle(color = Color(0xFFFEF08A), radius = moonR, center = Offset(moonX, moonY))
    drawCircle(color = Color(0xFFFFFBEB), radius = moonR * 0.85f, center = Offset(moonX, moonY))

    // Shadow cutout creating an elegant crescent shape
    drawCircle(color = Color(0xFF0A1128), radius = moonR * 0.88f, center = Offset(moonX - moonR * 0.42f, moonY - moonR * 0.20f))

    // 3. Subtle drifting nocturnal ground mist / fog bands (lightweight, zero performance impact)
    val fogPatches = listOf(
        Triple(0.20f, 0.35f, 180f),
        Triple(0.65f, 0.25f, 220f),
        Triple(0.35f, 0.70f, 240f),
        Triple(0.80f, 0.80f, 200f)
    )
    for ((fxRatio, fyRatio, fogW) in fogPatches) {
        val driftX = fxRatio * worldWidth + sin(gameTime * 0.25f + fxRatio * 10f) * 45f
        val driftY = fyRatio * worldHeight + cos(gameTime * 0.18f + fyRatio * 10f) * 20f
        val fogH = fogW * 0.45f
        drawOval(
            color = Color(0x0CBAE6FD),
            topLeft = Offset(driftX - fogW / 2f, driftY - fogH / 2f),
            size = Size(fogW, fogH)
        )
        drawOval(
            color = Color(0x0838BDF8),
            topLeft = Offset(driftX - fogW * 0.35f, driftY - fogH * 0.35f),
            size = Size(fogW * 0.7f, fogH * 0.7f)
        )
    }
}

private fun DrawScope.drawNightFirefliesAndGlow(worldWidth: Float, worldHeight: Float, gameTime: Float) {
    // Animated drifting fireflies across the battlefield
    for (seed in FIREFLY_SEEDS) {
        val xBase = seed[0] * worldWidth
        val yBase = seed[1] * worldHeight
        val speedX = seed[2]
        val speedY = seed[3]
        val pulseSpeed = seed[4]
        val phase = seed[5]
        val tint = seed[6]

        // Smooth harmonic drifting
        val x = (xBase + sin(gameTime * speedX + phase) * 70f + gameTime * 6f) % worldWidth
        val y = (yBase + cos(gameTime * speedY + phase * 1.3f) * 50f) % worldHeight

        val pulse = (sin(gameTime * pulseSpeed + phase) * 0.5f + 0.5f).coerceIn(0f, 1f)
        if (pulse < 0.08f) continue // Inactive during dark phase

        val haloColor = if (tint > 0.5f) Color(0x55FDE047) else Color(0x55A3E635)
        val coreColor = if (tint > 0.5f) Color(0xFFFEF08A) else Color(0xFFBEF264)

        // Outer soft glow halo
        drawCircle(
            color = haloColor.copy(alpha = haloColor.alpha * pulse),
            radius = 7.5f + pulse * 4f,
            center = Offset(x, y)
        )
        // Bright radiant firefly core
        drawCircle(
            color = coreColor.copy(alpha = pulse),
            radius = 2.2f + pulse * 0.8f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = pulse),
            radius = 1.0f,
            center = Offset(x, y)
        )
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

    if (map.environmentType == EnvironmentType.SNOW_VALLEY) {
        // Frozen Glacial Lake with crystalline ice and frost fractures
        // Snow bank shoreline
        drawCircle(color = Color(0xFFE2E8F0), radius = r + 14f, center = pondOffset)
        drawCircle(color = Color(0xFFCBD5E1), radius = r + 8f, center = pondOffset)

        // Glacial blue frozen ice body
        drawCircle(color = Color(0xFF38BDF8), radius = r, center = pondOffset)
        drawCircle(color = Color(0xFF7DD3FC), radius = r * 0.85f, center = pondOffset)
        drawCircle(color = Color(0xFFBAE6FD), radius = r * 0.65f, center = pondOffset)

        // Radial and zigzag ice fracture cracks
        for (i in 0..5) {
            val ang = i * 1.047f
            val startX = pondOffset.x + kotlin.math.cos(ang) * (r * 0.12f)
            val startY = pondOffset.y + kotlin.math.sin(ang) * (r * 0.12f)
            val midX = pondOffset.x + kotlin.math.cos(ang + 0.22f) * (r * 0.52f)
            val midY = pondOffset.y + kotlin.math.sin(ang + 0.22f) * (r * 0.52f)
            val endX = pondOffset.x + kotlin.math.cos(ang - 0.15f) * (r * 0.88f)
            val endY = pondOffset.y + kotlin.math.sin(ang - 0.15f) * (r * 0.88f)
            drawLine(color = Color(0xDDFFFFFF), start = Offset(startX, startY), end = Offset(midX, midY), strokeWidth = 2.2f)
            drawLine(color = Color(0xDDFFFFFF), start = Offset(midX, midY), end = Offset(endX, endY), strokeWidth = 1.6f)
        }

        // Glacial surface frost glare sheen
        drawOval(
            color = Color(0x55FFFFFF),
            topLeft = Offset(pondOffset.x - r * 0.6f, pondOffset.y - r * 0.4f),
            size = Size(r * 1.2f, r * 0.55f)
        )
        return
    }

    if (map.environmentType == EnvironmentType.NIGHT_FORTRESS) {
        // Mystic Moonlit Reflecting Pool
        // Dark slate and mossy bank shoreline
        drawCircle(color = Color(0xFF0F172A), radius = r + 14f, center = pondOffset)
        drawCircle(color = Color(0xFF1E293B), radius = r + 8f, center = pondOffset)

        // Deep midnight water body with dark sapphire indigo depth
        drawCircle(color = Color(0xFF021024), radius = r, center = pondOffset)
        drawCircle(color = Color(0xFF062044), radius = r * 0.85f, center = pondOffset)
        drawCircle(color = Color(0xFF0C356A), radius = r * 0.6f, center = pondOffset)

        // Shimmering moon reflection oval with gentle wave distortion
        val waveWobble = sin(time * 3f) * 4f
        drawOval(
            color = Color(0x55BAE6FD),
            topLeft = Offset(pondOffset.x - r * 0.35f + waveWobble, pondOffset.y - r * 0.25f),
            size = Size(r * 0.7f, r * 0.45f)
        )
        drawOval(
            color = Color(0x77FEF08A),
            topLeft = Offset(pondOffset.x - r * 0.20f + waveWobble * 0.6f, pondOffset.y - r * 0.15f),
            size = Size(r * 0.4f, r * 0.28f)
        )

        // Gentle harmonic ripple rings
        val rippleProgress = (time * 0.4f) % 1f
        drawCircle(
            color = Color(0x4438BDF8).copy(alpha = (1f - rippleProgress) * 0.45f),
            radius = r * (0.25f + rippleProgress * 0.65f),
            center = pondOffset,
            style = Stroke(width = 2.0f)
        )

        // Bioluminescent water lotus lilies
        drawCircle(color = Color(0xFF064E3B), radius = 11f, center = Offset(center.x - 24f, center.y - 14f))
        drawCircle(color = Color(0xFF059669), radius = 8f, center = Offset(center.x - 24f, center.y - 14f))
        drawCircle(color = Color(0xFF38BDF8), radius = 4f, center = Offset(center.x - 24f, center.y - 14f))
        drawCircle(color = Color.White, radius = 1.5f, center = Offset(center.x - 24f, center.y - 14f))

        drawCircle(color = Color(0xFF064E3B), radius = 9f, center = Offset(center.x + 22f, center.y + 12f))
        drawCircle(color = Color(0xFF059669), radius = 6.5f, center = Offset(center.x + 22f, center.y + 12f))
        drawCircle(color = Color(0xFFA855F7), radius = 3.5f, center = Offset(center.x + 22f, center.y + 12f))
        drawCircle(color = Color.White, radius = 1.2f, center = Offset(center.x + 22f, center.y + 12f))
        return
    }

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
    val (shadowColor, shoulderColor, roadColor, wagonColor, pebbleColor) = when (env) {
        EnvironmentType.GREEN_VALLEY -> listOf(Color(0xFF38230F), Color(0xFF8B5E34), Color(0xFFBC8A5F), Color(0x446F4E37), Color(0xFFD4A373))
        EnvironmentType.DESERT_CANYON -> listOf(Color(0xFF4A2810), Color(0xFF9C5221), Color(0xFFD68947), Color(0x44783815), Color(0xFFE8B27A))
        EnvironmentType.FOREST_CROSSROADS -> listOf(Color(0xFF1E1308), Color(0xFF4D3319), Color(0xFF7A542C), Color(0x4436220E), Color(0xFFA67C52))
        EnvironmentType.OBSIDIAN_TUNNEL -> listOf(Color(0xFF020617), Color(0xFF1E293B), Color(0xFF334155), Color(0x4464748B), Color(0xFF475569))
        EnvironmentType.DRAGON_COIL -> listOf(Color(0xFF141210), Color(0xFF3E3A36), Color(0xFF6E6760), Color(0x449E978E), Color(0xFF9E978E))
        EnvironmentType.SNOW_VALLEY -> listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF475569), Color(0x88BAE6FD), Color(0xFFE2E8F0))
        EnvironmentType.NIGHT_FORTRESS -> listOf(Color(0xFF070C16), Color(0xFF1E293B), Color(0xFF334155), Color(0x6638BDF8), Color(0xFF64748B))
    }

    // 1. Natural road trench / ditch shadow
    drawPath(
        path = composePath,
        color = shadowColor,
        style = Stroke(
            width = path.pathWidth + 18f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 2. Earth shoulder & edge grading
    drawPath(
        path = composePath,
        color = shoulderColor,
        style = Stroke(
            width = path.pathWidth + 8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 3. Main dirt/cobblestone path surface
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
            width = 8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f))
        )
    )

    // 5. Scattered cobblestones along the trail waypoints
    for (i in 0 until path.waypoints.size - 1) {
        val wp1 = path.waypoints[i]
        val wp2 = path.waypoints[i + 1]
        val midX = (wp1.x + wp2.x) / 2f
        val midY = (wp1.y + wp2.y) / 2f
        drawCircle(color = pebbleColor.copy(alpha = 0.5f), radius = 3.5f, center = Offset(midX + 7f, midY - 5f))
        drawCircle(color = shadowColor.copy(alpha = 0.4f), radius = 2.5f, center = Offset(midX - 8f, midY + 6f))
    }

    // 6. Night Fortress glowing markers and moonlit borders
    if (env == EnvironmentType.NIGHT_FORTRESS) {
        // Moonlit path edge highlights
        drawPath(
            path = composePath,
            color = Color(0x3338BDF8),
            style = Stroke(
                width = path.pathWidth + 3f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Subtle glowing cyan runic stone markers along each waypoint
        for (i in 0 until path.waypoints.size) {
            val wp = path.waypoints[i]
            drawCircle(color = Color(0x3338BDF8), radius = 7f, center = Offset(wp.x, wp.y))
            drawCircle(color = Color(0xFF38BDF8), radius = 2.5f, center = Offset(wp.x, wp.y))
            drawCircle(color = Color.White, radius = 1.0f, center = Offset(wp.x, wp.y))
        }
    }
}

private fun DrawScope.drawDecorations(decorations: List<MapDecoration>, env: EnvironmentType = EnvironmentType.GREEN_VALLEY) {
    for (d in decorations) {
        val center = Offset(d.position.x, d.position.y)

        // Ground drop shadow
        drawOval(
            color = Color(0x44000000),
            topLeft = Offset(center.x - d.size * 0.55f + 4f, center.y + d.size * 0.15f),
            size = Size(d.size * 1.1f, d.size * 0.6f)
        )

        when (d.type) {
            DecorationType.OAK_TREE -> {
                // Textured wooden trunk with root flare
                drawRoundRect(
                    color = Color(0xFF451A03),
                    topLeft = Offset(center.x - 7f, center.y - 4f),
                    size = Size(14f, d.size * 0.55f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                drawRoundRect(
                    color = Color(0xFF78350F),
                    topLeft = Offset(center.x - 5f, center.y - 2f),
                    size = Size(10f, d.size * 0.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                // Layered organic canopy puffs (layered greens with sunlight highlights)
                drawCircle(color = Color(0xFF14532D), radius = d.size * 0.48f, center = Offset(center.x, center.y - 4f))
                drawCircle(color = Color(0xFF166534), radius = d.size * 0.44f, center = Offset(center.x - 3f, center.y - 8f))
                drawCircle(color = Color(0xFF15803D), radius = d.size * 0.36f, center = Offset(center.x + 4f, center.y - 12f))
                drawCircle(color = Color(0xFF22C55E), radius = d.size * 0.22f, center = Offset(center.x - 6f, center.y - 16f))
                if (env == EnvironmentType.SNOW_VALLEY) {
                    drawCircle(color = Color(0xFFF1F5F9), radius = d.size * 0.25f, center = Offset(center.x - 3f, center.y - 12f))
                    drawCircle(color = Color(0xFFCBD5E1), radius = d.size * 0.18f, center = Offset(center.x + 4f, center.y - 14f))
                } else if (env == EnvironmentType.NIGHT_FORTRESS) {
                    // Moonlit edge highlight on canopy
                    drawCircle(color = Color(0x66BAE6FD), radius = d.size * 0.28f, center = Offset(center.x - 4f, center.y - 14f))
                }
            }
            DecorationType.PINE_TREE -> {
                // Trunk
                drawRoundRect(
                    color = Color(0xFF451A03),
                    topLeft = Offset(center.x - 5f, center.y + 10f),
                    size = Size(10f, 18f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                // Triangular pine tiers with highlights
                drawPineTier(center.x, center.y + 14f, d.size * 0.44f, Color(0xFF064E3B))
                drawPineTier(center.x, center.y - 2f, d.size * 0.36f, Color(0xFF047857))
                drawPineTier(center.x, center.y - 18f, d.size * 0.26f, Color(0xFF059669))
                if (env == EnvironmentType.SNOW_VALLEY) {
                    drawPineSnowTier(center.x, center.y + 11f, d.size * 0.40f)
                    drawPineSnowTier(center.x, center.y - 4f, d.size * 0.32f)
                    drawPineSnowTier(center.x, center.y - 20f, d.size * 0.22f)
                } else if (env == EnvironmentType.NIGHT_FORTRESS) {
                    // Moonlit crests on pine tiers
                    drawCircle(color = Color(0x55BAE6FD), radius = d.size * 0.18f, center = Offset(center.x - 2f, center.y - 20f))
                }
            }
            DecorationType.BOULDER -> {
                // Rock shadow base
                drawRoundRect(
                    color = if (env == EnvironmentType.SNOW_VALLEY) Color(0xFF1E293B) else Color(0xFF334155),
                    topLeft = Offset(center.x - d.size * 0.5f, center.y - d.size * 0.35f),
                    size = Size(d.size, d.size * 0.7f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Rock main face
                drawRoundRect(
                    color = if (env == EnvironmentType.SNOW_VALLEY) Color(0xFF475569) else Color(0xFF475569),
                    topLeft = Offset(center.x - d.size * 0.45f, center.y - d.size * 0.30f),
                    size = Size(d.size * 0.9f, d.size * 0.6f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Rock specular highlight
                drawRoundRect(
                    color = if (env == EnvironmentType.SNOW_VALLEY) Color(0xFF94A3B8) else Color(0xFF94A3B8),
                    topLeft = Offset(center.x - d.size * 0.35f, center.y - d.size * 0.25f),
                    size = Size(d.size * 0.45f, d.size * 0.3f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                if (env == EnvironmentType.SNOW_VALLEY) {
                    // Snow cap on top of boulder
                    val capW = d.size * 0.85f
                    val capH = d.size * 0.32f
                    drawRoundRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(center.x - capW / 2f, center.y - d.size * 0.38f),
                        size = Size(capW, capH),
                        cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(center.x - capW * 0.4f, center.y - d.size * 0.42f),
                        size = Size(capW * 0.8f, capH * 0.75f),
                        cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                    )
                }
            }
            DecorationType.BUSH -> {
                drawCircle(color = Color(0xFF14532D), radius = d.size * 0.5f, center = center)
                drawCircle(color = Color(0xFF16A34A), radius = d.size * 0.4f, center = Offset(center.x - 3f, center.y - 3f))
                drawCircle(color = Color(0xFF4ADE80), radius = d.size * 0.2f, center = Offset(center.x - 5f, center.y - 6f))
                // Red berries
                drawCircle(color = Color(0xFFEF4444), radius = 2.5f, center = Offset(center.x - 7f, center.y - 4f))
                drawCircle(color = Color(0xFFEF4444), radius = 2.5f, center = Offset(center.x + 6f, center.y + 2f))
                drawCircle(color = Color(0xFFEF4444), radius = 2f, center = Offset(center.x + 1f, center.y - 8f))
            }
            DecorationType.SNOW_PILE -> {
                val pw = d.size * 1.25f
                val ph = d.size * 0.75f
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(center.x - pw / 2f, center.y - ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(ph / 2f, ph / 2f)
                )
                drawRoundRect(
                    color = Color(0xFFF1F5F9),
                    topLeft = Offset(center.x - pw * 0.45f, center.y - ph * 0.55f),
                    size = Size(pw * 0.9f, ph * 0.85f),
                    cornerRadius = CornerRadius(ph / 2f, ph / 2f)
                )
                drawOval(
                    color = Color.White,
                    topLeft = Offset(center.x - pw * 0.25f, center.y - ph * 0.45f),
                    size = Size(pw * 0.5f, ph * 0.35f)
                )
            }
            DecorationType.FROZEN_BUSH -> {
                drawCircle(color = Color(0xFF334155), radius = d.size * 0.5f, center = center)
                drawCircle(color = Color(0xFF64748B), radius = d.size * 0.4f, center = Offset(center.x - 2f, center.y - 2f))
                drawCircle(color = Color(0xFF93C5FD), radius = d.size * 0.28f, center = Offset(center.x - 4f, center.y - 4f))
                drawCircle(color = Color(0xEEFFFFFF), radius = d.size * 0.16f, center = Offset(center.x - 5f, center.y - 6f))
                // Frosted ice berries
                drawCircle(color = Color(0xFF38BDF8), radius = 2.8f, center = Offset(center.x - 6f, center.y - 3f))
                drawCircle(color = Color(0xFF60A5FA), radius = 2.8f, center = Offset(center.x + 5f, center.y + 2f))
                drawCircle(color = Color(0xFFBAE6FD), radius = 2.2f, center = Offset(center.x + 1f, center.y - 7f))
                drawCircle(color = Color.White, radius = 1.0f, center = Offset(center.x - 6.5f, center.y - 3.5f))
            }
            DecorationType.FLOWER_PATCH -> {
                drawCircle(color = Color(0x6616A34A), radius = d.size * 0.5f, center = center)
                val flowerColors = listOf(Color(0xFFFFD166), Color(0xFFF43F5E), Color(0xFFFFFFFF), Color(0xFFA855F7))
                for (i in 0..4) {
                    val angle = i * 1.25f
                    val fx = center.x + cos(angle) * (d.size * 0.32f)
                    val fy = center.y + sin(angle) * (d.size * 0.32f)
                    drawCircle(color = flowerColors[i % flowerColors.size], radius = 3.5f, center = Offset(fx, fy))
                    drawCircle(color = Color(0xFFFFD166), radius = 1.2f, center = Offset(fx, fy))
                }
            }
            DecorationType.GLOW_MUSHROOM -> {
                // Bioluminescent ground glow aura
                drawCircle(color = Color(0x3306B6D4), radius = d.size * 0.9f, center = center)
                drawCircle(color = Color(0x2238BDF8), radius = d.size * 1.4f, center = center)

                // Mushroom Stipe (Stem)
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(center.x - 3.5f, center.y - 2f),
                    size = Size(7f, d.size * 0.55f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Main Bioluminescent Cap
                drawRoundRect(
                    color = Color(0xFF0891B2),
                    topLeft = Offset(center.x - d.size * 0.5f, center.y - d.size * 0.45f),
                    size = Size(d.size, d.size * 0.55f),
                    cornerRadius = CornerRadius(d.size * 0.28f, d.size * 0.28f)
                )
                // Radiant Inner Dome Highlight
                drawRoundRect(
                    color = Color(0xFF22D3EE),
                    topLeft = Offset(center.x - d.size * 0.38f, center.y - d.size * 0.42f),
                    size = Size(d.size * 0.76f, d.size * 0.38f),
                    cornerRadius = CornerRadius(d.size * 0.22f, d.size * 0.22f)
                )
                drawOval(
                    color = Color(0xFFE0F2FE),
                    topLeft = Offset(center.x - d.size * 0.22f, center.y - d.size * 0.38f),
                    size = Size(d.size * 0.44f, d.size * 0.20f)
                )

                // Glowing Spores
                drawCircle(color = Color.White, radius = 1.5f, center = Offset(center.x - d.size * 0.25f, center.y - d.size * 0.15f))
                drawCircle(color = Color.White, radius = 1.5f, center = Offset(center.x + d.size * 0.22f, center.y - d.size * 0.18f))
                drawCircle(color = Color(0xFF67E8F9), radius = 1.2f, center = Offset(center.x, center.y - d.size * 0.05f))
            }
            DecorationType.LANTERN_POST -> {
                // Ground warm lantern light pool
                drawCircle(color = Color(0x28F59E0B), radius = 32f, center = center)
                drawCircle(color = Color(0x3CFEF08A), radius = 18f, center = center)

                // Wrought-iron post
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(center.x - 2.5f, center.y - 18f),
                    size = Size(5f, 24f)
                )
                // Horizontal hanging arm
                drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(center.x - 10f, center.y - 18f),
                    size = Size(14f, 3f)
                )
                // Hanging lantern cage
                val lanternX = center.x - 8f
                val lanternY = center.y - 10f
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(lanternX - 5f, lanternY - 4f),
                    size = Size(10f, 12f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Glowing lantern glass core
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(lanternX - 3.5f, lanternY - 2.5f),
                    size = Size(7f, 9f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawCircle(color = Color(0xFFFEF08A), radius = 2.5f, center = Offset(lanternX, lanternY + 2f))
                drawCircle(color = Color.White, radius = 1.2f, center = Offset(lanternX, lanternY + 2f))
            }
            else -> {}
        }
    }
}

private fun DrawScope.drawPineSnowTier(cx: Float, cy: Float, radius: Float) {
    val path = Path().apply {
        moveTo(cx, cy - radius * 1.25f)
        lineTo(cx + radius * 0.9f, cy - radius * 0.15f)
        lineTo(cx - radius * 0.9f, cy - radius * 0.15f)
        close()
    }
    drawPath(path, Color(0xFFCBD5E1))
    val crustPath = Path().apply {
        moveTo(cx, cy - radius * 1.25f)
        lineTo(cx + radius * 0.7f, cy - radius * 0.35f)
        lineTo(cx - radius * 0.7f, cy - radius * 0.35f)
        close()
    }
    drawPath(crustPath, Color(0xFFF8FAFC))
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

private fun DrawScope.drawSpawnGate(start: Point2D, worldWidth: Float, worldHeight: Float, gameTime: Float) {
    val gateX = if (start.x < 0f) 28f else if (start.x > worldWidth) worldWidth - 28f else start.x
    val gateY = if (start.y < 0f) 28f else if (start.y > worldHeight) worldHeight - 28f else start.y
    val center = Offset(gateX, gateY)

    // Ground scorched stone shadow
    drawCircle(color = Color(0x66000000), radius = 36f, center = center)

    // Jagged stone portal ring
    drawCircle(color = Color(0xFF0F172A), radius = 32f, center = center)
    drawCircle(color = Color(0xFF334155), radius = 32f, center = center, style = Stroke(width = 5f))
    drawCircle(color = Color(0xFF475569), radius = 28f, center = center, style = Stroke(width = 2f))

    // Ominous swirling void vortex
    val pulse = sin(gameTime * 4f) * 2f
    drawCircle(color = Color(0xFF1E1B4B), radius = 22f + pulse, center = center)
    drawCircle(color = Color(0xFF4C0519), radius = 16f, center = center)
    drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.6f), radius = 8f + pulse * 0.5f, center = center)

    // Red warning torch flames on left and right pillars
    val flame1 = sin(gameTime * 11f) * 1.5f
    val flame2 = cos(gameTime * 13f) * 1.5f
    drawCircle(color = Color(0xFFFF5722), radius = 5f + flame1, center = Offset(center.x - 22f, center.y - 18f))
    drawCircle(color = Color(0xFFFFD166), radius = 2.5f, center = Offset(center.x - 22f, center.y - 19f))
    drawCircle(color = Color(0xFFFF5722), radius = 5f + flame2, center = Offset(center.x + 22f, center.y - 18f))
    drawCircle(color = Color(0xFFFFD166), radius = 2.5f, center = Offset(center.x + 22f, center.y - 19f))
}

private fun DrawScope.drawCastleBase(base: Base, gameTime: Float, env: EnvironmentType = EnvironmentType.GREEN_VALLEY) {
    val left = base.position.x - base.width / 2f
    val top = base.position.y - base.height / 2f
    val cx = base.position.x
    val cy = base.position.y

    // 1. Soft fortress ground drop shadow
    drawRoundRect(
        color = Color(0x55000000),
        topLeft = Offset(left + 8f, top + 12f),
        size = Size(base.width, base.height),
        cornerRadius = CornerRadius(16f, 16f)
    )

    // In Snow Valley, snow banked around the fortress perimeter
    if (env == EnvironmentType.SNOW_VALLEY) {
        drawRoundRect(
            color = Color(0xDDE2E8F0),
            topLeft = Offset(left - 6f, top + base.height - 10f),
            size = Size(base.width + 12f, 18f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(left - 3f, top + base.height - 8f),
            size = Size(base.width + 6f, 12f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }

    // 2. Left & Right Bastion Stone Watchtowers
    val towerR = 18f
    val leftTowerCenter = Offset(left + 12f, top + 14f)
    val rightTowerCenter = Offset(left + base.width - 12f, top + 14f)

    // Left Bastion Tower
    drawCircle(color = Color(0xFF334155), radius = towerR, center = leftTowerCenter)
    drawCircle(color = Color(0xFF475569), radius = towerR - 2.5f, center = leftTowerCenter)
    drawCircle(color = Color(0xFF64748B), radius = towerR - 5f, center = leftTowerCenter)
    drawCircle(color = Color(0xFF1E293B), radius = towerR, center = leftTowerCenter, style = Stroke(width = 2f))
    drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(leftTowerCenter.x - 2f, leftTowerCenter.y - 7f), size = Size(4f, 10f), cornerRadius = CornerRadius(1f, 1f))

    // Right Bastion Tower
    drawCircle(color = Color(0xFF334155), radius = towerR, center = rightTowerCenter)
    drawCircle(color = Color(0xFF475569), radius = towerR - 2.5f, center = rightTowerCenter)
    drawCircle(color = Color(0xFF64748B), radius = towerR - 5f, center = rightTowerCenter)
    drawCircle(color = Color(0xFF1E293B), radius = towerR, center = rightTowerCenter, style = Stroke(width = 2f))
    drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(rightTowerCenter.x - 2f, rightTowerCenter.y - 7f), size = Size(4f, 10f), cornerRadius = CornerRadius(1f, 1f))

    // Snow caps & warm lanterns on Bastion Towers in Snow Valley
    if (env == EnvironmentType.SNOW_VALLEY) {
        // Snow caps
        drawCircle(color = Color(0xFFCBD5E1), radius = towerR * 0.7f, center = Offset(leftTowerCenter.x, leftTowerCenter.y - 5f))
        drawCircle(color = Color(0xFFF8FAFC), radius = towerR * 0.55f, center = Offset(leftTowerCenter.x, leftTowerCenter.y - 6f))
        drawCircle(color = Color(0xFFCBD5E1), radius = towerR * 0.7f, center = Offset(rightTowerCenter.x, rightTowerCenter.y - 5f))
        drawCircle(color = Color(0xFFF8FAFC), radius = towerR * 0.55f, center = Offset(rightTowerCenter.x, rightTowerCenter.y - 6f))

        // Warm defensive heating braziers with gentle ambient glow
        val brazierPulse = (kotlin.math.sin(gameTime * 10f) * 0.5f + 0.5f) * 3f
        drawCircle(color = Color(0x44F59E0B), radius = 13f + brazierPulse, center = leftTowerCenter)
        drawCircle(color = Color(0xFFF59E0B), radius = 4f, center = leftTowerCenter)
        drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = leftTowerCenter)

        drawCircle(color = Color(0x44F59E0B), radius = 13f - brazierPulse, center = rightTowerCenter)
        drawCircle(color = Color(0xFFF59E0B), radius = 4f, center = rightTowerCenter)
        drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = rightTowerCenter)
    } else if (env == EnvironmentType.NIGHT_FORTRESS) {
        // Glowing night defense beacons on Bastion Towers
        val beaconPulse = (kotlin.math.sin(gameTime * 8f) * 0.5f + 0.5f) * 4f
        drawCircle(color = Color(0x5538BDF8), radius = 15f + beaconPulse, center = leftTowerCenter)
        drawCircle(color = Color(0xFF38BDF8), radius = 4.5f, center = leftTowerCenter)
        drawCircle(color = Color.White, radius = 2f, center = leftTowerCenter)

        drawCircle(color = Color(0x5538BDF8), radius = 15f - beaconPulse, center = rightTowerCenter)
        drawCircle(color = Color(0xFF38BDF8), radius = 4.5f, center = rightTowerCenter)
        drawCircle(color = Color.White, radius = 2f, center = rightTowerCenter)
    }

    // 3. Central Fortress Keep Walls (Stone masonry with highlights)
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(left + 8f, top + 6f),
        size = Size(base.width - 16f, base.height - 6f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(left + 11f, top + 9f),
        size = Size(base.width - 22f, base.height - 12f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Stone masonry brick seams
    for (row in 1..3) {
        val brickY = top + 10f + row * 12f
        drawLine(
            color = Color(0xFF334155),
            start = Offset(left + 16f, brickY),
            end = Offset(left + base.width - 16f, brickY),
            strokeWidth = 1.5f
        )
    }

    // 4. Crenellations (Castle battlements across the top wall)
    val battlementCount = 5
    val bWidth = (base.width - 32f) / (battlementCount * 2 - 1)
    for (i in 0 until battlementCount) {
        val bx = left + 16f + (i * 2) * bWidth
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(bx, top - 6f),
            size = Size(bWidth, 14f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = Color(0xFF64748B),
            topLeft = Offset(bx + 1f, top - 5f),
            size = Size(bWidth - 2f, 3f)
        )
        // Snow accumulation cushions on top of battlements in Snow Valley
        if (env == EnvironmentType.SNOW_VALLEY) {
            drawRoundRect(
                color = Color(0xFFCBD5E1),
                topLeft = Offset(bx - 1f, top - 10f),
                size = Size(bWidth + 2f, 6f),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(bx, top - 11f),
                size = Size(bWidth, 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        } else if (env == EnvironmentType.NIGHT_FORTRESS) {
            // Moonlit specular rim along battlement top
            drawRect(
                color = Color(0xFF93C5FD),
                topLeft = Offset(bx + 1f, top - 6f),
                size = Size(bWidth - 2f, 2.0f)
            )
        }
    }

    // 4.5. Citadel Arched Stained-Glass / Warm Windows in Night Fortress
    if (env == EnvironmentType.NIGHT_FORTRESS) {
        val winY = top + 14f
        val winW = 8f
        val winH = 14f
        for (w in listOf(cx - 20f, cx, cx + 20f)) {
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(w - winW / 2f - 1f, winY - 1f),
                size = Size(winW + 2f, winH + 2f),
                cornerRadius = CornerRadius(winW / 2f, winW / 2f)
            )
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(w - winW / 2f, winY),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(winW / 2f, winW / 2f)
            )
            drawCircle(color = Color(0xFFFEF08A), radius = 2f, center = Offset(w, winY + winH * 0.45f))
        }
    }

    // 5. Wooden Portcullis Gate
    val gateW = 34f
    val gateH = 34f
    val gateLeft = cx - gateW / 2f
    val gateTop = top + base.height - gateH - 4f

    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(gateLeft - 2f, gateTop - 2f),
        size = Size(gateW + 4f, gateH + 4f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFF5C3D2E),
        topLeft = Offset(gateLeft, gateTop),
        size = Size(gateW, gateH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Wood plank seams
    drawLine(Color(0xFF3E2723), Offset(gateLeft + gateW / 3f, gateTop), Offset(gateLeft + gateW / 3f, gateTop + gateH), strokeWidth = 1.5f)
    drawLine(Color(0xFF3E2723), Offset(gateLeft + 2f * gateW / 3f, gateTop), Offset(gateLeft + 2f * gateW / 3f, gateTop + gateH), strokeWidth = 1.5f)
    // Iron portcullis bars
    for (b in 1..4) {
        val barX = gateLeft + b * (gateW / 5f)
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(barX, gateTop),
            end = Offset(barX, gateTop + gateH - 2f),
            strokeWidth = 2.2f
        )
    }

    // 6. Wall Sconce Torches with Animated Flickering Flame
    val torchFlicker = (sin(gameTime * 12f) * 0.5f + cos(gameTime * 17f) * 0.5f) * 2.5f
    // Left Torch
    val torchLeftX = gateLeft - 10f
    val torchY = gateTop + 8f
    drawRect(Color(0xFF1E293B), Offset(torchLeftX - 1.5f, torchY), Size(3f, 10f))
    drawCircle(Color(0x44F59E0B), radius = 10f + torchFlicker, center = Offset(torchLeftX, torchY - 2f))
    drawCircle(Color(0xFFF97316), radius = 4f + torchFlicker * 0.5f, center = Offset(torchLeftX, torchY - 2f))
    drawCircle(Color(0xFFFDE047), radius = 2f, center = Offset(torchLeftX, torchY - 3f))

    // Right Torch
    val torchRightX = gateLeft + gateW + 10f
    drawRect(Color(0xFF1E293B), Offset(torchRightX - 1.5f, torchY), Size(3f, 10f))
    drawCircle(Color(0x44F59E0B), radius = 10f - torchFlicker, center = Offset(torchRightX, torchY - 2f))
    drawCircle(Color(0xFFF97316), radius = 4f - torchFlicker * 0.5f, center = Offset(torchRightX, torchY - 2f))
    drawCircle(Color(0xFFFDE047), radius = 2f, center = Offset(torchRightX, torchY - 3f))

    // 7. Royal Kingdom Banner on Flagpole (Wind waving animation)
    val flagX = cx
    val flagPoleTop = top - 36f
    // Wooden pole
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(flagX, top - 6f),
        end = Offset(flagX, flagPoleTop),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    // Golden finial
    drawCircle(Color(0xFFFFD166), radius = 3f, center = Offset(flagX, flagPoleTop))

    // Animated waving pennant banner
    val wave = sin(gameTime * 4.5f) * 4f
    val banner = Path().apply {
        moveTo(flagX, flagPoleTop + 3f)
        cubicTo(
            flagX + 10f, flagPoleTop + 3f + wave,
            flagX + 18f, flagPoleTop + 5f - wave,
            flagX + 26f, flagPoleTop + 8f + wave * 0.5f
        )
        lineTo(flagX + 22f, flagPoleTop + 16f)
        cubicTo(
            flagX + 16f, flagPoleTop + 14f - wave * 0.5f,
            flagX + 8f, flagPoleTop + 12f + wave,
            flagX, flagPoleTop + 12f
        )
        close()
    }
    drawPath(banner, Color(0xFF0284C7))
    drawCircle(Color(0xFFFFD166), radius = 1.5f, center = Offset(flagX + 12f, flagPoleTop + 8f))

    // 8. Attached Base Health Bar directly above the fortress
    val barW = base.width
    val barH = 8.5f
    val barTop = top - 48f
    val barLeft = left

    // Dark slate stone frame
    drawRoundRect(
        color = Color(0xEE0F172A),
        topLeft = Offset(barLeft - 2f, barTop - 2f),
        size = Size(barW + 4f, barH + 4f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(barLeft - 2f, barTop - 2f),
        size = Size(barW + 4f, barH + 4f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1f)
    )
    // Dark track background
    drawRoundRect(
        color = Color(0xFF450A0A),
        topLeft = Offset(barLeft, barTop),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Health fill
    val hpFillColor = when {
        base.healthPercentage > 0.5f -> Color(0xFF22C55E)
        base.healthPercentage > 0.25f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val fillWidth = (barW * base.healthPercentage).coerceAtLeast(0f)
    if (fillWidth > 0f) {
        drawRoundRect(
            color = hpFillColor,
            topLeft = Offset(barLeft, barTop),
            size = Size(fillWidth, barH),
            cornerRadius = CornerRadius(3f, 3f)
        )
        // Top gloss highlight
        drawRoundRect(
            color = Color(0x55FFFFFF),
            topLeft = Offset(barLeft, barTop),
            size = Size(fillWidth, barH * 0.45f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
    // Segment tick marks (4 dividers = 5 equal 20% segments)
    for (s in 1..4) {
        val segX = barLeft + (barW / 5f) * s
        drawLine(
            color = Color(0x660F172A),
            start = Offset(segX, barTop),
            end = Offset(segX, barTop + barH),
            strokeWidth = 1.2f
        )
    }
}

private fun DrawScope.drawTowers(
    towers: List<Tower>,
    selectedTower: Tower?,
    gameTime: Float,
    env: EnvironmentType = EnvironmentType.GREEN_VALLEY
) {
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

        // Snow accumulation ring around tower pedestal base
        if (env == EnvironmentType.SNOW_VALLEY) {
            val baseR = tower.spec.size * 0.48f
            drawCircle(
                color = Color(0x99CBD5E1),
                radius = baseR + 3.5f,
                center = center,
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color(0xCCF1F5F9),
                radius = baseR + 1.5f,
                center = center,
                style = Stroke(width = 2.0f)
            )
        } else if (env == EnvironmentType.NIGHT_FORTRESS) {
            val baseR = tower.spec.size * 0.48f
            // Moonlit pedestal rim glow & runic beacon foundation
            drawCircle(
                color = Color(0x2838BDF8),
                radius = baseR + 4.5f,
                center = center
            )
            drawCircle(
                color = Color(0x8893C5FD),
                radius = baseR + 1.5f,
                center = center,
                style = Stroke(width = 1.8f)
            )
        }

        when (tower.spec.type) {
            TowerType.MACHINE_GUN -> drawMachineGunTower(center, tower)
            TowerType.CANNON -> drawCannonTower(center, tower)
            TowerType.RAPID_FIRE -> drawRapidFireTower(center, tower, gameTime)
            TowerType.FROST_GUN -> drawFrostTower(center, tower, gameTime)
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

/**
 * 4. Frost / Cryo Tower: Sub-zero cryo projector that fires cryogenic orbs to heavily slow down enemies.
 * Features insulated coolant manifolds, liquid nitrogen canisters, and crystalline emitters.
 */
private fun DrawScope.drawFrostTower(center: Offset, tower: Tower, time: Float) {
    val baseR = tower.spec.size * 0.48f

    // 1. Soft ground drop shadow
    drawOval(
        color = Color(0x45000000),
        topLeft = Offset(center.x - baseR * 1.05f + 4f, center.y - baseR * 0.75f + 6f),
        size = Size(baseR * 2.1f, baseR * 1.6f)
    )

    // 2. Heavy fortified cryo pedestal base with frosted steel plating and cyan accents
    drawCircle(color = Color(0xFF0B1329), radius = baseR, center = center)
    drawCircle(color = Color(0xFF164E63), radius = baseR - 2.5f, center = center)
    drawCircle(color = Color(0xFF0891B2), radius = baseR * 0.85f, center = center)
    drawCircle(color = Color(0xFF38BDF8), radius = baseR * 0.82f, center = Offset(center.x - 1.5f, center.y - 1.5f), style = Stroke(width = 1.5f))

    // 3. Four Cryogenic Coolant Flasks on pedestal corners
    for (i in 0..3) {
        val ang = (i * Math.PI / 2.0 + Math.PI / 4.0).toFloat()
        val bx = center.x + cos(ang) * (baseR * 0.80f)
        val by = center.y + sin(ang) * (baseR * 0.80f)
        drawCircle(color = Color(0xFF082F49), radius = 4f, center = Offset(bx, by))
        drawCircle(color = Color(0xFF06B6D4), radius = 3f, center = Offset(bx, by))
        drawCircle(color = Color(0xFFBAE6FD), radius = 1.5f, center = Offset(bx - 0.8f, by - 0.8f))
    }

    // 4. Rotating Cryo Turret & Emitter Assembly
    rotate(degrees = tower.rotationAngle, pivot = center) {
        // Geared turntable ring
        drawCircle(color = Color(0xFF082F49), radius = 17f, center = center)
        drawCircle(color = Color(0xFF0E7490), radius = 14.5f, center = center)

        // Rear liquid nitrogen coolant cylinder with liquid level
        drawRoundRect(
            color = Color(0xFF0B1329),
            topLeft = Offset(center.x - 17f, center.y - 7.5f),
            size = Size(10f, 15f),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = Color(0xFF06B6D4),
            topLeft = Offset(center.x - 15f, center.y - 5.5f),
            size = Size(6f, 11f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = Color(0xFFE0F2FE),
            topLeft = Offset(center.x - 14f, center.y - 4.5f),
            size = Size(2f, 9f)
        )

        val recoil = tower.recoilFraction * 7f

        when (tower.spec.level) {
            1 -> {
                // Level 1: Cryo Blaster - Dual focusing prongs and insulated cooling shroud
                val barrelLength = 26f

                // Main cryo projector tube
                drawRoundRect(
                    color = Color(0xFF082F49),
                    topLeft = Offset(center.x + 2f - recoil, center.y - 5.5f),
                    size = Size(barrelLength, 11f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                drawRoundRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(center.x + 3f - recoil, center.y - 4f),
                    size = Size(barrelLength - 3f, 8f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Superconductor core glow line
                drawRect(
                    color = Color(0xFF67E8F9),
                    topLeft = Offset(center.x + 4f - recoil, center.y - 1.5f),
                    size = Size(barrelLength - 6f, 3f)
                )

                // Upper and lower sub-zero focus condenser prongs
                drawRoundRect(
                    color = Color(0xFF082F49),
                    topLeft = Offset(center.x + barrelLength - 3f - recoil, center.y - 8f),
                    size = Size(8f, 3.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawRoundRect(
                    color = Color(0xFF082F49),
                    topLeft = Offset(center.x + barrelLength - 3f - recoil, center.y + 4.5f),
                    size = Size(8f, 3.5f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawCircle(Color(0xFF38BDF8), radius = 2f, center = Offset(center.x + barrelLength + 3f - recoil, center.y - 6.2f))
                drawCircle(Color(0xFF38BDF8), radius = 2f, center = Offset(center.x + barrelLength + 3f - recoil, center.y + 6.2f))
            }
            2 -> {
                // Level 2: Glacial Cannon - Heavy insulated armored barrel with radiator heat sinks
                val barrelLength = 32f

                // Side cryo coolant pipes
                drawLine(Color(0xFF06B6D4), Offset(center.x - 7f, center.y - 11f), Offset(center.x + 12f - recoil, center.y - 8f), strokeWidth = 3f, cap = StrokeCap.Round)
                drawLine(Color(0xFF06B6D4), Offset(center.x - 7f, center.y + 11f), Offset(center.x + 12f - recoil, center.y + 8f), strokeWidth = 3f, cap = StrokeCap.Round)

                // Armored heavy barrel housing
                drawRoundRect(
                    color = Color(0xFF082F49),
                    topLeft = Offset(center.x + 1f - recoil, center.y - 7f),
                    size = Size(barrelLength, 14f),
                    cornerRadius = CornerRadius(3.5f, 3.5f)
                )
                drawRoundRect(
                    color = Color(0xFF0E7490),
                    topLeft = Offset(center.x + 2f - recoil, center.y - 5.5f),
                    size = Size(barrelLength - 3f, 11f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )

                // Sub-zero radiator cooling fins
                for (f in 0..3) {
                    drawRect(color = Color(0xFF38BDF8), topLeft = Offset(center.x + 5f + f * 5f - recoil, center.y - 9f), size = Size(2.5f, 18f))
                }

                // Glowing focusing crystal muzzle collar
                drawRoundRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(center.x + barrelLength - 3f - recoil, center.y - 8.5f),
                    size = Size(7f, 17f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
                drawCircle(Color(0xFFE0F2FE), radius = 3.5f, center = Offset(center.x + barrelLength + 2f - recoil, center.y))
            }
            else -> {
                // Level 3: Blizzard Mortar - Colossal cryo accelerator with sub-zero coils & crystal crown
                val barrelLength = 36f

                // Massive lateral cryo condenser tanks
                drawRoundRect(color = Color(0xFF082F49), topLeft = Offset(center.x - 8f, center.y - 17f), size = Size(14f, 8f), cornerRadius = CornerRadius(2.5f, 2.5f))
                drawRoundRect(color = Color(0xFF06B6D4), topLeft = Offset(center.x - 7f, center.y - 16f), size = Size(12f, 6f), cornerRadius = CornerRadius(2f, 2f))
                drawRoundRect(color = Color(0xFF082F49), topLeft = Offset(center.x - 8f, center.y + 9f), size = Size(14f, 8f), cornerRadius = CornerRadius(2.5f, 2.5f))
                drawRoundRect(color = Color(0xFF06B6D4), topLeft = Offset(center.x - 7f, center.y + 10f), size = Size(12f, 6f), cornerRadius = CornerRadius(2f, 2f))

                // Colossal accelerator barrel
                drawRoundRect(
                    color = Color(0xFF0B1329),
                    topLeft = Offset(center.x - 1f - recoil, center.y - 8.5f),
                    size = Size(barrelLength, 17f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xFF0284C7),
                    topLeft = Offset(center.x - recoil, center.y - 7f),
                    size = Size(barrelLength - 2f, 14f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Triple glowing superconductor accelerator rings
                for (r in 0..2) {
                    val rx = center.x + 8f + r * 9f - recoil
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(rx, center.y - 10f),
                        size = Size(4f, 20f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    drawRect(color = Color(0xFFE0F2FE), topLeft = Offset(rx + 1f, center.y - 7f), size = Size(2f, 14f))
                }

                // Spiked ice-crystalline muzzle emitter crown
                drawRoundRect(
                    color = Color(0xFF082F49),
                    topLeft = Offset(center.x + barrelLength - 3f - recoil, center.y - 11f),
                    size = Size(8f, 22f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawCircle(Color(0xFFE0F2FE), radius = 5f, center = Offset(center.x + barrelLength + 3f - recoil, center.y))
                drawCircle(Color(0xFF06B6D4), radius = 3f, center = Offset(center.x + barrelLength + 3f - recoil, center.y))
            }
        }

        // Central cryo sensor dome with cyan scanning pulse
        val pulseColor = if ((sin(time * 6f) * 0.5f + 0.5f) > 0.5f) Color(0xFF38BDF8) else Color(0xFF67E8F9)
        drawCircle(color = Color(0xFF082F49), radius = 10f, center = center)
        drawCircle(color = Color(0xFF0E7490), radius = 8f, center = center)
        drawCircle(color = pulseColor, radius = 5f, center = Offset(center.x - 1f, center.y - 1f))
        drawCircle(color = Color.White, radius = 1.8f, center = Offset(center.x - 1.5f, center.y - 1.5f))

        // Firing Animation: Cold flash burst with frost crystals
        if (tower.isFiring) {
            val tipX = center.x + (if (tower.spec.level == 1) 27f else if (tower.spec.level == 2) 34f else 39f) - recoil
            drawCircle(color = Color(0x6606B6D4), radius = 14f, center = Offset(tipX + 4f, center.y))
            drawCircle(color = Color(0xFF38BDF8), radius = 9f, center = Offset(tipX + 4f, center.y))
            drawCircle(color = Color(0xFFE0F2FE), radius = 5f, center = Offset(tipX + 4f, center.y))
            drawCircle(color = Color.White, radius = 2.5f, center = Offset(tipX + 4f, center.y))

            // Frost crystal spikes radiating from muzzle
            for (s in 0..3) {
                val sAngle = (s * Math.PI / 2.0).toFloat()
                val sx = tipX + 4f + cos(sAngle) * 11f
                val sy = center.y + sin(sAngle) * 11f
                drawLine(Color(0xFFE0F2FE), Offset(tipX + 4f, center.y), Offset(sx, sy), strokeWidth = 2f, cap = StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawEnemies(
    enemies: List<Enemy>,
    time: Float,
    env: EnvironmentType = EnvironmentType.GREEN_VALLEY
) {
    for (enemy in enemies) {
        if (!enemy.isAlive || enemy.reachedBase) continue

        // Wobble walking animation
        val wobbleY = sin(enemy.animWobbleTime) * 1.5f
        val center = Offset(enemy.position.x, enemy.position.y + wobbleY)

        // In Snow Valley, footsteps left in the snow
        if (env == EnvironmentType.SNOW_VALLEY) {
            val stepPhase = (enemy.totalProgress / 16f) % 2.0f
            val isLeft = stepPhase > 1.0f
            val rad = Math.toRadians(enemy.headingAngle.toDouble())
            val backX = center.x - kotlin.math.cos(rad).toFloat() * (enemy.spec.radius * 0.7f)
            val backY = center.y - kotlin.math.sin(rad).toFloat() * (enemy.spec.radius * 0.7f)
            val perpOffset = if (isLeft) 3.5f else -3.5f
            val perpX = -kotlin.math.sin(rad).toFloat() * perpOffset
            val perpY = kotlin.math.cos(rad).toFloat() * perpOffset

            drawOval(
                color = Color(0x5564748B),
                topLeft = Offset(backX + perpX - 2.5f, backY + perpY - 1.5f),
                size = Size(5f, 3f)
            )
            drawOval(
                color = Color(0x33BAE6FD),
                topLeft = Offset(backX + perpX - 1.5f, backY + perpY - 1f),
                size = Size(3f, 2f)
            )
        } else if (env == EnvironmentType.NIGHT_FORTRESS) {
            // Subtle cool moonlight rim outline around enemy ensuring pristine contrast and visibility
            drawCircle(
                color = Color(0x4438BDF8),
                radius = enemy.spec.radius * 1.15f,
                center = center
            )
        }

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

        // 1b. Warning Telegraphs & Ability Indicators
        if (enemy.shockwaveWarningTimer > 0f) {
            val warningFraction = (1f - (enemy.shockwaveWarningTimer / 1.5f)).coerceIn(0.1f, 1f)
            drawCircle(
                color = Color(0x33EF4444),
                radius = 200f * warningFraction,
                center = center
            )
            drawCircle(
                color = Color(0xCCEF4444),
                radius = 200f,
                center = center,
                style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), time * 25f))
            )
        }

        if (enemy.summonWarningTimer > 0f) {
            val pulse = sin(time * 12f) * 2.5f
            drawCircle(
                color = Color(0x358B5CF6),
                radius = (enemy.spec.radius * 1.5f) + pulse,
                center = center
            )
            drawCircle(
                color = Color(0xFFA855F7),
                radius = (enemy.spec.radius * 1.5f) + pulse,
                center = center,
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), time * 15f))
            )
        }

        if (enemy.isSpeedBurstActive) {
            val rad = Math.toRadians(enemy.headingAngle.toDouble())
            for (tr in 1..3) {
                val trDist = tr * 10f
                val tx = center.x - cos(rad).toFloat() * trDist
                val ty = center.y - sin(rad).toFloat() * trDist
                drawCircle(Color(0x5538BDF8), radius = enemy.spec.radius * (1f - tr * 0.22f), center = Offset(tx, ty))
            }
        }

        // 2. Character Sprite facing movement direction
        val spriteAlpha = if (enemy.isStealthed) 0.35f else 1.0f
        withTransform({
            // Apply stealth shimmer if stealthed
            if (spriteAlpha < 1.0f) {
                // Dimmed ghostly appearance
            }
        }) {
            rotate(degrees = enemy.headingAngle, pivot = center) {
                when (enemy.spec.type) {
                    EnemyType.SCOUT -> drawScoutEnemy(center, enemy, time)
                    EnemyType.SOLDIER -> drawSoldierEnemy(center, enemy, time)
                    EnemyType.HEAVY -> drawHeavyEnemy(center, enemy, time)
                    EnemyType.RUNNER -> drawRunnerEnemy(center, enemy, time)
                    EnemyType.SHIELD -> drawShieldEnemy(center, enemy, time)
                    EnemyType.FLYING -> drawFlyingEnemy(center, enemy, time)
                    EnemyType.HEALER -> drawHealerEnemy(center, enemy, time)
                    EnemyType.SUMMONER -> drawSummonerEnemy(center, enemy, time)
                    EnemyType.STEALTH -> drawStealthEnemy(center, enemy, time)
                    EnemyType.BOSS -> drawBossEnemy(center, enemy, time)
                }
            }
        }

        // 2b. Slow Status Aura & Orbiting Frost Crystals
        if (enemy.isSlowed) {
            val frostR = enemy.spec.radius * 1.18f
            val pulse = sin(time * 7f) * 2f

            // Freezing cyan frosted aura
            drawCircle(
                color = Color(0x3506B6D4),
                radius = frostR + pulse,
                center = center
            )
            drawCircle(
                color = Color(0x9938BDF8),
                radius = frostR + pulse,
                center = center,
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), time * 16f))
            )

            // Orbiting sub-zero ice crystals
            for (i in 0..3) {
                val fAngle = (i * Math.PI / 2.0 + time * 2.5f).toFloat()
                val fx = center.x + cos(fAngle) * (frostR + 4f)
                val fy = center.y + sin(fAngle) * (frostR + 4f)
                drawCircle(Color(0xFFE0F2FE), radius = 2.4f, center = Offset(fx, fy))
                drawCircle(Color(0xFF0284C7), radius = 1.2f, center = Offset(fx, fy))
            }
        }

        // 3. Energy Shield Bubble
        if (enemy.isEnergyShieldActive) {
            val pulse = sin(time * 8f) * 2.5f
            drawCircle(
                color = Color(0x3306B6D4),
                radius = enemy.spec.radius * 1.28f + pulse,
                center = center
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = enemy.spec.radius * 1.22f + pulse,
                center = center,
                style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), time * 20f))
            )
        }

        // 3b. Boss Shield Phase Barrier or Enraged Phase Aura
        if (enemy.spec.isBoss) {
            if (enemy.isShielded) {
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
            if (enemy.bossPhase == 3) {
                val enragedPulse = sin(time * 16f) * 3.5f
                drawCircle(
                    color = Color(0x33DC2626),
                    radius = enemy.spec.radius * 1.35f + enragedPulse,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = enemy.spec.radius * 1.30f + enragedPulse,
                    center = center,
                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), time * 30f))
                )
            }
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
            val frameColor = if (enemy.bossPhase == 3) Color(0xFFEF4444) else Color(0xFFF59E0B)
            drawRoundRect(
                color = frameColor,
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
                enemy.bossPhase == 3 -> Color(0xFFDC2626)
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
            // Boss Phase Indicator Dots (P1, P2, P3)
            val dotSpacing = 8f
            for (p in 1..3) {
                val dotX = center.x + (p - 2) * dotSpacing
                val dotColor = if (p <= enemy.bossPhase) {
                    if (enemy.bossPhase == 3) Color(0xFFEF4444) else Color(0xFFFDE047)
                } else Color(0xFF475569)
                drawCircle(
                    color = dotColor,
                    radius = if (p == enemy.bossPhase) 3f else 2f,
                    center = Offset(dotX, bossBarTop - 5f)
                )
            }
        } else if (enemy.healthPercentage < 1.0f || enemy.isEnergyShieldActive || enemy.spec.armor > 0f || enemy.spec.maxArmorHp > 0f) {
            // Standard Enemy Health Bar
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

            // Energy Shield Bar above health bar
            if (enemy.isEnergyShieldActive && enemy.spec.maxShieldHp > 0f) {
                val shieldFraction = (enemy.currentShieldHp / enemy.spec.maxShieldHp).coerceIn(0f, 1f)
                val shieldBarTop = barTop - 4f
                drawRoundRect(
                    color = Color(0xCC082F49),
                    topLeft = Offset(barLeft, shieldBarTop),
                    size = Size(barWidth, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
                drawRoundRect(
                    color = Color(0xFF06B6D4),
                    topLeft = Offset(barLeft, shieldBarTop),
                    size = Size(barWidth * shieldFraction, 3f),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }

            // Heavy Armor indicator pip
            if (enemy.spec.armor > 0f || enemy.spec.maxArmorHp > 0f) {
                val armorColor = if (enemy.isArmorBroken) Color(0xFFEF4444) else Color(0xFF94A3B8)
                drawCircle(color = armorColor, radius = 2.5f, center = Offset(barLeft - 5f, barTop + barHeight / 2f))
            }
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

    // Hit Reaction: Green/Yellow spore & leather stitch spark burst
    if (enemy.isHitFlashing) {
        val flashPhase = (enemy.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        // High-contrast electric flash silhouette
        drawCircle(Color(0xEEFFFFFF), radius = 15f, center = center)
        drawCircle(Color(0xFFFFE066), radius = 11f, center = center)
        // Micro sparks & splinters bursting outwards
        for (i in 0..3) {
            val ang = i * 1.57f + (1f - flashPhase) * 2.5f
            val dist = 14f + (1f - flashPhase) * 10f
            val px = center.x + cos(ang) * dist
            val py = center.y + sin(ang) * dist
            drawCircle(Color(0xFF84CC16), radius = 1.8f * flashPhase, center = Offset(px, py))
        }
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

    // Hit Reaction: Steel armor ricochet flash & deflection sparks
    if (enemy.isHitFlashing) {
        val flashPhase = (enemy.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        drawCircle(Color(0xEEFFFFFF), radius = 18f, center = center)
        drawCircle(Color(0xFF38BDF8), radius = 13f, center = center)
        // High-velocity deflection metal sparks
        for (i in 0..4) {
            val ang = i * 1.25f + (1f - flashPhase) * 3f
            val dist = 16f + (1f - flashPhase) * 12f
            val px = center.x + cos(ang) * dist
            val py = center.y + sin(ang) * dist
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(px, py),
                end = Offset(px + cos(ang) * 4f, py + sin(ang) * 4f),
                strokeWidth = 1.8f * flashPhase,
                cap = StrokeCap.Round
            )
        }
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

    // Hit Reaction: Heavy slag spark burst & fiery ember shockwave
    if (enemy.isHitFlashing) {
        val flashPhase = (enemy.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        drawCircle(Color(0xEEFFFFFF), radius = 26f, center = center)
        drawCircle(Color(0xFFEA580C), radius = 20f, center = center)
        drawCircle(Color(0xFFFACC15), radius = 14f, center = center)
        // Fiery slag sparks spraying outwards
        for (i in 0..5) {
            val ang = i * 1.05f + (1f - flashPhase) * 2f
            val dist = 20f + (1f - flashPhase) * 16f
            val px = center.x + cos(ang) * dist
            val py = center.y + sin(ang) * dist
            drawCircle(Color(0xFFF97316), radius = 2.4f * flashPhase, center = Offset(px, py))
            drawCircle(Color(0xFFFEF08A), radius = 1.2f * flashPhase, center = Offset(px, py))
        }
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

    // Hit Reaction: High-frequency electric discharge arcs & cyan particles
    if (enemy.isHitFlashing) {
        val flashPhase = (enemy.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        drawCircle(Color(0xEEFFFFFF), radius = 15f, center = center)
        drawCircle(Color(0xFF38BDF8), radius = 11f, center = center)
        // High-velocity electric arcs radiating along velocity vector
        for (i in 0..4) {
            val ang = i * 1.25f + (1f - flashPhase) * 4f
            val dist = 13f + (1f - flashPhase) * 11f
            val px = center.x + cos(ang) * dist
            val py = center.y + sin(ang) * dist
            drawLine(
                color = Color(0xFFFDE047),
                start = Offset(center.x + cos(ang) * 5f, center.y + sin(ang) * 5f),
                end = Offset(px, py),
                strokeWidth = 1.6f * flashPhase,
                cap = StrokeCap.Round
            )
            drawCircle(Color(0xFF38BDF8), radius = 1.3f * flashPhase, center = Offset(px, py))
        }
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
    val isVoidSovereign = enemy.spec.name == "Void Sovereign"
    val stride = sin(enemy.animWobbleTime * 0.45f)

    // Pulsing Abyssal Void Aura beneath Void Sovereign (Strongest Boss)
    if (isVoidSovereign) {
        val voidPulse = sin(time * 5f) * 0.15f + 0.85f
        val auraR = enemy.spec.radius * 1.32f * voidPulse
        // Outer dark void corona
        drawCircle(
            color = Color(0x667928CA),
            radius = auraR,
            center = center
        )
        // Electric magenta rune ring
        drawCircle(
            color = Color(0x99FF0080),
            radius = auraR * 0.88f,
            center = center,
            style = Stroke(width = 2.5f)
        )
        // Orbiting void energy spark motes
        for (m in 0..5) {
            val angle = time * 2.5f + (m * 1.047f)
            val dist = auraR * (0.6f + (m % 2) * 0.25f)
            val mx = center.x + cos(angle) * dist
            val my = center.y + sin(angle) * dist
            drawCircle(Color(0xFFFF0080), radius = 2.8f, center = Offset(mx, my))
            drawCircle(Color.White, radius = 1.3f, center = Offset(mx, my))
        }
    }

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
    val capeFill = if (isVoidSovereign) Color(0xFF1E0A3C) else Color(0xFF4C0519)
    val capeBorder = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFF59E0B)
    drawPath(capePath, capeFill)
    drawPath(capePath, capeBorder, style = Stroke(width = 1.8f))

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
    val chestFill = if (isVoidSovereign) Color(0xFF581C87) else Color(0xFF881337)
    val chestBorder = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFF59E0B)
    drawPath(chestInner, chestFill)
    drawPath(chestInner, chestBorder, style = Stroke(width = 2f))

    // Pulsing Furnace Core / Runic Heart Vent in chest
    val corePulse = (sin(time * 6f) * 0.25f + 0.75f)
    val corePos = Offset(center.x - 2f, center.y)
    val coreOuter = if (isVoidSovereign) Color(0xFF7928CA) else Color(0xFFEF4444)
    val coreInner = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFFDE047)
    drawCircle(Color(0xFF450A0A), radius = 7f, center = corePos)
    drawCircle(coreOuter, radius = 5.5f * corePulse, center = corePos)
    drawCircle(coreInner, radius = 3.2f * corePulse, center = corePos)
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
    val axeRuneColor = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFF59E0B)
    drawLine(axeRuneColor, Offset(axeHeadX + 12f, axeHeadY - 22f), Offset(axeHeadX + 17f, axeHeadY), strokeWidth = 2f)
    drawLine(axeRuneColor, Offset(axeHeadX + 17f, axeHeadY), Offset(axeHeadX + 12f, axeHeadY + 22f), strokeWidth = 2f)
    drawCircle(if (isVoidSovereign) Color(0xFFE0E7FF) else Color(0xFFFDE047), radius = 2.5f, center = Offset(axeHeadX + 4f, axeHeadY))

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
    val helmFill = if (isVoidSovereign) Color(0xFF3B0764) else Color(0xFF4C0519)
    drawRoundRect(
        color = helmFill,
        topLeft = Offset(headPos.x - 5f, headPos.y - 9f),
        size = Size(12f, 18f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // 5-Spike Crown Crest on helmet brow
    val crownX = headPos.x - 2f
    val crownSpikeColor = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFF59E0B)
    val crownGemColor = if (isVoidSovereign) Color(0xFFA855F7) else Color(0xFFEF4444)
    for (s in -2..2) {
        val spikeY = headPos.y + (s * 4f)
        val spikeLen = if (s == 0) 8f else if (Math.abs(s) == 1) 6f else 4f
        drawLine(crownSpikeColor, Offset(crownX, spikeY), Offset(crownX + spikeLen, spikeY), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawCircle(crownGemColor, radius = 1.2f, center = Offset(crownX + spikeLen, spikeY))
    }

    // Shadowed Visor Slit with Glowing Demonic Eyes
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(headPos.x + 5f, headPos.y - 8f),
        size = Size(4f, 16f),
        cornerRadius = CornerRadius(1.5f, 1.5f)
    )
    // Piercing Eyes
    val eyeColor = if (isVoidSovereign) Color(0xFFFF0080) else Color(0xFFEF4444)
    val pupilColor = if (isVoidSovereign) Color(0xFFE0E7FF) else Color(0xFFFDE047)
    drawRoundRect(
        color = eyeColor,
        topLeft = Offset(headPos.x + 6.5f, headPos.y - 6f),
        size = Size(2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawRoundRect(
        color = eyeColor,
        topLeft = Offset(headPos.x + 6.5f, headPos.y + 2f),
        size = Size(2f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawCircle(pupilColor, radius = 0.9f, center = Offset(headPos.x + 7.5f, headPos.y - 4f))
    drawCircle(pupilColor, radius = 0.9f, center = Offset(headPos.x + 7.5f, headPos.y + 4f))

    // Hit Reaction: Colossal armored deflection shockwave & crimson/gold spark burst
    if (enemy.isHitFlashing) {
        val flashPhase = (enemy.hitFlashTimer / 0.14f).coerceIn(0f, 1f)
        // Radiant whole-body flash
        drawCircle(Color(0xEEFFFFFF), radius = 42f, center = center)
        drawCircle(Color(0xFFFFD166), radius = 35f, center = center)
        drawCircle(Color(0xFFE11D48), radius = 28f, center = center)
        // Expanding deflection shockwave ring
        val ringR = 36f + (1f - flashPhase) * 18f
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = flashPhase * 0.85f),
            radius = ringR,
            center = center,
            style = Stroke(width = 3.5f * flashPhase)
        )
        // Radiating heavy champion sparks
        for (i in 0..7) {
            val ang = i * 0.785f + (1f - flashPhase) * 1.5f
            val dist = 32f + (1f - flashPhase) * 22f
            val px = center.x + cos(ang) * dist
            val py = center.y + sin(ang) * dist
            drawCircle(Color(0xFFFDE047), radius = 3.2f * flashPhase, center = Offset(px, py))
            drawCircle(Color.White, radius = 1.6f * flashPhase, center = Offset(px, py))
        }
    }
}

/**
 * 6. Shield Bearer Enemy: Heavy cobalt bulwark infantry carrying a massive energy-infused shield.
 * Frontal physical barrier deflects projectile damage; energy matrix shimmers when shield is active.
 */
private fun DrawScope.drawShieldEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 1.4f)
    val bodyR = enemy.spec.radius * 0.72f

    // Heavy combat boots
    val bootY = stride * 3f
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - 7f, center.y - 7f + bootY),
        size = Size(5f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - 7f, center.y + 3f - bootY),
        size = Size(5f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    // Armored Torso (Steel/Cobalt)
    drawCircle(color = Color(0xFF0F172A), radius = bodyR, center = center)
    drawCircle(color = Color(0xFF1E3A8A), radius = bodyR - 1.5f, center = center)
    drawCircle(color = Color(0xFF3B82F6), radius = bodyR * 0.65f, center = Offset(center.x - 1f, center.y))

    // Armored Helmet & Visor
    drawCircle(color = Color(0xFF1E293B), radius = bodyR * 0.55f, center = Offset(center.x + 2f, center.y))
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(center.x + 4f, center.y - 2f),
        size = Size(3f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )

    // Massive Frontal Tower Shield
    val shieldX = center.x + bodyR * 0.75f
    val shieldHeight = enemy.spec.radius * 2.2f
    val shieldWidth = 6.5f
    val shieldTop = center.y - shieldHeight / 2f

    // Shield Plate (Dark titanium with bright energetic border)
    val shieldColor = if (enemy.isEnergyShieldActive) Color(0xFF0284C7) else Color(0xFF334155)
    val shieldBorder = if (enemy.isEnergyShieldActive) Color(0xFF38BDF8) else Color(0xFF64748B)

    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(shieldX - 1f, shieldTop - 1f),
        size = Size(shieldWidth + 2f, shieldHeight + 2f),
        cornerRadius = CornerRadius(2.5f, 2.5f)
    )
    drawRoundRect(
        color = shieldColor,
        topLeft = Offset(shieldX, shieldTop),
        size = Size(shieldWidth, shieldHeight),
        cornerRadius = CornerRadius(2f, 2f)
    )
    drawRoundRect(
        color = shieldBorder,
        topLeft = Offset(shieldX, shieldTop),
        size = Size(shieldWidth, shieldHeight),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 1.5f)
    )

    // Central energetic crest / conduit line
    if (enemy.isEnergyShieldActive) {
        drawLine(
            color = Color(0xFFE0F2FE),
            start = Offset(shieldX + shieldWidth / 2f, shieldTop + 3f),
            end = Offset(shieldX + shieldWidth / 2f, shieldTop + shieldHeight - 3f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Color.White,
            radius = 2.5f,
            center = Offset(shieldX + shieldWidth / 2f, center.y)
        )
    }
}

/**
 * 7. Flying Aerial Drone / Winged Harpy:
 * Levitating above ground level, aerodynamic flapping wings, and glowing cockpit tracking lens.
 */
private fun DrawScope.drawFlyingEnemy(center: Offset, enemy: Enemy, time: Float) {
    val hoverY = sin(time * 7f) * 2.5f
    val elevatedCenter = Offset(center.x, center.y + hoverY)
    val flap = sin(time * 24f) * 5f
    val r = enemy.spec.radius * 0.7f

    // Swept-back aerodynamic wings
    val wingPath = Path().apply {
        // Left Wing
        moveTo(elevatedCenter.x - 2f, elevatedCenter.y - 4f)
        lineTo(elevatedCenter.x - r * 1.6f, elevatedCenter.y - r * 1.8f + flap)
        lineTo(elevatedCenter.x - r * 0.8f, elevatedCenter.y - r * 0.4f)
        close()
        // Right Wing
        moveTo(elevatedCenter.x - 2f, elevatedCenter.y + 4f)
        lineTo(elevatedCenter.x - r * 1.6f, elevatedCenter.y + r * 1.8f - flap)
        lineTo(elevatedCenter.x - r * 0.8f, elevatedCenter.y + r * 0.4f)
        close()
    }
    drawPath(path = wingPath, color = Color(0xFF0284C7))
    drawPath(path = wingPath, color = Color(0xFF38BDF8), style = Stroke(width = 1.5f))

    // Sleek aerodynamic fuselage
    drawOval(
        color = Color(0xFF0F172A),
        topLeft = Offset(elevatedCenter.x - r * 1.1f, elevatedCenter.y - r * 0.65f),
        size = Size(r * 2.2f, r * 1.3f)
    )
    drawOval(
        color = Color(0xFF0284C7),
        topLeft = Offset(elevatedCenter.x - r * 0.95f, elevatedCenter.y - r * 0.52f),
        size = Size(r * 1.9f, r * 1.04f)
    )

    // Cockpit optic sensor dome
    drawCircle(color = Color(0xFF082F49), radius = r * 0.42f, center = Offset(elevatedCenter.x + r * 0.4f, elevatedCenter.y))
    drawCircle(color = Color(0xFF38BDF8), radius = r * 0.32f, center = Offset(elevatedCenter.x + r * 0.4f, elevatedCenter.y))
    drawCircle(color = Color.White, radius = 1.5f, center = Offset(elevatedCenter.x + r * 0.45f, elevatedCenter.y - 1f))

    // Twin jet thruster flares behind
    val thrusterPulse = sin(time * 30f) * 1.5f
    drawCircle(Color(0xFF38BDF8), radius = 2.5f + thrusterPulse, center = Offset(elevatedCenter.x - r * 1.1f, elevatedCenter.y - 3f))
    drawCircle(Color(0xFF38BDF8), radius = 2.5f + thrusterPulse, center = Offset(elevatedCenter.x - r * 1.1f, elevatedCenter.y + 3f))
    drawCircle(Color.White, radius = 1.2f, center = Offset(elevatedCenter.x - r * 1.1f, elevatedCenter.y - 3f))
    drawCircle(Color.White, radius = 1.2f, center = Offset(elevatedCenter.x - r * 1.1f, elevatedCenter.y + 3f))
}

/**
 * 8. Healer / Combat Shaman Enemy:
 * Cloaked emerald robes with gold runes, ivory staff with a revolving healing gem, and healing aura.
 */
private fun DrawScope.drawHealerEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 1.5f)
    val bodyR = enemy.spec.radius * 0.72f

    // Forest green flowing robes
    drawCircle(color = Color(0xFF064E3B), radius = bodyR, center = center)
    drawCircle(color = Color(0xFF059669), radius = bodyR - 1.5f, center = center)
    drawCircle(color = Color(0xFF10B981), radius = bodyR * 0.60f, center = Offset(center.x - 1f, center.y))

    // Hooded cowl & golden trim
    drawCircle(color = Color(0xFF047857), radius = bodyR * 0.6f, center = Offset(center.x + 2f, center.y))
    drawCircle(color = Color(0xFFFDE047), radius = bodyR * 0.58f, center = Offset(center.x + 2f, center.y), style = Stroke(width = 1.2f))

    // Medical / Shamanic Cross Emblem on robe
    val crossSize = 3.5f
    drawLine(Color.White, Offset(center.x - 1f - crossSize, center.y), Offset(center.x - 1f + crossSize, center.y), strokeWidth = 2f)
    drawLine(Color.White, Offset(center.x - 1f, center.y - crossSize), Offset(center.x - 1f, center.y + crossSize), strokeWidth = 2f)

    // Ivory Healing Staff with glowing emerald crystal
    val staffX = center.x + bodyR * 0.8f
    val staffY = center.y - bodyR * 0.6f
    drawLine(
        color = Color(0xFFFEF3C7),
        start = Offset(staffX - 2f, staffY + 12f),
        end = Offset(staffX + 4f, staffY - 6f),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round
    )
    // Rotating pulsing emerald orb
    val orbPulse = sin(time * 8f) * 1.5f
    drawCircle(color = Color(0xFF047857), radius = 4f + orbPulse, center = Offset(staffX + 4f, staffY - 6f))
    drawCircle(color = Color(0xFF34D399), radius = 3f + orbPulse, center = Offset(staffX + 4f, staffY - 6f))
    drawCircle(color = Color.White, radius = 1.5f, center = Offset(staffX + 3.5f, staffY - 6.5f))
}

/**
 * 9. Summoner / Void Warlock Enemy:
 * Midnight obsidian robes, runic horn cowl, rotating dark void catalyst focus.
 */
private fun DrawScope.drawSummonerEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 1.3f)
    val bodyR = enemy.spec.radius * 0.72f

    // Dark obsidian robes
    drawCircle(color = Color(0xFF1E1B4B), radius = bodyR, center = center)
    drawCircle(color = Color(0xFF4C1D95), radius = bodyR - 1.5f, center = center)
    drawCircle(color = Color(0xFF6D28D9), radius = bodyR * 0.65f, center = Offset(center.x - 1f, center.y))

    // Horned cowl with glowing magenta gaze
    drawCircle(color = Color(0xFF2E1065), radius = bodyR * 0.55f, center = Offset(center.x + 2f, center.y))
    drawCircle(color = Color(0xFFC084FC), radius = 1.8f, center = Offset(center.x + 4.5f, center.y - 2.5f))
    drawCircle(color = Color(0xFFC084FC), radius = 1.8f, center = Offset(center.x + 4.5f, center.y + 2.5f))

    // Floating Void Focus Orb
    val orbX = center.x + bodyR * 0.9f
    val orbY = center.y + sin(time * 5f) * 3f
    val orbR = 4.5f
    drawCircle(color = Color(0xFF3B0764), radius = orbR + 2f, center = Offset(orbX, orbY))
    drawCircle(color = Color(0xFF9333EA), radius = orbR, center = Offset(orbX, orbY))
    drawCircle(color = Color(0xFFF472B6), radius = orbR * 0.6f, center = Offset(orbX, orbY))
    drawCircle(color = Color.White, radius = 1.5f, center = Offset(orbX - 1f, orbY - 1f))

    // Orbiting dark runes
    for (i in 0..2) {
        val rAng = (i * 2.094f + time * 3f).toFloat()
        val rx = orbX + cos(rAng) * 8f
        val ry = orbY + sin(rAng) * 8f
        drawCircle(color = Color(0xFFC084FC), radius = 1.5f, center = Offset(rx, ry))
    }
}

/**
 * 10. Stealth Stalker Enemy:
 * Sleek midnight ninja operative, phantom cloaking field, dual energized daggers.
 */
private fun DrawScope.drawStealthEnemy(center: Offset, enemy: Enemy, time: Float) {
    val stride = sin(enemy.animWobbleTime * 2.0f)
    val bodyR = enemy.spec.radius * 0.70f

    // Aerodynamic dark shinobi bodysuit
    drawCircle(color = Color(0xFF09090B), radius = bodyR, center = center)
    drawCircle(color = Color(0xFF18181B), radius = bodyR - 1.5f, center = center)
    drawCircle(color = Color(0xFF27272A), radius = bodyR * 0.65f, center = Offset(center.x - 1f, center.y))

    // Stealth cowl & glowing violet eye slits
    drawCircle(color = Color(0xFF09090B), radius = bodyR * 0.5f, center = Offset(center.x + 2f, center.y))
    drawRoundRect(
        color = Color(0xFFA855F7),
        topLeft = Offset(center.x + 4f, center.y - 3f),
        size = Size(2.5f, 1.8f),
        cornerRadius = CornerRadius(0.5f, 0.5f)
    )
    drawRoundRect(
        color = Color(0xFFA855F7),
        topLeft = Offset(center.x + 4f, center.y + 1.2f),
        size = Size(2.5f, 1.8f),
        cornerRadius = CornerRadius(0.5f, 0.5f)
    )

    // Dual reverse-grip phantom daggers
    val daggerLen = 8f
    drawLine(
        color = Color(0xFFC084FC),
        start = Offset(center.x + 1f, center.y - 7f),
        end = Offset(center.x + 1f + daggerLen, center.y - 8f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFC084FC),
        start = Offset(center.x + 1f, center.y + 7f),
        end = Offset(center.x + 1f + daggerLen, center.y + 8f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawProjectiles(projectiles: List<Projectile>, env: EnvironmentType = EnvironmentType.GREEN_VALLEY) {
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
                    if (env == EnvironmentType.NIGHT_FORTRESS) {
                        // High-contrast visible nocturnal bullet trail
                        drawLine(
                            color = Color(0x66F59E0B),
                            start = Offset(center.x - 26f, center.y),
                            end = Offset(center.x - 2f, center.y),
                            strokeWidth = 4.5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xDDFFFBEB),
                            start = Offset(center.x - 16f, center.y),
                            end = Offset(center.x, center.y),
                            strokeWidth = 2.4f,
                            cap = StrokeCap.Round
                        )
                    } else {
                        // Standard motion streak behind bullet
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
                    }

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
                    if (env == EnvironmentType.NIGHT_FORTRESS) {
                        // Glowing nocturnal fire ember trail
                        drawCircle(color = Color(0x55F97316), radius = 9f, center = Offset(center.x - 14f, center.y))
                        drawCircle(color = Color(0x99FEF08A), radius = 4f, center = Offset(center.x - 8f, center.y))
                    }

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
                    if (env == EnvironmentType.NIGHT_FORTRESS) {
                        // High-velocity glowing tracer dart streak for night visibility
                        drawLine(
                            color = Color(0x6638BDF8),
                            start = Offset(center.x - 28f, center.y),
                            end = Offset(center.x - 2f, center.y),
                            strokeWidth = 5.5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFBAE6FD),
                            start = Offset(center.x - 18f, center.y),
                            end = Offset(center.x + 2f, center.y),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }

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
            ProjectileType.CRYO_ORB -> {
                // Sub-zero Cryogenic Orb: glowing cyan frost core, crystal ice shards & cold vapor trail
                rotate(degrees = angle, pivot = center) {
                    // Frost mist vapor trail
                    drawLine(
                        color = Color(0x550284C7),
                        start = Offset(center.x - 20f, center.y),
                        end = Offset(center.x - 2f, center.y),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0x9938BDF8),
                        start = Offset(center.x - 14f, center.y),
                        end = Offset(center.x, center.y),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFE0F2FE),
                        start = Offset(center.x - 8f, center.y),
                        end = Offset(center.x + 2f, center.y),
                        strokeWidth = 1.8f,
                        cap = StrokeCap.Round
                    )

                    // Trailing frost mist particles
                    drawCircle(Color(0x8867E8F9), radius = 3.2f, center = Offset(center.x - 11f, center.y - 2.5f))
                    drawCircle(Color(0x88BAE6FD), radius = 2.4f, center = Offset(center.x - 15f, center.y + 2f))

                    // Solid icy crystalline core with cold specular glow
                    drawCircle(color = Color(0xFF082F49), radius = 7f, center = center)
                    drawCircle(color = Color(0xFF0284C7), radius = 5.8f, center = center)
                    drawCircle(color = Color(0xFF38BDF8), radius = 4.2f, center = Offset(center.x + 0.5f, center.y - 0.5f))
                    drawCircle(color = Color(0xFFE0F2FE), radius = 2.2f, center = Offset(center.x + 1f, center.y - 1f))
                    drawCircle(color = Color.White, radius = 1.2f, center = Offset(center.x + 1.2f, center.y - 1.2f))
                }
            }
        }
    }
}

private fun DrawScope.drawVisualEffects(effects: List<VisualEffect>, env: EnvironmentType = EnvironmentType.GREEN_VALLEY) {
    for (fx in effects) {
        val center = Offset(fx.position.x, fx.position.y)
        val alpha = (1f - fx.progress).coerceIn(0f, 1f)

        when (fx.type) {
            EffectType.CANNON_EXPLOSION -> {
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)

                // High-visibility nocturnal blast flash & shockwave ring in Night Fortress
                if (env == EnvironmentType.NIGHT_FORTRESS) {
                    drawCircle(
                        color = Color(0x66EA580C).copy(alpha = alpha * 0.65f),
                        radius = currentR * 1.35f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xAAFEF08A).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.60f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.95f),
                        radius = currentR * 0.30f,
                        center = center
                    )
                }

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
            EffectType.FROST_BURST -> {
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)

                // Sub-zero frost ground blast
                drawCircle(
                    color = Color(0x4406B6D4).copy(alpha = alpha * 0.5f),
                    radius = currentR * 0.95f,
                    center = center
                )

                // Expanding freezing shockwave ring
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = alpha * 0.85f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                drawCircle(
                    color = Color(0xFFBAE6FD).copy(alpha = alpha * 0.6f),
                    radius = currentR * 0.72f,
                    center = center,
                    style = Stroke(width = 1.8f * alpha)
                )

                // Shimmering ice shards bursting radially outwards
                for (s in 0..5) {
                    val sAng = (s * Math.PI / 3.0).toFloat() + fx.progress * 2.2f
                    val sDist = currentR * (0.6f + fx.progress * 0.5f)
                    val sx = center.x + cos(sAng) * sDist
                    val sy = center.y + sin(sAng) * sDist
                    drawCircle(color = Color(0xFFE0F2FE).copy(alpha = alpha), radius = 3f * alpha, center = Offset(sx, sy))
                    drawCircle(color = Color(0xFF0284C7).copy(alpha = alpha), radius = 1.6f * alpha, center = Offset(sx, sy))
                }

                // Cold cyan center flash
                drawCircle(
                    color = Color(0xFF67E8F9).copy(alpha = alpha),
                    radius = currentR * 0.48f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.25f,
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
                // Scout: Nimble pop - puff of forest dust, canvas satchel unravel, leather scraps and copper coins
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                // Light dust cloudlets
                for (i in 0..3) {
                    val ang = i * 1.57f + fx.progress * 1.4f
                    val px = center.x + cos(ang) * (currentR * 0.6f)
                    val py = center.y + sin(ang) * (currentR * 0.6f)
                    drawCircle(
                        color = Color(0xFFFEF3C7).copy(alpha = alpha * 0.75f),
                        radius = currentR * 0.38f,
                        center = Offset(px, py)
                    )
                }
                // Center burst flash
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha * 0.85f),
                    radius = currentR * 0.30f,
                    center = center
                )
                // Flying leather scraps & courier satchel fragments
                for (s in 0..3) {
                    val ang = s * 1.57f + fx.progress * 3.2f
                    val dist = currentR * (0.4f + fx.progress * 0.65f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF92400E).copy(alpha = alpha),
                        topLeft = Offset(sx - 2f, sy - 1.5f),
                        size = Size(4f, 3f),
                        cornerRadius = CornerRadius(0.8f, 0.8f)
                    )
                }
                // Spilled shiny copper scout coins
                for (c in 0..2) {
                    val ang = c * 2.09f + 0.4f + fx.progress * 2.5f
                    val dist = currentR * (0.3f + fx.progress * 0.7f)
                    val cx = center.x + cos(ang) * dist
                    val cy = center.y + sin(ang) * dist
                    drawCircle(Color(0xFFF59E0B).copy(alpha = alpha), radius = 2.2f * alpha, center = Offset(cx, cy))
                    drawCircle(Color(0xFFFEF08A).copy(alpha = alpha), radius = 1.0f * alpha, center = Offset(cx, cy))
                }
            }
            EffectType.RUNNER_DEATH -> {
                // Runner: High-speed electric implode-explode - cyan vapor plume, buzzing sparks, and shattered dagger shards
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)
                // Expanding sonic cyan shockwave ring
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = alpha * 0.8f),
                    radius = currentR * 0.9f,
                    center = center,
                    style = Stroke(width = 2.5f * alpha)
                )
                // Aerodynamic wind vapor trails in 4 quadrant diagonals
                for (i in 0..3) {
                    val ang = (i * 1.57f + 0.785f) + fx.progress * 1.8f
                    val px = center.x + cos(ang) * (currentR * 0.65f)
                    val py = center.y + sin(ang) * (currentR * 0.65f)
                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = alpha * 0.6f),
                        radius = currentR * 0.32f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFFBAE6FD).copy(alpha = alpha * 0.8f),
                        radius = currentR * 0.18f,
                        center = Offset(px, py)
                    )
                }
                // Central electric plasma burst
                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = alpha),
                    radius = currentR * 0.38f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.20f,
                    center = center
                )
                // Flying shattered dagger blades (pointed triangles/streaks)
                for (d in 0..3) {
                    val ang = d * 1.57f + fx.progress * 4.0f
                    val dist = currentR * (0.45f + fx.progress * 0.65f)
                    val dx = center.x + cos(ang) * dist
                    val dy = center.y + sin(ang) * dist
                    drawLine(
                        color = Color(0xFFF1F5F9).copy(alpha = alpha),
                        start = Offset(dx, dy),
                        end = Offset(dx + cos(ang) * 5f, dy + sin(ang) * 5f),
                        strokeWidth = 2.2f * alpha,
                        cap = StrokeCap.Round
                    )
                }
            }
            EffectType.SOLDIER_DEATH -> {
                // Soldier: Disciplined armor clash - billowing gray slate smoke, splintering wooden shield chunks, tumbling steel blade fragments
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)
                // Slate battle smoke clouds
                for (i in 0..4) {
                    val ang = i * 1.25f + fx.progress * 0.9f
                    val px = center.x + cos(ang) * (currentR * 0.60f)
                    val py = center.y + sin(ang) * (currentR * 0.60f)
                    drawCircle(
                        color = Color(0xFF64748B).copy(alpha = alpha * 0.70f),
                        radius = currentR * 0.42f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFFCBD5E1).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.26f,
                        center = Offset(px, py)
                    )
                }
                // Center metallic flash
                drawCircle(
                    color = Color(0xFFF8FAFC).copy(alpha = alpha * 0.9f),
                    radius = currentR * 0.35f,
                    center = center
                )
                // Splintering wooden shield planks
                for (w in 0..2) {
                    val ang = w * 2.09f + 0.3f + fx.progress * 2.0f
                    val dist = currentR * (0.4f + fx.progress * 0.55f)
                    val wx = center.x + cos(ang) * dist
                    val wy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFFB45309).copy(alpha = alpha),
                        topLeft = Offset(wx - 3.5f, wy - 2f),
                        size = Size(7f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                }
                // Tumbling gleaming steel plate & helmet crest shrapnel
                for (s in 0..3) {
                    val ang = s * 1.57f + fx.progress * 2.8f
                    val dist = currentR * (0.5f + fx.progress * 0.6f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF334155).copy(alpha = alpha),
                        topLeft = Offset(sx - 2.5f, sy - 2.5f),
                        size = Size(5f, 5f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = alpha),
                        radius = 2.0f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.HEAVY_DEATH -> {
                // Heavy: Colossal armor shatter - dual shockwave blast, dense black coal smoke, fiery furnace embers, and heavy jagged iron slabs
                val currentR = fx.maxRadius * (0.35f + fx.progress * 0.65f)
                // Outer concussive shockwave ring
                drawCircle(
                    color = Color(0xFFEA580C).copy(alpha = alpha * 0.75f),
                    radius = currentR * 0.95f,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                // Dense dark coal & soot smoke clouds
                for (i in 0..5) {
                    val ang = i * 1.04f + fx.progress * 0.7f
                    val px = center.x + cos(ang) * (currentR * 0.62f)
                    val py = center.y + sin(ang) * (currentR * 0.62f)
                    drawCircle(
                        color = Color(0xFF0F172A).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.46f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF334155).copy(alpha = alpha * 0.65f),
                        radius = currentR * 0.32f,
                        center = Offset(px, py)
                    )
                }
                // Center fiery furnace blast
                drawCircle(
                    color = Color(0xFFEA580C).copy(alpha = alpha),
                    radius = currentR * 0.52f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = alpha),
                    radius = currentR * 0.32f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.16f,
                    center = center
                )
                // Heavy flying spiked iron slabs & war maul fragments
                for (s in 0..5) {
                    val ang = s * 1.05f + fx.progress * 2.2f
                    val dist = currentR * (0.45f + fx.progress * 0.65f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF1E293B).copy(alpha = alpha),
                        topLeft = Offset(sx - 4.5f, sy - 3f),
                        size = Size(9f, 6f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    // High-heat orange glow on shrapnel edges
                    drawCircle(
                        color = Color(0xFFF97316).copy(alpha = alpha),
                        radius = 2.5f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.BOSS_DEATH -> {
                // Boss: Cataclysmic Dread Warlord Defeat - triple ground shockwaves, apocalyptic crimson/gold blast fireball, and flying royal regalia
                val currentR = fx.maxRadius * (0.25f + fx.progress * 0.75f)

                // 1. Primary ground shatter shockwave ring
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.85f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 5.0f * alpha)
                )
                // 2. Secondary crimson war shockwave ring
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha * 0.70f),
                    radius = currentR * 0.78f,
                    center = center,
                    style = Stroke(width = 4.0f * alpha)
                )
                // 3. Inner golden champion crown shockwave ring
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha * 0.55f),
                    radius = currentR * 0.55f,
                    center = center,
                    style = Stroke(width = 2.5f * alpha)
                )

                // Billowing heavy royal blast clouds in 8 directions
                for (i in 0..7) {
                    val ang = i * 0.785f + fx.progress * 0.5f
                    val dist = currentR * 0.55f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFF450A0A).copy(alpha = alpha * 0.85f),
                        radius = currentR * 0.42f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF78350F).copy(alpha = alpha * 0.65f),
                        radius = currentR * 0.30f,
                        center = Offset(px, py)
                    )
                }

                // Core cataclysmic nuclear flash
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha),
                    radius = currentR * 0.55f,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    radius = currentR * 0.38f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.22f,
                    center = center
                )

                // Flying shattered fortress armor plates & golden crown spikes
                for (s in 0..7) {
                    val ang = s * 0.785f + fx.progress * 2.4f
                    val dist = currentR * (0.5f + fx.progress * 0.6f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    // Dark fortress plate fragment
                    drawRoundRect(
                        color = Color(0xFF0F172A).copy(alpha = alpha),
                        topLeft = Offset(sx - 5f, sy - 3.5f),
                        size = Size(10f, 7f),
                        cornerRadius = CornerRadius(1.5f, 1.5f)
                    )
                    // Radiant golden crown spark
                    drawCircle(
                        color = Color(0xFFFFD166).copy(alpha = alpha),
                        radius = 4.0f * alpha,
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 1.8f * alpha,
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
            EffectType.DESTRUCTIBLE_HIT -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha * 0.9f),
                    radius = currentR * 0.35f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = currentR * 0.18f,
                    center = center
                )
                for (i in 0..4) {
                    val ang = i * 1.25f + fx.progress * 2f
                    val dist = currentR * (0.5f + fx.progress * 0.6f)
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = alpha * 0.75f),
                        radius = 2.5f * alpha,
                        center = Offset(px, py)
                    )
                }
            }
            EffectType.DESTRUCTIBLE_DEBRIS -> {
                val currentR = fx.maxRadius * (0.25f + fx.progress * 0.75f)
                for (i in 0..5) {
                    val ang = i * 1.047f + fx.progress * 0.6f
                    val dist = currentR * 0.55f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFFCBD5E1).copy(alpha = alpha * 0.65f),
                        radius = currentR * 0.32f,
                        center = Offset(px, py)
                    )
                }
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.85f),
                    radius = currentR * 0.35f,
                    center = center
                )
                for (s in 0..5) {
                    val ang = s * 1.047f + fx.progress * 3.5f
                    val dist = currentR * (0.4f + fx.progress * 0.8f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF78350F).copy(alpha = alpha),
                        topLeft = Offset(sx - 3f, sy - 2f),
                        size = Size(6f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                    drawCircle(
                        color = Color(0xFFFACC15).copy(alpha = alpha),
                        radius = 1.5f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.FLOATING_TOKEN -> {
                val floatY = center.y - (fx.progress * 36f)
                val coinCenter = Offset(center.x, floatY)

                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.35f),
                    radius = 18f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color(0xFFB45309).copy(alpha = alpha),
                    radius = 12f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha),
                    radius = 10f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha),
                    radius = 8f,
                    center = Offset(coinCenter.x - 1f, coinCenter.y - 1f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 3f,
                    center = Offset(coinCenter.x - 2f, coinCenter.y - 2f)
                )

                for (i in 0..3) {
                    val sAng = i * 1.57f + fx.progress * 4f
                    val sDist = 14f + fx.progress * 8f
                    val sx = coinCenter.x + cos(sAng) * sDist
                    val sy = coinCenter.y + sin(sAng) * sDist
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = alpha),
                        radius = 2f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.BUILD_CONSTRUCTION_DUST -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                drawCircle(
                    color = Color(0xFFBAE6FD).copy(alpha = alpha * 0.5f),
                    radius = currentR * 0.7f,
                    center = center,
                    style = Stroke(width = 2f * alpha)
                )
                for (i in 0..7) {
                    val ang = (i * Math.PI / 4.0).toFloat() + fx.progress * 0.4f
                    val dist = currentR * (0.6f + fx.progress * 0.4f)
                    val dx = center.x + cos(ang) * dist
                    val dy = center.y + sin(ang) * dist
                    drawCircle(
                        color = Color(0xFF94A3B8).copy(alpha = alpha * 0.55f),
                        radius = (8f + fx.progress * 10f) * alpha,
                        center = Offset(dx, dy)
                    )
                }
                for (s in 0..5) {
                    val sAng = s * 1.05f + fx.progress * 5f
                    val sDist = currentR * (0.4f + fx.progress * 0.8f)
                    val sx = center.x + cos(sAng) * sDist
                    val sy = center.y + sin(sAng) * sDist
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = alpha),
                        radius = 2.5f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.PURCHASE_COIN_BURST -> {
                val floatY = center.y - (fx.progress * 42f)
                val coinCenter = Offset(center.x, floatY)
                drawCircle(
                    color = Color(0xFFF59E0B).copy(alpha = alpha * 0.5f),
                    radius = 16f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color(0xFFD97706).copy(alpha = alpha),
                    radius = 11f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color(0xFFFBBF24).copy(alpha = alpha),
                    radius = 9f,
                    center = coinCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = 3f,
                    center = Offset(coinCenter.x - 2f, coinCenter.y - 2f)
                )
                for (i in 0..4) {
                    val sAng = (i * Math.PI * 2 / 5.0).toFloat() + fx.progress * 3f
                    val sDist = 14f + fx.progress * 18f
                    val sx = coinCenter.x + cos(sAng) * sDist
                    val sy = coinCenter.y + sin(sAng) * sDist
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = alpha),
                        radius = 2.5f * alpha,
                        center = Offset(sx, sy)
                    )
                }
            }
            EffectType.SPEED_BURST_TRAIL -> {
                val currentR = fx.maxRadius * (0.4f + fx.progress * 0.6f)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = alpha * 0.7f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 2.5f * alpha)
                )
                for (i in 0..3) {
                    val ang = i * 1.57f + fx.progress * 3f
                    val px = center.x + cos(ang) * (currentR * 0.7f)
                    val py = center.y + sin(ang) * (currentR * 0.7f)
                    drawCircle(Color(0xFFE0F2FE).copy(alpha = alpha), radius = 2f * alpha, center = Offset(px, py))
                }
            }
            EffectType.SHIELD_SHATTER -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF06B6D4).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                // Flying shattered shield shards
                for (s in 0..5) {
                    val ang = s * 1.047f + fx.progress * 2.5f
                    val dist = currentR * (0.5f + fx.progress * 0.7f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFFBAE6FD).copy(alpha = alpha),
                        topLeft = Offset(sx - 3f, sy - 2f),
                        size = Size(6f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                }
            }
            EffectType.ARMOR_CRACK_BURST -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = alpha * 0.85f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )
                for (c in 0..4) {
                    val ang = c * 1.256f + fx.progress * 3f
                    val dist = currentR * (0.4f + fx.progress * 0.6f)
                    val cx = center.x + cos(ang) * dist
                    val cy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF475569).copy(alpha = alpha),
                        topLeft = Offset(cx - 3.5f, cy - 2f),
                        size = Size(7f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                    drawCircle(Color(0xFFFDE047).copy(alpha = alpha), radius = 2f * alpha, center = Offset(cx, cy))
                }
            }
            EffectType.HEAL_WAVE -> {
                val currentR = fx.maxRadius * (0.2f + fx.progress * 0.8f)
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = alpha * 0.75f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )
                drawCircle(
                    color = Color(0xFF34D399).copy(alpha = alpha * 0.35f),
                    radius = currentR * 0.85f,
                    center = center
                )
                // Restorative crosses
                for (c in 0..3) {
                    val ang = c * 1.57f + fx.progress * 1.5f
                    val dist = currentR * 0.6f
                    val cx = center.x + cos(ang) * dist
                    val cy = center.y + sin(ang) * dist
                    drawLine(Color.White.copy(alpha = alpha), Offset(cx - 3f, cy), Offset(cx + 3f, cy), strokeWidth = 1.8f)
                    drawLine(Color.White.copy(alpha = alpha), Offset(cx, cy - 3f), Offset(cx, cy + 3f), strokeWidth = 1.8f)
                }
            }
            EffectType.SUMMON_RIFT -> {
                val currentR = fx.maxRadius * (0.2f + fx.progress * 0.8f)
                drawCircle(
                    color = Color(0xFF8B5CF6).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                drawCircle(
                    color = Color(0xFF2E1065).copy(alpha = alpha * 0.6f),
                    radius = currentR * 0.75f,
                    center = center
                )
                for (r in 0..4) {
                    val ang = r * 1.256f + fx.progress * 4f
                    val rx = center.x + cos(ang) * (currentR * 0.8f)
                    val ry = center.y + sin(ang) * (currentR * 0.8f)
                    drawCircle(Color(0xFFC084FC).copy(alpha = alpha), radius = 2.5f * alpha, center = Offset(rx, ry))
                }
            }
            EffectType.STEALTH_SMOKE -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                for (i in 0..5) {
                    val ang = i * 1.047f + fx.progress * 1.2f
                    val dist = currentR * 0.5f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(Color(0xFF3F3F46).copy(alpha = alpha * 0.65f), radius = currentR * 0.35f, center = Offset(px, py))
                    drawCircle(Color(0xFFA855F7).copy(alpha = alpha * 0.4f), radius = currentR * 0.2f, center = Offset(px, py))
                }
            }
            EffectType.BOSS_SHOCKWAVE_RING -> {
                val currentR = fx.maxRadius * (0.1f + fx.progress * 0.9f)
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = alpha * 0.85f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 5f * alpha)
                )
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = alpha * 0.5f),
                    radius = currentR * 0.85f,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )
                drawCircle(
                    color = Color(0xFFFDE047).copy(alpha = alpha * 0.3f),
                    radius = currentR * 0.5f,
                    center = center
                )
            }
            EffectType.BOSS_PHASE_FLASH -> {
                val currentR = fx.maxRadius * (0.2f + fx.progress * 0.8f)
                drawCircle(
                    color = Color(0xFFFACC15).copy(alpha = alpha),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 4f * alpha)
                )
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = currentR * 0.5f,
                    center = center
                )
            }
            EffectType.BOSS_FOOTSTEP_DUST -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0x77CBD5E1).copy(alpha = alpha * 0.5f),
                    radius = currentR,
                    center = center
                )
            }
            EffectType.SHIELD_DEATH -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF0284C7).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )
                for (s in 0..4) {
                    val ang = s * 1.256f + fx.progress * 2.5f
                    val dist = currentR * (0.4f + fx.progress * 0.6f)
                    val sx = center.x + cos(ang) * dist
                    val sy = center.y + sin(ang) * dist
                    drawRoundRect(
                        color = Color(0xFF38BDF8).copy(alpha = alpha),
                        topLeft = Offset(sx - 3f, sy - 2f),
                        size = Size(6f, 4f),
                        cornerRadius = CornerRadius(1f, 1f)
                    )
                }
            }
            EffectType.FLYING_DEATH -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 2.5f * alpha)
                )
                for (w in 0..3) {
                    val ang = w * 1.57f + fx.progress * 3.5f
                    val dist = currentR * 0.6f
                    val wx = center.x + cos(ang) * dist
                    val wy = center.y + sin(ang) * dist
                    drawCircle(Color(0xFFBAE6FD).copy(alpha = alpha), radius = 2.5f * alpha, center = Offset(wx, wy))
                }
            }
            EffectType.HEALER_DEATH -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3f * alpha)
                )
                for (i in 0..4) {
                    val ang = i * 1.256f + fx.progress * 2f
                    val dist = currentR * 0.6f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(Color(0xFF6EE7B7).copy(alpha = alpha), radius = 3f * alpha, center = Offset(px, py))
                }
            }
            EffectType.SUMMONER_DEATH -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                drawCircle(
                    color = Color(0xFF8B5CF6).copy(alpha = alpha * 0.8f),
                    radius = currentR,
                    center = center,
                    style = Stroke(width = 3.5f * alpha)
                )
                for (i in 0..5) {
                    val ang = i * 1.047f + fx.progress * 3f
                    val dist = currentR * 0.6f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(Color(0xFFC084FC).copy(alpha = alpha), radius = 3f * alpha, center = Offset(px, py))
                }
            }
            EffectType.STEALTH_DEATH -> {
                val currentR = fx.maxRadius * (0.3f + fx.progress * 0.7f)
                for (i in 0..5) {
                    val ang = i * 1.047f + fx.progress * 1.5f
                    val dist = currentR * 0.5f
                    val px = center.x + cos(ang) * dist
                    val py = center.y + sin(ang) * dist
                    drawCircle(Color(0xFF18181B).copy(alpha = alpha * 0.8f), radius = currentR * 0.4f, center = Offset(px, py))
                    drawCircle(Color(0xFFA855F7).copy(alpha = alpha * 0.6f), radius = currentR * 0.2f, center = Offset(px, py))
                }
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
    val fillColor = if (isValid) Color(0x2E22C55E) else Color(0x38EF4444)
    val dashPhase = (gameState.gameTime * 25f) % 20f

    // 1. Range preview with animated tactical dash
    drawCircle(color = fillColor, radius = spec.range, center = center)
    drawCircle(
        color = ringColor,
        radius = spec.range,
        center = center,
        style = Stroke(
            width = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), dashPhase)
        )
    )

    // Range Cardinal Crosshair Notches (N, S, E, W)
    val tickLen = 8f
    drawLine(ringColor, Offset(center.x, center.y - spec.range - tickLen), Offset(center.x, center.y - spec.range + tickLen), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x, center.y + spec.range - tickLen), Offset(center.x, center.y + spec.range + tickLen), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x - spec.range - tickLen, center.y), Offset(center.x - spec.range + tickLen, center.y), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x + spec.range - tickLen, center.y), Offset(center.x + spec.range + tickLen, center.y), strokeWidth = 2f)

    // 2. Tower Footprint Base
    val footR = spec.size / 2f
    drawCircle(color = if (isValid) Color(0x3322C55E) else Color(0x4DEF4444), radius = footR, center = center)
    drawCircle(color = ringColor, radius = footR, center = center, style = Stroke(width = 2.5f))

    // Tactical Corner Brackets
    val bSize = footR * 1.15f
    val bArm = 10f
    val bracketColor = if (isValid) Color(0xFF4ADE80) else Color(0xFFF87171)

    // Top-Left
    drawLine(bracketColor, Offset(center.x - bSize, center.y - bSize), Offset(center.x - bSize + bArm, center.y - bSize), strokeWidth = 2.5f)
    drawLine(bracketColor, Offset(center.x - bSize, center.y - bSize), Offset(center.x - bSize, center.y - bSize + bArm), strokeWidth = 2.5f)
    // Top-Right
    drawLine(bracketColor, Offset(center.x + bSize, center.y - bSize), Offset(center.x + bSize - bArm, center.y - bSize), strokeWidth = 2.5f)
    drawLine(bracketColor, Offset(center.x + bSize, center.y - bSize), Offset(center.x + bSize, center.y - bSize + bArm), strokeWidth = 2.5f)
    // Bottom-Left
    drawLine(bracketColor, Offset(center.x - bSize, center.y + bSize), Offset(center.x - bSize + bArm, center.y + bSize), strokeWidth = 2.5f)
    drawLine(bracketColor, Offset(center.x - bSize, center.y + bSize), Offset(center.x - bSize, center.y + bSize - bArm), strokeWidth = 2.5f)
    // Bottom-Right
    drawLine(bracketColor, Offset(center.x + bSize, center.y + bSize), Offset(center.x + bSize - bArm, center.y + bSize), strokeWidth = 2.5f)
    drawLine(bracketColor, Offset(center.x + bSize, center.y + bSize), Offset(center.x + bSize, center.y + bSize - bArm), strokeWidth = 2.5f)

    // 3. Render Tactical Ghost Weapon or Blocked Hazard
    if (isValid) {
        WeaponArtwork.drawWeapon(
            drawScope = this,
            type = spec.type,
            cx = center.x,
            cy = center.y,
            scale = 0.52f,
            isLocked = false,
            recoilProgress = 0f,
            animTime = gameState.gameTime,
            level = 1
        )
    } else {
        // Red Diagonal Hazard Crosshatch across footprint
        for (i in -3..3) {
            val offsetVal = i * 10f
            drawLine(
                color = Color(0xAAEF4444),
                start = Offset(center.x + offsetVal - footR * 0.7f, center.y - footR * 0.7f),
                end = Offset(center.x + offsetVal + footR * 0.7f, center.y + footR * 0.7f),
                strokeWidth = 2f
            )
        }

        // Blocked 🚫 Indicator
        drawCircle(
            color = Color(0xCCEF4444),
            radius = footR * 0.6f,
            center = center,
            style = Stroke(width = 3f)
        )
        drawLine(
            color = Color(0xCCEF4444),
            start = Offset(center.x - footR * 0.42f, center.y - footR * 0.42f),
            end = Offset(center.x + footR * 0.42f, center.y + footR * 0.42f),
            strokeWidth = 3f
        )

        // Highlight nearby colliding obstacles (trees, rocks, stones)
        val pulse = (kotlin.math.sin(gameState.gameTime * 8f) * 0.5f + 0.5f)
        for (dest in gameState.destructibles) {
            if (!dest.isAlive) continue
            val dist = kotlin.math.hypot(dest.position.x - pos.x, dest.position.y - pos.y)
            if (dist < footR + dest.radius + 8f) {
                drawCircle(
                    color = Color(0x44EF4444),
                    radius = dest.radius + 6f + pulse * 4f,
                    center = Offset(dest.position.x, dest.position.y)
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = dest.radius + 6f + pulse * 4f,
                    center = Offset(dest.position.x, dest.position.y),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Highlight nearby colliding towers
        for (tow in gameState.towers) {
            val dist = kotlin.math.hypot(tow.position.x - pos.x, tow.position.y - pos.y)
            if (dist < footR + tow.spec.size / 2f + 8f) {
                drawCircle(
                    color = Color(0x44EF4444),
                    radius = tow.spec.size / 2f + 6f + pulse * 4f,
                    center = Offset(tow.position.x, tow.position.y)
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = tow.spec.size / 2f + 6f + pulse * 4f,
                    center = Offset(tow.position.x, tow.position.y),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

private fun DrawScope.drawDestructibles(
    destructibles: List<DestructibleObject>,
    selectedDestructibleId: String? = null,
    towerTargetedIds: Set<String> = emptySet(),
    gameTime: Float = 0f,
    env: EnvironmentType = EnvironmentType.GREEN_VALLEY
) {
    for (obj in destructibles) {
        if (!obj.isAlive) continue
        val center = Offset(obj.position.x, obj.position.y)
        val r = obj.radius
        val isSelected = obj.id == selectedDestructibleId
        val isTowerTargeted = towerTargetedIds.contains(obj.id)

        // Drop shadow
        drawOval(
            color = Color(0x44000000),
            topLeft = Offset(center.x - r * 1.1f, center.y + r * 0.3f - r * 0.4f),
            size = Size(r * 2.2f, r * 0.8f)
        )

        // Pulsing ground selection ring when selected or targeted
        if (isSelected || isTowerTargeted) {
            val pulse = (kotlin.math.sin(gameTime * 6f) * 0.5f + 0.5f)
            val ringRadius = r * 1.35f + pulse * 3f
            drawCircle(
                color = Color(0x33EF4444),
                radius = ringRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFFEF4444),
                radius = ringRadius,
                center = center,
                style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            )
        }

        when (obj.type) {
            DestructibleType.TREE, DestructibleType.OAK_TREE -> {
                // Natural Oak Tree with detailed trunk, bark ridges, layered foliage canopy
                // Trunk
                val trunkW = r * 0.45f
                val trunkH = r * 0.8f
                drawRoundRect(
                    color = Color(0xFF5A3825),
                    topLeft = Offset(center.x - trunkW / 2f, center.y - trunkH * 0.2f),
                    size = Size(trunkW, trunkH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Bark detail lines
                drawLine(
                    color = Color(0xFF3D2314),
                    start = Offset(center.x - 2f, center.y),
                    end = Offset(center.x - 2f, center.y + trunkH * 0.5f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF78472A),
                    start = Offset(center.x + 3f, center.y + 2f),
                    end = Offset(center.x + 3f, center.y + trunkH * 0.45f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )

                // Foliage canopy cluster (3 natural puffy tiers)
                drawCircle(color = Color(0xFF1E4620), radius = r * 0.9f, center = Offset(center.x, center.y - r * 0.35f))
                drawCircle(color = Color(0xFF2D5A27), radius = r * 0.75f, center = Offset(center.x - r * 0.25f, center.y - r * 0.45f))
                drawCircle(color = Color(0xFF3F7D37), radius = r * 0.65f, center = Offset(center.x + r * 0.22f, center.y - r * 0.42f))
                drawCircle(color = Color(0xFF5A9E4B), radius = r * 0.45f, center = Offset(center.x - r * 0.1f, center.y - r * 0.6f))
                // Sunlit highlight
                drawCircle(color = Color(0xFF7CB342), radius = r * 0.25f, center = Offset(center.x - r * 0.2f, center.y - r * 0.7f))
                if (env == EnvironmentType.SNOW_VALLEY) {
                    drawCircle(color = Color(0xFFCBD5E1), radius = r * 0.45f, center = Offset(center.x - r * 0.1f, center.y - r * 0.65f))
                    drawCircle(color = Color(0xFFF8FAFC), radius = r * 0.35f, center = Offset(center.x - r * 0.15f, center.y - r * 0.7f))
                } else if (env == EnvironmentType.NIGHT_FORTRESS) {
                    drawCircle(color = Color(0x66BAE6FD), radius = r * 0.35f, center = Offset(center.x - r * 0.15f, center.y - r * 0.65f))
                }
            }
            DestructibleType.PINE_TREE -> {
                // Evergreen pine tree with layered triangular boughs
                val trunkW = r * 0.35f
                val trunkH = r * 0.65f
                drawRoundRect(
                    color = Color(0xFF4A2E1B),
                    topLeft = Offset(center.x - trunkW / 2f, center.y + r * 0.1f),
                    size = Size(trunkW, trunkH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
                // Bottom tier
                drawPineTier(center.x, center.y + r * 0.3f, r * 0.95f, Color(0xFF1B381E))
                drawPineTier(center.x, center.y + r * 0.25f, r * 0.85f, Color(0xFF234C28))
                // Middle tier
                drawPineTier(center.x, center.y - r * 0.1f, r * 0.75f, Color(0xFF2C5E32))
                drawPineTier(center.x, center.y - r * 0.15f, r * 0.65f, Color(0xFF38723F))
                // Top tier
                drawPineTier(center.x, center.y - r * 0.5f, r * 0.5f, Color(0xFF45884E))
                drawPineTier(center.x, center.y - r * 0.55f, r * 0.38f, Color(0xFF5AA664))
                if (env == EnvironmentType.SNOW_VALLEY) {
                    drawPineSnowTier(center.x, center.y + r * 0.22f, r * 0.78f)
                    drawPineSnowTier(center.x, center.y - r * 0.16f, r * 0.60f)
                    drawPineSnowTier(center.x, center.y - r * 0.56f, r * 0.35f)
                } else if (env == EnvironmentType.NIGHT_FORTRESS) {
                    drawCircle(color = Color(0x55BAE6FD), radius = r * 0.32f, center = Offset(center.x, center.y - r * 0.55f))
                }
            }
            DestructibleType.STONE, DestructibleType.SMALL_STONE, DestructibleType.LARGE_STONE, DestructibleType.LARGE_BOULDER -> {
                // Natural multi-faceted rock/boulder with highlights and cracks
                val isLarge = obj.type == DestructibleType.LARGE_STONE || obj.type == DestructibleType.LARGE_BOULDER
                val rockW = r * 2f
                val rockH = r * 1.5f
                val rockColor = if (isLarge) Color(0xFF475569) else Color(0xFF64748B)
                val highlightColor = if (isLarge) Color(0xFF64748B) else Color(0xFF94A3B8)
                val darkColor = Color(0xFF334155)

                // Main body
                drawRoundRect(
                    color = darkColor,
                    topLeft = Offset(center.x - rockW * 0.5f, center.y - rockH * 0.5f + 2f),
                    size = Size(rockW, rockH),
                    cornerRadius = CornerRadius(r * 0.45f, r * 0.4f)
                )
                drawRoundRect(
                    color = rockColor,
                    topLeft = Offset(center.x - rockW * 0.5f, center.y - rockH * 0.5f),
                    size = Size(rockW, rockH),
                    cornerRadius = CornerRadius(r * 0.45f, r * 0.4f)
                )
                // Upper-left sunlit facet
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(center.x - rockW * 0.4f, center.y - rockH * 0.42f),
                    size = Size(rockW * 0.55f, rockH * 0.45f),
                    cornerRadius = CornerRadius(r * 0.3f, r * 0.3f)
                )
                // Craggy rock fissure/crevice
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(center.x - r * 0.2f, center.y - r * 0.2f),
                    end = Offset(center.x + r * 0.3f, center.y + r * 0.15f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                if (isLarge) {
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(center.x + r * 0.1f, center.y - r * 0.3f),
                        end = Offset(center.x + r * 0.4f, center.y - r * 0.1f),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round
                    )
                }
                // Snow cap on top of stone in Snow Valley
                if (env == EnvironmentType.SNOW_VALLEY) {
                    val capW = rockW * 0.85f
                    val capH = rockH * 0.35f
                    drawRoundRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(center.x - capW / 2f, center.y - rockH * 0.5f - 2f),
                        size = Size(capW, capH),
                        cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(center.x - capW * 0.4f, center.y - rockH * 0.5f - 4f),
                        size = Size(capW * 0.8f, capH * 0.75f),
                        cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                    )
                } else if (env == EnvironmentType.NIGHT_FORTRESS) {
                    // Moonlit edge specular sheen
                    val capW = rockW * 0.7f
                    val capH = rockH * 0.22f
                    drawRoundRect(
                        color = Color(0x77BAE6FD),
                        topLeft = Offset(center.x - capW * 0.45f, center.y - rockH * 0.48f),
                        size = Size(capW, capH),
                        cornerRadius = CornerRadius(capH / 2f, capH / 2f)
                    )
                }
            }
            DestructibleType.WOODEN_CRATE, DestructibleType.REINFORCED_CRATE -> {
                // Wooden supply crate with iron corners, cross bracing, and plank seams
                val isReinforced = obj.type == DestructibleType.REINFORCED_CRATE
                val boxW = r * 1.8f
                val boxH = r * 1.8f
                val boxLeft = center.x - boxW / 2f
                val boxTop = center.y - boxH / 2f

                // Drop shadow frame
                drawRoundRect(
                    color = if (isReinforced) Color(0xFF1E293B) else Color(0xFF451A03),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Main wood planks
                drawRoundRect(
                    color = if (isReinforced) Color(0xFF475569) else Color(0xFF92400E),
                    topLeft = Offset(boxLeft + 2f, boxTop + 2f),
                    size = Size(boxW - 4f, boxH - 4f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = if (isReinforced) Color(0xFF64748B) else Color(0xFFB45309),
                    topLeft = Offset(boxLeft + 4f, boxTop + 4f),
                    size = Size(boxW - 8f, boxH - 8f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Diagonal cross-brace timbers
                drawLine(
                    color = if (isReinforced) Color(0xFF334155) else Color(0xFF78350F),
                    start = Offset(boxLeft + 5f, boxTop + 5f),
                    end = Offset(boxLeft + boxW - 5f, boxTop + boxH - 5f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = if (isReinforced) Color(0xFF334155) else Color(0xFF78350F),
                    start = Offset(boxLeft + boxW - 5f, boxTop + 5f),
                    end = Offset(boxLeft + 5f, boxTop + boxH - 5f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Square
                )

                // Iron reinforced corner brackets & rivets
                val ironColor = if (isReinforced) Color(0xFF0F172A) else Color(0xFF334155)
                val rivetColor = if (isReinforced) Color(0xFF94A3B8) else Color(0xFFCBD5E1)
                val cornerSize = 7f
                // Top-left
                drawRect(ironColor, Offset(boxLeft, boxTop), Size(cornerSize, 3f))
                drawRect(ironColor, Offset(boxLeft, boxTop), Size(3f, cornerSize))
                drawCircle(rivetColor, 1.2f, Offset(boxLeft + 2f, boxTop + 2f))
                // Top-right
                drawRect(ironColor, Offset(boxLeft + boxW - cornerSize, boxTop), Size(cornerSize, 3f))
                drawRect(ironColor, Offset(boxLeft + boxW - 3f, boxTop), Size(3f, cornerSize))
                drawCircle(rivetColor, 1.2f, Offset(boxLeft + boxW - 2f, boxTop + 2f))
                // Bottom-left
                drawRect(ironColor, Offset(boxLeft, boxTop + boxH - 3f), Size(cornerSize, 3f))
                drawRect(ironColor, Offset(boxLeft, boxTop + boxH - cornerSize), Size(3f, cornerSize))
                drawCircle(rivetColor, 1.2f, Offset(boxLeft + 2f, boxTop + boxH - 2f))
                // Bottom-right
                drawRect(ironColor, Offset(boxLeft + boxW - cornerSize, boxTop + boxH - 3f), Size(cornerSize, 3f))
                drawRect(ironColor, Offset(boxLeft + boxW - 3f, boxTop + boxH - cornerSize), Size(3f, cornerSize))
                drawCircle(rivetColor, 1.2f, Offset(boxLeft + boxW - 2f, boxTop + boxH - 2f))

                // Snow blanket across crate top in Snow Valley
                if (env == EnvironmentType.SNOW_VALLEY) {
                    drawRoundRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(boxLeft - 1f, boxTop - 4f),
                        size = Size(boxW + 2f, 7f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(boxLeft + 1f, boxTop - 5f),
                        size = Size(boxW - 2f, 5f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }
            }
        }

        // Damage cracks/scratches when taking damage
        if (obj.healthFraction < 0.75f) {
            drawLine(
                color = Color(0xAA000000),
                start = Offset(center.x - r * 0.3f, center.y - r * 0.2f),
                end = Offset(center.x + r * 0.1f, center.y + r * 0.1f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
        if (obj.healthFraction < 0.4f) {
            drawLine(
                color = Color(0xAA000000),
                start = Offset(center.x - r * 0.1f, center.y + r * 0.1f),
                end = Offset(center.x + r * 0.3f, center.y - r * 0.1f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        // Hit flash overlay when recently struck
        if (obj.isHitFlashing) {
            drawCircle(
                color = Color(0x88FFFFFF),
                radius = r * 1.1f,
                center = center
            )
        }

        // Target Indicator: Small red arrow floating slightly above the object (↓)
        if (isSelected || isTowerTargeted) {
            val bob = kotlin.math.sin(gameTime * 7f) * 4f
            val arrowTipY = center.y - r - 12f + bob
            val arrowHeight = 14f
            val arrowTopY = arrowTipY - arrowHeight
            val headSize = 7f

            // Red arrow stem
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(center.x, arrowTopY),
                end = Offset(center.x, arrowTipY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            // Red arrow head pointing down (↓)
            val arrowHead = Path().apply {
                moveTo(center.x, arrowTipY)
                lineTo(center.x - headSize, arrowTipY - headSize * 1.1f)
                lineTo(center.x + headSize, arrowTipY - headSize * 1.1f)
                close()
            }
            drawPath(arrowHead, color = Color(0xFFEF4444))
        }

        // Health Bar (shown when damaged, selected, or targeted)
        if (obj.healthFraction < 1.0f || isSelected || isTowerTargeted) {
            val barW = (r * 2f).coerceAtLeast(32f)
            val barH = 5f
            val barLeft = center.x - barW / 2f
            val barTop = center.y - r - 6f

            // Outer dark frame
            drawRoundRect(
                color = Color(0xDD0F172A),
                topLeft = Offset(barLeft - 1.5f, barTop - 1.5f),
                size = Size(barW + 3f, barH + 3f),
                cornerRadius = CornerRadius(2.5f, 2.5f)
            )
            // Empty background
            drawRoundRect(
                color = Color(0xFF450A0A),
                topLeft = Offset(barLeft, barTop),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Filled HP
            val hpColor = when {
                obj.healthFraction > 0.5f -> Color(0xFF22C55E)
                obj.healthFraction > 0.25f -> Color(0xFFEAB308)
                else -> Color(0xFFEF4444)
            }
            drawRoundRect(
                color = hpColor,
                topLeft = Offset(barLeft, barTop),
                size = Size((barW * obj.healthFraction).coerceAtLeast(1f), barH),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}
