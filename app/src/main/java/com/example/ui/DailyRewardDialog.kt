package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AndroidAudioPlayer
import com.example.data.DailyRewardCatalog
import com.example.data.DailyRewardTier
import com.example.data.ProgressionManager
import com.example.ui.components.TacticalSupplyChestGraphic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tactical Mobile Game Daily Supply Drop Protocol.
 * - Compact, sleek tactical modal
 * - 1-Click claim: tapping the crate card immediately collects the reward
 * - Clear 7-Day sequence with visual progression markers
 * - Proper 24-Hour countdown and duplicate-claim prevention
 */
@Composable
fun DailyRewardDialog(
    progressionManager: ProgressionManager,
    onDismiss: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    var canClaim by remember { mutableStateOf(progressionManager.canClaimDailyReward()) }
    var currentStreak by remember { mutableStateOf(progressionManager.getCurrentStreakDay()) }
    var remainingTimeText by remember { mutableStateOf(progressionManager.formatTimeUntilNextDailyReward()) }

    val todayTier = remember(currentStreak) { DailyRewardCatalog.getTierForDay(currentStreak) }

    var isClaiming by remember { mutableStateOf(false) }
    var hasClaimedJustNow by remember { mutableStateOf(false) }
    val chestScale = remember { Animatable(1f) }

    // Live countdown update when on cooldown
    LaunchedEffect(canClaim) {
        while (!canClaim) {
            delay(1000)
            val nowCanClaim = progressionManager.canClaimDailyReward()
            if (nowCanClaim) {
                canClaim = true
            }
            remainingTimeText = progressionManager.formatTimeUntilNextDailyReward()
        }
    }

    val executeClaim: () -> Unit = {
        if (!isClaiming && canClaim) {
            isClaiming = true
            coroutineScope.launch {
                audioPlayer.buttonClick()
                try {
                    // Scale punch animation
                    chestScale.animateTo(1.2f, tween(120, easing = FastOutSlowInEasing))
                    chestScale.animateTo(1.0f, tween(120, easing = FastOutSlowInEasing))

                    val result = progressionManager.claimDailyReward()
                    if (result != null) {
                        audioPlayer.victory()
                        hasClaimedJustNow = true
                        canClaim = false
                        currentStreak = progressionManager.getCurrentStreakDay()
                        remainingTimeText = progressionManager.formatTimeUntilNextDailyReward()
                        delay(1000)
                        onDismiss()
                    } else {
                        isClaiming = false
                    }
                } catch (e: Exception) {
                    isClaiming = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isClaiming) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // Full screen scrim capture to guarantee outside-click dismissal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isClaiming) {
                        audioPlayer.buttonClick()
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0B132B),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 340.dp)
                    .fillMaxWidth(0.88f)
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Prevent click propagation to outside scrim
                    }
                    .testTag("daily_reward_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Compact Top Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, Color(0xFFFBBF24), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DAILY DROP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, Color(0xFFFBBF24), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "DAY $currentStreak/7",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }

                        // Responsive 48dp touch target Close Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("daily_reward_close_button")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button
                                ) {
                                    if (!isClaiming) {
                                        audioPlayer.buttonClick()
                                        onDismiss()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFF475569), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Daily Reward",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Interactive One-Click Crate Card (Tapping claims immediately)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    if (canClaim) listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    else listOf(Color(0xFF131B2E), Color(0xFF0A0F1D))
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = if (canClaim) 1.5.dp else 1.dp,
                                color = when {
                                    hasClaimedJustNow -> Color(0xFF34D399)
                                    canClaim -> Color(0xFFF59E0B)
                                    else -> Color(0xFF334155)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (canClaim) {
                                    executeClaim()
                                } else if (!isClaiming) {
                                    audioPlayer.buttonClick()
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("daily_claim_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Compact Supply Drop Crate Graphic (scaled down to 76dp)
                            TacticalSupplyChestGraphic(
                                isOpen = hasClaimedJustNow,
                                glowColor = if (hasClaimedJustNow) Color(0xFF34D399) else Color(0xFFF59E0B),
                                modifier = Modifier
                                    .size(76.dp)
                                    .scale(chestScale.value)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Visual Reward Badges (Tokens & XP)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (todayTier.tokens > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                            .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = "Tokens",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+${todayTier.tokens} TOKENS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                }

                                if (todayTier.xp > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                            .border(0.5.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ElectricBolt,
                                            contentDescription = "XP",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+${todayTier.xp} XP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFBAE6FD)
                                        )
                                    }
                                }
                            }

                            if (todayTier.specialBadge != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "BONUS: ${todayTier.specialBadge.uppercase()}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF472B6)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // One-Click Claim Prompt / Status
                            if (hasClaimedJustNow) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF064E3B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "REWARD SECURED!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            } else if (isClaiming) {
                                Text(
                                    text = "SECURING DROP...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFBBF24)
                                )
                            } else if (canClaim) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "TAP CRATE TO CLAIM",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = Color(0xFF1A0A00)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "NEXT DROP IN $remainingTimeText",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Compact 7-Days Progression Strip (36dp height)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (day in 1..7) {
                            val tier = DailyRewardCatalog.getTierForDay(day)
                            val isPast = day < currentStreak || (!canClaim && day <= currentStreak)
                            val isCurrent = day == currentStreak && canClaim
                            val isUpcoming = day > currentStreak

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(
                                        color = when {
                                            isCurrent -> Color(0xFF1E293B)
                                            isPast -> Color(0xFF0F172A).copy(alpha = 0.6f)
                                            else -> Color(0xFF0A0F1D)
                                        },
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .border(
                                        width = if (isCurrent) 1.dp else 0.5.dp,
                                        color = when {
                                            isCurrent -> Color(0xFFFBBF24)
                                            isPast -> Color(0xFF10B981)
                                            else -> Color(0xFF334155)
                                        },
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .padding(vertical = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "D$day",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color(0xFFFDE68A) else Color(0xFF94A3B8)
                                )

                                if (isPast) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Claimed",
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(11.dp)
                                    )
                                } else if (isUpcoming) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(10.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MonetizationOn,
                                        contentDescription = "Ready",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(11.dp)
                                    )
                                }

                                Text(
                                    text = if (tier.tokens > 0) "${tier.tokens}" else "${tier.xp}x",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
