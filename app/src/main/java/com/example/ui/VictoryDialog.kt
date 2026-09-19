package com.example.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.audio.AndroidAudioPlayer
import com.example.game.GameState
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameStarsRow
import com.example.ui.components.GameStatRow

/**
 * Professional mobile game Victory modal screen.
 * Features:
 * - Celebration gold particle sparks drifting upward
 * - Smooth entrance scale animation
 * - Embossed golden laurel trophy emblem
 * - Staggered animated 3-star rating reveal
 * - Loot reward pill (+tokens)
 * - Tactical battle statistics readout
 * - Prominent 3D tactile NEXT LEVEL action button & secondary actions
 */
@Composable
fun VictoryDialog(
    gameState: GameState,
    hasNextLevel: Boolean = true,
    onNextLevel: () -> Unit,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val entranceScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        audioPlayer.victory()
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "victory_particles")
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparks"
    )

    // Full-screen backdrop in screen coordinates, consuming taps to shield the game world
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating celebration particles / gold sparks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sparkCount = 18

            for (i in 0 until sparkCount) {
                val seed = (i * 137) % 1000 / 1000f
                val x = w * (0.2f + seed * 0.6f) + (if (i % 2 == 0) 1 else -1) * (particleProgress * 40f)
                val y = h * 0.9f - ((particleProgress + seed) % 1f) * (h * 0.8f)
                val alpha = (1f - ((particleProgress + seed) % 1f)).coerceIn(0f, 1f)
                val radius = if (i % 3 == 0) 3.5f else 2f

                drawCircle(
                    color = if (i % 2 == 0) Color(0xFFFBBF24).copy(alpha = alpha * 0.8f) else Color(0xFF67E8F9).copy(alpha = alpha * 0.6f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        // Center Tactical Victory Panel
        Box(
            modifier = Modifier
                .scale(entranceScale.value)
                .widthIn(min = 380.dp, max = 660.dp)
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF090D18))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFEF08A).copy(alpha = 0.6f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Two-column horizontal arrangement tailored for landscape mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Column: Golden Trophy, Title, Map Name, Animated Stars, Score
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        // Golden Laurel Trophy emblem
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0x66F59E0B), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Victory Trophy",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "VICTORY ACHIEVED",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFFDE68A)
                        )

                        Text(
                            text = "${gameState.currentMap.name.uppercase()} SECURED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated Star Rating Display
                        GameStarsRow(
                            starsEarned = gameState.starsEarned,
                            totalStars = 3,
                            starSize = 30.dp,
                            animated = true
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Tactical Score Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x66000000), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x44CA8A04), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "TACTICAL SCORE: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "${gameState.finalScore}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD166)
                            )
                        }
                    }

                    // Right Column: Battle Report & Action Buttons
                    Column(
                        modifier = Modifier.weight(1.3f)
                    ) {
                        // Tactical Battle Statistics Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GameStatRow("Waves Repelled", "${gameState.currentWave}/${gameState.maxWaves}")
                            GameStatRow("Invaders Eliminated", "${gameState.enemiesKilledTotal}")
                            GameStatRow("Bounty Collected", "+${gameState.totalCoinsEarned} 🪙", valueColor = Color(0xFFFBBF24))
                            GameStatRow("Fortress Integrity", "${gameState.base.currentHp}/${gameState.base.maxHp} HP", valueColor = Color(0xFF4ADE80))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Next Level Hero Action Button (if available)
                        if (hasNextLevel) {
                            GameButton(
                                text = "NEXT MISSION",
                                icon = Icons.Default.ArrowForward,
                                variant = GameButtonVariant.PRIMARY,
                                height = 44.dp,
                                onClick = onNextLevel,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "victory_next_level_button"
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Secondary Row: Retry and Home Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                text = "CAMPAIGN",
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
