package com.example.data

import com.example.entities.TowerType

enum class ResearchCategory(val displayName: String, val towerType: TowerType?) {
    MACHINE_GUN("Machine Gun", TowerType.MACHINE_GUN),
    CANNON("Heavy Cannon", TowerType.CANNON),
    RAPID_FIRE("Rapid Fire", TowerType.RAPID_FIRE),
    FROST_GUN("Frost Gun", TowerType.FROST_GUN),
    CITADEL("Citadel Base", null)
}

data class ResearchNode(
    val id: String,
    val name: String,
    val category: ResearchCategory,
    val tier: Int, // 1 (Root), 2 (Branch), 3 (Capstone)
    val maxRank: Int,
    val baseCost: Int,
    val costIncrement: Int,
    val prerequisiteId: String? = null,
    val prerequisiteRank: Int = 1,
    val statBonusLabel: String,
    val description: String,
    val statType: StatBonusType
) {
    fun getCostForRank(currentRank: Int): Int {
        return baseCost + (currentRank * costIncrement)
    }
}

enum class StatBonusType {
    DAMAGE,
    FIRE_RATE,
    RANGE,
    ARMOR_PENETRATION,
    SPLASH_RADIUS,
    RELOAD_SPEED,
    ATTACK_SPEED,
    CRIT_CHANCE,
    PROJECTILE_SPEED,
    SLOW_POTENCY,
    SLOW_DURATION,
    BASE_HP,
    STARTING_GOLD
}

object ResearchCatalog {
    val allNodes = listOf(
        // === MACHINE GUN RESEARCH TREE ===
        ResearchNode(
            id = "mg_damage",
            name = "Heavy Caliber Munitions",
            category = ResearchCategory.MACHINE_GUN,
            tier = 1,
            maxRank = 4,
            baseCost = 80,
            costIncrement = 40,
            prerequisiteId = null,
            statBonusLabel = "+5% Bullet Damage per rank",
            description = "High-density steel penetrators maximize ballistic impact against armored and unarmored targets.",
            statType = StatBonusType.DAMAGE
        ),
        ResearchNode(
            id = "mg_fire_rate",
            name = "Overclocked Feeder",
            category = ResearchCategory.MACHINE_GUN,
            tier = 2,
            maxRank = 4,
            baseCost = 100,
            costIncrement = 50,
            prerequisiteId = "mg_damage",
            prerequisiteRank = 1,
            statBonusLabel = "+5% Fire Rate per rank",
            description = "Pneumatic cycling mechanism speeds up chamber feed for higher sustained cyclic rate.",
            statType = StatBonusType.FIRE_RATE
        ),
        ResearchNode(
            id = "mg_range",
            name = "Match-Grade Rifled Barrels",
            category = ResearchCategory.MACHINE_GUN,
            tier = 2,
            maxRank = 4,
            baseCost = 100,
            costIncrement = 50,
            prerequisiteId = "mg_damage",
            prerequisiteRank = 1,
            statBonusLabel = "+3% Effective Range per rank",
            description = "Precision barrel rifling stabilizes muzzle velocity, extending operational perimeter.",
            statType = StatBonusType.RANGE
        ),
        ResearchNode(
            id = "mg_ap",
            name = "Tungsten Core Sabots",
            category = ResearchCategory.MACHINE_GUN,
            tier = 3,
            maxRank = 3,
            baseCost = 160,
            costIncrement = 80,
            prerequisiteId = "mg_fire_rate",
            prerequisiteRank = 2,
            statBonusLabel = "+4% Armor Penetration per rank",
            description = "Dense tungsten cores punch through enemy protective plating with enhanced lethality.",
            statType = StatBonusType.ARMOR_PENETRATION
        ),

        // === CANNON RESEARCH TREE ===
        ResearchNode(
            id = "cannon_radius",
            name = "High-Yield Explosive Matrix",
            category = ResearchCategory.CANNON,
            tier = 1,
            maxRank = 4,
            baseCost = 90,
            costIncrement = 45,
            prerequisiteId = null,
            statBonusLabel = "+5% Blast Radius per rank",
            description = "Optimized fragmentation blast dispersion catches more grouped targets in shockwaves.",
            statType = StatBonusType.SPLASH_RADIUS
        ),
        ResearchNode(
            id = "cannon_damage",
            name = "Shaped Concussion Charges",
            category = ResearchCategory.CANNON,
            tier = 2,
            maxRank = 4,
            baseCost = 110,
            costIncrement = 55,
            prerequisiteId = "cannon_radius",
            prerequisiteRank = 1,
            statBonusLabel = "+5% Blast Damage per rank",
            description = "Focuses primary thermal and kinetic energy inwards at point of detonation.",
            statType = StatBonusType.DAMAGE
        ),
        ResearchNode(
            id = "cannon_reload",
            name = "Hydraulic Breach Assist",
            category = ResearchCategory.CANNON,
            tier = 2,
            maxRank = 4,
            baseCost = 110,
            costIncrement = 55,
            prerequisiteId = "cannon_radius",
            prerequisiteRank = 1,
            statBonusLabel = "+5% Reload Speed per rank",
            description = "Pressurized hydraulic cylinder cycles 120mm shells swiftly between volleys.",
            statType = StatBonusType.RELOAD_SPEED
        ),
        ResearchNode(
            id = "cannon_crit",
            name = "Shockwave Detonators",
            category = ResearchCategory.CANNON,
            tier = 3,
            maxRank = 3,
            baseCost = 175,
            costIncrement = 90,
            prerequisiteId = "cannon_damage",
            prerequisiteRank = 2,
            statBonusLabel = "+4% Critical Blast Chance per rank",
            description = "Secondary micro-charges trigger sympathetic detonations on fortified targets.",
            statType = StatBonusType.CRIT_CHANCE
        ),

        // === RAPID FIRE RESEARCH TREE ===
        ResearchNode(
            id = "rapid_speed",
            name = "High-Torque Rotary Drive",
            category = ResearchCategory.RAPID_FIRE,
            tier = 1,
            maxRank = 4,
            baseCost = 100,
            costIncrement = 50,
            prerequisiteId = null,
            statBonusLabel = "+5% Attack Speed per rank",
            description = "Brushless electric motor spins six-barrel assembly at relentless velocity.",
            statType = StatBonusType.ATTACK_SPEED
        ),
        ResearchNode(
            id = "rapid_crit",
            name = "Electro-Optical Sight",
            category = ResearchCategory.RAPID_FIRE,
            tier = 2,
            maxRank = 4,
            baseCost = 120,
            costIncrement = 60,
            prerequisiteId = "rapid_speed",
            prerequisiteRank = 1,
            statBonusLabel = "+3% Critical Chance per rank",
            description = "Predictive telemetry system targets vital armor seams and propulsion joints.",
            statType = StatBonusType.CRIT_CHANCE
        ),
        ResearchNode(
            id = "rapid_velocity",
            name = "Aerodynamic Flechettes",
            category = ResearchCategory.RAPID_FIRE,
            tier = 2,
            maxRank = 4,
            baseCost = 120,
            costIncrement = 60,
            prerequisiteId = "rapid_speed",
            prerequisiteRank = 1,
            statBonusLabel = "+5% Projectile Velocity per rank",
            description = "Fin-stabilized micro-darts reach fleeing runners with reduced atmospheric drag.",
            statType = StatBonusType.PROJECTILE_SPEED
        ),
        ResearchNode(
            id = "rapid_drum",
            name = "Extended Helical Drum",
            category = ResearchCategory.RAPID_FIRE,
            tier = 3,
            maxRank = 3,
            baseCost = 180,
            costIncrement = 90,
            prerequisiteId = "rapid_crit",
            prerequisiteRank = 2,
            statBonusLabel = "+4% Bonus Flechette Damage per rank",
            description = "Increased projectile mass inflicts deep ballistic trauma on high-speed invaders.",
            statType = StatBonusType.DAMAGE
        ),

        // === FROST GUN RESEARCH TREE ===
        ResearchNode(
            id = "frost_potency",
            name = "Endothermic Nitrogen Solution",
            category = ResearchCategory.FROST_GUN,
            tier = 1,
            maxRank = 4,
            baseCost = 110,
            costIncrement = 55,
            prerequisiteId = null,
            statBonusLabel = "+4% Movement Slow per rank",
            description = "Colder cryo compound drops joint mobility and decelerates advancing swarm leaders.",
            statType = StatBonusType.SLOW_POTENCY
        ),
        ResearchNode(
            id = "frost_duration",
            name = "Cryogenic Gel Adhesion",
            category = ResearchCategory.FROST_GUN,
            tier = 2,
            maxRank = 4,
            baseCost = 130,
            costIncrement = 65,
            prerequisiteId = "frost_potency",
            prerequisiteRank = 1,
            statBonusLabel = "+5% Freeze Duration per rank",
            description = "Viscous cryo compound clings to targets, keeping them immobilized for longer windows.",
            statType = StatBonusType.SLOW_DURATION
        ),
        ResearchNode(
            id = "frost_range",
            name = "Pressurized Dispersion Nozzle",
            category = ResearchCategory.FROST_GUN,
            tier = 2,
            maxRank = 4,
            baseCost = 130,
            costIncrement = 65,
            prerequisiteId = "frost_potency",
            prerequisiteRank = 1,
            statBonusLabel = "+3% Cryo Beam Range per rank",
            description = "High-pressure cryo valve expands perimeter coverage over wider choke points.",
            statType = StatBonusType.RANGE
        ),
        ResearchNode(
            id = "frost_shards",
            name = "Sub-Zero Shards",
            category = ResearchCategory.FROST_GUN,
            tier = 3,
            maxRank = 3,
            baseCost = 190,
            costIncrement = 95,
            prerequisiteId = "frost_duration",
            prerequisiteRank = 2,
            statBonusLabel = "+4% Cryo Impact Damage per rank",
            description = "Sharp crystalline ice spikes accompany each liquid spray, cracking frozen chitin.",
            statType = StatBonusType.DAMAGE
        ),

        // === CITADEL DEFENSE RESEARCH ===
        ResearchNode(
            id = "citadel_armor",
            name = "Reinforced Bulkheads",
            category = ResearchCategory.CITADEL,
            tier = 1,
            maxRank = 4,
            baseCost = 100,
            costIncrement = 50,
            prerequisiteId = null,
            statBonusLabel = "+10% Citadel Max HP per rank",
            description = "Blast-treated composite armor plating fortifies the command base against breach breaches.",
            statType = StatBonusType.BASE_HP
        ),
        ResearchNode(
            id = "citadel_gold",
            name = "War Chest Reserves",
            category = ResearchCategory.CITADEL,
            tier = 2,
            maxRank = 4,
            baseCost = 120,
            costIncrement = 60,
            prerequisiteId = "citadel_armor",
            prerequisiteRank = 1,
            statBonusLabel = "+15 Starting Gold per rank",
            description = "Logistics stockpiling grants additional combat funds upon deploying to any battlefield.",
            statType = StatBonusType.STARTING_GOLD
        )
    )

    fun getNodesForCategory(category: ResearchCategory): List<ResearchNode> {
        return allNodes.filter { it.category == category }
    }

    fun getNodeById(id: String): ResearchNode? {
        return allNodes.find { it.id == id }
    }
}
