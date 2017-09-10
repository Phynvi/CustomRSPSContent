package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.route.RouteFinder;
import com.rs.game.route.strategy.FixedTileStrategy;
import com.rs.utils.Logger;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
final class InstakillMinion extends NPC {

    private static final long serialVersionUID = 1127898547879748202L;

    private Player killTarget;

    InstakillMinion(int id, WorldTile tile) {
        super(id, tile, -1, true, true);
        Logger.log(Logger.DEBUG, "Instakill minion created");
        setForceAgressive(false);
        setRun(true);
    }

    void setKillTarget(Player target) {
        this.killTarget = target;
    }

    Player getKillTarget() {
        return killTarget;
    }

    void findIntelligentRoute() {
        int steps = RouteFinder.findRoute(RouteFinder.WALK_ROUTEFINDER, getX(), getY(),
                getPlane(), getSize(), new FixedTileStrategy(killTarget.getX(), killTarget.getY()),
                true);
        int[] bufferX = RouteFinder.getLastPathBufferX();
        int[] bufferY = RouteFinder.getLastPathBufferY();
        for (int i = steps - 1; i >= 0; i--) {
            if (!addWalkSteps(bufferX[i], bufferY[i], 25, true))
                break;
        }
    }

    void detonate() {
        setNextGraphics(new Graphics(5006));
        killTarget.applyHit(new Hit(this, killTarget.getMaxHitpoints(), Hit.HitLook.CRITICAL_DAMAGE));
        sendDeath(killTarget);
    }

    @Override
    public void processHit(Hit hit) {
        hit.setDamage(0);
        super.processHit(hit);
    }
}
