package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Mobile-Game Style Radial/Circular Tower Interaction Menu.
 * Complies with Requirements 1-7:
 * - Selected tower remains in the center
 * - 4 circular action buttons appear radially around the tower:
 *     [TARGET] (Top)
 *   [SELL] ← [TOWER] → [UPGRADE]
 *     [INFO] (Bottom)
 * - Animated outward spring transition
 * - Screen Edge Handling: fully clamped so no button or badge ever clips outside screen
 * - Compact Info tooltip toggle that preserves battlefield visibility
 */
@Composable
fun TowerRadialMenu(
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
    var showStatsBubble by remember(tower.id) { mutableStateOf(false) }

    // Spring animation for buttons radiating outward from the tower
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "radial_expand"
    )

    val menuRadiusPx = with(density) { 68.dp.toPx() }
    val buttonRadiusPx = with(density) { 24.dp.toPx() }
    val badgeSafeMarginPx = with(density) { 22.dp.toPx() }

    // Total radial extent needed from center to prevent clipping
    val totalExtentPx = menuRadiusPx + buttonRadiusPx + badgeSafeMarginPx

    // Clamp effective radial menu center so all 4 buttons remain 100% inside screen
    val safeMinX = totalExtentPx + with(density) { 6.dp.toPx() }
    val safeMaxX = maxWidthPx - totalExtentPx - with(density) { 6.dp.toPx() }
    val safeMinY = totalExtentPx + with(density) { 6.dp.toPx() }
    val safeMaxY = maxHeightPx - totalExtentPx - with(density) { 6.dp.toPx() }

    val centerX = if (safeMaxX > safeMinX) screenX.coerceIn(safeMinX, safeMaxX) else (maxWidthPx / 2f)
    val centerY = if (safeMaxY > safeMinY) screenY.coerceIn(safeMinY, safeMaxY) else (maxHeightPx / 2f)

    val canAffordUpgrade = playerCoins >= tower.spec.upgradeCost
    val isMaxLevel = tower.isMaxLevel

    // Cycle strategy: FIRST -> LAST -> STRONGEST -> CLOSEST -> FIRST
    fun cycleStrategy() {
        val next = when (tower.targetingStrategy) {
            TargetingStrategy.FIRST -> TargetingStrategy.LAST
            TargetingStrategy.LAST -> TargetingStrategy.STRONGEST
            TargetingStrategy.STRONGEST -> TargetingStrategy.CLOSEST
            TargetingStrategy.CLOSEST -> TargetingStrategy.FIRST
        }
        onStrategyChange(next)
    }

    Box(modifier = modifier) {
        // -------------------------------------------------------------
        // 1. TOP ACTION: TARGET STRATEGY (Crosshair / Target radar icon)
        // -------------------------------------------------------------
        val topBtnX = centerX
        val topBtnY = centerY - (menuRadiusPx * animProgress)
        val stratBadgeText = when (tower.targetingStrategy) {
            TargetingStrategy.FIRST -> "1ST"
            TargetingStrategy.LAST -> "LAST"
            TargetingStrategy.STRONGEST -> "STRONG"
            TargetingStrategy.CLOSEST -> "CLOSE"
        }

        RadialMenuButton(
            x = topBtnX,
            y = topBtnY,
            icon = Icons.Default.MyLocation,
            badgeText = stratBadgeText,
            badgeColor = Color(0xFF38BDF8),
            badgeBelow = false,
            primaryColor = Color(0xFF0369A1),
            borderColor = Color(0xFF38BDF8),
            scale = animProgress,
            onClick = { cycleStrategy() },
            testTag = "radial_action_target"
        )

        // -------------------------------------------------------------
        // 2. RIGHT ACTION: UPGRADE (Arrow Upward icon + Cost Badge)
        // -------------------------------------------------------------
        val rightBtnX = centerX + (menuRadiusPx * animProgress)
        val rightBtnY = centerY
        val upgradeBadge = if (isMaxLevel) "MAX" else "${tower.spec.upgradeCost}🪙"
        val upgradeColor = when {
            isMaxLevel -> Color(0xFF475569)
            canAffordUpgrade -> Color(0xFF15803D)
            else -> Color(0xFF334155)
        }
        val upgradeBorder = when {
            isMaxLevel -> Color(0xFF94A3B8)
            canAffordUpgrade -> Color(0xFF4ADE80)
            else -> Color(0xFFEF4444)
        }

        RadialMenuButton(
            x = rightBtnX,
            y = rightBtnY,
            icon = if (isMaxLevel) Icons.Default.Star else Icons.Default.ArrowUpward,
            badgeText = upgradeBadge,
            badgeColor = if (isMaxLevel) Color(0xFFCBD5E1) else if (canAffordUpgrade) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
            badgeBelow = true,
            primaryColor = upgradeColor,
            borderColor = upgradeBorder,
            scale = animProgress,
            enabled = !isMaxLevel && canAffordUpgrade,
            onClick = { onUpgrade() },
            testTag = "radial_action_upgrade"
        )

        // -------------------------------------------------------------
        // 3. LEFT ACTION: SELL (Coin icon + Refund Badge)
        // -------------------------------------------------------------
        val leftBtnX = centerX - (menuRadiusPx * animProgress)
        val leftBtnY = centerY
        val sellBadge = "+${tower.sellRefundCoins}🪙"

        RadialMenuButton(
            x = leftBtnX,
            y = leftBtnY,
            icon = Icons.Default.MonetizationOn,
            badgeText = sellBadge,
            badgeColor = Color(0xFFFBBF24),
            badgeBelow = true,
            primaryColor = Color(0xFF991B1B),
            borderColor = Color(0xFFF87171),
            scale = animProgress,
            onClick = { onSell() },
            testTag = "radial_action_sell"
        )

        // -------------------------------------------------------------
        // 4. BOTTOM ACTION: INFO (Information icon -> toggles stats bubble)
        // -------------------------------------------------------------
        val bottomBtnX = centerX
        val bottomBtnY = centerY + (menuRadiusPx * animProgress)

        RadialMenuButton(
            x = bottomBtnX,
            y = bottomBtnY,
            icon = Icons.Default.Info,
            badgeText = "LVL ${tower.spec.level}",
            badgeColor = Color(0xFFA5B4FC),
            badgeBelow = true,
            primaryColor = if (showStatsBubble) Color(0xFF4338CA) else Color(0xFF312E81),
            borderColor = if (showStatsBubble) Color(0xFF818CF8) else Color(0xFF6366F1),
            scale = animProgress,
            onClick = { showStatsBubble = !showStatsBubble },
            testTag = "radial_action_info"
        )

        // -------------------------------------------------------------
        // 5. COMPACT STATS TOOLTIP (Appears when Info button is toggled)
        // Sits neatly below or above the menu without covering battlefield
        // -------------------------------------------------------------
        if (showStatsBubble) {
            val tooltipY = if (centerY < maxHeightPx * 0.65f) {
                centerY + menuRadiusPx + buttonRadiusPx + with(density) { 32.dp.toPx() }
            } else {
                centerY - menuRadiusPx - buttonRadiusPx - with(density) { 62.dp.toPx() }
            }

            CompactStatsTooltip(
                tower = tower,
                x = centerX,
                y = tooltipY,
                maxWidthPx = maxWidthPx,
                onClose = { showStatsBubble = false }
            )
        }

        // -------------------------------------------------------------
        // 6. DISMISS / CLOSE BUTTON (Top-Right of radial cluster)
        // -------------------------------------------------------------
        val closeBtnX = centerX + (menuRadiusPx * 0.72f * animProgress)
        val closeBtnY = centerY - (menuRadiusPx * 0.72f * animProgress)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (closeBtnX - with(density) { 16.dp.toPx() }).roundToInt(),
                        (closeBtnY - with(density) { 16.dp.toPx() }).roundToInt()
                    )
                }
                .scale(animProgress)
                .alpha(animProgress.coerceIn(0f, 1f))
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xEE1E293B),
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, Color(0xFF94A3B8), CircleShape)
                    .clickable(onClick = onClose)
                    .testTag("radial_action_close")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual circular action button in the radial menu.
 */
@Composable
private fun RadialMenuButton(
    x: Float,
    y: Float,
    icon: ImageVector,
    badgeText: String,
    badgeColor: Color,
    badgeBelow: Boolean,
    primaryColor: Color,
    borderColor: Color,
    scale: Float,
    enabled: Boolean = true,
    onClick: () -> Unit,
    testTag: String
) {
    val density = LocalDensity.current
    val btnSizeDp = 44.dp
    val btnHalfPx = with(density) { (btnSizeDp / 2).toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (x - btnHalfPx).roundToInt(),
                    (y - btnHalfPx).roundToInt()
                )
            }
            .scale(scale)
            .alpha(scale.coerceIn(0f, 1f))
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!badgeBelow) {
                // Badge above button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, borderColor.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Circular Action Button
            Surface(
                onClick = onClick,
                enabled = enabled,
                shape = CircleShape,
                color = primaryColor,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
                modifier = Modifier.size(btnSizeDp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        Brush.radialGradient(
                            colors = listOf(
                                borderColor.copy(alpha = 0.4f),
                                primaryColor
                            )
                        )
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = badgeText,
                        tint = if (enabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (badgeBelow) {
                // Badge below button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xDD0F172A),
                    border = androidx.compose.foundation.BorderStroke(0.75.dp, borderColor.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sleek, compact floating statistics tooltip card.
 * Displays DMG, RNG, SPD, and Armor Piercing without obscuring the combat arena.
 */
@Composable
private fun CompactStatsTooltip(
    tower: Tower,
    x: Float,
    y: Float,
    maxWidthPx: Float,
    onClose: () -> Unit
) {
    val density = LocalDensity.current
    val cardWidthDp = 240.dp
    val cardHalfWidthPx = with(density) { (cardWidthDp / 2).toPx() }
    val clampedX = x.coerceIn(
        cardHalfWidthPx + with(density) { 8.dp.toPx() },
        maxWidthPx - cardHalfWidthPx - with(density) { 8.dp.toPx() }
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (clampedX - cardHalfWidthPx).roundToInt(),
                    y.roundToInt()
                )
            }
            .width(cardWidthDp)
            .shadow(10.dp, RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00F172A),
                        Color(0xF01E293B)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                ),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("compact_stats_tooltip")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${tower.spec.name} (Lvl ${tower.spec.level})",
                    color = Color(0xFFF1F5F9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close stats",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatChip(label = "DMG", value = tower.spec.damage.toInt().toString(), color = Color(0xFFF87171))
                StatChip(label = "RNG", value = tower.spec.range.toInt().toString(), color = Color(0xFF38BDF8))
                StatChip(
                    label = "SPD",
                    value = String.format(Locale.US, "%.1f/s", tower.spec.attacksPerSecond),
                    color = Color(0xFFFBBF24)
                )
                if (tower.spec.splashRadius > 0f) {
                    StatChip(label = "SPL", value = tower.spec.splashRadius.toInt().toString(), color = Color(0xFFFB923C))
                } else if (tower.spec.slowFactor > 0f) {
                    StatChip(
                        label = "SLOW",
                        value = "${(tower.spec.slowFactor * 100).toInt()}%",
                        color = Color(0xFF38BDF8)
                    )
                } else if (tower.spec.armorPiercing > 0f) {
                    StatChip(
                        label = "AP",
                        value = "${(tower.spec.armorPiercing * 100).toInt()}%",
                        color = Color(0xFFA78BFA)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = "$label: ",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
