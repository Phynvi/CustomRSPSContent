package com.rs.game.player.content.araxxor;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.rs.cores.CoresManager;
import com.rs.game.*;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;

/**
 * An environment configuration object containing all instance region information and
 * current rotation information.
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)<br/>
 *        1.1 (beta, 08/19/2017)
 */
public final class AraxxorEnvironment {

    private static final int NE_CHUNK_X = 579;
    private static final int NE_CHUNK_Y = 786;
    private static final int SW_CHUNK_X = 560;
    private static final int SW_CHUNK_Y = 780;

    private static final int[] northEastChunks = new int[] {NE_CHUNK_X, NE_CHUNK_Y};
    private static final int[] southWestChunks = new int[] {SW_CHUNK_X, SW_CHUNK_Y};

    private static final int chunkDimX = Math.abs(northEastChunks[0] - southWestChunks[0]);
    private static final int chunkDimY = Math.abs(northEastChunks[1] - southWestChunks[1]);

    private static final WorldTile TRUE_START_TILE = new WorldTile(4490, 6266, 1);
    private static final WorldTile TRUE_EXIT_TILE = new WorldTile(4599, 6268, 1);

    private int[] boundChunks;
    private final Paths[] currentPaths;

    private WorldTile rampReference;
    private WorldTile lpWallRerence;
    private WorldTile cutsceneAnchorOne;
    private WorldTile cutsceneAnchorTwo;
    private WorldTile cutsceneLookOne;
    private WorldTile cutsceneLookTwo;
    private WorldTile cutsceneAnchorThree;
    private WorldTile cutsceneLookThree;
    private WorldTile startTile;
    private WorldTile exitTile;

    private AraxxorFight fight;

    private List<WorldTile> acidPool;
    private List<WorldTile> lightTiles;

    private Paths activePath;


    /**
     * Constructs a new Araxxor environment configuration object.
     * @param fight           the {@link AraxxorFight} object bound to this environment
     * @param currentPaths    the available paths in this environment configuration, determined
     *                        by the current Araxxor cycle. See {@link AraxxorManager}.
     */
    AraxxorEnvironment(AraxxorFight fight, Paths[] currentPaths) {
        this.fight = fight;
        this.currentPaths = currentPaths;
        buildEnvironment();
    }

    /**
     * Constructs the initial environment layout. This process binds the map chunks corresponding
     * to the ground-truth Araxxor cave to an instance chunks array, spawns the rotation-appropriate
     * path entrances, and sends the players in the fight to the instanced region.
     */
    private void buildEnvironment() {

        CoresManager.getServiceProvider().executeNow(() -> {
            boundChunks = MapBuilder.findEmptyChunkBound(chunkDimX, chunkDimY);
            MapBuilder.copyAllPlanesMap(southWestChunks[0],
                    southWestChunks[1], boundChunks[0], boundChunks[1], chunkDimX, chunkDimY);
            initAcidPool();
            initReferenceTiles();
            Arrays.stream(currentPaths).forEach(path -> {
                WorldObject web = new WorldObject(path.getWebId(), 10, 0,
                        getRelativeTile(path.getEntranceTile()));
                WorldObject click = new WorldObject(path.getObjectId(), 10, 0,
                        getRelativeTile(path.getEntranceClickTile()));
                World.spawnObject(web);
                clipTiles(getRelativeTile(path.clipRoot));
                World.spawnObject(click);
            });
            Paths offPath = AraxxorManager.getOffPath();
            WorldObject rocks = new WorldObject(offPath.getBlockedId(),
                    10, 0, getRelativeTile(offPath.getEntranceTile()));
            World.spawnObject(rocks);
            clipTiles(getRelativeTile(offPath.clipRoot));
            World.spawnObject(new WorldObject(91520, 10, 0, rampReference));
            World.spawnObject(new WorldObject(91514, 10, 0, lpWallRerence));
            clipTiles(lpWallRerence);
            fight.getPlayers().forEach(player -> player.setNextWorldTile(startTile));
        });
    }

    /** Unbinds the instanced map chunks. */
    void destroyEnvironment() {
        CoresManager.getServiceProvider().executeWithDelay(() -> MapBuilder.destroyMap(boundChunks[0],
                boundChunks[1], chunkDimX, chunkDimY), 10000, TimeUnit.MILLISECONDS);

    }

    /**
     * Accessor method for the current active path.
     * @return the active path of the fight.
     */
    public Paths getActivePath() {
        return activePath;
    }

    /**
     * Accessor method for the bounded fight object.
     * @return the {@link AraxxorFight} bound to this environmnet
     */
    public AraxxorFight getFight() {
        return fight;
    }

    /**
     * Accessor method to the first cutscene anchor. This anchor
     * corresponds to the cutscene clip when looking down  light path
     * at Araxxor before he charges.
     * @return the first cutscene anchor tile pointer.
     */
    WorldTile getCutsceneAnchorOne() {
        return cutsceneAnchorOne;
    }

    /**
     * Accessor method to the second cutscene anchor. This anchor
     * corresponds to the cutscene clip of Araxxor climbing
     * back up onto the ceiling after hitting the wall.
     * @return the second cutscene anchor tile pointer.
     */
    WorldTile getCutsceneAnchorTwo() {
        return cutsceneAnchorTwo;
    }

    /**
     * Accessor method to the first "look" tile. This gives
     * a quasi-first person view of Araxxor before charging.
     * @return the first cutscene "look" tile.
     */
    WorldTile getCutsceneLookOne() {
        return cutsceneLookOne;
    }

    /**
     * Accessor method to the second "look" tile. This orients
     * the camera to look at the wall while Araxxor climbs
     * back up.
     * @return the second cutscene "look" tile pointer.
     */
    WorldTile getCutsceneLookTwo() {
        return cutsceneLookTwo;
    }

    /**
     * Accessor method to the third cutscene anchor. This anchor
     * corresponds to the cutscene clip when looking at the platform
     * Araxxor jumps from to get to the Araxxi platform.
     * @return the third cutscene anchor tile pointer.
     */
    WorldTile getCutsceneAnchorThree() {
        return cutsceneAnchorThree;
    }

    /**
     * Accessor method to the third "look" tile. This orients
     * the camera to look at the platform Araxxor jumps from.
     * @return the third cutscene "look" tile pointer.
     */
    WorldTile getCutsceneLookThree() {
        return cutsceneLookThree;
    }

    /**
     * Accessor method to the light path wall location.
     * This is the wall that Araxxor slams into during the LP cutscene.<br/><br/>
     * This field exists because the wall normally isn't spawned.
     * @return the tile location of the wall in Light Path
     */
    WorldTile getLpWallReference() {
        return lpWallRerence;
    }

    /**
     * Accessor method to the instance exit tile.
     * @return the instance exit tile pointer.
     */
    WorldTile getExitTile() {
        return exitTile;
    }

    /**
     * Accessor method to the ramp location at the end of the acid pool.<br/><br/>
     * This field exists because the ramp normally isn't spawned.
     * @return the tile location of the acid pool ramp
     */
    public WorldTile getRampReference() {
        return rampReference;
    }



    /**
     * Accessor method for the current available paths in the environment.
     * @return the array of current paths
     */
    public Paths[] getCurrentPaths() {
        return currentPaths;
    }

    /**
     * Finds a random empty, accessible 1x1 grid on which to send the egg bomb attack.
     *
     * 250 WorldTile locations are sampled in a 3 tile radius of the player, and
     * candidate grids are extracted from the sampled points.
     *
     * If a 1x1 grid cannot be found, the 'grid' defaults to a single WorldTile
     * underneath Araxxor's current target's feet.
     * @return an array of 4 WorldTile objects forming a 1x1 grid,
     *         or 1-length array containing the current targets location.
     */
    public WorldTile[] generateEggGrid(Player eggBombTarget, int radius) {

        WorldTile[] grid = new WorldTile[4];

        List<WorldTile> free = new ArrayList<>();
        WorldTile tile;
        while(free.size() < 100) {
            tile = new WorldTile(eggBombTarget, radius);
            if(verifyAllTileHashes(tile, eggBombTarget)  &&
                    World.canMoveNPC(tile, 1))
                free.add(tile);
        }

        Collections.shuffle(free);

        grid[0] = free.get(Utils.random(free.size()));
        grid[1] = new WorldTile(grid[0].getX(), grid[0].getY() + 1, grid[0].getPlane());
        grid[2] = new WorldTile(grid[0].getX() - 1, grid[0].getY(), grid[0].getPlane());
        grid[3] = new WorldTile(grid[0].getX() - 1, grid[0].getY() + 1, grid[0].getPlane());

        return grid;
    }

    /**
     * Verifies that a given WorldTile doesn't not collide with any
     * of the important, pre-existing reference tiles in the instance.
     * @param tile the tile to check for collisions
     * @return a <tt>boolean</tt> indicating whether or not
     *         there were any conflicts
     */
    private boolean verifyAllTileHashes(WorldTile tile, WorldTile eggBombTarget) {
        final int hash = tile.getTileHash();
        final boolean acidRamp = hash != rampReference.getTileHash();
        final boolean lpWall = hash != lpWallRerence.getTileHash();
        final boolean target = hash != eggBombTarget.getTileHash();
        return acidRamp && lpWall && target;
    }

    /**
     * Begins burning down the selected web. The web will burn for one minute before
     * it disappears, allowing the players to begin the next phase of the fight.
     * @param webId the id corresponding to the selected
     *              web to burn
     */
    void burnWeb(int webId) {
        fight.setWebBurning();
        activePath = Arrays.stream(currentPaths)
                           .filter(p -> webId == p.getObjectId())
                           .findFirst().orElse(null);
        Objects.requireNonNull(activePath);
        World.spawnObject(new WorldObject(activePath.getBurningWebId(),
                10, 0, fight.getConfig().getRelativeTile(activePath.getEntranceTile())));
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;

            private boolean checkShutdown() {
                boolean shutdown = fight.shuttingDown();
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 5) {
                    fight.notifyPlayersMsgBox("The web burns down, " +
                            "allowing access!");
                    unclipTiles(getRelativeTile(activePath.clipRoot));
                    World.removeObject(World.getObject(getRelativeTile(activePath.getEntranceTile())));
                    World.removeObject(World.getObject(getRelativeTile(activePath.getEntranceClickTile())));
                    if(activePath == AraxxorEnvironment.Paths.ACID)
                        fight.getBoss().setForceTargetDistance(5);
                    stop();
                }
                tick++;
            }
        }, 0, 1);

    }

    /**
     * Gets a relative tile in the instanced region given the ground-truth WorldTile.
     * @param tile the ground-truth WorldTile
     * @return the equivalent WorldTile in the instanced region
     */
    public WorldTile getRelativeTile(WorldTile tile) {
        return getRelativeTile(tile.getX(), tile.getY(), tile.getPlane());
    }

    /**
     * Converts a ground-truth x-coordinate to its equivalent value in the instanced region.
     * @param trueX the ground-truth x-coordinate
     * @return the equivalent x-coordinate in the instance
     */
    public int relativeX(int trueX) {
        return boundChunks[0] * 8 + ((trueX - southWestChunks[0] * 8) % (chunkDimX * 64));
    }

    /**
     * Converts a ground-truth y-coordinate to its equivalent value in the instanced region.
     * @param trueY the ground-truth y-coordinate
     * @return the equivalent y-coordinate in the instance
     */
    public int relativeY(int trueY) {
        return boundChunks[1] * 8 + ((trueY - southWestChunks[1] * 8) % (chunkDimY * 64));
    }

    /**
     * Checks to see if the player is standing in the light beam during the
     * light path mechanic.
     * @param player the player to check
     * @param lightTile the tile location of the light beam
     * @return whether or not the player is standing on the beam
     */
    boolean onLightTile(Player player, WorldTile lightTile) {
        WorldTile trueCenter = new WorldTile(lightTile.getX() + 1, lightTile.getY() + 1, lightTile.getPlane());
        return player.withinDistance(trueCenter, 1);
    }

    /**
     * Generates a new tile location for the next light beam during the
     * light path mechanic.
     * @return a new tile location for a light beam
     */
    WorldTile generateLightTile() {

        WorldTile lightTile = lightTiles.get(Utils.random(lightTiles.size()));
        while(!World.canMoveNPC(lightTile, 1)) {
            lightTile = lightTiles.get(Utils.random(lightTiles.size()));
        }
        return lightTile;
    }

    /**
     * Accessor method to acid pool verticies.<br/><br/>
     * In order to determine whether or not Araxxor is in the acid pool,
     * a polygon is formed which encapsulates the acid pool, and then the NPC
     * location is passed through a ray-crossing algorithm to see if the location
     * is within said polygon. The verticies of the polygon are contained
     * in the <code>acidPool</code> list.
     * @return the list of acid pool polygon verticies
     */
    public List<WorldTile> getAcidPool() {
        return acidPool;
    }

    /**
     * Initializes the acid pool verticies.
     */
    private void initAcidPool() {
        acidPool = new ArrayList<>();
        acidPool.add(getRelativeTile(new WorldTile(4523, 6258, 1)));
        acidPool.add(getRelativeTile(new WorldTile(4536, 6257, 1)));
        acidPool.add(getRelativeTile(new WorldTile(4537, 6272, 1)));
        acidPool.add(getRelativeTile(new WorldTile(4525, 6272, 1)));
    }

    /**
     * Initializes all the instance reference tiles.
     */
    private void initReferenceTiles() {
        rampReference = getRelativeTile(new WorldTile(4536, 6260, 1));
        lpWallRerence = getRelativeTile(new WorldTile(4550, 6246, 1));
        cutsceneAnchorOne = getRelativeTile(new WorldTile(4557, 6249, 1));
        cutsceneLookOne = getRelativeTile(new WorldTile(4519, 6249, 1));
        cutsceneAnchorTwo = getRelativeTile(new WorldTile(4542, 6244, 1));
        cutsceneLookTwo = getRelativeTile(new WorldTile(4563, 6259, 1));
        cutsceneAnchorThree = getRelativeTile(new WorldTile(4601, 6265, 1));
        cutsceneLookThree = getRelativeTile(new WorldTile(4580, 6265, 1));
        lightTiles = new ArrayList<>();
        for(int i = relativeX(4519); i <= relativeX(4550); i++) {
            for(int j = relativeY(6244); j <= relativeY(6254); j++) {
                lightTiles.add(new WorldTile(i, j, 1));
            }
        }
        startTile = getRelativeTile(TRUE_START_TILE);
        exitTile = getRelativeTile(TRUE_EXIT_TILE);
    }

    /**
     * Clips tiles in a square region spanned from the supplied
     * clipping root. This is used for inducing clippling behaviour
     * on the webs and rock walls (which for some reason aren't clippable
     * in their definitions).
     * @param clipRoot the root location to begin clipping tiles from
     */
    private void clipTiles(WorldTile clipRoot) {
        CoresManager.getServiceProvider().executeWithDelay(() -> {
            for(int i = 0; i < 8; i++) {
                for(int j = 0; j < 8; j++) {
                    World.getRegion(clipRoot.getRegionId()).forceGetRegionMap().clipTile(clipRoot.getPlane(),
                            clipRoot.getXInRegion() + i,
                            clipRoot.getYInRegion() + j);
                }
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Unclips the tiles originally clipped by {@link AraxxorEnvironment#clipTiles(WorldTile)}.
     * @param clipRoot the root location to begin unclipping tiles from
     */
    void unclipTiles(WorldTile clipRoot) {
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                World.getRegion(clipRoot.getRegionId()).forceGetRegionMap().unclipTile(clipRoot.getPlane(),
                        clipRoot.getXInRegion() + i,
                        clipRoot.getYInRegion() + j);
            }
        }
    }

    /**
     * Unclips a specified list of tiles.
     * @param tiles the tiles to unclip
     */
    public void unclipTiles(List<WorldTile> tiles) {
        tiles.forEach(t -> World.getRegion(t.getRegionId())
                .forceGetRegionMap().unclipTile(
                        t.getPlane(),
                        t.getXInRegion(),
                        t.getYInRegion()
                ));
    }

    /**
     * Gets a relative tile in the instanced region given the ground-truth x, y, and plane coordinate.
     * @param trueX the ground-truth x coordinate
     * @param trueY the ground-truth y coordinate
     * @param truePlane the ground-truth plane coordinate
     * @return the equivalent WorldTile in the instanced region
     */
    private WorldTile getRelativeTile(int trueX, int trueY, int truePlane) {
        int relX = relativeX(trueX);
        int relY = relativeY(trueY);
        return new WorldTile(relX, relY, truePlane);
    }

    public enum Paths {

        LIGHT(91667, 91511, 91507, 91512,
                new WorldTile(4507, 6248, 1), new WorldTile(4509, 6252, 1),
                new WorldTile(4509, 6246, 1)),
        ACID(91668, 91509, 91506, 91510,
                new WorldTile(4512, 6261, 1), new WorldTile(4513, 6264, 1),
                new WorldTile(4514, 6261, 1)),
        MINIONS(91669, 91504, 91505, 91508,
                new WorldTile(4504, 6272, 1), new WorldTile(4506, 6274, 1),
                new WorldTile(4506, 6274, 1));

        private int objectId;
        private int webId;
        private int burningWebId;
        private int blockedId;
        private WorldTile entranceTile;
        private WorldTile entranceClickTile;
        private WorldTile clipRoot;

        Paths(int objectId, int webId, int burningWebId, int blockedId, WorldTile entranceTile,
              WorldTile entranceClickTile, WorldTile clipRoot) {
            this.objectId = objectId;
            this.webId = webId;
            this.burningWebId = burningWebId;
            this.blockedId = blockedId;
            this.entranceTile = entranceTile;
            this.entranceClickTile = entranceClickTile;
            this.clipRoot = clipRoot;
        }

        protected int getObjectId() {
            return objectId;
        }
        protected int getWebId() {
            return webId;
        }
        protected int getBurningWebId() {
            return burningWebId;
        }
        protected int getBlockedId() {
            return blockedId;
        }
        protected WorldTile getEntranceTile() {
            return entranceTile;
        }
        protected WorldTile getEntranceClickTile() {
            return entranceClickTile;
        }
    }

}
