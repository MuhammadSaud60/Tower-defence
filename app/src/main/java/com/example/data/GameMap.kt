package com.example.data

import com.example.entities.GamePath
import com.example.entities.Point2D

enum class EnvironmentType {
    GREEN_VALLEY,
    DESERT_OUTPOST,
    FOREST_PASS
}

enum class DecorationType {
    OAK_TREE,
    PINE_TREE,
    BUSH,
    BOULDER,
    WATER_POND,
    FLOWER_PATCH
}

data class MapDecoration(
    val id: String,
    val type: DecorationType,
    val position: Point2D,
    val size: Float,
    val rotation: Float = 0f
)

data class GameMap(
    val id: String,
    val name: String,
    val description: String,
    val environmentType: EnvironmentType,
    val isUnlocked: Boolean,
    val starsEarned: Int,
    val path: GamePath,
    val basePosition: Point2D,
    val decorations: List<MapDecoration>,
    val waterPondCenter: Point2D? = null,
    val waterPondRadius: Float = 0f
) {
    /**
     * Checks if a tower can be placed at the candidate position.
     * Prevents placement on the road corridor, inside water bodies, or on base.
     */
    fun canPlaceAt(candidate: Point2D, towerRadius: Float = GameConfig.TOWER_SIZE / 2f): Boolean {
        // Must be within battlefield boundaries
        if (candidate.x < 50f || candidate.x > GameConfig.VIRTUAL_WIDTH - 50f ||
            candidate.y < 50f || candidate.y > GameConfig.VIRTUAL_HEIGHT - 50f
        ) {
            return false
        }

        // Check path clearance
        if (path.isPointOnPath(candidate, margin = path.pathWidth / 2f + towerRadius + 6f)) {
            return false
        }

        // Check water collision
        if (waterPondCenter != null && waterPondRadius > 0f) {
            if (candidate.distanceTo(waterPondCenter) < (waterPondRadius + towerRadius)) {
                return false
            }
        }

        // Check base fortress collision
        val baseLeft = basePosition.x - GameConfig.BASE_WIDTH / 2f - towerRadius
        val baseRight = basePosition.x + GameConfig.BASE_WIDTH / 2f + towerRadius
        val baseTop = basePosition.y - GameConfig.BASE_HEIGHT / 2f - towerRadius
        val baseBottom = basePosition.y + GameConfig.BASE_HEIGHT / 2f + towerRadius

        if (candidate.x in baseLeft..baseRight && candidate.y in baseTop..baseBottom) {
            return false
        }

        return true
    }

    companion object {
        fun getPresetMaps(): List<GameMap> {
            return listOf(
                createGreenValleyMap(isUnlocked = true, stars = 0),
                createDesertOutpostMap(isUnlocked = false, stars = 0),
                createForestPassMap(isUnlocked = false, stars = 0)
            )
        }

        fun createGreenValleyMap(isUnlocked: Boolean = true, stars: Int = 0): GameMap {
            val pathWaypoints = listOf(
                Point2D(-40f, 180f),
                Point2D(340f, 180f),
                Point2D(340f, 500f),
                Point2D(780f, 500f),
                Point2D(780f, 860f),
                Point2D(220f, 860f),
                Point2D(220f, 1220f),
                Point2D(820f, 1220f)
            )
            val path = GamePath(waypoints = pathWaypoints, pathWidth = GameConfig.PATH_WIDTH)
            val basePos = Point2D(830f, 1220f)
            val pondCenter = Point2D(550f, 680f)
            val pondRadius = 65f

            val decor = listOf(
                // Trees
                MapDecoration("t1", DecorationType.OAK_TREE, Point2D(120f, 100f), size = 64f),
                MapDecoration("t2", DecorationType.PINE_TREE, Point2D(540f, 120f), size = 52f),
                MapDecoration("t3", DecorationType.OAK_TREE, Point2D(880f, 140f), size = 68f),
                MapDecoration("t4", DecorationType.PINE_TREE, Point2D(140f, 380f), size = 56f),
                MapDecoration("t5", DecorationType.OAK_TREE, Point2D(520f, 360f), size = 60f),
                MapDecoration("t6", DecorationType.PINE_TREE, Point2D(900f, 660f), size = 54f),
                MapDecoration("t7", DecorationType.OAK_TREE, Point2D(120f, 660f), size = 64f),
                MapDecoration("t8", DecorationType.PINE_TREE, Point2D(460f, 1020f), size = 58f),
                MapDecoration("t9", DecorationType.OAK_TREE, Point2D(900f, 1020f), size = 66f),
                MapDecoration("t10", DecorationType.PINE_TREE, Point2D(100f, 1340f), size = 56f),
                MapDecoration("t11", DecorationType.OAK_TREE, Point2D(520f, 1340f), size = 64f),

                // Rocks & Boulders
                MapDecoration("r1", DecorationType.BOULDER, Point2D(240f, 100f), size = 32f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(660f, 360f), size = 28f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(920f, 400f), size = 36f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(400f, 860f), size = 30f),
                MapDecoration("r5", DecorationType.BOULDER, Point2D(340f, 1340f), size = 34f),

                // Bushes & Flowers
                MapDecoration("b1", DecorationType.BUSH, Point2D(420f, 220f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(700f, 560f), size = 24f),
                MapDecoration("b3", DecorationType.BUSH, Point2D(280f, 780f), size = 28f),
                MapDecoration("f1", DecorationType.FLOWER_PATCH, Point2D(180f, 260f), size = 22f),
                MapDecoration("f2", DecorationType.FLOWER_PATCH, Point2D(840f, 740f), size = 24f),
                MapDecoration("f3", DecorationType.FLOWER_PATCH, Point2D(680f, 1100f), size = 22f)
            )

            return GameMap(
                id = "green_valley",
                name = "Green Valley",
                description = "Lush grasslands with a tranquil lake, winding dirt trails, and fortified defensive castle.",
                environmentType = EnvironmentType.GREEN_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                path = path,
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius
            )
        }

        fun createDesertOutpostMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(500f, -40f),
                Point2D(500f, 320f),
                Point2D(180f, 320f),
                Point2D(180f, 800f),
                Point2D(820f, 800f),
                Point2D(820f, 1260f),
                Point2D(480f, 1260f)
            )
            return GameMap(
                id = "desert_outpost",
                name = "Desert Outpost",
                description = "Arid canyons, sand dunes, and desert shrubs. Unlocks by completing Green Valley.",
                environmentType = EnvironmentType.DESERT_OUTPOST,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                path = GamePath(waypoints, GameConfig.PATH_WIDTH),
                basePosition = Point2D(480f, 1260f),
                decorations = emptyList()
            )
        }

        fun createForestPassMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(-40f, 300f),
                Point2D(700f, 300f),
                Point2D(700f, 700f),
                Point2D(250f, 700f),
                Point2D(250f, 1100f),
                Point2D(800f, 1100f)
            )
            return GameMap(
                id = "forest_pass",
                name = "Forest Pass",
                description = "Dense ancient pine forests with narrow tactical chokepoints. Unlocks with 3 Stars.",
                environmentType = EnvironmentType.FOREST_PASS,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                path = GamePath(waypoints, GameConfig.PATH_WIDTH),
                basePosition = Point2D(800f, 1100f),
                decorations = emptyList()
            )
        }
    }
}
