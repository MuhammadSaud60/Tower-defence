package com.example.systems

import com.example.data.GameConfig

/**
 * Manages player funds, purchase validations, and combat rewards.
 */
class EconomySystem(
    initialCoins: Int = GameConfig.STARTING_COINS
) {
    var currentCoins: Int = initialCoins
        private set

    var totalCoinsEarned: Int = initialCoins
        private set

    fun canAfford(cost: Int): Boolean = currentCoins >= cost

    fun spend(amount: Int): Boolean {
        if (!canAfford(amount)) return false
        currentCoins -= amount
        return true
    }

    fun addCoins(amount: Int) {
        if (amount > 0) {
            currentCoins += amount
            totalCoinsEarned += amount
        }
    }

    fun rewardForEnemyKill(count: Int = 1) {
        addCoins(count * 5)
    }

    fun reset(startingCoins: Int = GameConfig.STARTING_COINS) {
        currentCoins = startingCoins
        totalCoinsEarned = startingCoins
    }
}
