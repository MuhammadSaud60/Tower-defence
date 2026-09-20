package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entities.TowerType
import kotlin.math.sin

/**
 * Tactical Tower Preview Panel.
 * Displays when a tower is selected, showcasing:
 * - 3D/illustrated tower render with subtle movement
 * - Live firing / attack animation preview
 * - Short combat role description
 * - Star rating combat stats (Damage, Range, Speed)
 * - Map placement prompt
 */
@Composable
fun TowerPreviewPanel(
    type: TowerType,
    cost: Int,
    playerCoins: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "preview_anim")

    // Ambient floating/breathing movement
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_float"
    )

    // Simulated attack firing loop (recoil & muzzle flash)
    val fireLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                when (type) {
                    TowerType.RAPID_FIRE -> 400
                    TowerType.MACHINE_GUN -> 650
                    TowerType.CANNON -> 1400
                    TowerType.FROST_GUN -> 1000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_fire_loop"
    )

    // Calculate simulated recoil kick (spike then decay)
    val recoilKick = if (fireLoop < 0.25f) (fireLoop / 0.25f) else (1f - (fireLoop - 0.25f) / 0.75f).coerceIn(0f, 1f)

    val (title, roleDescription, dmgStars, rngStars, spdStars) = remember(type) {
        when (type) {
            TowerType.MACHINE_GUN -> Pentuple(
                "MACHINE GUN",
                "Fast single target damage",
                4, 3, 5
            )
            TowerType.CANNON -> Pentuple(
                "HEAVY CANNON",
                "Massive explosive splash artillery",
                5, 4, 2
            )
            TowerType.RAPID_FIRE -> Pentuple(
                "RAPID FIRE",
                "High-velocity energy beam",
                3, 4, 5
            )
            TowerType.FROST_GUN -> Pentuple(
                "FROST GUN",
                "Cryo blast slows enemy advance",
                2, 3, 3
            )
        }
    }

    val canAfford = playerCoins >= cost

    Box(
        modifier = modifier
            .width(264.dp)
            .height(112.dp)
            .testTag("tower_preview_panel")
            .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Color(0xFF0284C7), spotColor = Color(0xFF38BDF8))
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFA0F172A), Color(0xFA090D16), Color(0xFD020617))
                )
            )
            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Animated Weapon Hologram Stage
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x88021429))
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f + sin(floatPhase) * 2.5f

                    // Turntable pedestal ring
                    drawOval(
                        color = Color(0x3338BDF8),
                        topLeft = Offset(cx - 24f, cy + 18f),
                        size = androidx.compose.ui.geometry.Size(48f, 14f)
                    )
                    drawOval(
                        color = Color(0xFF38BDF8),
                        topLeft = Offset(cx - 24f, cy + 18f),
                        size = androidx.compose.ui.geometry.Size(48f, 14f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw the animated weapon
                    WeaponArtwork.drawWeapon(
                        drawScope = this,
                        type = type,
                        cx = cx,
                        cy = cy,
                        scale = 0.68f,
                        isLocked = false,
                        recoilProgress = recoilKick,
                        animTime = floatPhase * 2f,
                        level = 1
                    )
                }
            }

            // RIGHT: Tactical Stats & Information
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Name + Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 12.sp
                        )
                        Text(
                            text = roleDescription,
                            color = Color(0xFF94A3B8),
                            fontSize = 7.5.sp,
                            maxLines = 1,
                            lineHeight = 8.5.sp
                        )
                    }

                    // Close / Cancel Button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0x33475569), CircleShape)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Placement",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Stats Ratings
                Column(verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    StatStarRow(label = "DMG", stars = dmgStars, activeColor = Color(0xFFF97316))
                    StatStarRow(label = "RNG", stars = rngStars, activeColor = Color(0xFF38BDF8))
                    StatStarRow(label = "SPD", stars = spdStars, activeColor = Color(0xFFFACC15))
                }

                // Deployment Prompt Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (canAfford) Color(0x3322C55E) else Color(0x33EF4444),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canAfford) "TAP MAP TO DEPLOY" else "NOT ENOUGH COINS",
                        color = if (canAfford) Color(0xFF4ADE80) else Color(0xFFFF6B6B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 9.sp
                    )
                    Text(
                        text = "$cost 🪙",
                        color = Color(0xFFFFD166),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 9.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatStarRow(
    label: String,
    stars: Int,
    activeColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
            for (i in 1..5) {
                Text(
                    text = if (i <= stars) "★" else "☆",
                    color = if (i <= stars) activeColor else Color(0xFF334155),
                    fontSize = 8.sp,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

private data class Pentuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
