package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Entity;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.CombatScriptsHandler;
import com.rs.game.npc.combat.NPCCombat;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.route.RouteFinder;
import com.rs.game.route.strategy.FixedTileStrategy;
import com.rs.utils.MapAreas;
import com.rs.utils.Utils;

/**
 * A combat handler object which improves combat pathfinding.
 * Credits entirely to Kris.
 * @author Kris, David O'Neill
 */
final class AraxyteCombatHandler extends NPCCombat {

    AraxyteCombatHandler(NPC npc) {
        super(npc);
    }

    @Override
    public int combatAttack() {
        Entity target = this.target;
        if (target == null || npc.isLocked())
            return 0;
        int attackStyle = npc.getCombatDefinitions().getAttackStyle();
        int maxDistance = attackStyle == NPCCombatDefinitions.MELEE || attackStyle == NPCCombatDefinitions.SPECIAL2 ? 0 : 7;
        if (npc.getFreezeDelay() >= Utils.currentTimeMillis()) {
            if (NPCCombatDefinitions.MELEE == attackStyle && !npc.withinDistance(target, 1))
                return 0;
            if (attackStyle != NPCCombatDefinitions.MELEE && !npc.withinDistance(target, maxDistance))
                return 0;
        }
        if (!npc.clipedProjectile(target, maxDistance == 0))
            return 0;
        int distanceX = target.getX() - npc.getX();
        int distanceY = target.getY() - npc.getY();
        int size = npc.getSize();
        if (distanceX > size + maxDistance ||
                distanceX < -size - maxDistance ||
                distanceY > size + maxDistance ||
                distanceY < -size - maxDistance)
            return 0;
        addAttackedByDelay(target);
        return CombatScriptsHandler.specialAttack(npc, target);
    }

    @Override
    public boolean checkAll() {
        final Entity target = this.target;
        if (target == null || npc.isDead() || npc.hasFinished() ||
                npc.isForceWalking() ||
                target.isDead() ||
                target.hasFinished() ||
                npc.getPlane() != target.getPlane() ||
                npc.isCantInteract() || npc.isCannotMove())
            return false;
        if (npc.getFreezeDelay() >= Utils.currentTimeMillis())
            return true;
        int distanceX = target.getX() - npc.getX();
        int distanceY = target.getY() - npc.getY();
        int size = npc.getSize();
        if (target.getX() < npc.getX() || target.getY() < npc.getY())
            size = npc.getSize() > target.getSize() ? npc.getSize() : target.getSize();
        int maxDistance;
        if (!npc.isNoDistanceCheck() && !npc.isCantFollowUnderCombat()) {
            maxDistance = 16;
            if (npc.getMapAreaNameHash() != -1) {
                if (!MapAreas.isAtArea(npc.getMapAreaNameHash(), npc) ||
                        (!npc.canBeAttackFromOutOfArea() &&
                                !MapAreas.isAtArea(npc.getMapAreaNameHash(), target))) {
                    npc.forceWalkRespawnTile();
                    return false;
                }
            } else if (distanceX > size + maxDistance ||
                    distanceX < -1 - maxDistance ||
                    distanceY > size + maxDistance ||
                    distanceY < -1 - maxDistance) {
                npc.forceWalkRespawnTile();
                return false;
            }
            if (distanceX > size + maxDistance ||
                    distanceX < -1 - maxDistance ||
                    distanceY > size + maxDistance ||
                    distanceY < -1 - maxDistance)
                return false;
        }
        if (!npc.isForceMultiAttacked()) {
            if (!target.isAtMultiArea() || !npc.isAtMultiArea()) {
                if (npc.getAttackedBy() != target && npc.getAttackedByDelay() > Utils.currentTimeMillis())
                    return false;
                if (target.getAttackedBy() != npc && target.getAttackedByDelay() > Utils.currentTimeMillis())
                    return false;
            }
        }
        if (!npc.isCantFollowUnderCombat()) {
            final int targetSize = target.getSize();
            if (distanceX < size && distanceX > -targetSize &&
                    distanceY < size && distanceY > -targetSize && !target.hasWalkSteps()) {
                npc.resetWalkSteps();
                if (!npc.addWalkSteps(target.getX() + 1, npc.getY())) {
                    npc.resetWalkSteps();
                    if (!npc.addWalkSteps(target.getX() - size, npc.getY())) {
                        npc.resetWalkSteps();
                        if (!npc.addWalkSteps(npc.getX(), target.getY() + 1)) {
                            npc.resetWalkSteps();
                            if (!npc.addWalkSteps(npc.getX(), target.getY() - size)) {
                                return true;
                            }
                        }
                    }
                }
                return true;
            }

            if (npc.getCombatDefinitions().getAttackStyle() == NPCCombatDefinitions.MELEE
                    && targetSize == 1 && size == 1
                    && Math.abs(npc.getX() - target.getX()) == 1
                    && Math.abs(npc.getY() - target.getY()) == 1
                    && !target.hasWalkSteps()) {
                if (!npc.addWalkSteps(target.getX(), npc.getY(), 1))
                    npc.addWalkSteps(npc.getX(), target.getY(), 1);
                return true;
            }
            final int attackStyle = npc.getCombatDefinitions().getAttackStyle();
            maxDistance = npc.isForceFollowClose() ? 0 :
                    (attackStyle == NPCCombatDefinitions.MELEE
                            || attackStyle == NPCCombatDefinitions.SPECIAL2) ? 0 : 7;
            npc.resetWalkSteps();
            if ((!npc.clipedProjectile(target, maxDistance == 0)) ||
                    distanceX > size + maxDistance ||
                    distanceX < -1 - maxDistance ||
                    distanceY > size + maxDistance ||
                    distanceY < -1 - maxDistance) {
                int steps = RouteFinder.findRoute(RouteFinder.WALK_ROUTEFINDER,
                        npc.getX(), npc.getY(), npc.getPlane(),
                        npc.getSize(), new FixedTileStrategy(target.getX(), target.getY()), true);
                int[] bufferX = RouteFinder.getLastPathBufferX();
                int[] bufferY = RouteFinder.getLastPathBufferY();
                if (combatDelay < 3)
                    combatDelay = 3;
                for (int i = steps - 1; i >= 0; i--) {
                    if (!npc.addWalkSteps(bufferX[i], bufferY[i], 25, true))
                        break;
                }
                return true;
            }

        }
        return true;
    }
}
