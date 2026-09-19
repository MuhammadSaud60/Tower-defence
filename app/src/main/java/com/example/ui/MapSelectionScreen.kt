package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.data.EnvironmentType
import com.example.data.GameMap
import com.example.data.ProgressionManager
import com.example.ui.components.GameIconButton
import com.example.ui.components.GameStarsRow
import kotlinx.coroutines.launch

/**
 * Professional mobile game Level Selection screen.
 * Replaces generic list cards with tactical mission dossiers:
 * - Landscape-optimized mission carousel
 * - Deep 3D tactile card chassis with press animation & audio confirmation
 * - Rich environment preview thumbnails (Snow, Night sky, Desert sands, Verdant valley)
 * - Animated star ratings and locked/unlocked state badges
 */
@Composable
fun MapSelectionScreen(
    progressionManager: ProgressionManager,
    onSelectMap: (GameMap) -> Unit,
    onBackToMenu: () -> Unit
) {
    val maps = listOf(
        GameMap.createGreenValleyMap(
            isUnlocked = progressionManager.isMapUnlocked("green_valley"),
            stars = progressionManager.getStarsForMap("green_valley")
        ),
        GameMap.createDesertOutpostMap(
            isUnlocked = progressionManager.isMapUnlocked("desert_outpost"),
            stars = progressionManager.getStarsForMap("desert_outpost")
        ),
        GameMap.createForestPassMap(
            isUnlocked = progressionManager.isMapUnlocked("forest_pass"),
            stars = progressionManager.getStarsForMap("forest_pass")
        ),
        GameMap.createSplitRoutesMap(
            isUnlocked = progressionManager.isMapUnlocked("split_routes"),
            stars = progressionManager.getStarsForMap("split_routes")
        ),
        GameMap.createCanyonTunnelMap(
            isUnlocked = progressionManager.isMapUnlocked("canyon_tunnel") || progressionManager.isMapUnlocked("map_4_tunnel"),
            stars = maxOf(progressionManager.getStarsForMap("canyon_tunnel"), progressionManager.getStarsForMap("map_4_tunnel"))
        ),
        GameMap.createTheCrossingMap(
            isUnlocked = progressionManager.isMapUnlocked("the_crossing"),
            stars = progressionManager.getStarsForMap("the_crossing")
        ),
        GameMap.createDragonsCoilMap(
            isUnlocked = progressionManager.isMapUnlocked("map_5_loop"),
            stars = progressionManager.getStarsForMap("map_5_loop")
        ),
        // Snow World Campaign (5 Levels with escalating wave counts & boss encounters)
        GameMap.createSnowOutpostMap(
            isUnlocked = progressionManager.isMapUnlocked("snow_outpost"),
            stars = progressionManager.getStarsForMap("snow_outpost")
        ),
        GameMap.createFrozenValleyMap(
            isUnlocked = progressionManager.isMapUnlocked("frozen_valley") || progressionManager.isMapUnlocked("snow_valley") || progressionManager.isMapUnlocked("map_8_snow"),
            stars = maxOf(progressionManager.getStarsForMap("frozen_valley"), progressionManager.getStarsForMap("snow_valley"), progressionManager.getStarsForMap("map_8_snow"))
        ),
        GameMap.createIceMountainMap(
            isUnlocked = progressionManager.isMapUnlocked("ice_mountain"),
            stars = progressionManager.getStarsForMap("ice_mountain")
        ),
        GameMap.createFrozenFortressMap(
            isUnlocked = progressionManager.isMapUnlocked("frozen_fortress"),
            stars = progressionManager.getStarsForMap("frozen_fortress")
        ),
        GameMap.createArcticBaseMap(
            isUnlocked = progressionManager.isMapUnlocked("arctic_base"),
            stars = progressionManager.getStarsForMap("arctic_base")
        ),
        GameMap.createNightFortressMap(
            isUnlocked = progressionManager.isMapUnlocked("night_fortress") || progressionManager.isMapUnlocked("map_9_night"),
            stars = maxOf(progressionManager.getStarsForMap("night_fortress"), progressionManager.getStarsForMap("map_9_night"))
        )
    )

    val totalStars = remember { progressionManager.getTotalStars() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070C18),
                        Color(0xFF0F1A2E),
                        Color(0xFF090D18)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("map_selection_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // TOP HEADER: Tactical Title & Stars Progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackToMenu,
                        contentDescription = "Back",
                        size = 40.dp,
                        testTag = "map_select_back_button"
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "CAMPAIGN MISSIONS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "SELECT OPERATIONAL THEATER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Stars Tally Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCA8A04), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalStars / 27",
                        color = Color(0xFFFDE68A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // HORIZONTAL MISSION CAROUSEL
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(maps) { index, map ->
                    TacticalMissionCard(
                        levelNumber = index + 1,
                        map = map,
                        onSelect = { onSelectMap(map) }
                    )
                }
            }
        }
    }
}

/**
 * Tactical Game Mission Card.
 * Designed like a physical military deployment dossier with 3D press feel,
 * environment preview viewport, star ratings, and locked/unlocked state.
 */
@Composable
private fun TacticalMissionCard(
    levelNumber: Int,
    map: GameMap,
    onSelect: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val pressOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val isUnlocked = map.isUnlocked

    val cardWidth = 220.dp
    val cardHeight = 240.dp
    val shape = RoundedCornerShape(10.dp)

    val borderColor = when {
        !isUnlocked -> Color(0xFF334155).copy(alpha = 0.5f)
        map.starsEarned == 3 -> Color(0xFFF59E0B)
        else -> Color(0xFF38BDF8)
    }

    val topFaceBrush = if (isUnlocked) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF131A26), Color(0xFF0A0F18))
        )
    }

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight + 4.dp)
            .testTag("map_card_${map.id}")
            .pointerInput(isUnlocked) {
                if (!isUnlocked) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch { pressOffset.animateTo(3f, tween(50)) }
                        val released = tryAwaitRelease()
                        coroutineScope.launch { pressOffset.animateTo(0f, tween(100)) }
                        if (released) {
                            audioPlayer.buttonClick()
                            onSelect()
                        }
                    }
                )
            }
    ) {
        // Base Drop Lip / Shadow
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .offset(y = 4.dp)
                .background(Color(0xFF050811), shape)
        )

        // Raised Tactical Card Face
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .offset { IntOffset(0, (pressOffset.value * density).toInt()) }
                .background(brush = topFaceBrush, shape = shape)
                .border(1.5.dp, borderColor, shape)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Ribbon: Level Number & Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isUnlocked) Color(0xFF0284C7) else Color(0xFF334155),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MISSION %02d".format(levelNumber),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (isUnlocked) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    if (isUnlocked) {
                        if (map.starsEarned == 3) {
                            Text(
                                text = "PERFECT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 0.5.sp
                            )
                        } else if (map.starsEarned > 0) {
                            Text(
                                text = "CLEARED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80),
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "LOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFF43F5E),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // 2. Large Environment Battlefield Preview Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                ) {
                    TacticalMapThumbnail(
                        map = map,
                        isUnlocked = isUnlocked,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Mission Name & Terrain Description
                Column {
                    Text(
                        text = map.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) Color.White else Color(0xFF64748B),
                        maxLines = 1
                    )
                    Text(
                        text = if (!map.missionChapter.isNullOrEmpty()) "${map.missionChapter} • ${map.totalWaves} WAVES"
                               else "${getEnvironmentLabel(map.environmentType)} • ${map.totalWaves} WAVES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFF475569),
                        maxLines = 1
                    )
                }

                // 4. Bottom Action Area: Stars & Deploy indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUnlocked) {
                        GameStarsRow(
                            starsEarned = map.starsEarned,
                            totalStars = 3,
                            starSize = 18.dp
                        )

                        // Deploy Arrow
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF22C55E), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "CLEAR PREV MISSION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getEnvironmentLabel(env: EnvironmentType): String = when (env) {
    EnvironmentType.GREEN_VALLEY -> "Verdant Outskirts • 15 Waves"
    EnvironmentType.DESERT_CANYON -> "Arid Dunes • Chokepoint"
    EnvironmentType.FOREST_CROSSROADS -> "Dense Timberland • Multi-Path"
    EnvironmentType.OBSIDIAN_TUNNEL -> "Subterranean Chasm • Heavy Armor"
    EnvironmentType.DRAGON_COIL -> "Serpentine Gorge • Orbital Siege"
    EnvironmentType.SNOW_VALLEY -> "Glacial Tundra • High Velocity"
    EnvironmentType.NIGHT_FORTRESS -> "Nocturnal Citadel • Apex Climax"
}

/**
 * Miniature canvas rendering the actual battlefield layout, path corridors, and environment mood.
 */
@Composable
private fun TacticalMapThumbnail(
    map: GameMap,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val (terrainColor, pathColor) = when (map.environmentType) {
                EnvironmentType.GREEN_VALLEY -> Color(0xFF3F6E2B) to Color(0xFFD4B46A)
                EnvironmentType.DESERT_CANYON -> Color(0xFFB57D48) to Color(0xFF8A5D32)
                EnvironmentType.FOREST_CROSSROADS -> Color(0xFF1E431B) to Color(0xFF65543C)
                EnvironmentType.OBSIDIAN_TUNNEL -> Color(0xFF131B29) to Color(0xFF4A5568)
                EnvironmentType.DRAGON_COIL -> Color(0xFF2E2823) to Color(0xFF6F5E4E)
                EnvironmentType.SNOW_VALLEY -> Color(0xFFCBD5E1) to Color(0xFF475569)
                EnvironmentType.NIGHT_FORTRESS -> Color(0xFF090E21) to Color(0xFF38BDF8)
            }

            // Draw base terrain
            drawRect(color = terrainColor)

            // Special environmental touches
            if (map.environmentType == EnvironmentType.SNOW_VALLEY) {
                // Frost patches
                drawCircle(Color(0xFFE2E8F0), 18f, Offset(canvasW * 0.2f, canvasH * 0.3f))
                drawCircle(Color(0xFFF1F5F9), 22f, Offset(canvasW * 0.75f, canvasH * 0.6f))
            } else if (map.environmentType == EnvironmentType.NIGHT_FORTRESS) {
                // Nocturnal starfield & moon
                drawCircle(Color(0xFFFEF08A), 1.2f, Offset(canvasW * 0.15f, canvasH * 0.2f))
                drawCircle(Color(0xFFFFFFFF), 1.0f, Offset(canvasW * 0.35f, canvasH * 0.15f))
                drawCircle(Color(0xFFBAE6FD), 1.2f, Offset(canvasW * 0.65f, canvasH * 0.25f))
                // Crescent moon
                drawCircle(Color(0xFFFEF08A), 5.5f, Offset(canvasW * 0.82f, canvasH * 0.28f))
                drawCircle(Color(0xFF090E21), 4.2f, Offset(canvasW * 0.80f, canvasH * 0.26f))
            }

            val scaleX = canvasW / map.worldWidth
            val scaleY = canvasH / map.worldHeight

            // Draw roads / paths
            for (p in map.paths) {
                val pts = p.waypoints
                if (pts.size >= 2) {
                    for (i in 0 until pts.size - 1) {
                        val start = Offset(pts[i].x * scaleX, pts[i].y * scaleY)
                        val end = Offset(pts[i + 1].x * scaleX, pts[i + 1].y * scaleY)
                        drawLine(
                            color = pathColor,
                            start = start,
                            end = end,
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Draw Base Citadel dot (Green)
            val baseOffset = Offset(map.basePosition.x * scaleX, map.basePosition.y * scaleY)
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 5.5f,
                center = baseOffset
            )

            // Draw Spawner dots (Crimson)
            for (sp in map.spawnPoints) {
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 4f,
                    center = Offset(sp.x * scaleX, sp.y * scaleY)
                )
            }
        }

        // Dark lock hatch overlay if classified
        if (!isUnlocked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC070B14))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Classified",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "CLASSIFIED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
