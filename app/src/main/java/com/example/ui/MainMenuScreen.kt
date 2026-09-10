package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ProgressionManager

@Composable
fun MainMenuScreen(
    progressionManager: ProgressionManager,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by remember { mutableStateOf(false) }
    val totalStars = remember { progressionManager.getTotalStars() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Branding, Title, Stars
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Defense Fortress Badge
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(3.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Defense Logo",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Game Title
                Text(
                    text = "Tower Defense",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                // Version Subtitle
                Text(
                    text = "Frontline Fortress • 20 Waves",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Total Stars Earned Badge
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Stars",
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalStars / 9 Stars Earned",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Right Column: Action Buttons
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Play / Mission Select Button
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELECT MISSION",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Settings Button
                OutlinedButton(
                    onClick = onSettingsClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        )
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SETTINGS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // How To Play Button
                OutlinedButton(
                    onClick = { showHowToPlay = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("how_to_play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TACTICAL GUIDE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    if (showHowToPlay) {
        HowToPlayDialog(onDismiss = { showHowToPlay = false })
    }
}

@Composable
private fun HowToPlayDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "COMMANDER'S GUIDE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                RuleItem(
                    title = "3 Specialized Defense Towers",
                    desc = "Gunner: Rapid single fire. Cannon: Heavy splash damage. Blaster: Rapid energy pulses."
                )
                RuleItem(
                    title = "Tower Upgrades & Resale",
                    desc = "Tap any placed tower to inspect stats, upgrade to Mk.II/III, adjust targeting, or sell for a 70% refund."
                )
                RuleItem(
                    title = "Invader Archetypes & Bosses",
                    desc = "Face Scouts, Soldiers, Heavies, and agile Runners. Every 5 waves (5, 10, 15, 20), a massive Boss enters the battlefield!"
                )
                RuleItem(
                    title = "Victory & Stars",
                    desc = "Defend the Castle across 20 waves. Finish with 80%+ base HP to earn 3 Stars and unlock new maps!"
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "READY FOR COMBAT",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Text(
            text = "• $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = desc,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            lineHeight = 16.sp
        )
    }
}
