package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.audio.AndroidAudioPlayer
import com.example.data.ProgressionReward
import com.example.game.GameState
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameStarsRow
import com.example.ui.components.GameStatRow
import kotlinx.coroutines.delay

/**
 * Professional mobile game Victory result screen.
 * Features:
 * - Transparent blurred/dimmed battlefield background with festive gold/amber celebration particle sparks
 * - Animated radial celebration sunburst rays
 * - Dramatic animated entry scale & laurel victory banner
 * - Staggered star reveal with satisfying sound & scale punch
 * - High-impact visual reward cards (Bounty Coins, XP energy, Stars, Boss bounty)
 * - Concise, scannable performance metrics (Waves, Enemies, Base HP)
 * - Large tactile 3D action buttons: NEXT LEVEL, RETRY, HOME
 */
@Composable
fun VictoryDialog(
    gameState: GameState,
    hasNextLevel: Boolean = true,
    progressionReward: ProgressionReward? = null,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val entranceScale = remember { Animatable(0.82f) }
    val bannerAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f)
        )
        bannerAlpha.animateTo(1f, animationSpec = tween(300))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victory_effects")
    val sparkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparks"
    )
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rays"
    )

    // Full screen overlay shielding battlefield while keeping landscape map visible underneath
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Celebration atmosphere: Rotating subtle god-rays & ascending gold sparks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h / 2f

            // 1. Festive golden rays
            val rayCount = 12
            val rayAngle = 360f / rayCount
            for (i in 0 until rayCount) {
                val currentAngle = Math.toRadians((i * rayAngle + rayRotation).toDouble())
                val endAngle = Math.toRadians((i * rayAngle + rayRotation + 12f).toDouble())
                val rayLen = kotlin.math.max(w, h) * 0.7f

                val path = Path().apply {
                    moveTo(centerX, centerY)
                    lineTo(
                        (centerX + kotlin.math.cos(currentAngle) * rayLen).toFloat(),
                        (centerY + kotlin.math.sin(currentAngle) * rayLen).toFloat()
                    )
                    lineTo(
                        (centerX + kotlin.math.cos(endAngle) * rayLen).toFloat(),
                        (centerY + kotlin.math.sin(endAngle) * rayLen).toFloat()
                    )
                    close()
                }
                drawPath(path, color = Color(0xFFFBBF24).copy(alpha = 0.04f))
            }

            // 2. Ascending golden celebration sparks
            val sparkCount = 22
            for (i in 0 until sparkCount) {
                val seed = (i * 137) % 1000 / 1000f
                val x = w * (0.15f + seed * 0.7f) + (if (i % 2 == 0) 1 else -1) * (sparkProgress * 35f)
                val y = h * 0.95f - ((sparkProgress + seed) % 1f) * (h * 0.9f)
                val alpha = (1f - ((sparkProgress + seed) % 1f)).coerceIn(0f, 1f)
                val radius = if (i % 4 == 0) 3.5f else 2f
                val sparkColor = if (i % 2 == 0) Color(0xFFFDE68A) else Color(0xFF38BDF8)

                drawCircle(
                    color = sparkColor.copy(alpha = alpha * 0.85f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Centered Mobile Game Victory Card
        Box(
            modifier = Modifier
                .scale(entranceScale.value)
                .widthIn(min = 400.dp, max = 680.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1B2434), Color(0xFF0F172A), Color(0xFF080C14))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFEF08A).copy(alpha = 0.7f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Two-column responsive landscape design
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // LEFT COLUMN: Victory Crest, Stars, and Mission Status
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.95f)
                    ) {
                        // Golden Laurel Victory Emblem
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0x77F59E0B), Color(0x22F59E0B), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Victory Trophy",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "VICTORY",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = Color(0xFFFDE68A)
                        )

                        Text(
                            text = "${gameState.currentMap.name.uppercase()} COMPLETED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large 1-3 Stars Rating with pop animation
                        GameStarsRow(
                            starsEarned = gameState.starsEarned,
                            totalStars = 3,
                            starSize = 32.dp,
                            animated = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Score Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x88000000), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x55CA8A04), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "TACTICAL SCORE: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${gameState.finalScore}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD166)
                            )
                        }
                    }

                    // RIGHT COLUMN: Visual Rewards, Performance stats & Action buttons
                    Column(
                        modifier = Modifier.weight(1.25f)
                    ) {
                        // VISUAL REWARD TILES ROW: Coins, XP, Stars, Boss Reward
                        Text(
                            text = "MISSION REWARDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Coins Reward
                            RewardItemCard(
                                icon = Icons.Default.MonetizationOn,
                                iconColor = Color(0xFFFBBF24),
                                value = "+${gameState.totalCoinsEarned}",
                                label = "Coins",
                                modifier = Modifier.weight(1f)
                            )

                            // 2. XP Reward
                            val xpAmount = progressionReward?.xpEarned ?: (gameState.enemiesKilledTotal * 5 + gameState.starsEarned * 50)
                            RewardItemCard(
                                icon = Icons.Default.Bolt,
                                iconColor = Color(0xFF38BDF8),
                                value = "+$xpAmount",
                                label = "XP",
                                modifier = Modifier.weight(1f)
                            )

                            // 3. Stars Badge
                            RewardItemCard(
                                icon = Icons.Default.Star,
                                iconColor = Color(0xFFFFD166),
                                value = "${gameState.starsEarned}/3",
                                label = "Stars",
                                modifier = Modifier.weight(1f)
                            )

                            // 4. Boss Bounty or Tokens
                            val tokensEarned = progressionReward?.tokensEarned ?: (gameState.starsEarned * 2)
                            val isBossMap = gameState.currentMap.totalWaves >= 15 || gameState.currentWave >= 15
                            RewardItemCard(
                                icon = if (isBossMap) Icons.Default.MilitaryTech else Icons.Default.Shield,
                                iconColor = if (isBossMap) Color(0xFFA855F7) else Color(0xFF34D399),
                                value = "+$tokensEarned",
                                label = if (isBossMap) "Boss" else "Tokens",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Unlock or Level Up Notification Banner if applicable
                        if (progressionReward != null && (progressionReward.leveledUp || progressionReward.unlockedTowers.isNotEmpty())) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x55312E81), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (progressionReward.leveledUp) {
                                        "COMMANDER LEVEL ${progressionReward.newLevel} ACHIEVED!"
                                    } else {
                                        "UNLOCKED: ${progressionReward.unlockedTowers.first().displayName}"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Concise Performance Stats
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0F1A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            GameStatRow("Waves Survived", "${gameState.currentWave}/${gameState.maxWaves}")
                            GameStatRow("Enemies Defeated", "${gameState.enemiesKilledTotal}")
                            GameStatRow(
                                "Base Health Remaining",
                                "${gameState.base.currentHp}/${gameState.base.maxHp} HP",
                                valueColor = Color(0xFF4ADE80)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ACTION BUTTONS: Next Level, Retry, Campaign
                        if (hasNextLevel) {
                            GameButton(
                                text = "NEXT LEVEL",
                                icon = Icons.Default.ArrowForward,
                                variant = GameButtonVariant.GOLD,
                                height = 44.dp,
                                onClick = onNextLevel,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "victory_next_level_button"
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF854D0E), Color(0xFFCA8A04), Color(0xFF854D0E))
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Trophy",
                                        tint = Color(0xFFFEF08A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ALL 15 CAMPAIGN SECTORS CONQUERED!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFEF08A),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GameButton(
                                text = "RETRY",
                                icon = Icons.Default.Replay,
                                variant = GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = onRetry,
                                modifier = Modifier.weight(1f),
                                testTag = "victory_retry_button"
                            )

                            GameButton(
                                text = "HOME",
                                icon = Icons.Default.Home,
                                variant = GameButtonVariant.SECONDARY,
                                height = 40.dp,
                                onClick = onHome,
                                modifier = Modifier.weight(1f),
                                testTag = "victory_home_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tactical visual reward tile displaying icon, value, and label with game styling.
 */
@Composable
private fun RewardItemCard(
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
        Text(
            text = label.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
    }
}
