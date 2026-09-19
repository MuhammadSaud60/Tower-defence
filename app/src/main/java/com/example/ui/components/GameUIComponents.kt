package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AndroidAudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GameButtonVariant {
    PRIMARY,    // High-priority hero green/emerald or amber
    GOLD,       // Glorious victory gold
    SECONDARY,  // Tactical slate/iron
    DANGER,     // Combat crimson
    NAV         // Bottom bar / tab selector
}

/**
 * 3D Tactile Game Button featuring:
 * - Raised top face with lighting bevel
 * - Deep shadow lip giving physical game-cartridge depth
 * - Physical press depression animation (moves down by 3dp)
 * - Built-in tactile audio feedback on click
 */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GameButtonVariant = GameButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    subText: String? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    testTag: String = ""
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val pressOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val (topColor, bottomColor, shadowColor, borderColor, textColor) = when (variant) {
        GameButtonVariant.PRIMARY -> ButtonPalette(
            top = Color(0xFF22C55E),
            bottom = Color(0xFF15803D),
            shadow = Color(0xFF14532D),
            border = Color(0xFF4ADE80),
            text = Color.White
        )
        GameButtonVariant.GOLD -> ButtonPalette(
            top = Color(0xFFFBBF24),
            bottom = Color(0xFFD97706),
            shadow = Color(0xFF92400E),
            border = Color(0xFFFDE68A),
            text = Color(0xFF451A03)
        )
        GameButtonVariant.SECONDARY -> ButtonPalette(
            top = Color(0xFF334155),
            bottom = Color(0xFF1E293B),
            shadow = Color(0xFF0F172A),
            border = Color(0xFF64748B),
            text = Color(0xFFE2E8F0)
        )
        GameButtonVariant.DANGER -> ButtonPalette(
            top = Color(0xFFEF4444),
            bottom = Color(0xFFB91C1C),
            shadow = Color(0xFF7F1D1D),
            border = Color(0xFFF87171),
            text = Color.White
        )
        GameButtonVariant.NAV -> ButtonPalette(
            top = Color(0xFF1E293B),
            bottom = Color(0xFF0F172A),
            shadow = Color(0xFF020617),
            border = Color(0xFF38BDF8),
            text = Color(0xFF38BDF8)
        )
    }

    val finalTop = if (enabled) topColor else Color(0xFF475569)
    val finalBottom = if (enabled) bottomColor else Color(0xFF334155)
    val finalShadow = if (enabled) shadowColor else Color(0xFF1E293B)
    val finalBorder = if (enabled) borderColor else Color(0xFF64748B).copy(alpha = 0.4f)
    val finalTextColor = if (enabled) textColor else Color(0xFF94A3B8)

    val shadowDepth = 4.dp
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .height(height + shadowDepth)
            .testTag(testTag)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch { pressOffset.animateTo(3f, tween(50)) }
                        val released = tryAwaitRelease()
                        coroutineScope.launch { pressOffset.animateTo(0f, tween(100)) }
                        if (released) {
                            audioPlayer.buttonClick()
                            onClick()
                        }
                    }
                )
            }
    ) {
        // Base Drop Shadow / Bottom 3D Lip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = shadowDepth)
                .background(finalShadow, shape)
        )

        // Raised Top Face that depresses on touch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset { IntOffset(0, (pressOffset.value * density).toInt()) }
                .background(
                    brush = Brush.verticalGradient(listOf(finalTop, finalBottom)),
                    shape = shape
                )
                .border(1.5.dp, finalBorder, shape)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle top highlight sheen
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
            ) {
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(6f, 2f),
                    end = Offset(size.width - 6f, 2f),
                    strokeWidth = 2f
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = finalTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = text.uppercase(),
                        color = finalTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            color = finalTextColor.copy(alpha = 0.75f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

private data class ButtonPalette(
    val top: Color,
    val bottom: Color,
    val shadow: Color,
    val border: Color,
    val text: Color
)

/**
 * Tactical Game Panel Container.
 * Features beveled borders, subtle metallic sheen, and corner rivet accents.
 */
@Composable
fun GamePanel(
    modifier: Modifier = Modifier,
    borderColor: Color = Color(0xFF334155),
    headerTitle: String? = null,
    headerIcon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    val cornerRadius = 12.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(16.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A2639),
                        Color(0xFF0F172A),
                        Color(0xFF090D16)
                    )
                ),
                shape = shape
            )
            .border(2.dp, borderColor, shape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                ),
                shape = shape
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (headerTitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x33475569),
                            shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (headerIcon != null) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = headerTitle.uppercase(),
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Tactical Game Icon Button with 3D press feel and sound feedback.
 */
@Composable
fun GameIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = Color.White,
    backgroundColor: Color = Color(0xFF1E293B),
    borderColor: Color = Color(0xFF475569),
    size: Dp = 42.dp,
    testTag: String = ""
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val pressOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope.launch { pressOffset.animateTo(2f, tween(50)) }
                        val released = tryAwaitRelease()
                        coroutineScope.launch { pressOffset.animateTo(0f, tween(100)) }
                        if (released) {
                            audioPlayer.buttonClick()
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Drop lip
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .offset(y = 2.dp)
                .background(Color(0xFF090D16), shape)
        )

        // Face
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .offset { IntOffset(0, (pressOffset.value * density).toInt()) }
                .background(
                    brush = Brush.verticalGradient(
                        listOf(backgroundColor, backgroundColor.copy(alpha = 0.85f))
                    ),
                    shape = shape
                )
                .border(1.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

/**
 * Currency / Token Badge with game coin icon and high-contrast numerical readout.
 */
@Composable
fun GameCurrencyBadge(
    amount: Int,
    modifier: Modifier = Modifier,
    label: String = "GOLD",
    isCompact: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF1A1A0B), Color(0xFF0F172A))
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .border(1.dp, Color(0xFFCA8A04), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = if (isCompact) 4.dp else 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = label,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(if (isCompact) 15.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$amount",
                color = Color(0xFFFDE68A),
                fontSize = if (isCompact) 13.sp else 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Animated Stars Row for Mission Selection and Victory screens.
 * Stars pop in with sequential spring animations when [animated] is true.
 */
@Composable
fun GameStarsRow(
    starsEarned: Int,
    modifier: Modifier = Modifier,
    totalStars: Int = 3,
    starSize: Dp = 28.dp,
    animated: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..totalStars) {
            val isEarned = i <= starsEarned
            val scaleAnim = remember { Animatable(if (animated) 0f else 1f) }

            if (animated && isEarned) {
                LaunchedEffect(i) {
                    delay((i * 180).toLong())
                    scaleAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .size(if (i == 2 && totalStars == 3) starSize + 4.dp else starSize),
                contentAlignment = Alignment.Center
            ) {
                // Background socket
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(if (i == 2 && totalStars == 3) starSize + 4.dp else starSize)
                )
                // Foreground star
                if (isEarned) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $i",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(if (i == 2 && totalStars == 3) starSize + 4.dp else starSize)
                    )
                }
            }
        }
    }
}

/**
 * Crisp Stat Row for Victory, Defeat, and Tactical reports.
 */
@Composable
fun GameStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x33000000), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = valueColor,
            letterSpacing = 0.5.sp
        )
    }
}
