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
    NIGHT_FORTRESS,
    DAY_NIGHT,
    TEMPEST_RAIN,
    CLOUDY_FOREST
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

data class WaterPond(
    val center: Point2D,
    val radius: Float
)

data class BlockedArea(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val reason: String = "Obstacle"
)

data class BridgeSegment(
    val id: String,
    val start: Point2D,
    val end: Point2D,
    val width: Float = 56f
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
    val waterPonds: List<WaterPond> = emptyList(),
    val tunnelRegion: TunnelRegion? = null,
    val tunnelRegions: List<TunnelRegion> = emptyList(),
    val bridges: List<BridgeSegment> = emptyList(),
    val blockedAreas: List<BlockedArea> = emptyList(),
    val destructibles: List<DestructibleObject> = emptyList(),
    val worldWidth: Float = 2000f,
    val worldHeight: Float = 1500f,
    val startingCameraCenter: Point2D = Point2D(460f, 440f),
    val defaultZoom: Float = 1.0f,
    val totalWaves: Int = GameConfig.TOTAL_WAVES,
    val missionChapter: String? = null,
    val allowedBuildSpots: List<Point2D>? = null,
    val initialBuildClearings: List<Point2D> = emptyList(),
    val startingCoins: Int? = null,
    val difficulty: String = "Normal",
    val hasDesertWind: Boolean = false,
    val hasRain: Boolean = (environmentType == EnvironmentType.TEMPEST_RAIN)
) {
    val isRaining: Boolean get() = hasRain || environmentType == EnvironmentType.TEMPEST_RAIN

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
        missionChapter: String? = null,
        allowedBuildSpots: List<Point2D>? = null,
        initialBuildClearings: List<Point2D> = emptyList(),
        startingCoins: Int? = null,
        difficulty: String = "Normal",
        hasDesertWind: Boolean = false,
        bridges: List<BridgeSegment> = emptyList(),
        hasRain: Boolean = (environmentType == EnvironmentType.TEMPEST_RAIN)
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
        bridges = bridges,
        blockedAreas = blockedAreas,
        destructibles = destructibles,
        worldWidth = worldWidth,
        worldHeight = worldHeight,
        startingCameraCenter = startingCameraCenter,
        defaultZoom = defaultZoom,
        totalWaves = totalWaves,
        missionChapter = missionChapter,
        allowedBuildSpots = allowedBuildSpots,
        initialBuildClearings = initialBuildClearings,
        startingCoins = startingCoins,
        difficulty = difficulty,
        hasDesertWind = hasDesertWind,
        hasRain = hasRain
    )

    val path: GamePath get() = paths.first()
    val spawnPoints: List<Point2D> get() = paths.map { it.startPoint }

    fun getPath(index: Int): GamePath = paths.getOrElse(index % paths.size) { paths.first() }

    fun getAllTunnelRegions(): List<TunnelRegion> {
        if (tunnelRegions.isNotEmpty()) return tunnelRegions
        return listOfNotNull(tunnelRegion)
    }

    fun getAllWaterPonds(): List<WaterPond> {
        val list = mutableListOf<WaterPond>()
        if (waterPondCenter != null && waterPondRadius > 0f) {
            list.add(WaterPond(waterPondCenter, waterPondRadius))
        }
        list.addAll(waterPonds)
        return list
    }

    fun isPointInTunnel(point: Point2D): Boolean {
        for (t in getAllTunnelRegions()) {
            if (point.x in t.boundsLeft..t.boundsRight && point.y in t.boundsTop..t.boundsBottom) {
                return true
            }
        }
        return false
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
        for (wp in getAllWaterPonds()) {
            if (candidate.distanceTo(wp.center) < (wp.radius + towerRadius)) {
                return false
            }
        }

        // 4. Solid cavern mountain obstruction for tunnel map
        for (t in getAllTunnelRegions()) {
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
        for (wp in getAllWaterPonds()) {
            if (candidate.distanceTo(wp.center) < (wp.radius + collisionRadius + 15f)) {
                return false
            }
        }

        // 4. Solid cavern mountain obstruction for tunnel map
        for (t in getAllTunnelRegions()) {
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

        // 7. Allowed build spot / initial build clearing clearance
        val clearings = allowedBuildSpots ?: initialBuildClearings
        for (spot in clearings) {
            if (candidate.distanceTo(spot) < (75f + collisionRadius)) {
                return false
            }
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
                ),
                createEclipseFrontierMap(
                    isUnlocked = isMapUnlocked("eclipse_frontier") || isMapUnlocked("solstice_frontier") || isMapUnlocked("night_fortress"),
                    stars = maxOf(getStars("eclipse_frontier"), getStars("solstice_frontier"))
                ),
                createTempestBastionMap(
                    isUnlocked = isMapUnlocked("storm_twin_bastion") || isMapUnlocked("tempest_bastion"),
                    stars = maxOf(getStars("storm_twin_bastion"), getStars("tempest_bastion"))
                ),
                createCloudyForestMap(
                    isUnlocked = isMapUnlocked("cloudy_dense_forest") || isMapUnlocked("cloudy_forest"),
                    stars = maxOf(getStars("cloudy_dense_forest"), getStars("cloudy_forest"))
                ),
                createSnowSummitMap(
                    isUnlocked = isMapUnlocked("snow_summit_descent") || isMapUnlocked("frostpeak_descent"),
                    stars = maxOf(getStars("snow_summit_descent"), getStars("frostpeak_descent"))
                ),
                createDesertDuneBastionMap(
                    isUnlocked = isMapUnlocked("desert_dune_bastion") || isMapUnlocked("dune_storm_stronghold"),
                    stars = maxOf(getStars("desert_dune_bastion"), getStars("dune_storm_stronghold"))
                ),
                createEmeraldTwinPassMap(
                    isUnlocked = isMapUnlocked("emerald_twin_pass") || isMapUnlocked("emerald_serpent_pass"),
                    stars = maxOf(getStars("emerald_twin_pass"), getStars("emerald_serpent_pass"))
                ),
                createForestRingBastionMap(
                    isUnlocked = isMapUnlocked("forest_ring_bastion") || isMapUnlocked("sylvan_ring_sanctuary"),
                    stars = maxOf(getStars("forest_ring_bastion"), getStars("sylvan_ring_sanctuary"))
                ),
                createFrozenPassMap(
                    isUnlocked = isMapUnlocked("frozen_pass"),
                    stars = getStars("frozen_pass")
                ),
                createObsidianCrossfireMap(
                    isUnlocked = isMapUnlocked("obsidian_crossfire"),
                    stars = getStars("obsidian_crossfire")
                ),
                createTempestRavineMap(
                    isUnlocked = isMapUnlocked("tempest_ravine"),
                    stars = getStars("tempest_ravine")
                ),
                createEclipseCitadelMap(
                    isUnlocked = isMapUnlocked("eclipse_citadel"),
                    stars = getStars("eclipse_citadel")
                ),
                createApexDragonSanctumMap(
                    isUnlocked = isMapUnlocked("apex_dragon_sanctum"),
                    stars = getStars("apex_dragon_sanctum")
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
        // SNOW LEVEL 4: FROZEN FORTRESS (24 Waves)
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
                totalWaves = 24,
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
        // SNOW LEVEL 5: ARCTIC BASE (25 Waves)
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
                totalWaves = 25,
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

        // ========================================================
        // MAP 14 — SOLSTICE ECLIPSE FRONTIER (Day & Night Proving Ground)
        // 20 Waves total: Waves 1-10 High Noon Sun, Waves 11-20 Midnight Siege
        // Difficulty: High (Waves 9-20: ONLY Bosses & Fast Enemies, with swarms up to 10 Bosses!)
        // ========================================================
        fun createEclipseFrontierMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val waypoints = listOf(
                Point2D(-40f, 620f),
                Point2D(380f, 620f),
                Point2D(620f, 380f),
                Point2D(1020f, 380f),
                Point2D(1280f, 680f),
                Point2D(980f, 1080f),
                Point2D(1350f, 1380f),
                Point2D(1820f, 1320f),
                Point2D(1950f, 920f),
                Point2D(2350f, 920f)
            )

            val basePos = Point2D(2350f, 920f)
            val pondCenter = Point2D(1540f, 860f)
            val pondRadius = 115f

            val path = GamePath(id = "eclipse_frontier_pass", waypoints = waypoints, pathWidth = GameConfig.PATH_WIDTH)

            val decor = listOf(
                // Day & Night contrasting flora and lanterns
                MapDecoration("ef_lp1", DecorationType.LANTERN_POST, Point2D(360f, 520f), size = 32f),
                MapDecoration("ef_lp2", DecorationType.LANTERN_POST, Point2D(680f, 460f), size = 32f),
                MapDecoration("ef_lp3", DecorationType.LANTERN_POST, Point2D(1220f, 740f), size = 32f),
                MapDecoration("ef_lp4", DecorationType.LANTERN_POST, Point2D(1040f, 1160f), size = 32f),
                MapDecoration("ef_lp5", DecorationType.LANTERN_POST, Point2D(1760f, 1220f), size = 32f),
                MapDecoration("ef_lp6", DecorationType.LANTERN_POST, Point2D(2240f, 820f), size = 32f),

                // Luminous crystals and glow mushrooms for nocturnal half
                MapDecoration("ef_gm1", DecorationType.GLOW_MUSHROOM, Point2D(840f, 620f), size = 26f),
                MapDecoration("ef_gm2", DecorationType.GLOW_MUSHROOM, Point2D(1440f, 680f), size = 28f),
                MapDecoration("ef_gm3", DecorationType.GLOW_MUSHROOM, Point2D(1740f, 820f), size = 26f),
                MapDecoration("ef_cry1", DecorationType.CRYSTAL, Point2D(1420f, 1020f), size = 28f),
                MapDecoration("ef_cry2", DecorationType.CRYSTAL, Point2D(1680f, 1040f), size = 28f),

                // Sun-warmed oaks and pines
                MapDecoration("ef_t1", DecorationType.OAK_TREE, Point2D(220f, 260f), size = 52f),
                MapDecoration("ef_t2", DecorationType.OAK_TREE, Point2D(820f, 220f), size = 54f),
                MapDecoration("ef_t3", DecorationType.PINE_TREE, Point2D(1180f, 220f), size = 50f),
                MapDecoration("ef_t4", DecorationType.PINE_TREE, Point2D(460f, 820f), size = 50f),
                MapDecoration("ef_t5", DecorationType.PINE_TREE, Point2D(680f, 1280f), size = 52f),
                MapDecoration("ef_t6", DecorationType.OAK_TREE, Point2D(1560f, 1540f), size = 52f),
                MapDecoration("ef_t7", DecorationType.PINE_TREE, Point2D(2120f, 1280f), size = 50f),

                // Boulders and rocks
                MapDecoration("ef_r1", DecorationType.BOULDER, Point2D(240f, 780f), size = 46f),
                MapDecoration("ef_r2", DecorationType.BOULDER, Point2D(880f, 880f), size = 48f),
                MapDecoration("ef_r3", DecorationType.BOULDER, Point2D(1440f, 1220f), size = 44f),
                MapDecoration("ef_r4", DecorationType.BOULDER, Point2D(2150f, 650f), size = 50f),

                // Bushes and flowers
                MapDecoration("ef_b1", DecorationType.BUSH, Point2D(520f, 520f), size = 30f),
                MapDecoration("ef_b2", DecorationType.BUSH, Point2D(1120f, 520f), size = 32f),
                MapDecoration("ef_b3", DecorationType.FLOWER_PATCH, Point2D(760f, 740f), size = 34f),
                MapDecoration("ef_b4", DecorationType.FLOWER_PATCH, Point2D(1620f, 1180f), size = 34f)
            )

            val destructibles = listOf(
                DestructibleObject("ef_d_tree_1", DestructibleType.OAK_TREE, Point2D(520f, 220f)),
                DestructibleObject("ef_d_rock_1", DestructibleType.LARGE_BOULDER, Point2D(1350f, 500f)),
                DestructibleObject("ef_d_tree_2", DestructibleType.PINE_TREE, Point2D(780f, 1420f)),
                DestructibleObject("ef_d_crate_1", DestructibleType.REINFORCED_CRATE, Point2D(1880f, 720f)),
                DestructibleObject("ef_d_tree_3", DestructibleType.PINE_TREE, Point2D(2450f, 680f))
            )

            val map = GameMap(
                id = "eclipse_frontier",
                name = "Eclipse Frontier",
                description = "Day & Night Proving Ground (20 Waves): High noon shines upon Waves 1–10 before midnight plunges the pass into darkness. From Wave 9 onwards, only elite fast invaders and swarms of up to 10 bosses strike!",
                environmentType = EnvironmentType.DAY_NIGHT,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(path),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 2650f,
                worldHeight = 1750f,
                startingCameraCenter = Point2D(1325f, 875f),
                defaultZoom = 0.80f,
                totalWaves = 20
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

        fun createTempestBastionMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val northWaypoints = listOf(
                Point2D(0f, 420f),
                Point2D(360f, 420f),
                Point2D(640f, 280f),
                Point2D(1020f, 280f),
                Point2D(1280f, 520f),
                Point2D(1620f, 520f),
                Point2D(1880f, 380f),
                Point2D(2140f, 480f),
                Point2D(2300f, 620f)
            )

            val southWaypoints = listOf(
                Point2D(0f, 1380f),
                Point2D(340f, 1380f),
                Point2D(620f, 1520f),
                Point2D(1000f, 1520f),
                Point2D(1260f, 1280f),
                Point2D(1600f, 1280f),
                Point2D(1860f, 1420f),
                Point2D(2140f, 1320f),
                Point2D(2300f, 1180f)
            )

            val basePos = Point2D(2300f, 900f)
            val pondCenter = Point2D(1440f, 900f)
            val pondRadius = 120f

            val pathNorth = GamePath(id = "storm_path_north", waypoints = northWaypoints, pathWidth = GameConfig.PATH_WIDTH)
            val pathSouth = GamePath(id = "storm_path_south", waypoints = southWaypoints, pathWidth = GameConfig.PATH_WIDTH)

            val decor = listOf(
                // Storm Bastion perimeter lightning rods & lanterns
                MapDecoration("tb_lp1", DecorationType.LANTERN_POST, Point2D(360f, 560f), size = 32f),
                MapDecoration("tb_lp2", DecorationType.LANTERN_POST, Point2D(1020f, 420f), size = 32f),
                MapDecoration("tb_lp3", DecorationType.LANTERN_POST, Point2D(1620f, 660f), size = 32f),
                MapDecoration("tb_lp4", DecorationType.LANTERN_POST, Point2D(2140f, 620f), size = 32f),
                MapDecoration("tb_lp5", DecorationType.LANTERN_POST, Point2D(360f, 1240f), size = 32f),
                MapDecoration("tb_lp6", DecorationType.LANTERN_POST, Point2D(1020f, 1380f), size = 32f),
                MapDecoration("tb_lp7", DecorationType.LANTERN_POST, Point2D(1620f, 1140f), size = 32f),
                MapDecoration("tb_lp8", DecorationType.LANTERN_POST, Point2D(2140f, 1180f), size = 32f),

                // Storm-weathered pines and ancient boulders
                MapDecoration("tb_t1", DecorationType.PINE_TREE, Point2D(220f, 220f), size = 54f),
                MapDecoration("tb_t2", DecorationType.PINE_TREE, Point2D(840f, 160f), size = 52f),
                MapDecoration("tb_t3", DecorationType.PINE_TREE, Point2D(1440f, 320f), size = 52f),
                MapDecoration("tb_t4", DecorationType.PINE_TREE, Point2D(220f, 1580f), size = 54f),
                MapDecoration("tb_t5", DecorationType.PINE_TREE, Point2D(840f, 1640f), size = 52f),
                MapDecoration("tb_t6", DecorationType.PINE_TREE, Point2D(1440f, 1480f), size = 52f),
                MapDecoration("tb_t7", DecorationType.PINE_TREE, Point2D(1980f, 900f), size = 50f),

                // Wet slippery bluffs & boulders
                MapDecoration("tb_r1", DecorationType.BOULDER, Point2D(650f, 900f), size = 48f),
                MapDecoration("tb_r2", DecorationType.BOULDER, Point2D(1150f, 900f), size = 46f),
                MapDecoration("tb_r3", DecorationType.BOULDER, Point2D(2480f, 600f), size = 50f),
                MapDecoration("tb_r4", DecorationType.BOULDER, Point2D(2480f, 1200f), size = 50f),

                // Storm crystals & rain flora
                MapDecoration("tb_c1", DecorationType.CRYSTAL, Point2D(1720f, 820f), size = 28f),
                MapDecoration("tb_c2", DecorationType.CRYSTAL, Point2D(1720f, 980f), size = 28f),
                MapDecoration("tb_b1", DecorationType.BUSH, Point2D(540f, 740f), size = 32f),
                MapDecoration("tb_b2", DecorationType.BUSH, Point2D(540f, 1060f), size = 32f)
            )

            val destructibles = listOf(
                DestructibleObject("tb_d_rock_1", DestructibleType.LARGE_BOULDER, Point2D(830f, 900f)),
                DestructibleObject("tb_d_tree_1", DestructibleType.PINE_TREE, Point2D(480f, 850f)),
                DestructibleObject("tb_d_crate_1", DestructibleType.REINFORCED_CRATE, Point2D(1800f, 900f)),
                DestructibleObject("tb_d_tree_2", DestructibleType.PINE_TREE, Point2D(2460f, 420f)),
                DestructibleObject("tb_d_rock_2", DestructibleType.LARGE_BOULDER, Point2D(2460f, 1380f))
            )

            val map = GameMap(
                id = "storm_twin_bastion",
                name = "Tempest Bastion",
                description = "Torrential Storm & Twin Towers (15 Waves): Heavy rain pours relentlessly as enemies march down dual paths assaulting North & South Bastion Towers. After Wave 7, only apex bosses attack!",
                environmentType = EnvironmentType.TEMPEST_RAIN,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                decorations = decor,
                waterPondCenter = pondCenter,
                waterPondRadius = pondRadius,
                destructibles = destructibles,
                worldWidth = 2650f,
                worldHeight = 1750f,
                startingCameraCenter = Point2D(1325f, 875f),
                defaultZoom = 0.80f,
                totalWaves = 15
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

        /**
         * Creates Level 16: "Cloudy Grove" (cloudy_dense_forest)
         * - Dense ancient forest filled with trees everywhere
         * - Day + cloudy overcast atmosphere with drifting canopy shadows
         * - Exactly 2 strategic cleared gun foundations where towers can be placed
         * - 3 distinct winding forest trails converging and merging at one junction
         * - Rocky mountain tunnel shielding enemies as they advance from the merge junction to the base
         */
        fun createCloudyForestMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2050f, 800f)

            // Path 1 (North Trail): Descends through dense northern pine groves to the merge junction
            val pathNorth = GamePath(
                id = "cloudy_trail_north",
                waypoints = listOf(
                    Point2D(0f, 280f),
                    Point2D(340f, 280f),
                    Point2D(560f, 420f),
                    Point2D(880f, 420f),
                    Point2D(1120f, 640f),
                    Point2D(1350f, 800f), // MERGE CHOKEPOINT
                    Point2D(1520f, 800f), // TUNNEL ENTRANCE
                    Point2D(1840f, 800f), // TUNNEL EXIT
                    basePos               // BASE FORTRESS TOWER
                ),
                pathWidth = 50f
            )

            // Path 2 (Center Trail): Runs through the central ancient tree thicket to the merge junction
            val pathCenter = GamePath(
                id = "cloudy_trail_center",
                waypoints = listOf(
                    Point2D(0f, 800f),
                    Point2D(360f, 800f),
                    Point2D(600f, 720f),
                    Point2D(880f, 800f),
                    Point2D(1140f, 800f),
                    Point2D(1350f, 800f), // MERGE CHOKEPOINT
                    Point2D(1520f, 800f), // TUNNEL ENTRANCE
                    Point2D(1840f, 800f), // TUNNEL EXIT
                    basePos               // BASE FORTRESS TOWER
                ),
                pathWidth = 50f
            )

            // Path 3 (South Trail): Ascends from southern fern hollows to the merge junction
            val pathSouth = GamePath(
                id = "cloudy_trail_south",
                waypoints = listOf(
                    Point2D(0f, 1320f),
                    Point2D(340f, 1320f),
                    Point2D(560f, 1180f),
                    Point2D(880f, 1180f),
                    Point2D(1120f, 960f),
                    Point2D(1350f, 800f), // MERGE CHOKEPOINT
                    Point2D(1520f, 800f), // TUNNEL ENTRANCE
                    Point2D(1840f, 800f), // TUNNEL EXIT
                    basePos               // BASE FORTRESS TOWER
                ),
                pathWidth = 50f
            )

            // Tunnel Region: Rocky mountain ridge spanning the merged highway
            val tunnel = TunnelRegion(
                id = "forest_cavern_tunnel",
                boundsLeft = 1520f,
                boundsTop = 680f,
                boundsRight = 1840f,
                boundsBottom = 920f,
                entrance = Point2D(1520f, 800f),
                exit = Point2D(1840f, 800f)
            )

            // ONLY 2 initial empty locations (no tree, no stone) overlooking the merge junction
            val clearingNorth = Point2D(1350f, 600f) // North flank initial empty clearing
            val clearingSouth = Point2D(1350f, 1000f) // South flank initial empty clearing
            val initialClearings = listOf(clearingNorth, clearingSouth)

            // Pre-seed map to check environment clearance
            val baseMap = GameMap(
                id = "cloudy_dense_forest",
                name = "Cloudy Grove",
                description = "Dense Ancient Forest (15 Waves): Only 2 clearings exist initially to place guns. All other buildable ground is packed with trees and stones. Target and blast the trees and stones to clear new locations and set more guns!",
                environmentType = EnvironmentType.CLOUDY_FOREST,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathCenter, pathSouth),
                basePosition = basePos,
                decorations = emptyList(),
                tunnelRegion = tunnel,
                destructibles = emptyList(),
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1350f, 800f),
                defaultZoom = 0.85f,
                totalWaves = 15,
                allowedBuildSpots = null,
                initialBuildClearings = initialClearings
            )

            // Populate all buildable ground across the entire forest with destructible trees and stones,
            // EXCEPT for the 2 initial empty clearings!
            val destructibles = mutableListOf<DestructibleObject>()
            var dId = 0
            val rnd = java.util.Random(42L)

            // Forest grid covering all buildable ground outside roads
            for (gx in 75..2325 step 90) {
                for (gy in 75..1525 step 90) {
                    val jx = gx.toFloat() + (rnd.nextFloat() * 26f - 13f)
                    val jy = gy.toFloat() + (rnd.nextFloat() * 26f - 13f)
                    val candPos = Point2D(jx, jy)

                    // EXCLUSIVITY: The 2 initial clearings have NO tree and NO stone
                    val inInitialClearing = initialClearings.any { candPos.distanceTo(it) < 85f }
                    if (inInitialClearing) continue

                    // Must be completely off-road and outside tunnel/base
                    if (baseMap.canSpawnEnvironmentObject(candPos, collisionRadius = 25f, roadSafetyMargin = 30f)) {
                        val (dType, hp, reward) = when (rnd.nextInt(5)) {
                            0 -> Triple(DestructibleType.PINE_TREE, 95f, 12)
                            1 -> Triple(DestructibleType.OAK_TREE, 110f, 14)
                            2 -> Triple(DestructibleType.TREE, 100f, 12)
                            3 -> Triple(DestructibleType.STONE, 130f, 15)
                            else -> Triple(DestructibleType.LARGE_STONE, 180f, 20)
                        }

                        destructibles.add(
                            DestructibleObject(
                                id = "cloudy_destructible_${dId++}",
                                type = dType,
                                position = candPos,
                                maxHp = hp,
                                currentHp = hp,
                                rewardTokens = reward,
                                radius = 24f
                            )
                        )
                    }
                }
            }

            // Scatter ground undergrowth (flowers, mushrooms) that do NOT block building
            val decor = mutableListOf<MapDecoration>()
            var decorIdx = 0
            for (i in 0 until 50) {
                val fx = 100f + rnd.nextFloat() * 2200f
                val fy = 100f + rnd.nextFloat() * 1400f
                val cand = Point2D(fx, fy)
                if (baseMap.canSpawnEnvironmentObject(cand, collisionRadius = 14f, roadSafetyMargin = 22f)) {
                    val decType = if (rnd.nextBoolean()) DecorationType.FLOWER_PATCH else DecorationType.GLOW_MUSHROOM
                    decor.add(
                        MapDecoration(
                            id = "forest_undergrowth_${decorIdx++}",
                            type = decType,
                            position = cand,
                            size = 18f + rnd.nextFloat() * 10f
                        )
                    )
                }
            }

            val finalMap = baseMap.copy(
                decorations = decor,
                destructibles = destructibles
            )
            validateMap(finalMap)
            return finalMap
        }

        // =========================================================================
        // MAP 17 — FROSTPEAK DESCENT (Day Snow Mountains)
        // Enemies descend from the high icy mountain peaks down to the stronghold tower at the mountain bottom.
        // Carpeted with abundant snow-covered boulders, rocks, and icy stones across 20 escalating waves.
        // =========================================================================
        fun createSnowSummitMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(1000f, 1680f)

            // Path 1 (West Glacier Ridge): Cascades down from the Northwest Peak
            val pathWest = GamePath(
                id = "frostpeak_descent_west",
                waypoints = listOf(
                    Point2D(600f, -40f),
                    Point2D(600f, 220f),
                    Point2D(350f, 440f),
                    Point2D(700f, 660f),
                    Point2D(1000f, 850f), // Slope convergence
                    Point2D(600f, 1080f),
                    Point2D(600f, 1340f),
                    Point2D(1400f, 1340f),
                    Point2D(1400f, 1600f),
                    basePos
                ),
                pathWidth = 50f
            )

            // Path 2 (East Glacier Ridge): Cascades down from the Northeast Peak
            val pathEast = GamePath(
                id = "frostpeak_descent_east",
                waypoints = listOf(
                    Point2D(1400f, -40f),
                    Point2D(1400f, 220f),
                    Point2D(1650f, 440f),
                    Point2D(1300f, 660f),
                    Point2D(1000f, 850f), // Slope convergence
                    Point2D(600f, 1080f),
                    Point2D(600f, 1340f),
                    Point2D(1400f, 1340f),
                    Point2D(1400f, 1600f),
                    basePos
                ),
                pathWidth = 50f
            )

            // Heavy abundance of snow-covered boulders, snowdrifts, and frozen mountain features
            val decor = mutableListOf<MapDecoration>(
                // Snow-covered boulders on upper Northwest peak
                MapDecoration("fps_b1", DecorationType.BOULDER, Point2D(420f, 100f), size = 56f),
                MapDecoration("fps_b2", DecorationType.BOULDER, Point2D(780f, 100f), size = 52f),
                MapDecoration("fps_b3", DecorationType.BOULDER, Point2D(200f, 260f), size = 60f),
                MapDecoration("fps_b4", DecorationType.BOULDER, Point2D(480f, 320f), size = 48f),
                MapDecoration("fps_sp1", DecorationType.SNOW_PILE, Point2D(340f, 140f), size = 46f),
                MapDecoration("fps_sp2", DecorationType.SNOW_PILE, Point2D(840f, 220f), size = 42f),
                MapDecoration("fps_fb1", DecorationType.FROZEN_BUSH, Point2D(240f, 400f), size = 32f),

                // Snow-covered boulders on upper Northeast peak
                MapDecoration("fps_b5", DecorationType.BOULDER, Point2D(1220f, 100f), size = 54f),
                MapDecoration("fps_b6", DecorationType.BOULDER, Point2D(1580f, 100f), size = 58f),
                MapDecoration("fps_b7", DecorationType.BOULDER, Point2D(1800f, 260f), size = 62f),
                MapDecoration("fps_b8", DecorationType.BOULDER, Point2D(1520f, 320f), size = 50f),
                MapDecoration("fps_sp3", DecorationType.SNOW_PILE, Point2D(1660f, 140f), size = 45f),
                MapDecoration("fps_sp4", DecorationType.SNOW_PILE, Point2D(1160f, 220f), size = 40f),
                MapDecoration("fps_fb2", DecorationType.FROZEN_BUSH, Point2D(1760f, 400f), size = 32f),

                // Central mountain plateau boulders & crystals
                MapDecoration("fps_b9", DecorationType.BOULDER, Point2D(1000f, 350f), size = 64f),
                MapDecoration("fps_b10", DecorationType.BOULDER, Point2D(1000f, 500f), size = 58f),
                MapDecoration("fps_b11", DecorationType.BOULDER, Point2D(450f, 580f), size = 52f),
                MapDecoration("fps_b12", DecorationType.BOULDER, Point2D(1550f, 580f), size = 52f),
                MapDecoration("fps_cry1", DecorationType.CRYSTAL, Point2D(860f, 420f), size = 30f),
                MapDecoration("fps_cry2", DecorationType.CRYSTAL, Point2D(1140f, 420f), size = 30f),
                MapDecoration("fps_sp5", DecorationType.SNOW_PILE, Point2D(900f, 620f), size = 48f),
                MapDecoration("fps_sp6", DecorationType.SNOW_PILE, Point2D(1100f, 620f), size = 48f),

                // Mid-mountain terrace boulders
                MapDecoration("fps_b13", DecorationType.BOULDER, Point2D(350f, 850f), size = 62f),
                MapDecoration("fps_b14", DecorationType.BOULDER, Point2D(1650f, 850f), size = 64f),
                MapDecoration("fps_b15", DecorationType.BOULDER, Point2D(800f, 960f), size = 54f),
                MapDecoration("fps_b16", DecorationType.BOULDER, Point2D(1200f, 960f), size = 54f),
                MapDecoration("fps_b17", DecorationType.BOULDER, Point2D(380f, 1200f), size = 56f),
                MapDecoration("fps_b18", DecorationType.BOULDER, Point2D(1620f, 1200f), size = 58f),
                MapDecoration("fps_sp7", DecorationType.SNOW_PILE, Point2D(250f, 1020f), size = 45f),
                MapDecoration("fps_sp8", DecorationType.SNOW_PILE, Point2D(1750f, 1020f), size = 45f),

                // Lower valley & fortress tower approaches (mountain base)
                MapDecoration("fps_b19", DecorationType.BOULDER, Point2D(1000f, 1180f), size = 60f),
                MapDecoration("fps_b20", DecorationType.BOULDER, Point2D(800f, 1480f), size = 55f),
                MapDecoration("fps_b21", DecorationType.BOULDER, Point2D(1200f, 1480f), size = 55f),
                MapDecoration("fps_b22", DecorationType.BOULDER, Point2D(650f, 1680f), size = 58f),
                MapDecoration("fps_b23", DecorationType.BOULDER, Point2D(1350f, 1680f), size = 58f),
                MapDecoration("fps_sp9", DecorationType.SNOW_PILE, Point2D(700f, 1550f), size = 46f),
                MapDecoration("fps_sp10", DecorationType.SNOW_PILE, Point2D(1300f, 1550f), size = 46f),
                MapDecoration("fps_cry3", DecorationType.CRYSTAL, Point2D(1000f, 1520f), size = 32f),

                // High alpine pine trees flanking the ridges
                MapDecoration("fps_pt1", DecorationType.PINE_TREE, Point2D(150f, 120f), size = 68f),
                MapDecoration("fps_pt2", DecorationType.PINE_TREE, Point2D(1850f, 120f), size = 68f),
                MapDecoration("fps_pt3", DecorationType.PINE_TREE, Point2D(120f, 650f), size = 70f),
                MapDecoration("fps_pt4", DecorationType.PINE_TREE, Point2D(1880f, 650f), size = 70f),
                MapDecoration("fps_pt5", DecorationType.PINE_TREE, Point2D(180f, 1450f), size = 72f),
                MapDecoration("fps_pt6", DecorationType.PINE_TREE, Point2D(1820f, 1450f), size = 72f)
            )

            val baseMap = GameMap(
                id = "snow_summit_descent",
                name = "Frostpeak Descent",
                description = "High snow mountains under bright winter daylight. Enemy legions surge downward from the icy summits while your stronghold tower stands firm at the mountain base amidst snow-covered boulder fields.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathWest, pathEast),
                basePosition = basePos,
                decorations = emptyList(),
                worldWidth = 2000f,
                worldHeight = 1800f,
                startingCameraCenter = Point2D(1000f, 900f),
                defaultZoom = 0.95f,
                totalWaves = 20,
                missionChapter = "FROSTPEAK"
            )

            // Destructible snow-covered stones and rocks scattered across the slopes
            val destructibles = mutableListOf<DestructibleObject>()
            val candRocks = listOf(
                Point2D(460f, 220f),
                Point2D(1540f, 220f),
                Point2D(260f, 500f),
                Point2D(1740f, 500f),
                Point2D(840f, 320f),
                Point2D(1160f, 320f),
                Point2D(1000f, 680f),
                Point2D(480f, 740f),
                Point2D(1520f, 740f),
                Point2D(820f, 1160f),
                Point2D(1180f, 1160f),
                Point2D(360f, 1460f),
                Point2D(1640f, 1460f),
                Point2D(1000f, 1380f),
                Point2D(780f, 1620f),
                Point2D(1220f, 1620f)
            )

            var rId = 1
            for (p in candRocks) {
                if (baseMap.canSpawnEnvironmentObject(p, collisionRadius = 24f, roadSafetyMargin = 28f)) {
                    val dType = if (rId % 3 == 0) DestructibleType.PINE_TREE else DestructibleType.LARGE_STONE
                    destructibles.add(
                        DestructibleObject(
                            id = "snow_summit_stone_${rId++}",
                            type = dType,
                            position = p,
                            maxHp = 220f,
                            currentHp = 220f,
                            rewardTokens = 22,
                            radius = 26f
                        )
                    )
                }
            }

            val finalMap = baseMap.copy(
                decorations = decor,
                destructibles = destructibles
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 18 — ARID DESERT (Dune Bastion)
        // Difficulty: Hard (15 Waves)
        // Waves:
        //   1: First the boss attacking with small enemies
        //   2-6: Fastest enemies with medium level enemies
        //   7-11: All type enemies with different bosses
        //   12-15: ONLY bosses in large amount
        // Starting coins: 1000
        // Setting: Large desert with big stones, desert wind, and 2 enemy paths!
        // ==========================================
        fun createDesertDuneBastionMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 800f)

            // Path 1 (Northern Sand Canyon): Winds through northern dune plateaus
            val pathNorth = GamePath(
                id = "dune_canyon_north",
                waypoints = listOf(
                    Point2D(-40f, 400f),
                    Point2D(350f, 400f),
                    Point2D(650f, 260f),
                    Point2D(1050f, 260f),
                    Point2D(1350f, 440f),
                    Point2D(1700f, 440f),
                    Point2D(1950f, 700f),
                    basePos
                ),
                pathWidth = 52f
            )

            // Path 2 (Southern Oasis Dunes): Winds through southern sunbaked dunes
            val pathSouth = GamePath(
                id = "dune_canyon_south",
                waypoints = listOf(
                    Point2D(-40f, 1200f),
                    Point2D(350f, 1200f),
                    Point2D(650f, 1340f),
                    Point2D(1050f, 1340f),
                    Point2D(1350f, 1160f),
                    Point2D(1700f, 1160f),
                    Point2D(1950f, 900f),
                    basePos
                ),
                pathWidth = 52f
            )

            val baseMap = GameMap(
                id = "desert_dune_bastion",
                name = "Dune Bastion",
                description = "Arid Desert (Hard • 15 Waves): A massive sunbaked desert with roaring desert winds and colossal stone monoliths. Enemies march along two treacherous paths. Begins with 1000 coins for defense against early and late boss rushes!",
                environmentType = EnvironmentType.DESERT_CANYON,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1100f, 800f),
                defaultZoom = 0.85f,
                totalWaves = 15,
                missionChapter = "DESERT",
                startingCoins = 1000,
                difficulty = "Hard",
                hasDesertWind = true
            )

            val decor = mutableListOf<MapDecoration>()

            // Giant stone monoliths and boulder formations in northern cliffs
            val northBigStones = listOf(
                Point2D(200f, 140f) to 75f,
                Point2D(500f, 110f) to 85f,
                Point2D(850f, 120f) to 90f,
                Point2D(1250f, 110f) to 80f,
                Point2D(1600f, 130f) to 85f,
                Point2D(1900f, 160f) to 70f
            )
            northBigStones.forEachIndexed { idx, (pos, sz) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = sz / 2f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("dune_nboulder_$idx", DecorationType.BOULDER, pos, sz, (idx * 35f) % 360f))
                }
            }

            // Giant stone monoliths and boulder formations in southern cliffs
            val southBigStones = listOf(
                Point2D(200f, 1460f) to 75f,
                Point2D(500f, 1490f) to 85f,
                Point2D(850f, 1480f) to 90f,
                Point2D(1250f, 1490f) to 80f,
                Point2D(1600f, 1470f) to 85f,
                Point2D(1900f, 1440f) to 70f
            )
            southBigStones.forEachIndexed { idx, (pos, sz) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = sz / 2f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("dune_sboulder_$idx", DecorationType.BOULDER, pos, sz, (idx * 45f) % 360f))
                }
            }

            // Central desert plateau big stones between the two paths
            val centralBigStones = listOf(
                Point2D(220f, 800f) to 70f,
                Point2D(550f, 680f) to 75f,
                Point2D(550f, 920f) to 75f,
                Point2D(880f, 740f) to 95f,
                Point2D(880f, 860f) to 90f,
                Point2D(1200f, 680f) to 80f,
                Point2D(1200f, 920f) to 80f,
                Point2D(1520f, 720f) to 85f,
                Point2D(1520f, 880f) to 85f,
                Point2D(1800f, 800f) to 65f
            )
            centralBigStones.forEachIndexed { idx, (pos, sz) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = sz / 2f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("dune_cboulder_$idx", DecorationType.BOULDER, pos, sz, (idx * 55f) % 360f))
                }
            }

            // Arid desert cacti & dry scrub
            val desertCacti = listOf(
                Point2D(120f, 520f),
                Point2D(420f, 520f),
                Point2D(780f, 480f),
                Point2D(1120f, 480f),
                Point2D(1500f, 320f),
                Point2D(120f, 1080f),
                Point2D(420f, 1080f),
                Point2D(780f, 1120f),
                Point2D(1120f, 1120f),
                Point2D(1500f, 1280f),
                Point2D(2050f, 520f),
                Point2D(2050f, 1080f)
            )
            desertCacti.forEachIndexed { idx, pos ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = 18f, roadSafetyMargin = 20f)) {
                    decor.add(MapDecoration("dune_cactus_$idx", DecorationType.CACTUS, pos, 36f, 0f))
                }
            }

            // Scatter destructible big stones across the desert for tactical clearing and bonus coins
            val destructibles = mutableListOf<DestructibleObject>()
            val candRocks = listOf(
                Point2D(380f, 680f),
                Point2D(380f, 920f),
                Point2D(700f, 620f),
                Point2D(700f, 980f),
                Point2D(720f, 800f),
                Point2D(1000f, 620f),
                Point2D(1000f, 980f),
                Point2D(1040f, 800f),
                Point2D(1350f, 680f),
                Point2D(1350f, 920f),
                Point2D(1380f, 800f),
                Point2D(1650f, 640f),
                Point2D(1650f, 960f),
                Point2D(1680f, 800f),
                Point2D(480f, 260f),
                Point2D(1250f, 260f),
                Point2D(480f, 1340f),
                Point2D(1250f, 1340f),
                Point2D(1880f, 520f),
                Point2D(1880f, 1080f),
                Point2D(2050f, 680f),
                Point2D(2050f, 920f)
            )

            var rId = 1
            for (p in candRocks) {
                if (baseMap.canSpawnEnvironmentObject(p, collisionRadius = 26f, roadSafetyMargin = 26f)) {
                    val isLarge = (rId % 2 == 0)
                    destructibles.add(
                        DestructibleObject(
                            id = "desert_bastion_stone_${rId++}",
                            type = if (isLarge) DestructibleType.LARGE_STONE else DestructibleType.STONE,
                            position = p,
                            maxHp = if (isLarge) 240f else 180f,
                            currentHp = if (isLarge) 240f else 180f,
                            rewardTokens = if (isLarge) 30 else 20,
                            radius = if (isLarge) 28f else 22f
                        )
                    )
                }
            }

            val finalMap = baseMap.copy(
                decorations = decor,
                destructibles = destructibles
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 19 — EMERALD SERPENT PASS (Level 19)
        // Difficulty: Hard • 15 Waves
        // Area: Large green + water + trees
        // Paths: Two zig-zag paths with 2 rock mountain tunnels
        // Starting Coins: 1500
        // ==========================================
        fun createEmeraldTwinPassMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 800f)

            // Path 1 (Northern Zig-Zag Path): Winding northern sylvan route through Tunnel 1
            val pathNorth = GamePath(
                id = "emerald_zigzag_north",
                waypoints = listOf(
                    Point2D(-40f, 360f),
                    Point2D(320f, 360f),
                    Point2D(540f, 200f),
                    Point2D(720f, 360f), // Enters Tunnel 1
                    Point2D(980f, 360f), // Exits Tunnel 1
                    Point2D(1240f, 200f),
                    Point2D(1520f, 440f),
                    Point2D(1800f, 440f),
                    Point2D(2020f, 700f),
                    basePos
                ),
                pathWidth = 52f
            )

            // Path 2 (Southern Zig-Zag Path): Winding southern meadow route through Tunnel 2
            val pathSouth = GamePath(
                id = "emerald_zigzag_south",
                waypoints = listOf(
                    Point2D(-40f, 1240f),
                    Point2D(320f, 1240f),
                    Point2D(540f, 1400f),
                    Point2D(800f, 1160f),
                    Point2D(1080f, 1240f), // Enters Tunnel 2
                    Point2D(1340f, 1240f), // Exits Tunnel 2
                    Point2D(1600f, 1400f),
                    Point2D(1820f, 1160f),
                    Point2D(2020f, 900f),
                    basePos
                ),
                pathWidth = 52f
            )

            // Tunnel 1: Northern Mountain Ridge Cavern
            val tunnelNorth = TunnelRegion(
                id = "emerald_tunnel_north",
                boundsLeft = 700f,
                boundsTop = 260f,
                boundsRight = 1000f,
                boundsBottom = 460f,
                entrance = Point2D(720f, 360f),
                exit = Point2D(980f, 360f)
            )

            // Tunnel 2: Southern Highland Ridge Cavern
            val tunnelSouth = TunnelRegion(
                id = "emerald_tunnel_south",
                boundsLeft = 1060f,
                boundsTop = 1140f,
                boundsRight = 1360f,
                boundsBottom = 1340f,
                entrance = Point2D(1080f, 1240f),
                exit = Point2D(1340f, 1240f)
            )

            val baseMap = GameMap(
                id = "emerald_twin_pass",
                name = "Emerald Serpent Pass",
                description = "Highland Emerald Realm (Hard • 15 Waves): A sprawling lush green valley with shimmering lakes, dense ancient trees, and two zig-zag paths carving through mountain tunnels. Starts with 1500 coins against twin boss assaults!",
                environmentType = EnvironmentType.GREEN_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1100f, 800f),
                defaultZoom = 0.85f,
                totalWaves = 15,
                missionChapter = "HIGHLANDS",
                startingCoins = 1500,
                difficulty = "Hard",
                waterPondCenter = Point2D(1100f, 780f),
                waterPondRadius = 160f,
                waterPonds = listOf(WaterPond(Point2D(1700f, 820f), 95f)),
                tunnelRegion = tunnelNorth,
                tunnelRegions = listOf(tunnelNorth, tunnelSouth)
            )

            val decor = mutableListOf<MapDecoration>()

            // 1. Lush Greenery & Trees along northern forest line
            val northTrees = listOf(
                Point2D(160f, 140f) to DecorationType.OAK_TREE,
                Point2D(340f, 100f) to DecorationType.PINE_TREE,
                Point2D(520f, 80f) to DecorationType.OAK_TREE,
                Point2D(700f, 120f) to DecorationType.PINE_TREE,
                Point2D(880f, 110f) to DecorationType.OAK_TREE,
                Point2D(1080f, 90f) to DecorationType.PINE_TREE,
                Point2D(1280f, 80f) to DecorationType.OAK_TREE,
                Point2D(1460f, 120f) to DecorationType.PINE_TREE,
                Point2D(1680f, 110f) to DecorationType.OAK_TREE,
                Point2D(1900f, 130f) to DecorationType.PINE_TREE,
                Point2D(2120f, 160f) to DecorationType.OAK_TREE
            )
            northTrees.forEachIndexed { idx, (pos, type) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = 32f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("emerald_ntree_$idx", type, pos, 64f, (idx * 45f) % 360f))
                }
            }

            // 2. Lush Greenery & Trees along southern forest line
            val southTrees = listOf(
                Point2D(160f, 1480f) to DecorationType.OAK_TREE,
                Point2D(360f, 1500f) to DecorationType.PINE_TREE,
                Point2D(560f, 1520f) to DecorationType.OAK_TREE,
                Point2D(760f, 1490f) to DecorationType.PINE_TREE,
                Point2D(960f, 1510f) to DecorationType.OAK_TREE,
                Point2D(1160f, 1520f) to DecorationType.PINE_TREE,
                Point2D(1420f, 1510f) to DecorationType.OAK_TREE,
                Point2D(1660f, 1490f) to DecorationType.PINE_TREE,
                Point2D(1880f, 1480f) to DecorationType.OAK_TREE,
                Point2D(2120f, 1460f) to DecorationType.PINE_TREE
            )
            southTrees.forEachIndexed { idx, (pos, type) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = 32f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("emerald_stree_$idx", type, pos, 64f, (idx * 55f) % 360f))
                }
            }

            // 3. Central Meadow Trees & Bushes around lakes
            val meadowFlora = listOf(
                Point2D(460f, 680f) to DecorationType.OAK_TREE,
                Point2D(560f, 860f) to DecorationType.PINE_TREE,
                Point2D(750f, 640f) to DecorationType.BUSH,
                Point2D(840f, 920f) to DecorationType.FLOWER_PATCH,
                Point2D(1360f, 650f) to DecorationType.OAK_TREE,
                Point2D(1440f, 880f) to DecorationType.FLOWER_PATCH,
                Point2D(1540f, 700f) to DecorationType.PINE_TREE,
                Point2D(1860f, 660f) to DecorationType.BUSH,
                Point2D(1880f, 960f) to DecorationType.OAK_TREE
            )
            meadowFlora.forEachIndexed { idx, (pos, type) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = 28f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("emerald_flora_$idx", type, pos, 54f, (idx * 40f) % 360f))
                }
            }

            // 4. Mountain Boulders flanking tunnel portals
            val tunnelBoulders = listOf(
                Point2D(660f, 250f) to 58f,
                Point2D(1020f, 250f) to 56f,
                Point2D(1020f, 1120f) to 58f,
                Point2D(1380f, 1120f) to 56f
            )
            tunnelBoulders.forEachIndexed { idx, (pos, sz) ->
                if (baseMap.canSpawnEnvironmentObject(pos, collisionRadius = sz / 2f, roadSafetyMargin = 25f)) {
                    decor.add(MapDecoration("emerald_tboulder_$idx", DecorationType.BOULDER, pos, sz, (idx * 60f) % 360f))
                }
            }

            // 5. Tactical Destructible Trees that players can harvest/clear to unlock prime tower spots
            val destructibles = mutableListOf<DestructibleObject>()
            val candidateTreePositions = listOf(
                Point2D(420f, 520f),
                Point2D(640f, 520f),
                Point2D(860f, 520f),
                Point2D(1100f, 520f),
                Point2D(1340f, 480f),
                Point2D(1640f, 560f),
                Point2D(420f, 1060f),
                Point2D(640f, 1020f),
                Point2D(900f, 1060f),
                Point2D(1460f, 1060f),
                Point2D(1680f, 1020f),
                Point2D(1920f, 800f)
            )

            var tId = 1
            for (p in candidateTreePositions) {
                if (baseMap.canSpawnEnvironmentObject(p, collisionRadius = 24f, roadSafetyMargin = 28f)) {
                    val isOak = tId % 2 == 0
                    destructibles.add(
                        DestructibleObject(
                            id = "emerald_tree_${tId++}",
                            type = if (isOak) DestructibleType.OAK_TREE else DestructibleType.PINE_TREE,
                            position = p,
                            maxHp = 120f,
                            currentHp = 120f,
                            rewardTokens = 15,
                            radius = 22f
                        )
                    )
                }
            }

            val finalMap = baseMap.copy(
                decorations = decor,
                destructibles = destructibles
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 20 — SYLVAN RING BASTION (Level 20)
        // Difficulty: Expert • 25 Waves
        // Area: Dense Forest + Central Water Lake & Rivers
        // Paths: Circular route around lake with 2 Mountain Tunnels + 3 Timber Bridges
        // Starting Coins: 1800
        // ==========================================
        fun createForestRingBastionMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(1200f, 800f)

            // Grand Circular Path: Carves around the central lake, crossing river channels on timber bridges and slicing through mountain caverns
            val ringPath = GamePath(
                id = "sylvan_ring_circuit",
                waypoints = listOf(
                    Point2D(-40f, 800f),      // West Spawn outside boundary
                    Point2D(220f, 800f),      // Approach to West Ridge
                    Point2D(300f, 800f),      // Tunnel 1 Entrance
                    Point2D(540f, 800f),      // Tunnel 1 Exit
                    Point2D(660f, 680f),      // Emerge into the ring basin
                    Point2D(820f, 480f),      // Northern curve
                    Point2D(980f, 400f),      // Approach Northern Bridge
                    Point2D(1040f, 400f),     // Bridge 1 Start (Northern River)
                    Point2D(1360f, 400f),     // Bridge 1 End
                    Point2D(1520f, 480f),     // East forest canopy curve
                    Point2D(1700f, 640f),     // Descend along eastern bank
                    Point2D(1780f, 800f),     // Approach East Ridge
                    Point2D(1780f, 860f),     // Tunnel 2 Entrance
                    Point2D(1780f, 1000f),    // Inside Tunnel 2
                    Point2D(1780f, 1120f),    // Tunnel 2 Exit
                    Point2D(1680f, 1260f),    // Southern forest curve
                    Point2D(1520f, 1380f),    // Approach Southern Bridge
                    Point2D(1360f, 1380f),    // Bridge 2 Start (Southern River)
                    Point2D(1040f, 1380f),    // Bridge 2 End
                    Point2D(860f, 1380f),     // South-West forest bank
                    Point2D(700f, 1220f),     // Ascend West bank completing circular ring
                    Point2D(660f, 1020f),     // Western Lake bank
                    Point2D(740f, 880f),      // Turn inward toward Citadel Causeway
                    Point2D(940f, 800f),      // Bridge 3 Start (Citadel Causeway Bridge)
                    Point2D(1140f, 800f),     // Bridge 3 End onto Island Base
                    basePos                   // Island Citadel Fortress Base (1200f, 800f)
                ),
                pathWidth = 52f
            )

            // Tunnel 1: West Ridge Mountain Cavern
            val tunnelWest = TunnelRegion(
                id = "sylvan_tunnel_west",
                boundsLeft = 280f,
                boundsTop = 680f,
                boundsRight = 560f,
                boundsBottom = 920f,
                entrance = Point2D(300f, 800f),
                exit = Point2D(540f, 800f)
            )

            // Tunnel 2: East Ridge Mountain Cavern
            val tunnelEast = TunnelRegion(
                id = "sylvan_tunnel_east",
                boundsLeft = 1680f,
                boundsTop = 840f,
                boundsRight = 1880f,
                boundsBottom = 1140f,
                entrance = Point2D(1780f, 860f),
                exit = Point2D(1780f, 1120f)
            )

            // Bridges crossing over water channels
            val bridges = listOf(
                BridgeSegment("bridge_north", Point2D(1040f, 400f), Point2D(1360f, 400f), width = 56f),
                BridgeSegment("bridge_south", Point2D(1360f, 1380f), Point2D(1040f, 1380f), width = 56f),
                BridgeSegment("bridge_citadel", Point2D(940f, 800f), Point2D(1140f, 800f), width = 58f)
            )

            // Water features: Central scenic lake, river channels, and sylvan ponds
            val centerLake = Point2D(1200f, 800f)
            val centerLakeRadius = 230f
            val northRiver = WaterPond(Point2D(1200f, 400f), 125f)
            val southRiver = WaterPond(Point2D(1200f, 1380f), 125f)
            val northwestPond = WaterPond(Point2D(460f, 320f), 95f)
            val southeastPond = WaterPond(Point2D(1980f, 1320f), 105f)

            val baseMap = GameMap(
                id = "forest_ring_bastion",
                name = "Sylvan Ring Bastion",
                description = "Primeval Ring Citadel (Expert • 25 Waves): A scenic sylvan basin nestled in dense ancient forest and tranquil blue waters. Enemies march along a circular river route crossing timber bridges and cutting through two mountain ridge tunnels. 25 relentless waves culminating in synchronized triple-threat assaults!",
                environmentType = EnvironmentType.GREEN_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(ringPath),
                basePosition = basePos,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                defaultZoom = 0.85f,
                totalWaves = 25,
                missionChapter = "SYLVAN REALM",
                startingCoins = 1800,
                difficulty = "Expert",
                waterPondCenter = centerLake,
                waterPondRadius = centerLakeRadius,
                waterPonds = listOf(northRiver, southRiver, northwestPond, southeastPond),
                tunnelRegion = tunnelWest,
                tunnelRegions = listOf(tunnelWest, tunnelEast),
                bridges = bridges
            )

            val decor = mutableListOf<MapDecoration>()

            // 1. Dense Ancient Sylvan Forest Canopy (Oak & Pine Trees around border)
            val perimeterForest = listOf(
                Point2D(140f, 140f) to DecorationType.OAK_TREE,
                Point2D(340f, 120f) to DecorationType.PINE_TREE,
                Point2D(640f, 100f) to DecorationType.OAK_TREE,
                Point2D(880f, 110f) to DecorationType.PINE_TREE,
                Point2D(1120f, 90f) to DecorationType.OAK_TREE,
                Point2D(1440f, 100f) to DecorationType.PINE_TREE,
                Point2D(1680f, 110f) to DecorationType.OAK_TREE,
                Point2D(1920f, 130f) to DecorationType.PINE_TREE,
                Point2D(2160f, 160f) to DecorationType.OAK_TREE,
                Point2D(2260f, 400f) to DecorationType.PINE_TREE,
                Point2D(2240f, 650f) to DecorationType.OAK_TREE,
                Point2D(2220f, 920f) to DecorationType.PINE_TREE,
                Point2D(2250f, 1180f) to DecorationType.OAK_TREE,
                Point2D(2160f, 1440f) to DecorationType.PINE_TREE,
                Point2D(1920f, 1480f) to DecorationType.OAK_TREE,
                Point2D(1680f, 1500f) to DecorationType.PINE_TREE,
                Point2D(1440f, 1510f) to DecorationType.OAK_TREE,
                Point2D(1120f, 1520f) to DecorationType.PINE_TREE,
                Point2D(880f, 1500f) to DecorationType.OAK_TREE,
                Point2D(640f, 1480f) to DecorationType.PINE_TREE,
                Point2D(380f, 1460f) to DecorationType.OAK_TREE,
                Point2D(160f, 1420f) to DecorationType.PINE_TREE,
                Point2D(120f, 1150f) to DecorationType.OAK_TREE,
                Point2D(130f, 550f) to DecorationType.PINE_TREE
            )
            var decId = 0
            for ((pos, type) in perimeterForest) {
                decor.add(MapDecoration(id = "sylvan_dec_${decId++}", type = type, position = pos, size = 38f))
            }

            // 2. Scenic Mountain Boulders around the tunnel ridges
            val mountainBoulders = listOf(
                Point2D(260f, 640f),
                Point2D(580f, 640f),
                Point2D(260f, 960f),
                Point2D(580f, 960f),
                Point2D(1640f, 800f),
                Point2D(1920f, 800f),
                Point2D(1640f, 1180f),
                Point2D(1920f, 1180f)
            )
            for (pos in mountainBoulders) {
                decor.add(MapDecoration(id = "sylvan_dec_${decId++}", type = DecorationType.BOULDER, position = pos, size = 32f))
            }

            // 3. Glowing Emerald Mushrooms & Wildflowers along the lake edge
            val floraDecor = listOf(
                Point2D(980f, 620f) to DecorationType.GLOW_MUSHROOM,
                Point2D(1420f, 620f) to DecorationType.FLOWER_PATCH,
                Point2D(1420f, 980f) to DecorationType.GLOW_MUSHROOM,
                Point2D(980f, 980f) to DecorationType.FLOWER_PATCH,
                Point2D(1080f, 260f) to DecorationType.FLOWER_PATCH,
                Point2D(1320f, 260f) to DecorationType.GLOW_MUSHROOM,
                Point2D(1080f, 1500f) to DecorationType.FLOWER_PATCH,
                Point2D(1320f, 1500f) to DecorationType.GLOW_MUSHROOM
            )
            for ((pos, type) in floraDecor) {
                decor.add(MapDecoration(id = "sylvan_dec_${decId++}", type = type, position = pos, size = 26f))
            }

            // 4. Natural Destructibles (Clearing rewards tokens and creates open gun spots)
            val destructibles = mutableListOf(
                DestructibleObject(
                    id = "destructible_tree_nw",
                    type = DestructibleType.OAK_TREE,
                    position = Point2D(340f, 500f),
                    maxHp = 140f,
                    currentHp = 140f,
                    rewardTokens = 15,
                    radius = 24f
                ),
                DestructibleObject(
                    id = "destructible_rock_ne",
                    type = DestructibleType.LARGE_BOULDER,
                    position = Point2D(1560f, 280f),
                    maxHp = 180f,
                    currentHp = 180f,
                    rewardTokens = 20,
                    radius = 25f
                ),
                DestructibleObject(
                    id = "destructible_crate_e",
                    type = DestructibleType.WOODEN_CRATE,
                    position = Point2D(1960f, 600f),
                    maxHp = 100f,
                    currentHp = 100f,
                    rewardTokens = 15,
                    radius = 22f
                ),
                DestructibleObject(
                    id = "destructible_tree_se",
                    type = DestructibleType.PINE_TREE,
                    position = Point2D(1480f, 1520f),
                    maxHp = 150f,
                    currentHp = 150f,
                    rewardTokens = 18,
                    radius = 24f
                ),
                DestructibleObject(
                    id = "destructible_rock_sw",
                    type = DestructibleType.LARGE_BOULDER,
                    position = Point2D(500f, 1160f),
                    maxHp = 170f,
                    currentHp = 170f,
                    rewardTokens = 20,
                    radius = 25f
                )
            )

            val finalMap = baseMap.copy(
                decorations = decor,
                destructibles = destructibles
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 21 — FROZEN PASS (Hard / Endgame)
        // Long winding mountain pass with hairpin S-curves, limited buildable perches,
        // pine trees and ice boulders, and 25 waves with 3-6 bosses every wave (10 on wave 25).
        // ==========================================
        fun createFrozenPassMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 1400f)

            val pathWinding = GamePath(
                id = "frozen_pass_main",
                waypoints = listOf(
                    Point2D(-40f, 260f),
                    Point2D(500f, 260f),
                    Point2D(800f, 480f),
                    Point2D(400f, 720f),
                    Point2D(800f, 960f),
                    Point2D(1300f, 960f),
                    Point2D(1600f, 600f),
                    Point2D(1200f, 280f),
                    Point2D(1900f, 280f),
                    Point2D(2150f, 650f),
                    Point2D(1800f, 1050f),
                    Point2D(1350f, 1400f),
                    basePos
                ),
                pathWidth = 52f
            )

            val decor = listOf(
                MapDecoration("fp_dec_1", DecorationType.PINE_TREE, Point2D(150f, 120f), 38f),
                MapDecoration("fp_dec_2", DecorationType.BOULDER, Point2D(650f, 150f), 32f),
                MapDecoration("fp_dec_3", DecorationType.SNOW_PILE, Point2D(950f, 480f), 28f),
                MapDecoration("fp_dec_4", DecorationType.FROZEN_BUSH, Point2D(250f, 720f), 24f),
                MapDecoration("fp_dec_5", DecorationType.PINE_TREE, Point2D(1450f, 800f), 38f),
                MapDecoration("fp_dec_6", DecorationType.BOULDER, Point2D(1400f, 450f), 34f),
                MapDecoration("fp_dec_7", DecorationType.SNOW_PILE, Point2D(1750f, 450f), 30f),
                MapDecoration("fp_dec_8", DecorationType.PINE_TREE, Point2D(2250f, 450f), 38f),
                MapDecoration("fp_dec_9", DecorationType.FROZEN_BUSH, Point2D(2050f, 900f), 26f),
                MapDecoration("fp_dec_10", DecorationType.BOULDER, Point2D(1600f, 1200f), 32f)
            )

            val destructibles = listOf(
                DestructibleObject("fp_dest_1", DestructibleType.PINE_TREE, Point2D(200f, 480f), 180f, 180f, 25, 24f),
                DestructibleObject("fp_dest_2", DestructibleType.LARGE_BOULDER, Point2D(1050f, 680f), 220f, 220f, 30, 26f),
                DestructibleObject("fp_dest_3", DestructibleType.PINE_TREE, Point2D(1550f, 140f), 190f, 190f, 25, 24f),
                DestructibleObject("fp_dest_4", DestructibleType.LARGE_BOULDER, Point2D(1500f, 1200f), 240f, 240f, 30, 26f),
                DestructibleObject("fp_dest_5", DestructibleType.LARGE_BOULDER, Point2D(600f, 1200f), 200f, 200f, 28, 25f)
            )

            val finalMap = GameMap(
                id = "frozen_pass",
                name = "Frozen Pass",
                description = "Winding alpine mountain pass with treacherous glacial switchbacks, restricted perches, and constant boss assault battalions.",
                environmentType = EnvironmentType.SNOW_VALLEY,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathWinding),
                basePosition = basePos,
                decorations = decor,
                destructibles = destructibles,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                totalWaves = 25,
                missionChapter = "GLACIAL RIDGE",
                startingCoins = 1400,
                difficulty = "Hard"
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 22 — OBSIDIAN CROSSFIRE (Hard / Endgame)
        // Dual intersecting subterranean lava canyon paths with cavern tunnels,
        // heavy armor mechs, stealth assassins, and 25 intense waves.
        // ==========================================
        fun createObsidianCrossfireMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 800f)

            val pathNorth = GamePath(
                id = "obsidian_north",
                waypoints = listOf(
                    Point2D(200f, -40f),
                    Point2D(450f, 350f),
                    Point2D(850f, 550f),
                    Point2D(1200f, 800f),
                    Point2D(1550f, 1150f),
                    Point2D(1900f, 1150f),
                    basePos
                ),
                pathWidth = 52f
            )

            val pathSouth = GamePath(
                id = "obsidian_south",
                waypoints = listOf(
                    Point2D(200f, 1640f),
                    Point2D(450f, 1250f),
                    Point2D(850f, 1050f),
                    Point2D(1200f, 800f),
                    Point2D(1550f, 450f),
                    Point2D(1900f, 450f),
                    basePos
                ),
                pathWidth = 52f
            )

            val tunnel = TunnelRegion(
                id = "obsidian_hub_tunnel",
                boundsLeft = 1050f,
                boundsTop = 680f,
                boundsRight = 1350f,
                boundsBottom = 920f,
                entrance = Point2D(1050f, 800f),
                exit = Point2D(1350f, 800f)
            )

            val decor = listOf(
                MapDecoration("oc_dec_1", DecorationType.CRYSTAL, Point2D(650f, 800f), 30f),
                MapDecoration("oc_dec_2", DecorationType.BOULDER, Point2D(1200f, 450f), 36f),
                MapDecoration("oc_dec_3", DecorationType.BOULDER, Point2D(1200f, 1150f), 36f),
                MapDecoration("oc_dec_4", DecorationType.CRYSTAL, Point2D(1750f, 800f), 32f),
                MapDecoration("oc_dec_5", DecorationType.LANTERN_POST, Point2D(1000f, 750f), 24f),
                MapDecoration("oc_dec_6", DecorationType.LANTERN_POST, Point2D(1400f, 750f), 24f)
            )

            val destructibles = listOf(
                DestructibleObject("oc_dest_1", DestructibleType.LARGE_BOULDER, Point2D(450f, 800f), 220f, 220f, 30, 26f),
                DestructibleObject("oc_dest_2", DestructibleType.WOODEN_CRATE, Point2D(1200f, 200f), 150f, 150f, 25, 22f),
                DestructibleObject("oc_dest_3", DestructibleType.WOODEN_CRATE, Point2D(1200f, 1400f), 150f, 150f, 25, 22f),
                DestructibleObject("oc_dest_4", DestructibleType.LARGE_BOULDER, Point2D(1900f, 800f), 240f, 240f, 30, 26f)
            )

            val finalMap = GameMap(
                id = "obsidian_crossfire",
                name = "Obsidian Crossfire",
                description = "Subterranean volcanic chasm where dual assault routes intersect beneath obsidian arches and magma bridges.",
                environmentType = EnvironmentType.OBSIDIAN_TUNNEL,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathSouth),
                basePosition = basePos,
                decorations = decor,
                destructibles = destructibles,
                tunnelRegion = tunnel,
                tunnelRegions = listOf(tunnel),
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                totalWaves = 25,
                missionChapter = "OBSIDIAN CRUCIBLE",
                startingCoins = 1500,
                difficulty = "Hard"
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 23 — TEMPEST RAVINE (Extreme / Endgame)
        // Three converging river cataracts under torrential storm rain,
        // sky drakes, shield vanguards, healers, and multi-boss squads.
        // ==========================================
        fun createTempestRavineMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(1200f, 800f)

            val pathNorth = GamePath(
                id = "tempest_north",
                waypoints = listOf(
                    Point2D(1200f, -40f),
                    Point2D(1200f, 350f),
                    Point2D(950f, 550f),
                    basePos
                ),
                pathWidth = 52f
            )

            val pathWest = GamePath(
                id = "tempest_west",
                waypoints = listOf(
                    Point2D(-40f, 800f),
                    Point2D(450f, 800f),
                    Point2D(700f, 1100f),
                    basePos
                ),
                pathWidth = 52f
            )

            val pathEast = GamePath(
                id = "tempest_east",
                waypoints = listOf(
                    Point2D(2440f, 800f),
                    Point2D(1950f, 800f),
                    Point2D(1700f, 1100f),
                    basePos
                ),
                pathWidth = 52f
            )

            val ponds = listOf(
                WaterPond(Point2D(750f, 500f), 85f),
                WaterPond(Point2D(1650f, 500f), 85f),
                WaterPond(Point2D(1200f, 1350f), 105f)
            )

            val decor = listOf(
                MapDecoration("tr_dec_1", DecorationType.OAK_TREE, Point2D(450f, 200f), 38f),
                MapDecoration("tr_dec_2", DecorationType.OAK_TREE, Point2D(1950f, 200f), 38f),
                MapDecoration("tr_dec_3", DecorationType.BOULDER, Point2D(750f, 750f), 32f),
                MapDecoration("tr_dec_4", DecorationType.BOULDER, Point2D(1650f, 750f), 32f),
                MapDecoration("tr_dec_5", DecorationType.GLOW_MUSHROOM, Point2D(1200f, 1050f), 26f)
            )

            val destructibles = listOf(
                DestructibleObject("tr_dest_1", DestructibleType.PINE_TREE, Point2D(450f, 400f), 190f, 190f, 25, 24f),
                DestructibleObject("tr_dest_2", DestructibleType.PINE_TREE, Point2D(1950f, 400f), 190f, 190f, 25, 24f),
                DestructibleObject("tr_dest_3", DestructibleType.LARGE_BOULDER, Point2D(450f, 1350f), 220f, 220f, 30, 26f),
                DestructibleObject("tr_dest_4", DestructibleType.LARGE_BOULDER, Point2D(1950f, 1350f), 220f, 220f, 30, 26f)
            )

            val finalMap = GameMap(
                id = "tempest_ravine",
                name = "Tempest Ravine",
                description = "Three raging river forks storm through thunderous cataracts into the flooded bastion under heavy torrential rain.",
                environmentType = EnvironmentType.TEMPEST_RAIN,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathNorth, pathWest, pathEast),
                basePosition = basePos,
                decorations = decor,
                waterPonds = ponds,
                destructibles = destructibles,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                totalWaves = 25,
                missionChapter = "TEMPEST CATARACTS",
                startingCoins = 1600,
                difficulty = "Extreme",
                hasRain = true
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 24 — SOLSTICE CITADEL (Master / Endgame)
        // Dual spiral siege lines circling a cosmic sanctuary during Day/Night cycle,
        // stealth stalkers, void summoners, and intense boss assault waves.
        // ==========================================
        fun createEclipseCitadelMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(1200f, 800f)

            val pathSpiral1 = GamePath(
                id = "solstice_spiral_1",
                waypoints = listOf(
                    Point2D(-40f, 250f),
                    Point2D(2150f, 250f),
                    Point2D(2150f, 1350f),
                    Point2D(650f, 1350f),
                    Point2D(650f, 650f),
                    Point2D(1200f, 650f),
                    basePos
                ),
                pathWidth = 52f
            )

            val pathSpiral2 = GamePath(
                id = "solstice_spiral_2",
                waypoints = listOf(
                    Point2D(2440f, 1350f),
                    Point2D(250f, 1350f),
                    Point2D(250f, 450f),
                    Point2D(1750f, 450f),
                    Point2D(1750f, 950f),
                    Point2D(1200f, 950f),
                    basePos
                ),
                pathWidth = 52f
            )

            val decor = listOf(
                MapDecoration("ec_dec_1", DecorationType.CRYSTAL, Point2D(1200f, 400f), 32f),
                MapDecoration("ec_dec_2", DecorationType.CRYSTAL, Point2D(1200f, 1200f), 32f),
                MapDecoration("ec_dec_3", DecorationType.BOULDER, Point2D(450f, 900f), 36f),
                MapDecoration("ec_dec_4", DecorationType.BOULDER, Point2D(1950f, 700f), 36f),
                MapDecoration("ec_dec_5", DecorationType.LANTERN_POST, Point2D(1050f, 750f), 24f),
                MapDecoration("ec_dec_6", DecorationType.LANTERN_POST, Point2D(1350f, 750f), 24f)
            )

            val destructibles = listOf(
                DestructibleObject("ec_dest_1", DestructibleType.LARGE_BOULDER, Point2D(1200f, 1500f), 240f, 240f, 32, 26f),
                DestructibleObject("ec_dest_2", DestructibleType.LARGE_BOULDER, Point2D(1200f, 100f), 240f, 240f, 32, 26f),
                DestructibleObject("ec_dest_3", DestructibleType.OAK_TREE, Point2D(100f, 900f), 180f, 180f, 25, 24f),
                DestructibleObject("ec_dest_4", DestructibleType.OAK_TREE, Point2D(2300f, 800f), 180f, 180f, 25, 24f)
            )

            val finalMap = GameMap(
                id = "eclipse_citadel",
                name = "Solstice Citadel",
                description = "Dual spiral siege lines orbiting the cosmic apex sanctuary during celestial day-and-night cycles.",
                environmentType = EnvironmentType.DAY_NIGHT,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathSpiral1, pathSpiral2),
                basePosition = basePos,
                decorations = decor,
                destructibles = destructibles,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                totalWaves = 25,
                missionChapter = "SOLSTICE SANCTUARY",
                startingCoins = 1700,
                difficulty = "Master"
            )
            validateMap(finalMap)
            return finalMap
        }

        // ==========================================
        // MAP 25 — APEX DRAGON SANCTUM (Grandmaster / Climax)
        // Two interlocking serpentine dragon ridges, subterranean tunnels,
        // all enemy archetypes combined, and 10 colossal Apex Bosses in Wave 25!
        // ==========================================
        fun createApexDragonSanctumMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap {
            val basePos = Point2D(2200f, 800f)

            val pathUpper = GamePath(
                id = "dragon_spine_upper",
                waypoints = listOf(
                    Point2D(-40f, 400f),
                    Point2D(450f, 400f),
                    Point2D(750f, 220f),
                    Point2D(1150f, 220f),
                    Point2D(1350f, 550f),
                    Point2D(950f, 800f),
                    Point2D(1350f, 1150f),
                    Point2D(1750f, 1150f),
                    Point2D(1950f, 950f),
                    basePos
                ),
                pathWidth = 52f
            )

            val pathLower = GamePath(
                id = "dragon_spine_lower",
                waypoints = listOf(
                    Point2D(-40f, 1200f),
                    Point2D(450f, 1200f),
                    Point2D(750f, 1380f),
                    Point2D(1150f, 1380f),
                    Point2D(1350f, 1050f),
                    Point2D(950f, 800f),
                    Point2D(1350f, 450f),
                    Point2D(1750f, 450f),
                    Point2D(1950f, 650f),
                    basePos
                ),
                pathWidth = 52f
            )

            val tunnel = TunnelRegion(
                id = "dragon_sanctum_tunnel",
                boundsLeft = 850f,
                boundsTop = 700f,
                boundsRight = 1050f,
                boundsBottom = 900f,
                entrance = Point2D(850f, 800f),
                exit = Point2D(1050f, 800f)
            )

            val decor = listOf(
                MapDecoration("ads_dec_1", DecorationType.CRYSTAL, Point2D(950f, 500f), 34f),
                MapDecoration("ads_dec_2", DecorationType.CRYSTAL, Point2D(950f, 1100f), 34f),
                MapDecoration("ads_dec_3", DecorationType.BOULDER, Point2D(1550f, 800f), 38f),
                MapDecoration("ads_dec_4", DecorationType.PINE_TREE, Point2D(450f, 200f), 38f),
                MapDecoration("ads_dec_5", DecorationType.PINE_TREE, Point2D(450f, 1400f), 38f),
                MapDecoration("ads_dec_6", DecorationType.LANTERN_POST, Point2D(2050f, 750f), 24f)
            )

            val destructibles = listOf(
                DestructibleObject("ads_dest_1", DestructibleType.LARGE_BOULDER, Point2D(450f, 800f), 260f, 260f, 35, 26f),
                DestructibleObject("ads_dest_2", DestructibleType.WOODEN_CRATE, Point2D(950f, 90f), 180f, 180f, 28, 22f),
                DestructibleObject("ads_dest_3", DestructibleType.WOODEN_CRATE, Point2D(950f, 1510f), 180f, 180f, 28, 22f),
                DestructibleObject("ads_dest_4", DestructibleType.LARGE_BOULDER, Point2D(1750f, 800f), 260f, 260f, 35, 26f)
            )

            val finalMap = GameMap(
                id = "apex_dragon_sanctum",
                name = "Dragon Sanctum",
                description = "The ultimate 25-wave endgame crucible. Serpentine dragon ridges, subterranean tunnels, and colossal titan battalions.",
                environmentType = EnvironmentType.DRAGON_COIL,
                isUnlocked = isUnlocked,
                starsEarned = stars,
                paths = listOf(pathUpper, pathLower),
                basePosition = basePos,
                decorations = decor,
                destructibles = destructibles,
                tunnelRegion = tunnel,
                tunnelRegions = listOf(tunnel),
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(1200f, 800f),
                totalWaves = 25,
                missionChapter = "APEX CRUCIBLE",
                startingCoins = 2000,
                difficulty = "Grandmaster"
            )
            validateMap(finalMap)
            return finalMap
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
                if (distToBase > 60f && map.environmentType != EnvironmentType.TEMPEST_RAIN && map.id != "storm_twin_bastion") {
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
