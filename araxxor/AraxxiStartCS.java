package com.rs.game.player.content.araxxor;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.cutscenes.Cutscene;
import com.rs.game.player.cutscenes.actions.CutsceneAction;
import com.rs.game.player.cutscenes.actions.LookCameraAction;
import com.rs.game.player.cutscenes.actions.PosCameraAction;

import java.util.ArrayList;
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
public class AraxxiStartCS extends Cutscene {

    @Override
    public CutsceneAction[] getActions(Player player) {

        final AraxxorEnvironment config = AraxxorManager.fightOf(player).getConfig();

        List<CutsceneAction> actions = new ArrayList<>();

        WorldTile anchor = config.getCutsceneAnchorThree();
        WorldTile look = config.getCutsceneLookThree();

        actions.add(new PosCameraAction(getX(player, anchor.getX()),
                getY(player, anchor.getY()), 2800, -1));
        actions.add(new LookCameraAction(getX(player, look.getX()),
                getY(player, look.getY()), 2000, 2));

        return actions.toArray(new CutsceneAction[actions.size()]);
    }

    @Override
    public boolean hiddenMinimap() {
        return false;
    }

    @Override
    public void stopCutscene(Player player) {
        stopWithoutReplacement(player);
    }
}
