package com.rs.game.player.content.araxxor.npcs;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.utils.Utils;

import java.util.List;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
final class BladedMinion extends AraxyteMinion {

    private static final long serialVersionUID = -6224300652348068052L;

    BladedMinion(int id, WorldTile tile, Araxyte parent) {
        super(id, tile, parent);
    }

    @Override
    protected void performRole() {
        setForceAgressive(true);
        setRun(true);
        setIntelligentRouteFinder(true);
        setTarget(parent.fight.getPlayers().get(Utils.random(parent.fight.getPlayers().size())));
    }
}
