package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import com.example.entities.TargetingStrategy
import com.example.entities.Tower
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Premium circular radial menu for tower actions.
 * Appears directly around the selected tower in the circle style requested by the player.
 *
 * Options:
 * - UPGRADE (Top 12:00) with price pill below
 * - TARGET FOCUS (Right 3:00) with strategy pill below
 * - SELL (Left 9:00) with refund pill below
 * - DISMISS (Bottom 6:00)
 * - Tower info summary badge
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
    val context = LocalContext.current
    val audioPlayer = remember { AndroidAudioPlayer(context) }

    // Bouncy spring expansion outward from center
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tower_radial_expand"
    )

    val radialDistanceDp = 76.dp
    val radialDistancePx = with(density) { radialDistanceDp.toPx() } * animProgress

    // Safe bounds clamping so buttons never render offscreen
    val marginPx = with(density) { 68.dp.toPx() }
    val clampedCenterX = screenX.coerceIn(marginPx, maxWidthPx - marginPx)
    val clampedCenterY = screenY.coerceIn(marginPx, maxHeightPx - marginPx)

    val canAffordUpgrade = playerCoins >= tower.spec.upgradeCost
    val isMaxLevel = tower.isMaxLevel

    fun cycleStrategy() {
        val next = when (tower.targetingStrategy) {
            TargetingStrategy.FIRST -> TargetingStrategy.LAST
            TargetingStrategy.LAST -> TargetingStrategy.STRONGEST
            TargetingStrategy.STRONGEST -> TargetingStrategy.CLOSEST
            TargetingStrategy.CLOSEST -> TargetingStrategy.FIRST
        }
        onStrategyChange(next)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tower_contextual_panel")
    ) {
        // 1. Center Reticle ring directly around the selected gun
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (clampedCenterX - with(density) { 22.dp.toPx() }).roundToInt(),
                        (clampedCenterY - with(density) { 22.dp.toPx() }).roundToInt()
                    )
                }
                .size(44.dp)
                .background(Color(0x3338BDF8), CircleShape)
                .border(2.dp, Color(0xFF38BDF8), CircleShape)
                .scale(animProgress)
                .testTag("tower_center_reticle")
        )

        // 2. Compact info summary chip above the radial circle
        val statsWidthDp = 220.dp
        val statsWidthPx = with(density) { statsWidthDp.toPx() }
        val statsTopPx = (clampedCenterY - radialDistancePx - with(density) { 56.dp.toPx() })
            .coerceIn(with(density) { 8.dp.toPx() }, maxHeightPx - with(density) { 36.dp.toPx() })
        val statsLeftPx = (clampedCenterX - statsWidthPx / 2f)
            .coerceIn(with(density) { 8.dp.toPx() }, maxWidthPx - statsWidthPx - with(density) { 8.dp.toPx() })

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xF50B1220),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .offset { IntOffset(statsLeftPx.roundToInt(), statsTopPx.roundToInt()) }
                .width(statsWidthDp)
                .scale(animProgress)
                .testTag("tower_stats_chip")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tower.spec.name.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (isMaxLevel) Color(0xFF78350F) else Color(0xFF1E293B),
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isMaxLevel) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isMaxLevel) "MAX" else "LVL ${tower.spec.level}",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isMaxLevel) Color(0xFFFDE68A) else Color(0xFF7DD3FC)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DMG ${tower.spec.damage.toInt()}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171)
                    )
                    Text(
                        text = "RNG ${tower.spec.range.toInt()}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "SPD ${String.format(Locale.US, "%.1f/s", 1f / tower.spec.attackCooldown)}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
        }

        // 3. TOP (12:00): Circular UPGRADE Button
        RadialOptionButton(
            icon = if (isMaxLevel) Icons.Default.Star else Icons.Default.ArrowUpward,
            label = if (isMaxLevel) "MAX" else "UPGRADE",
            badge = if (isMaxLevel) "MAX" else "${tower.spec.upgradeCost}🪙",
            badgeColor = if (isMaxLevel) Color(0xFFFBBF24) else if (canAffordUpgrade) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
            accentGradient = if (isMaxLevel) listOf(Color(0xFF78350F), Color(0xFF451A03))
            else if (canAffordUpgrade) listOf(Color(0xFF22C55E), Color(0xFF15803D))
            else listOf(Color(0xFF475569), Color(0xFF334155)),
            centerX = clampedCenterX,
            centerY = clampedCenterY - radialDistancePx,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            enabled = !isMaxLevel && canAffordUpgrade,
            onClick = {
                audioPlayer.buttonClick()
                onUpgrade()
            },
            testTag = "radial_action_upgrade"
        )

        // 4. RIGHT (3:00): Circular TARGET FOCUS Button
        val stratName = when (tower.targetingStrategy) {
            TargetingStrategy.FIRST -> "FIRST"
            TargetingStrategy.LAST -> "LAST"
            TargetingStrategy.STRONGEST -> "STRONG"
            TargetingStrategy.CLOSEST -> "CLOSE"
        }
        RadialOptionButton(
            icon = Icons.Default.MyLocation,
            label = "TARGET",
            badge = stratName,
            badgeColor = Color(0xFF7DD3FC),
            accentGradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            centerX = clampedCenterX + radialDistancePx,
            centerY = clampedCenterY,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            enabled = true,
            onClick = {
                audioPlayer.buttonClick()
                cycleStrategy()
            },
            testTag = "radial_action_target"
        )

        // 5. LEFT (9:00): Circular SELL Button
        RadialOptionButton(
            icon = Icons.Default.MonetizationOn,
            label = "SELL",
            badge = "+${tower.sellRefundCoins}🪙",
            badgeColor = Color(0xFFFDE68A),
            accentGradient = listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
            centerX = clampedCenterX - radialDistancePx,
            centerY = clampedCenterY,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            enabled = true,
            onClick = {
                audioPlayer.buttonClick()
                onSell()
            },
            testTag = "radial_action_sell"
        )

        // 6. BOTTOM (6:00): Circular CLOSE Button
        RadialOptionButton(
            icon = Icons.Default.Close,
            label = "CLOSE",
            badge = "",
            badgeColor = Color.White,
            accentGradient = listOf(Color(0xFF475569), Color(0xFF334155)),
            centerX = clampedCenterX,
            centerY = clampedCenterY + radialDistancePx,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            enabled = true,
            isSmall = true,
            onClick = {
                audioPlayer.buttonClick()
                onClose()
            },
            testTag = "radial_action_close"
        )
    }
}

@Composable
private fun RadialOptionButton(
    icon: ImageVector,
    label: String,
    badge: String,
    badgeColor: Color,
    accentGradient: List<Color>,
    centerX: Float,
    centerY: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    scale: Float,
    enabled: Boolean,
    isSmall: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val density = LocalDensity.current
    val buttonRadiusDp = if (isSmall) 22.dp else 26.dp
    val buttonRadiusPx = with(density) { buttonRadiusDp.toPx() }
    val margin = buttonRadiusPx + with(density) { 16.dp.toPx() }

    val clampedX = centerX.coerceIn(margin, maxWidthPx - margin)
    val clampedY = centerY.coerceIn(margin, maxHeightPx - margin)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = tween(50),
        label = "press_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset {
                IntOffset(
                    (clampedX - buttonRadiusPx).roundToInt(),
                    (clampedY - buttonRadiusPx).roundToInt()
                )
            }
            .scale(scale * pressScale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
        ) {
            // Circular Action Button
            Surface(
                shape = CircleShape,
                color = if (enabled) Color(0xFF1E293B) else Color(0xFF0F172A),
                shadowElevation = if (enabled) 8.dp else 2.dp,
                modifier = Modifier
                    .size(if (isSmall) 44.dp else 52.dp)
                    .border(
                        width = if (enabled) 2.dp else 1.dp,
                        brush = if (enabled) Brush.linearGradient(accentGradient)
                        else Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF334155))),
                        shape = CircleShape
                    )
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .testTag(testTag)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (enabled) Brush.radialGradient(
                                listOf(accentGradient[0].copy(alpha = 0.5f), Color(0xFF0F172A))
                            ) else Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        )
                        .alpha(if (enabled) 1f else 0.45f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (enabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(if (isSmall) 20.dp else 24.dp)
                    )
                }
            }

            // External Badge Pill placed cleanly below the circle (Never obscures the icon!)
            if (badge.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xF20F172A),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (enabled) accentGradient[0].copy(alpha = 0.8f) else Color(0xFF475569)
                    ),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (enabled) badgeColor else Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                    )
                }
            }
        }
    }
}
