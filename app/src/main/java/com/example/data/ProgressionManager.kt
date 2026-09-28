package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.entities.TowerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tower_defense_progression", Context.MODE_PRIVATE)

    // =========================================================================
    // 1. CAMPAIGN MAP STARS & LEVEL UNLOCKS (BACKWARD COMPATIBLE)
    // =========================================================================

    fun getStarsForMap(mapId: String): Int {
        return prefs.getInt("stars_$mapId", 0)
    }

    fun saveStarsForMap(mapId: String, stars: Int) {
        val current = getStarsForMap(mapId)
        if (stars > current) {
            prefs.edit().putInt("stars_$mapId", stars).apply()
        }
    }

    fun isMapUnlocked(mapId: String): Boolean {
        if (mapId == "green_valley" || mapId == "map_1_valley") return true
        if (prefs.getBoolean("unlocked_$mapId", false)) return true

        val totalStars = getTotalStars()
        return when (mapId) {
            "desert_outpost", "map_2_canyon" -> getStarsForMap("green_valley") >= 1 || totalStars >= 1
            "forest_pass", "map_3_crossroads" -> getStarsForMap("desert_outpost") >= 1 || totalStars >= 3
            "split_routes", "map_4_split" -> getStarsForMap("forest_pass") >= 1 || totalStars >= 5
            "map_4_tunnel", "canyon_tunnel" -> getStarsForMap("split_routes") >= 1 || getStarsForMap("forest_pass") >= 1 || totalStars >= 7
            "the_crossing", "map_6_crossing" -> getStarsForMap("map_4_tunnel") >= 1 || getStarsForMap("canyon_tunnel") >= 1 || totalStars >= 9
            "map_5_loop", "dragons_coil" -> getStarsForMap("the_crossing") >= 1 || getStarsForMap("map_4_tunnel") >= 1 || totalStars >= 11
            "snow_outpost" -> getStarsForMap("map_5_loop") >= 1 || getStarsForMap("dragons_coil") >= 1 || totalStars >= 13
            "frozen_valley", "snow_valley", "map_8_snow" -> getStarsForMap("snow_outpost") >= 1 || getStarsForMap("map_5_loop") >= 1 || totalStars >= 13
            "ice_mountain" -> getStarsForMap("frozen_valley") >= 1 || getStarsForMap("snow_valley") >= 1 || totalStars >= 15
            "frozen_fortress" -> getStarsForMap("ice_mountain") >= 1 || totalStars >= 17
            "arctic_base" -> getStarsForMap("frozen_fortress") >= 1 || totalStars >= 19
            "night_fortress", "map_9_night" -> getStarsForMap("arctic_base") >= 1 || getStarsForMap("frozen_fortress") >= 1 || getStarsForMap("snow_valley") >= 1 || totalStars >= 21
            "eclipse_frontier", "solstice_frontier" -> getStarsForMap("night_fortress") >= 1 || getStarsForMap("arctic_base") >= 1 || totalStars >= 23
            "storm_twin_bastion", "tempest_bastion" -> getStarsForMap("eclipse_frontier") >= 1 || getStarsForMap("night_fortress") >= 1 || totalStars >= 25
            "cloudy_dense_forest", "cloudy_forest" -> getStarsForMap("storm_twin_bastion") >= 1 || getStarsForMap("tempest_bastion") >= 1 || totalStars >= 27
            "snow_summit_descent", "frostpeak_descent" -> getStarsForMap("cloudy_dense_forest") >= 1 || getStarsForMap("cloudy_forest") >= 1 || totalStars >= 29
            "desert_dune_bastion", "dune_storm_stronghold" -> getStarsForMap("snow_summit_descent") >= 1 || getStarsForMap("frostpeak_descent") >= 1 || totalStars >= 31
            "emerald_twin_pass", "emerald_serpent_pass" -> getStarsForMap("desert_dune_bastion") >= 1 || getStarsForMap("dune_storm_stronghold") >= 1 || totalStars >= 33
            "forest_ring_bastion", "sylvan_ring_sanctuary" -> getStarsForMap("emerald_twin_pass") >= 1 || getStarsForMap("emerald_serpent_pass") >= 1 || totalStars >= 35
            "frozen_pass" -> getStarsForMap("forest_ring_bastion") >= 1 || getStarsForMap("sylvan_ring_sanctuary") >= 1 || totalStars >= 37
            "obsidian_crossfire" -> getStarsForMap("frozen_pass") >= 1 || totalStars >= 39
            "tempest_ravine" -> getStarsForMap("obsidian_crossfire") >= 1 || totalStars >= 41
            "eclipse_citadel" -> getStarsForMap("tempest_ravine") >= 1 || totalStars >= 43
            "apex_dragon_sanctum" -> getStarsForMap("eclipse_citadel") >= 1 || totalStars >= 45
            else -> false
        }
    }

    fun isTutorialCompleted(): Boolean {
        return prefs.getBoolean("tutorial_completed", false)
    }

    fun setTutorialCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("tutorial_completed", completed).apply()
    }

    fun unlockMap(mapId: String) {
        prefs.edit().putBoolean("unlocked_$mapId", true).apply()
    }

    fun getBestWave(mapId: String): Int {
        return prefs.getInt("best_wave_$mapId", 1)
    }

    fun recordWaveReached(mapId: String, wave: Int) {
        val best = getBestWave(mapId)
        if (wave > best) {
            prefs.edit().putInt("best_wave_$mapId", wave).apply()
        }
    }

    fun getTotalStars(): Int {
        return getStarsForMap("green_valley") +
                getStarsForMap("desert_outpost") +
                getStarsForMap("forest_pass") +
                getStarsForMap("map_4_tunnel") +
                getStarsForMap("the_crossing") +
                getStarsForMap("map_5_loop") +
                getStarsForMap("snow_outpost") +
                getStarsForMap("frozen_valley") +
                getStarsForMap("snow_valley") +
                getStarsForMap("ice_mountain") +
                getStarsForMap("frozen_fortress") +
                getStarsForMap("arctic_base") +
                getStarsForMap("night_fortress") +
                getStarsForMap("eclipse_frontier") +
                maxOf(getStarsForMap("storm_twin_bastion"), getStarsForMap("tempest_bastion")) +
                maxOf(getStarsForMap("cloudy_dense_forest"), getStarsForMap("cloudy_forest")) +
                maxOf(getStarsForMap("snow_summit_descent"), getStarsForMap("frostpeak_descent")) +
                maxOf(getStarsForMap("desert_dune_bastion"), getStarsForMap("dune_storm_stronghold")) +
                maxOf(getStarsForMap("emerald_twin_pass"), getStarsForMap("emerald_serpent_pass")) +
                maxOf(getStarsForMap("forest_ring_bastion"), getStarsForMap("sylvan_ring_sanctuary")) +
                getStarsForMap("frozen_pass") +
                getStarsForMap("obsidian_crossfire") +
                getStarsForMap("tempest_ravine") +
                getStarsForMap("eclipse_citadel") +
                getStarsForMap("apex_dragon_sanctum")
    }

    // =========================================================================
    // 2. PLAYER LEVEL & XP CURVE
    // =========================================================================

    fun getPlayerLevel(): Int {
        return prefs.getInt("player_level", 1)
    }

    fun getCurrentXp(): Int {
        return prefs.getInt("player_xp", 0)
    }

    fun getXpForNextLevel(level: Int = getPlayerLevel()): Int {
        return when (level) {
            1 -> 250
            2 -> 500
            3 -> 900
            4 -> 1400
            else -> 1400 + ((level - 4) * 600)
        }
    }

    fun getXpProgressRatio(): Float {
        val current = getCurrentXp()
        val needed = getXpForNextLevel()
        return if (needed > 0) (current.toFloat() / needed).coerceIn(0f, 1f) else 1f
    }

    fun addXp(amount: Int): LevelUpInfo? {
        if (amount <= 0) return null

        var level = getPlayerLevel()
        var xp = getCurrentXp() + amount
        var leveledUp = false
        val oldLevel = level
        var totalTokensReward = 0
        var lastUnlockedTower: TowerType? = null

        while (true) {
            val needed = getXpForNextLevel(level)
            if (xp >= needed) {
                xp -= needed
                level++
                leveledUp = true
                val levelBonus = 150 + (level * 25)
                totalTokensReward += levelBonus
                addTokens(levelBonus)

                // Check automatic tower unlocks
                when (level) {
                    2 -> if (!isTowerUnlocked(TowerType.CANNON)) {
                        unlockTower(TowerType.CANNON)
                        lastUnlockedTower = TowerType.CANNON
                    }
                    3 -> if (!isTowerUnlocked(TowerType.RAPID_FIRE)) {
                        unlockTower(TowerType.RAPID_FIRE)
                        lastUnlockedTower = TowerType.RAPID_FIRE
                    }
                    4 -> if (!isTowerUnlocked(TowerType.FROST_GUN)) {
                        unlockTower(TowerType.FROST_GUN)
                        lastUnlockedTower = TowerType.FROST_GUN
                    }
                }
            } else {
                break
            }
        }

        prefs.edit()
            .putInt("player_level", level)
            .putInt("player_xp", xp)
            .apply()

        return if (leveledUp) {
            LevelUpInfo(
                oldLevel = oldLevel,
                newLevel = level,
                rewardTokens = totalTokensReward,
                unlockedTower = lastUnlockedTower,
                unlockedFeatureDescription = when (level) {
                    2 -> "Heavy Cannon unlocked & Tier 2 Research available!"
                    3 -> "Rapid Fire Turret unlocked!"
                    4 -> "Frost Gun Cryo Turret unlocked!"
                    else -> "Advanced Research nodes unlocked!"
                }
            )
        } else null
    }

    fun getPlayerTitle(level: Int = getPlayerLevel()): String {
        return when {
            level >= 20 -> "Supreme Commander"
            level >= 15 -> "Field Marshal"
            level >= 10 -> "Brigadier General"
            level >= 8 -> "Colonel"
            level >= 6 -> "Major"
            level >= 4 -> "Captain"
            level >= 2 -> "Lieutenant"
            else -> "Cadet"
        }
    }

    fun getAvailableMilestoneChests(): List<MilestoneChest> = getAvailableUnclaimedChests()

    fun getClaimableAchievements(): List<Achievement> =
        AchievementCatalog.allAchievements.filter { isAchievementUnlocked(it.id) && !isAchievementClaimed(it.id) }

    fun recordCombatStats(
        enemiesKilled: Int,
        bossesKilled: Int,
        destructiblesCleared: Int,
        towersPlaced: Int,
        damageDealt: Long
    ) {
        recordStat("enemies_killed", enemiesKilled.toLong())
        recordStat("bosses_killed", bossesKilled.toLong())
        recordStat("destructibles_destroyed", destructiblesCleared.toLong())
        recordStat("towers_placed", towersPlaced.toLong())
        recordStat("damage_dealt", damageDealt)
    }

    // =========================================================================
    // 3. TOKENS ECONOMY
    // =========================================================================

    fun getTokens(): Int {
        return prefs.getInt("player_tokens", 200) // 200 starting tokens for immediate progression test
    }

    fun addTokens(amount: Int) {
        if (amount > 0) {
            val updated = getTokens() + amount
            prefs.edit().putInt("player_tokens", updated).apply()
        }
    }

    fun spendTokens(amount: Int): Boolean {
        val current = getTokens()
        if (amount in 1..current) {
            prefs.edit().putInt("player_tokens", current - amount).apply()
            recordStat("coins_spent", amount.toLong())
            return true
        }
        return false
    }

    // =========================================================================
    // 4. TOWER UNLOCK SYSTEM
    // =========================================================================

    fun isTowerUnlocked(type: TowerType): Boolean {
        return when (type) {
            TowerType.MACHINE_GUN -> true // Starting tower
            TowerType.CANNON -> {
                prefs.getBoolean("unlocked_tower_CANNON", false) ||
                        getPlayerLevel() >= 2 ||
                        getTotalStars() >= 2
            }
            TowerType.RAPID_FIRE -> {
                prefs.getBoolean("unlocked_tower_RAPID_FIRE", false) ||
                        getPlayerLevel() >= 3 ||
                        getTotalStars() >= 5
            }
            TowerType.FROST_GUN -> {
                prefs.getBoolean("unlocked_tower_FROST_GUN", false) ||
                        getPlayerLevel() >= 4 ||
                        getTotalStars() >= 8
            }
        }
    }

    fun unlockTower(type: TowerType) {
        prefs.edit().putBoolean("unlocked_tower_${type.name}", true).apply()
    }

    fun getTowerUnlockRequirement(type: TowerType): String {
        return when (type) {
            TowerType.MACHINE_GUN -> "Unlocked by Default"
            TowerType.CANNON -> "Reach Level 2 or 2 Stars"
            TowerType.RAPID_FIRE -> "Reach Level 3 or 5 Stars"
            TowerType.FROST_GUN -> "Reach Level 4 or 8 Stars"
        }
    }

    fun getUnlockedTowers(): List<TowerType> {
        return TowerType.values().filter { isTowerUnlocked(it) }
    }

    // =========================================================================
    // 5. PERMANENT RESEARCH LAB / UPGRADE TREE
    // =========================================================================

    fun getResearchRank(nodeId: String): Int {
        return prefs.getInt("research_$nodeId", 0)
    }

    fun canUpgradeResearch(node: ResearchNode): Boolean {
        val currentRank = getResearchRank(node.id)
        if (currentRank >= node.maxRank) return false
        val cost = node.getCostForRank(currentRank)
        if (getTokens() < cost) return false

        // Check prerequisite
        if (node.prerequisiteId != null) {
            val prereqRank = getResearchRank(node.prerequisiteId)
            if (prereqRank < node.prerequisiteRank) return false
        }
        return true
    }

    fun upgradeResearch(node: ResearchNode): Boolean {
        if (!canUpgradeResearch(node)) return false

        val currentRank = getResearchRank(node.id)
        val cost = node.getCostForRank(currentRank)
        if (spendTokens(cost)) {
            prefs.edit().putInt("research_${node.id}", currentRank + 1).apply()
            recordStat("research_upgrades_bought", 1L)
            return true
        }
        return false
    }

    // Research Multipliers applied to game specs
    fun getTowerDamageMultiplier(type: TowerType): Float {
        val rank = when (type) {
            TowerType.MACHINE_GUN -> getResearchRank("mg_damage")
            TowerType.CANNON -> getResearchRank("cannon_damage")
            TowerType.RAPID_FIRE -> getResearchRank("rapid_drum")
            TowerType.FROST_GUN -> getResearchRank("frost_shards")
        }
        return 1f + (rank * 0.05f)
    }

    fun getTowerFireRateMultiplier(type: TowerType): Float {
        val rank = when (type) {
            TowerType.MACHINE_GUN -> getResearchRank("mg_fire_rate")
            TowerType.CANNON -> getResearchRank("cannon_reload")
            TowerType.RAPID_FIRE -> getResearchRank("rapid_speed")
            TowerType.FROST_GUN -> 0
        }
        return 1f + (rank * 0.05f)
    }

    fun getTowerRangeMultiplier(type: TowerType): Float {
        val rank = when (type) {
            TowerType.MACHINE_GUN -> getResearchRank("mg_range")
            TowerType.CANNON -> 0
            TowerType.RAPID_FIRE -> 0
            TowerType.FROST_GUN -> getResearchRank("frost_range")
        }
        return 1f + (rank * 0.03f)
    }

    fun getTowerSplashMultiplier(type: TowerType): Float {
        return if (type == TowerType.CANNON) {
            1f + (getResearchRank("cannon_radius") * 0.05f)
        } else 1f
    }

    fun getTowerArmorPiercingBonus(type: TowerType): Float {
        return if (type == TowerType.MACHINE_GUN) {
            getResearchRank("mg_ap") * 0.04f
        } else 0f
    }

    fun getBaseHpBonus(): Float {
        return getResearchRank("citadel_armor") * 0.10f
    }

    fun getStartingGoldBonus(): Int {
        return getResearchRank("citadel_gold") * 15
    }

    // =========================================================================
    // 6. MISSION REWARDS & VICTORY PROCESSING
    // =========================================================================

    fun recordBattleVictory(
        mapId: String,
        starsEarned: Int,
        enemiesKilled: Int,
        bossesKilled: Int,
        finalBaseHpPercent: Float,
        destructiblesDestroyed: Int,
        towersPlaced: Int,
        damageDealt: Long
    ): MissionRewardResult {
        val previousStars = getStarsForMap(mapId)
        val isFirstClear = previousStars == 0
        saveStarsForMap(mapId, starsEarned)

        // Calculate Rewards
        var xpGained = 150
        var tokensGained = 80

        if (starsEarned >= 2) {
            xpGained += 50
            tokensGained += 30
        }
        if (starsEarned == 3) {
            xpGained += 100
            tokensGained += 50
        }
        if (bossesKilled > 0) {
            xpGained += bossesKilled * 50
            tokensGained += bossesKilled * 25
        }
        if (isFirstClear) {
            xpGained += 100
            tokensGained += 60
        }

        // Apply XP & Tokens
        addTokens(tokensGained)
        val levelUpInfo = addXp(xpGained)

        // Update Lifetime Combat Stats
        recordStat("enemies_killed", enemiesKilled.toLong())
        recordStat("bosses_killed", bossesKilled.toLong())
        recordStat("destructibles_destroyed", destructiblesDestroyed.toLong())
        recordStat("towers_placed", towersPlaced.toLong())
        recordStat("damage_dealt", damageDealt)
        recordStat("missions_won", 1L)

        // Check Newly Unlocked Towers
        val unlockedTowersList = mutableListOf<TowerType>()
        for (tower in TowerType.values()) {
            if (isTowerUnlocked(tower) && !prefs.getBoolean("notified_unlock_${tower.name}", false)) {
                unlockedTowersList.add(tower)
                prefs.edit().putBoolean("notified_unlock_${tower.name}", true).apply()
            }
        }

        // Check Newly Available Milestone Chests
        val newlyAvailableChests = getAvailableUnclaimedChests().map { it.title }

        return MissionRewardResult(
            mapId = mapId,
            starsEarned = starsEarned,
            xpGained = xpGained,
            tokensGained = tokensGained,
            isFirstClear = isFirstClear,
            levelUpInfo = levelUpInfo,
            unlockedTowers = unlockedTowersList,
            newlyUnlockedAchievements = emptyList(),
            newlyAvailableChests = newlyAvailableChests
        )
    }

    // =========================================================================
    // 7. LIFETIME STATISTICS
    // =========================================================================

    fun getStat(key: String): Long {
        return prefs.getLong("stat_$key", 0L)
    }

    fun recordStat(key: String, delta: Long) {
        if (delta != 0L) {
            val current = getStat(key)
            prefs.edit().putLong("stat_$key", current + delta).apply()
        }
    }

    fun getPlayerStats(): PlayerStats {
        return PlayerStats(
            enemiesKilled = getStat("enemies_killed"),
            bossesKilled = getStat("bosses_killed"),
            towersPlaced = getStat("towers_placed"),
            damageDealt = getStat("damage_dealt"),
            destructiblesDestroyed = getStat("destructibles_destroyed"),
            missionsWon = getStat("missions_won"),
            coinsSpent = getStat("coins_spent"),
            researchUpgradesPurchased = getStat("research_upgrades_bought")
        )
    }

    // =========================================================================
    // 8. ACHIEVEMENTS SYSTEM
    // =========================================================================

    fun getAchievementProgress(achievementId: String): Int {
        return when (achievementId) {
            "first_victory" -> getStat("missions_won").toInt().coerceAtMost(1)
            "flawless_defense" -> if (prefs.getBoolean("has_flawless_victory", false)) 1 else 0
            "boss_hunter" -> getStat("bosses_killed").toInt()
            "destroyer" -> getStat("destructibles_destroyed").toInt()
            "turret_architect" -> getStat("towers_placed").toInt()
            "arsenal_expansion" -> getUnlockedTowers().size
            "master_researcher" -> getStat("research_upgrades_bought").toInt()
            "star_general" -> getTotalStars()
            "invader_cull" -> getStat("enemies_killed").toInt()
            "commander_elite" -> getPlayerLevel()
            else -> 0
        }
    }

    fun isAchievementUnlocked(achievementId: String): Boolean {
        val achievement = AchievementCatalog.getById(achievementId) ?: return false
        return getAchievementProgress(achievementId) >= achievement.targetGoal
    }

    fun isAchievementClaimed(achievementId: String): Boolean {
        return prefs.getBoolean("achievement_claimed_$achievementId", false)
    }

    fun claimAchievement(achievementId: String): Pair<Int, Int>? {
        if (isAchievementUnlocked(achievementId) && !isAchievementClaimed(achievementId)) {
            val achievement = AchievementCatalog.getById(achievementId) ?: return null
            prefs.edit().putBoolean("achievement_claimed_$achievementId", true).apply()
            addTokens(achievement.rewardTokens)
            addXp(achievement.rewardXp)
            return achievement.rewardTokens to achievement.rewardXp
        }
        return null
    }

    fun hasUnclaimedAchievements(): Boolean {
        return AchievementCatalog.allAchievements.any {
            isAchievementUnlocked(it.id) && !isAchievementClaimed(it.id)
        }
    }

    // =========================================================================
    // 9. DAILY REWARD SYSTEM
    // =========================================================================

    private val DAILY_REWARD_COOLDOWN_MS = 24L * 3600L * 1000L // 24 hours

    fun canClaimDailyReward(): Boolean {
        val lastClaimMs = prefs.getLong("last_daily_claim_timestamp", 0L)
        if (lastClaimMs == 0L) return true
        val elapsed = System.currentTimeMillis() - lastClaimMs
        return elapsed >= DAILY_REWARD_COOLDOWN_MS
    }

    fun getTimeUntilNextDailyRewardMs(): Long {
        val lastClaimMs = prefs.getLong("last_daily_claim_timestamp", 0L)
        if (lastClaimMs == 0L) return 0L
        val elapsed = System.currentTimeMillis() - lastClaimMs
        return (DAILY_REWARD_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    fun formatTimeUntilNextDailyReward(): String {
        val remainingMs = getTimeUntilNextDailyRewardMs()
        if (remainingMs <= 0L) return "READY NOW"
        val totalMinutes = remainingMs / (60L * 1000L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun getCurrentStreakDay(): Int {
        return prefs.getInt("daily_streak_day", 1).coerceIn(1, 7)
    }

    fun claimDailyReward(): Pair<DailyRewardTier, String?>? {
        if (!canClaimDailyReward()) return null

        val now = System.currentTimeMillis()
        val currentStreak = getCurrentStreakDay()
        val tier = DailyRewardCatalog.getTierForDay(currentStreak)

        if (tier.tokens > 0) addTokens(tier.tokens)
        if (tier.xp > 0) addXp(tier.xp)

        // Advance streak for next day (cycles after 7)
        val nextStreak = if (currentStreak >= 7) 1 else currentStreak + 1

        prefs.edit()
            .putLong("last_daily_claim_timestamp", now)
            .putInt("daily_streak_day", nextStreak)
            .apply()

        return tier to tier.specialBadge
    }

    // =========================================================================
    // 10. MILESTONE REWARD CHESTS
    // =========================================================================

    fun isMilestoneChestClaimed(chestId: String): Boolean {
        return prefs.getBoolean("chest_claimed_$chestId", false)
    }

    fun isMilestoneChestAvailable(chest: MilestoneChest): Boolean {
        if (isMilestoneChestClaimed(chest.id)) return false
        return when (chest.type) {
            MilestoneType.STARS -> getTotalStars() >= chest.requiredValue
            MilestoneType.LEVEL -> getPlayerLevel() >= chest.requiredValue
        }
    }

    fun getAvailableUnclaimedChests(): List<MilestoneChest> {
        return MilestoneChestCatalog.allMilestones.filter { isMilestoneChestAvailable(it) }
    }

    fun claimMilestoneChest(chestId: String): MilestoneChest? {
        val chest = MilestoneChestCatalog.getById(chestId) ?: return null
        if (isMilestoneChestAvailable(chest)) {
            prefs.edit().putBoolean("chest_claimed_$chestId", true).apply()
            addTokens(chest.tokensReward)
            addXp(chest.xpReward)
            return chest
        }
        return null
    }
}
