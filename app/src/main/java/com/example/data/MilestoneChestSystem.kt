package com.example.data

enum class MilestoneType {
    STARS,
    LEVEL
}

data class MilestoneChest(
    val id: String,
    val title: String,
    val type: MilestoneType,
    val requiredValue: Int,
    val tokensReward: Int,
    val xpReward: Int,
    val badgeReward: String? = null,
    val description: String
)

object MilestoneChestCatalog {
    val allMilestones = listOf(
        MilestoneChest(
            id = "milestone_stars_3",
            title = "Bronze Supply Crate",
            type = MilestoneType.STARS,
            requiredValue = 3,
            tokensReward = 150,
            xpReward = 150,
            description = "Awarded for earning 3 Campaign Stars"
        ),
        MilestoneChest(
            id = "milestone_lvl_2",
            title = "Tactical Field Kit",
            type = MilestoneType.LEVEL,
            requiredValue = 2,
            tokensReward = 200,
            xpReward = 200,
            description = "Awarded for achieving Commander Level 2"
        ),
        MilestoneChest(
            id = "milestone_stars_6",
            title = "Iron Munitions Locker",
            type = MilestoneType.STARS,
            requiredValue = 6,
            tokensReward = 250,
            xpReward = 250,
            description = "Awarded for earning 6 Campaign Stars"
        ),
        MilestoneChest(
            id = "milestone_lvl_3",
            title = "Special Operations Crate",
            type = MilestoneType.LEVEL,
            requiredValue = 3,
            tokensReward = 300,
            xpReward = 300,
            description = "Awarded for achieving Commander Level 3"
        ),
        MilestoneChest(
            id = "milestone_stars_10",
            title = "Silver Citadel Cache",
            type = MilestoneType.STARS,
            requiredValue = 10,
            tokensReward = 350,
            xpReward = 350,
            description = "Awarded for earning 10 Campaign Stars"
        ),
        MilestoneChest(
            id = "milestone_lvl_5",
            title = "High Command War Chest",
            type = MilestoneType.LEVEL,
            requiredValue = 5,
            tokensReward = 500,
            xpReward = 500,
            badgeReward = "Apex Tactician",
            description = "Awarded for achieving Commander Level 5"
        ),
        MilestoneChest(
            id = "milestone_stars_15",
            title = "Gold Vanguard Vault",
            type = MilestoneType.STARS,
            requiredValue = 15,
            tokensReward = 500,
            xpReward = 500,
            badgeReward = "Star Marshal",
            description = "Awarded for earning 15 Campaign Stars"
        ),
        MilestoneChest(
            id = "milestone_stars_20",
            title = "Imperial Defense Vault",
            type = MilestoneType.STARS,
            requiredValue = 20,
            tokensReward = 750,
            xpReward = 750,
            badgeReward = "Hero of the Citadel",
            description = "Awarded for earning 20 Campaign Stars"
        )
    )

    fun getById(id: String): MilestoneChest? = allMilestones.find { it.id == id }
}
