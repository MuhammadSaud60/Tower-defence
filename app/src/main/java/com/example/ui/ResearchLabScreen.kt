package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.data.ProgressionManager
import com.example.data.ResearchCatalog
import com.example.data.ResearchCategory
import com.example.data.ResearchNode
import com.example.data.StatBonusType
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameCurrencyBadge
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Redesigned Tower Defense Upgrade & Research Center.
 * Meets all 15 directives:
 * - Top showcase with rich procedural tower render showing barrel attachments, base armor, and glowing energy cores
 * - Visual Stat Bars (Damage, Range, Fire Rate)
 * - Visual branching node tree (Purchased / Available / Locked)
 * - Interactive node selector with bottom upgrade station & upgrade shockwave animation
 */
@Composable
fun ResearchLabScreen(
    progressionManager: ProgressionManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(ResearchCategory.MACHINE_GUN) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val currentTokens = remember(refreshTrigger) { progressionManager.getTokens() }
    val categoryNodes = remember(selectedCategory, refreshTrigger) {
        ResearchCatalog.getNodesForCategory(selectedCategory)
    }

    // Currently selected node for inspection/upgrade
    var selectedNodeId by remember(selectedCategory) {
        mutableStateOf(categoryNodes.firstOrNull()?.id)
    }
    val selectedNode = categoryNodes.find { it.id == selectedNodeId } ?: categoryNodes.firstOrNull()

    // Level-up punch animation
    val upgradeFlashAnim = remember { Animatable(0f) }
    val towerScaleAnim = remember { Animatable(1f) }

    // Calculate total upgrade level in this category
    val totalRanks = remember(selectedCategory, refreshTrigger) {
        categoryNodes.sumOf { progressionManager.getResearchRank(it.id) }
    }
    val maxPossibleRanks = remember(selectedCategory) {
        categoryNodes.sumOf { it.maxRank }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF070B14), Color(0xFF0F172A), Color(0xFF0B1120))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("research_lab_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // 1. TOP HEADER: Navigation, Screen Title & Tokens Bank
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            audioPlayer.buttonClick()
                            onBack()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                            .testTag("research_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "RESEARCH LAB",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Permanent tech blueprints & weapons modifications",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                GameCurrencyBadge(
                    amount = currentTokens,
                    isCompact = false
                )
            }

            // 2. CATEGORY TABS SELECTOR (Horizontal with rank counts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ResearchCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    val isLocked = category.towerType != null && !progressionManager.isTowerUnlocked(category.towerType)
                    val catRanks = ResearchCatalog.getNodesForCategory(category).sumOf { progressionManager.getResearchRank(it.id) }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B).copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .clickable {
                                audioPlayer.buttonClick()
                                selectedCategory = category
                                selectedNodeId = ResearchCatalog.getNodesForCategory(category).firstOrNull()?.id
                            }
                            .testTag("research_tab_${category.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = category.displayName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) Color.White else if (isLocked) Color(0xFF64748B) else Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$catRanks",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. COMPACT TOWER OVERVIEW (Slim, non-bloated tactical card)
            CompactTowerOverviewCard(
                category = selectedCategory,
                totalRanks = totalRanks,
                maxRanks = maxPossibleRanks,
                towerScale = towerScaleAnim.value,
                flashAlpha = upgradeFlashAnim.value
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4. MAIN TREE VIEWPORT (Fits in viewport without endless scrolling)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                VisualUpgradeTree(
                    nodes = categoryNodes,
                    selectedNodeId = selectedNodeId,
                    progressionManager = progressionManager,
                    onSelectNode = { nodeId ->
                        audioPlayer.buttonClick()
                        selectedNodeId = nodeId
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. DOCKED BOTTOM ACTION STATION: Instantly accessible, thumb-friendly
            if (selectedNode != null) {
                val currentRank = progressionManager.getResearchRank(selectedNode.id)
                val isMaxRank = currentRank >= selectedNode.maxRank
                val canUpgrade = progressionManager.canUpgradeResearch(selectedNode)
                val prereqMet = selectedNode.prerequisiteId?.let {
                    progressionManager.getResearchRank(it) >= selectedNode.prerequisiteRank
                } ?: true
                val cost = if (!isMaxRank) selectedNode.getCostForRank(currentRank) else 0

                DockedUpgradeActionStation(
                    node = selectedNode,
                    currentRank = currentRank,
                    isMaxRank = isMaxRank,
                    canUpgrade = canUpgrade,
                    prereqMet = prereqMet,
                    cost = cost,
                    onUpgradeClick = {
                        if (progressionManager.upgradeResearch(selectedNode)) {
                            audioPlayer.towerUpgraded()
                            coroutineScope.launch {
                                towerScaleAnim.animateTo(1.15f, tween(100))
                                upgradeFlashAnim.animateTo(0.8f, tween(80))
                                upgradeFlashAnim.animateTo(0f, tween(200))
                                towerScaleAnim.animateTo(1.0f, tween(150))
                            }
                            refreshTrigger++
                        } else {
                            audioPlayer.invalidPlacement()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Compact Overview Bar for Tower visual representation, stats meters, and level badge.
 */
@Composable
private fun CompactTowerOverviewCard(
    category: ResearchCategory,
    totalRanks: Int,
    maxRanks: Int,
    towerScale: Float,
    flashAlpha: Float
) {
    val specialty = when (category) {
        ResearchCategory.MACHINE_GUN -> "KINETIC PIERCING"
        ResearchCategory.CANNON -> "EXPLOSIVE AOE"
        ResearchCategory.RAPID_FIRE -> "ENERGY BEAMS"
        ResearchCategory.FROST_GUN -> "CRYOGENIC CHILL"
        ResearchCategory.CITADEL -> "FORTRESS BASTION"
    }

    val (dmgRatio, rngRatio, spdRatio) = when (category) {
        ResearchCategory.MACHINE_GUN -> Triple(0.55f + (totalRanks * 0.035f), 0.50f + (totalRanks * 0.03f), 0.85f + (totalRanks * 0.02f))
        ResearchCategory.CANNON -> Triple(0.90f + (totalRanks * 0.03f), 0.70f + (totalRanks * 0.03f), 0.35f + (totalRanks * 0.04f))
        ResearchCategory.RAPID_FIRE -> Triple(0.40f + (totalRanks * 0.035f), 0.60f + (totalRanks * 0.03f), 0.95f + (totalRanks * 0.01f))
        ResearchCategory.FROST_GUN -> Triple(0.30f + (totalRanks * 0.03f), 0.55f + (totalRanks * 0.03f), 0.60f + (totalRanks * 0.03f))
        ResearchCategory.CITADEL -> Triple(0.50f, 0.50f, 0.50f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Mini Tower Graphic
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF0A0F1D), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                TowerProceduralGraphic(
                    category = category,
                    upgradeTier = (totalRanks / 3).coerceIn(0, 3),
                    modifier = Modifier
                        .size(40.dp)
                        .scale(towerScale)
                )

                if (flashAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = flashAlpha), RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Name & Specialty & Level
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.displayName.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LV $totalRanks/$maxRanks",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                Text(
                    text = specialty,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: 3 Mini Stat Bars
            Column(
                modifier = Modifier.width(76.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CompactStatBar(label = "DMG", ratio = dmgRatio.coerceIn(0.1f, 1f), color = Color(0xFFEF4444))
                CompactStatBar(label = "RNG", ratio = rngRatio.coerceIn(0.1f, 1f), color = Color(0xFF38BDF8))
                CompactStatBar(label = "SPD", ratio = spdRatio.coerceIn(0.1f, 1f), color = Color(0xFF10B981))
            }
        }
    }
}

/**
 * Mini high-contrast stat bar.
 */
@Composable
private fun CompactStatBar(label: String, ratio: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF94A3B8),
            modifier = Modifier.width(22.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * Procedural Canvas Illustration of Towers showing:
 * - Octagonal / reinforced base
 * - Turret mount & weapon barrels
 * - Level differences (single barrel -> dual heavy barrels -> quad/glowing high-tech barrels)
 */
@Composable
private fun TowerProceduralGraphic(
    category: ResearchCategory,
    upgradeTier: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tower_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // 1. Base Plate (Heavy Armor Hexagon / Octagon)
        val baseColor = Color(0xFF334155)
        val baseBorder = Color(0xFF64748B)

        drawCircle(
            color = Color(0xFF0F172A),
            radius = w * 0.44f,
            center = Offset(cx, cy + 6f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF475569), Color(0xFF1E293B)),
                center = Offset(cx, cy),
                radius = w * 0.42f
            ),
            radius = w * 0.40f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = baseBorder,
            radius = w * 0.40f,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )

        // Corner Rivets on Base
        for (i in 0 until 6) {
            val angle = i * (Math.PI * 2 / 6).toFloat()
            val rx = cx + cos(angle) * (w * 0.32f)
            val ry = cy + sin(angle) * (w * 0.32f)
            drawCircle(color = Color(0xFF94A3B8), radius = 3f, center = Offset(rx, ry))
        }

        // 2. Weapon Chassis & Barrels
        when (category) {
            ResearchCategory.MACHINE_GUN -> {
                // Turret Mantlet Ball
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF1E293B))),
                    radius = w * 0.22f,
                    center = Offset(cx, cy)
                )
                drawCircle(color = Color(0xFF94A3B8), radius = w * 0.22f, center = Offset(cx, cy), style = Stroke(2f))

                // Barrels: 1 barrel (tier 0), 2 barrels (tier 1-2), 4 barrels (tier 3)
                if (upgradeTier >= 2) {
                    // Dual / Quad Barrels
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx - 14f, cy - 38f),
                        size = Size(8f, 32f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx + 6f, cy - 38f),
                        size = Size(8f, 32f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Muzzle flash brakes
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(cx - 16f, cy - 42f), size = Size(12f, 5f))
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(cx + 4f, cy - 42f), size = Size(12f, 5f))
                } else {
                    // Single Heavy Barrel
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - 6f, cy - 36f),
                        size = Size(12f, 30f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    drawRect(color = Color(0xFFF59E0B), topLeft = Offset(cx - 8f, cy - 40f), size = Size(16f, 5f))
                }

                // Core indicator
                drawCircle(color = Color(0xFF22C55E).copy(alpha = glowPulse), radius = 5f, center = Offset(cx, cy))
            }

            ResearchCategory.CANNON -> {
                // Heavy Artillery Mantlet
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF475569), Color(0xFF0F172A))),
                    topLeft = Offset(cx - 22f, cy - 16f),
                    size = Size(44f, 32f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Large Bore Cannon Barrel
                val barrelW = if (upgradeTier >= 2) 20f else 16f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))),
                    topLeft = Offset(cx - barrelW / 2f, cy - 44f),
                    size = Size(barrelW, 36f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Heavy Muzzle Brake
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(cx - (barrelW + 8f) / 2f, cy - 48f),
                    size = Size(barrelW + 8f, 8f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Blast coils
                if (upgradeTier >= 1) {
                    drawRect(color = Color(0xFFF97316), topLeft = Offset(cx - barrelW / 2f, cy - 32f), size = Size(barrelW, 4f))
                    drawRect(color = Color(0xFFF97316), topLeft = Offset(cx - barrelW / 2f, cy - 22f), size = Size(barrelW, 4f))
                }
            }

            ResearchCategory.RAPID_FIRE -> {
                // High-Tech Capacitor Core
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF0F172A))),
                    radius = w * 0.24f,
                    center = Offset(cx, cy)
                )
                // Dual energy pylons
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(cx - 18f, cy - 40f),
                    size = Size(6f, 34f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(cx + 12f, cy - 40f),
                    size = Size(6f, 34f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Energy corona arc
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.8f * glowPulse),
                    radius = 9f,
                    center = Offset(cx, cy - 20f)
                )
            }

            ResearchCategory.FROST_GUN -> {
                // Cryo Dispersion Dish
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFBAE6FD), Color(0xFF0284C7), Color(0xFF082F49))),
                    radius = w * 0.26f,
                    center = Offset(cx, cy)
                )
                // Cryo Nozzle
                drawRoundRect(
                    color = Color(0xFFE0F2FE),
                    topLeft = Offset(cx - 10f, cy - 36f),
                    size = Size(20f, 28f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                // Frost Crystal Spikes
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * glowPulse),
                    radius = 7f,
                    center = Offset(cx, cy)
                )
            }

            ResearchCategory.CITADEL -> {
                // Bastion Fortress Shield
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0369A1))),
                    topLeft = Offset(cx - 24f, cy - 24f),
                    size = Size(48f, 48f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawCircle(color = Color(0xFFFBBF24), radius = 10f, center = Offset(cx, cy))
            }
        }
    }
}

/**
 * Compact Visual Upgrade Tree displaying Tier 1 -> Tier 2 -> Tier 3 nodes
 * connected with clear branch lines and node states (Purchased / Available / Locked).
 */
@Composable
private fun VisualUpgradeTree(
    nodes: List<ResearchNode>,
    selectedNodeId: String?,
    progressionManager: ProgressionManager,
    onSelectNode: (String) -> Unit
) {
    val tier1Nodes = nodes.filter { it.tier == 1 }
    val tier2Nodes = nodes.filter { it.tier == 2 }
    val tier3Nodes = nodes.filter { it.tier == 3 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0F1D), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // TIER 1: ROOT NODE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            tier1Nodes.forEach { node ->
                CompactTreeNodeItem(
                    node = node,
                    isSelected = node.id == selectedNodeId,
                    progressionManager = progressionManager,
                    onClick = { onSelectNode(node.id) }
                )
            }
        }

        // Branch Connector
        if (tier2Nodes.isNotEmpty()) {
            CompactTreeBranchConnector()
        }

        // TIER 2: SPECIALIZED BRANCHES
        if (tier2Nodes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                tier2Nodes.forEach { node ->
                    CompactTreeNodeItem(
                        node = node,
                        isSelected = node.id == selectedNodeId,
                        progressionManager = progressionManager,
                        onClick = { onSelectNode(node.id) },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }

        // Branch Connector
        if (tier3Nodes.isNotEmpty()) {
            CompactTreeBranchConnector()
        }

        // TIER 3: CAPSTONE WARFARE
        if (tier3Nodes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                tier3Nodes.forEach { node ->
                    CompactTreeNodeItem(
                        node = node,
                        isSelected = node.id == selectedNodeId,
                        progressionManager = progressionManager,
                        onClick = { onSelectNode(node.id) }
                    )
                }
            }
        }
    }
}

/**
 * Visual connector line between tree tiers.
 */
@Composable
private fun CompactTreeBranchConnector() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val midX = size.width / 2f
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.5f),
            start = Offset(midX, 0f),
            end = Offset(midX, size.height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
        )
    }
}

/**
 * Compact Individual Node Item in the upgrade tree.
 * States:
 * - Purchased (Max rank)
 * - Available (can upgrade or partially upgraded)
 * - Locked (prerequisites not met)
 */
@Composable
private fun CompactTreeNodeItem(
    node: ResearchNode,
    isSelected: Boolean,
    progressionManager: ProgressionManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRank = progressionManager.getResearchRank(node.id)
    val isMaxRank = currentRank >= node.maxRank
    val canUpgrade = progressionManager.canUpgradeResearch(node)
    val isPrereqMet = node.prerequisiteId?.let {
        progressionManager.getResearchRank(it) >= node.prerequisiteRank
    } ?: true

    val isLocked = !isPrereqMet

    val borderColor = when {
        isSelected -> Color(0xFFF59E0B)
        isMaxRank -> Color(0xFF10B981)
        canUpgrade -> Color(0xFF38BDF8)
        isLocked -> Color(0xFF334155)
        else -> Color(0xFF475569)
    }

    val backgroundColor = when {
        isSelected -> Color(0xFF1E293B)
        isMaxRank -> Color(0xFF064E3B).copy(alpha = 0.55f)
        canUpgrade -> Color(0xFF0C4A6E).copy(alpha = 0.55f)
        else -> Color(0xFF0F172A)
    }

    val statIcon = when (node.statType) {
        StatBonusType.DAMAGE -> Icons.Default.ElectricBolt
        StatBonusType.FIRE_RATE, StatBonusType.ATTACK_SPEED, StatBonusType.RELOAD_SPEED, StatBonusType.PROJECTILE_SPEED -> Icons.Default.Speed
        StatBonusType.ARMOR_PENETRATION, StatBonusType.BASE_HP -> Icons.Default.Shield
        else -> Icons.Default.Science
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .widthIn(min = 120.dp, max = 150.dp)
            .clickable { onClick() }
            .testTag("node_${node.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon (status or stat type icon)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else statIcon,
                        contentDescription = null,
                        tint = when {
                            isMaxRank -> Color(0xFF34D399)
                            isLocked -> Color(0xFF64748B)
                            canUpgrade -> Color(0xFF38BDF8)
                            else -> Color(0xFFFBBF24)
                        },
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Rank indicator "2/4"
                Text(
                    text = "$currentRank/${node.maxRank}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isMaxRank) Color(0xFF34D399) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = node.name,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) Color(0xFF64748B) else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = node.statBonusLabel,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = if (isLocked) Color(0xFF475569) else Color(0xFFFBBF24),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Visual Rank Pips [•][•][ ][ ]
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..node.maxRank) {
                    val isFilled = i <= currentRank
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = if (isFilled) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Docked Bottom Action Station: Displays the selected node's details (Name, Stat bonus, Description,
 * token cost) and a tactile UPGRADE button docked right at the bottom.
 */
@Composable
private fun DockedUpgradeActionStation(
    node: ResearchNode,
    currentRank: Int,
    isMaxRank: Boolean,
    canUpgrade: Boolean,
    prereqMet: Boolean,
    cost: Int,
    onUpgradeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Node info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.name.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMaxRank) "MAX RANK" else "RANK $currentRank/${node.maxRank}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMaxRank) Color(0xFF34D399) else Color(0xFF38BDF8)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = node.statBonusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = node.description,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Instant Action Button
            GameButton(
                text = when {
                    isMaxRank -> "MAX RANK"
                    !prereqMet -> "LOCKED"
                    canUpgrade -> "UPGRADE ($cost 🪙)"
                    else -> "NEED $cost 🪙"
                },
                icon = when {
                    isMaxRank -> Icons.Default.Check
                    !prereqMet -> Icons.Default.Lock
                    canUpgrade -> Icons.Default.Upgrade
                    else -> Icons.Default.Lock
                },
                variant = when {
                    isMaxRank -> GameButtonVariant.SECONDARY
                    canUpgrade -> GameButtonVariant.PRIMARY
                    else -> GameButtonVariant.SECONDARY
                },
                height = 38.dp,
                enabled = canUpgrade,
                isLocked = !prereqMet,
                onClick = onUpgradeClick,
                modifier = Modifier.widthIn(min = 125.dp, max = 155.dp),
                testTag = "upgrade_action_button"
            )
        }
    }
}
