package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.audio.AndroidAudioPlayer
import com.example.game.GameStatus
import com.example.ui.components.GameButton
import com.example.ui.components.GameButtonVariant
import com.example.ui.components.GamePanel
import com.example.ui.components.GameStatRow

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }

    var sfxEnabled by remember { mutableStateOf(audioPlayer.isSfxEnabled) }
    var sfxVolume by remember { mutableStateOf(audioPlayer.sfxVolume) }

    var musicEnabled by remember { mutableStateOf(audioPlayer.isMusicEnabled) }
    var musicVolume by remember { mutableStateOf(audioPlayer.musicVolume) }

    var ambienceEnabled by remember { mutableStateOf(audioPlayer.isAmbienceEnabled) }
    var ambienceVolume by remember { mutableStateOf(audioPlayer.ambienceVolume) }

    Dialog(onDismissRequest = onDismiss) {
        GamePanel(
            headerTitle = "AUDIO SETTINGS",
            headerIcon = Icons.Default.Settings,
            borderColor = Color(0xFF38BDF8),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 480.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Sound Effects (SFX)
                AudioControlRow(
                    title = "Sound Effects (SFX)",
                    enabled = sfxEnabled,
                    onEnabledChange = {
                        sfxEnabled = it
                        audioPlayer.isSfxEnabled = it
                        if (it) audioPlayer.buttonClick()
                    },
                    volume = sfxVolume,
                    onVolumeChange = {
                        sfxVolume = it
                        audioPlayer.sfxVolume = it
                    },
                    icon = Icons.Default.VolumeUp,
                    iconTint = Color(0xFF38BDF8),
                    tagPrefix = "sfx"
                )

                // 2. Background Music
                AudioControlRow(
                    title = "Music",
                    enabled = musicEnabled,
                    onEnabledChange = {
                        musicEnabled = it
                        audioPlayer.isMusicEnabled = it
                    },
                    volume = musicVolume,
                    onVolumeChange = {
                        musicVolume = it
                        audioPlayer.musicVolume = it
                    },
                    icon = Icons.Default.VolumeUp,
                    iconTint = Color(0xFFFBBF24),
                    tagPrefix = "music"
                )

                // 3. Ambience
                AudioControlRow(
                    title = "Battlefield Ambience",
                    enabled = ambienceEnabled,
                    onEnabledChange = {
                        ambienceEnabled = it
                        audioPlayer.isAmbienceEnabled = it
                    },
                    volume = ambienceVolume,
                    onVolumeChange = {
                        ambienceVolume = it
                        audioPlayer.ambienceVolume = it
                    },
                    icon = Icons.Default.VolumeUp,
                    iconTint = Color(0xFF4ADE80),
                    tagPrefix = "ambience"
                )

                Spacer(modifier = Modifier.height(4.dp))

                GameButton(
                    text = "SAVE & CLOSE",
                    variant = GameButtonVariant.PRIMARY,
                    height = 42.dp,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "settings_done_button"
                )
            }
        }
    }
}

@Composable
private fun AudioControlRow(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    tagPrefix: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF090E17), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (enabled) iconTint else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) Color.White else Color(0xFF94A3B8)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = iconTint
                    ),
                    modifier = Modifier.testTag("${tagPrefix}_toggle")
                )
            }

            if (enabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = iconTint,
                            activeTrackColor = iconTint,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${tagPrefix}_volume_slider")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

/**
 * Professional mobile game tactical Pause dialog.
 * Darkens gameplay with subtle vignette and displays center tactical console.
 */
@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    var showEmbeddedSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GamePanel(
            headerTitle = "MISSION PAUSED",
            headerIcon = Icons.Default.Pause,
            borderColor = Color(0xFF38BDF8),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .fillMaxWidth(0.75f)
                .wrapContentHeight()
                .testTag("pause_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "TACTICAL OPERATIONS FROZEN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Resume Operation (Hero button)
                GameButton(
                    text = "RESUME OPERATION",
                    icon = Icons.Default.PlayArrow,
                    variant = GameButtonVariant.PRIMARY,
                    height = 46.dp,
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "resume_button"
                )

                // Restart Mission
                GameButton(
                    text = "RESTART MISSION",
                    icon = Icons.Default.Refresh,
                    variant = GameButtonVariant.SECONDARY,
                    height = 42.dp,
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pause_restart_button"
                )

                // Audio Settings
                GameButton(
                    text = "AUDIO OPTIONS",
                    icon = Icons.Default.Settings,
                    variant = GameButtonVariant.SECONDARY,
                    height = 42.dp,
                    onClick = { showEmbeddedSettings = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pause_settings_button"
                )

                // Exit to Campaign
                GameButton(
                    text = "ABORT TO CAMPAIGN",
                    icon = Icons.Default.Home,
                    variant = GameButtonVariant.DANGER,
                    height = 42.dp,
                    onClick = onMainMenu,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pause_menu_button"
                )
            }
        }
    }

    if (showEmbeddedSettings) {
        SettingsDialog(onDismiss = { showEmbeddedSettings = false })
    }
}

/**
 * Professional mobile game tactical Defeat / Game Over screen.
 * Darker military failure atmosphere with battle stats and tactical action buttons.
 */
@Composable
fun GameOverDialog(
    gameStatus: GameStatus,
    currentWave: Int,
    totalWaves: Int,
    coinsEarned: Int,
    enemiesKilled: Int,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    val audioPlayer = remember { AndroidAudioPlayer.getInstance() }
    val entranceScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        audioPlayer.defeat()
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Dramatic central failure panel
        Box(
            modifier = Modifier
                .scale(entranceScale.value)
                .widthIn(min = 380.dp, max = 620.dp)
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF220E12), Color(0xFF13090B), Color(0xFF0A0406))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFF87171).copy(alpha = 0.5f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left Column: Defeat Emblem, Title, Wave Reached
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0x66EF4444), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dangerous,
                                contentDescription = "Defeat",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "MISSION FAILED",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFEF4444)
                        )

                        Text(
                            text = "CITADEL DEFENSE COMPROMISED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Wave progress pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x66000000), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0x44EF4444), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HALTED AT WAVE ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$currentWave OF $totalWaves",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD166)
                            )
                        }
                    }

                    // Right Column: Battle statistics & retry options
                    Column(
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F0709), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2E1218), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GameStatRow("Wave Progress", "$currentWave / $totalWaves")
                            GameStatRow("Invaders Repelled", "$enemiesKilled", valueColor = Color(0xFFFFD166))
                            GameStatRow("Bounty Collected", "$coinsEarned 🪙", valueColor = Color(0xFF38BDF8))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Retry Mission Hero Button
                        GameButton(
                            text = "RETRY MISSION",
                            icon = Icons.Default.Refresh,
                            variant = GameButtonVariant.DANGER,
                            height = 42.dp,
                            onClick = onRestart,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "game_over_retry_button"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Return to Campaign
                        GameButton(
                            text = "TACTICAL RETREAT (HOME)",
                            icon = Icons.Default.Home,
                            variant = GameButtonVariant.SECONDARY,
                            height = 40.dp,
                            onClick = onMainMenu,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "game_over_menu_button"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirmation popup shown when player presses Android back button during gameplay.
 */
@Composable
fun ExitMissionDialog(
    onContinue: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1001f)
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GamePanel(
            headerTitle = "ABORT MISSION?",
            headerIcon = Icons.Default.Warning,
            borderColor = Color(0xFFF59E0B),
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .fillMaxWidth(0.75f)
                .wrapContentHeight()
                .testTag("exit_mission_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Current mission progress will be lost.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameButton(
                        text = "CONTINUE",
                        variant = GameButtonVariant.PRIMARY,
                        height = 42.dp,
                        onClick = onContinue,
                        modifier = Modifier.weight(1f),
                        testTag = "exit_confirm_continue_button"
                    )

                    GameButton(
                        text = "EXIT TO HOME",
                        variant = GameButtonVariant.DANGER,
                        height = 42.dp,
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                        testTag = "exit_confirm_exit_button"
                    )
                }
            }
        }
    }
}
