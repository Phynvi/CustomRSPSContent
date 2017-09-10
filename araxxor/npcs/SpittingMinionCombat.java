package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Entity;
import com.rs.game.Hit;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScript;
import com.rs.game.npc.combat.NPCCombatDefinitions;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public class SpittingMinionCombat extends CombatScript {

    @Override
    public int attack(NPC npc, Entity target) {
        SpittingMinion minion = (SpittingMinion) npc;
        final NPCCombatDefinitions defs = minion.getCombatDefinitions();
        rangeAutoAttack(minion, target, defs);
        return 5;
    }

    private void rangeAutoAttack(SpittingMinion minion, Entity target, NPCCombatDefinitions defs) {

        if (target != null && !target.isDead() && !target.hasFinished()) {
            int totalDamage = getRandomMaxHit(minion, defs.getMaxHit(),
                    NPCCombatDefinitions.RANGE, target);
            Hit hit = getRangeHit(minion, totalDamage);

            World.sendProjectile(minion.getMiddleWorldTile(), target, 4997, 60, 30, 45, 0, 5, 200);
            delayHit(minion, 1, target, hit);
            // TODO : find and perform attack animation
        }
    }

    @Override
    public Object[] getKeys() {
        return new Object[] { AraxyteMinion.MinionType.SPITTING.npcId };
    }
}
