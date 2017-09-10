package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Animation;
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
final class MirrorbackMinion extends AraxyteMinion {

    private static final long serialVersionUID = 7619027551536727667L;

    MirrorbackMinion(int id, WorldTile tile, Araxyte parent) {
        super(id, tile, parent);
        setForceAgressive(false);
    }

    @Override
    public void processHit(Hit hit) {
        final int damage = hit.getDamage();
        Hit scaledHit = new Hit(hit.getSource(), damage / 2, hit.getLook());
        parent.applyHit(new Hit(MirrorbackMinion.this, damage / 2, hit.getLook()));
        super.processHit(scaledHit);
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
                if(MirrorbackMinion.this.isDead() || MirrorbackMinion.this.hasFinished()) {
                    stop();
                    sendDeath(null);
                    return;
                }
                if(parent.isDead() || parent.hasFinished()) {
                    stop();
                    sendDeath(null);
                    return;
                }
                if(tick % 5 == 0 && MirrorbackMinion.this.getAttackedBy() == null) {
                    MirrorbackMinion.this.setNextGraphics(new Graphics(5003));
                    MirrorbackMinion.this.setNextFaceEntity(parent);
                    parent.setNextGraphics(new Graphics(5004));
                    parent.applyHit(new Hit(MirrorbackMinion.this, 100, Hit.HitLook.HEALED_DAMAGE));
                }
                tick++;
            }

        }, 0, 1);

    }
}
