package com.example.data

import com.example.entities.GamePath
import com.example.entities.Point2D

enum class EnvironmentType {
    GREEN_VALLEY,
    DESERT_CANYON,
    FOREST_CROSSROADS,
    OBSIDIAN_TUNNEL,
    DRAGON_COIL
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
    SIGNPOST
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
    val worldWidth: Float = 2000f,
    val worldHeight: Float = 1500f,
    val startingCameraCenter: Point2D = Point2D(460f, 440f),
    val defaultZoom: Float = 1.0f
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
        worldWidth: Float = 2000f,
        worldHeight: Float = 1500f,
        startingCameraCenter: Point2D = Point2D(460f, 440f),
        defaultZoom: Float = 1.0f
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
        worldWidth = worldWidth,
        worldHeight = worldHeight,
        startingCameraCenter = startingCameraCenter,
        defaultZoom = defaultZoom
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

    companion object {
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
                worldWidth = 2200f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(650f, 500f),
                defaultZoom = 0.95f
            )
            validateMap(map)
            return map
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
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.90f
            )
            validateMap(map)
            return map
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
                worldWidth = 2800f,
                worldHeight = 1900f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.85f
            )
            validateMap(map)
            return map
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
                worldWidth = 3000f,
                worldHeight = 2000f,
                startingCameraCenter = Point2D(700f, 1000f),
                defaultZoom = 0.80f
            )
            validateMap(map)
            return map
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
                tunnelRegion = tunnel,
                worldWidth = 2400f,
                worldHeight = 1600f,
                startingCameraCenter = Point2D(650f, 450f),
                defaultZoom = 0.90f
            )
            validateMap(map)
            return map
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
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(1000f, 750f),
                defaultZoom = 0.90f
            )
            validateMap(map)
            return map
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
                worldWidth = 2600f,
                worldHeight = 1700f,
                startingCameraCenter = Point2D(1200f, 950f),
                defaultZoom = 0.90f
            )
            validateMap(map)
            return map
        }

        // Aliases for Map 4 and Map 5
        fun createObsidianTunnelMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createObsidianGrottoMap(isUnlocked, stars)

        fun createDragonCoilMap(isUnlocked: Boolean = false, stars: Int = 0): GameMap =
            createDragonsCoilMap(isUnlocked, stars)

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
        }
    }
}
