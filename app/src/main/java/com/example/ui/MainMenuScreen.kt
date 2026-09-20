package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProgressionManager
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameCurrencyBadge
import com.example.ui.components.GamePanel
import kotlin.math.cos
import kotlin.math.sin

private object MainMenuSessionState {
    var hasAutoShownDailyReward: Boolean = false
}

/**
 * Professional mobile tower defense opening screen & main menu.
 * Features:
 * - Dynamic animated battlefield background (twilight horizon, sweeping searchlights, floating embers)
 * - Top Profile Header: Commander rank, total earned stars, collected tokens
 * - Dramatic central game emblem with pulsing energy core & hero PLAY button
 * - Tactile 3D bottom game navigation buttons: PLAY, LEVELS, SETTINGS, TACTICS (ABOUT)
 */
@Composable
fun MainMenuScreen(
    progressionManager: ProgressionManager,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenResearchLab: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showTacticalBriefing by remember { mutableStateOf(false) }
    var showRewardChests by remember { mutableStateOf(false) }
    var showDailyReward by remember {
        val canClaim = progressionManager.canClaimDailyReward()
        val shouldAutoShow = canClaim && !MainMenuSessionState.hasAutoShownDailyReward
        if (shouldAutoShow) {
            MainMenuSessionState.hasAutoShownDailyReward = true
        }
        mutableStateOf(shouldAutoShow)
    }
    var refreshKey by remember { mutableStateOf(0) }

    val totalStars: Int = remember(refreshKey) { progressionManager.getTotalStars() }
    val totalTokens: Int = remember(refreshKey) { progressionManager.getTokens() }
    val playerLevel: Int = remember(refreshKey) { progressionManager.getPlayerLevel() }
    val playerTitle: String = remember(refreshKey) { progressionManager.getPlayerTitle() }
    val xpRatio: Float = remember(refreshKey) { progressionManager.getXpProgressRatio() }
    val canClaimDaily: Boolean = remember(refreshKey) { progressionManager.canClaimDailyReward() }
    val unclaimedChests: Int = remember(refreshKey) { progressionManager.getAvailableMilestoneChests().size }
    val unclaimedAchievements: Int = remember(refreshKey) { progressionManager.getClaimableAchievements().size }

    // Infinite animation transition for battlefield ambient effects
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_anim")

    // Sweeping searchlight angle
    val searchlightAngle by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searchlight"
    )

    // Floating embers drift
    val emberProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "embers"
    )

    // Hero button subtle pulse
    val heroPulse by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_menu_screen")
    ) {
        // 1. Dynamic Animated Battlefield Backdrop
        BattlefieldAnimatedBackdrop(
            searchlightAngle = searchlightAngle,
            emberProgress = emberProgress,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Main Menu Responsive Interface Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth >= screenHeight
            val isCompactHeight = screenHeight < 460.dp
            val isUltraCompactHeight = screenHeight < 360.dp

            if (isLandscape && isCompactHeight) {
                // LANDSCAPE MOBILE VIEW: Split 2-Column Responsive Layout
                // Eliminates vertical button crowding: Game identity on left, Action Hub on right
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isUltraCompactHeight) 16.dp else 24.dp,
                            vertical = if (isUltraCompactHeight) 4.dp else 8.dp
                        ),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TOP BAR: Player Profile, Stars, and Token Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Commander Profile Plate (Clickable -> Dossier)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xE60F172A), Color(0x991E293B))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { onOpenProfile() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF0369A1), CircleShape)
                                    .border(1.dp, Color(0xFF38BDF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = "Rank",
                                    tint = Color(0xFFFFD166),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = playerTitle.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = Color(0xFF38BDF8)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LV. $playerLevel",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    LinearProgressIndicator(
                                        progress = { xpRatio },
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(3.dp),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Currency & Stars Status Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stars Earned Badge (Clickable -> Crates)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (unclaimedChests > 0) Color(0xFFF59E0B) else Color(0xFFCA8A04),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showRewardChests = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Total Stars",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalStars / 27",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                if (unclaimedChests > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = Color(0xFFE11D48),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "+$unclaimedChests",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Tactical Currency Badge (Clickable -> Research Lab)
                            Box(modifier = Modifier.clickable { onOpenResearchLab() }) {
                                GameCurrencyBadge(
                                    amount = totalTokens,
                                    isCompact = true
                                )
                            }
                        }
                    }

                    // CENTER BODY: Left (Emblem & Title) | Right (Hero Play Button & Nav Buttons)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: Game Emblem Shield & Title
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            val emblemSize = if (isUltraCompactHeight) 44.dp else 52.dp
                            val innerSize = if (isUltraCompactHeight) 36.dp else 42.dp
                            val iconSize = if (isUltraCompactHeight) 22.dp else 26.dp

                            Box(
                                modifier = Modifier.size(emblemSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0x6638BDF8), Color.Transparent),
                                            center = Offset(cx, cy),
                                            radius = size.width * 0.6f
                                        ),
                                        radius = size.width * 0.6f,
                                        center = Offset(cx, cy)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(innerSize)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Shield",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(iconSize)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(if (isUltraCompactHeight) 2.dp else 4.dp))

                            Text(
                                text = "FRONTLINE",
                                fontSize = if (isUltraCompactHeight) 26.sp else 30.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.5.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "CITADEL DEFENSE • TACTICAL WARS",
                                fontSize = if (isUltraCompactHeight) 8.5.sp else 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                color = Color(0xFF38BDF8),
                                textAlign = TextAlign.Center
                            )
                        }

                        // RIGHT: Tactical Action Center (Hero Play Button + Secondary Button Row)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1.15f)
                                .widthIn(max = 380.dp)
                                .padding(start = 8.dp)
                        ) {
                            // Hero Green PLAY Button (Deployed safely with guaranteed vertical space)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(heroPulse)
                            ) {
                                GameButton(
                                    text = "DEPLOY TO BATTLE",
                                    subText = "CONTINUE CAMPAIGN",
                                    icon = Icons.Default.PlayArrow,
                                    variant = GameButtonVariant.PRIMARY,
                                    height = if (isUltraCompactHeight) 44.dp else 50.dp,
                                    onClick = onPlayClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "play_button"
                                )
                            }

                            // Dedicated vertical separation ensures zero overlap with Settings button
                            Spacer(modifier = Modifier.height(if (isUltraCompactHeight) 6.dp else 10.dp))

                            // Progression Hub Row 1: Levels, Research Lab, Medals, Crates
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GameButton(
                                    text = "LEVELS",
                                    icon = Icons.Default.Map,
                                    variant = GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 34.dp else 38.dp,
                                    onClick = onPlayClick,
                                    modifier = Modifier.weight(1f),
                                    testTag = "levels_button"
                                )

                                GameButton(
                                    text = "RESEARCH",
                                    icon = Icons.Default.Science,
                                    variant = GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 34.dp else 38.dp,
                                    onClick = onOpenResearchLab,
                                    modifier = Modifier.weight(1f),
                                    testTag = "research_button"
                                )

                                GameButton(
                                    text = if (unclaimedAchievements > 0) "MEDALS ($unclaimedAchievements)" else "MEDALS",
                                    icon = Icons.Default.EmojiEvents,
                                    variant = if (unclaimedAchievements > 0) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 34.dp else 38.dp,
                                    onClick = onOpenAchievements,
                                    modifier = Modifier.weight(1f),
                                    testTag = "achievements_button"
                                )

                                GameButton(
                                    text = if (unclaimedChests > 0) "CRATES ($unclaimedChests)" else "CRATES",
                                    icon = Icons.Default.Inventory2,
                                    variant = if (unclaimedChests > 0) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 34.dp else 38.dp,
                                    onClick = { showRewardChests = true },
                                    modifier = Modifier.weight(1f),
                                    testTag = "crates_button"
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Progression Hub Row 2: Daily Login, Dossier, Settings, Tactics Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GameButton(
                                    text = if (canClaimDaily) "DAILY (!)" else "DAILY",
                                    icon = Icons.Default.CalendarMonth,
                                    variant = if (canClaimDaily) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 32.dp else 36.dp,
                                    onClick = { showDailyReward = true },
                                    modifier = Modifier.weight(1f),
                                    testTag = "daily_reward_button"
                                )

                                GameButton(
                                    text = "DOSSIER",
                                    icon = Icons.Default.MilitaryTech,
                                    variant = GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 32.dp else 36.dp,
                                    onClick = onOpenProfile,
                                    modifier = Modifier.weight(1f),
                                    testTag = "profile_button"
                                )

                                GameButton(
                                    text = "SETTINGS",
                                    icon = Icons.Default.Settings,
                                    variant = GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 32.dp else 36.dp,
                                    onClick = onSettingsClick,
                                    modifier = Modifier.weight(1f),
                                    testTag = "settings_button"
                                )

                                GameButton(
                                    text = "ABOUT",
                                    icon = Icons.Default.Info,
                                    variant = GameButtonVariant.SECONDARY,
                                    height = if (isUltraCompactHeight) 32.dp else 36.dp,
                                    onClick = { showTacticalBriefing = true },
                                    modifier = Modifier.weight(1f),
                                    testTag = "about_button"
                                )
                            }
                        }
                    }
                }
            } else {
                // TABLET / TALL / DESKTOP VIEW: Centered Column with Structured Action Stack
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TOP BAR: Player Profile, Stars, and Token Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Commander Profile Plate (Clickable -> Dossier)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xE60F172A), Color(0x991E293B))
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable { onOpenProfile() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFF0369A1), CircleShape)
                                    .border(1.dp, Color(0xFF38BDF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MilitaryTech,
                                    contentDescription = "Rank",
                                    tint = Color(0xFFFFD166),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = playerTitle.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFF38BDF8)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LV. $playerLevel",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    LinearProgressIndicator(
                                        progress = { xpRatio },
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(4.dp),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        // Currency & Stars Status Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xE60F172A), RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (unclaimedChests > 0) Color(0xFFF59E0B) else Color(0xFFCA8A04),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { showRewardChests = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Total Stars",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$totalStars / 27 STARS",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                if (unclaimedChests > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFE11D48),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "+$unclaimedChests CRATES",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.clickable { onOpenResearchLab() }) {
                                GameCurrencyBadge(
                                    amount = totalTokens,
                                    isCompact = true
                                )
                            }
                        }
                    }

                    // CENTER BRANDING
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x6638BDF8), Color.Transparent),
                                        center = Offset(cx, cy),
                                        radius = size.width * 0.6f
                                    ),
                                    radius = size.width * 0.6f,
                                    center = Offset(cx, cy)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shield",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "FRONTLINE",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "CITADEL DEFENSE • TACTICAL WARS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF38BDF8),
                            textAlign = TextAlign.Center
                        )
                    }

                    // ACTIONS STACK: Primary Play Button + Secondary Button Row
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(heroPulse)
                                .widthIn(min = 240.dp, max = 320.dp)
                        ) {
                            GameButton(
                                text = "DEPLOY TO BATTLE",
                                subText = "CONTINUE CAMPAIGN",
                                icon = Icons.Default.PlayArrow,
                                variant = GameButtonVariant.PRIMARY,
                                height = 54.dp,
                                onClick = onPlayClick,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "play_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: Levels, Research Lab, Medals, Crates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameButton(
                                text = "LEVELS",
                                icon = Icons.Default.Map,
                                variant = GameButtonVariant.SECONDARY,
                                height = 44.dp,
                                onClick = onPlayClick,
                                modifier = Modifier.weight(1f),
                                testTag = "levels_button"
                            )

                            GameButton(
                                text = "RESEARCH",
                                icon = Icons.Default.Science,
                                variant = GameButtonVariant.SECONDARY,
                                height = 44.dp,
                                onClick = onOpenResearchLab,
                                modifier = Modifier.weight(1f),
                                testTag = "research_button"
                            )

                            GameButton(
                                text = if (unclaimedAchievements > 0) "MEDALS ($unclaimedAchievements)" else "MEDALS",
                                icon = Icons.Default.EmojiEvents,
                                variant = if (unclaimedAchievements > 0) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                height = 44.dp,
                                onClick = onOpenAchievements,
                                modifier = Modifier.weight(1f),
                                testTag = "achievements_button"
                            )

                            GameButton(
                                text = if (unclaimedChests > 0) "CRATES ($unclaimedChests)" else "CRATES",
                                icon = Icons.Default.Inventory2,
                                variant = if (unclaimedChests > 0) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                height = 44.dp,
                                onClick = { showRewardChests = true },
                                modifier = Modifier.weight(1f),
                                testTag = "crates_button"
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Row 2: Daily Login, Dossier, Settings, About
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameButton(
                                text = if (canClaimDaily) "DAILY (!)" else "DAILY",
                                icon = Icons.Default.CalendarMonth,
                                variant = if (canClaimDaily) GameButtonVariant.GOLD else GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = { showDailyReward = true },
                                modifier = Modifier.weight(1f),
                                testTag = "daily_reward_button"
                            )

                            GameButton(
                                text = "DOSSIER",
                                icon = Icons.Default.MilitaryTech,
                                variant = GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = onOpenProfile,
                                modifier = Modifier.weight(1f),
                                testTag = "profile_button"
                            )

                            GameButton(
                                text = "SETTINGS",
                                icon = Icons.Default.Settings,
                                variant = GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = onSettingsClick,
                                modifier = Modifier.weight(1f),
                                testTag = "settings_button"
                            )

                            GameButton(
                                text = "ABOUT",
                                icon = Icons.Default.Info,
                                variant = GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = { showTacticalBriefing = true },
                                modifier = Modifier.weight(1f),
                                testTag = "about_button"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTacticalBriefing) {
        TacticalCodexDialog(onDismiss = { showTacticalBriefing = false })
    }

    if (showRewardChests) {
        RewardChestDialog(
            progressionManager = progressionManager,
            onDismiss = {
                showRewardChests = false
                refreshKey++
            }
        )
    }

    if (showDailyReward) {
        DailyRewardDialog(
            progressionManager = progressionManager,
            onDismiss = {
                showDailyReward = false
                refreshKey++
            }
        )
    }
}

/**
 * Animated Battlefield canvas rendered behind the main menu.
 * Features:
 * - Twilight radial gradient
 * - Atmospheric ground fog and silhouetted fortress horizon
 * - Animated rotating searchlight beams
 * - Drifting ambient ember sparks
 */
@Composable
private fun BattlefieldAnimatedBackdrop(
    searchlightAngle: Float,
    emberProgress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Sky twilight gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF070B14),
                    Color(0xFF0F1A2E),
                    Color(0xFF162544)
                ),
                startY = 0f,
                endY = h * 0.85f
            )
        )

        // 2. Distant mountains silhouette
        val mountainPath = Path().apply {
            moveTo(0f, h * 0.65f)
            lineTo(w * 0.15f, h * 0.52f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.55f, h * 0.48f)
            lineTo(w * 0.78f, h * 0.58f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w, h * 0.60f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(mountainPath, color = Color(0xFF0C1424))

        // 3. Sweeping Searchlight beams from fortress towers
        val leftTowerBase = Offset(w * 0.18f, h * 0.68f)
        val beamLength = h * 0.9f
        val beamAngleLeft = -0.55f + searchlightAngle * 0.4f

        val beamPath = Path().apply {
            moveTo(leftTowerBase.x, leftTowerBase.y)
            lineTo(
                leftTowerBase.x + cos(beamAngleLeft - 0.12f) * beamLength,
                leftTowerBase.y - sin(beamAngleLeft - 0.12f) * beamLength
            )
            lineTo(
                leftTowerBase.x + cos(beamAngleLeft + 0.12f) * beamLength,
                leftTowerBase.y - sin(beamAngleLeft + 0.12f) * beamLength
            )
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3338BDF8), Color(0x0838BDF8), Color.Transparent),
                center = leftTowerBase,
                radius = beamLength
            )
        )

        val rightTowerBase = Offset(w * 0.82f, h * 0.68f)
        val beamAngleRight = -0.45f - searchlightAngle * 0.35f
        val beamPathRight = Path().apply {
            moveTo(rightTowerBase.x, rightTowerBase.y)
            lineTo(
                rightTowerBase.x - cos(beamAngleRight - 0.12f) * beamLength,
                rightTowerBase.y - sin(beamAngleRight - 0.12f) * beamLength
            )
            lineTo(
                rightTowerBase.x - cos(beamAngleRight + 0.12f) * beamLength,
                rightTowerBase.y - sin(beamAngleRight + 0.12f) * beamLength
            )
            close()
        }
        drawPath(
            path = beamPathRight,
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2838BDF8), Color(0x0638BDF8), Color.Transparent),
                center = rightTowerBase,
                radius = beamLength
            )
        )

        // 4. Foreground Citadel silhouettes & Battlements
        drawRect(
            color = Color(0xFF080D18),
            topLeft = Offset(0f, h * 0.72f),
            size = androidx.compose.ui.geometry.Size(w, h * 0.28f)
        )

        // Left Citadel Watchtower
        drawRect(
            color = Color(0xFF0A101D),
            topLeft = Offset(w * 0.15f, h * 0.60f),
            size = androidx.compose.ui.geometry.Size(42f, h * 0.15f)
        )
        // Right Citadel Watchtower
        drawRect(
            color = Color(0xFF0A101D),
            topLeft = Offset(w * 0.80f, h * 0.60f),
            size = androidx.compose.ui.geometry.Size(42f, h * 0.15f)
        )

        // 5. Ambient glowing ember sparks drifting up
        val emberSeed = 8
        for (i in 0 until emberSeed) {
            val emberX = (w * (0.1f + ((i * 127) % 800) / 1000f) + (emberProgress * 60f * (if (i % 2 == 0) 1 else -1))) % w
            val emberY = (h * 0.95f - ((emberProgress + i * 0.125f) % 1f) * (h * 0.6f))
            val emberRadius = if (i % 3 == 0) 3.5f else 2.2f
            val alpha = (1f - (((emberProgress + i * 0.125f) % 1f))).coerceIn(0f, 1f)

            drawCircle(
                color = Color(0xFFFFB703).copy(alpha = alpha * 0.75f),
                radius = emberRadius,
                center = Offset(emberX, emberY)
            )
        }
    }
}

/**
 * Tactical Codex Dialog providing military briefing on towers, enemies, and destructibles.
 */
@Composable
private fun TacticalCodexDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        GamePanel(
            headerTitle = "TACTICAL OPERATIONS CODEX",
            headerIcon = Icons.Default.Info,
            borderColor = Color(0xFF38BDF8),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 580.dp)
                .testTag("tactical_codex_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "FIELD STRATEGY MANUAL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD166),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "• Defend your citadel base from enemy incursions by deploying defense turrets on open ground.\n" +
                            "• Target natural obstacles (trees, boulders, crates) with your towers to clear space and reap valuable gold bounties.\n" +
                            "• Upgrade turrets to unlock heavy kinetic impact, armor-piercing rounds, and multi-barrel rapid salvos.\n" +
                            "• Boss columns appear at waves 5, 10, and 15. Prioritize focus fire to halt their advance!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(6.dp))

                GameButton(
                    text = "ACKNOWLEDGED",
                    variant = GameButtonVariant.PRIMARY,
                    height = 42.dp,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
