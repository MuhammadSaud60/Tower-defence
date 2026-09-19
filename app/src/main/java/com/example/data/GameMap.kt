package com.example.data

import com.example.entities.DestructibleObject
import com.example.entities.DestructibleType
import com.example.entities.GamePath
import com.example.entities.Point2D

enum class EnvironmentType {
    GREEN_VALLEY,
    DESERT_CANYON,
    FOREST_CROSSROADS,
    OBSIDIAN_TUNNEL,
    DRAGON_COIL,
    SNOW_VALLEY,
    NIGHT_FORTRESS
}

enum class DecorationType {
    OAK_TREE,
    PINE_TREE,
    BUSH,
    BOULDER,
    WATER_POND,
    FLOWER_PATCH,
    CACTUS,
    CRYSTAL,
    SIGNPOST,
    SNOW_PILE,
    FROZEN_BUSH,
    GLOW_MUSHROOM,
    LANTERN_POST
}

data class MapDecoration(
    val id: String,
    val type: DecorationType,
    val position: Point2D,
    val size: Float,
    val rotation: Float = 0f
)

data class TunnelRegion(
    val id: String = "cavern_tunnel",
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float,
    val entrance: Point2D,
    val exit: Point2D
)

data class BlockedArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val reason: String = "Obstacle"
)

data class GameMap(
    val id: String,
    val name: String,
    val description: String,
    val environmentType: EnvironmentType,
    val isUnlocked: Boolean,
    val starsEarned: Int,
    val paths: List<GamePath>,
    val basePosition: Point2D,
    val decorations: List<MapDecoration> = emptyList(),
    val waterPondCenter: Point2D? = null,
    val waterPondRadius: Float = 0f,
    val tunnelRegion: TunnelRegion? = null,
    val blockedAreas: List<BlockedArea> = emptyList(),
    val destructibles: List<DestructibleObject> = emptyList(),
    val worldWidth: Float = 2000f,
    val worldHeight: Float = 1500f,
    val startingCameraCenter: Point2D = Point2D(460f, 440f),
    val defaultZoom: Float = 1.0f,
    val totalWaves: Int = GameConfig.TOTAL_WAVES,
    val missionChapter: String? = null
) {
    // Single-path backward-compatible constructor
    constructor(
        id: String,
        name: String,
        description: String,
        environmentType: EnvironmentType,
        isUnlocked: Boolean,
        starsEarned: Int,
        path: GamePath,
        basePosition: Point2D,
        decorations: List<MapDecoration> = emptyList(),
        waterPondCenter: Point2D? = null,
        waterPondRadius: Float = 0f,
        tunnelRegion: TunnelRegion? = null,
        blockedAreas: List<BlockedArea> = emptyList(),
        destructibles: List<DestructibleObject> = emptyList(),
        worldWidth: Float = 2000f,
        worldHeight: Float = 1500f,
        startingCameraCenter: Point2D = Point2D(460f, 440f),
        defaultZoom: Float = 1.0f,
        totalWaves: Int = GameConfig.TOTAL_WAVES,
        missionChapter: String? = null
    ) : this(
        id = id,
        name = name,
        description = description,
        environmentType = environmentType,
        isUnlocked = isUnlocked,
        starsEarned = starsEarned,
        paths = listOf(path),
        basePosition = basePosition,
        decorations = decorations,
        waterPondCenter = waterPondCenter,
        waterPondRadius = waterPondRadius,
        tunnelRegion = tunnelRegion,
        blockedAreas = blockedAreas,
        destructibles = destructibles,
        worldWidth = worldWidth,
        worldHeight = worldHeight,
        startingCameraCenter = startingCameraCenter,
        defaultZoom = defaultZoom,
        totalWaves = totalWaves,
        missionChapter = missionChapter
    )

    val path: GamePath get() = paths.first()
    val spawnPoints: List<Point2D> get() = paths.map { it.startPoint }

    fun getPath(index: Int): GamePath = paths.getOrElse(index % paths.size) { paths.first() }

    fun isPointInTunnel(point: Point2D): Boolean {
        val t = tunnelRegion ?: return false
        return point.x in t.boundsLeft..t.boundsRight && point.y in t.boundsTop..t.boundsBottom
    }

    /**
     * Checks if a tower can be placed at candidate position.
     * Prevents placement on ANY path, water body, solid tunnel rock, blocked areas, or base fortress.
     */
    fun canPlaceAt(candidate: Point2D, towerRadius: Float = GameConfig.TOWER_SIZE / 2f): Boolean {
        // 1. Must be inside battlefield borders
        if (candidate.x < 50f || candidate.x > worldWidth - 50f ||
            candidate.y < 50f || candidate.y > worldHeight - 50f
        ) {
            return false
        }

        // 2. Check collision against all paths on the map
        for (p in paths) {
            if (p.isPointOnPath(candidate, margin = p.pathWidth / 2f + towerRadius + 6f)) {
                return false
            }
        }

        // 3. Water collision
        if (waterPondCenter != null && waterPondRadius > 0f) {
            if (candidate.distanceTo(waterPondCenter) < (waterPondRadius + towerRadius)) {
                return false
            }
        }

        // 4. Solid cavern mountain obstruction for tunnel map
        if (tunnelRegion != null) {
            val t = tunnelRegion
            if (candidate.x in (t.boundsLeft - towerRadius)..(t.boundsRight + towerRadius) &&
                candidate.y in (t.boundsTop - towerRadius)..(t.boundsBottom + towerRadius)
            ) {
                return false
            }
        }

        // 5. Blocked terrain areas
        for (block in blockedAreas) {
            if (candidate.x in (block.left - towerRadius)..(block.right + towerRadius) &&
                candidate.y in (block.top - towerRadius)..(block.bottom + towerRadius)
            ) {
                return false
            }
        }

        // 6. Base fortress collision
        val baseLeft = basePosition.x - GameConfig.BASE_WIDTH / 2f - towerRadius
        val baseRight = basePosition.x + GameConfig.BASE_WIDTH / 2f + towerRadius
        val baseTop = basePosition.y - GameConfig.BASE_HEIGHT / 2f - towerRadius
        val baseBottom = basePosition.y + GameConfig.BASE_HEIGHT / 2f + towerRadius

        if (candidate.x in baseLeft..baseRight && candidate.y in baseTop..baseBottom) {
            return false
        }

        return true
    }

    /**
     * Prevents environment objects (trees, stones, large stones, crates) from spawning on or near roads.
     * Road clearance area: path + (roadWidth / 2) + object collision radius + extra safety margin.
     * Also verifies map boundaries, water ponds, tunnels, blocked areas, and base fortress.
     */
    fun canSpawnEnvironmentObject(
        candidate: Point2D,
        collisionRadius: Float,
        roadSafetyMargin: Float = 25f
    ): Boolean {
        // 1. Must be comfortably within world boundaries
        val borderMargin = collisionRadius + 30f
        if (candidate.x < borderMargin || candidate.x > worldWidth - borderMargin ||
            candidate.y < borderMargin || candidate.y > worldHeight - borderMargin
        ) {
            return false
        }

        // 2. Road clearance check: Distance from EVERY path segment
        // Must be >= (pathWidth / 2) + collisionRadius + roadSafetyMargin
        for (p in paths) {
            val minAllowedDist = (p.pathWidth / 2f) + collisionRadius + roadSafetyMargin
            if (p.distanceToPath(candidate) < minAllowedDist) {
                return false
            }
        }

        // 3. Water body clearance
        if (waterPondCenter != null && waterPondRadius > 0f) {
            if (candidate.distanceTo(waterPondCenter) < (waterPondRadius + collisionRadius + 15f)) {
                return false
            }
        }

        // 4. Solid cavern mountain obstruction for tunnel map
        if (tunnelRegion != null) {
            val t = tunnelRegion
            if (candidate.x in (t.boundsLeft - collisionRadius)..(t.boundsRight + collisionRadius) &&
                candidate.y in (t.boundsTop - collisionRadius)..(t.boundsBottom + collisionRadius)
            ) {
                return false
            }
        }

        // 5. Blocked terrain areas
        for (block in blockedAreas) {
            if (candidate.x in (block.left - collisionRadius)..(block.right + collisionRadius) &&
                candidate.y in (block.top - collisionRadius)..(block.bottom + collisionRadius)
            ) {
                return false
            }
        }

        // 6. Base fortress collision
        val baseClearX = GameConfig.BASE_WIDTH / 2f + collisionRadius + 20f
        val baseClearY = GameConfig.BASE_HEIGHT / 2f + collisionRadius + 20f
        if (candidate.x in (basePosition.x - baseClearX)..(basePosition.x + baseClearX) &&
            candidate.y in (basePosition.y - baseClearY)..(basePosition.y + baseClearY)
        ) {
            return false
        }

        return true
    }

    /**
     * Finds a valid natural position near desiredPos that satisfies all road clearance
     * and obstacle clearance constraints without overlapping existing objects.
     */
    fun findValidNaturalPosition(
        desiredPos: Point2D,
        collisionRadius: Float,
        existingObjects: List<DestructibleObject> = emptyList(),
        roadSafetyMargin: Float = 25f
    ): Point2D? {
        if (canSpawnEnvironmentObject(desiredPos, collisionRadius, roadSafetyMargin)) {
            val overlapsExisting = existingObjects.any {
                it.position.distanceTo(desiredPos) < (collisionRadius + it.collisionRadius + 8f)
            }
            if (!overlapsExisting) return desiredPos
        }

        // Search outward in concentric rings
        val searchAngles = floatArrayOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
        for (dist in listOf(40f, 80f, 120f, 160f, 200f)) {
            for (angleDeg in searchAngles) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val testPos = Point2D(
                    desiredPos.x + (kotlin.math.cos(rad) * dist).toFloat(),
                    desiredPos.y + (kotlin.math.sin(rad) * dist).toFloat()
                )
                if (canSpawnEnvironmentObject(testPos, collisionRadius, roadSafetyMargin)) {
                    val overlaps = existingObjects.any {
                        it.position.distanceTo(testPos) < (collisionRadius + it.collisionRadius + 8f)
                    }
                    if (!overlaps) return testPos
                }
            }
        }
        return null
    }

    companion object {
        /**
         * Ensures all trees, stones, and crates are valid DestructibleObjects positioned
         * strictly off the enemy road corridor with an extra safety margin.
         * Also strips or repositions any decorative objects that might intersect the road.
         */
        fun buildNaturalEnvironmentForMap(
            map: GameMap,
            candidateDestructibles: List<DestructibleObject>,
            candidateDecorations: List<MapDecoration>
        ): Pair<List<DestructibleObject>, List<MapDecoration>> {
            val validatedDestructibles = mutableListOf<DestructibleObject>()
            val allCandidates = mutableListOf<DestructibleObject>()
            allCandidates.addAll(candidateDestructibles)

            // Convert decorative trees and boulders to real interactable DestructibleObjects
            for (dec in candidateDecorations) {
                when (dec.type) {
                    DecorationType.OAK_TREE -> {
                        allCandidates.add(
                            DestructibleObject(
                                id = "d_tree_${dec.id}",
                                type = DestructibleType.TREE,
                                position = dec.position
                            )
                        )
                    }
                    DecorationType.PINE_TREE -> {
                        allCandidates.add(
                            DestructibleObject(
                                id = "d_pine_${dec.id}",
                                type = DestructibleType.TREE,
                                position = dec.position
                            )
                        )
                    }
                    DecorationType.BOULDER -> {
                        allCandidates.add(
                            DestructibleObject(
                                id = "d_stone_${dec.id}",
                                type = if (dec.size > 45f) DestructibleType.LARGE_STONE else DestructibleType.STONE,
                                position = dec.position
                            )
                        )
                    }
                    else -> { /* Retained as micro-decoration below */ }
                }
            }

            // Road clearance check for every destructible object
            for (cand in allCandidates) {
                val validPos = map.findValidNaturalPosition(
                    desiredPos = cand.position,
                    collisionRadius = cand.collisionRadius,
                    existingObjects = validatedDestructibles,
                    roadSafetyMargin = 25f
                )
                if (validPos != null) {
                    validatedDestructibles.add(cand.copy(position = validPos))
                }
            }

            // Filter micro-decorations (bushes, flowers, cacti, crystals) to ensure NONE overlap the road
            val validatedDecorations = mutableListOf<MapDecoration>()
            for (dec in candidateDecorations) {
                if (dec.type == DecorationType.OAK_TREE || dec.type == DecorationType.PINE_TREE || dec.type == DecorationType.BOULDER) {
                    continue
                }
                val roadMargin = (dec.size / 2f) + 12f
                val overlapsRoad = map.paths.any { p ->
                    p.distanceToPath(dec.position) < ((p.pathWidth / 2f) + roadMargin)
                }
                if (!overlapsRoad) {
                    validatedDecorations.add(dec)
                }
            }

            return validatedDestructibles to validatedDecorations
        }

        fun getPresetMaps(
            isMapUnlocked: (String) -> Boolean = { it == "green_valley" || it == "map_1_valley" },
            getStars: (String) -> Int = { 0 }
        ): List<GameMap> {
            return listOf(
                createGreenValleyMap(
                    isUnlocked = isMapUnlocked("green_valley") || isMapUnlocked("map_1_valley"),
                    stars = maxOf(getStars("green_valley"), getStars("map_1_valley"))
                ),
                createDesertOutpostMap(
                    isUnlocked = isMapUnlocked("desert_outpost") || isMapUnlocked("map_2_canyon"),
                    stars = maxOf(getStars("desert_outpost"), getStars("map_2_canyon"))
                ),
                createForestPassMap(
                    isUnlocked = isMapUnlocked("forest_pass"),
                    stars = getStars("forest_pass")
                ),
                createSplitRoutesMap(
                    isUnlocked = isMapUnlocked("split_routes") || isMapUnlocked("map_4_split"),
                    stars = maxOf(getStars("split_routes"), getStars("map_4_split"))
                ),
                createCanyonTunnelMap(
                    isUnlocked = isMapUnlocked("canyon_tunnel") || isMapUnlocked("map_4_tunnel"),
                    stars = maxOf(getStars("canyon_tunnel"), getStars("map_4_tunnel"))
                ),
                createTheCrossingMap(
                    isUnlocked = isMapUnlocked("the_crossing") || isMapUnlocked("map_6_crossing"),
                    stars = maxOf(getStars("the_crossing"), getStars("map_6_crossing"))
                ),
                createDragonsCoilMap(
                    isUnlocked = isMapUnlocked("map_5_loop") || isMapUnlocked("dragons_coil"),
                    stars = maxOf(getStars("map_5_loop"), getStars("dragons_coil"))
                ),
                // Snow World Campaign (5 Levels)
                createSnowOutpostMap(
                    isUnlocked = isMapUnlocked("snow_outpost"),
                    stars = getStars("snow_outpost")
                ),
                createFrozenValleyMap(
                    isUnlocked = isMapUnlocked("frozen_valley") || isMapUnlocked("snow_valley") || isMapUnlocked("map_8_snow"),
                    stars = maxOf(getStars("frozen_valley"), getStars("snow_valley"), getStars("map_8_snow"))
                ),
                createIceMountainMap(
                    isUnlocked = isMapUnlocked("ice_mountain"),
                    stars = getStars("ice_mountain")
                ),
                createFrozenFortressMap(
                    isUnlocked = isMapUnlocked("frozen_fortress"),
                    stars = getStars("frozen_fortress")
                ),
                createArcticBaseMap(
                    isUnlocked = isMapUnlocked("arctic_base"),
                    stars = getStars("arctic_base")
                ),
                createNightFortressMap(
                    isUnlocked = isMapUnlocked("night_fortress") || isMapUnlocked("map_9_night"),
                    stars = maxOf(getStars("night_fortress"), getStars("map_9_night"))
                )
            )
        }

        // ==========================================
        // MAP 1 — STRAIGHT / SIMPLE (Verdant Valley)
        // Gentle curving path, plenty of open building room, lake, and extended valley
        // ==========================================
        fun createGreenValleyMap(isUnlocked: Boolean = true, stars: Int = 0): GameMap {
            val pathWaypoints = listOf(
                Point2D(-50f, 220f),
                Point2D(340f, 220f),
                Point2D(500f, 340f),
                Point2D(500f, 660f),
                Point2D(360f, 840f),
                Point2D(360f, 1060f),
                Point2D(750f, 1060f),
                Point2D(1100f, 820f),
                Point2D(1500f, 820f),
                Point2D(1500f, 1350f),
                Point2D(1950f, 1350f)
            )
            val path = GamePath(id = "valley_main", waypoints = pathWaypoints, pathWidth = GameConfig.PATH_WIDTH)
            val basePos = Point2D(1950f, 1350f)
            val pondCenter = Point2D(1050f, 520f)
            val pondRadius = 90f

            val decor = listOf(
                MapDecoration("t1", DecorationType.OAK_TREE, Point2D(120f, 90f), size = 64f),
                MapDecoration("t2", DecorationType.PINE_TREE, Point2D(720f, 110f), size = 56f),
                MapDecoration("t3", DecorationType.OAK_TREE, Point2D(160f, 400f), size = 60f),
                MapDecoration("t4", DecorationType.PINE_TREE, Point2D(880f, 340f), size = 58f),
                MapDecoration("t5", DecorationType.OAK_TREE, Point2D(140f, 920f), size = 64f),
                MapDecoration("t6", DecorationType.PINE_TREE, Point2D(540f, 920f), size = 54f),
                MapDecoration("t7", DecorationType.OAK_TREE, Point2D(120f, 1240f), size = 66f),
                MapDecoration("t8", DecorationType.PINE_TREE, Point2D(1150f, 220f), size = 62f),
                MapDecoration("t9", DecorationType.OAK_TREE, Point2D(1450f, 320f), size = 68f),
                MapDecoration("t10", DecorationType.PINE_TREE, Point2D(1750f, 420f), size = 58f),
                MapDecoration("t11", DecorationType.OAK_TREE, Point2D(1150f, 1040f), size = 64f),
                MapDecoration("t12", DecorationType.PINE_TREE, Point2D(1550f, 980f), size = 60f),
                MapDecoration("t13", DecorationType.OAK_TREE, Point2D(1750f, 1400f), size = 66f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(220f, 90f), size = 32f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(320f, 620f), size = 30f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(680f, 760f), size = 34f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(1250f, 660f), size = 42f),
                MapDecoration("r5", DecorationType.BOULDER, Point2D(1650f, 1250f), size = 38f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(420f, 140f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(220f, 920f), size = 28f),
                MapDecoration("b3", DecorationType.BUSH, Point2D(1200f, 880f), size = 26f),
                MapDecoration("b4", DecorationType.BUSH, Point2D(1720f, 1140f), size = 28f),
                MapDecoration("f1", DecorationType.FLOWER_PATCH, Point2D(260f, 320f), size = 24f),
                MapDecoration("f2", DecorationType.FLOWER_PATCH, Point2D(700f, 520f), size = 22f),
                MapDecoration("f3", DecorationType.FLOWER_PATCH, Point2D(820f, 1060f), size = 24f),
                MapDecoration("f4", DecorationType.FLOWER_PATCH, Point2D(1480f, 740f), size = 24f)
            )

            val destructiblesList = listOf(
                DestructibleObject("d_tree_1", DestructibleType.OAK_TREE, Point2D(360f, 400f)),
                DestructibleObject("d_rock_1", DestructibleType.SMALL_STONE, Point2D(650f, 480f)),
                DestructibleObject("d_crate_1", DestructibleType.WOODEN_CRATE, Point2D(880f, 650f)),
                DestructibleObject("d_rock_2", DestructibleType.LARGE_BOULDER, Point2D(1280f, 1000f)),
                DestructibleObject("d_crate_2", DestructibleType.WOODEN_CRATE, Point2D(420f, 740f)),
                DestructibleObject("d_tree_2", DestructibleType.PINE_TREE, Point2D(950f, 920f)),
                DestructibleObject("d_crate_3", DestructibleType.WOODEN_CRATE, Point2D(1350f, 680f))
            )

            val map = GameMap(
                id = "green_valley",
                name = "Verdant Valley",
                description = "Expansive open valley with winding roads, tranquil lake, and fortified ridge. Ample strategic building room.",
                environmentType = EnvironmentType.GREEN_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructiblesList,
                worldWidth = 2200f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(650f, 500f),
                defaultZoom = 0.95f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = destructiblesList,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ==========================================
        // MAP 2 — ZIGZAG (Switchback Canyon)
        // 4 sharp switchback turns through rocky desert
        // ==========================================
        fun createDesertOutpostMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(-50f, 220f),
                Point2D(1200f, 220f),
                Point2D(1200f, 580f),
                Point2D(300f, 580f),
                Point2D(300f, 950f),
                Point2D(1500f, 950f),
                Point2D(1500f, 1300f),
                Point2D(700f, 1300f),
                Point2D(700f, 1450f),
                Point2D(2350f, 1450f)
            )
            val path = GamePath(id = "canyon_zigzag", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val basePos = Point2D(2350f, 1450f)

            val decor = listOf(
                MapDecoration("c1", DecorationType.CACTUS, Point2D(120f, 90f), size = 36f),
                MapDecoration("c2", DecorationType.CACTUS, Point2D(1020f, 110f), size = 42f),
                MapDecoration("c3", DecorationType.CACTUS, Point2D(110f, 620f), size = 38f),
                MapDecoration("c4", DecorationType.CACTUS, Point2D(1280f, 920f), size = 40f),
                MapDecoration("c5", DecorationType.CACTUS, Point2D(1680f, 1140f), size = 44f),
                MapDecoration("c6", DecorationType.CACTUS, Point2D(2050f, 1200f), size = 42f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(550f, 320f), size = 48f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(700f, 620f), size = 52f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(850f, 920f), size = 50f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(350f, 1220f), size = 46f),
                MapDecoration("r5", DecorationType.BOULDER, Point2D(1450f, 1220f), size = 54f),
                MapDecoration("r6", DecorationType.BOULDER, Point2D(2100f, 1400f), size = 50f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(400f, 320f), size = 24f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(800f, 620f), size = 26f),
                MapDecoration("b3", DecorationType.BUSH, Point2D(1350f, 1250f), size = 24f)
            )

            val desertDestructibles = listOf(
                DestructibleObject("d_des_rock_1", DestructibleType.LARGE_BOULDER, Point2D(650f, 400f)),
                DestructibleObject("d_des_stone_1", DestructibleType.SMALL_STONE, Point2D(950f, 420f)),
                DestructibleObject("d_des_crate_1", DestructibleType.WOODEN_CRATE, Point2D(480f, 760f)),
                DestructibleObject("d_des_stone_2", DestructibleType.SMALL_STONE, Point2D(1120f, 780f)),
                DestructibleObject("d_des_rock_2", DestructibleType.LARGE_BOULDER, Point2D(800f, 1120f)),
                DestructibleObject("d_des_crate_2", DestructibleType.WOODEN_CRATE, Point2D(1250f, 1140f))
            )

            val map = GameMap(
                id = "desert_outpost",
                name = "Switchback Canyon",
                description = "Dramatic switchback zigzags across wide sandstone mesas. Hairpin turns offer multi-lane firing coverage.",
                environmentType = EnvironmentType.DESERT_CANYON,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                destructibles = desertDestructibles,
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.90f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = desertDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ==========================================
        // MAP 3 — FOREST PASS (Winding Forest Road)
        // Dense woodland, serpent path with multiple turns
        // ==========================================
        fun createForestPassMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(-50f, 240f),
                Point2D(400f, 240f),
                Point2D(400f, 600f),
                Point2D(1000f, 600f),
                Point2D(1000f, 1000f),
                Point2D(550f, 1000f),
                Point2D(550f, 1400f),
                Point2D(1400f, 1400f),
                Point2D(1400f, 900f),
                Point2D(1950f, 900f),
                Point2D(1950f, 1600f),
                Point2D(2500f, 1600f)
            )
            val path = GamePath(id = "forest_winding", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val basePos = Point2D(2500f, 1600f)

            val decor = listOf(
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(120f, 90f), size = 62f),
                MapDecoration("t2", DecorationType.OAK_TREE, Point2D(950f, 120f), size = 66f),
                MapDecoration("t3", DecorationType.PINE_TREE, Point2D(160f, 360f), size = 58f),
                MapDecoration("t4", DecorationType.OAK_TREE, Point2D(550f, 340f), size = 64f),
                MapDecoration("t5", DecorationType.PINE_TREE, Point2D(650f, 660f), size = 60f),
                MapDecoration("t6", DecorationType.OAK_TREE, Point2D(980f, 660f), size = 62f),
                MapDecoration("t7", DecorationType.PINE_TREE, Point2D(280f, 980f), size = 56f),
                MapDecoration("t8", DecorationType.OAK_TREE, Point2D(680f, 980f), size = 64f),
                MapDecoration("t9", DecorationType.PINE_TREE, Point2D(1300f, 580f), size = 62f),
                MapDecoration("t10", DecorationType.OAK_TREE, Point2D(1700f, 620f), size = 68f),
                MapDecoration("t11", DecorationType.PINE_TREE, Point2D(2200f, 700f), size = 64f),
                MapDecoration("t12", DecorationType.OAK_TREE, Point2D(2300f, 1400f), size = 66f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(520f, 200f), size = 36f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(160f, 680f), size = 34f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(1320f, 980f), size = 44f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(180f, 500f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(600f, 820f), size = 24f),
                MapDecoration("f1", DecorationType.FLOWER_PATCH, Point2D(400f, 660f), size = 24f)
            )

            val forestDestructibles = listOf(
                DestructibleObject("d_for_tree_1", DestructibleType.OAK_TREE, Point2D(250f, 420f)),
                DestructibleObject("d_for_rock_1", DestructibleType.SMALL_STONE, Point2D(700f, 420f)),
                DestructibleObject("d_for_crate_1", DestructibleType.WOODEN_CRATE, Point2D(800f, 800f)),
                DestructibleObject("d_for_tree_2", DestructibleType.PINE_TREE, Point2D(350f, 1200f)),
                DestructibleObject("d_for_rock_2", DestructibleType.LARGE_BOULDER, Point2D(1000f, 1200f)),
                DestructibleObject("d_for_crate_2", DestructibleType.WOODEN_CRATE, Point2D(1680f, 1200f))
            )

            val map = GameMap(
                id = "forest_pass",
                name = "Forest Pass",
                description = "Deep forest serpent trail through ancient woodlands. Choke points and tight bends reward precision defense.",
                environmentType = EnvironmentType.FOREST_CROSSROADS,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                destructibles = forestDestructibles,
                worldWidth = 2800f,
                worldHeight = 1900f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.85f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = forestDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ==========================================
        // MAP 4 — SPLIT ROUTES (Dual Flank Invasions)
        // Two independent routes: Path North & Path South
        // ==========================================
        fun createSplitRoutesMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2700f, 1000f)
            val pathNorth = GamePath(
                id = "split_north",
                waypoints = listOf(
                    Point2D(-50f, 300f),
                    Point2D(600f, 300f),
                    Point2D(1100f, 450f),
                    Point2D(1600f, 650f),
                    Point2D(2100f, 850f),
                    Point2D(2700f, 1000f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )

            val pathSouth = GamePath(
                id = "split_south",
                waypoints = listOf(
                    Point2D(-50f, 1700f),
                    Point2D(600f, 1700f),
                    Point2D(1100f, 1550f),
                    Point2D(1600f, 1350f),
                    Point2D(2100f, 1150f),
                    Point2D(2700f, 1000f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )

            val decor = listOf(
                MapDecoration("sp1", DecorationType.SIGNPOST, Point2D(520f, 740f), size = 32f),
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(120f, 90f), size = 60f),
                MapDecoration("t2", DecorationType.OAK_TREE, Point2D(950f, 120f), size = 66f),
                MapDecoration("t3", DecorationType.PINE_TREE, Point2D(180f, 540f), size = 58f),
                MapDecoration("t4", DecorationType.OAK_TREE, Point2D(520f, 580f), size = 64f),
                MapDecoration("t5", DecorationType.PINE_TREE, Point2D(760f, 720f), size = 62f),
                MapDecoration("t6", DecorationType.OAK_TREE, Point2D(150f, 1400f), size = 60f),
                MapDecoration("t7", DecorationType.PINE_TREE, Point2D(2400f, 600f), size = 64f),
                MapDecoration("t8", DecorationType.OAK_TREE, Point2D(2400f, 1400f), size = 66f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(600f, 420f), size = 42f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(200f, 980f), size = 36f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(320f, 360f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(720f, 920f), size = 24f),
                MapDecoration("f1", DecorationType.FLOWER_PATCH, Point2D(420f, 720f), size = 24f)
            )

            val splitDestructibles = listOf(
                DestructibleObject("d_spl_tree_1", DestructibleType.TREE, Point2D(800f, 1000f)),
                DestructibleObject("d_spl_tree_2", DestructibleType.TREE, Point2D(860f, 1040f)),
                DestructibleObject("d_spl_rock_1", DestructibleType.LARGE_STONE, Point2D(1400f, 1000f)),
                DestructibleObject("d_spl_stone_1", DestructibleType.STONE, Point2D(1460f, 960f)),
                DestructibleObject("d_spl_crate_1", DestructibleType.WOODEN_CRATE, Point2D(1800f, 1000f)),
                DestructibleObject("d_spl_crate_2", DestructibleType.WOODEN_CRATE, Point2D(2200f, 1000f))
            )

            val map = GameMap(
                id = "split_routes",
                name = "Split Routes",
                description = "Dual invasion flanks across a massive battlefield! Northern ridge and southern marsh converge at the citadel base.",
                environmentType = EnvironmentType.GREEN_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                decorations = decor,
                destructibles = splitDestructibles,
                worldWidth = 3000f,
                worldHeight = 2000f,
                startingCameraCenter = Point2D(700f, 1000f),
                defaultZoom = 0.80f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = splitDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // Backward-compatible alias
        fun createCrossroadsMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createSplitRoutesMap(isUnlocked, stars)

        // ==========================================
        // MAP 5 — TUNNEL / CANYON (Obsidian Canyon)
        // Road passes under massive solid mountain cavern
        // ==========================================
        fun createCanyonTunnelMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(220f, -50f),
                Point2D(220f, 300f),
                Point2D(620f, 300f),
                Point2D(620f, 440f),  // Tunnel entry archway
                Point2D(620f, 840f),  // Underground cavern passage
                Point2D(620f, 940f),  // Tunnel exit archway
                Point2D(340f, 1100f),
                Point2D(340f, 1340f),
                Point2D(900f, 1340f),
                Point2D(1550f, 1250f),
                Point2D(2100f, 1250f)
            )
            val path = GamePath(id = "tunnel_main", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val basePos = Point2D(2100f, 1250f)

            val tunnel = TunnelRegion(
                id = "obsidian_tunnel",
                boundsLeft = 460f,
                boundsTop = 440f,
                boundsRight = 780f,
                boundsBottom = 880f,
                entrance = Point2D(620f, 440f),
                exit = Point2D(620f, 880f)
            )

            val decor = listOf(
                MapDecoration("cry1", DecorationType.CRYSTAL, Point2D(430f, 410f), size = 32f),
                MapDecoration("cry2", DecorationType.CRYSTAL, Point2D(810f, 410f), size = 34f),
                MapDecoration("cry3", DecorationType.CRYSTAL, Point2D(430f, 910f), size = 30f),
                MapDecoration("cry4", DecorationType.CRYSTAL, Point2D(810f, 910f), size = 32f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(120f, 180f), size = 46f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(880f, 180f), size = 52f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(120f, 740f), size = 48f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(440f, 1180f), size = 42f),
                MapDecoration("r5", DecorationType.BOULDER, Point2D(1100f, 1260f), size = 50f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(380f, 220f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(180f, 1180f), size = 24f)
            )

            val tunnelDestructibles = listOf(
                DestructibleObject("d_tun_rock_1", DestructibleType.LARGE_STONE, Point2D(250f, 600f)),
                DestructibleObject("d_tun_stone_1", DestructibleType.STONE, Point2D(310f, 640f)),
                DestructibleObject("d_tun_crate_1", DestructibleType.WOODEN_CRATE, Point2D(200f, 850f)),
                DestructibleObject("d_tun_tree_1", DestructibleType.TREE, Point2D(1150f, 1000f)),
                DestructibleObject("d_tun_stone_2", DestructibleType.STONE, Point2D(1220f, 1050f)),
                DestructibleObject("d_tun_crate_2", DestructibleType.WOODEN_CRATE, Point2D(1650f, 1050f))
            )

            val map = GameMap(
                id = "canyon_tunnel",
                name = "Obsidian Canyon",
                description = "Rugged canyon plunging into a granite mountain tunnel. Enemies march subterranean beneath solid rock.",
                environmentType = EnvironmentType.OBSIDIAN_TUNNEL,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                destructibles = tunnelDestructibles,
                tunnelRegion = tunnel,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.90f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = tunnelDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // Backward-compatible alias
        fun createObsidianGrottoMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createCanyonTunnelMap(isUnlocked, stars)

        // ==========================================
        // MAP 6 — THE CROSSING (Intersecting Multi-Angle Routes)
        // ==========================================
        fun createTheCrossingMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2300f, 1300f)
            val routeWest = GamePath(
                id = "crossing_west",
                waypoints = listOf(
                    Point2D(-50f, 420f),
                    Point2D(450f, 420f),
                    Point2D(1000f, 750f),  // Intersection point
                    Point2D(1500f, 1050f),
                    Point2D(1900f, 1300f),
                    Point2D(2300f, 1300f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )

            val routeNorth = GamePath(
                id = "crossing_north",
                waypoints = listOf(
                    Point2D(1000f, -50f),
                    Point2D(1000f, 400f),
                    Point2D(1000f, 750f),  // Intersection point
                    Point2D(650f, 1100f),
                    Point2D(650f, 1500f),
                    Point2D(1450f, 1500f),
                    Point2D(2300f, 1300f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )

            val decor = listOf(
                MapDecoration("sp1", DecorationType.SIGNPOST, Point2D(780f, 600f), size = 32f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(280f, 260f), size = 44f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(1020f, 620f), size = 46f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(220f, 720f), size = 40f),
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(140f, 100f), size = 60f),
                MapDecoration("t2", DecorationType.OAK_TREE, Point2D(600f, 120f), size = 64f),
                MapDecoration("t3", DecorationType.PINE_TREE, Point2D(1150f, 180f), size = 58f),
                MapDecoration("t4", DecorationType.OAK_TREE, Point2D(420f, 920f), size = 62f),
                MapDecoration("b1", DecorationType.BUSH, Point2D(480f, 500f), size = 26f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(980f, 500f), size = 26f)
            )

            val crossingDestructibles = listOf(
                DestructibleObject("d_cro_tree_1", DestructibleType.TREE, Point2D(300f, 650f)),
                DestructibleObject("d_cro_tree_2", DestructibleType.TREE, Point2D(360f, 680f)),
                DestructibleObject("d_cro_stone_1", DestructibleType.STONE, Point2D(750f, 400f)),
                DestructibleObject("d_cro_rock_1", DestructibleType.LARGE_STONE, Point2D(1350f, 650f)),
                DestructibleObject("d_cro_crate_1", DestructibleType.WOODEN_CRATE, Point2D(1100f, 1100f)),
                DestructibleObject("d_cro_crate_2", DestructibleType.WOODEN_CRATE, Point2D(1650f, 1300f))
            )

            val map = GameMap(
                id = "the_crossing",
                name = "The Crossing",
                description = "Intersecting desert canyon routes meet at a grand crossroads. Strategic central placement attacks multi-lane assaults.",
                environmentType = EnvironmentType.DESERT_CANYON,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(routeWest, routeNorth),
                basePosition = basePos,
                decorations = decor,
                destructibles = crossingDestructibles,
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(1000f, 750f),
                defaultZoom = 0.90f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = crossingDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ==========================================
        // MAP 7 — LOOP / CURVED PATH (Dragon's Coil)
        // ==========================================
        fun createDragonsCoilMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(1200f, 950f)
            val waypoints = listOf(
                Point2D(200f, -50f),
                Point2D(200f, 260f),
                Point2D(2000f, 260f),
                Point2D(2000f, 1400f),
                Point2D(450f, 1400f),
                Point2D(450f, 650f),
                Point2D(1500f, 650f),
                Point2D(1500f, 950f),
                Point2D(1200f, 950f)
            )
            val path = GamePath(id = "dragon_loop", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)

            val decor = listOf(
                MapDecoration("r1", DecorationType.BOULDER, Point2D(560f, 120f), size = 46f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(1720f, 600f), size = 52f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(120f, 780f), size = 42f),
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(400f, 420f), size = 58f),
                MapDecoration("t2", DecorationType.PINE_TREE, Point2D(1050f, 420f), size = 60f),
                MapDecoration("cry1", DecorationType.CRYSTAL, Point2D(650f, 400f), size = 32f),
                MapDecoration("cry2", DecorationType.CRYSTAL, Point2D(750f, 850f), size = 32f)
            )

            val coilDestructibles = listOf(
                DestructibleObject("d_coil_rock_1", DestructibleType.LARGE_STONE, Point2D(1000f, 800f)),
                DestructibleObject("d_coil_stone_1", DestructibleType.STONE, Point2D(1080f, 820f)),
                DestructibleObject("d_coil_tree_1", DestructibleType.TREE, Point2D(800f, 450f)),
                DestructibleObject("d_coil_tree_2", DestructibleType.TREE, Point2D(860f, 480f)),
                DestructibleObject("d_coil_crate_1", DestructibleType.WOODEN_CRATE, Point2D(1350f, 800f)),
                DestructibleObject("d_coil_crate_2", DestructibleType.WOODEN_CRATE, Point2D(700f, 1150f))
            )

            val map = GameMap(
                id = "map_5_loop",
                name = "Dragon's Coil",
                description = "Massive spiral loop encircling an elevated high-ground plateau. Center towers fire repeatedly as enemies wind inward.",
                environmentType = EnvironmentType.DRAGON_COIL,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                destructibles = coilDestructibles,
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(1200f, 950f),
                defaultZoom = 0.90f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = coilDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // Aliases for Map 4 and Map 5
        fun createObsidianTunnelMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createObsidianGrottoMap(isUnlocked, stars)

        fun createDragonCoilMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createDragonsCoilMap(isUnlocked, stars)

        // ==========================================
        // SNOW WORLD CAMPAIGN PROGRESSION (5 UNIQUE LEVELS)
        // ==========================================

        // ------------------------------------------
        // SNOW LEVEL 1: SNOW OUTPOST (12 Waves)
        // Frontier military outpost in the snowbound valley foothills.
        // Single winding pass with strategic defense chokepoints.
        // ------------------------------------------
        fun createSnowOutpostMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2050f, 1300f)
            val waypoints = listOf(
                Point2D(-60f, 400f),
                Point2D(650f, 400f),
                Point2D(950f, 650f),
                Point2D(950f, 1050f),
                Point2D(1500f, 1050f),
                Point2D(1750f, 1300f),
                Point2D(2050f, 1300f)
            )
            val path = GamePath(id = "snow_outpost_pass", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val pondCenter = Point2D(1400f, 550f)
            val pondRadius = 110f

            val decor = listOf(
                MapDecoration("so_sp1", DecorationType.SNOW_PILE, Point2D(350f, 280f), size = 34f),
                MapDecoration("so_sp2", DecorationType.SNOW_PILE, Point2D(800f, 260f), size = 38f),
                MapDecoration("so_sp3", DecorationType.SNOW_PILE, Point2D(1200f, 850f), size = 42f),
                MapDecoration("so_sp4", DecorationType.SNOW_PILE, Point2D(1800f, 900f), size = 40f),
                MapDecoration("so_fb1", DecorationType.FROZEN_BUSH, Point2D(500f, 520f), size = 28f),
                MapDecoration("so_fb2", DecorationType.FROZEN_BUSH, Point2D(1150f, 1200f), size = 30f),
                MapDecoration("so_fb3", DecorationType.FROZEN_BUSH, Point2D(1600f, 850f), size = 28f),
                MapDecoration("so_r1", DecorationType.BOULDER, Point2D(400f, 600f), size = 46f),
                MapDecoration("so_r2", DecorationType.BOULDER, Point2D(1250f, 1250f), size = 52f),
                MapDecoration("so_r3", DecorationType.BOULDER, Point2D(1850f, 650f), size = 50f),
                MapDecoration("so_cry1", DecorationType.CRYSTAL, Point2D(1400f, 400f), size = 26f),
                MapDecoration("so_t1", DecorationType.PINE_TREE, Point2D(250f, 180f), size = 62f),
                MapDecoration("so_t2", DecorationType.PINE_TREE, Point2D(850f, 160f), size = 64f),
                MapDecoration("so_t3", DecorationType.PINE_TREE, Point2D(1700f, 250f), size = 60f),
                MapDecoration("so_t4", DecorationType.PINE_TREE, Point2D(650f, 1250f), size = 64f),
                MapDecoration("so_t5", DecorationType.PINE_TREE, Point2D(1400f, 1350f), size = 58f)
            )

            val destructibles = listOf(
                DestructibleObject("so_d_rock_1", DestructibleType.LARGE_STONE, Point2D(400f, 600f)),
                DestructibleObject("so_d_rock_2", DestructibleType.STONE, Point2D(1250f, 1250f)),
                DestructibleObject("so_d_tree_1", DestructibleType.PINE_TREE, Point2D(250f, 180f)),
                DestructibleObject("so_d_tree_2", DestructibleType.PINE_TREE, Point2D(850f, 160f)),
                DestructibleObject("so_d_tree_3", DestructibleType.PINE_TREE, Point2D(650f, 1250f)),
                DestructibleObject("so_d_crate_1", DestructibleType.WOODEN_CRATE, Point2D(800f, 850f)),
                DestructibleObject("so_d_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1600f, 1250f))
            )

            val map = GameMap(
                id = "snow_outpost",
                name = "Snow Outpost",
                description = "Frontier military garrison in the snowbound valley foothills. Establish defensive chokepoints along the frozen mountain road.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1000f, 850f),
                defaultZoom = 0.88f,
                totalWaves = 12,
                missionChapter = "SNOW CAMPAIGN • MISSION 1"
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = destructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ------------------------------------------
        // SNOW LEVEL 2: FROZEN VALLEY (18 Waves)
        // Sweeping mountain snow pass with frozen switchbacks and glacial lake.
        // ------------------------------------------
        fun createFrozenValleyMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2350f, 1550f)
            val waypoints = listOf(
                Point2D(-60f, 320f),
                Point2D(650f, 320f),
                Point2D(650f, 750f),
                Point2D(1250f, 750f),
                Point2D(1250f, 380f),
                Point2D(2050f, 380f),
                Point2D(2050f, 1150f),
                Point2D(1100f, 1150f),
                Point2D(1100f, 1550f),
                Point2D(2350f, 1550f)
            )
            val path = GamePath(id = "frozen_valley_pass", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val pondCenter = Point2D(1650f, 750f)
            val pondRadius = 130f

            val decor = listOf(
                MapDecoration("sp1", DecorationType.SNOW_PILE, Point2D(350f, 220f), size = 36f),
                MapDecoration("sp2", DecorationType.SNOW_PILE, Point2D(950f, 240f), size = 42f),
                MapDecoration("sp3", DecorationType.SNOW_PILE, Point2D(1700f, 280f), size = 38f),
                MapDecoration("sp4", DecorationType.SNOW_PILE, Point2D(520f, 850f), size = 40f),
                MapDecoration("sp5", DecorationType.SNOW_PILE, Point2D(1450f, 1050f), size = 46f),
                MapDecoration("sp6", DecorationType.SNOW_PILE, Point2D(1850f, 1420f), size = 44f),
                MapDecoration("fb1", DecorationType.FROZEN_BUSH, Point2D(480f, 420f), size = 28f),
                MapDecoration("fb2", DecorationType.FROZEN_BUSH, Point2D(820f, 860f), size = 30f),
                MapDecoration("fb3", DecorationType.FROZEN_BUSH, Point2D(1420f, 520f), size = 32f),
                MapDecoration("fb4", DecorationType.FROZEN_BUSH, Point2D(1880f, 960f), size = 30f),
                MapDecoration("fb5", DecorationType.FROZEN_BUSH, Point2D(950f, 1380f), size = 28f),
                MapDecoration("cry1", DecorationType.CRYSTAL, Point2D(1650f, 570f), size = 26f),
                MapDecoration("cry2", DecorationType.CRYSTAL, Point2D(1650f, 930f), size = 26f),
                MapDecoration("r1", DecorationType.BOULDER, Point2D(380f, 540f), size = 48f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(950f, 540f), size = 52f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(1550f, 1350f), size = 56f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(820f, 1020f), size = 44f),
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(300f, 160f), size = 62f),
                MapDecoration("t2", DecorationType.PINE_TREE, Point2D(950f, 160f), size = 64f),
                MapDecoration("t3", DecorationType.PINE_TREE, Point2D(1600f, 200f), size = 58f),
                MapDecoration("t4", DecorationType.PINE_TREE, Point2D(2350f, 320f), size = 66f),
                MapDecoration("t5", DecorationType.PINE_TREE, Point2D(2350f, 750f), size = 60f),
                MapDecoration("t6", DecorationType.PINE_TREE, Point2D(600f, 1380f), size = 60f),
                MapDecoration("t7", DecorationType.PINE_TREE, Point2D(1550f, 960f), size = 56f)
            )

            val snowDestructibles = listOf(
                DestructibleObject("d_snow_rock_1", DestructibleType.LARGE_STONE, Point2D(950f, 540f)),
                DestructibleObject("d_snow_rock_2", DestructibleType.STONE, Point2D(380f, 540f)),
                DestructibleObject("d_snow_rock_3", DestructibleType.LARGE_BOULDER, Point2D(1550f, 1350f)),
                DestructibleObject("d_snow_tree_1", DestructibleType.PINE_TREE, Point2D(300f, 160f)),
                DestructibleObject("d_snow_tree_2", DestructibleType.PINE_TREE, Point2D(950f, 160f)),
                DestructibleObject("d_snow_tree_3", DestructibleType.PINE_TREE, Point2D(1600f, 200f)),
                DestructibleObject("d_snow_tree_4", DestructibleType.PINE_TREE, Point2D(600f, 1380f)),
                DestructibleObject("d_snow_tree_5", DestructibleType.PINE_TREE, Point2D(1550f, 960f)),
                DestructibleObject("d_snow_crate_1", DestructibleType.WOODEN_CRATE, Point2D(950f, 920f)),
                DestructibleObject("d_snow_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1700f, 1350f))
            )

            val map = GameMap(
                id = "frozen_valley",
                name = "Frozen Valley",
                description = "Vast snow-covered mountain battlefield with icy switchbacks, frozen glacial ponds, and snow-laden pine groves. Formidable winter defense.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = snowDestructibles,
                worldWidth = 2800f,
                worldHeight = 1800f,
                startingCameraCenter = Point2D(950f, 750f),
                defaultZoom = 0.85f,
                totalWaves = 18,
                missionChapter = "SNOW CAMPAIGN • MISSION 2"
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = snowDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // Backward compatibility alias
        fun createSnowValleyMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createFrozenValleyMap(isUnlocked, stars)

        // ------------------------------------------
        // SNOW LEVEL 3: ICE MOUNTAIN (24 Waves)
        // Perilous hairpin zigzag climbing from mountain base to summit stronghold.
        // Elevated cliff plateaus afford multi-lane defensive coverage.
        // ------------------------------------------
        fun createIceMountainMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 350f)
            val waypoints = listOf(
                Point2D(-60f, 1600f),
                Point2D(750f, 1600f),
                Point2D(750f, 1150f),
                Point2D(1950f, 1150f),
                Point2D(1950f, 750f),
                Point2D(800f, 750f),
                Point2D(800f, 350f),
                Point2D(2200f, 350f)
            )
            val path = GamePath(id = "ice_mountain_pass", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)
            val pondCenter = Point2D(1400f, 950f)
            val pondRadius = 110f

            val decor = listOf(
                MapDecoration("im_sp1", DecorationType.SNOW_PILE, Point2D(400f, 1450f), size = 38f),
                MapDecoration("im_sp2", DecorationType.SNOW_PILE, Point2D(1350f, 1350f), size = 42f),
                MapDecoration("im_sp3", DecorationType.SNOW_PILE, Point2D(1400f, 550f), size = 44f),
                MapDecoration("im_sp4", DecorationType.SNOW_PILE, Point2D(1800f, 200f), size = 40f),
                MapDecoration("im_fb1", DecorationType.FROZEN_BUSH, Point2D(550f, 1750f), size = 30f),
                MapDecoration("im_fb2", DecorationType.FROZEN_BUSH, Point2D(1050f, 950f), size = 32f),
                MapDecoration("im_fb3", DecorationType.FROZEN_BUSH, Point2D(1650f, 550f), size = 30f),
                MapDecoration("im_r1", DecorationType.BOULDER, Point2D(400f, 950f), size = 54f),
                MapDecoration("im_r2", DecorationType.BOULDER, Point2D(1050f, 1350f), size = 52f),
                MapDecoration("im_r3", DecorationType.BOULDER, Point2D(2150f, 950f), size = 56f),
                MapDecoration("im_r4", DecorationType.BOULDER, Point2D(1350f, 200f), size = 50f),
                MapDecoration("im_cry1", DecorationType.CRYSTAL, Point2D(1400f, 800f), size = 28f),
                MapDecoration("im_cry2", DecorationType.CRYSTAL, Point2D(1400f, 1100f), size = 28f),
                MapDecoration("im_t1", DecorationType.PINE_TREE, Point2D(250f, 1350f), size = 64f),
                MapDecoration("im_t2", DecorationType.PINE_TREE, Point2D(1750f, 1350f), size = 66f),
                MapDecoration("im_t3", DecorationType.PINE_TREE, Point2D(1050f, 550f), size = 62f),
                MapDecoration("im_t4", DecorationType.PINE_TREE, Point2D(500f, 200f), size = 64f),
                MapDecoration("im_t5", DecorationType.PINE_TREE, Point2D(2400f, 550f), size = 66f)
            )

            val destructibles = listOf(
                DestructibleObject("im_d_rock_1", DestructibleType.LARGE_STONE, Point2D(400f, 950f)),
                DestructibleObject("im_d_rock_2", DestructibleType.LARGE_BOULDER, Point2D(2150f, 950f)),
                DestructibleObject("im_d_tree_1", DestructibleType.PINE_TREE, Point2D(250f, 1350f)),
                DestructibleObject("im_d_tree_2", DestructibleType.PINE_TREE, Point2D(1750f, 1350f)),
                DestructibleObject("im_d_tree_3", DestructibleType.PINE_TREE, Point2D(1050f, 550f)),
                DestructibleObject("im_d_crate_1", DestructibleType.WOODEN_CRATE, Point2D(1050f, 950f)),
                DestructibleObject("im_d_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1650f, 950f))
            )

            val map = GameMap(
                id = "ice_mountain",
                name = "Ice Mountain",
                description = "Perilous hairpin ascent through high-altitude blizzard peaks. Defend the summit stronghold from enemies climbing the winding glacial ridges.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 2700f,
                worldHeight = 1900f,
                startingCameraCenter = Point2D(1350f, 950f),
                defaultZoom = 0.84f,
                totalWaves = 24,
                missionChapter = "SNOW CAMPAIGN • MISSION 3"
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = destructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ------------------------------------------
        // SNOW LEVEL 4: FROZEN FORTRESS (30 Waves)
        // Ancient stone citadel carved into sheer blue glaciers.
        // Dual defense corridors protecting the inner castle sanctum.
        // ------------------------------------------
        fun createFrozenFortressMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2450f, 1450f)
            val pathNorth = GamePath(
                id = "fortress_north_rampart",
                waypoints = listOf(
                    Point2D(-60f, 450f),
                    Point2D(850f, 450f),
                    Point2D(1150f, 750f),
                    Point2D(1650f, 750f),
                    Point2D(1950f, 1100f),
                    Point2D(2450f, 1100f),
                    Point2D(2450f, 1450f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )
            val pathSouth = GamePath(
                id = "fortress_south_courtyard",
                waypoints = listOf(
                    Point2D(450f, 1920f),
                    Point2D(450f, 1300f),
                    Point2D(1150f, 1300f),
                    Point2D(1150f, 750f),
                    Point2D(1650f, 750f),
                    Point2D(1950f, 1100f),
                    Point2D(2450f, 1100f),
                    Point2D(2450f, 1450f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )
            val pondCenter = Point2D(1550f, 1450f)
            val pondRadius = 120f

            val decor = listOf(
                MapDecoration("ff_sp1", DecorationType.SNOW_PILE, Point2D(400f, 250f), size = 38f),
                MapDecoration("ff_sp2", DecorationType.SNOW_PILE, Point2D(1400f, 550f), size = 42f),
                MapDecoration("ff_sp3", DecorationType.SNOW_PILE, Point2D(800f, 1050f), size = 40f),
                MapDecoration("ff_sp4", DecorationType.SNOW_PILE, Point2D(2150f, 850f), size = 44f),
                MapDecoration("ff_fb1", DecorationType.FROZEN_BUSH, Point2D(650f, 650f), size = 30f),
                MapDecoration("ff_fb2", DecorationType.FROZEN_BUSH, Point2D(1800f, 900f), size = 32f),
                MapDecoration("ff_fb3", DecorationType.FROZEN_BUSH, Point2D(800f, 1500f), size = 30f),
                MapDecoration("ff_r1", DecorationType.BOULDER, Point2D(650f, 900f), size = 52f),
                MapDecoration("ff_r2", DecorationType.BOULDER, Point2D(1400f, 950f), size = 54f),
                MapDecoration("ff_r3", DecorationType.BOULDER, Point2D(2150f, 1350f), size = 50f),
                MapDecoration("ff_cry1", DecorationType.CRYSTAL, Point2D(1550f, 1300f), size = 28f),
                MapDecoration("ff_cry2", DecorationType.CRYSTAL, Point2D(1550f, 1600f), size = 28f),
                MapDecoration("ff_t1", DecorationType.PINE_TREE, Point2D(250f, 250f), size = 66f),
                MapDecoration("ff_t2", DecorationType.PINE_TREE, Point2D(1050f, 250f), size = 68f),
                MapDecoration("ff_t3", DecorationType.PINE_TREE, Point2D(1850f, 550f), size = 64f),
                MapDecoration("ff_t4", DecorationType.PINE_TREE, Point2D(200f, 1600f), size = 66f),
                MapDecoration("ff_t5", DecorationType.PINE_TREE, Point2D(2650f, 850f), size = 68f)
            )

            val destructibles = listOf(
                DestructibleObject("ff_d_rock_1", DestructibleType.LARGE_STONE, Point2D(650f, 900f)),
                DestructibleObject("ff_d_rock_2", DestructibleType.LARGE_BOULDER, Point2D(1400f, 950f)),
                DestructibleObject("ff_d_tree_1", DestructibleType.PINE_TREE, Point2D(250f, 250f)),
                DestructibleObject("ff_d_tree_2", DestructibleType.PINE_TREE, Point2D(1050f, 250f)),
                DestructibleObject("ff_d_crate_1", DestructibleType.WOODEN_CRATE, Point2D(1400f, 600f)),
                DestructibleObject("ff_d_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1950f, 1350f))
            )

            val map = GameMap(
                id = "frozen_fortress",
                name = "Frozen Fortress",
                description = "Ancient stone citadel carved into sheer blue glaciers. Massive perimeter ramparts and dual defense corridors protecting the inner sanctum.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 2900f,
                worldHeight = 1850f,
                startingCameraCenter = Point2D(1450f, 950f),
                defaultZoom = 0.82f,
                totalWaves = 30,
                missionChapter = "SNOW CAMPAIGN • MISSION 4"
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = destructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ------------------------------------------
        // SNOW LEVEL 5: ARCTIC BASE (36 Waves)
        // High-tech polar defense complex on the frozen ice shelf.
        // Extreme sub-zero conditions, frozen power pylons, and endless siege forces.
        // ------------------------------------------
        fun createArcticBaseMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2550f, 1400f)
            val path1 = GamePath(
                id = "arctic_perimeter_route",
                waypoints = listOf(
                    Point2D(-60f, 550f),
                    Point2D(850f, 550f),
                    Point2D(850f, 1450f),
                    Point2D(1750f, 1450f),
                    Point2D(1750f, 800f),
                    Point2D(2550f, 800f),
                    Point2D(2550f, 1400f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )
            val path2 = GamePath(
                id = "arctic_reactor_lane",
                waypoints = listOf(
                    Point2D(1350f, -60f),
                    Point2D(1350f, 550f),
                    Point2D(2150f, 550f),
                    Point2D(2150f, 1400f),
                    Point2D(2550f, 1400f)
                ),
                pathWidth = GameConfig.PATH_WIDTH
            )
            val pondCenter = Point2D(1300f, 1000f)
            val pondRadius = 125f

            val decor = listOf(
                MapDecoration("ab_sp1", DecorationType.SNOW_PILE, Point2D(450f, 350f), size = 42f),
                MapDecoration("ab_sp2", DecorationType.SNOW_PILE, Point2D(1100f, 350f), size = 40f),
                MapDecoration("ab_sp3", DecorationType.SNOW_PILE, Point2D(1300f, 1350f), size = 44f),
                MapDecoration("ab_sp4", DecorationType.SNOW_PILE, Point2D(2200f, 350f), size = 46f),
                MapDecoration("ab_fb1", DecorationType.FROZEN_BUSH, Point2D(600f, 750f), size = 32f),
                MapDecoration("ab_fb2", DecorationType.FROZEN_BUSH, Point2D(1550f, 650f), size = 30f),
                MapDecoration("ab_fb3", DecorationType.FROZEN_BUSH, Point2D(1950f, 1050f), size = 32f),
                MapDecoration("ab_r1", DecorationType.BOULDER, Point2D(500f, 1050f), size = 56f),
                MapDecoration("ab_r2", DecorationType.BOULDER, Point2D(1550f, 1150f), size = 54f),
                MapDecoration("ab_r3", DecorationType.BOULDER, Point2D(2350f, 1100f), size = 52f),
                MapDecoration("ab_cry1", DecorationType.CRYSTAL, Point2D(1300f, 850f), size = 30f),
                MapDecoration("ab_cry2", DecorationType.CRYSTAL, Point2D(1300f, 1150f), size = 30f),
                MapDecoration("ab_t1", DecorationType.PINE_TREE, Point2D(250f, 250f), size = 68f),
                MapDecoration("ab_t2", DecorationType.PINE_TREE, Point2D(1050f, 150f), size = 70f),
                MapDecoration("ab_t3", DecorationType.PINE_TREE, Point2D(1900f, 250f), size = 68f),
                MapDecoration("ab_t4", DecorationType.PINE_TREE, Point2D(400f, 1650f), size = 66f),
                MapDecoration("ab_t5", DecorationType.PINE_TREE, Point2D(2750f, 550f), size = 70f)
            )

            val destructibles = listOf(
                DestructibleObject("ab_d_rock_1", DestructibleType.LARGE_STONE, Point2D(500f, 1050f)),
                DestructibleObject("ab_d_rock_2", DestructibleType.LARGE_BOULDER, Point2D(1550f, 1150f)),
                DestructibleObject("ab_d_tree_1", DestructibleType.PINE_TREE, Point2D(250f, 250f)),
                DestructibleObject("ab_d_tree_2", DestructibleType.PINE_TREE, Point2D(1900f, 250f)),
                DestructibleObject("ab_d_crate_1", DestructibleType.REINFORCED_CRATE, Point2D(1100f, 750f)),
                DestructibleObject("ab_d_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1950f, 1350f))
            )

            val map = GameMap(
                id = "arctic_base",
                name = "Arctic Base",
                description = "High-tech polar defense complex on the frozen ice shelf. Deep sub-zero conditions, frozen power pylons, and endless waves of elite siege forces.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path1, path2),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 3000f,
                worldHeight = 2000f,
                startingCameraCenter = Point2D(1500f, 1000f),
                defaultZoom = 0.80f,
                totalWaves = 36,
                missionChapter = "SNOW CAMPAIGN • MISSION 5"
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = destructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        // ==========================================
        // MAP 9 — NIGHT FORTRESS (Moonlit Citadel)
        // Midnight battlefield with moonlit stone paths, bioluminescent plants,
        // glowing runestones, deep twilight pine groves, and illuminated castle base.
        // ==========================================
        fun createNightFortressMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(-40f, 380f),
                Point2D(440f, 380f),
                Point2D(700f, 540f),
                Point2D(700f, 980f),
                Point2D(460f, 1180f),
                Point2D(460f, 1420f),
                Point2D(880f, 1420f),
                Point2D(1140f, 1200f),
                Point2D(1140f, 740f),
                Point2D(1480f, 460f),
                Point2D(1920f, 460f),
                Point2D(2180f, 720f),
                Point2D(2180f, 1180f),
                Point2D(1850f, 1420f)
            )

            val basePos = Point2D(1850f, 1420f)
            val pondCenter = Point2D(1640f, 850f)
            val pondRadius = 135f

            val path = GamePath(id = "night_fortress_pass", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)

            val decor = listOf(
                // Bioluminescent glow mushrooms
                MapDecoration("gm1", DecorationType.GLOW_MUSHROOM, Point2D(880f, 620f), size = 26f),
                MapDecoration("gm2", DecorationType.GLOW_MUSHROOM, Point2D(1350f, 660f), size = 28f),
                MapDecoration("gm3", DecorationType.GLOW_MUSHROOM, Point2D(1640f, 650f), size = 26f),
                MapDecoration("gm4", DecorationType.GLOW_MUSHROOM, Point2D(1880f, 850f), size = 30f),
                MapDecoration("gm5", DecorationType.GLOW_MUSHROOM, Point2D(1000f, 1380f), size = 26f),
                MapDecoration("gm6", DecorationType.GLOW_MUSHROOM, Point2D(580f, 1340f), size = 26f),
                MapDecoration("gm7", DecorationType.GLOW_MUSHROOM, Point2D(2020f, 620f), size = 28f),

                // Ancient glowing lantern posts along key scenic lookouts
                MapDecoration("lp1", DecorationType.LANTERN_POST, Point2D(340f, 290f), size = 32f),
                MapDecoration("lp2", DecorationType.LANTERN_POST, Point2D(820f, 440f), size = 32f),
                MapDecoration("lp3", DecorationType.LANTERN_POST, Point2D(600f, 1080f), size = 32f),
                MapDecoration("lp4", DecorationType.LANTERN_POST, Point2D(1020f, 1080f), size = 32f),
                MapDecoration("lp5", DecorationType.LANTERN_POST, Point2D(1360f, 380f), size = 32f),
                MapDecoration("lp6", DecorationType.LANTERN_POST, Point2D(2060f, 380f), size = 32f),
                MapDecoration("lp7", DecorationType.LANTERN_POST, Point2D(2060f, 1300f), size = 32f),

                // Dark foliage and night bushes
                MapDecoration("b1", DecorationType.BUSH, Point2D(540f, 280f), size = 30f),
                MapDecoration("b2", DecorationType.BUSH, Point2D(840f, 860f), size = 32f),
                MapDecoration("b3", DecorationType.BUSH, Point2D(1420f, 680f), size = 30f),
                MapDecoration("b4", DecorationType.BUSH, Point2D(1860f, 620f), size = 32f),
                MapDecoration("b5", DecorationType.BUSH, Point2D(980f, 1460f), size = 28f),
                MapDecoration("b6", DecorationType.BUSH, Point2D(2280f, 540f), size = 30f),

                // Luminous crystals
                MapDecoration("cry1", DecorationType.CRYSTAL, Point2D(1480f, 1050f), size = 26f),
                MapDecoration("cry2", DecorationType.CRYSTAL, Point2D(1780f, 1050f), size = 26f),
                MapDecoration("cry3", DecorationType.CRYSTAL, Point2D(1640f, 1080f), size = 28f),

                // Dark moonlit stones and boulders
                MapDecoration("r1", DecorationType.BOULDER, Point2D(280f, 520f), size = 48f),
                MapDecoration("r2", DecorationType.BOULDER, Point2D(880f, 820f), size = 52f),
                MapDecoration("r3", DecorationType.BOULDER, Point2D(1350f, 880f), size = 54f),
                MapDecoration("r4", DecorationType.BOULDER, Point2D(2340f, 960f), size = 52f),
                MapDecoration("r5", DecorationType.BOULDER, Point2D(700f, 1340f), size = 46f),

                // Forest canopy pines and oaks
                MapDecoration("t1", DecorationType.PINE_TREE, Point2D(200f, 180f), size = 64f),
                MapDecoration("t2", DecorationType.PINE_TREE, Point2D(580f, 160f), size = 66f),
                MapDecoration("t3", DecorationType.OAK_TREE, Point2D(920f, 160f), size = 68f),
                MapDecoration("t4", DecorationType.PINE_TREE, Point2D(1680f, 220f), size = 64f),
                MapDecoration("t5", DecorationType.OAK_TREE, Point2D(2250f, 240f), size = 68f),
                MapDecoration("t6", DecorationType.PINE_TREE, Point2D(2420f, 750f), size = 66f),
                MapDecoration("t7", DecorationType.PINE_TREE, Point2D(2400f, 1350f), size = 64f),
                MapDecoration("t8", DecorationType.OAK_TREE, Point2D(280f, 900f), size = 68f),
                MapDecoration("t9", DecorationType.PINE_TREE, Point2D(260f, 1420f), size = 64f),
                MapDecoration("t10", DecorationType.PINE_TREE, Point2D(1380f, 1440f), size = 66f)
            )

            val nightDestructibles = listOf(
                DestructibleObject("d_night_rock_1", DestructibleType.LARGE_STONE, Point2D(280f, 520f)),
                DestructibleObject("d_night_rock_2", DestructibleType.STONE, Point2D(880f, 820f)),
                DestructibleObject("d_night_rock_3", DestructibleType.LARGE_BOULDER, Point2D(1350f, 880f)),
                DestructibleObject("d_night_rock_4", DestructibleType.LARGE_STONE, Point2D(2340f, 960f)),
                DestructibleObject("d_night_tree_1", DestructibleType.PINE_TREE, Point2D(200f, 180f)),
                DestructibleObject("d_night_tree_2", DestructibleType.OAK_TREE, Point2D(920f, 160f)),
                DestructibleObject("d_night_tree_3", DestructibleType.PINE_TREE, Point2D(1680f, 220f)),
                DestructibleObject("d_night_tree_4", DestructibleType.PINE_TREE, Point2D(2420f, 750f)),
                DestructibleObject("d_night_tree_5", DestructibleType.OAK_TREE, Point2D(280f, 900f)),
                DestructibleObject("d_night_crate_1", DestructibleType.WOODEN_CRATE, Point2D(920f, 980f)),
                DestructibleObject("d_night_crate_2", DestructibleType.REINFORCED_CRATE, Point2D(1350f, 520f))
            )

            val map = GameMap(
                id = "night_fortress",
                name = "Night Fortress",
                description = "Moonlit citadel surrounded by dark ancient forests, mystic reflecting waters, and bioluminescent flora. Defend the stronghold against elite night invaders.",
                environmentType = EnvironmentType.NIGHT_FORTRESS,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = nightDestructibles,
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(1100f, 850f),
                defaultZoom = 0.82f
            )
            val (finalDestructibles, finalDecorations) = buildNaturalEnvironmentForMap(
                map = map,
                candidateDestructibles = nightDestructibles,
                candidateDecorations = decor
            )
            val finalizedMap = map.copy(destructibles = finalDestructibles, decorations = finalDecorations)
            validateMap(finalizedMap)
            return finalizedMap
        }

        /**
         * Defensive map validation ensuring boundaries, base presence, and path integrity.
         */
        fun validateMap(map: GameMap) {
            if (map.worldWidth <= 0f || map.worldHeight <= 0f) {
                android.util.Log.e("GameMap", "Invalid map dimensions: ${map.worldWidth}x${map.worldHeight} for map ${map.id}")
                throw IllegalArgumentException("Map ${map.id} has invalid dimensions: ${map.worldWidth}x${map.worldHeight}")
            }
            if (map.basePosition.x < 0f || map.basePosition.x > map.worldWidth ||
                map.basePosition.y < 0f || map.basePosition.y > map.worldHeight
            ) {
                android.util.Log.e("GameMap", "Base position ${map.basePosition} is outside world bounds [0..${map.worldWidth}, 0..${map.worldHeight}] for map ${map.id}")
                throw IllegalArgumentException("Base position ${map.basePosition} is outside world bounds for map ${map.id}")
            }
            for (path in map.paths) {
                if (path.waypoints.size < 2) {
                    android.util.Log.e("GameMap", "Path ${path.id} has fewer than 2 waypoints in map ${map.id}")
                    throw IllegalArgumentException("Path ${path.id} has fewer than 2 waypoints")
                }
                val spawn = path.startPoint
                if (spawn.x < -100f || spawn.x > map.worldWidth + 100f ||
                    spawn.y < -100f || spawn.y > map.worldHeight + 100f
                ) {
                    android.util.Log.e("GameMap", "Spawn point $spawn is out of bounds for map ${map.id}")
                }
                val endPoint = path.endPoint
                val distToBase = endPoint.distanceTo(map.basePosition)
                if (distToBase > 60f) {
                    android.util.Log.w("GameMap", "Path ${path.id} end $endPoint does not connect to base ${map.basePosition} (distance: $distToBase)")
                }
            }

            // Verify that no destructible object was spawned on any road corridor
            for (d in map.destructibles) {
                for (path in map.paths) {
                    val dist = path.distanceToPath(d.position)
                    val minAllowed = (path.pathWidth / 2f) + d.collisionRadius
                    if (dist < minAllowed) {
                        android.util.Log.e("GameMap", "Destructible ${d.id} (${d.type}) is on path ${path.id} (dist $dist < $minAllowed)")
                        throw IllegalStateException("Destructible ${d.id} is placed on road ${path.id}")
                    }
                }
            }
        }
    }
}
