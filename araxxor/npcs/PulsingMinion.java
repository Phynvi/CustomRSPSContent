package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Graphics;
import com.rs.game.Hit;
import com.rs.game.WorldTile;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
final class PulsingMinion extends AraxyteMinion {

    private static final long serialVersionUID = -9041567740231276394L;

    private boolean distracted;

    PulsingMinion(int id, WorldTile tile, Araxyte parent) {
        super(id, tile, parent);
        setForceAgressive(false);
    }

    @Override
    public void processNPC() {
        if (isDead() || isCantInteract())
            return;
        if(getAttackedBy() != null) {
            setTarget(getAttackedBy());
            distracted = true;
        }
        else {
            setTarget(null);
            distracted = false;
        }
        super.processNPC();
    }

    @Override
    protected void performRole() {
        WorldTasksManager.schedule(new WorldTask() {

            int tick = 0;

            @Override
            public void run() {
                if(parent.fight.shuttingDown()) {
                    stop();
                    return;
                }
                if(PulsingMinion.this.isDead() || PulsingMinion.this.hasFinished()) {
                    stop();
                    sendDeath(null);
                    return;
                }
                if(parent.isDead() || parent.hasFinished()) {
                    stop();
                    sendDeath(null);
                    return;
                }
                if(tick % 5 == 0 && !distracted) {
                    PulsingMinion.this.setNextFaceEntity(parent);
                    PulsingMinion.this.setNextGraphics(new Graphics(5003));
                    parent.setNextGraphics(new Graphics(5004));
                    parent.applyHit(new Hit(PulsingMinion.this, 100, Hit.HitLook.HEALED_DAMAGE));
                }

                tick++;
            }

        }, 0, 1);
    }
}
