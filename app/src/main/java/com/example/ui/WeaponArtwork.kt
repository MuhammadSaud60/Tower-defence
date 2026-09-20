package com.example.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.entities.TowerType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Weapon Artwork & Attack Animation Renderer.
 * Provides realistic game-style 2D rendered artwork for Machine Gun,
 * Heavy Cannon, Rapid Fire, and Frost Gun with simulated firing recoil and attack effects.
 */
object WeaponArtwork {

    /**
     * Draws a detailed rendered weapon graphic centered at (cx, cy).
     * Supports ambient turntable rotation, recoil, and animated attack preview effects.
     */
    fun drawWeapon(
        drawScope: DrawScope,
        type: TowerType,
        cx: Float,
        cy: Float,
        scale: Float = 1f,
        isLocked: Boolean = false,
        recoilProgress: Float = 0f, // 0..1 for firing recoil kick
        animTime: Float = 0f,
        level: Int = 1
    ) {
        with(drawScope) {
            val recoilOffset = recoilProgress * 8f * scale

            when (type) {
                TowerType.MACHINE_GUN -> drawMachineGun(cx, cy, scale, isLocked, recoilOffset, animTime, level)
                TowerType.CANNON -> drawCannon(cx, cy, scale, isLocked, recoilOffset, animTime, level)
                TowerType.RAPID_FIRE -> drawRapidFire(cx, cy, scale, isLocked, animTime, level)
                TowerType.FROST_GUN -> drawFrostGun(cx, cy, scale, isLocked, animTime, level)
            }
        }
    }

    private fun DrawScope.drawMachineGun(
        cx: Float,
        cy: Float,
        scale: Float,
        isLocked: Boolean,
        recoilOffset: Float,
        animTime: Float,
        level: Int
    ) {
        val darkTint = if (isLocked) 0.35f else 1f
        val gunMetal = Color(0xFF1E293B).copy(alpha = darkTint)
        val steel = Color(0xFF475569).copy(alpha = darkTint)
        val oliveAmmo = if (isLocked) Color(0xFF1E293B) else Color(0xFF166534)
        val brass = if (isLocked) Color(0xFF475569) else Color(0xFFD97706)

        val barrelLen = (32f + level * 6f) * scale
        val barrelThick = (4.5f + level * 0.8f) * scale

        // 1. Olive Ammo Drum / Box
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = darkTint),
            topLeft = Offset(cx - 12f * scale, cy - 18f * scale),
            size = Size(14f * scale, 10f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRoundRect(
            color = oliveAmmo,
            topLeft = Offset(cx - 11f * scale, cy - 17f * scale),
            size = Size(12f * scale, 8f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Ammunition belt feed
        if (!isLocked) {
            for (b in 0..2) {
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(cx - 9f * scale + b * 3.5f * scale, cy - 11f * scale),
                    size = Size(2.2f * scale, 4f * scale)
                )
            }
        }

        // 2. Heavy Mantlet Shield
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = darkTint),
            topLeft = Offset(cx - 5f * scale - recoilOffset * 0.5f, cy - 16f * scale),
            size = Size(10f * scale, 32f * scale),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = steel,
            topLeft = Offset(cx - 4f * scale - recoilOffset * 0.5f, cy - 14f * scale),
            size = Size(8f * scale, 28f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // 3. Twin Rifled Barrels (with alternating reciprocating recoil)
        val topRecoil = recoilOffset * if (sin(animTime * 20f) > 0) 1f else 0.2f
        val botRecoil = recoilOffset * if (sin(animTime * 20f) <= 0) 1f else 0.2f

        // Top Barrel
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = darkTint),
            topLeft = Offset(cx + 2f * scale - topRecoil, cy - 8f * scale),
            size = Size(barrelLen, barrelThick),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = steel,
            topLeft = Offset(cx + 3f * scale - topRecoil, cy - 7.2f * scale),
            size = Size(barrelLen - 4f * scale, 1.8f * scale)
        )
        // Muzzle brake
        drawRect(
            color = Color(0xFFCBD5E1).copy(alpha = darkTint),
            topLeft = Offset(cx + barrelLen - 3f * scale - topRecoil, cy - 9f * scale),
            size = Size(4.5f * scale, 6f * scale)
        )

        // Bottom Barrel
        drawRoundRect(
            color = Color(0xFF0F172A).copy(alpha = darkTint),
            topLeft = Offset(cx + 2f * scale - botRecoil, cy + 3f * scale),
            size = Size(barrelLen, barrelThick),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = steel,
            topLeft = Offset(cx + 3f * scale - botRecoil, cy + 3.8f * scale),
            size = Size(barrelLen - 4f * scale, 1.8f * scale)
        )
        // Muzzle brake
        drawRect(
            color = Color(0xFFCBD5E1).copy(alpha = darkTint),
            topLeft = Offset(cx + barrelLen - 3f * scale - botRecoil, cy + 2f * scale),
            size = Size(4.5f * scale, 6f * scale)
        )

        // 4. Armored Gun Body / Receiver
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 16f * scale - recoilOffset, cy - 12f * scale),
            size = Size(20f * scale, 24f * scale),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(
            color = brass,
            radius = 3.5f * scale,
            center = Offset(cx - 7f * scale - recoilOffset, cy)
        )

        // 5. Muzzle Flash & Smoke Puff when firing
        if (!isLocked && recoilOffset > 1.5f) {
            val flashX = cx + barrelLen + 2f * scale
            // Fiery star flash
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 8f * scale,
                center = Offset(flashX, cy - 6f * scale)
            )
            drawCircle(
                color = Color.White,
                radius = 4f * scale,
                center = Offset(flashX, cy - 6f * scale)
            )
            // Smoke puff
            drawCircle(
                color = Color(0x6694A3B8),
                radius = 7f * scale,
                center = Offset(flashX + 6f * scale, cy - 8f * scale)
            )
        }
    }

    private fun DrawScope.drawCannon(
        cx: Float,
        cy: Float,
        scale: Float,
        isLocked: Boolean,
        recoilOffset: Float,
        animTime: Float,
        level: Int
    ) {
        val darkTint = if (isLocked) 0.35f else 1f
        val gunMetal = Color(0xFF0F172A).copy(alpha = darkTint)
        val armorSlate = Color(0xFF1E293B).copy(alpha = darkTint)
        val brass = if (isLocked) Color(0xFF475569) else Color(0xFFD97706)
        val hazardYellow = if (isLocked) Color(0xFF475569) else Color(0xFFF59E0B)

        val barrelLen = (38f + level * 8f) * scale
        val barrelThick = (18f + level * 4f) * scale

        // 1. Hydraulic Recoil Assist Pistons
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 8f * scale, cy - 18f * scale),
            size = Size(20f * scale, 5.5f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = Color(0xFFCBD5E1).copy(alpha = darkTint),
            topLeft = Offset(cx + 8f * scale - recoilOffset * 0.7f, cy - 16.5f * scale),
            size = Size(10f * scale, 2.5f * scale)
        )
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 8f * scale, cy + 12.5f * scale),
            size = Size(20f * scale, 5.5f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = Color(0xFFCBD5E1).copy(alpha = darkTint),
            topLeft = Offset(cx + 8f * scale - recoilOffset * 0.7f, cy + 14f * scale),
            size = Size(10f * scale, 2.5f * scale)
        )

        // 2. Blast Shield with hazard markings
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 14f * scale, cy - 22f * scale),
            size = Size(14f * scale, 44f * scale),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = Color(0xFF334155).copy(alpha = darkTint),
            topLeft = Offset(cx - 13f * scale, cy - 20f * scale),
            size = Size(12f * scale, 40f * scale),
            cornerRadius = CornerRadius(3f, 3f)
        )
        for (h in 0..2) {
            drawLine(
                color = hazardYellow,
                start = Offset(cx - 12f * scale, cy - 14f * scale + h * 9f * scale),
                end = Offset(cx - 3f * scale, cy - 7f * scale + h * 9f * scale),
                strokeWidth = 2f
            )
        }

        // 3. Massive Artillery Howitzer Barrel (with heavy kickback recoil)
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 4f * scale - recoilOffset, cy - barrelThick / 2f),
            size = Size(barrelLen, barrelThick),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawRoundRect(
            color = armorSlate,
            topLeft = Offset(cx - 2f * scale - recoilOffset, cy - (barrelThick - 3f * scale) / 2f),
            size = Size(barrelLen - 4f * scale, barrelThick - 3f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Steel rifling highlight
        drawRect(
            color = Color(0xFF475569).copy(alpha = darkTint),
            topLeft = Offset(cx - 2f * scale - recoilOffset, cy - 3f * scale),
            size = Size(barrelLen - 6f * scale, 2.5f * scale)
        )

        // 4. Brass Reinforcement Rings along barrel
        for (r in 0..1) {
            drawRoundRect(
                color = brass,
                topLeft = Offset(cx + 10f * scale + r * 12f * scale - recoilOffset, cy - (barrelThick + 3f * scale) / 2f),
                size = Size(4.5f * scale, barrelThick + 3f * scale),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }

        // 5. Flared Muzzle Ring & Dark Bore
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx + barrelLen - 4f * scale - recoilOffset, cy - (barrelThick + 5f * scale) / 2f),
            size = Size(8f * scale, barrelThick + 5f * scale),
            cornerRadius = CornerRadius(3f, 3f)
        )
        drawOval(
            color = Color.Black.copy(alpha = darkTint),
            topLeft = Offset(cx + barrelLen + 2f * scale - recoilOffset, cy - (barrelThick - 4f * scale) / 2f),
            size = Size(4f * scale, barrelThick - 4f * scale)
        )

        // 6. Breech Dome
        drawCircle(
            color = gunMetal,
            radius = 13f * scale,
            center = Offset(cx - 8f * scale - recoilOffset, cy)
        )
        drawCircle(
            color = Color(0xFF334155).copy(alpha = darkTint),
            radius = 10f * scale,
            center = Offset(cx - 8f * scale - recoilOffset, cy)
        )

        // 7. Cannon Blast Explosion Cone
        if (!isLocked && recoilOffset > 2.5f) {
            val blastX = cx + barrelLen + 6f * scale
            drawCircle(
                color = Color(0xFFEA580C),
                radius = 14f * scale,
                center = Offset(blastX, cy)
            )
            drawCircle(
                color = Color(0xFFFDE047),
                radius = 8f * scale,
                center = Offset(blastX - 2f * scale, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 4f * scale,
                center = Offset(blastX - 4f * scale, cy)
            )
        }
    }

    private fun DrawScope.drawRapidFire(
        cx: Float,
        cy: Float,
        scale: Float,
        isLocked: Boolean,
        animTime: Float,
        level: Int
    ) {
        val darkTint = if (isLocked) 0.35f else 1f
        val gunMetal = Color(0xFF0F172A).copy(alpha = darkTint)
        val cyanCore = if (isLocked) Color(0xFF334155) else Color(0xFF38BDF8)
        val brightCyan = if (isLocked) Color(0xFF475569) else Color(0xFF0284C7)

        val barrelLen = (34f + level * 6f) * scale

        // 1. Glowing Blue Energy Capacitor Core
        drawCircle(
            brush = Brush.radialGradient(listOf(cyanCore, brightCyan, Color(0xFF0F172A))),
            radius = 14f * scale,
            center = Offset(cx, cy)
        )

        // 2. Electromagnetic Accelerator Rails (Dual)
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx + 2f * scale, cy - 8f * scale),
            size = Size(barrelLen, 4.5f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = cyanCore,
            topLeft = Offset(cx + 4f * scale, cy - 7f * scale),
            size = Size(barrelLen - 5f * scale, 1.8f * scale)
        )

        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx + 2f * scale, cy + 3.5f * scale),
            size = Size(barrelLen, 4.5f * scale),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawRect(
            color = cyanCore,
            topLeft = Offset(cx + 4f * scale, cy + 4.5f * scale),
            size = Size(barrelLen - 5f * scale, 1.8f * scale)
        )

        // 3. Magnetic Coil Bands along rails
        for (c in 0..2) {
            drawRoundRect(
                color = brightCyan,
                topLeft = Offset(cx + 7f * scale + c * 8.5f * scale, cy - 10.5f * scale),
                size = Size(3.5f * scale, 21f * scale),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }

        // 4. Helical Power Drum behind core
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx - 14f * scale, cy - 12f * scale),
            size = Size(12f * scale, 24f * scale),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(
            color = if (isLocked) Color(0xFF334155) else Color(0xFF10B981),
            radius = 2.5f * scale,
            center = Offset(cx - 8f * scale, cy)
        )

        // 5. Center High-Voltage Arc Discharge
        drawCircle(
            color = Color.White.copy(alpha = if (isLocked) 0.2f else 0.9f),
            radius = 4f * scale,
            center = Offset(cx, cy)
        )

        // 6. Laser Ray / Plasma beam when active
        if (!isLocked) {
            val beamProgress = (sin(animTime * 15f) * 0.5f + 0.5f)
            val beamLen = 16f * scale + beamProgress * 12f * scale
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(cx + barrelLen, cy),
                end = Offset(cx + barrelLen + beamLen, cy),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(cx + barrelLen, cy),
                end = Offset(cx + barrelLen + beamLen * 0.8f, cy),
                strokeWidth = 1.5f * scale,
                cap = StrokeCap.Round
            )
        }
    }

    private fun DrawScope.drawFrostGun(
        cx: Float,
        cy: Float,
        scale: Float,
        isLocked: Boolean,
        animTime: Float,
        level: Int
    ) {
        val darkTint = if (isLocked) 0.35f else 1f
        val gunMetal = Color(0xFF0F172A).copy(alpha = darkTint)
        val frostCyan = if (isLocked) Color(0xFF334155) else Color(0xFF38BDF8)
        val iceWhite = if (isLocked) Color(0xFF475569) else Color(0xFFE0F2FE)

        val barrelLen = (30f + level * 6f) * scale

        // 1. Insulated Cryogenic Reservoir Base
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    if (isLocked) Color(0xFF334155) else Color(0xFFBAE6FD),
                    if (isLocked) Color(0xFF1E293B) else Color(0xFF0284C7),
                    Color(0xFF082F49)
                )
            ),
            radius = 15f * scale,
            center = Offset(cx, cy)
        )

        // 2. Cryo Dispersion Funnel Nozzle
        drawRoundRect(
            color = gunMetal,
            topLeft = Offset(cx + 2f * scale, cy - 10f * scale),
            size = Size(barrelLen, 20f * scale),
            cornerRadius = CornerRadius(5f, 5f)
        )
        drawRoundRect(
            color = iceWhite,
            topLeft = Offset(cx + 4f * scale, cy - 8.5f * scale),
            size = Size(barrelLen - 5f * scale, 17f * scale),
            cornerRadius = CornerRadius(3.5f, 3.5f)
        )

        // 3. Circulating Liquid Nitrogen Conduits
        drawLine(
            color = frostCyan,
            start = Offset(cx - 8f * scale, cy - 12f * scale),
            end = Offset(cx + barrelLen - 3f * scale, cy - 12f * scale),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = frostCyan,
            start = Offset(cx - 8f * scale, cy + 12f * scale),
            end = Offset(cx + barrelLen - 3f * scale, cy + 12f * scale),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )

        // 4. Center Frosted Core Dome
        drawCircle(
            color = Color.White.copy(alpha = if (isLocked) 0.2f else 0.85f),
            radius = 4f * scale,
            center = Offset(cx, cy)
        )

        // 5. Sub-Zero Ice Crystal Mist Particles
        if (!isLocked) {
            for (i in 0..3) {
                val ang = (i * PI / 2.0).toFloat() + animTime * 3f
                val dist = 10f * scale + sin(animTime * 4f + i) * 3f * scale
                val px = cx + barrelLen + 6f * scale + cos(ang) * dist
                val py = cy + sin(ang) * (dist * 0.7f)
                drawCircle(Color.White, radius = 2.2f * scale, center = Offset(px, py))
                drawCircle(Color(0xFFBAE6FD), radius = 1.2f * scale, center = Offset(px, py))
            }
        }
    }
}
