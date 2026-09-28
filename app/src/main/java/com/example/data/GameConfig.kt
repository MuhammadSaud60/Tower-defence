package com.example.data

/**
 * Global game configuration constants and specifications for Version 2.
 * Uses virtual coordinate space (1000f width x 1400f height).
 */
object GameConfig {
    const val VIRTUAL_WIDTH = 1000f
    const val VIRTUAL_HEIGHT = 1400f

    // Economy
    const val STARTING_COINS = 85
    const val WAVE_CLEAR_BONUS_COINS = 12

    // Base Fortress
    const val BASE_MAX_HP = 100
    const val DAMAGE_PER_ENEMY = 10
    const val DAMAGE_PER_BOSS = 30
    const val BASE_WIDTH = 110f
    const val BASE_HEIGHT = 90f

    // Waves
    const val TOTAL_WAVES = 20
    const val AUTO_NEXT_WAVE_DELAY_SECONDS = 3.0f

    // Path & Placement
    const val PATH_WIDTH = 80f
    const val TOWER_SIZE = 56f
    const val MIN_DISTANCE_BETWEEN_TOWERS = 64f

    // Machine Gun Tower stats
    const val MG_TOWER_COST = 40
    const val MG_TOWER_RANGE = 220f
    const val MG_TOWER_DAMAGE = 9f
    const val MG_TOWER_COOLDOWN = 0.22f

    // Cannon Tower stats (Heavy Gun)
    const val CANNON_TOWER_COST = 150
    const val CANNON_TOWER_RANGE = 310f
    const val CANNON_TOWER_DAMAGE = 65f
    const val CANNON_TOWER_COOLDOWN = 1.5f
    const val CANNON_SPLASH_RADIUS = 90f

    // Rapid Fire Tower stats - High damage + fast firing rate
    const val RAPID_TOWER_COST = 70
    const val RAPID_TOWER_RANGE = 260f
    const val RAPID_TOWER_DAMAGE = 14f
    const val RAPID_TOWER_COOLDOWN = 0.08f

    // Cryo / Frost Tower stats (Slows down enemies)
    const val FROST_TOWER_COST = 60
    const val FROST_TOWER_RANGE = 230f
    const val FROST_TOWER_DAMAGE = 6f
    const val FROST_TOWER_COOLDOWN = 0.75f
    const val FROST_TOWER_SLOW_FACTOR = 0.45f
    const val FROST_TOWER_SLOW_DURATION = 2.5f

    // Projectiles
    const val BULLET_SPEED = 900f
    const val CANNONBALL_SPEED = 500f
    const val PLASMA_SPEED = 850f
    const val CRYO_SPEED = 750f
    const val PROJECTILE_HIT_RADIUS = 16f
}
