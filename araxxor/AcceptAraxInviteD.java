package com.rs.game.player.content.araxxor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.rs.game.player.Player;
import com.rs.game.player.dialogue.Dialogue;
import com.rs.utils.Colors;

/**
 * <br/><br/>
 * This project is primarily the work of dlo3, with contributions
 * from Ataraxia devs Kris, Armak1ing, and CJay. It may only be distributed
 * <b>AS-IS</b>, and <b>FREE OF CHARGE</b>, with dlo3's consent.
 * Furthermore, this notice must remain in all distributions.
 * @author David O'Neill (dlo3)
 * @since 1.0 (alpha, 08/12/2017)<br/>
 *        1.1 (beta, 08/19/2017)
 */
public class AcceptAraxInviteD extends Dialogue {

    private Player leader;

    @Override
    public void start() {
        leader = (Player) parameters[0];
        stage = 0;
        sendDialogue(leader.getDisplayName() + " has invited you to join  " +
                    (leader.getAppearence().isMale() ? "his " : "her ") +
                    " battle at the Araxyte hive.", Colors.RED + "There is no way " +
                    "out other than death or Victory.");
    }

    @Override
    public void run(int interfaceId, int componentId) {
        if(stage == 0) {
            stage = 1;
            sendOptionsDialogue("Join " + leader.getDisplayName() + " in the Araxyte hive?",
                    "Yes, I'm ready.",
                    "No, I don't wish to join.");
        } else if(stage == 1) {
            switch(componentId) {
                case OPTION_1:
                    List<Player> group = new CopyOnWriteArrayList<>();
                    group.add(leader);
                    group.add(player);
                    AraxxorManager.generateFight(group);
                    end();
                    break;
                case OPTION_2:
                    leader.sendMessage(Colors.RED + player.getDisplayName()
                            + " has declined your offer.");
                    end();
                    break;
            }
        }
    }

    @Override
    public void finish() {

    }
}
