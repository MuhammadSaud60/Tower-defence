package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.GameConfig
import com.example.data.ProgressionManager
import com.example.entities.TowerType

/**
 * Mobile Strategy Game Weapon Selection Deck (Shop).
 * Docked cleanly at the bottom edge in Android landscape mode.
 * Presents rich visual weapon cards for all four towers with
 * clear costs, damage icons, and progression-locked states.
 */
@Composable
fun TowerShopDeck(
    playerCoins: Int,
    selectedType: TowerType?,
    progressionManager: ProgressionManager?,
    onSelectTower: (TowerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCannonLocked = progressionManager?.let { !it.isTowerUnlocked(TowerType.CANNON) } ?: false
    val isRapidLocked = progressionManager?.let { !it.isTowerUnlocked(TowerType.RAPID_FIRE) } ?: false
    val isFrostLocked = progressionManager?.let { !it.isTowerUnlocked(TowerType.FROST_GUN) } ?: false

    Box(
        modifier = modifier
            .testTag("tower_shop_deck")
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xD90B132B),
                        Color(0xF0070B14)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(Color(0x2238BDF8), Color(0x6638BDF8), Color(0x2238BDF8))
                ),
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. MACHINE GUN
            TowerCard(
                type = TowerType.MACHINE_GUN,
                cost = GameConfig.MG_TOWER_COST,
                playerCoins = playerCoins,
                isLocked = false,
                unlockRequirement = "",
                isSelected = selectedType == TowerType.MACHINE_GUN,
                onSelect = { onSelectTower(TowerType.MACHINE_GUN) }
            )

            // 2. HEAVY CANNON
            TowerCard(
                type = TowerType.CANNON,
                cost = GameConfig.CANNON_TOWER_COST,
                playerCoins = playerCoins,
                isLocked = isCannonLocked,
                unlockRequirement = "Lv. 2",
                isSelected = selectedType == TowerType.CANNON,
                onSelect = { onSelectTower(TowerType.CANNON) }
            )

            // 3. RAPID FIRE
            TowerCard(
                type = TowerType.RAPID_FIRE,
                cost = GameConfig.RAPID_TOWER_COST,
                playerCoins = playerCoins,
                isLocked = isRapidLocked,
                unlockRequirement = "Lv. 3",
                isSelected = selectedType == TowerType.RAPID_FIRE,
                onSelect = { onSelectTower(TowerType.RAPID_FIRE) }
            )

            // 4. FROST GUN
            TowerCard(
                type = TowerType.FROST_GUN,
                cost = GameConfig.FROST_TOWER_COST,
                playerCoins = playerCoins,
                isLocked = isFrostLocked,
                unlockRequirement = "Snow Lv. 5",
                isSelected = selectedType == TowerType.FROST_GUN,
                onSelect = { onSelectTower(TowerType.FROST_GUN) }
            )
        }
    }
}
