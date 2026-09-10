package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameConfig
import com.example.entities.Point2D
import com.example.entities.TowerSpec
import com.example.entities.TowerType
import kotlin.math.roundToInt

/**
 * Mobile Game Style Radial/Circular Tower Placement Selection Menu.
 * Complies with Requirement 2:
 * - Appears around or near the tapped empty buildable ground location
 * - Reticle/crosshair at the center build point
 * - Circular tower buttons:
 *       [Cannon] (Top)
 *   [Gunner] - + - [Rapid Fire]
 *       [Cancel] (Bottom)
 * - Highlights affordable towers; visually disables unaffordable ones
 * - Smooth spring-animated outward pop
 * - Clamped within screen edges so no button or label clips
 * - Compact and non-intrusive (no large rectangular bottom panel)
 */
@Composable
fun TowerBuildRadialMenu(
    buildPos: Point2D,
    screenX: Float,
    screenY: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    playerCoins: Int,
    onSelectTower: (TowerType) -> Unit,
    onClose: () -> Unit,
    highlightGunner: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    // Bouncy spring expansion outward from center
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "build_radial_expand"
    )

    // Radius distance for the radial options
    val radialDistanceDp = 76.dp
    val radialDistancePx = with(density) { radialDistanceDp.toPx() } * animProgress

    // Safe bounds clamping so buttons never render offscreen
    val marginPx = with(density) { 56.dp.toPx() }
    val clampedCenterX = screenX.coerceIn(marginPx, maxWidthPx - marginPx)
    val clampedCenterY = screenY.coerceIn(marginPx, maxHeightPx - marginPx)

    val cannonSpec = remember { TowerSpec.create(TowerType.CANNON, 1) }
    val gunnerSpec = remember { TowerSpec.create(TowerType.MACHINE_GUN, 1) }
    val rapidSpec = remember { TowerSpec.create(TowerType.RAPID_FIRE, 1) }

    Box(
        modifier = modifier.testTag("tower_build_radial_menu")
    ) {
        // 1. Center Reticle/Marker at exact build point
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (clampedCenterX - with(density) { 20.dp.toPx() }).roundToInt(),
                        (clampedCenterY - with(density) { 20.dp.toPx() }).roundToInt()
                    )
                }
                .size(40.dp)
                .background(Color(0x3338BDF8), CircleShape)
                .border(2.dp, Color(0xFF38BDF8), CircleShape)
                .testTag("build_reticle")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Build Location",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. CANNON (Top - 12:00)
        RadialBuildButton(
            label = "CANNON",
            tag = "CANNON",
            cost = cannonSpec.cost,
            playerCoins = playerCoins,
            accentGradient = listOf(Color(0xFFF97316), Color(0xFFC2410C)),
            centerX = clampedCenterX,
            centerY = clampedCenterY - radialDistancePx,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            onClick = { onSelectTower(TowerType.CANNON) }
        )

        // 3. GUNNER (Left - 9:00)
        RadialBuildButton(
            label = "GUNNER",
            tag = "GUNNER",
            cost = gunnerSpec.cost,
            playerCoins = playerCoins,
            accentGradient = listOf(Color(0xFF0284C7), Color(0xFF0369A1)),
            centerX = clampedCenterX - radialDistancePx,
            centerY = clampedCenterY,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            isHighlighted = highlightGunner,
            onClick = { onSelectTower(TowerType.MACHINE_GUN) }
        )

        // 4. RAPID FIRE (Right - 3:00)
        RadialBuildButton(
            label = "RAPID",
            tag = "RAPID_FIRE",
            cost = rapidSpec.cost,
            playerCoins = playerCoins,
            accentGradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            centerX = clampedCenterX + radialDistancePx,
            centerY = clampedCenterY,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            onClick = { onSelectTower(TowerType.RAPID_FIRE) }
        )

        // 5. CANCEL (Bottom - 6:00)
        RadialCloseButton(
            centerX = clampedCenterX,
            centerY = clampedCenterY + radialDistancePx * 0.85f,
            maxWidthPx = maxWidthPx,
            maxHeightPx = maxHeightPx,
            scale = animProgress,
            onClick = onClose
        )
    }
}

@Composable
private fun RadialBuildButton(
    label: String,
    tag: String,
    cost: Int,
    playerCoins: Int,
    accentGradient: List<Color>,
    centerX: Float,
    centerY: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    scale: Float,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val buttonRadiusDp = 26.dp
    val buttonRadiusPx = with(density) { buttonRadiusDp.toPx() }
    val margin = buttonRadiusPx + with(density) { 10.dp.toPx() }

    val clampedX = centerX.coerceIn(margin, maxWidthPx - margin)
    val clampedY = centerY.coerceIn(margin, maxHeightPx - margin)

    val canAfford = playerCoins >= cost
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset {
                IntOffset(
                    (clampedX - buttonRadiusPx).roundToInt(),
                    (clampedY - buttonRadiusPx).roundToInt()
                )
            }
            .scale(scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = canAfford,
                onClick = onClick
            )
        ) {
            if (isHighlighted) {
                Surface(
                    color = Color(0xFFFACC15),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "BUILD",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF78350F),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Circular Button
            Surface(
                shape = CircleShape,
                color = if (canAfford) Color(0xFF1E293B) else Color(0xFF0F172A),
                shadowElevation = if (canAfford) 8.dp else 2.dp,
                modifier = Modifier
                    .size(52.dp)
                    .border(
                        width = if (isHighlighted) 3.dp else if (canAfford) 2.dp else 1.dp,
                        brush = if (isHighlighted) Brush.linearGradient(listOf(Color(0xFFFACC15), Color(0xFFF59E0B)))
                        else if (canAfford) Brush.linearGradient(accentGradient)
                        else Brush.linearGradient(listOf(Color(0xFF475569), Color(0xFF334155))),
                        shape = CircleShape
                    )
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .testTag("build_button_$tag")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .background(
                            brush = if (canAfford) Brush.radialGradient(
                                listOf(accentGradient[0].copy(alpha = 0.45f), Color(0xFF0F172A))
                            ) else Brush.radialGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                        )
                        .alpha(if (canAfford) 1f else 0.45f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (canAfford) Color.White else Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${cost}🪙",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canAfford) Color(0xFFFFD166) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadialCloseButton(
    centerX: Float,
    centerY: Float,
    maxWidthPx: Float,
    maxHeightPx: Float,
    scale: Float,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val buttonRadiusDp = 18.dp
    val buttonRadiusPx = with(density) { buttonRadiusDp.toPx() }
    val margin = buttonRadiusPx + with(density) { 8.dp.toPx() }

    val clampedX = centerX.coerceIn(margin, maxWidthPx - margin)
    val clampedY = centerY.coerceIn(margin, maxHeightPx - margin)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset {
                IntOffset(
                    (clampedX - buttonRadiusPx).roundToInt(),
                    (clampedY - buttonRadiusPx).roundToInt()
                )
            }
            .scale(scale)
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xDD1E293B),
            modifier = Modifier
                .size(36.dp)
                .border(1.dp, Color(0xFF64748B), CircleShape)
                .clickable(onClick = onClick)
                .testTag("build_close_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Build",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
