package com.example.systems

import com.example.data.GameConfig
import com.example.entities.DestructibleObject
import com.example.entities.DestructibleType
import com.example.entities.EffectType
import com.example.entities.Enemy
import com.example.entities.EnemyType
import com.example.entities.Point2D
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
    val updatedDestructibles: List<DestructibleObject> = emptyList(),
    val updatedProjectiles: List<Projectile>,
    val updatedEffects: List<VisualEffect>,
    val enemiesKilled: Int,
    val coinsEarned: Int,
    val bossDefeated: Boolean,
    val newProjectilesFired: Int,
    val firedTowerTypes: Set<TowerType> = emptySet(),
    val hasCannonImpact: Boolean = false,
    val hasFrostImpact: Boolean = false,
    val hasBossImpact: Boolean = false,
    val hasEnemyHit: Boolean = false,
    val hasSmallEnemyHit: Boolean = false,
    val hasHeavyEnemyHit: Boolean = false,
    val hasObjectHit: Boolean = false,
    val hasObjectDestroyed: Boolean = false,
    val hasTreeHit: Boolean = false,
    val hasStoneHit: Boolean = false,
    val hasTreeDestroyed: Boolean = false,
    val hasStoneDestroyed: Boolean = false,
    val destroyedDestructibleIds: Set<String> = emptySet()
)

class CombatSystem(
    private val targetingSystem: TargetingSystem = TargetingSystem()
) {

    fun update(
        dt: Float,
        towers: List<Tower>,
        enemies: List<Enemy>,
        destructibles: List<DestructibleObject> = emptyList(),
        projectiles: List<Projectile>,
        existingEffects: List<VisualEffect>
    ): CombatTickResult {
        var enemiesKilled = 0
        var coinsEarned = 0
        var bossDefeated = false
        var newProjectilesCount = 0
        val firedTowerTypes = mutableSetOf<TowerType>()
        var hasCannonImpact = false
        var hasFrostImpact = false
        var hasBossImpact = false
        var hasEnemyHit = false
        var hasSmallEnemyHit = false
        var hasHeavyEnemyHit = false
        var hasObjectHit = false
        var hasObjectDestroyed = false
        var hasTreeHit = false
        var hasStoneHit = false
        var hasTreeDestroyed = false
        var hasStoneDestroyed = false

        val newEffects = mutableListOf<VisualEffect>()

        // 1. Map destructibles and enemies
        val destructibleMap = destructibles
            .map { it.tickFlash(dt) }
            .associateBy { it.id }
            .toMutableMap()
        val destroyedDestructibles = mutableSetOf<String>()

        // Process towers: cooldowns and firing
        val newProjectiles = mutableListOf<Projectile>()
        val updatedTowers = towers.map { tower ->
            val cooledTower = tower.tickCooldown(dt)

            var chosenTargetPos: Point2D? = null
            var chosenTargetId: String? = null
            var chosenTargetType: com.example.entities.TargetType = com.example.entities.TargetType.ENEMY
            var activeManualTargetId = cooledTower.manualTargetId
            var activeManualTargetType = cooledTower.manualTargetType

            // 1. Check if tower has an active manual target (e.g. player tapped tree/stone or enemy)
            if (activeManualTargetId != null) {
                val manualDestructible = destructibleMap[activeManualTargetId]?.takeIf {
                    it.isAlive && cooledTower.isObjectInRange(it.position, it.radius)
                }
                val manualEnemy = enemies.firstOrNull {
                    it.id == activeManualTargetId && it.isAlive && !it.reachedBase && cooledTower.isEnemyInRange(it.position)
                }

                if (manualDestructible != null) {
                    chosenTargetPos = manualDestructible.position
                    chosenTargetId = manualDestructible.id
                    chosenTargetType = com.example.entities.TargetType.DESTRUCTIBLE
                } else if (manualEnemy != null) {
                    chosenTargetPos = manualEnemy.position
                    chosenTargetId = manualEnemy.id
                    chosenTargetType = com.example.entities.TargetType.ENEMY
                } else {
                    // Manual target was destroyed or moved out of range
                    activeManualTargetId = null
                    activeManualTargetType = null
                }
            }

            // 2. Normal automatic targeting: ONLY target enemies according to targeting system.
            // Trees and stones are NEVER automatically selected!
            if (chosenTargetId == null) {
                val enemyTarget = targetingSystem.findTarget(cooledTower, enemies)
                if (enemyTarget != null) {
                    chosenTargetPos = enemyTarget.position
                    chosenTargetId = enemyTarget.id
                    chosenTargetType = com.example.entities.TargetType.ENEMY
                }
            }

            val towerWithTargetState = cooledTower.copy(
                manualTargetId = activeManualTargetId,
                manualTargetType = activeManualTargetType
            )

            if (chosenTargetPos != null && chosenTargetId != null) {
                val aimedTower = towerWithTargetState.aimAt(chosenTargetPos, chosenTargetId, chosenTargetType)

                if (aimedTower.canAttack) {
                    val projType = when (aimedTower.spec.type) {
                        TowerType.MACHINE_GUN -> ProjectileType.BULLET
                        TowerType.CANNON -> ProjectileType.CANNONBALL
                        TowerType.RAPID_FIRE -> ProjectileType.RAPID_SLUG
                        TowerType.FROST_GUN -> ProjectileType.CRYO_ORB
                    }
                    val speed = when (projType) {
                        ProjectileType.BULLET -> GameConfig.BULLET_SPEED
                        ProjectileType.CANNONBALL -> GameConfig.CANNONBALL_SPEED
                        ProjectileType.RAPID_SLUG -> GameConfig.PLASMA_SPEED
                        ProjectileType.CRYO_ORB -> GameConfig.CRYO_SPEED
                    }

                    newProjectiles.add(
                        Projectile(
                            type = projType,
                            currentPosition = aimedTower.position,
                            prevPosition = aimedTower.position,
                            targetId = chosenTargetId,
                            targetType = chosenTargetType,
                            targetWorldPosition = chosenTargetPos,
                            targetEnemyId = chosenTargetId,
                            targetLastKnownPosition = chosenTargetPos,
                            damage = aimedTower.spec.damage,
                            splashRadius = aimedTower.spec.splashRadius,
                            armorPiercing = aimedTower.spec.armorPiercing,
                            slowFactor = aimedTower.spec.slowFactor,
                            slowDuration = aimedTower.spec.slowDuration,
                            speed = speed
                        )
                    )
                    firedTowerTypes.add(aimedTower.spec.type)
                    newProjectilesCount++
                    aimedTower.resetCooldown()
                } else {
                    aimedTower
                }
            } else {
                towerWithTargetState.clearTarget()
            }
        }

        // 2. Map enemies for fast lookup and mutation
        val enemyMap = enemies.associateBy { it.id }.toMutableMap()

        // 3. Update projectiles
        val allProjectiles = projectiles + newProjectiles
        val remainingProjectiles = mutableListOf<Projectile>()

        for (proj in allProjectiles) {
            val targetEnemy = enemyMap[proj.targetId] ?: enemyMap[proj.targetEnemyId]
            val targetObj = destructibleMap[proj.targetId] ?: destructibleMap[proj.targetEnemyId]
            val targetPos = when (proj.targetType) {
                com.example.entities.TargetType.DESTRUCTIBLE -> targetObj?.takeIf { it.isAlive }?.position
                com.example.entities.TargetType.ENEMY -> targetEnemy?.takeIf { it.isAlive }?.position
            } ?: proj.targetWorldPosition

            val updatedProj = proj.advance(dt, targetPos)

            if (updatedProj.isHit) {
                val impactPos = updatedProj.currentPosition
                val isNormalGun = updatedProj.type == ProjectileType.BULLET || updatedProj.type == ProjectileType.RAPID_SLUG

                if (updatedProj.splashRadius > 0f) {
                    if (updatedProj.type == ProjectileType.CRYO_ORB) {
                        hasFrostImpact = true
                        newEffects.add(
                            VisualEffect(
                                type = EffectType.FROST_BURST,
                                position = impactPos,
                                maxLifetime = 0.38f,
                                maxRadius = updatedProj.splashRadius
                            )
                        )
                    } else {
                        hasCannonImpact = true
                        newEffects.add(
                            VisualEffect(
                                type = EffectType.CANNON_EXPLOSION,
                                position = impactPos,
                                maxLifetime = 0.45f,
                                maxRadius = updatedProj.splashRadius
                            )
                        )
                    }

                    // Damage all enemies in splash radius and apply status effects
                    for ((id, enemy) in enemyMap.entries) {
                        if (enemy.isAlive && enemy.position.distanceTo(impactPos) <= updatedProj.splashRadius) {
                            val falloff = (1f - (enemy.position.distanceTo(impactPos) / updatedProj.splashRadius) * 0.4f).coerceIn(0.5f, 1f)
                            val dmg = updatedProj.damage * falloff
                            var damaged = enemy.takeDamage(dmg, updatedProj.armorPiercing, isNormalGun = isNormalGun)
                            if (updatedProj.slowFactor > 0f) {
                                damaged = damaged.applySlow(updatedProj.slowFactor, updatedProj.slowDuration)
                            }
                            enemyMap[id] = damaged

                            if (enemy.spec.isBoss) {
                                hasBossImpact = true
                            } else if (enemy.spec.type == EnemyType.HEAVY) {
                                hasHeavyEnemyHit = true
                                hasEnemyHit = true
                            } else {
                                hasSmallEnemyHit = true
                                hasEnemyHit = true
                            }

                            if (!damaged.isAlive) {
                                enemiesKilled++
                                coinsEarned += enemy.spec.rewardCoins
                                if (enemy.spec.isBoss) bossDefeated = true
                                newEffects.add(createEnemyDeathEffect(enemy))
                            }
                        }
                    }

                    // Splash damage to destructible objects in radius
                    for ((id, obj) in destructibleMap.entries) {
                        if (obj.isAlive && obj.position.distanceTo(impactPos) <= (updatedProj.splashRadius + obj.radius)) {
                            val falloff = (1f - (obj.position.distanceTo(impactPos) / updatedProj.splashRadius) * 0.4f).coerceIn(0.5f, 1f)
                            val dmg = updatedProj.damage * falloff
                            val damaged = obj.takeDamage(dmg)
                            destructibleMap[id] = damaged
                            hasObjectHit = true
                            if (obj.type.isTree) {
                                hasTreeHit = true
                            } else if (obj.type.isStone) {
                                hasStoneHit = true
                            }

                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.DESTRUCTIBLE_HIT,
                                    position = obj.position,
                                    maxLifetime = 0.22f,
                                    maxRadius = obj.radius * 0.8f
                                )
                            )

                            if (!damaged.isAlive && destroyedDestructibles.add(id)) {
                                hasObjectDestroyed = true
                                if (obj.type.isTree) {
                                    hasTreeDestroyed = true
                                } else if (obj.type.isStone) {
                                    hasStoneDestroyed = true
                                }
                                coinsEarned += obj.rewardTokens
                                newEffects.add(
                                    VisualEffect(
                                        type = EffectType.DESTRUCTIBLE_DEBRIS,
                                        position = obj.position,
                                        maxLifetime = 0.5f,
                                        maxRadius = obj.radius * 1.5f
                                    )
                                )
                                newEffects.add(
                                    VisualEffect(
                                        type = EffectType.FLOATING_TOKEN,
                                        position = obj.position,
                                        maxLifetime = 0.85f,
                                        maxRadius = 24f,
                                        text = "+${obj.rewardTokens}🪙"
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // Direct hit
                    if (targetObj != null && targetObj.isAlive) {
                        hasObjectHit = true
                        if (targetObj.type.isTree) {
                            hasTreeHit = true
                        } else if (targetObj.type.isStone) {
                            hasStoneHit = true
                        }
                        val dmg = updatedProj.damage
                        val damaged = targetObj.takeDamage(dmg)
                        destructibleMap[targetObj.id] = damaged

                        newEffects.add(
                            VisualEffect(
                                type = EffectType.DESTRUCTIBLE_HIT,
                                position = impactPos,
                                maxLifetime = 0.22f,
                                maxRadius = targetObj.radius * 0.8f
                            )
                        )

                        if (!damaged.isAlive && destroyedDestructibles.add(targetObj.id)) {
                            hasObjectDestroyed = true
                            if (targetObj.type.isTree) {
                                hasTreeDestroyed = true
                            } else if (targetObj.type.isStone) {
                                hasStoneDestroyed = true
                            }
                            coinsEarned += targetObj.rewardTokens
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.DESTRUCTIBLE_DEBRIS,
                                    position = targetObj.position,
                                    maxLifetime = 0.5f,
                                    maxRadius = targetObj.radius * 1.5f
                                )
                            )
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.FLOATING_TOKEN,
                                    position = targetObj.position,
                                    maxLifetime = 0.85f,
                                    maxRadius = 24f,
                                    text = "+${targetObj.rewardTokens}🪙"
                                )
                            )
                        }
                    } else if (updatedProj.type == ProjectileType.CRYO_ORB) {
                        hasFrostImpact = true
                        newEffects.add(
                            VisualEffect(
                                type = EffectType.FROST_BURST,
                                position = impactPos,
                                maxLifetime = 0.32f,
                                maxRadius = 30f
                            )
                        )
                    } else {
                        val isBoss = targetEnemy?.spec?.isBoss == true
                        val isHeavy = targetEnemy?.spec?.type == EnemyType.HEAVY
                        if (isBoss) {
                            hasBossImpact = true
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.BOSS_HIT_IMPACT,
                                    position = impactPos,
                                    maxLifetime = 0.24f,
                                    maxRadius = 26f
                                )
                            )
                        } else if (isHeavy) {
                            hasHeavyEnemyHit = true
                            hasEnemyHit = true
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.MG_HIT_SPARK,
                                    position = impactPos,
                                    maxLifetime = 0.18f,
                                    maxRadius = 18f
                                )
                            )
                        } else if (updatedProj.type == ProjectileType.RAPID_SLUG) {
                            hasSmallEnemyHit = true
                            hasEnemyHit = true
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.RAPID_HIT_SPARK,
                                    position = impactPos,
                                    maxLifetime = 0.12f,
                                    maxRadius = 14f
                                )
                            )
                        } else {
                            hasSmallEnemyHit = true
                            hasEnemyHit = true
                            newEffects.add(
                                VisualEffect(
                                    type = EffectType.MG_HIT_SPARK,
                                    position = impactPos,
                                    maxLifetime = 0.18f,
                                    maxRadius = 18f
                                )
                            )
                        }
                    }

                    if (targetEnemy != null && targetEnemy.isAlive) {
                        var damaged = targetEnemy.takeDamage(updatedProj.damage, updatedProj.armorPiercing, isNormalGun = isNormalGun)
                        if (updatedProj.slowFactor > 0f) {
                            damaged = damaged.applySlow(updatedProj.slowFactor, updatedProj.slowDuration)
                        }
                        enemyMap[targetEnemy.id] = damaged

                        if (!damaged.isAlive) {
                            enemiesKilled++
                            coinsEarned += targetEnemy.spec.rewardCoins
                            if (targetEnemy.spec.isBoss) bossDefeated = true
                            newEffects.add(createEnemyDeathEffect(targetEnemy))
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

        val finalizedTowers = updatedTowers.map { tower ->
            if (tower.manualTargetId != null && destroyedDestructibles.contains(tower.manualTargetId)) {
                tower.clearManualTarget()
            } else {
                tower
            }
        }

        return CombatTickResult(
            updatedTowers = finalizedTowers,
            updatedEnemies = enemyMap.values.toList(),
            updatedDestructibles = destructibleMap.values.toList(),
            updatedProjectiles = remainingProjectiles,
            updatedEffects = updatedEffects,
            enemiesKilled = enemiesKilled,
            coinsEarned = coinsEarned,
            bossDefeated = bossDefeated,
            newProjectilesFired = newProjectilesCount,
            firedTowerTypes = firedTowerTypes,
            hasCannonImpact = hasCannonImpact,
            hasFrostImpact = hasFrostImpact,
            hasBossImpact = hasBossImpact,
            hasEnemyHit = hasEnemyHit,
            hasSmallEnemyHit = hasSmallEnemyHit,
            hasHeavyEnemyHit = hasHeavyEnemyHit,
            hasObjectHit = hasObjectHit,
            hasObjectDestroyed = hasObjectDestroyed,
            hasTreeHit = hasTreeHit,
            hasStoneHit = hasStoneHit,
            hasTreeDestroyed = hasTreeDestroyed,
            hasStoneDestroyed = hasStoneDestroyed,
            destroyedDestructibleIds = destroyedDestructibles
        )
    }

    private fun createEnemyDeathEffect(enemy: Enemy): VisualEffect {
        val type = when (enemy.spec.type) {
            EnemyType.SCOUT -> EffectType.SCOUT_DEATH
            EnemyType.SOLDIER -> EffectType.SOLDIER_DEATH
            EnemyType.HEAVY -> EffectType.HEAVY_DEATH
            EnemyType.RUNNER -> EffectType.RUNNER_DEATH
            EnemyType.BOSS -> EffectType.BOSS_DEATH
        }
        val radius = when (enemy.spec.type) {
            EnemyType.SCOUT -> 24f
            EnemyType.RUNNER -> 26f
            EnemyType.SOLDIER -> 34f
            EnemyType.HEAVY -> 48f
            EnemyType.BOSS -> 80f
        }
        val lifetime = when (enemy.spec.type) {
            EnemyType.BOSS -> 0.75f
            EnemyType.HEAVY -> 0.48f
            else -> 0.35f
        }
        return VisualEffect(
            type = type,
            position = enemy.position,
            maxLifetime = lifetime,
            maxRadius = radius
        )
    }
}
