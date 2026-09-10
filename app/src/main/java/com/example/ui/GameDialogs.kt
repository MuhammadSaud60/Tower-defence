package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import com.example.audio.AndroidAudioPlayer
import com.example.game.GameStatus

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var soundEnabled by remember { mutableStateOf(AndroidAudioPlayer.isSoundEnabled()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SETTINGS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sound",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sound Effects",
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            AndroidAudioPlayer.setSoundEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.testTag("sound_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("settings_done_button")
                ) {
                    Text(
                        text = "DONE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESUME", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESTART", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_menu_button")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "MAIN MENU", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

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
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentVeryDissatisfied,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "DEFEAT",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )

                Text(
                    text = "Base was destroyed!",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Wave Reached", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Text(text = "$currentWave / $totalWaves", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Enemies Defeated", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Text(text = "$enemiesKilled", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166))
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Gold Earned", fontSize = 13.sp, color = Color(0xFF94A3B8))
                        Text(text = "$coinsEarned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("game_over_retry_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "TRY AGAIN", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("game_over_menu_button")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "MAIN MENU", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}
