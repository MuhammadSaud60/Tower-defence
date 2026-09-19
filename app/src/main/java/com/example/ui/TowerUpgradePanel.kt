package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Floating tower controls rendered directly over the battlefield near the selected tower.
 * Complies with Requirements 3, 4, 5:
 * - Shows a small floating action icon near the tower
 * - Clicking that opens a compact popup with Upgrade, Sell, Target Strategy, and Close
 * - Kept strictly within screen boundaries so it never renders off-screen
 */
@Composable
fun FloatingTowerControls(
    tower: Tower,
    screenX: Float,
    screenY: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    playerCoins: Int,
    onUpgrade: () -> Unit,
    onSell: () -> Unit,
    onStrategyChange: (TargetingStrategy) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isPopupOpen by remember(tower.id) { mutableStateOf(true) }

    val btnSizePx = with(density) { 42.dp.toPx() }
    val popupWidthPx = with(density) { 260.dp.toPx() }
    val popupHeightPx = with(density) { 250.dp.toPx() }

    if (!isPopupOpen) {
        // 1. Small Floating Icon / Button near the tower
        val btnLeft = (screenX + with(density) { 20.dp.toPx() })
            .coerceIn(with(density) { 8.dp.toPx() }, maxWidthPx - btnSizePx - with(density) { 8.dp.toPx() })
        val btnTop = (screenY - with(density) { 36.dp.toPx() })
            .coerceIn(with(density) { 8.dp.toPx() }, maxHeightPx - btnSizePx - with(density) { 8.dp.toPx() })

        Box(
            modifier = modifier
                .offset { IntOffset(btnLeft.roundToInt(), btnTop.roundToInt()) }
        ) {
            Surface(
                onClick = { isPopupOpen = true },
                shape = CircleShape,
                color = Color(0xFF0284C7),
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("tower_options_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Upgrade,
                        contentDescription = "Tower Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    } else {
        // 2. Compact Floating Popup kept strictly within screen boundaries
        val popupLeft = (screenX - popupWidthPx / 2f)
            .coerceIn(with(density) { 8.dp.toPx() }, maxWidthPx - popupWidthPx - with(density) { 8.dp.toPx() })

        val preferredTopAbove = screenY - popupHeightPx - with(density) { 24.dp.toPx() }
        val popupTop = if (preferredTopAbove >= with(density) { 10.dp.toPx() }) {
            preferredTopAbove
        } else {
            (screenY + with(density) { 28.dp.toPx() })
                .coerceIn(with(density) { 10.dp.toPx() }, maxHeightPx - popupHeightPx - with(density) { 10.dp.toPx() })
        }

        Box(
            modifier = modifier
                .offset { IntOffset(popupLeft.roundToInt(), popupTop.roundToInt()) }
        ) {
            TowerUpgradePanel(
                tower = tower,
                playerCoins = playerCoins,
                onUpgrade = onUpgrade,
                onSell = onSell,
                onStrategyChange = onStrategyChange,
                onClose = onClose
            )
        }
    }
}

/**
 * Compact, floating popup for tower upgrades, selling, and targeting strategy.
 */
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF21E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f)),
        modifier = modifier
            .width(260.dp)
            .testTag("tower_upgrade_panel")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // Header: Name, Stars, and Close Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tower.spec.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Row {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Level $i",
                                tint = if (i <= tower.spec.level) Color(0xFFFFD166) else Color(0xFF475569),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("close_tower_panel")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats row: Damage, Range, Speed (and Armor Piercing if any)
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                StatItem(
                    label = "DMG",
                    value = String.format(Locale.US, "%.0f", tower.spec.damage),
                    icon = Icons.Default.ElectricBolt,
                    color = Color(0xFFF59E0B)
                )
                StatItem(
                    label = "RNG",
                    value = String.format(Locale.US, "%.0f", tower.spec.range),
                    icon = Icons.Default.Shield,
                    color = Color(0xFF38BDF8)
                )
                StatItem(
                    label = "APS",
                    value = String.format(Locale.US, "%.1f", tower.spec.attacksPerSecond),
                    icon = Icons.Default.Speed,
                    color = Color(0xFF10B981)
                )
                if (tower.spec.armorPiercing > 0f) {
                    StatItem(
                        label = "AP",
                        value = "${(tower.spec.armorPiercing * 100).toInt()}%",
                        icon = Icons.Default.ElectricBolt,
                        color = Color(0xFFA855F7)
                    )
                }
                if (tower.spec.slowFactor > 0f) {
                    StatItem(
                        label = "SLOW",
                        value = "${(tower.spec.slowFactor * 100).toInt()}%",
                        icon = Icons.Default.Speed,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Targeting Strategy row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Target:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TargetingStrategy.values().forEach { strategy ->
                        val isSelected = tower.targetingStrategy == strategy
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStrategyChange(strategy) },
                            label = {
                                Text(
                                    text = strategy.name.take(4),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons: Sell and Upgrade
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sell button
                OutlinedButton(
                    onClick = onSell,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("sell_tower_button")
                ) {
                    Text(
                        text = "Sell +${tower.sellRefundCoins}🪙",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Upgrade button
                val canAffordUpgrade = playerCoins >= tower.spec.upgradeCost
                val isMax = tower.isMaxLevel

                Button(
                    onClick = onUpgrade,
                    enabled = !isMax && canAffordUpgrade,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .testTag("upgrade_tower_button")
                ) {
                    if (isMax) {
                        Text(
                            text = "MAX LEVEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Upgrade,
                                contentDescription = "Upgrade",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Up ${tower.spec.upgradeCost}🪙",
                                fontSize = 11.sp,
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
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Text(
            text = label,
            fontSize = 8.sp,
            color = Color(0xFF94A3B8)
        )
    }
}
