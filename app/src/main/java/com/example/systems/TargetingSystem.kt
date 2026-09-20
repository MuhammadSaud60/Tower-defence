package com.example.systems

import com.example.entities.Enemy
import com.example.entities.TargetingStrategy
import com.example.entities.Tower

/**
 * Handles target acquisition for towers based on the chosen strategy:
 * First, Last, Strongest, Closest.
 */
class TargetingSystem {

    fun findTarget(tower: Tower, enemies: List<Enemy>): Enemy? {
        val candidates = enemies
            .asSequence()
            .filter { it.isAlive && !it.reachedBase }
            .filter { !it.isStealthed } // Stealth units cannot be acquired while cloaked
            .filter { !it.spec.isFlying || tower.canTargetFlying } // Ground siege towers (Cannon) cannot target air units
            .filter { tower.isEnemyInRange(it.position) }

        return when (tower.targetingStrategy) {
            TargetingStrategy.FIRST -> candidates.maxByOrNull { it.totalProgress }
            TargetingStrategy.LAST -> candidates.minByOrNull { it.totalProgress }
            TargetingStrategy.STRONGEST -> candidates.maxWithOrNull(
                compareBy<Enemy> { it.currentHp }.thenBy { if (it.spec.isBoss) 1 else 0 }
            )
            TargetingStrategy.CLOSEST -> candidates.minByOrNull { tower.position.distanceTo(it.position) }
        }
    }
}
