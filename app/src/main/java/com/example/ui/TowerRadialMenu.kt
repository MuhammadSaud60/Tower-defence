package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Premium mobile game contextual tower selection overlay.
 *
 * Features:
 * - Positioned near the selected tower without covering gameplay
 * - Procedural tower weapon artwork preview
 * - Quick-action Upgrade, Ability / Focus, and Sell buttons
 * - Spring popup entrance animation
 * - Strict screen bounds clamping for phones and tablets in landscape
 * - Avoids bulky bottom panels
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

    val panelWidthDp = 224.dp
    val panelHeightDp = 196.dp
    val panelWidthPx = with(density) { panelWidthDp.toPx() }
    val panelHeightPx = with(density) { panelHeightDp.toPx() }
    val marginPx = with(density) { 10.dp.toPx() }
    val topHudReservePx = with(density) { 52.dp.toPx() }
    val towerOffsetPx = with(density) { 42.dp.toPx() }

    // Smart horizontal placement:
    // Place to the right if space allows, otherwise place to the left of the tower
    val desiredX = if (screenX + towerOffsetPx + panelWidthPx <= maxWidthPx - marginPx) {
        screenX + towerOffsetPx
    } else if (screenX - towerOffsetPx - panelWidthPx >= marginPx) {
        screenX - towerOffsetPx - panelWidthPx
    } else {
        ((maxWidthPx - panelWidthPx) / 2f).coerceIn(marginPx, maxWidthPx - panelWidthPx - marginPx)
    }

    // Smart vertical placement: vertically align with tower, strictly inside safe viewport
    val desiredY = (screenY - panelHeightPx / 2f).coerceIn(
        marginPx + topHudReservePx,
        maxHeightPx - panelHeightPx - marginPx
    )

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

    Box(modifier = modifier.fillMaxSize()) {
        // Connector line pointing to tower
        Canvas(modifier = Modifier.fillMaxSize()) {
            val panelCenterX = desiredX + if (desiredX > screenX) 0f else panelWidthPx
            val panelCenterY = desiredY + panelHeightPx / 2f

            drawLine(
                color = Color(0x6638BDF8),
                start = Offset(screenX, screenY),
                end = Offset(panelCenterX, panelCenterY),
                strokeWidth = 2f
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 3f,
                center = Offset(screenX, screenY)
            )
        }

        // Popup Contextual Card
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                initialScale = 0.78f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(140)),
            modifier = Modifier.offset {
                IntOffset(desiredX.roundToInt(), desiredY.roundToInt())
            }
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xF50B1220),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .width(panelWidthDp)
                    .testTag("tower_contextual_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Header: Tower Weapon Artwork, Name, Level Badge, and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Tower Procedural Image
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(32.dp)) {
                                    WeaponArtwork.drawWeapon(
                                        drawScope = this,
                                        type = tower.spec.type,
                                        cx = size.width / 2f,
                                        cy = size.height / 2f,
                                        scale = 0.38f,
                                        isLocked = false,
                                        recoilProgress = 0f,
                                        animTime = 0f,
                                        level = tower.spec.level
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Name & Level
                            Column {
                                Text(
                                    text = tower.spec.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            text = if (isMaxLevel) "MAX LVL" else "LVL ${tower.spec.level}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isMaxLevel) Color(0xFFFDE68A) else Color(0xFF7DD3FC)
                                        )
                                    }
                                }
                            }
                        }

                        // Close button (48x48 interactive touch boundary)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(onClick = onClose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Compact Stats Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DMG ${tower.spec.damage.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171)
                        )
                        Text(
                            text = "RNG ${tower.spec.range.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "SPD ${String.format(Locale.US, "%.1f/s", 1f / tower.spec.attackCooldown)}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 1: Upgrade Button
                    ContextualMenuAction(
                        icon = if (isMaxLevel) Icons.Default.Star else Icons.Default.ArrowUpward,
                        title = if (isMaxLevel) "MAX LEVEL" else "UPGRADE",
                        badge = if (isMaxLevel) "MAX" else "${tower.spec.upgradeCost}🪙",
                        badgeColor = if (isMaxLevel) Color(0xFFFBBF24) else if (canAffordUpgrade) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                        bgColor = if (isMaxLevel) Color(0xFF1E293B) else if (canAffordUpgrade) Color(0xFF14532D) else Color(0xFF1E293B),
                        borderColor = if (isMaxLevel) Color(0xFF94A3B8) else if (canAffordUpgrade) Color(0xFF22C55E) else Color(0xFF475569),
                        enabled = !isMaxLevel && canAffordUpgrade,
                        onClick = onUpgrade,
                        testTag = "radial_action_upgrade"
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Action 2: Ability / Target Focus Button
                    val stratName = when (tower.targetingStrategy) {
                        TargetingStrategy.FIRST -> "FIRST"
                        TargetingStrategy.LAST -> "LAST"
                        TargetingStrategy.STRONGEST -> "STRONG"
                        TargetingStrategy.CLOSEST -> "CLOSE"
                    }
                    ContextualMenuAction(
                        icon = Icons.Default.MyLocation,
                        title = "ABILITY / FOCUS",
                        badge = stratName,
                        badgeColor = Color(0xFF7DD3FC),
                        bgColor = Color(0xFF0369A1),
                        borderColor = Color(0xFF38BDF8),
                        enabled = true,
                        onClick = { cycleStrategy() },
                        testTag = "radial_action_target"
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // Action 3: Sell Button
                    ContextualMenuAction(
                        icon = Icons.Default.MonetizationOn,
                        title = "SELL TOWER",
                        badge = "+${tower.sellRefundCoins}🪙",
                        badgeColor = Color(0xFFFDE68A),
                        bgColor = Color(0xFF7F1D1D),
                        borderColor = Color(0xFFEF4444),
                        enabled = true,
                        onClick = onSell,
                        testTag = "radial_action_sell"
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextualMenuAction(
    icon: ImageVector,
    title: String,
    badge: String,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = tween(50),
        label = "btn_press_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .scale(scale)
            .background(
                if (enabled) bgColor else Color(0xFF1E293B).copy(alpha = 0.6f),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (enabled) borderColor else Color(0xFF334155),
                RoundedCornerShape(6.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    audioPlayer.buttonClick()
                    onClick()
                }
            )
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.White else Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0x55000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (enabled) badgeColor else Color(0xFF64748B)
                )
            }
        }
    }
}
