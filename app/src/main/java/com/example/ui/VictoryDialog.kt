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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.GameState

@Composable
fun VictoryDialog(
    gameState: GameState,
    onPlayAgain: () -> Unit,
    onMapSelection: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("victory_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Trophy icon
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Victory",
                    tint = Color(0xFFFFD166),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "VICTORY!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "${gameState.currentMap.name} Defended!",
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stars display
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..3) {
                        val isEarned = i <= gameState.starsEarned
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star $i",
                            tint = if (isEarned) Color(0xFFFFD166) else Color(0xFF475569),
                            modifier = Modifier
                                .size(if (i == 2) 46.dp else 36.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    StatRow("Waves Completed", "${gameState.currentWave}/${gameState.maxWaves}")
                    StatRow("Final Score", "${gameState.finalScore}")
                    StatRow("Invaders Defeated", "${gameState.enemiesKilledTotal}")
                    StatRow("Coins Earned", "${gameState.totalCoinsEarned} 🪙")
                    StatRow("Base HP Preserved", "${gameState.base.currentHp}/${gameState.base.maxHp}")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onMapSelection,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("victory_map_select_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Map Select", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Maps", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("victory_play_again_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Play Again", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Replay", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF94A3B8))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
