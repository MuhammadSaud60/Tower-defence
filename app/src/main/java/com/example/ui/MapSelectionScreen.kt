package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameMap
import com.example.data.ProgressionManager

@Composable
fun MapSelectionScreen(
    progressionManager: ProgressionManager,
    onSelectMap: (GameMap) -> Unit,
    onBackToMenu: () -> Unit
) {
    val maps = listOf(
        GameMap.createGreenValleyMap(
            isUnlocked = progressionManager.isMapUnlocked("green_valley"),
            stars = progressionManager.getStarsForMap("green_valley")
        ),
        GameMap.createDesertOutpostMap(
            isUnlocked = progressionManager.isMapUnlocked("desert_outpost"),
            stars = progressionManager.getStarsForMap("desert_outpost")
        ),
        GameMap.createForestPassMap(
            isUnlocked = progressionManager.isMapUnlocked("forest_pass"),
            stars = progressionManager.getStarsForMap("forest_pass")
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("map_selection_screen")
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1C2541), CircleShape)
                    .testTag("map_select_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "SELECT MISSION",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Choose your tactical war theater",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Map list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(maps) { map ->
                MapCard(
                    map = map,
                    onSelect = { if (map.isUnlocked) onSelectMap(map) }
                )
            }
        }
    }
}

@Composable
private fun MapCard(
    map: GameMap,
    onSelect: () -> Unit
) {
    val borderColor = if (map.isUnlocked) Color(0xFF38BDF8) else Color(0xFF334155)
    val cardBg = if (map.isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = map.isUnlocked, onClick = onSelect)
            .testTag("map_card_${map.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (map.isUnlocked) Color(0xFF0284C7) else Color(0xFF334155),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (map.isUnlocked) Icons.Default.Terrain else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = map.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (map.isUnlocked) Color.White else Color(0xFF64748B)
                        )
                        Text(
                            text = if (map.isUnlocked) "20 Waves • Normal" else "LOCKED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (map.isUnlocked) Color(0xFF22C55E) else Color(0xFFEF4444)
                        )
                    }
                }

                // Stars display for unlocked map
                if (map.isUnlocked) {
                    Row {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i <= map.starsEarned) Color(0xFFFFD166) else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = map.description,
                fontSize = 13.sp,
                color = if (map.isUnlocked) Color(0xFFCBD5E1) else Color(0xFF64748B),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (map.isUnlocked) {
                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEPLOY TO MISSION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Surface(
                    color = Color(0x33334155),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (map.id == "desert_outpost") "Complete Green Valley to unlock" else "Earn 3 Stars to unlock",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
