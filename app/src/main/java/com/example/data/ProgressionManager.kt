package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ProgressionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tower_defense_progression", Context.MODE_PRIVATE)

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
        if (mapId == "green_valley") return true
        return prefs.getBoolean("unlocked_$mapId", false)
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
                getStarsForMap("forest_pass")
    }
}
