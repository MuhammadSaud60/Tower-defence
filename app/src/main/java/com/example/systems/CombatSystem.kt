package com.example.systems

import com.example.data.GameConfig
import com.example.entities.EffectType
import com.example.entities.Enemy
import com.example.entities.Projectile
import com.example.entities.ProjectileType
import com.example.entities.Tower
import com.example.entities.TowerType
import com.example.entities.VisualEffect

/**
 * Result data class from a single combat update tick.
 */
data class CombatTickResult(
    val updatedTowers: List<Tower>,
    val updatedEnemies: List<Enemy>,
    val updatedProjectiles: List<Projectile>,
    val updatedEffects: List<VisualEffect>,
    val enemiesKilled: Int,
    val coinsEarned: Int,
    val bossDefeated: Boolean,
    val newProjectilesFired: Int
)

class CombatSystem(
    private val targetingSystem: TargetingSystem = TargetingSystem()
) {

    fun update(
        dt: Float,
        towers: List<Tower>,
        enemies: List<Enemy>,
        projectiles: List<Projectile>,
        existingEffects: List<VisualEffect>
    ): CombatTickResult {
        var enemiesKilled = 0
        var coinsEarned = 0
        var bossDefeated = false
        var newProjectilesCount = 0

        val newEffects = mutableListOf<VisualEffect>()

        // 1. Process towers: cooldowns and firing
        val newProjectiles = mutableListOf<Projectile>()
        val updatedTowers = towers.map { tower ->
            val cooledTower = tower.tickCooldown(dt)
            val currentTarget = targetingSystem.findTarget(cooledTower, enemies)

            if (currentTarget != null) {
                val aimedTower = cooledTower.aimAt(currentTarget.position, currentTarget.id)

                if (aimedTower.canAttack) {
                    val projType = when (aimedTower.spec.type) {
                        TowerType.MACHINE_GUN -> ProjectileType.BULLET
                        TowerType.CANNON -> ProjectileType.CANNONBALL
                        TowerType.RAPID_FIRE -> ProjectileType.PLASMA_BOLT
                    }
                    val speed = when (projType) {
                        ProjectileType.BULLET -> GameConfig.BULLET_SPEED
                        ProjectileType.CANNONBALL -> GameConfig.CANNONBALL_SPEED
                        ProjectileType.PLASMA_BOLT -> GameConfig.PLASMA_SPEED
                    }

                    newProjectiles.add(
                        Projectile(
                            type = projType,
                            currentPosition = aimedTower.position,
                            targetEnemyId = currentTarget.id,
                            targetLastKnownPosition = currentTarget.position,
                            damage = aimedTower.spec.damage,
                            splashRadius = aimedTower.spec.splashRadius,
                            armorPiercing = aimedTower.spec.armorPiercing,
                            speed = speed
                        )
                    )
                    newProjectilesCount++
                    aimedTower.resetCooldown()
                } else {
                    aimedTower
                }
            } else {
                cooledTower.clearTarget()
            }
        }

        // 2. Map enemies for fast lookup and mutation
        val enemyMap = enemies.associateBy { it.id }.toMutableMap()

        // 3. Update projectiles
        val allProjectiles = projectiles + newProjectiles
        val remainingProjectiles = mutableListOf<Projectile>()

        for (proj in allProjectiles) {
            val targetEnemy = enemyMap[proj.targetEnemyId]
            val enemyPos = if (targetEnemy?.isAlive == true) targetEnemy.position else null

            val updatedProj = proj.advance(dt, enemyPos)

            if (updatedProj.isHit) {
                val impactPos = updatedProj.currentPosition

                if (updatedProj.splashRadius > 0f) {
                    // Cannon explosion splash
                    newEffects.add(
                        VisualEffect(
                            type = EffectType.CANNON_EXPLOSION,
                            position = impactPos,
                            maxLifetime = 0.4f,
                            maxRadius = updatedProj.splashRadius
                        )
                    )

                    // Damage all enemies in splash radius
                    for ((id, enemy) in enemyMap.entries) {
                        if (enemy.isAlive && enemy.position.distanceTo(impactPos) <= updatedProj.splashRadius) {
                            val falloff = (1f - (enemy.position.distanceTo(impactPos) / updatedProj.splashRadius) * 0.4f).coerceIn(0.5f, 1f)
                            val dmg = updatedProj.damage * falloff
                            val damaged = enemy.takeDamage(dmg, updatedProj.armorPiercing)
                            enemyMap[id] = damaged

                            if (!damaged.isAlive) {
                                enemiesKilled++
                                coinsEarned += enemy.spec.rewardCoins
                                if (enemy.spec.isBoss) bossDefeated = true
                                newEffects.add(
                                    VisualEffect(
                                        type = EffectType.ENEMY_DEATH_POOF,
                                        position = enemy.position,
                                        maxLifetime = 0.35f,
                                        maxRadius = enemy.spec.radius * 1.5f
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Direct hit
                    newEffects.add(
                        VisualEffect(
                            type = EffectType.HIT_SPARK,
                            position = impactPos,
                            maxLifetime = 0.16f,
                            maxRadius = 18f
                        )
                    )

                    if (targetEnemy != null && targetEnemy.isAlive) {
                        val damaged = targetEnemy.takeDamage(updatedProj.damage, updatedProj.armorPiercing)
                        enemyMap[targetEnemy.id] = damaged

                        if (!damaged.isAlive) {
                            enemiesKilled++
                            coinsEarned += targetEnemy.spec.rewardCoins
                            if (targetEnemy.spec.isBoss) bossDefeated = true
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.ENEMY_DEATH_POOF,
                                    position = targetEnemy.position,
                                    maxLifetime = 0.35f,
                                    maxRadius = targetEnemy.spec.radius * 1.5f
                                )
                            )
                        }
                    }
                }
            } else if (!updatedProj.isExpired) {
                remainingProjectiles.add(updatedProj)
            }
        }

        // 4. Update visual effects
        val updatedEffects = (existingEffects + newEffects)
            .map { it.advance(dt) }
            .filter { !it.isFinished }

        return CombatTickResult(
            updatedTowers = updatedTowers,
            updatedEnemies = enemyMap.values.toList(),
            updatedProjectiles = remainingProjectiles,
            updatedEffects = updatedEffects,
            enemiesKilled = enemiesKilled,
            coinsEarned = coinsEarned,
            bossDefeated = bossDefeated,
            newProjectilesFired = newProjectilesCount
        )
    }
}
