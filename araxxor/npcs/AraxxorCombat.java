package com.rs.game.player.content.araxxor.npcs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.rs.cores.CoresManager;
import com.rs.game.Animation;
import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.AraxxorEnvironment;
import com.rs.game.player.content.araxxor.AraxxorFight;
import com.rs.utils.Logger;
import com.rs.utils.Utils;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public class AraxxorCombat extends CombatScript {

    @Override
    public int attack(NPC npc, Entity target) {
        final Araxxor araxxor = (Araxxor) npc;
        Logger.log(Logger.DEBUG, araxxor.enrage);
        final NPCCombatDefinitions defs = araxxor.getCombatDefinitions();
        final AraxxorFight fight = araxxor.getFight();
        int style = araxxor.getOriginalId();
        int phase = araxxor.getFight().getPhase();
        if(phase >= 1) {
            int tick = araxxor.getAndIncrementGlobalTick();
            if(fight.getConfig().getActivePath() == AraxxorEnvironment.Paths.MINIONS) {
                if(phase >=2 && tick == araxxor.getNextMinionSpawn()
                        && (araxxor.getMinions() == null ||
                        araxxor.getMinions().isEmpty()) && araxxor.getMinionSpawns() < 4) {
                    araxxor.performMinionSpawn();
                    return 1;
                } else {
                    araxxor.skipMinionTick();
                }
            }
            if(tick == 5) {
                return araxxor.performSpecial();
            }
            else {
                List<Entity> possibleTargets = araxxor.getPossibleTargets();
                if(possibleTargets.isEmpty()) {
                    possibleTargets.addAll(fight.getPlayers());
                    fight.getPlayers().forEach(p -> {
                        if(p.hasFamiliar())
                            possibleTargets.add(p.getFamiliar());
                    });
                } else {
                    possibleTargets.addAll(new ArrayList<>(possibleTargets)
                            .stream()
                            .filter(t -> t instanceof Player &&
                                    ((Player) t).hasFamiliar())
                            .map(t -> ((Player) t).getFamiliar())
                            .collect(Collectors.toList()));
                }
                switch(style) {
                    case Araxyte.ARAXXOR_NPC_ID_MAGE:
                        mageAutoAttack(araxxor, target, possibleTargets, defs);
                        return (phase == 2 ? 10 : 14) - (araxxor.enrage > 75.0F ? 2 :
                                araxxor.enrage > 150.0F ? 4 : araxxor.enrage > 200.0F ? 6 : 0) ;
                    case Araxyte.ARAXXOR_NPC_ID_MELEE:
                        meleeAutoAttack(araxxor, target, defs);
                        return (phase == 2 ? 10 : 14) - (araxxor.enrage > 75.0F ? 2 :
                                araxxor.enrage > 150.0F ? 4 : araxxor.enrage > 200.0F ? 6 : 0);
                    case Araxyte.ARAXXOR_NPC_ID_RANGE:
                        rangeAutoAttack(araxxor, target, possibleTargets, defs);
                        return (phase == 2 ? 10 : 14) - (araxxor.enrage > 75.0F ? 2 :
                            araxxor.enrage > 150.0F ? 4 : araxxor.enrage > 200.0F ? 6 : 0);
                }
            }
        }
        return (phase == 2 ? 5 : 7) - (araxxor.enrage > 75.0F ? 1 :
                araxxor.enrage > 150.0F ? 2 : araxxor.enrage > 200.0F ? 3 : 0);
    }

    @Override
    public Object[] getKeys() {
        return new Object[] {
            Araxyte.ARAXXOR_NPC_ID_MAGE,
            Araxyte.ARAXXOR_NPC_ID_MELEE,
            Araxyte.ARAXXOR_NPC_ID_RANGE,
            Araxyte.ARAXXOR_NPC_ID_ACIDIC
        };
    }

    private void meleeAutoAttack(Araxxor araxxor, Entity target, NPCCombatDefinitions defs) {
        Hit unscaledHit = getMeleeHit(araxxor,
                getRandomMaxHit(araxxor, defs.getMaxHit(), NPCCombatDefinitions.MELEE, target));
        Hit scaledHit = araxxor.scaleBaseHit(target, unscaledHit.getDamage(), unscaledHit.getLook());
        if(scaledHit != null) {
            araxxor.setNextAnimation(new Animation(24094));
            delayHit(araxxor, 0, target, scaledHit);
        }
    }

    private void mageAutoAttack(Araxxor araxxor, Entity trueTarget,
                                 List<Entity> possibleTargets, NPCCombatDefinitions defs) {

        for(Entity entity : possibleTargets) {
            if (entity != null && !entity.isDead() && !entity.hasFinished()) {
                if(entity.withinDistance(araxxor, 3)) {
                    meleeAutoAttack(araxxor, entity, defs);
                    return;
                }
                float totalDamage = getRandomMaxHit(araxxor, defs.getMaxHit(),
                        NPCCombatDefinitions.RANGE, entity);
                totalDamage *= entity == trueTarget ? 1.0F : 0.75F;
                Hit unscaledHit = getMagicHit(araxxor, Math.round(totalDamage));
                Hit scaledHit = araxxor.scaleBaseHit(entity, unscaledHit.getDamage(), unscaledHit.getLook());
                if(scaledHit != null) {
                    World.sendProjectile(araxxor.getMiddleWorldTile(), entity, 4979, 60, 30, 45, 0, 5, 200);
                    CoresManager.getServiceProvider().executeWithDelay(() ->
                            World.sendGraphics(araxxor, new Graphics(4980), entity), 1);
                    delayHit(araxxor, 1, entity, scaledHit);
                }
            }
        }
        araxxor.setNextAnimation(new Animation(24047));
        araxxor.setNextGraphics(new Graphics(4988, 0, 180));
    }

    private void rangeAutoAttack(Araxxor araxxor, Entity trueTarget,
                                List<Entity> possibleTargets, NPCCombatDefinitions defs) {
        for(Entity entity : possibleTargets) {
            if (entity != null && !entity.isDead() && !entity.hasFinished()) {
                if(entity.withinDistance(araxxor, 1)) {
                    meleeAutoAttack(araxxor, entity, defs);
                    return;
                }
                float totalDamage = getRandomMaxHit(araxxor, defs.getMaxHit(),
                        NPCCombatDefinitions.RANGE, entity);
                totalDamage *= entity == trueTarget ? 1.0F : 0.75F;
                Hit unscaledHit = getRangeHit(araxxor, Math.round(totalDamage));
                Hit scaledHit = araxxor.scaleBaseHit(entity, unscaledHit.getDamage(), unscaledHit.getLook());
                if(scaledHit != null) {
                    World.sendProjectile(araxxor.getMiddleWorldTile(), entity, 4997, 60, 30, 45, 0, 5, 200);

                    delayHit(araxxor, 1, entity, scaledHit);
                    CoresManager.getServiceProvider().executeWithDelay(() ->
                            World.sendGraphics(araxxor, new Graphics(4993), entity), 1);
                    entity.getPoison().makePoisoned(150);
                }
            }
        }
        araxxor.setNextAnimation(new Animation(24047));
        araxxor.setNextGraphics(new Graphics(4988, 0, 180));
    }

}
