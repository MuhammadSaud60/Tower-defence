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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import com.example.entities.TowerType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.data.ProgressionManager
import com.example.ui.components.GameCurrencyBadge

/**
 * Commander Profile & Lifetime Combat Statistics Screen.
 */
@Composable
fun PlayerProfileScreen(
    progressionManager: ProgressionManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val level = remember { progressionManager.getPlayerLevel() }
    val currentXp = remember { progressionManager.getCurrentXp() }
    val nextXp = remember { progressionManager.getXpForNextLevel(level) }
    val xpRatio = remember { progressionManager.getXpProgressRatio() }
    val totalStars = remember { progressionManager.getTotalStars() }
    val tokens = remember { progressionManager.getTokens() }
    val stats = remember { progressionManager.getPlayerStats() }

    val rankTitle = when (level) {
        1 -> "Tactical Recruit"
        2 -> "Field Lieutenant"
        3 -> "Brigade Captain"
        4 -> "Citadel Major"
        5 -> "Apex Sentinel"
        else -> "Supreme Commander"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF070B14), Color(0xFF0F172A), Color(0xFF0A0F1D))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("player_profile_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. TOP BAR: Back Navigation & Screen Title
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
                            .testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "COMMANDER DOSSIER",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Permanent service record and operational statistics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                GameCurrencyBadge(amount = tokens, isCompact = false)
            }

            // 2. COMMANDER IDENTITY & ACCOUNT LEVEL CARD
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Military Insignia Crest
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                    ),
                                    shape = CircleShape
                                )
                                .border(2.dp, Color(0xFF38BDF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MilitaryTech,
                                contentDescription = "Rank Emblem",
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "COMMANDER LEVEL $level",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFFFDE68A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF0369A1), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = rankTitle.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // XP Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "XP: $currentXp / $nextXp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "+150 Tokens at Next Level",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { xpRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Overview Quick Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickInfoPill("Total Stars", "$totalStars / 39", Icons.Default.Star, Color(0xFFFBBF24), Modifier.weight(1f))
                        QuickInfoPill("Tokens Bank", "$tokens 🪙", Icons.Default.Shield, Color(0xFFFFD166), Modifier.weight(1f))
                        QuickInfoPill("Total Wins", "${stats.missionsWon}", Icons.Default.EmojiEvents, Color(0xFF10B981), Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2.5 ARSENAL: TOWERS UNLOCKED
            Text(
                text = "ARSENAL BLUEPRINTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TowerType.MACHINE_GUN,
                    TowerType.CANNON,
                    TowerType.RAPID_FIRE,
                    TowerType.FROST_GUN
                ).forEach { towerType ->
                    val isUnlocked = progressionManager.isTowerUnlocked(towerType)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUnlocked) Color(0xFF0284C7) else Color(0xFF334155)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                                contentDescription = if (isUnlocked) "Unlocked" else "Locked",
                                tint = if (isUnlocked) Color(0xFF34D399) else Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = towerType.displayName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = if (isUnlocked) "READY" else "LOCKED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. LIFETIME COMBAT STATISTICS GRID
            Text(
                text = "LIFETIME COMBAT ENGAGEMENTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFF38BDF8)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard("Invaders Destroyed", "${stats.enemiesKilled}", Color(0xFFEF4444), Modifier.weight(1f))
                    StatCard("Bosses Vanquished", "${stats.bossesKilled}", Color(0xFFF59E0B), Modifier.weight(1f))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard("Towers Deployed", "${stats.towersPlaced}", Color(0xFF38BDF8), Modifier.weight(1f))
                    StatCard("Obstacles Cleared", "${stats.destructiblesDestroyed}", Color(0xFF10B981), Modifier.weight(1f))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCard("Total Damage Inflicted", "${stats.damageDealt}", Color(0xFFA855F7), Modifier.weight(1f))
                    StatCard("Upgrades Purchased", "${stats.researchUpgradesPurchased}", Color(0xFFEC4899), Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. SERVICE MEDALS & RIBBONS SHOWCASE
            Text(
                text = "SERVICE CITATIONS & MEDALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFFFBBF24)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MedalBadge("First Blood", "First Victory", stats.missionsWon >= 1)
                MedalBadge("Goliath Slayer", "5 Bosses", stats.bossesKilled >= 5)
                MedalBadge("Master Architect", "30 Turrets", stats.towersPlaced >= 30)
                MedalBadge("Apex Commander", "Level 5", level >= 5)
            }
        }
    }
}

@Composable
private fun QuickInfoPill(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = label.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = accentColor)
        }
    }
}

@Composable
private fun MedalBadge(
    title: String,
    requirement: String,
    isEarned: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isEarned) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEarned) Color(0xFFCA8A04) else Color(0xFF334155)
        ),
        modifier = Modifier.size(width = 80.dp, height = 70.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = title,
                tint = if (isEarned) Color(0xFFFBBF24) else Color(0xFF475569),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                color = if (isEarned) Color.White else Color(0xFF64748B),
                maxLines = 1
            )
            Text(
                text = requirement,
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                color = if (isEarned) Color(0xFFFDE68A) else Color(0xFF475569)
            )
        }
    }
}
