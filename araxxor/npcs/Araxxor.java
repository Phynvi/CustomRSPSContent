package com.rs.game.player.content.araxxor.npcs;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.rs.cores.CoresManager;
import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.NewProjectile;
import com.rs.game.World;
import com.rs.game.WorldObject;
import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.AraxxorEnvironment;
import com.rs.game.player.content.araxxor.AraxxorFight;
import com.rs.game.player.content.araxxor.AraxxorInterfaces;
import com.rs.game.player.content.araxxor.AraxxorManager;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Logger;
import com.rs.utils.Utils;

/**
 * The {@code Araxxor} class defines NPC spawning mechanics and is directly linked to an {@link AraxxorFight} object.
 * The Araxxor combat script will refer directly to the {@code Araxxor} object to perform special attacks. All special
 * attacks are self-contained in private methods in the class, or in package-access methods in the parent {@link Araxyte}
 * class.
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public final class Araxxor extends Araxyte {

    private static final long serialVersionUID = -364604734529958613L;

    private int globalAttackTick;
    private int minionSpawns;
    private int nextMinionSpawn;
    private int originalId;
    private int totalAbsorbedAcid;
    private int totalRampHealth;
    private int leftoverPhaseOne;
    private int leftoverPhaseTwo;

    private boolean playersNotified;
    private boolean sentEggBomb;
    private boolean absorbingAcid;
    private boolean drainingAcid;

    private Player eggBombTarget;
    private SwingDirection swingDirection;

    /**
     * Constructs an {@code Araxxor} NPC object.
     * @param id the NPC id
     * @param tile spawn location
     * @param fight the {@link AraxxorFight} to which this {@code Araxxor} instance belongs.
     */
    public Araxxor(int id, WorldTile tile, AraxxorFight fight) {
        super(id, tile, fight);
        this.originalId = id;
        enrage = Collections.max(fight
                                 .getPlayers()
                                 .stream()
                                 .map(Player::getAraxxorEnrage)
                                 .collect(Collectors.toList()));
        totalRampHealth = 100;
        setForceTargetDistance(25);
        setRun(true);
        setIntelligentRouteFinder(true);
    }

    /**
     * Performs the parent {@code processNPC()} functionality, but also checks for phase-related
     * mechanic triggers.
     */
    @Override
    public void processNPC() {
        if (fight.getPhase() == 2 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.ACID
                && !absorbingAcid && isInAcidPool())
            absorbAcid();
        if (fight.getPhase() == 2 && fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.ACID
                && !absorbingAcid && !isInAcidPool() && totalAbsorbedAcid > 0 && !drainingAcid)
            drainAcid();
        super.processNPC();
    }

    @Override
    public int getCapDamage() {
        return 1250;
    }


    @Override
    public int getMaxDistance() {
        return (id == Araxyte.ARAXXOR_NPC_ID_MAGE ||
                id == Araxyte.ARAXXOR_NPC_ID_RANGE) ? 5 : 1;
    }

    /**
     * Performs the parent {@code processHit(Hit hit)} functionality, but also performs some important checks.
     *
     * If araxxor is in the healing web, 50% of the damage of {@code hit} will be reflected to all players
     * in the {@link AraxxorFight}.
     *
     * If there are Mirrorback minions present in the fight, 100% of all damage dealt (this damage is also
     * scaled with enrage) will be reflected back to the attacker.
     *
     * If the fight is not yet in phase two, and the incoming hit would bring Araxxor below 25% health,
     * he will be unharmed.
     *
     * If the fight is in phase two, and the incoming hit is lethal, Araxxor will heal to 50% HP.
     *
     * @param hit the incoming {@code Hit}. There is a 20% chance that hitting Araxxor will increment the players
     *            enrage value by 5.
     */
    @Override
    public void processHit(Hit hit) {
        if(!(hit.getSource() instanceof AraxyteMinion) &&
                minions != null && !minions.isEmpty() &&
                minionFactory.getLastSpawnedMinionType() == AraxyteMinion.MinionType.MIRRORBACK) {
            Entity attacker = hit.getSource();
            Hit scaledHit = scaleBaseHit(attacker, hit.getDamage(), Hit.HitLook.REFLECTED_DAMAGE);
            attacker.applyHit(scaledHit);
        } else {
            if(fight.getPhase() < 2 && getHitpoints() - hit.getDamage() <= 0.25 * getMaxHitpoints()) {
                if(!playersNotified) {
                    playersNotified = true;
                    fight.notifyPlayersMsgBox("The spiders above refuse to let Araxxor die!");
                    CoresManager.getServiceProvider().executeWithDelay(() ->
                            playersNotified = false, 20, TimeUnit.SECONDS);
                }
                return;
            } else if(fight.getPhase() == 2 && getHitpoints() - hit.getDamage() <= 0) {
                fight.notifyPlayersMsgBox("The spiders above refuse to let Araxxor die!");
                Araxxor.this.applyHit(new Hit(null, 5000 - getHitpoints(), Hit.HitLook.HEALED_DAMAGE));
                return;
            } else if(hit.getLook() != Hit.HitLook.HEALED_DAMAGE &&
                    fight.getPhase() == 3 && getHitpoints() - hit.getDamage() <= 0) {
                fight.nextPhase();
                return;
            }
            Entity source = hit.getSource();
            if(source instanceof Player) {
                if(Utils.random(5) == 0) {
                    int currentEnrage = ((Player) source).getAraxxorEnrage();
                    if(currentEnrage < 300) {
                        ((Player) source).addAraxxorEnrage(1);
                        ((Player) source).sendMessage("Araxxor becomes more enraged with you...");
                        if(enrage < 300)
                            enrage++;
                    }
                }
                Logger.log(Logger.DEBUG, "Player hit here.");
                if(((Player) source).getPrayer().usingPrayer(1, 16)) {
                    Araxxor.this.applyHit(new Hit(null, (int) (hit.getDamage() * 0.25), Hit.HitLook.HEALED_DAMAGE));
                    Logger.log(Logger.DEBUG, "Should have healed.");
                }
            }
            super.processHit(hit);
        }
    }

    /**
     * Mutator to set the leftover HP after the end of phase one.
     * @param leftover the leftover HP when phase one ended
     */
    public void setLeftoverPhaseOne(int leftover) {
        this.leftoverPhaseOne = leftover;
    }

    /**
     * Mutator to set the leftover HP after the end of phase two.
     * @param leftover the leftover HP when phase two ended
     */
    public void setLeftoverPhaseTwo(int leftover) {
        this.leftoverPhaseTwo = leftover;
    }

    /**
     * Gets the total leftover HP for the fight progression. If light path was
     * chosen, only the leftover HP from phase one will factor in.
     * @return the leftover HP for the fight progression
     */
    public int getLeftoverHP() {
        return leftoverPhaseTwo + (fight.getConfig().getActivePath()
                != AraxxorEnvironment.Paths.LIGHT ? leftoverPhaseOne : 0);
    }

    /**
     * Mutator for the current swing direction.
     * @param swingDirection the swing direction to set
     */
    public void setSwingDirection(SwingDirection swingDirection) {
        this.swingDirection = swingDirection;
    }

    /**
     * Accessor to the current swing direction.
     * @return Araxxor's current swing direction
     */
    public SwingDirection getSwingDirection() {
        return swingDirection;
    }

    /**
     * Accessor to the original id Araxxor was spawned with.
     * This is necessary for the combat script to remain consistent in
     * the case where Araxxor is transformed to the acidic form.
     * @return the original npc ID this <tt>Araxxor</tt> was spawned in
     */
    int getOriginalId() {
        return originalId;
    }

    /**
     * Increments the global attack tick by 1 and returns its value.
     * Araxxor performs a special attack every 5 global attack ticks.
     * @return the new global attack tick
     */
    int getAndIncrementGlobalTick() {
        int tick = globalAttackTick++;
        if(globalAttackTick == 6) {
            globalAttackTick = 0;
        }
        return tick;
    }

    @Override
    void performMinionSpawn() {
        super.performMinionSpawn();
        minionSpawns++;
        int ticksToNext = Utils.random(2, 5);
        int carryOver = (globalAttackTick + ticksToNext) % 5;
        nextMinionSpawn = carryOver > 0 ? carryOver :
                (globalAttackTick + ticksToNext);
    }

    int getMinionSpawns() {
        return minionSpawns;
    }

    int getNextMinionSpawn() {
        return nextMinionSpawn;
    }

    void skipMinionTick() {
        nextMinionSpawn += (nextMinionSpawn + 1 <= 5) ? 1 : (-nextMinionSpawn);
    }

    /**
     * Picks and performs a random Araxxor special attack. The same special attack will never be performed twice in
     * a row, nor repeated until Araxxor has exhausted all special attack options exactly once.
     * @return the attack delay in ticks for the selected special attack
     */
    @Override
    int performSpecial() {
        if(!sentEggBomb && fight.getPhase() >= 2)
            return performEggBomb();
        if(unusedSpecials.isEmpty()) {
            unusedSpecials.addAll(Arrays.asList(Specials.values()));
            filterSpecials();
            if(fight.getConfig().getActivePath() != AraxxorEnvironment.Paths.MINIONS)
                sentEggBomb = false;
        }
        Specials randomSpecial = unusedSpecials.get(Utils.random(unusedSpecials.size()));
        unusedSpecials.remove(randomSpecial);
        switch (randomSpecial) {
            case CLEAVE:
                performCleave();
                break;
            case WEB_HEAL:
                performWebHeal();
                break;
            case COCOON:
                performCocoon();
                break;
            case ACID_SPIDER:
                performInstakillSpiderSpawn();
                break;
        }
        if(fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.MINIONS)
            sentEggBomb = false;
        return randomSpecial.getAttackDelay();
    }

    /**
     * Removes the appropriate specials from Araxxors unused special list based
     * of the phase of the fight.
     *
     * In phase one, the only specials araxxor can perform are Cleave, Cocoon, and Web Heal.
     *
     * In Phase two, Araxxor will gain a path specific special attack (based on chosen path),
     * but will lose the Cocoon special attack.
     *
     * In Phase three, Araxxor can perform specials from any of the paths on the current rotation,
     * but still loses the Cocoon special attack.
     */
    private void filterSpecials() {
        unusedSpecials.removeIf(special -> special == Specials.SPAWN_MINIONS);
        if(fight.getPhase() == 1) {
            unusedSpecials.removeIf(special -> special == Specials.ACID_SPIDER);
        } else if(fight.getPhase() == 2) {
            unusedSpecials.removeIf(special -> special == Specials.COCOON);
            if(fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.MINIONS) {
                unusedSpecials.removeIf(special -> special == Specials.ACID_SPIDER);
            }
        } else {
            unusedSpecials.removeIf(special ->
                    (special == Specials.ACID_SPIDER &&
                            AraxxorManager.getOffPath() == AraxxorEnvironment.Paths.ACID) ||
                    (special == Specials.COCOON)
            );
        }
        Collections.shuffle(unusedSpecials);
    }

    /**
     * Performs the instakill acid spider spawn attack.
     *
     * Araxxor will shoot a projectile to random location within 4 tiles
     * of the players in the fight, which will spawn a small acid pool.
     * After 5 game ticks, a highly acidic spider will spawn from the
     * pool and begin charging a target (in group mode, two acid
     * pools and two acidic spiders will spawn, one for each group member).
     *
     * If the acidic spider reaches the player within 3 game ticks,
     * the player will automatically die in an acidic explosion.
     * If the acidic spider doesn't reach the player, it will die off.
     */
    private void performInstakillSpiderSpawn() {
        setNextAnimation(new Animation(24094));
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            List<WorldTile> acidPools;
            List<InstakillMinion> acidSpiders;

            private void pickLocations() {
                acidPools = new ArrayList<>();
                while (acidPools.size() < fight.getPlayers().size()) {
                    fight.getPlayers().forEach(player -> {
                        WorldTile tile = new WorldTile(player, 4);
                        if (verifyAllTileHashes(tile, player) &&
                                World.canMoveNPC(tile.getPlane(), tile.getX(), tile.getY(), 1) &&
                                !acidPools.contains(tile))
                            acidPools.add(tile);
                    });
                }
            }

            private boolean verifyAllTileHashes(WorldTile tile, Player player) {
                final int hash = tile.getTileHash();
                return hash != fight.getConfig().getRampReference().getTileHash() &&
                       hash != player.getTileHash();
            }

            private boolean checkShutdown() {
                boolean shutdown = fight.shuttingDown();
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 0) {
                    pickLocations();
                    acidPools.forEach(location -> World.spawnObject(new WorldObject(91671, 10, 0, location)));
                    fight.getPlayers().forEach(p -> p.getPackets().sendPlayerMessageBox("A highly " +
                            "acidic spider is about to spawn nearby!"));
                } else if(tick == 5) {
                    acidSpiders = new ArrayList<>();
                    acidPools.forEach(location -> acidSpiders.add(new InstakillMinion(19471, location)));
                    acidSpiders.forEach(spider -> {
                        spider.setKillTarget(fight.getPlayers().get(acidSpiders.indexOf(spider)));
                        spider.findIntelligentRoute();
                    });
                } else if(tick > 5 && tick < 8) {
                    List<InstakillMinion> detonated = new ArrayList<>();
                    for(InstakillMinion spider : acidSpiders) {
                        if(spider == null || spider.isDead() || spider.hasFinished() || spider.getKillTarget() == null ||
                                spider.getKillTarget().isDead() || spider.getKillTarget().hasFinished() ||
                                !fight.getPlayers().contains(spider.getKillTarget()))
                            continue;
                        if(spider.getTileHash() == spider.getKillTarget().getTileHash()) {
                            spider.detonate();
                            detonated.add(spider);
                            continue;
                        }
                        spider.resetWalkSteps();
                        spider.findIntelligentRoute();
                    }
                    acidSpiders.removeAll(detonated);
                } else if(tick >= 8) {
                    acidSpiders.forEach(spider -> {
                        spider.resetWalkSteps();
                        spider.setNextGraphics(new Graphics(5007));
                        spider.sendDeath(spider.getKillTarget());
                    });
                    acidPools.forEach(location -> World.removeObject(World.getObject(new
                            WorldTile(location.getX(), location.getY(), location.getPlane()))));
                    stop();
                }
                tick++;
            }
        }, 0, 1);
    }

    /**
     * Performs the egg bomb special attack.
     *
     * Araxxor will launch a fire bomb at his current target, and will
     * also simultaneously spawn 4 spider eggs at a random location within
     * the vicinity of the player. Standing on the eggs will cause the players
     * to take less damage from the bomb, but will generate high enrage.
     *
     * The bomb damage is split among both players if in Group difficulty.
     * Even if not the main target, a player must be on the eggs to take reduced
     * damage.
     * @return 5 - a 5 tick attack delay
     */
    private int performEggBomb() {
        sentEggBomb = true;
        final List<WorldTile> eggDrops = Arrays.asList(
                fight.getConfig().generateEggGrid(eggBombTarget =
                        fight.getPlayers().get(Utils.random(fight.getPlayers().size())), 5));
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;

            final List<WorldObject> eggs = new ArrayList<>(eggDrops.size());

            NewProjectile projectile = new NewProjectile(Araxxor.this,
                    Araxxor.this.eggBombTarget, 5073, 40, 40, 10, NewProjectile.DEFAULT_DELAY);
            int _duration = Utils.getDistance(Araxxor.this.eggBombTarget.getX(),
                    Araxxor.this.eggBombTarget.getY(), Araxxor.this.getX(), Araxxor.this.getY()) /
                    (projectile.getSpeed() / 10) / 2;
            int duration = _duration < 2 ? 2 : _duration;

            private boolean targetOnEggs(Player target) {
                return eggDrops.stream().anyMatch(tile ->
                        target.getTileHash() == tile.getTileHash());
            }

            private boolean checkShutdown() {
                boolean shutdown = fight.shuttingDown();
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 0) {
                    setNextAnimation(new Animation(24085));
                    World.sendProjectile(projectile);
                    eggDrops.forEach(tile -> World.sendGraphics(null, new Graphics(5009), tile));
                } else if(tick == 1) {
                    eggDrops.forEach(tile -> {
                        final WorldObject o  = new WorldObject(91636, 10, 0, tile);
                        eggs.add(o);
                        World.spawnObject(o);
                    });
                    fight.getConfig().unclipTiles(eggDrops);
                } else if(tick == duration) {
                    boolean blowUp = false;
                    for(Player target : fight.getPlayers()) {
                        if(target != null && !target.isDead() && !target.hasFinished()) {
                            if(targetOnEggs(target)) {
                                Hit scaledHit = scaleBaseHit(target, target == Araxxor.this.getTarget() ?
                                        100 : 50, Hit.HitLook.CRITICAL_DAMAGE);
                                target.applyHit(scaledHit);
                                target.addAraxxorEnrage(20);
                                target.sendMessage("Araxxor becomes more enraged with you...");
                                blowUp = true;
                            } else {
                                Hit scaledHit = scaleBaseHit(target, target == Araxxor.this.getTarget() ?
                                        300 : 200, Hit.HitLook.CRITICAL_DAMAGE);
                                target.applyHit(scaledHit);
                            }
                        }
                    }
                    for(WorldObject egg : eggs) {
                        World.removeObject(egg);
                        World.sendGraphics(null, new Graphics(blowUp ? 5010 : 5008), egg);
                    }
                    stop();
                }
                tick++;
            }

        }, 0, 1);

        return 5;
    }

    /**
     * Causes Araxxor to begin draining the acid he absorbed
     * in the acid pool. This method is invoked when Araxxor is no longer
     * in the acid pool, but only after he has at least begun absorbing acid
     * and/or still has acid to drain.
     */
    private void drainAcid() {
        drainingAcid = true;
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            final WorldTile rampReference = Araxxor.this.getFight().getConfig().getRampReference();

            private boolean checkShutdown() {
                boolean shutdown = fight.shuttingDown();
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(totalAbsorbedAcid == 0) {
                    Araxxor.this.transformIntoNPC(originalId);
                    drainingAcid = false;
                    stop();
                }
                if(isInAcidPool()) {
                    drainingAcid = false;
                    stop();
                }
                if(tick % 3 == 0) {
                    if(Araxxor.this.isOnRamp()) {
                        totalRampHealth -= (totalRampHealth - 4 < 0) ? totalRampHealth : 4;
                        totalAbsorbedAcid -= (totalAbsorbedAcid - 4 < 0) ? (totalAbsorbedAcid) : 4;
                        AraxxorInterfaces.updateAcidInterface(fight.getPlayers(), totalRampHealth, totalAbsorbedAcid);
                        if(World.getObject(rampReference).getId() != 91525) {
                            World.spawnObject(new WorldObject(91525, 10, 0,
                                    rampReference.getX(), rampReference.getY(), 1));
                        }
                        if(totalRampHealth == 0) {
                            World.spawnObject(new WorldObject(91526, 10, 0,
                                    fight.getConfig().relativeX(4536),
                                    fight.getConfig().relativeY(6260), 1));
                            World.spawnObject(new WorldObject(91670, 10, 0,
                                    fight.getConfig().relativeX(4554),
                                    fight.getConfig().relativeY(6265), 1));
                            Araxxor.this.transformIntoNPC(originalId);
                            Araxxor.this.leapOffAcidRamp();
                            fight.notifyPlayersMsgBox("As the platform degrades away, " +
                                    "Araxxor flees to the next area, " +
                                    "calling on the spiders above to keep him alive!");
                            fight.nextPhase();
                            drainingAcid = false;
                            stop();
                        }
                    } else {
                        totalAbsorbedAcid -= (totalAbsorbedAcid - 4 < 0) ? (totalAbsorbedAcid) : 4;
                        AraxxorInterfaces.updateAcidInterface(fight.getPlayers(), totalRampHealth, totalAbsorbedAcid);
                    }
                }
                tick++;
            }

        }, 0, 1);
    }

    /**
     * Causes Araxxor to begin absorbing acid from the acid pool.
     */
    private void absorbAcid() {
        absorbingAcid = true;
        transformIntoNPC(Araxyte.ARAXXOR_NPC_ID_ACIDIC);
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
                if(!isInAcidPool()) {
                    absorbingAcid = false;
                    stop();
                } else {
                    if(tick == 0 && totalAbsorbedAcid == 0)
                        AraxxorInterfaces.sendAcidInterface(fight.getPlayers());
                    if(tick % 3 == 0 && totalAbsorbedAcid <= 100) {
                        totalAbsorbedAcid += (totalAbsorbedAcid + 5 >= 100) ? (100 - totalAbsorbedAcid) : 5;
                        AraxxorInterfaces.updateAcidInterface(fight.getPlayers(), totalRampHealth, totalAbsorbedAcid);
                    }
                    if(totalAbsorbedAcid >= 100) {
                        fight.getPlayers().forEach(player -> player.getPackets().sendPlayerMessageBox("Araxxor " +
                                "has absorbed all the acid! Lure him to the ramp!"));
                        CoresManager.getServiceProvider().executeWithDelay(() -> absorbingAcid = false,
                                5, TimeUnit.SECONDS);
                        stop();
                    }
                }
                tick++;
            }

        }, 0, 1);
    }

    /**
     * Performs a ray-crossing algorithm on the polygon
     * formed by the acid pool verticies to determine
     * if Araxxor is in the acid pool region.
     * @return whether or not Araxxor is in the acid pool
     */
    private boolean isInAcidPool() {
        List<WorldTile> acidPool = fight.getConfig().getAcidPool();
        boolean inAcidPool = false;
        for(int i = 0, j = acidPool.size() - 1; i < acidPool.size(); j = i++) {
            if ((acidPool.get(i).getY() > getY()) != (acidPool.get(j).getY() > getY()) &&
                    (getX() < (acidPool.get(j).getX() - acidPool.get(i).getX())
                            * (getY() - acidPool.get(i).getY())
                            / (acidPool.get(j).getY()-acidPool.get(i).getY())
                            + acidPool.get(i).getX()))
                inAcidPool = !inAcidPool;
        }
        return inAcidPool;
    }

    private boolean isOnRamp() {
        return getX() > fight.getConfig().relativeX(4535) &&
                (getY() >= fight.getConfig().relativeY(6258) &&
                        getY() <= fight.getConfig().relativeY(6270));
    }

    private void leapOffAcidRamp() {
        setNextGraphics(new Graphics(-1));
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
                if(tick == 0) {
                    Araxxor.this.setNextAnimation(new Animation(24056));
                    Araxxor.this.setCantInteract(true);
                    Araxxor.this.setTarget(null);
                } else if(tick == 3) {
                    Araxxor.this.setNextWorldTile(fight.getConfig().getRelativeTile(new WorldTile(4564, 6259, 1)));
                    Araxxor.this.setNextAnimation(new Animation(24076));
                } else if(tick == 6) {
                    Araxxor.this.setCantInteract(false);
                    stop();
                }
                tick++;
            }

        }, 0, 1);
    }

    /**
     * Enum containing swing direction information, namely
     * the NPC and Player animation ids, and the associated
     * key press required to dodge the swing.
     */
    public enum SwingDirection {

        LEFT(24099, 96, 24100, "Araxxor swings his legs to the left!..."),
        DOWN(24101, 99, 24102, "Araxxxor plans to throw you into the air!..."),
        RIGHT(24111, 97, 24112, "Araxxor swings his legs to the right!..."),
        UP(24113, 98, 24114, "Araxxor plans to smash you into the ground!...");

        private int animationId;
        private int correctKeyStroke;
        private int playerAnimationId;
        private String warningMessage;

        private static final Map<Integer, Integer> keyAnimPlayerMap = new HashMap<>();

        static
        {
            Arrays.stream(SwingDirection.values()).forEach( e ->
                    keyAnimPlayerMap.put(e.correctKeyStroke, e.playerAnimationId));
        }

        SwingDirection(int animationId, int correctKeyStroke,
                       int playerAnimationId, String warningMessage) {
            this.animationId = animationId;
            this.correctKeyStroke = correctKeyStroke;
            this.playerAnimationId = playerAnimationId;
            this.warningMessage = warningMessage;
        }

        public static Animation animForKeyStroke(int key) {
            Integer id = keyAnimPlayerMap.get(key);
            return new Animation(keyAnimPlayerMap.get(key) == null ? -1 : id);
        }

        public int getCorrectKeyStroke() {
            return correctKeyStroke;
        }

        public int getAnimationId() {
            return animationId;
        }

        public String getWarningMessage() {
            return warningMessage;
        }

    }

}
