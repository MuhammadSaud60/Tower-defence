package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Professional mobile game developer splash screen for "MS Developers".
 * Displayed once upon game launch, automatically transitions to Main Menu.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    durationMillis: Long = 2300L
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    // Subtle breathing/pulse animation on the emblem
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    LaunchedEffect(Unit) {
        // Smooth entrance animation
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )

        // Hold display
        delay(durationMillis - 900L)

        // Smooth exit fade out
        alphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
        )
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF030712)
                    ),
                    radius = 900f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap to skip
                onSplashFinished()
            }
            .testTag("ms_developers_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // MS Developers Emblem
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x6638BDF8),
                                Color(0x2238BDF8),
                                Color.Transparent
                            ),
                            center = centerOffset,
                            radius = (size.width * 0.55f) * pulseGlow
                        ),
                        radius = size.width * 0.55f * pulseGlow,
                        center = centerOffset
                    )
                }

                // Geometric Crest with "M" and "S"
                Canvas(modifier = Modifier.size(90.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // Outer Hexagon Shield Border
                    val shieldPath = Path().apply {
                        moveTo(cx, 4f)
                        lineTo(w - 6f, h * 0.26f)
                        lineTo(w - 6f, h * 0.74f)
                        lineTo(cx, h - 4f)
                        lineTo(6f, h * 0.74f)
                        lineTo(6f, h * 0.26f)
                        close()
                    }

                    // Shield background fill
                    drawPath(
                        path = shieldPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0B132B))
                        )
                    )

                    // Shield border outline
                    drawPath(
                        path = shieldPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF38BDF8))
                        ),
                        style = Stroke(width = 3.5f, join = StrokeJoin.Round)
                    )

                    // Inner decorative geometric ring
                    drawCircle(
                        color = Color(0x3338BDF8),
                        radius = w * 0.32f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw Stylized "M" on left half
                    val mPath = Path().apply {
                        moveTo(cx - 26f, cy + 18f)
                        lineTo(cx - 26f, cy - 14f)
                        lineTo(cx - 13f, cy + 6f)
                        lineTo(cx - 3f, cy - 14f)
                        lineTo(cx - 3f, cy + 18f)
                    }
                    drawPath(
                        path = mPath,
                        color = Color(0xFF38BDF8),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw Stylized "S" on right half
                    val sPath = Path().apply {
                        moveTo(cx + 25f, cy - 12f)
                        lineTo(cx + 6f, cy - 12f)
                        lineTo(cx + 6f, cy + 2f)
                        lineTo(cx + 24f, cy + 2f)
                        lineTo(cx + 24f, cy + 18f)
                        lineTo(cx + 5f, cy + 18f)
                    }
                    drawPath(
                        path = sPath,
                        color = Color(0xFFFFD166),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Studio Title
            Text(
                text = "MS DEVELOPERS",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle / Studios Tagline
            Text(
                text = "G A M E   S T U D I O S",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = Color(0xFF38BDF8)
            )
        }
    }
}
