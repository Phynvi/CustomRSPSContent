package com.rs.game.player.content.araxxor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.rs.Settings;
import com.rs.cache.loaders.ItemDefinitions;
import com.rs.cores.CoresManager;
import com.rs.game.*;
import com.rs.game.item.Item;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.npcs.Araxxi;
import com.rs.game.player.content.araxxor.npcs.Araxxor;
import com.rs.game.player.content.araxxor.npcs.Araxyte;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.utils.Logger;
import com.rs.utils.NPCCombatDefinitionsL;
import com.rs.utils.Utils;

/**
 * The {@code AraxxorFight} is a data structure which represents
 * the state of an Araxxor instance for a group of players. It is responsible
 * for properly scheduling phases of the fight, and transitioning between
 * phases. It also controls mechanics which are independent of the boss NPCs,
 * such as the light path mechanic, and the suffocation effect after
 * defating phase two of acid path.
 *
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)<br/>
 *        1.1 (beta, 08/19/2017)
 */
public final class AraxxorFight {

    private boolean shutdown;

    private byte phase;
    private boolean webBurning;
    private boolean suffocate = true;
    private int lpWallHealth;

    private final AraxxorEnvironment config;

    private final List<Player> players;
    private Map<Player, AraxxorReward> rewardMap;

    private Araxyte boss;

    /**
     * Constructs a new Araxxor fight instance. The environment
     * is generated internally in this constructor call.
     * @param players the players in this fight
     */
    public AraxxorFight(List<Player> players) {
        this.players = players;
        this.config = new AraxxorEnvironment(this, AraxxorManager.getCurrentPaths());
        this.phase = 0;
        lpWallHealth = 100;
        this.players.forEach(player -> {
            player.getControlerManager().startControler("AraxxorController", this);
            player.setForceMultiArea(true);
        });
        spawnAraxxor();
    }

    void notifyPlayers(String notify) {
        players.forEach(p -> p.sendMessage(notify));
    }

    public void notifyPlayersMsgBox(String notify) {
        players.forEach(p -> p.getPackets().sendPlayerMessageBox(notify));
    }

    /**
     * Accessor to the players in this fight.
     * @return a list containing the players in the fight
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Accessor to the current stage of the fight.
     * @return the stage of the fight
     */
    public byte getPhase() {
        return phase;
    }

    /**
     * Accessor to the {@link Araxyte} boss NPC currently
     * spawned in the fight.
     * @return the current boss, either Araxxor or Araxxi
     */
    public Araxyte getBoss() {
        return boss;
    }

    /**
     * A direct handle on the environment configuration.
     * @return the {@link AraxxorEnvironment} governing this fight
     */
    public AraxxorEnvironment getConfig() {
        return config;
    }

    /**
     * Tells whether or not the players have selected a path.
     * @return a {@code boolean} indicating if one of the webs
     *          has begun burning
     */
    boolean webIsBurning() {
        return webBurning;
    }

    /**
     * Registers a web as burning with the fight.
     */
    void setWebBurning() {
        this.webBurning = true;
    }

    /**
     * Negates the suffocation flag. When this method is
     * called, the {@code WorldTask} responsible for
     * suffocating the players will invoke {@link WorldTask#stop()}.
     */
    void stopSuffocate() {
        this.suffocate = false;
    }

    /**
     * Returns the shutdown state.
     * @return {@code true} if the fight is shutting down,
     *          {@code false} otherwise
     */
    public boolean shuttingDown() {
        return shutdown;
    }

    /**
     * Retruns one of the players' rewards from this
     * fight instance.
     * @param player the player whose reward to pull
     *               from the rewards map.
     * @return the {@link AraxxorReward} mapped to the
     *         supplied player
     */
    AraxxorReward getReward(Player player) {
        return rewardMap.get(player);
    }

    /**
     * Increments the {@code stage} variable, and transitions to
     * the appropriate next stage of the fight.<br/><br/>
     * All necessary environment, player, or boss changes are handled
     * by this method.
     */
    public void nextPhase() {

        if (phase == 1)
            ((Araxxor) boss).setLeftoverPhaseOne(boss.getHitpoints());
        else if (phase == 2 && config.getActivePath() != AraxxorEnvironment.Paths.LIGHT)
            ((Araxxor) boss).setLeftoverPhaseTwo(boss.getHitpoints());

        phase++;

        if(phase == 3) {
            float leftover = ((Araxxor) boss).getLeftoverHP();
            if(leftover > 2000) {
                float healed = leftover * 0.8F;
                boss.applyHit(new Hit(null, Math.round(healed), Hit.HitLook.HEALED_DAMAGE));
                if(config.getActivePath() != AraxxorEnvironment.Paths.ACID)
                    players.forEach(p ->
                            p.getPackets().sendPlayerMessageBox("Araxxor is healed by the spiders above!"));
            }
        }
        if(phase == 2 && config.getActivePath() == AraxxorEnvironment.Paths.LIGHT) {
            players.forEach(AraxxorInterfaces::sendDarkness);
            CoresManager.getServiceProvider().scheduleFixedLengthTask(new LightPathMechanic(this),
                    0, 600, TimeUnit.MILLISECONDS);
        } else if(phase == 3 && config.getActivePath() == AraxxorEnvironment.Paths.ACID) {
            suffocatePlayers();
        } else if(phase == 3 && config.getActivePath() == AraxxorEnvironment.Paths.LIGHT) {
            boss.setNextWorldTile(config.getRelativeTile(new WorldTile(4564, 6262, 1)));
            boss.setNextAnimation(new Animation(24076));
            boss.setNextGraphics(new Graphics(5011));
            CoresManager.getServiceProvider().executeWithDelay(() -> {
                boss.setCantInteract(false);
                Player target = players.get(Utils.random(players.size()));
                boss.setNextFaceWorldTile(target);
                boss.setTarget(target);
            }, 5);
        } else if(phase == 4) {
            startAraxxi();
        }
    }

    /**
     * Starts the final stage of the fight, which is the Araxxi battle.
     */
    private void startAraxxi() {

        players.forEach(p -> {
            p.setNextWorldTile(config.getRelativeTile(new
                    WorldTile(4574, 6263, 1)));
            p.setNextFaceWorldTile(new WorldTile(p.getX() + 1,
                    p.getY(), p.getPlane()));
        });
        boss.setNextWorldTile(config.getRelativeTile(new WorldTile(4570, 6265, 1)));
        boss.setNextFaceWorldTile(config.getRelativeTile(new WorldTile(4571, 6265, 1)));
        WorldTasksManager.schedule(new WorldTask() {

            final byte[] dirBoss = Utils.getDirection(boss.getDirection());
            final byte[] dirPlayer = Utils.getDirection(players.get(
                    Utils.random(players.size())).getDirection());

            int tick = 0;

            private boolean checkShutdown() {
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 1) {
                    final int angleBoss = Utils.getAngle(dirBoss[0], dirBoss[1]);
                    final int anglePlayer = Utils.getAngle(dirPlayer[0], dirPlayer[1]);
                    boss.setNextAnimation(new Animation(24042));
                    boss.setNextForceMovement(new ForceMovement(boss, 0, config.getRelativeTile(
                            new WorldTile(4589, 6265, 1)), 4, angleBoss));
                    players.forEach(p -> {
                        p.getCutscenesManager().play("AraxxiStartCS");
                        p.setNextForceMovement(new ForceMovement(p, 0,
                                config.getRelativeTile(new WorldTile(4588, 6260, 1)), 4, anglePlayer));
                    });
                } else if(tick == 2) {
                    players.forEach(p -> p.setNextAnimation(new Animation(19502)));
                    boss.setNextAnimation(new Animation(-1));
                } else if(tick == 4) {
                    players.forEach(player ->
                            player.setNextWorldTile(config.getRelativeTile(new WorldTile(4588, 6260, 1))));
                    boss.setNextWorldTile(config.getRelativeTile(
                            new WorldTile(4589, 6265, 1)));
                    boss.setCantInteract(true);
                    boss.setTarget(null);
                    CoresManager.getServiceProvider().executeWithDelay(AraxxorFight.this::spawnAraxxi, 2);
                    stop();
                }
                ++tick;
            }

        }, 0, 0);
    }

    /**
     * Elegantly spawns Araxxi on the map.
     */
    private void spawnAraxxi() {
        final int deathId = NPCCombatDefinitionsL.getNPCCombatDefinitions(Araxyte.ARAXXI).getDeathEmote();
        boss.setNextAnimation(new Animation(deathId));
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            final WorldTile initialTile = new WorldTile(boss.getX(), boss.getY(), boss.getPlane());

            private boolean checkShutdown() {
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 0) {
                    World.sendGraphics(null, new Graphics(5000), initialTile);
                } else if(tick == 2) {
                    boss.sendDeath(null);
                    boss.setNextWorldTile(new WorldTile(boss.getX(), boss.getY(), boss.getPlane() + 1));
                    World.sendGraphics(null, new Graphics(5070), initialTile);
                } else if(tick == 8) {
                    boss = new Araxxi(Araxyte.ARAXXI, initialTile,
                            AraxxorFight.this);
                    boss.setTarget(players.get(Utils.random(players.size())));
                    boss.setCantInteract(false);
                    stop();
                }
                ++tick;
            }

        }, 0, 1);
    }

    /**
     * Indicate the fight is over (phase 5),
     * initialize the rewards map, increment player Araxxor
     * enrage level, spawn the exit tile, and pass
     * this fight instance to the {@link AraxxorManager} for
     * post-fight processing.
     */
    public void finish() {
        phase = 5;
        rewardMap = new HashMap<>();
        players.forEach(p -> {
            rewardMap.put(p, new AraxxorReward());
            p.addAraxxorEnrage(p.getAraxxorEnrage() >= 300 ? 0 : 20);
        });
        World.spawnObject(new WorldObject(45803, 10, 0, config.getExitTile()));
        World.sendGraphics(null, new Graphics(4982), config.getExitTile());
        AraxxorManager.process(this);
    }

    /**
     * Safely remove a specified player from this fight.
     * The fight is unmapped from the player in the {@link AraxxorManager},
     * the player is removed from this fight's player list, and
     * all interfaces are closed.<br/><br/>
     * If the player list is empty after this removal, the fight
     * will enter shutdown mode.
     * @param player the player to remove from this fight
     * @see AraxxorFight#shutdown()
     */
    void safeRemovePlayer(Player player) {
        AraxxorManager.unmapFight(player);
        player.setForceMultiArea(false);
        players.remove(player);
        AraxxorInterfaces.closeAll(player);
        player.getControlerManager().getControler().removeControler();
        player.setNextWorldTile(Settings.START_PLAYER_LOCATION);
        if(players.isEmpty())
            shutdown();
    }

    /**
     * Enter shutdown state. Set the {@code shutdown} flag to {@code true},
     * causing all {@code WorldTask} objects associated with the fight
     * to invoke {@link WorldTask#stop()}, finishing the boss NPC if required,
     * and unmapping the environment chunks.
     */
    private void shutdown() {
        shutdown = true;
        config.destroyEnvironment();
        if(boss != null && !boss.isDead() && !boss.hasFinished())
            boss.sendDeath(null);
    }

    /**
     * Begins the Araxxor charging cutscene.
     */
    void callChargeCutscene() {

        players.forEach(p -> {
            p.resetWalkSteps();
            p.lock();
            p.setNextWorldTile(config.getRelativeTile(new WorldTile(4548, 6249, 1)));
        });

        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            boolean correctKey = false;

            private boolean checkShutdown() {
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 1) {
                    AraxxorFight.this.boss.setCantInteract(true);
                    AraxxorFight.this.boss.setNextWorldTile(config.getRelativeTile(new WorldTile(4535, 6249, 1)));
                    AraxxorFight.this.boss.setNextFaceWorldTile(
                            AraxxorFight.this.config.getCutsceneAnchorOne());
                    AraxxorFight.this.players.forEach(p -> {
                        p.getCutscenesManager().play("LightPathCutscene");
                        p.setNextFaceWorldTile(AraxxorFight.this.boss);
                        AraxxorInterfaces.updateLighting(p, AraxxorInterfaces.RED);
                    });
                    Araxxor.SwingDirection swingDirection = Araxxor.SwingDirection.values()[
                            Utils.random(Araxxor.SwingDirection.values().length)];
                    ((Araxxor) AraxxorFight.this.boss).setSwingDirection(swingDirection);
                    AraxxorFight.this.boss.setNextAnimation(new Animation(swingDirection.getAnimationId()));
                    AraxxorFight.this.notifyPlayersMsgBox(swingDirection.getWarningMessage());
                    players.forEach(p -> AraxxorManager.controllerOf(p).allowKeyStroke());

                } else if(tick == 4) {
                    players.forEach(p -> {
                        if(!(correctKey = AraxxorManager.controllerOf(p).checkCorrectKey())) {
                            CoresManager.getServiceProvider().executeWithDelay(() -> {
                                p.applyHit(new Hit(AraxxorFight.this.boss,
                                        200, Hit.HitLook.CRITICAL_DAMAGE));
                                p.setNextAnimation(new Animation(10070));
                                p.getDialogueManager().startDialogue("SimpleMessage",
                                        "You fail to judge Araxxor's attack as he slams through you!");
                                p.sendMessage("You fail to judge Araxxor's attack as he slams through you!");
                            }, 600, TimeUnit.MILLISECONDS);
                        } else {
                            p.setNextAnimation(AraxxorManager.controllerOf(p).getAnimationForKeyPress());
                            p.getDialogueManager().startDialogue("SimpleMessage", "You dodge Araxxor!");
                            p.sendMessage("You dodge Araxxor!");
                        }
                    });
                } else if(tick == 7) {
                    AraxxorFight.this.players.forEach(Player::unlock);
                    AraxxorFight.this.boss.setNextWorldTile(config.getRelativeTile(new WorldTile(4546, 6249, 1)));
                    AraxxorFight.this.boss.setNextAnimation(new Animation(24103));
                    if(correctKey) {
                        lpWallHealth -= 50;
                    } else lpWallHealth -= 25;
                    players.forEach(AraxxorInterfaces::sendDarkness);
                } else if(tick == 10) {
                    AraxxorFight.this.boss.setNextAnimation(new Animation(24056));
                    if(lpWallHealth <= 0)
                        World.spawnObject(new WorldObject(91515, 10, 0, config.getLpWallReference()));
                } else if(tick == 16) {
                    AraxxorFight.this.boss.setNextWorldTile(new WorldTile(AraxxorFight.this.boss.getX(),
                            AraxxorFight.this.boss.getY(), AraxxorFight.this.boss.getPlane() + 1));
                    if(lpWallHealth > 0) {
                        CoresManager.getServiceProvider().executeWithDelay(AraxxorFight.this::callChargeCutscene,
                                5, TimeUnit.SECONDS);
                        players.forEach(p -> p.getPackets().sendPlayerMessageBox("Araxxor climbs " +
                                "back up, and plans to charge again!"));
                    }
                    else {
                        World.spawnObject(new WorldObject(91519, 10, 0, config.getLpWallReference()));
                        config.unclipTiles(config.getLpWallReference());
                        AraxxorFight.this.players.forEach(p -> p.getInterfaceManager().closeSecondaryOverlay());
                        CoresManager.getServiceProvider().executeWithDelay(AraxxorFight.this::nextPhase,
                                1200, TimeUnit.MILLISECONDS);
                    }
                    stop();
                }

                tick++;
            }

        }, 0, 0);
    }

    /**
     * Suffocate the players in the fight, causing poison damage
     * every two game ticks. The damage dealt increases steadily
     * until the players navigate past the acid path ramp.
     */
    private void suffocatePlayers() {
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            int damage = 25;

            private boolean checkShutdown() {
                if (shutdown) stop();
                return shutdown;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 0) {
                    players.forEach(player -> player.sendMessage("The noxious fumes " +
                            "from the leftover acid begin to suffocate you!"));
                }
                if(tick % 3 == 0) {
                    players.forEach(player -> player.getPackets().sendPlayerMessageBox("The noxious fumes " +
                            "from the leftover acid begin to suffocate you! Jump off the ramp!"));
                    damage += 10;
                }
                if(suffocate && tick % 2 == 0) {
                    players.forEach(player -> {
                        if(player != null && !player.isDead() && !player.hasFinished())
                            player.applyHit(boss.scaleBaseHit(player,
                                    damage, Hit.HitLook.POISON_DAMAGE));
                    });
                }
                if(!suffocate)
                    stop();
                tick++;
            }

        }, 0, 1);
    }

    /**
     * Performs the initial Araxxor spawn. Araxxor is randomly spawned
     * in one of his three forms (melee, range, mage), unless the group
     * leader (or just the player if solo mode) is holding an
     * Araxyte pheremone.
     */
    private void spawnAraxxor() {

        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;
            int araxxorId;

            final WorldTile spawn = new WorldTile(4503, 6261, 1);

            private boolean checkShutdown() {
                if (shutdown) stop();
                return shutdown;
            }

            private boolean processPheromone() {
                final Player pheromoneHolder =
                        players.stream()
                               .filter(p -> p.getInventory().containsItem(33870, 1))
                               .findFirst().orElse(null);
                final boolean pheremonePresent = pheromoneHolder != null;
                if(pheremonePresent) {
                    ItemDefinitions defs =
                            ItemDefinitions.getItemDefinitions(pheromoneHolder.getEquipment().getWeaponId());
                    if(defs.isMagicTypeWeapon())
                        araxxorId = Araxyte.ARAXXOR_NPC_ID_MELEE;
                    else if(defs.isRangeTypeWeapon())
                        araxxorId = Araxyte.ARAXXOR_NPC_ID_MAGE;
                    else if(defs.isMeleeTypeWeapon())
                        araxxorId = Araxyte.ARAXXOR_NPC_ID_RANGE;
                    else {
                        final int rand = Utils.random(3);
                        araxxorId = rand == 0 ?
                                Araxyte.ARAXXOR_NPC_ID_MELEE : rand == 1 ?
                                Araxyte.ARAXXOR_NPC_ID_MAGE : Araxyte.ARAXXOR_NPC_ID_RANGE;
                    }
                    players.forEach(p -> p.sendMessage("The Araxytes smell your " +
                            (players.size() > 1 ? "group's " : "") + "pheromone."));
                }
                return pheremonePresent;
            }

            @Override
            public void run() {
                if(checkShutdown())
                    return;
                if(tick == 10) {
                    if(!processPheromone()) {
                        final int rand = Utils.random(3);
                        araxxorId = rand == 0 ?
                                Araxyte.ARAXXOR_NPC_ID_MELEE : rand == 1 ?
                                Araxyte.ARAXXOR_NPC_ID_MAGE : Araxyte.ARAXXOR_NPC_ID_RANGE;
                    }
                    boss = new Araxxor(araxxorId, config.getRelativeTile(spawn), AraxxorFight.this);
                    boss.setCantInteract(true);
                    boss.setNextFaceWorldTile(new WorldTile(boss.getX() -1 ,
                            boss.getY(), boss.getPlane()));
                    boss.setNextAnimation(new Animation(24118));
                } else if(tick == 18) {
                    phase = 1;
                    boss.setCantInteract(false);
                    boss.setForceAgressive(true);
                    stop();
                }

                tick++;
            }
        }, 0, 1);
    }

}
