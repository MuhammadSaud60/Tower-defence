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
        if (mapId == "green_valley" || mapId == "map_1_valley") return true
        if (prefs.getBoolean("unlocked_$mapId", false)) return true

        val totalStars = getTotalStars()
        return when (mapId) {
            "desert_outpost", "map_2_canyon" -> getStarsForMap("green_valley") >= 1 || totalStars >= 1
            "forest_pass", "map_3_crossroads" -> getStarsForMap("desert_outpost") >= 1 || totalStars >= 3
            "split_routes", "map_4_split" -> getStarsForMap("forest_pass") >= 1 || totalStars >= 5
            "map_4_tunnel", "canyon_tunnel" -> getStarsForMap("split_routes") >= 1 || getStarsForMap("forest_pass") >= 1 || totalStars >= 7
            "the_crossing", "map_6_crossing" -> getStarsForMap("map_4_tunnel") >= 1 || getStarsForMap("canyon_tunnel") >= 1 || totalStars >= 9
            "map_5_loop", "dragons_coil" -> getStarsForMap("the_crossing") >= 1 || getStarsForMap("map_4_tunnel") >= 1 || totalStars >= 11
            else -> false
        }
    }

    fun isTutorialCompleted(): Boolean {
        return prefs.getBoolean("tutorial_completed", false)
    }

    fun setTutorialCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("tutorial_completed", completed).apply()
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
                getStarsForMap("forest_pass") +
                getStarsForMap("map_4_tunnel") +
                getStarsForMap("map_5_loop")
    }
}
