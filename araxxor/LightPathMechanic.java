package com.rs.game.player.content.araxxor;

import com.rs.cores.CoresManager;
import com.rs.cores.FixedLengthRunnable;
import com.rs.game.*;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.player.Player;
import com.rs.utils.Utils;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
final class LightPathMechanic extends FixedLengthRunnable {

    private int tick;
    private WorldTile lightTile;
    private Map<Player, List<Hit>> hitsTaken;
    private List<WorldTile> eggDrops;
    private List<WorldObject> eggs;
    private int projectileDuration = 0;
    private boolean continueFlag;
    private boolean attackTypeFlag;
    private int duration;
    private int currentDamage;

    private AraxxorEnvironment config;
    private AraxxorFight fight;

    LightPathMechanic(AraxxorFight fight) {
        this.fight = fight;
        this.config = this.fight.getConfig();
        duration = Utils.random(75, 150);
        lightTile = config.generateLightTile();
        hitsTaken = new HashMap<>();
        tick = 0;
        currentDamage = 25;
    }

    @Override
    public boolean repeat() {
        if(fight.shuttingDown())
            return false;
        if(tick == 0) {
            fight.getPlayers().forEach(player -> {
                hitsTaken.put(player, new ArrayList<>());
                player.sendMessage("Araxxor flees to the ceiling!");
            });
            World.spawnObject(new WorldObject(91665, 10, 0, lightTile));
            World.spawnObject(new WorldObject(91664, 10, 0, new WorldTile(lightTile.getX(),
                    lightTile.getY() + 1, lightTile.getPlane())));
            fight.getBoss().setNextAnimation(new Animation(24056));
            fight.getBoss().setTarget(null);
            fight.getBoss().setCantInteract(true);
            fight.getBoss().setForceAgressive(false);
            tick++;
        }
        if(tick == 4) {
            fight.getBoss().setLocation(new WorldTile(fight.getBoss().getX(), fight.getBoss().getY(),
                    fight.getBoss().getPlane() + 1));
            fight.getBoss().setCantInteract(false);
        }
        if(tick == duration - 25) {
            fight.getPlayers().forEach(player ->
                    player.getPackets().sendPlayerMessageBox("Araxxor " +
                            "begins to come down and charge..."));
        }
        if(tick % 5 == 0) {
            fight.getPlayers().forEach(p -> {
                if(p != null && !p.isDead() && !p.hasFinished()) {
                    NewProjectile projectile = new NewProjectile(
                            new WorldTile(p, 4),
                            p, attackTypeFlag ? 4979 : 4997, 25,
                            25, 0, 35,
                            15, 0);
                    World.sendProjectile(projectile);
                    CoresManager.getServiceProvider().executeWithDelay(() ->
                            p.applyHit(new Hit(fight.getBoss(), Utils.random(50, 300),
                                attackTypeFlag ?
                                        Hit.HitLook.MAGIC_DAMAGE :
                                        Hit.HitLook.RANGE_DAMAGE)),
                            projectile.getTime() / 335);
                    attackTypeFlag = !attackTypeFlag;
                }
            });
        }
        if(tick % 2 == 0) {
            fight.getPlayers().forEach(player -> {
                if(player != null && !player.isDead() && !player.hasFinished()) {
                    if(!config.onLightTile(player, lightTile)) {
                        Hit hit = new Hit(fight.getBoss(),
                                currentDamage+=2, Hit.HitLook.DESEASE_DAMAGE);
                        player.applyHit(hit);
                        hitsTaken.get(player).add(hit);
                        AraxxorInterfaces.updateLighting(player, AraxxorInterfaces.DARK);
                    }
                    else {
                        AraxxorInterfaces.updateLighting(player, AraxxorInterfaces.LIGHT);
                        currentDamage = 25;
                        final List<Hit> hits = hitsTaken.get(player);
                        if(hits != null) {
                            player.getPackets().sendGlobalConfig(1435, 150);
                            hits.forEach(hit -> {
                                int baseDamage = hit.getDamage();
                                Double scaled = (double) baseDamage * Utils.random(0.15, 0.5);
                                player.applyHit(new Hit(null, scaled.intValue(), Hit.HitLook.HEALED_DAMAGE));
                            });
                            hitsTaken.get(player).clear();
                        }
                    }
                }
            });
        }
        if(tick % 25 == 0) {
            World.removeObject(World.getObject(lightTile));
            World.removeObject(World.getObject(new WorldTile(lightTile.getX(),
                    lightTile.getY() + 1, lightTile.getPlane())));
            lightTile = config.generateLightTile();
            fight.getPlayers().forEach(player -> {
                if(player.getTileHash() != lightTile.getTileHash())
                    AraxxorInterfaces.updateLighting(player, AraxxorInterfaces.DARK);
            });
            World.spawnObject(new WorldObject(91665, 10, 0, lightTile));
            World.spawnObject(new WorldObject(91664, 10, 0, new WorldTile(lightTile.getX(),
                    lightTile.getY() + 1, lightTile.getPlane())));
        }
        if(tick % 15 == 0 && Utils.random(5) == 0) {
            continueFlag = true;
            Player target = fight.getPlayers().get(Utils.random(fight.getPlayers().size()));
            WorldTile fromTile = new WorldTile(target, 5);
            eggDrops = Arrays.asList(fight.getConfig().generateEggGrid(target, 3));
            NewProjectile projectile = new NewProjectile(fromTile,
                    target, 5073, 40, 40, 10, NewProjectile.DEFAULT_DELAY);
            projectileDuration = Utils.getDistance(target.getX(),
                    target.getY(), fromTile.getX(), fromTile.getY()) /
                    (projectile.getSpeed() / 10) / 2;
            if(projectileDuration < 2)
                projectileDuration+=(2 - projectileDuration);
            World.sendProjectile(projectile);
            eggDrops.forEach(tile -> World.sendGraphics(null, new Graphics(5009), tile));
        } else if (continueFlag && (tick % 15) - 1 == 0) {
            eggs = new ArrayList<>();
            eggDrops.forEach(tile -> {
                WorldObject o = new WorldObject(91636, 10, 0, tile);
                eggs.add(o);
                World.spawnObject(o);
            });
            fight.getConfig().unclipTiles(eggDrops);
        } else if(continueFlag && (projectileDuration == 0 ? 1 : ((tick % 15) - projectileDuration)) == 0) {
            fight.getBoss().setTarget(fight.getPlayers().get(Utils.random(fight.getPlayers().size())));
            boolean blowUp = false;
            for(Player target : fight.getPlayers()) {
                if(target != null && !target.isDead() && !target.hasFinished()) {
                    if(targetOnEggs(target)) {
                        Hit scaledHit = fight.getBoss().scaleBaseHit(target,
                                target == fight.getBoss().getTarget() ?
                                        100 : 50, Hit.HitLook.CRITICAL_DAMAGE);
                        target.applyHit(scaledHit);
                        target.addAraxxorEnrage(20);
                        target.sendMessage("Araxxor becomes more enraged with you...");
                        blowUp = true;
                    } else {
                        Hit scaledHit = fight.getBoss().scaleBaseHit(target,
                                target == fight.getBoss().getTarget() ?
                                        300 : 200, Hit.HitLook.CRITICAL_DAMAGE);
                        target.applyHit(scaledHit);
                    }
                }
            }
            for(WorldObject egg : eggs) {
                World.removeObject(egg);
                World.sendGraphics(null, new Graphics(blowUp ? 5010 : 5008), egg);
            }
            continueFlag = false;
        }
        if(tick == duration) {
            fight.callChargeCutscene();
            World.removeObject(World.getObject(lightTile));
            World.removeObject(World.getObject(new WorldTile(lightTile.getX(),
                    lightTile.getY() + 1, lightTile.getPlane())));
            eggDrops.forEach(tile -> {
                WorldObject obj = World.getObject(tile);
                if(obj != null) {
                    World.removeObject(obj);
                    World.sendGraphics(null, new Graphics(5008), tile);
                }

            });
            return false;
        }
        tick++;
        return true;
    }

    private boolean targetOnEggs(Player target) {
        return eggDrops
                .stream()
                .anyMatch(tile -> tile.getTileHash() == target.getTileHash());
    }
}
