package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.data.GameMap
import com.example.data.ProgressionManager
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameIconButton
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * World sector definition for grouping campaign missions into distinct thematic territories.
 */
enum class WorldSector(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val themeColor: Color,
    val accentColor: Color,
    val bgColors: List<Color>
) {
    FOREST(
        title = "VALLEY & FOREST",
        subtitle = "Temperate Woodlands & Crossroad Passes",
        icon = Icons.Default.Forest,
        themeColor = Color(0xFF22C55E),
        accentColor = Color(0xFF4ADE80),
        bgColors = listOf(Color(0xFF0D2313), Color(0xFF08150C), Color(0xFF050E08))
    ),
    DESERT(
        title = "DESERT & CANYON",
        subtitle = "Sunbaked Arid Dunes & Obsidian Caverns",
        icon = Icons.Default.Terrain,
        themeColor = Color(0xFFF59E0B),
        accentColor = Color(0xFFFBBF24),
        bgColors = listOf(Color(0xFF291A07), Color(0xFF191005), Color(0xFF0F0A03))
    ),
    SNOW(
        title = "ARCTIC TUNDRA",
        subtitle = "Glacial Valleys & Frozen Fortresses",
        icon = Icons.Default.AcUnit,
        themeColor = Color(0xFF38BDF8),
        accentColor = Color(0xFF7DD3FC),
        bgColors = listOf(Color(0xFF091E36), Color(0xFF051221), Color(0xFF030A14))
    ),
    NIGHT(
        title = "NIGHT CITADEL",
        subtitle = "Moonlit Stronghold & Apex Climax",
        icon = Icons.Default.DarkMode,
        themeColor = Color(0xFFA855F7),
        accentColor = Color(0xFFC084FC),
        bgColors = listOf(Color(0xFF1B0B33), Color(0xFF110721), Color(0xFF0A0414))
    ),
    ECLIPSE(
        title = "ECLIPSE SOLSTICE",
        subtitle = "Dual Realm: Day & Night, 10 Bosses",
        icon = Icons.Default.CrisisAlert,
        themeColor = Color(0xFFF97316),
        accentColor = Color(0xFFFBBF24),
        bgColors = listOf(Color(0xFF3B0764), Color(0xFF1E1B4B), Color(0xFF0F172A))
    ),
    TEMPEST(
        title = "TEMPEST BASTION",
        subtitle = "Torrential Rain & Twin Tower Siege",
        icon = Icons.Default.Bolt,
        themeColor = Color(0xFF06B6D4),
        accentColor = Color(0xFF38BDF8),
        bgColors = listOf(Color(0xFF082F49), Color(0xFF0C1929), Color(0xFF030712))
    ),
    CLOUDY(
        title = "CLOUDY FOREST",
        subtitle = "Dense Forest, 2 Gun Mounts & Mountain Tunnel",
        icon = Icons.Default.Forest,
        themeColor = Color(0xFF10B981),
        accentColor = Color(0xFF34D399),
        bgColors = listOf(Color(0xFF042F1A), Color(0xFF021B0F), Color(0xFF010E08))
    ),
    HIGHLANDS(
        title = "EMERALD HIGHLANDS",
        subtitle = "Sylvan Lakes, Twin Zig-Zag Tunnels & Twin Bosses",
        icon = Icons.Default.Forest,
        themeColor = Color(0xFF10B981),
        accentColor = Color(0xFF34D399),
        bgColors = listOf(Color(0xFF064E3B), Color(0xFF022C22), Color(0xFF021B0F))
    )
}

/**
 * Tactical metadata for a campaign node on the world map.
 */
data class CampaignMissionNode(
    val map: GameMap,
    val levelNumber: Int,
    val sector: WorldSector,
    val isBoss: Boolean,
    val bossName: String? = null,
    val xPosDp: Float = 0f,
    val yPosDp: Float = 0f
)

/**
 * Professional mobile game Campaign World Map.
 * Displays all 13 campaign missions in a connected S-Curve Zig-Zag Chain:
 * - Serpentine zig-zag layout displaying all 13 levels in full view
 * - Interlocking metallic chain links with animated energy conduit
 * - Distinct biome styling (Forest, Canyon/Desert, Snow Tundra, Night Citadel)
 * - Tactical nodes with status (Completed with 1-3 stars, Current active pulse, Locked)
 * - Special Boss encounters with distinct crests, skull badges, and aura effects
 * - Docked Mission Briefing strip with START MISSION launcher
 */
@Composable
fun MapSelectionScreen(
    progressionManager: ProgressionManager,
    onSelectMap: (GameMap) -> Unit,
    onBackToMenu: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }

    // 13 Campaign Maps aligned with progression system
    val maps = remember(progressionManager) {
        listOf(
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
            // Snow World Campaign (5 Levels)
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
            ),
            GameMap.createEclipseFrontierMap(
                isUnlocked = progressionManager.isMapUnlocked("eclipse_frontier") || progressionManager.isMapUnlocked("solstice_frontier"),
                stars = maxOf(progressionManager.getStarsForMap("eclipse_frontier"), progressionManager.getStarsForMap("solstice_frontier"))
            ),
            GameMap.createTempestBastionMap(
                isUnlocked = progressionManager.isMapUnlocked("storm_twin_bastion") || progressionManager.isMapUnlocked("tempest_bastion"),
                stars = maxOf(progressionManager.getStarsForMap("storm_twin_bastion"), progressionManager.getStarsForMap("tempest_bastion"))
            ),
            GameMap.createCloudyForestMap(
                isUnlocked = progressionManager.isMapUnlocked("cloudy_dense_forest") || progressionManager.isMapUnlocked("cloudy_forest"),
                stars = maxOf(progressionManager.getStarsForMap("cloudy_dense_forest"), progressionManager.getStarsForMap("cloudy_forest"))
            ),
            GameMap.createSnowSummitMap(
                isUnlocked = progressionManager.isMapUnlocked("snow_summit_descent") || progressionManager.isMapUnlocked("frostpeak_descent"),
                stars = maxOf(progressionManager.getStarsForMap("snow_summit_descent"), progressionManager.getStarsForMap("frostpeak_descent"))
            ),
            GameMap.createDesertDuneBastionMap(
                isUnlocked = progressionManager.isMapUnlocked("desert_dune_bastion") || progressionManager.isMapUnlocked("dune_storm_stronghold"),
                stars = maxOf(progressionManager.getStarsForMap("desert_dune_bastion"), progressionManager.getStarsForMap("dune_storm_stronghold"))
            ),
            GameMap.createEmeraldTwinPassMap(
                isUnlocked = progressionManager.isMapUnlocked("emerald_twin_pass") || progressionManager.isMapUnlocked("emerald_serpent_pass"),
                stars = maxOf(progressionManager.getStarsForMap("emerald_twin_pass"), progressionManager.getStarsForMap("emerald_serpent_pass"))
            ),
            GameMap.createForestRingBastionMap(
                isUnlocked = progressionManager.isMapUnlocked("forest_ring_bastion") || progressionManager.isMapUnlocked("sylvan_ring_sanctuary"),
                stars = maxOf(progressionManager.getStarsForMap("forest_ring_bastion"), progressionManager.getStarsForMap("sylvan_ring_sanctuary"))
            ),
            GameMap.createFrozenPassMap(
                isUnlocked = progressionManager.isMapUnlocked("frozen_pass"),
                stars = progressionManager.getStarsForMap("frozen_pass")
            ),
            GameMap.createObsidianCrossfireMap(
                isUnlocked = progressionManager.isMapUnlocked("obsidian_crossfire"),
                stars = progressionManager.getStarsForMap("obsidian_crossfire")
            ),
            GameMap.createTempestRavineMap(
                isUnlocked = progressionManager.isMapUnlocked("tempest_ravine"),
                stars = progressionManager.getStarsForMap("tempest_ravine")
            ),
            GameMap.createEclipseCitadelMap(
                isUnlocked = progressionManager.isMapUnlocked("eclipse_citadel"),
                stars = progressionManager.getStarsForMap("eclipse_citadel")
            ),
            GameMap.createApexDragonSanctumMap(
                isUnlocked = progressionManager.isMapUnlocked("apex_dragon_sanctum"),
                stars = progressionManager.getStarsForMap("apex_dragon_sanctum")
            )
        )
    }

    val totalStars = remember { progressionManager.getTotalStars() }

    // 25 Campaign Nodes structured for the Zig-Zag Chain (5 horizontal lines of 5 levels)
    val campaignNodes = remember(maps) {
        listOf(
            CampaignMissionNode(maps[0], 1, WorldSector.FOREST, false, null),
            CampaignMissionNode(maps[1], 2, WorldSector.FOREST, false, null),
            CampaignMissionNode(maps[2], 3, WorldSector.FOREST, true, "Forest Colossus"),
            CampaignMissionNode(maps[3], 4, WorldSector.DESERT, false, null),
            CampaignMissionNode(maps[4], 5, WorldSector.DESERT, false, null),
            CampaignMissionNode(maps[5], 6, WorldSector.DESERT, false, null),
            CampaignMissionNode(maps[6], 7, WorldSector.DESERT, true, "Desert Behemoth"),
            CampaignMissionNode(maps[7], 8, WorldSector.SNOW, false, null),
            CampaignMissionNode(maps[8], 9, WorldSector.SNOW, false, null),
            CampaignMissionNode(maps[9], 10, WorldSector.SNOW, false, null),
            CampaignMissionNode(maps[10], 11, WorldSector.SNOW, false, null),
            CampaignMissionNode(maps[11], 12, WorldSector.SNOW, true, "Frost Titan"),
            CampaignMissionNode(maps[12], 13, WorldSector.NIGHT, true, "Apex Overlord"),
            CampaignMissionNode(maps[13], 14, WorldSector.ECLIPSE, true, "10 Apex Bosses"),
            CampaignMissionNode(maps[14], 15, WorldSector.TEMPEST, true, "Twin Siege Bosses"),
            CampaignMissionNode(maps[15], 16, WorldSector.CLOUDY, true, "Ancient Forest Titan"),
            CampaignMissionNode(maps[16], 17, WorldSector.SNOW, true, "Summit Boss Avalanche"),
            CampaignMissionNode(maps[17], 18, WorldSector.DESERT, true, "Desert Boss Incursion (Hard)"),
            CampaignMissionNode(maps[18], 19, WorldSector.HIGHLANDS, true, "Twin Boss Serpent Pass (Hard)"),
            CampaignMissionNode(maps[19], 20, WorldSector.HIGHLANDS, true, "Sylvan Ring Citadel (25 Waves)"),
            CampaignMissionNode(maps[20], 21, WorldSector.SNOW, true, "Frozen Pass Titans (Hard)"),
            CampaignMissionNode(maps[21], 22, WorldSector.DESERT, true, "Obsidian Dreadnoughts (Hard)"),
            CampaignMissionNode(maps[22], 23, WorldSector.TEMPEST, true, "Tempest Leviathans (Extreme)"),
            CampaignMissionNode(maps[23], 24, WorldSector.ECLIPSE, true, "Solstice Overlords (Master)"),
            CampaignMissionNode(maps[24], 25, WorldSector.HIGHLANDS, true, "Apex Dragon Sovereigns (10 Bosses)")
        )
    }

    // Determine current active (frontier) mission index
    val currentMissionIndex = remember(campaignNodes) {
        val lastUnlockedIndex = campaignNodes.indexOfLast { it.map.isUnlocked }
        if (lastUnlockedIndex != -1) lastUnlockedIndex else 0
    }

    val mapScrollState = rememberScrollState()

    // Active Sector based on frontline mission
    val activeSector = campaignNodes[currentMissionIndex].sector

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070C16))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("map_selection_screen")
    ) {
        // Rich Procedural Fantasy Terrain Map (Forests, Canyons, Tundras, Citadel & Rivers)
        WorldTerrainCanvas(
            activeSector = activeSector,
            modifier = Modifier.fillMaxSize()
        )

        // Dynamic ambient particle overlay
        WorldMapParticles(sector = activeSector)

        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // TOP BAR: Title, Sector Beacon & Stars Counter
            // ==========================================
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Back Button & Sector Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackToMenu,
                        contentDescription = "Back",
                        size = 38.dp,
                        testTag = "map_select_back_button"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CAMPAIGN WORLD MAP",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Current World Capsule
                            Box(
                                modifier = Modifier
                                    .background(activeSector.themeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .border(1.dp, activeSector.themeColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = activeSector.icon,
                                        contentDescription = null,
                                        tint = activeSector.accentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeSector.title,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = activeSector.accentColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "PROGRESS THROUGH THEATERS • CONQUER ALL WAVES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Star & Progression Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Stars Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCA8A04), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$totalStars / 48",
                            color = Color(0xFFFDE68A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Total Unlocked Missions Badge
                    val unlockedCount = maps.count { it.isUnlocked }
                    Box(
                        modifier = Modifier
                            .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "SECTOR $unlockedCount/${maps.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // ==========================================
            // WORLD MAP: 5 LEVELS PER HORIZONTAL LINE
            // Each line displays 5 levels horizontally with dedicated road segments
            // ==========================================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val scopeMaxWidth = maxWidth
                val containerWidthPx = constraints.maxWidth.toFloat()
                val containerHeightPx = constraints.maxHeight.toFloat()
                val isLandscape = scopeMaxWidth >= 580.dp
                val density = LocalDensity.current

                val levelsPerLine = 5
                val numLines = (campaignNodes.size + levelsPerLine - 1) / levelsPerLine

                // Layout parameters: 5 levels spaced horizontally across each line
                val padX = with(density) { (if (isLandscape) 44.dp else 22.dp).toPx() }
                val maxLineWidth = with(density) { (if (isLandscape) 720.dp else 440.dp).toPx() }
                val netW = (containerWidthPx - padX * 2f).coerceAtLeast(100f).coerceAtMost(maxLineWidth)
                val startX = (containerWidthPx - netW) / 2f

                // Spacing parameters: 4 rows of 5 levels nicely proportioned for landscape and portrait
                val startY = with(density) { (if (isLandscape) 36.dp else 46.dp).toPx() }
                val rowStepY = with(density) { (if (isLandscape) 82.dp else 104.dp).toPx() }

                // Compute node positions: 5 levels per horizontal line
                val nodePositions: List<Offset> = remember(containerWidthPx, containerHeightPx, isLandscape, campaignNodes.size) {
                    val positions = mutableListOf<Offset>()
                    for (idx in campaignNodes.indices) {
                        val lineIdx = idx / levelsPerLine
                        val colIdx = idx % levelsPerLine
                        val x = startX + (colIdx.toFloat() / (levelsPerLine - 1).toFloat()) * netW
                        val y = startY + lineIdx * rowStepY
                        positions.add(Offset(x, y))
                    }
                    positions
                }

                // Generous clearance ensuring smooth scrolling across all devices and complete visibility of the 4th row
                val lastNodeY = nodePositions.lastOrNull()?.y ?: 400f
                val bottomClearancePx = with(density) { (if (isLandscape) 88.dp else 115.dp).toPx() }
                val totalContentHeightPx = maxOf(lastNodeY + bottomClearancePx, containerHeightPx + with(density) { 60.dp.toPx() })
                val totalMapHeightDp = with(density) { totalContentHeightPx.toDp() }

                // Smoothly scroll to the player's active frontline mission
                LaunchedEffect(currentMissionIndex) {
                    val targetLine = currentMissionIndex / levelsPerLine
                    val targetScrollPx = ((targetLine * rowStepY) - with(density) { 30.dp.toPx() }).coerceAtLeast(0f)
                    mapScrollState.animateScrollTo(
                        targetScrollPx.roundToInt(),
                        animationSpec = tween(650, easing = FastOutSlowInEasing)
                    )
                }

                // Generate road segments horizontally within each line of 5 levels (not one continuous chain)
                val roadSegments: List<CampaignRoadSegment> = remember(nodePositions) {
                    val segs = mutableListOf<CampaignRoadSegment>()
                    if (nodePositions.size < 2) return@remember segs

                    for (lineIdx in 0 until numLines) {
                        val lineStart = lineIdx * levelsPerLine
                        val lineEnd = minOf((lineIdx + 1) * levelsPerLine, nodePositions.size)
                        for (i in lineStart until lineEnd - 1) {
                            val p1 = nodePositions[i]
                            val p2 = nodePositions[i + 1]
                            val path = Path().apply {
                                moveTo(p1.x, p1.y)
                                val midX = (p1.x + p2.x) / 2f
                                val midY = (p1.y + p2.y) / 2f + (if (i % 2 == 0) -2.5f else 2.5f)
                                quadraticTo(midX, midY, p2.x, p2.y)
                            }
                            segs.add(CampaignRoadSegment(path, i, i + 1))
                        }
                    }
                    segs
                }

                // Scroll container taking the full viewport area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(mapScrollState)
                ) {
                    // Inner content Box with exact total map height so verticalScroll properly scrolls all rows
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(totalMapHeightDp)
                    ) {
                        // Procedural metallic chain and energy lines connecting levels horizontally in each line
                        CampaignChainCanvas(
                            segments = roadSegments,
                            nodes = campaignNodes,
                            currentMissionIndex = currentMissionIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(totalMapHeightDp)
                        )

                        // Render All Level Nodes
                        campaignNodes.forEachIndexed { index, node ->
                            if (index < nodePositions.size) {
                                val pos = nodePositions[index]
                                val isCompleted = node.map.isUnlocked && node.map.starsEarned > 0
                                val isCurrent = (index == currentMissionIndex)
                                val nodeSizeDp = if (node.isBoss) 52.dp else 44.dp
                                val halfSizePx = with(density) { (nodeSizeDp / 2f).toPx() }

                                CampaignNodeMarker(
                                    node = node,
                                    isCompleted = isCompleted,
                                    isCurrent = isCurrent,
                                    isSelected = isCurrent,
                                    onClick = {
                                        // Clicking directly starts the mission; no separate start button needed
                                        if (node.map.isUnlocked) {
                                            audioPlayer.waveStart()
                                            onSelectMap(node.map)
                                        } else {
                                            audioPlayer.buttonClick()
                                        }
                                    },
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = (pos.x - halfSizePx).roundToInt(),
                                                y = (pos.y - halfSizePx).roundToInt()
                                            )
                                        }
                                )
                            }
                        }
                    }
                }

                // Sleek floating scroll cue at the bottom when more levels exist below the current viewport
                if (mapScrollState.canScrollForward) {
                    Surface(
                        color = Color(0xEE0F172A),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Scroll down for more missions",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SCROLL DOWN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE2E8F0),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Represents a road segment linking two adjacent levels horizontally within a line.
 */
data class CampaignRoadSegment(
    val path: Path,
    val fromIndex: Int,
    val toIndex: Int
)

/**
 * Procedural canvas drawing an authentic fantasy campaign road:
 * - Wide earthen/stone carved pathway with outer cobblestone curb
 * - Biome-adapted surface texture and paving stones
 * - Glowing directional waypoints and animated golden travel energy
 * - Locked paths show weathered broken trail
 */
@Composable
private fun CampaignChainCanvas(
    segments: List<CampaignRoadSegment>,
    nodes: List<CampaignMissionNode>,
    currentMissionIndex: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "road_anim")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dash_offset"
    )

    val energyProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_pulse"
    )

    val pathMeasure = remember { PathMeasure() }

    Canvas(modifier = modifier) {
        for (segment in segments) {
            val path = segment.path
            val nextNode = nodes.getOrNull(segment.toIndex) ?: continue
            val isSegmentUnlocked = nextNode.map.isUnlocked
            val isFrontline = (segment.toIndex == currentMissionIndex)
            val sector = nextNode.sector

            // 1. Deep path trench / drop shadow
            drawPath(
                path = path,
                color = Color(0x9902060E),
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )

            // 2. Cobblestone road border / curb stones
            val curbColor = if (isSegmentUnlocked) Color(0xFF334155) else Color(0xFF1E293B)
            drawPath(
                path = path,
                color = curbColor,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )

            // 3. Main road surface layer
            val roadSurfaceColor = when {
                !isSegmentUnlocked -> Color(0xFF0F172A)
                sector == WorldSector.FOREST -> Color(0xFF16251C)
                sector == WorldSector.DESERT -> Color(0xFF2E2214)
                sector == WorldSector.SNOW -> Color(0xFF152238)
                sector == WorldSector.NIGHT -> Color(0xFF201335)
                sector == WorldSector.ECLIPSE -> Color(0xFF381B28)
                sector == WorldSector.TEMPEST -> Color(0xFF0C243B)
                sector == WorldSector.CLOUDY -> Color(0xFF092A1A)
                else -> Color(0xFF1E293B)
            }
            drawPath(
                path = path,
                color = roadSurfaceColor,
                style = Stroke(width = 11f, cap = StrokeCap.Round)
            )

            // 4. Cobblestone stepping stones along the road
            pathMeasure.setPath(path, false)
            val pathLength = pathMeasure.length
            val stoneSpacing = 18f
            var curDist = stoneSpacing * 0.7f
            var stoneIdx = 0

            while (curDist < pathLength - stoneSpacing * 0.5f) {
                val stonePos = pathMeasure.getPosition(curDist)
                val nextP = pathMeasure.getPosition((curDist + 3f).coerceAtMost(pathLength))
                val angleDeg = atan2(nextP.y - stonePos.y, nextP.x - stonePos.x) * 180f / PI.toFloat()

                val stoneColor = if (isSegmentUnlocked) {
                    if (stoneIdx % 2 == 0) Color(0xFF475569) else Color(0xFF64748B)
                } else {
                    Color(0xFF1E293B)
                }

                rotate(degrees = angleDeg, pivot = stonePos) {
                    drawRoundRect(
                        color = stoneColor.copy(alpha = if (isSegmentUnlocked) 0.65f else 0.35f),
                        topLeft = Offset(stonePos.x - 3.5f, stonePos.y - 4.5f),
                        size = Size(7f, 9f),
                        cornerRadius = CornerRadius(2.5f, 2.5f)
                    )
                }
                curDist += stoneSpacing
                stoneIdx++
            }

            // 5. Active Frontline Trail: Pulsing golden dashed road line & magical energy sparks
            if (isSegmentUnlocked) {
                if (isFrontline) {
                    // Golden energy guide line
                    drawPath(
                        path = path,
                        color = Color(0xFFFBBF24).copy(alpha = 0.5f),
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = path,
                        color = Color(0xFFFEF08A),
                        style = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), dashOffset)
                        )
                    )

                    // Flying magical traveler orbs along frontline road
                    if (pathLength > 0f) {
                        val sparkDist = (energyProgress * pathLength).coerceIn(0f, pathLength)
                        val sparkPos = pathMeasure.getPosition(sparkDist)

                        // Outer golden glow
                        drawCircle(
                            color = Color(0x66F59E0B),
                            radius = 12f,
                            center = sparkPos
                        )
                        // Inner brilliant spark
                        drawCircle(
                            color = Color(0xFFFFFBEB),
                            radius = 4.5f,
                            center = sparkPos
                        )
                    }
                } else {
                    // Completed road path: Subtle calm paved dashes
                    drawPath(
                        path = path,
                        color = Color(0xFF94A3B8).copy(alpha = 0.35f),
                        style = Stroke(
                            width = 1.8f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                        )
                    )
                }
            } else {
                // Locked path: subtle dotted trail
                drawPath(
                    path = path,
                    color = Color(0xFF334155).copy(alpha = 0.4f),
                    style = Stroke(
                        width = 1.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                    )
                )
            }
        }
    }
}

/**
 * Tactical Campaign Node Marker on the world map.
 * - 3D Embossed Gaming Medallion with metallic bevel and dark inner dial
 * - Boss levels feature crowned gold/crimson citadel crest and sinister battle runes
 * - Unlocked nodes shine with biome-matched gemstones
 * - Frontline mission features pulsating radiant beacon with animated directional chevron
 */
@Composable
private fun CampaignNodeMarker(
    node: CampaignMissionNode,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnlocked = node.map.isUnlocked
    val isBoss = node.isBoss
    val nodeSize = if (isBoss) 52.dp else 44.dp

    // Pulse animation for current active mission
    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.10f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isCurrent) 0.75f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )

    // Outer Bevel Metallic Border
    val outerBorderBrush = when {
        isSelected -> Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706)))
        isBoss && isUnlocked -> Brush.verticalGradient(listOf(Color(0xFFFCA5A5), Color(0xFFB91C1C)))
        isBoss && !isUnlocked -> Brush.verticalGradient(listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))
        isCurrent -> Brush.verticalGradient(listOf(Color(0xFFBAE6FD), Color(0xFF0284C7)))
        isCompleted -> Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFB45309)))
        isUnlocked -> Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFF334155)))
        else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A)))
    }

    // Inner Core Medallion Dial
    val coreBgBrush = when {
        isBoss && isUnlocked -> Brush.radialGradient(
            colors = listOf(Color(0xFF991B1B), Color(0xFF450A0A), Color(0xFF1C0505))
        )
        isBoss && !isUnlocked -> Brush.radialGradient(
            colors = listOf(Color(0xFF450A0A), Color(0xFF1F0303))
        )
        isCurrent -> Brush.radialGradient(
            colors = listOf(Color(0xFF0284C7), Color(0xFF075985), Color(0xFF082F49))
        )
        isCompleted -> Brush.radialGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        )
        isUnlocked -> Brush.radialGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
        )
    }

    Box(
        modifier = modifier
            .size(nodeSize)
            .scale(if (isCurrent) pulseScale else 1f)
            .testTag("campaign_node_${node.levelNumber}"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Outer radiant glow ring for current / selected node
        if (isCurrent || isSelected) {
            Box(
                modifier = Modifier
                    .size(nodeSize + 16.dp)
                    .background(
                        color = (if (isSelected) Color(0xFFFBBF24) else Color(0xFF38BDF8)).copy(alpha = haloAlpha),
                        shape = CircleShape
                    )
            )
        }

        // 2. Drop shadow base disc
        Box(
            modifier = Modifier
                .size(nodeSize)
                .offset(y = 2.dp)
                .background(Color(0x99000000), CircleShape)
        )

        // 4. Main 3D Tactile Medallion Button
        Box(
            modifier = Modifier
                .size(nodeSize)
                .clip(CircleShape)
                .background(brush = outerBorderBrush)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(brush = coreBgBrush)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isBoss) {
                    // Boss Level Crest
                    Icon(
                        imageVector = if (node.levelNumber == 13) Icons.Default.Castle else Icons.Default.CrisisAlert,
                        contentDescription = "Boss",
                        tint = if (isUnlocked) Color(0xFFFEE2E2) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LV.${node.levelNumber}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) Color(0xFFFECDD3) else Color(0xFF94A3B8)
                    )
                    Text(
                        text = if (node.levelNumber == 13) "APEX" else "BOSS",
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isUnlocked) Color(0xFFF87171) else Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                } else if (!isUnlocked) {
                    // Locked Level
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${node.levelNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                } else {
                    // Standard Unlocked Level
                    Text(
                        text = "${node.levelNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isCurrent) Color.White else Color(0xFFF1F5F9)
                    )
                }

                // Stars display underneath number
                if (isUnlocked && node.map.starsEarned > 0) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        repeat(node.map.starsEarned) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ambient background environmental particles matching current active sector (snowflakes, sparks, dust motes).
 */
@Composable
private fun WorldMapParticles(sector: WorldSector) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_particles")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "animProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val particleColor = when (sector) {
            WorldSector.SNOW -> Color(0xFFBAE6FD)
            WorldSector.DESERT -> Color(0xFFFDE68A)
            WorldSector.NIGHT -> Color(0xFFD8B4FE)
            WorldSector.FOREST -> Color(0xFF86EFAC)
            WorldSector.ECLIPSE -> Color(0xFFFDBA74)
            WorldSector.TEMPEST -> Color(0xFF67E8F9)
            WorldSector.CLOUDY -> Color(0xFF6EE7B7)
            else -> Color(0xFF67E8F9)
        }

        val particleCount = 18
        for (i in 0 until particleCount) {
            val seed = (i * 197) % 1000 / 1000f
            val x = w * ((seed * 1.3f + animProgress * 0.2f) % 1f)
            val y = h * ((seed * 0.9f + animProgress * (if (sector == WorldSector.SNOW) 0.5f else -0.3f)) % 1f)
            val alpha = (kotlin.math.sin((animProgress + seed) * Math.PI)).toFloat().coerceIn(0f, 1f) * 0.45f

            drawCircle(
                color = particleColor.copy(alpha = alpha),
                radius = if (i % 3 == 0) 3f else 1.8f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Rich procedural fantasy world map terrain canvas:
 * - 4 Seamlessly blended biome regions (Verdant Forest, Desert Canyon, Arctic Glacier, Citadel Void)
 * - Mountain ridges with snowy/rocky crests
 * - Winding azure rivers flowing from snowy mountains to the lowlands
 * - Clustered pine forests and canyon dunes
 * - Arcane citadel runes and dark void fissures in Sector IV
 */
@Composable
private fun WorldTerrainCanvas(
    activeSector: WorldSector,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // 1. Base Multi-Biome Gradient Canvas
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF07140B), // Deep Forest Green
                    Color(0xFF0F1E12), // Vibrant Woods
                    Color(0xFF221708), // Arid Canyon Sands
                    Color(0xFF1C1307), // Desert Red
                    Color(0xFF061B2E), // Arctic Blue Glacier
                    Color(0xFF03101E), // Frozen Lake
                    Color(0xFF140722), // Citadel Arcane Violet
                    Color(0xFF090312)  // Void Abyssal Black
                )
            ),
            size = size
        )

        // 2. Biome Contour Underlays (Organic geographic elevation)
        // Forest canopy mass (Left side: 0% to 32% width)
        val forestHill = Path().apply {
            moveTo(0f, h * 0.45f)
            cubicTo(w * 0.08f, h * 0.35f, w * 0.18f, h * 0.40f, w * 0.28f, h * 0.28f)
            lineTo(w * 0.30f, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(
            path = forestHill,
            color = Color(0xFF0C2416).copy(alpha = 0.65f)
        )

        // Desert Canyon Plateau (Mid-top: 25% to 55% width)
        val desertCanyon = Path().apply {
            moveTo(w * 0.26f, h * 0.85f)
            cubicTo(w * 0.38f, h * 0.70f, w * 0.48f, h * 0.75f, w * 0.58f, h * 0.60f)
            lineTo(w * 0.60f, h)
            lineTo(w * 0.24f, h)
            close()
        }
        drawPath(
            path = desertCanyon,
            color = Color(0xFF2B1D0B).copy(alpha = 0.55f)
        )

        // Arctic Glacier Ice Shelf (Mid-right: 50% to 80% width)
        val iceShelf = Path().apply {
            moveTo(w * 0.48f, 0f)
            cubicTo(w * 0.58f, h * 0.22f, w * 0.68f, h * 0.18f, w * 0.78f, h * 0.35f)
            lineTo(w * 0.82f, 0f)
            close()
        }
        drawPath(
            path = iceShelf,
            color = Color(0xFF0D2D4B).copy(alpha = 0.65f)
        )

        // 3. Winding Emerald & Crystal Rivers
        // Northern river from glacier to forest
        val riverNorth = Path().apply {
            moveTo(w * 0.65f, 0f)
            cubicTo(w * 0.55f, h * 0.25f, w * 0.45f, h * 0.15f, w * 0.35f, h * 0.35f)
            cubicTo(w * 0.25f, h * 0.45f, w * 0.15f, h * 0.40f, 0f, h * 0.55f)
        }
        drawPath(
            path = riverNorth,
            color = Color(0x330284C7),
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )
        drawPath(
            path = riverNorth,
            color = Color(0x6638BDF8),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        drawPath(
            path = riverNorth,
            color = Color(0x99BAE6FD),
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )

        // 4. Stylized Mountain Peaks & Ridges (Triangle peaks)
        // Mountain clusters in canyon/glacier boundary (x: 48% to 56%)
        val mountainPeaks = listOf(
            Triple(w * 0.48f, h * 0.22f, 18f),
            Triple(w * 0.52f, h * 0.18f, 24f),
            Triple(w * 0.56f, h * 0.25f, 19f),
            Triple(w * 0.20f, h * 0.70f, 20f),
            Triple(w * 0.24f, h * 0.66f, 25f),
            Triple(w * 0.74f, h * 0.75f, 22f),
            Triple(w * 0.78f, h * 0.70f, 28f)
        )

        for ((mx, my, msize) in mountainPeaks) {
            val mountainPath = Path().apply {
                moveTo(mx, my - msize)
                lineTo(mx - msize * 0.9f, my + msize * 0.7f)
                lineTo(mx + msize * 0.9f, my + msize * 0.7f)
                close()
            }
            // Mountain base
            drawPath(
                path = mountainPath,
                color = Color(0xFF1E293B).copy(alpha = 0.7f)
            )
            // Mountain snow/highlight crest
            val snowCap = Path().apply {
                moveTo(mx, my - msize)
                lineTo(mx - msize * 0.4f, my - msize * 0.2f)
                lineTo(mx + msize * 0.4f, my - msize * 0.2f)
                close()
            }
            drawPath(
                path = snowCap,
                color = Color(0xFFBAE6FD).copy(alpha = 0.5f)
            )
        }

        // 5. Stylized Forest Tree Clusters (Forest biome: x: 4% to 26%)
        val treeSpots = listOf(
            Offset(w * 0.05f, h * 0.20f), Offset(w * 0.08f, h * 0.16f), Offset(w * 0.11f, h * 0.22f),
            Offset(w * 0.06f, h * 0.65f), Offset(w * 0.09f, h * 0.70f), Offset(w * 0.13f, h * 0.66f),
            Offset(w * 0.16f, h * 0.38f), Offset(w * 0.19f, h * 0.42f), Offset(w * 0.22f, h * 0.36f)
        )
        for (spot in treeSpots) {
            drawCircle(
                color = Color(0xFF15803D).copy(alpha = 0.35f),
                radius = 8f,
                center = spot
            )
            drawCircle(
                color = Color(0xFF22C55E).copy(alpha = 0.45f),
                radius = 5f,
                center = spot
            )
        }

        // 6. Sector IV Citadel Arcane Runes & Fissures (Right side: 80% to 100%)
        val citadelCenterX = w * 0.90f
        val citadelCenterY = h * 0.48f

        // Outer Arcane Ring
        drawCircle(
            color = Color(0x33A855F7),
            radius = 65f,
            center = Offset(citadelCenterX, citadelCenterY),
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), 0f))
        )
        drawCircle(
            color = Color(0x1AD8B4FE),
            radius = 110f,
            center = Offset(citadelCenterX, citadelCenterY)
        )

        // Arcane ground fissures
        val fissure = Path().apply {
            moveTo(citadelCenterX - 45f, citadelCenterY + 30f)
            lineTo(citadelCenterX - 15f, citadelCenterY + 10f)
            lineTo(citadelCenterX + 25f, citadelCenterY + 25f)
            lineTo(citadelCenterX + 55f, citadelCenterY - 15f)
        }
        drawPath(
            path = fissure,
            color = Color(0xFFC084FC).copy(alpha = 0.4f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // 7. Tactical Vignette Overlay (Darkened borders for clean HUD contrast)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x77020617)),
                center = Offset(w / 2f, h / 2f),
                radius = (w / 2f).coerceAtLeast(h / 2f) * 1.2f
            ),
            size = size
        )
    }
}
