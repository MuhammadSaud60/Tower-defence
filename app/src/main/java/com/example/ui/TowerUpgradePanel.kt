package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import java.util.Locale

@Composable
fun TowerUpgradePanel(
    tower: Tower,
    playerCoins: Int,
    onUpgrade: () -> Unit,
    onSell: () -> Unit,
    onStrategyChange: (TargetingStrategy) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tower_upgrade_panel")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            // Header: Name, Stars, and Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tower.spec.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Level stars
                    Row {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Level $i",
                                tint = if (i <= tower.spec.level) Color(0xFFFFD166) else Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp).testTag("close_tower_panel")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row: Damage, Range, Fire rate
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                StatItem(
                    label = "Damage",
                    value = String.format(Locale.US, "%.0f", tower.spec.damage),
                    icon = Icons.Default.ElectricBolt,
                    color = Color(0xFFF59E0B)
                )
                StatItem(
                    label = "Range",
                    value = String.format(Locale.US, "%.0f", tower.spec.range),
                    icon = Icons.Default.Shield,
                    color = Color(0xFF38BDF8)
                )
                StatItem(
                    label = "Speed",
                    value = String.format(Locale.US, "%.1f/s", tower.spec.attacksPerSecond),
                    icon = Icons.Default.Speed,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Targeting strategy row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Target:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TargetingStrategy.values().forEach { strategy ->
                        val isSelected = tower.targetingStrategy == strategy
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStrategyChange(strategy) },
                            label = {
                                Text(
                                    text = strategy.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons: Upgrade and Sell
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sell button
                OutlinedButton(
                    onClick = onSell,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("sell_tower_button")
                ) {
                    Text(
                        text = "Sell +${tower.sellRefundCoins}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Upgrade button
                val canAffordUpgrade = playerCoins >= tower.spec.upgradeCost
                val isMax = tower.isMaxLevel

                Button(
                    onClick = onUpgrade,
                    enabled = !isMax && canAffordUpgrade,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.weight(1.5f).height(44.dp).testTag("upgrade_tower_button")
                ) {
                    if (isMax) {
                        Text(
                            text = "MAX LEVEL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Upgrade,
                                contentDescription = "Upgrade",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Upgrade (${tower.spec.upgradeCost}🪙)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAffordUpgrade) Color.White else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8)
        )
    }
}
