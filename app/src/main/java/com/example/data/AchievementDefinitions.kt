package com.example.data

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val targetGoal: Int,
    val rewardTokens: Int,
    val rewardXp: Int,
    val badgeTitle: String
)

enum class AchievementCategory(val displayName: String) {
    COMBAT("Combat Operations"),
    CAMPAIGN("Campaign Glory"),
    TACTICAL("Engineering & Science"),
    PROGRESSION("Command Ranks")
}

object AchievementCatalog {
    val allAchievements = listOf(
        Achievement(
            id = "first_victory",
            title = "First Blood",
            description = "Successfully complete your first defense mission.",
            category = AchievementCategory.CAMPAIGN,
            targetGoal = 1,
            rewardTokens = 100,
            rewardXp = 100,
            badgeTitle = "Recruit Defender"
        ),
        Achievement(
            id = "flawless_defense",
            title = "Iron Bastion",
            description = "Achieve a 3-star victory with 100% Citadel health remaining.",
            category = AchievementCategory.TACTICAL,
            targetGoal = 1,
            rewardTokens = 150,
            rewardXp = 150,
            badgeTitle = "Pristine Aegis"
        ),
        Achievement(
            id = "boss_hunter",
            title = "Goliath Slayer",
            description = "Defeat 5 giant boss invaders across combat missions.",
            category = AchievementCategory.COMBAT,
            targetGoal = 5,
            rewardTokens = 200,
            rewardXp = 250,
            badgeTitle = "Boss Hunter"
        ),
        Achievement(
            id = "destroyer",
            title = "Combat Sapper",
            description = "Demolish 20 natural obstacles (trees, boulders, relics) on the battlefield.",
            category = AchievementCategory.TACTICAL,
            targetGoal = 20,
            rewardTokens = 150,
            rewardXp = 150,
            badgeTitle = "Demolition Specialist"
        ),
        Achievement(
            id = "turret_architect",
            title = "Defense Grid",
            description = "Construct 30 total defensive turrets across missions.",
            category = AchievementCategory.TACTICAL,
            targetGoal = 30,
            rewardTokens = 200,
            rewardXp = 200,
            badgeTitle = "Turret Architect"
        ),
        Achievement(
            id = "arsenal_expansion",
            title = "Full Arsenal",
            description = "Unlock all 4 defensive tower archetypes.",
            category = AchievementCategory.PROGRESSION,
            targetGoal = 4,
            rewardTokens = 300,
            rewardXp = 300,
            badgeTitle = "Master Armorer"
        ),
        Achievement(
            id = "master_researcher",
            title = "Apex Science",
            description = "Purchase 5 permanent technology upgrades in the Research Lab.",
            category = AchievementCategory.PROGRESSION,
            targetGoal = 5,
            rewardTokens = 200,
            rewardXp = 200,
            badgeTitle = "Chief Scientist"
        ),
        Achievement(
            id = "star_general",
            title = "Constellation of Valor",
            description = "Earn 15 campaign stars across mission sectors.",
            category = AchievementCategory.CAMPAIGN,
            targetGoal = 15,
            rewardTokens = 350,
            rewardXp = 350,
            badgeTitle = "Star General"
        ),
        Achievement(
            id = "invader_cull",
            title = "Extinction Protocol",
            description = "Eliminate 300 hostile invaders.",
            category = AchievementCategory.COMBAT,
            targetGoal = 300,
            rewardTokens = 250,
            rewardXp = 250,
            badgeTitle = "Scourge of Invaders"
        ),
        Achievement(
            id = "commander_elite",
            title = "High Command",
            description = "Reach account Commander Level 5.",
            category = AchievementCategory.PROGRESSION,
            targetGoal = 5,
            rewardTokens = 300,
            rewardXp = 350,
            badgeTitle = "Grand Commander"
        )
    )

    fun getById(id: String): Achievement? = allAchievements.find { it.id == id }
}
