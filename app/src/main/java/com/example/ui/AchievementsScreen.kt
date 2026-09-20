package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.data.Achievement
import com.example.data.AchievementCatalog
import com.example.data.ProgressionManager
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GameCurrencyBadge

/**
 * Tactical Combat Achievements Screen.
 * Displays milestone challenges, progress meters, and allows claiming tokens and XP.
 */
@Composable
fun AchievementsScreen(
    progressionManager: ProgressionManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    var refreshKey by remember { mutableStateOf(0) }

    val achievements = remember(refreshKey) { AchievementCatalog.allAchievements }
    val tokens = remember(refreshKey) { progressionManager.getTokens() }

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
            .testTag("achievements_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // 1. TOP HEADER: Navigation & Tokens Bank
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
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
                            .size(40.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), CircleShape)
                            .testTag("achievements_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Achievements",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TACTICAL ACHIEVEMENTS",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Combat milestones grant tokens, XP, and honor badges",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                GameCurrencyBadge(amount = tokens, isCompact = false)
            }

            // 2. ACHIEVEMENTS LIST
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                achievements.forEach { achievement ->
                    val progress = progressionManager.getAchievementProgress(achievement.id)
                    val isUnlocked = progressionManager.isAchievementUnlocked(achievement.id)
                    val isClaimed = progressionManager.isAchievementClaimed(achievement.id)

                    AchievementCard(
                        achievement = achievement,
                        currentProgress = progress,
                        isUnlocked = isUnlocked,
                        isClaimed = isClaimed,
                        onClaim = {
                            val reward = progressionManager.claimAchievement(achievement.id)
                            if (reward != null) {
                                audioPlayer.victory()
                                refreshKey++
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    currentProgress: Int,
    isUnlocked: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    val progressRatio = (currentProgress.toFloat() / achievement.targetGoal).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = when {
                isClaimed -> Color(0xFF10B981)
                isUnlocked -> Color(0xFFF59E0B)
                else -> Color(0xFF334155)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_card_${achievement.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emblem Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = when {
                            isClaimed -> Color(0xFF065F46)
                            isUnlocked -> Color(0xFF78350F)
                            else -> Color(0xFF1E293B)
                        },
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (isUnlocked || isClaimed) Color(0xFFFBBF24) else Color(0xFF475569),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = if (isUnlocked || isClaimed) Color(0xFFFFD166) else Color(0xFF64748B),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details & Progress Bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    // Rewards Pill
                    Text(
                        text = "+${achievement.rewardTokens} 🪙  +${achievement.rewardXp} ⚡",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD166)
                    )
                }

                Text(
                    text = achievement.description,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp),
                        color = if (isUnlocked) Color(0xFF10B981) else Color(0xFF38BDF8),
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${minOf(currentProgress, achievement.targetGoal)} / ${achievement.targetGoal}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Button / Status
            when {
                isClaimed -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Claimed",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CLAIMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                }
                isUnlocked -> {
                    GameButton(
                        text = "CLAIM",
                        icon = Icons.Default.EmojiEvents,
                        variant = GameButtonVariant.PRIMARY,
                        height = 34.dp,
                        onClick = onClaim,
                        modifier = Modifier.width(96.dp),
                        testTag = "claim_btn_${achievement.id}"
                    )
                }
                else -> {
                    Text(
                        text = "LOCKED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
