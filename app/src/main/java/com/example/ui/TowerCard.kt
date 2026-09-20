package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entities.TowerType

/**
 * Tactical Combat Weapon Card.
 * Designed for mobile strategy game interfaces with rich visual weapon renders,
 * cost badges, damage type icons, attack style descriptors, and clear locked states.
 */
@Composable
fun TowerCard(
    type: TowerType,
    cost: Int,
    playerCoins: Int,
    isLocked: Boolean,
    unlockRequirement: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = !isLocked && playerCoins >= cost

    // Spring bouncy lift on selection
    val liftOffset by animateDpAsState(
        targetValue = if (isSelected) (-8).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tower_card_lift"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        label = "card_glow_alpha"
    )

    val (damageIcon, damageColor, attackStyle) = remember(type) {
        when (type) {
            TowerType.MACHINE_GUN -> Triple("⚡", Color(0xFF38BDF8), "SINGLE")
            TowerType.CANNON -> Triple("💥", Color(0xFFF97316), "SPLASH")
            TowerType.RAPID_FIRE -> Triple("⚡", Color(0xFFFACC15), "STREAM")
            TowerType.FROST_GUN -> Triple("❄️", Color(0xFF38BDF8), "SLOW")
        }
    }

    val cardBorderColor = when {
        isSelected -> Color(0xFF38BDF8)
        isLocked -> Color(0x33475569)
        canAfford -> Color(0x6638BDF8)
        else -> Color(0x33EF4444)
    }

    val cardBackgroundBrush = remember(isSelected, isLocked, canAfford) {
        when {
            isSelected -> Brush.verticalGradient(
                listOf(Color(0xF00369A1), Color(0xF00F172A), Color(0xF0021226))
            )
            isLocked -> Brush.verticalGradient(
                listOf(Color(0xE01E293B), Color(0xE00F172A))
            )
            canAfford -> Brush.verticalGradient(
                listOf(Color(0xE61E293B), Color(0xE60F172A), Color(0xF0090D16))
            )
            else -> Brush.verticalGradient(
                listOf(Color(0xD01E293B), Color(0xD00F172A))
            )
        }
    }

    val displayName = remember(type) {
        when (type) {
            TowerType.MACHINE_GUN -> "GUNNER"
            TowerType.CANNON -> "CANNON"
            TowerType.RAPID_FIRE -> "RAPID"
            TowerType.FROST_GUN -> "CRYO"
        }
    }

    Box(
        modifier = modifier
            .offset(y = liftOffset)
            .width(88.dp)
            .height(68.dp)
            .testTag("tower_card_${type.name.lowercase()}")
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = if (isSelected) Color(0xFF38BDF8) else Color.Black,
                spotColor = if (isSelected) Color(0xFF38BDF8) else Color.Black
            )
            .clip(RoundedCornerShape(10.dp))
            .background(cardBackgroundBrush)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
    ) {
        // Active Selection Glowing Marker Top Notch
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(28.dp)
                    .height(3.dp)
                    .background(Color(0xFF38BDF8), RoundedCornerShape(2.dp))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP ROW: Damage Type Icon + Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Damage Type Mini Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(damageColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = damageIcon,
                        fontSize = 8.sp,
                        lineHeight = 9.sp
                    )
                    Spacer(modifier = Modifier.width(1.5.dp))
                    Text(
                        text = attackStyle,
                        color = damageColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 8.sp
                    )
                }

                // Cost Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (canAfford) Color(0x33F59E0B) else Color(0x33EF4444),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "$cost",
                        color = if (canAfford) Color(0xFFFFD166) else Color(0xFFFF6B6B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 10.sp
                    )
                    Text(
                        text = "🪙",
                        fontSize = 7.sp,
                        lineHeight = 8.sp,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }

            // CENTER: Rendered Weapon Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 1.dp)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    WeaponArtwork.drawWeapon(
                        drawScope = this,
                        type = type,
                        cx = cx,
                        cy = cy,
                        scale = 0.52f,
                        isLocked = isLocked,
                        recoilProgress = 0f,
                        animTime = 0f,
                        level = 1
                    )
                }
            }

            // BOTTOM ROW: Weapon Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    color = if (isLocked) Color(0xFF94A3B8) else if (isSelected) Color(0xFF38BDF8) else Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    lineHeight = 9.5.sp
                )
            }
        }

        // LOCKED OVERLAY
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC090D16)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = unlockRequirement,
                        color = Color(0xFFCBD5E1),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 8.5.sp
                    )
                }
            }
        }
    }
}
