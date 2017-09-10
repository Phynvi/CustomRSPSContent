package com.rs.game.player.content.araxxor.npcs;

import com.rs.cores.CoresManager;
import com.rs.game.*;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.AraxxorFight;
import com.rs.game.player.content.araxxor.AraxxorInterfaces;
import com.rs.game.player.content.araxxor.AraxxorManager;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Logger;
import com.rs.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for Araxyte NPCs. Araxxor and Araxxi
 * share a lot of the same functionality, namely
 * special attacks.
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public abstract class Araxyte extends NPC {

    private static final long serialVersionUID = -1042174989848705013L;

    public static final int ARAXXOR_NPC_ID_MELEE = 19457;
    public static final int ARAXXOR_NPC_ID_MAGE = 19462;
    public static final int ARAXXOR_NPC_ID_RANGE = 19463;
    public static final int ARAXXI = 19464;

    static final int ARAXXOR_NPC_ID_ACIDIC = 19465;

    private Difficulty difficulty;
    private boolean inHealingWeb;
    private boolean cocoonReleaseFlag;

    int enrage;

    final transient MinionFactory minionFactory;
    List<AraxyteMinion> minions;
    List<Specials> unusedSpecials;

    AraxxorFight fight;

    /**
     * Constructs a new Araxyte NPC, binding the NPC to the
     * fight instance.
     * @param id the NPC id, corresponding to any of the Araxxor forms,
     *           or the Araxxi NPC id
     * @param tile the spawn location
     * @param fight the fight instance this Araxyte belongs to
     */
    public Araxyte(int id, WorldTile tile, AraxxorFight fight) {
        super(id, tile, -1, true, true);
        this.fight = fight;
        this.difficulty = this.fight.getPlayers().size() > 1 ? Difficulty.GROUP : Difficulty.SOLO;
        unusedSpecials = new ArrayList<>();
        minionFactory = new MinionFactory();
        setForceMultiArea(true);
        setNoDistanceCheck(true);
        setCombat(new AraxyteCombatHandler(this));
    }

    abstract int performSpecial();

    /**
     * Takes information from a base {@code Hit} object and scales the output damage according to this NPC's
     * difficulty, as well as the enrage mechanic. The scaled hit damage for a group can be represented as,
     * {@code ds = (gm + ep) * db}, where {@code ds} is the scaled damage, {@code ep} is the enrage percentage
     * modifier, {@code db} is the initial base hit damage, and {@code gm} is the group modifier.
     * @param target the intended target for the hit
     * @param damage the initial base hit damage
     * @param hitLook the {@code HitLook} attribute of the original hit
     * @return a new, appropriately scaled {@code Hit} object
     */
    public Hit scaleBaseHit(Entity target, int damage, Hit.HitLook hitLook) {
        if(target == null || target.isDead() || target.hasFinished())
            return null;
        float trueDamage = (float) damage;
        trueDamage *= difficulty == Difficulty.GROUP ? 1.3F : 1.0F;
        trueDamage *= 1.0F + (enrage / 100.0F);
        return new Hit(this, Math.round(trueDamage), hitLook);
    }

    /**
     * Perform the parent {@code processNPC()}, but adjust
     * the difficulty if the group size changes.
     */
    @Override
    public void processNPC() {
        if (isDead() || isCantInteract())
            return;
        if (!getCombat().process())
            checkAgressivity();
        difficulty = fight.getPlayers().size() > 1 ? Difficulty.GROUP : Difficulty.SOLO;
        super.processNPC();
    }

    @Override
    public double getMeleePrayerMultiplier() {
        return 0.5;
    }

    @Override
    public double getRangePrayerMultiplier() {
        return 0.5;
    }

    @Override
    public double getMagePrayerMultiplier() {
        return 0.5;
    }

    /**
     * Perform the parent {@code processHit(Hit hit)}, but
     * also reflect damage back to attackers if inside the
     * healing web.
     * @param hit the {@code Hit} to process
     */
    @Override
    public void processHit(Hit hit) {
        if(inHealingWeb) {
            final int damage = hit.getDamage();
            fight.getPlayers().forEach(player -> {
                if(player != null && !player.isDead() && !player.hasFinished() &&
                        hit.getLook() != Hit.HitLook.HEALED_DAMAGE)
                    player.applyHit(new Hit(this, Math.round((float) (damage * 0.50)),
                            Hit.HitLook.REFLECTED_DAMAGE));
            });
        }
        super.processHit(hit);
        AraxxorInterfaces.updateBossHealthInterface(fight.getPlayers(), this);
    }

    @Override
    public boolean canWalkNPC(int toX, int toY) {
        return true;
    }

    /**
     * Accessor to the fight instance.
     * @return the fight instance bound to this Araxyte
     */
    protected AraxxorFight getFight() {
        return fight;
    }

    /**
     * Adds a minion to the current minion list.
     * @param minion - the minion to add
     */
    void addMinion(AraxyteMinion minion) {
        if(minions == null)
            minions = new ArrayList<>();
        minions.add(minion);
    }

    /**
     * Clears the minion list after a 15 second delay.
     */
    private void scheduleMinionDespawn() {
        CoresManager.getServiceProvider().executeWithDelay(() -> {
            if(!minions.isEmpty())
                new ArrayList<>(Araxyte.this.minions).forEach(minion -> minion.sendDeath(null));
        }, 15, TimeUnit.SECONDS);
    }

    /**
     * Removes a minion from the current minion list.
     * @param minion - the minion to remove
     */
    void removeMinion(AraxyteMinion minion) {
        minions.remove(minion);
    }

    /**
     * Performs the minion spawn attack.<br/><br/>
     *
     * Araxxor will spawn a batch of spider minions (size 2-6 in
     * solo, size 5-10 in group). The type of minion is completely
     * random, but there are 5 types of minions that can spawn.<br/><br/>
     *
     * Bladed, Spitting, and Imbued minions each simply autoattack
     * the players in the fight with melee, ranged, and magic attacks
     * respectively.<br/><br/>
     *
     * Pulsing minions will primarily focus on significantly healing
     * Araxxor every three game ticks, but will attack with magic and stop
     * healing this Araxyte if they are attacked.<br/><br/>
     *
     * Mirrorback minions do not fight back. When they take damage, 50% of
     * the damage they take is reflected to this Araxyte. However, if Mirrorback
     * minions are spawned, 100% of all damage dealt directly to this Araxyte
     * will be reflected back to the attacking player.<br/><br/>
     */
    void performMinionSpawn() {
        final int batchSize = Utils.random(difficulty == Difficulty.SOLO ? 2 : 5,
                difficulty == Difficulty.SOLO ? 6 : 10);
        AraxyteMinion.MinionType type = AraxyteMinion.MinionType.values()[
                Utils.random(AraxyteMinion.MinionType.values().length)];
        while(type == minionFactory.getLastSpawnedMinionType())
            type = AraxyteMinion.MinionType.values()[Utils.random(AraxyteMinion.MinionType.values().length)];
        for(int i = 0; i < batchSize; i++) {
            WorldTile minionSpawnTile;
            while(true) {
                minionSpawnTile = new WorldTile(this, 4);
                if(minionSpawnTile.getTileHash() != getTileHash() &&
                        World.canMoveNPC(minionSpawnTile.getPlane(), minionSpawnTile.getX(), minionSpawnTile.getY(), 1))
                    break;
            }
            AraxyteMinion minion = minionFactory.spawnMinion(type.npcId, minionSpawnTile, this);
            minion.performRole();
        }
        fight.getPlayers().forEach(p -> p.getPackets().sendPlayerMessageBox("A batch of minions appears!"));
        scheduleMinionDespawn();
    }

    List<AraxyteMinion> getMinions() {
        return minions;
    }

    /**
     * Performs the Cleave special attack.<br/><br/>
     * Players are sweeped into melee range of Araxxor, after which he performs a massive melee blow to all players
     * inside a two square radius. This blow is potentially lethal depending on the difficulty and enrage level.<br/><br/>
     * Player have a brief escape window to dodge the attack.
     */
    void performCleave() {
        byte[] dir = Utils.getDirection(getDirection());
        WorldTile toTile = new WorldTile(this, 1);
        while(!World.canMoveNPC(toTile, 1))
            toTile = new WorldTile(this, 1);
        final WorldTile dragTo = new WorldTile(toTile);
        fight.getPlayers().stream().filter(player -> player != null &&
                !player.isDead() && !player.hasFinished()).forEach(player -> {
            player.resetWalkSteps();
            player.setNextForceMovement(new ForceMovement(dragTo, 1, Utils.getAngle(dir[0], dir[1])));
            player.setNextAnimation(new Animation(14388));
        });
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;

            @Override
            public void run() {
                if(fight.shuttingDown()) {
                    stop();
                    return;
                }
                if(tick == 0) {
                    fight.getPlayers().forEach(p -> p.setNextWorldTile(dragTo));
                    setNextAnimation(new Animation(24050));
                    setNextGraphics(new Graphics(4986));
                } else if(tick == 2) {
                    List<Entity> targetsInRange = new ArrayList<>();
                    fight.getPlayers().forEach(player -> {
                        if(player != null && !player.isDead() && !player.hasFinished() &&
                                player.withinDistance(dragTo, 2))
                            targetsInRange.add(player);
                    });
                    World.getRegion(Araxyte.this.getRegionId()).getNPCsIndexes().forEach(i -> {
                            NPC n = World.getNPCs().get(i);
                            if(n != null && !n.equals(Araxyte.this))
                                targetsInRange.add(n);
                        }
                    );
                    targetsInRange.forEach(target -> {
                        Hit scaledHit = scaleBaseHit(target, 300, Hit.HitLook.MELEE_DAMAGE);
                        if (scaledHit != null) target.applyHit(scaledHit);
                        if (target instanceof AraxyteMinion) {
                            fight.getPlayers().forEach(p -> {
                                if(p.getAraxxorEnrage() < 300) {
                                    p.sendMessage("Araxxor becomes more enraged with you...");
                                    p.addAraxxorEnrage(5);
                                }
                            });
                            if(enrage < 300)
                                enrage += 5;
                        }
                    });
                    stop();
                }
                tick++;
            }

        }, 0, 1);
    }

    /**
     * Performs the Cocoon special attack.<br/><br/>
     * Players are frozen in place, trapped in a tight spider web. They will suffer 200 damage per game tick
     * for as long as they are in the cocoon. The only way to escpae is to rapidly click away from their
     * current location. Even then, they are not guaranteed to escape immediately. The maximum duration is 5 game ticks,
     * but players can escape after 2 game ticks if they are lucky.
     */
    void performCocoon() {
        setNextAnimation(new Animation(24083));
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;

            @Override
            public void run() {
                if(fight.shuttingDown()) {
                    stop();
                    return;
                }
                if(tick == 0)
                    fight.getPlayers().forEach(player -> {
                        if(player != null && !player.isDead() && !player.hasFinished()) {
                            player.setCantWalk(true);
                            player.getAppearence().transformIntoNPC(19472);
                            player.getAppearence().setRenderEmote(3199);
                        }

                    });

                else if(cocoonReleaseFlag && tick > 2) {
                    fight.getPlayers().forEach(player -> {
                        if(player != null && !player.isDead() && !player.hasFinished()) {
                            AraxxorManager.controllerOf(player).resetSpamClicks();
                            player.setCantWalk(false);
                            player.getAppearence().setRenderEmote(-1);
                            player.getAppearence().transformIntoNPC(-1);
                            player.getPackets().sendPlayerMessageBox("You manage to " +
                                    "break free from the thick" +
                                    "webbing!");
                        }
                    });
                    setNextAnimation(new Animation(-1));
                    cocoonReleaseFlag = false;
                    stop();
                }

                else if(tick == 5) {
                    fight.getPlayers().forEach(player -> {
                        AraxxorManager.controllerOf(player).resetSpamClicks();
                        player.setCantWalk(false);
                        player.getAppearence().setRenderEmote(-1);
                        player.getAppearence().transformIntoNPC(-1);
                        player.setNextGraphics(new Graphics(4993));
                        player.getPackets().sendPlayerMessageBox("You manage to " +
                                "break free from the thick" +
                                "webbing!");
                    });
                    cocoonReleaseFlag = false;
                    setNextAnimation(new Animation(-1));
                    stop();
                }

                if(tick != 5) {
                    fight.getPlayers().forEach(player -> {
                        if(player != null && !player.isDead() && !player.hasFinished()) {
                            player.setNextAnimation(new Animation(24115));
                            Hit scaledHit = scaleBaseHit(player, 15, Hit.HitLook.POISON_DAMAGE);
                            player.applyHit(scaledHit);
                        }
                    });
                    tick++;
                }

            }
        }, 0, 1);
    }

    /**
     * Performs the web heal special attack.<br/><br/>
     * Araxxor retreats into a web, covering himself and healing for 1000 (2000 in group difficulty) every
     * 2 game ticks. Furthermore, 50% of the damage inflicted on Araxxor will be reflected back at the attackers
     * while in the web.
     */
    void performWebHeal() {
        setTarget(null);
        setCannotMove(true);
        setNextFaceEntity(null);
        setNextGraphics(new Graphics(4987));
        setNextAnimation(new Animation(24075));
        resetWalkSteps();
        inHealingWeb = true;
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
                if (tick % 2 == 0 && tick != 6 && Utils.random(2) == 1)
                    applyHit(new Hit(null, difficulty == Difficulty.SOLO ?
                            200 : 400, Hit.HitLook.HEALED_DAMAGE));
                else if(tick == 6) {
                    inHealingWeb = false;
                    setCannotMove(false);
                    Araxyte.this.setTarget(fight.getPlayers().get(Utils.random(
                            fight.getPlayers().size())));
                    stop();
                }
                tick++;
            }

        }, 0, 1);
    }

    public void releaseCocoon() {
        cocoonReleaseFlag = true;
    }

    /**
     * Represents the difficulty of this {@code Araxyte} NPC. The NPC will by default deal 30% more damage to groups
     * than it does to single players. This does not include enrage effects.
     */
    private enum Difficulty {
        SOLO, GROUP
    }

    /**
     * Small enum containing the special attack definitions, each enumeration containing the attack delay for that
     * special attack.
     */
    enum Specials {
        COCOON(10),
        CLEAVE(20),
        SPAWN_MINIONS(0),
        ACID_SPIDER(3),
        WEB_HEAL(35);

        private int attackDelay;

        Specials(int attackDelay) {
            this.attackDelay = attackDelay;
        }

        int getAttackDelay() {
            return attackDelay;
        }
    }

    enum BatchType {
        
    }
}
