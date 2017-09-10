package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.Entity;
import com.rs.game.Graphics;
import com.rs.game.WorldTile;
import com.rs.game.npc.NPC;
import com.rs.utils.Logger;

/**
 * Abstract base class for Araxyte minion NPCs. All minion subtypes
 * inherit the {@link AraxyteMinion#performRole()} method, with
 * type-specific implementations. Araxxor and Araxxi are capable
 * of spawning 5 different types of minions. See the subclasses
 * for their roles.
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
abstract class AraxyteMinion extends NPC {

    private static final long serialVersionUID = 7018370907786472835L;

    protected Araxyte parent;

    AraxyteMinion(int id, WorldTile tile, Araxyte parent) {
        super(id, tile, -1, true, true);
        this.parent = parent;
        this.parent.addMinion(this);
        setForceMultiArea(true);
        setNoDistanceCheck(true);
        setCombat(new AraxyteCombatHandler(this));
    }

    @Override
    public void sendDeath(final Entity source) {
        parent.removeMinion(this);
        setNextGraphics(new Graphics(5006));
        super.sendDeath(source);
    }

    @Override
    public int getCapDamage() {
        return 1250;
    }

    @Override
    public boolean canWalkNPC(int toX, int toY) {
        return true;
    }

    @Override
    public double getMeleePrayerMultiplier() {
        return 0.25;
    }

    @Override
    public double getRangePrayerMultiplier() {
        return 0.25;
    }

    @Override
    public double getMagePrayerMultiplier() {
        return 0.25;
    }


    protected abstract void performRole();

    /**
     * Small enum serving as a key producer for the {@link MinionFactory}.
     */
    enum MinionType {

        BLADED(19458),
        SPITTING(19460),
        IMBUED(19459),
        PULSING(19469),
        MIRRORBACK(19468);

        protected int npcId;

        MinionType(int npcId) {
            this.npcId = npcId;
        }

    }
}
