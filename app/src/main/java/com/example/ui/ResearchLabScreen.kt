package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Mobile Tower Defense Research Lab & Armory Screen.
 *
 * Designed specifically for strategy/tower defense games:
 * - Large visual weapon/tower artwork showcase in the center/hero area.
 * - Visual weapon upgrades with attachments, level shifts, and holographic turntable.
 * - Interactive technology tree with connected glowing energy conduits.
 * - Clear locked/unlocked/purchased states with tactical icons and short stat badges.
 * - Upgrade animations: weapon glow shockwave, flash effect, floating level-up toast.
 * - Responsive layout optimized for Android landscape screens.
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

    var selectedNodeId by remember(selectedCategory) {
        mutableStateOf(categoryNodes.firstOrNull()?.id)
    }
    val selectedNode = categoryNodes.find { it.id == selectedNodeId } ?: categoryNodes.firstOrNull()

    // Upgrade animations
    val weaponGlowScale = remember { Animatable(1f) }
    val weaponGlowAlpha = remember { Animatable(0f) }
    val upgradeFlashAnim = remember { Animatable(0f) }
    val towerScaleAnim = remember { Animatable(1f) }
    val toastAlpha = remember { Animatable(0f) }
    val toastOffsetY = remember { Animatable(0f) }
    var toastText by remember { mutableStateOf("") }

    // Category ranks & mastery
    val totalRanks = remember(selectedCategory, refreshTrigger) {
        categoryNodes.sumOf { progressionManager.getResearchRank(it.id) }
    }
    val maxPossibleRanks = remember(selectedCategory) {
        categoryNodes.sumOf { it.maxRank }
    }

    // Set of unlocked node IDs for rendering specific attachments
    val unlockedNodeIds = remember(selectedCategory, refreshTrigger) {
        categoryNodes.filter { progressionManager.getResearchRank(it.id) > 0 }.map { it.id }.toSet()
    }

    // Continuous ambient animations
    val infiniteTransition = rememberInfiniteTransition(label = "armory_ambient")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )
    val conduitPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "conduit"
    )
    val ambientTurntableAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turntable"
    )

    fun performUpgrade(node: ResearchNode) {
        if (progressionManager.upgradeResearch(node)) {
            audioPlayer.towerUpgraded()
            toastText = "+1 ${node.name.uppercase()}"
            coroutineScope.launch {
                // Trigger shockwave & flash & bounce & toast
                launch {
                    weaponGlowScale.snapTo(0.85f)
                    weaponGlowAlpha.snapTo(0.9f)
                    weaponGlowScale.animateTo(1.6f, tween(400, easing = FastOutSlowInEasing))
                    weaponGlowAlpha.animateTo(0f, tween(200))
                }
                launch {
                    upgradeFlashAnim.snapTo(0.8f)
                    upgradeFlashAnim.animateTo(0f, tween(250))
                }
                launch {
                    towerScaleAnim.snapTo(1f)
                    towerScaleAnim.animateTo(1.2f, tween(100))
                    towerScaleAnim.animateTo(1f, tween(200))
                }
                launch {
                    toastOffsetY.snapTo(10f)
                    toastAlpha.snapTo(1f)
                    toastOffsetY.animateTo(-24f, tween(600, easing = FastOutSlowInEasing))
                    toastAlpha.animateTo(0f, tween(300))
                }
            }
            refreshTrigger++
        } else {
            audioPlayer.invalidPlacement()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B14),
                        Color(0xFF0F172A),
                        Color(0xFF080D1A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("research_lab_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // ==========================================
            // 1. TOP BAR: Back, Title, Tabs, Currency
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Back button & Screen Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            audioPlayer.buttonClick()
                            onBack()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), CircleShape)
                            .testTag("research_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RESEARCH LAB",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "WEAPON UPGRADE ARMORY",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                // Center/Right: Category Switcher Tabs
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ResearchCategory.values().forEach { category ->
                        val isSelected = category == selectedCategory
                        val isLocked = category.towerType != null && !progressionManager.isTowerUnlocked(category.towerType)
                        val catRanks = ResearchCatalog.getNodesForCategory(category).sumOf { progressionManager.getResearchRank(it.id) }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B).copy(alpha = 0.85f),
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = category.displayName.uppercase(),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = if (isSelected) Color.White else if (isLocked) Color(0xFF64748B) else Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$catRanks",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                // Right: Currency Tokens Badge
                GameCurrencyBadge(
                    amount = currentTokens,
                    isCompact = true
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // ==========================================
            // RESPONSIVE BODY: Landscape vs Portrait
            // ==========================================
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isLandscape = maxWidth >= 580.dp

                if (isLandscape) {
                    // Two-Column Strategy Armory Layout for Landscape
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // LEFT / CENTER HERO: Large Tower/Weapon Artwork Showcase
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                        ) {
                            LargeWeaponShowcaseCard(
                                category = selectedCategory,
                                totalRanks = totalRanks,
                                maxRanks = maxPossibleRanks,
                                unlockedNodeIds = unlockedNodeIds,
                                towerScale = towerScaleAnim.value,
                                glowScale = weaponGlowScale.value,
                                glowAlpha = weaponGlowAlpha.value,
                                flashAlpha = upgradeFlashAnim.value,
                                radarAngle = radarAngle,
                                turntableAngle = ambientTurntableAngle,
                                toastText = toastText,
                                toastAlpha = toastAlpha.value,
                                toastOffsetY = toastOffsetY.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // RIGHT / BOTTOM: Interactive Tech Tree + Docked Upgrade Action Bar
                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        ) {
                            // Technology Strategy Tree Viewport
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                TechTreeStrategyView(
                                    nodes = categoryNodes,
                                    selectedNodeId = selectedNodeId,
                                    progressionManager = progressionManager,
                                    conduitPhase = conduitPulsePhase,
                                    onSelectNode = { nodeId ->
                                        audioPlayer.buttonClick()
                                        selectedNodeId = nodeId
                                    },
                                    onUpgradeNode = { node ->
                                        performUpgrade(node)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Docked Upgrade Action Bar
                            if (selectedNode != null) {
                                DockedUpgradeActionStation(
                                    node = selectedNode,
                                    progressionManager = progressionManager,
                                    onUpgrade = { performUpgrade(selectedNode) }
                                )
                            }
                        }
                    }
                } else {
                    // Vertical Stack Layout for Portrait
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            LargeWeaponShowcaseCard(
                                category = selectedCategory,
                                totalRanks = totalRanks,
                                maxRanks = maxPossibleRanks,
                                unlockedNodeIds = unlockedNodeIds,
                                towerScale = towerScaleAnim.value,
                                glowScale = weaponGlowScale.value,
                                glowAlpha = weaponGlowAlpha.value,
                                flashAlpha = upgradeFlashAnim.value,
                                radarAngle = radarAngle,
                                turntableAngle = ambientTurntableAngle,
                                toastText = toastText,
                                toastAlpha = toastAlpha.value,
                                toastOffsetY = toastOffsetY.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                        ) {
                            TechTreeStrategyView(
                                nodes = categoryNodes,
                                selectedNodeId = selectedNodeId,
                                progressionManager = progressionManager,
                                conduitPhase = conduitPulsePhase,
                                onSelectNode = { nodeId ->
                                    audioPlayer.buttonClick()
                                    selectedNodeId = nodeId
                                },
                                onUpgradeNode = { node ->
                                    performUpgrade(node)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedNode != null) {
                            DockedUpgradeActionStation(
                                node = selectedNode,
                                progressionManager = progressionManager,
                                onUpgrade = { performUpgrade(selectedNode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. LARGE WEAPON / TOWER ARTWORK SHOWCASE
// =========================================================================

/**
 * Hero Centerpiece Card displaying:
 * - High-tech Hologram Turntable Platform with radar scanner and energy rings.
 * - High-resolution procedural weapon artwork with attachments and level differences.
 * - Dynamic weapon upgrade glow, level-up punch, and floating celebration banner.
 * - Tactical Level badge, Progress bar, and short stat chips with icons.
 */
@Composable
private fun LargeWeaponShowcaseCard(
    category: ResearchCategory,
    totalRanks: Int,
    maxRanks: Int,
    unlockedNodeIds: Set<String>,
    towerScale: Float,
    glowScale: Float,
    glowAlpha: Float,
    flashAlpha: Float,
    radarAngle: Float,
    turntableAngle: Float,
    toastText: String,
    toastAlpha: Float,
    toastOffsetY: Float,
    modifier: Modifier = Modifier
) {
    // Determine level tier (1..3)
    val weaponLevel = when {
        totalRanks >= 12 -> 3
        totalRanks >= 6 -> 2
        else -> 1
    }

    val specialty = when (category) {
        ResearchCategory.MACHINE_GUN -> "KINETIC BALLISTIC PIERCING"
        ResearchCategory.CANNON -> "HEAVY ARTILLERY & EXPLOSIVE AOE"
        ResearchCategory.RAPID_FIRE -> "ELECTROMAGNETIC ENERGY BEAMS"
        ResearchCategory.FROST_GUN -> "CRYOGENIC CHILL & MOVEMENT SLOW"
        ResearchCategory.CITADEL -> "FORTIFIED BASTION COMMAND"
    }

    val (dmgBonus, rateBonus, rngBonus, specBonus) = when (category) {
        ResearchCategory.MACHINE_GUN -> Quad(
            "+${totalRanks * 5}%",
            "+${totalRanks * 5}%",
            "+${totalRanks * 3}%",
            "+${totalRanks * 4}% AP"
        )
        ResearchCategory.CANNON -> Quad(
            "+${totalRanks * 5}%",
            "+${totalRanks * 5}%",
            "+${totalRanks * 3}%",
            "+${totalRanks * 5}% AOE"
        )
        ResearchCategory.RAPID_FIRE -> Quad(
            "+${totalRanks * 4}%",
            "+${totalRanks * 5}%",
            "+${totalRanks * 5}%",
            "+${totalRanks * 3}% CRIT"
        )
        ResearchCategory.FROST_GUN -> Quad(
            "+${totalRanks * 4}%",
            "+${totalRanks * 3}%",
            "+${totalRanks * 3}%",
            "+${totalRanks * 5}% SLOW"
        )
        ResearchCategory.CITADEL -> Quad(
            "+${totalRanks * 10}% HP",
            "+${totalRanks * 15} 🪙",
            "-",
            "REINFORCED"
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (maxRanks > 0) totalRanks.toFloat() / maxRanks.toFloat() else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF38BDF8).copy(alpha = 0.8f), Color(0xFF1E293B))
            )
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Weapon name, Level Badge & Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category.displayName.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Text(
                        text = specialty,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFFFBBF24)
                    )
                }

                // Level Stars & Tier Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= weaponLevel) Color(0xFFFBBF24) else Color(0xFF475569),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TIER $weaponLevel",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }

            // CENTER HERO ARTWORK CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawHologramWeaponPlatform(
                        category = category,
                        level = weaponLevel,
                        unlockedNodeIds = unlockedNodeIds,
                        towerScale = towerScale,
                        glowScale = glowScale,
                        glowAlpha = glowAlpha,
                        flashAlpha = flashAlpha,
                        radarAngle = radarAngle,
                        turntableAngle = turntableAngle
                    )
                }

                // Floating Upgrade Notification Banner
                if (toastAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(y = toastOffsetY.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0284C7))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Upgrade,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = toastText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // BOTTOM METERS: Progress Bar & 4 Quick Stat Badges
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Progress Bar: X / Y Blueprints Unlocked
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RESEARCH PROGRESS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "$totalRanks / $maxRanks NODES (${(animatedProgress * 100).toInt()}%)",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF38BDF8)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFFBBF24))
                                ),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }

                // 4 Tactical Short Stat Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TacticalStatChip(
                        icon = Icons.Default.Upgrade,
                        label = "DMG",
                        value = dmgBonus,
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    TacticalStatChip(
                        icon = Icons.Default.Speed,
                        label = "RATE",
                        value = rateBonus,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    TacticalStatChip(
                        icon = Icons.Default.Star,
                        label = "RNG",
                        value = rngBonus,
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    TacticalStatChip(
                        icon = Icons.Default.Security,
                        label = "SPEC",
                        value = specBonus,
                        accentColor = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Compact high-contrast tactical stat chip.
 */
@Composable
private fun TacticalStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(9.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$label ",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

// =========================================================================
// 3. PROCEDURAL HOLOGRAPHIC WEAPON & PLATFORM DRAWING
// =========================================================================

/**
 * Detailed procedural Canvas rendering of the large weapon, attachments, and hologram turntable.
 */
private fun DrawScope.drawHologramWeaponPlatform(
    category: ResearchCategory,
    level: Int,
    unlockedNodeIds: Set<String>,
    towerScale: Float,
    glowScale: Float,
    glowAlpha: Float,
    flashAlpha: Float,
    radarAngle: Float,
    turntableAngle: Float
) {
    val cx = size.width / 2f
    val cy = size.height / 2f + 4f
    val baseRadius = (size.minDimension * 0.38f).coerceIn(40f, 90f)

    // 1. Holographic Floor Calibration Grid & Concentric Rings
    drawCircle(
        color = Color(0x1538BDF8),
        radius = baseRadius * 1.35f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0x3538BDF8),
        radius = baseRadius * 1.2f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
    )
    drawCircle(
        color = Color(0x2238BDF8),
        radius = baseRadius * 0.9f,
        center = Offset(cx, cy),
        style = Stroke(width = 1f)
    )

    // 4 Hologram Degree Markers (0, 90, 180, 270)
    for (i in 0..3) {
        val ang = (i * PI / 2.0).toFloat()
        val tickX1 = cx + cos(ang) * (baseRadius * 1.15f)
        val tickY1 = cy + sin(ang) * (baseRadius * 1.15f)
        val tickX2 = cx + cos(ang) * (baseRadius * 1.25f)
        val tickY2 = cy + sin(ang) * (baseRadius * 1.25f)
        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(tickX1, tickY1),
            end = Offset(tickX2, tickY2),
            strokeWidth = 2f
        )
    }

    // 2. Rotating Radar Scanner Beam
    val radarRad = Math.toRadians(radarAngle.toDouble())
    val sweepX = cx + cos(radarRad).toFloat() * (baseRadius * 1.2f)
    val sweepY = cy + sin(radarRad).toFloat() * (baseRadius * 1.2f)
    drawLine(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8), Color.Transparent),
            center = Offset(cx, cy),
            radius = baseRadius * 1.2f
        ),
        start = Offset(cx, cy),
        end = Offset(sweepX, sweepY),
        strokeWidth = 2.5f
    )

    // 3. Upgrade Shockwave Burst Aura
    if (glowAlpha > 0.01f) {
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.5f),
            radius = baseRadius * 1.3f * glowScale,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFBBF24).copy(alpha = glowAlpha),
            radius = baseRadius * 1.1f * glowScale,
            center = Offset(cx, cy),
            style = Stroke(width = 4f * glowAlpha)
        )
    }

    // 4. Fortified Gunmetal Turntable Pedestal
    drawCircle(color = Color(0xFF0F172A), radius = baseRadius, center = Offset(cx, cy + 4f))
    drawCircle(color = Color(0xFF1E293B), radius = baseRadius, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF334155), radius = baseRadius - 4f, center = Offset(cx, cy))
    drawCircle(
        color = Color(0xFF64748B),
        radius = baseRadius - 5f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )

    // Turntable Perimeter Hex Bolts
    for (i in 0..7) {
        val ang = (i * PI / 4.0).toFloat()
        val bx = cx + cos(ang) * (baseRadius * 0.88f)
        val by = cy + sin(ang) * (baseRadius * 0.88f)
        drawCircle(color = Color(0xFF0F172A), radius = 3.5f, center = Offset(bx, by))
        drawCircle(color = Color(0xFF94A3B8), radius = 2f, center = Offset(bx - 0.5f, by - 0.5f))
    }

    // 5. WEAPON ASSEMBLY WITH TOWER SCALE & SLIGHT AMBIENT ROTATION
    rotate(degrees = turntableAngle, pivot = Offset(cx, cy)) {
        val scaleFactor = towerScale * (baseRadius / 50f)

        when (category) {
            ResearchCategory.MACHINE_GUN -> {
                drawMachineGunShowcase(
                    cx = cx,
                    cy = cy,
                    level = level,
                    scale = scaleFactor,
                    unlockedNodeIds = unlockedNodeIds
                )
            }
            ResearchCategory.CANNON -> {
                drawCannonShowcase(
                    cx = cx,
                    cy = cy,
                    level = level,
                    scale = scaleFactor,
                    unlockedNodeIds = unlockedNodeIds
                )
            }
            ResearchCategory.RAPID_FIRE -> {
                drawRapidFireShowcase(
                    cx = cx,
                    cy = cy,
                    level = level,
                    scale = scaleFactor,
                    unlockedNodeIds = unlockedNodeIds
                )
            }
            ResearchCategory.FROST_GUN -> {
                drawFrostGunShowcase(
                    cx = cx,
                    cy = cy,
                    level = level,
                    scale = scaleFactor,
                    unlockedNodeIds = unlockedNodeIds
                )
            }
            ResearchCategory.CITADEL -> {
                drawCitadelShowcase(
                    cx = cx,
                    cy = cy,
                    level = level,
                    scale = scaleFactor,
                    unlockedNodeIds = unlockedNodeIds
                )
            }
        }
    }

    // 6. Upgrade Flash Overlay
    if (flashAlpha > 0.01f) {
        drawCircle(
            color = Color.White.copy(alpha = flashAlpha * 0.7f),
            radius = baseRadius * 1.4f,
            center = Offset(cx, cy)
        )
    }

    // 7. Tactical Hologram Corner Framing Brackets [ ]
    val bracketMargin = baseRadius * 1.25f
    val bracketLen = 14f
    val bracketColor = Color(0x8838BDF8)
    // Top-Left
    drawLine(bracketColor, Offset(cx - bracketMargin, cy - bracketMargin), Offset(cx - bracketMargin + bracketLen, cy - bracketMargin), strokeWidth = 2f)
    drawLine(bracketColor, Offset(cx - bracketMargin, cy - bracketMargin), Offset(cx - bracketMargin, cy - bracketMargin + bracketLen), strokeWidth = 2f)
    // Top-Right
    drawLine(bracketColor, Offset(cx + bracketMargin, cy - bracketMargin), Offset(cx + bracketMargin - bracketLen, cy - bracketMargin), strokeWidth = 2f)
    drawLine(bracketColor, Offset(cx + bracketMargin, cy - bracketMargin), Offset(cx + bracketMargin, cy - bracketMargin + bracketLen), strokeWidth = 2f)
    // Bottom-Left
    drawLine(bracketColor, Offset(cx - bracketMargin, cy + bracketMargin), Offset(cx - bracketMargin + bracketLen, cy + bracketMargin), strokeWidth = 2f)
    drawLine(bracketColor, Offset(cx - bracketMargin, cy + bracketMargin), Offset(cx - bracketMargin, cy - bracketMargin - bracketLen), strokeWidth = 2f)
    // Bottom-Right
    drawLine(bracketColor, Offset(cx + bracketMargin, cy + bracketMargin), Offset(cx + bracketMargin - bracketLen, cy + bracketMargin), strokeWidth = 2f)
    drawLine(bracketColor, Offset(cx + bracketMargin, cy + bracketMargin), Offset(cx + bracketMargin, cy + bracketMargin - bracketLen), strokeWidth = 2f)
}

// ---------------- MACHINE GUN SHOWCASE ----------------
private fun DrawScope.drawMachineGunShowcase(
    cx: Float,
    cy: Float,
    level: Int,
    scale: Float,
    unlockedNodeIds: Set<String>
) {
    val bLen = (28f + level * 6f) * scale
    val bThick = (4.5f + level * 0.8f) * scale

    // Level 1: Twin Barrels + Olive Ammo Box
    // Level 2: Extended Barrels + Front Mantlet Shield + Dual Ammo Boxes
    // Level 3: Quad Vulcan Gatling Cluster + Dual Drums + Heavy Angular Receiver

    // Side Ammo Boxes or Drums
    if (level >= 3) {
        // Dual Gatling Drums
        drawCircle(Color(0xFF0F172A), radius = 9f * scale, center = Offset(cx - 6f * scale, cy - 20f * scale))
        drawCircle(Color(0xFF166534), radius = 7f * scale, center = Offset(cx - 6f * scale, cy - 20f * scale))
        drawCircle(Color(0xFF0F172A), radius = 9f * scale, center = Offset(cx - 6f * scale, cy + 20f * scale))
        drawCircle(Color(0xFF166534), radius = 7f * scale, center = Offset(cx - 6f * scale, cy + 20f * scale))
    } else {
        // Side Box
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 10f * scale, cy - 20f * scale), Size(15f * scale, 10f * scale), CornerRadius(2f, 2f))
        drawRoundRect(Color(0xFF166534), Offset(cx - 9f * scale, cy - 19f * scale), Size(13f * scale, 8f * scale), CornerRadius(2f, 2f))
        if (level >= 2) {
            drawRoundRect(Color(0xFF0F172A), Offset(cx - 10f * scale, cy + 12f * scale), Size(15f * scale, 10f * scale), CornerRadius(2f, 2f))
            drawRoundRect(Color(0xFF166534), Offset(cx - 9f * scale, cy + 13f * scale), Size(13f * scale, 8f * scale), CornerRadius(2f, 2f))
        }
    }

    // Attachment: Golden Tungsten Sabot Belt (if unlocked)
    if (unlockedNodeIds.contains("mg_ap") || level >= 3) {
        for (b in 0..3) {
            drawRect(Color(0xFFF59E0B), Offset(cx - 8f * scale + b * 4f * scale, cy - 14f * scale), Size(2.8f * scale, 5f * scale))
        }
    }

    // Mantlet Armor Shield (Level 2 & 3)
    if (level >= 2) {
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 5f * scale, cy - 18f * scale), Size(11f * scale, 36f * scale), CornerRadius(4f, 4f))
        drawRoundRect(Color(0xFF475569), Offset(cx - 4f * scale, cy - 16f * scale), Size(8f * scale, 32f * scale), CornerRadius(3f, 3f))
    }

    // Barrels: Twin (Lvl 1-2) or Quad Vulcan (Lvl 3)
    if (level >= 3) {
        // 4 Gatling barrels
        drawRect(Color(0xFF0F172A), Offset(cx + 4f * scale, cy - 10f * scale), Size(bLen, 4f * scale))
        drawRect(Color(0xFF0F172A), Offset(cx + 4f * scale, cy - 4f * scale), Size(bLen + 3f * scale, 4.5f * scale))
        drawRect(Color(0xFF0F172A), Offset(cx + 4f * scale, cy + 2f * scale), Size(bLen + 3f * scale, 4.5f * scale))
        drawRect(Color(0xFF0F172A), Offset(cx + 4f * scale, cy + 8f * scale), Size(bLen, 4f * scale))
        // Brass clamps
        drawRoundRect(Color(0xFFD97706), Offset(cx + 22f * scale, cy - 12f * scale), Size(5f * scale, 26f * scale), CornerRadius(2f, 2f))
        drawRoundRect(Color(0xFFD97706), Offset(cx + bLen - 6f * scale, cy - 12f * scale), Size(5f * scale, 26f * scale), CornerRadius(2f, 2f))
    } else {
        // Twin Barrels
        drawRoundRect(Color(0xFF0F172A), Offset(cx + 3f * scale, cy - 9f * scale), Size(bLen, bThick), CornerRadius(2f, 2f))
        drawRect(Color(0xFF475569), Offset(cx + 4f * scale, cy - 8f * scale), Size(bLen - 5f * scale, 2f * scale))
        drawRoundRect(Color(0xFF0F172A), Offset(cx + 3f * scale, cy + 3f * scale), Size(bLen, bThick), CornerRadius(2f, 2f))
        drawRect(Color(0xFF475569), Offset(cx + 4f * scale, cy + 4f * scale), Size(bLen - 5f * scale, 2f * scale))
        // Slotted Muzzle Brakes
        drawRect(Color(0xFFCBD5E1), Offset(cx + bLen - 2f * scale, cy - 10f * scale), Size(5f * scale, 7f * scale))
        drawRect(Color(0xFFCBD5E1), Offset(cx + bLen - 2f * scale, cy + 2f * scale), Size(5f * scale, 7f * scale))
    }

    // Armored Receiver Body
    drawRoundRect(Color(0xFF1E293B), Offset(cx - 16f * scale, cy - 13f * scale), Size(22f * scale, 26f * scale), CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFF334155), Offset(cx - 14f * scale, cy - 11f * scale), Size(18f * scale, 22f * scale), CornerRadius(3f, 3f))

    // Attachment: Red Laser Aiming Scope (mg_range)
    if (unlockedNodeIds.contains("mg_range") || level >= 2) {
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 2f * scale, cy - 16f * scale), Size(12f * scale, 4f * scale), CornerRadius(1.5f, 1.5f))
        drawCircle(Color(0xFFEF4444), radius = 2.5f * scale, center = Offset(cx + 10f * scale, cy - 14f * scale))
        // Crimson targeting laser beam projecting forward
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color(0xCCEF4444), Color.Transparent)),
            start = Offset(cx + 10f * scale, cy - 14f * scale),
            end = Offset(cx + bLen + 40f * scale, cy - 14f * scale),
            strokeWidth = 1.5f
        )
    }

    // Attachment: Hydraulic Feeder Coil (mg_fire_rate)
    if (unlockedNodeIds.contains("mg_fire_rate") || level >= 2) {
        drawLine(Color(0xFF38BDF8), Offset(cx - 12f * scale, cy + 8f * scale), Offset(cx - 2f * scale, cy + 8f * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
    }

    // Center Gunner Cupola Dome with Glint
    drawCircle(Color(0xFF0F172A), radius = 13f * scale, center = Offset(cx, cy))
    drawCircle(Color(0xFF334155), radius = 10.5f * scale, center = Offset(cx, cy))
    drawCircle(Color(0xFF475569), radius = 7.5f * scale, center = Offset(cx - 2f * scale, cy - 2f * scale))
    drawCircle(Color.White, radius = 3f * scale, center = Offset(cx - 3.5f * scale, cy - 3.5f * scale))
}

// ---------------- CANNON SHOWCASE ----------------
private fun DrawScope.drawCannonShowcase(
    cx: Float,
    cy: Float,
    level: Int,
    scale: Float,
    unlockedNodeIds: Set<String>
) {
    val bLen = (36f + level * 8f) * scale
    val bThick = (18f + level * 5f) * scale

    // Side Hydraulic Recoil Assist Cylinders (Lvl 2-3)
    if (level >= 2 || unlockedNodeIds.contains("cannon_reload")) {
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 6f * scale, cy - 18f * scale), Size(22f * scale, 6f * scale), CornerRadius(2f, 2f))
        drawRect(Color(0xFFE2E8F0), Offset(cx + 10f * scale, cy - 16.5f * scale), Size(12f * scale, 3f * scale))
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 6f * scale, cy + 12f * scale), Size(22f * scale, 6f * scale), CornerRadius(2f, 2f))
        drawRect(Color(0xFFE2E8F0), Offset(cx + 10f * scale, cy + 13.5f * scale), Size(12f * scale, 3f * scale))
    }

    // Heavy Angled Blast Shields (Lvl 3) or Hazard Plates
    if (level >= 3 || unlockedNodeIds.contains("cannon_radius")) {
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 14f * scale, cy - 24f * scale), Size(16f * scale, 48f * scale), CornerRadius(5f, 5f))
        drawRoundRect(Color(0xFF334155), Offset(cx - 13f * scale, cy - 22f * scale), Size(13f * scale, 44f * scale), CornerRadius(4f, 4f))
        // Hazard safety stripes
        for (h in 0..3) {
            drawLine(Color(0xFFF59E0B), Offset(cx - 12f * scale, cy - 18f * scale + h * 10f * scale), Offset(cx - 2f * scale, cy - 10f * scale + h * 10f * scale), strokeWidth = 2f)
        }
    }

    // Massive Artillery Barrel
    drawRoundRect(Color(0xFF0F172A), Offset(cx - 4f * scale, cy - bThick / 2f), Size(bLen, bThick), CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFF1E293B), Offset(cx - 2f * scale, cy - (bThick - 3f * scale) / 2f), Size(bLen - 4f * scale, bThick - 3f * scale), CornerRadius(3f, 3f))
    drawRect(Color(0xFF475569), Offset(cx - 2f * scale, cy - 5f * scale), Size(bLen - 8f * scale, 3f * scale))

    // Barrel Reinforcement Ribs (Brass / Steel)
    for (r in 0 until (level + 1)) {
        drawRoundRect(Color(0xFFD97706), Offset(cx + 8f * scale + r * 11f * scale, cy - (bThick + 4f * scale) / 2f), Size(5f * scale, bThick + 4f * scale), CornerRadius(2f, 2f))
    }

    // Flared Muzzle Ring & Bore
    drawRoundRect(Color(0xFF0F172A), Offset(cx + bLen - 5f * scale, cy - (bThick + 6f * scale) / 2f), Size(10f * scale, bThick + 6f * scale), CornerRadius(4f, 4f))
    drawOval(Color(0xFF0F172A), Offset(cx + bLen + 3f * scale, cy - (bThick - 4f * scale) / 2f), Size(4f * scale, bThick - 4f * scale))

    // Attachment: Plasma Ignition Coils (cannon_crit)
    if (unlockedNodeIds.contains("cannon_crit") || level >= 3) {
        drawLine(Color(0xFFF97316), Offset(cx + 4f * scale, cy - bThick / 2f - 3f * scale), Offset(cx + bLen - 6f * scale, cy - bThick / 2f - 3f * scale), strokeWidth = 2f)
        drawLine(Color(0xFFF97316), Offset(cx + 4f * scale, cy + bThick / 2f + 3f * scale), Offset(cx + bLen - 6f * scale, cy + bThick / 2f + 3f * scale), strokeWidth = 2f)
    }

    // Breech Counterweight Dome
    drawCircle(Color(0xFF0F172A), radius = 14f * scale, center = Offset(cx - 8f * scale, cy))
    drawCircle(Color(0xFF334155), radius = 11f * scale, center = Offset(cx - 8f * scale, cy))
    drawCircle(Color(0xFFCBD5E1), radius = 4f * scale, center = Offset(cx - 10f * scale, cy - 3f * scale))
}

// ---------------- RAPID FIRE SHOWCASE ----------------
private fun DrawScope.drawRapidFireShowcase(
    cx: Float,
    cy: Float,
    level: Int,
    scale: Float,
    unlockedNodeIds: Set<String>
) {
    val bLen = (32f + level * 7f) * scale

    // Central Glowing Blue Energy Capacitor Core
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0F172A))),
        radius = 16f * scale,
        center = Offset(cx, cy)
    )

    // Electromagnetic Accelerator Rails
    drawRoundRect(Color(0xFF0F172A), Offset(cx + 2f * scale, cy - 9f * scale), Size(bLen, 5f * scale), CornerRadius(2f, 2f))
    drawRect(Color(0xFF38BDF8), Offset(cx + 4f * scale, cy - 8f * scale), Size(bLen - 6f * scale, 2f * scale))
    drawRoundRect(Color(0xFF0F172A), Offset(cx + 2f * scale, cy + 4f * scale), Size(bLen, 5f * scale), CornerRadius(2f, 2f))
    drawRect(Color(0xFF38BDF8), Offset(cx + 4f * scale, cy + 5f * scale), Size(bLen - 6f * scale, 2f * scale))

    if (level >= 2) {
        // Magnetic Coils along Rails
        for (c in 0..3) {
            drawRoundRect(Color(0xFF0284C7), Offset(cx + 8f * scale + c * 8f * scale, cy - 12f * scale), Size(4f * scale, 24f * scale), CornerRadius(1.5f, 1.5f))
        }
    }

    // Attachment: Helical Power Drum (rapid_drum)
    if (unlockedNodeIds.contains("rapid_drum") || level >= 3) {
        drawRoundRect(Color(0xFF0F172A), Offset(cx - 16f * scale, cy - 14f * scale), Size(14f * scale, 28f * scale), CornerRadius(5f, 5f))
        drawCircle(Color(0xFF10B981), radius = 3f * scale, center = Offset(cx - 9f * scale, cy))
    }

    // Attachment: Holographic Target Reticle (rapid_crit)
    if (unlockedNodeIds.contains("rapid_crit") || level >= 2) {
        val hudX = cx + bLen + 14f * scale
        drawCircle(Color(0xCC38BDF8), radius = 10f * scale, center = Offset(hudX, cy), style = Stroke(1.5f))
        drawCircle(Color(0xFF38BDF8), radius = 2f * scale, center = Offset(hudX, cy))
        drawLine(Color(0xCC38BDF8), Offset(hudX - 14f * scale, cy), Offset(hudX + 14f * scale, cy), strokeWidth = 1f)
        drawLine(Color(0xCC38BDF8), Offset(hudX, cy - 14f * scale), Offset(hudX, cy + 14f * scale), strokeWidth = 1f)
    }

    // Center Core Arc
    drawCircle(Color(0xFFBAE6FD), radius = 6f * scale, center = Offset(cx, cy))
}

// ---------------- FROST GUN SHOWCASE ----------------
private fun DrawScope.drawFrostGunShowcase(
    cx: Float,
    cy: Float,
    level: Int,
    scale: Float,
    unlockedNodeIds: Set<String>
) {
    val bLen = (30f + level * 6f) * scale

    // Insulated Cryogenic Reservoir Base
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFBAE6FD), Color(0xFF0284C7), Color(0xFF082F49))),
        radius = 16f * scale,
        center = Offset(cx, cy)
    )

    // Cryo Dispersion Funnel Nozzle
    drawRoundRect(Color(0xFF0F172A), Offset(cx + 2f * scale, cy - 11f * scale), Size(bLen, 22f * scale), CornerRadius(6f, 6f))
    drawRoundRect(Color(0xFFE0F2FE), Offset(cx + 4f * scale, cy - 9f * scale), Size(bLen - 6f * scale, 18f * scale), CornerRadius(4f, 4f))

    // Attachment: Circulating Liquid Nitrogen Conduits (frost_duration)
    if (unlockedNodeIds.contains("frost_duration") || level >= 2) {
        drawLine(Color(0xFF38BDF8), Offset(cx - 10f * scale, cy - 14f * scale), Offset(cx + bLen - 4f * scale, cy - 14f * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
        drawLine(Color(0xFF38BDF8), Offset(cx - 10f * scale, cy + 14f * scale), Offset(cx + bLen - 4f * scale, cy + 14f * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
    }

    // Attachment: Floating Sub-Zero Ice Crystal Shards (frost_shards / frost_potency)
    if (unlockedNodeIds.contains("frost_shards") || level >= 3) {
        for (i in 0..4) {
            val ang = (i * PI / 2.5).toFloat()
            val sx = cx + bLen + 8f * scale + cos(ang) * (12f * scale)
            val sy = cy + sin(ang) * (12f * scale)
            drawCircle(Color.White, radius = 3.5f * scale, center = Offset(sx, sy))
            drawCircle(Color(0xFFBAE6FD), radius = 2f * scale, center = Offset(sx, sy))
        }
    }

    // Center Frosted Core Dome
    drawCircle(Color.White, radius = 7f * scale, center = Offset(cx, cy))
}

// ---------------- CITADEL SHOWCASE ----------------
private fun DrawScope.drawCitadelShowcase(
    cx: Float,
    cy: Float,
    level: Int,
    scale: Float,
    unlockedNodeIds: Set<String>
) {
    // Bastion Fortress Bunker Structure
    drawRoundRect(Color(0xFF0F172A), Offset(cx - 24f * scale, cy - 24f * scale), Size(48f * scale, 48f * scale), CornerRadius(8f, 8f))
    drawRoundRect(Color(0xFF1E293B), Offset(cx - 21f * scale, cy - 21f * scale), Size(42f * scale, 42f * scale), CornerRadius(6f, 6f))

    // Armored Bulkhead Plating & Energy Shield Barrier
    if (unlockedNodeIds.contains("citadel_armor") || level >= 2) {
        drawCircle(
            color = Color(0x6638BDF8),
            radius = 32f * scale,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
        )
    }

    // Command Communications & Sensor Radar
    drawCircle(Color(0xFF38BDF8), radius = 10f * scale, center = Offset(cx, cy))
    drawCircle(Color(0xFFFBBF24), radius = 5f * scale, center = Offset(cx, cy))

    // Attachment: Gold Combat Reserves Vault Pods (citadel_gold)
    if (unlockedNodeIds.contains("citadel_gold") || level >= 2) {
        drawRoundRect(Color(0xFFD97706), Offset(cx - 22f * scale, cy + 14f * scale), Size(12f * scale, 8f * scale), CornerRadius(2f, 2f))
        drawRoundRect(Color(0xFFD97706), Offset(cx + 10f * scale, cy + 14f * scale), Size(12f * scale, 8f * scale), CornerRadius(2f, 2f))
    }
}

// =========================================================================
// 4. INTERACTIVE STRATEGY-GAME TECHNOLOGY TREE VIEW
// =========================================================================

/**
 * Technology tree rendered like a strategy game:
 * - Clear Tier 1 (Root) -> Tier 2 (Branches) -> Tier 3 (Capstone) visual hierarchy.
 * - Glowing animated energy conduits connecting parents to children.
 * - Tactical strategy-game node frames with clear Locked/Ready/Purchased/Max states.
 * - Icon-driven, short stats, zero bulky paragraph clutter.
 */
@Composable
private fun TechTreeStrategyView(
    nodes: List<ResearchNode>,
    selectedNodeId: String?,
    progressionManager: ProgressionManager,
    conduitPhase: Float,
    onSelectNode: (String) -> Unit,
    onUpgradeNode: (ResearchNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tier1Nodes = nodes.filter { it.tier == 1 }
    val tier2Nodes = nodes.filter { it.tier == 2 }
    val tier3Nodes = nodes.filter { it.tier == 3 }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0B1120).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // BACKGROUND: Connecting Strategy Conduits
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Col 1 (Tier 1): x = 0.16w
                // Col 2 (Tier 2): x = 0.50w
                // Col 3 (Tier 3): x = 0.84w
                val c1X = w * 0.18f
                val c2X = w * 0.50f
                val c3X = w * 0.82f

                val t1Y = h * 0.5f
                val t2AY = h * 0.28f
                val t2BY = h * 0.72f
                val t3Y = h * 0.5f

                // Conduit 1: Tier 1 -> Tier 2A
                val t1Rank = tier1Nodes.firstOrNull()?.let { progressionManager.getResearchRank(it.id) } ?: 0
                val isT1Unlocked = t1Rank >= 1

                drawCircuitConduit(
                    from = Offset(c1X, t1Y),
                    to = Offset(c2X, t2AY),
                    isUnlocked = isT1Unlocked,
                    phase = conduitPhase
                )

                // Conduit 2: Tier 1 -> Tier 2B
                drawCircuitConduit(
                    from = Offset(c1X, t1Y),
                    to = Offset(c2X, t2BY),
                    isUnlocked = isT1Unlocked,
                    phase = conduitPhase
                )

                // Conduit 3: Tier 2 -> Tier 3
                if (tier3Nodes.isNotEmpty()) {
                    val capstone = tier3Nodes.first()
                    val prereqMet = capstone.prerequisiteId?.let {
                        progressionManager.getResearchRank(it) >= capstone.prerequisiteRank
                    } ?: isT1Unlocked

                    val fromY = if (capstone.prerequisiteId?.contains("fire_rate") == true ||
                        capstone.prerequisiteId?.contains("radius") == true ||
                        capstone.prerequisiteId?.contains("speed") == true ||
                        capstone.prerequisiteId?.contains("duration") == true
                    ) t2AY else t2BY

                    drawCircuitConduit(
                        from = Offset(c2X, fromY),
                        to = Offset(c3X, t3Y),
                        isUnlocked = prereqMet,
                        phase = conduitPhase
                    )
                }
            }

            // FOREGROUND: Strategy Tech Nodes Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // COLUMN 1: TIER 1 (Root Blueprint)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "TIER I",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    tier1Nodes.forEach { node ->
                        StrategyTechNodeCard(
                            node = node,
                            isSelected = node.id == selectedNodeId,
                            progressionManager = progressionManager,
                            onClick = { onSelectNode(node.id) },
                            onUpgrade = { onUpgradeNode(node) }
                        )
                    }
                }

                // COLUMN 2: TIER 2 (Branching Specializations)
                Column(
                    modifier = Modifier.weight(1.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "TIER II",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    tier2Nodes.forEach { node ->
                        StrategyTechNodeCard(
                            node = node,
                            isSelected = node.id == selectedNodeId,
                            progressionManager = progressionManager,
                            onClick = { onSelectNode(node.id) },
                            onUpgrade = { onUpgradeNode(node) }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // COLUMN 3: TIER 3 (Apex Capstone)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (tier3Nodes.isNotEmpty()) "TIER III" else "",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    tier3Nodes.forEach { node ->
                        StrategyTechNodeCard(
                            node = node,
                            isSelected = node.id == selectedNodeId,
                            progressionManager = progressionManager,
                            onClick = { onSelectNode(node.id) },
                            onUpgrade = { onUpgradeNode(node) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws animated glowing circuit lines between technology nodes.
 */
private fun DrawScope.drawCircuitConduit(
    from: Offset,
    to: Offset,
    isUnlocked: Boolean,
    phase: Float
) {
    val midX = (from.x + to.x) / 2f
    val path = Path().apply {
        moveTo(from.x, from.y)
        cubicTo(midX, from.y, midX, to.y, to.x, to.y)
    }

    if (isUnlocked) {
        // Base glowing conduit line
        drawPath(
            path = path,
            color = Color(0x4438BDF8),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = Color(0xFF38BDF8),
            style = Stroke(
                width = 2f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), phase * 20f)
            )
        )
    } else {
        // Locked dim conduit
        drawPath(
            path = path,
            color = Color(0xFF1E293B),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = path,
            color = Color(0xFF334155),
            style = Stroke(
                width = 1.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            )
        )
    }
}

/**
 * Strategy-Game Technology Node Tile.
 */
@Composable
private fun StrategyTechNodeCard(
    node: ResearchNode,
    isSelected: Boolean,
    progressionManager: ProgressionManager,
    onClick: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRank = progressionManager.getResearchRank(node.id)
    val isMaxRank = currentRank >= node.maxRank
    val isPrereqMet = node.prerequisiteId?.let {
        progressionManager.getResearchRank(it) >= node.prerequisiteRank
    } ?: true
    val canUpgrade = progressionManager.canUpgradeResearch(node)
    val isLocked = !isPrereqMet

    // Strategy Node Theme Colors
    val borderColor = when {
        isSelected -> Color(0xFFFBBF24)
        isMaxRank -> Color(0xFFF59E0B)
        canUpgrade -> Color(0xFF10B981)
        isLocked -> Color(0xFF334155)
        else -> Color(0xFF0284C7)
    }

    val bgColor = when {
        isSelected -> Color(0xFF1E293B)
        isMaxRank -> Color(0xFF1E1B18)
        canUpgrade -> Color(0xFF0F231C)
        isLocked -> Color(0xFF0A0F1D)
        else -> Color(0xFF0F172A)
    }

    val icon = when (node.statType) {
        StatBonusType.DAMAGE -> Icons.Default.Upgrade
        StatBonusType.FIRE_RATE -> Icons.Default.Speed
        StatBonusType.RANGE -> Icons.Default.Star
        StatBonusType.ARMOR_PENETRATION -> Icons.Default.Security
        StatBonusType.SPLASH_RADIUS -> Icons.Default.ElectricBolt
        StatBonusType.RELOAD_SPEED -> Icons.Default.Speed
        StatBonusType.ATTACK_SPEED -> Icons.Default.Speed
        StatBonusType.CRIT_CHANCE -> Icons.Default.Star
        StatBonusType.PROJECTILE_SPEED -> Icons.Default.Speed
        StatBonusType.SLOW_POTENCY -> Icons.Default.Shield
        StatBonusType.SLOW_DURATION -> Icons.Default.Shield
        StatBonusType.BASE_HP -> Icons.Default.Shield
        StatBonusType.STARTING_GOLD -> Icons.Default.MonetizationOn
    }

    val shortStat = node.statBonusLabel.substringBefore(" per rank")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected || canUpgrade) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier
            .widthIn(min = 90.dp, max = 115.dp)
            .clickable { onClick() }
            .testTag("node_card_${node.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Status Badge & Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon in circular bezel
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isLocked) Color(0xFF1E293B) else borderColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else icon,
                        contentDescription = null,
                        tint = if (isLocked) Color(0xFF64748B) else borderColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Rank indicator / Status tag
                Text(
                    text = when {
                        isMaxRank -> "MAX"
                        isLocked -> "LOCK"
                        canUpgrade -> "READY"
                        else -> "$currentRank/${node.maxRank}"
                    },
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = borderColor
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Short Node Name
            Text(
                text = node.name,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) Color(0xFF64748B) else Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Short Stat Benefit Badge
            Text(
                text = shortStat,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isLocked) Color(0xFF475569) else Color(0xFF38BDF8),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Tactical Rank Pips: ● ● ○ ○
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (pip in 1..node.maxRank) {
                    val isFilled = pip <= currentRank
                    Box(
                        modifier = Modifier
                            .size(if (isFilled) 4.5.dp else 3.5.dp)
                            .background(
                                color = when {
                                    isFilled -> if (isMaxRank) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                                    isLocked -> Color(0xFF1E293B)
                                    else -> Color(0xFF334155)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

// =========================================================================
// 5. DOCKED BOTTOM UPGRADE ACTION STATION
// =========================================================================

/**
 * Docked tactical bottom control station:
 * Shows selected node details, required tokens, and the prominent action button.
 */
@Composable
private fun DockedUpgradeActionStation(
    node: ResearchNode,
    progressionManager: ProgressionManager,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRank = progressionManager.getResearchRank(node.id)
    val isMaxRank = currentRank >= node.maxRank
    val canUpgrade = progressionManager.canUpgradeResearch(node)
    val prereqMet = node.prerequisiteId?.let {
        progressionManager.getResearchRank(it) >= node.prerequisiteRank
    } ?: true
    val cost = if (!isMaxRank) node.getCostForRank(currentRank) else 0

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected Node Details
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.8f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!prereqMet) Icons.Default.Lock else Icons.Default.Upgrade,
                        contentDescription = null,
                        tint = if (!prereqMet) Color(0xFF94A3B8) else Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = node.name.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TIER ${node.tier} • RANK $currentRank/${node.maxRank}",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Text(
                        text = if (isMaxRank) "MAXIMUM BLUEPRINT LEVEL MASTERED"
                        else if (!prereqMet) "LOCKED • REQUIRES PREVIOUS BLUEPRINT AT RANK ${node.prerequisiteRank}"
                        else node.statBonusLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!prereqMet) Color(0xFFEF4444) else Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Button & Required Cost
            Box(
                modifier = Modifier.widthIn(min = 140.dp, max = 190.dp)
            ) {
                GameButton(
                    text = when {
                        isMaxRank -> "MAX LEVEL"
                        !prereqMet -> "LOCKED"
                        canUpgrade -> "RESEARCH"
                        else -> "UPGRADE"
                    },
                    subText = when {
                        isMaxRank -> "COMPLETED"
                        !prereqMet -> "UNAVAILABLE"
                        else -> "$cost 🪙"
                    },
                    icon = when {
                        isMaxRank -> Icons.Default.Check
                        !prereqMet -> Icons.Default.Lock
                        else -> Icons.Default.Upgrade
                    },
                    variant = when {
                        isMaxRank -> GameButtonVariant.SECONDARY
                        canUpgrade -> GameButtonVariant.GOLD
                        else -> GameButtonVariant.SECONDARY
                    },
                    height = 38.dp,
                    enabled = canUpgrade,
                    isLocked = !prereqMet,
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "upgrade_button"
                )
            }
        }
    }
}

// Quad data holder for stat chips
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
