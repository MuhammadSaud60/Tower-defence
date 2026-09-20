package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AndroidAudioPlayer
import com.example.data.MilestoneChest
import com.example.data.MilestoneChestCatalog
import com.example.data.MilestoneType
import com.example.data.ProgressionManager
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant

/**
 * Tactical Milestone Supply Crates Dialog.
 * Deterministic milestone crates awarded for earning campaign stars and reaching account levels.
 */
@Composable
fun RewardChestDialog(
    progressionManager: ProgressionManager,
    onDismiss: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    var refreshKey by remember { mutableStateOf(0) }

    val allChests = remember(refreshKey) { MilestoneChestCatalog.allMilestones }
    val totalStars = remember(refreshKey) { progressionManager.getTotalStars() }
    val playerLevel = remember(refreshKey) { progressionManager.getPlayerLevel() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    audioPlayer.buttonClick()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 560.dp)
                    .fillMaxWidth(0.92f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Prevent click propagation
                    }
                    .testTag("reward_chest_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TACTICAL MILESTONE CRATES",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Permanent supply caches unlocked through achievements",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("reward_chest_close_button")
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button
                                ) {
                                    audioPlayer.buttonClick()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .border(1.dp, Color(0xFF475569), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFE2E8F0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Crates Grid/List
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allChests.forEach { chest ->
                            val isClaimed = progressionManager.isMilestoneChestClaimed(chest.id)
                            val isAvailable = progressionManager.isMilestoneChestAvailable(chest)
                            val currentVal = if (chest.type == MilestoneType.STARS) totalStars else playerLevel
                            val targetVal = chest.requiredValue

                            CrateRowItem(
                                chest = chest,
                                currentVal = currentVal,
                                targetVal = targetVal,
                                isAvailable = isAvailable,
                                isClaimed = isClaimed,
                                onClaim = {
                                    if (progressionManager.claimMilestoneChest(chest.id) != null) {
                                        audioPlayer.victory()
                                        refreshKey++
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    GameButton(
                        text = "CLOSE REQUISITION",
                        variant = GameButtonVariant.SECONDARY,
                        height = 42.dp,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CrateRowItem(
    chest: MilestoneChest,
    currentVal: Int,
    targetVal: Int,
    isAvailable: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isClaimed -> Color(0xFF10B981)
                isAvailable -> Color(0xFFF59E0B)
                else -> Color(0xFF334155)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isAvailable) Color(0xFF78350F) else Color(0xFF0F172A),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (isAvailable) Color(0xFFFBBF24) else Color(0xFF475569),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (isAvailable) Color(0xFFFFD166) else Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chest.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = chest.description,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "REWARDS: +${chest.tokensReward} 🪙 TOKENS  +${chest.xpReward} ⚡ XP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD166)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isClaimed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "CLAIMED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
                isAvailable -> {
                    GameButton(
                        text = "UNSEAL",
                        icon = Icons.Default.EmojiEvents,
                        variant = GameButtonVariant.PRIMARY,
                        height = 32.dp,
                        onClick = onClaim,
                        modifier = Modifier.width(90.dp)
                    )
                }
                else -> {
                    Text(
                        text = "$currentVal / $targetVal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
