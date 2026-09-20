package com.example.data

import com.example.entities.TowerType

/**
 * Lifetime combat statistics tracked across all game sessions.
 */
data class PlayerStats(
    val enemiesKilled: Long = 0L,
    val bossesKilled: Long = 0L,
    val towersPlaced: Long = 0L,
    val damageDealt: Long = 0L,
    val destructiblesDestroyed: Long = 0L,
    val missionsWon: Long = 0L,
    val coinsSpent: Long = 0L,
    val researchUpgradesPurchased: Long = 0L
)

/**
 * Result data returned when a player completes a mission.
 */
data class MissionRewardResult(
    val mapId: String,
    val starsEarned: Int,
    val xpGained: Int,
    val tokensGained: Int,
    val isFirstClear: Boolean,
    val levelUpInfo: LevelUpInfo?,
    val unlockedTowers: List<TowerType>,
    val newlyUnlockedAchievements: List<String>,
    val newlyAvailableChests: List<String>
) {
    val xpEarned: Int get() = xpGained
    val tokensEarned: Int get() = tokensGained
    val leveledUp: Boolean get() = levelUpInfo != null
    val newLevel: Int get() = levelUpInfo?.newLevel ?: 1
}

typealias ProgressionReward = MissionRewardResult

/**
 * Information when the player reaches a new account level.
 */
data class LevelUpInfo(
    val oldLevel: Int,
    val newLevel: Int,
    val rewardTokens: Int,
    val unlockedTower: TowerType? = null,
    val unlockedFeatureDescription: String? = null
)
