package com.rs.game.player.content.araxxor.npcs;

import com.rs.cache.loaders.AnimationDefinitions;
import com.rs.game.*;

import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.AraxxorEnvironment;
import com.rs.game.player.content.araxxor.AraxxorFight;
import com.rs.game.player.content.araxxor.AraxxorInterfaces;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * The {@code Araxxi} class defines NPC spawning mechanics and is directly linked to an {@link AraxxorFight} object.
 * The Araxxi combat script will refer directly to the {@code Araxxi} object to perform special attacks. All special
 * attacks are self-contained in private methods in the class, or in package-access methods in the parent {@link Araxyte}
 * class.
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)<br/>
          1.1 (beta, 08/13/2017)
 */
public final class Araxxi extends Araxyte {

    private static final long serialVersionUID = -3154426975447002784L;

    private boolean capped;
    private boolean sludge;
    private int globalAttackTick;

    private boolean attackTypeFlag;

    private List<Specials> specials;

    /**
     * Constructs an {@code Araxxi} NPC.
     * @param id the NPC id
     * @param tile the spawn location
     * @param fight the fight instance this Araxxi belongs to
     */
    public Araxxi(int id, WorldTile tile, AraxxorFight fight) {
        super(id, tile, fight);
        setForceTargetDistance(15);
        setRun(true);
        this.specials = generateSpecials(this.fight);
        unusedSpecials = new ArrayList<>(specials);
        AraxxorInterfaces.updateBossHealthInterface(fight.getPlayers(), this);
    }

    /**
     * Generate the special attacks Araxxi should use
     * based on the fight configuration.
     * @param fight the fight this Araxxi belongs to
     * @return a list of special attacks to use
     */
    private List<Specials> generateSpecials(AraxxorFight fight) {
        List<Araxyte.Specials> specials = new ArrayList<>();
        AraxxorEnvironment config = fight.getConfig();
        List<AraxxorEnvironment.Paths> paths = Arrays.asList(config.getCurrentPaths());
        Arrays.stream(Araxxi.Specials.values()).forEach(s -> {
            if(s == Specials.SPAWN_MINIONS) {
                if(paths.contains(AraxxorEnvironment.Paths.MINIONS))
                    specials.add(s);
            } else if(s == Specials.CLEAVE) specials.add(s);
        });
        return specials;
    }

    /**
     * Picks and performs a random Araxxi special attack. The same special attack will never be performed twice in
     * a row, nor repeated until Araxxi has exhausted all special attack options exactly once.
     * @return the attack delay in ticks for the selected special attack
     */
    @Override
    int performSpecial() {
        Specials randomSpecial = unusedSpecials.get(Utils.random(unusedSpecials.size()));
        unusedSpecials.remove(randomSpecial);
        if(unusedSpecials.isEmpty())
            unusedSpecials.addAll(specials);
        switch(randomSpecial) {
            case CLEAVE:
                performCleave();
                break;
            case SPAWN_MINIONS:
                performMinionSpawn();
                break;
            case COCOON:
                performCocoon();
                break;
        }
        return randomSpecial.getAttackDelay();
    }

    /**
     * Increments the global attack tick by 1 and returns its value.
     * Araxxi performs a special attack every 6 global attack ticks.
     * @return the new global attack tick
     */
    int getAndIncrementGlobalTick() {
        int tick = globalAttackTick++;
        if(tick % 2 == 0)
            attackTypeFlag = !attackTypeFlag;
        if(globalAttackTick == 7)
            globalAttackTick = 0;
        return tick;
    }

    /**
     * Returns <tt>true</tt> if Araxxi should mage on the next auto attack.
     * @return <tt>true</tt> if Araxxi's next auto attack is a mage attack,
     *         false otherwise
     */
    boolean shouldMage() {
        return attackTypeFlag;
    }

    /**
     * Calculates the scaled attack delay due to the
     * enrage effect.
     * @return Araxxi's current attack delay scaled
     *         to enrage
     */
    int getAttackDelay() {
        return 5 - (enrage >= 75 ? 2 : 0);
    }

    /**
     * Perform the parent {@code processNPC()}, but also
     * check to see if it is time to cap damage at 500,
     * or to send (or resend) the sludge attack.
     */
    @Override
    public void processNPC() {
        if(isDead() || isCantInteract())
            return;
        if(!capped && getHitpoints() < getMaxHitpoints() * 0.5)
            capped = true;
        if(!sludge && getHitpoints() < getMaxHitpoints() * 0.25) {
            sludge = true;
            fight.getPlayers().forEach(p -> {
                if(p != null && !p.isDead() && !p.hasFinished())
                    performSludge(p);
            });
        }
        super.processNPC();
    }

    /**
     * Perform the parent {@link Araxyte#processHit(Hit)}, but
     * also adjust the enrage level and update the player interfaces.
     * @param hit the {@code Hit} to process
     */
    @Override
    public void processHit(Hit hit) {
        if(Utils.random(3) == 0 && enrage < 300)
            enrage += 5;
        super.processHit(hit);
    }

    /**
     * Get the current damage cap for this Araxxi.
     * @return the current damage cap
     */
    @Override
    public int getCapDamage() {
        return capped ? 500 : 1250;
    }

    /**
     * Perform the parent {@code sendDeath(Entity source)}, but
     * send custom death animations, and notify the fight instance
     * that it is time to finish and send the rewards.
     * @param source the source of this Araxxi's death
     */
    @Override
    public void sendDeath(final Entity source) {
        resetWalkSteps();
        setTarget(null);
        if (source instanceof Player)
            source.deathResetCombat();
        setNextAnimation(new Animation(-1));
        WorldTasksManager.schedule(new WorldTask() {
            int loop;
            final byte[] dir = Utils.getDirection(Araxxi.this.getDirection());
            final int rot = Utils.getAngle(dir[0], dir[1]);
            @SuppressWarnings("ConstantConditions")
            @Override
            public void run() {
                if (loop == 0)
                    setNextAnimation(new Animation(24106));
                else if(loop == AnimationDefinitions.getAnimationDefinitions(24106).getEmoteTime() / 600) {
                    World.spawnObject(new WorldObject(91673, 10, rot,
                            new WorldTile(Araxxi.this)));
                    Araxxi.this.reset();
                    Araxxi.this.finish();
                    Araxxi.this.fight.finish();
                    stop();
                }
                loop++;
            }
        }, 0, 1);
    }

    /**
     * Performs the sludge attack.<br/><br/>
     * This isn't a direct attack from Araxxi, but rather
     * a separate mechanic that begins along-side Araxxi's combat.
     * A bouncing black sludge ball takes a random number of bounces
     * before honing in on its target. In group difficulty, there will
     * be a separate sludge attack per player.
     * @param player the target for this method invocation
     */
    private void performSludge(Player player) {
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            int bounces = 0;
            int sent = 0;
            int finalStart = 0;
            int finalDelay = 0;
            int lastLaunch = 0;
            NewProjectile finalProjectile;
            final Player target = player;
            WorldTile[] bounceLocations;
            NewProjectile[] projectiles;
            int[] delays;

            private void generateBouncePath() {
                int gSize = fight.getPlayers().size();
                bounces = Utils.random(3, 5);
                bounceLocations = new WorldTile[bounces];
                delays = new int[bounces];
                projectiles = new NewProjectile[bounces];
                for(int i = 0; i < bounces; i++) {
                    WorldTile tile = null;
                    while(tile == null) {
                        tile = new WorldTile(fight.getPlayers().get(Utils.random(gSize)), 4);
                        if(!World.canMoveNPC(tile.getPlane(),
                                tile.getX(), tile.getY(), 1))
                            tile = null;
                    }
                    bounceLocations[i] = tile;
                    projectiles[i] = new NewProjectile(
                            i == 0 ? Araxxi.this : bounceLocations[i - 1],
                            bounceLocations[i],
                            5012, 25,
                            25, 0, 35,
                            15, 0);
                    delays[i] = projectiles[i].getTime() / 335;
                }
            }

            @Override
            public void run() {
                if(fight.shuttingDown()) {
                    stop();
                    return;
                }
                if(Araxxi.this.isDead() || Araxxi.this.hasFinished()) {
                    stop();
                    return;
                }
                if(tick == 0) {
                    generateBouncePath();
                    World.sendProjectile(projectiles[sent]);
                }
                if(sent < bounces && tick == (lastLaunch + delays[sent])) {
                    lastLaunch = tick;
                    if(++sent < bounces)
                        World.sendProjectile(projectiles[sent]);
                }
                if(sent == bounces && finalProjectile == null) {
                    finalProjectile = new NewProjectile(
                            bounceLocations[bounces - 1],
                            new WorldTile(target),
                            5012, 25,
                            25, 0,
                            35, 15, 0
                    );
                    finalStart = tick;
                    finalDelay = finalProjectile.getTime() / 335;
                    World.sendProjectile(finalProjectile);
                    fight.getPlayers().forEach(p -> p.setNextGraphics(new Graphics(5014)));
                }
                if(finalProjectile != null && tick == finalStart + finalDelay) {
                    if(target.withinDistance(finalProjectile.getTo(), 2)) {
                        fight.getPlayers().forEach(p -> {
                            p.applyHit(new Hit(Araxxi.this,
                                    p == target ? 250 : 150, Hit.HitLook.POISON_DAMAGE));
                            p.setNextGraphics(new Graphics(5017));
                        });
                    }
                    sludge = false;
                    stop();
                }
                tick++;
            }
        }, 0, 0);
    }
}
