package com.example.data

data class DailyRewardTier(
    val day: Int,
    val title: String,
    val tokens: Int,
    val xp: Int,
    val specialBadge: String? = null,
    val description: String
)

object DailyRewardCatalog {
    val schedule = listOf(
        DailyRewardTier(
            day = 1,
            title = "Tactical Ration",
            tokens = 150,
            xp = 0,
            description = "150 Tokens to bolster research"
        ),
        DailyRewardTier(
            day = 2,
            title = "Field Training",
            tokens = 0,
            xp = 250,
            description = "250 Commander XP for account leveling"
        ),
        DailyRewardTier(
            day = 3,
            title = "Munitions Crate",
            tokens = 200,
            xp = 150,
            description = "200 Tokens + 150 XP"
        ),
        DailyRewardTier(
            day = 4,
            title = "High-Command Grant",
            tokens = 300,
            xp = 0,
            description = "300 Research Tokens"
        ),
        DailyRewardTier(
            day = 5,
            title = "Combat Telemetry",
            tokens = 0,
            xp = 450,
            description = "450 Commander XP"
        ),
        DailyRewardTier(
            day = 6,
            title = "War Chest Supply",
            tokens = 350,
            xp = 250,
            description = "350 Tokens + 250 XP"
        ),
        DailyRewardTier(
            day = 7,
            title = "Apex Vanguard Cache",
            tokens = 600,
            xp = 600,
            specialBadge = "Veteran Vanguard",
            description = "600 Tokens + 600 XP + 'Veteran Vanguard' Ribbon"
        )
    )

    fun getTierForDay(day: Int): DailyRewardTier {
        val clampedDay = ((day - 1) % schedule.size) + 1
        return schedule[clampedDay - 1]
    }
}
