package com.rs.game.player.content.araxxor;

import com.rs.game.WorldTile;
import com.rs.game.player.Player;
import com.rs.game.player.cutscenes.Cutscene;
import com.rs.game.player.cutscenes.actions.CutsceneAction;
import com.rs.game.player.cutscenes.actions.LookCameraAction;
import com.rs.game.player.cutscenes.actions.PosCameraAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)
 */
public class LightPathCutscene extends Cutscene {


    @Override
    public CutsceneAction[] getActions(Player player) {
        AraxxorFight fight = AraxxorManager.fightOf(player);
        Objects.requireNonNull(fight);
        List<CutsceneAction> actions = new ArrayList<>();
        WorldTile firstAnchorPos = fight.getConfig().getCutsceneAnchorOne();
        WorldTile firstLookPos = fight.getConfig().getCutsceneLookOne();
        WorldTile secondAnchorPos = fight.getConfig().getCutsceneAnchorTwo();
        WorldTile secondLookPos = fight.getConfig().getCutsceneLookTwo();
        actions.add(new PosCameraAction(getX(player, firstAnchorPos.getX()),
                getY(player, firstAnchorPos.getY()), 2800, -1));
        actions.add(new LookCameraAction(getX(player, firstLookPos.getX()),
                getY(player, firstLookPos.getY()), 2000,
                5));
        actions.add(new PosCameraAction(getX(player, secondAnchorPos.getX()),
                getY(player, secondAnchorPos.getY()), 3500, -1));
        actions.add(new LookCameraAction(getX(player, secondLookPos.getX()),
                getY(player, secondLookPos.getY()), 3500,
                5));
        return actions.toArray(new CutsceneAction[actions.size()]);
    }

    @Override
    public boolean hiddenMinimap() {
        return true;
    }
}
