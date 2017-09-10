package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.*;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.player.content.araxxor.AraxxorFight;
import com.rs.utils.Logger;
import com.rs.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public class AraxxiCombat extends CombatScript {

    @Override
    public int attack(NPC npc, Entity target) {
        final Araxxi araxxi = (Araxxi) npc;
        final AraxxorFight fight = araxxi.getFight();
        final NPCCombatDefinitions defs = araxxi.getCombatDefinitions();
        final boolean mage = araxxi.shouldMage();
        List<Entity> possibleTargets = araxxi.getPossibleTargets();
        if(possibleTargets.isEmpty()) {
            possibleTargets.addAll(fight.getPlayers());
            fight.getPlayers().forEach(p -> {
                if(p.hasFamiliar())
                    possibleTargets.add(p.getFamiliar());
            });
        } else {
            possibleTargets.addAll(new
                    ArrayList<>(possibleTargets)
                    .stream()
                    .filter(t -> t instanceof Player && ((Player) t).hasFamiliar())
                    .map(t -> ((Player) t).getFamiliar())
                    .collect(Collectors.toList()));
        }
        int tick = araxxi.getAndIncrementGlobalTick();
        if(tick == 5)
            return araxxi.performSpecial();
        if (mage)
            mageAutoAttack(araxxi, target, possibleTargets, defs);
        else
            rangeAutoAttack(araxxi, target, possibleTargets, defs);
        return araxxi.getAttackDelay();
    }

    @Override
    public Object[] getKeys() {
        return new Object[] { Araxyte.ARAXXI };
    }

    private void meleeAutoAttack(Araxxi araxxi, Entity target, NPCCombatDefinitions defs) {
        Hit unscaledHit = getMeleeHit(araxxi,
                getRandomMaxHit(araxxi, defs.getMaxHit(), NPCCombatDefinitions.MELEE, target));
        Hit scaledHit = araxxi.scaleBaseHit(target, unscaledHit.getDamage(), unscaledHit.getLook());
        if(scaledHit != null) {
            araxxi.setNextAnimation(new Animation(24094));
            delayHit(araxxi, 0, target, scaledHit);
        }
    }

    private void mageAutoAttack(Araxxi araxxi, Entity target,
                                 List<Entity> possibleTargets, NPCCombatDefinitions defs) {

        for(Entity entity : possibleTargets) {
            if (entity != null && !entity.isDead() && !entity.hasFinished()) {
                if(entity.withinDistance(araxxi, 1)) {
                    meleeAutoAttack(araxxi, entity, defs);
                    return;
                }
                float dmg = getRandomMaxHit(araxxi, defs.getMaxHit(),
                        NPCCombatDefinitions.MAGE, entity);
                dmg *= entity == target ? 1.0F : 0.75F;
                Hit unscaledHit = getMagicHit(araxxi, Math.round(dmg));
                Hit scaledHit = araxxi.scaleBaseHit(entity, unscaledHit.getDamage(), unscaledHit.getLook());
                World.sendProjectile(araxxi.getMiddleWorldTile(), entity,4979, 60, 30, 45, 0, 5, 200);
                World.sendGraphics(araxxi, new Graphics(4980), entity);
                delayHit(araxxi, 2, entity, scaledHit);
            }
        }
        araxxi.setNextAnimation(new Animation(24047));
        araxxi.setNextGraphics(new Graphics(4988, 0, 180));
    }
    private void rangeAutoAttack(Araxxi araxxi, Entity trueTarget,
                                 List<Entity> possibleTargets, NPCCombatDefinitions defs) {

        for(Entity entity : possibleTargets) {
            if (entity != null && !entity.isDead() && !entity.hasFinished()) {
                if(entity.withinDistance(araxxi, 1)) {
                    meleeAutoAttack(araxxi, entity, defs);
                    return;
                }
                float totalDamage = getRandomMaxHit(araxxi, defs.getMaxHit(),
                        NPCCombatDefinitions.RANGE, entity);
                totalDamage *= entity == trueTarget ? 1.0F : 0.75F;
                Hit unscaledHit = getRangeHit(araxxi, Math.round(totalDamage));
                Hit scaledHit = araxxi.scaleBaseHit(entity, unscaledHit.getDamage(), unscaledHit.getLook());
                World.sendProjectile(araxxi.getMiddleWorldTile(), entity, 4997, 60, 30, 45, 0, 5, 200);
                World.sendGraphics(araxxi, new Graphics(4993), entity);
                delayHit(araxxi, 1, entity, scaledHit);
                entity.getPoison().makePoisoned(150);
            }
        }
        araxxi.setNextAnimation(new Animation(24047));
        araxxi.setNextGraphics(new Graphics(4988, 0, 180));
    }
}
