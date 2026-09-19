package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameConfig
import com.example.data.GameMap
import com.example.data.ProgressionManager

/**
 * Clean, attractive level selection screen with minimal card information:
 * - Level number (e.g. LEVEL 1)
 * - Level name (e.g. Green Valley)
 * - Small map preview/thumbnail
 * - Lock/unlock state
 * - Stars earned (★★★)
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
        GameMap.createSnowValleyMap(
            isUnlocked = progressionManager.isMapUnlocked("snow_valley") || progressionManager.isMapUnlocked("map_8_snow"),
            stars = maxOf(progressionManager.getStarsForMap("snow_valley"), progressionManager.getStarsForMap("map_8_snow"))
        ),
        GameMap.createNightFortressMap(
            isUnlocked = progressionManager.isMapUnlocked("night_fortress") || progressionManager.isMapUnlocked("map_9_night"),
            stars = maxOf(progressionManager.getStarsForMap("night_fortress"), progressionManager.getStarsForMap("map_9_night"))
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("map_selection_screen")
    ) {
        // Top Navigation Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1C2541), CircleShape)
                    .testTag("map_select_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "SELECT LEVEL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Choose your mission",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Clean Level Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(maps) { index, map ->
                LevelCard(
                    levelNumber = index + 1,
                    map = map,
                    onSelect = { if (map.isUnlocked) onSelectMap(map) }
                )
            }
        }
    }
}

@Composable
private fun LevelCard(
    levelNumber: Int,
    map: GameMap,
    onSelect: () -> Unit
) {
    val isUnlocked = map.isUnlocked
    val cardBg = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A)
    val borderColor = if (isUnlocked) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked, onClick = onSelect)
            .testTag("map_card_${map.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 1. Small Map Preview / Thumbnail
            MapThumbnail(
                map = map,
                isUnlocked = isUnlocked,
                modifier = Modifier
                    .size(width = 76.dp, height = 58.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            // 2. Level Details: Level #, Name, Stars / Lock state
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "LEVEL $levelNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = map.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.White else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isUnlocked) {
                    // Stars earned (★★★)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= map.starsEarned) Color(0xFFFFD166) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    // Clean Locked Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOCKED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            // 3. Action Indicator
            if (isUnlocked) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF0284C7), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean miniature thumbnail drawing the actual map terrain and path layout.
 */
@Composable
private fun MapThumbnail(
    map: GameMap,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val (terrainColor, pathColor) = when (map.environmentType) {
                com.example.data.EnvironmentType.GREEN_VALLEY -> Color(0xFF5D9A38) to Color(0xFFE2C480)
                com.example.data.EnvironmentType.DESERT_CANYON -> Color(0xFFD4A373) to Color(0xFFC29060)
                com.example.data.EnvironmentType.FOREST_CROSSROADS -> Color(0xFF2D5A27) to Color(0xFF7A6B53)
                com.example.data.EnvironmentType.OBSIDIAN_TUNNEL -> Color(0xFF1E293B) to Color(0xFF64748B)
                com.example.data.EnvironmentType.DRAGON_COIL -> Color(0xFF3F3B37) to Color(0xFF8C7A6B)
                com.example.data.EnvironmentType.SNOW_VALLEY -> Color(0xFFE2E8F0) to Color(0xFF475569)
                com.example.data.EnvironmentType.NIGHT_FORTRESS -> Color(0xFF0A1128) to Color(0xFF38BDF8)
            }

            // Terrain Background
            drawRect(color = terrainColor)

            // Night sky stars in thumbnail if Night Fortress
            if (map.environmentType == com.example.data.EnvironmentType.NIGHT_FORTRESS) {
                drawCircle(Color(0xFFFEF08A), 1.2f, Offset(canvasW * 0.15f, canvasH * 0.2f))
                drawCircle(Color(0xFFFFFFFF), 1.0f, Offset(canvasW * 0.35f, canvasH * 0.15f))
                drawCircle(Color(0xFFBAE6FD), 1.2f, Offset(canvasW * 0.65f, canvasH * 0.25f))
                drawCircle(Color(0xFFFEF08A), 1.0f, Offset(canvasW * 0.85f, canvasH * 0.18f))
                // Crescent moon
                drawCircle(Color(0xFFFEF08A), 4.5f, Offset(canvasW * 0.82f, canvasH * 0.28f))
                drawCircle(Color(0xFF0A1128), 3.5f, Offset(canvasW * 0.80f, canvasH * 0.26f))
            }

            val scaleX = canvasW / map.worldWidth
            val scaleY = canvasH / map.worldHeight

            // Draw paths as stylized mini roads
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
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Draw miniature Base Fortress dot
            val baseOffset = Offset(map.basePosition.x * scaleX, map.basePosition.y * scaleY)
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 4.5f,
                center = baseOffset
            )

            // Draw miniature spawn points
            for (sp in map.spawnPoints) {
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 3.5f,
                    center = Offset(sp.x * scaleX, sp.y * scaleY)
                )
            }
        }

        // Locked frost overlay with padlock
        if (!isUnlocked) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xBB0B132B))
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
