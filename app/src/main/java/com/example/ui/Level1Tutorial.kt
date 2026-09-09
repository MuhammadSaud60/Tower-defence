package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProgressionManager
import com.example.entities.Point2D
import com.example.game.GameState
import kotlinx.coroutines.delay

val TUTORIAL_RECOMMENDED_PLOT = Point2D(340f, 400f)

enum class TutorialStep {
    TAP_PLOT,
    TAP_GUN_BUTTON,
    COMPLETED
}

/**
 * Lightweight Level 1 guided tutorial banner:
 * Teaches player how to select an empty plot and place their first Gunner tower.
 * Only shown on Level 1 (Green Valley) until completed or skipped.
 */
@Composable
fun Level1TutorialBanner(
    gameState: GameState,
    progressionManager: ProgressionManager,
    modifier: Modifier = Modifier
) {
    val isLevel1 = gameState.currentMap.id == "green_valley"
    val isAlreadyDone = progressionManager.isTutorialCompleted()

    if (!isLevel1 || isAlreadyDone) return

    var dismissed by remember { mutableStateOf(false) }
    if (dismissed) return

    val currentStep = when {
        gameState.towers.isNotEmpty() -> TutorialStep.COMPLETED
        gameState.selectedBuildPos != null -> TutorialStep.TAP_GUN_BUTTON
        else -> TutorialStep.TAP_PLOT
    }

    LaunchedEffect(currentStep) {
        if (currentStep == TutorialStep.COMPLETED) {
            progressionManager.setTutorialCompleted(true)
            delay(4000)
            dismissed = true
        }
    }

    AnimatedVisibility(
        visible = !dismissed,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("level_1_tutorial_banner")
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (currentStep == TutorialStep.COMPLETED) Color(0xFF22C55E) else Color(0xFF0284C7),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = when (currentStep) {
                                TutorialStep.COMPLETED -> Icons.Default.CheckCircle
                                TutorialStep.TAP_GUN_BUTTON -> Icons.Default.TouchApp
                                TutorialStep.TAP_PLOT -> Icons.Default.NearMe
                            },
                            contentDescription = "Tutorial Step",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = when (currentStep) {
                                TutorialStep.TAP_PLOT -> "TUTORIAL: PLACE YOUR FIRST DEFENSE"
                                TutorialStep.TAP_GUN_BUTTON -> "TUTORIAL: SELECT GUNNER"
                                TutorialStep.COMPLETED -> "TUTORIAL COMPLETE!"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentStep) {
                                TutorialStep.TAP_PLOT -> "Tap the glowing ground marker to choose a tower."
                                TutorialStep.TAP_GUN_BUTTON -> "Tap the Gunner icon in the radial menu to build it."
                                TutorialStep.COMPLETED -> "Great! Your gun will automatically attack enemies in range."
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (currentStep != TutorialStep.COMPLETED) {
                    Button(
                        onClick = {
                            progressionManager.setTutorialCompleted(true)
                            dismissed = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("tutorial_skip_button")
                    ) {
                        Text("SKIP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                } else {
                    IconButton(
                        onClick = {
                            progressionManager.setTutorialCompleted(true)
                            dismissed = true
                        },
                        modifier = Modifier.size(28.dp).testTag("tutorial_dismiss_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
